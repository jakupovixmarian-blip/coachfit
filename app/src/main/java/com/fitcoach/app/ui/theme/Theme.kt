package com.fitcoach.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* =============================================================================
 * FitCoach 2.0 — identita "Hyperion" · Theme.kt
 * -----------------------------------------------------------------------------
 * Dark-only, come da vincolo: `darkTheme` resta nella firma per compatibilità
 * con i chiamanti esistenti ma è sempre true. Nessuna variante light.
 *
 * Identita Hyperion: ciano/blu = brand e azione, viola/magenta = AI,
 * verde = progresso. Le superfici non sono mai piatte (vedi HyperionBackground).
 *
 * ATTENZIONE all'integrazione: se il tuo Theme.kt attuale contiene altro
 * (ripple custom, LocalContentColor, gestione degli insets, CompositionLocal
 * tuoi), NON sovrascrivere il file: copia dentro il tuo solo il
 * `FitDarkColorScheme` qui sotto e aggiungi `shapes = FitShapes` e
 * `typography = FitTypography` alla chiamata MaterialTheme che hai già.
 * ========================================================================== */

private val FitDarkColorScheme = darkColorScheme(
    // Brand (ciano/blu)
    primary = Emerald,
    onPrimary = OnBrand,
    primaryContainer = BrandSoft,
    onPrimaryContainer = EmeraldLight,
    inversePrimary = EmeraldDark,

    // AI Coach (identità distinta: viola/magenta)
    secondary = AiAccent,
    onSecondary = OnAi,
    secondaryContainer = AiSoft,
    onSecondaryContainer = AiAccent,
    tertiary = AiAccentAlt,
    onTertiary = OnAi,
    tertiaryContainer = AiSoft,
    onTertiaryContainer = AiAccentAlt,

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

/* =============================================================================
 * Sfondo ambientale "aura"
 * -----------------------------------------------------------------------------
 * Hyperion vieta il nero piatto: il fondo e un gradiente lineare con tre aure
 * radiali (ciano in alto a sinistra, viola a destra a meta, verde in basso a
 * sinistra). E cio che impedisce al nero di sembrare piatto. Va disegnato
 * UNA volta dietro al contenuto dell'app (MainActivity), non in ogni schermata.
 * ========================================================================== */
@Composable
fun HyperionBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(FitAnthracite, FitBlack, Color(0xFF05080F)),
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun aura(color: Color, alpha: Float, cx: Float, cy: Float, r: Float) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                        center = Offset(w * cx, h * cy),
                        radius = maxOf(w, h) * r,
                    ),
                    size = size,
                )
            }
            aura(Emerald, 0.16f, 0.18f, -0.06f, 0.70f)
            aura(AiAccent, 0.15f, 0.96f, 0.44f, 0.62f)
            aura(SuccessGreen, 0.09f, 0.08f, 0.98f, 0.55f)
        }
        content()
    }
}
