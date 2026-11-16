package com.fitcoach.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/* =============================================================================
 * FitCoach 2.0 — "Obsidian Meridian" · Theme.kt
 * -----------------------------------------------------------------------------
 * Dark-only, come da vincolo: `darkTheme` resta nella firma per compatibilità
 * con i chiamanti esistenti ma è sempre true. Nessuna variante light.
 *
 * ATTENZIONE all'integrazione: se il tuo Theme.kt attuale contiene altro
 * (ripple custom, LocalContentColor, gestione degli insets, CompositionLocal
 * tuoi), NON sovrascrivere il file: copia dentro il tuo solo il
 * `FitDarkColorScheme` qui sotto e aggiungi `shapes = FitShapes` e
 * `typography = FitTypography` alla chiamata MaterialTheme che hai già.
 * ========================================================================== */

private val FitDarkColorScheme = darkColorScheme(
    // Brand
    primary = Emerald,
    onPrimary = OnBrand,
    primaryContainer = BrandSoft,
    onPrimaryContainer = EmeraldLight,
    inversePrimary = EmeraldDark,

    // AI Coach (identità distinta dal resto dell'app)
    secondary = AiAccent,
    onSecondary = OnAi,
    secondaryContainer = AiSoft,
    onSecondaryContainer = AiAccent,
    tertiary = FatColor,
    onTertiary = OnBrand,
    tertiaryContainer = MacroTrack,
    onTertiaryContainer = FatColor,

    // Fondi
    background = FitBlack,
    onBackground = TextPrimary,
    surface = FitSurface,
    onSurface = TextPrimary,
    surfaceVariant = FitSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Emerald,

    // Livelli di superficie (Material3 1.3.x)
    surfaceContainerLowest = FitBlack,
    surfaceContainerLow = FitAnthracite,
    surfaceContainer = FitSurface,
    surfaceContainerHigh = FitSurfaceVariant,
    surfaceContainerHighest = FitSurfaceInteractive,

    // Bordi
    outline = FitOutline,
    outlineVariant = FitOutlineStrong,

    // Errori
    error = ErrorRed,
    onError = OnBrand,
    errorContainer = MacroTrack,
    onErrorContainer = ErrorRed,

    // Altro
    scrim = Scrim,
    inverseSurface = TextPrimary,
    inverseOnSurface = FitBlack,
)

/**
 * Tema unico dell'app. Firma invariata: `FitCoachTheme(darkTheme = true, content)`.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun FitCoachTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FitDarkColorScheme,
        typography = FitTypography,
        shapes = FitShapes,
        content = content,
    )
}
