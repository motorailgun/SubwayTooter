package es.ariaontheplanet.quasar.actaccountsetting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.api.TootApiCallback
import es.ariaontheplanet.quasar.api.TootApiClient
import es.ariaontheplanet.quasar.api.TootParser
import es.ariaontheplanet.quasar.api.entity.TootAccount
import es.ariaontheplanet.quasar.api.entity.TootInstance
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.table.daoSavedAccount
import es.ariaontheplanet.quasar.api.entity.TootVisibility
import es.ariaontheplanet.quasar.push.PushBase
import es.ariaontheplanet.quasar.push.pushRepo
import jp.juggler.util.log.LogCategory
import jp.juggler.util.network.toPatch
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import es.ariaontheplanet.quasar.api.entity.TootAccount.Source

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

    // Profile editing state
    private val _editingDisplayName = MutableStateFlow("")
    val editingDisplayName = _editingDisplayName.asStateFlow()

    private val _editingNote = MutableStateFlow("")
    val editingNote = _editingNote.asStateFlow()

    private val _editingLocked = MutableStateFlow(false)
    val editingLocked = _editingLocked.asStateFlow()
    
    private val _editingVisibility = MutableStateFlow(TootVisibility.Public)
    val editingVisibility = _editingVisibility.asStateFlow()

    private val _editingDefaultSensitive = MutableStateFlow(false)
    val editingDefaultSensitive = _editingDefaultSensitive.asStateFlow()

    fun load(dbId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val acct = daoSavedAccount.loadAccount(dbId)
                _account.value = acct
                if (acct != null) {
                    // Load token info first (synchronously available)
                    loadTokenInfo()
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
                _editingDisplayName.value = ta.display_name
                _editingNote.value = ta.source?.note ?: ta.note ?: ""
                _editingLocked.value = ta.locked
                _editingVisibility.value = ta.source?.privacy?.let { TootVisibility.parseMastodon(it) } ?: TootVisibility.Public
                _editingDefaultSensitive.value = ta.source?.sensitive ?: false
                
                // Update DB with new account info
                a.loginAccount = ta
                a.accountJson = jsonObject
                saveAccount()
                
                // Load token info
                loadTokenInfo()
            }
        } catch (ex: Throwable) {
            log.e(ex, "initializeProfile failed")
        }
    }

    fun setDisplayName(v: String) { _editingDisplayName.value = v }
    fun setNote(v: String) { _editingNote.value = v }
    fun setLocked(v: Boolean) { _editingLocked.value = v }
    fun setVisibility(v: TootVisibility) { _editingVisibility.value = v }
    fun setDefaultSensitive(v: Boolean) { _editingDefaultSensitive.value = v }
    
    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri = _avatarUri.asStateFlow()
    
    private val _headerUri = MutableStateFlow<Uri?>(null)
    val headerUri = _headerUri.asStateFlow()

    fun setAvatar(uri: Uri?) { _avatarUri.value = uri }
    fun setHeader(uri: Uri?) { _headerUri.value = uri }

    fun confirmProfile() {
        val a = _account.value ?: return
        if (a.isPseudo) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val client = TootApiClient(getApplication(), callback = object : TootApiCallback {
                    override suspend fun isApiCancelled(): Boolean = false
                    override suspend fun publishApiProgress(s: String) {}
                })
                client.account = a
                
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                builder.addFormDataPart("display_name", _editingDisplayName.value)
                builder.addFormDataPart("note", _editingNote.value)
                builder.addFormDataPart("locked", _editingLocked.value.toString())
                builder.addFormDataPart("source[privacy]", _editingVisibility.value.strMastodon)
                builder.addFormDataPart("source[sensitive]", _editingDefaultSensitive.value.toString())
                
                val context = getApplication<Application>()
                
                _avatarUri.value?.let { uri ->
                    val file = copyUriToTempFile(context, uri, "avatar")
                    if (file != null) {
                        val mediaType = context.contentResolver.getType(uri)?.toMediaType()
                        builder.addFormDataPart("avatar", file.name, file.asRequestBody(mediaType))
                    }
                }

                _headerUri.value?.let { uri ->
                    val file = copyUriToTempFile(context, uri, "header")
                    if (file != null) {
                        val mediaType = context.contentResolver.getType(uri)?.toMediaType()
                        builder.addFormDataPart("header", file.name, file.asRequestBody(mediaType))
                    }
                }
                
                val result = client.request(
                    "/api/v1/accounts/update_credentials",
                    builder.build().toPatch()
                )
                
                if (result != null && result.jsonObject != null) {
                     // Update account from result
                     initializeProfile(a)
                     // Clear image Uris on success
                     _avatarUri.value = null
                     _headerUri.value = null
                } else {
                    _error.value = Throwable(result?.error ?: "Unknown error")
                }
                
            } catch (ex: Throwable) {
                _error.value = ex
                log.e(ex, "confirmProfile failed")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun copyUriToTempFile(context: android.content.Context, uri: Uri, prefix: String): File? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File.createTempFile(prefix, null, context.cacheDir)
            FileOutputStream(file).use { out ->
                stream.copyTo(out)
            }
            stream.close()
            file
        } catch (e: Exception) {
            log.e(e, "copyUriToTempFile failed")
            null
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
            updatePushSubscription(force = false)
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
    
    fun updatePushSubscription(force: Boolean) {
        val a = _account.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Determine if any notification is wanted
                val anyNotificationWanted = a.notificationBoost ||
                    a.notificationFavourite ||
                    a.notificationFollow ||
                    a.notificationMention ||
                    a.notificationReaction ||
                    a.notificationVote ||
                    a.notificationFollowRequest ||
                    a.notificationPost ||
                    a.notificationUpdate ||
                    a.notificationSeveredRelationships ||
                    a.notificationStatusReference

                // Use simple logger
                val subLogger = object : PushBase.SubscriptionLogger {
                     override val context: android.content.Context
                        get() = getApplication()
                     override fun i(msg: String) { log.i(msg) }
                     override fun e(msg: String) { log.e(msg) }
                     override fun e(ex: Throwable, msg: String) { log.e(ex, msg) }
                }

                getApplication<Application>().pushRepo.updateSubscription(
                    subLogger,
                    a,
                    willRemoveSubscription = !anyNotificationWanted,
                    forceUpdate = force
                )
            } catch (ex: Throwable) {
                log.e(ex, "updatePushSubscription failed")
            }
        }
    }

    // Token Management
    private val _tokenInfo = MutableStateFlow<Map<String, String?>>(emptyMap())
    val tokenInfo = _tokenInfo.asStateFlow()

    fun loadTokenInfo() {
        val a = _account.value ?: return
        try {
            val tokenJson = a.tokenJson
            if (tokenJson != null) {
                val tokenMap = mutableMapOf<String, String?>()
                tokenMap["access_token"] = tokenJson.string("access_token")?.take(20) + "..."
                tokenMap["token_type"] = tokenJson.string("token_type")
                tokenMap["scope"] = tokenJson.string("scope")
                tokenMap["created_at"] = tokenJson.long("created_at")?.let { 
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                        .format(java.util.Date(it * 1000))
                }
                _tokenInfo.value = tokenMap
            }
        } catch (ex: Throwable) {
            log.e(ex, "loadTokenInfo failed")
        }
    }

    private val _accountDeleted = MutableSharedFlow<Unit>()
    val accountDeleted = _accountDeleted.asSharedFlow()

    fun deleteAccount() {
        val a = _account.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
             _isLoading.value = true
             try {
                 daoSavedAccount.delete(a.db_id)
                 _accountDeleted.emit(Unit)
             } catch (ex: Throwable) {
                 _error.value = ex
                 log.e(ex, "deleteAccount failed")
             } finally {
                 _isLoading.value = false
             }
        }
    }
}
