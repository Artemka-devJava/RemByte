package ru.fixbyte.crm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Периодическая (~раз в 15 минут) фоновая проверка напоминаний, см. CrmApp. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ReminderChecker.checkAndNotify(applicationContext)
        return Result.success()
    }
}
