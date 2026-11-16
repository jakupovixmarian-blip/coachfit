package com.fitcoach.app.ui.screens.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.DialogActions
import com.fitcoach.app.ui.components.FitChoiceChip
import com.fitcoach.app.ui.components.FitDialog
import com.fitcoach.app.ui.components.FitSelectableRow
import com.fitcoach.app.ui.components.FitTextField
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.InfoBlue
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextSecondary

/* =============================================================================
 * Aggiunta di un alimento fuori piano al diario.
 * -----------------------------------------------------------------------------
 * Ricerca per nome/categoria, scansione del codice a barre (CameraX + ML Kit:
 * il risultato arriva dal ViewModel) e scelta del pasto. Il calcolo di kcal e
 * macro per la quantità scelta è del motore, qui arriva già formattato.
 * ========================================================================== */

/**
 * Risultato di ricerca alimento.
 *
 * @param name FoodItem.name
 * @param category FoodItem.category
 * @param kcalPer100 valore nutrizionale per 100 g dell'archivio interno
 * @param sourceLabel provenienza del valore (CREA / USDA)
 * @param alreadyLogged true se l'alimento è già presente nel pasto scelto
 */
data class FoodSearchResultUi(
    val name: String,
    val category: String = "",
    val kcalPer100: Int = 0,
    val sourceLabel: String = "",
    val alreadyLogged: Boolean = false,
)

@Composable
fun AddFoodDialog(
    query: String,
    results: List<FoodSearchResultUi>,
    selectedFood: String?,
    gramsInput: String,
    mealOptions: List<String>,
    selectedMealIndex: Int,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectFood: (String) -> Unit,
    onGramsChange: (String) -> Unit,
    onSelectMeal: (Int) -> Unit,
    onScanBarcode: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isScanning: Boolean = false,
    scanMessage: String? = null,
    computedKcalLabel: String = "",
    computedMacroLabel: String = "",
    errorMessage: String? = null,
) {
    FitDialog(
        title = "Aggiungi alimento",
        subtitle = "Fuori piano: viene conteggiato nel riepilogo del giorno",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = "Aggiungi al diario",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmEnabled = selectedFood != null && gramsInput.isNotBlank(),
            )
        },
    ) {
        FitTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Cerca alimento",
            placeholder = "Nome o categoria (es. pollo, latticini…)",
            modifier = Modifier.fillMaxWidth(),
        )

        GhostButton(
            text = if (isScanning) "Ricerca del prodotto in corso…" else "Scansiona codice a barre",
            onClick = onScanBarcode,
            enabled = !isScanning,
        )

        scanMessage?.let { message ->
            Text(text = message, style = FitTextStyles.caption, color = InfoBlue)
        }

        if (isSearching) {
            Text(
                text = "Ricerca del prodotto in corso…",
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        if (errorMessage != null) {
            Text(text = errorMessage, style = FitTextStyles.caption, color = ErrorRed)
        }

        if (results.isEmpty() && !isSearching) {
            Text(
                text = "Nessun alimento trovato.",
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
        }

        results.forEach { result ->
            FitSelectableRow(
                title = result.name,
                subtitle = buildString {
                    if (result.category.isNotBlank()) append(result.category)
                    if (result.kcalPer100 > 0) {
                        if (isNotEmpty()) append(" · ")
                        append("${result.kcalPer100} kcal/100 g")
                    }
                    if (result.sourceLabel.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(result.sourceLabel)
                    }
                },
                selected = result.name == selectedFood,
                onClick = { onSelectFood(result.name) },
                trailing = {
                    if (result.alreadyLogged) {
                        Pill(text = "Già registrato", color = TextSecondary)
                    }
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FitTextField(
                value = gramsInput,
                onValueChange = onGramsChange,
                label = "Quantità (g)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }

        if (computedKcalLabel.isNotBlank()) {
            Text(
                text = "$computedKcalLabel · $computedMacroLabel",
                style = FitTextStyles.numericS,
                color = Emerald,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Pasto", style = FitTextStyles.caption, color = TextLow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                mealOptions.forEachIndexed { index, meal ->
                    FitChoiceChip(
                        text = meal,
                        selected = index == selectedMealIndex,
                        onClick = { onSelectMeal(index) },
                    )
                }
            }
        }

        Text(
            text = "I valori provengono dall'archivio alimentare interno (CREA / USDA). Il diario registra il consumo reale, non il piano.",
            style = FitTextStyles.caption,
            color = TextLow,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
