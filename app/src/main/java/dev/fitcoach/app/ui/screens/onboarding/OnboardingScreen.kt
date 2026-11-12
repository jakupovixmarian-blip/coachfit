package dev.fitcoach.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitcoach.app.core.util.Format
import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.ui.components.ChipSelector
import dev.fitcoach.app.ui.components.FitCard
import dev.fitcoach.app.ui.components.KeyValueRow
import dev.fitcoach.app.ui.components.LabeledField
import dev.fitcoach.app.ui.components.MultiChipSelector
import dev.fitcoach.app.ui.components.NumberField
import dev.fitcoach.app.ui.components.SectionTitle

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }

    Scaffold(
        topBar = {},
        snackbarHost = {
            state.savedMessage?.let {
                Snackbar(modifier = Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it)
                        Text("OK", modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Anamnesi FitCoach", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Passo ${state.stepIndex + 1} di ${OnboardingStep.entries.size} - ${state.step.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (state.stepIndex + 1f) / OnboardingStep.entries.size },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            when (state.step) {
                OnboardingStep.BASICS -> BasicsStep(state, viewModel)
                OnboardingStep.GOAL -> GoalStep(state, viewModel)
                OnboardingStep.EXPERIENCE -> ExperienceStep(state, viewModel)
                OnboardingStep.LIFESTYLE -> LifestyleStep(state, viewModel)
                OnboardingStep.HEALTH -> HealthStep(state, viewModel)
                OnboardingStep.TRAINING -> TrainingStep(state, viewModel)
                OnboardingStep.NUTRITION -> NutritionStep(state, viewModel)
                OnboardingStep.SUMMARY -> SummaryStep(state)
            }

            if (state.contradictions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FitCard {
                    Column {
                        Text("Controllo coerenza", style = MaterialTheme.typography.titleMedium)
                        state.contradictions.forEach { issue ->
                            Spacer(Modifier.height(4.dp))
                            Text("- $issue", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.confirmContradictions() }) {
                            Text("Confermo, prosegui")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.stepIndex > 0) {
                    OutlinedButton(onClick = { viewModel.back() }, modifier = Modifier.weight(1f)) {
                        Text("Indietro")
                    }
                }
                if (state.isLast) {
                    Button(onClick = { viewModel.finish() }, modifier = Modifier.weight(1f)) {
                        Text("Conferma e salva")
                    }
                } else {
                    Button(onClick = { viewModel.next() }, modifier = Modifier.weight(1f)) {
                        Text("Avanti")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BasicsStep(state: OnboardingState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledField("Nome", state.profile.name, { v -> vm.update { it.copy(name = v) } })
        NumberField("Eta (anni)", state.profile.age?.toString().orEmpty(), { v ->
            vm.update { it.copy(age = v.toIntOrNull()) }
        }, allowDecimal = false)
        ChipSelector("Sesso", Sex.entries.toList(), state.profile.sex, { it.name.lowercase() }, { v ->
            vm.update { it.copy(sex = v) }
        })
        NumberField("Altezza (cm)", state.profile.heightCm?.toString().orEmpty(), { v ->
            vm.update { it.copy(heightCm = v.toDoubleOrNull()) }
        })
        NumberField("Peso (kg)", state.profile.weightKg?.toString().orEmpty(), { v ->
            vm.update { it.copy(weightKg = v.toDoubleOrNull()) }
        })
        NumberField("Grasso corporeo % (se nota)", state.profile.bodyFatPercent?.toString().orEmpty(), { v ->
            vm.update { it.copy(bodyFatPercent = v.toDoubleOrNull()) }
        })
        Text(
            "Puoi lasciare vuoto cio che non conosci: non vengono inventati valori.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalStep(state: OnboardingState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChipSelector("Obiettivo", Goal.entries.toList(), state.profile.goal, { goalLabel(it) }, { v ->
            vm.update { it.copy(goal = v) }
        })
    }
}

@Composable
private fun ExperienceStep(state: OnboardingState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChipSelector("Livello", ExperienceLevel.entries.toList(), state.profile.experience, { levelLabel(it) }, { v ->
            vm.update { it.copy(experience = v) }
        })
        NumberField("Anni di allenamento", state.profile.trainingYears?.toString().orEmpty(), { v ->
            vm.update { it.copy(trainingYears = v.toDoubleOrNull()) }
        })
        NumberField("Settimane di continuita attuale", state.profile.consistencyWeeks?.toString().orEmpty(), { v ->
            vm.update { it.copy(consistencyWeeks = v.toIntOrNull()) }
        }, allowDecimal = false)
        LabeledField("Carichi conosciuti (es. panca 60x10)", state.profile.knownLoads, { v ->
            vm.update { it.copy(knownLoads = v) }
        }, singleLine = false)
    }
}

@Composable
private fun LifestyleStep(state: OnboardingState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledField("Attivita lavorativa", state.profile.jobActivity, { v ->
            vm.update { it.copy(jobActivity = v) }
        })
        NumberField("Passi giornalieri", state.profile.dailySteps?.toString().orEmpty(), { v ->
            vm.update { it.copy(dailySteps = v.toIntOrNull()) }
        }, allowDecimal = false)
        NumberField("Ore di sonno", state.profile.sleepHours?.toString().orEmpty(), { v ->
            vm.update { it.copy(sleepHours = v.toDoubleOrNull()) }
        })
        NumberField("Livello di stress (1-10)", state.profile.stressLevel?.toString().orEmpty(), { v ->
            vm.update { it.copy(stressLevel = v.toIntOrNull()) }
        }, allowDecimal = false)
        NumberField("Qualita del recupero (1-10)", state.profile.recoveryLevel?.toString().orEmpty(), { v ->
            vm.update { it.copy(recoveryLevel = v.toIntOrNull()) }
        }, allowDecimal = false)
    }
}

@Composable
private fun HealthStep(state: OnboardingState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledField("Infortuni e limitazioni dichiarate", state.profile.injuries, { v ->
            vm.update { it.copy(injuries = v) }
        }, singleLine = false)
        Text(
            "Le limitazioni inserite vengono considerate nella generazione dei programmi.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrainingStep(state: OnboardingState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MultiChipSelector(
            label = "Attrezzatura disponibile",
            options = Equipment.entries.toList(),
            selected = state.profile.equipment.split(",").mapNotNull { name ->
                Equipment.entries.firstOrNull { it.name == name.trim() }
            }.toSet(),
            optionLabel = { it.label },
            onToggle = { item ->
                val current = state.profile.equipment.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
                if (!current.remove(item.name)) current.add(item.name)
                vm.update { it.copy(equipment = current.joinToString(",")) }
            }
        )
        NumberField("Giorni disponibili a settimana", state.profile.availableDays?.toString().orEmpty(), { v ->
            vm.update { it.copy(availableDays = v.toIntOrNull()) }
        }, allowDecimal = false)
        NumberField("Durata seduta (minuti)", state.profile.sessionMinutes?.toString().orEmpty(), { v ->
            vm.update { it.copy(sessionMinutes = v.toIntOrNull()) }
        }, allowDecimal = false)
    }
}

@Composable
private fun NutritionStep(state: OnboardingState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledField("Preferenze alimentari", state.profile.dietPreference, { v ->
            vm.update { it.copy(dietPreference = v) }
        })
        LabeledField("Allergie e intolleranze", state.profile.allergies, { v ->
            vm.update { it.copy(allergies = v) }
        })
        NumberField("Numero di pasti al giorno", state.profile.mealsPerDay?.toString().orEmpty(), { v ->
            vm.update { it.copy(mealsPerDay = v.toIntOrNull()) }
        }, allowDecimal = false)
        LabeledField("Orari dei pasti", state.profile.mealTimes, { v ->
            vm.update { it.copy(mealTimes = v) }
        })
        NumberField("Budget settimanale (EUR)", state.profile.weeklyBudget?.toString().orEmpty(), { v ->
            vm.update { it.copy(weeklyBudget = v.toDoubleOrNull()) }
        })
        NumberField("Tempo per cucinare (minuti/giorno)", state.profile.cookingMinutes?.toString().orEmpty(), { v ->
            vm.update { it.copy(cookingMinutes = v.toIntOrNull()) }
        }, allowDecimal = false)
        LabeledField("Alimenti preferiti", state.profile.likedFoods, { v ->
            vm.update { it.copy(likedFoods = v) }
        }, singleLine = false)
        LabeledField("Alimenti non graditi", state.profile.dislikedFoods, { v ->
            vm.update { it.copy(dislikedFoods = v) }
        }, singleLine = false)
    }
}

@Composable
private fun SummaryStep(state: OnboardingState) {
    val p = state.profile
    FitCard {
        Column {
            SectionTitle("Riepilogo completo")
            KeyValueRow("Nome", p.name.ifBlank { "-" })
            KeyValueRow("Eta", p.age?.toString() ?: "Non indicata")
            KeyValueRow("Sesso", p.sex.name.lowercase())
            KeyValueRow("Altezza", p.heightCm?.let { "${it.toInt()} cm" } ?: "Non indicata")
            KeyValueRow("Peso", p.weightKg?.let { Format.kg(it) } ?: "Non indicato")
            KeyValueRow("Grasso corporeo", p.bodyFatPercent?.let { Format.percent(it) } ?: "Non indicato")
            KeyValueRow("Obiettivo", goalLabel(p.goal))
            KeyValueRow("Esperienza", levelLabel(p.experience))
            KeyValueRow("Anni allenamento", p.trainingYears?.toString() ?: "Non indicati")
            KeyValueRow("Giorni disponibili", p.availableDays?.toString() ?: "Non indicati")
            KeyValueRow("Durata seduta", p.sessionMinutes?.let { "$it min" } ?: "Non indicata")
            KeyValueRow("Attrezzatura", p.equipment.ifBlank { "Non indicata" })
            KeyValueRow("Limitazioni", p.injuries.ifBlank { "Nessuna dichiarata" })
            KeyValueRow("Preferenze alimentari", p.dietPreference.ifBlank { "Nessuna" })
            KeyValueRow("Allergie", p.allergies.ifBlank { "Nessuna" })
            KeyValueRow("Pasti al giorno", p.mealsPerDay?.toString() ?: "Non indicati")
            KeyValueRow("Budget settimanale", p.weeklyBudget?.let { Format.money(it) } ?: "Non indicato")

            Spacer(Modifier.height(10.dp))
            Text("Stime calcolate", style = MaterialTheme.typography.titleMedium)
            KeyValueRow("Metabolismo basale", p.bmr()?.let { Format.kcal(it) } ?: "Dati insufficienti")
            KeyValueRow("Fabbisogno stimato", p.maintenanceCalories()?.let { Format.kcal(it) } ?: "Dati insufficienti")
            KeyValueRow("Calorie obiettivo", p.targetCalories()?.let { Format.kcal(it) } ?: "Dati insufficienti")
            KeyValueRow("Proteine", p.proteinTarget()?.let { Format.grams(it) } ?: "-")
            KeyValueRow("Grassi", p.fatTarget()?.let { Format.grams(it) } ?: "-")
            KeyValueRow("Carboidrati", p.carbsTarget()?.let { Format.grams(it) } ?: "-")

            Spacer(Modifier.height(8.dp))
            Text(
                "Premendo Conferma i dati vengono salvati in modo permanente. Potrai modificarli dalle Impostazioni.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun goalLabel(goal: Goal): String = when (goal) {
    Goal.FAT_LOSS -> "Dimagrimento"
    Goal.RECOMP -> "Ricomposizione"
    Goal.MUSCLE_GAIN -> "Massa muscolare"
    Goal.STRENGTH -> "Forza"
    Goal.MAINTENANCE -> "Mantenimento"
    Goal.HEALTH -> "Salute generale"
}

private fun levelLabel(level: ExperienceLevel): String = when (level) {
    ExperienceLevel.BEGINNER -> "Principiante"
    ExperienceLevel.INTERMEDIATE -> "Intermedio"
    ExperienceLevel.ADVANCED -> "Avanzato"
}
