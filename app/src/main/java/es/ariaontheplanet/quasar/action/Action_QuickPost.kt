package es.ariaontheplanet.quasar.action

import androidx.annotation.StringRes
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.actmain.currentPostTarget
import es.ariaontheplanet.quasar.api.entity.TootVisibility
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.dialog.pickAccount
import es.ariaontheplanet.quasar.util.PostImpl
import es.ariaontheplanet.quasar.util.PostInteractions
import es.ariaontheplanet.quasar.util.PostResult
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast

private val log = LogCategory("Action_QuickPost")

private class QuickPostInteractions(val activity: ActMain) : PostInteractions {
    override suspend fun confirm(message: String) {
        activity.confirm(message)
    }

    override suspend fun confirm(@StringRes messageId: Int, vararg args: Any?) {
        activity.confirm(messageId, *args)
    }

    override suspend fun confirm(
        message: String,
        isConfirmEnabled: Boolean,
        setConfirmEnabled: (Boolean) -> Unit,
    ) {
        activity.confirm(message, isConfirmEnabled, setConfirmEnabled)
    }

    override fun showToast(error: Boolean, message: String) {
        activity.showToast(error, message)
    }

    override fun showToast(error: Boolean, @StringRes messageId: Int, vararg args: Any?) {
        activity.showToast(error, activity.getString(messageId, *args))
    }
}

/**
 * QuickPostSheet の「送信」ボタンから直接 API 投稿を行う。
 * 添付・投票・予約投稿はサポートせず、CW・Visibility・本文のみ。
 * 完了したら sheet を閉じて状態をリセット、失敗時は VM の sending を解除する。
 */
fun ActMain.submitQuickPost(
    text: String,
    cwEnabled: Boolean,
    cwText: String,
    visibility: TootVisibility,
) {
    val content = text.trim()
    if (content.isEmpty()) {
        showToast(true, R.string.post_error_contents_empty)
        return
    }
    if (cwEnabled && cwText.trim().isEmpty()) {
        showToast(true, R.string.post_error_contents_warning_empty)
        return
    }

    launchMain {
        val account = currentPostTarget
            ?.takeIf { it.db_id != -1L && !it.isPseudo }
            ?: pickAccount(
                bAllowPseudo = false,
                bAuto = true,
                message = getString(R.string.account_picker_toot)
            )
            ?: return@launchMain

        viewModel.setQuickPostSending(true)
        try {
            val result = PostImpl(
                context = this@submitQuickPost,
                interactions = QuickPostInteractions(this@submitQuickPost),
                account = account,
                content = content,
                spoilerText = if (cwEnabled) cwText.trim() else null,
                visibilityArg = visibility,
                bNSFW = false,
                inReplyToId = null,
                attachmentListArg = null,
                enqueteItemsArg = null,
                pollType = null,
                pollExpireSeconds = 0,
                pollHideTotals = false,
                pollMultipleChoice = false,
                scheduledAt = 0L,
                scheduledId = null,
                redraftStatusId = null,
                editStatusId = null,
                emojiMapCustom = App1.custom_emoji_lister.getMapNonBlocking(account),
                useQuoteToot = false,
                lang = account.lang,
            ).runSuspend()

            when (result) {
                is PostResult.Normal,
                is PostResult.Scheduled,
                -> {
                    viewModel.resetQuickPost()
                    viewModel.closeQuickPostSheet()
                }
            }
        } catch (ex: Throwable) {
            log.e(ex, "submitQuickPost failed")
            showToast(true, ex.message ?: "post failed")
        } finally {
            viewModel.setQuickPostSending(false)
        }
    }
}
