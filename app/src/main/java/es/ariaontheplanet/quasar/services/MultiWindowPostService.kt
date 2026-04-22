package es.ariaontheplanet.quasar.services

import android.app.Activity
import android.content.Intent
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.lang.ref.WeakReference

/**
 * Cross-Activity signalling for the multi-window ActPost flow.
 *
 * Replaces the old WeakReference<ActMain> held by ActMainRegistry. ActMain
 * (still an Activity for now) subscribes to [completeEvents] to react to
 * posts completed in a detached ActPost window, and calls [closeAll] in
 * onDestroy to tear those windows down.
 */
class MultiWindowPostService {

    private val activityRefs = mutableListOf<WeakReference<Activity>>()

    private val _completeEvents = MutableSharedFlow<Intent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val completeEvents: SharedFlow<Intent> = _completeEvents

    fun register(activity: Activity) {
        // Prune dead refs opportunistically so the list doesn't grow forever.
        activityRefs.removeAll { it.get() == null }
        activityRefs += WeakReference(activity)
    }

    fun closeAll() {
        activityRefs.forEach {
            try {
                it.get()?.finish()
            } catch (ex: Throwable) {
                log.e(ex, "close failed?")
            }
        }
        activityRefs.clear()
    }

    fun emitComplete(intent: Intent) {
        if (!_completeEvents.tryEmit(intent)) {
            log.w("emitComplete buffer full, dropped intent")
        }
    }

    companion object {
        private val log = LogCategory("MultiWindowPostService")
    }
}
