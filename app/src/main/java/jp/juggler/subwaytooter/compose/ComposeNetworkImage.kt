package jp.juggler.subwaytooter.compose

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory

private val log = LogCategory("ComposeNetworkImage")

/**
 * Compose-native network image component using Glide.
 * Replaces MyNetworkImageView with a cleaner Compose implementation.
 *
 * @param url The image URL to load (static image)
 * @param modifier Compose modifier for layout
 * @param cornerRadius Corner radius in pixels (0f = square, >0 = rounded)
 * @param animatedUrl Optional animated image URL (GIF/APNG). If null, uses [url].
 * @param contentDescription Accessibility description
 * @param scaleType ImageView scale type (default: CENTER_CROP)
 * @param defaultDrawable Drawable to show while loading
 * @param errorDrawable Drawable to show on error
 */
@Composable
fun ComposeNetworkImage(
    url: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 0f,
    animatedUrl: String? = null,
    contentDescription: String? = null,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    defaultDrawable: Drawable? = null,
    errorDrawable: Drawable? = null,
) {
    val context = LocalContext.current

    // Determine which URL to load based on animation preference
    val effectiveUrl = if (PrefB.bpImageAnimationEnable.value) {
        animatedUrl?.notEmpty() ?: url
    } else {
        url
    }

    if (effectiveUrl.isNullOrEmpty()) {
        // Show default drawable if no URL
        Box(modifier = modifier) {
            if (defaultDrawable != null) {
                AndroidView(
                    modifier = modifier,
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            this.scaleType = ImageView.ScaleType.CENTER_CROP
                            this.setImageDrawable(defaultDrawable)
                        }
                    }
                )
            }
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                this.scaleType = scaleType
                this.contentDescription = contentDescription
                importantForAccessibility =
                    if (contentDescription != null) android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    else android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        },
        update = { imageView ->
            imageView.scaleType = scaleType
            imageView.contentDescription = contentDescription

            try {
                val glide = Glide.with(imageView.context)

                // Common request listener
                val listener = object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (e != null) {
                            log.e(e, "Image load failed for: $effectiveUrl")
                        }
                        errorDrawable?.let { imageView.setImageDrawable(it) }
                        return errorDrawable != null
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                }

                // Apply corner radius if needed
                if (cornerRadius > 0f) {
                    // Load as bitmap to apply rounded corners
                    glide.asBitmap()
                        .load(effectiveUrl)
                        .into(object : com.bumptech.glide.request.target.BitmapImageViewTarget(imageView) {
                            override fun setResource(resource: Bitmap?) {
                                if (resource != null) {
                                    val drawable = RoundedBitmapDrawableFactory.create(
                                        imageView.resources,
                                        resource
                                    ).apply {
                                        this.cornerRadius = cornerRadius
                                    }
                                    imageView.setImageDrawable(drawable)
                                } else {
                                    super.setResource(resource)
                                }
                            }
                        })
                } else {
                    // No corner radius, load as drawable
                    glide.load(effectiveUrl)
                        .listener(listener)
                        .into(imageView)
                }

                // Set placeholder if provided
                defaultDrawable?.let { 
                    if (imageView.drawable == null) {
                        imageView.setImageDrawable(it) 
                    }
                }
            } catch (ex: Throwable) {
                log.e(ex, "Glide load failed")
                errorDrawable?.let { imageView.setImageDrawable(it) }
            }
        }
    )

    DisposableEffect(effectiveUrl) {
        onDispose {
            // No explicit cleanup needed - Glide handles lifecycle automatically
        }
    }
}
