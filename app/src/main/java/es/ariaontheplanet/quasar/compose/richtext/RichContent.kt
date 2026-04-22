package es.ariaontheplanet.quasar.compose.richtext

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.AnnotatedString

// Block-oriented rich-text representation. Produced by
// CharSequence.toRichContent() and consumed by the RichText composable.
// Replaces the AndroidView-wrapped SpannableTextView bridge for Compose
// rendering of decoded toot text, account bios, display names, etc.

data class RichContent(val blocks: List<RichBlock>) {
    companion object {
        val Empty = RichContent(emptyList())
    }
}

sealed interface RichBlock {
    /** A run of text, with inline styling + inline-content placeholders (emoji). */
    data class Paragraph(
        val text: AnnotatedString,
        val inline: Map<String, InlineTextContent> = emptyMap(),
    ) : RichBlock

    /** <blockquote> — draws a left bar + indent, then renders [children]. */
    data class BlockQuote(val children: List<RichBlock>) : RichBlock

    /** <pre><code> — monospace, tinted background. */
    data class CodeBlock(val text: String) : RichBlock

    /** <hr> — a horizontal divider. */
    data object Hr : RichBlock

    /** Numbered or bulleted list item. */
    data class ListItem(
        val ordered: Boolean,
        val index: Int,
        val children: List<RichBlock>,
    ) : RichBlock

    /** Plain indent — used for <dd>. */
    data class Indent(val children: List<RichBlock>) : RichBlock
}

/** Annotation tag used by MyClickableSpan migration. [AnnotatedString] carries
 *  the URL on this tag; RichText's click handler looks it up. */
const val RICH_LINK_TAG: String = "rich-link"

/** Annotation tag used by custom-emoji placeholders — the value is the shortcode/url. */
const val RICH_EMOJI_TAG: String = "rich-emoji"
