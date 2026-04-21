package es.ariaontheplanet.quasar

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import java.util.WeakHashMap
import es.ariaontheplanet.quasar.actmain.rebuildFixedColumns
import es.ariaontheplanet.quasar.api.TootApiClient
import es.ariaontheplanet.quasar.column.ColumnType
import es.ariaontheplanet.quasar.di.appModule
import es.ariaontheplanet.quasar.di.viewModelModule
import es.ariaontheplanet.quasar.emoji.EmojiMap
import es.ariaontheplanet.quasar.pref.LazyContextHolder
import es.ariaontheplanet.quasar.pref.PrefI
import es.ariaontheplanet.quasar.services.OkHttpQualifiers
import es.ariaontheplanet.quasar.services.TtsService
import es.ariaontheplanet.quasar.table.HighlightWord
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.util.CustomEmojiCache
import es.ariaontheplanet.quasar.util.CustomEmojiLister
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.EmptyScope
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.initializeToastUtils
import jp.juggler.util.network.toPostRequestBuilder
import jp.juggler.util.os.applicationContextSafe
import kotlinx.coroutines.launch
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.conscrypt.Conscrypt
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import ru.gildor.coroutines.okhttp.await
import java.security.Security
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class App1 : Application() {

    override fun onCreate() {
        log.d("onCreate")
        LazyContextHolder.init(applicationContextSafe)
        super.onCreate()
        startKoin {
            androidContext(this@App1)
            modules(appModule, viewModelModule)
        }
        // Route all Coil AsyncImage calls through the Koin-managed ImageLoader.
        coil3.SingletonImageLoader.setSafe { GlobalContext.get().get() }
        initializeToastUtils(this)
        prepare(applicationContext, "App1.onCreate")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LazyContextHolder.init(applicationContextSafe)
    }

    override fun onTerminate() {
        log.d("onTerminate")
        super.onTerminate()
    }

    companion object {

        internal val log = LogCategory("App1")

        // Forward to Koin — the container owns construction. Existing callers
        // keep working via these properties; follow-ups migrate them to inject directly.
        val ok_http_client: OkHttpClient
            get() = GlobalContext.get().get(named(OkHttpQualifiers.API))

        private val ok_http_client2: OkHttpClient
            get() = GlobalContext.get().get(named(OkHttpQualifiers.CACHED))

        val ok_http_client_media_viewer: OkHttpClient
            get() = GlobalContext.get().get(named(OkHttpQualifiers.MEDIA))

        val custom_emoji_cache: CustomEmojiCache
            get() = GlobalContext.get().get()

        val custom_emoji_lister: CustomEmojiLister
            get() = GlobalContext.get().get()

        @Volatile
        private var prepared = false

        fun prepare(appContext: Context, caller: String): AppState {
            if (!prepared) {
                synchronized(App1::class.java) {
                    if (!prepared) runFirstTimeInit(appContext, caller)
                    prepared = true
                }
            }
            return GlobalContext.get().get()
        }

        private fun runFirstTimeInit(appContext: Context, caller: String) {
            log.d("initialize AppState. caller=$caller")

            // initialize EmojiMap
            EmojiMap.load(appContext)

            // emoji2 はデフォルトで自動初期化を行うのだが、新し目のPlayサービスに依存してるため
            // Playサービスが古い端末ではEmojiCompatの初期化がまだ行われていない状態になる
            // ワークアラウンドとして、アプリ内にバンドルしたデータを使うBundledEmojiCompatConfigで初期化する
            // (初期化が既に行われている場合は無害である)
            EmojiCompat.init(
                BundledEmojiCompatConfig(appContext) { command ->
                    EmptyScope.launch(AppDispatchers.IO) {
                        try {
                            command.run()
                        } catch (ex: Throwable) {
                            log.w(ex, "BundledEmojiCompatConfig fontLoadExecutor failed.")
                        }
                    }
                }
            )

            // initialize Conscrypt
            Security.insertProviderAt(
                Conscrypt.newProvider(),
                1 /* 1 means first position */
            )

            // OkHttp noise at FINE level only — clients themselves are Koin singles.
            Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE

            // CustomEmojiCache / CustomEmojiLister are Koin singles now.
            // Trigger construction here so onNetworkChanged() hooks fire as before.
            custom_emoji_cache
            custom_emoji_lister

            ColumnType.dump()

            log.d("initialize fixed columns...")
            val state: AppState = GlobalContext.get().get()
            state.loadCurrentAccount()?.let { account ->
                state.rebuildFixedColumns(account)
            }

            log.d("prepare() complete! caller=$caller")
        }

        fun getAppState(context: Context, caller: String = "getAppState"): AppState {
            return prepare(context.applicationContext, caller)
        }

        fun sound(item: HighlightWord) {
            try {
                GlobalContext.get().get<TtsService>().sound(item)
            } catch (ex: Throwable) {
                log.e(ex, "sound failed.")
            }
        }

        fun setActivityTheme(
            activity: ComponentActivity,
            forceDark: Boolean = false,
        ) {
            prepare(activity.applicationContext, "setActivityTheme")
            
            activity.setTheme(
                if (forceDark) {
                    R.style.AppTheme_Dark
                } else {
                    R.style.AppTheme_Light
                }
            )
            activity.enableEdgeToEdgeEx(forceDark = forceDark)
        }

        internal val CACHE_CONTROL = CacheControl.Builder()
            .maxAge(1, TimeUnit.DAYS) // キャッシュが新鮮であると考えられる時間
            .build()

        suspend fun getHttpCached(url: String): ByteArray? {
            val caller = RuntimeException("caller's stackTrace.")
            val response: Response

            try {
                val request_builder = Request.Builder()
                    .cacheControl(CACHE_CONTROL)
                    .url(url)

                val call = ok_http_client2.newCall(request_builder.build())
                response = call.await()
            } catch (ex: Throwable) {
                log.e(ex, "getHttp network error. $url")
                return null
            }

            if (!response.isSuccessful) {
                log.e(
                    caller,
                    TootApiClient.formatResponse(response, "getHttp response error. $url")
                )
                return null
            }

            return try {
                response.body.bytes()
            } catch (ex: Throwable) {
                log.e(ex, "getHttp content error. $url")
                null
            }
        }

        suspend fun getHttpCachedString(
            url: String,
            accessInfo: SavedAccount? = null,
            misskeyPost: Boolean = false,
            builderBlock: (Request.Builder) -> Unit = {},
        ): String? {
            val response: Response

            try {
                val request_builder = when {
                    misskeyPost && accessInfo?.isMisskey == true ->
                        accessInfo.putMisskeyApiToken().toPostRequestBuilder()
                            .url(url)
                            .cacheControl(CACHE_CONTROL)

                    else ->
                        Request.Builder()
                            .url(url)
                            .cacheControl(CACHE_CONTROL)
                            .also {
                                accessInfo?.bearerAccessToken?.notEmpty()?.let { a ->
                                    it.header("Authorization", "Bearer $a")
                                }
                            }
                }
                builderBlock(request_builder)
                val call = ok_http_client2.newCall(request_builder.build())
                response = call.await()
            } catch (ex: Throwable) {
                log.e(ex, "getHttp network error. $url")
                return null
            }

            if (!response.isSuccessful) {
                log.e(TootApiClient.formatResponse(response, "getHttp response error. $url"))
                return null
            }

            return try {
                response.body.string()
            } catch (ex: Throwable) {
                log.e(ex, "getHttp content error. $url")
                null
            }
        }
    }
}

val kJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
