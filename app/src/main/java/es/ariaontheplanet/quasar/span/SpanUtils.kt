package es.ariaontheplanet.quasar.span

import android.text.Spanned

fun selfStart(start: Int, text: CharSequence?, span: Any?): Boolean {
    return text is Spanned && text.getSpanStart(span) == start
}
