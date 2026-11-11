package com.example.audio

import android.content.Context

/**
 * Caliper Hardware & Sensory Feedback Engine
 * Handles acoustic tones, synthetic pips, and micro-vernier haptic pulses.
 */
object CaliperHardwareEngine {

    enum class SoundType {
        TICK, SNAP, SUCCESS, THUD, PIP
    }

    val TICK = SoundType.TICK
    val SNAP = SoundType.SNAP
    val SUCCESS = SoundType.SUCCESS
    val THUD = SoundType.THUD
    val PIP = SoundType.PIP

    fun playSound(sound: SoundType) {
        when (sound) {
            SoundType.TICK -> CaliperSoundManager.playSnap()
            SoundType.SNAP -> CaliperSoundManager.playSnap()
            SoundType.SUCCESS -> CaliperSoundManager.playSuccess()
            SoundType.THUD -> CaliperSoundManager.playThud()
            SoundType.PIP -> CaliperSoundManager.playPip()
        }
    }

    fun pulseHaptic(context: Context, isHeavy: Boolean = false) {
        if (isHeavy) {
            CaliperHapticManager.bunkDoubleTap(context)
        } else {
            CaliperHapticManager.tick(context)
        }
    }
}
