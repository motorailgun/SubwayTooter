package es.ariaontheplanet.quasar.util

import android.content.Context
import android.graphics.Bitmap
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import jp.juggler.util.log.LogCategory

private val log = LogCategory("LoadIcon")

suspend fun Context.loadIcon(url: String?, size: Int): Bitmap? = try {
    val request = ImageRequest.Builder(this)
        .data(url)
        .size(size, size)
        .allowHardware(false)
        .build()
    when (val result = SingletonImageLoader.get(this).execute(request)) {
        is SuccessResult -> (result.image as? BitmapImage)?.bitmap
        else -> null
    }
} catch (ex: Throwable) {
    log.w(ex, "url=$url")
    null
}
