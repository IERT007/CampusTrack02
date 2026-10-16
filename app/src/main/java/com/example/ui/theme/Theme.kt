package com.example.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.view.WindowCompat
import com.example.audio.CaliperHapticManager

/**
 * Caliper OS Flagship Color Palette & System
 * Features:
 * - Canvas: True Pitch Black (#000000)
 * - Card Surfaces: Deep Obsidian Glass (#0E121B @ 85% alpha)
 * - Luminous Top-Edge Border: Hairline 0.5.dp specular highlight (0.16f alpha to Transparent)
 * - Universal unified palette: High clarity, zero ambient blue-navy wash
 */
@Immutable
data class CaliperColors(
    val isMonochrome: Boolean,
    val baseSurface: Color,
    val containerSurface: Color,
    val cardFill: Color,
    val cardBorderTop: Color,
    val cardBorderBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    // Semantic Tokens
    val primaryAccent: Color,
    val safeZone: Color,
    val safeZoneContainer: Color,
    val scheduledLecture: Color,
    val deadlineWarning: Color,
    val bunkDanger: Color,
    val streakGold: Color,
    val divider: Color
)

val FlagshipObsidianColors = CaliperColors(
    isMonochrome = false,
    baseSurface = Color(0xFF000000), // True Pitch Black canvas background
    containerSurface = Color(0xFF07090E),
    cardFill = Color(0xFF0E121B).copy(alpha = 0.85f), // Deep Obsidian Glass
    cardBorderTop = Color.White.copy(alpha = 0.16f), // Luminous specular top border
    cardBorderBottom = Color.Transparent,
    textPrimary = Color(0xFFFFFFFF), // Pure Crisp White
    textSecondary = Color(0xFFCBD5E1), // Soft silver secondary labels
    textMuted = Color(0xFF94A3B8),
    primaryAccent = PsychologyBlue,
    safeZone = PsychologyGreen,
    safeZoneContainer = PsychologyGreen.copy(alpha = 0.15f),
    scheduledLecture = PsychologyBlue,
    deadlineWarning = PsychologyOrange,
    bunkDanger = PsychologyRed,
    streakGold = PsychologyGold,
    divider = Color.White.copy(alpha = 0.08f)
)

val LocalCaliperColors = staticCompositionLocalOf { FlagshipObsidianColors }

object CaliperTheme {
    val colors: CaliperColors
        @Composable
        get() = LocalCaliperColors.current
}

/**
 * Living Fluid Touch Feedback Modifier
 * ColorOS/OxygenOS fluid dynamics spring compression:
 * - On Press: Animates scale smoothly down to 0.965f with immediate tactile micro-tick
 * - On Release/Cancel: Snaps back to 1.0f using spring physics (dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
 */
fun Modifier.fluidTouchFeedback(
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fluid_touch_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {
                    CaliperHapticManager.tick(context)
                }

                // Wait for up or cancel
                val up = try {
                    waitForUpOrCancellation()
                } catch (_: Exception) {
                    null
                }
                isPressed = false
                if (up != null && onClick != null) {
                    onClick()
                }
            }
        }
}

/**
 * Tactile Pressed & Inset Mechanics
 * Scales button to 0.965f on press with haptic feedback
 */
fun Modifier.tactilePressEffect(
    context: Context? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tactile_inset_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = Color.White.copy(alpha = 0.18f))
        ) {
            if (context != null) {
                CaliperHapticManager.tick(context)
            }
            onClick()
        }
}

@Composable
fun MyApplicationTheme(
    isMonochrome: Boolean = false,
    content: @Composable () -> Unit
) {
    val caliperColors = FlagshipObsidianColors

    val materialColorScheme = darkColorScheme(
        primary = caliperColors.primaryAccent,
        background = caliperColors.baseSurface,
        surface = caliperColors.containerSurface,
        onPrimary = Color.Black,
        onBackground = caliperColors.textPrimary,
        onSurface = caliperColors.textPrimary,
        error = caliperColors.bunkDanger
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = caliperColors.baseSurface.toArgb()
                window.navigationBarColor = caliperColors.baseSurface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalCaliperColors provides caliperColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
