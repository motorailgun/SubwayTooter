package es.ariaontheplanet.quasar.actpost

import es.ariaontheplanet.quasar.ActPost
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.TootTag
import es.ariaontheplanet.quasar.dialog.actionsDialog
import es.ariaontheplanet.quasar.dialog.launchEmojiPicker
import es.ariaontheplanet.quasar.emoji.CustomEmoji
import es.ariaontheplanet.quasar.emoji.UnicodeEmoji
import es.ariaontheplanet.quasar.pref.PrefB
import es.ariaontheplanet.quasar.util.EmojiDecoder
import jp.juggler.util.coroutine.launchAndShowError
import kotlin.math.min

fun ActPost.openEmojiPickerForContent() {
    launchEmojiPicker(
        this,
        account,
        closeOnSelected = PrefB.bpEmojiPickerCloseOnSelected.value,
    ) { emoji, bInstanceHasCustomEmoji ->
        val et = views.etContent
        val src = et.text.toString()
        val srcLen = src.length
        val start = min(srcLen, et.selectionStart)
        val end = min(srcLen, et.selectionEnd)
        val prefix = src.substring(0, start)
        val suffix = src.substring(end)
        val sep = EmojiDecoder.customEmojiSeparator()
        val insertText = buildString {
            when (emoji) {
                is CustomEmoji -> {
                    if (!EmojiDecoder.canStartShortCode(prefix, prefix.length)) append(sep)
                    append(":${emoji.shortcode}:")
                    if (sep != ' ') append(sep)
                }
                is UnicodeEmoji -> if (!bInstanceHasCustomEmoji) {
                    if (!EmojiDecoder.canStartShortCode(prefix, prefix.length)) append(sep)
                    append(":${emoji.unifiedName}:")
                    if (sep != ' ') append(sep)
                } else {
                    append(emoji.unifiedCode)
                }
            }
        }
        val newText = prefix + insertText + suffix
        et.setText(newText)
        et.setSelection(prefix.length + insertText.length)
    }
}

fun ActPost.openFeaturedTagList(list: List<TootTag>?) {
    launchAndShowError {
        actionsDialog(getString(R.string.featured_hashtags)) {
            list?.forEach { tag ->
                action("#${tag.name}") {
                    insertHashTagIntoContent(tag.name)
                }
            }
            action(getString(R.string.input_sharp_itself)) {
                val et = views.etContent
                val src = et.text.toString()
                val srcLen = src.length
                val start = min(srcLen, et.selectionStart)
                val end = min(srcLen, et.selectionEnd)
                val prefix = src.substring(0, start)
                val suffix = src.substring(end)
                val insertText = buildString {
                    if (!EmojiDecoder.canStartHashtag(prefix, prefix.length)) append(' ')
                    append('#')
                }
                et.setText(prefix + insertText + suffix)
                et.setSelection(prefix.length + insertText.length)
            }
        }
    }
}

private fun ActPost.insertHashTagIntoContent(tagWithoutSharp: String) {
    val et = views.etContent
    val src = et.text.toString()
    val srcLen = src.length
    val start = min(srcLen, et.selectionStart)
    val end = min(srcLen, et.selectionEnd)
    val prefix = src.substring(0, start)
    val suffix = src.substring(end)
    val insertText = buildString {
        if (!EmojiDecoder.canStartHashtag(prefix, prefix.length)) append(' ')
        append('#')
        append(tagWithoutSharp)
        append(' ')
    }
    et.setText(prefix + insertText + suffix)
    et.setSelection(prefix.length + insertText.length)
}
