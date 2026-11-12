package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. DUAL-THEME ENGINE PALETTE DEFINITIONS
// ============================================================================

// --- Mode A: Monochrome Dark Mode (Pure White & Black Luxury Glass) ---
val MonochromePureBlack = Color(0xFF000000)
val MonochromeObsidian = Color(0xFF05070A)
val MonochromeSurface = Color(0xFF080B10)
val MonochromeCardFill = Color(0xFF0E121B).copy(alpha = 0.8f)
val MonochromeCardBorder = Color.White.copy(alpha = 0.10f)
val MonochromePureWhite = Color(0xFFFFFFFF)
val MonochromeSilverGray = Color(0xFFCBD5E1) // Soft silver secondary labels
val MonochromeMutedGray = Color(0xFF94A3B8)

// --- Mode B: Normal Mode (Color Psychology Semantic Palette) ---
val PsychologyDarkSlate = Color(0xFF000000) // True Pitch Black canvas background
val PsychologySurface = Color(0xFF07090E)
val PsychologyCardSurface = Color(0xFF0E121B).copy(alpha = 0.8f) // Subtle translucent obsidian
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
val AmoledBackground = Color(0xFF000000) // True Pitch Black
val AmoledSurface = Color(0xFF05070A)
val AmoledCardSurface = Color(0xFF0E121B).copy(alpha = 0.85f) // Deep Obsidian Glass
val AmoledCardSurfaceElevated = Color(0xFF141924).copy(alpha = 0.85f)

// Frosted Glass Containers - Luminous Directional Top-Edge Border (0.16f to Transparent)
val GlassBorderTop = Color.White.copy(alpha = 0.16f)
val GlassBorderBottom = Color.Transparent
val GlassFill = Color(0xFF0E121B).copy(alpha = 0.85f)
val GlassFillStrong = Color(0xFF141A26).copy(alpha = 0.85f)
val GlassFillLight = Color.White.copy(alpha = 0.03f)

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
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFCBD5E1)
val TextMuted = Color(0xFF94A3B8)

// Strictness Colors
val StrictRed = PsychologyRed
val ModerateAmber = PsychologyAmber
val ChillEmerald = PsychologyGreen
