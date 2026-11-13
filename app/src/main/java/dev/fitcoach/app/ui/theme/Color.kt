package dev.fitcoach.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette "FitCoach 2.0" presa dal mockup: fondo blu-notte quasi nero, card
 * slate con bordo sottile, testo bianco/grigio e accenti neon (ciano, viola,
 * verde, arancio) usati uno per tipo di dato.
 */

// --- Accenti neon -----------------------------------------------------------
val NeonCyan = Color(0xFF00E5FF)
val NeonCyanSoft = Color(0xFF66F0FF)
val NeonBlue = Color(0xFF2F80FF)
val NeonViolet = Color(0xFFA64BFF)
val NeonMagenta = Color(0xFFFF007F)
val NeonGreen = Color(0xFF00FF66)
val NeonOrange = Color(0xFFFF8C00)
val NeonRed = Color(0xFFFF4D5E)
val NeonYellow = Color(0xFFFFD84D)

/** Inchiostro scuro da usare sopra i riempimenti neon. */
val InkOnNeon = Color(0xFF04121A)

// --- Superfici --------------------------------------------------------------
val BgDeep = Color(0xFF0A0D14)
val BgNavy = Color(0xFF0E121B)
val CardNavy = Color(0xFF181D27)
val CardNavyHigh = Color(0xFF1E2430)
val StrokeNavy = Color(0xFF262E3C)
val StrokeNavySoft = Color(0xFF1B2230)

// --- Testo ------------------------------------------------------------------
val TextHi = Color(0xFFF5F8FF)
val TextMid = Color(0xFF8E96A4)
val TextLow = Color(0xFF5F6879)

// --- Gradienti --------------------------------------------------------------
val GradientCyan = listOf(NeonCyan, NeonBlue)
val GradientViolet = listOf(NeonViolet, NeonMagenta)
val GradientGreen = listOf(NeonGreen, Color(0xFF00C2A8))
val GradientOrange = listOf(NeonOrange, Color(0xFFFF5A5F))

// Alias storici: mantenuti perché altre schermate li importano già.
val GreenPrimary = NeonGreen
val GreenDark = Color(0xFF00C2A8)
val GreenLight = Color(0xFF7CFFB2)

val AmberAccent = NeonOrange
val BlueAccent = NeonCyan
val RedAccent = NeonRed
val VioletAccent = NeonViolet

val OnPrimaryInk = InkOnNeon

val SurfaceDark = BgDeep
val SurfaceDark2 = CardNavy
val SurfaceDark3 = CardNavyHigh
val SurfaceDark4 = Color(0xFF28323B)
val OutlineDark = StrokeNavy
val OnSurfaceDark = TextHi
val OnSurfaceDarkVariant = TextMid

val SurfaceLight = Color(0xFFF4F6F8)
val SurfaceLight2 = Color(0xFFFFFFFF)
val SurfaceLight3 = Color(0xFFE9EDF1)
val OutlineLight = Color(0xFFD3DAE1)
val OnSurfaceLight = Color(0xFF11171D)
val OnSurfaceLightVariant = Color(0xFF5A6673)

val HeroGradientStart = Color(0xFF13203A)
val HeroGradientEnd = Color(0xFF0B1120)
val AccentGradientStart = NeonCyan
val AccentGradientEnd = NeonViolet
