package jp.juggler.subwaytooter.pref

import androidx.annotation.StringRes
import jp.juggler.subwaytooter.R

enum class AdditionalButtonsPosition(
    val idx: Int, // spinner index start from 0
    @StringRes val captionId: Int,
) {
    Top(0, R.string.top),
    Bottom(1, R.string.bottom),
    Start(2, R.string.start),
    End(3, R.string.end),
    ;

    companion object {
        fun fromIndex(i: Int) = entries.find { it.idx == i } ?: Top
    }
}
