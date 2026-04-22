package es.ariaontheplanet.quasar.compose

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Network image component for avatar/media/card images.
 *
 * Thin wrapper over ComposeNetworkImage that kept the old param names
 * from the pre-Coil NetworkImage; leaves ~20 call sites unchanged.
 */
@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    cornerRadius: Float = 0f,
    staticUrl: String? = null,
    animatedUrl: String? = null,
    contentDescription: String? = null,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    defaultDrawable: android.graphics.drawable.Drawable? = null,
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
        contentDescription = "Preview Image"
    )
}
