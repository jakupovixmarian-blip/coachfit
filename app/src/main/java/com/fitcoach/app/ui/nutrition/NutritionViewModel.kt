package com.fitcoach.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.FoodSearchRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.SubstitutionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.nutrition.FoodSubstitutionEngine
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.usecase.ProgramGenerationResult
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NutritionUiState(
    val loading: Boolean = true,
    val plan: NutritionPlan? = null,
    val selectedDayIndex: Int = 0,
    val message: String? = null,
    val generating: Boolean = false,
    val epochDay: Long = NutritionDiaryRepository.todayEpochDay(),
    /** planEntryId -> id voce diario attiva (spunta). */
    val checks: Map<Long, Long> = emptyMap(),
    // Sostituzione vincolata
    val substitutionEntry: MealEntry? = null,
    val substitutes: List<FoodItem> = emptyList(),
    /** foodId -> compatibilita precalcolata, per mostrare quali varianti sono valide. */
    val substituteStatuses: Map<Long, FoodSubstitutionEngine.Status> = emptyMap(),
    val substituteEvaluating: Boolean = false,
    val preview: FoodSubstitutionEngine.Result? = null,
    val previewFood: FoodItem? = null,
    // Ricerca alimentare progressiva (§6)
    val foodSearchQuery: String = "",
    val foodSearchResults: List<FoodItem> = emptyList(),
    val foodSearchLoading: Boolean = false,
    val foodSearchNote: String? = null,
    // Modifica manuale del piano: pasto a cui aggiungere un alimento.
    val addFoodToMeal: Meal? = null,
    val addFoodGrams: String = "",
    val selectedFoodToAdd: FoodItem? = null
) {
    val selectedDay: NutritionDay?
        get() = plan?.days?.firstOrNull { it.dayIndex == selectedDayIndex }
}

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val nutritionRepository: NutritionRepository,
    private val foodRepository: FoodRepository,
    private val diaryRepository: NutritionDiaryRepository,
    private val substitutionRepository: SubstitutionRepository,
    private val foodSearchRepository: FoodSearchRepository,
    private val generationCoordinator: ProgramGenerationCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(NutritionUiState())
    val state: StateFlow<NutritionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val plan = nutritionRepository.getActivePlan()
            val checks = diaryRepository.planChecks(_state.value.epochDay).mapValues { it.value.id }
            _state.value = _state.value.copy(
                loading = false,
                plan = plan,
                selectedDayIndex = _state.value.selectedDayIndex.coerceIn(0, 6),
                checks = checks
            )
        }
    }

    fun selectDay(index: Int) {
        _state.value = _state.value.copy(selectedDayIndex = index.coerceIn(0, 6))
    }

    fun toggleEntry(entry: MealEntry) {
        viewModelScope.launch {
            val day = _state.value.selectedDay ?: return@launch
            val meal = day.meals.firstOrNull { m -> m.entries.any { it.id == entry.id } } ?: return@launch
            diaryRepository.togglePlanEntry(entry, meal.slot, meal.time, _state.value.epochDay)
            val checks = diaryRepository.planChecks(_state.value.epochDay).mapValues { it.value.id }
            _state.value = _state.value.copy(checks = checks)
        }
    }

    fun generatePlan() {
        viewModelScope.launch {
            val profile = userRepository.get()
            if (profile == null || !profile.isComplete) {
                _state.value = _state.value.copy(message = "Completa prima il profilo.")
                return@launch
            }
            _state.value = _state.value.copy(generating = true, message = null)
            val result = generationCoordinator.generate(
                profile = profile,
                scope = ProgramGenerationCoordinator.Scope.NUTRITION
            )
            val message = when (result) {
                is ProgramGenerationResult.Success -> "Piano alimentare generato dal Coach AI."
                is ProgramGenerationResult.MissingData ->
                    "Dati mancanti: ${result.missing.joinToString()}."
                is ProgramGenerationResult.AnamnesisNotConfirmed ->
                    "Completa l'anamnesi guidata prima di generare."
                is ProgramGenerationResult.Error -> result.message
            }
            val plan = nutritionRepository.getActivePlan()
            _state.value = _state.value.copy(generating = false, plan = plan, message = message)
        }
    }

    fun startSubstitution(entry: MealEntry) {
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            val all = foodRepository.getAll()
            val original = all.firstOrNull { it.id == entry.foodItemId }
                ?: all.firstOrNull { it.name == entry.foodName }
            val subs = if (original != null) {
                substitutionRepository.candidates(original, profile)
            } else {
                all.take(8)
            }
            _state.value = _state.value.copy(
                substitutionEntry = entry,
                substitutes = subs,
                substituteStatuses = emptyMap(),
                substituteEvaluating = subs.isNotEmpty(),
                preview = null,
                previewFood = null
            )
            evaluateSubstitutes(entry, subs)
        }
    }

    /**
     * Precalcola la compatibilita di ogni variante, cosi il dialogo mostra subito
     * quali funzionano (invece di far scoprire "non idonea" solo al tocco) e le
     * ordina mettendo per prime quelle applicabili.
     */
    private suspend fun evaluateSubstitutes(entry: MealEntry, candidates: List<FoodItem>) {
        val dayIndex = _state.value.selectedDayIndex
        val statuses = linkedMapOf<Long, FoodSubstitutionEngine.Status>()
        for (food in candidates) {
            val result = runCatching {
                substitutionRepository.preview(entry.id, dayIndex, food)
            }.getOrNull()
            if (result != null) statuses[food.id] = result.status
        }
        val ordered = candidates.sortedWith(
            compareBy(
                { statuses[it.id] == FoodSubstitutionEngine.Status.INCOMPATIBLE },
                { statuses[it.id]?.ordinal ?: Int.MAX_VALUE }
            )
        )
        // Aggiorna solo se il dialogo e ancora aperto sulla stessa voce.
        if (_state.value.substitutionEntry?.id == entry.id) {
            _state.value = _state.value.copy(
                substitutes = ordered,
                substituteStatuses = statuses,
                substituteEvaluating = false
            )
        }
    }

    fun cancelSubstitution() {
        _state.value = _state.value.copy(
            substitutionEntry = null, substitutes = emptyList(), preview = null, previewFood = null,
            substituteStatuses = emptyMap(), substituteEvaluating = false,
            foodSearchQuery = "", foodSearchResults = emptyList(), foodSearchNote = null
        )
    }

    fun updateFoodSearch(query: String) {
        _state.value = _state.value.copy(foodSearchQuery = query, foodSearchNote = null)
    }

    /**
     * Ricerca progressiva (§6): prima nel database locale, poi (se non ci sono
     * risultati) nelle fonti esterne (Open Food Facts, USDA). I risultati trovati
     * vengono salvati localmente per gli usi futuri.
     */
    fun searchFoods() {
        val query = _state.value.foodSearchQuery.trim()
        if (query.length < 2) {
            _state.value = _state.value.copy(
                foodSearchResults = emptyList(),
                foodSearchNote = "Digita almeno 2 caratteri."
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(foodSearchLoading = true, foodSearchNote = null)
            val outcome = runCatching { foodSearchRepository.search(query) }.getOrNull()
            _state.value = _state.value.copy(
                foodSearchLoading = false,
                foodSearchResults = outcome?.results.orEmpty(),
                foodSearchNote = outcome?.note
                    ?: if (outcome == null) "Ricerca non disponibile: controlla la connessione." else null
            )
        }
    }

    /** Verifica un alimento trovato con la ricerca come sostituto della voce corrente. */
    fun previewSubstituteFromSearch(food: FoodItem) = previewSubstitution(food)

    /** Calcola il controllo preventivo per il sostituto scelto, senza applicare nulla. */
    fun previewSubstitution(substitute: FoodItem) {
        viewModelScope.launch {
            val entry = _state.value.substitutionEntry ?: return@launch
            val result = substitutionRepository.preview(entry.id, _state.value.selectedDayIndex, substitute)
            _state.value = _state.value.copy(preview = result, previewFood = substitute)
        }
    }

    /** Applica la sostituzione solo se compatibile o compatibile con aggiustamento. */
    fun applySubstitution() {
        viewModelScope.launch {
            val entry = _state.value.substitutionEntry ?: return@launch
            val substitute = _state.value.previewFood ?: return@launch
            val result = _state.value.preview ?: return@launch
            if (result.status == FoodSubstitutionEngine.Status.INCOMPATIBLE) {
                _state.value = _state.value.copy(message = "Sostituzione non compatibile con i vincoli del piano.")
                return@launch
            }
            runCatching {
                substitutionRepository.apply(
                    entryId = entry.id,
                    dayIndex = _state.value.selectedDayIndex,
                    epochDay = _state.value.epochDay,
                    substitute = substitute,
                    result = result
                )
            }.onFailure {
                _state.value = _state.value.copy(message = "Errore nell'applicazione: ${it.message}")
                return@launch
            }
            _state.value = _state.value.copy(
                substitutionEntry = null,
                substitutes = emptyList(),
                preview = null,
                previewFood = null,
                message = "Sostituzione applicata: ${substitute.name} (${result.substituteGrams.toInt()} g)."
            )
            load()
        }
    }

    fun confirmMeal(mealId: Long, confirmed: Boolean) {
        viewModelScope.launch {
            nutritionRepository.confirmMeal(mealId, confirmed)
            _state.value = _state.value.copy(
                message = if (confirmed) "Pasto confermato." else "Conferma rimossa."
            )
            load()
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    /** Apre il riquadro per aggiungere manualmente un alimento a un pasto. */
    fun openAddFood(meal: Meal) {
        _state.value = _state.value.copy(
            addFoodToMeal = meal, addFoodGrams = "", selectedFoodToAdd = null,
            foodSearchQuery = "", foodSearchResults = emptyList(), foodSearchNote = null
        )
    }

    fun closeAddFood() {
        _state.value = _state.value.copy(addFoodToMeal = null, selectedFoodToAdd = null, addFoodGrams = "")
    }

    fun updateAddFoodGrams(value: String) {
        _state.value = _state.value.copy(addFoodGrams = value.filter { c -> c.isDigit() || c == ',' || c == '.' })
    }

    fun selectFoodToAdd(food: FoodItem) {
        _state.value = _state.value.copy(selectedFoodToAdd = food)
    }

    /** Conferma l'aggiunta manuale dell'alimento scelto con i grammi indicati. */
    fun confirmAddFood() {
        viewModelScope.launch {
            val meal = _state.value.addFoodToMeal ?: return@launch
            val food = _state.value.selectedFoodToAdd
            if (food == null) {
                _state.value = _state.value.copy(message = "Seleziona un alimento da aggiungere.")
                return@launch
            }
            val grams = _state.value.addFoodGrams.replace(',', '.').toDoubleOrNull()
            if (grams == null || grams <= 0 || grams > 3000) {
                _state.value = _state.value.copy(message = "Inserisci una quantita valida in grammi.")
                return@launch
            }
            nutritionRepository.addEntryToMeal(meal.id, food, grams, food.defaultUnit)
            _state.value = _state.value.copy(
                addFoodToMeal = null, selectedFoodToAdd = null, addFoodGrams = "",
                message = "${food.name} aggiunto a ${meal.slot.label}."
            )
            load()
        }
    }

    /** Rimuove manualmente un alimento dal piano. */
    fun removeEntry(entry: MealEntry) {
        viewModelScope.launch {
            nutritionRepository.removeEntry(entry)
            _state.value = _state.value.copy(message = "${entry.foodName} rimosso dal piano.")
            load()
        }
    }
}