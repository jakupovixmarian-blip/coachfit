package dev.fitcoach.app.ui.screens.progress

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.ui.components.ChartPoint
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.LineChart
import dev.fitcoach.app.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementDetailScreen(
    type: String,
    onBack: () -> Unit,
    viewModel: MeasurementDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.type.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FitCard {
                Column {
                    SectionTitle("Andamento")
                    if (state.entries.isEmpty()) {
                        Text(
                            "Nessun dato registrato per questa misura.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        KeyValueRow("Rilevazioni", state.entries.size.toString())
                        KeyValueRow("Valore attuale", "%.1f %s".format(state.entries.last().value, state.type.unit))
                        state.delta?.let { KeyValueRow("Variazione totale", "%+.1f %s".format(it, state.type.unit)) }
                        state.minValue?.let { KeyValueRow("Minimo", "%.1f %s".format(it, state.type.unit)) }
                        state.maxValue?.let { KeyValueRow("Massimo", "%.1f %s".format(it, state.type.unit)) }
                        state.average?.let { KeyValueRow("Media", "%.1f %s".format(it, state.type.unit)) }
                        Spacer(Modifier.height(8.dp))
                        LineChart(
                            points = state.entries.map {
                                ChartPoint(DateUtils.fromEpochDay(it.dateEpochDay), it.value)
                            }
                        )
                    }
                }
            }

            FitCard {
                Column {
                    SectionTitle("Storico")
                    state.entries.reversed().forEach { measurement ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                DateUtils.format(DateUtils.fromEpochDay(measurement.dateEpochDay)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "%.1f %s".format(measurement.value, measurement.unit),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { viewModel.delete(measurement) }) { Text("Elimina") }
                        }
                        if (measurement.notes.isNotBlank()) {
                            Text(
                                measurement.notes,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
