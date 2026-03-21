package jp.juggler.subwaytooter.actmediaviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.RepeatModeUtil
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootAttachment
import jp.juggler.util.network.MySslSocketFactory
import jp.juggler.subwaytooter.view.PinchBitmapView
import javax.net.ssl.HttpsURLConnection

@Composable
fun MediaViewerScreen(
    viewModel: MediaViewerViewModel,
    onDownload: (TootAttachment) -> Unit,
    onMore: (TootAttachment) -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // ExoPlayer lifecycle
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    // Update buffering state if needed
                }
            })
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // Update ExoPlayer
    LaunchedEffect(state.videoUrl) {
        if (state.videoUrl != null) {
            // Needed for some servers?
            HttpsURLConnection.setDefaultSSLSocketFactory(MySslSocketFactory)
            
            val item = MediaItem.fromUri(state.videoUrl!!)
            exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            // Repeat mode?
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE // Simplified
        } else {
            exoPlayer.stop()
        }
    }
    
    LaunchedEffect(state.isMuted) {
        exoPlayer.volume = if (state.isMuted) 0f else 1f
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Description
        if (state.showDescription && !state.description.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .background(Color(0x80000000))
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Text(
                    text = state.description!!,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.bitmap != null -> {
                    ImageContent(
                        bitmap = state.bitmap!!,
                        onSwipe = { dx, _ -> viewModel.loadDelta(dx) },
                        onMove = { w, h, scale ->
                             // Calculate zoom text and update VM
                             viewModel.setStatusText("Zoom: ${"%.2f".format(scale)}")
                        },
                        onClose = onClose
                    )
                }
                state.videoUrl != null -> {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                controllerAutoShow = false
                                setShowRewindButton(false)
                                setShowFastForwardButton(false)
                                setShowPreviousButton(false)
                                setShowNextButton(false)
                                setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.webUrl != null -> {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.setSupportZoom(true)
                            }
                        },
                        update = { view ->
                            view.loadUrl(state.webUrl!!)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.error != null -> {
                    Text(
                        text = state.error!!,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        
        // Footer controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x80000000))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.loadDelta(-1) }) {
                Icon(painterResource(R.drawable.ic_arrow_start), contentDescription = "Previous", tint = Color.White)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = { onDownload(state.mediaList[state.idx]) }) {
                Icon(painterResource(R.drawable.ic_download), contentDescription = "Download", tint = Color.White)
            }
            
            IconButton(onClick = { onMore(state.mediaList[state.idx]) }) {
                Icon(painterResource(R.drawable.ic_more), contentDescription = "More", tint = Color.White)
            }
            
            if (state.isVideo) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.isMuted,
                        onCheckedChange = { viewModel.toggleMute() }
                    )
                    Text("Mute", color = Color.White)
                 }
            }
            
            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { viewModel.loadDelta(1) }) {
                Icon(painterResource(R.drawable.ic_arrow_end), contentDescription = "Next", tint = Color.White)
            }
        }
        
        // Status Text
        if (state.statusText != null) {
            Text(
                text = state.statusText!!,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 8.dp, bottom = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ImageContent(
    bitmap: Bitmap,
    onSwipe: (Int, Int) -> Unit,
    onMove: (Float, Float, Float) -> Unit,
    onClose: () -> Unit,
) {
    AndroidView(
        factory = { ctx ->
            PinchBitmapView(ctx).apply {
                setCallback(object : PinchBitmapView.Callback {
                    override fun onSwipe(deltaX: Int, deltaY: Int) {
                        if (deltaX != 0) {
                            onSwipe(deltaX, deltaY)
                        } else {
                            // Vertical swipe to close
                            onClose()
                        }
                    }
                    override fun onMove(bitmapW: Float, bitmapH: Float, tx: Float, ty: Float, scale: Float) {
                        onMove(bitmapW, bitmapH, scale)
                    }
                })
            }
        },
        update = { view ->
            view.setBitmap(bitmap)
        },
        modifier = Modifier.fillMaxSize()
    )
}
