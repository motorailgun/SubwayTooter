package es.ariaontheplanet.quasar.compose

import androidx.compose.ui.graphics.Color

// App-level palette tokens. Values mirror res/values/colors.xml so the Compose
// side can drop the XML custom attrs in a later pass without the values drifting.
internal object StColorTokens {
    // Light + Dark shared accent family
    val AccentBlue = Color(0xFF0088FF)
    val AccentBlueBright = Color(0xFF00A2FF)
    val AccentBlueTint = Color(0x200088FF)
    val AccentBlueBrightTint = Color(0x2000A2FF)

    // Mastodon branded accents
    val MastodonBookmark = Color(0xFF9A151A)
    val MastodonBoost = Color(0xFF158297)
    val MastodonFavourite = Color(0xFFA8AB16)
    val MastodonFollow = Color(0xFF06D3C4)
    val MastodonReaction = Color(0xFF99AEC7)
    val MastodonBoostTint = Color(0x20158297)

    // Shared semantic
    val SemanticAlert = Color(0xFFFF0000)
}
