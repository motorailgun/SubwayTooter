package jp.juggler.subwaytooter.columnviewholder

import jp.juggler.subwaytooter.ActColumnCustomize
import jp.juggler.subwaytooter.App1
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.action.accountResendConfirmMail
import jp.juggler.subwaytooter.action.listCreate
import jp.juggler.subwaytooter.action.notificationDeleteAll
import jp.juggler.subwaytooter.actmain.closeColumn
import jp.juggler.subwaytooter.actmain.closeColumnAll
import jp.juggler.subwaytooter.api.entity.TootAnnouncement
import jp.juggler.subwaytooter.column.ColumnLoadReason
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.column.addColumnViewHolder
import jp.juggler.subwaytooter.column.fireShowContent
import jp.juggler.subwaytooter.column.isSearchColumn
import jp.juggler.subwaytooter.column.canReloadWhenRefreshTop
import jp.juggler.subwaytooter.column.startLoading
import jp.juggler.subwaytooter.column.startRefresh
import jp.juggler.subwaytooter.compose.ColumnCallbacks
import jp.juggler.subwaytooter.ui.languageFilter.LanguageFilterActivity.Companion.openLanguageFilterActivity
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.log.withCaption
import java.util.regex.Pattern

private val log = LogCategory("ColumnViewHolderActions")

fun ColumnViewHolder.onListListUpdated() {
    columnUiState.listName = ""
}

fun ColumnViewHolder.checkRegexFilterError(src: String): String? {
    try {
        if (src.isEmpty()) {
            return null
        }
        val m = Pattern.compile(src).matcher("")
        if (m.find()) {
            return activity.getString(R.string.regex_filter_matches_empty_string)
        }
        return null
    } catch (ex: Throwable) {
        val message = ex.message
        return if (message != null && message.isNotEmpty()) {
            message
        } else {
            ex.withCaption(activity.resources, R.string.regex_error)
        }
    }
}

fun ColumnViewHolder.isRegexValid(): Boolean {
    val s = columnUiState.regexFilterText
    val error = checkRegexFilterError(s)
    columnUiState.regexFilterError = error ?: ""
    return error == null
}

fun ColumnViewHolder.reloadBySettingChange() {
    activity.appState.saveColumnList()
    column?.startLoading(ColumnLoadReason.SettingChange)
}

/**
 * Handle checkbox state changes. Called from ColumnCallbacks.
 */
fun ColumnViewHolder.onCheckedChangedImpl(key: String, isChecked: Boolean) {
    val column = this.column ?: return

    if (bindingBusy) return

    column.addColumnViewHolder(this)

    when (key) {
        "dontClose" -> {
            column.dontClose = isChecked
            columnUiState.closeButtonEnabled = !isChecked
            activity.appState.saveColumnList()
        }

        "showMediaDescription" -> {
            column.showMediaDescription = isChecked
            reloadBySettingChange()
        }

        "withAttachment" -> {
            column.withAttachment = isChecked
            reloadBySettingChange()
        }

        "remoteOnly" -> {
            column.remoteOnly = isChecked
            reloadBySettingChange()
        }

        "withHighlight" -> {
            column.withHighlight = isChecked
            reloadBySettingChange()
        }

        "dontShowBoost" -> {
            column.dontShowBoost = isChecked
            reloadBySettingChange()
        }

        "dontShowReply" -> {
            column.dontShowReply = isChecked
            reloadBySettingChange()
        }

        "dontShowReaction" -> {
            column.dontShowReaction = isChecked
            reloadBySettingChange()
        }

        "dontShowVote" -> {
            column.dontShowVote = isChecked
            reloadBySettingChange()
        }

        "dontShowNormalToot" -> {
            column.dontShowNormalToot = isChecked
            reloadBySettingChange()
        }

        "dontShowNonPublicToot" -> {
            column.dontShowNonPublicToot = isChecked
            reloadBySettingChange()
        }

        "dontShowFavourite" -> {
            column.dontShowFavourite = isChecked
            reloadBySettingChange()
        }

        "dontShowFollow" -> {
            column.dontShowFollow = isChecked
            reloadBySettingChange()
        }

        "instanceLocal" -> {
            column.instanceLocal = isChecked
            reloadBySettingChange()
        }

        "dontStreaming" -> {
            column.dontStreaming = isChecked
            activity.appState.saveColumnList()
            activity.appState.streamManager.updateStreamingColumns()
        }

        "dontAutoRefresh" -> {
            column.dontAutoRefresh = isChecked
            activity.appState.saveColumnList()
        }

        "hideMediaDefault" -> {
            column.hideMediaDefault = isChecked
            activity.appState.saveColumnList()
            column.fireShowContent(reason = "HideMediaDefault in ColumnSetting", reset = true)
        }

        "systemNotificationNotRelated" -> {
            column.systemNotificationNotRelated = isChecked
            activity.appState.saveColumnList()
        }

        "enableSpeech" -> {
            column.enableSpeech = isChecked
            activity.appState.saveColumnList()
        }

        "oldApi" -> {
            column.useOldApi = isChecked
            reloadBySettingChange()
        }
    }
}

/**
 * Build ColumnCallbacks that wire Compose UI interactions to existing action methods.
 */
fun ColumnViewHolder.buildColumnCallbacks(): ColumnCallbacks = ColumnCallbacks(
    onHeaderClick = { scrollToTop2() },

    onColumnClose = {
        column?.let { activity.closeColumn(it) }
    },

    onColumnCloseLongClick = {
        activity.appState.columnIndex(column)?.let { activity.closeColumnAll(it) }
    },

    onColumnReload = {
        val column = this.column ?: return@ColumnCallbacks
        App1.custom_emoji_cache.clearErrorCache()

        if (column.isSearchColumn) {
            column.searchQuery = columnUiState.searchQuery
            column.searchResolve = columnUiState.searchResolve
        } else if (column.type == ColumnType.REACTIONS) {
            updateReactionQueryView()
        } else if (column.type == ColumnType.AGG_BOOSTS) {
            // aggStatusLimit is already set via onAggStart
        }
        columnUiState.isRefreshing = false
        column.startLoading(ColumnLoadReason.ForceReload)
    },

    onColumnSettingToggle = {
        val newShow = !columnUiState.settingsVisible
        showColumnSetting(newShow)
        if (newShow) hideAnnouncements()
    },

    onAnnouncementsToggle = { toggleAnnouncements() },
    onAnnouncementsPrev = {
        val column = this.column ?: return@ColumnCallbacks
        column.announcementId =
            TootAnnouncement.move(column.announcements, column.announcementId, -1)
        activity.appState.saveColumnList()
        showAnnouncements()
    },
    onAnnouncementsNext = {
        val column = this.column ?: return@ColumnCallbacks
        column.announcementId =
            TootAnnouncement.move(column.announcements, column.announcementId, +1)
        activity.appState.saveColumnList()
        showAnnouncements()
    },

    // Settings checkboxes
    onDontCloseChanged = { onCheckedChangedImpl("dontClose", it) },
    onShowMediaDescriptionChanged = { onCheckedChangedImpl("showMediaDescription", it) },
    onRemoteOnlyChanged = { onCheckedChangedImpl("remoteOnly", it) },
    onWithAttachmentChanged = { onCheckedChangedImpl("withAttachment", it) },
    onWithHighlightChanged = { onCheckedChangedImpl("withHighlight", it) },
    onDontShowBoostChanged = { onCheckedChangedImpl("dontShowBoost", it) },
    onDontShowFollowChanged = { onCheckedChangedImpl("dontShowFollow", it) },
    onDontShowFavouriteChanged = { onCheckedChangedImpl("dontShowFavourite", it) },
    onDontShowReplyChanged = { onCheckedChangedImpl("dontShowReply", it) },
    onDontShowReactionChanged = { onCheckedChangedImpl("dontShowReaction", it) },
    onDontShowVoteChanged = { onCheckedChangedImpl("dontShowVote", it) },
    onDontShowNormalTootChanged = { onCheckedChangedImpl("dontShowNormalToot", it) },
    onDontShowNonPublicTootChanged = { onCheckedChangedImpl("dontShowNonPublicToot", it) },
    onInstanceLocalChanged = { onCheckedChangedImpl("instanceLocal", it) },
    onDontStreamingChanged = { onCheckedChangedImpl("dontStreaming", it) },
    onDontAutoRefreshChanged = { onCheckedChangedImpl("dontAutoRefresh", it) },
    onHideMediaDefaultChanged = { onCheckedChangedImpl("hideMediaDefault", it) },
    onSystemNotificationNotRelatedChanged = {
        onCheckedChangedImpl("systemNotificationNotRelated", it)
    },
    onEnableSpeechChanged = { onCheckedChangedImpl("enableSpeech", it) },
    onOldApiChanged = { onCheckedChangedImpl("oldApi", it) },

    // Settings buttons
    onDeleteNotification = {
        column?.let { activity.notificationDeleteAll(it.accessInfo) }
    },
    onColorAndBackground = {
        activity.appState.columnIndex(column)?.let { colIdx ->
            activity.arColumnColor.launch(
                ActColumnCustomize.createIntent(activity, colIdx)
            )
        }
    },
    onLanguageFilter = {
        activity.appState.columnIndex(column)?.let { colIdx ->
            openLanguageFilterActivity(activity.arLanguageFilter, colIdx)
        }
    },

    // Regex filter
    onRegexFilterChanged = { text ->
        if (!bindingBusy && !isPageDestroyed) {
            columnUiState.regexFilterText = text
            val error = checkRegexFilterError(text)
            columnUiState.regexFilterError = error ?: ""
            if (error == null) {
                column?.regexText = text
                delayLoadByContentInvalidated()
            }
        }
    },

    // Hashtag extra
    onHashtagExtraAnyChanged = { text ->
        if (!bindingBusy && !isPageDestroyed) {
            column?.hashtagAny = text
            delayLoadByContentInvalidated()
        }
    },
    onHashtagExtraAllChanged = { text ->
        if (!bindingBusy && !isPageDestroyed) {
            column?.hashtagAll = text
            delayLoadByContentInvalidated()
        }
    },
    onHashtagExtraNoneChanged = { text ->
        if (!bindingBusy && !isPageDestroyed) {
            column?.hashtagNone = text
            delayLoadByContentInvalidated()
        }
    },

    // Search bar
    onSearchExecute = { query, resolve ->
        val column = this.column ?: return@ColumnCallbacks
        if (column.isSearchColumn) {
            column.searchQuery = query.trim()
            column.searchResolve = resolve
        }
        activity.appState.saveColumnList()
        column.startLoading(ColumnLoadReason.ForceReload)
    },
    onSearchClear = {
        val column = this.column ?: return@ColumnCallbacks
        column.searchQuery = ""
        column.searchResolve = columnUiState.searchResolve
        columnUiState.searchQuery = ""
        columnUiState.emojiQueryItems.clear()
        activity.appState.saveColumnList()
        column.startLoading(ColumnLoadReason.ForceReload)
    },
    onSearchResolveChanged = { checked ->
        columnUiState.searchResolve = checked
    },
    onEmojiAdd = { addEmojiQuery() },
    onEmojiQueryRemove = { /* handled via long-click in updateReactionQueryView */ },

    // Agg boost bar
    onAggStart = { limit ->
        val column = this.column ?: return@ColumnCallbacks
        if (limit > 0) {
            column.aggStatusLimit = limit
            activity.appState.saveColumnList()
            column.startLoading(ColumnLoadReason.ForceReload)
        }
    },

    // List bar
    onListAdd = { name ->
        val column = this.column ?: return@ColumnCallbacks
        val tv = name.trim()
        if (tv.isEmpty()) {
            activity.showToast(true, R.string.list_name_empty)
            return@ColumnCallbacks
        }
        launchMain { activity.listCreate(column.accessInfo, tv) }
    },

    // Quick filter
    onQuickFilterClick = { filter -> clickQuickFilter(filter) },

    // Announcements
    onReactionAdd = { /* handled by showReactionBox */ },
    onReactionClick = { /* handled by showReactionBox click listeners */ },

    // Body
    onRefresh = { isBottom ->
        val column = this.column ?: return@ColumnCallbacks
        column.addColumnViewHolder(this)

        if (!isBottom && column.canReloadWhenRefreshTop()) {
            columnUiState.isRefreshing = false
            activity.handler.post {
                this.column?.startLoading(ColumnLoadReason.PullToRefresh)
            }
            return@ColumnCallbacks
        }
        column.startRefresh(false, isBottom)
    },
    onRefreshErrorClick = {
        val column = this.column ?: return@ColumnCallbacks
        column.mRefreshLoadingErrorPopupState = 1 - column.mRefreshLoadingErrorPopupState
        showRefreshError()
    },
    onConfirmMail = {
        column?.let { activity.accountResendConfirmMail(it.accessInfo) }
    },
)
