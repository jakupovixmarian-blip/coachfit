package com.fitcoach.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceInteractive
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { viewModel.generateWorkout() }, enabled = !state.generating) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.generating) "Generazione..." else "Genera scheda")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { navController.navigate(com.fitcoach.app.ui.navigation.Routes.CUSTOM_WORKOUT) }) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crea scheda personalizzata")
                        }
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
                SectionHeader("Allenamento", plan.splitType.label, modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = { navController.navigate(com.fitcoach.app.ui.navigation.Routes.CUSTOM_WORKOUT) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Crea scheda personalizzata", tint = TextSecondary)
                    }
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

        if (plan.mesocycles.isNotEmpty()) {
            item {
                val meso = plan.mesocycles.first()
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SectionHeader("Mesociclo", "${meso.weekCount} settimane · ${meso.progressionModel}")
                        }
                        Text(
                            meso.deloadWeekIndex?.let { "Deload S$it" } ?: "Deload non impostato",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        IconButton(onClick = { viewModel.openEditMesocycle(meso) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Modifica mesociclo", tint = TextSecondary)
                        }
                    }
                }
            }
        }

        if (state.plans.isNotEmpty()) {
            item {
                SectionCard {
                    Column {
                        SectionHeader("Schede salvate", "Scegli quale avviare")
                        Spacer(Modifier.height(8.dp))
                        state.plans.forEach { saved ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        saved.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${saved.splitType.label} · ${saved.days.count { !it.isRestDay }} giorni",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                if (saved.isActive) {
                                    Pill("Attiva", Emerald)
                                } else {
                                    TextButton(onClick = { viewModel.activatePlan(saved.id) }) { Text("Attiva") }
                                }
                                IconButton(onClick = {
                                    navController.navigate(com.fitcoach.app.ui.navigation.Routes.CUSTOM_WORKOUT_EDIT.replace("{planId}", saved.id.toString()))
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Modifica scheda", tint = TextSecondary)
                                }
                                IconButton(onClick = { viewModel.duplicatePlan(saved) }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Duplica scheda", tint = TextSecondary)
                                }
                                IconButton(onClick = { viewModel.openRenamePlan(saved) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Rinomina scheda", tint = TextSecondary)
                                }
                                IconButton(onClick = { viewModel.askDeletePlan(saved) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Elimina scheda", tint = TextSecondary)
                                }
                            }
                        }
                    }
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
                    SectionHeader("Giorno selezionato", "Scegli il giorno da allenare")
                    val todayIndex = LocalDate.now().dayOfWeek.value - 1
                    val todayIsRest = plan.days.firstOrNull { it.dayIndex == todayIndex }?.isRestDay != false
                    if (todayIsRest && state.selectedDayIndex != todayIndex) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Oggi non e previsto allenamento: e mostrato il prossimo giorno utile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        plan.days.sortedBy { it.dayIndex }.forEach { d ->
                            val selected = d.dayIndex == state.selectedDayIndex
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectDay(d.dayIndex) },
                                enabled = !d.isRestDay,
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(weekdayNames.getOrElse(d.dayIndex) { "?" }.take(3))
                                        Text(
                                            if (d.isRestDay) "Riposo" else "${d.totalSets}s",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader(
                        "${weekdayNames.getOrElse(state.selectedDayIndex) { "" }} · " +
                            "${LocalDate.ofEpochDay(state.selectedEpochDay)}",
                        today?.title ?: "Giorno di riposo"
                    )
                    Spacer(Modifier.height(10.dp))
                    if (today == null) {
                        Text(
                            "Nessuna seduta programmata per il giorno selezionato. Puoi recuperare " +
                                "oppure iniziare una sessione libera.",
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
                        Text(
                            "${today.totalSets} serie · ${today.exercises.size} esercizi. " +
                                "Avvia la sessione per registrare serie, ripetizioni e carichi.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.startSession() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Avvia sessione di ${weekdayNames.getOrElse(state.selectedDayIndex) { "oggi" }}")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.startWeightSetup() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Imposta carichi e ripetizioni")
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

        if (today != null) {
            today.exercises.forEach { planned ->
                item {
                    ExerciseCard(
                        planned = planned,
                        loggedSets = viewModel.sessionSetsFor(planned.exerciseId),
                        lastPerformance = state.lastPerformances[planned.exerciseId],
                        expanded = state.expandedExerciseId == planned.exerciseId,
                        restRemaining = if (state.restExerciseId == planned.exerciseId)
                            state.restRemainingSeconds else 0,
                        restTotal = if (state.restExerciseId == planned.exerciseId)
                            state.restTotalSeconds else 0,
                        restReason = state.restReason,
                        timerSeconds = state.exerciseTimers[planned.exerciseId] ?: 0,
                        timerRunning = planned.exerciseId in state.exerciseTimerRunning,
                        onToggle = { viewModel.toggleExercise(planned.exerciseId) },
                        onToggleTimer = { viewModel.toggleExerciseTimer(planned.exerciseId) },
                        onResetTimer = { viewModel.resetExerciseTimer(planned.exerciseId) },
                        onSkipRest = { viewModel.skipRest() },
                        onAddRest = { viewModel.addRestTime(it) },
                        onManualRest = { viewModel.startRestWithSeconds(it) },
                        onLogSet = { viewModel.logSetFor(planned) },
                        onSubstitute = { viewModel.openSubstitution(planned) },
                        onRemove = { viewModel.removeExercise(planned) },
                        onEdit = { viewModel.openEdit(planned) }
                    )
                }
            }

            if (session?.status == com.fitcoach.app.domain.model.SessionStatus.IN_PROGRESS) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openFinishDialog() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Termina allenamento")
                        }
                        OutlinedButton(
                            onClick = { viewModel.abandonSession() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Abbandona")
                        }
                    }
                }
                if (!state.resting) {
                    item {
                        OutlinedButton(
                            onClick = { viewModel.startManualRest() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Avvia recupero")
                        }
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
                    SectionHeader("Settimana", "Tocca un giorno per aprirlo")
                    Spacer(Modifier.height(8.dp))
                    plan.days.sortedBy { it.dayIndex }.forEach { day ->
                        WeekDayCard(
                            day = day,
                            selected = day.dayIndex == state.selectedDayIndex,
                            onSelect = { if (!day.isRestDay) viewModel.selectDay(day.dayIndex) }
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

    if (state.setupWeights) {
        state.setupQueue.getOrNull(state.setupIndex)?.let { planned ->
            WeightSetupDialog(
                planned = planned,
                position = state.setupIndex + 1,
                total = state.setupQueue.size,
                onSave = { weight, reps -> viewModel.saveSetupWeight(weight, reps) },
                onSkip = { viewModel.skipSetupStep() },
                onDismiss = { viewModel.closeWeightSetup() }
            )
        }
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

    state.editingMesocycle?.let { meso ->
        MesocycleDialog(
            weeks = state.mesocycleWeeksInput,
            deload = state.mesocycleDeloadInput,
            progression = state.mesocycleProgressionInput,
            onChange = viewModel::updateMesocycleFields,
            onConfirm = viewModel::saveMesocycle,
            onDismiss = viewModel::closeEditMesocycle
        )
    }

    state.editingPlan?.let { planToEdit ->
        PlanNameDialog(
            title = "Rinomina scheda",
            initialName = planToEdit.name,
            onConfirm = viewModel::renamePlan,
            onDismiss = viewModel::closeRenamePlan
        )
    }
    state.deletingPlan?.let { planToDelete ->
        AlertDialog(
            onDismissRequest = viewModel::closeDeletePlan,
            title = { Text("Eliminare la scheda?") },
            text = { Text("La scheda \"${planToDelete.name}\" e la sua struttura verranno eliminate. Lo storico delle sessioni restera invariato.") },
            confirmButton = {
                TextButton(onClick = viewModel::deletePlan) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = viewModel::closeDeletePlan) { Text("Annulla") } }
        )
    }

    SnackbarHost(snackbar)
}

@Composable
private fun MesocycleDialog(
    weeks: String,
    deload: String,
    progression: String,
    onChange: (String, String, String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica mesociclo") },
        text = {
            Column {
                OutlinedTextField(weeks, { onChange(it, deload, progression) }, label = { Text("Durata (settimane)") }, singleLine = true)
                OutlinedTextField(deload, { onChange(weeks, it, progression) }, label = { Text("Settimana deload (opzionale)") }, singleLine = true)
                OutlinedTextField(progression, { onChange(weeks, deload, it) }, label = { Text("Modello di progressione") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Salva nuova versione") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun PlanNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome scheda") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun WeekDayCard(day: WorkoutDay, selected: Boolean, onSelect: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dayLabel = weekdayNames.getOrElse(day.dayIndex) { "Giorno ${day.dayIndex + 1}" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "$dayLabel · ${if (day.isRestDay) "Riposo" else day.title}" +
                        if (selected) "  ·  in visione" else "",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Emerald else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (day.isRestDay) "Nessuna seduta programmata"
                    else buildString {
                        append("${day.totalSets} serie · ${day.exercises.size} esercizi")
                        if (day.muscleGroups.isNotEmpty()) {
                            append(" · ")
                            append(day.muscleGroups.joinToString { it.label })
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (!day.isRestDay) {
                IconButton(onClick = onSelect) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Apri $dayLabel",
                        tint = if (selected) Emerald else TextSecondary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Comprimi" else "Espandi",
                    tint = Emerald
                )
            }
        }
        if (expanded && !day.isRestDay) {
            Spacer(Modifier.height(8.dp))
            day.exercises.sortedBy { it.orderIndex }.forEachIndexed { index, exercise ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${index + 1}. ${exercise.exerciseName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${exercise.primaryMuscle.label} · " +
                                "rec ${exercise.restSeconds}s · RIR ${exercise.targetRir}" +
                                if (exercise.technique.isNotBlank()) " · ${exercise.technique}" else "" +
                                if (exercise.intensityTechnique != IntensityTechnique.NONE)
                                    " · ${exercise.intensityTechnique.label}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Pill(
                        "${exercise.sets}×${exercise.minReps}-${exercise.maxReps}",
                        CarbsColor
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
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
                    var dayExpanded by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !day.isRestDay) { dayExpanded = !dayExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${weekdayNames[day.dayIndex]} · ${day.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (day.isRestDay) "Giorno di riposo"
                                    else "${day.totalSets} serie · ${day.exercises.size} esercizi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (!day.isRestDay) {
                                Icon(
                                    imageVector = if (dayExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (dayExpanded) "Comprimi" else "Espandi",
                                    tint = Emerald
                                )
                            }
                        }
                        if (dayExpanded && !day.isRestDay) {
                            day.exercises.sortedBy { it.orderIndex }.forEachIndexed { i, ex ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${i + 1}. ${ex.exerciseName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${ex.sets}×${ex.minReps}-${ex.maxReps}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CarbsColor
                                    )
                                }
                            }
                        }
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
    expanded: Boolean,
    restRemaining: Int,
    restTotal: Int,
    restReason: String,
    timerSeconds: Int,
    timerRunning: Boolean,
    onToggle: () -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSkipRest: () -> Unit,
    onAddRest: (Int) -> Unit,
    onManualRest: (Int) -> Unit,
    onLogSet: () -> Unit,
    onSubstitute: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    SectionCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
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
                if (timerSeconds > 0) {
                    Spacer(Modifier.width(8.dp))
                    Pill(formatRest(timerSeconds), if (timerRunning) Emerald else TextSecondary)
                }
                planned.targetWeightKg?.let {
                    Spacer(Modifier.width(6.dp))
                    Pill("%.1f kg".format(it), CarbsColor)
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Chiudi esercizio" else "Apri esercizio",
                    tint = TextSecondary
                )
            }

            if (!expanded) {
                Text(
                    "Tocca per serie, ripetizioni, carichi e cronometro",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
                return@Column
            }

            if (planned.selectionReason.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(planned.selectionReason, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            // La sezione deve comparire quando la tecnica c'e', non solo quando
            // c'e' anche la motivazione: altrimenti una tecnica applicata dalla
            // chat resta invisibile nella scheda.
            if (planned.intensityTechnique != IntensityTechnique.NONE) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tecnica: ${planned.intensityTechnique.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Emerald
                )
                if (planned.techniqueReason.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        planned.techniqueReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
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

            Spacer(Modifier.height(10.dp))
            SetGrid(planned = planned, loggedSets = loggedSets)

            Spacer(Modifier.height(10.dp))
            ExerciseTimerRow(
                seconds = timerSeconds,
                running = timerRunning,
                onToggle = onToggleTimer,
                onReset = onResetTimer
            )

            if (restTotal > 0) {
                Spacer(Modifier.height(10.dp))
                RestTimerInline(
                    remaining = restRemaining,
                    total = restTotal,
                    reason = restReason,
                    onSkip = onSkipRest,
                    onAdd = onAddRest,
                    onSetManual = onManualRest
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onLogSet() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Serie ${loggedSets.size + 1}")
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

/**
 * Riquadri serie / ripetizioni / carico.
 *
 * Le serie pianificate restano visibili anche prima di essere registrate: il
 * carico proposto dal motore e' un suggerimento, quello effettivo compare solo
 * dopo che l'utente lo registra. Una serie non registrata non mostra un carico.
 */
@Composable
private fun SetGrid(planned: PlannedExercise, loggedSets: List<WorkoutSet>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SetGridCell("Serie", "${loggedSets.size}/${planned.sets}", Modifier.weight(1f))
            SetGridCell("Rep", "${planned.minReps}-${planned.maxReps}", Modifier.weight(1f))
            SetGridCell("Carico", planned.targetWeightKg?.let { "%.1f kg".format(it) } ?: "da definire",
                Modifier.weight(1f))
            SetGridCell("RIR", planned.targetRir.toString(), Modifier.weight(0.7f))
        }
        if (loggedSets.isNotEmpty()) {
            loggedSets.sortedBy { it.setIndex }.forEach { s ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Serie ${s.setIndex + 1}: ${"%.1f".format(s.weightKg)} kg × ${s.actualReps}" +
                            (s.rir?.let { " @RIR $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.Filled.Check, contentDescription = "Serie registrata", tint = Emerald,
                        modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SetGridCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(FitRadius.sm))
            .background(FitSurfaceInteractive)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExerciseTimerRow(
    seconds: Int,
    running: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FitRadius.sm))
            .background(FitSurfaceInteractive)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Timer, contentDescription = null,
            tint = if (running) Emerald else TextSecondary)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Cronometro esercizio", style = MaterialTheme.typography.labelSmall,
                color = TextSecondary)
            Text(formatRest(seconds), style = MaterialTheme.typography.titleMedium,
                color = if (running) Emerald else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onReset, enabled = seconds > 0) { Text("Azzera") }
        Button(onClick = onToggle) { Text(if (running) "Pausa" else "Avvia") }
    }
}

@Composable
private fun RestTimerInline(
    remaining: Int,
    total: Int,
    reason: String,
    onSkip: () -> Unit,
    onAdd: (Int) -> Unit,
    onSetManual: (Int) -> Unit
) {
    val progress = if (total > 0) (total - remaining).toFloat() / total else 0f
    var manualInput by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FitRadius.sm))
            .background(FitSurfaceInteractive)
            .padding(12.dp)
    ) {
        Text(reason.ifBlank { "Recupero" }, style = MaterialTheme.typography.labelSmall,
            color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(formatRest(remaining), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = Emerald)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Emerald
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onAdd(30) }, modifier = Modifier.weight(1f)) { Text("+30s") }
            OutlinedButton(onClick = { onAdd(60) }, modifier = Modifier.weight(1f)) { Text("+1 min") }
            Button(onClick = onSkip, modifier = Modifier.weight(1f)) { Text("Salta") }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it.filter { c -> c.isDigit() } },
                label = { Text("Minuti") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val minutes = manualInput.toIntOrNull()
                    if (minutes != null) onSetManual(minutes * 60)
                    manualInput = ""
                },
                enabled = (manualInput.toIntOrNull() ?: 0) > 0
            ) { Text("Avvia") }
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
private fun WeightSetupDialog(
    planned: PlannedExercise,
    position: Int,
    total: Int,
    onSave: (Double?, Int?) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf(planned.targetWeightKg?.let { "%.1f".format(it) } ?: "") }
    var reps by remember { mutableStateOf(planned.targetReps?.toString() ?: planned.minReps.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${planned.exerciseName} ($position/$total)") },
        text = {
            Column {
                Text(
                    "Imposta il carico e le ripetizioni previsti. Potrai modificarli in ogni " +
                        "serie durante l'allenamento.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Carico (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it.filter { c -> c.isDigit() } },
                    label = { Text("Ripetizioni") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(weight.replace(',', '.').toDoubleOrNull(), reps.toIntOrNull())
            }) { Text(if (position == total) "Fine" else "Avanti") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) { Text("Salta") }
                TextButton(onClick = onDismiss) { Text("Chiudi") }
            }
        }
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
