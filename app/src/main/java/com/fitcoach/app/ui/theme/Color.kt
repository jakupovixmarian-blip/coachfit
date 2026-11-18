package com.fitcoach.app.ui.theme

import androidx.compose.ui.graphics.Color

/* =============================================================================
 * FitCoach 2.0 · Color.kt — identita "Hyperion"
 * -----------------------------------------------------------------------------
 * REGOLA CRITICA: i nomi dei `val` esistenti NON cambiano, perche molti
 * composable li usano direttamente (Emerald, FitSurface, TextSecondary, ...).
 * Cambiano SOLO i valori esadecimali: tutta l'app si ritinta senza toccare
 * un singolo chiamante.
 *
 * Fonte unica dei valori: consegna grafica Hyperion (design/tokens.json).
 * Hyperion sostituisce integralmente la precedente identita "Obsidian
 * Meridian" (smeraldo #00E599).
 *
 * Direzione: fitness intelligence. Superfici profonde a piu livelli, luce
 * controllata (ciano neon -> blu elettrico), viola/magenta riservati all'AI,
 * verde riservato ai progressi. Nessuna superficie piatta.
 *
 * Un solo significato per famiglia cromatica:
 *   ciano/blu     -> brand e azione
 *   viola/magenta -> AI e analisi
 *   verde         -> progresso raggiunto
 *   giallo        -> attenzione
 *   rosso/arancio -> errore o dolore
 *
 * Mappa dei nomi storici -> ruolo semantico Hyperion:
 *   Emerald               -> brand.cyan (azione primaria, tab attivo, ring kcal)
 *   EmeraldDark           -> brand.cyanPressed
 *   EmeraldLight          -> ciano chiaro (testo/icona su fondo scuro)
 *   FitBlack              -> base.abyss (fondo pagina)
 *   FitAnthracite         -> base.night (secondo strato di sfondo)
 *   FitSurface            -> base.surface0 (pannello base)
 *   FitSurfaceVariant     -> base.surface1 (pannello elevato, campi)
 *   FitSurfaceInteractive -> base.surface2 (elemento interattivo, chip)
 *   FitOutline            -> line.hairline (bianco 7%)
 *   FitOutlineStrong      -> line.strong (bianco 14%)
 *   TextPrimary           -> text.high
 *   TextSecondary         -> text.mid
 *   TextLow               -> text.low
 *   TextDisabled          -> text.disabled
 *   Protein/Carbs/Fat     -> macro.protein / macro.carbs / macro.fat
 *   High/Medium/LowCarb   -> tipi di giornata del carb cycling
 *   ErrorRed              -> state.danger
 *
 * Palette vietata (vecchia identita Obsidian Meridian, non reintrodurre):
 *   #00E599 #00C584 #5CFFC4 #00D2FF #9D4EDD #FFD54F #080B0A #14181A #1A1F22
 * ========================================================================== */

// ---------------------------------------------------------------------------
// Base e superfici (profondita a piu livelli)
// ---------------------------------------------------------------------------
/** Fondo pagina: abisso blu-nero. */
val FitBlack = Color(0xFF04060B)

/** Secondo strato del gradiente di sfondo. */
val FitAnthracite = Color(0xFF070C18)

/** Superficie base dei pannelli. */
val FitSurface = Color(0xFF0A1120)

/** Superficie elevata: pannelli rialzati, campi di input. */
val FitSurfaceVariant = Color(0xFF0E1728)

/** Superficie interattiva: chip, elementi premuti. */
val FitSurfaceInteractive = Color(0xFF142034)

/** Tracce di barre e ring, divisori forti. */
val FitSurfaceTrack = Color(0xFF1B2A44)

/** Hairline dei bordi (bianco 7%). */
val FitOutline = Color(0x12FFFFFF)

/** Bordo rinforzato per stati focus/selezionati (bianco 14%). */
val FitOutlineStrong = Color(0x24FFFFFF)

/** Scrim dei dialog e dei bottom sheet (abisso 72%). */
val Scrim = Color(0xB804060B)

/** Superficie "glass" della bottom bar e delle bolle AI (72% di opacita). */
val GlassSurface = Color(0xB80E1728)

// ---------------------------------------------------------------------------
// Brand: i nomi restano "Emerald*" per compatibilita, i valori sono ciano/blu
// ---------------------------------------------------------------------------
/** Accento primario interattivo (brand.cyan). */
val Emerald = Color(0xFF22E0FF)

/** Accento premuto (brand.cyanPressed). */
val EmeraldDark = Color(0xFF12C4E0)

/** Accento chiaro: testo/icona su fondo scuro, link, stato attivo. */
val EmeraldLight = Color(0xFF7DEBFF)

/** Fine del gradiente primario (brand.blue). */
val BrandBlue = Color(0xFF2F6BFF)

/** Profondita del gradiente (brand.blueDeep). */
val BrandBlueDeep = Color(0xFF1B3FCC)

/** Contenuto sopra il brand (testo su pulsante primario). */
val OnBrand = Color(0xFF03121A)

/** Fondo soft del brand (12%): indicatore tab attiva, chip. */
val BrandSoft = Color(0x1F22E0FF)

/** Bordo soft del brand (38%). */
val BrandBorder = Color(0x6122E0FF)

/** Gradiente identitario del brand (ciano -> blu). */
val BrandGradient: List<Color> = listOf(Emerald, BrandBlue)

// ---------------------------------------------------------------------------
// AI Coach: identita visiva distinta (viola/magenta)
// ---------------------------------------------------------------------------
/** Accento esclusivo del Coach AI (ai.violet). */
val AiAccent = Color(0xFF8B5CF6)

/** Secondo punto del gradiente AI (ai.magenta). */
val AiAccentAlt = Color(0xFFE23BB0)

/** Fondo soft AI (14%). */
val AiSoft = Color(0x248B5CF6)

/** Bordo AI (42%): proposte, bolle, indicatori. */
val AiBorder = Color(0x6B8B5CF6)

/** Contenuto sopra l'accento AI. */
val OnAi = Color(0xFF0B0C14)

/** Gradiente identitario dell'AI (viola -> magenta). */
val AiGradient: List<Color> = listOf(AiAccent, AiAccentAlt)

// ---------------------------------------------------------------------------
// Testo
// ---------------------------------------------------------------------------
val TextPrimary = Color(0xFFEAF2FF)
val TextSecondary = Color(0xFFA8B8D0)

/** Testo terziario: etichette di asse, meta, inattivi della bottom bar. */
val TextLow = Color(0xFF6C7E9A)
val TextDisabled = Color(0xFF48586F)

// ---------------------------------------------------------------------------
// Macro
// ---------------------------------------------------------------------------
val ProteinColor = Color(0xFF2BE08B)
val CarbsColor = Color(0xFF22E0FF)
val FatColor = Color(0xFF8B5CF6)

/** Binario vuoto delle barre macro. */
val MacroTrack = Color(0xFF1B2A44)

// ---------------------------------------------------------------------------
// Carbo cycling: HIGH / MEDIUM / LOW (famiglia dei carboidrati: ciano/blu)
// ---------------------------------------------------------------------------
val HighCarbColor = Color(0xFF22E0FF)
val MediumCarbColor = Color(0xFF2F6BFF)
val LowCarbColor = Color(0xFF6C7E9A)

// ---------------------------------------------------------------------------
// Stati
// ---------------------------------------------------------------------------
val SuccessGreen = Color(0xFF2BE08B)
val WarningAmber = Color(0xFFFFB020)
val ErrorRed = Color(0xFFFF4D6D)
val InfoBlue = Color(0xFF22E0FF)

/** Dolore segnalato in allenamento. */
val PainOrange = Color(0xFFFF6B3D)

/** Stato offline / dato non aggiornato. */
val OfflineGray = Color(0xFF6C7E9A)

// ---------------------------------------------------------------------------
// Dati e grafici
// ---------------------------------------------------------------------------
val ChartGrid = Color(0xFF16233A)
val ChartAxisLabel = Color(0xFF6C7E9A)

/** Opacita del riempimento sotto la linea (area). */
const val ChartAreaAlpha = 0.18f

/** Ordine stabile delle serie per i grafici multi-linea. */
val DataSeries: List<Color> = listOf(
    Color(0xFF22E0FF),
    Color(0xFF2F6BFF),
    Color(0xFF8B5CF6),
    Color(0xFFE23BB0),
    Color(0xFF2BE08B),
    Color(0xFFFFB020),
)

/** Gradiente dei progressi (verde -> ciano). */
val ProgressGradient: List<Color> = listOf(SuccessGreen, Emerald)

/** Sfondo del blocco hero (tre strati). */
val HeroSurfaceGradient: List<Color> = listOf(
    Color(0xFF101D33),
    Color(0xFF0B1220),
    Color(0xFF070C18),
)

/** Skeleton di caricamento. */
val SkeletonBase = Color(0xFF101A2C)
val SkeletonHighlight = Color(0xFF1B2A44)
