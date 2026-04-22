package es.ariaontheplanet.quasar

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import es.ariaontheplanet.quasar.api.entity.TootAccount
import es.ariaontheplanet.quasar.api.entity.TootVisibility
import es.ariaontheplanet.quasar.pref.PrefI
import es.ariaontheplanet.quasar.pref.lazyContext
import es.ariaontheplanet.quasar.table.UserRelation
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.fixColor
import jp.juggler.util.ui.setIconDrawableId
import kotlin.math.min
import com.google.android.material.R as MR

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

// ActMainの初期化時に更新される
fun calcIconRound(wh: Int) = wh.toFloat() * 0.165f

fun calcIconRound(lp: ViewGroup.LayoutParams) =
    min(lp.width, lp.height).toFloat() * 0.165f

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
