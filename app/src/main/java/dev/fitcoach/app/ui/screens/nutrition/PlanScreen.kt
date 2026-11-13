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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.MealItem
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.ProgressTrack
import dev.fitcoach.app.ui.components.ScreenHeader
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.StatTile
import dev.fitcoach.app.ui.components.FitChip
import dev.fitcoach.app.ui.components.FitPrimaryButton
import dev.fitcoach.app.ui.components.FitSecondaryButton
import dev.fitcoach.app.ui.theme.BlueAccent
import dev.fitcoach.app.ui.theme.VioletAccent

private val DAY_NAMES = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

@Composable
fun PlanScreen(
    onOpenDiary: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    var editingItem by remember { mutableStateOf<MealItem?>(null) }
    var addingToSlot by remember { mutableStateOf<MealSlot?>(null) }

    val plan = state.plan

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(
            title = "Piano alimentare",
            subtitle = "Generato dall'agente o costruito a mano, con alimenti reali"
        )
        Spacer(Modifier.height(8.dp))

        if (plan == null) {
            EmptyState(
                title = "Nessun piano attivo",
                subtitle = "Genera un piano a 7 giorni con l'agente, oppure crealo manualmente e aggiungi gli alimenti."
            )
            if (generating != null) {
                Text(
                    generating ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                FitPrimaryButton(onClick = { viewModel.generateWithAi() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Genera con l'agente AI")
                }
                Spacer(Modifier.height(8.dp))
                FitSecondaryButton(
                    onClick = { viewModel.generateWeekLocally() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calcola 7 giorni sui dati in archivio")
                }
                Spacer(Modifier.height(8.dp))
                FitSecondaryButton(onClick = { viewModel.createEmptyPlan() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Crea piano manuale (7 giorni)")
                }
            }
            return
        }

        // Totali del giorno
        val totals = state.items.totals(state.foodById)
        FitCard {
            Column {
                SectionTitle("Totali giorno ${DAY_NAMES.getOrElse(state.selectedDayIndex) { "" }}")
                Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    Text(totals.kcal.toInt().toString(), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "kcal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    if (plan.targetKcal > 0) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            "target ${Format.kcal(plan.targetKcal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                if (plan.targetKcal > 0) {
                    Spacer(Modifier.height(8.dp))
                    ProgressTrack((totals.kcal / plan.targetKcal).toFloat())
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile("Proteine", Format.grams(totals.protein), accent = MaterialTheme.colorScheme.primary)
                    StatTile("Carboidrati", Format.grams(totals.carbs), accent = BlueAccent)
                    StatTile("Grassi", Format.grams(totals.fat), accent = VioletAccent)
                }
                Spacer(Modifier.height(8.dp))
                FitPrimaryButton(onClick = onOpenDiary, modifier = Modifier.fillMaxWidth()) { Text("Registra nel diario") }
                Spacer(Modifier.height(8.dp))
                if (generating != null) {
                    Text(
                        generating ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    FitSecondaryButton(
                        onClick = { viewModel.generateWithAi() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rigenera con l'agente AI") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items((0..6).toList()) { index ->
                FitChip(
                    selected = state.selectedDayIndex == index,
                    onClick = { viewModel.selectDay(index) },
                    label = { Text(DAY_NAMES.getOrElse(index) { "$index" }) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MealSlot.entries.sortedBy { mealSlotOrder(it) }.forEach { slot ->
                val slotItems = state.items.filter { it.mealSlot == slot }
                item(key = "header_${slot.name}") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(slot.label, style = MaterialTheme.typography.titleMedium)
                        Row {
                            val slotTotals = slotItems.totals(state.foodById)
                            Text(
                                Format.kcal(slotTotals.kcal),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(onClick = { addingToSlot = slot }) {
                                Icon(Icons.Filled.Add, contentDescription = "Aggiungi alimento")
                            }
                        }
                    }
                }
                items(slotItems, key = { it.id }) { item ->
                    val food = state.foodById[item.foodId]
                    FitCard {
                        Column {
                            Text(food?.name ?: "Alimento rimosso", style = MaterialTheme.typography.titleMedium)
                            if (food != null) {
                                val macros = food.macrosFor(item.grams)
                                Text(
                                    "${Format.grams(item.grams)} - ${Format.kcal(macros.kcal)} - P ${Format.grams(macros.protein)} C ${Format.grams(macros.carbs)} G ${Format.grams(macros.fat)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                food.priceFor(item.grams)?.let { price ->
                                    Text("Costo stimato ${Format.money(price)}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FitSecondaryButton(onClick = { editingItem = item }) { Text("Modifica grammi") }
                                IconButton(onClick = { viewModel.deleteItem(item) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Rimuovi")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    editingItem?.let { item ->
        var grams by remember(item.id) { mutableStateOf(item.grams.toString()) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Modifica grammatura") },
            text = {
                Column {
                    Text(state.foodById[item.foodId]?.name ?: "Alimento")
                    Spacer(Modifier.height(8.dp))
                    NumberField("Grammi", grams, { grams = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    grams.toDoubleOrNull()?.let { viewModel.updateGrams(item, it) }
                    editingItem = null
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { editingItem = null }) { Text("Annulla") } }
        )
    }

    addingToSlot?.let { slot ->
        FoodPickerDialog(
            foods = state.foods,
            onDismiss = { addingToSlot = null },
            onPick = { food, grams ->
                viewModel.addItem(state.selectedDayIndex, slot, food.id, grams)
                addingToSlot = null
            }
        )
    }

    state.message?.let { msg ->
        Text(
            msg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
