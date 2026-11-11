package com.fitcoach.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.domain.model.DailyBalance
import com.fitcoach.app.domain.model.DiaryEntry
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.WeeklyNutritionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiaryUiState(
    val loading: Boolean = true,
    val epochDay: Long = NutritionDiaryRepository.todayEpochDay(),
    val balance: DailyBalance? = null,
    val entries: List<DiaryEntry> = emptyList(),
    val weekly: WeeklyNutritionSummary? = null,
    val message: String? = null,
    // Dialog aggiunta alimento
    val addDialog: Boolean = false,
    val query: String = "",
    val searchResults: List<FoodItem> = emptyList(),
    val pendingFood: FoodItem? = null
) {
    val byMeal: Map<MealSlot, List<DiaryEntry>>
        get() = MealSlot.entries.associateWith { slot ->
            entries.filter { it.mealSlot == slot && it.active }.sortedBy { it.createdAt }
        }.filterValues { it.isNotEmpty() }
}

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: NutritionDiaryRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiaryUiState())
    val state: StateFlow<DiaryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val epochDay = _state.value.epochDay
            val balance = diaryRepository.balanceForDay(epochDay)
            val entries = diaryRepository.entriesForDay(epochDay)
            val weekly = diaryRepository.weeklySummary(NutritionDiaryRepository.weekStart(epochDay))
            _state.value = _state.value.copy(
                loading = false,
                balance = balance,
                entries = entries,
                weekly = weekly
            )
        }
    }

    fun previousDay() {
        _state.value = _state.value.copy(epochDay = _state.value.epochDay - 1)
        load()
    }

    fun nextDay() {
        if (_state.value.epochDay >= NutritionDiaryRepository.todayEpochDay()) return
        _state.value = _state.value.copy(epochDay = _state.value.epochDay + 1)
        load()
    }

    fun updateGrams(entryId: Long, grams: Double) {
        viewModelScope.launch {
            if (grams <= 0) return@launch
            diaryRepository.updateGrams(entryId, grams)
            load()
        }
    }

    fun toggleActive(entryId: Long, active: Boolean) {
        viewModelScope.launch {
            diaryRepository.setActive(entryId, active)
            load()
        }
    }

    fun moveToMeal(entryId: Long, slot: MealSlot) {
        viewModelScope.launch {
            diaryRepository.moveToMeal(entryId, slot)
            load()
        }
    }

    fun delete(entryId: Long) {
        viewModelScope.launch {
            diaryRepository.delete(entryId)
            load()
        }
    }

    fun openAddDialog() {
        _state.value = _state.value.copy(addDialog = true, query = "", searchResults = emptyList(), pendingFood = null)
    }

    fun closeAddDialog() {
        _state.value = _state.value.copy(addDialog = false, pendingFood = null)
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        viewModelScope.launch {
            val results = if (query.isBlank()) emptyList() else foodRepository.search(query).take(20)
            _state.value = _state.value.copy(searchResults = results)
        }
    }

    fun selectFood(food: FoodItem) {
        _state.value = _state.value.copy(pendingFood = food)
    }

    fun addExtraFood(food: FoodItem, grams: Double, slot: MealSlot) {
        viewModelScope.launch {
            if (grams <= 0) {
                _state.value = _state.value.copy(message = "Inserisci una quantita valida.")
                return@launch
            }
            diaryRepository.addExtraFood(food, grams, slot, slot.defaultTime, _state.value.epochDay)
            _state.value = _state.value.copy(addDialog = false, pendingFood = null, message = "Alimento registrato nel diario.")
            load()
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
