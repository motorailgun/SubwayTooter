package es.ariaontheplanet.quasar.actpost

import android.app.Activity
import android.content.Intent
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.ActPost
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.actmain.onCompleteActPost
import es.ariaontheplanet.quasar.api.entity.TootPollsType
import es.ariaontheplanet.quasar.api.entity.TootVisibility
import es.ariaontheplanet.quasar.api.entity.unknownHostAndDomain
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.dialog.actionsDialog
import es.ariaontheplanet.quasar.pref.PrefB
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.table.daoPostDraft
import es.ariaontheplanet.quasar.table.daoSavedAccount
import es.ariaontheplanet.quasar.table.sortedByNickname
import es.ariaontheplanet.quasar.util.*
import jp.juggler.util.*
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.CharacterGroup
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast

private val log = LogCategory("ActPostExtra")

fun ActPost.appendContentText(
    src: String?,
    selectBefore: Boolean = false,
) {
    if (src?.isEmpty() != false) return
    val svEmoji = DecodeOptions(
        context = this,
        decodeEmoji = true,
        authorDomain = account ?: unknownHostAndDomain,
        emojiSizeMode = account.emojiSizeMode(),
    ).decodeEmoji(src)
    if (svEmoji.isEmpty()) return

    val currentText = views.etContent.text.toString()
    val emojiStr = svEmoji.toString()
    val needsSpace = currentText.isNotEmpty() &&
        !CharacterGroup.isWhitespace(currentText.last().code)
    val prefix = if (needsSpace) " " else ""

    if (selectBefore) {
        val selStart = currentText.length + prefix.length
        val newText = "$currentText$prefix $emojiStr"
        views.etContent.setText(newText)
        views.etContent.setSelection(selStart)
    } else {
        val newText = "$currentText$prefix$emojiStr"
        views.etContent.setText(newText)
        views.etContent.setSelection(newText.length)
    }
}

fun ActPost.appendContentText(src: Intent) {
    val list = ArrayList<String>()

    var sv: String?
    sv = src.string(Intent.EXTRA_SUBJECT)
    if (sv?.isNotEmpty() == true) list.add(sv)
    sv = src.string(Intent.EXTRA_TEXT)
    if (sv?.isNotEmpty() == true) list.add(sv)

    if (list.isNotEmpty()) {
        appendContentText(list.joinToString(" "))
    }
}

// returns true if has content
fun ActPost.hasContent(): Boolean {
    val content = views.etContent.text.toString()
    val contentWarning =
        if (contentWarningChecked) views.etContentWarning.text.toString() else ""

    return when {
        content.isNotBlank() -> true
        contentWarning.isNotBlank() -> true
        hasPoll() -> true
        else -> false
    }
}

fun ActPost.resetText() {
    isPostComplete = false

    resetReply()

    resetMushroom()
    states.redraftStatusId = null
    states.editStatusId = null
    states.timeSchedule = 0L
    attachmentPicker.reset()
    scheduledStatus = null
    attachmentList.clear()
    quoteChecked = false
    views.etContent.setText("")
    pollTypeIndex = 0
    pollMultipleChoiceChecked = false
    pollHideTotalsChecked = false
    etChoices.forEach { it.setText("") }
    accountList = daoSavedAccount.loadAccountList().sortedByNickname()
    if (accountList.isEmpty()) {
        showToast(true, R.string.please_add_account)
        finish()
    }
}

suspend fun ActPost.afterUpdateText() {
    // 2017/9/13 VISIBILITY_WEB_SETTING から VISIBILITY_PUBLICに変更した
    // VISIBILITY_WEB_SETTING だと 1.5未満のタンスでトラブルになるので…
    states.visibility = states.visibility ?: account?.visibility ?: TootVisibility.Public

    // アカウント未選択なら表示を更新する
    // 選択済みなら変えない
    if (account == null) selectAccount(null)

    showContentWarningEnabled()
    showMediaAttachment()
    showVisibility()
    showReplyTo()
    showPoll()
    showQuotedRenote()
    showSchedule()
    updateTextCount()
}

// 初期化時と投稿完了時とリセット確認後に呼ばれる
suspend fun ActPost.updateText(
    intent: Intent,
    saveDraft: Boolean = true,
    resetAccount: Boolean = true,
) {
    if (!canSwitchAccount()) return

    if (saveDraft && hasContent()) {
        confirm(R.string.post_reset_confirm)
        saveDraft()
    }

    resetText()

    // Android 9 から、明示的にフォーカスを当てる必要がある
    try { contentFocusRequester.requestFocus() } catch (_: Exception) {}

    this.attachmentList.clear()
    saveAttachmentList()

    if (resetAccount) {
        states.visibility = null
        this.account = null
        intent.long(ActPost.KEY_ACCOUNT_DB_ID)
            ?.let { dbId -> accountList.find { it.db_id == dbId } }
            ?.let { selectAccount(it) }
    }

    val sharedIntent = intent.getIntentExtra(ActPost.KEY_SHARED_INTENT)

    if (sharedIntent != null) {
        initializeFromSharedIntent(sharedIntent)
    }

    appendContentText(intent.string(ActPost.KEY_INITIAL_TEXT))

    if (resetAccount) {
        // QuickPostSheet からの引き継ぎ。CW・Visibility は新規セッションのみ適用
        if (intent.getBooleanExtra(ActPost.KEY_INITIAL_CW_ENABLED, false)) {
            contentWarningChecked = true
            intent.string(ActPost.KEY_INITIAL_CW_TEXT)?.let {
                views.etContentWarning.setText(it)
            }
        }
        intent.string(ActPost.KEY_INITIAL_VISIBILITY)?.let { name ->
            runCatching { TootVisibility.valueOf(name) }
                .getOrNull()
                ?.let { states.visibility = it }
        }
    }

    val account = this.account

    if (account != null) {
        intent.string(ActPost.KEY_REPLY_STATUS)
            ?.let { initializeFromReplyStatus(account, it) }
    }

    appendContentText(account?.defaultText, selectBefore = true)
    nsfwChecked = account?.defaultSensitive ?: false

    if (account != null) {
        // 再編集
        intent.string(ActPost.KEY_REDRAFT_STATUS)
            ?.let { initializeFromRedraftStatus(account, it) }

        // 再編集
        intent.string(ActPost.KEY_EDIT_STATUS)
            ?.let { initializeFromEditStatus(account, it) }

        // 予約編集の再編集
        intent.string(ActPost.KEY_SCHEDULED_STATUS)
            ?.let { initializeFromScheduledStatus(account, it) }
    }

    afterUpdateText()
}

fun ActPost.initializeFromSharedIntent(sharedIntent: Intent) {
    try {
        val hasUri = when (sharedIntent.action) {
            Intent.ACTION_VIEW -> {
                val uri = sharedIntent.data
                val type = sharedIntent.type
                if (uri != null) {
                    addAttachment(uri, type)
                    true
                } else {
                    false
                }
            }

            Intent.ACTION_SEND -> {
                val uri = sharedIntent.getStreamUriExtra()
                val type = sharedIntent.type
                if (uri != null) {
                    addAttachment(uri, type)
                    true
                } else {
                    false
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val listUri = sharedIntent.getStreamUriListExtra()
                    ?.filterNotNull()
                if (listUri?.isNotEmpty() == true) {
                    for (uri in listUri) {
                        addAttachment(uri)
                    }
                    true
                } else {
                    false
                }
            }

            else -> false
        }

        if (!hasUri || !PrefB.bpIgnoreTextInSharedMedia.value) {
            appendContentText(sharedIntent)
        }
    } catch (ex: Throwable) {
        log.e(ex, "initializeFromSharedIntent failed.")
    }
}

fun ActPost.performMore() {
    launchAndShowError {
        actionsDialog {
            action(getString(R.string.open_picker_emoji)) {
                 openEmojiPickerForContent()
            }

            action(getString(R.string.clear_text)) {
                views.etContent.setText("")
                views.etContentWarning.setText("")
            }

            action(getString(R.string.clear_text_and_media)) {
                views.etContent.setText("")
                views.etContentWarning.setText("")
                attachmentList.clear()
                saveAttachmentList()
                showMediaAttachment()
            }

            if (daoPostDraft.hasDraft()) action(getString(R.string.restore_draft)) {
                openDraftPicker()
            }

            action(getString(R.string.plugin_app_intro)) {
                openPluginList()
            }
        }
    }
}

fun ActPost.performPost() {
    viewModel.performPost(isMultiWindowPost)
}

fun ActPost.showContentWarningEnabled() {
    // no-op: CW field visibility is controlled directly by Compose state.
}
