package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.util.CustomShare
import jp.juggler.subwaytooter.util.cn
import jp.juggler.util.data.notEmpty
import jp.juggler.util.ui.dismissSafe
import jp.juggler.util.queryIntentActivitiesCompat

class DlgAppPicker(
    val activity: Activity,
    val intent: Intent,
    val autoSelect: Boolean = false,
    val addCopyAction: Boolean = false,
    val filter: (ResolveInfo) -> Boolean = { true },
    val callback: (String) -> Unit,
) {

    companion object {
        fun Char.isAlpha() = ('A' <= this && this <= 'Z') || ('a' <= this && this <= 'z')
    }

    class ListItem(
        val icon: Drawable?,
        val text: String,
        val componentName: String,
    )

    val list = ArrayList<ListItem>().apply {

        val pm = activity.packageManager
        val listResolveInfo = pm.queryIntentActivitiesCompat(intent, PackageManager.MATCH_ALL)

        for (it in listResolveInfo) {
            if (!this@DlgAppPicker.filter(it)) continue
            val cn = "${it.activityInfo.packageName}/${it.activityInfo.name}"
            val label = (it.loadLabel(pm).notEmpty() ?: cn).toString()
            add(ListItem(it.loadIcon(pm), label, cn))
        }

        // 自動選択オフの場合、末尾にクリップボード項目を追加する
        if (addCopyAction && !autoSelect) {
            val (label, icon) = CustomShare.getInfo(activity, CustomShare.CN_CLIPBOARD.cn())
            add(ListItem(icon, label.toString(), CustomShare.CN_CLIPBOARD))
        }

        sortWith { a, b ->
            val a1 = a.text.firstOrNull() ?: '\u0000'
            val b1 = b.text.firstOrNull() ?: '\u0000'
            when {
                !a1.isAlpha() && b1.isAlpha() -> -1
                a1.isAlpha() && !b1.isAlpha() -> 1
                else -> a.text.compareTo(b.text, ignoreCase = true)
            }
        }
    }

    // returns false if fallback required
    fun show() = when {
        list.isEmpty() -> false

        autoSelect && list.size == 1 -> {
            callback(list.first().componentName)
            true
        }

        else -> {
            val dialog = Dialog(activity)
            val composeView = ComposeView(activity).apply {
                setContent {
                    StThemedContent {
                        AppPickerContent(
                            items = list,
                            onSelect = { item ->
                                dialog.dismissSafe()
                                callback(item.componentName)
                            },
                            onCancel = { dialog.dismissSafe() },
                        )
                    }
                }
            }
            dialog.setContentView(composeView)
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
            dialog.show()
            true
        }
    }
}

@Composable
private fun AppPickerContent(
    items: List<DlgAppPicker.ListItem>,
    onSelect: (DlgAppPicker.ListItem) -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        androidx.compose.foundation.layout.Column {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
            ) {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val icon = item.icon
                        if (icon != null) {
                            val bitmap = remember(icon) {
                                icon.toBitmap(width = 32, height = 32).asImageBitmap()
                            }
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 12.dp),
                            )
                        }
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
