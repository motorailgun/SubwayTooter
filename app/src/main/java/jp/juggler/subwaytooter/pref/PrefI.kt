package jp.juggler.subwaytooter.pref

import android.util.Log
import jp.juggler.subwaytooter.itemviewholder.AdditionalButtonsPosition
import jp.juggler.subwaytooter.pref.impl.IntPref

object PrefI {
    // int

    @Suppress("unused")
    const val BACK_ASK_ALWAYS = 0
    const val BACK_CLOSE_COLUMN = 1
    const val BACK_OPEN_COLUMN_LIST = 2
    const val BACK_EXIT_APP = 3

    val ipBackButtonAction = IntPref("back_button_action", BACK_CLOSE_COLUMN)

    val ipUiTheme = IntPref("ui_theme", 2, noRemove = true)

//	val ipResizeImage = IntPref("resize_image", 4)

    const val RC_SIMPLE = 0
    const val RC_ACTUAL = 1

    @Suppress("unused")
    const val RC_NONE = 2

    val ipRepliesCount = IntPref("RepliesCount", RC_SIMPLE)
    val ipBoostsCount = IntPref("BoostsCount", RC_ACTUAL)
    val ipFavouritesCount = IntPref("FavouritesCount", RC_ACTUAL)

    val ipRefreshAfterToot = IntPref("refresh_after_toot", 0)
    const val RAT_REFRESH_SCROLL = 0

    @Suppress("unused")
    const val RAT_REFRESH_DONT_SCROLL = 1
    const val RAT_DONT_REFRESH = 2

    @Suppress("unused")
    const val VS_BY_ACCOUNT = 0
    const val VS_MASTODON = 1
    const val VS_MISSKEY = 2
    val ipVisibilityStyle = IntPref("ipVisibilityStyle", VS_BY_ACCOUNT)

    val ipAdditionalButtonsPosition =
        IntPref("AdditionalButtonsPosition", AdditionalButtonsPosition.End.idx)

    val ipLastColumnPos = IntPref("last_column_pos", -1)
    val ipBoostButtonJustify = IntPref("ipBoostButtonJustify", 0) // 0=左,1=中央,2=右

    private const val JWCP_DEFAULT = 0
    const val JWCP_START = 1
    const val JWCP_END = 2
    val ipJustifyWindowContentPortrait =
        IntPref("JustifyWindowContentPortrait", JWCP_DEFAULT) // 0=default,1=start,2=end

    const val GSP_HEAD = 0
    private const val GSP_TAIL = 1
    val ipGapHeadScrollPosition = IntPref("GapHeadScrollPosition", GSP_TAIL)
    val ipGapTailScrollPosition = IntPref("GapTailScrollPosition", GSP_TAIL)

    val ipMediaBackground = IntPref("MediaBackground", 1)

    val ipLogSaveLevel = IntPref("LogSaveLevel", Log.WARN)

    const val EMOJI_WIDE_AUTO = 0
    const val EMOJI_WIDE_ENABLE = 1
    const val EMOJI_WIDE_DISABLE = 2
    val ipEmojiWideMode = IntPref("EmojiWideMode", EMOJI_WIDE_AUTO)
}
