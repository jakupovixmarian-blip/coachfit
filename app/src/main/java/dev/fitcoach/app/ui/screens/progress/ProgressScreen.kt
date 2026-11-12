package dev.fitcoach.app.ui.screens.progress

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.domain.model.MeasurementType
import dev.fitcoach.app.ui.components.BarChart
import dev.fitcoach.app.ui.components.ChipSelector
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.SectionTitle
import java.time.LocalDate

@Composable
fun ProgressScreen(
    onOpenMeasurement: (String) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Progressi", style = MaterialTheme.typography.headlineSmall) }

        item {
            FitCard {
                Column {
                    SectionTitle("Peso")
                    val weights = state.byType(MeasurementType.WEIGHT)
                    if (weights.isEmpty()) {
                        Text(
                            "Nessuna pesata registrata.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        KeyValueRow("Peso attuale (ultima misura)", Format.kg(weights.last().value))
                        if (weights.size >= 2) {
                            val delta = weights.last().value - weights.first().value
                            KeyValueRow("Variazione totale", "%+.1f kg".format(delta))
                            KeyValueRow("Rilevazioni", weights.size.toString())
                        }
                        Spacer(Modifier.height(8.dp))
                        dev.fitcoach.app.ui.components.LineChart(
                            points = weights.map {
                                dev.fitcoach.app.ui.components.ChartPoint(
                                    DateUtils.fromEpochDay(it.dateEpochDay),
                                    it.value
                                )
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Nuova misurazione")
                    }
                }
            }
        }

        item {
            FitCard {
                Column {
                    SectionTitle("Volume settimanale (kg sollevati)")
                    BarChart(state.weeklyVolume)
                    KeyValueRow("Serie totali (4 settimane)", state.weeklySets.toString())
                }
            }
        }

        item {
            FitCard {
                Column {
                    SectionTitle("Circonferenze")
                    val types = MeasurementType.entries.filter { it != MeasurementType.WEIGHT && it != MeasurementType.CUSTOM }
                    if (state.measurements.none { it.type in types }) {
                        Text(
                            "Nessuna circonferenza registrata.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        types.forEach { type ->
                            val entries = state.byType(type)
                            if (entries.isNotEmpty()) {
                                val delta = if (entries.size >= 2) entries.last().value - entries.first().value else 0.0
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(onClick = { onOpenMeasurement(type.name) }) {
                                        Text(type.label)
                                    }
                                    Text(
                                        "%.1f %s (%+.1f)".format(entries.last().value, type.unit, delta),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onOpenMeasurement(MeasurementType.WEIGHT.name) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Apri dettaglio peso") }
                }
            }
        }

        item {
            FitCard {
                Column {
                    SectionTitle("Storico misurazioni")
                    if (state.measurements.isEmpty()) {
                        Text(
                            "Nessuna misurazione salvata.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.measurements.take(20).forEach { measurement ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${DateUtils.formatShort(DateUtils.fromEpochDay(measurement.dateEpochDay))} - ${measurement.label}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row {
                                    Text(
                                        "%.1f %s".format(measurement.value, measurement.unit),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = { viewModel.deleteMeasurement(measurement) }) { Text("X") }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showAdd) {
        var type by remember { mutableStateOf(MeasurementType.WEIGHT) }
        var value by remember { mutableStateOf("") }
        var customLabel by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(DateUtils.today()) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Nuova misurazione") },
            text = {
                Column {
                    ChipSelector(
                        label = "Tipo",
                        options = MeasurementType.entries.toList(),
                        selected = type,
                        optionLabel = { it.label },
                        onSelect = { type = it }
                    )
                    if (type == MeasurementType.CUSTOM) {
                        LabeledField("Etichetta", customLabel, { customLabel = it })
                    }
                    NumberField("Valore (${type.unit.ifBlank { "unita" }})", value, { value = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { date = date.minusDays(1) }) { Text("Giorno -") }
                        Text(DateUtils.format(date))
                        OutlinedButton(onClick = { date = date.plusDays(1) }) { Text("Giorno +") }
                    }
                    LabeledField("Note", notes, { notes = it }, singleLine = false)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    value.toDoubleOrNull()?.let { v ->
                        viewModel.addMeasurement(type, v, date, customLabel, notes)
                        showAdd = false
                    }
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Annulla") } }
        )
    }
}
