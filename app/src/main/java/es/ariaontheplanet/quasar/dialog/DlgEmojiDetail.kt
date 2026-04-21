package es.ariaontheplanet.quasar.dialog

import android.app.Dialog
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.compose.AsyncImage
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.view.NetworkEmojiView
import jp.juggler.util.ui.dismissSafe

/**
 * 絵文字の詳細表示タイプ
 */
sealed interface EmojiDetailPreview {
    /** カスタム絵文字：NetworkEmojiViewで表示 */
    data class CustomEmoji(
        val url: String?,
        val initialAspect: Float?,
        val disableAnimation: Boolean,
    ) : EmojiDetailPreview

    /** Unicode絵文字(Twemoji)：画像アセットで表示 */
    data class UnicodeImage(
        val assetsName: String?,
        val drawableId: Int,
        val isSvg: Boolean,
    ) : EmojiDetailPreview

    /** Unicode絵文字（互換）：テキストで表示 */
    data class UnicodeText(
        val text: String,
    ) : EmojiDetailPreview
}

fun ComponentActivity.showEmojiDetailDialog(
    detail: String,
    preview: EmojiDetailPreview,
) {
    val dialog = Dialog(this)
    val composeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@showEmojiDetailDialog)
        setViewTreeSavedStateRegistryOwner(this@showEmojiDetailDialog)
        setContent {
            EmojiDetailContent(
                detail = detail,
                preview = preview,
                onDismiss = { dialog.dismissSafe() },
            )
        }
    }
    dialog.setTitle(R.string.emoji_detail)
    dialog.setContentView(composeView)
    dialog.window?.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )
    dialog.show()
}

@Composable
private fun EmojiDetailContent(
    detail: String,
    preview: EmojiDetailPreview,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Preview
            when (preview) {
                is EmojiDetailPreview.CustomEmoji -> {
                    AndroidView(
                        factory = { context ->
                            NetworkEmojiView(context).apply {
                                val dp200 = (200 * context.resources.displayMetrics.density + 0.5f).toInt()
                                setEmoji(
                                    url = preview.url,
                                    initialAspect = preview.initialAspect,
                                    defaultHeight = dp200,
                                )
                            }
                        },
                        modifier = Modifier.size(200.dp),
                    )
                }

                is EmojiDetailPreview.UnicodeImage -> {
                    val model: Any = if (preview.isSvg) {
                        "file:///android_asset/${preview.assetsName}"
                    } else {
                        preview.drawableId
                    }
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                    )
                }

                is EmojiDetailPreview.UnicodeText -> {
                    Text(
                        text = preview.text,
                        fontSize = 100.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // JSON detail
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        // Button bar
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.ok))
        }
    }
}
