package jp.juggler.subwaytooter.columnviewholder

import jp.juggler.subwaytooter.api.entity.TootReaction
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.subwaytooter.compose.EmojiQueryItem
import jp.juggler.subwaytooter.dialog.launchEmojiPicker
import jp.juggler.subwaytooter.emoji.CustomEmoji
import jp.juggler.subwaytooter.emoji.UnicodeEmoji
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.util.emojiSizeMode

fun ColumnViewHolder.addEmojiQuery(reaction: TootReaction? = null) {
    val column = this.column ?: return
    if (reaction == null) {
        launchEmojiPicker(activity, column.accessInfo, closeOnSelected = true) { emoji, _ ->
            val newReaction = when (emoji) {
                is UnicodeEmoji -> TootReaction(name = emoji.unifiedCode)
                is CustomEmoji -> TootReaction(
                    name = emoji.shortcode,
                    url = emoji.url,
                    staticUrl = emoji.staticUrl
                )
            }
            addEmojiQuery(newReaction)
        }
        return
    }
    val list = TootReaction.decodeEmojiQuery(column.searchQuery).toMutableList()
    list.add(reaction)
    column.searchQuery = TootReaction.encodeEmojiQuery(list)
    updateReactionQueryView()
    activity.appState.saveColumnList()
}

fun ColumnViewHolder.removeEmojiQuery(target: TootReaction?) {
    target ?: return
    val list = TootReaction.decodeEmojiQuery(column?.searchQuery).filter { it.name != target.name }
    column?.searchQuery = TootReaction.encodeEmojiQuery(list)
    updateReactionQueryView()
    activity.appState.saveColumnList()
}

fun ColumnViewHolder.updateReactionQueryView() {
    val column = this.column ?: return

    // Clear old invalidators
    for (invalidator in emojiQueryInvalidatorList) {
        invalidator.register(null)
    }
    emojiQueryInvalidatorList.clear()

    val ui = columnUiState
    ui.emojiQueryItems.clear()

    val options = DecodeOptions(
        activity,
        column.accessInfo,
        decodeEmoji = true,
        enlargeEmoji = DecodeOptions.emojiScaleReaction,
        enlargeCustomEmoji = DecodeOptions.emojiScaleReaction,
        emojiSizeMode = column.accessInfo.emojiSizeMode(),
    )

    TootReaction.decodeEmojiQuery(column.searchQuery).forEach { reaction ->
        val ssb = reaction.toSpannableStringBuilder(options, status = null)
        ui.emojiQueryItems.add(
            EmojiQueryItem(
                reaction = reaction,
                displayText = ssb,
            )
        )
    }
}
