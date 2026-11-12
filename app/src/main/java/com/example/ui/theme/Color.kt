package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. CALM-KIT WARM IVORY & SAGE NEUMORPHIC PALETTE (PRIMARY)
// ============================================================================
val CalmIvoryCanvas = Color(0xFFF4EFE8)          // Warm Stone Ivory Background
val CalmIvorySurface = Color(0xFFEBE4DA)         // Recessed / Inset Surface
val CalmCardRaised = Color(0xFFF9F7F3)           // Elevated Tactile Card
val CalmCardGlass = Color(0xFFF9F7F3).copy(alpha = 0.88f)
val CalmShadowDark = Color(0xFFD5CEC2)           // Soft Warm Drop Shadow
val CalmHighlightLight = Color(0xFFFFFFFF)       // Top-Left Specular Bevel

val CalmSagePrimary = Color(0xFF5F7464)          // Primary Sage Green Accent
val CalmSageDark = Color(0xFF47594B)             // Pressed Sage State
val CalmMoss = Color(0xFFA7B8A7)                 // Secondary Soft Sage
val CalmMist = Color(0xFFD9E1D9)                 // Subtle Track / Pill Fill
val CalmSand = Color(0xFFC9BDAF)                 // Warm Sand Neutral

val CalmInkPrimary = Color(0xFF1E2420)           // Deep Charcoal-Olive Ink
val CalmInkSecondary = Color(0xFF5A635D)         // Muted Slate-Sage Label
val CalmInkMuted = Color(0xFF8A928C)             // Subtle Timestamp / Hint

// ============================================================================
// 2. MONOCHROME / OBSIDIAN DARK MODE PALETTE
// ============================================================================
val MonochromePureBlack = Color(0xFF000000)
val MonochromeObsidian = Color(0xFF05070A)
val MonochromeSurface = Color(0xFF080B10)
val MonochromeCardFill = Color(0xFF0E121B).copy(alpha = 0.85f)
val MonochromeCardBorder = Color.White.copy(alpha = 0.14f)
val MonochromePureWhite = Color(0xFFFFFFFF)
val MonochromeSilverGray = Color(0xFFCBD5E1)
val MonochromeMutedGray = Color(0xFF94A3B8)

// ============================================================================
// 3. SEMANTIC & PSYCHOLOGY TOKENS (CALIBRATED FOR WARM IVORY CONTRAST)
// ============================================================================
val PsychologyDarkSlate = CalmIvoryCanvas
val PsychologySurface = CalmIvorySurface
val PsychologyCardSurface = CalmCardGlass
val PsychologyGreen = Color(0xFF2E7D5B)            // Deep Botanical Emerald
val PsychologyGreenDark = Color(0xFF1F5C42)
val PsychologyBlue = Color(0xFF3B6E8C)             // Muted Slate Blue
val PsychologyTeal = Color(0xFF2A7B76)
val PsychologyOrange = Color(0xFFC86D3B)           // Warm Terracotta Warning
val PsychologyAmber = Color(0xFFB87D28)            // Warm Ochre
val PsychologyRed = Color(0xFFC44545)              // Muted Crimson Alert
val PsychologyGold = Color(0xFFB5892E)             // Warm Brass Streak

// ============================================================================
// 4. FOUNDATION SURFACE & GLASS TOKENS (COMPATIBILITY ALIASES)
// ============================================================================
val AmoledBackground = CalmIvoryCanvas
val AmoledSurface = CalmIvorySurface
val AmoledCardSurface = CalmCardGlass
val AmoledCardSurfaceElevated = CalmCardRaised

val GlassBorderTop = Color.White.copy(alpha = 0.85f)
val GlassBorderBottom = CalmShadowDark.copy(alpha = 0.45f)
val GlassFill = CalmCardGlass
val GlassFillStrong = CalmCardRaised
val GlassFillLight = CalmIvorySurface.copy(alpha = 0.65f)

val VelvetSageEmerald = PsychologyGreen
val SageMint = CalmMoss
val IceBlue = PsychologyBlue
val MutedAmber = PsychologyAmber
val MutedChampagneAmber = PsychologyAmber
val SoftCoral = PsychologyRed
val SoftCoralRose = PsychologyRed
val SlateTeal = PsychologyTeal
val ArchitecturalTitanium = CalmInkSecondary
val SlateAccent = CalmInkSecondary

val NeonCyan = CalmSagePrimary
val NeonEmerald = PsychologyGreen
val ElectricViolet = Color(0xFF6B638C)
val IceSky = PsychologyBlue
val WarningAmber = PsychologyAmber
val DangerRed = PsychologyRed
val ChillGreen = PsychologyGreen

val TextPrimary = CalmInkPrimary
val TextSecondary = CalmInkSecondary
val TextMuted = CalmInkMuted

val StrictRed = PsychologyRed
val ModerateAmber = PsychologyAmber
val ChillEmerald = PsychologyGreen
