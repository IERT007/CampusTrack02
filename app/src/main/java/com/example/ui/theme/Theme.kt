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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.audio.CaliperHapticManager

/**
 * 5 Curated Eye-Soothing Theme Presets for Caliper v2.0
 */
enum class CaliperThemePreset(
    val key: String,
    val displayName: String,
    val subtitle: String
) {
    OBSIDIAN_ICE(
        key = "obsidian_ice",
        displayName = "Obsidian Ice",
        subtitle = "Pitch black • Ice blue • Emerald"
    ),
    CARBON_MINT(
        key = "carbon_mint",
        displayName = "Carbon Mint",
        subtitle = "Deep carbon • Soft mint teal"
    ),
    MIDNIGHT_INDIGO(
        key = "midnight_indigo",
        displayName = "Midnight Indigo",
        subtitle = "Night navy • Calm indigo"
    ),
    WARM_GRAPHITE(
        key = "warm_graphite",
        displayName = "Warm Graphite",
        subtitle = "Warm charcoal • Amber gold"
    ),
    PURE_MONOCHROME(
        key = "pure_monochrome",
        displayName = "Pure Monochrome",
        subtitle = "True black • High-contrast silver"
    );

    companion object {
        fun fromKey(key: String?): CaliperThemePreset {
            return entries.firstOrNull { it.key == key } ?: OBSIDIAN_ICE
        }
    }
}

/**
 * Centralized Design Token System for Caliper v2.0
 * Every surface, border, text, and semantic accent reads from CaliperTheme.colors.
 */
@Immutable
data class CaliperColorTokens(
    val preset: CaliperThemePreset,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val primaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val safeGreen: Color,
    val warnAmber: Color,
    val dangerRed: Color,
    val offSlate: Color,
    val onPrimaryAccent: Color = Color.Black,
    val isMonochrome: Boolean = false
) {
    // Seamless compatibility accessors for existing components
    val baseSurface: Color get() = background
    val containerSurface: Color get() = surface
    val cardFill: Color get() = surfaceVariant.copy(alpha = 0.88f)
    val cardBorderTop: Color get() = border
    val cardBorderBottom: Color get() = Color.Transparent
    val textMuted: Color get() = textSecondary.copy(alpha = 0.78f)
    val safeZone: Color get() = safeGreen
    val safeZoneContainer: Color get() = safeGreen.copy(alpha = 0.15f)
    val scheduledLecture: Color get() = primaryAccent
    val deadlineWarning: Color get() = warnAmber
    val bunkDanger: Color get() = dangerRed
    val streakGold: Color get() = warnAmber
    val divider: Color get() = border.copy(alpha = 0.55f)
}

typealias CaliperColors = CaliperColorTokens

val ObsidianIceTokens = CaliperColorTokens(
    preset = CaliperThemePreset.OBSIDIAN_ICE,
    background = ObsidianIceBg,
    surface = ObsidianIceSurface,
    surfaceVariant = ObsidianIceSurfaceVariant,
    border = ObsidianIceBorder,
    primaryAccent = ObsidianIceAccent,
    textPrimary = ObsidianIceTextPrimary,
    textSecondary = ObsidianIceTextSecondary,
    safeGreen = ObsidianIceSafe,
    warnAmber = ObsidianIceWarn,
    dangerRed = ObsidianIceDanger,
    offSlate = ObsidianIceOffSlate,
    onPrimaryAccent = Color.Black,
    isMonochrome = false
)

val CarbonMintTokens = CaliperColorTokens(
    preset = CaliperThemePreset.CARBON_MINT,
    background = CarbonMintBg,
    surface = CarbonMintSurface,
    surfaceVariant = CarbonMintSurfaceVariant,
    border = CarbonMintBorder,
    primaryAccent = CarbonMintAccent,
    textPrimary = CarbonMintTextPrimary,
    textSecondary = CarbonMintTextSecondary,
    safeGreen = CarbonMintSafe,
    warnAmber = CarbonMintWarn,
    dangerRed = CarbonMintDanger,
    offSlate = CarbonMintOffSlate,
    onPrimaryAccent = Color.Black,
    isMonochrome = false
)

val MidnightIndigoTokens = CaliperColorTokens(
    preset = CaliperThemePreset.MIDNIGHT_INDIGO,
    background = MidnightIndigoBg,
    surface = MidnightIndigoSurface,
    surfaceVariant = MidnightIndigoSurfaceVariant,
    border = MidnightIndigoBorder,
    primaryAccent = MidnightIndigoAccent,
    textPrimary = MidnightIndigoTextPrimary,
    textSecondary = MidnightIndigoTextSecondary,
    safeGreen = MidnightIndigoSafe,
    warnAmber = MidnightIndigoWarn,
    dangerRed = MidnightIndigoDanger,
    offSlate = MidnightIndigoOffSlate,
    onPrimaryAccent = Color.Black,
    isMonochrome = false
)

val WarmGraphiteTokens = CaliperColorTokens(
    preset = CaliperThemePreset.WARM_GRAPHITE,
    background = WarmGraphiteBg,
    surface = WarmGraphiteSurface,
    surfaceVariant = WarmGraphiteSurfaceVariant,
    border = WarmGraphiteBorder,
    primaryAccent = WarmGraphiteAccent,
    textPrimary = WarmGraphiteTextPrimary,
    textSecondary = WarmGraphiteTextSecondary,
    safeGreen = WarmGraphiteSafe,
    warnAmber = WarmGraphiteWarn,
    dangerRed = WarmGraphiteDanger,
    offSlate = WarmGraphiteOffSlate,
    onPrimaryAccent = Color.Black,
    isMonochrome = false
)

val PureMonochromeTokens = CaliperColorTokens(
    preset = CaliperThemePreset.PURE_MONOCHROME,
    background = PureMonoBg,
    surface = PureMonoSurface,
    surfaceVariant = PureMonoSurfaceVariant,
    border = PureMonoBorder,
    primaryAccent = PureMonoAccent,
    textPrimary = PureMonoTextPrimary,
    textSecondary = PureMonoTextSecondary,
    safeGreen = PureMonoSafe,
    warnAmber = PureMonoWarn,
    dangerRed = PureMonoDanger,
    offSlate = PureMonoOffSlate,
    onPrimaryAccent = Color.Black,
    isMonochrome = true
)

val FlagshipObsidianColors = ObsidianIceTokens

fun tokensForPreset(preset: CaliperThemePreset): CaliperColorTokens = when (preset) {
    CaliperThemePreset.OBSIDIAN_ICE -> ObsidianIceTokens
    CaliperThemePreset.CARBON_MINT -> CarbonMintTokens
    CaliperThemePreset.MIDNIGHT_INDIGO -> MidnightIndigoTokens
    CaliperThemePreset.WARM_GRAPHITE -> WarmGraphiteTokens
    CaliperThemePreset.PURE_MONOCHROME -> PureMonochromeTokens
}

fun tokensForPresetKey(key: String?): CaliperColorTokens =
    tokensForPreset(CaliperThemePreset.fromKey(key))

val LocalCaliperColors = staticCompositionLocalOf { ObsidianIceTokens }

object CaliperTheme {
    val colors: CaliperColorTokens
        @Composable
        get() = LocalCaliperColors.current
}

/**
 * Living Fluid Touch Feedback Modifier
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

/**
 * Tactile Pressed & Inset Mechanics
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
    themePresetKey: String = CaliperThemePreset.OBSIDIAN_ICE.key,
    isMonochrome: Boolean = false,
    content: @Composable () -> Unit
) {
    val resolvedPreset = if (isMonochrome && themePresetKey == CaliperThemePreset.OBSIDIAN_ICE.key) {
        CaliperThemePreset.PURE_MONOCHROME
    } else {
        CaliperThemePreset.fromKey(themePresetKey)
    }

    val caliperColors = remember(resolvedPreset) { tokensForPreset(resolvedPreset) }

    val materialColorScheme = darkColorScheme(
        primary = caliperColors.primaryAccent,
        onPrimary = caliperColors.onPrimaryAccent,
        secondary = caliperColors.safeGreen,
        onSecondary = Color.Black,
        tertiary = caliperColors.warnAmber,
        background = caliperColors.background,
        onBackground = caliperColors.textPrimary,
        surface = caliperColors.surface,
        onSurface = caliperColors.textPrimary,
        surfaceVariant = caliperColors.surfaceVariant,
        onSurfaceVariant = caliperColors.textSecondary,
        outline = caliperColors.border,
        error = caliperColors.dangerRed,
        onError = Color.White
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = caliperColors.background.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = caliperColors.background.toArgb()
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
