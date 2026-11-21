package com.fitcoach.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.local.entity.SavedRecipeEntity
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.RecipeRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.ComposedRecipe
import com.fitcoach.app.domain.nutrition.MealSlots
import com.fitcoach.app.domain.nutrition.Recipe
import com.fitcoach.app.domain.nutrition.RecipeComposer
import com.fitcoach.app.domain.nutrition.RecipeEngine
import com.fitcoach.app.domain.nutrition.RecipeStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
    val loading: Boolean = true,
    val selectedSlot: MealSlot = MealSlot.BREAKFAST,
    val recipes: List<Recipe> = emptyList(),
    /** Pasti della giornata selezionata per cui esiste almeno una ricetta. */
    val availableSlots: List<MealSlot> = emptyList(),
    val dayIndex: Int = 0,
    val message: String? = null,
    /** Giorni del piano disponibili nel selettore. */
    val dayCount: Int = 0,
    /** Slot che il profilo prevede davvero (dipende dai pasti al giorno). */
    val profileSlots: List<MealSlot> = emptyList(),
    /** Ricette create e salvate dall'utente. */
    val saved: List<SavedRecipeEntity> = emptyList(),
    /** Anteprima della ricetta composta, in attesa di conferma. */
    val draft: ComposedRecipe? = null,
    val draftSlot: MealSlot? = null,
    val draftStyle: RecipeStyle = RecipeStyle.CLASSIC,
    /** Messaggio dedicato al compositore, separato da quello dell'elenco. */
    val composeMessage: String? = null,
    val composing: Boolean = false
)

/**
 * Ricette della giornata: per ogni pasto mostra solo le ricette i cui
 * ingredienti sono davvero presenti in quel pasto del piano attivo.
 */
@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val foodRepository: FoodRepository,
    private val userRepository: UserRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecipesUiState())
    val state: StateFlow<RecipesUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            recipeRepository.observeSaved().collect { saved ->
                _state.value = _state.value.copy(saved = saved)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            val plan = nutritionRepository.getActivePlan()
            if (plan == null) {
                _state.value = _state.value.copy(
                    loading = false,
                    recipes = emptyList(),
                    availableSlots = emptyList(),
                    message = "Nessun piano alimentare attivo: genera il piano per vedere le ricette."
                )
                return@launch
            }
            val day = plan.days.firstOrNull { it.dayIndex == _state.value.dayIndex }
                ?: plan.days.firstOrNull()
            if (day == null) {
                _state.value = _state.value.copy(loading = false, recipes = emptyList())
                return@launch
            }
            val profile = userRepository.get()
            val foods = foodRepository.getAll()
            val bySlot = RecipeEngine.forDay(day.meals, profile, foods)
            val slots = MealSlot.entries.filter { bySlot.containsKey(it) }
            val selected = if (bySlot.containsKey(_state.value.selectedSlot)) {
                _state.value.selectedSlot
            } else {
                slots.firstOrNull() ?: _state.value.selectedSlot
            }
            _state.value = _state.value.copy(
                loading = false,
                dayIndex = day.dayIndex,
                dayCount = plan.days.size,
                profileSlots = MealSlots.forMealsPerDay(profile?.mealsPerDay ?: 5),
                availableSlots = slots,
                selectedSlot = selected,
                recipes = bySlot[selected].orEmpty(),
                message = if (slots.isEmpty()) {
                    "Per questa giornata nessun pasto ha gli alimenti necessari a una ricetta."
                } else {
                    null
                }
            )
        }
    }

    fun selectSlot(slot: MealSlot) {
        _state.value = _state.value.copy(selectedSlot = slot)
        load()
    }

    fun selectDay(index: Int) {
        _state.value = _state.value.copy(dayIndex = index)
        load()
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }

    fun consumeComposeMessage() { _state.value = _state.value.copy(composeMessage = null) }

    fun setDraftStyle(style: RecipeStyle) {
        _state.value = _state.value.copy(draftStyle = style)
        _state.value.draftSlot?.let { compose(it) }
    }

    /**
     * Compone la ricetta per [slot] dagli alimenti reali del pasto nel giorno
     * selezionato. Il risultato resta un'anteprima finche l'utente non salva.
     */
    fun compose(slot: MealSlot) {
        viewModelScope.launch {
            _state.value = _state.value.copy(composing = true, composeMessage = null)
            val plan = nutritionRepository.getActivePlan()
            if (plan == null) {
                _state.value = _state.value.copy(
                    composing = false,
                    draft = null,
                    draftSlot = slot,
                    composeMessage = "Serve un piano alimentare attivo per creare una ricetta."
                )
                return@launch
            }
            val day = plan.days.firstOrNull { it.dayIndex == _state.value.dayIndex }
                ?: plan.days.firstOrNull()
            if (day == null) {
                _state.value = _state.value.copy(
                    composing = false, draft = null, draftSlot = slot,
                    composeMessage = "Nessuna giornata nel piano."
                )
                return@launch
            }
            val meal = day.meals.firstOrNull { it.slot == slot }
            if (meal == null || meal.entries.isEmpty()) {
                _state.value = _state.value.copy(
                    composing = false, draft = null, draftSlot = slot,
                    composeMessage = "Il piano non prevede ${slot.label} in questo giorno: " +
                        "nessun alimento da cui partire."
                )
                return@launch
            }
            val profile = userRepository.get()
            val foods = foodRepository.getAll()
            val avoid = buildSet {
                profile?.excludedFoods?.forEach { add(com.fitcoach.app.domain.model.FoodItem.normalizeName(it)) }
            }
            val composed = RecipeComposer.create(
                entries = meal.entries,
                slot = slot,
                foods = foods,
                style = _state.value.draftStyle,
                avoid = avoid
            )
            _state.value = _state.value.copy(
                composing = false,
                draft = composed,
                draftSlot = slot,
                composeMessage = if (composed == null) {
                    "${slot.label}: servono almeno due alimenti utilizzabili per creare una ricetta."
                } else {
                    null
                }
            )
        }
    }

    fun discardDraft() {
        _state.value = _state.value.copy(draft = null, draftSlot = null, composeMessage = null)
    }

    fun saveDraft() {
        val draft = _state.value.draft ?: return
        viewModelScope.launch {
            recipeRepository.save(draft, _state.value.dayIndex)
            _state.value = _state.value.copy(
                draft = null,
                draftSlot = null,
                composeMessage = "Ricetta salvata."
            )
        }
    }

    fun deleteSaved(id: Long) {
        viewModelScope.launch { recipeRepository.delete(id) }
    }

    fun ingredientsOf(entity: SavedRecipeEntity) = recipeRepository.ingredientsOf(entity)

    fun stepsOf(entity: SavedRecipeEntity) = recipeRepository.stepsOf(entity)
}
