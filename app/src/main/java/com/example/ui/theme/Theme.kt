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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.audio.CaliperHapticManager

@Immutable
data class CaliperColors(
    val isMonochrome: Boolean = false,
    val baseSurface: Color = Color(0xFF000000),
    val containerSurface: Color = Color(0xFF0E121B).copy(alpha = 0.85f),
    val cardFill: Color = Color(0xFF0E121B).copy(alpha = 0.85f),
    val cardBorderTop: Color = Color.White.copy(alpha = 0.16f),
    val cardBorderBottom: Color = Color.White.copy(alpha = 0.04f),
    val textPrimary: Color = Color(0xFFF8FAFC),
    val textSecondary: Color = Color(0xFF94A3B8),
    val textMuted: Color = Color(0xFF64748B),
    val divider: Color = Color.White.copy(alpha = 0.08f),
    val safeZone: Color = Color(0xFF22C55E),
    val safeZoneContainer: Color = Color(0xFF22C55E).copy(alpha = 0.14f),
    val bunkDanger: Color = Color(0xFFEF4444),
    val deadlineWarning: Color = Color(0xFFF59E0B),
    val scheduledLecture: Color = Color(0xFF6366F1),
    val streakGold: Color = Color(0xFFFBBF24),
    val primaryAccent: Color = Color(0xFFE2E8F0)
) {
    val background: Color get() = baseSurface
    val cardSurface: Color get() = cardFill
    val elevatedSurface: Color get() = containerSurface
    val borderHighlight: Color get() = cardBorderTop
    val topSpecularGradient: List<Color> get() = listOf(cardBorderTop, Color.Transparent)
    val secondaryAccent: Color get() = scheduledLecture
    val safeEmerald: Color get() = safeZone
    val warningAmber: Color get() = deadlineWarning
    val dangerCrimson: Color get() = bunkDanger
}

val FlagshipObsidianColors = CaliperColors(
    isMonochrome = false,
    baseSurface = Color(0xFF000000),
    containerSurface = Color(0xFF141923).copy(alpha = 0.90f),
    cardFill = Color(0xFF0E121B).copy(alpha = 0.85f),
    cardBorderTop = Color.White.copy(alpha = 0.16f),
    cardBorderBottom = Color.White.copy(alpha = 0.04f),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    divider = Color.White.copy(alpha = 0.08f),
    safeZone = Color(0xFF22C55E),
    safeZoneContainer = Color(0xFF22C55E).copy(alpha = 0.14f),
    bunkDanger = Color(0xFFEF4444),
    deadlineWarning = Color(0xFFF59E0B),
    scheduledLecture = Color(0xFF6366F1),
    streakGold = Color(0xFFFBBF24),
    primaryAccent = Color(0xFFE2E8F0)
)

val LocalCaliperColors = staticCompositionLocalOf { FlagshipObsidianColors }

object CaliperTheme {
    val colors: CaliperColors
        @Composable
        get() = LocalCaliperColors.current
}

/**
 * ColorOS / OxygenOS Living Fluid Touch Physics Modifier:
 * - On PointerEventType.Press: Smoothly compresses scale down to 0.965f with immediate tactile tick.
 * - On PointerEventType.Release / Cancel: Snaps back to 1.0f using MediumBouncy / StiffnessLow spring.
 */
fun Modifier.fluidTouchFeedback(
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = if (isPressed) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "fluid_touch_scale"
    )

    val pointerModifier = Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when (event.type) {
                        PointerEventType.Press -> {
                            if (!isPressed) {
                                isPressed = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        PointerEventType.Release, PointerEventType.Exit -> {
                            if (isPressed) {
                                isPressed = false
                            }
                        }
                        else -> {
                            if (event.changes.none { it.pressed } && isPressed) {
                                isPressed = false
                            }
                        }
                    }
                }
            }
        }

    if (onClick != null) {
        this
            .then(pointerModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    } else {
        this.then(pointerModifier)
    }
}

fun Modifier.tactilePressEffect(
    context: Context? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tactile_scale"
    )

    val base = this
        .fluidTouchFeedback(enabled = true, onClick = null)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }

    if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                context?.let { CaliperHapticManager.tick(it) }
                onClick()
            }
        )
    } else {
        base
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val caliperColors = FlagshipObsidianColors
    val colorScheme = darkColorScheme(
        primary = caliperColors.primaryAccent,
        secondary = caliperColors.scheduledLecture,
        tertiary = caliperColors.safeZone,
        background = caliperColors.baseSurface,
        surface = caliperColors.cardFill,
        surfaceVariant = caliperColors.containerSurface,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = caliperColors.textPrimary,
        onSurface = caliperColors.textPrimary,
        onSurfaceVariant = caliperColors.textSecondary,
        error = caliperColors.bunkDanger
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    CompositionLocalProvider(LocalCaliperColors provides caliperColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
