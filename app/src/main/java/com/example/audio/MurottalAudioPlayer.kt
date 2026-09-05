package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.model.MurottalPreset
import com.example.data.model.MurottalPresetCatalogue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

enum class MurottalState {
    IDLE,
    PREPARING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

data class MurottalPlaybackInfo(
    val state: MurottalState = MurottalState.IDLE,
    val title: String = "",
    val qari: String = "",
    val surah: String = "",
    val isPlaying: Boolean = false,
    val volumePercent: Int = 80,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val errorMessage: String = ""
)

object MurottalAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _playbackInfo = MutableStateFlow(MurottalPlaybackInfo())
    val playbackInfo: StateFlow<MurottalPlaybackInfo> = _playbackInfo.asStateFlow()

    private var currentVolume: Float = 0.8f

    fun playPreset(preset: MurottalPreset, volumePercent: Int = 80) {
        playUrl(
            url = preset.audioUrl,
            title = preset.surahName,
            qari = preset.qariName,
            surah = preset.surahName,
            volumePercent = volumePercent
        )
    }

    fun playPresetById(presetId: String, volumePercent: Int = 80) {
        val preset = MurottalPresetCatalogue.getPresetById(presetId)
        playPreset(preset, volumePercent)
    }

    fun playFile(
        context: Context,
        filePath: String,
        title: String,
        qari: String,
        surah: String,
        volumePercent: Int = 80
    ) {
        scope.launch(Dispatchers.Main) {
            stopInternal(notify = false)
            _playbackInfo.update {
                it.copy(
                    state = MurottalState.PREPARING,
                    title = title.ifBlank { "Murottal Al-Qur'an" },
                    qari = qari.ifBlank { "Qari Pilihan" },
                    surah = surah.ifBlank { title },
                    isPlaying = false,
                    volumePercent = volumePercent,
                    errorMessage = ""
                )
            }

            try {
                val file = File(filePath)
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    if (file.exists()) {
                        java.io.FileInputStream(file).use { fis ->
                            setDataSource(fis.fd)
                        }
                    } else {
                        setDataSource(context, Uri.parse(filePath))
                    }
                    setVolumeForPlayer(volumePercent)
                    setOnPreparedListener { player ->
                        try {
                            player.start()
                            _playbackInfo.update {
                                it.copy(
                                    state = MurottalState.PLAYING,
                                    isPlaying = true,
                                    durationMs = try { player.duration } catch (_: Exception) { 0 }
                                )
                            }
                            startProgressTracker()
                        } catch (ex: Exception) {
                            _playbackInfo.update {
                                it.copy(
                                    state = MurottalState.ERROR,
                                    isPlaying = false,
                                    errorMessage = ex.localizedMessage ?: "Error memulai pemutar"
                                )
                            }
                        }
                    }
                    setOnCompletionListener {
                        stopInternal(notify = true)
                    }
                    setOnErrorListener { _, what, extra ->
                        _playbackInfo.update {
                            it.copy(
                                state = MurottalState.ERROR,
                                isPlaying = false,
                                errorMessage = "Gagal memutar audio (Code $what, $extra)"
                            )
                        }
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                _playbackInfo.update {
                    it.copy(
                        state = MurottalState.ERROR,
                        isPlaying = false,
                        errorMessage = e.localizedMessage ?: "Gagal memuat file audio"
                    )
                }
            }
        }
    }

    fun playUrl(
        url: String,
        title: String,
        qari: String,
        surah: String,
        volumePercent: Int = 80
    ) {
        scope.launch(Dispatchers.Main) {
            stopInternal(notify = false)
            _playbackInfo.update {
                it.copy(
                    state = MurottalState.PREPARING,
                    title = title.ifBlank { "Murottal Al-Qur'an" },
                    qari = qari.ifBlank { "Qari Pilihan" },
                    surah = surah.ifBlank { title },
                    isPlaying = false,
                    volumePercent = volumePercent,
                    errorMessage = ""
                )
            }

            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(url)
                    setVolumeForPlayer(volumePercent)
                    setOnPreparedListener { player ->
                        try {
                            player.start()
                            _playbackInfo.update {
                                it.copy(
                                    state = MurottalState.PLAYING,
                                    isPlaying = true,
                                    durationMs = try { player.duration } catch (_: Exception) { 0 }
                                )
                            }
                            startProgressTracker()
                        } catch (ex: Exception) {
                            _playbackInfo.update {
                                it.copy(
                                    state = MurottalState.ERROR,
                                    isPlaying = false,
                                    errorMessage = ex.localizedMessage ?: "Error memulai pemutar"
                                )
                            }
                        }
                    }
                    setOnCompletionListener {
                        stopInternal(notify = true)
                    }
                    setOnErrorListener { _, what, extra ->
                        _playbackInfo.update {
                            it.copy(
                                state = MurottalState.ERROR,
                                isPlaying = false,
                                errorMessage = "Koneksi audio terputus (Code $what, $extra)"
                            )
                        }
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                _playbackInfo.update {
                    it.copy(
                        state = MurottalState.ERROR,
                        isPlaying = false,
                        errorMessage = e.localizedMessage ?: "Gagal memuat URL audio"
                    )
                }
            }
        }
    }

    fun stop() {
        scope.launch(Dispatchers.Main) {
            stopInternal(notify = true)
        }
    }

    fun stopWithFadeOut(durationMs: Long = 1500L, onFinished: (() -> Unit)? = null) {
        scope.launch(Dispatchers.Default) {
            try {
                val steps = 15
                val stepDelay = durationMs / steps
                val initialVol = currentVolume
                for (i in steps downTo 0) {
                    val vol = (initialVol * (i.toFloat() / steps.toFloat())).coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        try {
                            mediaPlayer?.setVolume(vol, vol)
                        } catch (_: Exception) {}
                    }
                    delay(stepDelay)
                }
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                stopInternal(notify = true)
                onFinished?.invoke()
            }
        }
    }

    fun setVolume(volumePercent: Int) {
        val pct = volumePercent.coerceIn(0, 100)
        currentVolume = pct / 100f
        _playbackInfo.update { it.copy(volumePercent = pct) }
        try {
            mediaPlayer?.setVolume(currentVolume, currentVolume)
        } catch (_: Exception) {}
    }

    private fun setVolumeForPlayer(volumePercent: Int) {
        val pct = volumePercent.coerceIn(0, 100)
        currentVolume = pct / 100f
        try {
            mediaPlayer?.setVolume(currentVolume, currentVolume)
        } catch (_: Exception) {}
    }

    private fun stopInternal(notify: Boolean) {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null

        if (notify) {
            _playbackInfo.update {
                it.copy(
                    state = MurottalState.STOPPED,
                    isPlaying = false,
                    currentPositionMs = 0
                )
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    _playbackInfo.update {
                        it.copy(
                            currentPositionMs = try { mp.currentPosition } catch (_: Exception) { 0 },
                            durationMs = try { mp.duration } catch (_: Exception) { 0 }
                        )
                    }
                }
                delay(1000)
            }
        }
    }
}
