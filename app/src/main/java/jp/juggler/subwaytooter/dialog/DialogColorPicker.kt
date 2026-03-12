package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ImageView
import androidx.annotation.ColorInt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
    fun dp(v: Int) = (v * density + 0.5f).toInt()

    var selectedColor = colorInitial ?: Color.BLACK
    var currentAlpha = if (alphaEnabled) Color.alpha(selectedColor) else 255

    val previewView = ImageView(activity).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        )
        scaleType = ImageView.ScaleType.FIT_XY
    }

    fun updatePreview() {
        val rgb = selectedColor and 0xFFFFFF
        val color = if (alphaEnabled) (currentAlpha shl 24) or rgb
        else 0xFF000000.toInt() or rgb
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(color)
            setStroke(dp(1), Color.GRAY)
        }
        previewView.setImageDrawable(drawable)
    }

    val hexInput = EditText(activity).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        hint = if (alphaEnabled) "AARRGGBB" else "RRGGBB"
        setText(
            if (alphaEnabled) "%08X".format(selectedColor)
            else "%06X".format(selectedColor and 0xFFFFFF)
        )
        isSingleLine = true
        setPadding(dp(8), dp(4), dp(8), dp(4))
    }

    val gridLayout = GridLayout(activity).apply {
        columnCount = 6
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun updateHexFromColor() {
        val rgb = selectedColor and 0xFFFFFF
        hexInput.setText(
            if (alphaEnabled) "%08X".format((currentAlpha shl 24) or rgb)
            else "%06X".format(rgb)
        )
    }

    for (color in PRESET_COLORS) {
        val swatch = FrameLayout(activity).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = dp(40)
                height = dp(40)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(dp(1), Color.GRAY)
            }
            background = drawable
            setOnClickListener {
                selectedColor = color
                updateHexFromColor()
                updatePreview()
            }
        }
        gridLayout.addView(swatch)
    }

    val hexLabel = TextView(activity).apply {
        text = "#"
        textSize = 16f
        setPadding(dp(8), 0, dp(4), 0)
    }
    val hexRow = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) }
        addView(hexLabel)
        addView(hexInput)
    }

    hexInput.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) {
            try {
                val text = s?.toString() ?: return
                val parsed = text.toLong(16).toInt()
                if (alphaEnabled && text.length == 8) {
                    currentAlpha = Color.alpha(parsed)
                    selectedColor = parsed
                    updatePreview()
                } else if (!alphaEnabled && text.length == 6) {
                    selectedColor = 0xFF000000.toInt() or parsed
                    updatePreview()
                }
            } catch (_: NumberFormatException) {
            }
        }
    })

    val contentLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(8))
        addView(previewView)
        addView(LinearLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            orientation = LinearLayout.VERTICAL
            addView(gridLayout)
        })
        addView(hexRow)
    }

    if (alphaEnabled) {
        val alphaLabel = TextView(activity).apply {
            text = "Alpha: $currentAlpha"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        val alphaSeekBar = SeekBar(activity).apply {
            max = 255
            progress = currentAlpha
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        alphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                currentAlpha = progress
                alphaLabel.text = "Alpha: $currentAlpha"
                updateHexFromColor()
                updatePreview()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        contentLayout.addView(alphaLabel)
        contentLayout.addView(alphaSeekBar)
    }

    val scrollView = ScrollView(activity).apply {
        addView(contentLayout)
    }

    updatePreview()

    val dialog = AlertDialog.Builder(activity)
        .setView(scrollView)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            val rgb = selectedColor and 0xFFFFFF
            val result = if (alphaEnabled) (currentAlpha shl 24) or rgb
            else 0xFF000000.toInt() or rgb
            cont.resume(result)
        }
        .setNegativeButton(android.R.string.cancel) { d, _ ->
            d.dismiss()
        }
        .setOnCancelListener {
            if (cont.isActive) cont.cancel()
        }
        .create()

    cont.invokeOnCancellation {
        try { dialog.dismiss() } catch (_: Throwable) {}
    }

    dialog.show()
}
