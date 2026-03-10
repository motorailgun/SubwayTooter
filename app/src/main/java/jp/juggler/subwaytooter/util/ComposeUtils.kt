package jp.juggler.subwaytooter.util

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import jp.juggler.subwaytooter.App1
import jp.juggler.subwaytooter.pref.PrefI

/**
 * Returns the Material3 ColorScheme for the current UI theme preference.
 * - Light (0): M3 default lightColorScheme
 * - Dark (1): M3 default darkColorScheme
 * - Mastodon (2): Custom dark scheme with Mastodon branding colors
 */
fun Activity.getStColorTheme(forceDark: Boolean = false): ColorScheme {
    App1.prepare(applicationContext, "getStColorTheme")
    var nTheme = PrefI.ipUiTheme.value
    if (forceDark && nTheme == 0) nTheme = 1
    return when (nTheme) {
        2 -> mastodonDarkColorScheme()
        1 -> darkColorScheme()
        else -> lightColorScheme()
    }
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

