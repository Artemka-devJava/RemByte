package ru.fixbyte.crm.next.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Единственная точка доступа к REST API FixByte CRM — общий (commonMain) аналог
 * старого Android-клиента `ru.fixbyte.crm.Api` (Net.kt), тот же бэкенд, тот же
 * cookie-based auth, тот же авто-релогин при протухшей сессии.
 */
object Api {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val settings by lazy { createSettings("fixbyte_prefs") }
    private val secureStore by lazy { SecureStore() }
    private val cookieStorage by lazy { PersistentCookiesStorage(settings) }

    private val client: HttpClient by lazy {
        HttpClient {
            followRedirects = false
            expectSuccess = false
            install(HttpCookies) { storage = cookieStorage }
            install(ContentNegotiation) { json(json) }
            install(Logging) { level = LogLevel.INFO }
        }
    }

    /** Тот же авторизованный клиент — используется Coil (см. theme/ImageLoader.kt) для картинок. */
    val httpClient: HttpClient get() = client

    var baseUrl: String
        get() = settings.getString(KEY_BASE_URL, "http://10.0.2.2:9087")
        set(value) { settings.putString(KEY_BASE_URL, value.trim().trimEnd('/')) }

    var lastUsername: String
        get() = settings.getString(KEY_USERNAME, "")
        set(value) { settings.putString(KEY_USERNAME, value) }

    private var password: String?
        get() = secureStore.get(KEY_PASSWORD)
        set(value) { secureStore.set(KEY_PASSWORD, value) }

    fun hasCredentials(): Boolean = lastUsername.isNotBlank() && !password.isNullOrBlank()

    /** Полный URL для картинки/файла (в DTO приходит относительный путь). */
    fun absoluteUrl(path: String) = if (path.startsWith("http")) path else baseUrl + path

    // ── Аутентификация ────────────────────────────────────────

    suspend fun login(username: String, password: String) {
        val response = client.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
        requireOk(response, "Не удалось войти")
        lastUsername = username
        this.password = password
    }

    suspend fun isLoggedIn(): Boolean = runCatching {
        client.get("$baseUrl/api/auth/me").status.isSuccess()
    }.getOrDefault(false)

    suspend fun logout() {
        runCatching { client.post("$baseUrl/api/auth/logout") }
        cookieStorage.clear()
        lastUsername = ""
        password = null
    }

    suspend fun ensureLoggedIn(): Boolean {
        if (isLoggedIn()) return true
        if (!hasCredentials()) return false
        val user = lastUsername
        val pass = password ?: return false
        return runCatching { login(user, pass) }.isSuccess
    }

    private val reauthMutex = Mutex()

    private suspend fun tryReauth(): Boolean = reauthMutex.withLock {
        val user = lastUsername
        val pass = password
        if (user.isBlank() || pass.isNullOrBlank()) return@withLock false
        runCatching { login(user, pass) }.isSuccess
    }

    private fun isAuthFailure(response: HttpResponse): Boolean {
        val status = response.status
        if (status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden) return true
        if (status.value in 300..399) {
            return response.headers[HttpHeaders.Location]?.contains("/login") == true
        }
        return false
    }

    /** Выполняет запрос; при 401/403/редиректе на /login — один раз перелогинивается и повторяет. */
    private suspend fun authed(block: suspend () -> HttpResponse): HttpResponse {
        var response = block()
        if (isAuthFailure(response) && tryReauth()) {
            response = block()
        }
        return response
    }

    private suspend fun requireOk(response: HttpResponse, fallback: String) {
        if (!response.status.isSuccess()) {
            throw ApiException(parseError(response) ?: "$fallback (${response.status.value})")
        }
    }

    private suspend fun parseError(response: HttpResponse): String? {
        val text = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
        if (text.isBlank()) return null
        return runCatching { json.decodeFromString<ErrorResponse>(text).error }.getOrNull() ?: text
    }

    // ── Клиенты ───────────────────────────────────────────────

    suspend fun clients(): List<ClientBriefDto> {
        val response = authed { client.get("$baseUrl/api/clients") }
        requireOk(response, "Не удалось получить клиентов")
        return response.body()
    }

    suspend fun searchClients(query: String): List<ClientBriefDto> {
        val response = authed { client.get("$baseUrl/api/clients/search") { parameter("name", query) } }
        requireOk(response, "Не удалось найти клиентов")
        return response.body()
    }

    suspend fun client(id: Long): ClientDetailDto {
        val response = authed { client.get("$baseUrl/api/clients/$id") }
        requireOk(response, "Не удалось получить клиента")
        return response.body()
    }

    suspend fun summary(id: Long): ClientSummaryDto {
        val response = authed { client.get("$baseUrl/api/clients/$id/summary") }
        requireOk(response, "Не удалось получить сводку")
        return response.body()
    }

    /**
     * Создать клиента. При дубле телефона сервер отдаёт 409 + существующую карточку.
     * Если заданы [problem]/[estimatePrice] — следом заводится первичная заявка.
     */
    suspend fun createClient(
        name: String,
        phone: String,
        type: String,
        problem: String? = null,
        estimatePrice: Double? = null
    ): CreateClientResult {
        val response = authed {
            client.post("$baseUrl/api/clients") {
                contentType(ContentType.Application.Json)
                setBody(CreateClientRequest(name, phone, type))
            }
        }
        val result: CreateClientResult = when {
            response.status.isSuccess() -> CreateClientResult.Created(response.body<IdResponse>().id)
            response.status == HttpStatusCode.Conflict -> {
                val dup = response.body<DuplicateClientResponse>()
                CreateClientResult.Duplicate(dup.id, dup.name)
            }
            else -> throw ApiException(parseError(response) ?: "Не удалось создать клиента (${response.status.value})")
        }
        if (result is CreateClientResult.Created) {
            val hasProblem = !problem.isNullOrBlank()
            val hasPrice = (estimatePrice ?: 0.0) > 0.0
            if (hasProblem || hasPrice) {
                val ok = runCatching {
                    createOrderInternal(result.id, problem?.trim(), estimatePrice, "Первичная заявка (моб. приложение)")
                }.isSuccess
                return result.copy(orderFailed = !ok)
            }
        }
        return result
    }

    // ── Заказы ────────────────────────────────────────────────

    suspend fun clientOrders(clientId: Long): List<OrderBriefDto> {
        val response = authed { client.get("$baseUrl/api/orders/client/$clientId") }
        requireOk(response, "Не удалось получить заказы")
        return response.body<List<OrderBriefDto>>().sortedByDescending { it.createdAt ?: "" }
    }

    suspend fun order(id: Long): OrderBriefDto {
        val response = authed { client.get("$baseUrl/api/orders/$id") }
        requireOk(response, "Не удалось получить заказ")
        return response.body()
    }

    suspend fun createOrder(clientId: Long, deviceDescription: String?, estimatePrice: Double?): Long =
        createOrderInternal(clientId, deviceDescription, estimatePrice, "Заявка (моб. приложение)")

    private suspend fun createOrderInternal(
        clientId: Long,
        deviceDescription: String?,
        estimatePrice: Double?,
        notes: String
    ): Long {
        val lines = estimateLine(estimatePrice)
        val response = authed {
            client.post("$baseUrl/api/orders") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateOrderRequest(
                        client = ClientRefRequest(clientId),
                        notes = notes,
                        deviceDescription = deviceDescription?.takeIf { it.isNotBlank() },
                        lines = lines
                    )
                )
            }
        }
        requireOk(response, "Заявка не создана")
        return runCatching { response.body<IdResponse>().id }.getOrDefault(0L)
    }

    suspend fun updateOrder(orderId: Long, deviceDescription: String?, estimatePrice: Double?) {
        val response = authed {
            client.put("$baseUrl/api/orders/$orderId") {
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateOrderRequest(
                        notes = "Заявка (моб. приложение)",
                        deviceDescription = deviceDescription?.takeIf { it.isNotBlank() },
                        lines = estimateLine(estimatePrice)
                    )
                )
            }
        }
        requireOk(response, "Не удалось сохранить заказ")
    }

    private fun estimateLine(estimatePrice: Double?): List<OrderLineDto> =
        if ((estimatePrice ?: 0.0) > 0.0)
            listOf(OrderLineDto("Предварительная оценка (со слов клиента)", estimatePrice!!, 1))
        else emptyList()

    /** PDF акта приёмки по заказу. */
    suspend fun acceptanceActPdf(orderId: Long): ByteArray = downloadBytes("$baseUrl/api/orders/$orderId/act")

    suspend fun downloadBytes(url: String): ByteArray {
        val response = authed { client.get(url) }
        requireOk(response, "Не удалось получить файл")
        return response.body()
    }

    // ── Фото заказа ───────────────────────────────────────────

    suspend fun uploadOrderPhoto(orderId: Long, bytes: ByteArray, filename: String) {
        val name = if (filename.endsWith(".jpg")) filename else "$filename.jpg"
        val response = authed {
            client.submitFormWithBinaryData(
                url = "$baseUrl/api/orders/$orderId/attachments",
                formData = formData {
                    append("files", bytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"files\"; filename=\"$name\"")
                    })
                }
            )
        }
        requireOk(response, "Не удалось загрузить фото")
    }

    suspend fun deleteOrderAttachment(orderId: Long, url: String) {
        val response = authed {
            client.delete("$baseUrl/api/orders/$orderId/attachments") { parameter("url", url) }
        }
        if (!response.status.isSuccess() && response.status != HttpStatusCode.NoContent) {
            throw ApiException("Не удалось удалить (${response.status.value})")
        }
    }

    // ── Напоминания ───────────────────────────────────────────

    suspend fun reminders(): RemindersResponse {
        val response = authed { client.get("$baseUrl/api/reminders") }
        requireOk(response, "Не удалось получить напоминания")
        return response.body()
    }

    // ── Фото клиента ──────────────────────────────────────────

    suspend fun photos(clientId: Long): List<ClientPhotoDto> {
        val response = authed { client.get("$baseUrl/api/clients/$clientId/photos") }
        requireOk(response, "Не удалось получить фото")
        return response.body()
    }

    suspend fun uploadPhoto(clientId: Long, bytes: ByteArray, filename: String, mime: String, caption: String?) {
        val response = authed {
            client.submitFormWithBinaryData(
                url = "$baseUrl/api/clients/$clientId/photos",
                formData = formData {
                    append("files", bytes, Headers.build {
                        append(HttpHeaders.ContentType, mime)
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"files\"; filename=\"$filename\"")
                    })
                    if (!caption.isNullOrBlank()) append("caption", caption.trim())
                }
            )
        }
        requireOk(response, "Не удалось загрузить фото")
    }

    suspend fun deletePhoto(clientId: Long, photoId: Long) {
        val response = authed { client.delete("$baseUrl/api/clients/$clientId/photos/$photoId") }
        if (!response.status.isSuccess() && response.status != HttpStatusCode.NoContent) {
            throw ApiException("Не удалось удалить (${response.status.value})")
        }
    }

    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USERNAME = "last_username"
    private const val KEY_PASSWORD = "password"
}
