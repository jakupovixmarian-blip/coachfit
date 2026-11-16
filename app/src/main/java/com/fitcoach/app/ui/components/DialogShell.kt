package com.fitcoach.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fitcoach.app.ui.theme.BrandSoft
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurface
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextDisabled
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary

/* =============================================================================
 * DialogShell.kt — contenitore unico dei dialog dell'app.
 * -----------------------------------------------------------------------------
 * Tutti i dialog (alimentazione, diario, spesa, allenamento) usano questa
 * cornice: stessa superficie, stesso raggio, stessa gerarchia del titolo.
 * Sono composable stateless: ricevono valori già calcolati e restituiscono
 * eventi. Nessun ViewModel, nessuna query: la logica resta dove è sempre stata.
 * ========================================================================== */

/**
 * Cornice di un dialog: titolo, sottotitolo opzionale, corpo scrollabile e
 * riga di azioni. Il corpo non supera il 70% dello schermo per non coprire la
 * schermata sottostante su telefoni piccoli (360dp).
 */
@Composable
fun FitDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(FitRadius.sheet)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(FitSurface)
                .border(width = 1.dp, color = FitOutline, shape = shape)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = title, style = FitTextStyles.h3, color = TextPrimary)
                if (subtitle != null) {
                    Text(text = subtitle, style = FitTextStyles.caption, color = TextLow)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/** Campo di testo del design system: etichetta sopra, superficie piena. */
@Composable
fun FitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(FitRadius.md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(FitSurfaceVariant)
            .border(width = 1.dp, color = FitOutline, shape = shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text = label.uppercase(), style = FitTextStyles.caption, color = TextLow)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = FitTextStyles.body,
                        color = TextDisabled,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    enabled = enabled,
                    textStyle = FitTextStyles.body.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Emerald),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/** Chip selezionabile: usata per i pasti, le opzioni e i filtri dei dialog. */
@Composable
fun FitChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = FitRadius.pill
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) BrandSoft else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Emerald else FitOutline,
                shape = shape,
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            style = FitTextStyles.label,
            color = if (selected) Emerald else TextSecondary,
        )
    }
}

/** Riga selezionabile di un elenco (risultati di ricerca, esercizi, alimenti). */
@Composable
fun FitSelectableRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(FitRadius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) BrandSoft else FitSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (selected) Emerald else FitOutline,
                shape = shape,
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = FitTextStyles.bodyStrong, color = TextPrimary)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = FitTextStyles.caption, color = TextLow)
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        } else if (selected) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "✓", style = FitTextStyles.numericS, color = Emerald)
        }
    }
}

/** Casella di spunta del design system (nessuna dipendenza da Material). */
@Composable
fun FitCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(FitRadius.xs)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(shape)
                .background(if (checked) Emerald else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (checked) Emerald else FitOutline,
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    style = FitTextStyles.caption,
                    color = com.fitcoach.app.ui.theme.OnBrand,
                )
            }
        }
        if (label != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = FitTextStyles.body, color = TextPrimary)
                if (subtitle != null) {
                    Text(text = subtitle, style = FitTextStyles.caption, color = TextLow)
                }
            }
        }
    }
}

/** Riga di azioni standard: secondaria compatta + primaria che prende lo spazio. */
@Composable
fun RowScope.DialogActions(
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Annulla",
    confirmEnabled: Boolean = true,
) {
    GhostButton(text = dismissText, onClick = onDismiss)
    Box(modifier = Modifier.weight(1f)) {
        PrimaryButton(text = confirmText, onClick = onConfirm, enabled = confirmEnabled)
    }
}
