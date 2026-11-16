package com.fitcoach.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.DialogActions
import com.fitcoach.app.ui.components.FitChoiceChip
import com.fitcoach.app.ui.components.FitDialog
import com.fitcoach.app.ui.components.FitSelectableRow
import com.fitcoach.app.ui.components.FitTextField
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextSecondary

/* =============================================================================
 * Scelta di un esercizio dalla libreria (scheda personalizzata).
 * L'elenco è la libreria interna (Exercise): nome, gruppo muscolare,
 * attrezzatura. Nessuna generazione: l'utente sceglie, l'app registra.
 * ========================================================================== */

/**
 * Voce della libreria esercizi.
 *
 * @param name Exercise.name
 * @param muscle Exercise.muscleGroup (primario, secondari in coda)
 * @param equipment Exercise.equipment
 * @param alreadyInDay true se l'esercizio è già nella seduta
 */
data class ExercisePickerItemUi(
    val name: String,
    val muscle: String = "",
    val equipment: String = "",
    val alreadyInDay: Boolean = false,
)

@Composable
fun ExercisePickerDialog(
    query: String,
    items: List<ExercisePickerItemUi>,
    selectedName: String?,
    muscleFilters: List<String>,
    selectedFilterIndex: Int,
    onQueryChange: (String) -> Unit,
    onSelectFilter: (Int) -> Unit,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isSearching: Boolean = false,
) {
    FitDialog(
        title = "Scegli un esercizio",
        subtitle = "Libreria esercizi · la scheda resta modificabile",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = "Aggiungi alla seduta",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmEnabled = selectedName != null,
            )
        },
    ) {
        FitTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Cerca",
            placeholder = "Esercizio o gruppo muscolare",
            modifier = Modifier.fillMaxWidth(),
        )

        if (muscleFilters.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                muscleFilters.forEachIndexed { index, filter ->
                    FitChoiceChip(
                        text = filter,
                        selected = index == selectedFilterIndex,
                        onClick = { onSelectFilter(index) },
                    )
                }
            }
        }

        if (isSearching) {
            Text(
                text = "Cerco nella libreria…",
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        if (items.isEmpty() && !isSearching) {
            Text(
                text = "Nessun esercizio corrisponde alla ricerca.",
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
        }

        items.forEach { item ->
            FitSelectableRow(
                title = item.name,
                subtitle = listOfNotNull(
                    item.muscle.takeIf { it.isNotBlank() },
                    item.equipment.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                selected = item.name == selectedName,
                onClick = { onSelect(item.name) },
                trailing = {
                    if (item.alreadyInDay) {
                        Pill(text = "Già in seduta", color = TextSecondary)
                    }
                },
            )
        }
    }
}
