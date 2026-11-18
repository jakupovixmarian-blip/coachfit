package com.fitcoach.app.ui.workout

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.TextSecondary

private val weekdayNames = listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")

@Composable
fun CustomWorkoutScreen(
    navController: NavHostController,
    editPlanId: Long? = null,
    viewModel: CustomWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(editPlanId) {
        if (editPlanId != null) viewModel.load(editPlanId)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (state.saved) {
        LaunchedEffect(Unit) {
            // Torna alla schermata Allenamento (dove la nuova scheda e gia attiva).
            navController.popBackStack(
                com.fitcoach.app.ui.navigation.Routes.WORKOUT,
                inclusive = false
            )
        }
    }

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    var libraryForDay by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (editPlanId == null) "Crea scheda personalizzata" else "Modifica scheda",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Scegli giorni, esercizi, serie, ripetizioni e recuperi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Struttura", "${state.days.count { !it.isRestDay }} giorni di allenamento")
                    Spacer(Modifier.height(8.dp))
                    state.days.forEach { day ->
                        DayEditor(
                            day = day,
                            expanded = state.editingDayIndex == day.dayIndex,
                            onToggleExpand = {
                                if (state.editingDayIndex == day.dayIndex) viewModel.stopEditingDay()
                                else viewModel.startEditingDay(day.dayIndex)
                            },
                            onToggleRest = { viewModel.toggleRestDay(day.dayIndex) },
                            onTitle = { viewModel.updateDayTitle(day.dayIndex, it) },
                            onAddExercise = { libraryForDay = day.dayIndex },
                            onRemove = { viewModel.removeExercise(day.dayIndex, it) },
                            onMove = { index, delta -> viewModel.moveExercise(day.dayIndex, index, delta) },
                            onUpdate = { index, sets, min, max, rest, rir ->
                                viewModel.updateExercise(day.dayIndex, index, sets, min, max, rest, rir)
                            }
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.save() },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.saving) "Salvataggio..." else if (editPlanId == null) "Salva scheda come attiva" else "Salva modifiche")
            }
        }
    }

    libraryForDay?.let { dayIndex ->
        ExercisePickerDialog(
            exercises = state.libraryFiltered,
            query = state.libraryQuery,
            onQueryChange = viewModel::updateLibraryQuery,
            onPick = { exercise ->
                viewModel.addExerciseToDay(dayIndex, exercise)
            },
            onDismiss = { libraryForDay = null }
        )
    }

    SnackbarHost(snackbar)
}

@Composable
private fun DayEditor(
    day: DraftDay,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleRest: () -> Unit,
    onTitle: (String) -> Unit,
    onAddExercise: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onUpdate: (Int, Int?, Int?, Int?, Int?, Int?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    weekdayNames.getOrElse(day.dayIndex) { "Giorno ${day.dayIndex + 1}" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (day.isRestDay) "Riposo"
                    else "${day.exercises.size} esercizi · ${day.totalSets} serie",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (!day.isRestDay) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Comprimi" else "Espandi",
                    tint = Emerald
                )
            }
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = day.title,
                onValueChange = onTitle,
                label = { Text("Titolo del giorno") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = day.isRestDay,
                    onClick = onToggleRest,
                    label = { Text("Giorno di riposo") }
                )
            }
            Spacer(Modifier.height(8.dp))
            day.exercises.forEachIndexed { index, exercise ->
                ExerciseRowEditor(
                    index = index,
                    exercise = exercise,
                    canMoveUp = index > 0,
                    canMoveDown = index < day.exercises.size - 1,
                    onRemove = { onRemove(index) },
                    onMoveUp = { onMove(index, -1) },
                    onMoveDown = { onMove(index, 1) },
                    onUpdate = { s, mn, mx, r, rir -> onUpdate(index, s, mn, mx, r, rir) }
                )
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Aggiungi esercizio")
            }
        }
    }
}

@Composable
private fun ExerciseRowEditor(
    index: Int,
    exercise: DraftExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdate: (Int?, Int?, Int?, Int?, Int?) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${index + 1}. ${exercise.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Pill(exercise.primaryMuscle.label, Emerald)
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Su")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Giù")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Rimuovi")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Serie", exercise.sets, Modifier.weight(1f)) { onUpdate(it, null, null, null, null) }
            NumberField("Min rep", exercise.minReps, Modifier.weight(1f)) { onUpdate(null, it, null, null, null) }
            NumberField("Max rep", exercise.maxReps, Modifier.weight(1f)) { onUpdate(null, null, it, null, null) }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Recupero (s)", exercise.restSeconds, Modifier.weight(1f)) { onUpdate(null, null, null, it, null) }
            NumberField("RIR", exercise.targetRir, Modifier.weight(1f)) { onUpdate(null, null, null, null, it) }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }.take(4)
            text = filtered
            filtered.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    query: String,
    onQueryChange: (String) -> Unit,
    onPick: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scegli un esercizio") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Cerca") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (exercises.isEmpty()) {
                    Text(
                        "Nessun esercizio trovato.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    LazyColumn(Modifier.height(360.dp)) {
                        items(exercises, key = { it.id }) { exercise ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(exercise) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        exercise.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${exercise.primaryMuscle.label} · ${exercise.category.name.lowercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Icon(Icons.Filled.Add, contentDescription = "Aggiungi", tint = Emerald)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}
