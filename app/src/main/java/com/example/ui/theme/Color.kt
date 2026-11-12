package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. REFERENCE SHEET NEUMORPHIC PALETTE (LIGHT MODE - WARM SAND & DEEP TEAL)
// ============================================================================
val CalmIvoryCanvas = Color(0xFFEFECE6)          // Base Layer
val CalmIvorySurface = Color(0xFFE5DFD5)         // Inset Layer (Recessed)
val CalmCardRaised = Color(0xFFF7F4EE)           // Raised Layer (Elevated)
val CalmCardPressed = Color(0xFFDDD5C8)          // Pressed Layer
val CalmCardGlass = Color(0xFFF7F4EE).copy(alpha = 0.94f)
val CalmShadowDark = Color(0xFFC9C0B1)           // Warm Drop Shadow
val CalmHighlightLight = Color(0xFFFFFFFF)       // Top-Left Specular Bevel

val CalmSagePrimary = Color(0xFF174E4F)          // Deep Teal-Slate Primary Accent
val CalmSageDark = Color(0xFF0F3839)             // Pressed Deep Teal
val CalmMoss = Color(0xFF5F7464)                 // Secondary Sage
val CalmMist = Color(0xFFD8DDD8)                 // Track / Subtle Pill Fill
val CalmSand = Color(0xFFC9BDAF)                 // Warm Sand Neutral

val CalmInkPrimary = Color(0xFF162423)           // High-Contrast Deep Charcoal-Teal Ink
val CalmInkSecondary = Color(0xFF475755)         // Crisp Slate-Teal Label
val CalmInkMuted = Color(0xFF73807E)             // Muted Timestamp / Hint

// ============================================================================
// 2. DARK NEUMORPHIC OBSIDIAN PALETTE (DARK MODE)
// ============================================================================
val DarkBaseCanvas = Color(0xFF111517)           // Deep Charcoal-Obsidian Base
val DarkInsetSurface = Color(0xFF0B0E10)         // Recessed Dark Inset
val DarkRaisedCard = Color(0xFF191F22)           // Elevated Dark Card
val DarkPressedCard = Color(0xFF13181B)          // Pressed Dark State
val DarkShadowDeep = Color(0xFF060809)           // Deep Drop Shadow
val DarkHighlightRim = Color(0xFF283136)         // Subtle Top-Left Rim Light
val DarkTealAccent = Color(0xFF2DD4BF)           // Luminous Teal Accent for Dark Mode

// ============================================================================
// 3. SEMANTIC & PSYCHOLOGY TOKENS
// ============================================================================
val PsychologyDarkSlate = CalmIvoryCanvas
val PsychologySurface = CalmIvorySurface
val PsychologyCardSurface = CalmCardGlass
val PsychologyGreen = Color(0xFF1E7A56)
val PsychologyGreenDark = Color(0xFF14573D)
val PsychologyBlue = Color(0xFF25638A)
val PsychologyTeal = Color(0xFF174E4F)
val PsychologyOrange = Color(0xFFC86428)
val PsychologyAmber = Color(0xFFB4781E)
val PsychologyRed = Color(0xFFC23B3B)
val PsychologyGold = Color(0xFFB08222)

// ============================================================================
// 4. COMPATIBILITY ALIASES
// ============================================================================
val AmoledBackground = CalmIvoryCanvas
val AmoledSurface = CalmIvorySurface
val AmoledCardSurface = CalmCardGlass
val AmoledCardSurfaceElevated = CalmCardRaised

val GlassBorderTop = Color.White.copy(alpha = 0.90f)
val GlassBorderBottom = CalmShadowDark.copy(alpha = 0.65f)
val GlassFill = CalmCardGlass
val GlassFillStrong = CalmCardRaised
val GlassFillLight = CalmIvorySurface

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
val ElectricViolet = Color(0xFF5E548E)
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
