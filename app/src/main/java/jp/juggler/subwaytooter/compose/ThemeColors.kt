package jp.juggler.subwaytooter.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.R as MR
import jp.juggler.util.ui.attrColor

/**
 * Theme colors extracted from the current theme attributes.
 * Used to avoid direct Activity dependency in ColumnViewHolder.
 */
data class ThemeColors(
    val colorOnSurface: Int,
    val colorSurfaceContainerLow: Int,
    val colorSurfaceContainerHigh: Int,
    val colorOnSurfaceVariant: Int,
)

/**
 * Composable function to extract and remember theme colors from the current context.
 */
@Composable
fun rememberThemeColors(): ThemeColors {
    val context = LocalContext.current
    return remember {
        ThemeColors(
            colorOnSurface = context.attrColor(MR.attr.colorOnSurface),
            colorSurfaceContainerLow = context.attrColor(MR.attr.colorSurfaceContainerLow),
            colorSurfaceContainerHigh = context.attrColor(MR.attr.colorSurfaceContainerHigh),
            colorOnSurfaceVariant = context.attrColor(MR.attr.colorOnSurfaceVariant),
        )
    }
}
