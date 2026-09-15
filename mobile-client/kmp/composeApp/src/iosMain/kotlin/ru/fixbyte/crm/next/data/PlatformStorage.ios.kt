package ru.fixbyte.crm.next.data

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createSettings(name: String): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

/**
 * TODO(iOS): перейти на Keychain. Пока используется NSUserDefaults (как и в
 * нынешнем нативном iOS-клиенте — см. mobile-client/ios) — не удалось
 * проверить сборку Keychain-обёртки на CFDictionary в этом окружении (нет
 * macOS/Xcode), поэтому взят заведомо рабочий вариант того же уровня
 * защищённости, что и сегодня, а не непроверенный код.
 */
actual class SecureStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun get(key: String): String? = defaults.stringForKey("secure_$key")

    actual fun set(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey("secure_$key")
        else defaults.setObject(value, forKey = "secure_$key")
    }
}
