package com.example.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.audio.CaliperHapticManager

/**
 * Caliper Dual-Theme Color System
 * Provides semantic color mapping for:
 * 1. Monochrome Dark Mode (True Black #000000, crisp white typography, soft silver labels & neutral glass borders)
 * 2. Normal Mode (True Black #000000 canvas, translucent obsidian #0E121B cards, semantic green & blue badges)
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

val MonochromeDarkColors = CaliperColors(
    isMonochrome = true,
    baseSurface = Color(0xFF000000), // True Black
    containerSurface = Color(0xFF060709),
    cardFill = Color(0xFF0E121B).copy(alpha = 0.8f),
    cardBorderTop = Color.White.copy(alpha = 0.10f), // Neutral glass borders
    cardBorderBottom = Color.White.copy(alpha = 0.10f),
    textPrimary = Color(0xFFFFFFFF), // Pure Crisp White
    textSecondary = Color(0xFFCBD5E1), // Soft silver
    textMuted = Color(0xFF94A3B8),
    primaryAccent = Color(0xFFFFFFFF),
    safeZone = Color(0xFFFFFFFF),
    safeZoneContainer = Color.White.copy(alpha = 0.08f),
    scheduledLecture = Color(0xFFE2E8F0),
    deadlineWarning = Color(0xFFCBD5E1),
    bunkDanger = Color(0xFF94A3B8),
    streakGold = Color(0xFFFFFFFF),
    divider = Color.White.copy(alpha = 0.08f)
)

val NormalPsychologyColors = CaliperColors(
    isMonochrome = false,
    baseSurface = Color(0xFF000000), // True Pitch Black canvas background
    containerSurface = Color(0xFF07090E),
    cardFill = Color(0xFF0E121B).copy(alpha = 0.8f), // Subtle translucent obsidian
    cardBorderTop = Color.White.copy(alpha = 0.15f),
    cardBorderBottom = Color.White.copy(alpha = 0.04f),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    primaryAccent = PsychologyBlue,
    safeZone = PsychologyGreen,
    safeZoneContainer = PsychologyGreen.copy(alpha = 0.15f),
    scheduledLecture = PsychologyBlue,
    deadlineWarning = PsychologyOrange,
    bunkDanger = PsychologyRed,
    streakGold = PsychologyGold,
    divider = Color.White.copy(alpha = 0.06f)
)

val LocalCaliperColors = staticCompositionLocalOf { NormalPsychologyColors }

object CaliperTheme {
    val colors: CaliperColors
        @Composable
        get() = LocalCaliperColors.current
}

/**
 * Tactile Pressed & Inset Mechanics
 * Scales button subtly to 0.97f on press with haptic feedback
 */
fun Modifier.tactilePressEffect(
    context: Context? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
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
    val caliperColors = if (isMonochrome) MonochromeDarkColors else NormalPsychologyColors

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
