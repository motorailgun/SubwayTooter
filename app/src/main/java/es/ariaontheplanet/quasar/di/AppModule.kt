package es.ariaontheplanet.quasar.di

import es.ariaontheplanet.quasar.services.AppBusyState
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

// App-scope singles. Phase 4b+ moves OkHttp clients, CustomEmojiCache,
// TtsService, ColumnRepository etc. in one at a time.
val appModule = module {
    singleOf(::AppBusyState)
}
