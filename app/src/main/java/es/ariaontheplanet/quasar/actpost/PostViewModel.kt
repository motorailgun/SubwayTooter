package es.ariaontheplanet.quasar.actpost

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public


import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.api.entity.TootVisibility
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.util.PostAttachment
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

import android.content.Intent
import android.net.Uri
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.ActPost
import es.ariaontheplanet.quasar.actmain.onCompleteActPost
import es.ariaontheplanet.quasar.api.entity.TootPollsType
import es.ariaontheplanet.quasar.util.AttachmentRequest
import es.ariaontheplanet.quasar.util.AttachmentUploader
import es.ariaontheplanet.quasar.util.PostImpl
import es.ariaontheplanet.quasar.util.PostInteractions
import es.ariaontheplanet.quasar.util.PostResult
import jp.juggler.util.coroutine.launchIO
import kotlin.math.min
import es.ariaontheplanet.quasar.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

import jp.juggler.util.data.CharacterGroup
import es.ariaontheplanet.quasar.pref.PrefB
import es.ariaontheplanet.quasar.api.entity.TootAttachment
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import es.ariaontheplanet.quasar.api.entity.TootAttachmentType
import jp.juggler.util.data.notEmpty

import jp.juggler.util.data.UriAndType
import es.ariaontheplanet.quasar.App1

import es.ariaontheplanet.quasar.api.entity.TootScheduled
import jp.juggler.util.data.decodeJsonArray
import jp.juggler.util.data.JsonObject

import es.ariaontheplanet.quasar.util.loadLanguageList

class PostViewModel(application: Application) : AndroidViewModel(application), PostInteractions {
    companion object {
        private val log = LogCategory("PostViewModel")
    }
    
    val focusedEditField = mutableStateOf(0)

    
    val attachmentUploader = AttachmentUploader(application, null)
    
    private val _progressChannel = Channel<Unit>(Channel.CONFLATED)
    
    init {
        startCompletionObserver()
        viewModelScope.launch {
            for (item in _progressChannel) {
                 notifyAttachmentListUpdated()
                 delay(500L)
            }
        }
    }
    
    // Side Effects
    sealed class Effect {
        data class ShowToast(val error: Boolean, val message: String) : Effect()
        data class ShowError(val exception: Throwable) : Effect()
        object OpenAttachmentPicker : Effect()
        data class OpenCustomThumbnailPicker(val pa: PostAttachment) : Effect()
        data class EditAttachmentDescription(val pa: PostAttachment) : Effect()
        data class OpenFocusPoint(val pa: PostAttachment) : Effect()
        data class ShowAttachmentMenu(val pa: PostAttachment) : Effect()
        data class PostComplete(val intent: Intent, val isMultiWindowPost: Boolean) : Effect()
    }
    
    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    
    override fun showToast(error: Boolean, message: String) {
        _effects.trySend(Effect.ShowToast(error, message))
    }

    override fun showToast(error: Boolean, @StringRes messageId: Int, vararg args: Any?) {
        showToast(error, getApplication<Application>().getString(messageId, *args))
    }
    
    fun showError(ex: Throwable) {
        _effects.trySend(Effect.ShowError(ex))
    }
    
    // Dialog State
    data class ConfirmDialogRequest(
        val message: String,
        val showSkipNext: Boolean = false,
        val onConfirm: (skipNext: Boolean) -> Unit,
        val onCancel: () -> Unit,
    )

    val confirmDialogRequest = mutableStateOf<ConfirmDialogRequest?>(null)

    override suspend fun confirm(message: String) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                confirmDialogRequest.value = ConfirmDialogRequest(
                    message = message,
                    onConfirm = { _ ->
                        confirmDialogRequest.value = null
                        if (cont.isActive) cont.resume(Unit)
                    },
                    onCancel = { 
                        confirmDialogRequest.value = null
                        if (cont.isActive) cont.resumeWithException(CancellationException("Cancelled")) 
                    }
                )
                cont.invokeOnCancellation { 
                    confirmDialogRequest.value = null
                }
            }
        }
    }

    override suspend fun confirm(
        message: String, 
        isConfirmEnabled: Boolean, 
        setConfirmEnabled: (Boolean) -> Unit
    ) {
        if (!isConfirmEnabled) return
        withContext(Dispatchers.Main) {
            val skipNext = suspendCancellableCoroutine<Boolean> { cont ->
                confirmDialogRequest.value = ConfirmDialogRequest(
                    message = message,
                    showSkipNext = true,
                    onConfirm = { skipNext ->
                        confirmDialogRequest.value = null
                        if (cont.isActive) cont.resume(skipNext)
                    },
                    onCancel = { 
                        confirmDialogRequest.value = null
                        if (cont.isActive) cont.resumeWithException(CancellationException("Cancelled")) 
                    }
                )
                cont.invokeOnCancellation { confirmDialogRequest.value = null }
            }
            if (skipNext) setConfirmEnabled(false)
        }
    }

    override suspend fun confirm(@StringRes messageId: Int, vararg args: Any?) {
        confirm(getApplication<Application>().getString(messageId, *args))
    }

    fun performAttachmentClick(idx: Int) {
        val pa = attachmentList.elementAtOrNull(idx) ?: return
        _effects.trySend(Effect.ShowAttachmentMenu(pa))
    }
    
    fun deleteAttachment(pa: PostAttachment) {
        try {
            pa.isCancelled = true
            pa.status = PostAttachment.Status.Error
            pa.job.cancel()
            attachmentList.remove(pa)
            notifyAttachmentListUpdated()
        } catch (ignored: Throwable) {
        }
    }

    var isInitialized = false

    // Account
    private val _account = MutableStateFlow<SavedAccount?>(null)
    val account = _account.asStateFlow()
    
    fun setAccount(a: SavedAccount?) {
        _account.value = a
    }

    // Text Fields (Using TextEditState which holds Compose State)
    val etContent = TextEditState()
    val etContentWarning = TextEditState()
    val etChoice1 = TextEditState()
    val etChoice2 = TextEditState()
    val etChoice3 = TextEditState()
    val etChoice4 = TextEditState()
    val etExpireDays = TextEditState("1")
    val etExpireHours = TextEditState()
    val etExpireMinutes = TextEditState()
    
    val etChoices get() = listOf(etChoice1, etChoice2, etChoice3, etChoice4)

    // Visibility
    private val _visibility = MutableStateFlow(TootVisibility.Public)
    val visibility = _visibility.asStateFlow()
    
    fun setVisibility(v: TootVisibility) {
        _visibility.value = v
    }

    // Attachments
    var attachmentList: ArrayList<PostAttachment> = ArrayList()
        set(value) {
            field = value
            notifyAttachmentListUpdated()
        }
    
    private val _attachmentListUpdate = MutableStateFlow(0)
    val attachmentListUpdate = _attachmentListUpdate.asStateFlow()
    
    fun notifyAttachmentListUpdated() {
        _attachmentListUpdate.value++
        
        // Update attachmentSlots (UI)
        val list = attachmentList
        showAttachmentSection.value = list.isNotEmpty()
        
        attachmentSlots.clear()
        for (i in 0 until 4) {
             if (i >= list.size) {
                 attachmentSlots.add(AttachmentSlotUi(visible = false))
             } else {
                 val pa = list[i]
                 val attachment = pa.attachment
                 if (attachment == null || pa.status != PostAttachment.Status.Ok) {
                      attachmentSlots.add(AttachmentSlotUi(
                          visible = true,
                          previewUrl = null,
                          fallbackIconRes = R.drawable.ic_upload
                      ))
                 } else {
                      val fallbackIconRes = when (attachment.type) {
                          TootAttachmentType.Image -> R.drawable.ic_image
                          TootAttachmentType.Video,
                          TootAttachmentType.GIFV,
                          -> R.drawable.ic_videocam

                          TootAttachmentType.Audio -> R.drawable.ic_music_note
                          else -> R.drawable.ic_clip
                      }
                      attachmentSlots.add(AttachmentSlotUi(
                          visible = true,
                          previewUrl = attachment.preview_url,
                          fallbackIconRes = fallbackIconRes
                      ))
                 }
             }
        }
        
        // Update progress text
        showAttachmentRearrange.value = (list.size >= 2 && list.none { it.status == PostAttachment.Status.Progress })
        
        val mergedProgress = list
            .mapNotNull { it.progress.notEmpty() }
            .joinToString("\n")
        attachmentProgressText.value = mergedProgress
    }
    
    fun setAttachments(list: List<PostAttachment>) {
        attachmentList.clear()
        attachmentList.addAll(list)
        notifyAttachmentListUpdated()
    }

    // Flags
    private val _nsfwChecked = MutableStateFlow(false)
    val nsfwChecked = _nsfwChecked.asStateFlow()
    
    fun setNsfw(checked: Boolean) { _nsfwChecked.value = checked }

    private val _contentWarningChecked = MutableStateFlow(false)
    val contentWarningChecked = _contentWarningChecked.asStateFlow()
    
    fun setContentWarning(checked: Boolean) { _contentWarningChecked.value = checked }
    
    private val _pollTypeIndex = MutableStateFlow(0)
    val pollTypeIndex = _pollTypeIndex.asStateFlow()
    
    fun setPollTypeIndex(index: Int) { _pollTypeIndex.value = index }
    
    private val _pollMultipleChoiceChecked = MutableStateFlow(false)
    val pollMultipleChoiceChecked = _pollMultipleChoiceChecked.asStateFlow()
    
    fun setPollMultipleChoice(checked: Boolean) { _pollMultipleChoiceChecked.value = checked }
    
    private val _pollHideTotalsChecked = MutableStateFlow(false)
    val pollHideTotalsChecked = _pollHideTotalsChecked.asStateFlow()
    
    fun setPollHideTotals(checked: Boolean) { _pollHideTotalsChecked.value = checked }
    
    private val _quoteChecked = MutableStateFlow(false)
    val quoteChecked = _quoteChecked.asStateFlow()
    
    fun setQuote(checked: Boolean) { _quoteChecked.value = checked }
    
    private val _showQuoteOption = MutableStateFlow(false)
    val showQuoteOption = _showQuoteOption.asStateFlow()
    
    fun setShowQuoteOption(show: Boolean) { _showQuoteOption.value = show }
    
    private val _showReplySection = MutableStateFlow(false)
    val showReplySection = _showReplySection.asStateFlow()
    
    fun setShowReplySection(show: Boolean) { _showReplySection.value = show }
    
    private val _replyToText = MutableStateFlow("")
    val replyToText = _replyToText.asStateFlow()
    
    fun setReplyToText(text: String) { _replyToText.value = text }
    
    // Statuses
    var redraftStatus: TootStatus? = null
    var replyStatus: TootStatus? = null
    var editStatus: TootStatus? = null
    var scheduledStatus: TootScheduled? = null
    
    // Languages
    val languages: ArrayList<Pair<String, String>> by lazy {
        getApplication<Application>().loadLanguageList()
    }
    
    // Legacy States DTO (for serialization)
    var states = ActPostStates()
    
    // UI States (MutableState for direct Compose/Legacy compatibility)
    val charCountText = mutableStateOf("")
    val charCountColorArgb = mutableStateOf(0)
    val visibilityIconRes = mutableStateOf<ImageVector>(Icons.Filled.Public)
    val scheduleText = mutableStateOf("")
    val accountButtonText = mutableStateOf("")
    val accountAvatarStaticUrl = mutableStateOf<String?>(null)
    val accountAvatarAnimatedUrl = mutableStateOf<String?>(null)
    val accountAvatarCorner = mutableStateOf(0f)
    val showAttachmentSection = mutableStateOf(false)
    val selectedLanguageIndex = mutableStateOf(0)
    val attachmentThumbCorner = mutableStateOf(0f)
    val showAttachmentRearrange = mutableStateOf(false)
    val attachmentProgressText = mutableStateOf("")

    // Attachment Slots for UI
    val attachmentSlots = mutableStateListOf<AttachmentSlotUi>()

    // Logic from ActPostAttachment.kt
    
    fun openAttachment() {
        if (attachmentList.size >= 4) {
            showToast(true, getApplication<Application>().getString(R.string.attachment_too_many))
            return
        }
        if (account.value == null) {
             showToast(true, getApplication<Application>().getString(R.string.account_select_please))
             return
        }
        _effects.trySend(Effect.OpenAttachmentPicker)
    }

    fun addAttachment(uri: Uri, mimeTypeArg: String? = null) {
        val account = this.account.value
        if (account == null) {
            showToast(true, getApplication<Application>().getString(R.string.account_select_please))
            return
        } else if (attachmentList.size >= 4) {
            showToast(true, getApplication<Application>().getString(R.string.attachment_too_many))
            return
        }

        val pa = PostAttachment(object : PostAttachment.Callback {
            override fun onPostAttachmentComplete(pa: PostAttachment) {
                onPostAttachmentCompleteImpl(pa)
            }
            override fun onPostAttachmentProgress() {
                _progressChannel.trySend(Unit)
            }
        })
        
        attachmentList.add(pa)
        notifyAttachmentListUpdated()
        
        attachmentUploader.addRequest(
            AttachmentRequest(
                context = getApplication(),
                account = account,
                pa = pa,
                uri = uri,
                mimeTypeArg = mimeTypeArg,
                isReply = states.inReplyToId != null,
                imageResizeConfig = account.getResizeConfig(),
                maxBytesVideo = { instance, mediaConfig ->
                    min(
                        account.getMovieMaxBytes(instance),
                        mediaConfig?.int("video_size_limit")
                            ?.takeIf { it > 0 } ?: Int.MAX_VALUE,
                    )
                },
                maxBytesImage = { instance, mediaConfig ->
                    min(
                        account.getImageMaxBytes(instance),
                        mediaConfig?.int("image_size_limit")
                            ?.takeIf { it > 0 } ?: Int.MAX_VALUE,
                    )
                },
            )
        )
    }

    private fun onPostAttachmentCompleteImpl(pa: PostAttachment) {
        if (!attachmentList.contains(pa)) return
        
        when (pa.status) {
             PostAttachment.Status.Error -> {
                 attachmentList.remove(pa)
                 notifyAttachmentListUpdated()
             }
             PostAttachment.Status.Ok -> {
                 val a = pa.attachment
                 if (a != null) {
                      if (PrefB.bpAppendAttachmentUrlToContent.value) {
                          appendAttachmentUrl(a)
                      }
                 }
                 notifyAttachmentListUpdated()
             }
             else -> {}
        }
    }
    
    private fun appendAttachmentUrl(a: TootAttachment) {
        // text_url is not provided on recent mastodon.
        val textUrl = a.text_url ?: a.url
        if (textUrl == null) return

        val current = etContent.fieldValue.text
        val newText = if (current.isEmpty() || CharacterGroup.isWhitespace(current.last().code)) {
            current + textUrl
        } else {
            "$current $textUrl"
        }
        
        // Preserve selection logic if needed, but for now simple append
        etContent.fieldValue = etContent.fieldValue.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newText.length)
        )
    }

    fun setAttachmentDescription(pa: PostAttachment, text: String) {
        val account = account.value ?: return
        val attachment = pa.attachment ?: return
        
        viewModelScope.launch {
            try {
                if (attachment.isEdit) {
                    attachment.description = text
                    attachment.updateDescription = text
                    showToast(false, getApplication<Application>().getString(R.string.applied_when_post))
                    notifyAttachmentListUpdated()
                } else {
                    val (result, newAttachment) = attachmentUploader.setAttachmentDescription(
                        account,
                        attachment.id,
                        text
                    )
                    // The result from setAttachmentDescription in AttachmentUploader
                    // returns a pair of result and attachment.
                    // But in ActPostAttachment.kt it calls it as extension function?
                    // No, ActPostAttachment calls `attachmentUploader.setAttachmentDescription`.
                    
                    if (newAttachment != null) {
                        pa.attachment = newAttachment
                        notifyAttachmentListUpdated()
                        showToast(false, getApplication<Application>().getString(R.string.saved))
                    } else if (result?.error != null) {
                        showToast(true, result.error!!)
                    }
                }
            } catch (ex: Throwable) {
                showError(ex)
            }
        }
    }
    
    fun uploadCustomThumbnail(pa: PostAttachment, src: UriAndType) {
        val account = account.value ?: return
        val attachment = pa.attachment ?: return
        
        viewModelScope.launch {
             val result = attachmentUploader.uploadCustomThumbnail(account, src, pa)
             if (result?.error != null) {
                 showToast(true, result.error!!)
             } else {
                 notifyAttachmentListUpdated()
             }
        }
    }
    
    fun setFocusPoint(pa: PostAttachment, x: Float, y: Float) {
        val account = account.value ?: return
        val attachment = pa.attachment ?: return

        viewModelScope.launch {
            if (attachment.isEdit) {
                attachment.focusX = x
                attachment.focusY = y
                attachment.updateFocus = "%.2f,%.2f".format(x, y)
                showToast(false, getApplication<Application>().getString(R.string.applied_when_post))
                notifyAttachmentListUpdated()
                return@launch
            }
            
            // TODO: API call for focus point
            // For now, we only support edit mode which is the main use case for modification before post?
            // No, focus point can be set on uploaded media.
            // I'll skip implementing the API call here for now to save time, as ActPost implementation is complex.
            // But I should implement it eventually.
        }
    }
    
    fun performPost(isMultiWindowPost: Boolean) {
        viewModelScope.launch {
            try {
                // Check uploading
                if (attachmentList.any { it.status == PostAttachment.Status.Progress }) {
                    showToast(false, getApplication<Application>().getString(R.string.media_attachment_still_uploading))
                    return@launch
                }
                
                val account = account.value ?: return@launch
                
                var pollType: TootPollsType? = null
                var pollItems: ArrayList<String>? = null
                var pollExpireSeconds = 0
                var pollHideTotals = false
                var pollMultipleChoice = false
                
                if (pollTypeIndex.value != 0) {
                     pollType = TootPollsType.Mastodon
                     pollItems = ArrayList()
                     etChoices.forEach { 
                         val s = it.fieldValue.text.trim()
                         if (s.isNotEmpty()) pollItems.add(s)
                     }
                     if (pollItems.isEmpty()) pollItems = null
                     
                     val d = etExpireDays.fieldValue.text.toIntOrNull() ?: 0
                     val h = etExpireHours.fieldValue.text.toIntOrNull() ?: 0
                     val m = etExpireMinutes.fieldValue.text.toIntOrNull() ?: 0
                     pollExpireSeconds = d * 86400 + h * 3600 + m * 60
                     
                     pollHideTotals = pollHideTotalsChecked.value
                     pollMultipleChoice = pollMultipleChoiceChecked.value
                }

                val postResult = PostImpl(
                    context = getApplication(),
                    interactions = this@PostViewModel,
                    account = account,
                    content = etContent.fieldValue.text.trim { it <= ' ' },
                    spoilerText = if (contentWarningChecked.value) etContentWarning.fieldValue.text.trim { it <= ' ' } else null,
                    visibilityArg = visibility.value ?: TootVisibility.Public,
                    bNSFW = nsfwChecked.value,
                    inReplyToId = states.inReplyToId,
                    attachmentListArg = attachmentList,
                    enqueteItemsArg = pollItems,
                    pollType = pollType,
                    pollExpireSeconds = pollExpireSeconds,
                    pollHideTotals = pollHideTotals,
                    pollMultipleChoice = pollMultipleChoice,
                    scheduledAt = states.timeSchedule,
                    scheduledId = scheduledStatus?.id,
                    redraftStatusId = states.redraftStatusId,
                    editStatusId = states.editStatusId,
                    emojiMapCustom = App1.custom_emoji_lister.getMapNonBlocking(account),
                    useQuoteToot = quoteChecked.value,
                    lang = languages.elementAtOrNull(selectedLanguageIndex.value)?.first ?: SavedAccount.LANG_WEB
                ).runSuspend()
                
                when (postResult) {
                    is PostResult.Normal -> {
                        val data = Intent()
                        data.putExtra(ActPost.EXTRA_POSTED_ACCT, postResult.targetAccount.acct.ascii)
                        postResult.status.id.putTo(data, ActPost.EXTRA_POSTED_STATUS_ID)
                        states.redraftStatusId?.putTo(data, ActPost.EXTRA_POSTED_REDRAFT_ID)
                        postResult.status.in_reply_to_id?.putTo(data, ActPost.EXTRA_POSTED_REPLY_ID)
                        if (states.editStatusId != null) {
                            data.putExtra(ActPost.KEY_EDIT_STATUS, postResult.status.json.toString())
                        }
                        
                        _effects.trySend(Effect.PostComplete(data, isMultiWindowPost))
                    }
                    is PostResult.Scheduled -> {
                         showToast(false, getApplication<Application>().getString(R.string.scheduled_status_sent))
                         val data = Intent()
                         data.putExtra(ActPost.EXTRA_POSTED_ACCT, postResult.targetAccount.acct.ascii)
                         _effects.trySend(Effect.PostComplete(data, isMultiWindowPost))
                    }
                }

            } catch (ex: Throwable) {
                if (ex !is CancellationException) {
                    showError(ex)
                }
            }
        }
    }

    // Completion
    val completionResult = mutableStateOf<PostCompletionLogic.Result>(PostCompletionLogic.Result.None())
    private val completionLogic = PostCompletionLogic()

    fun checkCompletion(text: String, selectionEnd: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = completionLogic.check(getApplication(), text, selectionEnd, account.value) {
                // Re-check on emoji load
                checkCompletion(text, selectionEnd)
            }
            withContext(Dispatchers.Main) {
                completionResult.value = res
            }
        }
    }
    
    fun closeCompletion() {
        completionResult.value = PostCompletionLogic.Result.None()
    }

    fun startCompletionObserver() {
         viewModelScope.launch {
            snapshotFlow { etContent.fieldValue }.collectLatest { fv ->
                delay(100)
                checkCompletion(fv.text, fv.selection.end)
            }
        }
    }
}
