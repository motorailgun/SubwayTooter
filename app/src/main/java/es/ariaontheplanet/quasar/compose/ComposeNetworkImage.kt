package es.ariaontheplanet.quasar.compose

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.ariaontheplanet.quasar.pref.PrefB
import jp.juggler.util.data.notEmpty

/**
 * Compose-native network image component, backed by Coil 3's [AsyncImage].
 * Replaces the earlier AndroidView+Glide bridge.
 *
 * @param url static image URL
 * @param cornerRadius rounded corners in pixels (0f = square)
 * @param animatedUrl optional animated URL — selected when [PrefB.bpImageAnimationEnable] is on
 * @param scaleType ImageView scale type (mapped to Compose [ContentScale])
 * @param defaultDrawable shown while loading / when URL is blank
 * @param errorDrawable shown on load failure
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
    val density = LocalDensity.current

    val effectiveUrl = if (PrefB.bpImageAnimationEnable.value) {
        animatedUrl?.notEmpty() ?: url
    } else {
        url
    }

    val shape = if (cornerRadius > 0f) {
        RoundedCornerShape(with(density) { cornerRadius.toDp() })
    } else null

    val imageModifier = if (shape != null) modifier.clip(shape) else modifier

    // No URL → show the placeholder / default only.
    if (effectiveUrl.isNullOrEmpty()) {
        val painter = defaultDrawable?.toPainter()
        Box(modifier = imageModifier) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = scaleType.toComposeContentScale(),
                    modifier = Modifier,
                )
            }
        }
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(effectiveUrl)
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        modifier = imageModifier,
        contentScale = scaleType.toComposeContentScale(),
        placeholder = defaultDrawable?.toPainter(),
        error = errorDrawable?.toPainter(),
        fallback = defaultDrawable?.toPainter(),
    )
}

private fun ImageView.ScaleType.toComposeContentScale(): ContentScale = when (this) {
    ImageView.ScaleType.CENTER -> ContentScale.None
    ImageView.ScaleType.CENTER_CROP -> ContentScale.Crop
    ImageView.ScaleType.CENTER_INSIDE -> ContentScale.Inside
    ImageView.ScaleType.FIT_CENTER,
    ImageView.ScaleType.FIT_START,
    ImageView.ScaleType.FIT_END -> ContentScale.Fit
    ImageView.ScaleType.FIT_XY -> ContentScale.FillBounds
    else -> ContentScale.Fit
}

private fun Drawable.toPainter(): Painter =
    BitmapPainter(toBitmap().asImageBitmap())

/**
 * Thin wrapper over [ComposeNetworkImage] that kept the old param names from
 * the pre-Coil bridge; leaves ~20 call sites unchanged.
 */
@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    cornerRadius: Float = 0f,
    staticUrl: String? = null,
    animatedUrl: String? = null,
    contentDescription: String? = null,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    defaultDrawable: Drawable? = null,
) {
    ComposeNetworkImage(
        url = staticUrl,
        modifier = modifier,
        cornerRadius = cornerRadius,
        animatedUrl = animatedUrl,
        contentDescription = contentDescription,
        scaleType = scaleType,
        defaultDrawable = defaultDrawable,
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewNetworkImage() {
    NetworkImage(
        staticUrl = "https://example.com/image.png",
        contentDescription = "Preview Image",
    )
}
