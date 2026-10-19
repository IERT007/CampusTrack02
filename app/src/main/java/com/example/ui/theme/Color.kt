package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. PRIMARY ACCENTS & METALLICS
// ============================================================================
val ElectricIndigo = Color(0xFF6366F1)
val ElectricViolet = Color(0xFF8B5CF6)
val ArchitecturalTitanium = Color(0xFFE2E8F0)
val MetallicSilver = Color(0xFFE2E8F0)
val SlateAccent = Color(0xFF64748B)
val SlateTeal = Color(0xFF14B8A6)
val NeonCyan = Color(0xFF06B6D4)
val IceBlue = Color(0xFF38BDF8)
val IceSky = Color(0xFF7DD3FC)

// ============================================================================
// 2. FOUNDATION SURFACE & GLASS TOKENS (TRUE PITCH BLACK & OBSIDIAN GLASS)
// ============================================================================
val AmoledBackground = Color(0xFF000000) // True Pitch Black (#000000)
val AmoledSurface = Color(0xFF070A0F)
val AmoledCardSurface = Color(0xFF0E121B).copy(alpha = 0.85f)
val AmoledCardSurfaceElevated = Color(0xFF141923).copy(alpha = 0.90f)
val DeepObsidianCard = Color(0xFF0E121B).copy(alpha = 0.85f)
val ObsidianGlassSurface = Color(0xFF0E121B).copy(alpha = 0.85f)
val ObsidianGlassElevated = Color(0xFF141923).copy(alpha = 0.90f)

val GlassFill = Color(0xFF0E121B).copy(alpha = 0.85f)
val GlassFillLight = Color.White.copy(alpha = 0.06f)
val GlassFillStrong = Color(0xFF141923).copy(alpha = 0.92f)
val GlassBorderTop = Color.White.copy(alpha = 0.16f)
val GlassBorderBottom = Color.White.copy(alpha = 0.04f)
val SpecularHighlightBorder = Color.White.copy(alpha = 0.16f)
val LuminousTopBorderGradient = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)

// ============================================================================
// 3. MONOCHROME TOKENS
// ============================================================================
val MonochromePureBlack = Color(0xFF000000)
val MonochromeObsidian = Color(0xFF0E121B)
val MonochromeSurface = Color(0xFF141923)
val MonochromeCardFill = Color(0xFF0E121B).copy(alpha = 0.85f)
val MonochromeCardBorder = Color.White.copy(alpha = 0.16f)
val MonochromePureWhite = Color(0xFFF8FAFC)
val MonochromeSilverGray = Color(0xFF94A3B8)
val MonochromeMutedGray = Color(0xFF64748B)

// ============================================================================
// 4. SEMANTIC TELEMETRY PALETTE
// ============================================================================
val EmeraldGreen = Color(0xFF10B981)
val VelvetSageEmerald = Color(0xFF22C55E)
val ChillEmerald = Color(0xFF10B981)
val ChillGreen = Color(0xFF22C55E)
val SageMint = Color(0xFF34D399)
val NeonEmerald = Color(0xFF10B981)

val AmberWarning = Color(0xFFF59E0B)
val WarningAmber = Color(0xFFF59E0B)
val ModerateAmber = Color(0xFFF59E0B)
val MutedAmber = Color(0xFFD97706)
val MutedChampagneAmber = Color(0xFFFBBF24)

val CrimsonDanger = Color(0xFFEF4444)
val DangerRed = Color(0xFFEF4444)
val StrictRed = Color(0xFFEF4444)
val SoftCoral = Color(0xFFF87171)
val SoftCoralRose = Color(0xFFFB7185)

// ============================================================================
// 5. TYPOGRAPHY HIERARCHY TOKENS
// ============================================================================
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// ============================================================================
// 6. PSYCHOLOGY & AUDIT TOKENS
// ============================================================================
val PsychologyGreen = Color(0xFF22C55E)
val PsychologyGreenDark = Color(0xFF16A34A)
val PsychologyAmber = Color(0xFFF59E0B)
val PsychologyGold = Color(0xFFEAB308)
val PsychologyOrange = Color(0xFFF97316)
val PsychologyRed = Color(0xFFEF4444)
val PsychologyBlue = Color(0xFF3B82F6)
val PsychologyTeal = Color(0xFF14B8A6)
val PsychologyDarkSlate = Color(0xFF0E121B)
val PsychologyCardSurface = Color(0xFF0E121B).copy(alpha = 0.85f)
val PsychologySurface = Color(0xFF000000)
