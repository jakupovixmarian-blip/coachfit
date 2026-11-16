package com.fitcoach.app.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.fitcoach.app.ui.components.FieldRow
import com.fitcoach.app.ui.components.FitChoiceChip
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.SegmentedControl
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
 * Allenamento — scheda settimanale, calendario, volume, sessione in corso.
 * -----------------------------------------------------------------------------
 * Solo presentazione. Carichi, RIR target, volume e progressione arrivano già
 * decisi dal motore (Double Progression) e dal CoachOrchestrator: qui vengono
 * mostrati e registrati, mai calcolati.
 *
 * Le callback sono raccolte in tre contenitori SENZA valori di default, così
 * il compilatore impedisce di lasciare un pulsante senza azione.
 * ========================================================================== */

/**
 * Azioni della schermata Allenamento.
 * Tutte obbligatorie: una voce senza callback non compila.
 */
class WorkoutActions(
    val onSelectDay: (Int) -> Unit,
    val onWeekChange: (Int) -> Unit,
    val onStartSession: () -> Unit,
    val onResumeSession: () -> Unit,
    val onFinishSession: () -> Unit,
    val onOpenExercise: (Long) -> Unit,
    val onEditExercise: (Long) -> Unit,
    val onMoveExercise: (Long, Int) -> Unit,
    val onSubstituteExercise: (Long) -> Unit,
    val onTechniqueNote: (Long) -> Unit,
    val onSleepLog: () -> Unit,
    val onOpenCustomPlan: () -> Unit,
    val onOpenLibrary: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onOpenCalendar: () -> Unit,
    val onExtraSession: () -> Unit,
    val onStableEdit: () -> Unit,
    val onTransformRoutine: () -> Unit,
    val onLibraryQueryChange: (String) -> Unit,
    val onAddFromLibrary: (String) -> Unit,
    val onOpenSetEntry: () -> Unit,
    val onReportPain: () -> Unit,
    val onSkipRest: () -> Unit,
    val onAddRestTime: () -> Unit,
    val onRetry: () -> Unit,
)

/** Azioni della sessione in corso. */
class SessionActions(
    val onWeightChange: (String) -> Unit,
    val onRepsChange: (String) -> Unit,
    val onRirChange: (String) -> Unit,
    val onRpeChange: (String) -> Unit,
    val onLogSet: () -> Unit,
    val onReportPain: () -> Unit,
    val onSkipRest: () -> Unit,
    val onAddRestTime: () -> Unit,
    val onNextExercise: () -> Unit,
    val onFinish: () -> Unit,
    val onOpenSetEntry: () -> Unit,
)

/** Azioni dei dialog aperti dalla schermata. */
class WorkoutDialogActions(
    val onDismiss: () -> Unit,
    val onSetDraftWeight: (String) -> Unit,
    val onSetDraftReps: (String) -> Unit,
    val onSetDraftRir: (String) -> Unit,
    val onSetDraftRpe: (String) -> Unit,
    val onSetDraftRest: (String) -> Unit,
    val onSetDraftPain: (Boolean) -> Unit,
    val onConfirmSetEntry: () -> Unit,
    val onEditorSets: (String) -> Unit,
    val onEditorReps: (String) -> Unit,
    val onEditorRir: (String) -> Unit,
    val onEditorRest: (String) -> Unit,
    val onEditorNote: (String) -> Unit,
    val onConfirmExerciseEditor: () -> Unit,
    val onSelectAlternative: (String) -> Unit,
    val onConfirmSubstitution: () -> Unit,
    val onSelectCalendarDay: (Int) -> Unit,
    val onConfirmFinish: () -> Unit,
    val onLibraryQueryChange: (String) -> Unit,
    val onAddFromLibrary: (String) -> Unit,
    val onOpenExtraSession: () -> Unit,
    val onOpenStableEdit: () -> Unit,
)

@Composable
fun WorkoutContent(
    state: WorkoutUiState,
    actions: WorkoutActions,
    sessionActions: SessionActions,
    dialogActions: WorkoutDialogActions,
    modifier: Modifier = Modifier,
) {
    val error = state.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WorkoutHeader(state)

        when {
            state.isLoading -> {
                LoadingBox()
                LoadingBox()
            }

            error != null -> EmptyState(
                title = "Scheda non disponibile",
                message = error,
                actionLabel = "Riprova",
                onAction = actions.onRetry,
            )

            !state.hasPlan -> EmptyState(
                title = "Nessun programma di allenamento",
                message = "Il programma viene generato dal Coach AI a partire dall'anamnesi: nessuna scheda viene precaricata.",
                actionLabel = "Apri anamnesi",
                onAction = actions.onRetry,
            )

            else -> {
                if (state.warnings.isNotEmpty()) {
                    WarningsBlock(state.warnings)
                }

                WeekSection(state = state, actions = actions)

                val day = state.day
                if (day != null) {
                    DaySummary(day = day, sleepHours = state.sleepHours, onSleepLog = actions.onSleepLog)
                    ExercisesSection(day = day, actions = actions)
                } else {
                    EmptyState(
                        title = "Giorno di recupero",
                        message = "Nessuna scheda programmata per il giorno selezionato.",
                    )
                }

                VolumeSection(state.volumeByMuscle)
                CalendarSection(state = state, actions = actions)

                if (state.isSessionActive && state.session != null) {
                    SessionBlock(state = state.session, actions = sessionActions)
                } else {
                    SessionEntryActions(state = state, actions = actions)
                }

                if (state.loadLog.isNotEmpty()) {
                    LoadLogBlock(state.loadLog)
                }

                HistorySection(state = state, actions = actions)
                LibrarySection(state = state, actions = actions)
            }
        }
    }

    WorkoutDialogHost(state = state, actions = actions, dialogActions = dialogActions)
}

/* --------------------------------------------------------------------------- */

@Composable
private fun WorkoutHeader(state: WorkoutUiState) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Allenamento", style = FitTextStyles.display, color = TextPrimary)
            val subtitle = listOfNotNull(
                state.splitLabel.takeIf { it.isNotBlank() },
                state.planName.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = FitTextStyles.bodySmall, color = TextSecondary)
            }
        }
        if (state.mesocycleLabel.isNotBlank()) {
            Pill(text = state.mesocycleLabel, color = Emerald)
        }
    }
}

@Composable
private fun WarningsBlock(warnings: List<String>) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "AVVISI", style = FitTextStyles.caption, color = WarningAmber)
            warnings.forEach { warning ->
                Text(text = warning, style = FitTextStyles.bodySmall, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun WeekSection(state: WorkoutUiState, actions: WorkoutActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Settimana ${state.weekIndex} di ${state.weekTotal}",
            subtitle = "${state.completedDays} di ${state.plannedDays} allenamenti completati",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostButton(
                text = "‹",
                onClick = { actions.onWeekChange(state.weekIndex - 1) },
                enabled = state.canGoToPreviousWeek,
            )
            Box(
                modifier = Modifier.weight(1f),
            ) {
                AnimatedBar(fraction = state.weekProgress, height = 6.dp)
            }
            GhostButton(
                text = "›",
                onClick = { actions.onWeekChange(state.weekIndex + 1) },
                enabled = state.canGoToNextWeek,
            )
        }

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
                            .clickable { actions.onSelectDay(index) }
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
                                .background(statusTint(trainingDay.status)),
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySummary(day: WorkoutDayUi, sleepHours: Double?, onSleepLog: () -> Unit) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = day.name, style = FitTextStyles.h3, color = TextPrimary)
                    Text(text = day.focus, style = FitTextStyles.caption, color = TextSecondary)
                }
                if (day.isRestDay) {
                    Pill(text = "Riposo", color = TextSecondary)
                }
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
            DataRow(
                label = "Sonno",
                value = if (sleepHours != null) "%.1f h".format(sleepHours) else "non registrato",
                valueColor = if (sleepHours != null) TextPrimary else TextLow,
            )
            GhostButton(text = "Registra sonno", onClick = onSleepLog)
            if (day.note != null) {
                Text(text = day.note, style = FitTextStyles.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ExercisesSection(day: WorkoutDayUi, actions: WorkoutActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Esercizi", subtitle = day.focus)

        if (day.exercises.isEmpty()) {
            Text(
                text = if (day.isRestDay) {
                    "Giorno di riposo: nessun esercizio programmato."
                } else {
                    "Nessun esercizio in questa scheda. Aggiungine uno dalla libreria o crea una scheda personalizzata."
                },
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
        }

        day.exercises.forEachIndexed { index, exercise ->
            ExerciseCard(
                exercise = exercise,
                isFirst = index == 0,
                isLast = index == day.exercises.lastIndex,
                actions = actions,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(
                text = "Crea scheda personalizzata",
                onClick = actions.onOpenCustomPlan,
                modifier = Modifier.weight(1f),
            )
            GhostButton(text = "Libreria", onClick = actions.onOpenLibrary)
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: PlannedExerciseUi,
    isFirst: Boolean,
    isLast: Boolean,
    actions: WorkoutActions,
) {
    SectionCard {
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
                    Text(text = exercise.name, style = FitTextStyles.bodyStrong, color = TextPrimary)
                    Text(
                        text = listOfNotNull(
                            exercise.muscleGroup.takeIf { it.isNotBlank() },
                            exercise.equipment.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = FitTextStyles.caption,
                        color = TextLow,
                    )
                }
                when {
                    exercise.painFlagged -> Pill(text = "Dolore", color = WarningAmber)
                    exercise.hasAlternatives -> Pill(text = "Alternative", color = TextSecondary)
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
            if (exercise.techniqueNote != null) {
                Text(
                    text = exercise.techniqueNote,
                    style = FitTextStyles.caption,
                    color = TextSecondary,
                )
            }
            if (exercise.description.isNotBlank()) {
                Text(
                    text = exercise.description,
                    style = FitTextStyles.bodySmall,
                    color = TextSecondary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FitChoiceChip(
                    text = "Dettagli",
                    selected = false,
                    onClick = { actions.onOpenExercise(exercise.id) },
                )
                FitChoiceChip(
                    text = "Modifica",
                    selected = false,
                    onClick = { actions.onEditExercise(exercise.id) },
                )
                if (exercise.hasAlternatives) {
                    FitChoiceChip(
                        text = "Sostituisci",
                        selected = false,
                        onClick = { actions.onSubstituteExercise(exercise.id) },
                    )
                }
                FitChoiceChip(
                    text = "Nota tecnica",
                    selected = false,
                    onClick = { actions.onTechniqueNote(exercise.id) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GhostButton(
                    text = "Sposta su",
                    onClick = { actions.onMoveExercise(exercise.id, -1) },
                    enabled = !isFirst,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    text = "Sposta giù",
                    onClick = { actions.onMoveExercise(exercise.id, 1) },
                    enabled = !isLast,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VolumeSection(volume: List<MuscleVolumeUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Volume per gruppo muscolare",
            subtitle = "Serie settimanali effettive contro il target del mesociclo",
        )
        if (volume.isEmpty()) {
            Text(
                text = "Il volume compare dopo la prima settimana registrata.",
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
            return@Column
        }
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MuscleVolumeChart(volume = volume)
                volume.forEach { entry ->
                    DataRow(
                        label = entry.muscle,
                        value = "${entry.sets} / ${entry.targetSets} serie",
                        valueColor = if (entry.sets < entry.targetSets) WarningAmber else SuccessGreen,
                    )
                    if (entry.deltaLabel.isNotBlank()) {
                        Text(
                            text = entry.deltaLabel,
                            style = FitTextStyles.caption,
                            color = TextLow,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSection(state: WorkoutUiState, actions: WorkoutActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Calendario allenamenti",
            subtitle = "Settimana corrente · gli override valgono solo per questa settimana",
        )
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.calendar.isEmpty()) {
                    Text(
                        text = "Nessun giorno programmato in questa settimana.",
                        style = FitTextStyles.bodySmall,
                        color = TextLow,
                    )
                }
                state.calendar.forEach { day ->
                    DataRow(
                        label = "${day.weekdayLabel} ${day.dateLabel}",
                        value = if (day.planName.isBlank()) statusLabel(day.status) else day.planName,
                        valueColor = statusTint(day.status),
                    )
                    if (day.isOverride) {
                        Text(
                            text = day.overrideNote ?: "Seduta modificata solo per questa settimana",
                            style = FitTextStyles.caption,
                            color = WarningAmber,
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(
                text = "Apri calendario",
                onClick = actions.onOpenCalendar,
                modifier = Modifier.weight(1f),
            )
            GhostButton(text = "+ Straordinaria", onClick = actions.onExtraSession)
        }
        GhostButton(text = "Modifica stabile della scheda", onClick = actions.onStableEdit)
    }
}

@Composable
private fun SessionEntryActions(state: WorkoutUiState, actions: WorkoutActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Sessione", subtitle = "Registra le serie: la progressione la gestisce il motore")
        PrimaryButton(text = "Inizia allenamento", onClick = actions.onStartSession)
        GhostButton(text = "Riprendi sessione", onClick = actions.onResumeSession)
        GhostButton(text = "Inizia sessione libera", onClick = actions.onOpenSetEntry)
        GhostButton(text = "Termina allenamento", onClick = actions.onFinishSession)
        GhostButton(text = "Trasforma in routine", onClick = actions.onTransformRoutine)
        if (state.painNote != null) {
            Text(
                text = state.painNote,
                style = FitTextStyles.caption,
                color = ErrorRed,
            )
        }
    }
}

@Composable
private fun LoadLogBlock(loadLog: List<SetLogUi>) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "DIARIO CARICHI", style = FitTextStyles.caption, color = TextLow)
            loadLog.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.setNumber.toString(),
                        style = FitTextStyles.numericS,
                        color = TextLow,
                        modifier = Modifier.width(22.dp),
                    )
                    Text(
                        text = "${formatLoad(entry.weightKg)} kg × ${entry.reps}",
                        style = FitTextStyles.numericS,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = buildString {
                            entry.rir?.let { append("RIR $it") }
                            entry.rpe?.let {
                                if (isNotEmpty()) append(" · ")
                                append("RPE $it")
                            }
                        },
                        style = FitTextStyles.caption,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (entry.isCompleted) "✓" else "·",
                        style = FitTextStyles.numericS,
                        color = if (entry.isCompleted) SuccessGreen else TextLow,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySection(state: WorkoutUiState, actions: WorkoutActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = "Storico sessioni",
            subtitle = if (state.history.isEmpty()) {
                "Nessuna sessione conclusa"
            } else {
                "${state.history.size} sessioni registrate"
            },
        )
        GhostButton(text = "Apri storico", onClick = actions.onOpenHistory)
    }
}

@Composable
private fun LibrarySection(state: WorkoutUiState, actions: WorkoutActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Libreria esercizi",
            subtitle = "Aggiungi alla seduta di oggi",
        )
        FieldRow(
            label = "Cerca",
            value = if (state.libraryQuery.isBlank()) "esercizio o gruppo muscolare" else state.libraryQuery,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Petto", "Dorso", "Gambe", "Spalle").forEach { muscle ->
                FitChoiceChip(
                    text = muscle,
                    selected = state.libraryQuery.equals(muscle, ignoreCase = true),
                    onClick = { actions.onLibraryQueryChange(muscle) },
                )
            }
        }
        state.library.take(6).forEach { item ->
            SectionCard(modifier = Modifier.clickable { actions.onAddFromLibrary(item.name) }) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = FitTextStyles.bodyStrong,
                                color = TextPrimary,
                            )
                            Text(
                                text = listOfNotNull(
                                    item.muscle.takeIf { it.isNotBlank() },
                                    item.equipment.takeIf { it.isNotBlank() },
                                ).joinToString(" · "),
                                style = FitTextStyles.caption,
                                color = TextLow,
                            )
                        }
                        Pill(
                            text = if (item.inTodaySession) "In seduta" else "Aggiungi",
                            color = if (item.inTodaySession) SuccessGreen else Emerald,
                        )
                    }
                }
            }
        }
        GhostButton(text = "Apri libreria completa", onClick = actions.onOpenLibrary)
    }
}

/* ---------------------------------------------------------------------------
 * Sessione in corso
 * ------------------------------------------------------------------------ */

/** Wrapper pubblico: la sessione può essere mostrata anche da sola. */
@Composable
fun SessionContent(
    state: SessionUiState,
    actions: SessionActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SessionBlock(state = state, actions = actions)
    }
}

@Composable
private fun SessionBlock(state: SessionUiState, actions: SessionActions) {
    val exercise = state.currentExercise

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "RECUPERO", style = FitTextStyles.caption, color = TextLow)
                    Text(
                        text = formatSeconds(state.restSecondsLeft),
                        style = FitTextStyles.numericXL,
                        color = WarningAmber,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton(
                            text = "+30s",
                            onClick = actions.onAddRestTime,
                            modifier = Modifier.weight(1f),
                        )
                        GhostButton(
                            text = "Salta",
                            onClick = actions.onSkipRest,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
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
                        if (exercise.techniqueNote != null) {
                            Text(
                                text = exercise.techniqueNote,
                                style = FitTextStyles.caption,
                                color = TextSecondary,
                            )
                        }
                    }

                    exercise.sets.forEach { logged ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = logged.setNumber.toString(),
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
                                text = buildString {
                                    logged.rir?.let { append("RIR $it") }
                                    logged.rpe?.let {
                                        if (isNotEmpty()) append(" · ")
                                        append("RPE $it")
                                    }
                                },
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
                            onValueChange = actions.onWeightChange,
                            modifier = Modifier.weight(1f),
                        )
                        NumericField(
                            label = "Ripetizioni",
                            value = state.repsInput,
                            keyboardType = KeyboardType.Number,
                            onValueChange = actions.onRepsChange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(
                            label = "RIR",
                            value = state.rirInput,
                            keyboardType = KeyboardType.Number,
                            onValueChange = actions.onRirChange,
                            modifier = Modifier.weight(1f),
                        )
                        NumericField(
                            label = "RPE",
                            value = state.rpeInput,
                            keyboardType = KeyboardType.Number,
                            onValueChange = actions.onRpeChange,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    PrimaryButton(
                        text = "Registra serie",
                        onClick = actions.onLogSet,
                        enabled = state.canLogSet && !state.isSaving,
                    )
                    GhostButton(text = "Compila serie in dettaglio", onClick = actions.onOpenSetEntry)
                    GhostButton(text = "Dolore durante l'esercizio", onClick = actions.onReportPain)
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

        if (state.errorMessage != null) {
            Text(text = state.errorMessage, style = FitTextStyles.caption, color = ErrorRed)
        }

        if (state.sleepHours != null) {
            DataRow(label = "Sonno registrato", value = "%.1f h".format(state.sleepHours))
        }

        if (!state.isLastExercise) {
            GhostButton(text = "Esercizio successivo", onClick = actions.onNextExercise)
        }
        PrimaryButton(
            text = "Termina sessione",
            onClick = actions.onFinish,
            enabled = !state.isSaving,
        )
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

/* ---------------------------------------------------------------------------
 * Host dei dialog: un solo dialog alla volta, pilotato da state.dialog.
 * ------------------------------------------------------------------------ */

@Composable
private fun WorkoutDialogHost(
    state: WorkoutUiState,
    actions: WorkoutActions,
    dialogActions: WorkoutDialogActions,
) {
    when (state.dialog) {
        WorkoutDialog.NONE -> Unit

        WorkoutDialog.SET_ENTRY -> SetEntryDialog(
            draft = state.setDraft,
            onWeightChange = dialogActions.onSetDraftWeight,
            onRepsChange = dialogActions.onSetDraftReps,
            onRirChange = dialogActions.onSetDraftRir,
            onRpeChange = dialogActions.onSetDraftRpe,
            onRestChange = dialogActions.onSetDraftRest,
            onTogglePain = dialogActions.onSetDraftPain,
            onConfirm = dialogActions.onConfirmSetEntry,
            onDismiss = dialogActions.onDismiss,
            isSaving = state.session?.isSaving == true,
        )

        WorkoutDialog.EXERCISE_EDITOR -> ExerciseEditorDialog(
            draft = state.editorDraft,
            onSetsChange = dialogActions.onEditorSets,
            onRepsChange = dialogActions.onEditorReps,
            onRirChange = dialogActions.onEditorRir,
            onRestChange = dialogActions.onEditorRest,
            onNoteChange = dialogActions.onEditorNote,
            onConfirm = dialogActions.onConfirmExerciseEditor,
            onDismiss = dialogActions.onDismiss,
        )

        WorkoutDialog.EXERCISE_LIBRARY -> ExerciseLibraryDialog(
            query = state.libraryQuery,
            items = state.library,
            muscleFilters = emptyList(),
            selectedFilterIndex = -1,
            onQueryChange = dialogActions.onLibraryQueryChange,
            onSelectFilter = { },
            onAddToSession = dialogActions.onAddFromLibrary,
            onDismiss = dialogActions.onDismiss,
        )

        WorkoutDialog.SUBSTITUTION -> ExerciseSubstitutionDialog(
            exerciseName = state.day?.exercises
                ?.firstOrNull { it.hasAlternatives }
                ?.name
                .orEmpty(),
            alternatives = state.alternatives,
            selectedName = state.selectedAlternative,
            onSelect = dialogActions.onSelectAlternative,
            onConfirm = dialogActions.onConfirmSubstitution,
            onDismiss = dialogActions.onDismiss,
        )

        WorkoutDialog.SESSION_HISTORY -> SessionHistoryDialog(
            sessions = state.history,
            onDismiss = dialogActions.onDismiss,
        )

        WorkoutDialog.CALENDAR -> WorkoutCalendarDialog(
            days = state.calendar,
            selectedIndex = state.selectedDayIndex,
            onSelectDay = dialogActions.onSelectCalendarDay,
            onExtraSession = dialogActions.onOpenExtraSession,
            onStableEdit = dialogActions.onOpenStableEdit,
            onDismiss = dialogActions.onDismiss,
        )

        WorkoutDialog.FINISH -> FinishDialog(
            dayName = state.session?.dayName ?: state.day?.name.orEmpty(),
            completedSets = state.session?.completedSets ?: 0,
            totalSets = state.session?.totalSets ?: 0,
            volumeLabel = state.day?.volumeLabel.orEmpty(),
            painFlagged = state.session?.isPainReported == true,
            onConfirm = dialogActions.onConfirmFinish,
            onDismiss = dialogActions.onDismiss,
            onTransformRoutine = actions.onTransformRoutine,
            isSaving = state.session?.isSaving == true,
        )
    }
}

/* --------------------------------------------------------------------------- */

private fun formatLoad(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()

private fun formatSeconds(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
