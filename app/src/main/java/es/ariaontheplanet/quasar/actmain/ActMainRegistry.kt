package es.ariaontheplanet.quasar.actmain

import es.ariaontheplanet.quasar.ActMain
import java.lang.ref.WeakReference

// ActPost など他Activityから現在のActMainへ戻すための一時レジストリ。
// Phase 5 (Navigation-Compose) で Navigator に置き換えて撤廃する。
object ActMainRegistry {
    var ref: WeakReference<ActMain>? = null
    val current: ActMain? get() = ref?.get()
}
