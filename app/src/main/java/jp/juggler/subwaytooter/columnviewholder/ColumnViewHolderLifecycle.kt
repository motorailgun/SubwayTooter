package jp.juggler.subwaytooter.columnviewholder

import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.lazy.LazyListState
import androidx.core.net.toUri
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.actmain.closePopup
import jp.juggler.subwaytooter.column.*
import jp.juggler.subwaytooter.compose.TimelineState
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.util.ScrollPosition
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.log.LogCategory
import jp.juggler.util.media.createResizedBitmap
import jp.juggler.util.ui.createRoundDrawable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val log = LogCategory("ColumnViewHolderLifeCycle")

fun ColumnViewHolder.closeBitmaps() {
    try {
        columnUiState.columnBgImageBitmap = null

        lastImageBitmap?.recycle()
        lastImageBitmap = null

        lastImageTask?.cancel()
        lastImageTask = null

        lastImageUri = null
    } catch (ex: Throwable) {
        log.e(ex, "closeBitmaps failed.")
    }
}

fun ColumnViewHolder.loadBackgroundImage(url: String?) {
    try {
        if (url == null || url.isEmpty() || PrefB.bpDontShowColumnBackgroundImage.value) {
            closeBitmaps()
            return
        }

        if (url == lastImageUri) return

        closeBitmaps()

        lastImageUri = url
        val activity = this.activity
        val screenW = activity.resources.displayMetrics.widthPixels
        val screenH = activity.resources.displayMetrics.heightPixels

        lastImageTask = launchMain {
            val bitmap = try {
                withContext(AppDispatchers.IO) {
                    try {
                        createResizedBitmap(
                            activity,
                            url.toUri(),
                            when {
                                screenW > screenH -> screenW
                                else -> screenH
                            },
                        )
                    } catch (ex: Throwable) {
                        log.e(ex, "createResizedBitmap failed.")
                        null
                    }
                }
            } catch (ex: Throwable) {
                log.w(ex, "loadBackgroundImage failed.")
                null
            }
            if (bitmap != null) {
                if (!coroutineContext.isActive || url != lastImageUri) {
                    bitmap.recycle()
                } else {
                    lastImageBitmap = bitmap
                    columnUiState.columnBgImageBitmap = bitmap
                }
            }
        }
    } catch (ex: Throwable) {
        log.e(ex, "loadBackgroundImage failed.")
    }
}

fun ColumnViewHolder.onPageDestroy(pageIdx: Int) {
    val column = this.column
    if (column != null) {
        ColumnViewHolder.log.d("onPageDestroy [$pageIdx] ${columnUiState.columnName}")
        saveScrollPosition()
        timelineState = null
        lazyListState = null
        column.removeColumnViewHolder(this)
        this.column = null
    }

    closeBitmaps()

    activity.closePopup()
}

fun ColumnViewHolder.onPageCreate(column: Column, pageIdx: Int, pageCount: Int) {
    bindingBusy = true
    try {
        this.column = column
        this.pageIdx = pageIdx

        ColumnViewHolder.log.d("onPageCreate [$pageIdx] ${column.getColumnName(true)}")

        val bSimpleList = !column.isConversation && PrefB.bpSimpleList.value
        val ui = columnUiState
        val isNotificationColumn = column.isNotificationColumn
        val bAllowFilter = column.canStatusFilter()

        // ──── Header ────
        ui.columnIndex = activity.getString(R.string.column_index, pageIdx + 1, pageCount)
        ui.columnStatus = "?"
        ui.columnIconResId = column.getIconId()

        // ──── Create Compose timeline state ────
        val scrollSave = column.scrollSave
        val newLazyListState = LazyListState(
            firstVisibleItemIndex = scrollSave?.adapterIndex ?: 0,
            firstVisibleItemScrollOffset = scrollSave?.offset ?: 0,
        )
        this.lazyListState = newLazyListState

        val newTimelineState = TimelineState()
        this.timelineState = newTimelineState

        // ──── Settings visibility ────
        ui.settingsVisible = false

        // Clear emoji invalidators
        for (invalidator in emojiQueryInvalidatorList) {
            invalidator.register(null)
        }
        emojiQueryInvalidatorList.clear()

        for (invalidator in extraInvalidatorList) {
            invalidator.register(null)
        }
        extraInvalidatorList.clear()

        // ──── Checkbox values ────
        ui.dontClose = column.dontClose
        ui.showMediaDescription = column.showMediaDescription
        ui.remoteOnly = column.remoteOnly
        ui.withAttachment = column.withAttachment
        ui.withHighlight = column.withHighlight
        ui.dontShowBoost = column.dontShowBoost
        ui.dontShowFollow = column.dontShowFollow
        ui.dontShowFavourite = column.dontShowFavourite
        ui.dontShowReply = column.dontShowReply
        ui.dontShowReaction = column.dontShowReaction
        ui.dontShowVote = column.dontShowVote
        ui.dontShowNormalToot = column.dontShowNormalToot
        ui.dontShowNonPublicToot = column.dontShowNonPublicToot
        ui.instanceLocal = column.instanceLocal
        ui.dontStreaming = column.dontStreaming
        ui.dontAutoRefresh = column.dontAutoRefresh
        ui.hideMediaDefault = column.hideMediaDefault
        ui.systemNotificationNotRelated = column.systemNotificationNotRelated
        ui.enableSpeech = column.enableSpeech
        ui.oldApi = column.useOldApi

        // ──── Text fields ────
        ui.regexFilterText = column.regexText
        ui.searchQuery = column.searchQuery
        ui.searchResolve = column.searchResolve

        if (column.type == ColumnType.AGG_BOOSTS) {
            ui.statusLoadLimit = column.aggStatusLimit.toString()
        }

        // ──── Checkbox visibility ────
        ui.showRemoteOnly = column.canRemoteOnly()
        ui.showWithAttachment = bAllowFilter
        ui.showWithHighlight = bAllowFilter
        ui.showRegexFilter = bAllowFilter
        ui.showLanguageFilter = bAllowFilter

        ui.showDontShowBoost = column.canFilterBoost()
        ui.showDontShowReply = column.canFilterReply()
        ui.showDontShowNormalToot = column.canFilterNormalToot()
        ui.showDontShowNonPublicToot = column.canFilterNonPublicToot()
        ui.showDontShowReaction = isNotificationColumn && column.isMisskey
        ui.showDontShowVote = isNotificationColumn
        ui.showDontShowFavourite = isNotificationColumn && !column.isMisskey
        ui.showDontShowFollow = isNotificationColumn

        ui.showInstanceLocal = column.type == ColumnType.HASHTAG

        ui.showDontStreaming = column.canStreamingType()
        ui.showDontAutoRefresh = column.canAutoRefresh()
        ui.showHideMediaDefault = column.canNSFWDefault()
        ui.showSystemNotificationNotRelated = column.isNotificationColumn
        ui.showEnableSpeech = column.canSpeech()
        ui.showOldApi = column.type == ColumnType.DIRECT_MESSAGES

        ui.showDeleteNotification = column.isNotificationColumn

        // ──── Search / list / agg bars ────
        ui.searchFormBgColor = colorSurfaceContainerHigh

        ui.aggBoostBarVisible = column.type == ColumnType.AGG_BOOSTS
        ui.listBarVisible = column.type == ColumnType.LIST_LIST

        when {
            column.isSearchColumn -> {
                ui.searchBarVisible = true
                ui.showEmojiQueryMode = false
                ui.showSearchInput = true
                ui.showSearchClear = PrefB.bpShowSearchClear.value
                ui.showResolveCheckbox = column.type == ColumnType.SEARCH
            }

            column.type == ColumnType.REACTIONS && column.accessInfo.isMastodon -> {
                ui.searchBarVisible = true
                ui.showEmojiQueryMode = true
                ui.showSearchInput = false
                ui.showSearchClear = false
                ui.showResolveCheckbox = false
            }

            else -> {
                ui.searchBarVisible = false
            }
        }

        // ──── Hashtag extra ────
        ui.showHashtagExtra = column.hasHashtagExtra
        ui.hashtagExtraAny = column.hashtagAny
        ui.hashtagExtraAll = column.hashtagAll
        ui.hashtagExtraNone = column.hashtagNone

        // Regex filter error display
        if (bAllowFilter) {
            isRegexValid()
        }

        // ──── Refresh layout state ────
        val canRefreshTop = column.canRefreshTopBySwipe()
        val canRefreshBottom = column.canRefreshBottomBySwipe()
        ui.canRefreshTop = canRefreshTop
        ui.canRefreshBottom = canRefreshBottom



        // ──── Refresh error ────
        ui.refreshErrorVisible = false
        bRefreshErrorWillShown = false

        // ──── Quick filter ────
        ui.quickFilterVisible = isNotificationColumn
        ui.quickFilterInsideSetting = PrefB.bpMoveNotificationsQuickFilter.value

        // ──── Announcements ────
        lastAnnouncementShown = -1L
        val announcementsBgColor = colorSurfaceContainerHigh
        ui.settingsBgColor = colorSurfaceContainerLow

        // ──── Connect column ────
        column.addColumnViewHolder(this)

        // ──── Build callbacks ────
        columnCallbacks = buildColumnCallbacks()

        // ──── Set compose content ────
        setComposeContent()

        // ──── Show content ────
        showColumnColor()
        showContent(reason = "onPageCreate", reset = true)
    } finally {
        bindingBusy = false
    }
}
