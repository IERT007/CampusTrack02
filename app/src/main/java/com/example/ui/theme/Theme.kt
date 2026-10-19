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
import androidx.compose.material3.lightColorScheme
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
    val isLightGlass: Boolean = true,
    val baseSurface: Color = Color(0xFFE8E2D8),
    val baseSurfaceSecondary: Color = Color(0xFFDCD5C9),
    val containerSurface: Color = Color(0xFFF3EFE8).copy(alpha = 0.88f),
    val cardFill: Color = Color(0xFFF8F5EF).copy(alpha = 0.84f),
    val cardFillSecondary: Color = Color(0xFFEDE7DD).copy(alpha = 0.82f),
    val cardBorderTop: Color = Color(0xFFFFFFFF).copy(alpha = 0.78f),
    val cardBorderBottom: Color = Color(0xFFC9C0B3).copy(alpha = 0.32f),
    val textPrimary: Color = Color(0xFF23201C),
    val textSecondary: Color = Color(0xFF6B6359),
    val textMuted: Color = Color(0xFF968D82),
    val divider: Color = Color(0xFF23201C).copy(alpha = 0.08f),
    val safeZone: Color = Color(0xFF4F6F58),
    val safeZoneContainer: Color = Color(0xFF4F6F58).copy(alpha = 0.14f),
    val bunkDanger: Color = Color(0xFFC2544B),
    val deadlineWarning: Color = Color(0xFFC68642),
    val scheduledLecture: Color = Color(0xFF8C7D6A),
    val streakGold: Color = Color(0xFFB5853B),
    val primaryAccent: Color = Color(0xFF5D5346)
) {
    val background: Color get() = baseSurface
    val cardSurface: Color get() = cardFill
    val elevatedSurface: Color get() = containerSurface
    val borderHighlight: Color get() = cardBorderTop
    val topSpecularGradient: List<Color> get() = listOf(cardBorderTop, cardBorderBottom)
    val secondaryAccent: Color get() = scheduledLecture
    val safeEmerald: Color get() = safeZone
    val warningAmber: Color get() = deadlineWarning
    val dangerCrimson: Color get() = bunkDanger
}

/**
 * Warm Organic Alabaster, Stone & Botanical Sage Frosted Glass Palette
 * (Matches the reference design aesthetic)
 */
val WarmAlabasterGlassColors = CaliperColors(
    isMonochrome = false,
    isLightGlass = true,
    baseSurface = Color(0xFFE8E2D8),
    baseSurfaceSecondary = Color(0xFFDCD4C6),
    containerSurface = Color(0xFFF4F0E8).copy(alpha = 0.90f),
    cardFill = Color(0xFFF9F6F0).copy(alpha = 0.82f),
    cardFillSecondary = Color(0xFFEBE5DA).copy(alpha = 0.80f),
    cardBorderTop = Color(0xFFFFFFFF).copy(alpha = 0.85f),
    cardBorderBottom = Color(0xFFC7BEB0).copy(alpha = 0.35f),
    textPrimary = Color(0xFF221F1B),
    textSecondary = Color(0xFF6B6359),
    textMuted = Color(0xFF968E83),
    divider = Color(0xFF221F1B).copy(alpha = 0.08f),
    safeZone = Color(0xFF4E6E58),
    safeZoneContainer = Color(0xFF4E6E58).copy(alpha = 0.14f),
    bunkDanger = Color(0xFFC2544B),
    deadlineWarning = Color(0xFFC68642),
    scheduledLecture = Color(0xFF8B7D6B),
    streakGold = Color(0xFFB38338),
    primaryAccent = Color(0xFF4E6E58)
)

val FlagshipObsidianColors = CaliperColors(
    isMonochrome = false,
    isLightGlass = false,
    baseSurface = Color(0xFF0B0E14),
    baseSurfaceSecondary = Color(0xFF121722),
    containerSurface = Color(0xFF171C28).copy(alpha = 0.90f),
    cardFill = Color(0xFF131822).copy(alpha = 0.85f),
    cardFillSecondary = Color(0xFF1B2230).copy(alpha = 0.85f),
    cardBorderTop = Color.White.copy(alpha = 0.18f),
    cardBorderBottom = Color.White.copy(alpha = 0.04f),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    divider = Color.White.copy(alpha = 0.08f),
    safeZone = Color(0xFF4E8763),
    safeZoneContainer = Color(0xFF4E8763).copy(alpha = 0.18f),
    bunkDanger = Color(0xFFE06C62),
    deadlineWarning = Color(0xFFD99B52),
    scheduledLecture = Color(0xFF9E8E7A),
    streakGold = Color(0xFFD9A54C),
    primaryAccent = Color(0xFFD5CBBF)
)

val LocalCaliperColors = staticCompositionLocalOf { WarmAlabasterGlassColors }

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
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
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
        targetValue = if (isPressed) 0.97f else 1.0f,
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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val caliperColors = if (darkTheme) FlagshipObsidianColors else WarmAlabasterGlassColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = caliperColors.scheduledLecture,
            secondary = caliperColors.safeZone,
            tertiary = caliperColors.deadlineWarning,
            background = caliperColors.baseSurface,
            surface = caliperColors.cardFill,
            surfaceVariant = caliperColors.containerSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = caliperColors.textPrimary,
            onSurface = caliperColors.textPrimary,
            onSurfaceVariant = caliperColors.textSecondary,
            error = caliperColors.bunkDanger
        )
    } else {
        lightColorScheme(
            primary = caliperColors.scheduledLecture,
            secondary = caliperColors.safeZone,
            tertiary = caliperColors.deadlineWarning,
            background = caliperColors.baseSurface,
            surface = caliperColors.cardFill,
            surfaceVariant = caliperColors.containerSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
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
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
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
