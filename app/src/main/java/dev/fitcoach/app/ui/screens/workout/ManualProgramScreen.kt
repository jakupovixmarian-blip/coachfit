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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.SectionTitle

/**
 * Manual builder for a personal program, fully independent from the AI. It is the
 * only place where the user types sets and reps themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualProgramScreen(
    onBack: () -> Unit,
    onOpenRoutine: (Long) -> Unit,
    viewModel: ManualProgramViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPickerForDay by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crea il tuo programma") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FitCard {
                    Column {
                        SectionTitle("Programma personale")
                        Text(
                            "Qui decidi tu ogni numero: giornate, esercizi, serie, ripetizioni e recuperi. " +
                                "Non usa l'AI e non applica schemi automatici.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        LabeledField("Nome del programma", state.programName, { viewModel.setProgramName(it) })
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.addDay() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Aggiungi giornata")
                        }
                    }
                }
            }

            itemsIndexed(state.days) { dayIndex, day ->
                FitCard {
                    Column {
                        LabeledField("Nome giornata", day.label, { viewModel.setDayLabel(dayIndex, it) })
                        Spacer(Modifier.height(8.dp))
                        day.exercises.forEachIndexed { exerciseIndex, item ->
                            val exercise = state.exercisesById[item.exerciseId]
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(exercise?.name ?: "Esercizio", style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = { viewModel.removeExercise(dayIndex, exerciseIndex) }) {
                                        Text("Rimuovi")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    NumberField("Serie", item.sets.toString(), { v ->
                                        v.toIntOrNull()?.let { value ->
                                            viewModel.updateExercise(dayIndex, exerciseIndex) { it.copy(sets = value) }
                                        }
                                    }, Modifier.weight(1f), allowDecimal = false)
                                    NumberField("Rip min", item.repsMin.toString(), { v ->
                                        v.toIntOrNull()?.let { value ->
                                            viewModel.updateExercise(dayIndex, exerciseIndex) { it.copy(repsMin = value) }
                                        }
                                    }, Modifier.weight(1f), allowDecimal = false)
                                    NumberField("Rip max", item.repsMax.toString(), { v ->
                                        v.toIntOrNull()?.let { value ->
                                            viewModel.updateExercise(dayIndex, exerciseIndex) { it.copy(repsMax = value) }
                                        }
                                    }, Modifier.weight(1f), allowDecimal = false)
                                }
                                NumberField("Recupero (secondi)", item.restSeconds.toString(), { v ->
                                    v.toIntOrNull()?.let { value ->
                                        viewModel.updateExercise(dayIndex, exerciseIndex) { it.copy(restSeconds = value) }
                                    }
                                }, allowDecimal = false)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { showPickerForDay = dayIndex },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Aggiungi esercizio") }
                        TextButton(onClick = { viewModel.removeDay(dayIndex) }) { Text("Elimina giornata") }
                    }
                }
            }

            item {
                Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Salva programma")
                }
            }
            state.message?.let { message ->
                item {
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    showPickerForDay?.let { dayIndex ->
        ExercisePickerDialog(
            onDismiss = { showPickerForDay = null },
            onPick = { exercise ->
                viewModel.addExercise(dayIndex, exercise.id)
                showPickerForDay = null
            }
        )
    }
}
