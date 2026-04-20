package es.ariaontheplanet.quasar.compose

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.pref.PrefF

// App-specific text styles that don't map to M3 Typography slots.
// Phase 1 bridgehead: additive infrastructure — nothing is wired in yet.
// Phase 1c will provide this via CompositionLocalProvider in StScreen/StThemedContent
// and start migrating call sites off hard-coded `fontSize = Nsp` literals.
@Immutable
data class StExtendedTypography(
    val timelineBody: TextStyle,
    val acct: TextStyle,
    val notificationTl: TextStyle,
    val columnHeader: TextStyle,
)

private fun Float.orDefault(fallback: Float): Float =
    if (isFinite() && this > 0f) this else fallback

private fun timelineFontFamily(): FontFamily {
    val tf = ActMain.timelineFont
    return if (tf === Typeface.DEFAULT) FontFamily.Default else FontFamily(tf)
}

private fun timelineFontFamilyBold(): FontFamily {
    val tf = ActMain.timelineFontBold
    return if (tf === Typeface.DEFAULT_BOLD) FontFamily.Default else FontFamily(tf)
}

@Composable
fun stExtendedTypography(): StExtendedTypography {
    val timelineSize = ActMain.timelineFontSizeSp.orDefault(PrefF.default_timeline_font_size)
    val acctSize = PrefF.fpAcctFontSize.value.orDefault(PrefF.default_acct_font_size)
    val notificationSize = PrefF.fpNotificationTlFontSize.value
        .orDefault(PrefF.default_notification_tl_font_size)
    val headerSize = PrefF.fpHeaderTextSize.value.orDefault(PrefF.default_header_font_size)

    val body = timelineFontFamily()
    val bold = timelineFontFamilyBold()

    return StExtendedTypography(
        timelineBody = TextStyle(fontFamily = body, fontSize = timelineSize.sp),
        acct = TextStyle(fontFamily = body, fontSize = acctSize.sp),
        notificationTl = TextStyle(fontFamily = body, fontSize = notificationSize.sp),
        columnHeader = TextStyle(
            fontFamily = bold,
            fontSize = headerSize.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

private val DefaultStExtendedTypography = StExtendedTypography(
    timelineBody = TextStyle(fontSize = PrefF.default_timeline_font_size.sp),
    acct = TextStyle(fontSize = PrefF.default_acct_font_size.sp),
    notificationTl = TextStyle(fontSize = PrefF.default_notification_tl_font_size.sp),
    columnHeader = TextStyle(
        fontSize = PrefF.default_header_font_size.sp,
        fontWeight = FontWeight.Bold,
    ),
)

val LocalStExtendedTypography = staticCompositionLocalOf { DefaultStExtendedTypography }
