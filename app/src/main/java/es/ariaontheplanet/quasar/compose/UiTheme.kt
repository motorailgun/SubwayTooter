package es.ariaontheplanet.quasar.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

// Single source of truth for the active theme.
// stColorScheme() and stExtendedColors() both branch on this so the two halves
// cannot drift. Phase 3 will point this at a user preference via AppPreferences.
enum class UiTheme { Light, Dark, Mastodon }

@Composable
fun currentUiTheme(): UiTheme =
    if (isSystemInDarkTheme()) UiTheme.Dark else UiTheme.Light
