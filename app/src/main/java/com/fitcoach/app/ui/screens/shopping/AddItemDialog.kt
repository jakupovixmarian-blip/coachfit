package com.fitcoach.app.ui.screens.shopping

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fitcoach.app.ui.components.DialogActions
import com.fitcoach.app.ui.components.FitDialog
import com.fitcoach.app.ui.components.FitTextField
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow

/* =============================================================================
 * Aggiunta manuale di un articolo alla lista della spesa.
 * La lista nasce dal piano alimentare effettivo: questo dialog serve solo per
 * gli extra che l'utente vuole comprare comunque.
 * ========================================================================== */

@Composable
fun AddItemDialog(
    name: String,
    quantity: String,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    FitDialog(
        title = "Aggiungi alimento",
        subtitle = "Aggiunta manuale alla spesa settimanale",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            DialogActions(
                confirmText = "Aggiungi",
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmEnabled = name.isNotBlank(),
            )
        },
    ) {
        FitTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Alimento",
            placeholder = "Es. olio extravergine",
            modifier = Modifier.fillMaxWidth(),
        )
        FitTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = "Quantità",
            placeholder = "Es. 1 L · 500 g · 2 confezioni",
            keyboardType = KeyboardType.Text,
            modifier = Modifier.fillMaxWidth(),
        )

        if (errorMessage != null) {
            Text(text = errorMessage, style = FitTextStyles.caption, color = ErrorRed)
        }

        Text(
            text = "La lista viene rigenerata dal piano: le voci aggiunte a mano restano finché non riapri la spesa.",
            style = FitTextStyles.caption,
            color = TextLow,
        )
    }
}
