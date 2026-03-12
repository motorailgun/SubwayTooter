package jp.juggler.subwaytooter.util

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import jp.juggler.subwaytooter.App1

/**
 * Composable function that returns the appropriate M3 ColorScheme.
 */
@Composable
fun stColorScheme(): ColorScheme {
    return themeToColorScheme(isSystemInDarkTheme())
}

/**
 * Non-composable version for use outside of @Composable scope
 */
fun Activity.getStColorTheme(): ColorScheme {
    App1.prepare(applicationContext, "getStColorTheme")
    val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return themeToColorScheme(isDark)
}

private fun themeToColorScheme(isDark: Boolean): ColorScheme = if (isDark) {
    mastodonDarkColorScheme()
} else {
    lightColorScheme()
}

/**
 * Mastodon-branded dark color scheme.
 */
private fun mastodonDarkColorScheme(): ColorScheme {
    val colorBackground = Color(0xFF282C37)
    val colorOnBackground = Color(0xFFDDDDDD)
    val colorSurface = Color(0xFF444B5D)
    val colorPrimary = Color(0xFF6185BA)
    val colorLink = Color(0xFF4E92D6)
    return darkColorScheme(
        background = colorBackground,
        onBackground = colorOnBackground,

        primary = colorPrimary,
        onPrimary = colorOnBackground,

        secondary = colorLink,
        onSecondary = colorOnBackground,

        surface = colorSurface,
        onSurface = colorOnBackground,
        onSurfaceVariant = colorOnBackground,
        surfaceVariant = Color(0xFF383838),

        error = Color(0xFFFF0000),

        outline = Color(0x66FFFFFF),
        outlineVariant = Color(0x66FFFFFF),
    )
}

fun ComponentActivity.fireBackPressed() =
    onBackPressedDispatcher.onBackPressed()

