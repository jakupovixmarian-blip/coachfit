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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercise = state.exercise

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Esercizio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (exercise?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Preferito"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (exercise == null) {
                Text("Esercizio non trovato")
                return@Column
            }

            FitCard {
                Column {
                    SectionTitle("Esecuzione")
                    Text(exercise.setup.ifBlank { "Impostazione non disponibile." }, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(exercise.execution.ifBlank { "Esecuzione non disponibile." }, style = MaterialTheme.typography.bodyMedium)
                    if (exercise.commonMistakes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Errori comuni", style = MaterialTheme.typography.titleMedium)
                        Text(exercise.commonMistakes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            FitCard {
                Column {
                    SectionTitle("Classificazione")
                    KeyValueRow("Muscolo principale", exercise.primaryMuscle.label)
                    KeyValueRow("Muscoli secondari", exercise.secondaryMuscles.joinToString(", ") { it.label }.ifBlank { "-" })
                    KeyValueRow("Attrezzatura", exercise.equipment.label)
                    KeyValueRow("Schema di movimento", exercise.pattern.label)
                }
            }

            FitCard {
                Column {
                    SectionTitle("Statistiche reali")
                    if (state.history.isEmpty()) {
                        Text(
                            "Nessun dato registrato: le statistiche compaiono dopo la prima serie completata.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        KeyValueRow("Serie registrate", state.history.size.toString())
                        KeyValueRow("Miglior carico", state.bestWeightKg?.let { Format.kg(it) } ?: "-")
                        KeyValueRow("Miglior 1RM stimato", state.bestOneRm?.let { Format.kg(it) } ?: "-")
                        KeyValueRow("Volume totale", Format.kg(state.totalVolume))
                        KeyValueRow("Miglior ripetizioni", state.personalBestReps?.toString() ?: "-")
                        state.last?.let { last ->
                            Spacer(Modifier.height(6.dp))
                            Text("Ultima esecuzione", style = MaterialTheme.typography.titleMedium)
                            last.sets.forEach { set ->
                                KeyValueRow(
                                    "Serie ${set.setIndex}",
                                    "${set.weightKg?.let { Format.kg(it) } ?: "-"} x ${set.reps ?: "-"} @RIR ${set.rir ?: "-"}"
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
