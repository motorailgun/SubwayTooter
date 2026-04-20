package es.ariaontheplanet.quasar.compose

import androidx.compose.ui.graphics.Color

// App-level palette tokens.
// StExtendedColors reads from here; Phase 1 will add a Mastodon variant
// using Mastodon_colorButtonAccent* values from res/values/colors.xml.
internal object StColorTokens {
    val AccentBlue = Color(0xFF0088FF)
    val AccentBlueBright = Color(0xFF00A2FF)
    val SemanticAlert = Color(0xFFFF0000)
    val AccentBlueTint = Color(0x200088FF)
    val AccentBlueBrightTint = Color(0x2000A2FF)
}
