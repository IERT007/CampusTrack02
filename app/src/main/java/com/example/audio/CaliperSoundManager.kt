package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Caliper Procedural Audio Engine
 * Generates tactile, micro-acoustic feedback tones directly in memory using PCM 16-bit synthesis.
 * Zero external audio assets required; 100% offline, deterministic, and instant response.
 */
object CaliperSoundManager {
    var isAudioEnabled: Boolean = true

    fun playTone(tone: ToneType) {
        if (!isAudioEnabled) return

        CoroutineScope(Dispatchers.Default).launch {
            val sampleRate = 44100
            val (frequency, durationMs) = when (tone) {
                ToneType.SNAP_TICK -> Pair(1800.0, 10)       // Crisp navigation tick
                ToneType.SUCCESS_CHIME -> Pair(880.0, 45)    // A5 confirmation harmonic
                ToneType.MUTED_THUD -> Pair(220.0, 30)       // Low frequency bunk register
                ToneType.ALERT_PIP -> Pair(1200.0, 25)       // High priority warning ping
            }

            val numSamples = (durationMs * sampleRate / 1000)
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val decayEnvelope = 1.0 - (i.toDouble() / numSamples)
                val sample = (Math.sin(2.0 * Math.PI * i * frequency / sampleRate) * Short.MAX_VALUE * decayEnvelope).toInt()
                buffer[i] = sample.toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            delay(durationMs.toLong() + 30)
            track.release()
        }
    }

    enum class ToneType {
        SNAP_TICK, SUCCESS_CHIME, MUTED_THUD, ALERT_PIP
    }

    // Convenience calls
    fun playSnap() = playTone(ToneType.SNAP_TICK)
    fun playSuccess() = playTone(ToneType.SUCCESS_CHIME)
    fun playThud() = playTone(ToneType.MUTED_THUD)
    fun playAlert() = playTone(ToneType.ALERT_PIP)
    fun playPip() = playTone(ToneType.ALERT_PIP)
    fun init(context: Context) {}
}
