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
import dev.fitcoach.app.ui.components.SectionTitle

@Composable
fun WorkoutScreen(
    onOpenSession: (Long) -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onOpenExercise: (Long) -> Unit,
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
        Text("Allenamento", style = MaterialTheme.typography.headlineSmall)

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

        FitCard {
            Column {
                SectionTitle("Programma attivo")
                if (state.routine == null) {
                    Text(
                        "Nessun programma. Generane uno dai tuoi dati oppure crealo manualmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    KeyValueRow("Nome", state.routine!!.name)
                    KeyValueRow("Esercizi", state.routineExercises.size.toString())
                    if (state.routineExercises.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        state.routineExercises.take(8).forEach { re ->
                            val ex = state.exercisesById[re.exerciseId]
                            TextButton(onClick = { onOpenExercise(re.exerciseId) }) {
                                Text(
                                    "${ex?.name ?: "Esercizio"} - ${re.targetSets}x${re.targetRepsMin}-${re.targetRepsMax} @RIR${re.targetRir}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.startSession() },
                        modifier = Modifier.weight(1f),
                        enabled = state.inProgressSession == null
                    ) { Text("Inizia seduta") }
                    OutlinedButton(
                        onClick = { state.routine?.let { onOpenRoutine(it.id) } },
                        modifier = Modifier.weight(1f),
                        enabled = state.routine != null
                    ) { Text("Modifica scheda") }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { viewModel.generateRoutine() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Genera programma dai dati")
                }
            }
        }

        if (state.suggestions.isNotEmpty()) {
            FitCard {
                Column {
                    SectionTitle("Suggerimenti di progressione")
                    state.suggestions.forEach { (exerciseId, reason) ->
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
