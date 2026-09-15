package ru.fixbyte.crm.next.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import ru.fixbyte.crm.next.AppContext

actual fun createSettings(name: String): Settings =
    SharedPreferencesSettings(AppContext.context.getSharedPreferences(name, Context.MODE_PRIVATE))

/** Пароль хранится в EncryptedSharedPreferences (Android Keystore), а не открытым текстом. */
actual class SecureStore actual constructor() {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(AppContext.context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            AppContext.context,
            "fixbyte_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    actual fun get(key: String): String? = prefs.getString(key, null)

    actual fun set(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }
}
