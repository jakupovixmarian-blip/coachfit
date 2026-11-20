package com.fitcoach.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.BudgetScope
import com.fitcoach.app.domain.model.CookingTimeAvailable
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Level3
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TechnicalLevel
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.TransportMode
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.model.WorkShift
import com.fitcoach.app.domain.model.WorkType
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 0,
    val totalSteps: Int = 8,
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
    // Stile di vita / recupero
    val workType: WorkType = WorkType.SEDENTARY,
    val workShift: WorkShift = WorkShift.NONE,
    val dailySteps: String = "",
    val transportMode: TransportMode = TransportMode.CAR,
    val extraSport: Int = 0,
    val sleepHours: String = "",
    val sleepQuality: Level3 = Level3.MEDIUM,
    val stressLevel: Level3 = Level3.MEDIUM,
    val recoveryCapacity: Level3 = Level3.MEDIUM,
    // Dettagli allenamento
    val trainingYears: String = "",
    val technicalLevel: TechnicalLevel = TechnicalLevel.NOVICE,
    val priorityMuscles: Set<MuscleGroup> = emptySet(),
    val preferredExercises: String = "",
    val dislikedExercises: String = "",
    val unknownExercises: String = "",
    val maxSessionMinutes: String = "",
    // Nutrizione
    val preferences: Set<DietaryPreference> = setOf(DietaryPreference.NONE),
    val excludedFoods: String = "",
    val allergies: String = "",
    val mealsPerDay: Int = 5,
    val breakfastPreference: BreakfastPreference = BreakfastPreference.SWEET,
    val likedFoods: String = "",
    val dislikedFoods: String = "",
    val cookingTime: CookingTimeAvailable = CookingTimeAvailable.MEDIUM,
    val mealPrepPossible: Boolean = false,
    val prefersSimpleFoods: Boolean = true,
    val budgetEur: String = "",
    val monthlyBudgetEur: String = "",
    val budgetScope: BudgetScope = BudgetScope.PERSONAL_ONLY,
    val budgetPersons: Int = 1,
    // Salute
    val physicalLimitations: String = "",
    val movementsToAvoid: String = "",
    // Esito
    val error: String? = null,
    val warnings: List<String> = emptyList(),
    val finished: Boolean = false,
    /**
     * Id delle domande dell'anamnesi su cui l'utente ha agito in questo form.
     *
     * Le selezioni hanno un default visibile (obiettivo RECOMP, esperienza
     * BEGINNER, 3 giorni, 5 pasti): registrare il valore senza sapere se e stato
     * toccato lo farebbe passare per una scelta dell'utente.
     */
    val touched: Set<String> = emptySet()
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository,
    private val progressRepository: ProgressRepository
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
                    workType = existing.workType,
                    workShift = existing.workShift,
                    dailySteps = existing.dailySteps?.toString().orEmpty(),
                    transportMode = existing.transportMode,
                    extraSport = existing.extraSportSessionsPerWeek,
                    sleepHours = existing.sleepHours?.let { "%.1f".format(it) }.orEmpty(),
                    sleepQuality = existing.sleepQuality,
                    stressLevel = existing.stressLevel,
                    recoveryCapacity = existing.recoveryCapacity,
                    trainingYears = existing.trainingYears?.let { "%.1f".format(it) }.orEmpty(),
                    technicalLevel = existing.technicalLevel,
                    priorityMuscles = existing.priorityMuscles,
                    preferredExercises = existing.preferredExercises.joinToString(", "),
                    dislikedExercises = existing.dislikedExercises.joinToString(", "),
                    unknownExercises = existing.unknownExercises.joinToString(", "),
                    maxSessionMinutes = existing.maxSessionMinutes?.toString().orEmpty(),
                    preferences = existing.dietaryPreferences.ifEmpty { _state.value.preferences },
                    excludedFoods = existing.excludedFoods.joinToString(", "),
                    allergies = existing.allergies.joinToString(", "),
                    mealsPerDay = existing.mealsPerDay,
                    breakfastPreference = existing.breakfastPreference,
                    likedFoods = existing.likedFoods.joinToString(", "),
                    dislikedFoods = existing.dislikedFoods.joinToString(", "),
                    cookingTime = existing.cookingTimeAvailable,
                    mealPrepPossible = existing.mealPrepPossible,
                    prefersSimpleFoods = existing.prefersSimpleFoods,
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

    /** Applica una scelta a chip/selettore e la registra come dichiarata dall'utente. */
    fun select(id: String, transform: (OnboardingUiState) -> OnboardingUiState) {
        _state.value = transform(_state.value).let { it.copy(touched = it.touched + id) }
    }

    fun toggleEquipment(e: Equipment) = update { s ->
        s.copy(
            equipment = if (e in s.equipment) s.equipment - e else s.equipment + e,
            touched = s.touched + "availableEquipment"
        )
    }

    fun togglePreference(p: DietaryPreference) = update { s ->
        val base = if (p == DietaryPreference.NONE) {
            setOf(DietaryPreference.NONE)
        } else {
            (s.preferences - DietaryPreference.NONE).let { if (p in it) it - p else it + p }
                .ifEmpty { setOf(DietaryPreference.NONE) }
        }
        s.copy(preferences = base, touched = s.touched + "dietaryPreferences")
    }

    fun togglePriorityMuscle(m: MuscleGroup) = update { s ->
        s.copy(
            priorityMuscles = if (m in s.priorityMuscles) s.priorityMuscles - m else s.priorityMuscles + m,
            touched = s.touched + "priorityMuscles"
        )
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
                workType = s.workType,
                workShift = s.workShift,
                dailySteps = s.dailySteps.toIntOrNull()?.takeIf { it in 0..60000 },
                transportMode = s.transportMode,
                extraSportSessionsPerWeek = s.extraSport.coerceIn(0, 14),
                sleepHours = s.sleepHours.replace(',', '.').toDoubleOrNull()?.takeIf { it in 3.0..14.0 },
                sleepQuality = s.sleepQuality,
                stressLevel = s.stressLevel,
                recoveryCapacity = s.recoveryCapacity,
                trainingYears = s.trainingYears.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..60.0 },
                technicalLevel = s.technicalLevel,
                priorityMuscles = s.priorityMuscles,
                preferredExercises = s.preferredExercises.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                dislikedExercises = s.dislikedExercises.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                unknownExercises = s.unknownExercises.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                maxSessionMinutes = s.maxSessionMinutes.toIntOrNull()?.takeIf { it in 20..180 },
                dietaryPreferences = s.preferences,
                excludedFoods = s.excludedFoods.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                allergies = s.allergies.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                physicalLimitations = s.physicalLimitations.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                movementsToAvoid = s.movementsToAvoid.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                weeklyFoodBudgetEur = s.budgetEur.replace(',', '.').toDoubleOrNull(),
                monthlyFoodBudgetEur = s.monthlyBudgetEur.replace(',', '.').toDoubleOrNull()
                    ?.takeIf { it > 0 },
                budgetScope = s.budgetScope,
                budgetPersons = s.budgetPersons.coerceAtLeast(1),
                anamnesisCompleted = false,
                anamnesisConfirmedAt = null,
                anamnesisSummary = "",
                mealsPerDay = s.mealsPerDay,
                breakfastPreference = s.breakfastPreference,
                likedFoods = s.likedFoods.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                dislikedFoods = s.dislikedFoods.split(",", ";").map { it.trim() }.filter { it.isNotBlank() },
                cookingTimeAvailable = s.cookingTime,
                mealPrepPossible = s.mealPrepPossible,
                prefersSimpleFoods = s.prefersSimpleFoods,
                // Solo i campi che l'utente ha davvero compilato: i default del
                // form (obiettivo RECOMP, lavoro SEDENTARY, 5 pasti) non sono
                // dichiarazioni e non devono arrivare all'AI come dati reali.
                answeredQuestionIds = answeredFrom(s, age, height, weight)
            )

            _state.value = s.copy(saving = true, error = null)
            userRepository.save(profile)
            preferencesRepository.getOrCreate()
            progressRepository.addWeight(
                WeightRecord(dateEpochDay = LocalDate.now().toEpochDay(), weightKg = weight)
            )

            // La generazione dei programmi avviene SOLO dopo la conferma
            // dell'anamnesi guidata (spec. §3.7): qui salviamo il profilo di base.
            _state.value = _state.value.copy(saving = false, finished = true)
        }
    }

    /**
     * Id delle domande dell'anamnesi a cui l'utente ha risposto in questo form.
     *
     * Il form usa selezioni con default (obiettivo, esperienza, giorni, tipo di
     * lavoro, pasti, budget): salvarle come "risposte" farebbe passare per
     * dichiarati valori che l'utente non ha mai scelto. Le selezioni sono quindi
     * registrate solo quando il campo testuale corrispondente e stato compilato.
     * Ogni id deve esistere in [AnamnesisEngine.questions]: un id inventato non
     * verrebbe mai letto e il dato risulterebbe comunque non dichiarato.
     */
    private fun answeredFrom(s: OnboardingUiState, age: Int, height: Double, weight: Double): Set<String> =
        buildSet {
            addAll(s.touched)
            add("age"); add("heightCm"); add("weightKg")
            if (s.bodyFat.isNotBlank()) add("bodyFatPercent")
            if (s.dailySteps.isNotBlank()) add("dailySteps")
            if (s.sleepHours.isNotBlank()) add("sleepHours")
            if (s.trainingYears.isNotBlank()) add("trainingYears")
            if (s.maxSessionMinutes.isNotBlank()) add("maxSessionMinutes")
            if (s.allergies.isNotBlank()) add("allergies")
            if (s.likedFoods.isNotBlank()) add("likedFoods")
            if (s.dislikedFoods.isNotBlank()) add("dislikedFoods")
            if (s.monthlyBudgetEur.isNotBlank()) add("monthlyFoodBudgetEur")
            if (s.preferredExercises.isNotBlank()) add("preferredExercises")
            if (s.dislikedExercises.isNotBlank()) add("dislikedExercises")
            if (s.physicalLimitations.isNotBlank()) add("physicalLimitations")
            if (s.movementsToAvoid.isNotBlank()) add("movementsToAvoid")
        }
}
