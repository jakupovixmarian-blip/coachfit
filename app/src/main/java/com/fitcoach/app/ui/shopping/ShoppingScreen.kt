package com.fitcoach.app.ui.shopping

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.ShoppingItem
import com.fitcoach.app.ui.components.EmptyState
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.TextSecondary

@Composable
fun ShoppingScreen(
    navController: NavHostController,
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

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
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Spesa settimanale", "Generata dal piano alimentare effettivo")
                Row {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rigenera", tint = Emerald)
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, state.shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Condividi lista"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Condividi", tint = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.showAddDialog(true) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Aggiungi", tint = Emerald)
                    }
                }
            }
        }

        item {
            SectionCard {
                Column {
                    SectionHeader("Avanzamento", "${state.purchased} di ${state.total} alimenti")
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Emerald,
                        trackColor = FitSurfaceVariant
                    )
                }
            }
        }

        if (state.items.isEmpty()) {
            item {
                EmptyState(
                    title = "Lista vuota",
                    message = "Genera prima un piano alimentare in Alimentazione: la lista della spesa " +
                        "viene calcolata automaticamente dai 7 giorni del piano."
                )
            }
        }

        state.grouped.forEach { (category, items) ->
            item { Text(category.label, style = MaterialTheme.typography.titleSmall, color = Emerald, fontWeight = FontWeight.SemiBold) }
            items(items, key = { it.id }) { item ->
                ShoppingRow(
                    item = item,
                    onChecked = { viewModel.toggleChecked(item.id, it) },
                    onAlreadyHave = { viewModel.toggleAlreadyHave(item.id, it) },
                    onDelete = { viewModel.delete(item.id) }
                )
            }
        }
    }

    if (state.addDialog) {
        AddItemDialog(
            onConfirm = { name, grams -> viewModel.addManual(name, grams, null) },
            onDismiss = { viewModel.showAddDialog(false) }
        )
    }

    SnackbarHost(snackbar)
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    onChecked: (Boolean) -> Unit,
    onAlreadyHave: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.checked, onCheckedChange = onChecked)
            Column(Modifier.weight(1f)) {
                Text(
                    item.foodName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.checked || item.alreadyHave) TextSecondary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${item.totalGrams.toInt()} g" + if (item.alreadyHave) " · già in casa" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            IconButton(onClick = { onAlreadyHave(!item.alreadyHave) }) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = "Già in casa",
                    tint = if (item.alreadyHave) Emerald else TextSecondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    onConfirm: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi alimento") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(name, grams.replace(',', '.').toDoubleOrNull() ?: 0.0)
            }) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
