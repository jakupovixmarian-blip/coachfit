package com.fitcoach.app.ui.diary

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.DailyBalance
import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.DiarySource
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.WeeklyNutritionSummary
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.CarbsColor
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ProteinColor
import com.fitcoach.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("EEEE d MMMM")

@Composable
fun DiaryScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel = hiltViewModel()
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

    val balance = state.balance
    val isToday = state.epochDay == com.fitcoach.app.data.repository.NutritionDiaryRepository.todayEpochDay()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousDay() }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Giorno precedente", tint = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Diario nutrizionale", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        LocalDate.ofEpochDay(state.epochDay).format(dateFmt).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
                IconButton(onClick = { viewModel.nextDay() }, enabled = !isToday) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Giorno successivo", tint = if (isToday) TextSecondary.copy(alpha = 0.3f) else TextSecondary)
                }
            }
        }

        if (balance != null) {
            item { BalanceTable(balance) }
        }

        item {
            Button(onClick = { viewModel.openAddDialog() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Aggiungi alimento fuori piano")
            }
        }

        if (state.byMeal.isEmpty()) {
            item {
                EmptyState(
                    title = "Nessun alimento registrato",
                    message = "Spunta gli alimenti del piano in Alimentazione oppure aggiungi qui ciò che hai mangiato fuori piano."
                )
            }
        }

        state.byMeal.forEach { (slot, entries) ->
            item { Text(slot.label, style = MaterialTheme.typography.titleSmall, color = Emerald, fontWeight = FontWeight.SemiBold) }
            items(entries, key = { it.id }) { entry ->
                DiaryEntryRow(
                    entry = entry,
                    onGrams = { viewModel.updateGrams(entry.id, it) },
                    onToggle = { viewModel.toggleActive(entry.id, it) },
                    onMove = { viewModel.moveToMeal(entry.id, it) },
                    onDelete = { viewModel.delete(entry.id) }
                )
            }
        }

        state.weekly?.let { weekly ->
            item { WeeklySummaryCard(weekly) }
        }
    }

    if (state.addDialog) {
        AddFoodDialog(
            state = state,
            onQuery = viewModel::onQueryChange,
            onSelect = viewModel::selectFood,
            onScan = viewModel::openScanner,
            onConfirm = { food, grams, slot -> viewModel.addExtraFood(food, grams, slot) },
            onDismiss = viewModel::closeAddDialog
        )
    }

    if (state.scannerOpen) {
        com.fitcoach.app.ui.components.BarcodeScannerDialog(
            onBarcode = viewModel::onBarcodeScanned,
            onDismiss = viewModel::closeScanner
        )
    }

    SnackbarHost(snackbar)
}

@Composable
private fun BalanceTable(balance: DailyBalance) {
    SectionCard {
        Column {
            SectionHeader("Riepilogo del giorno", "Obiettivo · Consumato · Rimanente")
            Spacer(Modifier.height(12.dp))
            BalanceRow("Calorie", balance.targetKcal, balance.consumedKcal, balance.remainingKcal, balance.kcalExceeded, "kcal", Emerald)
            BalanceRow("Proteine", balance.targetProtein, balance.consumedProtein, balance.remainingProtein, balance.proteinExceeded, "g", ProteinColor)
            BalanceRow("Carboidrati", balance.targetCarbs, balance.consumedCarbs, balance.remainingCarbs, balance.carbsExceeded, "g", CarbsColor)
            BalanceRow("Grassi", balance.targetFat, balance.consumedFat, balance.remainingFat, balance.fatExceeded, "g", MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BalanceRow(
    label: String,
    target: Double,
    consumed: Double,
    remaining: Double,
    exceeded: Double,
    unit: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("${target.toInt()}$unit", modifier = Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text("${consumed.toInt()}$unit", modifier = Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium, color = accent)
        if (exceeded > 0.5) {
            Text("+${exceeded.toInt()}$unit", style = MaterialTheme.typography.bodyMedium, color = CarbsColor, fontWeight = FontWeight.SemiBold)
        } else {
            Text("${remaining.toInt()}$unit", style = MaterialTheme.typography.bodyMedium, color = Emerald)
        }
    }
}

@Composable
private fun DiaryEntryRow(
    entry: DiaryEntry,
    onGrams: (Double) -> Unit,
    onToggle: (Boolean) -> Unit,
    onMove: (MealSlot) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var gramsText by remember { mutableStateOf(entry.grams.toInt().toString()) }
    var menu by remember { mutableStateOf(false) }

    SectionCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = entry.active, onCheckedChange = onToggle)
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.foodName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (entry.active) MaterialTheme.colorScheme.onSurface else TextSecondary
                    )
                    Text(
                        "${entry.grams.toInt()} g · ${entry.kcal.toInt()} kcal · P ${entry.protein.toInt()} · C ${entry.carbs.toInt()} · G ${entry.fat.toInt()}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
                if (entry.source == DiarySource.EXTRA) {
                    Pill("Extra", CarbsColor)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { editing = !editing }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifica quantita", tint = TextSecondary)
                }
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { menu = true }) { Text("Sposta") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        MealSlot.entries.forEach { slot ->
                            DropdownMenuItem(
                                text = { Text(slot.label) },
                                onClick = { menu = false; onMove(slot) }
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = TextSecondary)
                }
            }
            if (editing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Quantità consumata (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onGrams(gramsText.replace(',', '.').toDoubleOrNull() ?: entry.grams)
                        editing = false
                    }) { Text("Salva") }
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(weekly: WeeklyNutritionSummary) {
    SectionCard {
        Column {
            SectionHeader("Riepilogo settimanale", "Consumi reali degli ultimi 7 giorni")
            Spacer(Modifier.height(10.dp))
            SummaryLine("Giorni registrati", "${weekly.daysTracked}")
            SummaryLine("Media calorie", "${weekly.avgKcal.toInt()} kcal")
            SummaryLine("Media proteine", "${weekly.avgProtein.toInt()} g")
            SummaryLine("Media carboidrati", "${weekly.avgCarbs.toInt()} g")
            SummaryLine("Media grassi", "${weekly.avgFat.toInt()} g")
            SummaryLine("Calorie previste (media)", "${weekly.avgPlannedKcal.toInt()} kcal")
            SummaryLine("Aderenza al piano", "${weekly.avgAdherencePercent.toInt()}%")
            SummaryLine("Giorni in target", "${weekly.daysOnTarget}")
            SummaryLine("Alimenti extra", "${weekly.extraEntries}")
            val diff = weekly.kcalDifference
            SummaryLine(
                "Differenza previsto/consumato",
                "${"%+.0f".format(diff)} kcal",
                if (diff <= 0) Emerald else CarbsColor
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AddFoodDialog(
    state: DiaryUiState,
    onQuery: (String) -> Unit,
    onSelect: (FoodItem) -> Unit,
    onScan: () -> Unit,
    onConfirm: (FoodItem, Double, MealSlot) -> Unit,
    onDismiss: () -> Unit
) {
    val pending = state.pendingFood
    var grams by remember(pending) { mutableStateOf("100") }
    var slot by remember(pending) { mutableStateOf(MealSlot.MORNING_SNACK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pending == null) "Cerca alimento" else pending.name) },
        text = {
            Column {
                if (pending == null) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQuery,
                        label = { Text("Nome o categoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scansiona codice a barre")
                    }
                    Spacer(Modifier.height(10.dp))
                    if (state.barcodeLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Ricerca del prodotto in corso...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    if (state.searchResults.isEmpty() && state.query.isNotBlank()) {
                        Text("Nessun alimento trovato.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    state.searchResults.take(8).forEach { food ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${food.kcalPer100g.toInt()} kcal/100g · P ${food.proteinPer100g.toInt()} · C ${food.carbsPer100g.toInt()} · G ${food.fatPer100g.toInt()}",
                                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                                )
                            }
                            TextButton(onClick = { onSelect(food) }) { Text("Scegli") }
                        }
                    }
                } else {
                    Text(
                        "${pending.kcalPer100g.toInt()} kcal/100g · P ${pending.proteinPer100g.toInt()} · C ${pending.carbsPer100g.toInt()} · G ${pending.fatPer100g.toInt()}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = grams,
                        onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Quantità (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Pasto", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MealSlot.entries.take(3).forEach { s ->
                            TextButton(onClick = { slot = s }) {
                                Text(s.label, color = if (slot == s) Emerald else TextSecondary)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MealSlot.entries.drop(3).forEach { s ->
                            TextButton(onClick = { slot = s }) {
                                Text(s.label, color = if (slot == s) Emerald else TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (pending != null) {
                TextButton(onClick = {
                    onConfirm(pending, grams.replace(',', '.').toDoubleOrNull() ?: 0.0, slot)
                }) { Text("Aggiungi al diario") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}
