package dev.fitcoach.app.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.MealItem
import dev.fitcoach.app.domain.model.MealPlan
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.model.PlanDay
import dev.fitcoach.app.domain.repository.FoodRepository
import dev.fitcoach.app.domain.repository.MealPlanRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.usecase.CoachAgent
import dev.fitcoach.app.core.result.AppError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanUiState(
    val plan: MealPlan? = null,
    val days: List<PlanDay> = emptyList(),
    val selectedDayIndex: Int = 0,
    val items: List<MealItem> = emptyList(),
    val foods: List<Food> = emptyList(),
    val message: String? = null
) {
    val foodById: Map<Long, Food> get() = foods.associateBy { it.id }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val foodRepository: FoodRepository,
    private val profileRepository: ProfileRepository,
    private val coachAgent: CoachAgent
) : ViewModel() {

    private val selectedDayIndex = MutableStateFlow(0)
    private val reload = MutableStateFlow(0)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _generating = MutableStateFlow<String?>(null)
    val generating: StateFlow<String?> = _generating.asStateFlow()

    private val planAndItems = combine(
        mealPlanRepository.observeActive(),
        selectedDayIndex,
        reload
    ) { plan, dayIndex, _ -> plan to dayIndex }
        .flatMapLatest { (plan, dayIndex) ->
            flow {
                if (plan == null) {
                    emit(Triple<MealPlan?, List<PlanDay>, List<MealItem>>(null, emptyList(), emptyList()))
                } else {
                    val days = mealPlanRepository.getDays(plan.id)
                    val day = days.firstOrNull { it.dayIndex == dayIndex }
                    val items = day?.let { mealPlanRepository.getItems(it.id) } ?: emptyList()
                    emit(Triple(plan, days, items))
                }
            }
        }

    val state: StateFlow<PlanUiState> = combine(
        planAndItems,
        foodRepository.observeAll(),
        selectedDayIndex,
        _message
    ) { (plan, days, items), foods, dayIndex, msg ->
        PlanUiState(
            plan = plan,
            days = days,
            selectedDayIndex = dayIndex,
            items = items,
            foods = foods,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanUiState())

    fun selectDay(index: Int) { selectedDayIndex.value = index }

    fun dismissMessage() { _message.value = null }

    /** Generates the plan with the agent, one day at a time, reporting progress. */
    fun generateWithAi() {
        viewModelScope.launch {
            _generating.value = "Avvio generazione..."
            try {
                val outcome = coachAgent.generateMealPlan("") { done, total ->
                    _generating.value = "Giorno ${done + 1} di $total..."
                }
                _message.value = "Piano generato: ${outcome.dayCount} giorni, ${outcome.itemCount} voci"
                refresh()
            } catch (error: AppError) {
                _message.value = error.message
            } catch (t: Throwable) {
                _message.value = t.message ?: "Errore durante la generazione"
            } finally {
                _generating.value = null
            }
        }
    }

    private fun refresh() { reload.value = reload.value + 1 }

    fun createEmptyPlan() {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            val plan = MealPlan(
                name = "Piano manuale",
                isActive = false,
                targetKcal = profile?.targetCalories() ?: 0.0,
                targetProtein = profile?.proteinTarget() ?: 0.0,
                targetCarbs = profile?.carbsTarget() ?: 0.0,
                targetFat = profile?.fatTarget() ?: 0.0
            )
            val days = (0..6).map { PlanDay(planId = 0L, dayIndex = it) }
            val id = mealPlanRepository.savePlanAtomic(plan, days, emptyMap())
            mealPlanRepository.setActive(id)
            _message.value = "Piano creato con 7 giorni"
            refresh()
        }
    }

    fun addItem(dayIndex: Int, slot: MealSlot, foodId: Long, grams: Double) {
        viewModelScope.launch {
            val plan = mealPlanRepository.getActive() ?: return@launch
            val days = mealPlanRepository.getDays(plan.id)
            val day = days.firstOrNull { it.dayIndex == dayIndex } ?: return@launch
            val existing = mealPlanRepository.getItems(day.id)
            val newItem = MealItem(planDayId = day.id, mealSlot = slot, foodId = foodId, grams = grams)
            mealPlanRepository.savePlanAtomic(plan, days, mapOf(dayIndex to existing + newItem))
            mealPlanRepository.setActive(plan.id)
            _message.value = "Alimento aggiunto"
            refresh()
        }
    }

    fun updateGrams(item: MealItem, grams: Double) {
        viewModelScope.launch {
            mealPlanRepository.updateItem(item.copy(grams = grams))
            _message.value = "Grammatura aggiornata"
            refresh()
        }
    }

    fun deleteItem(item: MealItem) {
        viewModelScope.launch {
            mealPlanRepository.deleteItem(item.id)
            _message.value = "Alimento rimosso"
            refresh()
        }
    }
}
