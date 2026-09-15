package ru.fixbyte.crm.next

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.fixbyte.crm.next.platform.ReminderWorker
import java.util.concurrent.TimeUnit

class CrmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Должно случиться раньше любой Activity — WorkManager может разбудить
        // процесс и выполнить ReminderWorker без единого открытого экрана.
        AppContext.init(this)
        scheduleReminderChecks()
    }

    private fun scheduleReminderChecks() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("reminder-check", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
