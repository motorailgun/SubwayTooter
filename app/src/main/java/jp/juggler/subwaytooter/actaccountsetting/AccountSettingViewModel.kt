package jp.juggler.subwaytooter.actaccountsetting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.juggler.subwaytooter.api.TootApiCallback
import jp.juggler.subwaytooter.api.TootApiClient
import jp.juggler.subwaytooter.api.TootParser
import jp.juggler.subwaytooter.api.entity.TootAccount
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoSavedAccount
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountSettingViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private val log = LogCategory("AccountSettingViewModel")
    }

    private val _account = MutableStateFlow<SavedAccount?>(null)
    val account = _account.asStateFlow()

    private val _tootAccount = MutableStateFlow<TootAccount?>(null)
    val tootAccount = _tootAccount.asStateFlow()

    private val _tootInstance = MutableStateFlow<TootInstance?>(null)
    val tootInstance = _tootInstance.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()

    // Revision counter to trigger UI updates for mutable SavedAccount
    private val _revision = MutableStateFlow(0)
    val revision = _revision.asStateFlow()

    fun load(dbId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val acct = daoSavedAccount.loadAccount(dbId)
                _account.value = acct
                if (acct != null) {
                    // initializeProfile() logic
                    initializeProfile(acct)
                }
            } catch (ex: Throwable) {
                _error.value = ex
                log.e(ex, "load failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun initializeProfile(a: SavedAccount) {
        if (a.isPseudo) return
        
        try {
            val client = TootApiClient(getApplication(), callback = object : TootApiCallback {
                override suspend fun isApiCancelled(): Boolean = false
                override suspend fun publishApiProgress(s: String) {}
            })
            client.account = a
            
            val accessToken = a.tokenJson?.string("access_token")
            if (accessToken == null) return

            val jsonObject = client.verifyAccount(
                accessToken = accessToken,
                outTokenInfo = null,
                misskeyVersion = a.misskeyVersion
            )
            
            val ta = TootParser(getApplication(), a).account(jsonObject)
            if (ta != null) {
                _tootAccount.value = ta
                
                // Update DB with new account info
                a.loginAccount = ta
                a.accountJson = jsonObject
                saveAccount()
            }
        } catch (ex: Throwable) {
            log.e(ex, "initializeProfile failed")
        }
    }
    
    fun requestRefresh() {
        _revision.value += 1
    }
    
    fun updateNotificationPull(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationPullEnable != enabled) {
            a.notificationPullEnable = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationPush(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationPushEnable != enabled) {
            a.notificationPushEnable = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateDefaultSensitive(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.defaultSensitive != enabled) {
            a.defaultSensitive = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateDontHideNsfw(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.dontHideNsfw != enabled) {
            a.dontHideNsfw = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateExpandCw(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.expandCw != enabled) {
            a.expandCw = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationMention(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationMention != enabled) {
            a.notificationMention = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationBoost(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationBoost != enabled) {
            a.notificationBoost = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationFavourite(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationFavourite != enabled) {
            a.notificationFavourite = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationFollow(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationFollow != enabled) {
            a.notificationFollow = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationFollowRequest(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationFollowRequest != enabled) {
            a.notificationFollowRequest = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationReaction(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationReaction != enabled) {
            a.notificationReaction = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationVote(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationVote != enabled) {
            a.notificationVote = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationPost(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationPost != enabled) {
            a.notificationPost = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationUpdate(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationUpdate != enabled) {
            a.notificationUpdate = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationStatusReference(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationStatusReference != enabled) {
            a.notificationStatusReference = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun updateNotificationSeveredRelationships(enabled: Boolean) {
        val a = _account.value ?: return
        if (a.notificationSeveredRelationships != enabled) {
            a.notificationSeveredRelationships = enabled
            saveAccount()
            requestRefresh()
        }
    }

    fun saveAccount() {
        val a = _account.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            daoSavedAccount.save(a)
        }
    }
}
