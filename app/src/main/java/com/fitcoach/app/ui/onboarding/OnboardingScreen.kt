package com.fitcoach.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.ui.components.LoadingBox
import com.fitcoach.app.ui.components.SectionCard
import com.fitcoach.app.ui.components.SectionHeader
import com.fitcoach.app.ui.navigation.Routes
import com.fitcoach.app.ui.theme.Emerald
import com.fitcoach.app.ui.theme.ErrorRed
import com.fitcoach.app.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }

    if (state.finished) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Profilo salvato!", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))
            Text(
                "Piano alimentare e scheda di allenamento generati.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary
            )
            if (state.warnings.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                state.warnings.forEach {
                    Text("· $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { navController.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            } }) {
                Text("Vai alla dashboard")
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Configura il tuo profilo", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (state.step + 1) / state.totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = Emerald
            )
            Spacer(Modifier.height(4.dp))
            Text("Passo ${state.step + 1} di ${state.totalSteps}",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            when (state.step) {
                0 -> item { StepAnagrafica(state, viewModel) }
                1 -> item { StepObiettivo(state, viewModel) }
                2 -> item { StepAttrezzatura(state, viewModel) }
                3 -> item { StepNutrizione(state, viewModel) }
                4 -> item { StepSalute(state, viewModel) }
            }

            state.error?.let {
                item {
                    Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.step > 0) {
                OutlinedButton(
                    onClick = { viewModel.back() },
                    modifier = Modifier.weight(1f)
                ) { Text("Indietro") }
            }
            Button(
                onClick = {
                    if (state.step == state.totalSteps - 1) viewModel.finish() else viewModel.next()
                },
                modifier = Modifier.weight(1f),
                enabled = !state.saving
            ) {
                Text(
                    when {
                        state.saving -> "Generazione..."
                        state.step == state.totalSteps - 1 -> "Genera programmi"
                        else -> "Avanti"
                    }
                )
            }
        }
    }
}

@Composable
private fun StepAnagrafica(state: OnboardingUiState, vm: OnboardingViewModel) {
    SectionCard {
        Column {
            SectionHeader("Dati personali", "Servono per i calcoli energetici")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.name, onValueChange = { v -> vm.update { it.copy(name = v) } },
                label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.age, onValueChange = { v -> vm.update { it.copy(age = v.filter(Char::isDigit)) } },
                    label = { Text("Eta") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.heightCm, onValueChange = { v -> vm.update { it.copy(heightCm = v) } },
                    label = { Text("Altezza (cm)") }, singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.weightKg, onValueChange = { v -> vm.update { it.copy(weightKg = v) } },
                    label = { Text("Peso (kg)") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.bodyFat, onValueChange = { v -> vm.update { it.copy(bodyFat = v) } },
                    label = { Text("Massa grassa % (facolt.)") }, singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Sesso biologico (per le formule)", style = MaterialTheme.typography.bodySmall,
                color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sex.entries.forEach { sex ->
                    FilterChip(
                        selected = state.sex == sex,
                        onClick = { vm.update { it.copy(sex = sex) } },
                        label = { Text(sexLabel(sex)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepObiettivo(state: OnboardingUiState, vm: OnboardingViewModel) {
    SectionCard {
        Column {
            SectionHeader("Obiettivo e allenamento")
            Spacer(Modifier.height(12.dp))
            Goal.entries.forEach { goal ->
                FilterChip(
                    selected = state.goal == goal,
                    onClick = { vm.update { it.copy(goal = goal) } },
                    label = { Column { Text(goal.label, fontWeight = FontWeight.SemiBold); Text(goal.description,
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Esperienza di allenamento", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            TrainingExperience.entries.forEach { exp ->
                FilterChip(
                    selected = state.experience == exp,
                    onClick = { vm.update { it.copy(experience = exp) } },
                    label = { Text(exp.label) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Giorni disponibili a settimana", style = MaterialTheme.typography.bodySmall,
                color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (2..6).forEach { d ->
                    FilterChip(
                        selected = state.trainingDays == d,
                        onClick = { vm.update { it.copy(trainingDays = d) } },
                        label = { Text("$d") }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepAttrezzatura(state: OnboardingUiState, vm: OnboardingViewModel) {
    SectionCard {
        Column {
            SectionHeader("Attrezzatura disponibile", "Determina gli esercizi generabili")
            Spacer(Modifier.height(12.dp))
            Equipment.entries.forEach { eq ->
                FilterChip(
                    selected = eq in state.equipment,
                    onClick = { vm.toggleEquipment(eq) },
                    label = { Text(eq.label) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StepNutrizione(state: OnboardingUiState, vm: OnboardingViewModel) {
    SectionCard {
        Column {
            SectionHeader("Preferenze alimentari")
            Spacer(Modifier.height(12.dp))
            DietaryPreference.entries.forEach { pref ->
                FilterChip(
                    selected = pref in state.preferences,
                    onClick = { vm.togglePreference(pref) },
                    label = { Text(pref.label) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.excludedFoods, onValueChange = { v -> vm.update { it.copy(excludedFoods = v) } },
                label = { Text("Alimenti esclusi (separati da virgola)") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.allergies, onValueChange = { v -> vm.update { it.copy(allergies = v) } },
                label = { Text("Allergie / intolleranze") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("Pasti al giorno", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (3..6).forEach { m ->
                    FilterChip(
                        selected = state.mealsPerDay == m,
                        onClick = { vm.update { it.copy(mealsPerDay = m) } },
                        label = { Text("$m") }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.budgetEur, onValueChange = { v -> vm.update { it.copy(budgetEur = v) } },
                label = { Text("Budget settimanale € (facolt.)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepSalute(state: OnboardingUiState, vm: OnboardingViewModel) {
    SectionCard {
        Column {
            SectionHeader("Salute e limitazioni", "Per la tua sicurezza")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.physicalLimitations,
                onValueChange = { v -> vm.update { it.copy(physicalLimitations = v) } },
                label = { Text("Limitazioni fisiche (es. spalla dx)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.movementsToAvoid,
                onValueChange = { v -> vm.update { it.copy(movementsToAvoid = v) } },
                label = { Text("Movimenti da evitare (es. stacco da terra)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "FitCoach non è un professionista sanitario e non fornisce diagnosi. " +
                    "I piani generati non sostituiscono il parere di un medico o di un professionista qualificato.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

private fun sexLabel(sex: Sex) = when (sex) {
    Sex.MALE -> "Uomo"
    Sex.FEMALE -> "Donna"
    Sex.UNSPECIFIED -> "Non specificato"
}
