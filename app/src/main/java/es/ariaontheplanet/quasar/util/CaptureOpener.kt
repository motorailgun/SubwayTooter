package es.ariaontheplanet.quasar.util

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.ComponentActivity
import es.ariaontheplanet.quasar.pref.PrefDevice
import es.ariaontheplanet.quasar.pref.prefDevice
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.UriAndType
import jp.juggler.util.data.checkMimeTypeAndGrant
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.isOk

/**
 * 動画や音声をキャプチャする
 * - Uriは呼び出し先に任せっきり
 */
class CaptureOpener(
    private val onCaptured: suspend (UriAndType) -> Unit,
) {
    companion object {
        private val log = LogCategory("CaptureOpener")
    }

    private lateinit var activity: ComponentActivity

    private val prefDevice: PrefDevice
        get() = activity.prefDevice

    private val prCapture = permissionSpecCapture.requester {
        open(
            prefDevice.captureAction,
            prefDevice.captureErrorCaption,
        )
    }

    private val arCapture = ActivityResultHandler(log) { handleCaptureResult(it) }

    fun register(activity: ComponentActivity) {
        this.activity = activity
        prCapture.register(activity)
        arCapture.register(activity)
    }

    fun open(
        action: String?,
        errorCaption: String?,
    ) {
        action ?: return
        errorCaption ?: return
        prefDevice.setCaptureParams(action, errorCaption)
        if (!prCapture.checkOrLaunch()) return
        try {
            arCapture.launch(Intent(action))
        } catch (ex: Throwable) {
            log.e(ex, errorCaption)
            activity.showToast(ex, errorCaption)
        }
    }

    private fun handleCaptureResult(r: ActivityResult) {
        activity.launchAndShowError {
            if (r.isOk) {
                r.data?.checkMimeTypeAndGrant(activity.contentResolver)?.firstOrNull()?.let {
                    onCaptured(it)
                }
            }
        }
    }
}
