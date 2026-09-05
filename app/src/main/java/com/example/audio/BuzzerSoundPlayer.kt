package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object BuzzerSoundPlayer {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {}
    }

    /**
     * Plays a pleasant 3-tone Islamic chime alert for Adhan arrival
     */
    fun playAdhanArrivalChime(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                // Harmonic chime notes (E4, G#4, B4, E5 arpeggio)
                val notes = listOf(330, 415, 493, 659, 830, 987)
                for (freq in notes) {
                    playSyntheticTone(freq, 220)
                    delay(120)
                }
            } catch (e: Exception) {
                // Fallback to ToneGenerator
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Short beep for last 5 seconds countdown before Iqomah / Adhan
     */
    fun playCountdownBeep(isFinal: Boolean = false) {
        try {
            if (isFinal) {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            }
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes smooth sine wave tone via AudioTrack
     */
    private fun playSyntheticTone(freqHz: Int, durationMs: Int) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)
            val doublePi = 2.0 * Math.PI

            for (i in 0 until numSamples) {
                // Envelope decay to make chime sound gentle and pleasing
                val envelope = 1.0 - (i.toDouble() / numSamples.toDouble())
                val sample = (sin(doublePi * i * freqHz / sampleRate) * 32767 * envelope * 0.7).toInt()
                buffer[i] = sample.toShort()
            }

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer.size * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
        } catch (_: Exception) {}
    }
}
