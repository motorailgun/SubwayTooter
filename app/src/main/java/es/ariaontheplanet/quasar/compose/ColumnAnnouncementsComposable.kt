package es.ariaontheplanet.quasar.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.compose.richtext.RichText
import es.ariaontheplanet.quasar.compose.richtext.toRichContent
import es.ariaontheplanet.quasar.span.MyClickableSpan

/**
 * Compose replacement for inflateAnnouncementsBox() and showAnnouncements() in ColumnViewHolder.
 * Displays announcement caption, paging, content, and reaction buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnAnnouncementsBox(
    uiState: ColumnUiState,
    callbacks: ColumnCallbacks,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = uiState.announcementsBoxVisible) {
        var expanded by remember { mutableStateOf(true) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(uiState.searchFormBgColor))
                .padding(vertical = 2.dp),
        ) {
            val contentColor = Color(uiState.announcementContentColor)

            // Caption + paging row — tap anywhere outside the paging buttons to toggle.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = contentColor,
                )
                Text(
                    text = stringResource(R.string.announcements),
                    color = contentColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )

                if (uiState.announcementEnablePaging) {
                    IconButton(onClick = callbacks.onAnnouncementsPrev) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_start),
                            contentDescription = stringResource(R.string.previous),
                            tint = contentColor,
                        )
                    }
                }

                Text(
                    text = uiState.announcementsIndex,
                    color = contentColor,
                    modifier = Modifier.padding(start = 4.dp),
                )

                if (uiState.announcementEnablePaging) {
                    IconButton(onClick = callbacks.onAnnouncementsNext) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_end),
                            contentDescription = stringResource(R.string.next),
                            tint = contentColor,
                        )
                    }
                }
            }

            // Content area (scrollable, max height)
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    // Period
                    uiState.announcementPeriod?.let { period ->
                        Text(
                            text = period,
                            color = contentColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 3.dp),
                        )
                    }

                    // Content text — decoded HTML with mentions/hashtags/emoji.
                    val announcementContent = remember(uiState.announcementContent) {
                        uiState.announcementContent.toRichContent(MyClickableSpan.defaultLinkColor)
                    }
                    RichText(
                        content = announcementContent,
                        color = contentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp),
                    )

                    // Reaction buttons
                    if (uiState.announcementReactions.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // Add reaction button
                            IconButton(
                                onClick = { callbacks.onReactionAdd(-1) },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = stringResource(R.string.reaction_add),
                                    tint = contentColor,
                                )
                            }

                            // Existing reactions — displayText carries an emoji span.
                            uiState.announcementReactions.forEachIndexed { index, item ->
                                val reactionContent = remember(item.displayText) {
                                    item.displayText.toRichContent()
                                }
                                Button(
                                    onClick = { callbacks.onReactionClick(index) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (item.isMe)
                                            contentColor.copy(alpha = 0.2f)
                                        else Color.Transparent,
                                        contentColor = contentColor,
                                    ),
                                ) {
                                    RichText(content = reactionContent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
