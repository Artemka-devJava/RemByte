package ru.fixbyte.crm.next.ui.common

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import ru.fixbyte.crm.next.data.Api

/**
 * Coil грузит картинки тем же Ktor-клиентом, что и остальной API — иначе
 * `/api/clients/{id}/photos/{pid}/raw` вернёт 401 (нужна cookie сессии).
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun InstallAuthenticatedImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(Api.httpClient)) }
            .crossfade(true)
            .build()
    }
}
