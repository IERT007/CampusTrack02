package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. DUAL-THEME ENGINE PALETTE DEFINITIONS
// ============================================================================

// --- Mode A: Monochrome Dark Mode (Pure White & Black Luxury Glass) ---
val MonochromePureBlack = Color(0xFF000000)
val MonochromeObsidian = Color(0xFF060709)
val MonochromeSurface = Color(0xFF0C0E12)
val MonochromeCardFill = Color(0x0AFFFFFF)      // Color.White.copy(alpha = 0.04f)
val MonochromeCardBorder = Color(0x14FFFFFF)    // Color.White.copy(alpha = 0.08f)
val MonochromePureWhite = Color(0xFFFFFFFF)
val MonochromeSilverGray = Color(0xFF94A3B8)
val MonochromeMutedGray = Color(0xFF64748B)

// --- Mode B: Normal Mode (Color Psychology Semantic Palette) ---
val PsychologyDarkSlate = Color(0xFF0B0E14)
val PsychologySurface = Color(0xFF10141D)
val PsychologyCardSurface = Color(0xFF151B26)
val PsychologyGreen = Color(0xFF22C55E)          // Safe-Zone status (Calm & Balanced)
val PsychologyGreenDark = Color(0xFF16A34A)
val PsychologyBlue = Color(0xFF0EA5E9)           // Scheduled lecture cards & Timetable (Reliable & Focus)
val PsychologyTeal = Color(0xFF14B8A6)
val PsychologyOrange = Color(0xFFF97316)         // Assignment/Drawing Sheet deadlines & Buffer warning
val PsychologyAmber = Color(0xFFF59E0B)
val PsychologyRed = Color(0xFFEF4444)            // Bunk action & Debar danger
val PsychologyGold = Color(0xFFEAB308)           // Daily Attendance Streak badge

// ============================================================================
// 2. FOUNDATION SURFACE & GLASS TOKENS
// ============================================================================
val AmoledBackground = Color(0xFF05070B)
val AmoledSurface = Color(0xFF0C0E14)
val AmoledCardSurface = Color(0xFF10141D)
val AmoledCardSurfaceElevated = Color(0xFF161B26)

// Frosted Glass Containers (alpha = 0.04f to 0.08f)
val GlassBorderTop = Color(0x2EFFFFFF)
val GlassBorderBottom = Color(0x0AFFFFFF)
val GlassFill = Color(0x0AFFFFFF)               // 0.04f white fill
val GlassFillStrong = Color(0x14FFFFFF)
val GlassFillLight = Color(0x08FFFFFF)

// Core Caliper Luxury Palette Aliases
val VelvetSageEmerald = PsychologyGreen
val SageMint = PsychologyGreen
val IceBlue = PsychologyBlue
val MutedAmber = PsychologyAmber
val MutedChampagneAmber = PsychologyAmber
val SoftCoral = PsychologyRed
val SoftCoralRose = PsychologyRed
val SlateTeal = PsychologyTeal
val ArchitecturalTitanium = MonochromeMutedGray
val SlateAccent = MonochromeSilverGray

// UI Semantic Compatibility Aliases
val NeonCyan = PsychologyBlue
val NeonEmerald = PsychologyGreen
val ElectricViolet = Color(0xFFA78BFA)
val IceSky = PsychologyBlue
val WarningAmber = PsychologyAmber
val DangerRed = PsychologyRed
val ChillGreen = PsychologyGreen

// Text Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Strictness Colors
val StrictRed = PsychologyRed
val ModerateAmber = PsychologyAmber
val ChillEmerald = PsychologyGreen
