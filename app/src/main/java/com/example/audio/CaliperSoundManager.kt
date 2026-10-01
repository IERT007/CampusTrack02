package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Caliper Procedural Audio Engine
 * Generates tactile, micro-acoustic feedback tones directly in memory using PCM 16-bit synthesis.
 * Zero external audio assets required; 100% offline, deterministic, and instant response.
 */
object CaliperSoundManager {

    private const val TAG = "CaliperSound"
    private const val SAMPLE_RATE = 44100

    var isAudioEnabled: Boolean = true

    private var trackSnap: AudioTrack? = null
    private var trackSuccess: AudioTrack? = null
    private var trackThud: AudioTrack? = null
    private var trackAlert: AudioTrack? = null

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            trackSnap = createTrack(generateSnapTone())
            trackSuccess = createTrack(generateSuccessChime())
            trackThud = createTrack(generateMutedThud())
            trackAlert = createTrack(generateAlertPip())
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Audio synthesis initialization error: ${e.message}")
        }
    }

    /**
     * Tone A: Snap/Tick
     * 1200 Hz micro click (18ms fast exponential decay).
     * Used for date picking, calendar navigation, and tab switching.
     */
    fun playSnap() {
        playTrack(trackSnap)
    }

    /**
     * Tone B: Success Chime
     * Harmonic rising chime (587 Hz D5 -> 880 Hz A5, 90ms).
     * Used for marking "Present", completing tasks, or adding items.
     */
    fun playSuccess() {
        playTrack(trackSuccess)
    }

    /**
     * Tone C: Muted Thud
     * Low frequency damped thud (130 Hz, 40ms).
     * Used for marking "Bunked" or deleting entries.
     */
    fun playThud() {
        playTrack(trackThud)
    }

    /**
     * Tone D: Alert Pip
     * High clarity double pip (1760 Hz, dual 25ms bursts).
     * Used for countdown urgency thresholds and cancellation alerts.
     */
    fun playAlert() {
        playTrack(trackAlert)
    }

    private fun playTrack(track: AudioTrack?) {
        if (!isAudioEnabled || track == null) return
        try {
            track.pause()
            track.flush()
            track.setNotificationMarkerPosition(0)
            track.reloadStaticData()
            track.play()
        } catch (e: Exception) {
            // Non-critical sonification error
        }
    }

    private fun createTrack(pcmData: ShortArray): AudioTrack {
        val bufferSizeInBytes = pcmData.size * 2
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .build()

        val track = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSizeInBytes,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(pcmData, 0, pcmData.size)
        return track
    }

    private fun generateSnapTone(): ShortArray {
        val durationMs = 18
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        val freq = 1200.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 220.0) // Rapid decay
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.4
            pcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    private fun generateSuccessChime(): ShortArray {
        val durationMs = 90
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        val split = numSamples / 2

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = if (i < split) 587.33 else 880.0 // D5 -> A5
            val envelope = if (i < split) {
                exp(-t * 30.0)
            } else {
                exp(-(t - (split.toDouble() / SAMPLE_RATE)) * 25.0)
            }
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.35
            pcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    private fun generateMutedThud(): ShortArray {
        val durationMs = 45
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        val freq = 130.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 90.0)
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.5
            pcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    private fun generateAlertPip(): ShortArray {
        val durationMs = 70
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val pcm = ShortArray(numSamples)
        val freq = 1760.0 // A6

        val pip1End = (SAMPLE_RATE * 0.025).toInt()
        val pip2Start = (SAMPLE_RATE * 0.045).toInt()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val active = (i < pip1End) || (i >= pip2Start)
            if (active) {
                val envelope = exp(-((i % pip1End).toDouble() / SAMPLE_RATE) * 50.0)
                val sample = sin(2.0 * PI * freq * t) * envelope * 0.35
                pcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            } else {
                pcm[i] = 0
            }
        }
        return pcm
    }
}
