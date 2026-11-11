package com.fitcoach.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.usecase.GenerateProgramsUseCase
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 0,
    val totalSteps: Int = 5,
    val loading: Boolean = true,
    val saving: Boolean = false,
    // Anagrafica
    val name: String = "",
    val age: String = "",
    val sex: Sex = Sex.UNSPECIFIED,
    val heightCm: String = "",
    val weightKg: String = "",
    val bodyFat: String = "",
    // Allenamento
    val experience: TrainingExperience = TrainingExperience.BEGINNER,
    val goal: Goal = Goal.RECOMP,
    val trainingDays: Int = 3,
    val equipment: Set<Equipment> = setOf(Equipment.BARBELL, Equipment.DUMBBELL, Equipment.MACHINE),
    // Nutrizione
    val preferences: Set<DietaryPreference> = setOf(DietaryPreference.NONE),
    val excludedFoods: String = "",
    val allergies: String = "",
    val mealsPerDay: Int = 5,
    val budgetEur: String = "",
    // Salute
    val physicalLimitations: String = "",
    val movementsToAvoid: String = "",
    // Esito
    val error: String? = null,
    val warnings: List<String> = emptyList(),
    val finished: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository,
    private val progressRepository: ProgressRepository,
    private val generatePrograms: GenerateProgramsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = userRepository.get()
            if (existing != null) {
                _state.value = _state.value.copy(
                    loading = false,
                    name = existing.name,
                    age = existing.ageYears?.toString().orEmpty(),
                    sex = existing.sex,
                    heightCm = existing.heightCm?.let { "%.0f".format(it) }.orEmpty(),
                    weightKg = existing.weightKg?.let { "%.1f".format(it) }.orEmpty(),
                    bodyFat = existing.bodyFatPercent?.let { "%.1f".format(it) }.orEmpty(),
                    experience = existing.experience,
                    goal = existing.goal,
                    trainingDays = existing.trainingDaysPerWeek,
                    equipment = existing.availableEquipment.ifEmpty { _state.value.equipment },
                    preferences = existing.dietaryPreferences.ifEmpty { _state.value.preferences },
                    excludedFoods = existing.excludedFoods.joinToString(", "),
                    allergies = existing.allergies.joinToString(", "),
                    mealsPerDay = existing.mealsPerDay,
                    budgetEur = existing.weeklyFoodBudgetEur?.toString().orEmpty(),
                    physicalLimitations = existing.physicalLimitations.joinToString(", "),
                    movementsToAvoid = existing.movementsToAvoid.joinToString(", ")
                )
            } else {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun next() { _state.value = _state.value.copy(step = (_state.value.step + 1).coerceAtMost(_state.value.totalSteps - 1)) }
    fun back() { _state.value = _state.value.copy(step = (_state.value.step - 1).coerceAtLeast(0)) }

    fun update(transform: (OnboardingUiState) -> OnboardingUiState) {
        _state.value = transform(_state.value)
    }

    fun toggleEquipment(e: Equipment) = update { s ->
        s.copy(equipment = if (e in s.equipment) s.equipment - e else s.equipment + e)
    }

    fun togglePreference(p: DietaryPreference) = update { s ->
        val base = if (p == DietaryPreference.NONE) {
            setOf(DietaryPreference.NONE)
        } else {
            (s.preferences - DietaryPreference.NONE).let { if (p in it) it - p else it + p }
                .ifEmpty { setOf(DietaryPreference.NONE) }
        }
        s.copy(preferences = base)
    }

    fun finish() {
        viewModelScope.launch {
            val s = _state.value
            val age = s.age.toIntOrNull()
            val height = s.heightCm.replace(',', '.').toDoubleOrNull()
            val weight = s.weightKg.replace(',', '.').toDoubleOrNull()

            val missing = buildList {
                if (age == null || age !in 14..90) add("eta valida")
                if (height == null || height !in 120.0..230.0) add("altezza valida")
                if (weight == null || weight !in 30.0..300.0) add("peso valido")
            }
            if (missing.isNotEmpty() || age == null || height == null || weight == null) {
                _state.value = s.copy(error = "Inserisci: ${missing.joinToString()}.")
                return@launch
            }

            val profile = UserProfile(
                name = s.name.trim(),
                ageYears = age,
                sex = s.sex,
                heightCm = height,
                weightKg = weight,
                bodyFatPercent = s.bodyFat.replace(',', '.').toDoubleOrNull()?.takeIf { it in 1.0..70.0 },
                experience = s.experience,
                goal = s.goal,
                trainingDaysPerWeek = s.trainingDays,
                availableEquipment = s.equipment,
                dietaryPreferences = s.preferences,
                excludedFoods = s.excludedFoods.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                allergies = s.allergies.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                physicalLimitations = s.physicalLimitations.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                movementsToAvoid = s.movementsToAvoid.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                weeklyFoodBudgetEur = s.budgetEur.replace(',', '.').toDoubleOrNull(),
                mealsPerDay = s.mealsPerDay
            )

            _state.value = s.copy(saving = true, error = null)
            userRepository.save(profile)
            preferencesRepository.getOrCreate()
            progressRepository.addWeight(
                WeightRecord(dateEpochDay = LocalDate.now().toEpochDay(), weightKg = weight)
            )

            val result = generatePrograms(
                profile = profile,
                trainingDayIndices = WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
            )
            when (result) {
                is GenerateProgramsUseCase.Result.Success -> {
                    _state.value = _state.value.copy(
                        saving = false, finished = true, warnings = result.warnings
                    )
                }
                is GenerateProgramsUseCase.Result.MissingData ->
                    _state.value = _state.value.copy(
                        saving = false, error = "Dati mancanti: ${result.missing.joinToString()}."
                    )
                is GenerateProgramsUseCase.Result.Error ->
                    _state.value = _state.value.copy(saving = false, error = result.message)
            }
        }
    }
}
