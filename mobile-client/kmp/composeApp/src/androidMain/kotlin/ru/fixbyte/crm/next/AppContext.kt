package ru.fixbyte.crm.next

import android.content.Context

/**
 * Держит applicationContext для платформенных `actual` в data/PlatformStorage —
 * Android SharedPreferences/EncryptedSharedPreferences не могут быть созданы без Context,
 * а expect/actual createSettings()/SecureStore() в commonMain его не получают напрямую.
 * Инициализируется в MainActivity.onCreate до первого обращения к Api.
 */
object AppContext {
    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}
