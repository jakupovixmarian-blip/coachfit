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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.NumberField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    routineId: Long,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheda") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.save(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }) { Text("Salva") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FitCard {
                    Column {
                        LabeledField("Nome scheda", state.name, { viewModel.setName(it) })
                        Spacer(Modifier.height(6.dp))
                        LabeledField("Descrizione", state.description, { viewModel.setDescription(it) }, singleLine = false)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Aggiungi esercizio")
                        }
                    }
                }
            }
            itemsIndexed(state.items, key = { index, item -> if (item.id != 0L) item.id else -index - 1L }) { index, item ->
                val exercise = state.exercisesById[item.exerciseId]
                FitCard {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(exercise?.name ?: "Esercizio", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${item.targetSets} x ${item.targetRepsMin}-${item.targetRepsMax} @RIR ${item.targetRir}, recupero ${item.restSeconds}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.removeItem(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Rimuovi")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = index }) { Text("Modifica") }
                            OutlinedButton(onClick = { viewModel.move(index, -1) }) { Text("Su") }
                            OutlinedButton(onClick = { viewModel.move(index, 1) }) { Text("Giu") }
                            exercise?.let {
                                OutlinedButton(onClick = { onOpenExercise(it.id) }) { Text("Dettagli") }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            onDismiss = { showPicker = false },
            onPick = { ex ->
                viewModel.addExercise(ex.id)
                showPicker = false
            }
        )
    }

    editing?.let { index ->
        val item = state.items.getOrNull(index)
        if (item == null) { editing = null; return@let }
        var sets by remember(index, item.targetSets) { mutableStateOf(item.targetSets.toString()) }
        var repsMin by remember(index, item.targetRepsMin) { mutableStateOf(item.targetRepsMin.toString()) }
        var repsMax by remember(index, item.targetRepsMax) { mutableStateOf(item.targetRepsMax.toString()) }
        var rir by remember(index, item.targetRir) { mutableStateOf(item.targetRir.toString()) }
        var rest by remember(index, item.restSeconds) { mutableStateOf(item.restSeconds.toString()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Modifica prescrizione") },
            text = {
                Column {
                    NumberField("Serie", sets, { sets = it }, allowDecimal = false)
                    NumberField("Ripetizioni minime", repsMin, { repsMin = it }, allowDecimal = false)
                    NumberField("Ripetizioni massime", repsMax, { repsMax = it }, allowDecimal = false)
                    NumberField("RIR target", rir, { rir = it }, allowDecimal = false)
                    NumberField("Recupero (secondi)", rest, { rest = it }, allowDecimal = false)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateItem(
                        index,
                        item.copy(
                            targetSets = sets.toIntOrNull() ?: item.targetSets,
                            targetRepsMin = repsMin.toIntOrNull() ?: item.targetRepsMin,
                            targetRepsMax = repsMax.toIntOrNull() ?: item.targetRepsMax,
                            targetRir = rir.toIntOrNull() ?: item.targetRir,
                            restSeconds = rest.toIntOrNull() ?: item.restSeconds
                        )
                    )
                    editing = null
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Annulla") } }
        )
    }
}
