package com.example.musicplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private fun ts(w: FontWeight, size: TextUnit, lineHeight: TextUnit, letterSpacing: TextUnit = 0.sp) =
    TextStyle(fontFamily = FontFamily.Default, fontWeight = w, fontSize = size, lineHeight = lineHeight, letterSpacing = letterSpacing)

// Jost / Instrument Sans exigiriam fontes embarcadas ou a dependência de Google Fonts; usa-se FontFamily.Default
// reproduzindo tamanhos, pesos, alturas de linha e letterSpacing da escala do Stitch (versão mobile).
val Typography = Typography(
    displayLarge = ts(FontWeight.Bold, 32.sp, 38.sp, (-0.25).sp),
    displayMedium = ts(FontWeight.Bold, 28.sp, 34.sp, (-0.25).sp),
    displaySmall = ts(FontWeight.Bold, 24.sp, 30.sp),
    headlineLarge = ts(FontWeight.SemiBold, 28.sp, 34.sp),
    headlineMedium = ts(FontWeight.SemiBold, 24.sp, 30.sp),
    headlineSmall = ts(FontWeight.SemiBold, 20.sp, 26.sp, (-0.2).sp), // tracking-tight
    titleLarge = ts(FontWeight.SemiBold, 18.sp, 24.sp),
    titleMedium = ts(FontWeight.SemiBold, 16.sp, 22.sp, 0.15.sp),
    titleSmall = ts(FontWeight.SemiBold, 14.sp, 20.sp, 0.1.sp),
    bodyLarge = ts(FontWeight.Normal, 16.sp, 24.sp, 0.25.sp),
    bodyMedium = ts(FontWeight.Normal, 14.sp, 20.sp, 0.25.sp),
    bodySmall = ts(FontWeight.Normal, 12.sp, 16.sp, 0.4.sp),
    labelLarge = ts(FontWeight.SemiBold, 14.sp, 20.sp, 0.1.sp),
    labelMedium = ts(FontWeight.Medium, 12.sp, 16.sp, 0.5.sp),
    labelSmall = ts(FontWeight.Medium, 11.sp, 14.sp, 0.5.sp)
)
