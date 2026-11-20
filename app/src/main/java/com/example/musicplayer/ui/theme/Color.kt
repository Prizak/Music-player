package com.example.musicplayer.ui.theme

import androidx.compose.ui.graphics.Color

val Coral = Color(0xFFFF4D6D)
val Amber = Color(0xFFFFAA00)
val Cyan = Color(0xFF00E5FF)

// Dark (OLED)
val DarkBackground = Color(0xFF121316)
val DarkSurfaceLowest = Color(0xFF0D0E11)
val DarkSurfaceLow = Color(0xFF1A1B1F)
val DarkSurface = Color(0xFF1E1F24)
val DarkSurfaceHigh = Color(0xFF292A2F)
val DarkSurfaceHighest = Color(0xFF34353A)
val DarkOnSurface = Color(0xFFE4E2E6)
val DarkOnSurfaceVariant = Color(0xFFC8C5D0)
val DarkOutline = Color(0xFF918F9A)
val DarkOutlineVariant = Color(0xFF47464F)

// Light
val LightPrimary = Color(0xFFE63956)
val LightPrimaryContainer = Color(0xFFFFE8EC)
val LightSecondary = Color(0xFF006874)
val LightBackground = Color(0xFFFAF8FD)
val LightSurfaceLowest = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFF4F2F8)
val LightSurface = Color(0xFFEEEBF2)
val LightSurfaceHigh = Color(0xFFE8E5EC)
val LightSurfaceHighest = Color(0xFFE2DFE7)
val LightOnSurface = Color(0xFF1B1B1F)
val LightOnSurfaceVariant = Color(0xFF47464F)
val LightOutline = Color(0xFF777680)
val LightOutlineVariant = Color(0xFFC8C5D0)

val CoralHot = Color(0xFFFF3366)
val CoralDeep = Color(0xFFE63956)
val RankGold = Color(0xFFFFD700)
val RankSilver = Color(0xFFC0C0C0)
val RankBronze = Color(0xFFCD7F32)

/** Gradiente de marca do Stitch (coral -> #FF3366 -> âmbar), usado em chips ativos, botão play e progresso. */
val BrandGradient: androidx.compose.ui.graphics.Brush
    get() = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Coral, CoralHot, Amber))
