package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.*
import jp.juggler.subwaytooter.api.entity.TootAttachment
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.*
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.*
import jp.juggler.util.log.*
import jp.juggler.util.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
        val bmp = bitmap
        if (bmp == null) {
            showToast(true, result.error ?: "error")
            return
        } else if (!isLiveActivity) {
            return
        }

        val dialog = Dialog(this)
        val activity = this
        val composeView = ComposeView(this).apply {
            setContent {
                StThemedContent {
                    FocusPointContent(
                        bitmap = bmp,
                        initialFocusX = attachment.focusX,
                        initialFocusY = attachment.focusY,
                        onOk = { fx, fy ->
                            launchMain {
                                try {
                                    if (callback(fx, fy)) {
                                        dialog.dismissSafe()
                                    }
                                } catch (ex: Throwable) {
                                    activity.showToast(ex, "can't set focus point.")
                                }
                            }
                        },
                        onCancel = { dialog.dismissSafe() },
                    )
                }
            }
        }
        dialog.setContentView(composeView)
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

@Composable
private fun FocusPointContent(
    bitmap: Bitmap,
    initialFocusX: Float,
    initialFocusY: Float,
    onOk: (Float, Float) -> Unit,
    onCancel: () -> Unit,
) {
    var focusX by remember { mutableFloatStateOf(if (initialFocusX.isNaN()) 0f else initialFocusX) }
    var focusY by remember { mutableFloatStateOf(if (initialFocusY.isNaN()) 0f else initialFocusY) }
    // Tick counter for crosshair color animation (cycles every 500ms)
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500L)
            tick++
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Canvas with bitmap + focus point crosshair
        FocusPointCanvas(
            bitmap = bitmap,
            focusX = focusX,
            focusY = focusY,
            tick = tick,
            onFocusChanged = { fx, fy ->
                focusX = fx
                focusY = fy
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
        )

        // Button bar
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
                onClick = { onOk(focusX, focusY) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    }
}

@Composable
private fun FocusPointCanvas(
    bitmap: Bitmap,
    focusX: Float,
    focusY: Float,
    tick: Long,
    onFocusChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val strokeWidth = 4f // dp equivalent handled by density in drawScope
    val circleRadius = 20f
    val crossRadius = 26f

    fun updateFocus(x: Float, y: Float, canvasWidth: Float, canvasHeight: Float,
                    drawX: Float, drawY: Float, drawW: Float, drawH: Float) {
        if (drawW <= 0f || drawH <= 0f) return
        val fx = (((x - drawX) / drawW) * 2f - 1f).clip(-1f, 1f)
        val fy = (((y - drawY) / drawH) * 2f - 1f).clip(-1f, 1f) * -1f
        onFocusChanged(fx, fy)
    }

    // We need to store draw geometry for pointer input
    var lastDrawX = remember { 0f }
    var lastDrawY = remember { 0f }
    var lastDrawW = remember { 0f }
    var lastDrawH = remember { 0f }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    updateFocus(offset.x, offset.y, size.width.toFloat(), size.height.toFloat(),
                        lastDrawX, lastDrawY, lastDrawW, lastDrawH)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    updateFocus(change.position.x, change.position.y,
                        size.width.toFloat(), size.height.toFloat(),
                        lastDrawX, lastDrawY, lastDrawW, lastDrawH)
                }
            }
    ) {
        val viewW = size.width
        val viewH = size.height
        if (viewW <= 0f || viewH <= 0f) return@Canvas

        val bitmapW = bitmap.width.toFloat()
        val bitmapH = bitmap.height.toFloat()
        if (bitmapW <= 0f || bitmapH <= 0f) return@Canvas

        val viewAspect = viewW / viewH
        val bitmapAspect = bitmapW / bitmapH

        val drawW: Float
        val drawH: Float
        if (bitmapAspect >= viewAspect) {
            drawW = viewW
            drawH = (viewW / bitmapW) * bitmapH
        } else {
            drawH = viewH
            drawW = (viewH / bitmapH) * bitmapW
        }
        val drawX = (viewW - drawW) * 0.5f
        val drawY = (viewH - drawH) * 0.5f

        // Store for pointer input
        lastDrawX = drawX
        lastDrawY = drawY
        lastDrawW = drawW
        lastDrawH = drawH

        // Draw bitmap
        drawImage(
            image = imageBitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
            dstSize = IntSize(drawW.toInt(), drawH.toInt()),
        )

        // Draw focus crosshair with cycling color
        @Suppress("KotlinConstantConditions")
        val crossColor = when (tick % 3) {
            2L -> Color.Red
            1L -> Color.Blue
            else -> Color.Green
        }
        val pointX = (focusX + 1f) * 0.5f * drawW + drawX
        val pointY = (-focusY + 1f) * 0.5f * drawH + drawY

        drawCircle(
            color = crossColor,
            radius = circleRadius,
            center = Offset(pointX, pointY),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = crossColor,
            start = Offset(pointX, pointY - crossRadius),
            end = Offset(pointX, pointY + crossRadius),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = crossColor,
            start = Offset(pointX - crossRadius, pointY),
            end = Offset(pointX + crossRadius, pointY),
            strokeWidth = strokeWidth,
        )
    }
}
