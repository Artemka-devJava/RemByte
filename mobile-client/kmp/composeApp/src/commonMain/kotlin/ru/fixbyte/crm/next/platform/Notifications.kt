package ru.fixbyte.crm.next.platform

/** Запросить у пользователя разрешение на уведомления (Android 13+ / iOS). */
expect fun ensureNotificationPermission()

/** Показать уведомление прямо сейчас. [id] — стабильный ключ (для замены/дедупа). */
expect fun showNotification(id: String, title: String, body: String)
