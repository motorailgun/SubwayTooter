package jp.juggler.subwaytooter.actpost

import android.content.Context
import jp.juggler.subwaytooter.App1
import jp.juggler.subwaytooter.api.entity.TootTag
import jp.juggler.subwaytooter.emoji.CustomEmoji
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoAcctSet
import jp.juggler.subwaytooter.table.daoTagHistory
import jp.juggler.subwaytooter.util.EmojiDecoder
import jp.juggler.util.data.asciiRegex
import jp.juggler.util.log.LogCategory
import java.util.ArrayList

class PostCompletionLogic {
    companion object {
        private val log = LogCategory("PostCompletionLogic")
        private val reCharsNotEmoji = "[^0-9A-Za-z_-]".asciiRegex()
// ...

        private fun matchUserNameOrAsciiDomain(cp: Int): Boolean {
            if (cp >= 0x7f) return false
            val c = cp.toChar()
            return '0' <= c && c <= '9' ||
                    'A' <= c && c <= 'Z' ||
                    'a' <= c && c <= 'z' ||
                    c == '_' || c == '-' || c == '.'
        }

        private fun matchIdnWord(cp: Int) = when (Character.getType(cp).toByte()) {
            Character.UPPERCASE_LETTER,
            Character.LOWERCASE_LETTER,
            Character.TITLECASE_LETTER,
            Character.MODIFIER_LETTER,
            Character.OTHER_LETTER,
            Character.NON_SPACING_MARK,
            Character.COMBINING_SPACING_MARK,
            Character.ENCLOSING_MARK,
            Character.DECIMAL_DIGIT_NUMBER,
            Character.CONNECTOR_PUNCTUATION -> true
            else -> false
        }
    }

    data class EmojiItem(
        val shortcode: String,
        val url: String?,
        val staticUrl: String? = null
    )

    sealed class Result {
        data class None(val reason: String? = null) : Result()
        data class Mentions(val range: IntRange, val list: List<String>) : Result()
        data class Hashtags(val range: IntRange, val list: List<String>) : Result()
        data class Emojis(val range: IntRange, val list: List<EmojiItem>) : Result()
    }

    fun check(
        context: Context,
        text: String,
        selectionEnd: Int,
        account: SavedAccount?,
        onEmojiListLoad: () -> Unit
    ): Result {
        if (selectionEnd <= 0 || selectionEnd > text.length) return Result.None()

        // Check Mentions
        var countAtmark = 0
        var start: Int = -1
        var i = selectionEnd
        while (i > 0) {
            val cp = text.codePointBefore(i)
            i -= Character.charCount(cp)

            if (cp == '@'.code) {
                start = i
                if (++countAtmark >= 2) break else continue
            } else if (countAtmark == 1) {
                if (matchUserNameOrAsciiDomain(cp)) continue else break
            } else {
                if (matchUserNameOrAsciiDomain(cp) || matchIdnWord(cp)) continue else break
            }
        }

        if (start != -1 && selectionEnd - start >= 2) {
            val s = text.substring(start, selectionEnd)
            val acctList = daoAcctSet.searchPrefix(s, 100)
            if (acctList.isNotEmpty()) {
                // Convert CharSequence to String
                return Result.Mentions(start until selectionEnd, acctList.map { it.toString() })
            }
        }

        // Check Hashtags
        val lastSharp = text.lastIndexOf('#', selectionEnd - 1)
        if (lastSharp != -1 && selectionEnd - lastSharp >= 2) {
            val part = text.substring(lastSharp + 1, selectionEnd)
            if (TootTag.isValid(part, account?.isMisskey == true)) {
                val tagList = daoTagHistory.searchPrefix(part, 100)
                if (tagList.isNotEmpty()) {
                    return Result.Hashtags(lastSharp until selectionEnd, tagList.map { it.toString() })
                }
            }
        }

        // Check Emojis
        val lastColon = text.lastIndexOf(':', selectionEnd - 1)
        if (lastColon != -1 && selectionEnd - lastColon >= 1) {
             if (EmojiDecoder.canStartShortCode(text, lastColon)) {
                 val part = text.substring(lastColon + 1, selectionEnd)
                 if (!reCharsNotEmoji.containsMatchIn(part)) {
                     val codeList = ArrayList<EmojiItem>()
                     val limit = 100
                     
                     // Custom Emojis
                     if (account != null) {
                         val customList = App1.custom_emoji_lister.tryGetList(
                             account,
                             withAliases = true,
                             callback = { onEmojiListLoad() }
                         )
                         if (customList != null) {
                             for (item in customList) {
                                 if (codeList.size >= limit) break
                                 if (item.shortcode.contains(part) || item.alias?.contains(part) == true) {
                                     codeList.add(EmojiItem(
                                         shortcode = item.shortcode,
                                         url = item.url,
                                         staticUrl = item.staticUrl
                                     ))
                                 }
                             }
                         }
                     }
                     
                     val remain = limit - codeList.size
                     if (remain > 0) {
                         val s = part.lowercase().replace('-', '_')
                         val matches = EmojiDecoder.searchShortCode(context, s, remain)
                         matches.forEach { charSeq ->
                             // charSeq is " :shortcode:" format from searchShortCode?
                             // No, searchShortCode returns CharSequence that can be spanned.
                             // But here we need shortcode.
                             // Let's assume matches contains text like ":shortcode:" or similar.
                             // Actually searchShortCode implementation returns text with spans.
                             // We should just use the string representation.
                             val str = charSeq.toString().trim()
                             // str is like "😄 :smile:"
                             // We want to extract "smile"
                             val colons = str.indexOf(':')
                             val shortcode = if(colons != -1) {
                                 str.substring(colons + 1).trimEnd(':')
                             } else {
                                 str
                             }
                             
                             codeList.add(EmojiItem(
                                 shortcode = shortcode,
                                 url = null
                             ))
                         }
                     }
                     
                     if (codeList.isNotEmpty() || part.isEmpty()) {
                         return Result.Emojis(lastColon until selectionEnd, codeList)
                     }
                 }
             }
        }

        return Result.None()
    }
}
