package es.ariaontheplanet.quasar.actnickname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.table.AcctColor
import es.ariaontheplanet.quasar.table.daoAcctColor
import jp.juggler.util.coroutine.AppDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NicknameUiState(
    val nickname: String = "",
    val colorFg: Int = 0,
    val colorBg: Int = 0,
    val notificationSoundUri: String? = null,
)

class NicknameViewModel(
    private val acctAscii: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NicknameUiState())
    val uiState: StateFlow<NicknameUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val ac = withContext(AppDispatchers.IO) { daoAcctColor.load(acctAscii) }
            _uiState.update {
                it.copy(
                    nickname = ac.nickname ?: "",
                    colorFg = ac.colorFg,
                    colorBg = ac.colorBg,
                    notificationSoundUri = ac.notificationSound,
                )
            }
        }
    }

    fun setNickname(v: String) = _uiState.update { it.copy(nickname = v) }
    fun setColorFg(v: Int) = _uiState.update { it.copy(colorFg = v) }
    fun setColorBg(v: Int) = _uiState.update { it.copy(colorBg = v) }
    fun setNotificationSoundUri(v: String?) =
        _uiState.update { it.copy(notificationSoundUri = v) }

    suspend fun save() {
        val s = _uiState.value
        withContext(AppDispatchers.IO) {
            daoAcctColor.save(
                System.currentTimeMillis(),
                AcctColor(
                    acctAscii = acctAscii,
                    nicknameSave = s.nickname.trim { it <= ' ' },
                    colorFg = s.colorFg,
                    colorBg = s.colorBg,
                    notificationSoundSaved = s.notificationSoundUri ?: "",
                )
            )
        }
    }
}
