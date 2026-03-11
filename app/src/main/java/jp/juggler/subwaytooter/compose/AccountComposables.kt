@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package jp.juggler.subwaytooter.compose

import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootAccountRef
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.setFollowIcon
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.api.entity.SuggestionSource
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.table.daoUserRelation
import jp.juggler.util.data.notZero

/**
 * Composable for rendering an account/follow item in the timeline.
 */
@Composable
fun AccountItemContent(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    whoRef: TootAccountRef,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
) {
    val who = whoRef.get()
    val contentColorCompose = Color(contentColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { callbacks.onFollowClick(column, whoRef, whoRef) },
                onLongClick = { callbacks.onFollowLongClick(column, whoRef, whoRef) },
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        NetworkImage(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
            staticUrl = accessInfo.supplyBaseUrl(who.avatar_static),
            animatedUrl = accessInfo.supplyBaseUrl(who.avatar),
            contentDescription = stringResource(R.string.thumbnail),
            scaleType = ImageView.ScaleType.FIT_END,
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Name and Acct
        Column(modifier = Modifier.weight(1f)) {
            SpannableTextView(
                text = whoRef.decoded_display_name,
                textColor = contentColor,
                textSizeSp = ActMain.timelineFontSizeSp.takeIf { it.isFinite() } ?: Float.NaN,
                typeface = ActMain.timelineFontBold,
                handler = activity.handler,
            )

            AcctText(
                accessInfo = accessInfo,
                who = who,
                acctColor = acctColor,
            )

            // Last active status
            // (handled by the account's setAccountExtra in the original code)
        }

        // Follow button area
        FollowButtonArea(
            activity = activity,
            column = column,
            accessInfo = accessInfo,
            whoRef = whoRef,
            callbacks = callbacks,
            contentColor = contentColor,
        )
    }

    // Follow request accept/deny buttons
    if (column.type == ColumnType.FOLLOW_REQUESTS) {
        FollowRequestButtons(
            callbacks = callbacks,
            column = column,
            whoRef = whoRef,
            contentColor = contentColor,
        )
    }
}

/**
 * Follow/unfollow button with "followed by" indicator.
 */
@Composable
fun FollowButtonArea(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    whoRef: TootAccountRef,
    callbacks: TimelineCallbacks,
    contentColor: Int,
) {
    val who = whoRef.get()
    val relation = remember(accessInfo.db_id, who.id) {
        daoUserRelation.load(accessInfo.db_id, who.id)
    }

    // Follow button icon based on relation
    val followIconId = when {
        relation.getFollowing(who) -> R.drawable.ic_follow_cross
        relation.getRequested(who) -> R.drawable.ic_follow_wait
        else -> R.drawable.ic_follow_plus
    }

    IconButton(
        onClick = {
            callbacks.onFollowButtonClick(column, whoRef, whoRef)
        },
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            painter = painterResource(id = followIconId),
            contentDescription = stringResource(R.string.follow),
            tint = Color(contentColor),
        )
    }
}

/**
 * Follow request accept/deny button row.
 */
@Composable
fun FollowRequestButtons(
    callbacks: TimelineCallbacks,
    column: Column,
    whoRef: TootAccountRef,
    contentColor: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
    ) {
        IconButton(
            onClick = { callbacks.onFollowRequestAccept(column, whoRef) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = stringResource(R.string.follow_accept),
                tint = Color(contentColor),
            )
        }

        IconButton(
            onClick = { callbacks.onFollowRequestDeny(column, whoRef) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(R.string.follow_deny),
                tint = Color(contentColor),
            )
        }
    }
}
