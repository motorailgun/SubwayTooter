package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.flow.MutableStateFlow

private val log = LogCategory("SuspendProgress")

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    StThemedContent(content = content)
}

class SuspendProgress(val activity: ComponentActivity) {
    private val dialog = Dialog(activity)

    suspend fun <T : Any?> run(
        message: String,
        title: String,
        cancellable: Boolean,
        block: suspend (Reporter) -> T,
    ): T = Reporter().use { reporter ->
        try {
            reporter.setMessage(message)
            reporter.setTitle(title)

            val composeView = ComposeView(activity).apply {
                setContent {
                    AppTheme {
                        val currentTitle by reporter.flowTitle.collectAsState()
                        val currentMessage by reporter.flowMessage.collectAsState()

                        Surface {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (currentTitle.isNotEmpty()) {
                                        Text(
                                            text = currentTitle.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    if (currentMessage.isNotEmpty()) {
                                        Text(
                                            text = currentMessage.toString(),
                                            minLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            dialog.setContentView(composeView)
            dialog.setCancelable(cancellable)
            dialog.setCanceledOnTouchOutside(cancellable)
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            dialog.show()

            block(reporter)
        } finally {
            dialog.dismissSafe()
        }
    }

    inner class Reporter : AutoCloseable {
        val flowMessage = MutableStateFlow<CharSequence>("")
        val flowTitle = MutableStateFlow<CharSequence>("")

        override fun close() {
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
