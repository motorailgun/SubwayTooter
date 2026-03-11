@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package jp.juggler.subwaytooter.compose

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootAccountRef
import jp.juggler.subwaytooter.api.entity.TootAggBoost
import jp.juggler.subwaytooter.api.entity.TootAttachmentLike
import jp.juggler.subwaytooter.api.entity.TootNotification
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.api.entity.TootVisibility
import jp.juggler.subwaytooter.calcIconRound
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.stylerBoostAlpha
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.table.daoContentWarning
import jp.juggler.subwaytooter.table.daoMediaShown
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.util.ui.getSpannedString
import jp.juggler.util.data.notEmpty
import jp.juggler.util.data.notZero

/**
 * Composable that renders a TootStatus item in the timeline.
 */
@Composable
fun StatusItemContent(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    status: TootStatus,
    bSimpleList: Boolean,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
) {
    val reblog = status.reblog

    when {
        reblog == null -> {
            // Normal status, maybe with reply
            StatusWithReplyContent(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                status = status,
                bSimpleList = bSimpleList,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )
        }

        status.isQuoteToot -> {
            // Quote toot - show reply-to info then the quoting status
            ReplyHeader(
                activity = activity,
                accessInfo = accessInfo,
                replyer = status.account,
                target = reblog.accountRef,
                iconId = R.drawable.ic_quote,
                text = activity.getSpannedString(
                    R.string.quote_to,
                    reblog.accountRef.decoded_display_name,
                ),
                callbacks = callbacks,
                item = status,
                statusReply = reblog,
                contentColor = contentColor,
            )
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
        }

        else -> {
            // Boost without quote - show boost header then the reblogged status
            BoostHeader(
                activity = activity,
                accessInfo = accessInfo,
                whoRef = status.accountRef,
                time = status.time_created_at,
                iconId = R.drawable.ic_repeat,
                stringId = R.string.display_name_boosted_by,
                callbacks = callbacks,
                item = status,
                contentColor = contentColor,
                acctColor = acctColor,
            )
            StatusWithReplyContent(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                status = reblog,
                bSimpleList = bSimpleList,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )
        }
    }
}

/**
 * Renders an aggregated boost item.
 */
@Composable
fun AggBoostItemContent(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    item: TootAggBoost,
    bSimpleList: Boolean,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
) {
    val headBoost = item.boosterStatuses.first()
    val boosterCount = item.boosterStatuses.size

    if (boosterCount == 1) {
        BoostHeader(
            activity = activity,
            accessInfo = accessInfo,
            whoRef = headBoost.accountRef,
            time = headBoost.time_created_at,
            iconId = R.drawable.ic_repeat,
            stringId = R.string.display_name_boosted_by,
            callbacks = callbacks,
            item = item,
            contentColor = contentColor,
            acctColor = acctColor,
        )
    } else {
        BoostHeader(
            activity = activity,
            accessInfo = accessInfo,
            whoRef = headBoost.accountRef,
            time = headBoost.time_created_at,
            iconId = R.drawable.ic_repeat,
            stringId = R.string.display_name_boosted_by_agg,
            callbacks = callbacks,
            item = item,
            contentColor = contentColor,
            acctColor = acctColor,
            extraFormatArg = "%d".format(boosterCount - 1),
        )
    }

    StatusWithReplyContent(
        activity = activity,
        column = column,
        accessInfo = accessInfo,
        status = item.originalStatus,
        bSimpleList = bSimpleList,
        callbacks = callbacks,
        contentColor = contentColor,
        acctColor = acctColor,
    )
}

/**
 * A status that may have reply info above it.
 */
@Composable
fun StatusWithReplyContent(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    status: TootStatus,
    bSimpleList: Boolean,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
    colorBg: Int = 0,
) {
    // Show reply header if applicable
    val reply = status.reply
    val inReplyToId = status.in_reply_to_id
    val inReplyToAccountId = status.in_reply_to_account_id

    if (reply != null) {
        ReplyHeader(
            activity = activity,
            accessInfo = accessInfo,
            replyer = status.account,
            target = reply.accountRef,
            iconId = R.drawable.ic_reply,
            text = activity.getSpannedString(
                R.string.reply_to,
                reply.accountRef.decoded_display_name,
            ),
            callbacks = callbacks,
            item = status,
            statusReply = reply,
            contentColor = contentColor,
        )
    }

    StatusBody(
        activity = activity,
        column = column,
        accessInfo = accessInfo,
        status = status,
        bSimpleList = bSimpleList,
        callbacks = callbacks,
        contentColor = contentColor,
        acctColor = acctColor,
        colorBg = colorBg,
    )
}

/**
 * The boost header row.
 */
@Composable
fun BoostHeader(
    activity: ActMain,
    accessInfo: SavedAccount,
    whoRef: TootAccountRef,
    time: Long,
    @DrawableRes iconId: Int,
    @StringRes stringId: Int,
    callbacks: TimelineCallbacks,
    item: Any?,
    contentColor: Int,
    acctColor: Int,
    extraFormatArg: String? = null,
) {
    val who = whoRef.get()
    val contentColorCompose = Color(contentColor)
    val acctColorCompose = Color(acctColor)
    val displayName = whoRef.decoded_display_name

    val text = if (extraFormatArg != null) {
        activity.getSpannedString(stringId, displayName, extraFormatArg)
    } else {
        activity.getSpannedString(stringId, displayName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { callbacks.onBoostHeaderClick(item as? Column ?: return@combinedClickable, item as? jp.juggler.subwaytooter.api.entity.TimelineItem, whoRef) },
                onLongClick = { callbacks.onBoostHeaderLongClick(item as? Column ?: return@combinedClickable, item as? jp.juggler.subwaytooter.api.entity.TimelineItem, whoRef) },
            )
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Boost icon
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .alpha(stylerBoostAlpha),
            tint = contentColorCompose,
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Boost avatar
        NetworkImage(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp)),
            staticUrl = accessInfo.supplyBaseUrl(who.avatar_static),
            animatedUrl = accessInfo.supplyBaseUrl(who.avatar),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Boost text and acct/time
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Acct
                AcctText(
                    accessInfo = accessInfo,
                    who = who,
                    acctColor = acctColor,
                    modifier = Modifier.weight(1f),
                )
                // Time
                Text(
                    text = TootStatus.formatTime(activity, time, true),
                    color = acctColorCompose,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }

            SpannableTextView(
                text = text,
                textColor = contentColor,
                handler = activity.handler,
            )
        }
    }
}

/**
 * The reply header row.
 */
@Composable
fun ReplyHeader(
    activity: ActMain,
    accessInfo: SavedAccount,
    replyer: jp.juggler.subwaytooter.api.entity.TootAccount?,
    target: TootAccountRef?,
    @DrawableRes iconId: Int,
    text: CharSequence,
    callbacks: TimelineCallbacks,
    item: Any?,
    statusReply: TootStatus? = null,
    contentColor: Int,
) {
    val contentColorCompose = Color(contentColor)
    val targetAccount = target?.get()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Reply header click
            }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .alpha(stylerBoostAlpha),
            tint = contentColorCompose,
        )

        // Reply target avatar (if different from replyer)
        if (targetAccount != null && targetAccount.avatar != replyer?.avatar) {
            Spacer(modifier = Modifier.width(4.dp))
            NetworkImage(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp)),
                staticUrl = accessInfo.supplyBaseUrl(targetAccount.avatar_static),
                animatedUrl = accessInfo.supplyBaseUrl(targetAccount.avatar),
                contentDescription = null,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        SpannableTextView(
            text = text,
            textColor = contentColor,
            modifier = Modifier.weight(1f),
            handler = activity.handler,
        )
    }
}

/**
 * The main status body: avatar, name, acct, time, content, media, buttons.
 */
@Composable
fun StatusBody(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    status: TootStatus,
    bSimpleList: Boolean,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    acctColor: Int,
    colorBg: Int = 0,
    fadeText: Boolean = false,
) {
    val contentColorCompose = Color(contentColor)
    val acctColorCompose = Color(acctColor)
    val who = status.accountRef.get()
    val whoRef = status.accountRef
    val context = LocalContext.current

    // Check if filtered
    val filteredWord = status.filteredWord
    if (filteredWord != null) {
        val filterText = buildString {
            append(activity.getString(R.string.filtered))
            if (PrefB.bpShowFilteredWord.value) append(" / $filteredWord")
            if (PrefB.bpShowUsernameFilteredPost.value) {
                val s = status.reblog ?: status
                append(" / ${s.account.display_name} @${s.account.acct}")
            }
        }
        MessageHolderContent(
            text = filterText,
            gravity = Gravity.CENTER_HORIZONTAL,
            contentColor = contentColor,
        )
        return
    }

    // Background color
    val bgColor = colorBg.notZero()
        ?: status.getBackgroundColorType(accessInfo).let { vis ->
            when (vis) {
                TootVisibility.UnlistedHome -> jp.juggler.subwaytooter.itemviewholder.ItemViewHolder.toot_color_unlisted
                TootVisibility.PrivateFollowers -> jp.juggler.subwaytooter.itemviewholder.ItemViewHolder.toot_color_follower
                TootVisibility.DirectSpecified -> jp.juggler.subwaytooter.itemviewholder.ItemViewHolder.toot_color_direct_user
                TootVisibility.DirectPrivate -> jp.juggler.subwaytooter.itemviewholder.ItemViewHolder.toot_color_direct_me
                TootVisibility.Limited -> jp.juggler.subwaytooter.itemviewholder.ItemViewHolder.toot_color_follower
                else -> 0
            }
        }.notZero() ?: 0

    val bgModifier = if (bgColor != 0) {
        Modifier.background(Color(bgColor))
    } else {
        Modifier
    }

    Column(
        modifier = bgModifier.fillMaxWidth(),
    ) {
        // Acct and Time row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AcctText(
                accessInfo = accessInfo,
                who = who,
                acctColor = acctColor,
                modifier = Modifier.weight(1f),
            )

            StatusTimeText(
                activity = activity,
                status = status,
                accessInfo = accessInfo,
                acctColor = acctColor,
            )
        }

        // Avatar + Name + Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            // Avatar
            NetworkImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        callbacks.onAvatarClick(column, status, whoRef)
                    }
                    .padding(top = 4.dp, end = 4.dp),
                staticUrl = accessInfo.supplyBaseUrl(who.avatar_static),
                animatedUrl = accessInfo.supplyBaseUrl(who.avatar),
                contentDescription = context.getString(R.string.thumbnail),
            )

            // Name + Content column
            Column(modifier = Modifier.weight(1f)) {
                // Display name
                SpannableTextView(
                    text = whoRef.decoded_display_name,
                    textColor = contentColor,
                    textSizeSp = ActMain.timelineFontSizeSp.takeIf { it.isFinite() }
                        ?: Float.NaN,
                    typeface = ActMain.timelineFontBold,
                    handler = activity.handler,
                )

                // Content Warning
                val decodedSpoilerText = status.decoded_spoiler_text
                val autoCw = status.auto_cw
                val autoCwText = autoCw?.decodedSpoilerText

                var cwShown by remember(status) {
                    mutableStateOf(
                        when {
                            decodedSpoilerText.isNotEmpty() ->
                                daoContentWarning.isShown(status, accessInfo.expandCw)
                            autoCwText != null ->
                                daoContentWarning.isShown(status, accessInfo.expandCw)
                            else -> true // No CW
                        }
                    )
                }

                val hasCw = decodedSpoilerText.isNotEmpty() || autoCwText != null

                if (hasCw) {
                    ContentWarningRow(
                        cwText = if (decodedSpoilerText.isNotEmpty()) decodedSpoilerText else autoCwText!!,
                        isShown = cwShown,
                        contentColor = contentColor,
                        handler = activity.handler,
                        onToggle = {
                            cwShown = !cwShown
                            callbacks.onContentWarningToggle(status)
                        },
                    )
                }

                // Main content (visible if no CW or CW shown)
                if (!hasCw || cwShown) {
                    val fadeAlpha = if (fadeText) ActMain.eventFadeAlpha else 1f

                    // Mentions
                    status.decoded_mentions?.let { mentions ->
                        if (mentions.isNotEmpty()) {
                            SpannableTextView(
                                text = mentions,
                                textColor = contentColor,
                                textSizeSp = ActMain.timelineFontSizeSp.takeIf { it.isFinite() }
                                    ?: Float.NaN,
                                modifier = Modifier.alpha(fadeAlpha),
                                movementMethod = true,
                                handler = activity.handler,
                            )
                        }
                    }

                    // Main text content
                    SpannableTextView(
                        text = status.decoded_content,
                        textColor = contentColor,
                        textSizeSp = ActMain.timelineFontSizeSp.takeIf { it.isFinite() }
                            ?: Float.NaN,
                        lineSpacingMultiplier = 1.1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(fadeAlpha),
                        movementMethod = true,
                        handler = activity.handler,
                    )

                    // Media attachments
                    status.media_attachments?.let { media ->
                        if (media.isNotEmpty()) {
                            MediaAttachments(
                                activity = activity,
                                accessInfo = accessInfo,
                                status = status,
                                column = column,
                                media = media,
                                callbacks = callbacks,
                                contentColor = contentColor,
                            )
                        }
                    }
                }

                // Status action buttons (if not simple list)
                if (!bSimpleList) {
                    StatusActionButtons(
                        activity = activity,
                        column = column,
                        status = status,
                        callbacks = callbacks,
                        contentColor = contentColor,
                    )
                }

                // Application name
                val application = status.application
                if (application != null && PrefB.bpShowAppName.value) {
                    Text(
                        text = activity.getString(R.string.application_is, application.name ?: ""),
                        color = contentColorCompose,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * Content warning row with toggle button.
 */
@Composable
fun ContentWarningRow(
    cwText: CharSequence,
    isShown: Boolean,
    contentColor: Int,
    handler: android.os.Handler,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(
                    id = if (isShown) R.drawable.outline_compress_24
                    else R.drawable.outline_expand_24
                ),
                contentDescription = stringResource(
                    if (isShown) R.string.hide else R.string.show
                ),
                tint = Color(contentColor),
            )
        }

        SpannableTextView(
            text = cwText,
            textColor = contentColor,
            modifier = Modifier.weight(1f),
            movementMethod = true,
            handler = handler,
        )
    }
}

/**
 * Media attachments grid/list.
 */
@Composable
fun MediaAttachments(
    activity: ActMain,
    accessInfo: SavedAccount,
    status: TootStatus,
    column: Column,
    media: ArrayList<TootAttachmentLike>,
    callbacks: TimelineCallbacks,
    contentColor: Int,
) {
    val defaultShown = when {
        column.hideMediaDefault -> false
        accessInfo.dontHideNsfw -> true
        else -> !status.sensitive
    }
    var isShown by remember(status) {
        mutableStateOf(daoMediaShown.isShown(status, defaultShown))
    }

    if (!isShown) {
        // "Tap to show" button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 3.dp)
                .clickable {
                    isShown = true
                    callbacks.onShowMedia(status)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.tap_to_show),
                color = Color(contentColor),
            )
        }
    } else {
        // Media thumbnails
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
        ) {
            // Hide media button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = {
                        isShown = false
                        callbacks.onHideMedia(status)
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.hide),
                        tint = Color(contentColor).copy(alpha = stylerBoostAlpha),
                    )
                }
            }

            // Thumbnail grid (2 columns)
            val chunkedMedia = media.take(4).chunked(2)
            for (row in chunkedMedia) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for ((idx, attachment) in row.withIndex()) {
                        val globalIdx = chunkedMedia.indexOf(row) * 2 + idx
                        NetworkImage(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    callbacks.onMediaClick(status, globalIdx)
                                },
                            staticUrl = accessInfo.supplyBaseUrl(attachment.urlForThumbnail()),
                            animatedUrl = accessInfo.supplyBaseUrl(attachment.urlForThumbnail()),
                        )
                    }
                    // Pad with empty space if only 1 item in row
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Media count indicator if >4
            if (media.size > 4) {
                Text(
                    text = activity.getString(R.string.media_count, media.size),
                    color = Color(contentColor),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * Status time display with badges.
 */
@Composable
fun StatusTimeText(
    activity: ActMain,
    status: TootStatus,
    accessInfo: SavedAccount,
    acctColor: Int,
) {
    // For now, use a simple text. The full badge rendering uses SpannableStringBuilder
    // with icon spans, which we bridge via AndroidView for full fidelity.
    val timeText = remember(status.time_created_at) {
        TootStatus.formatTime(activity, status.time_created_at, true)
    }
    Text(
        text = timeText,
        color = Color(acctColor),
        fontSize = 12.sp,
        maxLines = 1,
    )
}

/**
 * Account acct text with nickname/color support.
 */
@Composable
fun AcctText(
    accessInfo: SavedAccount,
    who: jp.juggler.subwaytooter.api.entity.TootAccount,
    acctColor: Int,
    modifier: Modifier = Modifier,
) {
    val ac = remember(accessInfo, who) { daoAcctColor.load(accessInfo, who) }
    val displayAcct = remember(ac) {
        when {
            daoAcctColor.hasNickname(ac) -> ac.nickname
            PrefB.bpShortAcctLocalUser.value -> "@${who.acct.pretty}"
            else -> "@${ac.nickname}"
        }
    }
    val fgColor = ac.colorFg.notZero() ?: acctColor
    val bgColorInt = ac.colorBg

    Text(
        text = displayAcct,
        color = Color(fgColor),
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .then(
                if (bgColorInt != 0) Modifier.background(Color(bgColorInt))
                else Modifier
            )
            .padding(horizontal = 4.dp),
    )
}
