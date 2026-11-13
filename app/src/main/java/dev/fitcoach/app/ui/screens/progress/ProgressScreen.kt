package dev.fitcoach.app.ui.screens.progress

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.Measurement
import dev.fitcoach.app.domain.model.MeasurementType
import dev.fitcoach.app.ui.components.BarChart
import dev.fitcoach.app.ui.components.ChartPoint
import dev.fitcoach.app.ui.components.ChipSelector
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.GradientButton
import dev.fitcoach.app.ui.components.HeroCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.LineChart
import dev.fitcoach.app.ui.components.MetricCard
import dev.fitcoach.app.ui.components.NeonBadge
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.PillTabs
import dev.fitcoach.app.ui.components.RingProgress
import dev.fitcoach.app.ui.components.ScreenHeader
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.Tag
import dev.fitcoach.app.ui.theme.BgDeep
import dev.fitcoach.app.ui.theme.CardNavy
import dev.fitcoach.app.ui.theme.GradientGreen
import dev.fitcoach.app.ui.theme.NeonCyan
import dev.fitcoach.app.ui.theme.NeonGreen
import dev.fitcoach.app.ui.theme.NeonMagenta
import dev.fitcoach.app.ui.theme.NeonOrange
import dev.fitcoach.app.ui.theme.NeonViolet
import dev.fitcoach.app.ui.theme.StrokeNavy
import dev.fitcoach.app.ui.theme.TextLow
import dev.fitcoach.app.ui.theme.TextMid

/**
 * Progressi nel layout del mockup: anello di aderenza in card hero, griglia 2x2 di
 * card metrica con sparkline, tab pill e grafici neon per peso e volume.
 */
@Composable
fun ProgressScreen(
    onOpenMeasurement: (String) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Peso", "Volume", "Misure", "Storico")

    val weights = state.byType(MeasurementType.WEIGHT)
    val weightSeries = weights.sortedBy { it.dateEpochDay }.map { it.value.toFloat() }
    val weightDelta = if (weights.size >= 2) {
        val sorted = weights.sortedBy { it.dateEpochDay }
        sorted.last().value - sorted.first().value
    } else null
    val totalVolume = state.weeklyVolume.sumOf { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    ScreenHeader("Progressi", "Misure e andamento nel tempo")
                }
                NeonBadge(icon = Icons.Filled.TrendingUp, tint = NeonGreen, size = 46.dp)
            }
        }

        item {
            HeroCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("Aderenza al piano")
                        val adherence = state.adherencePercent
                        Text(
                            if (adherence != null) "${adherence.toInt()}%" else "—",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (adherence != null) "sedute completate sul previsto"
                            else "Servono più sedute per calcolarla",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMid
                        )
                        Spacer(Modifier.height(10.dp))
                        Tag("Serie 4 settimane: ${state.weeklySets}", color = NeonCyan)
                    }
                    Spacer(Modifier.width(12.dp))
                    RingProgress(
                        progress = ((state.adherencePercent ?: 0.0) / 100.0).toFloat(),
                        size = 108.dp,
                        strokeWidth = 12.dp,
                        colors = listOf(NeonCyan, NeonViolet)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    label = "Peso attuale",
                    value = state.currentWeight?.let { "${"%.1f".format(it)} kg" } ?: "—",
                    modifier = Modifier.weight(1f),
                    delta = weightDelta?.let {
                        val sign = if (it > 0) "+" else ""
                        "$sign${"%.1f".format(it)} kg"
                    },
                    deltaColor = when {
                        weightDelta == null -> NeonCyan
                        weightDelta > 0 -> NeonOrange
                        else -> NeonGreen
                    },
                    accent = NeonCyan,
                    series = weightSeries
                )
                MetricCard(
                    label = "Volume totale",
                    value = "${totalVolume.toInt()} kg",
                    modifier = Modifier.weight(1f),
                    delta = "4 settimane",
                    deltaColor = NeonViolet,
                    accent = NeonViolet,
                    series = state.weeklyVolume.map { it.second.toFloat() }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    label = "Serie totali",
                    value = state.weeklySets.toString(),
                    modifier = Modifier.weight(1f),
                    delta = "ultime 4 settimane",
                    deltaColor = NeonGreen,
                    accent = NeonGreen
                )
                MetricCard(
                    label = "Misurazioni",
                    value = state.measurements.size.toString(),
                    modifier = Modifier.weight(1f),
                    delta = "registrate",
                    deltaColor = NeonOrange,
                    accent = NeonOrange
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillTabs(
                    tabs = tabs,
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (tab == 0) {
            item {
                FitCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("Andamento peso", Modifier.weight(1f))
                            GhostTagClick("Dettaglio") { onOpenMeasurement(MeasurementType.WEIGHT.name) }
                        }
                        LineChart(
                            points = weights.sortedBy { it.dateEpochDay }
                                .map { ChartPoint(DateUtils.fromEpochDay(it.dateEpochDay), it.value) }
                        )
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow("Rilevazioni", weights.size.toString())
                        weights.maxByOrNull { it.value }?.let {
                            KeyValueRow("Massimo", "${"%.1f".format(it.value)} kg", valueColor = NeonOrange)
                        }
                        weights.minByOrNull { it.value }?.let {
                            KeyValueRow("Minimo", "${"%.1f".format(it.value)} kg", valueColor = NeonGreen)
                        }
                    }
                }
            }
        }

        if (tab == 1) {
            item {
                FitCard {
                    Column {
                        SectionTitle("Volume per settimana")
                        BarChart(state.weeklyVolume)
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow("Serie totali (4 settimane)", state.weeklySets.toString())
                    }
                }
            }
        }

        if (tab == 2) {
            item {
                FitCard {
                    Column {
                        SectionTitle("Circonferenze")
                        val groups = MeasurementType.entries
                            .filter { it != MeasurementType.WEIGHT }
                            .groupBy { it.group }
                        groups.forEach { (group, types) ->
                            Text(
                                group,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextLow,
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                            )
                            types.forEach { type ->
                                val entries = state.byType(type)
                                val latest = entries.maxByOrNull { it.dateEpochDay }
                                MeasureRow(
                                    label = type.label,
                                    value = latest?.let { "${"%.1f".format(it.value)} ${type.unit}" } ?: "—",
                                    count = entries.size,
                                    accent = if (latest != null) NeonCyan else TextLow,
                                    onClick = { onOpenMeasurement(type.name) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (tab == 3) {
            if (state.measurements.isEmpty()) {
                item {
                    FitCard {
                        EmptyState(
                            "Nessuna misurazione",
                            "Aggiungi peso, circonferenze o massa grassa per vedere i grafici."
                        )
                    }
                }
            } else {
                item {
                    FitCard {
                        Column {
                            SectionTitle("Ultime rilevazioni")
                            state.measurements.take(20).forEach { measurement ->
                                HistoryRow(
                                    measurement = measurement,
                                    onOpen = { onOpenMeasurement(measurement.type.name) },
                                    onDelete = { viewModel.deleteMeasurement(measurement) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            GradientButton(
                text = "Nuova misurazione",
                icon = Icons.Filled.Add,
                gradient = GradientGreen,
                onClick = { showAdd = true }
            )
        }
    }

    if (showAdd) {
        AddMeasurementDialog(
            onDismiss = { showAdd = false },
            onSave = { type, value, date, customLabel, notes ->
                viewModel.addMeasurement(type, value, date, customLabel, notes)
                showAdd = false
            }
        )
    }
}

/** Piccolo pulsante testuale con freccia usato nelle intestazioni delle card. */
@Composable
private fun GhostTagClick(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(NeonCyan.copy(alpha = 0.10f))
            .border(1.dp, NeonCyan.copy(alpha = 0.28f), CircleShape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun MeasureRow(
    label: String,
    value: String,
    count: Int,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(MaterialTheme.shapes.small)
            .background(CardNavy)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMid,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Spacer(Modifier.width(8.dp))
        Text("${count}x", style = MaterialTheme.typography.labelSmall, color = TextLow)
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextLow,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun HistoryRow(
    measurement: Measurement,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(MaterialTheme.shapes.small)
            .background(CardNavy)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
        ) {
            Text(
                measurement.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                DateUtils.format(DateUtils.fromEpochDay(measurement.dateEpochDay)),
                style = MaterialTheme.typography.labelSmall,
                color = TextLow
            )
        }
        Text(
            "${"%.1f".format(measurement.value)} ${measurement.unit}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(NeonMagenta.copy(alpha = 0.12f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Elimina",
                tint = NeonMagenta,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AddMeasurementDialog(
    onDismiss: () -> Unit,
    onSave: (MeasurementType, Double, java.time.LocalDate, String, String) -> Unit
) {
    var type by remember { mutableStateOf(MeasurementType.WEIGHT) }
    var value by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.today()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardNavy,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { date = date.minusDays(1) }) { Text("Giorno -") }
                    Text(DateUtils.format(date), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { date = date.plusDays(1) }) { Text("Giorno +") }
                }
                LabeledField("Note", notes, { notes = it }, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                value.toDoubleOrNull()?.let { v -> onSave(type, v, date, customLabel, notes) }
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
