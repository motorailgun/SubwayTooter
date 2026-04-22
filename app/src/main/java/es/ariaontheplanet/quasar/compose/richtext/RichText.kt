package es.ariaontheplanet.quasar.compose.richtext

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * Renders [RichContent] produced by [toRichContent].
 *
 * @param content the decoded block list
 * @param modifier Compose modifier
 * @param color default text colour; [Color.Unspecified] means inherit from [LocalTextStyle]
 * @param style base text style (size, font family, etc.)
 * @param maxLines applied to each [RichBlock.Paragraph]. 0 = unlimited.
 * @param onLinkClick invoked with the URL when the user taps a link annotation.
 */
@Composable
fun RichText(
    content: RichContent,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = 0,
    onLinkClick: ((String) -> Unit)? = null,
) {
    if (content.blocks.isEmpty()) return
    // Simple path — single paragraph with no block decoration: emit a Text directly.
    val only = content.blocks.singleOrNull() as? RichBlock.Paragraph
    if (only != null) {
        RichParagraph(
            block = only,
            modifier = modifier,
            color = color,
            style = style,
            maxLines = maxLines,
            onLinkClick = onLinkClick,
        )
        return
    }
    Column(modifier = modifier) {
        content.blocks.forEach { block ->
            RichBlockContent(block, color, style, maxLines, onLinkClick)
        }
    }
}

@Composable
private fun RichBlockContent(
    block: RichBlock,
    color: Color,
    style: TextStyle,
    maxLines: Int,
    onLinkClick: ((String) -> Unit)?,
) {
    when (block) {
        is RichBlock.Paragraph -> RichParagraph(block, Modifier, color, style, maxLines, onLinkClick)
        is RichBlock.BlockQuote -> Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Spacer(
                Modifier
                    .width(4.dp)
                    .background(color = MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                block.children.forEach { RichBlockContent(it, color, style, maxLines, onLinkClick) }
            }
        }
        is RichBlock.CodeBlock -> Text(
            text = block.text,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            style = style.copy(fontFamily = FontFamily.Monospace),
            color = color,
        )
        RichBlock.Hr -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        is RichBlock.ListItem -> Row(modifier = Modifier.padding(vertical = 2.dp)) {
            val marker = if (block.ordered) "${block.index + 1}." else "•"
            Text(text = marker, modifier = Modifier.padding(end = 6.dp), color = color, style = style)
            Column {
                block.children.forEach { RichBlockContent(it, color, style, maxLines, onLinkClick) }
            }
        }
        is RichBlock.Indent -> Row(modifier = Modifier.padding(start = 24.dp)) {
            Column {
                block.children.forEach { RichBlockContent(it, color, style, maxLines, onLinkClick) }
            }
        }
    }
}

@Composable
private fun RichParagraph(
    block: RichBlock.Paragraph,
    modifier: Modifier,
    color: Color,
    style: TextStyle,
    maxLines: Int,
    onLinkClick: ((String) -> Unit)?,
) {
    val overflow = if (maxLines > 0) TextOverflow.Ellipsis else TextOverflow.Clip

    val displayText = if (block.animRanges.isEmpty()) {
        block.text
    } else {
        animateTextScale(block.text, block.animRanges)
    }

    Text(
        text = displayText,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = if (maxLines > 0) maxLines else Int.MAX_VALUE,
        overflow = overflow,
        inlineContent = block.inline,
    )
}

/**
 * Rebuilds [base] once per animation frame with the scale-oscillating
 * SpanStyle applied to each [ranges] entry. The two MFM animations:
 *
 * - MisskeyBigSpan: period ≈ 3.14 s, scale oscillates 1.2…1.5 em.
 * - MisskeyMotionSpan: period ≈ 0.63 s, scale oscillates 0.8…1.0 em.
 *
 * (A true sine wave is approximated with a linear triangle wave; the
 * visual difference at these scales is imperceptible.)
 */
@Composable
private fun animateTextScale(
    base: AnnotatedString,
    ranges: List<AnimRange>,
): AnnotatedString {
    val transition = rememberInfiniteTransition(label = "mfmScale")
    val bigScale by transition.animateFloat(
        initialValue = 1.2f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1570, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mfmBig",
    )
    val motionScale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 314, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mfmMotion",
    )
    return remember(base, ranges, bigScale, motionScale) {
        buildAnnotatedString {
            append(base)
            ranges.forEach { r ->
                val scale = when (r.type) {
                    AnimType.MisskeyBig -> bigScale
                    AnimType.MisskeyMotion -> motionScale
                }
                addStyle(SpanStyle(fontSize = scale.em), r.start, r.end)
            }
        }
    }
}
