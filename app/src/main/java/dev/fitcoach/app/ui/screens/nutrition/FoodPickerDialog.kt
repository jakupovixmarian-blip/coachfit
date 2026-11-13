package dev.fitcoach.app.ui.screens.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.FitSecondaryButton

@Composable
fun FoodPickerDialog(
    foods: List<Food>,
    onDismiss: () -> Unit,
    onPick: (Food, Double) -> Unit,
    viewModel: FoodViewModel = hiltViewModel()
) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val allFoods by viewModel.foods.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Food?>(null) }
    var grams by remember { mutableStateOf("100") }
    var showCreate by remember { mutableStateOf(false) }

    val filtered = (foods.ifEmpty { allFoods }).filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.brand.contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selected == null) "Scegli alimento" else selected!!.name) },
        text = {
            if (selected == null) {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Cerca nell'archivio") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FitSecondaryButton(onClick = { viewModel.onQueryChange(query); viewModel.searchOpenFoodFacts() }) {
                            Text("Cerca Open Food Facts")
                        }
                        FitSecondaryButton(onClick = { showCreate = true }) { Text("Nuovo") }
                    }
                    search.info?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (search.loading) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator()
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(filtered, key = { "f_${it.id}" }) { food ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(food.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${Format.kcal(food.kcalPer100g)}/100g - P ${Format.grams(food.proteinPer100g)} C ${Format.grams(food.carbsPer100g)} G ${Format.grams(food.fatPer100g)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("Fonte: ${food.source.label} - ${food.state.label}", style = MaterialTheme.typography.labelMedium)
                                Row {
                                    TextButton(onClick = { selected = food }) { Text("Usa") }
                                    if (food.isCustom) {
                                        TextButton(onClick = { viewModel.deleteFood(food) }) { Text("Elimina") }
                                    }
                                }
                            }
                        }
                        if (search.remoteResults.isNotEmpty()) {
                            item { Text("Risultati Open Food Facts (importabili)", style = MaterialTheme.typography.titleMedium) }
                            items(search.remoteResults, key = { "r_${it.barcode ?: it.name}" }) { food ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Text(food.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${Format.kcal(food.kcalPer100g)}/100g - ${food.brand}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row {
                                        TextButton(onClick = { viewModel.importFood(food); selected = food }) { Text("Importa e usa") }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column {
                    val food = selected!!
                    Text("Valori per 100 g: ${Format.kcal(food.kcalPer100g)}")
                    Text("Proteine ${Format.grams(food.proteinPer100g)} - Carboidrati ${Format.grams(food.carbsPer100g)} - Grassi ${Format.grams(food.fatPer100g)}")
                    Spacer(Modifier.height(10.dp))
                    NumberField("Grammi", grams, { grams = it })
                    grams.toDoubleOrNull()?.let { g ->
                        val macros = food.macrosFor(g)
                        Spacer(Modifier.height(6.dp))
                        Text("Totale: ${Format.kcal(macros.kcal)} - P ${Format.grams(macros.protein)} C ${Format.grams(macros.carbs)} G ${Format.grams(macros.fat)}")
                    }
                }
            }
        },
        confirmButton = {
            if (selected == null) {
                TextButton(onClick = onDismiss) { Text("Chiudi") }
            } else {
                TextButton(onClick = {
                    grams.toDoubleOrNull()?.takeIf { it > 0 }?.let { onPick(selected!!, it) }
                }) { Text("Aggiungi") }
            }
        },
        dismissButton = {
            if (selected != null) TextButton(onClick = { selected = null }) { Text("Indietro") }
        }
    )

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var kcal by remember { mutableStateOf("") }
        var protein by remember { mutableStateOf("") }
        var carbs by remember { mutableStateOf("") }
        var fat by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Nuovo alimento") },
            text = {
                Column {
                    dev.fitcoach.app.ui.components.LabeledField("Nome", name, { name = it })
                    NumberField("Kcal per 100 g", kcal, { kcal = it })
                    NumberField("Proteine per 100 g", protein, { protein = it })
                    NumberField("Carboidrati per 100 g", carbs, { carbs = it })
                    NumberField("Grassi per 100 g", fat, { fat = it })
                    Text(
                        "Inserisci i valori reali dall'etichetta: non vengono inventati.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val k = kcal.toDoubleOrNull()
                    if (name.isNotBlank() && k != null) {
                        viewModel.addManualFood(
                            name, k,
                            protein.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0,
                            fat.toDoubleOrNull() ?: 0.0
                        )
                        showCreate = false
                    }
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Annulla") } }
        )
    }
}
