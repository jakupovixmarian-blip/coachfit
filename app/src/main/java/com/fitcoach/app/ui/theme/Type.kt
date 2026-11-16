package com.fitcoach.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fitcoach.app.R

/* =============================================================================
 * FitCoach 2.0 — "Obsidian Meridian" · Type.kt
 * -----------------------------------------------------------------------------
 * Tre famiglie, tutte incluse nell'APK (offline-first, nessun downloadable font):
 *   Sora            -> display, titoli, numeri di sezione
 *   Inter           -> testo corrente
 *   JetBrains Mono  -> TUTTI i numeri di dati (tabular, allineati in colonna)
 *
 * Come sono dichiarate:
 *   res/font/sora.xml           -> famiglia che espone i pesi dal TTF variabile
 *   res/font/inter.xml          -> idem
 *   res/font/jetbrains_mono.xml -> mappa i 4 TTF statici sui pesi
 * Vedi android/FONTS.md per il download e per il piano B.
 *
 * Regola: nessun testo sotto 12.sp. Il corpo è 15.sp, il secondario 13.sp.
 * ========================================================================== */

/** Sora — titoli e display. */
val SoraFamily = FontFamily(Font(R.font.sora))

/** Inter — testo corrente. */
val InterFamily = FontFamily(Font(R.font.inter))

/** JetBrains Mono — numeri e dati (cifre tabulari). */
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/**
 * Stili Material3 rimappati sulla scala Obsidian Meridian.
 * `labelLarge` è pensato per le etichette MAIUSCOLE (12.sp, tracking 0.9):
 * applica `.uppercase()` nel composable quando serve.
 */
val FitTypography = Typography(
    // --- Display: numeri chiave e titoli di schermata ---------------------
    displayLarge = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),

    // --- Headline: intestazioni di sezione --------------------------------
    headlineLarge = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),

    // --- Title: titoli di pannello e voci di lista -----------------------
    titleLarge = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),

    // --- Body ------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),

    // --- Label -----------------------------------------------------------
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.9.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
)

/**
 * Stili fuori dai 15 slot Material3: display numerici e testi che non hanno
 * un equivalente. Usali direttamente, es. `Text("1 640", style = FitTextStyles.numericXL)`.
 */
object FitTextStyles {
    /** Titolo di schermata (34/40). */
    val display = FitTypography.displayLarge

    /** Intestazione di sezione maggiore (28/34). */
    val h1 = FitTypography.headlineLarge

    /** Intestazione di blocco (22/28). */
    val h2 = FitTypography.headlineMedium

    /** Titolo di pannello (18/24). */
    val h3 = FitTypography.headlineSmall

    /** Corpo standard (15/22). */
    val body = FitTypography.bodyLarge

    /** Corpo enfatizzato (15/22, 600). */
    val bodyStrong = FitTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)

    /** Corpo secondario (13/19). */
    val bodySmall = FitTypography.bodyMedium

    /** Etichetta MAIUSCOLA: usala già in uppercase, non ruotarla nel layout. */
    val label = FitTypography.labelLarge

    /** Didascalia / meta (12/16). */
    val caption = FitTypography.bodySmall

    /** Numero protagonista (32/36, mono tabulare). */
    val numericXL = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    )

    /** Numero di tile (24/28). */
    val numericL = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    )

    /** Numero in riga (17/22). */
    val numericM = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    )

    /** Numero piccolo: assi dei grafici, grammi, serie (13/18). */
    val numericS = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    )
}
