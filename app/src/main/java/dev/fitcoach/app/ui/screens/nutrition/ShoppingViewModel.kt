package dev.fitcoach.app.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.ShoppingItem
import dev.fitcoach.app.domain.repository.FoodRepository
import dev.fitcoach.app.domain.repository.MealPlanRepository
import dev.fitcoach.app.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingUiState(
    val weekStart: Long = DateUtils.startOfWeek().toEpochDay(),
    val items: List<ShoppingItem> = emptyList(),
    val message: String? = null
) {
    val total: Double get() = items.mapNotNull { it.estimatedPrice }.sum()
    val remaining: Double get() = items.filter { !it.checked }.mapNotNull { it.estimatedPrice }.sum()
}

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val weekStart = MutableStateFlow(DateUtils.startOfWeek().toEpochDay())
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val state: StateFlow<ShoppingUiState> = combine(
        weekStart,
        shoppingRepository.observeWeek(weekStart.value),
        _message
    ) { week, items, msg ->
        ShoppingUiState(weekStart = week, items = items, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingUiState())

    fun dismissMessage() { _message.value = null }

    fun shiftWeek(weeks: Long) {
        val newStart = DateUtils.fromEpochDay(weekStart.value).plusWeeks(weeks)
        weekStart.value = DateUtils.startOfWeek(newStart).toEpochDay()
    }

    fun addItem(name: String, grams: Double, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            shoppingRepository.add(
                ShoppingItem(
                    name = name.trim(),
                    quantityGrams = grams,
                    category = category.ifBlank { "Altro" },
                    weekStartEpochDay = weekStart.value
                )
            )
        }
    }

    fun toggle(item: ShoppingItem) {
        viewModelScope.launch { shoppingRepository.setChecked(item.id, !item.checked) }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch { shoppingRepository.delete(item.id) }
    }

    /** Builds the shopping list from the active meal plan, aggregating real quantities. */
    fun generateFromPlan() {
        viewModelScope.launch {
            val plan = mealPlanRepository.getActive()
            if (plan == null) {
                _message.value = "Nessun piano attivo da cui generare la lista"
                return@launch
            }
            val days = mealPlanRepository.getDays(plan.id)
            val quantities = mutableMapOf<Long, Double>()
            days.forEach { day ->
                mealPlanRepository.getItems(day.id).forEach { item ->
                    quantities[item.foodId] = (quantities[item.foodId] ?: 0.0) + item.grams
                }
            }
            if (quantities.isEmpty()) {
                _message.value = "Il piano attivo non contiene alimenti"
                return@launch
            }
            val items = quantities.mapNotNull { (foodId, grams) ->
                val food = foodRepository.getById(foodId) ?: return@mapNotNull null
                ShoppingItem(
                    name = food.name,
                    quantityGrams = grams,
                    category = food.category.ifBlank { "Altro" },
                    estimatedPrice = food.priceFor(grams),
                    weekStartEpochDay = weekStart.value
                )
            }
            shoppingRepository.replaceWeek(weekStart.value, items)
            _message.value = "Lista generata dal piano (${items.size} voci)"
        }
    }

    fun clearWeek() {
        viewModelScope.launch {
            shoppingRepository.replaceWeek(weekStart.value, emptyList())
            _message.value = "Lista svuotata"
        }
    }
}
