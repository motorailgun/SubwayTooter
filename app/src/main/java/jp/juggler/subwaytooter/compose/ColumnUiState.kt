package jp.juggler.subwaytooter.compose

import android.graphics.Bitmap
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jp.juggler.subwaytooter.api.entity.TootAnnouncement
import jp.juggler.subwaytooter.api.entity.TootReaction
import jp.juggler.subwaytooter.column.Column

/**
 * Compose-side state holder for all column UI (header, settings, search, filter, announcements, body).
 * ColumnViewHolder updates these fields; Compose reads them and recomposes accordingly.
 */
@Stable
class ColumnUiState {
    // ──────── Column header ────────
    var columnName by mutableStateOf("")
    var columnContext by mutableStateOf("")
    var columnContextColorFg by mutableIntStateOf(0)
    var columnContextColorBg by mutableIntStateOf(0)
    var columnContextPadLr by mutableIntStateOf(0)
    var columnIndex by mutableStateOf("")
    var columnStatus by mutableStateOf<CharSequence>("")
    var columnIconResId by mutableIntStateOf(0)
    var headerNameColor by mutableIntStateOf(0)
    var headerPageNumberColor by mutableIntStateOf(0)
    var headerBgColor by mutableIntStateOf(0)
    var headerBgColorIsDefault by mutableStateOf(true)
    var closeButtonEnabled by mutableStateOf(true)

    // ──────── Column settings ────────
    var settingsVisible by mutableStateOf(false)
    var settingsBgColor by mutableIntStateOf(0)

    // Checkboxes
    var dontClose by mutableStateOf(false)
    var showMediaDescription by mutableStateOf(false)
    var remoteOnly by mutableStateOf(false)
    var withAttachment by mutableStateOf(false)
    var withHighlight by mutableStateOf(false)
    var dontShowBoost by mutableStateOf(false)
    var dontShowFollow by mutableStateOf(false)
    var dontShowFavourite by mutableStateOf(false)
    var dontShowReply by mutableStateOf(false)
    var dontShowReaction by mutableStateOf(false)
    var dontShowVote by mutableStateOf(false)
    var dontShowNormalToot by mutableStateOf(false)
    var dontShowNonPublicToot by mutableStateOf(false)
    var instanceLocal by mutableStateOf(false)
    var dontStreaming by mutableStateOf(false)
    var dontAutoRefresh by mutableStateOf(false)
    var hideMediaDefault by mutableStateOf(false)
    var systemNotificationNotRelated by mutableStateOf(false)
    var enableSpeech by mutableStateOf(false)
    var oldApi by mutableStateOf(false)

    // Visibility of checkboxes/rows
    var showRemoteOnly by mutableStateOf(false)
    var showWithAttachment by mutableStateOf(false)
    var showWithHighlight by mutableStateOf(false)
    var showRegexFilter by mutableStateOf(false)
    var showLanguageFilter by mutableStateOf(false)
    var showDontShowBoost by mutableStateOf(false)
    var showDontShowReply by mutableStateOf(false)
    var showDontShowNormalToot by mutableStateOf(false)
    var showDontShowNonPublicToot by mutableStateOf(false)
    var showDontShowReaction by mutableStateOf(false)
    var showDontShowVote by mutableStateOf(false)
    var showDontShowFavourite by mutableStateOf(false)
    var showDontShowFollow by mutableStateOf(false)
    var showInstanceLocal by mutableStateOf(false)
    var showDontStreaming by mutableStateOf(false)
    var showDontAutoRefresh by mutableStateOf(false)
    var showHideMediaDefault by mutableStateOf(false)
    var showSystemNotificationNotRelated by mutableStateOf(false)
    var showEnableSpeech by mutableStateOf(false)
    var showOldApi by mutableStateOf(false)
    var showDeleteNotification by mutableStateOf(false)

    // Regex filter
    var regexFilterText by mutableStateOf("")
    var regexFilterError by mutableStateOf("")

    // Hashtag extra fields
    var showHashtagExtra by mutableStateOf(false)
    var hashtagExtraAny by mutableStateOf("")
    var hashtagExtraAll by mutableStateOf("")
    var hashtagExtraNone by mutableStateOf("")

    // ──────── Search bar ────────
    var searchBarVisible by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var searchResolve by mutableStateOf(false)
    var showSearchClear by mutableStateOf(false)
    var showResolveCheckbox by mutableStateOf(false)
    // Emoji query mode (for reaction columns)
    var showEmojiQueryMode by mutableStateOf(false)
    var showSearchInput by mutableStateOf(true)
    val emojiQueryItems = mutableStateListOf<EmojiQueryItem>()

    // ──────── Agg boost bar ────────
    var aggBoostBarVisible by mutableStateOf(false)
    var statusLoadLimit by mutableStateOf("")

    // ──────── List bar ────────
    var listBarVisible by mutableStateOf(false)
    var listName by mutableStateOf("")

    // ──────── Quick filter ────────
    var quickFilterVisible by mutableStateOf(false)
    var quickFilterInsideSetting by mutableStateOf(false)
    var quickFilter by mutableIntStateOf(Column.QUICK_FILTER_ALL)
    var showQuickFilterReaction by mutableStateOf(false)
    var showQuickFilterFavourite by mutableStateOf(true)
    var quickFilterBgColor by mutableIntStateOf(0)
    var quickFilterFgColor by mutableIntStateOf(0)
    var quickFilterSelectedBgColor by mutableIntStateOf(0)

    // ──────── Announcements ────────
    var announcementButtonVisible by mutableStateOf(false)
    var announcementBadgeVisible by mutableStateOf(false)
    var announcementsBoxVisible by mutableStateOf(false)
    var announcementsExpanded by mutableStateOf(false)
    var announcementsCaption by mutableStateOf("")
    var announcementsIndex by mutableStateOf("")
    var announcementPeriod by mutableStateOf<String?>(null)
    var announcementContent by mutableStateOf<CharSequence>("")
    var announcementContentColor by mutableIntStateOf(0)
    var announcementEnablePaging by mutableStateOf(false)
    val announcementReactions = mutableStateListOf<AnnouncementReactionItem>()
    var lastAnnouncementShown by mutableLongStateOf(0L)

    // ──────── Column body ────────
    var contentColor by mutableIntStateOf(0)
    var columnBgColor by mutableIntStateOf(0)
    var columnBgImageAlpha by mutableStateOf(1f)
    var columnBgImageBitmap by mutableStateOf<Bitmap?>(null)
    var showLoading by mutableStateOf(false)
    var loadingMessage by mutableStateOf("")
    var showConfirmMail by mutableStateOf(false)
    var showRefreshLayout by mutableStateOf(false)

    // Refresh error
    var refreshErrorText by mutableStateOf("")
    var refreshErrorSingleLine by mutableStateOf(false)
    var refreshErrorVisible by mutableStateOf(false)

    // Refresh layout
    var canRefreshTop by mutableStateOf(false)
    var canRefreshBottom by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)

    // Search bg color for search/list/agg bars
    var searchFormBgColor by mutableIntStateOf(0)

    // ──────── revision counter for forcing recomposition ────────
    var uiRevision by mutableIntStateOf(0)

    fun forceRecompose() {
        uiRevision++
    }
}

/**
 * Represents a single emoji query item (for reaction search columns).
 */
data class EmojiQueryItem(
    val reaction: TootReaction,
    val displayText: CharSequence,
)

/**
 * Represents an announcement reaction button.
 */
data class AnnouncementReactionItem(
    val reaction: TootReaction,
    val displayText: CharSequence,
    val isMe: Boolean,
    val url: String? = null,
)
