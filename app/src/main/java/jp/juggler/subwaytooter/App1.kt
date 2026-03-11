package jp.juggler.subwaytooter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import java.util.WeakHashMap
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.load.model.GlideUrl
import jp.juggler.subwaytooter.api.TootApiClient
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.emoji.EmojiMap
import jp.juggler.subwaytooter.pref.LazyContextHolder
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.subwaytooter.table.HighlightWord
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.util.CustomEmojiCache
import jp.juggler.subwaytooter.util.CustomEmojiLister
import jp.juggler.subwaytooter.util.ProgressResponseBody
import jp.juggler.subwaytooter.util.getUserAgent
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.EmptyScope
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.initializeToastUtils
import jp.juggler.util.network.MySslSocketFactory
import jp.juggler.util.network.toPostRequestBuilder
import jp.juggler.util.os.applicationContextSafe
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.conscrypt.Conscrypt
import ru.gildor.coroutines.okhttp.await
import java.io.File
import java.io.InputStream
import java.security.Security
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max

class App1 : Application() {

    override fun onCreate() {
        log.d("onCreate")
        LazyContextHolder.init(applicationContextSafe)
        super.onCreate()
        initializeToastUtils(this)
        prepare(applicationContext, "App1.onCreate")
        registerActivityLifecycleCallbacks(themeLifecycleCallbacks)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LazyContextHolder.init(applicationContextSafe)
    }

    override fun onTerminate() {
        log.d("onTerminate")
        super.onTerminate()
    }

    private val themeLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            val applied = appliedThemes[activity] ?: return
            var newTheme = PrefI.ipUiTheme.value
            if (applied.forceDark && newTheme == 0) newTheme = 1
            if (newTheme != applied.effectiveTheme) {
                activity.recreate()
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            appliedThemes.remove(activity)
        }
    }

    companion object {

        internal val log = LogCategory("App1")

        private data class AppliedTheme(val effectiveTheme: Int, val forceDark: Boolean)

        private val appliedThemes = WeakHashMap<Activity, AppliedTheme>()

        private fun Context.userAgentInterceptor() =
            Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", getUserAgent())
                        .build()
                )
            }

        private fun Context.prepareOkHttp(
            timeoutSecondsConnect: Int,
            timeoutSecondsRead: Int,
        ): OkHttpClient.Builder {
            val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .allEnabledCipherSuites()
                .allEnabledTlsVersions()
                .build()

            return OkHttpClient.Builder()
                .connectTimeout(timeoutSecondsConnect.toLong(), TimeUnit.SECONDS)
                .readTimeout(timeoutSecondsRead.toLong(), TimeUnit.SECONDS)
                .writeTimeout(timeoutSecondsRead.toLong(), TimeUnit.SECONDS)
                .pingInterval(10, TimeUnit.SECONDS)
                .connectionSpecs(Collections.singletonList(spec))
                .sslSocketFactory(MySslSocketFactory, MySslSocketFactory.trustManager)
                .addInterceptor(ProgressResponseBody.makeInterceptor())
                .addInterceptor(userAgentInterceptor())
        }

        lateinit var ok_http_client: OkHttpClient

        private lateinit var ok_http_client2: OkHttpClient

        lateinit var ok_http_client_media_viewer: OkHttpClient

        @SuppressLint("StaticFieldLeak")
        lateinit var custom_emoji_cache: CustomEmojiCache

        @SuppressLint("StaticFieldLeak")
        lateinit var custom_emoji_lister: CustomEmojiLister

        fun prepare(appContext: Context, caller: String): AppState {
            var state = appStateX
            if (state != null) return state

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

            log.d("create okhttp client")
            run {
                Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE

                val apiReadTimeout = max(3, PrefS.spApiReadTimeout.toInt())

                // API用のHTTP設定はキャッシュを使わない
                ok_http_client = appContext.prepareOkHttp(apiReadTimeout, apiReadTimeout)
                    .build()

                // ディスクキャッシュ
                val cacheDir = File(appContext.cacheDir, "http2")
                val cache = Cache(cacheDir, 30000000L)

                // カスタム絵文字用のHTTP設定はキャッシュを使う
                ok_http_client2 = appContext.prepareOkHttp(apiReadTimeout, apiReadTimeout)
                    .cache(cache)
                    .build()

                // 内蔵メディアビューア用のHTTP設定はタイムアウトを調整可能
                val mediaReadTimeout = max(3, PrefS.spMediaReadTimeout.toInt())
                ok_http_client_media_viewer =
                    appContext.prepareOkHttp(mediaReadTimeout, mediaReadTimeout)
                        .cache(cache)
                        .build()
            }

            val handler = Handler(appContext.mainLooper)

            log.d("create custom emoji cache.")
            custom_emoji_cache = CustomEmojiCache(appContext, handler)
            custom_emoji_lister = CustomEmojiLister(appContext, handler)

            ColumnType.dump()

            log.d("create  AppState.")

            state = AppState(appContext, handler)
            appStateX = state

            // getAppState()を使える状態にしてからカラム一覧をロードする
            log.d("load column list...")
            state.loadColumnList()

            log.d("prepare() complete! caller=$caller")

            return state
        }

        @SuppressLint("StaticFieldLeak")
        private var appStateX: AppState? = null

        fun getAppState(context: Context, caller: String = "getAppState"): AppState {
            return prepare(context.applicationContext, caller)
        }

        fun sound(item: HighlightWord) {
            try {
                appStateX?.sound(item)
            } catch (ex: Throwable) {
                log.e(ex, "sound failed.")
                // java.lang.NoSuchFieldError:
                // at jp.juggler.subwaytooter.App1$Companion.sound (App1.kt:544)
                // at jp.juggler.subwaytooter.column.Column$startRefresh$task$1.onPostExecute (Column.kt:2432)
            }
        }

        @Suppress("UNUSED_PARAMETER")
        fun registerGlideComponents(context: Context, glide: Glide, registry: Registry) {
            // カスタムされたokhttpを優先的に使うためにprependを指定する
            registry.prepend(
                GlideUrl::class.java,
                InputStream::class.java,
                OkHttpUrlLoader.Factory(ok_http_client)
            )
        }

        fun applyGlideOptions(context: Context, builder: GlideBuilder) {

            // ログレベル
            builder.setLogLevel(Log.ERROR)

            // エラー処理
            val catcher = GlideExecutor.UncaughtThrowableStrategy { ex ->
                log.e(ex, "glide uncaught error.")
            }
            builder.setDiskCacheExecutor(
                GlideExecutor.newDiskCacheBuilder()
                    .setUncaughtThrowableStrategy(catcher).build()
            )
            builder.setSourceExecutor(
                GlideExecutor.newSourceBuilder()
                    .setUncaughtThrowableStrategy(catcher).build()
            )

            builder.setDiskCache(InternalCacheDiskCacheFactory(context, 10L * 1024L * 1024L))
        }

        fun setActivityTheme(
            activity: ComponentActivity,
            forceDark: Boolean = false,
        ) {
            prepare(activity.applicationContext, "setActivityTheme")

            var nTheme = PrefI.ipUiTheme.value
            if (forceDark && nTheme == 0) nTheme = 1
            activity.setTheme(
                when (nTheme) {
                    2 -> R.style.AppTheme_Mastodon
                    1 -> R.style.AppTheme_Dark
                    /* 0 */ else -> R.style.AppTheme_Light
                }
            )
            activity.enableEdgeToEdgeEx(forceDark = forceDark)
            appliedThemes[activity] = AppliedTheme(
                effectiveTheme = nTheme,
                forceDark = forceDark,
            )
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
