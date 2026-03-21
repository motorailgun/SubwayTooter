package jp.juggler.subwaytooter.actpost

import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import jp.juggler.subwaytooter.ActPost
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.ApiTask
import jp.juggler.subwaytooter.api.TootApiResult
import jp.juggler.subwaytooter.api.entity.ServiceType
import jp.juggler.subwaytooter.api.entity.TootAttachment
import jp.juggler.subwaytooter.api.entity.TootAttachment.Companion.tootAttachmentJson
import jp.juggler.subwaytooter.api.entity.TootAttachment.Companion.tootAttachment
import jp.juggler.subwaytooter.api.entity.TootAttachmentType
import jp.juggler.subwaytooter.api.entity.parseItem
import jp.juggler.subwaytooter.api.runApiTask
import jp.juggler.subwaytooter.dialog.actionsDialog
import jp.juggler.subwaytooter.dialog.decodeAttachmentBitmap
import jp.juggler.subwaytooter.dialog.dialogAttachmentRearrange
import jp.juggler.subwaytooter.dialog.focusPointDialog
import jp.juggler.subwaytooter.dialog.showMediaDescEditDialog
import jp.juggler.subwaytooter.util.PostAttachment
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.buildJsonObject
import jp.juggler.util.data.decodeJsonArray
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.dialogOrToast
import jp.juggler.util.log.showToast
import jp.juggler.util.log.withCaption
import jp.juggler.util.network.toPutRequestBuilder
import jp.juggler.util.ui.InputTypeEx
import jp.juggler.util.ui.isLiveActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val log = LogCategory("ActPostAttachment")

// AppStateに保存する
fun ActPost.saveAttachmentList() {
    if (!isMultiWindowPost) appState.attachmentList = this.attachmentList
}

fun ActPost.decodeAttachments(sv: String) {
    attachmentList.clear()
    try {
        sv.decodeJsonArray().objectList().forEach {
            try {
                attachmentList.add(PostAttachment(tootAttachmentJson(it)))
            } catch (ex: Throwable) {
                log.e(ex, "can't parse TootAttachment.")
            }
        }
        viewModel.notifyAttachmentListUpdated()
    } catch (ex: Throwable) {
        log.e(ex, "decodeAttachments failed.")
    }
}

fun ActPost.openAttachment() {
    viewModel.openAttachment()
}

fun ActPost.addAttachment(
    uri: Uri,
    mimeTypeArg: String? = null,
) {
    viewModel.addAttachment(uri, mimeTypeArg)
}

fun ActPost.showMediaAttachment() {
    viewModel.notifyAttachmentListUpdated()
}

fun ActPost.showMediaAttachmentProgress() {
    viewModel.notifyAttachmentListUpdated()
}

// 添付した画像をタップ
fun ActPost.performAttachmentClick(pa: PostAttachment) {
    launchAndShowError {
        actionsDialog(getString(R.string.media_attachment)) {
            action(getString(R.string.set_description)) {
                editAttachmentDescription(pa)
            }
            if (pa.attachment?.canFocus == true) {
                action(getString(R.string.set_focus_point)) {
                    openFocusPoint(pa)
                }
            }
            if (account?.isMastodon == true) {
                if (pa.attachment?.isEdit == true) {
                    // https://github.com/tateisu/SubwayTooter/issues/237
                    // 既存の投稿の編集時にサムネイルを更新できるようにするのが著しく面倒くさい
                    // 一旦未対応とする
                } else when (pa.attachment?.type) {
                    TootAttachmentType.Audio,
                    TootAttachmentType.GIFV,
                    TootAttachmentType.Video,
                    -> action(getString(R.string.custom_thumbnail)) {
                        attachmentPicker.openThumbnailPicker(pa)
                    }

                    else -> Unit
                }
            }
            action(getString(R.string.delete)) {
                deleteAttachment(pa)
            }
        }
    }
}

fun ActPost.deleteAttachment(pa: PostAttachment) {
    AlertDialog.Builder(this)
        .setTitle(R.string.confirm_delete_attachment)
        .setPositiveButton(R.string.ok) { _, _ ->
            viewModel.deleteAttachment(pa)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}

suspend fun ActPost.openFocusPoint(pa: PostAttachment) {
    val attachment = pa.attachment ?: return
    focusPointDialog(
        attachment = attachment,
        callback = { x, y -> sendFocusPoint(pa, attachment, x, y) }
    )
}

suspend fun ActPost.sendFocusPoint(
    pa: PostAttachment,
    attachment: TootAttachment,
    x: Float,
    y: Float,
): Boolean {
    val account = this.account ?: error("missing account")
    if (attachment.isEdit) {
        viewModel.setFocusPoint(pa, x, y)
        return true
    }

    // TODO: move API call to ViewModel
    var resultAttachment: TootAttachment? = null
    val result = runApiTask(account, progressStyle = ApiTask.PROGRESS_NONE) { client ->
        try {
            client.request(
                "/api/v1/media/${attachment.id}",
                buildJsonObject {
                    put("focus", "%.2f,%.2f".format(x, y))
                }.toPutRequestBuilder()
            )?.also { result ->
                resultAttachment = parseItem(result.jsonObject) {
                    tootAttachment(ServiceType.MASTODON, it)
                }
            }
        } catch (ex: Throwable) {
            TootApiResult(ex.withCaption("set focus point failed."))
        }
    }
    result ?: return true
    return when (val newAttachment = resultAttachment) {
        null -> {
            showToast(true, result.error)
            false
        }

        else -> {
            pa.attachment = newAttachment
            viewModel.notifyAttachmentListUpdated()
            true
        }
    }
}

suspend fun ActPost.editAttachmentDescription(
    pa: PostAttachment,
) {
    val a = pa.attachment
    if (a == null) {
        showToast(true, R.string.attachment_description_cant_edit_while_uploading)
        return
    }
    
    var bitmap: Bitmap? = null
    try {
        // サムネイルをロード
        val url = a.preview_url
        if (url != null) {
            val result = runApiTask { client ->
                try {
                    val (result, data) = client.getHttpBytes(url)
                    data?.let {
                        bitmap = decodeAttachmentBitmap(it, 1024)
                            ?: return@runApiTask TootApiResult("image decode failed.")
                    }
                    result
                } catch (ex: Throwable) {
                    TootApiResult(ex.withCaption("preview loading failed."))
                }
            }
            result ?: return
            if (!isLiveActivity) return
            result.error?.let {
                showToast(true, result.error ?: "error")
                // not exit
            }
        }
        // ダイアログを表示
        showMediaDescEditDialog(
            title = getString(R.string.attachment_description),
            bitmap = bitmap,
            inputType = InputTypeEx.textMultiLine,
            initialText = a.description,
            onEmptyText = { showToast(true, R.string.description_empty) },
        ) { text ->
             viewModel.setAttachmentDescription(pa, text)
             true
        }
    } finally {
        bitmap?.recycle()
    }
}

fun ActPost.rearrangeAttachments() = lifecycleScope.launch {
    try {
        val rearranged = dialogAttachmentRearrange(attachmentList)
        val remain = ArrayList(attachmentList)
        val newList = buildList {
            rearranged.map { a ->
                val pa = remain.find { it === a }
                if (pa != null) {
                    add(pa)
                    remain.remove(pa)
                }
            }
            addAll(remain)
        }
        viewModel.setAttachments(newList)
    } catch (ex: Throwable) {
        log.e(ex, "attachmentRearrange failed.")
        if (ex !is CancellationException) {
            dialogOrToast(ex.withCaption("attachmentRearrange failed."))
        }
    }
}
