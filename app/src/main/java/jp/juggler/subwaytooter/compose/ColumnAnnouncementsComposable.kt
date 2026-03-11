package jp.juggler.subwaytooter.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R

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
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(uiState.searchFormBgColor))
                .padding(vertical = 2.dp),
        ) {
            val contentColor = Color(uiState.announcementContentColor)

            // Caption + paging row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(R.string.announcements),
                    color = contentColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )

                if (uiState.announcementEnablePaging) {
                    IconButton(onClick = callbacks.onAnnouncementsPrev) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_start),
                            contentDescription = stringResource(R.string.previous),
                            tint = contentColor.copy(
                                alpha = if (uiState.announcementEnablePaging) 1f else 0.3f
                            ),
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
                            tint = contentColor.copy(
                                alpha = if (uiState.announcementEnablePaging) 1f else 0.3f
                            ),
                        )
                    }
                }
            }

            // Content area (scrollable, max height)
            if (uiState.announcementsExpanded) {
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

                    // Content text
                    Text(
                        text = uiState.announcementContent.toString(),
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

                            // Existing reactions
                            uiState.announcementReactions.forEachIndexed { index, item ->
                                Button(
                                    onClick = { callbacks.onReactionClick(index) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (item.isMe)
                                            contentColor.copy(alpha = 0.2f)
                                        else Color.Transparent,
                                        contentColor = contentColor,
                                    ),
                                ) {
                                    Text(text = item.displayText.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
