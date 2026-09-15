package ru.fixbyte.crm.next.data

import com.russhwolf.settings.Settings

/** Обычное (не секретное) key-value хранилище: адрес сервера, cookie сессии, логин. */
expect fun createSettings(name: String): Settings

/** Секретное хранилище пароля: Android Keystore / iOS Keychain. */
expect class SecureStore() {
    fun get(key: String): String?
    fun set(key: String, value: String?)
}
