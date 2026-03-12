package jp.juggler.subwaytooter.actmain

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.util.TootColorConfig
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefF
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.subwaytooter.pref.impl.StringPref
import jp.juggler.subwaytooter.span.MyClickableSpan
import jp.juggler.subwaytooter.util.CustomShare
import jp.juggler.subwaytooter.view.ListDivider
import jp.juggler.util.data.clip
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.getAdaptiveRippleDrawableRound
import jp.juggler.util.ui.resDrawable
import jp.juggler.util.ui.wrapAndTint
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

fun ActMain.loadColumnMin() =
    (PrefS.spColumnWidth.value
        .toFloatOrNull()
        ?.takeIf { it.isFinite() && it >= 100f }
        ?: ActMain.COLUMN_WIDTH_MIN_DP.toFloat()
            ).dpToPx(this)

fun ActMain.justifyWindowContentPortrait() {
    when (PrefI.ipJustifyWindowContentPortrait.value) {
        PrefI.JWCP_START -> {
            val iconW = (ActMain.stripIconSize * 1.5f + 0.5f).toInt()
            val padding = resources.displayMetrics.widthPixels / 2 - iconW

            fun ViewGroup.addViewBeforeLast(v: View) = addView(v, childCount - 1)
            (views.svColumnStrip.parent as LinearLayout).addViewBeforeLast(
                View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(padding, 0)
                }
            )
            views.llQuickTootBar.addViewBeforeLast(
                View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(padding, 0)
                }
            )
        }

        PrefI.JWCP_END -> {
            val iconW = (ActMain.stripIconSize * 1.5f + 0.5f).toInt()
            val borderWidth = 1f.dpToPx(this)
            val padding = resources.displayMetrics.widthPixels / 2 - iconW - borderWidth

            fun ViewGroup.addViewAfterFirst(v: View) = addView(v, 1)
            (views.svColumnStrip.parent as LinearLayout).addViewAfterFirst(
                View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(padding, 0)
                }
            )
            views.llQuickTootBar.addViewAfterFirst(
                View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(padding, 0)
                }
            )
        }
    }
}

//////////////////////////////////////////////////////

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
    ListDivider.color = 0
    TabletColumnDivider.color = 0
    TootColorConfig.toot_color_unlisted = 0
    TootColorConfig.toot_color_follower = 0
    TootColorConfig.toot_color_direct_user = 0
    TootColorConfig.toot_color_direct_me = 0
    MyClickableSpan.showLinkUnderline = PrefB.bpShowLinkUnderline.value
    MyClickableSpan.defaultLinkColor = attrColor(androidx.appcompat.R.attr.colorPrimary)

    views.llFormRoot.setBackgroundColor(attrColor(MR.attr.colorSurface))

    CustomShare.reloadCache(this)
}

fun ActMain.showFooterColor() {
    val colorColumnStripBackground = attrColor(MR.attr.colorSurfaceContainer)
    val colorOnSurface = attrColor(MR.attr.colorOnSurface)

    views.svColumnStrip.setBackgroundColor(colorColumnStripBackground)
    views.llQuickTootBar.setBackgroundColor(colorColumnStripBackground)

    views.vBottomPadding.setBackgroundColor(colorColumnStripBackground)

    val colorButtonBg = colorColumnStripBackground

    views.btnMenu.background =
        getAdaptiveRippleDrawableRound(this, colorButtonBg, colorOnSurface)
    views.btnToot.background =
        getAdaptiveRippleDrawableRound(this, colorButtonBg, colorOnSurface)
    views.btnQuickToot.background =
        getAdaptiveRippleDrawableRound(this, colorButtonBg, colorOnSurface)
    views.ivQuickTootAccount.background =
        getAdaptiveRippleDrawableRound(this, colorButtonBg, colorOnSurface)
    views.btnQuickTootMenu.background =
        getAdaptiveRippleDrawableRound(this, colorButtonBg, colorOnSurface)

    val d = resDrawable(R.drawable.ic_question).wrapAndTint(color = colorOnSurface)
    views.ivQuickTootAccount.setDefaultImage(d)

    val csl = ColorStateList.valueOf(colorOnSurface)
    views.btnToot.imageTintList = csl
    views.btnMenu.imageTintList = csl
    views.btnQuickToot.imageTintList = csl
    views.btnQuickTootMenu.imageTintList = csl

    views.vFooterDivider1.setBackgroundColor(colorColumnStripBackground)
    views.vFooterDivider2.setBackgroundColor(colorColumnStripBackground)

    views.llColumnStrip.indicatorColor = attrColor(MR.attr.colorOnSurfaceVariant)
}

fun ActMain.closePopup() {
    // no-op: old StatusButtonsPopup removed
}
