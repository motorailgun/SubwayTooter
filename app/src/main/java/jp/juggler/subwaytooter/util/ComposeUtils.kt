package jp.juggler.subwaytooter.util

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import jp.juggler.subwaytooter.App1
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.pref.lazyPref

/**
 * Composable function that observes the UI theme preference reactively.
 *
 * Returns the appropriate M3 ColorScheme based on the current theme setting,
 * and automatically recomposes when the preference changes (no app restart needed).
 */
@Composable
fun stColorScheme(): ColorScheme {
    val themeState = remember { mutableIntStateOf(PrefI.ipUiTheme.value) }
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrefI.ipUiTheme.key) {
                themeState.intValue = PrefI.ipUiTheme.value
            }
        }
        lazyPref.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            lazyPref.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return themeToColorScheme(themeState.intValue)
}

/**
 * Non-composable version for use outside of @Composable scope
 * (e.g., ViewModel data processing, View-based dialogs).
 */
fun Activity.getStColorTheme(): ColorScheme {
    App1.prepare(applicationContext, "getStColorTheme")
    return themeToColorScheme(PrefI.ipUiTheme.value)
}

private fun themeToColorScheme(nTheme: Int): ColorScheme = when (nTheme) {
    2 -> mastodonDarkColorScheme()
    1 -> darkColorScheme()
    else -> lightColorScheme()
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

