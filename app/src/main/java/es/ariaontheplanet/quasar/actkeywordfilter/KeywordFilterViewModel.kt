package es.ariaontheplanet.quasar.actkeywordfilter

import androidx.lifecycle.ViewModel
import es.ariaontheplanet.quasar.api.entity.EntityId
import es.ariaontheplanet.quasar.api.entity.TootFilter
import es.ariaontheplanet.quasar.api.entity.TootFilterContext
import es.ariaontheplanet.quasar.api.entity.TootFilterKeyword
import es.ariaontheplanet.quasar.table.SavedAccount
import jp.juggler.util.data.JsonArray
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.buildJsonArray
import jp.juggler.util.data.buildJsonObject
import jp.juggler.util.data.notEmpty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KeywordEntry(
    val stateId: Long,
    val serverKeywordId: String?,
    val keyword: String,
    val wholeWord: Boolean,
)

data class KeywordFilterUiState(
    val accountText: String = "",
    val titleText: String = "",
    val keywords: List<KeywordEntry> = emptyList(),
    val actionHide: Boolean = false,
    val contextHome: Boolean = true,
    val contextNotification: Boolean = true,
    val contextPublic: Boolean = true,
    val contextThread: Boolean = true,
    val contextProfile: Boolean = true,
    val expireSelection: Int = 0,
    val expireText: String = "",
    val showBackDialog: Boolean = false,
    val loading: Boolean = false,
)

class KeywordFilterViewModel : ViewModel() {

    var account: SavedAccount? = null
    var filterId: EntityId? = null
    var filterExpire: Long = 0L
    val deleteIds = mutableSetOf<String>()
    private var nextKeywordStateId = 0L

    private val _uiState = MutableStateFlow(KeywordFilterUiState())
    val uiState: StateFlow<KeywordFilterUiState> = _uiState.asStateFlow()

    fun setAccountText(v: String) = _uiState.update { it.copy(accountText = v) }
    fun setTitleText(v: String) = _uiState.update { it.copy(titleText = v) }
    fun setActionHide(v: Boolean) = _uiState.update { it.copy(actionHide = v) }
    fun setContextHome(v: Boolean) = _uiState.update { it.copy(contextHome = v) }
    fun setContextNotification(v: Boolean) = _uiState.update { it.copy(contextNotification = v) }
    fun setContextPublic(v: Boolean) = _uiState.update { it.copy(contextPublic = v) }
    fun setContextThread(v: Boolean) = _uiState.update { it.copy(contextThread = v) }
    fun setContextProfile(v: Boolean) = _uiState.update { it.copy(contextProfile = v) }
    fun setExpireSelection(v: Int) = _uiState.update { it.copy(expireSelection = v) }
    fun setExpireText(v: String) = _uiState.update { it.copy(expireText = v) }
    fun setShowBackDialog(v: Boolean) = _uiState.update { it.copy(showBackDialog = v) }
    fun setLoading(v: Boolean) = _uiState.update { it.copy(loading = v) }

    fun addKeyword(fk: TootFilterKeyword) {
        val entry = KeywordEntry(
            stateId = nextKeywordStateId++,
            serverKeywordId = fk.id?.toString()?.notEmpty(),
            keyword = fk.keyword.trim(),
            wholeWord = fk.whole_word,
        )
        _uiState.update { it.copy(keywords = it.keywords + entry) }
    }

    fun updateKeyword(stateId: Long, keyword: String? = null, wholeWord: Boolean? = null) {
        _uiState.update { state ->
            state.copy(
                keywords = state.keywords.map { k ->
                    if (k.stateId == stateId) {
                        k.copy(
                            keyword = keyword ?: k.keyword,
                            wholeWord = wholeWord ?: k.wholeWord,
                        )
                    } else k
                }
            )
        }
    }

    fun deleteKeyword(entry: KeywordEntry) {
        entry.serverKeywordId?.let { deleteIds.add(it) }
        _uiState.update { state ->
            state.copy(keywords = state.keywords.filterNot { it.stateId == entry.stateId })
        }
    }

    fun applyLoaded(filter: TootFilter) {
        filterExpire = filter.time_expires_at
        val kws = filter.keywords.ifEmpty { listOf(TootFilterKeyword(keyword = "")) }
        _uiState.update {
            it.copy(
                loading = false,
                contextHome = filter.hasContext(TootFilterContext.Home),
                contextNotification = filter.hasContext(TootFilterContext.Notifications),
                contextPublic = filter.hasContext(TootFilterContext.Public),
                contextThread = filter.hasContext(TootFilterContext.Thread),
                contextProfile = filter.hasContext(TootFilterContext.Account),
                actionHide = filter.hide,
                titleText = filter.title.notEmpty()
                    ?: filter.keywords.firstOrNull()?.keyword ?: "",
            )
        }
        // IDs for the keyword entries are assigned by addKeyword's counter
        kws.forEach { addKeyword(it) }
    }

    fun filterParamBase(expireDurationList: IntArray): JsonObject = buildJsonObject {
        val s = _uiState.value

        fun JsonArray.putContextChecked(checked: Boolean, fc: TootFilterContext) {
            if (checked) add(fc.apiName)
        }

        put("context", JsonArray().apply {
            putContextChecked(s.contextHome, TootFilterContext.Home)
            putContextChecked(s.contextNotification, TootFilterContext.Notifications)
            putContextChecked(s.contextPublic, TootFilterContext.Public)
            putContextChecked(s.contextThread, TootFilterContext.Thread)
            putContextChecked(s.contextProfile, TootFilterContext.Account)
        })

        when (val seconds = expireDurationList.elementAtOrNull(s.expireSelection) ?: -1) {
            // don't change
            -1 -> Unit
            // unlimited
            0 -> when {
                // already unlimited — don't change
                filterExpire <= 0L -> Unit
                // XXX: currently there is no way to remove expires from existing filter.
                else -> put("expires_in", Int.MAX_VALUE)
            }
            else -> put("expires_in", seconds)
        }
    }

    fun buildV1Params(expireDurationList: IntArray): JsonObject {
        val s = _uiState.value
        val ks = s.keywords.first()
        return filterParamBase(expireDurationList).apply {
            put("irreversible", s.actionHide)
            put("phrase", ks.keyword.trim())
            put("whole_word", ks.wholeWord)
        }
    }

    fun buildV2Params(expireDurationList: IntArray, title: String): JsonObject {
        val s = _uiState.value
        return filterParamBase(expireDurationList).apply {
            put("title", title)
            put("filter_action", if (s.actionHide) "hide" else "warn")
            put("keywords_attributes", buildJsonArray {
                s.keywords.forEach { k ->
                    add(buildJsonObject {
                        put("keyword", k.keyword.trim())
                        put("whole_word", k.wholeWord)
                        k.serverKeywordId?.let { put("id", it) }
                    })
                }
                deleteIds.forEach { id ->
                    add(buildJsonObject {
                        put("id", id)
                        put("_destroy", id)
                    })
                }
            })
        }
    }

    fun validateForSave(): SaveValidation {
        val s = _uiState.value
        if (s.keywords.isEmpty() || s.keywords.any { it.keyword.trim().isEmpty() }) {
            return SaveValidation.KeywordEmpty
        }
        val title = s.titleText.trim()
        if (title.isEmpty()) return SaveValidation.TitleEmpty
        return SaveValidation.Ok(title)
    }

    sealed interface SaveValidation {
        data object KeywordEmpty : SaveValidation
        data object TitleEmpty : SaveValidation
        data class Ok(val title: String) : SaveValidation
    }
}
