package com.fitcoach.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.Recipe
import com.fitcoach.app.domain.nutrition.RecipeEngine
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
    val message: String? = null
)

/**
 * Ricette della giornata: per ogni pasto mostra solo le ricette i cui
 * ingredienti sono davvero presenti in quel pasto del piano attivo.
 */
@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val foodRepository: FoodRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecipesUiState())
    val state: StateFlow<RecipesUiState> = _state.asStateFlow()

    init {
        load()
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
}
