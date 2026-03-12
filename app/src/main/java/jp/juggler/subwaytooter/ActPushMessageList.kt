package jp.juggler.subwaytooter

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import jp.juggler.subwaytooter.api.entity.NotificationType.Companion.toNotificationType
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.dialog.actionsDialog
import jp.juggler.subwaytooter.dialog.runInProgress
import jp.juggler.subwaytooter.push.PushMessageIconColor
import jp.juggler.subwaytooter.push.pushMessageIconAndColor
import jp.juggler.subwaytooter.push.pushRepo
import jp.juggler.subwaytooter.table.PushMessage
import jp.juggler.subwaytooter.table.daoAccountNotificationStatus
import jp.juggler.subwaytooter.table.daoPushMessage
import jp.juggler.subwaytooter.table.daoSavedAccount
import jp.juggler.subwaytooter.util.permissionSpecNotification
import jp.juggler.subwaytooter.util.requester
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.encodeBase64Url
import jp.juggler.util.data.notBlank
import jp.juggler.util.data.notZero
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.dialogOrToast
import jp.juggler.util.os.saveToDownload
import jp.juggler.util.time.formatLocalTime
import jp.juggler.util.ui.resDrawable
import jp.juggler.util.ui.wrapAndTint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter

class ActPushMessageList : ComponentActivity() {
    companion object {
        private val log = LogCategory("ActPushMessageList")
    }

    private val messages = mutableStateListOf<PushMessage>()

    private val prNotification = permissionSpecNotification.requester {
        // 特に何もしない
    }

    private val acctMap by lazy {
        daoSavedAccount.loadRealAccounts().associateBy { it.acct }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prNotification.register(this)
        prNotification.checkOrLaunch()
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        backPressed { finish() }
        setContent {
            StScreen(
                title = getString(R.string.push_message_history),
                onBack = { finish() },
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    items(messages, key = { it.id }) { pm ->
                        val type = pm.notificationType?.toNotificationType()
                        val iconColor = type.pushMessageIconAndColor()
                        PushMessageRow(
                            pm = pm,
                            errorDrawable = tintIcon(pm, iconColor),
                            onClick = { itemActions(pm) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        lifecycleScope.launch {
            PushMessage.flowDataChanged.collect {
                try {
                    val list = withContext(AppDispatchers.IO) {
                        daoPushMessage.listAll()
                    }
                    messages.clear()
                    messages.addAll(list)
                } catch (ex: Throwable) {
                    log.e(ex, "load failed.")
                }
            }
        }
    }

    private fun itemActions(pm: PushMessage) {
        launchAndShowError {
            actionsDialog {
                action(getString(R.string.push_message_re_decode)) {
                    pushRepo.reprocess(pm)
                }
                action(getString(R.string.push_message_save_to_download_folder)) {
                    export(pm)
                }
                action(getString(R.string.push_message_save_to_download_folder_with_secret_key)) {
                    export(pm, exportKeys = true)
                }
            }
        }
    }

    /**
     * エクスポート、というか端末のダウンロードフォルダに保存する
     */
    private suspend fun export(pm: PushMessage, exportKeys: Boolean = false) {
        val path = runInProgress {
            withContext(AppDispatchers.DEFAULT) {
                saveToDownload(
                    displayName = "PushMessageDump-${pm.id}.txt",
                ) { PrintWriter(it).apply { dumpMessage(pm, exportKeys) }.flush() }
            }
        }
        if (!path.isNullOrEmpty()) {
            dialogOrToast(R.string.saved_to, path)
        }
    }

    private fun PrintWriter.dumpMessage(pm: PushMessage, exportKeys: Boolean) {
        println("timestamp: ${pm.timestamp.formatLocalTime()}")
        println("timeSave: ${pm.timeSave.formatLocalTime()}")
        println("timeDismiss: ${pm.timeDismiss.formatLocalTime()}")
        println("to: ${pm.loginAcct}")
        println("type: ${pm.notificationType}")
        println("id: ${pm.notificationId}")
        println("text: ${pm.textExpand}")
        println("formatJson=${pm.formatJson.toString(1, sort = true)}")
        println("messageJson=${pm.messageJson?.toString(1, sort = true)}")
        println("dataSize: ${pm.rawBody?.size}")
        if (exportKeys) {
            val acct = pm.loginAcct
            if (acct == null) {
                println("!!secret key is not exported because missing recepients acct.")
            } else {
                val status = daoAccountNotificationStatus.load(acct)
                if (status == null) {
                    println("!!secret key is not exported because missing status for acct $acct .")
                } else {
                    println("receiverPrivateBytes=${status.pushKeyPrivate?.encodeBase64Url()}")
                    println("receiverPublicBytes=${status.pushKeyPublic?.encodeBase64Url()}")
                    println("senderPublicBytes=${status.pushServerKey?.encodeBase64Url()}")
                    println("authSecret=${status.pushAuthSecret?.encodeBase64Url()}")
                }
            }
        }
        println("headerJson=${pm.headerJson}")
        println("rawBody=${pm.rawBody?.encodeBase64Url()}")
    }

    private val tintIconMap = HashMap<String, Drawable>()

    private fun tintIcon(pm: PushMessage, ic: PushMessageIconColor): Drawable =
        tintIconMap.getOrPut("${ic.name}-${pm.loginAcct}") {
            val context = this
            val a = acctMap[pm.loginAcct]
            val c = ic.colorRes.notZero()?.let { ContextCompat.getColor(context, it) }
                ?: a?.notificationAccentColor?.notZero()
                ?: ContextCompat.getColor(this, R.color.colorOsNotificationAccent)
            context.resDrawable(ic.iconId).wrapAndTint(color = c)
        }
}

@Composable
private fun PushMessageRow(
    pm: PushMessage,
    errorDrawable: Drawable,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column {
            GlideImage(
                model = pm.iconSmall,
                errorDrawable = errorDrawable,
                modifier = Modifier.size(48.dp),
            )
            GlideImage(
                model = pm.iconLarge,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = pushMessageText(pm),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 40.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun GlideImage(
    model: Any?,
    modifier: Modifier = Modifier,
    errorDrawable: Drawable? = null,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        },
        modifier = modifier,
        update = { imageView ->
            val request = Glide.with(imageView).load(model)
            if (errorDrawable != null) {
                request.error(errorDrawable)
            }
            request.into(imageView)
        },
    )
}

private fun pushMessageText(pm: PushMessage): String = arrayOf(
    "when: ${pm.timestamp.formatLocalTime()}",
    pm.timeDismiss.takeIf { it > 0L }?.let { "既読: ${it.formatLocalTime()}" },
    "to: ${pm.loginAcct}",
    "type: ${pm.notificationType}",
    "id: ${pm.notificationId}",
    "dataSize: ${pm.rawBody?.size}",
    pm.textExpand,
    pm.formatError?.let { "error: $it" },
).mapNotNull { it?.notBlank() }.joinToString("\n")

@Preview(showBackground = true)
@Composable
fun PreviewPushMessageRow() {
    PushMessageRow(
        pm = PushMessage(),
        errorDrawable = android.graphics.drawable.ColorDrawable(0),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewGlideImage() {
    GlideImage(model = null)
}
