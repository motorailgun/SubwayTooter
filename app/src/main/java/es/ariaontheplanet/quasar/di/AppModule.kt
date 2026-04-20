package es.ariaontheplanet.quasar.di

import org.koin.dsl.module

// App-scope singles (OkHttp clients, CustomEmojiCache, preferences, etc.).
// Currently empty — call sites still use App1.ok_http_client / App1.custom_emoji_cache
// statics. Phase 4b+ moves those in one at a time.
val appModule = module {
}
