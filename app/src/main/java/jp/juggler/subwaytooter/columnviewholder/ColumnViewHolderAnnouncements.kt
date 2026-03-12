package jp.juggler.subwaytooter.columnviewholder

import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableStringBuilder
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootAnnouncement
import jp.juggler.subwaytooter.api.entity.TootReaction
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.api.runApiTask
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.subwaytooter.compose.AnnouncementReactionItem
import jp.juggler.subwaytooter.dialog.launchEmojiPicker
import jp.juggler.subwaytooter.emoji.CustomEmoji
import jp.juggler.subwaytooter.emoji.UnicodeEmoji
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.span.NetworkEmojiSpan
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.util.EmojiDecoder
import jp.juggler.subwaytooter.util.emojiSizeMode
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.buildJsonObject
import jp.juggler.util.data.encodePercent
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.showToast
import jp.juggler.util.network.toDeleteRequestBuilder
import jp.juggler.util.network.toPutRequestBuilder

fun ColumnViewHolder.hideAnnouncements() {
    val column = column ?: return

    if (column.announcementHideTime <= 0L) {
        column.announcementHideTime = System.currentTimeMillis()
    }

    activity.appState.saveColumnList()
    showAnnouncements()
}

fun ColumnViewHolder.toggleAnnouncements() {
    val column = column ?: return

    if (columnUiState.announcementsBoxVisible) {
        if (column.announcementHideTime <= 0L) {
            column.announcementHideTime = System.currentTimeMillis()
        }
    } else {
        showColumnSetting(false)
        column.announcementHideTime = 0L
    }
    activity.appState.saveColumnList()
    showAnnouncements()
}

fun ColumnViewHolder.showAnnouncements(force: Boolean = true) {
    val column = column ?: return

    if (!force && lastAnnouncementShown >= column.announcementUpdated) {
        return
    }
    lastAnnouncementShown = SystemClock.elapsedRealtime()
    clearExtras()

    val ui = columnUiState

    val listShown = TootAnnouncement.filterShown(column.announcements)
    if (listShown?.isEmpty() != false) {
        showAnnouncementsEmpty()
        return
    }

    ui.announcementButtonVisible = true

    val expand = column.announcementHideTime <= 0L
    ui.announcementsBoxVisible = expand

    ui.announcementBadgeVisible = false
    if (!expand) {
        val newer = listShown.find { it.updated_at > column.announcementHideTime }
        if (newer != null) {
            column.announcementId = newer.id
            ui.announcementBadgeVisible = true
        }
        return
    }

    val item = listShown.find { it.id == column.announcementId }
        ?: listShown[0]

    val itemIndex = listShown.indexOf(item)
    val enablePaging = listShown.size > 1
    val contentColor = column.getContentColor()

    ui.announcementEnablePaging = enablePaging
    ui.announcementContentColor = contentColor
    ui.announcementsCaption = activity.getString(R.string.announcements)
    ui.announcementsIndex =
        activity.getString(R.string.announcements_index, itemIndex + 1, listShown.size)

    showAnnouncementContent(item)
    showReactionBox(column, item)
}

private fun ColumnViewHolder.clearExtras() {
    for (invalidator in extraInvalidatorList) {
        invalidator.register(null)
    }
    extraInvalidatorList.clear()
}

private fun ColumnViewHolder.showAnnouncementsEmpty() {
    val ui = columnUiState
    ui.announcementButtonVisible = false
    ui.announcementsBoxVisible = false
    ui.announcementBadgeVisible = false
}

private fun ColumnViewHolder.showAnnouncementContent(item: TootAnnouncement) {
    val ui = columnUiState

    var periods: StringBuilder? = null
    fun String.appendPeriod() {
        val sb = periods
        if (sb == null) {
            periods = StringBuilder(this)
        } else {
            sb.append("\n")
            sb.append(this)
        }
    }

    val (strStart, strEnd) = TootStatus.formatTimeRange(item.starts_at, item.ends_at, item.all_day)

    when {
        strStart == "" && strEnd == "" -> Unit
        strStart == strEnd ->
            activity.getString(R.string.announcements_period1, strStart).appendPeriod()

        else ->
            activity.getString(R.string.announcements_period2, strStart, strEnd).appendPeriod()
    }

    if (item.updated_at > item.published_at) {
        val strUpdateAt = TootStatus.formatTime(activity, item.updated_at, false)
        activity.getString(R.string.edited_at, strUpdateAt).appendPeriod()
    }

    ui.announcementPeriod = periods?.toString()
    ui.announcementContent = item.decoded_content
}

private fun ColumnViewHolder.showReactionBox(
    column: Column,
    item: TootAnnouncement,
) {
    val ui = columnUiState
    ui.announcementReactions.clear()

    val disableEmojiAnimation = PrefB.bpDisableEmojiAnimation.value

    val options = DecodeOptions(
        activity,
        column.accessInfo,
        decodeEmoji = true,
        enlargeEmoji = 1.5f,
        authorDomain = column.accessInfo,
        emojiSizeMode = column.accessInfo.emojiSizeMode(),
    )

    val reactions = item.reactions?.filter { it.count > 0L } ?: emptyList()

    for (reaction in reactions) {
        val url = if (disableEmojiAnimation) {
            reaction.staticUrl.notEmpty() ?: reaction.url.notEmpty()
        } else {
            reaction.url.notEmpty() ?: reaction.staticUrl.notEmpty()
        }

        val displayText: CharSequence = if (url == null) {
            EmojiDecoder.decodeEmoji(options, "${reaction.name} ${reaction.count}")
        } else {
            SpannableStringBuilder("${reaction.name} ${reaction.count}").also { sb ->
                sb.setSpan(
                    NetworkEmojiSpan(
                        url,
                        scale = 1.5f,
                        sizeMode = options.emojiSizeMode,
                        initialAspect = reaction.aspect,
                    ),
                    0,
                    reaction.name.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        ui.announcementReactions.add(
            AnnouncementReactionItem(
                reaction = reaction,
                displayText = displayText,
                isMe = reaction.me,
                url = url,
            )
        )
    }
}

fun ColumnViewHolder.reactionAdd(item: TootAnnouncement, sample: TootReaction?) {
    val column = column ?: return

    if (sample == null) {
        launchEmojiPicker(activity, column.accessInfo, closeOnSelected = true) { emoji, _ ->
            val code = when (emoji) {
                is UnicodeEmoji -> emoji.unifiedCode
                is CustomEmoji -> emoji.shortcode
            }
            ColumnViewHolder.log.d("addReaction: $code ${emoji.javaClass.simpleName}")
            reactionAdd(item, TootReaction.parseFedibird(buildJsonObject {
                put("name", code)
                put("count", 1)
                put("me", true)
                if (emoji is CustomEmoji) {
                    putNotNull("url", emoji.url)
                    putNotNull("static_url", emoji.staticUrl)
                }
            }))
        }
        return
    }
    activity.launchAndShowError {
        activity.runApiTask(column.accessInfo) { client ->
            client.request(
                "/api/v1/announcements/${item.id}/reactions/${sample.name.encodePercent()}",
                jp.juggler.util.data.JsonObject().toPutRequestBuilder()
            )
        }?.let { result ->
            when (result.jsonObject) {
                null -> activity.showToast(true, result.error)
                else -> {
                    sample.count = 0
                    val list = item.reactions
                    if (list == null) {
                        item.reactions = mutableListOf(sample)
                    } else {
                        val reaction = list.find { it.name == sample.name }
                        if (reaction == null) {
                            list.add(sample)
                        } else {
                            reaction.me = true
                            ++reaction.count
                        }
                    }
                    column.announcementUpdated = SystemClock.elapsedRealtime()
                    showAnnouncements()
                }
            }
        }
    }
}

fun ColumnViewHolder.reactionRemove(item: TootAnnouncement, name: String) {
    val column = column ?: return
    launchMain {
        activity.runApiTask(column.accessInfo) { client ->
            client.request(
                "/api/v1/announcements/${item.id}/reactions/${name.encodePercent()}",
                jp.juggler.util.data.JsonObject().toDeleteRequestBuilder()
            )
        }?.let { result ->
            when (result.jsonObject) {
                null -> activity.showToast(true, result.error)
                else -> item.reactions?.iterator()?.let {
                    while (it.hasNext()) {
                        val reaction = it.next()
                        if (reaction.name == name) {
                            reaction.me = false
                            if (--reaction.count <= 0) it.remove()
                            break
                        }
                    }
                    column.announcementUpdated = SystemClock.elapsedRealtime()
                    showAnnouncements()
                }
            }
        }
    }
}
