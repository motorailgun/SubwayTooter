package es.ariaontheplanet.quasar.compose

import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import es.ariaontheplanet.quasar.util.NetworkEmojiInvalidator
import es.ariaontheplanet.quasar.view.MyLinkMovementMethod

/**
 * Network image component for avatar/media/card images using Glide.
 * Uses the new ComposeNetworkImage instead of legacy MyNetworkImageView.
 *
 * @param modifier Compose modifier
 * @param cornerRadius corner radius in pixels (0f = square)
 * @param staticUrl static image URL
 * @param animatedUrl animated image URL (null = same as staticUrl)
 * @param contentDescription accessibility text
 * @param scaleType ImageView scale type
 * @param defaultDrawable default drawable if image fails
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

/**
 * AndroidView wrapper for AppCompatTextView that supports Spannable text with custom emoji animation.
 *
 * @param modifier Compose modifier
 * @param text the Spannable or CharSequence to display
 * @param textColor text color (ARGB int)
 * @param textSizeSp text size in SP (Float.NaN to use default)
 * @param typeface typeface for text
 * @param lineSpacingMultiplier line spacing multiplier
 * @param maxLines max lines (0 = unlimited)
 * @param movementMethod if true, enables MyLinkMovementMethod for clickable links
 * @param handler Handler for NetworkEmojiInvalidator
 * @param onInvalidatorReady callback to receive the NetworkEmojiInvalidator for external updates
 */
@Composable
fun SpannableTextView(
    modifier: Modifier = Modifier,
    text: CharSequence? = null,
    textColor: Int = 0,
    textSizeSp: Float = Float.NaN,
    typeface: android.graphics.Typeface? = null,
    lineSpacingMultiplier: Float = 1f,
    maxLines: Int = 0,
    movementMethod: Boolean = false,
    handler: android.os.Handler? = null,
    onInvalidatorReady: ((NetworkEmojiInvalidator) -> Unit)? = null,
) {
    val invalidator = remember { handler?.let { h -> arrayOfNulls<NetworkEmojiInvalidator>(1) } }

    DisposableEffect(Unit) {
        onDispose {
            invalidator?.firstOrNull()?.register(null)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AppCompatTextView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                if (movementMethod) {
                    this.movementMethod = MyLinkMovementMethod
                }
                if (handler != null) {
                    val inv = NetworkEmojiInvalidator(handler, this)
                    invalidator?.set(0, inv)
                    onInvalidatorReady?.invoke(inv)
                }
            }
        },
        update = { tv ->
            if (textColor != 0) tv.setTextColor(textColor)
            if (!textSizeSp.isNaN()) tv.textSize = textSizeSp
            typeface?.let { tv.typeface = it }
            if (lineSpacingMultiplier != 1f) tv.setLineSpacing(0f, lineSpacingMultiplier)
            if (maxLines > 0) tv.maxLines = maxLines

            // Set text through invalidator if available, otherwise directly
            val inv = invalidator?.firstOrNull()
            if (inv != null && text != null) {
                inv.text = text
            } else {
                tv.text = text
            }
        },
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

@Preview(showBackground = true)
@Composable
fun PreviewSpannableTextView() {
    SpannableTextView(
        text = "Hello, world!"
    )
}
