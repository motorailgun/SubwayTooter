package es.ariaontheplanet.quasar.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.text.Spannable
import androidx.core.content.ContextCompat
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.span.MyClickableSpan
import es.ariaontheplanet.quasar.table.HighlightWord
import es.ariaontheplanet.quasar.table.daoHighlightWord
import jp.juggler.util.coroutine.launchIO
import jp.juggler.util.coroutine.runOnMainLooper
import jp.juggler.util.data.asciiPattern
import jp.juggler.util.data.defaultLocale
import jp.juggler.util.data.mayUri
import jp.juggler.util.data.notBlank
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import java.lang.ref.WeakReference
import java.util.Arrays
import java.util.LinkedList
import java.util.Random
import kotlin.math.max

// Owns the TextToSpeech queue + notification-sound ringtone side effects.
// Extracted from AppState in Phase 4b.
class TtsService(
    private val context: Context,
    private val columnRepo: ColumnRepository,
) {

    companion object {
        private val log = LogCategory("TtsService")

        private const val TTS_STATUS_NONE = 0
        private const val TTS_STATUS_INITIALIZING = 1
        private const val TTS_STATUS_INITIALIZED = 2

        private const val TTS_SPEAK_WAIT_EXPIRE = 1000L * 100
        private val random = Random()

        private val reSpaces = "[\\s　]+".asciiPattern()

        private var utteranceIdSeed = 0

        private fun getStatusText(status: TootStatus?): Spannable? = when {
            status == null -> null
            status.decoded_spoiler_text.isNotEmpty() -> status.decoded_spoiler_text
            status.decoded_content.isNotEmpty() -> status.decoded_content
            else -> null
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private var willSpeechEnabled = false
    private var tts: TextToSpeech? = null
    private var ttsStatus = TTS_STATUS_NONE
    private var ttsSpeakStart = 0L
    private var ttsSpeakEnd = 0L

    private val voiceList = ArrayList<Voice>()

    private val ttsQueue = LinkedList<String>()

    private val duplicationCheck = LinkedList<DedupItem>()

    private var lastRingtone: WeakReference<Ringtone>? = null

    private var lastSound: Long = 0

    private val isTextToSpeechRequired: Boolean
        get() = columnRepo.columnList.any { it.enableSpeech } ||
            daoHighlightWord.hasTextToSpeechHighlightWord()

    private val ttsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            log.i("ttsReceiver onReceive action=${intent?.action}")
            when (intent?.action) {
                TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED -> {
                    ttsSpeakEnd = SystemClock.elapsedRealtime()
                    handler.post(procFlushSpeechQueue)
                }
            }
        }
    }

    private val procFlushSpeechQueue = object : Runnable {
        override fun run() {
            try {
                handler.removeCallbacks(this)

                val queueCount = ttsQueue.size
                if (queueCount <= 0) {
                    return
                }

                val tts = this@TtsService.tts
                if (tts == null) {
                    log.d("proc_flushSpeechQueue: tts is null")
                    return
                }

                val now = SystemClock.elapsedRealtime()

                if (ttsSpeakStart >= max(1L, ttsSpeakEnd)) {
                    // まだ終了イベントを受け取っていない
                    val expireRemain = TTS_SPEAK_WAIT_EXPIRE + ttsSpeakStart - now
                    if (expireRemain <= 0) {
                        log.d("proc_flushSpeechQueue: tts_speak wait expired.")
                        restartTTS()
                    } else {
                        log.d(
                            "proc_flushSpeechQueue: tts is speaking. queue_count=$queueCount, expire_remain=${
                                "%.3f".format(expireRemain.div(1000f))
                            }"
                        )
                        handler.postDelayed(this, expireRemain)
                        return
                    }
                    return
                }

                val sv = ttsQueue.removeFirst()
                log.d("proc_flushSpeechQueue: speak $sv")

                val voiceCount = voiceList.size
                if (voiceCount > 0) {
                    val n = random.nextInt(voiceCount)
                    tts.voice = voiceList[n]
                }

                ttsSpeakStart = now
                tts.speak(
                    sv,
                    TextToSpeech.QUEUE_ADD,
                    null, // Bundle params
                    (++utteranceIdSeed).toString() // String utteranceId
                )
            } catch (ex: Throwable) {
                log.e(ex, "proc_flushSpeechQueue catch exception.")
                restartTTS()
            }
        }

        fun restartTTS() {
            log.d("restart TextToSpeech")
            tts?.shutdown()
            tts = null
            ttsStatus = TTS_STATUS_NONE
            enableSpeech()
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun enableSpeech() {
        this.willSpeechEnabled = isTextToSpeechRequired

        if (willSpeechEnabled && tts == null && ttsStatus == TTS_STATUS_NONE) {
            ttsStatus = TTS_STATUS_INITIALIZING
            context.showToast(false, R.string.text_to_speech_initializing)
            log.d("initializing TextToSpeech…")

            launchIO {

                var tmpTts: TextToSpeech? = null

                val ttsInitListener: TextToSpeech.OnInitListener =
                    TextToSpeech.OnInitListener { status ->

                        val tts = tmpTts
                        if (tts == null || TextToSpeech.SUCCESS != status) {
                            context.showToast(
                                false,
                                R.string.text_to_speech_initialize_failed,
                                status
                            )
                            log.d("speech initialize failed. status=$status")
                            return@OnInitListener
                        }

                        runOnMainLooper {
                            if (!willSpeechEnabled) {
                                context.showToast(false, R.string.text_to_speech_shutdown)
                                log.d("shutdown TextToSpeech…")
                                tts.shutdown()
                            } else {
                                this@TtsService.tts = tts
                                ttsStatus = TTS_STATUS_INITIALIZED
                                ttsSpeakStart = 0L
                                ttsSpeakEnd = 0L

                                voiceList.clear()
                                try {
                                    val voiceSet = try {
                                        tts.voices
                                        // may raise NullPointerException is tts has no collection
                                    } catch (ignored: Throwable) {
                                        null
                                    }
                                    if (voiceSet.isNullOrEmpty()) {
                                        log.d("TextToSpeech.getVoices returns null or empty set.")
                                    } else {
                                        val lang = defaultLocale(context).toLanguageTag()
                                        for (v in voiceSet) {
                                            log.d("Voice ${v.name} ${v.locale.toLanguageTag()} $lang")
                                            if (lang != v.locale.toLanguageTag()) continue
                                            voiceList.add(v)
                                        }
                                    }
                                } catch (ex: Throwable) {
                                    log.e(ex, "TextToSpeech.getVoices raises exception.")
                                }

                                handler.post(procFlushSpeechQueue)

                                ContextCompat.registerReceiver(
                                    context,
                                    ttsReceiver,
                                    IntentFilter(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED),
                                    ContextCompat.RECEIVER_EXPORTED,
                                    // RECEIVER_NOT_EXPORTED だと読み上げ完了を受け取れない
                                )
                            }
                        }
                    }

                tmpTts = TextToSpeech(context, ttsInitListener)
            }
            return
        }

        if (!willSpeechEnabled && tts != null) {
            context.showToast(false, R.string.text_to_speech_shutdown)
            log.d("shutdown TextToSpeech…")
            tts?.shutdown()
            tts = null
            ttsStatus = TTS_STATUS_NONE
        }
    }

    fun addSpeech(status: TootStatus) {
        if (tts == null) return

        val text = getStatusText(status).notBlank() ?: return

        val spanList = text.getSpans(0, text.length, MyClickableSpan::class.java)
        if (spanList == null || spanList.isEmpty()) {
            addSpeech(text.toString())
            return
        }
        Arrays.sort(spanList) { a, b ->
            val aStart = text.getSpanStart(a)
            val bStart = text.getSpanStart(b)
            aStart - bStart
        }
        val strText = text.toString()
        val sb = StringBuilder()
        var lastEnd = 0
        var hasUrl = false
        for (span in spanList) {
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            if (start > lastEnd) {
                sb.append(strText.substring(lastEnd, start))
            }
            lastEnd = end
            //
            val spanText = strText.substring(start, end)
            if (spanText.isNotEmpty()) {
                val c = spanText[0]
                if (c == '#' || c == '@') {
                    // #hashtag や @user はそのまま読み上げる
                    sb.append(spanText)
                } else {
                    // それ以外はURL省略
                    hasUrl = true
                    sb.append(" ")
                }
            }
        }
        val textEnd = strText.length
        if (textEnd > lastEnd) {
            sb.append(strText.substring(lastEnd, textEnd))
        }
        if (hasUrl) {
            sb.append(context.getString(R.string.url_omitted))
        }
        addSpeech(sb.toString())
    }

    fun addSpeech(text: String, dedupMode: DedupMode = DedupMode.Recent) {
        if (tts == null) return

        val sv = reSpaces.matcher(text).replaceAll(" ").trim { it <= ' ' }
        if (sv.isEmpty()) return

        if (dedupMode != DedupMode.None) {
            synchronized(this) {
                val check = duplicationCheck.find { it.text.equals(sv, ignoreCase = true) }
                if (check == null) {
                    duplicationCheck.addLast(DedupItem(sv))
                    if (duplicationCheck.size > 60) duplicationCheck.removeFirst()
                } else {
                    val now = SystemClock.elapsedRealtime()
                    val delta = now - check.time
                    // 古い項目が残っていることがあるので、check.timeの更新は必須
                    check.time = now

                    if (dedupMode == DedupMode.Recent) return
                    if (dedupMode == DedupMode.RecentExpire && delta < 5000L) return
                }
            }
        }

        ttsQueue.add(sv)
        if (ttsQueue.size > 30) ttsQueue.removeFirst()

        handler.post(procFlushSpeechQueue)
    }

    private fun stopLastRingtone() {
        lastRingtone?.get()?.let { r ->
            try {
                r.stop()
            } catch (ex: Throwable) {
                log.e(ex, "stopLastRingtone failed.")
            } finally {
                lastRingtone = null
            }
        }
    }

    fun sound(item: HighlightWord) {
        // 短時間に何度もならないようにする
        val now = SystemClock.elapsedRealtime()
        if (now - lastSound < 500L) return
        lastSound = now

        stopLastRingtone()

        if (item.sound_type == HighlightWord.SOUND_TYPE_NONE) return

        fun Uri?.tryRingtone(): Boolean {
            try {
                if (this != null) {
                    RingtoneManager.getRingtone(context, this)?.let { ringTone ->
                        lastRingtone = WeakReference(ringTone)
                        ringTone.play()
                        return true
                    }
                }
            } catch (ex: Throwable) {
                log.e(ex, "tryRingtone failed.")
            }
            return false
        }

        if (item.sound_type == HighlightWord.SOUND_TYPE_CUSTOM &&
            item.sound_uri.mayUri().tryRingtone()
        ) return

        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).tryRingtone()
    }
}
