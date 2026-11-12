package dev.fitcoach.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.timer.RestTimerService
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: WorkoutSessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val timerState by RestTimerService.state.collectAsStateWithLifecycle()
    var showFinish by remember { mutableStateOf(false) }
    var showAddExercise by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }
    LaunchedEffect(state.session?.notes) {
        state.session?.notes?.let { if (notes.isEmpty()) notes = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.session?.name ?: "Seduta") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.saveNotes(notes); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (timerState.active) {
                    FitCard {
                        Column {
                            Text("Recupero: ${timerState.remainingSeconds}s", style = MaterialTheme.typography.titleMedium)
                            Text(timerState.exerciseName, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { RestTimerService.skip(context) }) { Text("Salta") }
                                OutlinedButton(
                                    onClick = {
                                        if (timerState.running) RestTimerService.pause(context)
                                        else RestTimerService.resume(context)
                                    }
                                ) { Text(if (timerState.running) "Pausa" else "Riprendi") }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                FitCard {
                    Column {
                        val progress = if (state.plannedTotalSets > 0) {
                            state.effectiveSets.toFloat() / state.plannedTotalSets
                        } else 0f
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Serie effettive", style = MaterialTheme.typography.bodyMedium)
                            Text("${state.effectiveSets}/${state.plannedTotalSets}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow("Volume totale", Format.kg(state.totalVolumeKg))
                        KeyValueRow("Esercizi", state.blocks.size.toString())
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showAddExercise = true }, modifier = Modifier.weight(1f)) {
                                Text("Aggiungi esercizio")
                            }
                            Button(onClick = { showFinish = true }, modifier = Modifier.weight(1f)) {
                                Text("Termina")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.blocks, key = { it.exercise.id }) { block ->
                    FitCard {
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${block.exercise.primaryMuscle.label} - ${block.exercise.equipment.label}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.addSet(block) }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Aggiungi serie")
                                }
                            }

                            block.routineExercise?.let { re ->
                                Text(
                                    "Obiettivo ${re.targetSets}x${re.targetRepsMin}-${re.targetRepsMax} @RIR ${re.targetRir} - recupero ${re.restSeconds}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            block.lastPerformance?.let { last ->
                                val best = last.sets.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 1) }
                                if (best != null) {
                                    Text(
                                        "Ultima volta: ${Format.kg(best.weightKg ?: 0.0)} x ${best.reps ?: 0}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            block.suggestionReason?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(6.dp))

                            block.sets.forEach { set ->
                                SetRow(
                                    set = set,
                                    onUpdate = { w, r, rir, pain -> viewModel.updateSet(set, w, r, rir, pain) },
                                    onToggle = { viewModel.toggleCompleted(set) },
                                    onDelete = { viewModel.deleteSet(set) },
                                    onRest = {
                                        RestTimerService.start(
                                            context,
                                            block.routineExercise?.restSeconds ?: 90,
                                            block.exercise.id,
                                            block.exercise.name
                                        )
                                    }
                                )
                            }

                            if (block.sets.isEmpty()) {
                                Text(
                                    "Nessuna serie. Aggiungi la prima con +.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Volume esercizio: ${Format.kg(block.volumeKg)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showFinish) {
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text("Termina seduta") },
            text = {
                Column {
                    LabeledField("Note della seduta", notes, { notes = it }, singleLine = false)
                    Spacer(Modifier.height(6.dp))
                    if (state.hasRecordedWork) {
                        Text(
                            "La seduta viene salvata con ${state.effectiveSets} serie effettive e volume ${Format.kg(state.totalVolumeKg)}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "Non hai registrato nessuna serie. Una seduta senza serie non viene conteggiata nei progressi: " +
                                "chiudendola verra salvata come seduta vuota.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.finishSession(notes, force = !state.hasRecordedWork)
                    showFinish = false
                }) { Text(if (state.hasRecordedWork) "Salva e chiudi" else "Chiudi come seduta vuota") }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = {
                        viewModel.discardSession()
                        showFinish = false
                    }) { Text("Elimina seduta") }
                    TextButton(onClick = { showFinish = false }) { Text("Continua") }
                }
            }
        )
    }

    if (showAddExercise) {
        ExercisePickerDialog(
            onDismiss = { showAddExercise = false },
            onPick = { exercise ->
                viewModel.addExercise(exercise.id)
                showAddExercise = false
            }
        )
    }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2000)
            viewModel.dismissMessage()
        }
    }
}

@Composable
private fun SetRow(
    set: LoggedSet,
    onUpdate: (Double?, Int?, Int?, Boolean) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onRest: () -> Unit
) {
    var weight by remember(set.id, set.weightKg) { mutableStateOf(set.weightKg?.toString().orEmpty()) }
    var reps by remember(set.id, set.reps) { mutableStateOf(set.reps?.toString().orEmpty()) }
    var rir by remember(set.id, set.rir) { mutableStateOf(set.rir?.toString().orEmpty()) }
    var pain by remember(set.id, set.pain) { mutableStateOf(set.pain) }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = set.completed, onCheckedChange = { onToggle() })
            Text("Serie ${set.setIndex}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onRest) { Text("T", style = MaterialTheme.typography.labelLarge) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Elimina serie") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberField(
                label = "kg",
                value = weight,
                onValueChange = {
                    weight = it
                    onUpdate(it.toDoubleOrNull(), reps.toIntOrNull(), rir.toIntOrNull(), pain)
                },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "rip",
                value = reps,
                onValueChange = {
                    reps = it
                    onUpdate(weight.toDoubleOrNull(), it.toIntOrNull(), rir.toIntOrNull(), pain)
                },
                modifier = Modifier.weight(1f),
                allowDecimal = false
            )
            NumberField(
                label = "RIR",
                value = rir,
                onValueChange = {
                    rir = it
                    onUpdate(weight.toDoubleOrNull(), reps.toIntOrNull(), it.toIntOrNull(), pain)
                },
                modifier = Modifier.weight(1f),
                allowDecimal = false
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = pain, onCheckedChange = {
                pain = it
                onUpdate(weight.toDoubleOrNull(), reps.toIntOrNull(), rir.toIntOrNull(), it)
            })
            Text("Dolore o disagio durante la serie", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ExercisePickerDialog(
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
    viewModel: ExerciseListViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }

    val filtered = exercises.filter { ex ->
        (query.isBlank() || ex.name.contains(query, ignoreCase = true)) &&
            (selectedMuscle == null || ex.primaryMuscle.label == selectedMuscle)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scegli esercizio") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cerca") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.height(320.dp)) {
                    items(filtered, key = { it.id }) { ex ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(ex.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${ex.primaryMuscle.label} - ${ex.equipment.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { onPick(ex) }) { Text("Aggiungi") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}
