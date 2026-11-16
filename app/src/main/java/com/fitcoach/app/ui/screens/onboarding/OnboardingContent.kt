package com.fitcoach.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.components.AnimatedBar
import com.fitcoach.app.ui.components.DataRow
import com.fitcoach.app.ui.components.FitChoiceChip
import com.fitcoach.app.ui.components.FitTextField
import com.fitcoach.app.ui.components.GhostButton
import com.fitcoach.app.ui.components.PrimaryButton
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.components.StepDots
import com.fitcoach.app.ui.components.fitScreenBackground
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.FitTextStyles
import com.fitcoach.app.ui.theme.TextLow
import com.fitcoach.app.ui.theme.TextPrimary
import com.fitcoach.app.ui.theme.TextSecondary
import com.fitcoach.app.ui.theme.WarningAmber

/* =============================================================================
 * FitCoach 2.0 — Onboarding · OnboardingContent
 * -----------------------------------------------------------------------------
 * Flusso guidato iniziale su 8 passi reali:
 *   StepAnagrafica · StepObiettivo · StepDettagliAllenamento · StepAttrezzatura
 *   StepNutrizione · StepStileVita · StepSalute · StepRiepilogo
 *
 * Solo presentazione: i campi raccolti alimentano l'anamnesi e la generazione
 * AI, che restano nel ViewModel. Nessuna schermata inventata, nessun dato finto.
 * ========================================================================== */

/**
 * Azioni dell'onboarding. Tutte obbligatorie: nessun pulsante decorativo.
 */
class OnboardingActions(
    val onNameChange: (String) -> Unit,
    val onAgeChange: (String) -> Unit,
    val onHeightChange: (String) -> Unit,
    val onWeightChange: (String) -> Unit,
    val onBodyFatChange: (String) -> Unit,
    val onSexSelect: (SexOption) -> Unit,
    val onGoalSelect: (GoalOption) -> Unit,
    val onExperienceSelect: (ExperienceOption) -> Unit,
    val onDaysPerWeekChange: (String) -> Unit,
    val onSessionMinutesChange: (String) -> Unit,
    val onPreferredTimeChange: (String) -> Unit,
    val onTrainingLocationChange: (String) -> Unit,
    val onEquipmentToggle: (EquipmentOption) -> Unit,
    val onDietSelect: (DietPreference) -> Unit,
    val onExcludedFoodsChange: (String) -> Unit,
    val onAllergiesChange: (String) -> Unit,
    val onIntolerancesChange: (String) -> Unit,
    val onMealsPerDayChange: (String) -> Unit,
    val onSleepHoursChange: (String) -> Unit,
    val onSleepQualitySelect: (SleepQuality) -> Unit,
    val onStressLevelChange: (String) -> Unit,
    val onActivityLevelSelect: (ActivityLevel) -> Unit,
    val onWorkScheduleChange: (String) -> Unit,
    val onWaterLitersChange: (String) -> Unit,
    val onLimitationToggle: (String) -> Unit,
    val onAvoidMovementsChange: (String) -> Unit,
    val onMedicalNotesChange: (String) -> Unit,
    val onBack: () -> Unit,
    val onNext: () -> Unit,
    val onSkipStep: () -> Unit,
    val onJumpToStep: (OnboardingStep) -> Unit,
    val onStartAnamnesis: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fitScreenBackground())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OnboardingHeader(state)

        if (state.isLoading) {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Preparo il tuo profilo…",
                        style = FitTextStyles.bodySmall,
                        color = TextSecondary,
                    )
                    AnimatedBar(fraction = 0.6f, height = 6.dp)
                }
            }
            return@Column
        }

        if (state.errorMessage != null) {
            SectionCard {
                Text(
                    text = state.errorMessage,
                    style = FitTextStyles.bodySmall,
                    color = ErrorRed,
                )
            }
        }

        when (state.step) {
            OnboardingStep.ANAGRAFICA -> StepAnagrafica(state, actions)
            OnboardingStep.OBIETTIVO -> StepObiettivo(state, actions)
            OnboardingStep.DETTAGLI_ALLENAMENTO -> StepDettagliAllenamento(state, actions)
            OnboardingStep.ATTREZZATURA -> StepAttrezzatura(state, actions)
            OnboardingStep.NUTRIZIONE -> StepNutrizione(state, actions)
            OnboardingStep.STILE_VITA -> StepStileVita(state, actions)
            OnboardingStep.SALUTE -> StepSalute(state, actions)
            OnboardingStep.RIEPILOGO -> StepRiepilogo(state, actions)
        }

        OnboardingFooter(state = state, actions = actions)
    }
}

/* --------------------------------------------------------------------------- */

@Composable
private fun OnboardingHeader(state: OnboardingUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.stepLabel,
                style = FitTextStyles.caption,
                color = Emerald,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(state.progress * 100).toInt()}%",
                style = FitTextStyles.caption,
                color = TextLow,
            )
        }
        AnimatedBar(fraction = state.progress, height = 4.dp)
        StepDots(count = state.stepCount, current = state.stepIndex)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = state.step.title, style = FitTextStyles.display, color = TextPrimary)
        Text(text = state.step.subtitle, style = FitTextStyles.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun StepAnagrafica(state: OnboardingUiState, actions: OnboardingActions) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = "Dati personali", subtitle = "Servono per il calcolo energetico")
            FitTextField(
                value = state.name,
                onValueChange = actions.onNameChange,
                label = "Nome",
                placeholder = "Come ti chiami",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FitTextField(
                    value = state.age,
                    onValueChange = actions.onAgeChange,
                    label = "Età",
                    placeholder = "anni",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                FitTextField(
                    value = state.heightCm,
                    onValueChange = actions.onHeightChange,
                    label = "Altezza",
                    placeholder = "cm",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FitTextField(
                    value = state.weightKg,
                    onValueChange = actions.onWeightChange,
                    label = "Peso",
                    placeholder = "kg",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                FitTextField(
                    value = state.bodyFatPercent,
                    onValueChange = actions.onBodyFatChange,
                    label = "Massa grassa",
                    placeholder = "% (facoltativo)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(text = "Sesso biologico", style = FitTextStyles.caption, color = TextLow)
            ChoiceRow(
                labels = SexOption.entries.map { it.label },
                selectedIndex = SexOption.entries.indexOf(state.sex),
                onSelect = { actions.onSexSelect(SexOption.entries[it]) },
            )
        }
    }
}

@Composable
private fun StepObiettivo(state: OnboardingUiState, actions: OnboardingActions) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = "Obiettivo", subtitle = "Il programma viene costruito su questo")
            ChipGrid(
                labels = GoalOption.entries.map { it.label },
                isSelected = { GoalOption.entries[it] == state.goal },
                onSelect = { actions.onGoalSelect(GoalOption.entries[it]) },
            )
        }
    }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = "Esperienza",
                subtitle = "Determina volume iniziale e velocità di progressione",
            )
            ChipGrid(
                labels = ExperienceOption.entries.map { it.label },
                isSelected = { ExperienceOption.entries[it] == state.experience },
                onSelect = { actions.onExperienceSelect(ExperienceOption.entries[it]) },
            )
        }
    }
}

@Composable
private fun StepDettagliAllenamento(state: OnboardingUiState, actions: OnboardingActions) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = "Dettagli allenamento", subtitle = "Vincoli reali della tua settimana")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FitTextField(
                    value = state.daysPerWeek,
                    onValueChange = actions.onDaysPerWeekChange,
                    label = "Giorni a settimana",
                    placeholder = "1-7",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                FitTextField(
                    value = state.sessionMinutes,
                    onValueChange = actions.onSessionMinutesChange,
                    label = "Durata seduta",
                    placeholder = "min",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            FitTextField(
                value = state.preferredTime,
                onValueChange = actions.onPreferredTimeChange,
                label = "Orario preferito",
                placeholder = "es. 18:30",
            )
            FitTextField(
                value = state.trainingLocation,
                onValueChange = actions.onTrainingLocationChange,
                label = "Dove ti alleni",
                placeholder = "es. palestra, casa",
            )
        }
    }
}

@Composable
private fun StepAttrezzatura(state: OnboardingUiState, actions: OnboardingActions) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = "Attrezzatura disponibile",
                subtitle = "Solo gli esercizi compatibili entreranno in scheda",
            )
            if (state.equipment.isEmpty()) {
                Text(
                    text = "Attrezzatura non ancora caricata.",
                    style = FitTextStyles.bodySmall,
                    color = TextLow,
                )
            }
            state.equipment.forEach { choice ->
                FitChoiceChip(
                    text = choice.option.label,
                    selected = choice.isSelected,
                    onClick = { actions.onEquipmentToggle(choice.option) },
                )
            }
            if (state.equipment.none { it.isSelected }) {
                Text(
                    text = "Seleziona almeno un'attrezzatura (anche corpo libero).",
                    style = FitTextStyles.caption,
                    color = WarningAmber,
                )
            }
        }
    }
}

@Composable
private fun StepNutrizione(state: OnboardingUiState, actions: OnboardingActions) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = "Preferenze alimentari", subtitle = "Nessun alimento viene imposto")
            Text(text = "Stile alimentare", style = FitTextStyles.caption, color = TextLow)
            ChipGrid(
                labels = DietPreference.entries.map { it.label },
                isSelected = { DietPreference.entries[it] == state.diet },
                onSelect = { actions.onDietSelect(DietPreference.entries[it]) },
            )
            FitTextField(
                value = state.excludedFoods,
                onValueChange = actions.onExcludedFoodsChange,
                label = "Alimenti esclusi",
                placeholder = "separati da virgola",
                singleLine = false,
            )
            FitTextField(
                value = state.allergies,
                onValueChange = actions.onAllergiesChange,
                label = "Allergie",
                placeholder = "separate da virgola",
                singleLine = false,
            )
            FitTextField(
                value = state.intolerances,
                onValueChange = actions.onIntolerancesChange,
                label = "Intolleranze",
                placeholder = "separate da virgola",
                singleLine = false,
            )
            FitTextField(
                value = state.mealsPerDay,
                onValueChange = actions.onMealsPerDayChange,
                label = "Pasti al giorno",
                placeholder = "es. 5",
                keyboardType = KeyboardType.Number,
            )
        }
    }
}

@Composable
private fun StepStileVita(state: OnboardingUiState, actions: OnboardingActions) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = "Sonno e recupero", subtitle = "Il recupero guida il volume sostenibile")
            FitTextField(
                value = state.sleepHours,
                onValueChange = actions.onSleepHoursChange,
                label = "Ore di sonno",
                placeholder = "es. 7.5",
                keyboardType = KeyboardType.Decimal,
            )
            Text(text = "Qualità del sonno", style = FitTextStyles.caption, color = TextLow)
            ChoiceRow(
                labels = SleepQuality.entries.map { it.label },
                selectedIndex = SleepQuality.entries.indexOf(state.sleepQuality),
                onSelect = { actions.onSleepQualitySelect(SleepQuality.entries[it]) },
            )
            FitTextField(
                value = state.stressLevel,
                onValueChange = actions.onStressLevelChange,
                label = "Livello di stress",
                placeholder = "1-10",
                keyboardType = KeyboardType.Number,
            )
            Text(text = "Attività quotidiana", style = FitTextStyles.caption, color = TextLow)
            ChipGrid(
                labels = ActivityLevel.entries.map { it.label },
                isSelected = { ActivityLevel.entries[it] == state.activityLevel },
                onSelect = { actions.onActivityLevelSelect(ActivityLevel.entries[it]) },
            )
            FitTextField(
                value = state.workSchedule,
                onValueChange = actions.onWorkScheduleChange,
                label = "Orari di lavoro",
                placeholder = "es. 9:00-18:00, turni",
            )
            FitTextField(
                value = state.waterLiters,
                onValueChange = actions.onWaterLitersChange,
                label = "Acqua al giorno",
                placeholder = "litri",
                keyboardType = KeyboardType.Decimal,
            )
        }
    }
}

@Composable
private fun StepSalute(state: OnboardingUiState, actions: OnboardingActions) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                title = "Salute e limitazioni",
                subtitle = "Gli esercizi incompatibili vengono esclusi dalla scheda",
            )
            if (state.limitations.isEmpty()) {
                Text(
                    text = "Nessuna limitazione da selezionare.",
                    style = FitTextStyles.bodySmall,
                    color = TextLow,
                )
            }
            state.limitations.forEach { limitation ->
                FitChoiceChip(
                    text = limitation.label,
                    selected = limitation.isSelected,
                    onClick = { actions.onLimitationToggle(limitation.label) },
                )
            }
            FitTextField(
                value = state.avoidMovements,
                onValueChange = actions.onAvoidMovementsChange,
                label = "Movimenti da evitare",
                placeholder = "es. stacco da terra",
                singleLine = false,
            )
            FitTextField(
                value = state.medicalNotes,
                onValueChange = actions.onMedicalNotesChange,
                label = "Note mediche",
                placeholder = "facoltativo",
                singleLine = false,
            )
        }
    }
}

@Composable
private fun StepRiepilogo(state: OnboardingUiState, actions: OnboardingActions) {
    if (state.warnings.isNotEmpty()) {
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "DA CONTROLLARE", style = FitTextStyles.caption, color = WarningAmber)
                state.warnings.forEach { warning ->
                    Text(
                        text = warning,
                        style = FitTextStyles.bodySmall,
                        color = TextPrimary,
                    )
                }
            }
        }
    }

    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
                title = "Riepilogo",
                subtitle = "Tocca una voce per correggerla",
            )
            if (state.summary.isEmpty()) {
                Text(
                    text = "Riepilogo non ancora disponibile.",
                    style = FitTextStyles.bodySmall,
                    color = TextLow,
                )
            }
            state.summary.forEach { row ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    DataRow(
                        label = row.label,
                        value = row.value,
                        valueColor = if (row.isMissing) WarningAmber else TextPrimary,
                    )
                    FitChoiceChip(
                        text = "Modifica · ${row.step.title}",
                        selected = false,
                        onClick = { actions.onJumpToStep(row.step) },
                    )
                }
            }
        }
    }

    if (state.generationError != null) {
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = state.generationError,
                    style = FitTextStyles.bodySmall,
                    color = ErrorRed,
                )
                GhostButton(text = "Riprova generazione AI", onClick = actions.onRetry)
            }
        }
    }
}

@Composable
private fun OnboardingFooter(state: OnboardingUiState, actions: OnboardingActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.isLastStep) {
            PrimaryButton(
                text = if (state.isStartingAnamnesis) {
                    "Generazione in corso…"
                } else {
                    "Inizia l'anamnesi guidata"
                },
                onClick = actions.onStartAnamnesis,
                enabled = state.canStartAnamnesis && !state.isStartingAnamnesis,
            )
        } else {
            PrimaryButton(
                text = "Continua",
                onClick = actions.onNext,
                enabled = state.canContinue,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(
                text = "Indietro",
                onClick = actions.onBack,
                enabled = state.canGoBack,
                modifier = Modifier.weight(1f),
            )
            GhostButton(
                text = "Salta passo",
                onClick = actions.onSkipStep,
                enabled = !state.isLastStep,
                modifier = Modifier.weight(1f),
            )
        }
        if (!state.isLastStep) {
            Text(
                text = "Puoi completare i dettagli anche nell'anamnesi.",
                style = FitTextStyles.caption,
                color = TextLow,
            )
        }
    }
}

/* --------------------------------------------------------------------------- */

@Composable
private fun ChoiceRow(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            FitChoiceChip(
                text = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChipGrid(
    labels: List<String>,
    isSelected: (Int) -> Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            FitChoiceChip(
                text = label,
                selected = isSelected(index),
                onClick = { onSelect(index) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
