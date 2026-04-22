package es.ariaontheplanet.quasar

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration

import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.More
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import es.ariaontheplanet.quasar.compose.NetworkImage

import es.ariaontheplanet.quasar.action.saveWindowSize
import es.ariaontheplanet.quasar.actpost.PostViewModel
import es.ariaontheplanet.quasar.actpost.ActPostStates
import es.ariaontheplanet.quasar.actpost.AttachmentSlotUi
import es.ariaontheplanet.quasar.actpost.FeaturedTagCache
import es.ariaontheplanet.quasar.actpost.TextEditState
import es.ariaontheplanet.quasar.actpost.addAttachment
import es.ariaontheplanet.quasar.actpost.applyMushroomText
import es.ariaontheplanet.quasar.actpost.editAttachmentDescription
import es.ariaontheplanet.quasar.actpost.openAttachment
import es.ariaontheplanet.quasar.actpost.openMushroom
import es.ariaontheplanet.quasar.actpost.openEmojiPickerForContent
import es.ariaontheplanet.quasar.actpost.openFeaturedTagList
import es.ariaontheplanet.quasar.actpost.openFocusPoint
import es.ariaontheplanet.quasar.actpost.openVisibilityPicker
import es.ariaontheplanet.quasar.actpost.performAccountChooser
import es.ariaontheplanet.quasar.actpost.performAttachmentClick
import es.ariaontheplanet.quasar.actpost.performMore
import es.ariaontheplanet.quasar.actpost.performPost
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment
import androidx.lifecycle.lifecycleScope
import es.ariaontheplanet.quasar.actmain.ActMainRegistry
import es.ariaontheplanet.quasar.actmain.onCompleteActPost
import es.ariaontheplanet.quasar.actpost.editAttachmentDescription
import es.ariaontheplanet.quasar.actpost.openFocusPoint
import es.ariaontheplanet.quasar.actpost.performAttachmentClick
import es.ariaontheplanet.quasar.actpost.rearrangeAttachments
import es.ariaontheplanet.quasar.actpost.resetText
import es.ariaontheplanet.quasar.actpost.afterUpdateText
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import es.ariaontheplanet.quasar.actpost.removeReply
import es.ariaontheplanet.quasar.actpost.resetSchedule
import es.ariaontheplanet.quasar.actpost.restoreState
import es.ariaontheplanet.quasar.actpost.saveAttachmentList
import es.ariaontheplanet.quasar.actpost.saveDraft
import es.ariaontheplanet.quasar.actpost.saveState
import es.ariaontheplanet.quasar.actpost.showQuotedRenote
import es.ariaontheplanet.quasar.actpost.showReplyTo
import es.ariaontheplanet.quasar.actpost.showVisibility
import es.ariaontheplanet.quasar.actpost.updateText
import es.ariaontheplanet.quasar.actpost.updateTextCount
import es.ariaontheplanet.quasar.api.entity.TootScheduled
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.getVisibilityIconId
import es.ariaontheplanet.quasar.pref.PrefB
import es.ariaontheplanet.quasar.pref.PrefI
import es.ariaontheplanet.quasar.span.MyClickableSpan
import es.ariaontheplanet.quasar.span.MyClickableSpanHandler
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.util.AttachmentPicker
import es.ariaontheplanet.quasar.util.AttachmentUploader
import es.ariaontheplanet.quasar.util.PostAttachment
import es.ariaontheplanet.quasar.util.loadLanguageList
import es.ariaontheplanet.quasar.util.openBrowser

import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchIO
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.UriAndType
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.string
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.isNotOk
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap

data class AttachmentSlotUi(
    val visible: Boolean = false,
    val previewUrl: String? = null,
    val fallbackIconRes: Int = R.drawable.ic_clip,
)

class ActPostViews(val activity: ActPost) {
    val etContentWarning get() = activity.etContentWarning
    val etContent get() = activity.etContent
    val etChoice1 get() = activity.etChoice1
    val etChoice2 get() = activity.etChoice2
    val etChoice3 get() = activity.etChoice3
    val etChoice4 get() = activity.etChoice4
    val etExpireDays get() = activity.etExpireDays
    val etExpireHours get() = activity.etExpireHours
    val etExpireMinutes get() = activity.etExpireMinutes
}

class ActPost : ComponentActivity(),
    MyClickableSpanHandler {

    companion object {
        private val log = LogCategory("ActPost")

        var refActPost: WeakReference<ActPost>? = null

        const val EXTRA_POSTED_ACCT = "posted_acct"
        const val EXTRA_POSTED_STATUS_ID = "posted_status_id"
        const val EXTRA_POSTED_REPLY_ID = "posted_reply_id"
        const val EXTRA_POSTED_REDRAFT_ID = "posted_redraft_id"
        const val EXTRA_MULTI_WINDOW = "multiWindow"

        const val KEY_ACCOUNT_DB_ID = "account_db_id"
        const val KEY_REPLY_STATUS = "reply_status"
        const val KEY_REDRAFT_STATUS = "redraft_status"
        const val KEY_EDIT_STATUS = "edit_status"
        const val KEY_INITIAL_TEXT = "initial_text"
        const val KEY_INITIAL_CW_ENABLED = "initial_cw_enabled"
        const val KEY_INITIAL_CW_TEXT = "initial_cw_text"
        const val KEY_INITIAL_VISIBILITY = "initial_visibility"
        const val KEY_SHARED_INTENT = "sent_intent"
        const val KEY_QUOTE = "quote"
        const val KEY_SCHEDULED_STATUS = "scheduled_status"

        const val STATE_ALL = "all"

        /////////////////////////////////////////////////

        fun createIntent(
            context: Context,
            accountDbId: Long,
            multiWindowMode: Boolean,
            // 再編集する投稿。アカウントと同一のタンスであること
            redraftStatus: TootStatus? = null,
            // 編集する投稿。アカウントと同一のタンスであること
            editStatus: TootStatus? = null,
            // 返信対象の投稿。同一タンス上に同期済みであること
            replyStatus: TootStatus? = null,
            //初期テキスト
            initialText: String? = null,
            // 外部アプリから共有されたインテント
            sharedIntent: Intent? = null,
            // 返信ではなく引用トゥートを作成する
            quote: Boolean = false,
            //(Mastodon) 予約投稿の編集
            scheduledStatus: TootScheduled? = null,
            // QuickPostSheet → 全画面遷移時の引き継ぎ
            initialCwEnabled: Boolean = false,
            initialCwText: String? = null,
            initialVisibility: es.ariaontheplanet.quasar.api.entity.TootVisibility? = null,
        ) = Intent(context, ActPost::class.java).apply {
            putExtra(EXTRA_MULTI_WINDOW, multiWindowMode)
            putExtra(KEY_ACCOUNT_DB_ID, accountDbId)
            initialText?.let { putExtra(KEY_INITIAL_TEXT, it) }
            if (initialCwEnabled) putExtra(KEY_INITIAL_CW_ENABLED, true)
            initialCwText?.let { putExtra(KEY_INITIAL_CW_TEXT, it) }
            initialVisibility?.let { putExtra(KEY_INITIAL_VISIBILITY, it.name) }
            redraftStatus?.let { putExtra(KEY_REDRAFT_STATUS, it.json.toString()) }
            editStatus?.let { putExtra(KEY_EDIT_STATUS, it.json.toString()) }
            replyStatus?.let {
                putExtra(KEY_REPLY_STATUS, it.json.toString())
                putExtra(KEY_QUOTE, quote)
            }
            sharedIntent?.let { putExtra(KEY_SHARED_INTENT, it) }
            scheduledStatus?.let { putExtra(KEY_SCHEDULED_STATUS, it.src.toString()) }
        }
    }

    // Text states
    val viewModel: PostViewModel by viewModel()
    
    val etContent get() = viewModel.etContent
    val etContentWarning get() = viewModel.etContentWarning
    val etChoice1 get() = viewModel.etChoice1
    val etChoice2 get() = viewModel.etChoice2
    val etChoice3 get() = viewModel.etChoice3
    val etChoice4 get() = viewModel.etChoice4
    val etExpireDays get() = viewModel.etExpireDays
    val etExpireHours get() = viewModel.etExpireHours
    val etExpireMinutes get() = viewModel.etExpireMinutes

    val views by lazy { ActPostViews(this) }
    val etChoices: List<TextEditState> get() = viewModel.etChoices
    
    /** Which text field has focus: 0=content, 1=cw, 2-5=choice1-4. Used by Mushroom plugin. */
    var focusedEditField: Int
        get() = viewModel.focusedEditField.value
        set(value) { viewModel.focusedEditField.value = value }

    /** FocusRequester wired to etContent's BasicTextField. */
    val contentFocusRequester = FocusRequester()

    // Flag Delegates to ViewModel
    var nsfwChecked: Boolean
        get() = viewModel.nsfwChecked.value
        set(value) { viewModel.setNsfw(value) }
        
    var contentWarningChecked: Boolean
        get() = viewModel.contentWarningChecked.value
        set(value) { viewModel.setContentWarning(value) }

    var pollTypeIndex: Int
        get() = viewModel.pollTypeIndex.value
        set(value) { viewModel.setPollTypeIndex(value) }
        
    var pollMultipleChoiceChecked: Boolean
        get() = viewModel.pollMultipleChoiceChecked.value
        set(value) { viewModel.setPollMultipleChoice(value) }
        
    var pollHideTotalsChecked: Boolean
        get() = viewModel.pollHideTotalsChecked.value
        set(value) { viewModel.setPollHideTotals(value) }
        
    var quoteChecked: Boolean
        get() = viewModel.quoteChecked.value
        set(value) { viewModel.setQuote(value) }
        
    var showQuoteOption: Boolean
        get() = viewModel.showQuoteOption.value
        set(value) { viewModel.setShowQuoteOption(value) }
        
    var showReplySection: Boolean
        get() = viewModel.showReplySection.value
        set(value) { viewModel.setShowReplySection(value) }
        
    var replyToText: String
        get() = viewModel.replyToText.value
        set(value) { viewModel.setReplyToText(value) }

    var charCountText: String
        get() = viewModel.charCountText.value
        set(value) { viewModel.charCountText.value = value }
    var charCountColorArgb: Int
        get() = viewModel.charCountColorArgb.value
        set(value) { viewModel.charCountColorArgb.value = value }
    var visibilityIconRes: ImageVector
        get() = viewModel.visibilityIconRes.value
        set(value) { viewModel.visibilityIconRes.value = value }
    var scheduleText: String
        get() = viewModel.scheduleText.value
        set(value) { viewModel.scheduleText.value = value }
    
    // Legacy mutableState variables that are not yet in ViewModel or specific to UI
    var accountButtonText: String
        get() = viewModel.accountButtonText.value
        set(value) { viewModel.accountButtonText.value = value }
    var accountAvatarStaticUrl: String?
        get() = viewModel.accountAvatarStaticUrl.value
        set(value) { viewModel.accountAvatarStaticUrl.value = value }
    var accountAvatarAnimatedUrl: String?
        get() = viewModel.accountAvatarAnimatedUrl.value
        set(value) { viewModel.accountAvatarAnimatedUrl.value = value }
    var accountAvatarCorner: Float
        get() = viewModel.accountAvatarCorner.value
        set(value) { viewModel.accountAvatarCorner.value = value }
    var showAttachmentSection: Boolean
        get() = viewModel.showAttachmentSection.value
        set(value) { viewModel.showAttachmentSection.value = value }
    var selectedLanguageIndex: Int
        get() = viewModel.selectedLanguageIndex.value
        set(value) { viewModel.selectedLanguageIndex.value = value }
    var attachmentSlots: List<AttachmentSlotUi>
        get() = viewModel.attachmentSlots
        set(value) { 
             viewModel.attachmentSlots.clear()
             viewModel.attachmentSlots.addAll(value)
        }
    var attachmentThumbCorner: Float
        get() = viewModel.attachmentThumbCorner.value
        set(value) { viewModel.attachmentThumbCorner.value = value }
    var showAttachmentRearrange: Boolean
        get() = viewModel.showAttachmentRearrange.value
        set(value) { viewModel.showAttachmentRearrange.value = value }
    var attachmentProgressText: String
        get() = viewModel.attachmentProgressText.value
        set(value) { viewModel.attachmentProgressText.value = value }

    lateinit var handler: Handler
    lateinit var appState: AppState
    // attachmentUploader moved to ViewModel
    lateinit var attachmentPicker: AttachmentPicker

    var density: Float = 0f

    val languages by lazy {
        loadLanguageList()
    }

    ///////////////////////////////////////////////////

    // SavedAccount.acctAscii => FeaturedTagCache
    val featuredTagCache = ConcurrentHashMap<String, FeaturedTagCache>()

    // background job
    var jobFeaturedTag: WeakReference<Job>? = null
    var jobMaxCharCount: WeakReference<Job>? = null

    ///////////////////////////////////////////////////

    var states: ActPostStates
        get() = viewModel.states
        set(value) { viewModel.states = value }

    var accountList: List<SavedAccount> = emptyList()
    var account: SavedAccount?
        get() = viewModel.account.value
        set(value) { viewModel.setAccount(value) }
        
    var attachmentList: ArrayList<PostAttachment>
        get() = viewModel.attachmentList
        set(value) { viewModel.attachmentList = value }
        
    var isPostComplete: Boolean = false
    var scheduledStatus: TootScheduled? = null

    /////////////////////////////////////////////////////////////////////

    val isMultiWindowPost: Boolean
        get() = intent.getBooleanExtra(EXTRA_MULTI_WINDOW, false)

    val arMushroom = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.string("replace_key")?.let { text ->
            when (states.mushroomInput) {
                0 -> applyMushroomText(views.etContent, text)
                1 -> applyMushroomText(views.etContentWarning, text)
                else -> for (i in 0..3) {
                    if (states.mushroomInput == i + 2) {
                        applyMushroomText(etChoices[i], text)
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            launchAndShowError {
                finish()
                // 戻るボタンを押したときとonPauseで2回保存することになるが、
                // 同じ内容はDB上は重複しないはず…
                saveDraft()
            }
        }
        if (isMultiWindowPost) ActMainRegistry.current?.closeList?.add(WeakReference(this))
        appState = App1.getAppState(this)
        handler = appState.handler
        attachmentPicker = AttachmentPicker(this, object : AttachmentPicker.Callback {
            override suspend fun onPickAttachment(item: UriAndType) {
                viewModel.addAttachment(item.uri, item.mimeType)
            }

            override suspend fun onPickCustomThumbnail(
                attachmentId: String?,
                src: UriAndType?,
            ) {
                src ?: return
                val pa = attachmentList.find { it.attachment?.id?.toString() == attachmentId }
                    ?: error("missing attachment for attachmentId=$attachmentId")
                viewModel.uploadCustomThumbnail(pa, src)
            }
        })

        density = resources.displayMetrics.density
        arMushroom.register(this)

        if (!viewModel.isInitialized) {
            viewModel.isInitialized = true
            charCountColorArgb = attrColor(android.R.attr.textColorPrimary)
            visibilityIconRes = (states.visibility ?: es.ariaontheplanet.quasar.api.entity.TootVisibility.Public)
                .getVisibilityIconId(account?.isMisskey == true)
            scheduleText = getString(R.string.unspecified)
            pollTypeIndex = 0
            nsfwChecked = false
            contentWarningChecked = false
            quoteChecked = false
            showQuoteOption = false
            showReplySection = false
            replyToText = ""
            accountButtonText = getString(R.string.not_selected_2)
            accountAvatarStaticUrl = null
            accountAvatarAnimatedUrl = null
            accountAvatarCorner = calcIconRound(dp(32))
            showAttachmentSection = false
            selectedLanguageIndex = 0
            attachmentSlots = List(4) { AttachmentSlotUi() }.toMutableList()
            attachmentThumbCorner = calcIconRound(dp(48))
            showAttachmentRearrange = false
            attachmentProgressText = ""
        }

        App1.setActivityTheme(this)
        setContent {
            val confirmRequest by viewModel.confirmDialogRequest
            if (confirmRequest != null) {
                val (skipNext, setSkipNext) = remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = confirmRequest!!.onCancel,
                    confirmButton = {
                        TextButton(onClick = { confirmRequest!!.onConfirm(skipNext) }) { Text(stringResource(android.R.string.ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = confirmRequest!!.onCancel) { Text(stringResource(android.R.string.cancel)) }
                    },
                    text = {
                        Column {
                            Text(confirmRequest!!.message)
                            if (confirmRequest!!.showSkipNext) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = skipNext,
                                        onCheckedChange = setSkipNext
                                    )
                                    Text(stringResource(R.string.dont_confirm_again))
                                }
                            }
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues())
                    .onSizeChanged {
                        if (isMultiWindowPost) saveWindowSize()
                    }
            ) {
                if (PrefB.bpPostButtonBarTop.value) PostFooterBar()
                ActPostScreen(
                    activity = this@ActPost,
                    modifier = Modifier.weight(1f),
                )
                if (!PrefB.bpPostButtonBarTop.value) PostFooterBar()
            }
        }
        initUI()

        lifecycleScope.launch {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is PostViewModel.Effect.ShowToast -> showToast(effect.error, effect.message)
                    is PostViewModel.Effect.ShowError -> showToast(true, effect.exception.message ?: "Error")
                    PostViewModel.Effect.OpenAttachmentPicker -> attachmentPicker.openPicker()
                    is PostViewModel.Effect.OpenCustomThumbnailPicker -> attachmentPicker.openThumbnailPicker(effect.pa)
                    is PostViewModel.Effect.EditAttachmentDescription -> editAttachmentDescription(effect.pa)
                    is PostViewModel.Effect.OpenFocusPoint -> openFocusPoint(effect.pa)
                    is PostViewModel.Effect.ShowAttachmentMenu -> performAttachmentClick(effect.pa)
                    is PostViewModel.Effect.PostComplete -> {
                        ActMainRegistry.current?.onCompleteActPost(effect.intent)
                        if (effect.isMultiWindowPost) {
                             resetText()
                             launchAndShowError {
                                 updateText(Intent(), saveDraft = false, resetAccount = false)
                                 afterUpdateText()
                             }
                        } else {
                            setResult(Activity.RESULT_OK, effect.intent)
                            isPostComplete = true
                            finish()
                        }
                    }
                }
            }
        }

        // 初期化の続きをコルーチンでやる
        launchAndShowError {
            when (savedInstanceState) {
                null -> updateText(intent, saveDraft = false)
                else -> restoreState(savedInstanceState)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // showMediaAttachment() handled by ViewModel
        showVisibility()
        updateTextCount()
        launchAndShowError { showReplyTo() }
        showQuotedRenote()
    }

    override fun onResume() {
        super.onResume()
        refActPost = WeakReference(this)
    }

    override fun onPause() {
        super.onPause()
        if (!isPostComplete) launchMain {
            try {
                // 編集中にホーム画面を押したり他アプリに移動する場合は下書きを保存する
                // やや過剰な気がするが、自アプリに戻ってくるときにランチャーからアイコンタップされると
                // メイン画面より上にあるアクティビティはすべて消されてしまうので
                // このタイミングで保存するしかない
                saveDraft()
            } catch (ex: Throwable) {
                log.e(ex, "can't save draft.")
                showToast(ex, "can't save draft.")
            }
        }
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return when {
            super.onKeyShortcut(keyCode, event) -> true
            event?.isCtrlPressed == true && keyCode == KeyEvent.KEYCODE_T -> {
                viewModel.performPost(isMultiWindowPost)
                true
            }

            else -> false
        }
    }

    override fun onMyClickableSpanClicked(viewClicked: View, span: MyClickableSpan) {
        openBrowser(span.linkInfo.url)
    }


    @Composable
    private fun PostFooterBar() {
        val horizontalPadding = footerHorizontalPaddingDp()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = horizontalPadding),
        ) {
            FooterIconButton(Icons.Filled.AttachFile, getString(R.string.media_attachment)) {
                openAttachment()
            }
            FooterIconButton(
                visibilityIconRes,
                getString(R.string.visibility),
            ) {
                openVisibilityPicker()
            }
            FooterIconButton(Icons.AutoMirrored.Filled.More, getString(R.string.more)) {
                performMore()
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = charCountText,
                color = Color(charCountColorArgb),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .align(androidx.compose.ui.Alignment.CenterVertically),
            )
            FooterIconButton(Icons.AutoMirrored.Filled.Send, getString(R.string.toot)) {
                viewModel.performPost(isMultiWindowPost)
            }
        }
    }

    @Composable
    private fun FooterIconButton(
        iconRes: ImageVector,
        contentDescription: String,
        onClick: () -> Unit,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = iconRes,
                contentDescription = contentDescription,
            )
        }
    }

    @Composable
    fun FooterIconButtonToggle(
        iconRes: Int,
        contentDescription: String,
        onClick: () -> Unit,
    ) {
        var isToggled by rememberSaveable { mutableStateOf(false) }

        IconButton(
            onClick = {
                isToggled = !isToggled
                onClick()
            },
            modifier =
                Modifier.fillMaxHeight()
                        .alpha(if(isToggled) 0.6f else 1f),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription
            )
        }
    }

    internal fun footerHorizontalPaddingDp() = run {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        val basePx = if (
            widthDp >= 640f &&
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            when (PrefI.ipJustifyWindowContentPortrait.value) {
                PrefI.JWCP_START,
                PrefI.JWCP_END,
                -> 0
                else -> kotlin.math.max(0, (dm.widthPixels - (0.5f + 460f * dm.density).toInt()) / 2)
            }
        } else {
            kotlin.math.max(0, (dm.widthPixels - (0.5f + 460f * dm.density).toInt()) / 2)
        }
        (basePx / dm.density).dp
    }

    fun initUI() {

        // Observe all text fields to update the character count
        launchMain {
            androidx.compose.runtime.snapshotFlow { views.etContent.fieldValue.text }
                .collectLatest { updateTextCount() }
        }
        launchMain {
            androidx.compose.runtime.snapshotFlow { views.etContentWarning.fieldValue.text }
                .collectLatest { updateTextCount() }
        }
        for (et in etChoices) {
            launchMain {
                androidx.compose.runtime.snapshotFlow { et.fieldValue.text }
                    .collectLatest { updateTextCount() }
            }
        }

    }
}
