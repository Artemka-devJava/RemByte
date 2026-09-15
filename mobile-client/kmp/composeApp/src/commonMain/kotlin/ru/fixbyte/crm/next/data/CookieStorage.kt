package ru.fixbyte.crm.next.data

import com.russhwolf.settings.Settings
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cookie сессии сохраняются между запусками приложения (аналог PrefsCookieJar в
 * старом Android-клиенте) — иначе после закрытия приложения нужно было бы входить заново.
 */
@Serializable
private data class StoredCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String
)

class PersistentCookiesStorage(private val settings: Settings) : CookiesStorage {

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache: MutableList<StoredCookie> by lazy { load().toMutableList() }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        cache
            .filter { requestUrl.host.equals(it.domain, ignoreCase = true) || requestUrl.host.endsWith(".${it.domain}") }
            .map { Cookie(it.name, it.value, CookieEncoding.RAW, domain = it.domain, path = it.path) }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = mutex.withLock {
        val domain = cookie.domain ?: requestUrl.host
        val path = cookie.path ?: "/"
        cache.removeAll { it.name == cookie.name && it.domain == domain }
        if (cookie.value.isNotEmpty()) {
            cache += StoredCookie(cookie.name, cookie.value, domain, path)
        }
        persist()
    }

    suspend fun clear() = mutex.withLock {
        cache.clear()
        persist()
    }

    private fun load(): List<StoredCookie> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<StoredCookie>>(raw) }.getOrDefault(emptyList())
    }

    private fun persist() {
        settings.putString(KEY, json.encodeToString(cache))
    }

    override fun close() = Unit

    private companion object {
        const val KEY = "session_cookies"
    }
}
