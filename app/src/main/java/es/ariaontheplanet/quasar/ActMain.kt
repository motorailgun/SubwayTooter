package es.ariaontheplanet.quasar

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.activity.addCallback
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import es.ariaontheplanet.quasar.action.openColumnList
import es.ariaontheplanet.quasar.actmain.MainScreen
import es.ariaontheplanet.quasar.actmain.MainViewModel
import es.ariaontheplanet.quasar.actmain.isVisibleColumn
import es.ariaontheplanet.quasar.actmain.scrollToColumn
import es.ariaontheplanet.quasar.columnviewholder.scrollToTop2
import es.ariaontheplanet.quasar.util.provideViewModel
import kotlinx.coroutines.launch
import es.ariaontheplanet.quasar.action.accessTokenPrompt
import es.ariaontheplanet.quasar.action.timeline
import es.ariaontheplanet.quasar.actmain.SideMenuAdapter
import es.ariaontheplanet.quasar.actmain.afterNotificationGranted
import es.ariaontheplanet.quasar.actmain.closePopup
import es.ariaontheplanet.quasar.actmain.defaultInsertPosition
import es.ariaontheplanet.quasar.actmain.handleIntentUri
import es.ariaontheplanet.quasar.actmain.handleSharedIntent
import es.ariaontheplanet.quasar.actmain.importAppData
import es.ariaontheplanet.quasar.actmain.isOrderChanged
import es.ariaontheplanet.quasar.actmain.justifyWindowContentPortrait
import es.ariaontheplanet.quasar.actmain.launchDialogs
import es.ariaontheplanet.quasar.actmain.onBackPressedImpl
import es.ariaontheplanet.quasar.actmain.onCompleteActPost
import es.ariaontheplanet.quasar.actmain.onMyClickableSpanClickedImpl
import es.ariaontheplanet.quasar.actmain.refreshAfterPost
import es.ariaontheplanet.quasar.actmain.reloadAccountSetting
import es.ariaontheplanet.quasar.actmain.reloadColors
import es.ariaontheplanet.quasar.actmain.reloadFonts
import es.ariaontheplanet.quasar.actmain.reloadIconSize
import es.ariaontheplanet.quasar.actmain.reloadMediaHeight
import es.ariaontheplanet.quasar.actmain.reloadTextSize
import es.ariaontheplanet.quasar.action.openPost
import es.ariaontheplanet.quasar.actmain.reloadTimeZone
import es.ariaontheplanet.quasar.actmain.resizeColumnWidth
import es.ariaontheplanet.quasar.actmain.scrollColumnStrip
import es.ariaontheplanet.quasar.actmain.scrollToColumn
import es.ariaontheplanet.quasar.actmain.scrollToLastColumn
import es.ariaontheplanet.quasar.actmain.searchFromActivityResult
import es.ariaontheplanet.quasar.actmain.setColumnsOrder
import es.ariaontheplanet.quasar.actmain.showFooterColor
import es.ariaontheplanet.quasar.actmain.updateColumnStrip
import es.ariaontheplanet.quasar.actmain.updateColumnStripSelection
import es.ariaontheplanet.quasar.actpost.CompletionHelper
import es.ariaontheplanet.quasar.api.entity.Acct
import es.ariaontheplanet.quasar.api.entity.EntityId
import es.ariaontheplanet.quasar.api.entity.TootVisibility
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.ColumnLoadReason
import es.ariaontheplanet.quasar.column.ColumnType
import es.ariaontheplanet.quasar.column.fireColumnColor
import es.ariaontheplanet.quasar.column.fireRelativeTime
import es.ariaontheplanet.quasar.column.fireShowColumnHeader
import es.ariaontheplanet.quasar.column.fireShowContent
import es.ariaontheplanet.quasar.column.onActivityStart
import es.ariaontheplanet.quasar.column.onLanguageFilterChanged
import es.ariaontheplanet.quasar.column.saveScrollPosition
import es.ariaontheplanet.quasar.column.startLoading
import es.ariaontheplanet.quasar.column.viewHolder
import es.ariaontheplanet.quasar.dialog.DlgQuickTootMenu
import es.ariaontheplanet.quasar.notification.checkNotificationImmediateAll
import es.ariaontheplanet.quasar.pref.PrefB
import es.ariaontheplanet.quasar.pref.PrefI
import es.ariaontheplanet.quasar.pref.PrefS
import es.ariaontheplanet.quasar.span.MyClickableSpan
import es.ariaontheplanet.quasar.span.MyClickableSpanHandler
import es.ariaontheplanet.quasar.table.daoSavedAccount
import es.ariaontheplanet.quasar.ui.languageFilter.LanguageFilterActivity
import es.ariaontheplanet.quasar.util.DecodeOptions.Companion.reloadEmojiScale
import es.ariaontheplanet.quasar.util.EmojiDecoder
import es.ariaontheplanet.quasar.util.openBrowser
import es.ariaontheplanet.quasar.util.permissionSpecNotification
import es.ariaontheplanet.quasar.util.requester
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.anyArrayOf
import jp.juggler.util.data.notEmpty
import jp.juggler.util.int
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.benchmark
import jp.juggler.util.log.showToast
import jp.juggler.util.long
import jp.juggler.util.string
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.isNotOk
import jp.juggler.util.ui.setContentViewAndInsets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.LinkedList
import com.google.android.material.R as MR

class ActMain : ComponentActivity(),
    MyClickableSpanHandler {

    val viewModel by lazy {
        provideViewModel(this) { MainViewModel(application) }
    }

    val isDrawerOpen: Boolean
        get() = viewModel.isDrawerOpen.value

    fun openDrawer() {
        viewModel.openDrawer()
    }

    fun closeDrawer() {
        viewModel.closeDrawer()
    }

    companion object {
        private val log = LogCategory("ActMain")

        const val COLUMN_WIDTH_MIN_DP = 300

        const val STATE_CURRENT_PAGE = "current_page"

        const val RESULT_APP_DATA_IMPORT = Activity.RESULT_FIRST_USER

        // ActPostから参照される
        var refActMain: WeakReference<ActMain>? = null

        // 外部からインテントを受信した後、アカウント選択中に画面回転したらアカウント選択からやり直す
        internal var sharedIntent2: Intent? = null

        // アプリ設定のキャッシュ
        var boostButtonSize = 1
        var replyIconSize = 1
        var headerIconSize = 1
        var stripIconSize = 1
        var screenBottomPadding = 0
        var timelineFont: Typeface = Typeface.DEFAULT
        var timelineFontBold: Typeface = Typeface.DEFAULT_BOLD
        var eventFadeAlpha = 1f

        var timelineFontSizeSp = Float.NaN
        var timelineSpacing: Float? = null
    }

    // アプリ設定のキャッシュ
    var density = 0f
    var acctPadLr = 0
    var acctFontSizeSp = Float.NaN
    var notificationTlFontSizeSp = Float.NaN
    var headerTextSizeSp = Float.NaN
    var avatarIconSize: Int = 0
    var notificationTlIconSize: Int = 0

    // マルチウィンドウモードで子ウィンドウを閉じるのに使う
    val closeList = LinkedList<WeakReference<Activity>>()

    // onResume() .. onPause() の間なら真
    private var isResumed = false

    // onStart() .. onStop() の間なら真
    var isStartedEx = false

    // onActivityResultで設定されてonResumeで消化される
    // 状態保存の必要なし
    var postedAcct: Acct? = null // acctAscii
    var postedStatusId: EntityId? = null
    var postedReplyId: EntityId? = null
    var postedRedraftId: EntityId? = null

    // 画面上のUI操作で生成されて
    // onPause,onPageDestroy 等のタイミングで閉じられる
    // 状態保存の必要なし
    // (removed: popupStatusButtons)

    // var phoneViews: ActMainPhoneViews? = null
    // var tabletViews: ActMainTabletViews? = null

    var nScreenColumn: Int = 0
    var nColumnWidth: Int = 0 // dividerの幅を含む

    var nAutoCwCellWidth = 0
    var nAutoCwLines = 0

    var dlgPrivacyPolicy: WeakReference<Dialog>? = null

    /*
    val views by lazy {
         // Legacy views removed
    }
    */

    lateinit var completionHelper: CompletionHelper
    lateinit var handler: Handler
    lateinit var appState: AppState
    lateinit var sideMenuAdapter: SideMenuAdapter

    var subscriptionUpdaterCalled = false

    //////////////////////////////////////////////////////////////////
    // 読み取り専用のプロパティ

    private fun toastCallback(stringId: Int): () -> Unit =
        { showToast(false, stringId) }

    val followCompleteCallback = toastCallback(R.string.follow_succeeded)
    val unfollowCompleteCallback = toastCallback(R.string.unfollow_succeeded)
    val cancelFollowRequestCompleteCallback = toastCallback(R.string.follow_request_cancelled)
    val favouriteCompleteCallback = toastCallback(R.string.favourite_succeeded)
    val unfavouriteCompleteCallback = toastCallback(R.string.unfavourite_succeeded)
    val bookmarkCompleteCallback = toastCallback(R.string.bookmark_succeeded)
    val unbookmarkCompleteCallback = toastCallback(R.string.unbookmark_succeeded)
    val boostCompleteCallback = toastCallback(R.string.boost_succeeded)
    val unboostCompleteCallback = toastCallback(R.string.unboost_succeeded)
    val reactionCompleteCallback = toastCallback(R.string.reaction_succeeded)

    // 相対時刻の表記を定期的に更新する
    private val procUpdateRelativeTime = object : Runnable {
        override fun run() {
            handler.removeCallbacks(this)
            if (!isStartedEx) return
            if (PrefB.bpRelativeTimestamp.value) {
                appState.columnList.forEach { it.fireRelativeTime() }
                handler.postDelayed(this, 10000L)
            }
        }
    }

    val arColumnColor = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        appState.saveColumnList()
        r.data?.int(ActColumnCustomize.EXTRA_COLUMN_INDEX)
            ?.let { appState.column(it) }
            ?.let {
                it.fireColumnColor()
                it.fireShowContent(
                    reason = "ActMain column color changed",
                    reset = true
                )
            }
        updateColumnStrip()
    }

    val arLanguageFilter = ActivityResultHandler(log) { r ->
        LanguageFilterActivity.decodeResult(r)?.let { columnIndex ->
            appState.saveColumnList()
            appState.column(columnIndex)?.onLanguageFilterChanged()
        }
    }

    val arNickname = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        updateColumnStrip()
        appState.columnList.forEach { it.fireShowColumnHeader() }
    }

    val arAppSetting = ActivityResultHandler(log) { r ->
        Column.reloadDefaultColor(this)
        showFooterColor()
        updateColumnStrip()
        enableEdgeToEdgeEx(forceDark = false)
        if (r.resultCode == RESULT_APP_DATA_IMPORT) {
            r.data?.data?.let { importAppData(it) }
        }
    }

    val arAbout = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.string(ActAbout.EXTRA_SEARCH)?.notEmpty()?.let { search ->
            timeline(
                defaultInsertPosition,
                ColumnType.SEARCH,
                args = anyArrayOf(search, true)
            )
        }
    }

    val arAccountSetting = ActivityResultHandler(log) { r ->
        launchAndShowError {
            updateColumnStrip()
            appState.columnList.forEach { it.fireShowColumnHeader() }
            when (r.resultCode) {
                RESULT_OK -> r.data?.data?.let { openBrowser(it) }

                ActAccountSetting.RESULT_INPUT_ACCESS_TOKEN ->
                    r.data?.long(ActAccountSetting.EXTRA_DB_ID)
                        ?.let { daoSavedAccount.loadAccount(it) }
                        ?.let { accessTokenPrompt(it.apiHost) }
            }
        }
    }

    val arColumnList = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.getIntegerArrayListExtra(ActColumnList.EXTRA_ORDER)
            ?.takeIf { isOrderChanged(it) }
            ?.let { setColumnsOrder(it) }
        r.data?.int(ActColumnList.EXTRA_SELECTION)
            ?.takeIf { it in 0 until appState.columnCount }
            ?.let { scrollToColumn(it) }
    }

    val arActText = ActivityResultHandler(log) { r ->
        when (r.resultCode) {
            ActText.RESULT_SEARCH_NOTESTOCK -> searchFromActivityResult(
                r.data,
                ColumnType.SEARCH_NOTESTOCK
            )
        }
    }

    val arActPost = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.let { data ->
            onCompleteActPost(data)
        }
    }

    val prNotification = permissionSpecNotification.requester {
        launchAndShowError {
            afterNotificationGranted()
        }
    }

    private var startAfterJob: WeakReference<Job>? = null

    //////////////////////////////////////////////////////////////////
    // ライフサイクルイベント

    override fun onCreate(savedInstanceState: Bundle?) {
        log.d("onCreate")
        installSplashScreen()
        refActMain = WeakReference(this)
        // supportRequestWindowFeature not needed without AppCompat
        super.onCreate(savedInstanceState)
        
        onBackPressedDispatcher.addCallback(this) { 
            viewModel.onBackPressed() 
        }

        // Back Press Handling
        lifecycleScope.launch {
            viewModel.backPressEffect.collectLatest { effect ->
                when(effect) {
                    MainViewModel.BackPressEffect.Finish -> finish()
                    MainViewModel.BackPressEffect.OpenColumnList -> openColumnList()
                    is MainViewModel.BackPressEffect.ShowToast -> showToast(effect.isError, effect.textId)
                }
            }
        }

        prNotification.register(this)
        arColumnColor.register(this)
        arLanguageFilter.register(this)
        arNickname.register(this)
        arAppSetting.register(this)
        arAbout.register(this)
        arAccountSetting.register(this)
        arColumnList.register(this)
        arActPost.register(this)
        arActText.register(this)

        appState = App1.getAppState(this)
        appState.mainViewModel = viewModel
        handler = appState.handler
        density = appState.density
        completionHelper = CompletionHelper()
        
        sideMenuAdapter = SideMenuAdapter(this, handler)

        App1.setActivityTheme(this)
        
        setContent {
            MainScreen(
                viewModel = viewModel,
                sideMenuAdapter = sideMenuAdapter,
                onClickMenu = { openDrawer() },
                onClickToot = { openPost() },
                onLongClickToot = { viewModel.toggleQuickTootMenu() },
                onClickColumn = { idx ->
                    val column = appState.column(idx)
                    if (column != null) {
                        // TODO: Implement scrollToTop logic with Compose state
                        // For now just scroll to column
                        scrollToColumn(idx)
                    }
                },
                onDrawerClosed = { completionHelper.closeAcctPopup() },
            )
        }

        EmojiDecoder.useTwemoji = PrefB.bpUseTwemoji.value

        acctPadLr = dp(4)
        reloadTextSize()
        reloadEmojiScale()

        initUI()

        updateColumnStrip()
        scrollToLastColumn()

        if (savedInstanceState == null) {
            checkNotificationImmediateAll(this)
        }

        if (savedInstanceState != null) {
            sharedIntent2?.let { handleSharedIntent(it) }
        }
    }

    override fun onDestroy() {
        log.d("onDestroy")
        super.onDestroy()
        refActMain = null
        completionHelper.onDestroy()

        // 子画面を全て閉じる
        closeList.forEach {
            try {
                it.get()?.finish()
            } catch (ex: Throwable) {
                log.e(ex, "close failed?")
            }
        }
        closeList.clear()

        // View holders are now automatically cleaned up via DisposableEffect in ColumnWrapper
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        log.w("onNewIntent: isResumed=$isResumed")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        log.w("onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        // resizeColumnWidth removed - handled by Compose
    }

    override fun onSaveInstanceState(outState: Bundle) {
        log.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        // phoneTab logic removed - Compose handles state restoration or ViewModel
        appState.columnList.forEach { it.saveScrollPosition() }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        log.d("onRestoreInstanceState")
        super.onRestoreInstanceState(savedInstanceState)
        // phoneTab logic removed
    }

    override fun onStart() {
        log.d("onStart")
        isStartedEx = true
        super.onStart()
        galaxyBackgroundWorkaround()
        benchmark("onStart total") {
            reloadEmojiScale()
            benchmark("reload color") { reloadColors() }
            benchmark("reload timezone") { reloadTimeZone() }

            sideMenuAdapter.onActivityStart()

            launchDialogs()

            // 残りの処理はActivityResultの処理より後回しにしたい
            lifecycleScope.launch {
                try {
                    delay(1L)
                    benchmark("onStartAfter total") {

                        benchmark("sweepBuggieData") {
                            // バグいアカウントデータを消す
                            try {
                                daoSavedAccount.sweepBuggieData()
                            } catch (ex: Throwable) {
                                log.e(ex, "sweepBuggieData failed.")
                            }
                        }

                        val newAccounts = benchmark("loadAccountList") {
                            daoSavedAccount.loadAccountList()
                        }

                        benchmark("removeColumnByAccount") {
                            val setDbId = newAccounts.map { it.db_id }.toSet()
                            // アカウント設定から戻ってきたら、カラムを消す必要があるかもしれない
                            appState.columnList
                                .mapIndexedNotNull { index, column ->
                                    when {
                                        column.accessInfo.isNA -> index
                                        setDbId.contains(column.accessInfo.db_id) -> index
                                        else -> null
                                    }
                                }.takeIf { it.size != appState.columnCount }
                                ?.let { setColumnsOrder(it) }
                        }

                        benchmark("fireColumnColor") {
                            // 背景画像を表示しない設定が変更された時にカラムの背景を設定しなおす
                            appState.columnList.forEach { column ->
                                column.viewHolder?.lastAnnouncementShown = 0L
                                column.fireColumnColor()
                            }
                        }
                        benchmark("reloadAccountSetting") {
                            // 各カラムのアカウント設定を読み直す
                            reloadAccountSetting(newAccounts)
                        }
                        benchmark("refreshAfterPost") {
                            // 投稿直後ならカラムの再取得を行う
                            refreshAfterPost()
                        }
                        benchmark("column.onActivityStart") {
                            // 画面復帰時に再取得などを行う
                            appState.columnList.forEach { it.onActivityStart() }
                        }
                        benchmark("streamManager.onScreenStart") {
                            // 画面復帰時にストリーミング接続を開始する
                            appState.streamManager.onScreenStart()
                        }
                        benchmark("updateColumnStripSelection") {
                            // カラムの表示範囲インジケータを更新
                            updateColumnStripSelection(-1, -1f)
                        }
                        benchmark("fireShowContent") {
                            appState.columnList.forEach {
                                it.fireShowContent(reason = "ActMain onStart", reset = true)
                            }
                        }
                        benchmark("proc_updateRelativeTime") {
                            // 相対時刻表示の更新
                            procUpdateRelativeTime.run()
                        }
                        benchmark("enableSpeech") {
                            // スピーチの開始
                            appState.enableSpeech()
                        }
                    }
                } catch (ex: Throwable) {
                    log.e(ex, "startAfter failed.")
                }
            }.let { startAfterJob = WeakReference(it) }
        }
    }

    override fun onStop() {
        log.d("onStop")
        isStartedEx = false
        startAfterJob?.get()?.cancel()
        startAfterJob = null
        handler.removeCallbacks(procUpdateRelativeTime)

        completionHelper.closeAcctPopup()

        closePopup()

        appState.streamManager.onScreenStop()

        appState.columnList.forEach { it.saveScrollPosition() }

        appState.saveColumnList(bEnableSpeech = false)

        super.onStop()
    }

    override fun onResume() {
        log.d("onResume")
        isResumed = true

        // super.onResume() から呼ばれる isTopOfTask() が
        // android.os.RemoteException をたまに出すが、放置する
        super.onResume()

        if (PrefB.bpDontScreenOff.value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // 外部から受け取ったUriの処理
        val uri = ActCallback.lastUri.getAndSet(null)
        if (uri != null) {
            handleIntentUri(uri)
        }

        // 外部から受け取ったUriの処理
        val intent = ActCallback.sharedIntent.getAndSet(null)
        if (intent != null) {
            handleSharedIntent(intent)
        }
    }

    override fun onPause() {
        log.d("onPause")
        
        // 最後に表示していたカラムの位置
        val lastPos = viewModel.currentPage.value
        log.d("ipLastColumnPos save $lastPos")
        PrefI.ipLastColumnPos.value = lastPos

        appState.columnList.forEach { it.saveScrollPosition() }

        appState.saveColumnList(bEnableSpeech = false)

        isResumed = false
        super.onPause()
    }

    //////////////////////////////////////////////////////////////////
    // UIイベント

    // (ViewPager overrides removed)

    override fun onMyClickableSpanClicked(viewClicked: View, span: MyClickableSpan) =
        onMyClickableSpanClickedImpl(viewClicked, span)

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return when {
            super.onKeyShortcut(keyCode, event) -> true
            event?.isCtrlPressed == true && keyCode == KeyEvent.KEYCODE_N -> {
                openPost()
                true
            }

            else -> false
        }
    }

    //////////////////////////////////////////////////////////////////
    // UI初期化

    // ビューのlateinit変数を初期化する
    // private fun findViews() (Removed)
    
    internal fun initUI() {
        Column.reloadDefaultColor(this)

        galaxyBackgroundWorkaround()

        reloadFonts()
        reloadIconSize()

        justifyWindowContentPortrait()

        reloadMediaHeight()
        showFooterColor()
        
        // Observe current page changes
        lifecycleScope.launch {
            viewModel.currentPage.collect { position ->
                appState.column(position)?.let { column ->
                    column.startLoading(ColumnLoadReason.PageSelect)
                    scrollColumnStrip(position)
                    completionHelper.setInstance(column.accessInfo.takeIf { !it.isNA })
                }
            }
        }
    }

    private fun galaxyBackgroundWorkaround() {
        log.i(
            "galaxyBackgroundWorkaround: Build MANUFACTURER=${
                Build.MANUFACTURER
            }, BRAND=${
                Build.BRAND
            }, MODEL=${
                Build.MODEL
            }"
        )
        if (Build.MANUFACTURER?.contains("samsung", ignoreCase = true) == true) {
            val colorBarBg = attrColor(MR.attr.colorSurface)
            // Window Insets の色を再設定する
            window.setBackgroundDrawable(ColorDrawable(colorBarBg))
            // 余計なオーバードローを一回追加する
            window.decorView.rootView.setBackgroundColor(colorBarBg)
        }
    }
}
