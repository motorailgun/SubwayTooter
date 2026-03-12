package jp.juggler.subwaytooter.actpost

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Wraps [TextFieldValue] in mutable Compose state,
 * exposing an EditText-compatible API for the actpost code.
 */
class TextEditState(initialText: String = "") {
    var fieldValue: TextFieldValue by mutableStateOf(TextFieldValue(initialText))

    /** Current text as CharSequence (no spans). */
    val text: CharSequence get() = fieldValue.text

    val selectionStart: Int get() = fieldValue.selection.start
    val selectionEnd: Int get() = fieldValue.selection.end

    /** Replace content and place cursor at end. Strips any spans. */
    fun setText(text: CharSequence) {
        val s = text.toString()
        fieldValue = TextFieldValue(s, selection = TextRange(s.length))
    }

    /** Move cursor / set selection. */
    fun setSelection(start: Int, end: Int = start) {
        val len = fieldValue.text.length
        fieldValue = fieldValue.copy(
            selection = TextRange(
                start.coerceIn(0, len),
                end.coerceIn(0, len),
            )
        )
    }
}
