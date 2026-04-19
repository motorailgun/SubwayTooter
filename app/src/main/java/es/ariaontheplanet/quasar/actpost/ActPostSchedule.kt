package es.ariaontheplanet.quasar.actpost

import es.ariaontheplanet.quasar.ActPost
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.TootParser
import es.ariaontheplanet.quasar.api.entity.TootAttachment
import es.ariaontheplanet.quasar.api.entity.TootScheduled
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.api.entity.parseItem
import es.ariaontheplanet.quasar.dialog.DlgDateTime
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.util.PostAttachment
import jp.juggler.util.data.cast
import jp.juggler.util.data.decodeJsonObject
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory

private val log = LogCategory("ActPostSchedule")

fun ActPost.showSchedule() {
    scheduleText = when (states.timeSchedule) {
        0L -> getString(R.string.unspecified)
        else -> TootStatus.formatTime(this, states.timeSchedule, true)
    }
}

fun ActPost.performSchedule() {
    DlgDateTime(this).open(states.timeSchedule) { t ->
        states.timeSchedule = t
        showSchedule()
    }
}

fun ActPost.resetSchedule() {
    states.timeSchedule = 0L
    showSchedule()
}

suspend fun ActPost.initializeFromScheduledStatus(account: SavedAccount, jsonText: String) {
    try {
        val item = parseItem(jsonText.decodeJsonObject()) {
            val parser = TootParser(this, account)
            TootScheduled(parser, it)
        } ?: error("initializeFromScheduledStatus: parse failed.")

        scheduledStatus = item

        states.timeSchedule = item.timeScheduledAt
        states.visibility = item.visibility
        nsfwChecked = item.sensitive

        views.etContent.setText(item.text ?: "")

        val cw = item.spoilerText
        views.etContentWarning.setText(cw ?: "")
        contentWarningChecked = cw?.isNotEmpty() == true

        // 2019/1/7 どうも添付データを古い投稿から引き継げないようだ…。
        // 2019/1/22 https://github.com/tootsuite/mastodon/pull/9894 で直った。
        item.mediaAttachments
            ?.mapNotNull { src ->
                src.cast<TootAttachment>()
                    ?.apply { redraft = true }
                    ?.let { PostAttachment(it) }
            }
            ?.notEmpty()
            ?.let {
                this.attachmentList.clear()
                this.attachmentList.addAll(it)
                saveAttachmentList()
            }
    } catch (ex: Throwable) {
        log.e(ex, "initializeFromScheduledStatus failed.")
    }
}
