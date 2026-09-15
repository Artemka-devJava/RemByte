package ru.fixbyte.crm.next.reminders

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.fixbyte.crm.next.data.Api
import ru.fixbyte.crm.next.data.ReminderDto
import ru.fixbyte.crm.next.data.StaleOrderDto
import ru.fixbyte.crm.next.data.createSettings
import ru.fixbyte.crm.next.platform.showNotification

/**
 * Сверяет открытые напоминания и «зависшие» (готовые, но не забранные)
 * заказы с сервером (`/api/reminders` — то же, чем пользуется веб-дашборд) и
 * показывает системное уведомление по тем, что уже наступили и ещё не были
 * показаны. Дедуп — по id, хранится локально; id сам «отваливается» из
 * набора, как только напоминание/заказ пропадает из открытого списка сервера
 * (выполнено / удалено / забрано).
 *
 * Вызывается: при каждом запуске приложения (App.kt) и периодически в фоне
 * на Android (см. platform/ReminderWorker.android.kt) — на iOS фонового
 * планировщика нет, там сработает только при следующем открытии приложения.
 */
object ReminderChecker {

    private val settings by lazy { createSettings("fixbyte_reminders") }

    suspend fun checkAndNotify() {
        if (!Api.ensureLoggedIn()) return
        val data = runCatching { Api.reminders() }.getOrNull() ?: return
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        checkReminders(data.reminders, now)
        checkStaleOrders(data.staleOrders)
    }

    private fun checkReminders(reminders: List<ReminderDto>, now: LocalDateTime) {
        val openIds = reminders.map { it.id }.toSet()
        val notified = loadIds(KEY_NOTIFIED_REMINDERS).intersect(openIds).toMutableSet()

        for (r in reminders) {
            if (r.id in notified) continue
            val due = r.dueAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: continue
            if (due > now) continue
            showNotification(
                id = "reminder_${r.id}",
                title = "Напоминание",
                body = r.text.ifBlank { "Свяжитесь с клиентом" }
            )
            notified += r.id
        }

        saveIds(KEY_NOTIFIED_REMINDERS, notified)
    }

    private fun checkStaleOrders(staleOrders: List<StaleOrderDto>) {
        val openIds = staleOrders.map { it.orderId }.toSet()
        val notified = loadIds(KEY_NOTIFIED_STALE).intersect(openIds).toMutableSet()

        for (o in staleOrders) {
            if (o.orderId in notified) continue
            showNotification(
                id = "stale_${o.orderId}",
                title = "Готов, но не забрали",
                body = "${o.clientName.ifBlank { "Клиент" }} — заказ ${o.orderNumber.ifBlank { "№?" }}"
            )
            notified += o.orderId
        }

        saveIds(KEY_NOTIFIED_STALE, notified)
    }

    private fun loadIds(key: String): Set<Long> =
        settings.getStringOrNull(key)
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    private fun saveIds(key: String, ids: Set<Long>) {
        settings.putString(key, ids.joinToString(","))
    }

    private const val KEY_NOTIFIED_REMINDERS = "notified_reminders"
    private const val KEY_NOTIFIED_STALE = "notified_stale_orders"
}
