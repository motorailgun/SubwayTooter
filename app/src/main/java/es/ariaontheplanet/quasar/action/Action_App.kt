package es.ariaontheplanet.quasar.action

import android.net.Uri
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.actmain.handleOtherUri
import es.ariaontheplanet.quasar.api.entity.TootApplication
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.dialog.DlgOpenUrl
import es.ariaontheplanet.quasar.table.daoMutedApp
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.dismissSafe

// Fixed-columns refactor: `fun ActMain.openColumnList()` used to launch ActColumnList
// via `arColumnList`. Both the launcher and the UI entry points are gone; the
// function would not compile and has no callers, so it is deleted.

// アプリをミュートする
fun ActMain.appMute(application: TootApplication?) = launchAndShowError {
    application ?: return@launchAndShowError
    confirm(R.string.mute_application_confirm, application.name)
    daoMutedApp.save(application.name)
    appState.onMuteUpdated()
    showToast(false, R.string.app_was_muted)
}

fun ActMain.openColumnFromUrl() {
    DlgOpenUrl.show(this) { dialog, url ->
        try {
            if (handleOtherUri(Uri.parse(url))) {
                dialog.dismissSafe()
            }
        } catch (ex: Throwable) {
            showToast(ex, R.string.url_parse_failed)
        }
    }
}
