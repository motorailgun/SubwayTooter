package es.ariaontheplanet.quasar

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import es.ariaontheplanet.quasar.actmediaviewer.MediaViewerScreen
import es.ariaontheplanet.quasar.actmediaviewer.MediaViewerViewModel
import es.ariaontheplanet.quasar.api.entity.ServiceType
import es.ariaontheplanet.quasar.api.entity.TootAttachment
import es.ariaontheplanet.quasar.api.entity.TootAttachmentLike
import es.ariaontheplanet.quasar.api.entity.TootAttachmentMSP
import es.ariaontheplanet.quasar.api.entity.TootAttachmentType
import es.ariaontheplanet.quasar.api.entity.TootAttachment.Companion.tootAttachmentJson
import es.ariaontheplanet.quasar.dialog.actionsDialog
import es.ariaontheplanet.quasar.util.permissionSpecMediaDownload
import es.ariaontheplanet.quasar.util.requester
import es.ariaontheplanet.quasar.util.provideViewModel
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.asciiRegex
import jp.juggler.util.data.decodeJsonArray
import es.ariaontheplanet.quasar.api.entity.encodeJson
import jp.juggler.util.data.encodeUTF8
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.overrideActivityTransitionCompat
import jp.juggler.util.TransitionOverrideType
import jp.juggler.util.data.mayUri
import jp.juggler.util.network.MySslSocketFactory
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.LinkedList
import kotlin.math.max

class ActMediaViewer : ComponentActivity() {

    companion object {
        internal val log = LogCategory("ActMediaViewer")

        internal val download_history_list = LinkedList<DownloadHistory>()
        internal const val DOWNLOAD_REPEAT_EXPIRE = 3000L

        internal const val EXTRA_IDX = "idx"
        internal const val EXTRA_DATA = "data"
        internal const val EXTRA_SERVICE_TYPE = "serviceType"
        internal const val EXTRA_SHOW_DESCRIPTION = "showDescription"

        internal fun <T : TootAttachmentLike> encodeMediaList(list: ArrayList<T>?) =
            list?.encodeJson()?.toString() ?: "[]"

        internal fun decodeMediaList(src: String?) =
            ArrayList<TootAttachment>().apply {
                src?.decodeJsonArray()?.objectList()
                    ?.map { tootAttachmentJson(it) }
                    ?.let { addAll(it) }
            }

        fun open(
            activity: ActMain,
            showDescription: Boolean,
            serviceType: ServiceType,
            list: ArrayList<TootAttachmentLike>,
            idx: Int,
        ) {
            val intent = Intent(activity, ActMediaViewer::class.java)
            intent.putExtra(EXTRA_IDX, idx)
            intent.putExtra(EXTRA_SERVICE_TYPE, serviceType.ordinal)
            intent.putExtra(EXTRA_DATA, encodeMediaList(list))
            intent.putExtra(EXTRA_SHOW_DESCRIPTION, showDescription)
            activity.startActivity(intent)

            activity.overrideActivityTransitionCompat(
                TransitionOverrideType.Open,
                R.anim.slide_from_bottom,
                android.R.anim.fade_out,
            )
        }
    }

    class DownloadHistory(val time: Long, val url: String)

    private val viewModel: MediaViewerViewModel by lazy {
        provideViewModel(this) { MediaViewerViewModel(application) }
    }

    private val prDownload = permissionSpecMediaDownload.requester { 
        val state = viewModel.state.value
        val ta = state.mediaList.getOrNull(state.idx)
        if (ta != null) download(ta)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prDownload.register(this)

        App1.setActivityTheme(this, forceDark = true)
        
        if (savedInstanceState == null) {
            val idx = intent.getIntExtra(EXTRA_IDX, 0)
            val serviceTypeOrdinal = intent.getIntExtra(EXTRA_SERVICE_TYPE, 0)
            val dataString = intent.getStringExtra(EXTRA_DATA)
            val showDesc = intent.getBooleanExtra(EXTRA_SHOW_DESCRIPTION, true)
            
            val list = decodeMediaList(dataString)
            val serviceType = ServiceType.entries.getOrNull(serviceTypeOrdinal) ?: ServiceType.MASTODON
            
            viewModel.initialize(list, idx, serviceType, showDesc)
        }
        
        setContent {
            MediaViewerScreen(
                viewModel = viewModel,
                onDownload = { download(it) },
                onMore = { more(it) },
                onClose = { finish() }
            )
        }
    }

    override fun finish() {
        super.finish()
        overrideActivityTransitionCompat(
            TransitionOverrideType.Close,
            R.anim.fade_in,
            R.anim.slide_to_bottom,
        )
    }
    
    // ──────── Download Logic ────────

    private fun download(ta: TootAttachmentLike) {
        if (!prDownload.checkOrLaunch()) return

        val downLoadManager: DownloadManager = getSystemService(DOWNLOAD_SERVICE) as? DownloadManager
            ?: error("missing DownloadManager system service")

        val url = if (ta is TootAttachment) {
            ta.getLargeUrl()
        } else {
            null
        } ?: return

        // ボタン連打対策
        run {
            val now = SystemClock.elapsedRealtime()

            // 期限切れの履歴を削除
            val it = download_history_list.iterator()
            while (it.hasNext()) {
                val dh = it.next()
                if (now - dh.time >= DOWNLOAD_REPEAT_EXPIRE) {
                    // この履歴は十分に古いので捨てる
                    it.remove()
                } else if (url == dh.url) {
                    // 履歴に同じURLがあればエラーとする
                    showToast(false, R.string.dont_repeat_download_to_same_url)
                    return
                }
            }
            // 履歴の末尾に追加(履歴は古い順に並ぶ)
            download_history_list.addLast(DownloadHistory(now, url))
        }

        /**
         * Linuxはフォルダ中のファイルの名前の上限が255バイトと決まっている。
         * その上限に収まるように文字を切りたいが、それはUTF-8の区切りを考慮する必要がある。
         */
        fun shortenName(src: String, limitBytes: Int): String {
            val bytes = src.encodeUTF8()
            if (bytes.size <= limitBytes) return src
            // 制限バイト数の終端の一つ先
            var pos = limitBytes
            while (pos >= 0) {
                if (bytes[pos].toInt().and(0x80) == 0) {
                    // 現在位置がUTF-8の後続ではないなら、その手前までを返す
                    return String(bytes, 0, pos, StandardCharsets.UTF_8)
                }
                // 現在位置はUTF-8の文字の2バイト目以降なので、この手前で切ると文字が壊れる
                --pos
            }
            // UTF-8表現がおかしい
            return "media"
        }

        var fileName = url.mayUri()?.pathSegments?.findLast { !it.isNullOrBlank() }
            ?: url.replaceFirst("https?://".asciiRegex(), "")

        // Windowsでファイル名に使えない文字を避ける
        fileName = """[\\/|"<>?*:-]+""".toRegex().replace(fileName, "-")

        // 末尾から20文字以内にある最初のドット
        val extDotPos = fileName.indexOf('.', startIndex = max(0, fileName.length - 20))
        val fileNameMaxBytes = 255
        fileName = if (extDotPos == -1) {
            // 拡張子なし
            shortenName(fileName, fileNameMaxBytes)
        } else {
            // 拡張子の手前だけを短縮する
            val extPart = fileName.substring(extDotPos)
            val extPartBytes = extPart.encodeUTF8().size
            val namePart = shortenName(
                fileName.substring(0, extDotPos),
                fileNameMaxBytes - extPartBytes
            )
            "$namePart$extPart"
        }

        val request = DownloadManager.Request(url.toUri())
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setTitle(fileName)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)

        // Android 10 以降では allowScanningByMediaScanner は無視される
        if (Build.VERSION.SDK_INT < 29) {
            //メディアスキャンを許可する
            @Suppress("DEPRECATION")
            request.allowScanningByMediaScanner()
        }

        //ダウンロード中・ダウンロード完了時にも通知を表示する
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        downLoadManager.enqueue(request)
        showToast(false, R.string.downloading)
    }

    private fun share(action: String, url: String) {

        try {
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (action == Intent.ACTION_SEND) {
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, url)
            } else {
                intent.data = url.toUri()
            }

            startActivity(intent)
        } catch (ex: Throwable) {
            showToast(ex, "can't open app.")
        }
    }

    private fun copy(url: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            ?: throw NotImplementedError("missing ClipboardManager system service")

        try {
            //クリップボードに格納するItemを作成
            val item = ClipData.Item(url)

            val mimeType = arrayOfNulls<String>(1)
            mimeType[0] = ClipDescription.MIMETYPE_TEXT_PLAIN

            //クリップボードに格納するClipDataオブジェクトの作成
            val cd = ClipData(ClipDescription("media URL", mimeType), item)

            //クリップボードにデータを格納
            cm.setPrimaryClip(cd)

            showToast(false, R.string.url_is_copied)
        } catch (ex: Throwable) {
            showToast(ex, "clipboard access failed.")
        }
    }

    private fun more(ta: TootAttachmentLike) {
        launchAndShowError {
            actionsDialog {
                fun addMoreMenu(
                    captionPrefix: String,
                    url: String?,
                    @Suppress("SameParameterValue") action: String,
                ) {
                    val uri = url.mayUri() ?: return
                    val caption = getString(R.string.open_browser_of, captionPrefix)
                    action(caption) {
                        try {
                            val intent = Intent(action, uri)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        } catch (ex: Throwable) {
                            showToast(ex, "can't open app.")
                        }
                    }
                }
                if (ta is TootAttachment) {
                    val url = ta.getLargeUrl()
                    if (url != null) {
                        action(getString(R.string.open_in_browser)) {
                            share(Intent.ACTION_VIEW, url)
                        }
                        action(getString(R.string.share_url)) {
                            share(Intent.ACTION_SEND, url)
                        }
                        action(getString(R.string.copy_url)) {
                            copy(url)
                        }
                    }
                    addMoreMenu("url", ta.url, Intent.ACTION_VIEW)
                    addMoreMenu("remote_url", ta.remote_url, Intent.ACTION_VIEW)
                    addMoreMenu("preview_url", ta.preview_url, Intent.ACTION_VIEW)
                    addMoreMenu("preview_remote_url", ta.preview_remote_url, Intent.ACTION_VIEW)
                    addMoreMenu("text_url", ta.text_url, Intent.ACTION_VIEW)
                } else if (ta is TootAttachmentMSP) {
                    val url = ta.preview_url
                    action(getString(R.string.open_in_browser)) {
                        share(Intent.ACTION_VIEW, url)
                    }
                    action(getString(R.string.share_url)) {
                        share(Intent.ACTION_SEND, url)
                    }
                    action(getString(R.string.copy_url)) {
                        copy(url)
                    }
                }

                if (TootAttachmentType.Image == viewModel.state.value.mediaList.elementAtOrNull(viewModel.state.value.idx)?.type) {
                    action(getString(R.string.background_pattern)) { mediaBackgroundDialog() }
                }
            }
        }
    }

    private fun mediaBackgroundDialog() {
        launchAndShowError {
            actionsDialog(getString(R.string.background_pattern)) {}
        }
    }
}

