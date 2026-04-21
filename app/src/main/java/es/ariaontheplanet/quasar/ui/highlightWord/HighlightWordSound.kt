package es.ariaontheplanet.quasar.ui.highlightWord

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import es.ariaontheplanet.quasar.table.HighlightWord
import jp.juggler.util.data.mayUri
import jp.juggler.util.log.LogCategory
import java.lang.ref.WeakReference

// One global ringtone slot shared by the highlight-word list and the edit
// screen. Previously lived on ActHighlightWordList.Companion; lifted to a
// file-level helper so the Activity could be deleted.
private val log = LogCategory("HighlightWordSound")

@Volatile
private var lastRingtone: WeakReference<Ringtone>? = null

fun stopLastHighlightRingtone() {
    try {
        lastRingtone?.get()?.stop()
    } catch (ex: Throwable) {
        log.e(ex, "stopLastHighlightRingtone failed.")
    } finally {
        lastRingtone = null
    }
}

private fun tryRingtone(context: Context, uri: Uri?): Boolean {
    try {
        uri?.let { RingtoneManager.getRingtone(context, it) }?.let { ringtone ->
            stopLastHighlightRingtone()
            lastRingtone = WeakReference(ringtone)
            ringtone.play()
            return true
        }
    } catch (ex: Throwable) {
        log.e(ex, "tryRingtone failed.")
    }
    return false
}

fun playHighlightSound(context: Context, item: HighlightWord?) {
    if (lastRingtone?.get()?.isPlaying == true) {
        stopLastHighlightRingtone()
        return
    }

    item ?: return
    when (item.sound_type) {
        HighlightWord.SOUND_TYPE_NONE -> Unit
        HighlightWord.SOUND_TYPE_CUSTOM -> {
            if (tryRingtone(context, item.sound_uri.mayUri())) return
            tryRingtone(
                context,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            )
        }
        else -> tryRingtone(
            context,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
    }
}
