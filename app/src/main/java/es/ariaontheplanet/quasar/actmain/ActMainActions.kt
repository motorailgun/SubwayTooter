package es.ariaontheplanet.quasar.actmain

import android.text.Spannable
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.work.WorkManager
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.TootAccountRef
import es.ariaontheplanet.quasar.api.entity.TootTag.Companion.findHashtagFromUrl
import es.ariaontheplanet.quasar.appsetting.appSettingRoot
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.ColumnType
import es.ariaontheplanet.quasar.columnviewholder.ColumnViewHolder
// import es.ariaontheplanet.quasar.columnviewholder.TabletColumnViewHolder
import es.ariaontheplanet.quasar.pref.*
import es.ariaontheplanet.quasar.push.PushWorker
import es.ariaontheplanet.quasar.span.MyClickableSpan
import es.ariaontheplanet.quasar.util.checkPrivacyPolicy
import es.ariaontheplanet.quasar.util.openCustomTab
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.addTo
import jp.juggler.util.data.cast
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit

private val log = LogCategory("ActMainActions")

fun ActMain.onBackPressedImpl() {
    viewModel.onBackPressed()
}

fun ActMain.onMyClickableSpanClickedImpl(viewClicked: View, span: MyClickableSpan) {
    // ビュー階層を下から辿って文脈を取得する
    var column: Column? = null
    var whoRef: TootAccountRef? = null
    var view = viewClicked
    loop@ while (true) {
        when (val tag = view.tag) {
            is ColumnViewHolder -> {
                column = tag.column
                whoRef = null
                break@loop
            }

            is Column -> {
                column = tag
                whoRef = null
                break@loop
            }

            else -> when (val parent = view.parent) {
                is View -> view = parent
                else -> break@loop
            }
        }
    }

    val hashtagList = ArrayList<String>().apply {
        try {
            val cs = viewClicked.cast<TextView>()?.text
            if (cs is Spannable) {
                for (s in cs.getSpans(0, cs.length, MyClickableSpan::class.java)) {
                    val li = s.linkInfo
                    val pair = li.url.findHashtagFromUrl()
                    if (pair != null) add(if (li.text.startsWith('#')) li.text else "#${pair.first}")
                }
            }
        } catch (ex: Throwable) {
            log.e(ex, "can't create hashtagList")
        }
    }

    val linkInfo = span.linkInfo

    openCustomTab(
        this,
        nextPosition(column),
        linkInfo.url,
        accessInfo = column?.accessInfo,
        tagList = hashtagList.notEmpty(),
        whoRef = whoRef,
        linkInfo = linkInfo
    )
}

fun ActMain.launchDialogs() {
    launchAndShowError {
        // プライバシーポリシー
        val agreed = try {
            checkPrivacyPolicy()
        } catch (ex: Throwable) {
            log.e(ex, "checkPrivacyPolicy failed.")
            return@launchAndShowError
        }
        // 同意がないなら残りの何かは表示しない
        if (!agreed) return@launchAndShowError

        // 通知権限の確認を一度だけ行う
        if (!prefDevice.supressRequestNotificationPermission) {
            prefDevice.supressRequestNotificationPermission = true
            if (!prNotification.checkOrLaunch()) return@launchAndShowError
        }

        // 画面を作成したあと一度だけ行う
        // 画像ビューアから戻ってきたときなどは行わない
        if (!subscriptionUpdaterCalled) {
            subscriptionUpdaterCalled = true
            afterNotificationGranted()
        }
    }
}

suspend fun ActMain.afterNotificationGranted() {
    sideMenuAdapter.filterListItems()

    // Workの掃除
    WorkManager.getInstance(applicationContext).pruneWork()

    // 認証やアクセストークン更新から戻ってきた時に処理を重ねたくない
    delay(2000L)
    if (!accountVerifyMutex.isLocked) {
        // 定期的にendpointを再登録したい
        PushWorker.enqueueRegisterEndpoint(applicationContext, keepAliveMode = true)
    }
}
