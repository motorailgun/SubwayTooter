package jp.juggler.subwaytooter

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Web
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import jp.juggler.subwaytooter.api.entity.TootAccount
import jp.juggler.subwaytooter.api.entity.TootVisibility
import jp.juggler.subwaytooter.emoji.EmojiMap
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.pref.lazyContext
import jp.juggler.subwaytooter.span.EmojiImageSpan
import jp.juggler.subwaytooter.span.createSpan
import jp.juggler.subwaytooter.table.UserRelation
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.fixColor
import jp.juggler.util.ui.mixColor
import jp.juggler.util.ui.scan
import jp.juggler.util.ui.setIconDrawableId
import kotlin.math.max
import kotlin.math.min
import com.google.android.material.R as MR

private val log = LogCategory("Styler")

fun defaultColorIcon(context: Context, iconId: Int): Drawable? =
    ContextCompat.getDrawable(context, iconId)?.also {
        it.setTint(context.attrColor(MR.attr.colorOnSurface))
        it.setTintMode(PorterDuff.Mode.SRC_IN)
    }

fun TootVisibility.getVisibilityIconId(isMisskeyData: Boolean): ImageVector {
    val isMisskey = when (PrefI.ipVisibilityStyle.value) {
        PrefI.VS_MASTODON -> false
        PrefI.VS_MISSKEY -> true
        else -> isMisskeyData
    }
    return when {
        isMisskey -> when (this) {
            TootVisibility.Public -> Icons.Filled.Public
            TootVisibility.UnlistedHome -> Icons.Outlined.Home
            TootVisibility.PrivateFollowers -> Icons.Outlined.LockOpen
            TootVisibility.DirectSpecified -> Icons.Outlined.Mail
            TootVisibility.DirectPrivate -> Icons.Outlined.Lock
            TootVisibility.WebSetting -> Icons.Outlined.Web
            TootVisibility.AccountSetting -> Icons.Outlined.QuestionMark

            TootVisibility.LocalPublic -> Icons.Outlined.Apartment
            TootVisibility.LocalHome -> Icons.Outlined.Home
            TootVisibility.LocalFollowers -> Icons.Outlined.LockOpen

            TootVisibility.Unknown -> Icons.Outlined.QuestionMark
            TootVisibility.Limited -> Icons.Outlined.AccountCircle
            TootVisibility.Mutual -> Icons.AutoMirrored.Outlined.CompareArrows
        }

        else -> when (this) {
            TootVisibility.Public -> Icons.Filled.Public
            TootVisibility.UnlistedHome -> Icons.Outlined.Home
            TootVisibility.PrivateFollowers -> Icons.Outlined.LockOpen
            TootVisibility.DirectSpecified -> Icons.Outlined.Mail
            TootVisibility.DirectPrivate -> Icons.Outlined.Lock
            TootVisibility.WebSetting -> Icons.Outlined.Web
            TootVisibility.AccountSetting -> Icons.Outlined.QuestionMark

            TootVisibility.LocalPublic -> Icons.Outlined.Apartment
            TootVisibility.LocalHome -> Icons.Outlined.Home
            TootVisibility.LocalFollowers -> Icons.Outlined.LockOpen

            TootVisibility.Unknown -> Icons.Outlined.QuestionMark
            TootVisibility.Limited -> Icons.Outlined.AccountCircle
            TootVisibility.Mutual -> Icons.AutoMirrored.Outlined.CompareArrows
        }
    }
}

fun TootVisibility.getVisibilityString(isMisskeyData: Boolean): String {
    val isMisskey = when (PrefI.ipVisibilityStyle.value) {
        PrefI.VS_MASTODON -> false
        PrefI.VS_MISSKEY -> true
        else -> isMisskeyData
    }
    return lazyContext.getString(
        when {
            isMisskey -> when (this) {
                TootVisibility.Public -> R.string.visibility_public
                TootVisibility.UnlistedHome -> R.string.visibility_home
                TootVisibility.PrivateFollowers -> R.string.visibility_followers
                TootVisibility.DirectSpecified -> R.string.visibility_direct
                TootVisibility.DirectPrivate -> R.string.visibility_private
                TootVisibility.WebSetting -> R.string.visibility_web_setting
                TootVisibility.AccountSetting -> R.string.visibility_account_setting

                TootVisibility.LocalPublic -> R.string.visibility_local_public
                TootVisibility.LocalHome -> R.string.visibility_local_home
                TootVisibility.LocalFollowers -> R.string.visibility_local_followers

                TootVisibility.Unknown -> R.string.visibility_unknown
                TootVisibility.Limited -> R.string.visibility_limited
                TootVisibility.Mutual -> R.string.visibility_mutual
            }

            else -> when (this) {
                TootVisibility.Public -> R.string.visibility_public
                TootVisibility.UnlistedHome -> R.string.visibility_unlisted
                TootVisibility.PrivateFollowers -> R.string.visibility_followers
                TootVisibility.DirectSpecified -> R.string.visibility_direct
                TootVisibility.DirectPrivate -> R.string.visibility_direct
                TootVisibility.WebSetting -> R.string.visibility_web_setting
                TootVisibility.AccountSetting -> R.string.visibility_account_setting

                TootVisibility.LocalPublic -> R.string.visibility_local_public
                TootVisibility.LocalHome -> R.string.visibility_local_unlisted
                TootVisibility.LocalFollowers -> R.string.visibility_local_followers

                TootVisibility.Unknown -> R.string.visibility_unknown
                TootVisibility.Limited -> R.string.visibility_limited
                TootVisibility.Mutual -> R.string.visibility_mutual
            }
        }
    )
}

// アイコン付きの装飾テキストを返す
fun getVisibilityCaption(
    context: Context,
    isMisskeyData: Boolean,
    visibility: TootVisibility,
): CharSequence {
    val sb = SpannableStringBuilder()

    // removed for now

    return sb
}

fun setFollowIcon(
    context: Context,
    ibFollow: ImageButton,
    ivDot: ImageView,
    relation: UserRelation,
    who: TootAccount,
    defaultColor: Int,
    alphaMultiplier: Float,
) {
    val colorFollowed = context.attrColor(R.attr.colorButtonAccentFollow)

    val colorFollowRequest = context.attrColor(R.attr.colorButtonAccentFollowRequest)

    val colorError = context.attrColor(androidx.appcompat.R.attr.colorError)

    // 被フォロー状態
    when {

        relation.blocked_by -> {
            ivDot.visibility = View.VISIBLE
            setIconDrawableId(
                context,
                ivDot,
                R.drawable.ic_blocked_by,
                color = colorError,
                alphaMultiplier = alphaMultiplier
            )
        }

        relation.requested_by -> {
            ivDot.visibility = View.VISIBLE
            setIconDrawableId(
                context,
                ivDot,
                R.drawable.ic_requested_by,
                color = colorFollowRequest,
                alphaMultiplier = alphaMultiplier
            )
        }

        relation.followed_by -> {
            ivDot.visibility = View.VISIBLE
            setIconDrawableId(
                context,
                ivDot,
                R.drawable.ic_follow_dot,
                color = colorFollowed,
                alphaMultiplier = alphaMultiplier
            )
            // 被フォローリクエスト状態の時に followed_by が 真と偽の両方がありえるようなので
            // Relationshipだけを見ても被フォローリクエスト状態は分からないっぽい
            // 仕方ないので馬鹿正直に「 followed_byが真ならバッジをつける」しかできない
        }

        else -> {
            ivDot.visibility = View.GONE
        }
    }

    // フォローボタン
    // follow button
    val color: Int
    val iconId: Int
    val contentDescription: String

    when {
        relation.blocking -> {
            iconId = R.drawable.ic_block
            color = defaultColor
            contentDescription = context.getString(R.string.follow)
        }

        relation.muting -> {
            iconId = R.drawable.ic_volume_off
            color = defaultColor
            contentDescription = context.getString(R.string.follow)
        }

        relation.getFollowing(who) -> {
            iconId = R.drawable.ic_follow_cross
            color = colorFollowed
            contentDescription = context.getString(R.string.unfollow)
        }

        relation.getRequested(who) -> {
            iconId = R.drawable.ic_follow_wait
            color = colorFollowRequest
            contentDescription = context.getString(R.string.unfollow)
        }

        else -> {
            iconId = R.drawable.ic_follow_plus
            color = defaultColor
            contentDescription = context.getString(R.string.follow)
        }
    }

    setIconDrawableId(
        context,
        ibFollow,
        iconId,
        color = color,
        alphaMultiplier = alphaMultiplier
    )
    ibFollow.contentDescription = contentDescription
}

private fun getHorizontalPadding(v: View, dpDelta: Float): Int {
    // Essential Phone PH-1は 短辺439dp
    val formWidthMax = 460f
    val dm = v.resources.displayMetrics
    val screenW = dm.widthPixels
    val contentW = (0.5f + formWidthMax * dm.density).toInt()
    val padW = max(0, (screenW - contentW) / 2)
    return padW + (0.5f + dpDelta * dm.density).toInt()
}

private fun getOrientationString(orientation: Int?) = when (orientation) {
    null -> "null"
    Configuration.ORIENTATION_LANDSCAPE -> "landscape"
    Configuration.ORIENTATION_PORTRAIT -> "portrait"
    Configuration.ORIENTATION_UNDEFINED -> "undefined"
    else -> orientation.toString()
}

fun fixHorizontalPadding(v: View, dpDelta: Float = 12f) {
    val padT = v.paddingTop
    val padB = v.paddingBottom

    val dm = v.resources.displayMetrics
    val widthDp = dm.widthPixels / dm.density
    if (widthDp >= 640f && v.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
        val padLr = (0.5f + dpDelta * dm.density).toInt()
        when (PrefI.ipJustifyWindowContentPortrait.value) {
            PrefI.JWCP_START -> {
                v.setPaddingRelative(padLr, padT, padLr + dm.widthPixels / 2, padB)
                return
            }

            PrefI.JWCP_END -> {
                v.setPaddingRelative(padLr + dm.widthPixels / 2, padT, padLr, padB)
                return
            }

            else -> Unit
        }
    }

    val padLr = getHorizontalPadding(v, dpDelta)
    v.setPaddingRelative(padLr, padT, padLr, padB)
}

fun fixHorizontalMargin(v: View) {
    val lp = v.layoutParams
    if (lp is ViewGroup.MarginLayoutParams) {

        val dm = v.resources.displayMetrics
        val orientationString = getOrientationString(v.resources?.configuration?.orientation)
        val widthDp = dm.widthPixels / dm.density
        log.d("fixHorizontalMargin: orientation=$orientationString, w=${widthDp}dp, h=${dm.heightPixels / dm.density}")

        if (widthDp >= 640f && v.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
            when (PrefI.ipJustifyWindowContentPortrait.value) {
                PrefI.JWCP_START -> {
                    lp.marginStart = 0
                    lp.marginEnd = dm.widthPixels / 2
                    return
                }

                PrefI.JWCP_END -> {
                    lp.marginStart = dm.widthPixels / 2
                    lp.marginEnd = 0
                    return
                }
            }
        }

        val padLr = getHorizontalPadding(v, 0f)
        lp.leftMargin = padLr
        lp.rightMargin = padLr
    }
}

// ActMainの初期化時に更新される
fun calcIconRound(wh: Int) = wh.toFloat() * 0.165f

fun calcIconRound(lp: ViewGroup.LayoutParams) =
    min(lp.width, lp.height).toFloat() * 0.165f

fun SpannableStringBuilder.appendColorShadeIcon(
    context: Context,
    @DrawableRes drawableId: Int,
    text: String,
    color: Int? = null,
): SpannableStringBuilder {
    val start = this.length
    this.append(text)
    val end = this.length
    this.setSpan(
        EmojiImageSpan(context, drawableId, useColorShader = true, color = color),
        start,
        end,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return this
}

fun SpannableStringBuilder.appendMisskeyReaction(
    context: Context,
    emojiUtf16: String,
    text: String,
): SpannableStringBuilder {

    val emoji = EmojiMap.unicodeMap[emojiUtf16]
    when {
        emoji == null ->
            append("text")

        PrefB.bpUseTwemoji.value -> {
            val start = this.length
            append(text)
            val end = this.length
            this.setSpan(
                emoji.createSpan(context),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        else ->
            this.append(emoji.unifiedCode)
    }
    return this
}

fun Context.setSwitchColor(root: View?) {
    root ?: return
    val colorBg = attrColor(MR.attr.colorSurface)
    val colorOff = attrColor(MR.attr.colorOutline)
    val colorOn = android.graphics.Color.BLACK or 0x0080ff

    val colorDisabled = mixColor(colorBg, colorOff)

    val colorTrackDisabled = mixColor(colorBg, colorDisabled)
    val colorTrackOn = mixColor(colorBg, colorOn)
    val colorTrackOff = mixColor(colorBg, colorOff)

    // https://stackoverflow.com/a/25635526/9134243
    val thumbStates = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        ),
        intArrayOf(
            colorDisabled,
            colorOn,
            colorOff
        )
    )

    val trackStates = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        ),
        intArrayOf(
            colorTrackDisabled,
            colorTrackOn,
            colorTrackOff
        )
    )

    root.scan {
        (it as? SwitchCompat)?.apply {
            thumbTintList = thumbStates
            trackTintList = trackStates
        }
    }
}

fun ViewGroup.generateLayoutParamsEx(): ViewGroup.LayoutParams? =
    try {
        // Create MarginLayoutParams with MATCH_PARENT dimensions.
        // When added to a ViewGroup, Android will convert it to the correct
        // LayoutParams subclass via checkLayoutParams/generateLayoutParams.
        ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    } catch (ex: Throwable) {
        log.e(ex, "generateLayoutParamsEx failed")
        null
    }


fun ComponentActivity.enableEdgeToEdgeEx(forceDark: Boolean) {
    val colorBarBg = when{
        forceDark -> Color.BLACK
        else -> attrColor(MR.attr.colorSurface)
    }

    val barStyle = if (forceDark) {
        SystemBarStyle.dark(scrim = fixColor(src = colorBarBg, lExpect = 0f))
    } else {
        SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
    }
    
    enableEdgeToEdge(
        statusBarStyle = barStyle,
        navigationBarStyle = barStyle,
    )

    window?.apply{
        // API29以降でinsets部分の色を指定するのは基本的にコレ
        setBackgroundDrawable(ColorDrawable(colorBarBg))

        // 3ボタンナビゲーションで80% 不透明の背景が追加される挙動を無効化する
        if(Build.VERSION.SDK_INT >= 29){
            isNavigationBarContrastEnforced = false
        }
        // ステータスバーに80% 不透明の背景が追加される挙動を無効化する
        if(Build.VERSION.SDK_INT in 29..34 ){
            @Suppress("DEPRECATION")
            isStatusBarContrastEnforced = false
        }
    }
}
