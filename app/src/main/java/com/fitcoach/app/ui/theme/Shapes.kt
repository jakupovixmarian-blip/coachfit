package com.fitcoach.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* =============================================================================
 * FitCoach 2.0 — "Obsidian Meridian" · Shapes.kt
 * -----------------------------------------------------------------------------
 * Regola: nessun raggio sopra 22.dp sulle superfici. Solo bottom sheet e
 * dialog usano 28.dp. I bordi troppo arrotondati sono esplicitamente vietati
 * dalla direzione grafica.
 * ========================================================================== */

/** Raggi come costanti: usali per gli shape non coperti da MaterialTheme.shapes. */
object FitRadius {
    /** Chip interni, mini-badge. */
    val xs = 8.dp

    /** Campi compatti, indicatori. */
    val sm = 10.dp

    /** Tile metriche, campi di input, pulsanti. */
    val md = 14.dp

    /** Pannelli (SectionCard). */
    val lg = 18.dp

    /** Proposte AI, sheet inline, card in evidenza. */
    val xl = 22.dp

    /** Solo bottom sheet e dialog. */
    val sheet = 28.dp

    /** Pillole, tag, indicatori tondi. */
    val pill = CircleShape
}

/**
 * Shapes del tema. Va passato a MaterialTheme:
 *   MaterialTheme(colorScheme = ..., typography = FitTypography, shapes = FitShapes)
 */
val FitShapes = Shapes(
    extraSmall = RoundedCornerShape(FitRadius.xs),
    small = RoundedCornerShape(FitRadius.sm),
    medium = RoundedCornerShape(FitRadius.md),
    large = RoundedCornerShape(FitRadius.lg),
    extraLarge = RoundedCornerShape(FitRadius.xl),
)
