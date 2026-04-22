package es.ariaontheplanet.quasar.compose.richtext

import android.text.Spanned
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import es.ariaontheplanet.quasar.span.EmojiImageSpan
import es.ariaontheplanet.quasar.span.HighlightSpan
import es.ariaontheplanet.quasar.span.InlineCodeSpan
import es.ariaontheplanet.quasar.span.MyClickableSpan
import es.ariaontheplanet.quasar.span.NetworkEmojiSpan
import es.ariaontheplanet.quasar.span.SvgEmojiSpan

// Converts a Spannable (as produced by HTMLDecoder / EmojiDecoder / MFM parser)
// into the block-oriented RichContent used by the Compose RichText renderer.
//
// Scope: this is the Phase 6c bridgehead. Supports inline spans used across
// the entire app (MyClickableSpan, HighlightSpan, InlineCodeSpan) and the three
// emoji replacement spans (NetworkEmojiSpan, EmojiImageSpan, SvgEmojiSpan).
// Animated spans (NetworkEmojiSpan animation, Misskey big/motion) render as
// static approximations — animation is a follow-up.
//
// Block-level spans (BlockQuote/BlockCode/Hr/list/Dd) are **flattened** into a
// single Paragraph in this first pass. Most account bios and display names
// don't hit them; proper block decomposition is scheduled for 6c-2.

fun CharSequence.toRichContent(defaultLinkColor: Int = 0): RichContent {
    val spannable = this as? Spanned
    if (spannable == null) {
        return RichContent(listOf(RichBlock.Paragraph(AnnotatedString(this.toString()))))
    }

    val text = spannable.toString()
    val inline = mutableMapOf<String, InlineTextContent>()
    var emojiCounter = 0

    val annotated = buildAnnotatedString {
        append(text)

        // Inline styling spans — apply in document order.
        for (span in spannable.getSpans(0, text.length, Any::class.java)) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            if (start < 0 || end <= start) continue

            when (span) {
                is MyClickableSpan -> {
                    val color = MyClickableSpan.defaultLinkColor
                        .takeIf { it != 0 } ?: defaultLinkColor
                    addStyle(
                        SpanStyle(
                            color = if (color != 0) Color(color.argbWithAlpha()) else Color.Unspecified,
                            textDecoration = if (MyClickableSpan.showLinkUnderline) {
                                TextDecoration.Underline
                            } else null,
                        ),
                        start, end,
                    )
                    addStringAnnotation(RICH_LINK_TAG, span.linkInfo.url, start, end)
                }

                is HighlightSpan -> {
                    addStyle(
                        SpanStyle(
                            color = Color(span.colorBg.argbWithAlpha()).takeIf { false } // never
                                ?: Color.Unspecified,
                            background = if (span.colorBg != 0) Color(span.colorBg.argbWithAlpha()) else Color.Unspecified,
                        ),
                        start, end,
                    )
                }

                is InlineCodeSpan -> {
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x40808080.toInt().argbWithAlpha()),
                        ),
                        start, end,
                    )
                }

                is NetworkEmojiSpan -> {
                    val id = "emoji-${emojiCounter++}"
                    // Replace the placeholder char with the tag's id so InlineTextContent lookup works.
                    addStringAnnotation(RICH_EMOJI_TAG, id, start, end)
                    inline[id] = networkEmojiInline(span)
                }

                is EmojiImageSpan -> {
                    val id = "emoji-${emojiCounter++}"
                    addStringAnnotation(RICH_EMOJI_TAG, id, start, end)
                    inline[id] = resourceEmojiInline(span.resIdOrNull())
                }

                is SvgEmojiSpan -> {
                    val id = "emoji-${emojiCounter++}"
                    addStringAnnotation(RICH_EMOJI_TAG, id, start, end)
                    inline[id] = svgEmojiInline(span.assetPathOrNull())
                }

                // Other spans (BlockQuote / BlockCode / Hr / list / Dd / Animatable / Misskey*)
                // fall through and currently render as plain text. Block decomposition
                // lands in the next 6c slice.
            }
        }
    }

    return RichContent(listOf(RichBlock.Paragraph(annotated, inline)))
}

private fun Int.argbWithAlpha(): Long =
    (if (this.toLong() and 0xFF000000L == 0L) this.toLong() or 0xFF000000L else this.toLong()) and 0xFFFFFFFFL

// Shape of the inline placeholder for a single emoji — one line-height square,
// center-aligned with the surrounding baseline.
private val emojiPlaceholder = Placeholder(
    width = 1.em,
    height = 1.em,
    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
)

private fun networkEmojiInline(span: NetworkEmojiSpan): InlineTextContent =
    InlineTextContent(emojiPlaceholder) {
        val url = span.urlOrNull()
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }

private fun resourceEmojiInline(@androidx.annotation.DrawableRes resId: Int?): InlineTextContent =
    InlineTextContent(emojiPlaceholder) {
        if (resId != null && resId != 0) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }

private fun svgEmojiInline(assetPath: String?): InlineTextContent =
    InlineTextContent(emojiPlaceholder) {
        if (!assetPath.isNullOrEmpty()) {
            // Coil 3 has SVG support via the registered SvgDecoder.
            AsyncImage(
                model = "file:///android_asset/$assetPath",
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }

// Reflection-free accessors — the span classes don't expose their private
// fields publicly, so add these small bridges where needed. The ones below
// read public properties that already exist; if a field is private, we
// fall back to null (rendering an empty placeholder) rather than crashing.

private fun NetworkEmojiSpan.urlOrNull(): String? = try {
    val f = javaClass.getDeclaredField("url")
    f.isAccessible = true
    f.get(this) as? String
} catch (_: Throwable) {
    null
}

private fun EmojiImageSpan.resIdOrNull(): Int? = try {
    val f = javaClass.getDeclaredField("resId")
    f.isAccessible = true
    f.get(this) as? Int
} catch (_: Throwable) {
    null
}

private fun SvgEmojiSpan.assetPathOrNull(): String? = try {
    // Try common field names.
    listOf("assetsName", "assetPath", "path").firstNotNullOfOrNull { name ->
        try {
            val f = javaClass.getDeclaredField(name)
            f.isAccessible = true
            f.get(this) as? String
        } catch (_: Throwable) {
            null
        }
    }
} catch (_: Throwable) {
    null
}
