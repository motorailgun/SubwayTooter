package es.ariaontheplanet.quasar.di

import android.os.Handler
import android.os.Looper
import es.ariaontheplanet.quasar.services.AppBusyState
import es.ariaontheplanet.quasar.services.ColumnRepository
import es.ariaontheplanet.quasar.services.TtsService
import es.ariaontheplanet.quasar.util.CustomEmojiCache
import es.ariaontheplanet.quasar.util.CustomEmojiLister
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::AppBusyState)
    singleOf(::ColumnRepository)
    single { TtsService(androidContext(), get()) }

    // Main-looper Handler shared by emoji services that still rely on it.
    single { Handler(Looper.getMainLooper()) }

    single { CustomEmojiCache(androidContext(), get()) }
    single { CustomEmojiLister(androidContext(), get()) }
}
