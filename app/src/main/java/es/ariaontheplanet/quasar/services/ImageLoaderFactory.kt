package es.ariaontheplanet.quasar.services

import android.content.Context
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import okhttp3.OkHttpClient

// Single app-wide Coil ImageLoader. Reuses the API OkHttp single (named
// OkHttpQualifiers.API) so cache-control and user-agent match the rest of
// the app. Decoders cover SVG (for custom emoji previews) and animated
// GIF/APNG (AnimatedImageDecoder on API 28+, GifDecoder on older devices).
fun buildAppImageLoader(
    context: Context,
    okHttpClient: OkHttpClient,
): ImageLoader = ImageLoader.Builder(context)
    .components {
        add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
        add(SvgDecoder.Factory())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            add(AnimatedImageDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }
    .crossfade(true)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .diskCachePolicy(CachePolicy.ENABLED)
    .build()
