package com.fitcoach.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitcoach.app.domain.model.GearCatalog
import com.fitcoach.app.domain.model.GearCategory
import com.fitcoach.app.domain.model.GearInventory
import com.fitcoach.app.domain.model.GearItem
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.TextSecondary

/**
 * Inventario dettagliato dell'attrezzatura.
 *
 * Ogni categoria si apre separatamente perche' l'elenco completo e' lungo e in
 * un'unica lista l'utente perde il segno di cosa ha gia' selezionato. Gli
 * attrezzi selezionati mostrano i campi di dettaglio, quelli non selezionati
 * solo il nome: i campi vuoti non vanno compilati a forza.
 */
@Composable
fun GearInventorySection(
    inventory: GearInventory,
    onTogglePreset: (String) -> Unit,
    onUpdateItem: (GearItem) -> Unit,
    onAddCustom: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var expandedCategory by rememberSaveable { mutableStateOf<GearCategory?>(null) }
    var customName by rememberSaveable { mutableStateOf("") }
    var selectedCount by remember(inventory) { mutableStateOf(inventory.usable.size) }

    SectionCard {
        Column {
            Text("Inventario attrezzatura", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Indica gli attrezzi che hai davvero. Il coach usa questo elenco per " +
                    "prescrivere solo esercizi eseguibili e per calcolare i carichi.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (selectedCount == 0) "Nessun attrezzo selezionato"
                else "$selectedCount attrezzi selezionati",
                style = MaterialTheme.typography.bodySmall, color = Emerald
            )

            Spacer(Modifier.height(10.dp))
            GearCatalog.byCategory().forEach { (category, presets) ->
                val open = expandedCategory == category
                val chosen = presets.count { p ->
                    inventory.presets.any { it.name.equals(p.name, ignoreCase = true) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        category.label + if (chosen > 0) " ($chosen)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        expandedCategory = if (open) null else category
                    }) {
                        Icon(
                            if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (open) "Chiudi" else "Apri",
                            tint = TextSecondary
                        )
                    }
                }
                if (open) {
                    presets.forEach { preset ->
                        val selected = inventory.presets.any {
                            it.name.equals(preset.name, ignoreCase = true)
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { onTogglePreset(preset.name) },
                            label = { Text(preset.name) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                        if (selected) {
                            val item = inventory.presets.first {
                                it.name.equals(preset.name, ignoreCase = true)
                            }
                            GearDetailsEditor(item = item, onChange = onUpdateItem)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Attrezzatura personalizzata", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Nome attrezzo") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        onAddCustom(customName)
                        customName = ""
                    },
                    enabled = customName.isNotBlank()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Aggiungi", tint = Emerald)
                }
            }
            inventory.custom.forEach { item ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemove(item.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Rimuovi", tint = TextSecondary)
                    }
                }
                GearDetailsEditor(item = item, onChange = onUpdateItem)
            }
        }
    }
}

/**
 * Dettagli facoltativi di un attrezzo. I campi numerici accettano testo libero
 * e vengono interpretati solo se validi: un valore non numerico non deve
 * azzerare silenziosamente un limite di carico gia' inserito.
 */
@Composable
private fun GearDetailsEditor(item: GearItem, onChange: (GearItem) -> Unit) {
    var brand by remember(item.id) { mutableStateOf(item.brand) }
    var model by remember(item.id) { mutableStateOf(item.model) }
    var minW by remember(item.id) { mutableStateOf(item.minWeightKg?.let { fmt(it) } ?: "") }
    var maxW by remember(item.id) { mutableStateOf(item.maxWeightKg?.let { fmt(it) } ?: "") }
    var incr by remember(item.id) { mutableStateOf(item.incrementsKg.joinToString("/") { fmt(it) }) }
    var qty by remember(item.id) { mutableStateOf(item.quantity.toString()) }
    var notes by remember(item.id) { mutableStateOf(item.notes) }

    fun push(updated: GearItem) = onChange(updated)

    Column(Modifier.padding(start = 8.dp, bottom = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = qty,
                onValueChange = {
                    qty = it
                    it.toIntOrNull()?.let { n -> push(item.copy(quantity = n.coerceAtLeast(1))) }
                },
                label = { Text("Q.tà") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(96.dp)
            )
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it; push(item.copy(brand = it)) },
                label = { Text("Marca") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = model,
            onValueChange = { model = it; push(item.copy(model = it)) },
            label = { Text("Modello") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minW,
                onValueChange = {
                    minW = it
                    push(item.copy(minWeightKg = it.replace(',', '.').toDoubleOrNull()))
                },
                label = { Text("Min kg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = maxW,
                onValueChange = {
                    maxW = it
                    push(item.copy(maxWeightKg = it.replace(',', '.').toDoubleOrNull()))
                },
                label = { Text("Max kg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = incr,
            onValueChange = {
                incr = it
                push(
                    item.copy(
                        incrementsKg = it.split('/', ',', ';')
                            .mapNotNull { v -> v.trim().replace(',', '.').toDoubleOrNull() }
                    )
                )
            },
            label = { Text("Incrementi kg (es. 1.25/2.5/5)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it; push(item.copy(notes = it)) },
            label = { Text("Note") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        val invalid = minW.isNotBlank() && minW.replace(',', '.').toDoubleOrNull() == null
        if (invalid) {
            Text("Il valore minimo non è un numero valido: non verrà salvato.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

/** Rimuove un attrezzo dall'inventario (usato dalla riga di riepilogo). */
@Composable
fun GearRemoveButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text("Rimuovi") }
}
