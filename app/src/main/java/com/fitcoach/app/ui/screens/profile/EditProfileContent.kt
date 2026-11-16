package com.fitcoach.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.SegmentedControl
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceInteractive
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.SuccessGreen
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* ============================================================================
 * Modifica profilo (design/screens.md §14) — sola presentazione.
 * Cambiare un dato che influenza il piano NON modifica nulla in automatico:
 * il coach proporrà un aggiornamento e l'utente decide.
 * ========================================================================== */

@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    onBack: () -> Unit,
    onFieldChange: (ProfileField, String) -> Unit,
    onGoalSelect: (Int) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(FitRadius.sm))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(text = "‹", style = FitTextStyles.bodyStrong, color = TextSecondary)
                }
                Text(text = "Profilo", style = FitTextStyles.label, color = TextLow)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(FitSurfaceInteractive)
                        .border(1.dp, FitOutline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.initials,
                        style = FitTextStyles.bodyStrong,
                        color = Emerald,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = state.name, style = FitTextStyles.display, color = TextPrimary)
                    if (state.goalLabel.isNotBlank()) {
                        Pill(text = state.goalLabel, color = Emerald)
                    }
                }
            }
        }

        SectionHeader(title = "Dati fisici", subtitle = "Valori modificabili in qualsiasi momento")

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileFieldInput(
                    label = "Nome",
                    value = state.name,
                    error = state.errors[ProfileField.NAME],
                    onValueChange = { onFieldChange(ProfileField.NAME, it) },
                )
                ProfileFieldInput(
                    label = "Peso",
                    value = state.weight,
                    unit = "kg",
                    error = state.errors[ProfileField.WEIGHT],
                    numeric = true,
                    onValueChange = { onFieldChange(ProfileField.WEIGHT, it) },
                )
                ProfileFieldInput(
                    label = "Altezza",
                    value = state.height,
                    unit = "cm",
                    error = state.errors[ProfileField.HEIGHT],
                    numeric = true,
                    onValueChange = { onFieldChange(ProfileField.HEIGHT, it) },
                )
                ProfileFieldInput(
                    label = "Età",
                    value = state.age,
                    unit = "anni",
                    error = state.errors[ProfileField.AGE],
                    numeric = true,
                    onValueChange = { onFieldChange(ProfileField.AGE, it) },
                )
                if (state.sexLabel.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sesso",
                            style = FitTextStyles.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = state.sexLabel,
                            style = FitTextStyles.numericS,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }

        if (state.goalOptions.isNotEmpty()) {
            SectionHeader(title = "Obiettivo")
            SegmentedControl(
                options = state.goalOptions,
                selectedIndex = state.selectedGoalIndex,
                onSelect = onGoalSelect,
            )
        }

        SectionHeader(title = "Preferenze")
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileFieldInput(
                    label = "Pasti al giorno",
                    value = state.mealsPerDay,
                    unit = "pasti",
                    error = state.errors[ProfileField.MEALS_PER_DAY],
                    numeric = true,
                    onValueChange = { onFieldChange(ProfileField.MEALS_PER_DAY, it) },
                )
                ProfileFieldInput(
                    label = "Giorni di allenamento",
                    value = state.trainingDays,
                    unit = "/ sett",
                    error = state.errors[ProfileField.TRAINING_DAYS],
                    numeric = true,
                    onValueChange = { onFieldChange(ProfileField.TRAINING_DAYS, it) },
                )
                ProfileFieldInput(
                    label = "Attrezzatura",
                    value = state.equipment,
                    error = state.errors[ProfileField.EQUIPMENT],
                    onValueChange = { onFieldChange(ProfileField.EQUIPMENT, it) },
                )
            }
        }

        SectionHeader(
            title = "Salute",
            subtitle = "Allergie, limitazioni e dolori: il coach ne tiene conto",
        )
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileFieldInput(
                    label = "Allergie e intolleranze",
                    value = state.allergies,
                    error = state.errors[ProfileField.ALLERGIES],
                    multiline = true,
                    onValueChange = { onFieldChange(ProfileField.ALLERGIES, it) },
                )
                ProfileFieldInput(
                    label = "Limitazioni fisiche",
                    value = state.limitations,
                    error = state.errors[ProfileField.LIMITATIONS],
                    multiline = true,
                    onValueChange = { onFieldChange(ProfileField.LIMITATIONS, it) },
                )
                ProfileFieldInput(
                    label = "Dolori o movimenti da evitare",
                    value = state.pains,
                    error = state.errors[ProfileField.PAINS],
                    multiline = true,
                    onValueChange = { onFieldChange(ProfileField.PAINS, it) },
                )
            }
        }

        if (state.planImpactWarning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FitRadius.md))
                    .background(FitSurfaceVariant)
                    .border(
                        width = 1.dp,
                        color = WarningAmber.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(FitRadius.md),
                    )
                    .padding(14.dp),
            ) {
                Text(
                    text = "Questo dato influenza il piano: il coach proporrà un aggiornamento, nessuna modifica automatica.",
                    style = FitTextStyles.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        state.savedMessage?.let { message ->
            Text(text = message, style = FitTextStyles.caption, color = SuccessGreen)
        }

        Spacer(modifier = Modifier.height(2.dp))
        PrimaryButton(
            text = if (state.isSaving) "Salvataggio…" else "Salva modifiche",
            onClick = onSave,
            enabled = !state.isSaving,
        )
    }
}

@Composable
private fun ProfileFieldInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String = "",
    error: String? = null,
    numeric: Boolean = false,
    multiline: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label, style = FitTextStyles.caption) },
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            isError = error != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            ),
            trailingIcon = {
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        style = FitTextStyles.numericS,
                        color = TextLow,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            },
            shape = RoundedCornerShape(FitRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = FitSurfaceVariant,
                unfocusedContainerColor = FitSurfaceVariant,
                focusedBorderColor = Emerald,
                unfocusedBorderColor = if (error != null) ErrorRed else FitOutline,
                cursorColor = Emerald,
                focusedLabelColor = Emerald,
                unfocusedLabelColor = TextLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { message ->
            Text(text = message, style = FitTextStyles.caption, color = ErrorRed)
        }
    }
}
