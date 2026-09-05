package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.util.Log
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@OptIn(UnstableApi::class)
@Composable
fun RtspCctvPlayerView(
    rtspUrl: String,
    modifier: Modifier = Modifier,
    isMuted: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }

    // RenderersFactory with automatic software/hardware decoder fallback
    val renderersFactory = remember(context) {
        DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    }

    // TrackSelector configured to exceed renderer capabilities and disable problematic audio streams when muted
    val trackSelector = remember(context) {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .build()
        }
    }

    // Disable audio track decoding when isMuted is true to prevent unsupported audio codec errors (like G.711/PCMA)
    LaunchedEffect(isMuted, trackSelector) {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, isMuted)
            .build()
    }

    val exoPlayer = remember(context, rtspUrl, retryCount) {
        val trimmedUrl = rtspUrl.trim()
        if (trimmedUrl.isBlank()) null
        else {
            try {
                ExoPlayer.Builder(context, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .build().apply {
                        val uri = Uri.parse(trimmedUrl)
                        val useTcp = (retryCount % 2 == 0) // Try TCP first, alternate with UDP on retry
                        val mediaSource = if (trimmedUrl.startsWith("rtsp://", ignoreCase = true)) {
                            RtspMediaSource.Factory()
                                .setForceUseRtpTcp(useTcp)
                                .setTimeoutMs(10000)
                                .setDebugLoggingEnabled(false)
                                .createMediaSource(MediaItem.fromUri(uri))
                        } else {
                            val mediaItem = MediaItem.fromUri(uri)
                            RtspMediaSource.Factory()
                                .setTimeoutMs(10000)
                                .createMediaSource(mediaItem)
                        }

                        setMediaSource(mediaSource)
                        repeatMode = Player.REPEAT_MODE_OFF
                        playWhenReady = true
                        volume = if (isMuted) 0f else 1f
                        prepare()
                    }
            } catch (e: Exception) {
                Log.e("RtspCctvPlayerView", "Gagal inisialisasi pemutar RTSP", e)
                errorMessage = "Gagal menginisialisasi pemutar: ${e.localizedMessage ?: e.message}"
                null
            }
        }
    }

    // Handle Mute/Unmute state dynamically
    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    // Set up Player event listener
    DisposableEffect(exoPlayer) {
        val player = exoPlayer
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        errorMessage = null
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                Log.e("RtspCctvPlayerView", "PlaybackException code=${error.errorCode} (${error.errorCodeName}): ${error.message}", error)

                val cause = error.cause
                val rawMsg = error.message ?: ""
                val causeMsg = cause?.message ?: ""
                val combined = "$rawMsg $causeMsg".lowercase()

                val desc = when {
                    // 1. Authentication failure (401 Unauthorized)
                    combined.contains("401") || combined.contains("unauthorized") ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Kamera butuh Username & Password (401 Unauthorized).\nPastikan URL berisi username:password (misal rtsp://user:pass@IP:554/...)"

                    // 2. Stream path not found (404 Not Found)
                    combined.contains("404") || combined.contains("not found") ->
                        "Aliran stream tidak ditemukan (404 Not Found).\nPeriksa path kamera (misal /stream1, /stream2, atau /V_ENC_001)."

                    // 3. Network unreachable / Timeout / Refused
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                    combined.contains("timeout") || combined.contains("refused") || combined.contains("unreachable") || combined.contains("no route") ->
                        "Tidak dapat menghubungi IP CCTV (Timeout/Refused).\nPastikan IP kamera & TV berada di Wi-Fi/LAN yang sama."

                    // 4. Decoder failure specifically
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ->
                        "Decoder hardware TV mengalami kendala memproses frame video.\nMencoba software fallback..."

                    else -> {
                        val detail = causeMsg.ifBlank { rawMsg.ifBlank { error.errorCodeName } }
                        "Kendala stream CCTV ($detail)"
                    }
                }
                errorMessage = desc
                onError?.invoke(desc)

                // Auto-retry connection after 4 seconds
                coroutineScope.launch {
                    delay(4000)
                    retryCount++
                }
            }
        }

        player?.addListener(listener)

        onDispose {
            player?.removeListener(listener)
            player?.stop()
            player?.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        this.useController = false
                        this.resizeMode = resizeMode
                        this.layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    view.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading / Buffering Overlay
        AnimatedVisibility(
            visible = isBuffering && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF10B981),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "Menghubungkan ke Kamera CCTV...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error message overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD9881337))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .align(Alignment.Center)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📹 Kamera CCTV Tidak Terhubung",
                        color = Color(0xFFFDE68A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "Mencoba menyambung kembali otomatis...",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MultiCctvPlayerGrid(
    cameras: List<com.example.data.model.CctvCameraItem>,
    activeCameraId: String,
    displayLayout: String = "SINGLE", // SINGLE, SPLIT_2, GRID_4
    modifier: Modifier = Modifier,
    isMuted: Boolean = true
) {
    if (cameras.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada kamera CCTV yang dikonfigurasi.",
                color = Color.White,
                fontSize = 16.sp
            )
        }
        return
    }

    when (displayLayout) {
        "SPLIT_2" -> {
            val displayCams = cameras.filter { it.isEnabled }.take(2)
            if (displayCams.size <= 1) {
                val singleCam = displayCams.firstOrNull() ?: cameras.first()
                SingleCameraViewWithLabel(singleCam, modifier, isMuted)
            } else {
                Row(
                    modifier = modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displayCams.forEach { cam ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            SingleCameraViewWithLabel(cam, Modifier.fillMaxSize(), isMuted)
                        }
                    }
                }
            }
        }
        "GRID_4" -> {
            val displayCams = cameras.filter { it.isEnabled }.take(4)
            if (displayCams.size <= 1) {
                val singleCam = displayCams.firstOrNull() ?: cameras.first()
                SingleCameraViewWithLabel(singleCam, modifier, isMuted)
            } else if (displayCams.size == 2) {
                Row(
                    modifier = modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displayCams.forEach { cam ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            SingleCameraViewWithLabel(cam, Modifier.fillMaxSize(), isMuted)
                        }
                    }
                }
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        displayCams.take(2).forEach { cam ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                SingleCameraViewWithLabel(cam, Modifier.fillMaxSize(), isMuted)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        displayCams.drop(2).take(2).forEach { cam ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                SingleCameraViewWithLabel(cam, Modifier.fillMaxSize(), isMuted)
                            }
                        }
                    }
                }
            }
        }
        else -> { // "SINGLE"
            val activeCam = cameras.firstOrNull { it.id == activeCameraId } ?: cameras.firstOrNull { it.isEnabled } ?: cameras.first()
            SingleCameraViewWithLabel(activeCam, modifier, isMuted)
        }
    }
}

@Composable
private fun SingleCameraViewWithLabel(
    camera: com.example.data.model.CctvCameraItem,
    modifier: Modifier = Modifier,
    isMuted: Boolean = true
) {
    Box(modifier = modifier) {
        RtspCctvPlayerView(
            rtspUrl = camera.streamUrl,
            modifier = Modifier.fillMaxSize(),
            isMuted = isMuted || camera.isMuted
        )
        // Camera Label badge at top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFEF4444))
                )
                Text(
                    text = camera.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
