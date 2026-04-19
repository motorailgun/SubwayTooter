package es.ariaontheplanet.quasar.actmediaviewer

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.TootAttachment
import es.ariaontheplanet.quasar.api.entity.TootAttachmentType
import es.ariaontheplanet.quasar.api.entity.ServiceType
import es.ariaontheplanet.quasar.util.reUrlGif
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.withCaption
import jp.juggler.util.media.imageOrientation
import jp.juggler.util.media.resolveOrientation
import jp.juggler.util.media.rotateSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.ByteArrayInputStream
import kotlin.math.max
import kotlin.math.min

class MediaViewerViewModel(
    application: Application,
) : AndroidViewModel(application) {

    companion object {
        private val log = LogCategory("MediaViewerViewModel")
        
        private fun checkMaxBitmapSize(): Int {
            var bitsMin = 10 // 1024 px
            var bitsMax = 16 // 65536 px
            while (bitsMax > bitsMin) {
                val bitsMid = (bitsMin + bitsMax + 1).shr(1)
                val px = 1.shl(bitsMid)
                val canCreate = try {
                    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
                    bitmap.recycle()
                    true
                } catch (ex: Throwable) {
                    false
                }
                when {
                    canCreate -> bitsMin = bitsMid
                    else -> bitsMax = bitsMid - 1
                }
            }
            return min(8192, 1.shl(bitsMin))
        }

        private val maxBitmapSize by lazy { checkMaxBitmapSize() }
    }

    data class State(
        val mediaList: List<TootAttachment> = emptyList(),
        val idx: Int = 0,
        val serviceType: ServiceType = ServiceType.MASTODON,
        val showDescription: Boolean = false,
        val description: String? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        
        // Image state
        val bitmap: Bitmap? = null,
        val originalWidth: Int = 0,
        val originalHeight: Int = 0,
        
        // Video/Web state
        val videoUrl: String? = null,
        val webUrl: String? = null,
        val isVideo: Boolean = false,
        val isMuted: Boolean = false,
        val playerPos: Long = 0L,
        
        // Status text (zoom level etc)
        val statusText: String? = null,
    )

    fun savePlayerPosition(pos: Long) {
        _state.update { it.copy(playerPos = pos) }
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    fun initialize(
        list: List<TootAttachment>,
        idx: Int,
        serviceType: ServiceType,
        showDescription: Boolean,
    ) {
        _state.update {
            it.copy(
                mediaList = list,
                idx = if (idx in list.indices) idx else 0,
                serviceType = serviceType,
                showDescription = showDescription
            )
        }
        load()
    }

    fun loadDelta(delta: Int) {
        val s = _state.value
        val size = s.mediaList.size
        if (size < 2) return
        val newIdx = (s.idx + size + delta) % size
        
        _state.update { it.copy(idx = newIdx) }
        load()
    }
    
    fun toggleMute() {
        _state.update { it.copy(isMuted = !it.isMuted) }
    }
    
    fun toggleDescription() {
        _state.update { it.copy(showDescription = !it.showDescription) }
    }
    
    fun setStatusText(text: String?) {
        _state.update { it.copy(statusText = text) }
    }

    private fun load() {
        loadJob?.cancel()
        
        val s = _state.value
        if (s.idx < 0 || s.idx >= s.mediaList.size) {
             _state.update { it.copy(error = getApplication<Application>().getString(R.string.media_attachment_empty)) }
            return
        }
        
        val ta = s.mediaList[s.idx]
        val desc = ta.description
        
        _state.update { 
            it.copy(
                description = desc,
                isLoading = true,
                error = null,
                bitmap = null,
                videoUrl = null,
                webUrl = null,
                isVideo = false,
                statusText = null,
                playerPos = 0L,
            )
        }

        when (ta.type) {
            TootAttachmentType.Unknown -> loadOther(ta)
            TootAttachmentType.Image -> when {
                reUrlGif.containsMatchIn(ta.remote_url ?: "") -> loadOther(ta)
                else -> loadImage(ta)
            }
            TootAttachmentType.Video,
            TootAttachmentType.GIFV,
            TootAttachmentType.Audio -> loadVideo(ta)
        }
    }
    
    private fun loadOther(ta: TootAttachment) {
        val urlList = ta.getLargeUrlList()
        if (urlList.isEmpty()) {
            _state.update { it.copy(error = "missing media attachment url.", isLoading = false) }
            return
        }
        val url = urlList.first()
        _state.update { 
            it.copy(
                webUrl = url,
                statusText = "${ta.type.id} ${url}",
                isLoading = false
            )
        }
    }
    
    private fun loadVideo(ta: TootAttachment) {
        val url = ta.getLargeUrl()
        if (url == null) {
            _state.update { it.copy(error = "missing media attachment url.", isLoading = false) }
            return
        }
        _state.update { 
            it.copy(
                videoUrl = url,
                isVideo = true,
                isLoading = false,
            )
        }
    }
    
    private fun loadImage(ta: TootAttachment) {
        val urlList = ta.getLargeUrlList()
        if (urlList.isEmpty()) {
             _state.update { it.copy(error = "missing media attachment url.", isLoading = false) }
            return
        }
        
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                var bitmapResult: Bitmap? = null
                var lastError: String? = null
                
                val client = App1.ok_http_client_media_viewer
                val options = BitmapFactory.Options()
                
                for (url in urlList) {
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .cacheControl(App1.CACHE_CONTROL)
                            .addHeader("Accept", "image/webp,image/*,*/*;q=0.8")
                            .build()
                        
                        client.newCall(request).execute().use { response ->
                             if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
                             val body = response.body ?: throw java.io.IOException("Empty body")
                             
                             val bytes = body.bytes()
                             if (bytes.size >= 50000000) error("media attachment is larger than 50000000")
                             
                             val (b, error) = decodeBitmap(options, bytes)
                             if (b != null) {
                                 bitmapResult = b
                                 return@use
                             }
                             if (error != null) lastError = error
                        }
                        if (bitmapResult != null) break
                    } catch (ex: Throwable) {
                         if (ex is CancellationException) throw ex
                         lastError = "load error. ${ex.withCaption()} url=$url"
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (bitmapResult != null) {
                        _state.update { 
                            it.copy(
                                bitmap = bitmapResult, 
                                isLoading = false,
                                originalWidth = options.outWidth,
                                originalHeight = options.outHeight
                            ) 
                        }
                    } else {
                        _state.update { it.copy(error = lastError ?: "load failed", isLoading = false) }
                    }
                }
            } catch (ex: CancellationException) {
                // ignore
            } catch (ex: Throwable) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(error = ex.withCaption("load failed."), isLoading = false) }
                }
            }
        }
    }
    
    private fun decodeBitmap(
        options: BitmapFactory.Options,
        data: ByteArray,
        pixelMax: Int = maxBitmapSize,
    ): Pair<Bitmap?, String?> {
        val orientation: Int? = ByteArrayInputStream(data).imageOrientation()

        options.inJustDecodeBounds = true
        options.inScaled = false
        options.outWidth = 0
        options.outHeight = 0
        BitmapFactory.decodeByteArray(data, 0, data.size, options)
        var w = options.outWidth
        var h = options.outHeight
        if (w <= 0 || h <= 0) {
            return Pair(null, "can't decode image bounds.")
        }
        
        var bits = 0
        while (w > pixelMax || h > pixelMax) {
            ++bits
            w = w shr 1
            h = h shr 1
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = 1 shl bits

        val bitmap1 = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            ?: return Pair(null, "BitmapFactory.decodeByteArray returns null.")

        val srcWidth = bitmap1.width.toFloat()
        val srcHeight = bitmap1.height.toFloat()
        if (srcWidth <= 0f || srcHeight <= 0f) {
            bitmap1.recycle()
            return Pair(null, "image size <= 0")
        }

        val dstSize = rotateSize(orientation, srcWidth, srcHeight)
        val dstSizeInt = Point(
            max(1, (dstSize.x + 0.5f).toInt()),
            max(1, (dstSize.y + 0.5f).toInt())
        )

        val matrix = Matrix()
        matrix.reset()
        matrix.postTranslate(srcWidth * -0.5f, srcHeight * -0.5f)
        matrix.resolveOrientation(orientation)
        matrix.postTranslate(dstSize.x * 0.5f, dstSize.y * 0.5f)

        val bitmap2 = try {
            Bitmap.createBitmap(dstSizeInt.x, dstSizeInt.y, Bitmap.Config.ARGB_8888)
        } catch (ex: Throwable) {
            return Pair(bitmap1, ex.withCaption("createBitmap failed."))
        }

        try {
            android.graphics.Canvas(bitmap2).drawBitmap(
                bitmap1,
                matrix,
                android.graphics.Paint().apply { isFilterBitmap = true }
            )
        } catch (ex: Throwable) {
            bitmap2.recycle()
            return Pair(bitmap1, ex.withCaption("drawBitmap failed."))
        }

        try {
            bitmap1.recycle()
        } catch (ignored: Throwable) {
        }
        return Pair(bitmap2, null)
    }

    override fun onCleared() {
        super.onCleared()
        _state.value.bitmap?.recycle()
    }
}
