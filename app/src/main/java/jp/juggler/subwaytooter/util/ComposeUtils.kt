package jp.juggler.subwaytooter.util

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns the default Material3 [ColorScheme] based on system dark-mode state.
 * Used internally by StScreen / StThemedContent.
 */
@Composable
internal fun stColorScheme(): ColorScheme =
    if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

fun ComponentActivity.fireBackPressed() =
    onBackPressedDispatcher.onBackPressed()

