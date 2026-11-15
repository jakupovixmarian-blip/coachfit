package com.fitcoach.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.IntensityTechnique
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.model.WorkoutSet
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.TextSecondary
import java.time.LocalDate

private val weekdayNames = listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")

@Composable
fun WorkoutScreen(
    navController: NavHostController,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    val plan = state.plan
    if (plan == null) {
        Column(Modifier.fillMaxSize()) {
            EmptyState(
                title = "Nessuna scheda di allenamento",
                message = "Genera una programmazione periodizzata con split, volume e " +
                    "progressione dei carichi in base al tuo profilo.",
                action = {
                    Button(onClick = { viewModel.generateWorkout() }, enabled = !state.generating) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.generating) "Generazione..." else "Genera scheda")
                    }
                }
            )
            SnackbarHost(snackbar)
        }
        return
    }

    val today = state.today
    val session = state.session

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Allenamento", plan.splitType.label)
                Row {
                    IconButton(onClick = { viewModel.showCalendar(true) }) {
                        Icon(Icons.Filled.EventNote, contentDescription = "Calendario allenamenti", tint = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.showLibrary(true) }) {
                        Icon(Icons.Filled.MenuBook, contentDescription = "Libreria esercizi", tint = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.showHistory(true) }) {
                        Icon(Icons.Filled.History, contentDescription = "Storico sessioni", tint = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.generateWorkout() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rigenera", tint = Emerald)
                    }
                }
            }
        }

        item {
            TabRow(
                selectedTabIndex = if (state.section == WorkoutSection.PROGRAM) 0 else 1
            ) {
                WorkoutSection.entries.forEach { section ->
                    Tab(
                        selected = state.section == section,
                        onClick = { viewModel.selectSection(section) },
                        text = { Text(section.label) }
                    )
                }
            }
        }

        if (state.section == WorkoutSection.LOG) {
            item {
                ExerciseLogSection(
                    today = today,
                    performancesByExercise = state.performancesByExercise
                )
            }
        } else {

        item {
            SectionCard {
                Column {
                    SectionHeader(
                        "Oggi · ${weekdayNames.getOrElse(java.time.LocalDate.now().dayOfWeek.value - 1) { "" }}",
                        today?.title ?: "Giorno di riposo"
                    )
                    Spacer(Modifier.height(10.dp))
                    if (today == null) {
                        Text(
                            "Nessuna seduta programmata per oggi. Puoi recuperare oppure iniziare una sessione libera.",
                            style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::startFreeSession) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Inizia sessione libera")
                        }
                    } else if (session?.status == com.fitcoach.app.domain.model.SessionStatus.COMPLETED) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Emerald)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Seduta completata · ${session.completedSets} serie · " +
                                    "volume ${session.totalVolumeKg.toInt()} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { viewModel.startSession() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Riprendi sessione")
                        }
                        if (today?.id == 0L) {
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(onClick = viewModel::convertFreeSessionToRoutine) {
                                Text("Trasforma in routine")
                            }
                        }
                    } else if (session == null) {
                        Button(onClick = { viewModel.startSession() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Avvia sessione")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill("In corso", Emerald)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "⏱ ${formatRest(state.sessionElapsedSeconds)} · " +
                                    "${session.completedSets} serie · volume ${session.totalVolumeKg.toInt()} kg",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (state.resting) {
            item {
                RestTimerCard(
                    remaining = state.restRemainingSeconds,
                    total = state.restTotalSeconds,
                    reason = state.restReason,
                    onSkip = { viewModel.skipRest() },
                    onAdd = { viewModel.addRestTime(30) }
                )
            }
        }

        if (today != null) {
            today.exercises.forEach { planned ->
                item {
                    ExerciseCard(
                        planned = planned,
                        loggedSets = viewModel.sessionSetsFor(planned.exerciseId),
                        lastPerformance = state.lastPerformances[planned.exerciseId],
                        onLogSet = { viewModel.logSetFor(planned) },
                        onSubstitute = { viewModel.openSubstitution(planned) },
                        onRemove = { viewModel.removeExercise(planned) },
                        onEdit = { viewModel.openEdit(planned) }
                    )
                }
            }

            if (session?.status == com.fitcoach.app.domain.model.SessionStatus.IN_PROGRESS) {
                item {
                    Button(
                        onClick = { viewModel.openFinishDialog() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Termina allenamento")
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Settimana", "Volume per gruppo muscolare")
                    Spacer(Modifier.height(8.dp))
                    if (plan.weeklyVolumePerGroup.isEmpty()) {
                        Text("Volume non disponibile.", style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    } else {
                        plan.weeklyVolumePerGroup.entries
                            .sortedByDescending { it.value }
                            .forEach { (group, sets) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(group.label, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text("$sets serie", style = MaterialTheme.typography.labelMedium,
                                        color = Emerald)
                                }
                            }
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Prossime sedute", "Periodizzazione")
                    Spacer(Modifier.height(8.dp))
                    plan.days.filter { !it.isRestDay }.forEach { day ->
                        Text(
                            "${weekdayNames.getOrElse(day.dayIndex) { "?" }}: ${day.title} · ${day.totalSets} serie",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
        }
    }

    state.form?.let { form ->
        SetEntryDialog(
            form = form,
            onWeight = { viewModel.updateForm(weight = it) },
            onReps = { viewModel.updateForm(reps = it) },
            onRir = { viewModel.updateForm(rir = it) },
            onPain = { viewModel.updateForm(pain = it) },
            onTechnique = { viewModel.updateForm(technique = it) },
            onSave = { viewModel.saveSet() },
            onDismiss = { viewModel.closeForm() }
        )
    }

    if (state.finishDialog) {
        FinishDialog(
            sleep = state.sleepQuality,
            rpe = state.perceivedEffort,
            painNotes = state.painNotes,
            onSleep = { viewModel.updateFinish(sleep = it) },
            onRpe = { viewModel.updateFinish(rpe = it) },
            onPain = { viewModel.updateFinish(painNotes = it) },
            onConfirm = { viewModel.completeSession() },
            onDismiss = { viewModel.closeFinishDialog() }
        )
    }

    state.substituteFor?.let { planned ->
        SubstitutionDialog(
            planned = planned,
            options = state.substituteOptions,
            onSelect = { viewModel.applySubstitution(it) },
            onDismiss = { viewModel.closeSubstitution() }
        )
    }

    state.editingExercise?.let { planned ->
        ExerciseEditorDialog(
            planned = planned,
            onSave = { sets, min, max, rest, rir, technique ->
                viewModel.updateExerciseTemplate(planned, sets, min, max, rest, rir, technique)
                viewModel.closeEdit()
            },
            onDismiss = viewModel::closeEdit
        )
    }

    if (state.showHistory) {
        SessionHistoryDialog(
            history = state.history,
            onDismiss = { viewModel.showHistory(false) }
        )
    }

    if (state.showCalendar) {
        WorkoutCalendarDialog(
            days = state.calendarDays,
            weekStartEpochDay = state.calendarWeekStartEpochDay,
            onWeekChange = viewModel::changeCalendarWeek,
            onExtraSession = { dayIndex -> viewModel.addExtraSession(state.calendarWeekStartEpochDay + dayIndex) },
            onMove = viewModel::moveWorkout,
            onDismiss = { viewModel.showCalendar(false) }
        )
    }

    if (state.showLibrary) {
        ExerciseLibraryDialog(
            exercises = state.libraryExercises,
            query = state.libraryQuery,
            onQueryChange = viewModel::updateLibraryQuery,
            onAdd = viewModel::addExerciseToToday,
            onDismiss = { viewModel.showLibrary(false) }
        )
    }

    SnackbarHost(snackbar)
}

@Composable
private fun ExerciseLogSection(
    today: WorkoutDay?,
    performancesByExercise: Map<String, List<ExercisePerformance>>
) {
    SectionCard {
        Column {
            SectionHeader("Diario carichi", "Carichi e ripetizioni registrati")
            Spacer(Modifier.height(8.dp))
            if (performancesByExercise.isEmpty()) {
                Text(
                    "Nessuna prestazione registrata. Durante l'allenamento inserisci carico, " +
                        "ripetizioni e RIR: compariranno qui con l'andamento nel tempo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                return@Column
            }

            val todayIds = today?.exercises?.map { it.exerciseId }?.toSet() ?: emptySet()

            // Prima gli esercizi della seduta odierna, poi gli altri in ordine alfabetico.
            val ordered = performancesByExercise.entries
                .sortedWith(
                    compareByDescending<Map.Entry<String, List<ExercisePerformance>>> { entry ->
                        entry.value.any { it.exerciseId in todayIds }
                    }.thenBy { it.key }
                )

            ordered.forEach { (name, history) ->
                val recent = history.sortedByDescending { it.dateEpochDay }
                val last = recent.firstOrNull()
                val previous = recent.getOrNull(1)
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        if (last != null) {
                            val delta = if (previous != null) {
                                (last.topWeightKg - previous.topWeightKg).let {
                                    if (it > 0) " (+${"%.1f".format(it)} kg)" else if (it < 0) " (${"%.1f".format(it)} kg)" else ""
                                }
                            } else ""
                            Text(
                                "${"%.1f".format(last.topWeightKg)} kg × ${last.topReps}$delta",
                                style = MaterialTheme.typography.labelMedium,
                                color = Emerald
                            )
                        }
                    }
                    if (last != null) {
                        Text(
                            buildString {
                                append("${last.totalSets} serie · volume ${last.volumeKg.toInt()} kg")
                                last.estimated1Rm?.let { append(" · 1RM ≈ ${it.toInt()} kg") }
                                last.avgRir?.let { append(" · RIR ${"%.1f".format(it)}") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    // Mini andamento: ultimi carichi top.
                    if (recent.size > 1) {
                        Text(
                            "Ultimi: " + recent.take(6).reversed().joinToString(" → ") {
                                "${"%.0f".format(it.topWeightKg)}×${it.topReps}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryDialog(
    history: List<com.fitcoach.app.domain.model.WorkoutSession>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Storico sessioni") },
        text = {
            Column {
                if (history.isEmpty()) {
                    Text("Nessuna sessione registrata.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else {
                    history.take(12).forEach { s ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                "${LocalDate.ofEpochDay(s.dateEpochDay)} · ${s.dayTitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                buildString {
                                    append("${s.completedSets} serie · volume ${s.totalVolumeKg.toInt()} kg")
                                    s.durationMinutes?.let { append(" · ${it} min") }
                                    s.perceivedEffort?.let { append(" · RPE ${"%.1f".format(it)}") }
                                    if (s.painReported) append(" · dolore segnalato")
                                },
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}

@Composable
private fun WorkoutCalendarDialog(
    days: List<WorkoutDay>,
    weekStartEpochDay: Long,
    onWeekChange: (Long) -> Unit,
    onExtraSession: (Int) -> Unit,
    onMove: (Long, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val permanent = remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Calendario allenamenti")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onWeekChange(-1) }) { Text("‹") }
                    Text("Settimana ${LocalDate.ofEpochDay(weekStartEpochDay)}", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { onWeekChange(1) }) { Text("›") }
                }
            }
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Modifica stabile", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (permanent.value) "Aggiorna la programmazione futura" else "Solo settimana corrente",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(checked = permanent.value, onCheckedChange = { permanent.value = it })
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                items(days.size) { index ->
                    val day = days[index]
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            "${weekdayNames[day.dayIndex]} · ${day.title}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (day.isRestDay) "Giorno di riposo" else "${day.totalSets} serie programmate",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekdayNames.forEachIndexed { targetIndex, label ->
                                TextButton(
                                    onClick = { onMove(day.id, targetIndex, permanent.value) },
                                    enabled = targetIndex != day.dayIndex
                                ) { Text(label.take(2)) }
                            }
                        }
                        TextButton(onClick = { onExtraSession(day.dayIndex) }) {
                            Text("+ Seduta straordinaria")
                        }
                    }
                }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}



@Composable
private fun ExerciseLibraryDialog(
    exercises: List<Exercise>,
    query: String,
    onQueryChange: (String) -> Unit,
    onAdd: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val normalizedQuery = query.trim().lowercase()
    val filtered = exercises.filter { exercise ->
        normalizedQuery.isBlank() || listOf(
            exercise.name,
            exercise.primaryMuscle.label,
            exercise.equipment.joinToString { it.label }
        ).any { it.lowercase().contains(normalizedQuery) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Libreria esercizi (${filtered.size})") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Cerca esercizio o muscolo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(360.dp)) {
                    items(filtered.size) { index ->
                        val exercise = filtered[index]
                        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${exercise.primaryMuscle.label} · ${exercise.category.name} · " +
                                    "${exercise.minReps}-${exercise.maxReps} reps · ${exercise.restSeconds}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                "Attrezzatura: ${exercise.equipment.joinToString { it.label }.ifBlank { "corpo libero" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            TextButton(onClick = { onAdd(exercise) }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Aggiungi alla seduta di oggi")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}

@Composable
private fun RestTimerCard(
    remaining: Int,
    total: Int,
    reason: String,
    onSkip: () -> Unit,
    onAdd: () -> Unit
) {
    val progress = if (total > 0) (total - remaining).toFloat() / total else 0f
    SectionCard {
        Column {
            SectionHeader("Recupero", formatRest(remaining))
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = Emerald
            )
            if (reason.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(reason, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("+30s") }
                Button(onClick = onSkip, modifier = Modifier.weight(1f)) { Text("Salta") }
            }
        }
    }
}

private fun formatRest(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun ExerciseCard(
    planned: PlannedExercise,
    loggedSets: List<WorkoutSet>,
    lastPerformance: ExercisePerformance?,
    onLogSet: () -> Unit,
    onSubstitute: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    SectionCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        planned.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${planned.primaryMuscle.label} · ${planned.sets}×${planned.minReps}-${planned.maxReps} · " +
                            "RIR ${planned.targetRir} · rec ${planned.restSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                planned.targetWeightKg?.let { Pill("%.1f kg".format(it), CarbsColor) }
            }
            if (planned.selectionReason.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(planned.selectionReason, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (planned.intensityTechnique != IntensityTechnique.NONE && planned.techniqueReason.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(planned.techniqueReason, style = MaterialTheme.typography.bodySmall, color = Emerald)
            }
            if (planned.restReason.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Recupero: ${planned.restReason}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            lastPerformance?.let { perf ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ultima volta: ${"%.1f".format(perf.topWeightKg)} kg × ${perf.topReps} " +
                        "(top serie, ${perf.totalSets} serie, ${perf.totalReps} rip)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Emerald
                )
            }
            if (planned.technique.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(planned.technique, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            if (loggedSets.isNotEmpty()) {
                loggedSets.sortedBy { it.setIndex }.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Serie ${s.setIndex + 1}: ${s.weightKg} kg × ${s.actualReps}" +
                                (s.rir?.let { " @RIR $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Emerald,
                            modifier = Modifier.width(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onLogSet() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Registra serie ${loggedSets.size + 1}")
                }
                OutlinedButton(onClick = onSubstitute) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Sostituisci esercizio")
                }
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifica esercizio")
                }
                OutlinedButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Rimuovi esercizio")
                }
            }
        }
    }
}

@Composable
private fun ExerciseEditorDialog(
    planned: PlannedExercise,
    onSave: (Int, Int, Int, Int, Int, IntensityTechnique) -> Unit,
    onDismiss: () -> Unit
) {
    val sets = remember { mutableStateOf(planned.sets.toString()) }
    val minReps = remember { mutableStateOf(planned.minReps.toString()) }
    val maxReps = remember { mutableStateOf(planned.maxReps.toString()) }
    val rest = remember { mutableStateOf(planned.restSeconds.toString()) }
    val rir = remember { mutableStateOf(planned.targetRir.toString()) }
    val technique = remember { mutableStateOf(planned.intensityTechnique) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica ${planned.exerciseName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Questa modifica riguarda il programma futuro e non cambia lo storico.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(sets.value, { sets.value = it }, label = { Text("Serie") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(minReps.value, { minReps.value = it }, label = { Text("Min") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(maxReps.value, { maxReps.value = it }, label = { Text("Max") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(rest.value, { rest.value = it }, label = { Text("Recupero s") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(rir.value, { rir.value = it }, label = { Text("RIR") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Tecnica di intensità", style = MaterialTheme.typography.labelLarge)
                IntensityTechnique.entries.forEach { option ->
                    TextButton(onClick = { technique.value = option }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (technique.value == option) "✓ ${option.label}" else option.label)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        confirmButton = {
            Button(onClick = {
                val parsed = listOf(sets.value, minReps.value, maxReps.value, rest.value, rir.value).map { it.toIntOrNull() }
                if (parsed.all { it != null }) onSave(parsed[0]!!, parsed[1]!!, parsed[2]!!, parsed[3]!!, parsed[4]!!, technique.value)
            }) { Text("Salva") }
        }
    )
}

@Composable
private fun SubstitutionDialog(
    planned: PlannedExercise,
    options: List<com.fitcoach.app.domain.model.Exercise>,
    onSelect: (com.fitcoach.app.domain.model.Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sostituisci ${planned.exerciseName}") },
        text = {
            Column {
                Text(
                    "Le alternative mantengono lo stimolo previsto (${planned.primaryMuscle.label}).",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                if (options.isEmpty()) {
                    Text("Nessuna alternativa disponibile con l'attrezzatura e le limitazioni attuali.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    options.forEach { ex ->
                        TextButton(
                            onClick = { onSelect(ex) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(ex.name, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${ex.primaryMuscle.label} · ${ex.minReps}-${ex.maxReps} · rec ${ex.restSeconds}s",
                                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun SetEntryDialog(
    form: SetEntryForm,
    onWeight: (String) -> Unit,
    onReps: (String) -> Unit,
    onRir: (String) -> Unit,
    onPain: (Boolean) -> Unit,
    onTechnique: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${form.planned.exerciseName} · serie ${form.setIndex + 1}") },
        text = {
            Column {
                form.suggestion?.let { s ->
                    Text(
                        s.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (s.increaseApplied) Emerald else TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = form.weightKg,
                    onValueChange = onWeight,
                    label = { Text("Carico (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = form.reps,
                    onValueChange = onReps,
                    label = { Text("Ripetizioni") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = form.rir,
                    onValueChange = onRir,
                    label = { Text("RIR (ripetizioni in riserva)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = form.painReported, onCheckedChange = onPain)
                    Text("Dolore durante l'esercizio", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = form.poorTechnique, onCheckedChange = onTechnique)
                    Text("Tecnica scorretta", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Salva serie") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun FinishDialog(
    sleep: Int,
    rpe: String,
    painNotes: String,
    onSleep: (Int) -> Unit,
    onRpe: (String) -> Unit,
    onPain: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Termina allenamento") },
        text = {
            Column {
                Text("Qualità del sonno (1-5)", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { v ->
                        FilterChip(selected = sleep == v, onClick = { onSleep(v) },
                            label = { Text("$v") })
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = rpe,
                    onValueChange = onRpe,
                    label = { Text("RPE percepito (1-10)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = painNotes,
                    onValueChange = onPain,
                    label = { Text("Dolori o note (facoltativo)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (painNotes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Dolore segnalato: il carico non verra aumentato automaticamente. " +
                            "Se il dolore persiste, consulta un professionista.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Conferma") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
