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
import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.BudgetScope
import com.fitcoach.app.domain.model.CookingTimeAvailable
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.ui.settings.GearInventorySection
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Level3
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TechnicalLevel
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.TransportMode
import com.fitcoach.app.domain.model.WorkShift
import com.fitcoach.app.domain.model.WorkType
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
                "Ora completiamo l'anamnesi guidata: il coach ti farà alcune domande " +
                    "di approfondimento prima di generare alimentazione e allenamento.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary
            )
            if (state.warnings.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                state.warnings.forEach {
                    Text("· $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = {
                navController.navigate(Routes.ANAMNESIS) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            }) {
                Text("Continua con l'anamnesi")
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
                2 -> item { StepStileVita(state, viewModel) }
                3 -> item { StepAttrezzatura(state, viewModel) }
                4 -> item { StepDettagliAllenamento(state, viewModel) }
                5 -> item { StepNutrizione(state, viewModel) }
                6 -> item { StepSalute(state, viewModel) }
                7 -> item { StepRiepilogo(state) }
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
                        state.step == state.totalSteps - 1 -> "Salva profilo"
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
                        onClick = { vm.select("sex") { it.copy(sex = sex) } },
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
                    onClick = { vm.select("goal") { it.copy(goal = goal) } },
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
                    onClick = { vm.select("experience") { it.copy(experience = exp) } },
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
                        onClick = { vm.select("trainingDaysPerWeek") { it.copy(trainingDays = d) } },
                        label = { Text("$d") }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepAttrezzatura(state: OnboardingUiState, vm: OnboardingViewModel) {
    Column {
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
        Spacer(Modifier.height(12.dp))
        GearInventorySection(
            inventory = state.gearInventory,
            onTogglePreset = vm::toggleGearPreset,
            onUpdateItem = vm::updateGearItem,
            onAddCustom = vm::addCustomGear,
            onRemove = vm::removeGearItem
        )
        if (state.gearInventory.isEmpty) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Inventario non compilato: il coach non conosce ancora i tuoi attrezzi e " +
                    "potrebbe proporti esercizi non eseguibili. Puoi completarlo anche dopo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.likedFoods, onValueChange = { v -> vm.update { it.copy(likedFoods = v) } },
                label = { Text("Alimenti che ti piacciono") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.dislikedFoods, onValueChange = { v -> vm.update { it.copy(dislikedFoods = v) } },
                label = { Text("Alimenti che NON ti piacciono") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("Colazione preferita", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BreakfastPreference.entries.forEach { bp ->
                    FilterChip(
                        selected = state.breakfastPreference == bp,
                        onClick = { vm.select("breakfastPreference") { it.copy(breakfastPreference = bp) } },
                        label = { Text(bp.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Pasti al giorno", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (3..6).forEach { m ->
                    FilterChip(
                        selected = state.mealsPerDay == m,
                        onClick = { vm.select("mealsPerDay") { it.copy(mealsPerDay = m) } },
                        label = { Text("$m") }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Tempo per cucinare", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CookingTimeAvailable.entries.forEach { ct ->
                    FilterChip(
                        selected = state.cookingTime == ct,
                        onClick = { vm.select("cookingTimeAvailable") { it.copy(cookingTime = ct) } },
                        label = { Text(ct.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = state.mealPrepPossible,
                    onClick = { vm.select("mealPrepPossible") { it.copy(mealPrepPossible = !it.mealPrepPossible) } },
                    label = { Text("Posso preparare i pasti in anticipo (meal prep)") }
                )
            }
            Spacer(Modifier.height(8.dp))
            FilterChip(
                selected = state.prefersSimpleFoods,
                onClick = { vm.update { it.copy(prefersSimpleFoods = !it.prefersSimpleFoods) } },
                label = { Text("Preferisco alimenti semplici da preparare") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.budgetEur, onValueChange = { v -> vm.update { it.copy(budgetEur = v) } },
                label = { Text("Budget settimanale € (facolt.)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.monthlyBudgetEur,
                onValueChange = { v -> vm.update { it.copy(monthlyBudgetEur = v) } },
                label = { Text("Budget alimentare mensile € (facolt.)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Il budget mensile è un vincolo reale: FitCoach calcola il costo del piano " +
                    "e lo riformula se supera il budget.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text("A cosa si riferisce il budget?", style = MaterialTheme.typography.bodySmall,
                color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            BudgetScope.entries.forEach { scope ->
                FilterChip(
                    selected = state.budgetScope == scope,
                    onClick = { vm.select("budgetScope") { it.copy(budgetScope = scope) } },
                    label = { Text(scope.label) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Persone coperte dal budget: ", style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
                OutlinedButton(onClick = {
                    vm.setBudgetPersons(state.budgetPersons - 1)
                }) { Text("−") }
                Text("  ${state.budgetPersons}  ", color = MaterialTheme.colorScheme.onBackground)
                OutlinedButton(onClick = {
                    vm.setBudgetPersons(state.budgetPersons + 1)
                }) { Text("+") }
            }
        }
    }
}

@Composable
private fun StepStileVita(state: OnboardingUiState, vm: OnboardingViewModel) {
    SectionCard {
        Column {
            SectionHeader("Stile di vita e recupero", "Migliora la stima del dispendio energetico")
            Spacer(Modifier.height(12.dp))
            Text("Tipo di lavoro", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            WorkType.entries.forEach { wt ->
                FilterChip(
                    selected = state.workType == wt,
                    onClick = { vm.select("workType") { it.copy(workType = wt) } },
                    label = { Text(wt.label) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Turni di lavoro", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkShift.entries.forEach { ws ->
                    FilterChip(
                        selected = state.workShift == ws,
                        onClick = { vm.select("workShift") { it.copy(workShift = ws) } },
                        label = { Text(ws.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.dailySteps,
                    onValueChange = { v -> vm.update { it.copy(dailySteps = v.filter(Char::isDigit)) } },
                    label = { Text("Passi/giorno") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.sleepHours,
                    onValueChange = { v -> vm.update { it.copy(sleepHours = v) } },
                    label = { Text("Ore di sonno") }, singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Trasporto abituale", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransportMode.entries.forEach { tm ->
                    FilterChip(
                        selected = state.transportMode == tm,
                        onClick = { vm.select("transportMode") { it.copy(transportMode = tm) } },
                        label = { Text(tm.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Qualita del sonno", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Level3Chips(state.sleepQuality) { v -> vm.select("sleepQuality") { it.copy(sleepQuality = v) } }
            Spacer(Modifier.height(12.dp))
            Text("Livello di stress", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Level3Chips(state.stressLevel) { v -> vm.select("stressLevel") { it.copy(stressLevel = v) } }
            Spacer(Modifier.height(12.dp))
            Text("Capacita di recupero percepita", style = MaterialTheme.typography.bodySmall,
                color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Level3Chips(state.recoveryCapacity) { v -> vm.select("recoveryCapacity") { it.copy(recoveryCapacity = v) } }
        }
    }
}

@Composable
private fun StepDettagliAllenamento(state: OnboardingUiState, vm: OnboardingViewModel) {
    SectionCard {
        Column {
            SectionHeader("Dettagli allenamento", "Personalizza la scheda")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.trainingYears,
                    onValueChange = { v -> vm.update { it.copy(trainingYears = v) } },
                    label = { Text("Anni di allenamento") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.maxSessionMinutes,
                    onValueChange = { v -> vm.update { it.copy(maxSessionMinutes = v.filter(Char::isDigit)) } },
                    label = { Text("Durata max seduta (min)") }, singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Livello tecnico", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            TechnicalLevel.entries.forEach { tl ->
                FilterChip(
                    selected = state.technicalLevel == tl,
                    onClick = { vm.select("technicalLevel") { it.copy(technicalLevel = tl) } },
                    label = { Text(tl.label) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Gruppi muscolari prioritari", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            MuscleGroup.entries.forEach { mg ->
                FilterChip(
                    selected = mg in state.priorityMuscles,
                    onClick = { vm.togglePriorityMuscle(mg) },
                    label = { Text(mg.label) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.preferredExercises,
                onValueChange = { v -> vm.update { it.copy(preferredExercises = v) } },
                label = { Text("Esercizi che preferisci") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.dislikedExercises,
                onValueChange = { v -> vm.update { it.copy(dislikedExercises = v) } },
                label = { Text("Esercizi che NON ti piacciono") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.unknownExercises,
                onValueChange = { v -> vm.update { it.copy(unknownExercises = v) } },
                label = { Text("Esercizi che non conosci") }, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepRiepilogo(state: OnboardingUiState) {
    SectionCard {
        Column {
            SectionHeader("Riepilogo", "Controlla i dati prima di generare i programmi")
            Spacer(Modifier.height(12.dp))
            val rows = listOf(
                "Nome" to state.name.ifBlank { "-" },
                "Eta" to "${state.age} anni",
                "Altezza" to "${state.heightCm} cm",
                "Peso" to "${state.weightKg} kg",
                "Obiettivo" to state.goal.label,
                "Esperienza" to state.experience.label,
                "Giorni/settimana" to "${state.trainingDays}",
                "Lavoro" to state.workType.label,
                "Sonno" to "${state.sleepHours.ifBlank { "-" }} h (${state.sleepQuality.label})",
                "Recupero" to state.recoveryCapacity.label,
                "Pasti/giorno" to "${state.mealsPerDay}",
                "Colazione" to state.breakfastPreference.label,
                "Allergie" to state.allergies.ifBlank { "nessuna" },
                "Limitazioni" to state.physicalLimitations.ifBlank { "nessuna" }
            )
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(k, style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                        modifier = Modifier.weight(1f))
                    Text(v, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1.4f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Premi «Genera programmi» per creare piano alimentare e scheda. " +
                    "Potrai modificare ogni parametro in seguito.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        }
    }
}

@Composable
private fun Level3Chips(value: Level3, onSelect: (Level3) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Level3.entries.forEach { l ->
            FilterChip(
                selected = value == l,
                onClick = { onSelect(l) },
                label = { Text(l.label) }
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
