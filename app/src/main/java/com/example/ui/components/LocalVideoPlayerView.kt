package com.example.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@Composable
fun LocalVideoPlayerView(
    videoPath: String,
    modifier: Modifier = Modifier,
    isMuted: Boolean = true,
    isLooping: Boolean = true,
    volume: Float = 1.0f,
    onCompletion: () -> Unit = {},
    onError: () -> Unit = {}
) {
    val context = LocalContext.current
    val videoFile = remember(videoPath) {
        if (videoPath.isNotBlank()) File(videoPath) else null
    }

    if (videoFile == null || !videoFile.exists() || !videoFile.isFile) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }

    val shouldLoop = isLooping
    val muteSetting = isMuted
    val targetVolume = volume

    DisposableEffect(videoPath, isMuted, isLooping, volume) {
        onDispose {
            try {
                mediaPlayerRef?.let { mp ->
                    if (mp.isPlaying) mp.stop()
                    mp.reset()
                    mp.release()
                }
            } catch (_: Exception) {}
            mediaPlayerRef = null
            surfaceRef?.release()
            surfaceRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                            val surface = Surface(st)
                            surfaceRef = surface
                            try {
                                val mp = MediaPlayer().apply {
                                    setDataSource(ctx, Uri.fromFile(videoFile))
                                    setSurface(surface)
                                    this.isLooping = shouldLoop
                                    val actualVol = if (muteSetting) 0f else targetVolume.coerceIn(0f, 1f)
                                    setVolume(actualVol, actualVol)
                                    setOnPreparedListener { player ->
                                        val videoWidth = player.videoWidth.toFloat()
                                        val videoHeight = player.videoHeight.toFloat()
                                        if (videoWidth > 0 && videoHeight > 0 && width > 0 && height > 0) {
                                            val scaleX = width / videoWidth
                                            val scaleY = height / videoHeight
                                            val maxScale = Math.max(scaleX, scaleY)
                                            val scaledWidth = videoWidth * maxScale
                                            val scaledHeight = videoHeight * maxScale
                                            val dx = (width - scaledWidth) / 2f
                                            val dy = (height - scaledHeight) / 2f

                                            val matrix = android.graphics.Matrix()
                                            matrix.setScale(scaledWidth / width, scaledHeight / height, width / 2f, height / 2f)
                                            setTransform(matrix)
                                        }
                                        player.start()
                                    }
                                    setOnCompletionListener {
                                        if (!shouldLoop) {
                                            onCompletion()
                                        }
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        onError()
                                        true
                                    }
                                    prepareAsync()
                                }
                                mediaPlayerRef = mp
                            } catch (e: Exception) {
                                onError()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            try {
                                mediaPlayerRef?.let { mp ->
                                    if (mp.isPlaying) mp.stop()
                                    mp.reset()
                                    mp.release()
                                }
                            } catch (_: Exception) {}
                            mediaPlayerRef = null
                            surfaceRef?.release()
                            surfaceRef = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },
            update = { _ ->
                mediaPlayerRef?.let { mp ->
                    try {
                        val actualVol = if (muteSetting) 0f else targetVolume.coerceIn(0f, 1f)
                        mp.setVolume(actualVol, actualVol)
                    } catch (_: Exception) {}
                }
            }
        )
    }
}
