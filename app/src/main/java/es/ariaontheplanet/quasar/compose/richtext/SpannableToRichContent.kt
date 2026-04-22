package es.ariaontheplanet.quasar.compose.richtext

import android.text.Spanned
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import es.ariaontheplanet.quasar.span.BlockCodeSpan
import es.ariaontheplanet.quasar.span.BlockQuoteSpan
import es.ariaontheplanet.quasar.span.DdSpan
import es.ariaontheplanet.quasar.span.EmojiImageSpan
import es.ariaontheplanet.quasar.span.HighlightSpan
import es.ariaontheplanet.quasar.span.HrSpan
import es.ariaontheplanet.quasar.span.InlineCodeSpan
import es.ariaontheplanet.quasar.span.MyClickableSpan
import es.ariaontheplanet.quasar.span.NetworkEmojiSpan
import es.ariaontheplanet.quasar.span.OrderedListItemSpan
import es.ariaontheplanet.quasar.span.SvgEmojiSpan
import es.ariaontheplanet.quasar.span.UnorderedListItemSpan

// Converts a Spannable (as produced by HTMLDecoder / EmojiDecoder / MFM parser)
// into the block-oriented RichContent used by the Compose RichText renderer.
//
// Inline spans are rendered natively (MyClickableSpan → LinkAnnotation,
// HighlightSpan → background, InlineCodeSpan → monospace background,
// {Network,Emoji,Svg}EmojiSpan → InlineTextContent placeholders).
//
// Block-level spans are emitted as separate RichBlock variants so they
// render with their decorations rather than flattening into a paragraph.
// Nested blocks are not decomposed recursively — an inner block falling
// inside an outer block's range is currently flattened into the outer's
// paragraph. Most Mastodon HTML is single-level so this is acceptable for
// now.

fun CharSequence.toRichContent(
    defaultLinkColor: Int = 0,
    onLinkClick: ((String) -> Unit)? = null,
): RichContent {
    val spannable = this as? Spanned
    if (spannable == null) {
        return RichContent(listOf(RichBlock.Paragraph(AnnotatedString(this.toString()))))
    }

    val length = spannable.length
    if (length == 0) return RichContent.Empty

    // Collect block-level spans, sorted by start, outer blocks first on ties.
    // We greedily take non-overlapping blocks; inner overlaps flatten into
    // the outer paragraph.
    val blockSpans = spannable.getSpans(0, length, Any::class.java)
        .mapNotNull { span ->
            val kind = blockKindOf(span) ?: return@mapNotNull null
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            if (start < 0 || end <= start) return@mapNotNull null
            Triple(kind, start, end) to span
        }
        .sortedWith(
            compareBy<Pair<Triple<BlockKind, Int, Int>, Any>> { it.first.second }
                .thenByDescending { it.first.third },
        )

    val blocks = mutableListOf<RichBlock>()
    var cursor = 0

    for ((boundary, span) in blockSpans) {
        val (kind, start, end) = boundary
        if (start < cursor) continue // skip inner/overlapping

        // Plain paragraph for any text before this block.
        if (cursor < start) {
            blocks += buildParagraph(spannable, cursor, start, defaultLinkColor, onLinkClick)
        }

        blocks += when (kind) {
            BlockKind.BlockQuote -> RichBlock.BlockQuote(
                listOf(buildParagraph(spannable, start, end, defaultLinkColor, onLinkClick)),
            )
            BlockKind.BlockCode -> RichBlock.CodeBlock(spannable.substring(start, end))
            BlockKind.Hr -> RichBlock.Hr
            BlockKind.OrderedItem -> {
                val index = (span as OrderedListItemSpan).order.trim().toIntOrNull()
                    ?.minus(1) ?: 0
                RichBlock.ListItem(
                    ordered = true,
                    index = index,
                    children = listOf(buildParagraph(spannable, start, end, defaultLinkColor, onLinkClick)),
                )
            }
            BlockKind.UnorderedItem -> RichBlock.ListItem(
                ordered = false,
                index = 0,
                children = listOf(buildParagraph(spannable, start, end, defaultLinkColor, onLinkClick)),
            )
            BlockKind.Indent -> RichBlock.Indent(
                listOf(buildParagraph(spannable, start, end, defaultLinkColor, onLinkClick)),
            )
        }

        cursor = end
    }

    // Trailing text.
    if (cursor < length) {
        blocks += buildParagraph(spannable, cursor, length, defaultLinkColor, onLinkClick)
    }

    if (blocks.isEmpty()) {
        // All block spans were degenerate; render the whole thing as one paragraph.
        blocks += buildParagraph(spannable, 0, length, defaultLinkColor, onLinkClick)
    }

    return RichContent(blocks)
}

private enum class BlockKind {
    BlockQuote,
    BlockCode,
    Hr,
    OrderedItem,
    UnorderedItem,
    Indent,
}

private fun blockKindOf(span: Any): BlockKind? = when (span) {
    is BlockQuoteSpan -> BlockKind.BlockQuote
    is BlockCodeSpan -> BlockKind.BlockCode
    is HrSpan -> BlockKind.Hr
    is OrderedListItemSpan -> BlockKind.OrderedItem
    is UnorderedListItemSpan -> BlockKind.UnorderedItem
    is DdSpan -> BlockKind.Indent
    else -> null
}

private fun buildParagraph(
    spannable: Spanned,
    start: Int,
    end: Int,
    defaultLinkColor: Int,
    onLinkClick: ((String) -> Unit)?,
): RichBlock.Paragraph {
    val text = spannable.subSequence(start, end).toString()
    val inline = mutableMapOf<String, InlineTextContent>()
    var emojiCounter = 0

    val annotated = buildAnnotatedString {
        append(text)

        // Translate each span's absolute range into a paragraph-local range.
        for (span in spannable.getSpans(start, end, Any::class.java)) {
            val sAbs = spannable.getSpanStart(span)
            val eAbs = spannable.getSpanEnd(span)
            if (sAbs < 0 || eAbs <= sAbs) continue
            // Clamp to the paragraph range.
            val sLocal = (sAbs - start).coerceAtLeast(0)
            val eLocal = (eAbs - start).coerceAtMost(end - start)
            if (eLocal <= sLocal) continue

            when (span) {
                is MyClickableSpan -> {
                    val color = MyClickableSpan.defaultLinkColor
                        .takeIf { it != 0 } ?: defaultLinkColor
                    val linkStyle = SpanStyle(
                        color = if (color != 0) Color(color.argbWithAlpha()) else Color.Unspecified,
                        textDecoration = if (MyClickableSpan.showLinkUnderline) {
                            TextDecoration.Underline
                        } else null,
                    )
                    val url = span.linkInfo.url
                    if (onLinkClick != null) {
                        val link = LinkAnnotation.Clickable(
                            tag = RICH_LINK_TAG,
                            styles = TextLinkStyles(style = linkStyle),
                            linkInteractionListener = { onLinkClick(url) },
                        )
                        addLink(link, sLocal, eLocal)
                    } else {
                        addStyle(linkStyle, sLocal, eLocal)
                        addStringAnnotation(RICH_LINK_TAG, url, sLocal, eLocal)
                    }
                }

                is HighlightSpan -> {
                    addStyle(
                        SpanStyle(
                            background = if (span.colorBg != 0) Color(span.colorBg.argbWithAlpha()) else Color.Unspecified,
                        ),
                        sLocal, eLocal,
                    )
                }

                is InlineCodeSpan -> {
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x40808080.toInt().argbWithAlpha()),
                        ),
                        sLocal, eLocal,
                    )
                }

                is NetworkEmojiSpan -> {
                    val id = "emoji-${emojiCounter++}"
                    addStringAnnotation(RICH_EMOJI_TAG, id, sLocal, eLocal)
                    inline[id] = networkEmojiInline(span)
                }

                is EmojiImageSpan -> {
                    val id = "emoji-${emojiCounter++}"
                    addStringAnnotation(RICH_EMOJI_TAG, id, sLocal, eLocal)
                    inline[id] = resourceEmojiInline(span.resId)
                }

                is SvgEmojiSpan -> {
                    val id = "emoji-${emojiCounter++}"
                    addStringAnnotation(RICH_EMOJI_TAG, id, sLocal, eLocal)
                    inline[id] = svgEmojiInline(span.assetsName)
                }
            }
        }
    }

    return RichBlock.Paragraph(annotated, inline)
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
        AsyncImage(
            model = span.url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }

private fun resourceEmojiInline(@androidx.annotation.DrawableRes resId: Int): InlineTextContent =
    InlineTextContent(emojiPlaceholder) {
        if (resId != 0) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }

private fun svgEmojiInline(assetsName: String): InlineTextContent =
    InlineTextContent(emojiPlaceholder) {
        // Coil 3 has SVG support via the registered SvgDecoder.
        AsyncImage(
            model = "file:///android_asset/$assetsName",
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }
