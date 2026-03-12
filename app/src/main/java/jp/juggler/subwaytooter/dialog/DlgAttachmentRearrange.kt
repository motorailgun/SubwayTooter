package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.bumptech.glide.Glide
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.defaultColorIcon
import jp.juggler.subwaytooter.util.PostAttachment
import jp.juggler.util.coroutine.cancellationException
import jp.juggler.util.data.ellipsizeDot3
import jp.juggler.util.ui.dismissSafe
import jp.juggler.util.ui.dp as dpPx
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

/**
 * 投稿画面で添付メディアを並べ替えるダイアログを開き、OKボタンが押されるまで非同期待機する。
 * OK以外の方法で閉じたらCancellationExceptionを投げる。
 */
suspend fun ComponentActivity.dialogAttachmentRearrange(
    initialList: List<PostAttachment>,
): List<PostAttachment> = suspendCancellableCoroutine { cont ->
    val dialog = Dialog(this)
    val composeView = ComposeView(this).apply {
        setContent {
            StThemedContent {
                AttachmentRearrangeContent(
                    initialList = initialList,
                    onOk = { reorderedList ->
                        if (cont.isActive) cont.resume(reorderedList) { _, _, _ -> }
                        dialog.dismissSafe()
                    },
                    onCancel = { dialog.dismissSafe() },
                )
            }
        }
    }
    dialog.setContentView(composeView)
    dialog.setOnDismissListener {
        if (cont.isActive) cont.resumeWithException(cancellationException())
    }
    cont.invokeOnCancellation { dialog.dismissSafe() }
    dialog.window?.setLayout(dpPx(300), dpPx(440))
    dialog.show()
}

@Composable
private fun AttachmentRearrangeContent(
    initialList: List<PostAttachment>,
    onOk: (List<PostAttachment>) -> Unit,
    onCancel: () -> Unit,
) {
    val items = remember { mutableStateListOf(*initialList.toTypedArray()) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeight by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val iconPlaceHolder = remember { defaultColorIcon(context, R.drawable.ic_hourglass) }
    val iconError = remember { defaultColorIcon(context, R.drawable.ic_error) }
    val iconFallback = remember { defaultColorIcon(context, R.drawable.ic_clip) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header description
        Text(
            text = stringResource(R.string.attachment_rearrange_desc),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Reorderable item list
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 48.dp, vertical = 8.dp),
        ) {
            items.forEachIndexed { index, item ->
                val isDragging = draggingIndex == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            if (itemHeight == 0f) {
                                itemHeight = coordinates.size.height.toFloat()
                            }
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationY = dragOffsetY
                            }
                        }
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                            else MaterialTheme.colorScheme.surface
                        )
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggingIndex = -1
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = -1
                                    dragOffsetY = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount
                                    val threshold = itemHeight / 2
                                    if (threshold > 0f) {
                                        val di = draggingIndex
                                        if (dragOffsetY > threshold && di < items.lastIndex) {
                                            val moved = items.removeAt(di)
                                            items.add(di + 1, moved)
                                            draggingIndex = di + 1
                                            dragOffsetY -= itemHeight
                                        } else if (dragOffsetY < -threshold && di > 0) {
                                            val moved = items.removeAt(di)
                                            items.add(di - 1, moved)
                                            draggingIndex = di - 1
                                            dragOffsetY += itemHeight
                                        }
                                    }
                                },
                            )
                        }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AttachmentThumbnail(
                        item = item,
                        iconPlaceHolder = iconPlaceHolder,
                        iconError = iconError,
                        iconFallback = iconFallback,
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // テキストの表示: type + description
                    Text(
                        text = item.attachment?.run {
                            "${type.id} ${description?.ellipsizeDot3(40) ?: ""}"
                        } ?: "",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Divider + button bar
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.cancel))
            }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            TextButton(
                onClick = { onOk(items.toList()) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    }
}

@Composable
private fun AttachmentThumbnail(
    item: PostAttachment,
    iconPlaceHolder: Drawable?,
    iconError: Drawable?,
    iconFallback: Drawable?,
) {
    val imageUrl = item.attachment?.preview_url
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setBackgroundColor(0x80808080.toInt())
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        modifier = Modifier.size(80.dp),
        update = { imageView ->
            val context = imageView.context
            when {
                imageUrl.isNullOrEmpty() -> {
                    val icon = when (item.status) {
                        PostAttachment.Status.Progress -> iconPlaceHolder
                        PostAttachment.Status.Error -> iconError
                        else -> iconFallback
                    }
                    Glide.with(context).clear(imageView)
                    imageView.setImageDrawable(icon)
                }

                else -> {
                    Glide.with(context)
                        .load(imageUrl)
                        .placeholder(iconPlaceHolder)
                        .error(iconError)
                        .fallback(iconFallback)
                        .into(imageView)
                }
            }
        },
    )
}
