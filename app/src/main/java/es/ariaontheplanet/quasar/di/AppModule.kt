package es.ariaontheplanet.quasar.di

import es.ariaontheplanet.quasar.services.AppBusyState
import es.ariaontheplanet.quasar.services.ColumnRepository
import es.ariaontheplanet.quasar.services.TtsService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

// App-scope singles. Phase 4e will move OkHttp clients and CustomEmojiCache in.
val appModule = module {
    singleOf(::AppBusyState)
    singleOf(::ColumnRepository)
    single { TtsService(androidContext(), get()) }
}
