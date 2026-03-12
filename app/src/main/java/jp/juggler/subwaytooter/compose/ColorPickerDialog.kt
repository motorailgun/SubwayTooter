package jp.juggler.subwaytooter.compose

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R

/**
 * Preset color palette for the color picker.
 * Material Design colors covering common use cases.
 */
private val presetColors = listOf(
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
 * A simple color picker dialog with preset colors and optional hex input.
 *
 * @param colorInitial The initial color (ARGB int).
 * @param alphaEnabled Whether the alpha slider is shown.
 * @param onDismiss Called when the dialog is dismissed without selection.
 * @param onColorSelected Called with the selected ARGB int color.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    colorInitial: Int,
    alphaEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    var selectedColor by remember { mutableIntStateOf(colorInitial) }
    var alpha by remember {
        mutableFloatStateOf(
            if (alphaEnabled) AndroidColor.alpha(colorInitial) / 255f else 1f
        )
    }
    var hexInput by remember {
        mutableStateOf(
            if (alphaEnabled) {
                "%08X".format(colorInitial)
            } else {
                "%06X".format(colorInitial and 0xFFFFFF)
            }
        )
    }

    fun currentColor(): Int {
        val rgb = selectedColor and 0xFFFFFF
        return if (alphaEnabled) {
            val a = (alpha * 255).toInt().coerceIn(0, 255)
            (a shl 24) or rgb
        } else {
            0xFF000000.toInt() or rgb
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(currentColor()))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(12.dp))

                // Color grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (color in presetColors) {
                        val isSelected = (color and 0xFFFFFF) == (selectedColor and 0xFFFFFF)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF000000.toInt() or (color and 0xFFFFFF)))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        )
                                    }
                                )
                                .clickable {
                                    selectedColor = color
                                    hexInput = if (alphaEnabled) {
                                        "%08X".format(currentColor())
                                    } else {
                                        "%06X".format(color and 0xFFFFFF)
                                    }
                                },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Hex input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("#", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(4.dp))
                    TextField(
                        value = hexInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isLetterOrDigit() }
                                .take(if (alphaEnabled) 8 else 6)
                            hexInput = filtered
                            try {
                                val parsed = filtered.toLong(16).toInt()
                                if (alphaEnabled && filtered.length == 8) {
                                    alpha = AndroidColor.alpha(parsed) / 255f
                                    selectedColor = parsed
                                } else if (!alphaEnabled && filtered.length == 6) {
                                    selectedColor = 0xFF000000.toInt() or parsed
                                }
                            } catch (_: NumberFormatException) {
                                // ignore invalid hex
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Alpha slider
                if (alphaEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Alpha: ${(alpha * 255).toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Slider(
                        value = alpha,
                        onValueChange = {
                            alpha = it
                            hexInput = "%08X".format(currentColor())
                        },
                        valueRange = 0f..1f,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor()) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
