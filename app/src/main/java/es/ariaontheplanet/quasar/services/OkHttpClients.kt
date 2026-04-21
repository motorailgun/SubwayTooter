package es.ariaontheplanet.quasar.services

import android.content.Context
import es.ariaontheplanet.quasar.util.ProgressResponseBody
import es.ariaontheplanet.quasar.util.getUserAgent
import jp.juggler.util.network.MySslSocketFactory
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.Collections
import java.util.concurrent.TimeUnit

// Qualifiers for the three OkHttpClient singles.
object OkHttpQualifiers {
    const val API = "api"
    const val CACHED = "cached"
    const val MEDIA = "media"
}

private fun Context.userAgentInterceptor() =
    Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", getUserAgent())
                .build()
        )
    }

fun Context.prepareOkHttp(
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
