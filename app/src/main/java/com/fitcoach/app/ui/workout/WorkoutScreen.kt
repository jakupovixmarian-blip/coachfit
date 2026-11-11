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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
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
import com.fitcoach.app.domain.model.PlannedExercise
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
                IconButton(onClick = { viewModel.generateWorkout() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Rigenera", tint = Emerald)
                }
            }
        }

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
                            "Nessuna seduta programmata per oggi. Recupera e preparati alla prossima.",
                            style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                        )
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
                                "${session.completedSets} serie registrate · volume ${session.totalVolumeKg.toInt()} kg",
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
                        sessionActive = viewModel.isSessionActive(),
                        onAddSet = { index -> viewModel.openSetForm(planned, index) }
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

    SnackbarHost(snackbar)
}

@Composable
private fun ExerciseCard(
    planned: PlannedExercise,
    loggedSets: List<WorkoutSet>,
    sessionActive: Boolean,
    onAddSet: (Int) -> Unit
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
            OutlinedButton(
                onClick = { onAddSet(loggedSets.size) },
                enabled = sessionActive
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (sessionActive) "Registra serie ${loggedSets.size + 1}" else "Avvia la sessione")
            }
        }
    }
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
