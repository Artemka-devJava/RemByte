package ru.fixbyte.crm.next.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.fixbyte.crm.next.reminders.ReminderChecker

/** Периодическая (~раз в 15 минут) фоновая проверка напоминаний, см. CrmApplication. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ReminderChecker.checkAndNotify()
        return Result.success()
    }
}
