package ru.fixbyte.crm

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Сверяет открытые напоминания и «зависшие» (готовые, но не забранные) заказы
 * с сервером (/api/reminders — тот же эндпоинт, что и веб-дашборд) и
 * показывает системное уведомление по тем, что уже наступили и ещё не были
 * показаны. Дедуп — по id, хранится в Prefs — id сам «отваливается», как
 * только напоминание/заказ пропадает из открытого списка сервера
 * (выполнено/удалено/забрано).
 *
 * Зеркалит mobile-client/kmp/.../reminders/ReminderChecker.kt: тот же
 * эндпоинт и та же логика, независимая реализация под стек этого (не-KMP)
 * приложения — OkHttp/org.json вместо ktor, SharedPreferences вместо
 * multiplatform-settings.
 *
 * Вызывается: при каждом запуске приложения (LoginActivity) и периодически в
 * фоне через WorkManager (см. ReminderWorker, CrmApp.scheduleReminderChecks).
 */
object ReminderChecker {

    suspend fun checkAndNotify(context: Context) {
        if (!Api.ensureLoggedIn()) return
        val data = runCatching { Api.reminders() }.getOrNull() ?: return
        val now = System.currentTimeMillis()

        checkReminders(context, data.reminders, now)
        checkStaleOrders(context, data.staleOrders)
    }

    private fun checkReminders(context: Context, reminders: List<ReminderDto>, now: Long) {
        val openIds = reminders.map { it.id }.toSet()
        val notified = Api.prefs.loadNotifiedReminderIds().intersect(openIds).toMutableSet()

        for (r in reminders) {
            if (r.id in notified) continue
            val due = parseServerDateTime(r.dueAt) ?: continue
            if (due > now) continue
            Notifications.show(context, "reminder_${r.id}", "Напоминание", r.text.ifBlank { "Свяжитесь с клиентом" })
            notified += r.id
        }
        Api.prefs.saveNotifiedReminderIds(notified)
    }

    private fun checkStaleOrders(context: Context, staleOrders: List<StaleOrderDto>) {
        val openIds = staleOrders.map { it.orderId }.toSet()
        val notified = Api.prefs.loadNotifiedStaleOrderIds().intersect(openIds).toMutableSet()

        for (o in staleOrders) {
            if (o.orderId in notified) continue
            Notifications.show(
                context, "stale_${o.orderId}", "Готов, но не забрали",
                "${o.clientName.ifBlank { "Клиент" }} — заказ ${o.orderNumber.ifBlank { "№?" }}"
            )
            notified += o.orderId
        }
        Api.prefs.saveNotifiedStaleOrderIds(notified)
    }

    /**
     * Сервер отдаёт LocalDateTime без зоны, например "2026-09-16T14:30:00"
     * (иногда с дробными секундами) — трактуем как локальное время устройства,
     * как и остальной проект (fixbyte.reminders.* без TZ-конвертации).
     */
    private fun parseServerDateTime(raw: String?): Long? {
        if (raw.isNullOrBlank() || raw.length < 19) return null
        return runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            fmt.timeZone = TimeZone.getDefault()
            fmt.parse(raw.substring(0, 19))?.time
        }.getOrNull()
    }
}
