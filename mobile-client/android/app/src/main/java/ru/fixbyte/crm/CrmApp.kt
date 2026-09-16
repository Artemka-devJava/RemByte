package ru.fixbyte.crm

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import java.util.concurrent.TimeUnit

/**
 * Инициализация API и загрузчика картинок (Coil ходит тем же OkHttp-клиентом
 * с cookie сессии — иначе /api/clients/{id}/photos/{id}/raw вернёт 401).
 */
class CrmApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Api.init(this)
        scheduleReminderChecks()
    }

    // Планируется здесь, а не в Activity — WorkManager может разбудить процесс
    // и выполнить ReminderWorker без единого открытого экрана.
    private fun scheduleReminderChecks() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("reminder-check", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { Api.http }
            .crossfade(true)
            .build()
}
