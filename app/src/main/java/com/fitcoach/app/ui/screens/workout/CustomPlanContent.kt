package com.fitcoach.app.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.StatTile
import com.fitcoach.app.ui.components.TrendChart
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FatColor
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.InfoBlue
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* ============================================================================
 * Scheda personalizzata / dettaglio giorno (design/screens.md §6).
 * Sola presentazione: serie, ripetizioni, RIR e recupero arrivano da
 * PlannedExercise. Nessuna immagine inventata: se l'illustrazione tecnica non
 * è disponibile si mostra un placeholder geometrico neutro.
 * ========================================================================== */

@Composable
fun CustomPlanContent(
    state: CustomPlanUiState,
    onBack: () -> Unit,
    onStartSession: () -> Unit,
    onSubstitute: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlanHeader(
            dayName = state.dayName,
            weekLabel = state.weekLabel,
            splitLabel = state.splitLabel,
            onBack = onBack,
        )

        state.mesocycleWarning?.let { warning ->
            PlanNoticePanel(text = warning, accent = WarningAmber)
        }

        if (state.isRestDay) {
            PlanNoticePanel(
                text = state.restHint.ifBlank { "Giorno di recupero: nessun allenamento programmato." },
                accent = InfoBlue,
            )
        } else if (state.isEmpty) {
            PlanNoticePanel(text = "Nessun esercizio in questa giornata.", accent = TextLow)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Esercizi",
                    value = "${state.meta.exercises}",
                    accent = Emerald,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Serie",
                    value = "${state.meta.totalSets}",
                    accent = InfoBlue,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Volume",
                    value = state.meta.volumeLabel,
                    accent = CarbsColor,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Durata",
                    value = state.meta.durationLabel,
                    accent = FatColor,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionHeader(
                title = "Esercizi",
                subtitle = "Serie, ripetizioni, RIR e recupero previsti",
            )

            state.exercises.forEach { exercise ->
                PlanExerciseRow(
                    exercise = exercise,
                    expanded = expandedIndex == exercise.index,
                    onToggle = {
                        expandedIndex = if (expandedIndex == exercise.index) null else exercise.index
                    },
                    onSubstitute = { onSubstitute(exercise.index) },
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            PrimaryButton(text = "Avvia questo allenamento", onClick = onStartSession)
        }
    }
}

@Composable
private fun PlanHeader(
    dayName: String,
    weekLabel: String,
    splitLabel: String,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(FitRadius.sm))
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(text = "‹", style = FitTextStyles.bodyStrong, color = TextSecondary)
            }
            Text(text = "Scheda", style = FitTextStyles.label, color = TextLow)
        }
        Text(text = dayName, style = FitTextStyles.display, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (weekLabel.isNotBlank()) Pill(text = weekLabel, color = Emerald)
            if (splitLabel.isNotBlank()) Pill(text = splitLabel, color = InfoBlue)
        }
    }
}

@Composable
private fun PlanExerciseRow(
    exercise: PlanExerciseUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSubstitute: () -> Unit,
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = planIndexLabel(exercise.index),
                    style = FitTextStyles.numericS,
                    color = if (expanded) Emerald else TextLow,
                    modifier = Modifier.width(30.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = exercise.name, style = FitTextStyles.bodyStrong, color = TextPrimary)
                    if (exercise.muscleLine.isNotBlank()) {
                        Text(
                            text = exercise.muscleLine,
                            style = FitTextStyles.caption,
                            color = TextSecondary,
                        )
                    }
                    Text(
                        text = "${exercise.prescription} · RIR ${exercise.rirLabel} · rec ${exercise.restLabel}",
                        style = FitTextStyles.numericS,
                        color = TextSecondary,
                    )
                }
                exercise.loadLabel?.let { load ->
                    Text(text = load, style = FitTextStyles.numericS, color = Emerald)
                }
            }

            if (expanded) {
                PlanIllustration(equipment = exercise.equipment)

                if (exercise.description.isNotBlank()) {
                    Text(
                        text = exercise.description,
                        style = FitTextStyles.bodySmall,
                        color = TextSecondary,
                    )
                }

                if (exercise.alternatives.isNotEmpty()) {
                    Text(text = "Alternative", style = FitTextStyles.label, color = TextLow)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        exercise.alternatives.forEach { alternative ->
                            Pill(text = alternative, color = InfoBlue)
                        }
                    }
                }

                if (exercise.hasHistory) {
                    Text(text = "Storico carichi", style = FitTextStyles.label, color = TextLow)
                    TrendChart(values = exercise.history, labels = emptyList())
                } else {
                    Text(
                        text = "Nessun carico registrato",
                        style = FitTextStyles.caption,
                        color = TextLow,
                    )
                }

                exercise.painNote?.let { note ->
                    Text(text = "⚠ $note", style = FitTextStyles.caption, color = WarningAmber)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(
                    text = "Sostituisci",
                    onClick = onSubstitute,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    text = if (expanded) "Chiudi" else "Dettagli",
                    onClick = onToggle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlanIllustration(equipment: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(FitRadius.md))
            .background(FitSurfaceVariant)
            .border(1.dp, FitOutline, RoundedCornerShape(FitRadius.md)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "illustrazione tecnica", style = FitTextStyles.label, color = TextLow)
            Text(
                text = if (equipment.isBlank()) {
                    "non disponibile per questo esercizio"
                } else {
                    "attrezzatura · $equipment"
                },
                style = FitTextStyles.caption,
                color = TextLow,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PlanNoticePanel(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FitRadius.md))
            .background(FitSurfaceVariant)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(FitRadius.md))
            .padding(14.dp),
    ) {
        Text(text = text, style = FitTextStyles.bodySmall, color = TextSecondary)
    }
}

private fun planIndexLabel(index: Int): String = if (index < 10) "0$index" else "$index"
