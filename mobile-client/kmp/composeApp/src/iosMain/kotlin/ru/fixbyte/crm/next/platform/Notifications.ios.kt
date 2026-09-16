package ru.fixbyte.crm.next.platform

// TODO(iOS): не проверено компиляцией — см. оговорку в Platform.ios.kt.
// Фоновая проверка напоминаний (ReminderChecker.checkAndNotify), пока
// приложение свёрнуто/закрыто, теперь тоже есть — см.
// iosApp/iosApp/AppDelegate.swift (BGTaskScheduler). До открытия того файла
// она срабатывала только при запуске/возврате приложения на передний план
// (см. App.kt) — так и остаётся резервным путём: BGTaskScheduler на iOS не
// гарантирует конкретное время срабатывания, только нижнюю границу.

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual fun ensureNotificationPermission() {
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound
    ) { _, _ -> }
}

actual fun showNotification(id: String, title: String, body: String) {
    val content = UNMutableNotificationContent()
    content.title = title
    content.body = body
    content.sound = UNNotificationSound.defaultSound()

    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = id,
        content = content,
        trigger = null
    )
    UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { }
}
