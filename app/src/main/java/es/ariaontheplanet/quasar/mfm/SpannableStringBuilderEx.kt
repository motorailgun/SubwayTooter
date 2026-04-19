package es.ariaontheplanet.quasar.mfm

import android.text.SpannableStringBuilder
import es.ariaontheplanet.quasar.api.entity.TootMention
import java.util.ArrayList

// デコード結果にはメンションの配列を含む。TootStatusのパーサがこれを回収する。
class SpannableStringBuilderEx(
    var mentions: ArrayList<TootMention>? = null,
) : SpannableStringBuilder()
