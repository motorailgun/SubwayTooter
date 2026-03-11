package jp.juggler.subwaytooter.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.R

/**
 * Compose replacement for inflateColumnHeader() in ColumnViewHolder.
 * Displays column icon, name, context (account), status, index, and action buttons.
 */
@Composable
fun ColumnHeaderBar(
    uiState: ColumnUiState,
    callbacks: ColumnCallbacks,
    modifier: Modifier = Modifier,
) {
    val rev = uiState.uiRevision // read to trigger recomposition

    val headerNameColor = Color(uiState.headerNameColor)
    val headerPageNumberColor = Color(uiState.headerPageNumberColor)
    val headerBgColor = if (uiState.headerBgColorIsDefault) {
        Color.Unspecified
    } else {
        Color(uiState.headerBgColor)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .let { if (headerBgColor != Color.Unspecified) it.background(headerBgColor) else it }
            .clickable { callbacks.onHeaderClick() }
            .padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        // Top row: context (account), status, index
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
        ) {
            // Account context
            Text(
                text = uiState.columnContext,
                color = if (uiState.columnContextColorFg != 0) Color(uiState.columnContextColorFg) else headerPageNumberColor,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .let {
                        if (uiState.columnContextColorBg != 0)
                            it.background(Color(uiState.columnContextColorBg))
                        else it
                    }
                    .padding(horizontal = uiState.columnContextPadLr.dp),
            )
            // Status
            Text(
                text = uiState.columnStatus.toString(),
                color = headerPageNumberColor,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 12.dp),
            )
            // Index
            Text(
                text = uiState.columnIndex,
                color = headerPageNumberColor,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        // Bottom row: icon, name, action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Column icon
            if (uiState.columnIconResId != 0) {
                Icon(
                    painter = painterResource(uiState.columnIconResId),
                    contentDescription = null,
                    tint = headerNameColor,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 4.dp),
                )
            }

            // Column name
            Text(
                text = uiState.columnName,
                color = headerNameColor,
                modifier = Modifier.weight(1f),
            )

            // Announcements button + badge
            if (uiState.announcementButtonVisible) {
                Box {
                    IconButton(onClick = callbacks.onAnnouncementsToggle) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info_outline),
                            contentDescription = stringResource(R.string.announcements),
                            tint = headerNameColor,
                        )
                    }
                    if (uiState.announcementBadgeVisible) {
                        Icon(
                            painter = painterResource(R.drawable.announcements_dot),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .padding(end = 4.dp, top = 4.dp),
                        )
                    }
                }
            }

            // Setting button
            IconButton(onClick = callbacks.onColumnSettingToggle) {
                Icon(
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = stringResource(R.string.setting),
                    tint = headerNameColor,
                )
            }

            // Reload button
            IconButton(onClick = callbacks.onColumnReload) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.reload),
                    tint = headerNameColor,
                )
            }

            // Close button
            IconButton(
                onClick = callbacks.onColumnClose,
                enabled = uiState.closeButtonEnabled,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_column),
                    tint = headerNameColor.copy(
                        alpha = if (uiState.closeButtonEnabled) 1f else 0.3f
                    ),
                )
            }
        }
    }
}
