package es.ariaontheplanet.quasar.services

import android.os.SystemClock

enum class DedupMode {
    None,
    RecentExpire,
    Recent,
}

class DedupItem(
    val text: String,
    var time: Long = SystemClock.elapsedRealtime(),
)
