package jp.juggler.subwaytooter.actpost

import jp.juggler.subwaytooter.ActPost
import jp.juggler.util.data.notEmpty

private fun Double?.finiteOrZero(): Double = if (this?.isFinite() == true) this else 0.0

fun ActPost.showPoll() {
    // no-op: poll section visibility is controlled by Compose state.
}

// 投票が有効で何か入力済みなら真
fun ActPost.hasPoll(): Boolean {
    if (pollTypeIndex <= 0) return false
    return etChoices.any { it.text.toString().isNotBlank() }
}

fun ActPost.pollChoiceList() = ArrayList<String>().apply {
    for (et in etChoices) {
        et.text.toString().trim { it <= ' ' }.notEmpty()?.let { add(it) }
    }
}

fun ActPost.pollExpireSeconds(): Int {
    val d = views.etExpireDays.text.toString().trim().toDoubleOrNull().finiteOrZero()
    val h = views.etExpireHours.text.toString().trim().toDoubleOrNull().finiteOrZero()
    val m = views.etExpireMinutes.text.toString().trim().toDoubleOrNull().finiteOrZero()
    return (d * 86400.0 + h * 3600.0 + m * 60.0).toInt()
}
