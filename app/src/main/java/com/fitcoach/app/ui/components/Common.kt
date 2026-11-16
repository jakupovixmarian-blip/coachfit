package com.fitcoach.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.theme.AiSoft
import com.fitcoach.app.ui.theme.BrandBorder
import com.fitcoach.app.ui.theme.BrandSoft
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.EmeraldLight
import com.fitcoach.app.ui.theme.FatColor
import com.fitcoach.app.ui.theme.FitAnthracite
import com.fitcoach.app.ui.theme.FitBlack
import com.fitcoach.app.ui.theme.FitDuration
import com.fitcoach.app.ui.theme.FitEasing
import com.fitcoach.app.ui.theme.FitMotion
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurface
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.MacroTrack
import com.fitcoach.app.ui.theme.ProteinColor
import com.fitcoach.app.ui.theme.SkeletonBase
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.rememberMotionEnabled

/* =============================================================================
 * FitCoach 2.0 — "Obsidian Meridian" · Common.kt
 * -----------------------------------------------------------------------------
 * Sostituisce integralmente ui/components/Common.kt.
 * Le firme sono quelle del contratto: i chiamanti esistenti restano validi.
 *   SectionCard / SectionHeader / StatTile / MacroBar / MacroLegend
 *   Pill / LoadingBox / EmptyState
 *
 * Nota su MacroLegend ed EmptyState: il contratto non riportava i parametri
 * esatti ("vedi file"). Ho usato la forma più probabile. Se il tuo file ha
 * parametri diversi, sostituisci SOLO la testa della funzione (le righe
 * `fun ...(...)`) e lascia il corpo: il resto del file non dipende da loro.
 * ========================================================================== */

/**
 * Pannello base. Superficie multilivello: gradiente verticale appena percepibile
 * da FitSurfaceVariant a FitSurface, bordo 1.dp, raggio 18.dp, padding 16.dp.
 * Nessun raggio oltre 22.dp: da direzione grafica.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(FitRadius.lg)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(FitSurfaceVariant, FitSurface),
                ),
            )
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .padding(16.dp),
    ) {
        content()
    }
}

/**
 * Intestazione di blocco: barretta di accento verticale + titolo Sora + sottotitolo.
 * Sostituisce il vecchio header con pallino: più gerarchico, stessa occupazione.
 */
@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 18.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(EmeraldLight, Emerald))),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FitTextStyles.h3,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Metrica singola: etichetta MAIUSCOLA, pallino di accento, valore in mono
 * tabulare colorato. Pensata per stare in griglie da 2 o 3 colonne.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    accent: Color = Emerald,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(FitRadius.md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(FitAnthracite)
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label.uppercase(),
                style = FitTextStyles.label,
                color = TextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = FitTextStyles.numericL,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Barra proporzionale dei macro sulle kcal reali: P*4 / C*4 / F*9.
 * Se i tre valori sono tutti a zero mostra solo la traccia: nessuna divisione
 * per zero, nessun segmento di larghezza 0 (Compose rifiuta weight(0f)).
 */
@Composable
fun MacroBar(
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double,
    modifier: Modifier = Modifier,
) {
    val proteinKcal = proteinGrams.coerceAtLeast(0.0) * 4.0
    val carbsKcal = carbsGrams.coerceAtLeast(0.0) * 4.0
    val fatKcal = fatGrams.coerceAtLeast(0.0) * 9.0
    val total = proteinKcal + carbsKcal + fatKcal

    val targetProtein = if (total > 0.0) (proteinKcal / total).toFloat() else 0f
    val targetCarbs = if (total > 0.0) (carbsKcal / total).toFloat() else 0f
    val targetFat = if (total > 0.0) (fatKcal / total).toFloat() else 0f

    val proteinFraction by animateFloatAsState(
        targetValue = targetProtein,
        animationSpec = FitMotion.standard(),
        label = "macroProtein",
    )
    val carbsFraction by animateFloatAsState(
        targetValue = targetCarbs,
        animationSpec = FitMotion.standard(),
        label = "macroCarbs",
    )
    val fatFraction by animateFloatAsState(
        targetValue = targetFat,
        animationSpec = FitMotion.standard(),
        label = "macroFat",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MacroTrack),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (proteinFraction > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(proteinFraction)
                    .background(ProteinColor),
            )
        }
        if (carbsFraction > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(carbsFraction)
                    .background(CarbsColor),
            )
        }
        if (fatFraction > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(fatFraction)
                    .background(FatColor),
            )
        }
    }
}

/**
 * Legenda dei macro con grammi. Stessi parametri di MacroBar, così i due
 * componenti si passano gli stessi valori senza ricalcoli.
 */
@Composable
fun MacroLegend(
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MacroLegendItem("P", proteinGrams, ProteinColor, Modifier.weight(1f))
        MacroLegendItem("C", carbsGrams, CarbsColor, Modifier.weight(1f))
        MacroLegendItem("G", fatGrams, FatColor, Modifier.weight(1f))
    }
}

@Composable
private fun MacroLegendItem(
    letter: String,
    grams: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = letter,
            style = FitTextStyles.label,
            color = TextLow,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "${grams.toInt()} g",
            style = FitTextStyles.numericS,
            color = TextPrimary,
            maxLines = 1,
        )
    }
}

/** Etichetta di stato: giornata HIGH/MEDIUM/LOW, "AI", "PROPOSTA", ecc. */
@Composable
fun Pill(text: String, color: Color = Emerald) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(width = 1.dp, color = color.copy(alpha = 0.42f), shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = FitTextStyles.label,
            color = color,
            maxLines = 1,
        )
    }
}

/**
 * Segnaposto di caricamento: blocco che respira.
 * Se il sistema ha le animazioni disattivate resta statico (nessun loop).
 */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    val motionEnabled = rememberMotionEnabled()
    val transition = rememberInfiniteTransition(label = "loading")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FitDuration.Skeleton, easing = FitEasing.Standard),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loadingPulse",
    )
    val alpha = if (motionEnabled) pulse else 0.6f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(FitRadius.lg))
            .background(SkeletonBase.copy(alpha = alpha)),
    )
}

/**
 * Stato vuoto: titolo, messaggio e azione opzionale.
 * Non è una schermata statica: se c'è un'azione viene eseguita davvero.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(FitRadius.lg)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FitAnthracite.copy(alpha = 0.55f))
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AiSoft)
                .border(width = 1.dp, color = BrandBorder, shape = CircleShape),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = FitTextStyles.h3,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = FitTextStyles.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(10.dp))
            action()
        } else if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = FitTextStyles.bodyStrong,
                    color = Emerald,
                )
            }
        }
    }
}

/**
 * Fondo pagina standard: FitBlack + alone luminoso in alto (controllato, non neon).
 * Usalo come primo Modifier delle schermate che oggi usano un semplice background.
 */
@Composable
fun fitScreenBackground(): Brush = Brush.verticalGradient(
    colors = listOf(BrandSoft, FitSurface, FitBlack),
    startY = 0f,
    endY = 1400f,
)

// Import usati solo dal gradiente di pagina.
private val UnusedBrandSoftRef = BrandSoft
