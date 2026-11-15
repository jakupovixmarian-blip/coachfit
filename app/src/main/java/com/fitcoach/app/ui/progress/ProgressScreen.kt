package com.fitcoach.app.ui.progress
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.ui.components.ChartPoint
import com.fitcoach.app.ui.components.LineChart
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.ProteinColor
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.domain.adaptive.WeeklyCheckInAnalyzer
import com.fitcoach.app.domain.model.ProgramChange
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
    val context = LocalContext.current
    val deleteCandidate = remember { mutableStateOf<com.fitcoach.app.domain.model.ProgressPhoto?>(null) }
    val selectedPhotoIds = remember { mutableStateOf<Set<Long>>(emptySet()) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addPhoto(it.toString())
        }
    }

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
                    SectionHeader("Composizione corporea", "Circonferenze e massa grassa")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.circumferenceSiteInput,
                            onValueChange = viewModel::updateCircumferenceSite,
                            label = { Text("Sede (es. vita)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.circumferenceCmInput,
                            onValueChange = viewModel::updateCircumferenceCm,
                            label = { Text("cm") },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = viewModel::addCircumference) {
                        Text("Salva circonferenza")
                    }
                    val latestBySite = state.circumferences
                        .groupBy { it.site }
                        .mapValues { (_, values) -> values.maxBy { it.dateEpochDay } }
                    latestBySite.toSortedMap().forEach { (site, record) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(site, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "%.1f cm · ${LocalDate.ofEpochDay(record.dateEpochDay).format(dayFmt)}"
                                    .format(record.valueCm),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    val n = state.weights.size
                    SectionHeader("Peso corporeo", "$n ${if (n == 1) "misurazione" else "misurazioni"}")
                    Spacer(Modifier.height(10.dp))
                    val points = state.weights
                        .sortedBy { it.dateEpochDay }
                        .map { ChartPoint(LocalDate.ofEpochDay(it.dateEpochDay).format(dayFmt), it.weightKg) }
                    LineChart(points = points, lineColor = Emerald, unitSuffix = " kg")
                }
            }
        }


        item {
            SectionCard {
                Column {
                    SectionHeader("Fotografie progresso", "Salvate localmente · nessun invio automatico all'AI")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { photoLauncher.launch(arrayOf("image/*")) }) { Text("Aggiungi fotografia") }
                    if (state.photos.isEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Nessuna fotografia registrata.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    } else {
                        state.photos.forEach { photo ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PhotoThumbnail(photo.uri)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        LocalDate.ofEpochDay(photo.capturedAt / 86_400_000L).format(dayFmt),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Consenso AI", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Switch(
                                            checked = photo.consentForAi,
                                            onCheckedChange = { viewModel.setPhotoAiConsent(photo, it) }
                                        )
                                    }
                                }
                                TextButton(onClick = {
                                    selectedPhotoIds.value = if (photo.id in selectedPhotoIds.value) {
                                        selectedPhotoIds.value - photo.id
                                    } else if (selectedPhotoIds.value.size < 2) {
                                        selectedPhotoIds.value + photo.id
                                    } else {
                                        selectedPhotoIds.value
                                    }
                                }) {
                                    Text(if (photo.id in selectedPhotoIds.value) "Selezionata" else "Confronta")
                                }
                                TextButton(onClick = { deleteCandidate.value = photo }) { Text("Elimina") }
                            }
                        }
                        val comparison = state.photos.filter { it.id in selectedPhotoIds.value }
                            .takeIf { it.size == 2 } ?: state.photos.take(2)
                        if (comparison.size == 2) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (selectedPhotoIds.value.size == 2) "Confronto selezionato" else "Confronto ultime due date",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                comparison.forEach { PhotoThumbnail(it.uri, Modifier.weight(1f)) }
                            }
                        }
                    }
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
            WeeklyCheckInSection(
                state = state,
                onRunCheckIn = { viewModel.runCheckIn() },
                onApprove = { viewModel.approveCheckInProposal(it) },
                onReject = { viewModel.rejectCheckInProposal(it) }
            )
        }
    }

    deleteCandidate.value?.let { photo ->
        AlertDialog(
            onDismissRequest = { deleteCandidate.value = null },
            title = { Text("Eliminare fotografia?") },
            text = { Text("La fotografia verrà rimossa dal registro locale.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePhoto(photo)
                    deleteCandidate.value = null
                }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate.value = null }) { Text("Annulla") } }
        )
    }

    SnackbarHost(snackbar)
}

@Composable
private fun PhotoThumbnail(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Fotografia progresso",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(84.dp)
        )
    } else {
        Text("Foto non disponibile", modifier = modifier.padding(8.dp), color = TextSecondary)
    }
}

@Composable
private fun WeeklyCheckInSection(
    state: ProgressUiState,
    onRunCheckIn: () -> Unit,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit
) {
    SectionCard {
        Column {
            SectionHeader("Check-in settimanale", "Valutazione periodica del coach")
            Spacer(Modifier.height(10.dp))

            val report = state.checkInReport
            if (report != null) {
                CheckInMetrics(report)
                Spacer(Modifier.height(10.dp))
            } else {
                state.latestCheckIn?.let { c ->
                    Text(c.summary, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                } ?: Text(
                    "Nessun check-in registrato. Esegui il primo check-in per far analizzare i dati al coach.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
            }

            if (state.checkInEvaluation.isNotBlank()) {
                Text("Valutazione del Coach", style = MaterialTheme.typography.labelLarge, color = Emerald)
                Spacer(Modifier.height(4.dp))
                Text(state.checkInEvaluation, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(10.dp))
            }

            if (state.checkInProposals.isNotEmpty()) {
                Text("Modifiche proposte", style = MaterialTheme.typography.labelLarge, color = Emerald)
                Spacer(Modifier.height(4.dp))
                state.checkInProposals.forEach { change ->
                    CheckInProposal(change, onApprove = { onApprove(change.id) }, onReject = { onReject(change.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }

            OutlinedButton(onClick = onRunCheckIn, enabled = !state.checkInRunning) {
                Icon(Icons.Filled.Assessment, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.checkInRunning) "Analisi in corso..." else "Esegui check-in ora")
            }
        }
    }
}

@Composable
private fun CheckInMetrics(report: WeeklyCheckInAnalyzer.Report) {
    Column {
        if (!report.dataSufficient) {
            Pill("Dati insufficienti", ErrorRed)
            Spacer(Modifier.height(8.dp))
            Text(
                "Servono almeno 2 settimane di consumi reali e 3 settimane di peso medio per decisioni affidabili.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
        }
        MetricRow("Peso medio", report.avgWeightKg?.let { "%.1f kg".format(it) } ?: "n/d")
        MetricRow(
            "Andamento peso",
            (report.weightTrendKgPerWeek?.let { "%+.2f kg/sett".format(it) } ?: "n/d") +
                if (report.weightReliable) "" else " (dati parziali)"
        )
        MetricRow("Massa grassa", report.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "non disponibile")
        MetricRow("Aderenza nutrizionale", report.adherencePercent?.let { "%.0f%%".format(it) } ?: "n/d")
        MetricRow(
            "Calorie medie",
            report.avgKcalConsumed?.let { "%.0f kcal".format(it) } ?: "n/d"
        )
        MetricRow("Allenamenti", "${report.sessionsCompleted}/${report.sessionsPlanned}")
        MetricRow(
            "Prestazioni",
            "${report.progressionCount} in crescita, ${report.stagnationCount} stabili, ${report.regressionCount} in calo"
        )
        MetricRow("Recupero", "${report.recoverySeverity}/3")
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CheckInProposal(
    change: ProgramChange,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(change.title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            Pill(change.domain.name, Emerald)
        }
        Spacer(Modifier.height(4.dp))
        if (change.proposedChange.isNotBlank()) {
            Text(change.proposedChange, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface)
        }
        if (change.rationale.isNotBlank()) {
            Text("Motivazione: ${change.rationale}", style = MaterialTheme.typography.labelSmall,
                color = TextSecondary)
        }
        if (change.expectedOutcome.isNotBlank()) {
            Text("Risultato atteso: ${change.expectedOutcome}", style = MaterialTheme.typography.labelSmall,
                color = TextSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApprove) { Text("Approva") }
            TextButton(onClick = onReject) { Text("Rifiuta") }
        }
    }
}
