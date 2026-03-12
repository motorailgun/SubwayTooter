package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.*
import jp.juggler.subwaytooter.api.entity.TootAttachment
import jp.juggler.subwaytooter.view.FocusPointView
import jp.juggler.util.*
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.*
import jp.juggler.util.log.*
import jp.juggler.util.ui.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val log = LogCategory("DlgFocusPoint")

fun decodeAttachmentBitmap(
    data: ByteArray,
    @Suppress("SameParameterValue") pixelMax: Int,
): Bitmap? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    options.inScaled = false
    options.outWidth = 0
    options.outHeight = 0
    BitmapFactory.decodeByteArray(data, 0, data.size, options)
    var w = options.outWidth
    var h = options.outHeight
    if (w <= 0 || h <= 0) {
        log.e("can't decode bounds.")
        return null
    }
    var bits = 0
    while (w > pixelMax || h > pixelMax) {
        ++bits
        w = w shr 1
        h = h shr 1
    }
    options.inJustDecodeBounds = false
    options.inSampleSize = 1 shl bits
    return BitmapFactory.decodeByteArray(data, 0, data.size, options)
}

suspend fun ComponentActivity.focusPointDialog(
    attachment: TootAttachment,
    callback: suspend (x: Float, y: Float) -> Boolean,
) {
    var bitmap: Bitmap? = null
    try {
        val url = attachment.preview_url
        if (url == null) {
            showToast(false, "missing preview_url")
            return
        }
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
        if (bitmap == null) {
            showToast(true, result.error ?: "error")
            return
        } else if (!isLiveActivity) {
            return
        }
        val dialog = Dialog(this)
        val ivFocus = FocusPointView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(ivFocus, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply {
                setMargins(dp(12), dp(12), dp(12), dp(12))
            })
            val buttonBar = LinearLayout(this@focusPointDialog).apply {
                orientation = LinearLayout.HORIZONTAL
                val btnCancel = Button(this@focusPointDialog).apply {
                    text = getString(R.string.cancel)
                    setOnClickListener { dialog.dismissSafe() }
                }
                addView(btnCancel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                val btnOk = Button(this@focusPointDialog).apply {
                    text = getString(R.string.ok)
                    setOnClickListener {
                        launchMain {
                            try {
                                if (callback(ivFocus.focusX, ivFocus.focusY)) {
                                    dialog.dismissSafe()
                                }
                            } catch (ex: Throwable) {
                                showToast(ex, "can't set focus point.")
                            }
                        }
                    }
                }
                addView(btnOk, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            addView(buttonBar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        dialog.setContentView(root)
        ivFocus.setAttachment(attachment, bitmap!!)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        // dialogが閉じるまで待ってからbitmapをリサイクルする
        suspendCancellableCoroutine { cont ->
            dialog.setOnDismissListener {
                if (cont.isActive) cont.resume(Unit)
            }
            cont.invokeOnCancellation {
                dialog.dismissSafe()
            }
            dialog.show()
        }
    } finally {
        bitmap?.recycle()
        bitmap = null
    }
}
