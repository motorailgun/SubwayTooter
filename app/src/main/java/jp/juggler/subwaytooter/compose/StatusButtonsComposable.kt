@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package jp.juggler.subwaytooter.compose

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.InstanceCapability
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.api.entity.TootNotification
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.stylerBoostAlpha
import jp.juggler.util.data.notZero
import jp.juggler.util.ui.applyAlphaMultiplier

/**
 * Status action button bar (reply, boost, favourite, bookmark, more).
 */
@Composable
fun StatusActionButtons(
    activity: ActMain,
    column: Column,
    status: TootStatus,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    notification: TootNotification? = null,
) {
    val contentColorCompose = Color(contentColor)
    val accessInfo = column.accessInfo
    val ti = remember(accessInfo) { TootInstance.getCached(accessInfo) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Conversation button
        ActionButton(
            iconId = R.drawable.ic_forum,
            contentDescription = stringResource(R.string.conversation_view),
            tint = contentColorCompose,
            onClick = { callbacks.onConversationButtonClick(column, status, notification) },
        )

        // Reply button
        val replyIconId = if (status.in_reply_to_id != null) {
            R.drawable.ic_reply
        } else {
            R.drawable.ic_reply
        }
        ActionButtonWithCount(
            iconId = replyIconId,
            contentDescription = stringResource(R.string.reply),
            tint = contentColorCompose,
            count = status.replies_count,
            onClick = { callbacks.onReplyButtonClick(column, status, notification) },
            onLongClick = { callbacks.onReplyButtonLongClick(column, status, notification) },
        )

        // Boost button
        val boostColor = when {
            status.reblogged -> Color(0xFF2B90D9.toInt()) // boosted color
            else -> contentColorCompose
        }
        ActionButtonWithCount(
            iconId = R.drawable.ic_repeat,
            contentDescription = stringResource(R.string.boost),
            tint = boostColor,
            count = status.reblogs_count,
            onClick = { callbacks.onBoostButtonClick(column, status, notification) },
            onLongClick = { callbacks.onBoostButtonLongClick(column, status, notification) },
        )

        // Favourite button
        val favColor = when {
            status.favourited -> Color(0xFFCA8F04.toInt()) // favourited color
            else -> contentColorCompose
        }
        ActionButtonWithCount(
            iconId = if (accessInfo.isMisskey) R.drawable.ic_star else R.drawable.ic_star_outline,
            contentDescription = stringResource(R.string.favourite),
            tint = favColor,
            count = status.favourites_count,
            onClick = { callbacks.onFavouriteButtonClick(column, status, notification) },
            onLongClick = { callbacks.onFavouriteButtonLongClick(column, status, notification) },
        )

        // Bookmark button (if enabled)
        if (PrefB.bpShowBookmarkButton.value) {
            val bmColor = when {
                status.bookmarked -> Color(0xFFCA8F04.toInt())
                else -> contentColorCompose
            }
            ActionButton(
                iconId = if (status.bookmarked) R.drawable.ic_bookmark_added else R.drawable.ic_bookmark,
                contentDescription = stringResource(R.string.bookmark),
                tint = bmColor,
                onClick = { callbacks.onBookmarkButtonClick(column, status, notification) },
            )
        }

        // Quote button (if supported)
        if (ti?.let { InstanceCapability.quote(it) } == true) {
            ActionButton(
                iconId = R.drawable.ic_quote,
                contentDescription = stringResource(R.string.quote),
                tint = contentColorCompose,
                onClick = { callbacks.onQuoteButtonClick(column, status, notification) },
            )
        }

        // Reaction button (if supported)
        if (ti?.let { InstanceCapability.canEmojiReaction(accessInfo, it) } == true) {
            ActionButton(
                iconId = R.drawable.ic_add,
                contentDescription = stringResource(R.string.reaction),
                tint = contentColorCompose,
                onClick = { callbacks.onReactionButtonClick(column, status, notification) },
            )
        }

        // More button
        ActionButton(
            iconId = R.drawable.ic_more,
            contentDescription = stringResource(R.string.more),
            tint = contentColorCompose,
            onClick = { callbacks.onMoreButtonClick(column, status, notification) },
        )
    }
}

/**
 * Single action button.
 */
@Composable
fun ActionButton(
    iconId: Int,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Action button with optional count badge.
 */
@Composable
fun ActionButtonWithCount(
    iconId: Int,
    contentDescription: String,
    tint: Color,
    count: Long?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        if (count != null && count > 0) {
            Text(
                text = count.toString(),
                color = tint,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}
