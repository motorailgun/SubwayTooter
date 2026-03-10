package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import jp.juggler.subwaytooter.R
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DlgConfirm {

    internal fun Activity.createConfirmView(
        message: CharSequence,
        showSkipNext: Boolean = false,
        showMuteDuration: Boolean = false,
    ): Triple<LinearLayout, CheckBox?, Spinner?> {
        val density = resources.displayMetrics.density
        val dp12 = (12 * density + 0.5f).toInt()
        val dp8 = (8 * density + 0.5f).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tvMessage = TextView(this).apply {
            text = message
            textSize = 16f
            setPadding(dp12, dp12, dp12, 0)
        }
        root.addView(
            tvMessage,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val cbSkipNext = if (showSkipNext) {
            CheckBox(this).apply {
                setText(R.string.dont_confirm_again)
                setPadding(dp12, dp8, dp12, 0)
            }.also {
                root.addView(
                    it,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
        } else null

        val spMuteDuration = if (showMuteDuration) {
            val ll = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp12, dp8, dp12, 0)
            }
            val tvDuration = TextView(this).apply {
                setText(R.string.duration)
            }
            ll.addView(
                tvDuration,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            Spinner(this).apply {
                minimumHeight = (40 * density + 0.5f).toInt()
            }.also { sp ->
                ll.addView(
                    sp,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
            root.addView(
                ll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            ll.findViewWithTag<Spinner>(null)
                ?: ll.getChildAt(1) as? Spinner
        } else null

        return Triple(root, cbSkipNext, spMuteDuration)
    }

    suspend fun Activity.confirm(
        message: String,
        isConfirmEnabled: Boolean,
        setConfirmEnabled: (newConfirmEnabled: Boolean) -> Unit,
    ) {
        if (!isConfirmEnabled) return
        val (root, cbSkipNext, _) = createConfirmView(message, showSkipNext = true)
        val skipNext = suspendCancellableCoroutine { cont ->
            try {
                val dialog = AlertDialog.Builder(this)
                    .setView(root)
                    .setCancelable(true)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        if (cont.isActive) cont.resume(cbSkipNext?.isChecked == true)
                    }
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog cancelled."))
                }
                dialog.show()
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
        if (skipNext) setConfirmEnabled(false)
    }

    suspend fun Activity.confirm(@StringRes messageId: Int, vararg args: Any?) =
        confirm(getString(messageId, *args))

    suspend fun Activity.confirm(message: CharSequence, title: CharSequence? = null) {
        suspendCancellableCoroutine { cont ->
            try {
                val (root, _, _) = createConfirmView(message)
                val dialog = AlertDialog.Builder(this).apply {
                    setView(root)
                    setCancelable(true)
                    title?.let { setTitle(it) }
                    setNegativeButton(R.string.cancel, null)
                    setPositiveButton(R.string.ok) { _, _ ->
                        if (cont.isActive) cont.resume(Unit)
                    }
                }.create()
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog closed."))
                }
                dialog.show()
                cont.invokeOnCancellation { dialog.dismissSafe() }
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
    }

    suspend fun Activity.okDialog(@StringRes messageId: Int, vararg args: Any?) =
        okDialog(getString(messageId, *args))

    suspend fun Activity.okDialog(message: CharSequence, title: CharSequence? = null) {
        suspendCancellableCoroutine { cont ->
            try {
                val density = resources.displayMetrics.density
                val dp12 = (12 * density + 0.5f).toInt()

                val tvMessage = TextView(this).apply {
                    movementMethod = LinkMovementMethod.getInstance()
                    autoLinkMask = Linkify.WEB_URLS
                    textSize = 16f
                    setPadding(dp12, dp12, dp12, dp12)
                    text = message
                }

                val dialog = AlertDialog.Builder(this).apply {
                    setView(tvMessage)
                    setCancelable(true)
                    title?.let { setTitle(it) }
                    setPositiveButton(R.string.ok) { _, _ ->
                        if (cont.isActive) cont.resume(Unit)
                    }
                }.create()
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog closed."))
                }
                dialog.show()
                cont.invokeOnCancellation { dialog.dismissSafe() }
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
    }
}
