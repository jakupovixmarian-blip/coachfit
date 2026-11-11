package com.fitcoach.app.ui.progress

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.ui.components.ChartPoint
import com.fitcoach.app.ui.components.LineChart
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ProteinColor
import com.fitcoach.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("dd/MM")

@Composable
fun ProgressScreen(
    navController: NavHostController,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { SectionHeader("Progressi", "Analisi dei tuoi risultati reali") }

        item {
            SectionCard {
                Column {
                    SectionHeader("Registra il peso", "Misurazione al mattino, a digiuno")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.weightInput,
                            onValueChange = viewModel::updateWeightInput,
                            label = { Text("Peso (kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.bfInput,
                            onValueChange = viewModel::updateBfInput,
                            label = { Text("Massa grassa %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { viewModel.addWeight() }) { Text("Salva misurazione") }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Peso corporeo", "${state.weights.size} misurazioni")
                    Spacer(Modifier.height(10.dp))
                    val points = state.weights
                        .sortedBy { it.dateEpochDay }
                        .map { ChartPoint(LocalDate.ofEpochDay(it.dateEpochDay).format(dayFmt), it.weightKg) }
                    LineChart(points = points, lineColor = Emerald, unitSuffix = " kg")
                }
            }
        }

        state.trend?.let { trend ->
            item {
                SectionCard {
                    Column {
                        SectionHeader("Media settimanale", "Tendenza su ${trend.weeksOfData} settimane")
                        Spacer(Modifier.height(10.dp))
                        val weekly = trend.weeklyAverages
                            .filter { it.avgWeightKg != null }
                            .map {
                                ChartPoint(
                                    "S${it.weekStartEpochDay % 100}",
                                    it.avgWeightKg!!
                                )
                            }
                        LineChart(points = weekly, lineColor = ProteinColor, unitSuffix = " kg")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = trend.trendKgPerWeek?.let { "Tendenza: %+.2f kg / settimana".format(it) }
                                ?: "Tendenza non calcolabile",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if ((trend.trendKgPerWeek ?: 0.0) <= 0) CarbsColor else Emerald
                        )
                    }
                }
            }
        }

        if (state.volumeBySession.size >= 2) {
            item {
                SectionCard {
                    Column {
                        SectionHeader("Volume di allenamento", "Carico totale per seduta (kg)")
                        Spacer(Modifier.height(10.dp))
                        val points = state.volumeBySession.map {
                            ChartPoint(LocalDate.ofEpochDay(it.epochDay).format(dayFmt), it.volumeKg)
                        }
                        LineChart(points = points, lineColor = CarbsColor, unitSuffix = " kg")
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Check-in settimanale", "Valutazione periodica")
                    Spacer(Modifier.height(10.dp))
                    state.latestCheckIn?.let { c ->
                        Text(c.summary, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(10.dp))
                    } ?: Text(
                        "Nessun check-in registrato. Esegui il primo check-in per far analizzare i dati al coach.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    OutlinedButton(onClick = { viewModel.runCheckIn() }) {
                        Icon(Icons.Filled.Assessment, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Esegui check-in ora")
                    }
                }
            }
        }
    }

    SnackbarHost(snackbar)
}
