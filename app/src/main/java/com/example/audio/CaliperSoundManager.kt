package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.SoundEffectConstants
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Caliper v2.0 Soft Acoustic Feedback Engine
 * Replaces harsh high-frequency sine beeps with ultra-soft, low-amplitude,
 * exponentially damped wooden/glass micro-clicks and native Android UI clicks.
 */
object CaliperSoundManager {
    @Volatile
    var isAudioEnabled: Boolean = true

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays Android's native system UI click sound if audio is enabled.
     */
    fun playSystemClick(view: View?) {
        if (!isAudioEnabled) return
        view?.playSoundEffect(SoundEffectConstants.CLICK)
    }

    fun playTone(tone: ToneType) {
        if (!isAudioEnabled) return

        audioScope.launch {
            try {
                val sampleRate = 44100
                // Warm, ear-soothing low-amplitude filtered impulses (no harsh piercing frequencies)
                val (baseFreqHz, harmonicHz, durationMs, maxGain, decayRate) = when (tone) {
                    ToneType.SNAP_TICK -> AcousticSpec(380.0, 760.0, 9, 0.09, 9.0)       // Soft wooden micro-tick
                    ToneType.SUCCESS_CHIME -> AcousticSpec(440.0, 660.0, 18, 0.11, 5.5)  // Warm glass fifth tap
                    ToneType.MUTED_THUD -> AcousticSpec(210.0, 315.0, 14, 0.10, 7.5)     // Damped low thud
                    ToneType.ALERT_PIP -> AcousticSpec(510.0, 765.0, 12, 0.09, 7.0)      // Gentle matte glass pip
                }

                val numSamples = (durationMs * sampleRate / 1000).coerceAtLeast(64)
                val buffer = ShortArray(numSamples)
                val attackSamples = (numSamples * 0.12).coerceAtLeast(8.0)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples

                    // Smooth cosine-ramp attack to eliminate digital pops, paired with exponential decay
                    val attack = if (i < attackSamples) {
                        0.5 * (1.0 - kotlin.math.cos(PI * i / attackSamples))
                    } else {
                        1.0
                    }
                    val decay = exp(-decayRate * progress)
                    val envelope = attack * decay

                    // Subtle pitch drop mimics organic physical wood/glass tap resonance
                    val freqDrop = 1.0 - (0.08 * progress)
                    val fundamental = sin(2.0 * PI * (baseFreqHz * freqDrop) * t)
                    val softOvertone = 0.28 * sin(2.0 * PI * (harmonicHz * freqDrop) * t) * exp(-12.0 * progress)

                    val amplitude = (fundamental + softOvertone) * envelope * maxGain * Short.MAX_VALUE
                    buffer[i] = amplitude.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
                delay(durationMs.toLong() + 25L)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Gracefully ignore if audio hardware is busy or unavailable
            }
        }
    }

    private data class AcousticSpec(
        val baseFreqHz: Double,
        val harmonicHz: Double,
        val durationMs: Int,
        val maxGain: Double,
        val decayRate: Double
    )

    enum class ToneType {
        SNAP_TICK, SUCCESS_CHIME, MUTED_THUD, ALERT_PIP
    }

    // Convenience calls (all preserve backwards compatibility)
    fun playSnap() = playTone(ToneType.SNAP_TICK)
    fun playSnap(view: View?) {
        if (!isAudioEnabled) return
        if (view != null) playSystemClick(view) else playTone(ToneType.SNAP_TICK)
    }
    fun playSuccess() = playTone(ToneType.SUCCESS_CHIME)
    fun playThud() = playTone(ToneType.MUTED_THUD)
    fun playAlert() = playTone(ToneType.ALERT_PIP)
    fun playPip() = playTone(ToneType.ALERT_PIP)
    fun init(context: Context) {}
}
