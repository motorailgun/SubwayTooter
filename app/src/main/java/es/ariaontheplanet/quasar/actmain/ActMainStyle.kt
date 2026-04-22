package es.ariaontheplanet.quasar.actmain

import android.content.Context
import android.graphics.Typeface
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.util.TootColorConfig
import es.ariaontheplanet.quasar.pref.PrefB
import es.ariaontheplanet.quasar.pref.PrefF
import es.ariaontheplanet.quasar.pref.PrefS
import es.ariaontheplanet.quasar.pref.impl.StringPref
import es.ariaontheplanet.quasar.span.MyClickableSpan
import es.ariaontheplanet.quasar.util.CustomShare
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import java.util.*
import kotlin.math.max
import com.google.android.material.R as MR

private val log = LogCategory("ActMainStyle")

private fun Float.dpToPx(context: Context) =
    (this * context.resources.displayMetrics.density + 0.5f).toInt()

// initUIから呼ばれる
fun reloadFonts() {
    ActMain.timelineFont = PrefS.spTimelineFont.value.notEmpty()?.let {
        try {
            Typeface.createFromFile(it)
        } catch (ex: Throwable) {
            log.e(ex, "timelineFont load failed")
            null
        }
    } ?: Typeface.DEFAULT

    ActMain.timelineFontBold = PrefS.spTimelineFontBold.value.notEmpty()?.let {
        try {
            Typeface.createFromFile(it)
        } catch (ex: Throwable) {
            log.e(ex, "timelineFontBold load failed")
            null
        }
    } ?: try {
        Typeface.create(ActMain.timelineFont, Typeface.BOLD)
    } catch (ex: Throwable) {
        log.e(ex, "timelineFontBold create from timelineFont failed")
        null
    } ?: Typeface.DEFAULT_BOLD
}

private fun ActMain.parseIconSize(stringPref: StringPref, minDp: Float = 1f) =
    (try {
        stringPref.value
            .toFloatOrNull()
            ?.takeIf { it.isFinite() && it >= minDp }
    } catch (ex: Throwable) {
        log.e(ex, "parseIconSize failed.")
        null
    } ?: stringPref.defVal.toFloat()).dpToPx(this)

// initUIから呼ばれる
fun ActMain.reloadIconSize() {
    avatarIconSize = parseIconSize(PrefS.spAvatarIconSize)
    notificationTlIconSize = parseIconSize(PrefS.spNotificationTlIconSize)
    ActMain.boostButtonSize = parseIconSize(PrefS.spBoostButtonSize)
    ActMain.replyIconSize = parseIconSize(PrefS.spReplyIconSize)
    ActMain.headerIconSize = parseIconSize(PrefS.spHeaderIconSize)
    ActMain.stripIconSize = parseIconSize(PrefS.spStripIconSize)
    ActMain.screenBottomPadding = parseIconSize(PrefS.spScreenBottomPadding, minDp = 0f)

    ActMain.eventFadeAlpha = 1f
}

fun ActMain.reloadMediaHeight() {
    appState.mediaThumbHeight = (
            PrefS.spMediaThumbHeight.value
                .toFloatOrNull()
                ?.takeIf { it >= 32f }
                ?: 64f
            ).dpToPx(this)
}

private fun Float.clipFontSize(): Float =
    if (isNaN()) this else max(1f, this)

fun ActMain.reloadTextSize() {
    ActMain.timelineFontSizeSp = PrefF.fpTimelineFontSize.value.clipFontSize()
    acctFontSizeSp = PrefF.fpAcctFontSize.value.clipFontSize()
    notificationTlFontSizeSp = PrefF.fpNotificationTlFontSize.value.clipFontSize()
    headerTextSizeSp = PrefF.fpHeaderTextSize.value.clipFontSize()
    val fv = PrefS.spTimelineSpacing.value.toFloatOrNull()
    ActMain.timelineSpacing = if (fv != null && fv.isFinite() && fv != 0f) fv else null
}

// onStart時に呼ばれる
fun reloadTimeZone() {
    try {
        var tz = TimeZone.getDefault()
        val tzId = PrefS.spTimeZone.value
        if (tzId.isNotEmpty()) {
            tz = TimeZone.getTimeZone(tzId)
        }
        log.w("reloadTimeZone: tz=${tz.displayName}")
        TootStatus.dateFormatFull.timeZone = tz
    } catch (ex: Throwable) {
        log.e(ex, "getTimeZone failed.")
    }
}

// onStart時に呼ばれる
// カラーカスタマイズを読み直す
fun ActMain.reloadColors() {
    // TabletColumnDivider.color = 0 (Removed)
    TootColorConfig.toot_color_unlisted = 0
    TootColorConfig.toot_color_follower = 0
    TootColorConfig.toot_color_direct_user = 0
    TootColorConfig.toot_color_direct_me = 0
    MyClickableSpan.showLinkUnderline = PrefB.bpShowLinkUnderline.value
    MyClickableSpan.defaultLinkColor = attrColor(androidx.appcompat.R.attr.colorPrimary)

    // views.llFormRoot.setBackgroundColor(attrColor(MR.attr.colorSurface))

    CustomShare.reloadCache(this)
}
