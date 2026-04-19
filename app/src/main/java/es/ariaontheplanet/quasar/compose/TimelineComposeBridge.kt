package es.ariaontheplanet.quasar.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.action.clickBookmark
import es.ariaontheplanet.quasar.action.clickBoost
import es.ariaontheplanet.quasar.action.clickCardImage
import es.ariaontheplanet.quasar.action.clickConversation
import es.ariaontheplanet.quasar.action.clickDomainBlock
import es.ariaontheplanet.quasar.action.clickFavourite
import es.ariaontheplanet.quasar.action.clickFollowInfo
import es.ariaontheplanet.quasar.action.clickFollowRequestAccept
import es.ariaontheplanet.quasar.action.clickListMoreButton
import es.ariaontheplanet.quasar.action.clickListTl
import es.ariaontheplanet.quasar.action.clickQuote
import es.ariaontheplanet.quasar.action.clickReaction
import es.ariaontheplanet.quasar.action.clickReply
import es.ariaontheplanet.quasar.action.clickReplyInfo
import es.ariaontheplanet.quasar.action.followFromAnotherAccount
import es.ariaontheplanet.quasar.action.longClickTootTag
import es.ariaontheplanet.quasar.action.openFilterMenu
import es.ariaontheplanet.quasar.action.tagDialog
import es.ariaontheplanet.quasar.action.userProfileLocal
import es.ariaontheplanet.quasar.actmain.nextPosition
import es.ariaontheplanet.quasar.api.entity.TootConversationSummary
import es.ariaontheplanet.quasar.api.entity.TootDomainBlock
import es.ariaontheplanet.quasar.api.entity.TootFilter
import es.ariaontheplanet.quasar.api.entity.TootGap
import es.ariaontheplanet.quasar.api.entity.TootScheduled
import es.ariaontheplanet.quasar.api.entity.TootSearchGap
import es.ariaontheplanet.quasar.api.entity.TootTag
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.startGap
import es.ariaontheplanet.quasar.dialog.DlgContextMenu
import es.ariaontheplanet.quasar.util.openCustomTab
import jp.juggler.util.data.cast
import jp.juggler.util.log.LogCategory

private val log = LogCategory("TimelineComposeBridge")

/**
 * Creates a ComposeView that hosts the timeline LazyColumn.
 * This is used to replace the RecyclerView in ColumnViewHolder.
 */
fun createTimelineComposeView(
    activity: ActMain,
): ComposeView {
    return ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }
}

/**
 * Sets up the ComposeView content with the timeline.
 */
fun ComposeView.setTimelineContent(
    activity: ActMain,
    column: Column,
    timelineState: TimelineState,
    bSimpleList: Boolean,
    lazyListState: LazyListState,
) {
    val callbacks = buildTimelineCallbacks(activity)

    setContent {
        TimelineColumn(
            activity = activity,
            column = column,
            timelineState = timelineState,
            bSimpleList = bSimpleList,
            callbacks = callbacks,
            lazyListState = lazyListState,
        )
    }
}

/**
 * Builds TimelineCallbacks wired to existing action methods.
 */
fun buildTimelineCallbacks(activity: ActMain): TimelineCallbacks {
    return TimelineCallbacks(
        // Avatar
        onAvatarClick = { column, item, whoRef ->
            val pos = activity.nextPosition(column)
            whoRef?.let { ref ->
                if (column.accessInfo.isNA || column.accessInfo.isPseudo) {
                    DlgContextMenu(activity, column, ref, null, item?.cast(), null).show()
                } else {
                    activity.userProfileLocal(pos, column.accessInfo, ref.get())
                }
            }
        },
        onAvatarLongClick = { column, item, whoRef ->
            whoRef?.let { ref ->
                DlgContextMenu(activity, column, ref, null, item?.cast(), null).show()
            }
        },

        // Boost header
        onBoostHeaderClick = { column, item, whoRef ->
            val pos = activity.nextPosition(column)
            whoRef?.let { ref ->
                if (column.accessInfo.isPseudo) {
                    DlgContextMenu(activity, column, ref, null, item?.cast(), null).show()
                } else {
                    activity.userProfileLocal(pos, column.accessInfo, ref.get())
                }
            }
        },
        onBoostHeaderLongClick = { column, _, whoRef ->
            whoRef?.let { ref ->
                DlgContextMenu(activity, column, ref, null, null, null).show()
            }
        },

        // Reply header
        onReplyHeaderClick = { column, item, statusReply ->
            val pos = activity.nextPosition(column)
            activity.clickReplyInfo(pos, column.accessInfo, column.type, statusReply, null)
        },

        // Follow
        onFollowClick = { column, item, whoRef ->
            val pos = activity.nextPosition(column)
            activity.clickFollowInfo(pos, column.accessInfo, whoRef) { ref ->
                DlgContextMenu(activity, column, ref, null, item?.cast(), null).show()
            }
        },
        onFollowLongClick = { column, item, whoRef ->
            whoRef?.let { ref ->
                DlgContextMenu(activity, column, ref, null, item?.cast(), null).show()
            }
        },
        onFollowButtonClick = { column, item, whoRef ->
            val pos = activity.nextPosition(column)
            activity.clickFollowInfo(pos, column.accessInfo, whoRef, forceMenu = true) { ref ->
                DlgContextMenu(activity, column, ref, null, item?.cast(), null).show()
            }
        },
        onFollowButtonLongClick = { column, _, whoRef ->
            val pos = activity.nextPosition(column)
            whoRef?.get()?.let { who ->
                activity.followFromAnotherAccount(pos, column.accessInfo, who)
            }
        },

        // Follow request
        onFollowRequestAccept = { column, whoRef ->
            activity.clickFollowRequestAccept(column.accessInfo, whoRef, accept = true)
        },
        onFollowRequestDeny = { column, whoRef ->
            activity.clickFollowRequestAccept(column.accessInfo, whoRef, accept = false)
        },

        // Gap
        onGapHeadClick = { column, item ->
            item?.cast<TootGap>()?.let { column.startGap(it, isHead = true) }
        },
        onGapTailClick = { column, item ->
            item?.cast<TootGap>()?.let { column.startGap(it, isHead = false) }
        },

        // Tag
        onTagClick = { column, item ->
            val pos = activity.nextPosition(column)
            when (item) {
                is TootTag -> when (item.type) {
                    TootTag.TagType.Tag -> activity.tagDialog(
                        column.accessInfo,
                        pos,
                        item.url,
                        column.accessInfo.apiHost,
                        item.name,
                        tagInfo = item,
                    )

                    TootTag.TagType.Link -> item.url?.let {
                        openCustomTab(activity, pos, it, accessInfo = column.accessInfo)
                    }
                }

                is TootSearchGap -> column.startGap(item, isHead = true)
                is TootConversationSummary -> activity.clickConversation(
                    pos, column.accessInfo, null, summary = item
                )

                is TootGap -> {
                    when {
                        column.type.gapDirection(column, true) -> column.startGap(item, isHead = true)
                        column.type.gapDirection(column, false) -> column.startGap(item, isHead = false)
                    }
                }

                is TootDomainBlock -> activity.clickDomainBlock(column.accessInfo, item)
                is TootScheduled -> {
                    // clickScheduledToot - TODO
                }
            }
        },
        onTagLongClick = { column, item ->
            val pos = activity.nextPosition(column)
            when (item) {
                is TootTag -> activity.longClickTootTag(pos, column.accessInfo, item)
            }
        },

        // List
        onListClick = { column, item ->
            val pos = activity.nextPosition(column)
            activity.clickListTl(pos, column.accessInfo, item)
        },
        onListMoreClick = { column, item ->
            val pos = activity.nextPosition(column)
            activity.clickListMoreButton(pos, column.accessInfo, item)
        },

        // Filter
        onFilterClick = { column, item ->
            item?.cast<TootFilter>()?.let { activity.openFilterMenu(column.accessInfo, it) }
        },

        // Card
        onCardImageClick = { column, _, status ->
            val pos = activity.nextPosition(column)
            activity.clickCardImage(pos, column.accessInfo, status?.card)
        },
        onCardImageLongClick = { column, _, status ->
            val pos = activity.nextPosition(column)
            activity.clickCardImage(pos, column.accessInfo, status?.card, longClick = true)
        },

        // Conversation icons
        onConversationIconsClick = { column, item ->
            val pos = activity.nextPosition(column)
            item?.cast<TootConversationSummary>()?.let {
                activity.clickConversation(pos, column.accessInfo, null, summary = it)
            }
        },

        // Content warning
        onContentWarningToggle = { _ ->
            // Toggle is handled by Compose state; persistence happens in the composable
        },

        // Status buttons (using the same patterns as StatusButtons.kt onClick)
        onReplyButtonClick = { column, status, _ ->
            status?.let {
                activity.clickReply(column.accessInfo, it)
            }
        },
        onBoostButtonClick = { column, status, _ ->
            status?.let {
                activity.clickBoost(column.accessInfo, it, willToast = false)
            }
        },
        onFavouriteButtonClick = { column, status, _ ->
            status?.let {
                activity.clickFavourite(column.accessInfo, it, willToast = false)
            }
        },
        onBookmarkButtonClick = { column, status, _ ->
            status?.let {
                activity.clickBookmark(column.accessInfo, it, willToast = false)
            }
        },
        onQuoteButtonClick = { column, status, _ ->
            status?.let {
                activity.clickQuote(column.accessInfo, it)
            }
        },
        onReactionButtonClick = { column, status, _ ->
            status?.let {
                activity.clickReaction(column.accessInfo, column, it)
            }
        },
        onMoreButtonClick = { column, status, notification ->
            status?.let {
                DlgContextMenu(
                    activity,
                    column,
                    it.accountRef,
                    it,
                    notification,
                    null,
                ).show()
            }
        },
        onConversationButtonClick = { column, status, _ ->
            val pos = activity.nextPosition(column)
            status?.let {
                activity.clickConversation(pos, column.accessInfo, status = it)
            }
        },
    )
}
