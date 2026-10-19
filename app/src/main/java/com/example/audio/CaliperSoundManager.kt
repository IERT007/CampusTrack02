package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object CaliperSoundManager {
    enum class ToneType {
        PIP, SNAP, SUCCESS, THUD, ALERT
    }

    var isAudioEnabled: Boolean = true

    fun init(context: Context) {
        // Initialized on demand via ToneGenerator
    }

    fun playTone(type: ToneType) {
        if (!isAudioEnabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35)
            val tone = when (type) {
                ToneType.PIP -> ToneGenerator.TONE_PROP_BEEP
                ToneType.SNAP -> ToneGenerator.TONE_PROP_ACK
                ToneType.SUCCESS -> ToneGenerator.TONE_PROP_PROMPT
                ToneType.THUD -> ToneGenerator.TONE_PROP_NACK
                ToneType.ALERT -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            }
            tg.startTone(tone, 35)
        } catch (_: Exception) {
        }
    }

    fun playConfirm(context: Context? = null) = playTone(ToneType.SNAP)
    fun playPip() = playTone(ToneType.PIP)
    fun playSnap() = playTone(ToneType.SNAP)
    fun playSuccess() = playTone(ToneType.SUCCESS)
    fun playThud() = playTone(ToneType.THUD)
    fun playAlert() = playTone(ToneType.ALERT)
}
