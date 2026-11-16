package com.fitcoach.app.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.AnimatedBar
import com.fitcoach.app.ui.components.DataRow
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.StatTile
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.BrandSoft
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.SuccessGreen
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* =============================================================================
 * Allenamento: scheda settimanale + sessione in corso.
 * Solo presentazione: carichi, RIR e progressione arrivano già decisi dal
 * motore (Double Progression) e vengono solo visualizzati/registrati.
 * ========================================================================== */

@Composable
fun WorkoutContent(
    state: WorkoutUiState,
    onSelectDay: (Int) -> Unit,
    onStartSession: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.errorMessage
    val day = state.day

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Allenamento", style = FitTextStyles.display, color = TextPrimary)
                if (state.planName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = state.planName, style = FitTextStyles.bodySmall, color = TextSecondary)
                }
            }
            if (state.mesocycleLabel.isNotBlank()) {
                Pill(text = state.mesocycleLabel, color = Emerald)
            }
        }

        when {
            state.isLoading -> {
                LoadingBox()
                LoadingBox()
            }

            error != null -> EmptyState(
                title = "Scheda non disponibile",
                message = error,
                actionLabel = "Riprova",
                onAction = onRetry,
            )

            !state.hasPlan -> EmptyState(
                title = "Nessun programma di allenamento",
                message = "Il programma viene generato dal Coach AI a partire dall'anamnesi: nessuna scheda viene precaricata.",
                actionLabel = "Apri anamnesi",
                onAction = onRetry,
            )

            else -> {
                WeekRow(state = state, onSelectDay = onSelectDay)
                if (day != null) {
                    DaySummary(day = day)
                    SectionHeader(title = "Esercizi", subtitle = day.focus)
                    day.exercises.forEach { exercise ->
                        ExerciseCard(exercise = exercise, onOpen = onOpenExercise)
                    }
                    if (day.note != null) {
                        SectionCard {
                            Text(
                                text = day.note,
                                style = FitTextStyles.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    PrimaryButton(text = "Inizia allenamento", onClick = onStartSession)
                } else {
                    EmptyState(
                        title = "Giorno di recupero",
                        message = "Nessuna scheda programmata per il giorno selezionato.",
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekRow(state: WorkoutUiState, onSelectDay: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Settimana",
            subtitle = "${state.completedDays} di ${state.plannedDays} allenamenti completati",
        )
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                state.week.forEachIndexed { index, trainingDay ->
                    val shape = RoundedCornerShape(FitRadius.sm)
                    val selected = index == state.selectedDayIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (selected) BrandSoft else Color.Transparent)
                            .clickable { onSelectDay(index) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = trainingDay.dayLabel,
                            style = FitTextStyles.caption,
                            color = if (selected) Emerald else TextLow,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trainingDay.dateLabel,
                            style = FitTextStyles.numericS,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Column(
                            modifier = Modifier
                                .height(4.dp)
                                .width(16.dp)
                                .clip(FitRadius.pill)
                                .background(statusColor(trainingDay.status)),
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySummary(day: WorkoutDayUi) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(text = day.name, style = FitTextStyles.h3, color = TextPrimary)
                Text(text = day.focus, style = FitTextStyles.caption, color = TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    label = "Esercizi",
                    value = day.exerciseCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Serie",
                    value = day.totalSets.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Durata",
                    value = "${day.estimatedMinutes}m",
                    modifier = Modifier.weight(1f),
                )
            }
            DataRow(label = "Volume settimanale", value = day.volumeLabel)
        }
    }
}

@Composable
private fun ExerciseCard(exercise: PlannedExerciseUi, onOpen: (Long) -> Unit) {
    SectionCard(modifier = Modifier.clickable { onOpen(exercise.id) }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeShape = RoundedCornerShape(FitRadius.xs)
                Column(
                    modifier = Modifier
                        .clip(badgeShape)
                        .background(FitSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = exercise.order.toString(),
                        style = FitTextStyles.numericS,
                        color = Emerald,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = FitTextStyles.bodyStrong,
                        color = TextPrimary,
                    )
                    Text(
                        text = listOfNotNull(exercise.muscleGroup, exercise.equipment).joinToString(" · "),
                        style = FitTextStyles.caption,
                        color = TextLow,
                    )
                }
                if (exercise.hasAlternatives) {
                    Pill(text = "Alt", color = TextSecondary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataRow(
                    label = "Serie × rip",
                    value = "${exercise.sets} × ${exercise.repsLabel}",
                    modifier = Modifier.weight(1f),
                )
                DataRow(
                    label = "RIR",
                    value = exercise.rirLabel,
                    modifier = Modifier.weight(1f),
                )
                DataRow(
                    label = "Recupero",
                    value = exercise.restLabel,
                    modifier = Modifier.weight(1f),
                )
            }

            if (exercise.targetLoadLabel != null) {
                Text(
                    text = "Carico target: ${exercise.targetLoadLabel}",
                    style = FitTextStyles.caption,
                    color = Emerald,
                )
            }
            if (exercise.lastPerformanceLabel != null) {
                Text(
                    text = "Ultima volta: ${exercise.lastPerformanceLabel}",
                    style = FitTextStyles.caption,
                    color = TextLow,
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------
 * Sessione in corso
 * ------------------------------------------------------------------------ */

@Composable
fun SessionContent(
    state: SessionUiState,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRirChange: (String) -> Unit,
    onLogSet: () -> Unit,
    onReportPain: () -> Unit,
    onSkipRest: () -> Unit,
    onNextExercise: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = state.currentExercise
    val error = state.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = state.dayName, style = FitTextStyles.h3, color = TextPrimary)
                Text(
                    text = "${state.completedSets} di ${state.totalSets} serie registrate",
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                )
            }
            Pill(text = "Serie ${state.setNumber}", color = Emerald)
        }

        AnimatedBar(fraction = state.progress, height = 6.dp)

        if (state.isResting && state.restSecondsLeft != null) {
            RestTimer(seconds = state.restSecondsLeft, onSkip = onSkipRest)
        }

        if (exercise != null) {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(
                            text = exercise.name,
                            style = FitTextStyles.bodyStrong,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${exercise.muscleGroup} · target ${exercise.targetRepsLabel} · RIR ${exercise.targetRirLabel}",
                            style = FitTextStyles.caption,
                            color = TextLow,
                        )
                    }

                    exercise.sets.forEach { logged ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${logged.setNumber}",
                                style = FitTextStyles.numericS,
                                color = TextLow,
                                modifier = Modifier.width(22.dp),
                            )
                            Text(
                                text = "${formatLoad(logged.weightKg)} kg × ${logged.reps}",
                                style = FitTextStyles.numericS,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (logged.rir != null) "RIR ${logged.rir}" else "",
                                style = FitTextStyles.caption,
                                color = TextSecondary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (logged.isCompleted) "✓" else "·",
                                style = FitTextStyles.numericS,
                                color = if (logged.isCompleted) SuccessGreen else TextLow,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(
                            label = "Carico kg",
                            value = state.weightInput,
                            keyboardType = KeyboardType.Decimal,
                            onValueChange = onWeightChange,
                            modifier = Modifier.weight(1f),
                        )
                        NumericField(
                            label = "Ripetizioni",
                            value = state.repsInput,
                            keyboardType = KeyboardType.Number,
                            onValueChange = onRepsChange,
                            modifier = Modifier.weight(1f),
                        )
                        NumericField(
                            label = "RIR",
                            value = state.rirInput,
                            keyboardType = KeyboardType.Number,
                            onValueChange = onRirChange,
                            modifier = Modifier.weight(0.8f),
                        )
                    }

                    PrimaryButton(
                        text = "Registra serie",
                        onClick = onLogSet,
                        enabled = state.canLogSet && !state.isSaving,
                    )
                    GhostButton(text = "Dolore durante l'esercizio", onClick = onReportPain)
                }
            }
        }

        if (state.isPainReported) {
            SectionCard {
                Text(
                    text = "Dolore segnalato: la serie non verrà usata per la progressione dei carichi. Interrompi l'esercizio se persiste.",
                    style = FitTextStyles.bodySmall,
                    color = ErrorRed,
                )
            }
        }

        if (error != null) {
            Text(text = error, style = FitTextStyles.caption, color = ErrorRed)
        }

        if (!state.isLastExercise) {
            GhostButton(text = "Esercizio successivo", onClick = onNextExercise)
        }
        PrimaryButton(text = "Termina sessione", onClick = onFinish, enabled = !state.isSaving)
    }
}

@Composable
private fun RestTimer(seconds: Int, onSkip: () -> Unit) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "RECUPERO", style = FitTextStyles.caption, color = TextLow)
            Text(
                text = formatSeconds(seconds),
                style = FitTextStyles.numericXL,
                color = WarningAmber,
            )
            GhostButton(text = "Salta recupero", onClick = onSkip)
        }
    }
}

@Composable
private fun NumericField(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(FitRadius.md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(FitSurfaceVariant)
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = label.uppercase(), style = FitTextStyles.caption, color = TextLow)
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = FitTextStyles.numericM.copy(color = TextPrimary),
            cursorBrush = SolidColor(Emerald),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun statusColor(status: DayStatus): Color = when (status) {
    DayStatus.COMPLETED -> SuccessGreen
    DayStatus.TODAY -> Emerald
    DayStatus.SCHEDULED -> TextLow
    DayStatus.REST -> FitOutline
}

private fun formatLoad(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()

private fun formatSeconds(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
