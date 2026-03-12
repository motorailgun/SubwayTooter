package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.compose.StThemedContent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Material Design preset colors for the simple color picker.
 */
private val PRESET_COLORS = intArrayOf(
    0xFFF44336.toInt(), // Red
    0xFFE91E63.toInt(), // Pink
    0xFF9C27B0.toInt(), // Purple
    0xFF673AB7.toInt(), // Deep Purple
    0xFF3F51B5.toInt(), // Indigo
    0xFF2196F3.toInt(), // Blue
    0xFF03A9F4.toInt(), // Light Blue
    0xFF00BCD4.toInt(), // Cyan
    0xFF009688.toInt(), // Teal
    0xFF4CAF50.toInt(), // Green
    0xFF8BC34A.toInt(), // Light Green
    0xFFCDDC39.toInt(), // Lime
    0xFFFFEB3B.toInt(), // Yellow
    0xFFFFC107.toInt(), // Amber
    0xFFFF9800.toInt(), // Orange
    0xFFFF5722.toInt(), // Deep Orange
    0xFF795548.toInt(), // Brown
    0xFF9E9E9E.toInt(), // Grey
    0xFF607D8B.toInt(), // Blue Grey
    0xFF000000.toInt(), // Black
    0xFF333333.toInt(), // Dark Grey
    0xFF666666.toInt(), // Medium Grey
    0xFFCCCCCC.toInt(), // Light Grey
    0xFFFFFFFF.toInt(), // White
)

/**
 * Simple color picker dialog as a suspend function replacement for
 * com.jrummyapps.android.colorpicker.dialogColorPicker.
 *
 * Shows a grid of preset colors, a hex input, and an optional alpha slider.
 */
suspend fun Activity.dialogColorPicker(
    @ColorInt colorInitial: Int?,
    alphaEnabled: Boolean,
): Int = suspendCancellableCoroutine { cont ->
    val activity = this
    val density = resources.displayMetrics.density

    val dialog = Dialog(activity).apply {
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        setOnCancelListener {
            if (cont.isActive) cont.cancel()
        }
    }

    val composeView = ComposeView(activity).apply {
        setContent {
            StThemedContent {
                ColorPickerContent(
                    colorInitial = colorInitial ?: android.graphics.Color.BLACK,
                    alphaEnabled = alphaEnabled,
                    onOk = { color ->
                        dialog.dismiss()
                        if (cont.isActive) cont.resume(color)
                    },
                    onCancel = { dialog.cancel() },
                )
            }
        }
    }
    dialog.setContentView(composeView)
    dialog.window?.let { w ->
        w.attributes = w.attributes.apply {
            width = (0.5f + 280f * density).toInt()
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
    }

    cont.invokeOnCancellation {
        try {
            dialog.dismiss()
        } catch (_: Throwable) {
        }
    }

    dialog.show()
}

@Composable
private fun ColorPickerContent(
    @ColorInt colorInitial: Int,
    alphaEnabled: Boolean,
    onOk: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedColor by remember { mutableStateOf(colorInitial) }
    var currentAlpha by remember {
        mutableStateOf(if (alphaEnabled) android.graphics.Color.alpha(colorInitial) else 255)
    }
    var hexText by remember {
        mutableStateOf(
            if (alphaEnabled) "%08X".format(colorInitial)
            else "%06X".format(colorInitial and 0xFFFFFF)
        )
    }

    val previewColor = remember(selectedColor, currentAlpha) {
        val rgb = selectedColor and 0xFFFFFF
        if (alphaEnabled) Color((currentAlpha shl 24) or rgb)
        else Color(0xFF000000.toInt() or rgb)
    }

    fun updateHexFromColor() {
        val rgb = selectedColor and 0xFFFFFF
        hexText = if (alphaEnabled) "%08X".format((currentAlpha shl 24) or rgb)
        else "%06X".format(rgb)
    }

    Surface(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewColor)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            )

            Spacer(Modifier.height(12.dp))

            // Color grid (6 columns)
            val columns = 6
            val rows = (PRESET_COLORS.size + columns - 1) / columns
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < PRESET_COLORS.size) {
                            val color = PRESET_COLORS[index]
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        selectedColor = color
                                        updateHexFromColor()
                                    },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Hex input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#", fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text
                        try {
                            val parsed = text.toLong(16).toInt()
                            if (alphaEnabled && text.length == 8) {
                                currentAlpha = android.graphics.Color.alpha(parsed)
                                selectedColor = parsed
                            } else if (!alphaEnabled && text.length == 6) {
                                selectedColor = 0xFF000000.toInt() or parsed
                            }
                        } catch (_: NumberFormatException) {
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(if (alphaEnabled) "AARRGGBB" else "RRGGBB") },
                )
            }

            // Alpha slider
            if (alphaEnabled) {
                Spacer(Modifier.height(12.dp))
                Text("Alpha: $currentAlpha", fontSize = 14.sp)
                Slider(
                    value = currentAlpha.toFloat(),
                    onValueChange = {
                        currentAlpha = it.roundToInt()
                        updateHexFromColor()
                    },
                    valueRange = 0f..255f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(12.dp))

            // OK / Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val rgb = selectedColor and 0xFFFFFF
                    val result = if (alphaEnabled) (currentAlpha shl 24) or rgb
                    else 0xFF000000.toInt() or rgb
                    onOk(result)
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}
