@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package jp.juggler.subwaytooter.compose

import android.view.Gravity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.MisskeyAntenna
import jp.juggler.subwaytooter.api.entity.TimelineItem
import jp.juggler.subwaytooter.api.entity.TootConversationSummary
import jp.juggler.subwaytooter.api.entity.TootDomainBlock
import jp.juggler.subwaytooter.api.entity.TootFilter
import jp.juggler.subwaytooter.api.entity.TootGap
import jp.juggler.subwaytooter.api.entity.TootList
import jp.juggler.subwaytooter.api.entity.TootMessageHolder
import jp.juggler.subwaytooter.api.entity.TootNotification
import jp.juggler.subwaytooter.api.entity.NotificationType
import jp.juggler.subwaytooter.api.entity.TootScheduled
import jp.juggler.subwaytooter.api.entity.TootSearchGap
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.api.entity.TootTag
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.util.data.ellipsizeDot3

/**
 * Gap item - shows "read gap" with head/tail buttons.
 */
@Composable
fun GapItemContent(
    column: Column,
    item: TootGap,
    callbacks: TimelineCallbacks,
    contentColor: Int,
) {
    val contentColorCompose = Color(contentColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val showHead = column.type.gapDirection(column, true)
        val showTail = column.type.gapDirection(column, false)

        if (showHead) {
            IconButton(
                onClick = { callbacks.onGapHeadClick(column, item) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                    contentDescription = "Load newer",
                    tint = contentColorCompose,
                )
            }
        }

        Button(
            onClick = { callbacks.onTagClick(column, item) },
            colors = ButtonDefaults.textButtonColors(contentColor = contentColorCompose),
        ) {
            Text(
                text = stringResource(R.string.read_gap),
                color = contentColorCompose,
            )
        }

        if (showTail) {
            IconButton(
                onClick = { callbacks.onGapTailClick(column, item) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_drop_up),
                    contentDescription = "Load older",
                    tint = contentColorCompose,
                )
            }
        }
    }
}

/**
 * Search gap item.
 */
@Preview(showBackground = true)
@Composable
fun PreviewSearchGapItemContent() {
    SearchGapItemContent(
        item = TootSearchGap(TootSearchGap.SearchType.Hashtag),
        callbacks = TimelineCallbacks(),
        contentColor = android.graphics.Color.BLACK
    )
}

@Composable
fun SearchGapItemContent(
    item: TootSearchGap,
    callbacks: TimelineCallbacks,
    contentColor: Int,
) {
    val text = when (item.type) {
        TootSearchGap.SearchType.Hashtag -> stringResource(R.string.read_more_hashtag)
        TootSearchGap.SearchType.Account -> stringResource(R.string.read_more_account)
        TootSearchGap.SearchType.Status -> stringResource(R.string.read_more_status)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = { /* Search gap click handled in parent */ },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(contentColor)),
        ) {
            Text(text = text, color = Color(contentColor))
        }
    }
}

/**
 * Hashtag item - either trend tag with history or simple search tag.
 */
@Composable
fun TagItemContent(
    activity: ActMain,
    item: TootTag,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
) {
    val contentColorCompose = Color(contentColor)
    val acctColorCompose = Color(acctColor)

    if (item.history?.isNotEmpty() == true) {
        // Trend tag with history chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { callbacks.onTagClick(jp.juggler.subwaytooter.column.Column::class.java.cast(null)!!, item) },
                    onLongClick = { callbacks.onTagLongClick(jp.juggler.subwaytooter.column.Column::class.java.cast(null)!!, item) },
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val name = when (item.type) {
                    TootTag.TagType.Link -> item.url?.ellipsizeDot3(256) ?: ""
                    TootTag.TagType.Tag -> "#${item.name.ellipsizeDot3(256)}"
                }
                Text(
                    text = name,
                    color = contentColorCompose,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val desc = when (item.type) {
                    TootTag.TagType.Link -> "${item.name}\n${item.description ?: ""}"
                    TootTag.TagType.Tag -> buildString {
                        if (item.following == true) {
                            append(activity.getString(R.string.following))
                            append(" ")
                        }
                        append(
                            activity.getString(
                                R.string.people_talking,
                                item.accountDaily,
                                item.accountWeekly
                            )
                        )
                    }
                }
                Text(
                    text = desc,
                    color = acctColorCompose,
                    fontSize = 12.sp,
                )
            }

            Text(
                text = "${item.countDaily}(${item.countWeekly})",
                color = contentColorCompose,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            // Tag history chart would go here (TagHistoryView)
            // For now, a placeholder
            Spacer(modifier = Modifier.size(64.dp, 32.dp))
        }
    } else {
        // Simple search tag
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = { callbacks.onTagClick(jp.juggler.subwaytooter.column.Column::class.java.cast(null)!!, item) },
                colors = ButtonDefaults.textButtonColors(contentColor = contentColorCompose),
            ) {
                Text(text = "#${item.name}", color = contentColorCompose)
            }
        }
    }
}

/**
 * Domain block item.
 */
@Composable
fun DomainBlockItemContent(
    item: TootDomainBlock,
    callbacks: TimelineCallbacks,
    contentColor: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = { /* Domain block click */ },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(contentColor)),
        ) {
            Text(text = item.domain.pretty, color = Color(contentColor))
        }
    }
}

/**
 * List item (TootList / MisskeyAntenna).
 */
@Composable
fun ListItemContent(
    item: TootList,
    callbacks: TimelineCallbacks,
    contentColor: Int,
) {
    val contentColorCompose = Color(contentColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = { callbacks.onListClick(jp.juggler.subwaytooter.column.Column::class.java.cast(null)!!, item) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(contentColor = contentColorCompose),
        ) {
            Text(
                text = item.title ?: "",
                color = contentColorCompose,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = { callbacks.onListMoreClick(jp.juggler.subwaytooter.column.Column::class.java.cast(null)!!, item) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = stringResource(R.string.more),
                tint = contentColorCompose,
            )
        }
    }
}

/**
 * Misskey Antenna item.
 */
@Composable
fun AntennaItemContent(
    item: MisskeyAntenna,
    callbacks: TimelineCallbacks,
    contentColor: Int,
) {
    val contentColorCompose = Color(contentColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = { callbacks.onListClick(jp.juggler.subwaytooter.column.Column::class.java.cast(null)!!, item) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(contentColor = contentColorCompose),
        ) {
            Text(
                text = item.name,
                color = contentColorCompose,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = { callbacks.onListMoreClick(jp.juggler.subwaytooter.column.Column::class.java.cast(null)!!, item) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = stringResource(R.string.more),
                tint = contentColorCompose,
            )
        }
    }
}

/**
 * Filter item.
 */
@Composable
fun FilterItemContent(
    activity: ActMain,
    item: TootFilter,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
) {
    val contentColorCompose = Color(contentColor)
    val acctColorCompose = Color(acctColor)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .clickable { callbacks.onFilterClick(jp.juggler.subwaytooter.column.Column::class.java.cast(null)!!, item) }
            .padding(8.dp),
    ) {
        Text(
            text = item.displayString,
            color = contentColorCompose,
            fontWeight = FontWeight.Bold,
        )

        val detail = buildString {
            val contextNames =
                item.contextNames.joinToString("/") { activity.getString(it) }
            append(activity.getString(R.string.filter_context))
            append(": ")
            append(contextNames)

            val action = when (item.hide) {
                true -> activity.getString(R.string.filter_action_hide)
                else -> activity.getString(R.string.filter_action_warn)
            }
            append('\n')
            append(activity.getString(R.string.filter_action))
            append(": ")
            append(action)

            if (item.time_expires_at > 0L) {
                append('\n')
                append(activity.getString(R.string.filter_expires_at))
                append(": ")
                append(TootStatus.formatTime(activity, item.time_expires_at, false))
            }
        }

        Text(
            text = detail,
            color = acctColorCompose,
            fontSize = 12.sp,
        )
    }
}

/**
 * Simple message holder.
 */
@Preview(showBackground = true)
@Composable
fun PreviewMessageHolderContentItem() {
    MessageHolderContent(
        item = TootMessageHolder(text = "This is a simple message holder"),
        contentColor = android.graphics.Color.BLACK
    )
}

@Composable
fun MessageHolderContent(
    item: TootMessageHolder,
    contentColor: Int,
) {
    MessageHolderContent(
        text = item.text,
        gravity = item.gravity,
        contentColor = contentColor,
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageHolderContent() {
    MessageHolderContent(
        text = "This is a simple message",
        gravity = Gravity.CENTER_HORIZONTAL,
        contentColor = android.graphics.Color.BLACK
    )
}

@Composable
fun MessageHolderContent(
    text: CharSequence,
    gravity: Int = Gravity.CENTER_HORIZONTAL,
    contentColor: Int,
) {
    val alignment = when (gravity) {
        Gravity.START, Gravity.LEFT -> Alignment.Start
        Gravity.END, Gravity.RIGHT -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val textAlign = when (gravity) {
        Gravity.START, Gravity.LEFT -> TextAlign.Start
        Gravity.END, Gravity.RIGHT -> TextAlign.End
        else -> TextAlign.Center
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = alignment,
    ) {
        Text(
            text = text.toString(),
            color = Color(contentColor),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Conversation summary item - renders the last status + conversation icons.
 */
@Composable
fun ConversationSummaryItemContent(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    item: TootConversationSummary,
    bSimpleList: Boolean,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
) {
    // Render the last status
    StatusWithReplyContent(
        activity = activity,
        column = column,
        accessInfo = accessInfo,
        status = item.last_status,
        bSimpleList = bSimpleList,
        callbacks = callbacks,
        contentColor = contentColor,
        acctColor = acctColor,
    )

    // Conversation participant icons
    val lastAccountId = item.last_status.account.id
    val accountsOther = item.accounts.filter { it.get().id != lastAccountId }

    if (accountsOther.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable { callbacks.onConversationIconsClick(column, item) }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val label = if (accountsOther.size <= 1) {
                stringResource(R.string.conversation_to)
            } else {
                stringResource(R.string.participants)
            }
            Text(
                text = label,
                color = Color(contentColor),
                modifier = Modifier.padding(end = 4.dp),
            )

            for ((idx, whoRef) in accountsOther.take(4).withIndex()) {
                val who = whoRef.get()
                NetworkImage(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 3.dp),
                    staticUrl = accessInfo.supplyBaseUrl(who.avatar_static),
                    animatedUrl = accessInfo.supplyBaseUrl(who.avatar),
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP,
                )
            }

            if (accountsOther.size > 4) {
                Text(
                    text = stringResource(R.string.participants_and_more),
                    color = Color(contentColor),
                )
            }
        }
    }

    // "Show conversation" button if this is a reply
    if (item.last_status.in_reply_to_id != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = { callbacks.onTagClick(column, item) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(contentColor)),
            ) {
                Text(
                    text = stringResource(R.string.show_conversation),
                    color = Color(contentColor),
                )
            }
        }
    }
}

/**
 * Scheduled status item.
 */
@Composable
fun ScheduledItemContent(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    item: TootScheduled,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
) {
    val contentColorCompose = Color(contentColor)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        // Time
        Text(
            text = TootStatus.formatTime(activity, item.timeScheduledAt, true),
            color = Color(acctColor),
            fontSize = 12.sp,
        )

        // Content text
        item.text?.let { text ->
            Text(
                text = text,
                color = contentColorCompose,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Spoiler text
        item.spoilerText?.takeIf { it.isNotEmpty() }?.let { spoiler ->
            Text(
                text = spoiler,
                color = contentColorCompose,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Scheduled label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = { callbacks.onTagClick(column, item) },
                colors = ButtonDefaults.textButtonColors(contentColor = contentColorCompose),
            ) {
                Text(
                    text = activity.getString(R.string.scheduled_status) + " " +
                            TootStatus.formatTime(activity, item.timeScheduledAt, true),
                    color = contentColorCompose,
                )
            }
        }
    }
}

/**
 * Notification item - delegates to the appropriate composable based on notification type.
 * For now, shows boost/reply/mention header + status body.
 */
@Composable
fun NotificationItemContent(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    notification: TootNotification,
    bSimpleList: Boolean,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
) {
    val whoRef = notification.accountRef
    val status = notification.status

    // Notification header (who did what)
    if (whoRef != null) {
        val (notifIconId, notifStringId) = notificationTypeIconAndString(notification)

        BoostHeader(
            activity = activity,
            accessInfo = accessInfo,
            whoRef = whoRef,
            time = notification.time_created_at,
            iconId = notifIconId,
            stringId = notifStringId,
            callbacks = callbacks,
            item = notification,
            contentColor = contentColor,
            acctColor = acctColor,
        )
    }

    // Status body (if the notification has an associated status)
    if (status != null) {
        StatusBody(
            activity = activity,
            column = column,
            accessInfo = accessInfo,
            status = status,
            bSimpleList = bSimpleList,
            callbacks = callbacks,
            contentColor = contentColor,
            acctColor = acctColor,
        )
    } else if (whoRef != null) {
        // For follow notifications without status, show account info
        AccountItemContent(
            activity = activity,
            column = column,
            accessInfo = accessInfo,
            whoRef = whoRef,
            callbacks = callbacks,
            contentColor = contentColor,
            acctColor = acctColor,
        )
    }
}

/**
 * Maps NotificationType to icon drawable and string resource for header display.
 */
private fun notificationTypeIconAndString(n: TootNotification): Pair<Int, Int> {
    return when (n.type) {
        NotificationType.Favourite ->
            R.drawable.ic_star_outline to R.string.display_name_favourited_by

        NotificationType.Reblog ->
            R.drawable.ic_repeat to R.string.display_name_boosted_by

        NotificationType.Renote ->
            R.drawable.ic_repeat to R.string.display_name_boosted_by

        NotificationType.Follow ->
            R.drawable.ic_person_add to R.string.display_name_followed_by

        NotificationType.Unfollow ->
            R.drawable.ic_follow_cross to R.string.display_name_unfollowed_by

        NotificationType.AdminSignup ->
            R.drawable.ic_add to R.string.display_name_signed_up

        NotificationType.AdminReport ->
            R.drawable.ic_follow_wait to R.string.display_name_report

        NotificationType.Mention, NotificationType.Reply ->
            R.drawable.ic_reply to R.string.display_name_replied_by

        NotificationType.EmojiReactionPleroma,
        NotificationType.EmojiReactionFedibird,
        NotificationType.Reaction,
        ->
            R.drawable.ic_add to R.string.display_name_reaction_by

        NotificationType.Quote ->
            R.drawable.ic_quote to R.string.display_name_quoted_by

        NotificationType.Status ->
            R.drawable.ic_edit to R.string.display_name_posted_by

        NotificationType.Update ->
            R.drawable.ic_edit to R.string.display_name_posted_by

        NotificationType.StatusReference ->
            R.drawable.ic_edit to R.string.display_name_posted_by

        NotificationType.FollowRequest,
        NotificationType.FollowRequestMisskey,
        ->
            R.drawable.ic_follow_wait to R.string.display_name_follow_request_by

        NotificationType.FollowRequestAcceptedMisskey ->
            R.drawable.ic_person_add to R.string.display_name_follow_request_accepted_by

        NotificationType.Vote,
        NotificationType.PollVoteMisskey,
        ->
            R.drawable.ic_vote to R.string.display_name_voted_by

        NotificationType.Poll ->
            R.drawable.ic_vote to R.string.display_name_voted_by

        NotificationType.ScheduledStatus ->
            R.drawable.ic_timer to R.string.display_name_posted_by

        NotificationType.SeveredRelationships ->
            R.drawable.ic_follow_cross to R.string.display_name_unfollowed_by

        is NotificationType.Unknown ->
            R.drawable.ic_question to R.string.display_name_posted_by
    }
}
