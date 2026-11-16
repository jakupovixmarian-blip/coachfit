package com.fitcoach.app.ui.theme

import androidx.compose.ui.graphics.Color

/* =============================================================================
 * FitCoach 2.0 — "Obsidian Meridian" · Color.kt
 * -----------------------------------------------------------------------------
 * REGOLA CRITICA: i nomi dei `val` esistenti NON cambiano, perché molti
 * composable li usano direttamente (Emerald, FitSurface, TextSecondary, ...).
 * Cambiano SOLO i valori esadecimali: tutta l'app si ritinta senza toccare
 * un singolo chiamante.
 *
 * Mappa dei nomi storici -> nuovo ruolo semantico:
 *   Emerald            -> brand interattivo (Ion)
 *   EmeraldDark        -> brand premuto
 *   EmeraldLight       -> brand chiaro (testo/icona su fondo scuro)
 *   FitBlack           -> fondo applicazione
 *   FitAnthracite      -> superficie affossata (sunken)
 *   FitSurface         -> superficie base dei pannelli
 *   FitSurfaceVariant  -> superficie rialzata (tile, campi, chip)
 *   FitOutline         -> hairline dei bordi
 *   TextPrimary        -> testo alto contrasto
 *   TextSecondary      -> testo medio
 *   TextDisabled       -> testo disabilitato / inattivo
 *   Protein/Carbs/Fat  -> macro P / C / G
 *   High/Medium/LowCarb-> tipi di giornata del carb cycling
 *   ErrorRed           -> errore
 *
 * Palette vietata (vecchia identità, non reintrodurre):
 *   #10B981 #0B0B0C #16171B #1C1D21 #26272C #ECEDEE #A1A3A9 #60A5FA #F59E0B #A78BFA
 * ========================================================================== */

// ---------------------------------------------------------------------------
// Base e superfici
// ---------------------------------------------------------------------------
/** Fondo applicazione. */
val FitBlack = Color(0xFF07080B)

/** Superficie affossata (dietro ai pannelli, aree vuote, fondo dei grafici). */
val FitAnthracite = Color(0xFF0A0C11)

/** Superficie base dei pannelli (SectionCard). */
val FitSurface = Color(0xFF0E1117)

/** Superficie rialzata: tile, campi di input, chip, bolle AI. */
val FitSurfaceVariant = Color(0xFF151A23)

/** Superficie interattiva premuta / secondaria. */
val FitSurfaceInteractive = Color(0xFF1D2330)

/** Hairline dei bordi (1.dp). */
val FitOutline = Color(0xFF232A38)

/** Bordo rafforzato per stati focus/selezionati. */
val FitOutlineStrong = Color(0xFF33405A)

/** Scrim dei dialog e dei bottom sheet (nero 72%). */
val Scrim = Color(0xB8000000)

/** Superficie "glass" della bottom bar e delle bolle AI (72% di opacità). */
val GlassSurface = Color(0xB8151A23)

// ---------------------------------------------------------------------------
// Brand — i nomi restano "Emerald*" per compatibilità, i valori sono Ion
// ---------------------------------------------------------------------------
/** Accento primario interattivo. */
val Emerald = Color(0xFF6E7BFF)

/** Accento premuto. */
val EmeraldDark = Color(0xFF5A67F0)

/** Accento chiaro: testo/icona su fondo scuro, link, stato attivo. */
val EmeraldLight = Color(0xFF9AA3FF)

/** Contenuto sopra il brand (testo su pulsante primario). */
val OnBrand = Color(0xFF0B0C14)

/** Fondo soft del brand (14%): indicatore tab attiva, chip. */
val BrandSoft = Color(0x246E7BFF)

/** Bordo soft del brand (38%). */
val BrandBorder = Color(0x616E7BFF)

// ---------------------------------------------------------------------------
// AI Coach — identità visiva distinta (magenta -> Ion)
// ---------------------------------------------------------------------------
/** Accento esclusivo del Coach AI. */
val AiAccent = Color(0xFFC86BFF)

/** Secondo punto del gradiente AI. */
val AiAccentAlt = Color(0xFF6E7BFF)

/** Fondo soft AI (12%). */
val AiSoft = Color(0x1FC86BFF)

/** Bordo AI (34%): proposte, bolle, indicatori. */
val AiBorder = Color(0x57C86BFF)

/** Contenuto sopra l'accento AI. */
val OnAi = Color(0xFF0B0C14)

/** Gradiente identitario dell'AI (brush orizzontale). */
val AiGradient: List<Color> = listOf(AiAccent, AiAccentAlt)

// ---------------------------------------------------------------------------
// Testo
// ---------------------------------------------------------------------------
val TextPrimary = Color(0xFFF4F6FA)
val TextSecondary = Color(0xFFAAB3C5)

/** Testo terziario: etichette di asse, meta, inattivi della bottom bar. */
val TextLow = Color(0xFF78829A)
val TextDisabled = Color(0xFF5A6376)

// ---------------------------------------------------------------------------
// Macro
// ---------------------------------------------------------------------------
val ProteinColor = Color(0xFFFF6B8B)
val CarbsColor = Color(0xFFFFC24B)
val FatColor = Color(0xFF5BC8D8)

/** Binario vuoto delle barre macro. */
val MacroTrack = Color(0xFF1D2330)

// ---------------------------------------------------------------------------
// Carb cycling — HIGH / MEDIUM / LOW
// ---------------------------------------------------------------------------
val HighCarbColor = Color(0xFFFFC24B)
val MediumCarbColor = Color(0xFF6E7BFF)
val LowCarbColor = Color(0xFF5BC8D8)

// ---------------------------------------------------------------------------
// Stati
// ---------------------------------------------------------------------------
val SuccessGreen = Color(0xFF45C89B)
val WarningAmber = Color(0xFFFFB020)
val ErrorRed = Color(0xFFFF5C6E)
val InfoBlue = Color(0xFF6E7BFF)

/** Stato offline / dato non aggiornato. */
val OfflineGray = Color(0xFF78829A)

// ---------------------------------------------------------------------------
// Dati e grafici
// ---------------------------------------------------------------------------
val ChartGrid = Color(0xFF1B2130)
val ChartAxisLabel = Color(0xFF78829A)

/** Opacità del riempimento sotto la linea (area). */
const val ChartAreaAlpha = 0.16f

/** Ordine stabile delle serie per i grafici multi-linea. */
val DataSeries: List<Color> = listOf(
    Color(0xFF6E7BFF),
    Color(0xFF5BC8D8),
    Color(0xFFC86BFF),
    Color(0xFFFFC24B),
    Color(0xFFFF6B8B),
    Color(0xFF45C89B),
)

/** Skeleton di caricamento. */
val SkeletonBase = Color(0xFF151A23)
val SkeletonHighlight = Color(0xFF222939)
