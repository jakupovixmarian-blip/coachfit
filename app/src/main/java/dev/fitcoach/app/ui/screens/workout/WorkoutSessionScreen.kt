package dev.fitcoach.app.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.timer.RestTimerService
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.GhostButton
import dev.fitcoach.app.ui.components.GradientButton
import dev.fitcoach.app.ui.components.HeroCard
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.NeonBadge
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.ProgressTrack
import dev.fitcoach.app.ui.components.RestTimerRing
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.Tag
import dev.fitcoach.app.ui.theme.BgDeep
import dev.fitcoach.app.ui.theme.CardNavy
import dev.fitcoach.app.ui.theme.GradientCyan
import dev.fitcoach.app.ui.theme.GradientGreen
import dev.fitcoach.app.ui.theme.GradientViolet
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.NeonGreen
import dev.fitcoach.app.ui.theme.NeonMagenta
import dev.fitcoach.app.ui.theme.NeonOrange
import dev.fitcoach.app.ui.theme.NeonViolet
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextLow
import dev.fitcoach.app.ui.theme.TextMid

/**
 * Seduta in corso nel layout del mockup: header con titolo e avanzamento, timer di
 * recupero in card hero con anello a gradiente, statistiche in pill e una card per
 * esercizio con tabella serie compatta.
 */
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

    val setProgress = if (state.plannedTotalSets > 0) {
        state.effectiveSets.toFloat() / state.plannedTotalSets
    } else 0f

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardNavy)
                        .border(1.dp, StrokeNavy, CircleShape)
                        .clickable { viewModel.saveNotes(notes); onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.session?.name ?: "Seduta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "${state.effectiveSets}/${state.plannedTotalSets} serie · ${Format.kg(state.totalVolumeKg)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLow
                    )
                }
                NeonBadge(icon = Icons.Filled.FitnessCenter, tint = NeonViolet, size = 40.dp)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (timerState.active || timerState.stopwatchActive) {
                    HeroCard {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle(
                                    if (timerState.stopwatchActive) "Cronometro" else "Recupero",
                                    Modifier.weight(1f)
                                )
                                Tag(
                                    if (timerState.running) "In corso" else "In pausa",
                                    color = if (timerState.running) NeonGreen else NeonOrange
                                )
                            }
                            if (timerState.stopwatchActive) {
                                RestTimerRing(
                                    remainingSeconds = timerState.elapsedSeconds,
                                    totalSeconds = timerState.elapsedSeconds,
                                    size = 132.dp
                                )
                            } else {
                                RestTimerRing(
                                    remainingSeconds = timerState.remainingSeconds,
                                    totalSeconds = timerState.totalSeconds
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                timerState.exerciseName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextMid
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TimerControl(
                                    text = "-15s",
                                    enabled = !timerState.stopwatchActive,
                                    modifier = Modifier.weight(1f)
                                ) { RestTimerService.adjust(context, -15) }
                                TimerControl(
                                    text = "+15s",
                                    enabled = !timerState.stopwatchActive,
                                    modifier = Modifier.weight(1f)
                                ) { RestTimerService.adjust(context, 15) }
                                TimerControl(
                                    text = if (timerState.running) "Pausa" else "Riprendi",
                                    icon = if (timerState.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (timerState.running) RestTimerService.pause(context)
                                    else RestTimerService.resume(context)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            GradientButton(
                                text = "Salta recupero",
                                icon = Icons.Filled.Timer,
                                gradient = GradientViolet,
                                onClick = { RestTimerService.skip(context) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                FitCard {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Avanzamento seduta",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(setProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = NeonCyan
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ProgressTrack(progress = setProgress, color = NeonCyan)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SessionStat(
                                "Volume",
                                Format.kg(state.totalVolumeKg),
                                NeonCyan,
                                Modifier.weight(1f)
                            )
                            SessionStat("Esercizi", state.blocks.size.toString(), NeonViolet, Modifier.weight(1f))
                            SessionStat("Serie", state.effectiveSets.toString(), NeonGreen, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GhostButton(
                                text = "Aggiungi",
                                icon = Icons.Filled.Add,
                                onClick = { showAddExercise = true },
                                modifier = Modifier.weight(1f)
                            )
                            GradientButton(
                                text = "Termina",
                                onClick = { showFinish = true },
                                gradient = GradientGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.blocks, key = { it.exercise.id }) { block ->
                    FitCard {
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        block.exercise.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Tag(block.exercise.primaryMuscle.label, color = NeonCyan)
                                        Tag(block.exercise.equipment.label, color = NeonViolet)
                                    }
                                }
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.12f))
                                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape)
                                        .clickable { viewModel.addSet(block) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Aggiungi serie",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            block.routineExercise?.let { re ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Obiettivo ${re.targetSets}x${re.targetRepsMin}-${re.targetRepsMax} @RIR ${re.targetRir} · recupero ${block.restSeconds}s",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMid
                                )
                            }
                            block.lastPerformance?.let { last ->
                                val best = last.sets.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 1) }
                                if (best != null) {
                                    Text(
                                        "Ultima volta ${Format.kg(best.weightKg ?: 0.0)} x ${best.reps ?: 0}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextLow
                                    )
                                }
                            }
                            block.suggestionReason?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.labelMedium, color = NeonGreen)
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(Color(0xFF10151F))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SERIE", style = MaterialTheme.typography.labelSmall, color = TextLow)
                                Text("KG / RIP / RIR", style = MaterialTheme.typography.labelSmall, color = TextLow)
                                Text("AZIONI", style = MaterialTheme.typography.labelSmall, color = TextLow)
                            }
                            Spacer(Modifier.height(6.dp))

                            block.sets.forEach { set ->
                                SetRow(
                                    set = set,
                                    restSeconds = block.restSeconds,
                                    onUpdate = { w, r, rir, pain -> viewModel.updateSet(set, w, r, rir, pain) },
                                    onToggle = { viewModel.toggleCompleted(set) },
                                    onDelete = { viewModel.deleteSet(set) },
                                    onRest = {
                                        RestTimerService.start(
                                            context,
                                            block.restSeconds,
                                            block.exercise.id,
                                            block.exercise.name
                                        )
                                    },
                                    onStopwatch = {
                                        RestTimerService.startStopwatch(
                                            context,
                                            block.exercise.id,
                                            block.exercise.name
                                        )
                                    },
                                    onAdjustRest = { delta -> viewModel.adjustRest(block, delta) }
                                )
                            }

                            if (block.sets.isEmpty()) {
                                Text(
                                    "Nessuna serie. Aggiungi la prima con +.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextLow
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Volume esercizio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextLow
                                )
                                Text(
                                    Format.kg(block.volumeKg),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonOrange
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMid
                        )
                    } else {
                        Text(
                            "Non hai registrato nessuna serie. Una seduta senza serie non viene conteggiata nei progressi: " +
                                "chiudendola verra salvata come seduta vuota.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonMagenta
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

/** Cella statistica della seduta con accento neon. */
@Composable
private fun SessionStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(Color(0xFF10151F))
            .border(1.dp, StrokeNavy, MaterialTheme.shapes.small)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextLow)
    }
}

/** Bottone secondario compatto usato nel timer di recupero. */
@Composable
private fun TimerControl(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.10f else 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

@Composable
private fun SetRow(
    set: LoggedSet,
    restSeconds: Int,
    onUpdate: (Double?, Int?, Int?, Boolean) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onRest: () -> Unit,
    onStopwatch: () -> Unit,
    onAdjustRest: (Int) -> Unit
) {
    var weight by remember(set.id, set.weightKg) { mutableStateOf(set.weightKg?.toString().orEmpty()) }
    var reps by remember(set.id, set.reps) { mutableStateOf(set.reps?.toString().orEmpty()) }
    var rir by remember(set.id, set.rir) { mutableStateOf(set.rir?.toString().orEmpty()) }
    var pain by remember(set.id, set.pain) { mutableStateOf(set.pain) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (set.completed) NeonGreen.copy(alpha = 0.07f) else Color(0xFF121722))
            .border(
                1.dp,
                if (set.completed) NeonGreen.copy(alpha = 0.35f) else StrokeNavy,
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = set.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonGreen,
                    uncheckedColor = TextLow,
                    checkmarkColor = BgDeep
                )
            )
            Text(
                "Serie ${set.setIndex}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            SetIconButton(text = "T", tint = NeonCyan, onClick = onRest)
            Spacer(Modifier.width(6.dp))
            SetIconButton(text = "C", tint = NeonViolet, onClick = onStopwatch)
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(NeonMagenta.copy(alpha = 0.12f))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Elimina serie",
                    tint = NeonMagenta,
                    modifier = Modifier.size(15.dp)
                )
            }
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
            Checkbox(
                checked = pain,
                onCheckedChange = {
                    pain = it
                    onUpdate(weight.toDoubleOrNull(), reps.toIntOrNull(), rir.toIntOrNull(), it)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonOrange,
                    uncheckedColor = TextLow,
                    checkmarkColor = BgDeep
                )
            )
            Text("Dolore o disagio", style = MaterialTheme.typography.labelMedium, color = TextMid)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Recupero ${restSeconds}s", style = MaterialTheme.typography.labelSmall, color = TextLow)
            RestChip("-15") { onAdjustRest(-15) }
            RestChip("+15") { onAdjustRest(15) }
        }
    }
}

@Composable
private fun SetIconButton(text: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun RestChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(CardNavy)
            .border(1.dp, StrokeNavy, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
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
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ex.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${ex.primaryMuscle.label} · ${ex.equipment.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextLow
                                )
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(NeonCyan.copy(alpha = 0.12f))
                                    .clickable { onPick(ex) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Aggiungi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}
