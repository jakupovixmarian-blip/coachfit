package dev.fitcoach.app.ui.screens.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.domain.model.DiaryEntry
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.ScreenHeader
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.ProgressTrack
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.StatTile
import dev.fitcoach.app.ui.components.FitSecondaryButton
import dev.fitcoach.app.ui.theme.BlueAccent
import dev.fitcoach.app.ui.theme.VioletAccent

@Composable
fun DiaryScreen(viewModel: DiaryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }
    var addingSlot by remember { mutableStateOf<MealSlot?>(null) }

    val totals = state.totals

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Diario alimentare", "Cosa hai mangiato oggi")
        Spacer(Modifier.height(8.dp))

        FitCard {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeDate(-1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Giorno precedente")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(DateUtils.format(state.date), style = MaterialTheme.typography.titleMedium)
                        Text(
                            DateUtils.dayName(state.date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.changeDate(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Giorno successivo")
                    }
                }
                FitSecondaryButton(onClick = { viewModel.goToToday() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Vai a oggi")
                }
                Spacer(Modifier.height(10.dp))

                val target = state.targetKcal
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(totals.kcal.toInt().toString(), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "kcal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    target?.let { "obiettivo ${Format.kcal(it)}" } ?: "obiettivo non calcolabile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (target != null && target > 0) {
                    Spacer(Modifier.height(8.dp))
                    ProgressTrack((totals.kcal / target).toFloat())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Rimanenti ${Format.kcal((target - totals.kcal).coerceAtLeast(0.0))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile("Proteine", Format.grams(totals.protein), accent = MaterialTheme.colorScheme.primary)
                    StatTile("Carboidrati", Format.grams(totals.carbs), accent = BlueAccent)
                    StatTile("Grassi", Format.grams(totals.fat), accent = VioletAccent)
                }
                Spacer(Modifier.height(10.dp))
                KeyValueRow("Target proteine", state.targetProtein?.let { Format.grams(it) } ?: "-")
                KeyValueRow("Target carboidrati", state.targetCarbs?.let { Format.grams(it) } ?: "-")
                KeyValueRow("Target grassi", state.targetFat?.let { Format.grams(it) } ?: "-")
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MealSlot.entries.sortedBy { mealSlotOrder(it) }.forEach { slot ->
                val slotEntries = state.entries.filter { it.mealSlot == slot }
                item(key = "h_${slot.name}") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(slot.label, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { addingSlot = slot }) {
                            Icon(Icons.Filled.Add, contentDescription = "Aggiungi")
                        }
                    }
                }
                if (slotEntries.isEmpty()) {
                    item(key = "e_${slot.name}") {
                        Text(
                            "Nessun alimento registrato",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
                items(slotEntries, key = { it.id }) { entry ->
                    val food = state.foodById[entry.foodId]
                    FitCard {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = entry.consumed,
                                    onCheckedChange = { viewModel.toggleConsumed(entry) }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(food?.name ?: "Alimento", style = MaterialTheme.typography.titleMedium)
                                    val macros = food?.macrosFor(entry.grams) ?: MacroSet.ZERO
                                    Text(
                                        "${Format.grams(entry.grams)} - ${Format.kcal(macros.kcal)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!entry.consumed) {
                                        Text(
                                            "Non conteggiato (consumo annullato)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                IconButton(onClick = { editing = entry }) {
                                    Text("g", style = MaterialTheme.typography.labelLarge)
                                }
                                IconButton(onClick = { viewModel.delete(entry) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Elimina")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    editing?.let { entry ->
        var grams by remember(entry.id) { mutableStateOf(entry.grams.toString()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Modifica quantita") },
            text = {
                Column {
                    Text(state.foodById[entry.foodId]?.name ?: "Alimento")
                    Spacer(Modifier.height(8.dp))
                    NumberField("Grammi", grams, { grams = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    grams.toDoubleOrNull()?.let { viewModel.updateGrams(entry, it) }
                    editing = null
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Annulla") } }
        )
    }

    addingSlot?.let { slot ->
        FoodPickerDialog(
            foods = state.foods,
            onDismiss = { addingSlot = null },
            onPick = { food, grams ->
                viewModel.addFood(slot, food.id, grams)
                addingSlot = null
            }
        )
    }

    state.entries.let {
        if (it.isEmpty() && state.targetKcal == null) {
            EmptyState(
                title = "Diario vuoto",
                subtitle = "Aggiungi alimenti con il pulsante + accanto al pasto. I totali si aggiornano subito."
            )
        }
    }
}
