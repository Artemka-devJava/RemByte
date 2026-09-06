package ru.fixbyte.crm

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

/**
 * Инициализация API и загрузчика картинок (Coil ходит тем же OkHttp-клиентом
 * с cookie сессии — иначе /api/clients/{id}/photos/{id}/raw вернёт 401).
 */
class CrmApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Api.init(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { Api.http }
            .crossfade(true)
            .build()
}
