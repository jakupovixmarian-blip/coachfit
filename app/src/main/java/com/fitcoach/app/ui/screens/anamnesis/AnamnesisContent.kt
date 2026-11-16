package com.fitcoach.app.ui.screens.anamnesis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.Pill
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.SegmentedControl
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.BrandSoft
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitOutline
import com.fitcoach.app.ui.theme.FitRadius
import com.fitcoach.app.ui.theme.FitSurfaceVariant
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary

/* ============================================================================
 * Anamnesi — primo avvio e modifica successiva. Cinque passi, un solo punto di
 * ingresso al profilo: al termine si chiama onSubmit, che invoca
 * buildFromAnamnesis. Nessun seed: finché l'anamnesi non è completa l'app resta
 * su questa schermata.
 *
 * Solo presentazione: lo stato arriva da fuori, nessuna scrittura su database.
 * ========================================================================== */

@Composable
fun AnamnesisContent(
    state: AnamnesisUiState,
    onChange: (AnamnesisUiState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
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
        Column {
            Text(text = "Anamnesi", style = FitTextStyles.display, color = TextPrimary)
            Text(
                text = "Passo ${state.step + 1} di ${state.stepCount}",
                style = FitTextStyles.caption,
                color = TextSecondary,
            )
        }

        StepBar(progress = state.progress)

        state.errorMessage?.let { message ->
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = message, style = FitTextStyles.bodySmall, color = ErrorRed)
                    GhostButton(text = "Riprova", onClick = onRetry)
                }
            }
        }

        when (state.step) {
            0 -> AnthropometricsStep(state, onChange)
            1 -> GoalStep(state, onChange)
            2 -> ExperienceStep(state, onChange)
            3 -> EquipmentStep(state, onChange)
            else -> ConstraintsStep(state, onChange)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(
                text = "Indietro",
                onClick = onBack,
                enabled = state.step > 0 && !state.isGenerating,
                modifier = Modifier.weight(1f),
            )
            if (state.isLastStep) {
                PrimaryButton(
                    text = if (state.isGenerating) "Generazione…" else "Genera piano",
                    onClick = onSubmit,
                    enabled = !state.isGenerating,
                    modifier = Modifier.weight(1f),
                )
            } else {
                PrimaryButton(
                    text = "Avanti",
                    onClick = onNext,
                    enabled = state.canGoNext,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StepBar(progress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(5) { index ->
            val active = progress > index / 5f
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (active) Emerald else FitSurfaceVariant),
            )
        }
    }
}

@Composable
private fun AnthropometricsStep(state: AnamnesisUiState, onChange: (AnamnesisUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Dati di partenza", subtitle = "Servono per metabolismo e target calorico")
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedControl(
                    options = SexOption.entries.map { it.label },
                    selectedIndex = SexOption.entries.indexOf(state.sex),
                    onSelect = { onChange(state.copy(sex = SexOption.entries[it])) },
                )
                NumberField(
                    label = "Età (anni)",
                    value = state.age,
                    onValueChange = { onChange(state.copy(age = it)) },
                )
                NumberField(
                    label = "Altezza (cm)",
                    value = state.heightCm,
                    onValueChange = { onChange(state.copy(heightCm = it)) },
                )
                NumberField(
                    label = "Peso (kg)",
                    value = state.weightKg,
                    onValueChange = { onChange(state.copy(weightKg = it)) },
                )
            }
        }
    }
}

@Composable
private fun GoalStep(state: AnamnesisUiState, onChange: (AnamnesisUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Obiettivo", subtitle = "Determina ripartizione calorica e volumi")
        GoalOption.entries.forEach { option ->
            val selected = state.goal == option
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FitRadius.lg))
                    .background(if (selected) BrandSoft else FitSurfaceVariant)
                    .clickable { onChange(state.copy(goal = option)) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = option.label,
                        style = FitTextStyles.bodyStrong,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Pill(text = "Scelto", color = Emerald)
                    }
                }
                Text(text = option.description, style = FitTextStyles.caption, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ExperienceStep(state: AnamnesisUiState, onChange: (AnamnesisUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Esperienza", subtitle = "Cambia frequenza e progressione iniziale")
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedControl(
                    options = ExperienceOption.entries.map { it.label },
                    selectedIndex = ExperienceOption.entries.indexOf(state.experience),
                    onSelect = { onChange(state.copy(experience = ExperienceOption.entries[it])) },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Giorni a settimana",
                            style = FitTextStyles.bodySmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Disponibilità reale",
                            style = FitTextStyles.caption,
                            color = TextLow,
                        )
                    }
                    GhostButton(
                        text = "−",
                        onClick = {
                            onChange(state.copy(daysPerWeek = (state.daysPerWeek - 1).coerceAtLeast(2)))
                        },
                        enabled = state.daysPerWeek > 2,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.daysPerWeek}",
                        style = FitTextStyles.numericS,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GhostButton(
                        text = "+",
                        onClick = {
                            onChange(state.copy(daysPerWeek = (state.daysPerWeek + 1).coerceAtMost(6)))
                        },
                        enabled = state.daysPerWeek < 6,
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentStep(state: AnamnesisUiState, onChange: (AnamnesisUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Attrezzatura e attività", subtitle = "Vincolano la scelta degli esercizi")
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedControl(
                    options = EquipmentOption.entries.map { it.label },
                    selectedIndex = EquipmentOption.entries.indexOf(state.equipment),
                    onSelect = { onChange(state.copy(equipment = EquipmentOption.entries[it])) },
                )
                Text(text = "Attività quotidiana", style = FitTextStyles.caption, color = TextLow)
                SegmentedControl(
                    options = ActivityOption.entries.map { it.label },
                    selectedIndex = ActivityOption.entries.indexOf(state.activity),
                    onSelect = { onChange(state.copy(activity = ActivityOption.entries[it])) },
                )
            }
        }
    }
}

@Composable
private fun ConstraintsStep(state: AnamnesisUiState, onChange: (AnamnesisUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Vincoli e recupero", subtitle = "Allergie e limitazioni restano vincolanti")
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MultilineField(
                    label = "Allergie o intolleranze",
                    value = state.allergies,
                    onValueChange = { onChange(state.copy(allergies = it)) },
                )
                MultilineField(
                    label = "Limitazioni fisiche o movimenti da evitare",
                    value = state.limitations,
                    onValueChange = { onChange(state.copy(limitations = it)) },
                )
                NumberField(
                    label = "Ore di sonno medie",
                    value = state.sleepHours,
                    onValueChange = { onChange(state.copy(sleepHours = it)) },
                )
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text(text = label, style = FitTextStyles.caption) },
        singleLine = true,
        shape = RoundedCornerShape(FitRadius.md),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = FitSurfaceVariant,
            unfocusedContainerColor = FitSurfaceVariant,
            focusedBorderColor = Emerald,
            unfocusedBorderColor = FitOutline,
            cursorColor = Emerald,
            focusedLabelColor = Emerald,
            unfocusedLabelColor = TextLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MultilineField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, style = FitTextStyles.caption) },
        minLines = 2,
        shape = RoundedCornerShape(FitRadius.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = FitSurfaceVariant,
            unfocusedContainerColor = FitSurfaceVariant,
            focusedBorderColor = Emerald,
            unfocusedBorderColor = FitOutline,
            cursorColor = Emerald,
            focusedLabelColor = Emerald,
            unfocusedLabelColor = TextLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
