package ru.fixbyte.crm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
// HttpUrl используется в сигнатуре CookieJar
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

data class ClientBrief(
    val id: Long,
    val name: String,
    val phone: String,
    val type: String?,
    val tags: String?,
    val archived: Boolean
)

data class ClientDetail(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val type: String?,
    val tags: String?,
    val notes: String?,
    val preferredChannel: String?,
    val archived: Boolean
)

data class ClientSummary(val ordersCount: Long, val revenue: Double, val debt: Double)

data class ClientPhoto(val id: Long, val url: String, val caption: String?, val createdAt: String?)

sealed interface CreateClientResult {
    /** Клиент создан. [orderFailed] = попытка завести первичную заявку не удалась. */
    data class Created(val id: Long, val orderFailed: Boolean = false) : CreateClientResult
    data class Duplicate(val id: Long, val name: String) : CreateClientResult
}

/**
 * Cookie сессии, сохраняются в SharedPreferences — чтобы вход не слетал
 * между запусками приложения.
 */
private class PrefsCookieJar(private val prefs: Prefs) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host
        val current = prefs.loadCookies(host).toMutableMap()
        cookies.forEach { current[it.name] = it.toString() }
        prefs.saveCookies(host, current.values.toSet())
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return prefs.loadCookies(url.host).mapNotNull { Cookie.parse(url, it) }
    }

    private fun Set<String>.toMutableMap(): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        forEach { raw -> raw.substringBefore('=').trim().let { name -> map[name] = raw } }
        return map
    }
}

/**
 * Единственная точка доступа к API FixByte CRM.
 */
object Api {

    lateinit var prefs: Prefs
        private set

    fun init(context: Context) {
        prefs = Prefs(context.applicationContext)
    }

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(PrefsCookieJar(prefs))
            // Не идти автоматически по 302 → /login: иначе неавторизованный
            // ответ выглядит как успешный (200 + HTML формы входа).
            .followRedirects(false)
            .followSslRedirects(false)
            // Сессия протухла → войти повторно сохранёнными данными и повторить запрос.
            .addInterceptor(ReauthInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** Клиент без интерцептора повторного входа — только для самого /api/auth/login. */
    private val bareHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(PrefsCookieJar(prefs))
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun base() = prefs.baseUrl.trimEnd('/')
    private fun u(path: String) = base() + path

    /** Полный URL для картинки (photo.url приходит относительным). */
    fun absoluteUrl(path: String) = if (path.startsWith("http")) path else base() + path

    // ── Повторный вход при протухшей сессии ───────────────────

    private val reauthLock = Any()
    @Volatile private var lastReauthAt = 0L

    /** Признак того, что ответ — это «нужно авторизоваться» (401/403 или редирект на /login). */
    private fun isAuthFailure(r: Response): Boolean {
        if (r.code == 401 || r.code == 403) return true
        if (r.isRedirect) return (r.header("Location") ?: "").contains("/login")
        return false
    }

    /** Синхронный вход сохранёнными данными. Вызывается из интерцептора. */
    private fun loginSync(username: String, password: String): Boolean {
        val body = JSONObject().put("username", username).put("password", password)
            .toString().toRequestBody(JSON)
        return runCatching {
            bareHttp.newCall(Request.Builder().url(u("/api/auth/login")).post(body).build())
                .execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun tryReauth(): Boolean {
        synchronized(reauthLock) {
            // Другой поток только что успешно перелогинился — просто повторяем запрос.
            if (System.currentTimeMillis() - lastReauthAt < 3000) return true
            val user = prefs.lastUsername
            val pass = prefs.password
            if (user.isBlank() || pass.isBlank()) return false
            val ok = loginSync(user, pass)
            if (ok) lastReauthAt = System.currentTimeMillis()
            return ok
        }
    }

    private class ReauthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            if (!Api.isAuthFailure(response)) return response
            if (request.url.encodedPath.startsWith("/api/auth/")) return response
            if (!Api.tryReauth()) return response
            response.close()
            return chain.proceed(request)
        }
    }

    /**
     * Убедиться, что вход выполнен: если серверная сессия недействительна,
     * попробовать войти сохранёнными логином/паролем.
     */
    suspend fun ensureLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        if (isLoggedIn()) return@withContext true
        if (!prefs.hasCredentials()) return@withContext false
        runCatching { login(prefs.lastUsername, prefs.password); true }.getOrDefault(false)
    }

    // ── Аутентификация ────────────────────────────────────────

    suspend fun login(username: String, password: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("username", username).put("password", password)
            .toString().toRequestBody(JSON)
        http.newCall(Request.Builder().url(u("/api/auth/login")).post(body).build()).execute().use { r ->
            if (!r.isSuccessful) throw ApiException(errorText(r) ?: "Не удалось войти (${r.code})")
        }
        // Запоминаем — чтобы дальше входить автоматически (в т.ч. после протухания сессии).
        prefs.lastUsername = username
        prefs.password = password
    }

    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(Request.Builder().url(u("/api/auth/me")).build()).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(Request.Builder().url(u("/api/auth/logout")).post(EMPTY).build()).execute().close()
        }
        prefs.clearCookies()
        prefs.clearCredentials()
    }

    // ── Клиенты ───────────────────────────────────────────────

    suspend fun clients(): List<ClientBrief> = withContext(Dispatchers.IO) {
        get("/api/clients").let { txt ->
            val arr = JSONArray(txt)
            (0 until arr.length()).map { brief(arr.getJSONObject(it)) }
        }
    }

    suspend fun searchClients(query: String): List<ClientBrief> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val arr = JSONArray(get("/api/clients/search?name=$q"))
        (0 until arr.length()).map { brief(arr.getJSONObject(it)) }
    }

    suspend fun client(id: Long): ClientDetail = withContext(Dispatchers.IO) {
        detail(JSONObject(get("/api/clients/$id")))
    }

    /**
     * Создать клиента. При дубле телефона сервер отдаёт 409 + существующую карточку.
     *
     * Если заданы [problem] (жалоба на ПК) и/или [estimatePrice] (озвученная
     * примерная цена) — следом заводится первичная заявка (заказ со статусом NEW):
     * описание = проблема, строка «Предварительная оценка» = озвученная цена.
     */
    suspend fun createClient(
        name: String,
        phone: String,
        type: String,
        problem: String? = null,
        estimatePrice: Double? = null
    ): CreateClientResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("name", name).put("phone", phone).put("type", type).put("isActive", true)
            .toString().toRequestBody(JSON)

        val result = http.newCall(Request.Builder().url(u("/api/clients")).post(body).build()).execute().use { r ->
            val txt = r.body?.string().orEmpty()
            when {
                r.isSuccessful -> CreateClientResult.Created(JSONObject(txt).getLong("id"))
                r.code == 409 -> {
                    val o = JSONObject(txt)
                    CreateClientResult.Duplicate(o.getLong("id"), o.optString("name", ""))
                }
                else -> throw ApiException(parseErr(txt) ?: "Не удалось создать клиента (${r.code})")
            }
        }

        if (result is CreateClientResult.Created) {
            val hasProblem = !problem.isNullOrBlank()
            val hasPrice = (estimatePrice ?: 0.0) > 0.0
            if (hasProblem || hasPrice) {
                val ok = runCatching {
                    createInitialOrder(result.id, problem?.trim(), estimatePrice)
                }.isSuccess
                return@withContext result.copy(orderFailed = !ok)
            }
        }
        result
    }

    /** Первичная заявка для только что созданного клиента. */
    private fun createInitialOrder(clientId: Long, deviceDescription: String?, estimatePrice: Double?) {
        val order = JSONObject()
            .put("client", JSONObject().put("id", clientId))
            .put("status", "NEW")
            .put("paidAmount", 0)
            .put("notes", "Первичная заявка (моб. приложение)")
        if (!deviceDescription.isNullOrBlank()) order.put("deviceDescription", deviceDescription)

        val lines = JSONArray()
        if ((estimatePrice ?: 0.0) > 0.0) {
            lines.put(
                JSONObject()
                    .put("name", "Предварительная оценка (со слов клиента)")
                    .put("unitPrice", estimatePrice)
                    .put("quantity", 1)
            )
        }
        order.put("lines", lines)

        http.newCall(
            Request.Builder().url(u("/api/orders")).post(order.toString().toRequestBody(JSON)).build()
        ).execute().use { r ->
            if (!r.isSuccessful) throw ApiException("Заявка не создана (${r.code})")
        }
    }

    /** Скачать файл (например фото) тем же клиентом — с cookie сессии. */
    suspend fun downloadBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw ApiException("Не удалось получить файл (${r.code})")
            r.body?.bytes() ?: ByteArray(0)
        }
    }

    suspend fun summary(id: Long): ClientSummary = withContext(Dispatchers.IO) {
        val o = JSONObject(get("/api/clients/$id/summary"))
        ClientSummary(
            o.optLong("ordersCount"),
            o.optDouble("revenue", 0.0),
            o.optDouble("debt", 0.0)
        )
    }

    suspend fun photos(id: Long): List<ClientPhoto> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("/api/clients/$id/photos"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ClientPhoto(
                o.getLong("id"),
                o.optString("url", ""),
                o.optStringOrNull("caption"),
                o.optStringOrNull("createdAt")
            )
        }
    }

    suspend fun uploadPhoto(
        clientId: Long,
        bytes: ByteArray,
        filename: String,
        mime: String,
        caption: String?
    ) = withContext(Dispatchers.IO) {
        val part = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "files", filename,
                bytes.toRequestBody(mime.toMediaType(), 0, bytes.size)
            )
        if (!caption.isNullOrBlank()) part.addFormDataPart("caption", caption.trim())

        http.newCall(Request.Builder().url(u("/api/clients/$clientId/photos")).post(part.build()).build())
            .execute().use { r ->
                if (isAuthFailure(r)) throw ApiException("Сессия истекла — проверьте логин и пароль в настройках")
                if (!r.isSuccessful) throw ApiException(errorText(r) ?: "Не удалось загрузить фото (${r.code})")
            }
    }

    suspend fun deletePhoto(clientId: Long, photoId: Long) = withContext(Dispatchers.IO) {
        http.newCall(
            Request.Builder().url(u("/api/clients/$clientId/photos/$photoId")).delete().build()
        ).execute().use { r ->
            if (!r.isSuccessful && r.code != 204) throw ApiException("Не удалось удалить (${r.code})")
        }
    }

    // ── Вспомогательное ───────────────────────────────────────

    private fun get(path: String): String {
        http.newCall(Request.Builder().url(u(path)).build()).execute().use { r ->
            if (isAuthFailure(r)) throw ApiException("Сессия истекла, войдите заново")
            if (!r.isSuccessful) throw ApiException("Ошибка запроса (${r.code})")
            return r.body?.string().orEmpty()
        }
    }

    private fun brief(o: JSONObject) = ClientBrief(
        o.getLong("id"),
        o.optString("name", ""),
        o.optString("phone", ""),
        o.optStringOrNull("type"),
        o.optStringOrNull("tags"),
        o.optStringOrNull("archivedAt") != null
    )

    private fun detail(o: JSONObject) = ClientDetail(
        o.getLong("id"),
        o.optString("name", ""),
        o.optString("phone", ""),
        o.optStringOrNull("email"),
        o.optStringOrNull("address"),
        o.optStringOrNull("type"),
        o.optStringOrNull("tags"),
        o.optStringOrNull("notes"),
        o.optStringOrNull("preferredChannel"),
        o.optStringOrNull("archivedAt") != null
    )

    private fun errorText(r: okhttp3.Response): String? = parseErr(r.body?.string().orEmpty())

    private fun parseErr(body: String): String? {
        if (body.isBlank()) return null
        return runCatching { JSONObject(body).optString("error", body) }.getOrDefault(body)
    }

    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val EMPTY = ByteArray(0).toRequestBody(null, 0, 0)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).ifBlank { null }
}
