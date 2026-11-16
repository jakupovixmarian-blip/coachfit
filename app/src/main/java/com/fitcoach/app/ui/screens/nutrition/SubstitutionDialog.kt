package com.fitcoach.app.ui.screens.nutrition

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
import com.fitcoach.app.ui.components.DataRow
import com.fitcoach.app.ui.components.DialogActions
import com.fitcoach.app.ui.components.FitChoiceChip
import com.fitcoach.app.ui.components.FitDialog
import com.fitcoach.app.ui.components.FitSelectableRow
import com.fitcoach.app.ui.components.FitTextField
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* =============================================================================
 * Sostituzione di un alimento del piano.
 * -----------------------------------------------------------------------------
 * La quantità sostitutiva arriva già calcolata dal motore di sostituzione
 * (tolleranze kcal 5%, P/C/G 10%): qui viene solo mostrata e confermata.
 * Nessun calcolo nutrizionale in questo file.
 * ========================================================================== */

/**
 * Un'alternativa proposta per l'alimento corrente.
 *
 * @param name nome dell'alimento sostitutivo (FoodItem.name)
 * @param gramsLabel grammatica già formattata, es. "180 g"
 * @param kcalLabel calorie della quantità sostitutiva, es. "196 kcal"
 * @param macroLabel macro della quantità sostitutiva, es. "P 23 · C 4 · G 9"
 * @param compatible esito del controllo di tolleranza del motore
 * @param note avvertenza mostrata sotto la riga (allergeni, equivalenza parziale)
 */
data class SubstitutionCandidateUi(
    val name: String,
    val gramsLabel: String,
    val kcalLabel: String,
    val macroLabel: String,
    val compatible: Boolean = true,
    val note: String? = null,
)

@Composable
fun SubstitutionDialog(
    mealLabel: String,
    currentFood: String,
    currentQuantityLabel: String,
    query: String,
    candidates: List<SubstitutionCandidateUi>,
    selectedName: String?,
    previewGramsLabel: String,
    notes: List<String>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    FitDialog(
        title = "Sostituisci alimento",
        subtitle = "$mealLabel · $currentFood $currentQuantityLabel",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = if (previewGramsLabel.isBlank()) "Sostituisci" else "Sostituisci con $previewGramsLabel",
                onConfirm = onApply,
                onDismiss = onDismiss,
                confirmEnabled = selectedName != null && !isSearching,
            )
        },
    ) {
        FitTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Cerca alternativa",
            placeholder = "Es. yogurt greco, bresaola…",
            modifier = Modifier.fillMaxWidth(),
        )

        if (isSearching) {
            Text(
                text = "Valuto la compatibilità delle alternative…",
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        if (errorMessage != null) {
            Text(text = errorMessage, style = FitTextStyles.caption, color = ErrorRed)
        }

        if (candidates.isEmpty() && !isSearching) {
            Text(
                text = "Nessun alimento compatibile con i valori del pasto. Prova un altro nome o una categoria diversa.",
                style = FitTextStyles.bodySmall,
                color = TextLow,
            )
        }

        candidates.forEach { candidate ->
            FitSelectableRow(
                title = candidate.name,
                subtitle = "${candidate.gramsLabel} · ${candidate.kcalLabel} · ${candidate.macroLabel}",
                selected = candidate.name == selectedName,
                onClick = { onSelect(candidate.name) },
                trailing = {
                    Pill(
                        text = if (candidate.compatible) "Compatibile" else "Fuori tolleranza",
                        color = if (candidate.compatible) Emerald else WarningAmber,
                    )
                },
            )
            candidate.note?.let { note ->
                Text(
                    text = note,
                    style = FitTextStyles.caption,
                    color = TextLow,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }
        }

        if (selectedName != null && previewGramsLabel.isNotBlank()) {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Quantità sostitutiva",
                        style = FitTextStyles.caption,
                        color = TextLow,
                    )
                    Text(
                        text = "$selectedName · $previewGramsLabel",
                        style = FitTextStyles.numericM,
                        color = Emerald,
                    )
                }
            }
        }

        if (notes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Note", style = FitTextStyles.caption, color = TextLow)
                notes.forEach { note ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "•",
                            style = FitTextStyles.caption,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(text = note, style = FitTextStyles.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DataRow(label = "Pasto", value = mealLabel)
                DataRow(label = "Alimento attuale", value = "$currentFood $currentQuantityLabel")
                DataRow(
                    label = "Tolleranza",
                    value = "kcal 5% · P/C/G 10%",
                    valueColor = TextPrimary,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Suggerimento", style = FitTextStyles.caption, color = TextLow)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FitChoiceChip(
                    text = "Stessa categoria",
                    selected = true,
                    onClick = { },
                )
                FitChoiceChip(
                    text = "Solo compatibili",
                    selected = false,
                    onClick = { },
                )
            }
        }
    }
}
