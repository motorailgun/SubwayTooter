package es.ariaontheplanet.quasar.columnviewholder

import es.ariaontheplanet.quasar.api.entity.TootReaction
import es.ariaontheplanet.quasar.column.getContentColor
import es.ariaontheplanet.quasar.compose.EmojiQueryItem
import es.ariaontheplanet.quasar.dialog.launchEmojiPicker
import es.ariaontheplanet.quasar.emoji.CustomEmoji
import es.ariaontheplanet.quasar.emoji.UnicodeEmoji
import es.ariaontheplanet.quasar.util.DecodeOptions
import es.ariaontheplanet.quasar.util.emojiSizeMode

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
