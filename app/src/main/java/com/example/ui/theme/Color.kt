package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Deep Space & Celestial Nebula Palette
// ==========================================

// Deep Space Void & Starlight Backdrops
val SpaceVoid = Color(0xFF030712)         // Deepest interstellar black
val SpaceBackground = Color(0xFF080D1A)   // Galactic dark navy
val SpaceSurface = Color(0xFF0F172A)      // Deep shuttle surface
val SpaceCard = Color(0xFF131D36)         // Nebula observation bay card
val SpaceCardSurface = SpaceCard
val SpaceCardBorder = Color(0xFF1E2D4E)   // Constellation grid border
val SpaceCardBorderGlow = Color(0xFF38BDF8)

// Cosmic Starlight Accents
val CosmicCyan = Color(0xFF38BDF8)        // Stellar accretion disk cyan
val CosmicCyanGlow = Color(0xFF7DD3FC)
val CosmicCyanContainer = Color(0xFF0C2B47)

val SupernovaPurple = Color(0xFF818CF8)   // Pulsar purple / violet
val SupernovaPurpleContainer = Color(0xFF231F4D)

val DeepNebulaMagenta = Color(0xFFA855F7) // Nebula dust magenta
val DeepNebulaMagentaContainer = Color(0xFF3B1D54)

val OrbitEmerald = Color(0xFF10B981)      // Bio-station green / online beacon
val OrbitEmeraldGlow = Color(0xFF34D399)
val OrbitEmeraldContainer = Color(0xFF064E3B)

val SolarFlareGold = Color(0xFFFBBF24)    // Solar flare gold / token number highlight
val SolarFlareContainer = Color(0xFF452B04)

val MarsCrimson = Color(0xFFF43F5E)       // Solar storm alert
val NovaPink = Color(0xFFEC4899)          // Quasar pink

// Space Typography
val StarlightPrimary = Color(0xFFF8FAFC)  // Crisp star white
val StarlightSecondary = Color(0xFF94A3B8)// Lunar gray
val StarlightTertiary = Color(0xFF64748B) // Deep orbit dust gray

// External Research Sources
val WebBrowserBlue = Color(0xFF38BDF8)
val RedditOrange = Color(0xFFFF5722)
val YouTubeRed = Color(0xFFFF1744)
val WikiGray = Color(0xFF94A3B8)

// Compatibility Aliases for legacy screens
val ObsidianDeep = SpaceVoid
val ObsidianBackground = SpaceBackground
val ObsidianSurface = SpaceSurface
val ObsidianCard = SpaceCard
val ObsidianCardBorder = SpaceCardBorder
val ObsidianCardBorderGlow = SpaceCardBorderGlow
val ElectricIndigo = SupernovaPurple
val ElectricIndigoContainer = SupernovaPurpleContainer
val ElectricCyan = CosmicCyan
val ElectricCyanGlow = CosmicCyanGlow
val ElectricCyanContainer = CosmicCyanContainer
val ElectricViolet = DeepNebulaMagenta
val ElectricVioletContainer = DeepNebulaMagentaContainer
val NeonEmerald = OrbitEmerald
val NeonEmeraldGlow = OrbitEmeraldGlow
val NeonEmeraldContainer = OrbitEmeraldContainer
val AmberWarning = SolarFlareGold
val CrimsonError = MarsCrimson
val SunsetRose = NovaPink
val TextPrimary = StarlightPrimary
val TextSecondary = StarlightSecondary
val TextTertiary = StarlightTertiary

// Space Celestial Gradients
val SpaceNebulaGradient = listOf(Color(0xFF030712), Color(0xFF0B132B), Color(0xFF1C1A44))
val StarlightGlowGradient = listOf(CosmicCyan, SupernovaPurple, DeepNebulaMagenta)
val UserMessageGradient = listOf(Color(0xFF4338CA), Color(0xFF0284C7))
val AssistantMessageGradient = listOf(Color(0xFF131D36), Color(0xFF0F172A))
val HeroCardGradient = listOf(Color(0xFF1E1B4B), Color(0xFF0C2442), Color(0xFF080D1A))
val AccentGlowGradient = listOf(DeepNebulaMagenta, CosmicCyan)
val EmeraldGlowGradient = listOf(OrbitEmerald, CosmicCyan)
val CardBorderGradient = listOf(CosmicCyan.copy(alpha = 0.5f), SupernovaPurple.copy(alpha = 0.25f))
val CardBorderGradientSubtle = listOf(SupernovaPurple.copy(alpha = 0.3f), CosmicCyan.copy(alpha = 0.15f))
val CyberGlowGradient = listOf(CosmicCyan, SupernovaPurple)
val GoldBadgeGradient = listOf(SolarFlareGold, Color(0xFFD97706))
val TokenRankGradient = listOf(SolarFlareGold, CosmicCyan)
