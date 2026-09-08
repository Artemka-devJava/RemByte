package ru.fixbyte.crm

import android.content.Context

/**
 * Простое хранилище: адрес сервера, учётные данные и cookie сессии.
 *
 * Логин и пароль хранятся в приватных SharedPreferences приложения (как и
 * cookie сессии) — чтобы вводить их один раз в «Настройках», а дальше
 * приложение входило само, в т.ч. после того, как серверная сессия протухла.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("fixbyte_crm", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = sp.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

    var lastUsername: String
        get() = sp.getString(KEY_USERNAME, "") ?: ""
        set(value) = sp.edit().putString(KEY_USERNAME, value).apply()

    var password: String
        get() = sp.getString(KEY_PASSWORD, "") ?: ""
        set(value) = sp.edit().putString(KEY_PASSWORD, value).apply()

    fun hasCredentials(): Boolean = lastUsername.isNotBlank() && password.isNotBlank()

    /** Забыть пароль (логин оставляем как подсказку для формы входа). */
    fun clearCredentials() {
        sp.edit().remove(KEY_PASSWORD).apply()
    }

    /** Cookie в формате "name=value" по хостам, сериализованные строкой. */
    fun loadCookies(host: String): Set<String> =
        sp.getStringSet(cookieKey(host), emptySet()) ?: emptySet()

    fun saveCookies(host: String, cookies: Set<String>) {
        sp.edit().putStringSet(cookieKey(host), cookies).apply()
    }

    fun clearCookies() {
        val editor = sp.edit()
        sp.all.keys.filter { it.startsWith(COOKIE_PREFIX) }.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun cookieKey(host: String) = COOKIE_PREFIX + host

    companion object {
        // 10.0.2.2 — это localhost хост-машины для Android-эмулятора.
        const val DEFAULT_BASE_URL = "http://10.0.2.2:9087"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val COOKIE_PREFIX = "cookies_"
    }
}
