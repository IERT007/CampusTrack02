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
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.audio.CaliperHapticManager

@Immutable
data class CaliperColors(
    val isMonochrome: Boolean,
    val baseSurface: Color,
    val containerSurface: Color,
    val cardFill: Color,
    val pressedFill: Color,
    val shadowDark: Color,
    val highlightLight: Color,
    val cardBorderTop: Color,
    val cardBorderBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val safeZone: Color,
    val safeZoneContainer: Color,
    val scheduledLecture: Color,
    val deadlineWarning: Color,
    val bunkDanger: Color,
    val streakGold: Color,
    val divider: Color
)

val CalmKitIvoryColors = CaliperColors(
    isMonochrome = false,
    baseSurface = CalmIvoryCanvas,
    containerSurface = CalmIvorySurface,
    cardFill = CalmCardRaised,
    pressedFill = CalmCardPressed,
    shadowDark = CalmShadowDark,
    highlightLight = CalmHighlightLight,
    cardBorderTop = Color.White.copy(alpha = 0.95f),
    cardBorderBottom = CalmShadowDark.copy(alpha = 0.70f),
    textPrimary = CalmInkPrimary,
    textSecondary = CalmInkSecondary,
    textMuted = CalmInkMuted,
    primaryAccent = CalmSagePrimary,
    onPrimaryAccent = Color.White,
    safeZone = PsychologyGreen,
    safeZoneContainer = CalmMist,
    scheduledLecture = PsychologyBlue,
    deadlineWarning = PsychologyOrange,
    bunkDanger = PsychologyRed,
    streakGold = PsychologyGold,
    divider = CalmShadowDark.copy(alpha = 0.65f)
)

val FlagshipObsidianColors = CaliperColors(
    isMonochrome = true,
    baseSurface = Color(0xFF0E1214),
    containerSurface = Color(0xFF1C2327),
    cardFill = Color(0xFF161C1F),
    pressedFill = Color(0xFF101417),
    shadowDark = Color(0xFF050708),
    highlightLight = Color(0xFF2E383D),
    cardBorderTop = Color(0xFF2E383D),
    cardBorderBottom = Color(0xFF080A0C),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFE2E8F0),
    textMuted = Color(0xFF94A3B8),
    primaryAccent = Color(0xFF2DD4BF),
    onPrimaryAccent = Color(0xFF041A18),
    safeZone = Color(0xFF34D399),
    safeZoneContainer = Color(0xFF1E3A34),
    scheduledLecture = Color(0xFF60A5FA),
    deadlineWarning = Color(0xFFFB923C),
    bunkDanger = Color(0xFFF87171),
    streakGold = Color(0xFFFBBF24),
    divider = Color.White.copy(alpha = 0.18f)
)

val LocalCaliperColors = staticCompositionLocalOf { CalmKitIvoryColors }

object CaliperTheme {
    val colors: CaliperColors
        @Composable
        get() = LocalCaliperColors.current
}

fun Modifier.fluidTouchFeedback(
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.962f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
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
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {
                    CaliperHapticManager.tick(context)
                }
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

fun Modifier.tactilePressEffect(
    context: Context? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.962f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
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
            indication = ripple(color = CalmSagePrimary.copy(alpha = 0.18f))
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
    val caliperColors = if (isMonochrome) FlagshipObsidianColors else CalmKitIvoryColors

    val materialColorScheme = if (isMonochrome) {
        darkColorScheme(
            primary = caliperColors.primaryAccent,
            background = caliperColors.baseSurface,
            surface = caliperColors.cardFill,
            surfaceVariant = caliperColors.containerSurface,
            onPrimary = caliperColors.onPrimaryAccent,
            onBackground = caliperColors.textPrimary,
            onSurface = caliperColors.textPrimary,
            onSurfaceVariant = caliperColors.textSecondary,
            error = caliperColors.bunkDanger
        )
    } else {
        lightColorScheme(
            primary = caliperColors.primaryAccent,
            background = caliperColors.baseSurface,
            surface = caliperColors.cardFill,
            surfaceVariant = caliperColors.containerSurface,
            onPrimary = caliperColors.onPrimaryAccent,
            onBackground = caliperColors.textPrimary,
            onSurface = caliperColors.textPrimary,
            onSurfaceVariant = caliperColors.textSecondary,
            error = caliperColors.bunkDanger
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = caliperColors.baseSurface.toArgb()
                window.navigationBarColor = caliperColors.baseSurface.toArgb()
                val useDarkIcons = !isMonochrome
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useDarkIcons
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
