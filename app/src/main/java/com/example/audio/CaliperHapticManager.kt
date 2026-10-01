package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Caliper Tactile Vernier Haptic Engine
 * Provides rich, engineered tactile patterns matching physical Vernier caliper resistance and clicks.
 */
object CaliperHapticManager {

    var isHapticsEnabled: Boolean = true

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Micro Vernier Tick
     * Ultra-short 12ms pulse for date picking, slot tapping, and slider detents.
     */
    fun tick(context: Context) {
        if (!isHapticsEnabled) return
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(12)
        }
    }

    /**
     * Double Tap Vibration
     * Distinct dual-pulse pattern for marking Bunk or warnings.
     */
    fun bunkDoubleTap(context: Context) {
        if (!isHapticsEnabled) return
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 20, 50, 25)
            val amplitudes = intArrayOf(0, 160, 0, 220)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 25, 50, 30), -1)
        }
    }

    /**
     * Reassuring Success Click
     * Clean confirmation pulse for marking "Present", completing tasks, or saving data.
     */
    fun successClick(context: Context) {
        if (!isHapticsEnabled) return
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(22)
        }
    }

    /**
     * Spring Vernier Click
     * Medium tactile spring feedback for pull-to-refresh and expanding trays.
     */
    fun springClick(context: Context) {
        if (!isHapticsEnabled) return
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(35)
        }
    }
}
