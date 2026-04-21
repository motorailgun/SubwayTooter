package es.ariaontheplanet.quasar.di

import android.os.Handler
import android.os.Looper
import es.ariaontheplanet.quasar.AppState
import es.ariaontheplanet.quasar.pref.PrefS
import es.ariaontheplanet.quasar.services.AppBusyState
import es.ariaontheplanet.quasar.services.ColumnRepository
import es.ariaontheplanet.quasar.services.OkHttpQualifiers
import es.ariaontheplanet.quasar.services.TtsService
import es.ariaontheplanet.quasar.services.prepareOkHttp
import es.ariaontheplanet.quasar.util.CustomEmojiCache
import es.ariaontheplanet.quasar.util.CustomEmojiLister
import okhttp3.Cache
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import kotlin.math.max

val appModule = module {
    singleOf(::AppBusyState)
    singleOf(::ColumnRepository)
    single { TtsService(androidContext(), get()) }
    single { AppState(androidContext(), get()) }

    // Main-looper Handler shared by emoji services that still rely on it.
    single { Handler(Looper.getMainLooper()) }

    single { CustomEmojiCache(androidContext(), get()) }
    single { CustomEmojiLister(androidContext(), get()) }

    // Shared disk cache used by the cached + media-viewer OkHttp clients.
    single { Cache(File(androidContext().cacheDir, "http2"), 30_000_000L) }

    single(named(OkHttpQualifiers.API)) {
        val apiReadTimeout = max(3, PrefS.spApiReadTimeout.toInt())
        androidContext().prepareOkHttp(apiReadTimeout, apiReadTimeout).build()
    }
    single(named(OkHttpQualifiers.CACHED)) {
        val apiReadTimeout = max(3, PrefS.spApiReadTimeout.toInt())
        androidContext()
            .prepareOkHttp(apiReadTimeout, apiReadTimeout)
            .cache(get())
            .build()
    }
    single(named(OkHttpQualifiers.MEDIA)) {
        val mediaReadTimeout = max(3, PrefS.spMediaReadTimeout.toInt())
        androidContext()
            .prepareOkHttp(mediaReadTimeout, mediaReadTimeout)
            .cache(get())
            .build()
    }
}
