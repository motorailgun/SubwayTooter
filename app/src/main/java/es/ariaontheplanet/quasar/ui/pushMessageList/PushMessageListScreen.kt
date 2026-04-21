package es.ariaontheplanet.quasar.ui.pushMessageList

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.Acct
import es.ariaontheplanet.quasar.api.entity.NotificationType.Companion.toNotificationType
import es.ariaontheplanet.quasar.dialog.actionsDialog
import es.ariaontheplanet.quasar.dialog.runInProgress
import es.ariaontheplanet.quasar.push.PushMessageIconColor
import es.ariaontheplanet.quasar.push.pushMessageIconAndColor
import es.ariaontheplanet.quasar.push.pushRepo
import es.ariaontheplanet.quasar.table.PushMessage
import es.ariaontheplanet.quasar.table.daoAccountNotificationStatus
import es.ariaontheplanet.quasar.table.daoPushMessage
import es.ariaontheplanet.quasar.table.daoSavedAccount
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
import kotlinx.coroutines.withContext
import java.io.PrintWriter

private val log = LogCategory("PushMessageListScreen")

@Composable
fun PushMessageListScreen() {
    val activity = LocalActivity.current as? ComponentActivity
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<PushMessage>() }
    val tintIconMap = remember { HashMap<String, Drawable>() }
    val acctMap = remember { daoSavedAccount.loadRealAccounts().associateBy { it.acct } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 特に何もしない */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        PushMessage.flowDataChanged.collect {
            try {
                val list = withContext(AppDispatchers.IO) { daoPushMessage.listAll() }
                messages.clear()
                messages.addAll(list)
            } catch (ex: Throwable) {
                log.e(ex, "load failed.")
            }
        }
    }

    fun onItemClick(pm: PushMessage) {
        activity ?: return
        activity.launchAndShowError {
            activity.actionsDialog {
                action(activity.getString(R.string.push_message_re_decode)) {
                    activity.pushRepo.reprocess(pm)
                }
                action(activity.getString(R.string.push_message_save_to_download_folder)) {
                    exportMessage(activity, pm, exportKeys = false)
                }
                action(activity.getString(R.string.push_message_save_to_download_folder_with_secret_key)) {
                    exportMessage(activity, pm, exportKeys = true)
                }
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(messages, key = { it.id }) { pm ->
            val type = pm.notificationType?.toNotificationType()
            val iconColor = type.pushMessageIconAndColor()
            PushMessageRow(
                pm = pm,
                errorDrawable = tintIcon(context, tintIconMap, acctMap, pm, iconColor),
                onClick = { onItemClick(pm) },
            )
            HorizontalDivider()
        }
    }
}

private suspend fun exportMessage(
    activity: ComponentActivity,
    pm: PushMessage,
    exportKeys: Boolean,
) {
    val path = activity.runInProgress<String?> {
        withContext(AppDispatchers.DEFAULT) {
            activity.saveToDownload(
                displayName = "PushMessageDump-${pm.id}.txt",
            ) { PrintWriter(it).apply { dumpMessage(pm, exportKeys) }.flush() }
        }
    }
    if (!path.isNullOrEmpty()) {
        activity.dialogOrToast(R.string.saved_to, path)
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

private fun tintIcon(
    context: android.content.Context,
    cache: HashMap<String, Drawable>,
    acctMap: Map<Acct, es.ariaontheplanet.quasar.table.SavedAccount>,
    pm: PushMessage,
    ic: PushMessageIconColor,
): Drawable = cache.getOrPut("${ic.name}-${pm.loginAcct}") {
    val a = pm.loginAcct?.let { acctMap[it] }
    val c = ic.colorRes.notZero()?.let { ContextCompat.getColor(context, it) }
        ?: a?.notificationAccentColor?.notZero()
        ?: ContextCompat.getColor(context, R.color.colorOsNotificationAccent)
    context.resDrawable(ic.iconId).wrapAndTint(color = c)
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
