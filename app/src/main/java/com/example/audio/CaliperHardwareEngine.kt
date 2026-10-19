package com.example.audio

import android.app.Activity
import android.content.Context
import android.os.Build

object CaliperHardwareEngine {
    enum class SoundType {
        TICK, PIP, SNAP, SUCCESS, THUD
    }

    val TICK = SoundType.TICK
    val PIP = SoundType.PIP
    val SNAP = SoundType.SNAP
    val SUCCESS = SoundType.SUCCESS
    val THUD = SoundType.THUD

    fun enableHighRefreshRate(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = activity.display ?: return
                val highestMode = display.supportedModes.maxByOrNull { it.refreshRate } ?: return
                val params = activity.window.attributes
                params.preferredDisplayModeId = highestMode.modeId
                activity.window.attributes = params
            }
        } catch (_: Exception) {
        }
    }

    fun pulseHaptic(context: Context, heavy: Boolean = false) {
        if (heavy) {
            CaliperHapticManager.successClick(context)
        } else {
            CaliperHapticManager.tick(context)
        }
    }

    fun playSound(type: SoundType) {
        when (type) {
            SoundType.TICK, SoundType.PIP -> CaliperSoundManager.playPip()
            SoundType.SNAP -> CaliperSoundManager.playSnap()
            SoundType.SUCCESS -> CaliperSoundManager.playSuccess()
            SoundType.THUD -> CaliperSoundManager.playThud()
        }
    }
}
