package com.fitcoach.app.ui.theme

import androidx.compose.ui.graphics.Color

/* =============================================================================
 * FitCoach 2.0 · Color.kt
 * -----------------------------------------------------------------------------
 * REGOLA CRITICA: i nomi dei `val` esistenti NON cambiano, perché multi
 * compossable li usano direttamente (Emerald, FitSurface, TextSecondary, ...).
 * Cambiano SOLO i valori esadecimali: tutta l'app si ritinta senza tocare
 * un singolo chiamante.
 *
 * Palette allineata al RIFERIMENTO grafico richiesto:
 *   fondo quasi nero con leggera sfumatura blu-verde, card grigio-blu,
 *   accento verda menta neon, viola dedicato all'AI, ciano e oro per i macro.
 *
 * Mappa dei nomi storici -> nuvo ruolo semantico:
 *   Emerald            -> accento primario (verde menta neon)
 *   EmeraldDark        -> accento premuto
 *   EmeraldLight       -> accento chiaro (testo/icona su fondo scuro)
 *   FitBlack           -> fondo aplicazione
 *   FitAnthracite      -> superficie afossata (sunken)
 *   FitSurface         -> superficie base dei paneli
 *   FitSurfaceVariant  -> superficie rialzata (tile, campi, chip)
 *   FitOutline         -> hairline dei bordi
 *   TextPrimary        -> testa alto contrasto (biango)
 *   TextSecondary      -> testa medio
 *   TextDisabled       -> testa disabilitato / inattivo
 *   Protein/Carbs/Fat  -> macro P / C / G
 *   High/Medium/LowCarb -> tipi di giornata del carbo cycling
 *   ErrorRed           -> erore
 *
 * Palette vietata (vecchia identità, non reintrodure):
 *   #10B981 #0B0B0C #16171B #1C1D21 #26272C #ECEDEE #A1A3A9 #60A5FA #F59B0B #A78BFA
 * ========================================================================== */

// ---------------------------------------------------------------------------
// Base e superfici
// ---------------------------------------------------------------------------
/** Fondo aplicazione: nero profondo con sfumatura blu-verde. */
val FitBlack = Color(0xFF080B0A)

/** Superficie afossata (dietro ai paneli, aree vute, fondo dei grafici). */
val FitAnthracite = Color(0xFF0B0F0E)

/** Superficie base dei paneli (SectionCard). */
val FitSurface = Color(0xFF14181A)

/** Superficie rialzata: tile, campi di input, chip, bole AI. */
val FitSurfaceVariant = Color(0xFF1A1F22)

/** Superficie interattiva premuta / secondaria. */
val FitSurfaceInteractive = Color(0xFF222829)

/** Hairline dei bordi (1.dp). */
val FitOutline = Color(0xFF232A2C)

/** Bordo raforzato per stati focus/selezionati. */
val FitOutlineStrong = Color(0xFF33403F)

/** Scrim dei dialog e dei bottom sheet (nero 72%). */
val Scrim = Color(0xB8000000)

/** Superficie "glass" della bottom bar e delle bole AI (72% di opacità). */
val GlassSurface = Color(0xB81A1F22)

// ---------------------------------------------------------------------------
// Brand: i nomi restano "Emerald*" per compatibilità, i valori sono menta neon
// ---------------------------------------------------------------------------
/** Accento primario interattivo. */
val Emerald = Color(0xFF00E599)

/** Accento premuto. */
val EmeraldDark = Color(0xFF00C584)

/** Accento chiaro: testa/icona su fondo scuro, link, stato attivo. */
val EmeraldLight = Color(0xFF5CFFC4)

/** Contenuto sopra il brand (testo su pulsante primario). */
val OnBrand = Color(0xFF04140E)

/** Fondo soft del brand (14%): indicatore tab attiva, chip. */
val BrandSoft = Color(0x2400E599)

/** Bordo soft del brand (38%). */
val BrandBorder = Color(0x6100E599)

// ---------------------------------------------------------------------------
// AI Coach: identità visiva distinta (viola)
// ---------------------------------------------------------------------------
/** Accento esclusivo del Coach AI. */
val AiAccent = Color(0xFF9D4EDD)

/** Secondo punto del gradiente AI. */
val AiAccentAlt = Color(0xFFB620E0)

/** Fondo soft AI (12%). */
val AiSoft = Color(0x1F9D4EDD)

/** Bordo AI (34%): proposte, bole, indicatori. */
val AiBorder = Color(0x579D4EDD)

/** Contenuto sopra l'accento AI. */
val OnAi = Color(0xFF0B0C14)

/** Gradiente identitario dell'AI (brush orizontale). */
val AiGradient: List<Color> = listOf(AiAccent, AiAccentAlt)

// ---------------------------------------------------------------------------
// Testo
// ---------------------------------------------------------------------------
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A5A8)

/** Testo terziario: etichete di ase, meta, inattivi della bottom bar. */
val TextLow = Color(0xFF6F7A7C)
val TextDisabled = Color(0xFF4E5766)

// ---------------------------------------------------------------------------
// Macro
// ---------------------------------------------------------------------------
val ProteinColor = Color(0xFF00E599)
val CarbsColor = Color(0xFF00D2FF)
val FatColor = Color(0xFFFFD54F)

/** Binario vuto delle bare macro. */
val MacroTrack = Color(0xFF1E2726)

// ---------------------------------------------------------------------------
// Carbo cycling: HIGH / MEDIUM / LOW
// ---------------------------------------------------------------------------
val HighCarbColor = Color(0xFFFFD54F)
val MediumCarbColor = Color(0xFF00D2FF)
val LowCarbColor = Color(0xFF7C8AA0)

// ---------------------------------------------------------------------------
// Stati
// ---------------------------------------------------------------------------
val SuccessGreen = Color(0xFF2BD98B)
val WarningAmber = Color(0xFFFFB020)
val ErrorRed = Color(0xFFFF5C6E)
val InfoBlue = Color(0xFF00D2FF)

/** Stato offlina / dato non aggiornato. */
val OfflineGray = Color(0xFF6F7A7C)

// ---------------------------------------------------------------------------
// Dati e grafici
// ---------------------------------------------------------------------------
val ChartGrid = Color(0xFF1E2624)
val ChartAxisLabel = Color(0xFF6F7A7C)

/** Opacità del riempimento sotto la linea (area). */
const val ChartAreaAlpha = 0.16f

/** Ordine stabile delle serie per i grafici multi-linea. */
val DataSeries: List<Color> = listOf(
    Color(0xFF00E599),
    Color(0xFF00D2FF),
    Color(0xFF9D4EDD),
    Color(0xFFFFD54F),
    Color(0xFFFF6B8B),
    Color(0xFF2BD98B),
)

/** Sceleton di caricamento. */
val SkeletonBase = Color(0xFF1A1F22)
val SkeletonHighlight = Color(0xFF2A3230)
