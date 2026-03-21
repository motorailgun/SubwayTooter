package jp.juggler.subwaytooter.actmain

import android.text.Spannable
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.work.WorkManager
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.action.openColumnList
import jp.juggler.subwaytooter.api.entity.TootAccountRef
import jp.juggler.subwaytooter.api.entity.TootTag.Companion.findHashtagFromUrl
import jp.juggler.subwaytooter.appsetting.appSettingRoot
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.columnviewholder.ColumnViewHolder
// import jp.juggler.subwaytooter.columnviewholder.TabletColumnViewHolder
import jp.juggler.subwaytooter.pref.*
import jp.juggler.subwaytooter.push.PushWorker
import jp.juggler.subwaytooter.span.MyClickableSpan
import jp.juggler.subwaytooter.util.checkPrivacyPolicy
import jp.juggler.subwaytooter.util.openCustomTab
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
