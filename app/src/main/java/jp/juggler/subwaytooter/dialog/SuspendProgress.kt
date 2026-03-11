package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val log = LogCategory("SuspendProgress")

class SuspendProgress(val activity: ComponentActivity) {

    private val density = activity.resources.displayMetrics.density
    private val dp16 = (16 * density + 0.5f).toInt()
    private val dp8 = (8 * density + 0.5f).toInt()
    private val dp280 = (280 * density + 0.5f).toInt()
    private val dp48 = (48 * density + 0.5f).toInt()
    private val dp60 = (60 * density + 0.5f).toInt()

    private val tvTitle = TextView(activity).apply {
        setTypeface(null, Typeface.BOLD)
        textSize = 18f
        minimumWidth = dp280
        minimumHeight = dp48
        gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
    }

    private val tvMessage = TextView(activity).apply {
        minimumWidth = dp280
        minimumHeight = dp60
        gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
    }

    private val root = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp16, dp16, dp16, dp16)
        addView(tvTitle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp8 })
        addView(tvMessage, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }

    private val dialog = Dialog(activity)

    suspend fun <T : Any?> run(
        message: String,
        title: String,
        cancellable: Boolean,
        block: suspend (Reporter) -> T,
    ): T = Reporter().use { reporter ->
        try {
            dialog.setContentView(root)
            reporter.setMessage(message)
            reporter.setTitle(title)
            dialog.setCancelable(cancellable)
            dialog.setCanceledOnTouchOutside(cancellable)
            dialog.show()
            block(reporter)
        } finally {
            dialog.dismissSafe()
        }
    }

    inner class Reporter : AutoCloseable {
        private val flowMessage = MutableStateFlow<CharSequence>("")
        private val flowTitle = MutableStateFlow<CharSequence>("")

        private val jobMessage = activity.lifecycleScope.launch(AppDispatchers.MainImmediate) {
            try {
                flowMessage.collect {
                    if (it.isNotEmpty()) {
                        tvMessage.visibility = View.VISIBLE
                        tvMessage.text = it
                    } else {
                        tvMessage.visibility = View.GONE
                    }
                }
            } catch (ex: Throwable) {
                when (ex) {
                    is CancellationException, is ClosedReceiveChannelException -> Unit
                    else -> log.w(ex, "error.")
                }
            }
        }
        private val jobTitle = activity.lifecycleScope.launch(AppDispatchers.MainImmediate) {
            try {
                flowTitle.collect {
                    if (it.isNotEmpty()) {
                        tvTitle.visibility = View.VISIBLE
                        tvTitle.text = it
                    } else {
                        tvTitle.visibility = View.GONE
                    }
                }
            } catch (ex: Throwable) {
                when (ex) {
                    is CancellationException, is ClosedReceiveChannelException -> Unit
                    else -> log.w(ex, "error.")
                }
            }
        }

        override fun close() {
            jobMessage.cancel()
            jobTitle.cancel()
        }

        fun setMessage(msg: CharSequence) {
            flowMessage.value = msg
        }

        fun setTitle(title: CharSequence) {
            flowTitle.value = title
        }
    }
}

suspend fun <T : Any?> ComponentActivity.runInProgress(
    message: String = "please wait…",
    title: String = "",
    cancellable: Boolean = true,
    block: suspend (SuspendProgress.Reporter) -> T,
): T = SuspendProgress(this).run(
    message = message,
    title = title,
    cancellable = cancellable,
    block = block
)
