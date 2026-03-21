package jp.juggler.subwaytooter.actpost

import android.app.Application
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
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.api.entity.TootVisibility
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.util.PostAttachment
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

class PostViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private val log = LogCategory("PostViewModel")
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

    // Focused Edit Field Index
    val focusedEditField = mutableStateOf(-1)

}
