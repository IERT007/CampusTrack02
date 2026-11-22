package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// CALIPER v2.0 — 5 EYE-SOOTHING THEME PRESET PALETTES
// ============================================================================

// 1. Obsidian Ice (Default)
val ObsidianIceBg = Color(0xFF000000)
val ObsidianIceSurface = Color(0xFF090D14)
val ObsidianIceSurfaceVariant = Color(0xFF101622)
val ObsidianIceBorder = Color(0x28FFFFFF)
val ObsidianIceAccent = Color(0xFF38BDF8)
val ObsidianIceTextPrimary = Color(0xFFF8FAFC)
val ObsidianIceTextSecondary = Color(0xFF94A3B8)
val ObsidianIceSafe = Color(0xFF34D399)
val ObsidianIceWarn = Color(0xFFF59E0B)
val ObsidianIceDanger = Color(0xFFEF4444)
val ObsidianIceOffSlate = Color(0xFF64748B)

// 2. Carbon Mint
val CarbonMintBg = Color(0xFF0B0E11)
val CarbonMintSurface = Color(0xFF12171D)
val CarbonMintSurfaceVariant = Color(0xFF182028)
val CarbonMintBorder = Color(0x24CCFBF1)
val CarbonMintAccent = Color(0xFF2DD4BF)
val CarbonMintTextPrimary = Color(0xFFF0FDFA)
val CarbonMintTextSecondary = Color(0xFF94A3B8)
val CarbonMintSafe = Color(0xFF34D399)
val CarbonMintWarn = Color(0xFFFBBF24)
val CarbonMintDanger = Color(0xFFF87171)
val CarbonMintOffSlate = Color(0xFF64748B)

// 3. Midnight Indigo
val MidnightIndigoBg = Color(0xFF090B12)
val MidnightIndigoSurface = Color(0xFF101423)
val MidnightIndigoSurfaceVariant = Color(0xFF171D32)
val MidnightIndigoBorder = Color(0x28C7D2FE)
val MidnightIndigoAccent = Color(0xFF818CF8)
val MidnightIndigoTextPrimary = Color(0xFFEEF2FF)
val MidnightIndigoTextSecondary = Color(0xFF9CA3AF)
val MidnightIndigoSafe = Color(0xFF34D399)
val MidnightIndigoWarn = Color(0xFFFBBF24)
val MidnightIndigoDanger = Color(0xFFFB7185)
val MidnightIndigoOffSlate = Color(0xFF64748B)

// 4. Warm Graphite
val WarmGraphiteBg = Color(0xFF121110)
val WarmGraphiteSurface = Color(0xFF1B1917)
val WarmGraphiteSurfaceVariant = Color(0xFF24211E)
val WarmGraphiteBorder = Color(0x26FDE68A)
val WarmGraphiteAccent = Color(0xFFF59E0B)
val WarmGraphiteTextPrimary = Color(0xFFFAFAF9)
val WarmGraphiteTextSecondary = Color(0xFFA8A29E)
val WarmGraphiteSafe = Color(0xFF4ADE80)
val WarmGraphiteWarn = Color(0xFFF59E0B)
val WarmGraphiteDanger = Color(0xFFF87171)
val WarmGraphiteOffSlate = Color(0xFF78716C)

// 5. Pure Monochrome (Crisp pitch black + high-contrast white/silver)
val PureMonoBg = Color(0xFF000000)
val PureMonoSurface = Color(0xFF0A0A0A)
val PureMonoSurfaceVariant = Color(0xFF141414)
val PureMonoBorder = Color(0x30FFFFFF)
val PureMonoAccent = Color(0xFFF8FAFC)
val PureMonoTextPrimary = Color(0xFFFFFFFF)
val PureMonoTextSecondary = Color(0xFFB4BCC8)
val PureMonoSafe = Color(0xFFE2E8F0)
val PureMonoWarn = Color(0xFFCBD5E1)
val PureMonoDanger = Color(0xFFF87171)
val PureMonoOffSlate = Color(0xFF64748B)

// ============================================================================
// BACKWARDS-COMPATIBLE TOKEN ALIASES (For secondary dialogs & widgets)
// ============================================================================
val MonochromePureBlack = ObsidianIceBg
val MonochromeObsidian = ObsidianIceSurface
val MonochromeSurface = ObsidianIceSurface
val MonochromeCardFill = ObsidianIceSurfaceVariant.copy(alpha = 0.85f)
val MonochromeCardBorder = Color.White.copy(alpha = 0.12f)
val MonochromePureWhite = Color(0xFFFFFFFF)
val MonochromeSilverGray = Color(0xFFCBD5E1)
val MonochromeMutedGray = Color(0xFF94A3B8)

val PsychologyDarkSlate = ObsidianIceBg
val PsychologySurface = ObsidianIceSurface
val PsychologyCardSurface = ObsidianIceSurfaceVariant.copy(alpha = 0.85f)
val PsychologyGreen = ObsidianIceSafe
val PsychologyGreenDark = Color(0xFF10B981)
val PsychologyBlue = ObsidianIceAccent
val PsychologyTeal = CarbonMintAccent
val PsychologyOrange = Color(0xFFF97316)
val PsychologyAmber = ObsidianIceWarn
val PsychologyRed = ObsidianIceDanger
val PsychologyGold = Color(0xFFEAB308)

val AmoledBackground = ObsidianIceBg
val AmoledSurface = ObsidianIceSurface
val AmoledCardSurface = ObsidianIceSurfaceVariant.copy(alpha = 0.85f)
val AmoledCardSurfaceElevated = Color(0xFF141924).copy(alpha = 0.85f)

val GlassBorderTop = Color.White.copy(alpha = 0.16f)
val GlassBorderBottom = Color.Transparent
val GlassFill = ObsidianIceSurfaceVariant.copy(alpha = 0.85f)
val GlassFillStrong = Color(0xFF141A26).copy(alpha = 0.85f)
val GlassFillLight = Color.White.copy(alpha = 0.03f)

val VelvetSageEmerald = ObsidianIceSafe
val SageMint = ObsidianIceSafe
val IceBlue = ObsidianIceAccent
val MutedAmber = ObsidianIceWarn
val MutedChampagneAmber = ObsidianIceWarn
val SoftCoral = ObsidianIceDanger
val SoftCoralRose = ObsidianIceDanger
val SlateTeal = CarbonMintAccent
val ArchitecturalTitanium = ObsidianIceTextSecondary
val SlateAccent = MonochromeSilverGray

val NeonCyan = ObsidianIceAccent
val NeonEmerald = ObsidianIceSafe
val ElectricViolet = MidnightIndigoAccent
val IceSky = ObsidianIceAccent
val WarningAmber = ObsidianIceWarn
val DangerRed = ObsidianIceDanger
val ChillGreen = ObsidianIceSafe

val TextPrimary = ObsidianIceTextPrimary
val TextSecondary = MonochromeSilverGray
val TextMuted = ObsidianIceTextSecondary

val StrictRed = ObsidianIceDanger
val ModerateAmber = ObsidianIceWarn
val ChillEmerald = ObsidianIceSafe
