package dev.fitcoach.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.ScreenHeader
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.StatTile
import dev.fitcoach.app.ui.components.Tag

@Composable
fun WorkoutScreen(
    onOpenSession: (Long) -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onOpenExercise: (Long) -> Unit,
    onOpenManualProgram: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader("Allenamento", "Programma, sedute e progressione")

        state.inProgressSession?.let { session ->
            FitCard {
                Column {
                    SectionTitle("Seduta in corso")
                    KeyValueRow("Nome", session.name)
                    KeyValueRow("Iniziata", DateUtils.format(DateUtils.fromEpochDay(session.dateEpochDay)))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onOpenSession(session.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Riprendi seduta")
                    }
                }
            }
        }

        state.generationWarnings.takeIf { it.isNotEmpty() }?.let { warnings ->
            FitCard {
                Column {
                    SectionTitle("Controllo del programma")
                    warnings.forEach { warning ->
                        Text(
                            "- $warning",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = { viewModel.dismissWarnings() }) { Text("Ho capito") }
                }
            }
        }

        FitCard {
            Column {
                SectionTitle("Programma attivo")
                if (state.program.isEmpty()) {
                    Text(
                        "Nessun programma. Generane uno dai tuoi dati oppure crealo manualmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        StatTile("Sedute", state.dayCount.toString(), accent = MaterialTheme.colorScheme.primary)
                        StatTile("Esercizi", state.program.sumOf { it.exercises.size }.toString())
                        StatTile("Serie", state.program.sumOf { it.totalSets }.toString())
                    }
                    Spacer(Modifier.height(4.dp))
                    state.program.forEach { day ->
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                day.routine.dayLabel.ifBlank { day.routine.name },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Tag("${day.exercises.size} esercizi · ${day.totalSets} serie")
                        }
                        day.exercises.forEach { re ->
                            val ex = state.exercisesById[re.exerciseId]
                            TextButton(onClick = { onOpenExercise(re.exerciseId) }) {
                                Text(
                                    "${ex?.name ?: "Esercizio"} - ${re.targetSets}x${re.targetRepsMin}-${re.targetRepsMax} @RIR${re.targetRir}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.startSession(day.routine.id) },
                                modifier = Modifier.weight(1f),
                                enabled = state.inProgressSession == null
                            ) { Text("Inizia ${day.routine.dayLabel.ifBlank { day.routine.name }}") }
                            OutlinedButton(
                                onClick = { onOpenRoutine(day.routine.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Modifica") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.generateProgram() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.generating
                ) { Text(if (state.generating) "Generazione in corso..." else "Genera programma con l'agente AI") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenManualProgram, modifier = Modifier.fillMaxWidth()) {
                    Text("Crea il tuo programma personale")
                }
            }
        }

        if (state.suggestions.isNotEmpty()) {
            FitCard {
                Column {
                    SectionTitle("Suggerimenti di progressione")
                    state.suggestions.entries.take(8).forEach { (exerciseId, reason) ->
                        val name = state.exercisesById[exerciseId]?.name ?: "Esercizio"
                        Spacer(Modifier.height(4.dp))
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        FitCard {
            Column {
                SectionTitle("Cronologia sedute")
                if (state.recentSessions.isEmpty()) {
                    EmptyState("Nessuna seduta completata", "Le sedute concluse compaiono qui con i carichi registrati.")
                } else {
                    state.recentSessions.forEach { session ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { onOpenSession(session.id) }) {
                                Text("${DateUtils.formatShort(DateUtils.fromEpochDay(session.dateEpochDay))} - ${session.name}")
                            }
                            TextButton(onClick = { viewModel.deleteSession(session) }) { Text("Elimina") }
                        }
                    }
                }
            }
        }

        state.message?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
