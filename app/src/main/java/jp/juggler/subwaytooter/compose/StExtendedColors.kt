package jp.juggler.subwaytooter.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * App-specific semantic colors that don't map directly to Material3 ColorScheme slots.
 *
 * These are for action button accents and other app-unique visual semantics.
 * Standard M3 colors (background, surface, error, outline, etc.) should be accessed
 * via [androidx.compose.material3.MaterialTheme.colorScheme] instead.
 */
@Immutable
data class StExtendedColors(
    /** Accent color for the boost (reblog) button when active. */
    val buttonAccentBoost: Color,
    /** Accent color for the favourite button when active. */
    val buttonAccentFavourite: Color,
    /** Accent color for the bookmark button when active. */
    val buttonAccentBookmark: Color,
    /** Accent color for the follow button when active. */
    val buttonAccentFollow: Color,
    /** Accent color for a pending follow request. */
    val buttonAccentFollowRequest: Color,
    /** Accent color for the reaction button when active. */
    val buttonAccentReaction: Color,
    /** Background tint for the main (focused) toot in a conversation thread. */
    val conversationMainTootBg: Color,
)

val LightStExtendedColors = StExtendedColors(
    buttonAccentBoost = Color(0xFF0088FF),
    buttonAccentFavourite = Color(0xFF0088FF),
    buttonAccentBookmark = Color(0xFF0088FF),
    buttonAccentFollow = Color(0xFF0088FF),
    buttonAccentFollowRequest = Color(0xFFFF0000),
    buttonAccentReaction = Color(0xFF0088FF),
    conversationMainTootBg = Color(0x200088FF),
)

val DarkStExtendedColors = StExtendedColors(
    buttonAccentBoost = Color(0xFF00A2FF),
    buttonAccentFavourite = Color(0xFF00A2FF),
    buttonAccentBookmark = Color(0xFF00A2FF),
    buttonAccentFollow = Color(0xFF00A2FF),
    buttonAccentFollowRequest = Color(0xFFFF0000),
    buttonAccentReaction = Color(0xFF00A2FF),
    conversationMainTootBg = Color(0x2000A2FF),
)

val LocalStExtendedColors = staticCompositionLocalOf { LightStExtendedColors }

/**
 * Accessor for app-specific extended colors.
 *
 * Usage: `StThemeEx.colors.buttonAccentBoost`
 */
object StThemeEx {
    val colors: StExtendedColors
        @Composable
        get() = LocalStExtendedColors.current
}

/**
 * Returns the appropriate [StExtendedColors] based on system dark-mode state.
 */
@Composable
fun stExtendedColors(): StExtendedColors =
    if (isSystemInDarkTheme()) DarkStExtendedColors else LightStExtendedColors
