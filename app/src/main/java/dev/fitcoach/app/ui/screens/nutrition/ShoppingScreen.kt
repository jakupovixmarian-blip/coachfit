package dev.fitcoach.app.ui.screens.nutrition

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import dev.fitcoach.app.ui.components.EmptyState
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.ScreenHeader
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.SectionTitle
import dev.fitcoach.app.ui.components.FitPrimaryButton
import dev.fitcoach.app.ui.components.FitSecondaryButton

@Composable
fun ShoppingScreen(viewModel: ShoppingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Lista della spesa", "Somma degli alimenti del piano")
        Spacer(Modifier.height(8.dp))

        FitCard {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Settimana dal ${DateUtils.format(DateUtils.fromEpochDay(state.weekStart))}")
                    Row {
                        TextButton(onClick = { viewModel.shiftWeek(-1) }) { Text("Prec") }
                        TextButton(onClick = { viewModel.shiftWeek(1) }) { Text("Succ") }
                    }
                }
                KeyValueRow("Costo totale stimato", Format.money(state.total))
                KeyValueRow("Da acquistare", Format.money(state.remaining))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FitPrimaryButton(onClick = { viewModel.generateFromPlan() }, modifier = Modifier.weight(1f)) {
                        Text("Genera dal piano")
                    }
                    FitSecondaryButton(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) {
                        Text("Aggiungi")
                    }
                }
                Spacer(Modifier.height(6.dp))
                FitSecondaryButton(onClick = { viewModel.clearWeek() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Svuota settimana")
                }
                Text(
                    "I costi sono stime basate sui prezzi per kg inseriti negli alimenti. Se assenti, non vengono mostrati.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (state.items.isEmpty()) {
            EmptyState(
                title = "Lista vuota",
                subtitle = "Genera la lista dal piano alimentare attivo oppure aggiungi voci manualmente."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val grouped = state.items.groupBy { it.category.ifBlank { "Altro" } }
                grouped.forEach { (category, items) ->
                    item(key = "cat_$category") {
                        SectionTitle(category, Modifier.padding(top = 6.dp))
                    }
                    items(items, key = { it.id }) { item ->
                        FitCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.checked, onCheckedChange = { viewModel.toggle(item) })
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    val qty = if (item.quantityGrams >= 1000) {
                                        "%.2f kg".format(item.quantityGrams / 1000.0)
                                    } else Format.grams(item.quantityGrams)
                                    Text(qty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    item.estimatedPrice?.let {
                                        Text("Stima ${Format.money(it)}", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                IconButton(onClick = { viewModel.delete(item) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Elimina")
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var grams by remember { mutableStateOf("500") }
        var category by remember { mutableStateOf("Altro") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Aggiungi alla lista") },
            text = {
                Column {
                    LabeledField("Nome", name, { name = it })
                    NumberField("Quantita (g)", grams, { grams = it })
                    LabeledField("Categoria", category, { category = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addItem(name, grams.toDoubleOrNull() ?: 0.0, category)
                    showAdd = false
                }) { Text("Aggiungi") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Annulla") } }
        )
    }
}
