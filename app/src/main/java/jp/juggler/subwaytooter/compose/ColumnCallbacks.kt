package jp.juggler.subwaytooter.compose

/**
 * Callbacks for column UI interactions.
 * These are wired by ColumnViewHolder to existing action methods.
 */
data class ColumnCallbacks(
    // Header
    val onHeaderClick: () -> Unit = {},
    val onColumnClose: () -> Unit = {},
    val onColumnCloseLongClick: () -> Unit = {},
    val onColumnReload: () -> Unit = {},
    val onColumnSettingToggle: () -> Unit = {},
    val onAnnouncementsToggle: () -> Unit = {},
    val onAnnouncementsPrev: () -> Unit = {},
    val onAnnouncementsNext: () -> Unit = {},

    // Settings checkboxes
    val onDontCloseChanged: (Boolean) -> Unit = {},
    val onShowMediaDescriptionChanged: (Boolean) -> Unit = {},
    val onRemoteOnlyChanged: (Boolean) -> Unit = {},
    val onWithAttachmentChanged: (Boolean) -> Unit = {},
    val onWithHighlightChanged: (Boolean) -> Unit = {},
    val onDontShowBoostChanged: (Boolean) -> Unit = {},
    val onDontShowFollowChanged: (Boolean) -> Unit = {},
    val onDontShowFavouriteChanged: (Boolean) -> Unit = {},
    val onDontShowReplyChanged: (Boolean) -> Unit = {},
    val onDontShowReactionChanged: (Boolean) -> Unit = {},
    val onDontShowVoteChanged: (Boolean) -> Unit = {},
    val onDontShowNormalTootChanged: (Boolean) -> Unit = {},
    val onDontShowNonPublicTootChanged: (Boolean) -> Unit = {},
    val onInstanceLocalChanged: (Boolean) -> Unit = {},
    val onDontStreamingChanged: (Boolean) -> Unit = {},
    val onDontAutoRefreshChanged: (Boolean) -> Unit = {},
    val onHideMediaDefaultChanged: (Boolean) -> Unit = {},
    val onSystemNotificationNotRelatedChanged: (Boolean) -> Unit = {},
    val onEnableSpeechChanged: (Boolean) -> Unit = {},
    val onOldApiChanged: (Boolean) -> Unit = {},

    // Settings buttons
    val onDeleteNotification: () -> Unit = {},
    val onColorAndBackground: () -> Unit = {},
    val onLanguageFilter: () -> Unit = {},

    // Regex filter
    val onRegexFilterChanged: (String) -> Unit = {},

    // Hashtag extra
    val onHashtagExtraAnyChanged: (String) -> Unit = {},
    val onHashtagExtraAllChanged: (String) -> Unit = {},
    val onHashtagExtraNoneChanged: (String) -> Unit = {},

    // Search bar
    val onSearchExecute: (String, Boolean) -> Unit = { _, _ -> },
    val onSearchClear: () -> Unit = {},
    val onSearchResolveChanged: (Boolean) -> Unit = {},
    val onEmojiAdd: () -> Unit = {},
    val onEmojiQueryRemove: (Int) -> Unit = {},

    // Agg boost bar
    val onAggStart: (Int) -> Unit = {},

    // List bar
    val onListAdd: (String) -> Unit = {},

    // Quick filter
    val onQuickFilterClick: (Int) -> Unit = {},

    // Announcements
    val onReactionAdd: (Int) -> Unit = {},
    val onReactionClick: (Int) -> Unit = {},

    // Body
    val onRefresh: (Boolean) -> Unit = {},  // true = bottom, false = top
    val onRefreshErrorClick: () -> Unit = {},
    val onConfirmMail: () -> Unit = {},
)
