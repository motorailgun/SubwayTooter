package jp.juggler.subwaytooter.compose

import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.api.entity.TimelineItem
import jp.juggler.subwaytooter.api.entity.TootAccountRef
import jp.juggler.subwaytooter.api.entity.TootNotification
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.column.Column

/**
 * Callbacks for timeline item interactions.
 * Passed down to composable functions to handle clicks/long-clicks.
 */
data class TimelineCallbacks(
    // Avatar
    val onAvatarClick: (Column, TimelineItem?, TootAccountRef?) -> Unit = { _, _, _ -> },
    val onAvatarLongClick: (Column, TimelineItem?, TootAccountRef?) -> Unit = { _, _, _ -> },

    // Boost header
    val onBoostHeaderClick: (Column, TimelineItem?, TootAccountRef?) -> Unit = { _, _, _ -> },
    val onBoostHeaderLongClick: (Column, TimelineItem?, TootAccountRef?) -> Unit = { _, _, _ -> },

    // Reply header
    val onReplyHeaderClick: (Column, TimelineItem?, TootStatus?) -> Unit = { _, _, _ -> },
    val onReplyHeaderLongClick: (Column, TimelineItem?, TootStatus?) -> Unit = { _, _, _ -> },

    // Follow info
    val onFollowClick: (Column, TimelineItem?, TootAccountRef?) -> Unit = { _, _, _ -> },
    val onFollowLongClick: (Column, TimelineItem?, TootAccountRef?) -> Unit = { _, _, _ -> },
    val onFollowButtonClick: (Column, TimelineItem?, TootAccountRef?) -> Unit = { _, _, _ -> },
    val onFollowButtonLongClick: (Column, TimelineItem?, TootAccountRef?) -> Unit = { _, _, _ -> },

    // Follow request
    val onFollowRequestAccept: (Column, TootAccountRef?) -> Unit = { _, _ -> },
    val onFollowRequestDeny: (Column, TootAccountRef?) -> Unit = { _, _ -> },

    // Gap
    val onGapHeadClick: (Column, TimelineItem?) -> Unit = { _, _ -> },
    val onGapTailClick: (Column, TimelineItem?) -> Unit = { _, _ -> },

    // Tag / Search
    val onTagClick: (Column, TimelineItem?) -> Unit = { _, _ -> },
    val onTagLongClick: (Column, TimelineItem?) -> Unit = { _, _ -> },

    // List
    val onListClick: (Column, TimelineItem?) -> Unit = { _, _ -> },
    val onListMoreClick: (Column, TimelineItem?) -> Unit = { _, _ -> },

    // Filter
    val onFilterClick: (Column, TimelineItem?) -> Unit = { _, _ -> },

    // Media
    val onMediaClick: (TootStatus?, Int) -> Unit = { _, _ -> },
    val onMediaDescriptionClick: (String) -> Unit = {},
    val onShowMedia: (TimelineItem?) -> Unit = {},
    val onHideMedia: (TimelineItem?) -> Unit = {},

    // Content Warning
    val onContentWarningToggle: (TimelineItem?) -> Unit = {},

    // Card
    val onCardImageClick: (Column, TimelineItem?, TootStatus?) -> Unit = { _, _, _ -> },
    val onCardImageLongClick: (Column, TimelineItem?, TootStatus?) -> Unit = { _, _, _ -> },

    // Conversation
    val onConversationIconsClick: (Column, TimelineItem?) -> Unit = { _, _ -> },

    // Row click (simple list mode)
    val onItemClick: (Column, TimelineItem?, TootStatus?) -> Unit = { _, _, _ -> },

    // Status button bar
    val onReplyButtonClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onReplyButtonLongClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onBoostButtonClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onBoostButtonLongClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onFavouriteButtonClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onFavouriteButtonLongClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onBookmarkButtonClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onBookmarkButtonLongClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onQuoteButtonClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onReactionButtonClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onMoreButtonClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
    val onConversationButtonClick: (Column, TootStatus?, TootNotification?) -> Unit = { _, _, _ -> },
)

/**
 * Factory to create TimelineCallbacks wired to existing action methods in ActMain.
 */
fun createTimelineCallbacks(activity: ActMain): TimelineCallbacks {
    return TimelineCallbacks(
        // All callbacks are wired through the existing action infrastructure.
        // Individual callback implementations will be connected as the migration progresses.
        // For now, these provide the full callback surface.
    )
}
