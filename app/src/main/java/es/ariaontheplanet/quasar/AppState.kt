package es.ariaontheplanet.quasar

import android.content.Context
import android.os.Handler
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.ColumnEncoder
import es.ariaontheplanet.quasar.column.onMuteUpdated
import es.ariaontheplanet.quasar.pref.prefDevice
import es.ariaontheplanet.quasar.services.AppBusyState
import es.ariaontheplanet.quasar.services.ColumnRepository
import es.ariaontheplanet.quasar.services.DedupMode
import es.ariaontheplanet.quasar.services.TtsService
import es.ariaontheplanet.quasar.streaming.StreamManager
import es.ariaontheplanet.quasar.table.HighlightWord
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.table.daoSavedAccount
import kotlinx.coroutines.flow.StateFlow
import es.ariaontheplanet.quasar.util.NetworkStateTracker
import es.ariaontheplanet.quasar.util.PostAttachment
import jp.juggler.util.data.JsonException
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.toJsonArray
import jp.juggler.util.log.LogCategory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppState(
    internal val context: Context,
    internal val handler: Handler,
) : KoinComponent {

    private val busyState: AppBusyState by inject()
    private val columnRepo: ColumnRepository by inject()
    private val ttsService: TtsService by inject()

    // Reference to MainViewModel for accessing view holder registry
    // Set by ActMain during initialization
    var mainViewModel: es.ariaontheplanet.quasar.actmain.MainViewModel? = null

    companion object {

        internal val log = LogCategory("AppState")
    }

    internal val density: Float

    internal val streamManager: StreamManager

    internal var mediaThumbHeight: Int = 0

    // The single "current" account that drives the 4 fixed columns.
    // null when no account has been chosen (first-run / all accounts removed).
    val currentAccount: StateFlow<SavedAccount?>
        get() = columnRepo.currentAccount

    fun setCurrentAccount(account: SavedAccount?) {
        columnRepo.setCurrentAccount(account)
        context.prefDevice.currentAccountDbId = account?.db_id
    }

    /**
     * Loads the previously-selected account from prefs, falling back to the first
     * non-pseudo account in the DB. Returns the loaded account (or null if none exist).
     */
    fun loadCurrentAccount(): SavedAccount? {
        val saved = context.prefDevice.currentAccountDbId
            ?.let { daoSavedAccount.loadAccount(it) }
            ?.takeIf { !it.isPseudo }
        val chosen = saved
            ?: daoSavedAccount.loadAccountList().firstOrNull { !it.isPseudo }
        columnRepo.setCurrentAccount(chosen)
        return chosen
    }

    val columnList: List<Column> get() = columnRepo.columnList
    val columnCount: Int get() = columnRepo.columnCount
    fun column(i: Int) = columnRepo.column(i)
    fun columnIndex(column: Column?) = columnRepo.columnIndex(column)

    fun editColumnList(save: Boolean = true, block: (ArrayList<Column>) -> Unit) {
        columnRepo.editColumnList(block)
        if (save) saveColumnList()
    }

    internal var attachmentList: ArrayList<PostAttachment>? = null

    val networkTracker: NetworkStateTracker

    // initからプロパティにアクセスする場合、そのプロパティはinitより上で定義されていないとダメっぽい
    // そしてその他のメソッドからval プロパティにアクセスする場合、そのプロパティはメソッドより上で初期化されていないとダメっぽい
    init {

        this.density = context.resources.displayMetrics.density
        this.streamManager = StreamManager(this)
        this.networkTracker = NetworkStateTracker(context) {
            App1.custom_emoji_cache.onNetworkChanged()
            App1.custom_emoji_lister.onNetworkChanged()
        }
    }

    internal fun encodeColumnList() =
        columnList.mapIndexedNotNull { index, column ->
            try {
                val dst = JsonObject()
                ColumnEncoder.encode(column, dst, index)
                dst
            } catch (ex: JsonException) {
                log.e(ex, "encodeColumnList: encode failed at $index.")
                null
            }
        }.toJsonArray()

    // Fixed-columns refactor: the column list is rebuilt in-memory on every launch and
    // every account switch, so persisting it is pointless. Existing callers are kept
    // compiling; this is now a no-op aside from re-evaluating TTS.
    internal fun saveColumnList(bEnableSpeech: Boolean = true) {
        if (bEnableSpeech) enableSpeech()
    }

    fun isBusyFav(account: SavedAccount, status: TootStatus) =
        busyState.isBusyFav(account, status)

    fun setBusyFav(account: SavedAccount, status: TootStatus) =
        busyState.setBusyFav(account, status)

    fun resetBusyFav(account: SavedAccount, status: TootStatus) =
        busyState.resetBusyFav(account, status)

    fun isBusyBookmark(account: SavedAccount, status: TootStatus) =
        busyState.isBusyBookmark(account, status)

    fun setBusyBookmark(account: SavedAccount, status: TootStatus) =
        busyState.setBusyBookmark(account, status)

    fun resetBusyBookmark(account: SavedAccount, status: TootStatus) =
        busyState.resetBusyBookmark(account, status)

    fun isBusyBoost(account: SavedAccount, status: TootStatus) =
        busyState.isBusyBoost(account, status)

    fun setBusyBoost(account: SavedAccount, status: TootStatus) =
        busyState.setBusyBoost(account, status)

    fun resetBusyBoost(account: SavedAccount, status: TootStatus) =
        busyState.resetBusyBoost(account, status)

    fun enableSpeech() = ttsService.enableSpeech()

    internal fun addSpeech(status: TootStatus) = ttsService.addSpeech(status)

    internal fun addSpeech(text: String, dedupMode: DedupMode = DedupMode.Recent) =
        ttsService.addSpeech(text, dedupMode)

    internal fun sound(item: HighlightWord) = ttsService.sound(item)

    fun onMuteUpdated() {
        TootStatus.updateMuteData(force = true)
        columnList.forEach { it.onMuteUpdated() }
    }
}
