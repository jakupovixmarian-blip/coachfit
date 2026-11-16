package com.fitcoach.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.BarChart
import com.fitcoach.app.ui.components.DataRow
import com.fitcoach.app.ui.components.DialogActions
import com.fitcoach.app.ui.components.FitCheckbox
import com.fitcoach.app.ui.components.FitChoiceChip
import com.fitcoach.app.ui.components.FitDialog
import com.fitcoach.app.ui.components.FitSelectableRow
import com.fitcoach.app.ui.components.FitTextField
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.SuccessGreen
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* =============================================================================
 * Allenamento — i 7 dialog della schermata.
 * -----------------------------------------------------------------------------
 * Tutti stateless: stato in ingresso, eventi in uscita. Le callback non hanno
 * valori di default, così il compilatore impedisce un pulsante senza azione.
 * Nessuna logica di progressione qui: i carichi proposti arrivano già calcolati.
 * ========================================================================== */

/**
 * Registrazione di una serie: carico, ripetizioni, RIR, RPE e recupero.
 * L'utente registra e basta: la progressione la decide il motore.
 */
@Composable
fun SetEntryDialog(
    draft: SetEntryDraftUi,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRirChange: (String) -> Unit,
    onRpeChange: (String) -> Unit,
    onRestChange: (String) -> Unit,
    onTogglePain: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
) {
    FitDialog(
        title = "Serie ${draft.setNumber}",
        subtitle = if (draft.targetLabel.isNotBlank()) draft.targetLabel else draft.exerciseName,
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = if (isSaving) "Salvataggio…" else "Salva serie",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmEnabled = draft.weightInput.isNotBlank() &&
                    draft.repsInput.isNotBlank() &&
                    !isSaving,
            )
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FitTextField(
                value = draft.weightInput,
                onValueChange = onWeightChange,
                label = "Carico kg",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            FitTextField(
                value = draft.repsInput,
                onValueChange = onRepsChange,
                label = "Ripetizioni",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FitTextField(
                value = draft.rirInput,
                onValueChange = onRirChange,
                label = "RIR",
                placeholder = "0-5",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            FitTextField(
                value = draft.rpeInput,
                onValueChange = onRpeChange,
                label = "RPE",
                placeholder = "1-10",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            FitTextField(
                value = draft.restInput,
                onValueChange = onRestChange,
                label = "Recupero s",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }

        FitCheckbox(
            checked = draft.painFlagged,
            onCheckedChange = onTogglePain,
            label = "Dolore durante la serie",
            subtitle = "La serie non verrà usata per la progressione dei carichi",
        )

        if (draft.errorMessage != null) {
            Text(text = draft.errorMessage, style = FitTextStyles.caption, color = ErrorRed)
        }

        Text(
            text = "RIR = ripetizioni in riserva. RPE = sforzo percepito. Se il RIR è troppo alto il motore non aumenta il carico.",
            style = FitTextStyles.caption,
            color = TextLow,
        )
    }
}

/** Modifica della prescrizione di un esercizio (serie, ripetizioni, RIR, recupero, nota). */
@Composable
fun ExerciseEditorDialog(
    draft: ExerciseEditorDraftUi,
    onSetsChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRirChange: (String) -> Unit,
    onRestChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FitDialog(
        title = if (draft.isNew) "Nuovo esercizio" else "Modifica esercizio",
        subtitle = draft.name,
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = "Salva",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmEnabled = draft.setsInput.isNotBlank() && draft.repsInput.isNotBlank(),
            )
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FitTextField(
                value = draft.setsInput,
                onValueChange = onSetsChange,
                label = "Serie",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            FitTextField(
                value = draft.repsInput,
                onValueChange = onRepsChange,
                label = "Ripetizioni",
                placeholder = "6-10",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FitTextField(
                value = draft.rirInput,
                onValueChange = onRirChange,
                label = "RIR target",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            FitTextField(
                value = draft.restInput,
                onValueChange = onRestChange,
                label = "Recupero s",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }

        FitTextField(
            value = draft.techniqueNote,
            onValueChange = onNoteChange,
            label = "Nota tecnica",
            placeholder = "Esecuzione, tempi, avvertenze",
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )

        if (draft.errorMessage != null) {
            Text(text = draft.errorMessage, style = FitTextStyles.caption, color = ErrorRed)
        }

        Text(
            text = "La modifica cambia la scheda attuale: la progressione dei carichi resta gestita dal motore.",
            style = FitTextStyles.caption,
            color = TextLow,
        )
    }
}

/** Libreria esercizi con ricerca e filtro per gruppo muscolare. */
@Composable
fun ExerciseLibraryDialog(
    query: String,
    items: List<ExerciseLibraryItemUi>,
    muscleFilters: List<String>,
    selectedFilterIndex: Int,
    onQueryChange: (String) -> Unit,
    onSelectFilter: (Int) -> Unit,
    onAddToSession: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isSearching: Boolean = false,
) {
    FitDialog(
        title = "Libreria esercizi",
        subtitle = "Aggiungi alla seduta di oggi",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = "Chiudi",
                onConfirm = onDismiss,
                onDismiss = onDismiss,
                dismissText = "Annulla",
            )
        },
    ) {
        FitTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Cerca",
            placeholder = "Esercizio o gruppo muscolare",
            modifier = Modifier.fillMaxWidth(),
        )

        if (muscleFilters.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                muscleFilters.forEachIndexed { index, filter ->
                    FitChoiceChip(
                        text = filter,
                        selected = index == selectedFilterIndex,
                        onClick = { onSelectFilter(index) },
                    )
                }
            }
        }

        if (isSearching) {
            Text(text = "Cerco nella libreria…", style = FitTextStyles.caption, color = TextSecondary)
        }

        if (items.isEmpty() && !isSearching) {
            Text(
                text = "Nessun esercizio corrisponde alla ricerca.",
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
        }

        items.forEach { item ->
            FitSelectableRow(
                title = item.name,
                subtitle = listOfNotNull(
                    item.muscle.takeIf { it.isNotBlank() },
                    item.equipment.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                selected = false,
                onClick = { onAddToSession(item.name) },
                trailing = {
                    if (item.inTodaySession) {
                        Pill(text = "In seduta", color = SuccessGreen)
                    } else {
                        Pill(text = "Aggiungi", color = Emerald)
                    }
                },
            )
        }
    }
}

/** Sostituzione di un esercizio con un'alternativa dello stesso gruppo. */
@Composable
fun ExerciseSubstitutionDialog(
    exerciseName: String,
    alternatives: List<ExerciseAlternativeUi>,
    selectedName: String?,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentPrescription: String = "",
) {
    FitDialog(
        title = "Sostituisci esercizio",
        subtitle = "$exerciseName · $currentPrescription",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = "Sostituisci",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmEnabled = selectedName != null,
            )
        },
    ) {
        if (alternatives.isEmpty()) {
            Text(
                text = "Nessuna alternativa disponibile per questo esercizio con l'attrezzatura attuale.",
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
        }

        alternatives.forEach { alternative ->
            FitSelectableRow(
                title = alternative.name,
                subtitle = listOfNotNull(
                    alternative.muscle.takeIf { it.isNotBlank() },
                    alternative.equipment.takeIf { it.isNotBlank() },
                    alternative.prescription.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                selected = alternative.name == selectedName,
                onClick = { onSelect(alternative.name) },
            )
            alternative.note?.let { note ->
                Text(
                    text = note,
                    style = FitTextStyles.caption,
                    color = TextLow,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        Text(
            text = "La sostituzione mantiene serie, ripetizioni e recupero previsti: cambia l'esercizio, non lo stimolo.",
            style = FitTextStyles.caption,
            color = TextLow,
        )
    }
}

/** Storico delle sessioni concluse, con volume e durata. */
@Composable
fun SessionHistoryDialog(
    sessions: List<SessionHistoryUi>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FitDialog(
        title = "Storico sessioni",
        subtitle = if (sessions.isEmpty()) "Nessuna sessione registrata" else "${sessions.size} sessioni",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = "Chiudi",
                onConfirm = onDismiss,
                onDismiss = onDismiss,
            )
        },
    ) {
        if (sessions.isEmpty()) {
            Text(
                text = "Le sessioni concluse compaiono qui con volume totale e durata.",
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
        }

        sessions.forEach { session ->
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.dayName,
                                style = FitTextStyles.bodyStrong,
                                color = TextPrimary,
                            )
                            Text(
                                text = session.dateLabel,
                                style = FitTextStyles.caption,
                                color = TextLow,
                            )
                        }
                        if (session.painFlagged) {
                            Pill(text = "Dolore", color = WarningAmber)
                        }
                    }
                    DataRow(label = "Serie", value = session.sets.toString())
                    DataRow(label = "Volume", value = session.volumeLabel)
                    DataRow(label = "Durata", value = session.durationLabel)
                }
            }
        }
    }
}

/** Calendario allenamenti: settimana corrente, seduta straordinaria, modifica stabile. */
@Composable
fun WorkoutCalendarDialog(
    days: List<CalendarDayUi>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    onExtraSession: () -> Unit,
    onStableEdit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FitDialog(
        title = "Calendario allenamenti",
        subtitle = "Settimana corrente · gli override valgono solo per questa settimana",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = "Chiudi",
                onConfirm = onDismiss,
                onDismiss = onDismiss,
            )
        },
    ) {
        if (days.isEmpty()) {
            Text(
                text = "Nessun giorno programmato in questa settimana.",
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
        }

        days.forEachIndexed { index, day ->
            FitSelectableRow(
                title = "${day.weekdayLabel} ${day.dateLabel}",
                subtitle = if (day.planName.isBlank()) {
                    if (day.status == DayStatus.REST) "Riposo" else "Nessuna scheda"
                } else {
                    day.planName
                },
                selected = index == selectedIndex,
                onClick = { onSelectDay(index) },
                trailing = {
                    if (day.isOverride) {
                        Pill(text = "Override", color = WarningAmber)
                    } else {
                        Pill(text = statusLabel(day.status), color = statusTint(day.status))
                    }
                },
            )
            day.overrideNote?.let { note ->
                Text(
                    text = note,
                    style = FitTextStyles.caption,
                    color = TextLow,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        FitChoiceChip(
            text = "+ Seduta straordinaria",
            selected = false,
            onClick = onExtraSession,
        )
        FitChoiceChip(
            text = "Modifica stabile della scheda",
            selected = false,
            onClick = onStableEdit,
        )
    }
}

/** Chiusura della sessione: riepilogo e conferma. */
@Composable
fun FinishDialog(
    dayName: String,
    completedSets: Int,
    totalSets: Int,
    volumeLabel: String,
    painFlagged: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onTransformRoutine: () -> Unit = {},
    isSaving: Boolean = false,
) {
    FitDialog(
        title = "Termina allenamento",
        subtitle = dayName,
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = if (isSaving) "Salvataggio…" else "Termina",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmEnabled = !isSaving,
            )
        },
    ) {
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DataRow(label = "Serie registrate", value = "$completedSets di $totalSets")
                DataRow(label = "Volume totale", value = volumeLabel)
                DataRow(
                    label = "Dolore segnalato",
                    value = if (painFlagged) "Sì" else "No",
                    valueColor = if (painFlagged) WarningAmber else TextPrimary,
                )
            }
        }

        Text(
            text = "Le serie registrate alimentano la progressione dei carichi: la prossima scheda viene adattata dal motore.",
            style = FitTextStyles.caption,
            color = TextLow,
        )

        if (painFlagged) {
            Text(
                text = "Hai segnalato dolore: le serie interessate non verranno usate per aumentare il carico.",
                style = FitTextStyles.caption,
                color = ErrorRed,
            )
        }

        FitChoiceChip(
            text = "Trasforma in routine",
            selected = false,
            onClick = onTransformRoutine,
        )
    }
}

/** Etichetta leggibile di uno stato giorno. */
internal fun statusLabel(status: DayStatus): String = when (status) {
    DayStatus.COMPLETED -> "Fatto"
    DayStatus.TODAY -> "Oggi"
    DayStatus.SCHEDULED -> "Previsto"
    DayStatus.REST -> "Riposo"
}

/** Colore coerente con lo stato giorno. */
internal fun statusTint(status: DayStatus) = when (status) {
    DayStatus.COMPLETED -> SuccessGreen
    DayStatus.TODAY -> Emerald
    DayStatus.SCHEDULED -> TextSecondary
    DayStatus.REST -> TextLow
}

/** Grafico del volume settimanale per gruppo muscolare (usato dalla schermata). */
@Composable
fun MuscleVolumeChart(
    volume: List<MuscleVolumeUi>,
    modifier: Modifier = Modifier,
) {
    if (volume.isEmpty()) return
    BarChart(
        values = volume.map { it.sets.toFloat() },
        labels = volume.map { it.muscle.take(4) },
        modifier = modifier,
        color = Emerald,
        highlightIndex = volume.indices.maxByOrNull { volume[it].sets } ?: -1,
    )
}
