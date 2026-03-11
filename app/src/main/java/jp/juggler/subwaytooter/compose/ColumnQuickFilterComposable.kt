package jp.juggler.subwaytooter.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.column.Column

/**
 * Compose replacement for inflateQuickFilter() + showQuickFilter().
 * Horizontal scrollable row of notification filter buttons.
 */
@Composable
fun ColumnQuickFilterBar(
    uiState: ColumnUiState,
    callbacks: ColumnCallbacks,
    modifier: Modifier = Modifier,
) {
    // Don't show if in settings mode (rendered inside settings panel instead)
    if (uiState.quickFilterInsideSetting) return

    AnimatedVisibility(visible = uiState.quickFilterVisible) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(uiState.quickFilterBgColor))
                .horizontalScroll(rememberScrollState()),
        ) {
            val fgColor = Color(uiState.quickFilterFgColor)
            val selectedBg = Color(uiState.quickFilterSelectedBgColor)
            val normalBg = Color(uiState.quickFilterBgColor)

            // All
            QuickFilterTextButton(
                text = stringResource(R.string.all),
                selected = uiState.quickFilter == Column.QUICK_FILTER_ALL,
                fgColor = fgColor,
                selectedBg = selectedBg,
                normalBg = normalBg,
                onClick = { callbacks.onQuickFilterClick(Column.QUICK_FILTER_ALL) },
            )

            // Mention
            QuickFilterIconButton(
                iconRes = R.drawable.ic_reply,
                contentDescription = stringResource(R.string.mention2),
                selected = uiState.quickFilter == Column.QUICK_FILTER_MENTION,
                fgColor = fgColor,
                selectedBg = selectedBg,
                normalBg = normalBg,
                onClick = { callbacks.onQuickFilterClick(Column.QUICK_FILTER_MENTION) },
            )

            // Favourite (hidden on Misskey)
            if (uiState.showQuickFilterFavourite) {
                QuickFilterIconButton(
                    iconRes = R.drawable.ic_star_outline,
                    contentDescription = stringResource(R.string.favourite),
                    selected = uiState.quickFilter == Column.QUICK_FILTER_FAVOURITE,
                    fgColor = fgColor,
                    selectedBg = selectedBg,
                    normalBg = normalBg,
                    onClick = { callbacks.onQuickFilterClick(Column.QUICK_FILTER_FAVOURITE) },
                )
            }

            // Boost
            QuickFilterIconButton(
                iconRes = R.drawable.ic_repeat,
                contentDescription = stringResource(R.string.boost),
                selected = uiState.quickFilter == Column.QUICK_FILTER_BOOST,
                fgColor = fgColor,
                selectedBg = selectedBg,
                normalBg = normalBg,
                onClick = { callbacks.onQuickFilterClick(Column.QUICK_FILTER_BOOST) },
            )

            // Follow
            QuickFilterIconButton(
                iconRes = R.drawable.ic_person_add,
                contentDescription = stringResource(R.string.follow),
                selected = uiState.quickFilter == Column.QUICK_FILTER_FOLLOW,
                fgColor = fgColor,
                selectedBg = selectedBg,
                normalBg = normalBg,
                onClick = { callbacks.onQuickFilterClick(Column.QUICK_FILTER_FOLLOW) },
            )

            // Post
            QuickFilterIconButton(
                iconRes = R.drawable.ic_send,
                contentDescription = stringResource(R.string.notification_type_post),
                selected = uiState.quickFilter == Column.QUICK_FILTER_POST,
                fgColor = fgColor,
                selectedBg = selectedBg,
                normalBg = normalBg,
                onClick = { callbacks.onQuickFilterClick(Column.QUICK_FILTER_POST) },
            )

            // Reaction (Misskey only)
            if (uiState.showQuickFilterReaction) {
                QuickFilterIconButton(
                    iconRes = R.drawable.ic_add,
                    contentDescription = stringResource(R.string.reaction),
                    selected = uiState.quickFilter == Column.QUICK_FILTER_REACTION,
                    fgColor = fgColor,
                    selectedBg = selectedBg,
                    normalBg = normalBg,
                    onClick = { callbacks.onQuickFilterClick(Column.QUICK_FILTER_REACTION) },
                )
            }

            // Vote
            QuickFilterIconButton(
                iconRes = R.drawable.ic_vote,
                contentDescription = stringResource(R.string.vote_polls),
                selected = uiState.quickFilter == Column.QUICK_FILTER_VOTE,
                fgColor = fgColor,
                selectedBg = selectedBg,
                normalBg = normalBg,
                onClick = { callbacks.onQuickFilterClick(Column.QUICK_FILTER_VOTE) },
            )
        }
    }
}

@Composable
private fun QuickFilterTextButton(
    text: String,
    selected: Boolean,
    fgColor: Color,
    selectedBg: Color,
    normalBg: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) selectedBg else normalBg,
            contentColor = fgColor,
        ),
    ) {
        Text(text = text)
    }
}

@Composable
private fun QuickFilterIconButton(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    fgColor: Color,
    selectedBg: Color,
    normalBg: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) selectedBg else normalBg,
        ),
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = fgColor,
        )
    }
}
