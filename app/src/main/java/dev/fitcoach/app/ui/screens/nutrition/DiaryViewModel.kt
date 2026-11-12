package dev.fitcoach.app.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.DiaryEntry
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.MealSlot
import dev.fitcoach.app.domain.repository.DiaryRepository
import dev.fitcoach.app.domain.repository.FoodRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DiaryUiState(
    val dateEpochDay: Long = DateUtils.today().toEpochDay(),
    val entries: List<DiaryEntry> = emptyList(),
    val foods: List<Food> = emptyList(),
    val targetKcal: Double? = null,
    val targetProtein: Double? = null,
    val targetCarbs: Double? = null,
    val targetFat: Double? = null
) {
    val foodById: Map<Long, Food> get() = foods.associateBy { it.id }
    val totals: MacroSet get() = entries.filter { it.consumed }.fold(MacroSet.ZERO) { acc, e ->
        val food = foodById[e.foodId] ?: return@fold acc
        acc + food.macrosFor(e.grams)
    }
    val date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    foodRepository: FoodRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    private val dateFlow = MutableStateFlow(DateUtils.today().toEpochDay())
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val entriesFlow = dateFlow.flatMapLatest { diaryRepository.observeByDate(it) }

    val state: StateFlow<DiaryUiState> = combine(
        dateFlow,
        entriesFlow,
        foodRepository.observeAll(),
        profileRepository.observeProfile()
    ) { date, entries, foods, profile ->
        DiaryUiState(
            dateEpochDay = date,
            entries = entries,
            foods = foods,
            targetKcal = profile?.targetCalories(),
            targetProtein = profile?.proteinTarget(),
            targetCarbs = profile?.carbsTarget(),
            targetFat = profile?.fatTarget()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryUiState())

    fun changeDate(deltaDays: Long) { dateFlow.value = dateFlow.value + deltaDays }
    fun goToToday() { dateFlow.value = DateUtils.today().toEpochDay() }
    fun dismissMessage() { _message.value = null }

    fun addFood(slot: MealSlot, foodId: Long, grams: Double) {
        viewModelScope.launch {
            diaryRepository.log(
                DiaryEntry(
                    dateEpochDay = dateFlow.value,
                    mealSlot = slot,
                    foodId = foodId,
                    grams = grams,
                    consumed = true
                )
            )
            _message.value = "Alimento registrato"
        }
    }

    fun updateGrams(entry: DiaryEntry, grams: Double) {
        viewModelScope.launch { diaryRepository.update(entry.copy(grams = grams)) }
    }

    fun toggleConsumed(entry: DiaryEntry) {
        viewModelScope.launch {
            diaryRepository.update(entry.copy(consumed = !entry.consumed))
            _message.value = if (!entry.consumed) "Consumo confermato" else "Consumo annullato"
        }
    }

    fun delete(entry: DiaryEntry) {
        viewModelScope.launch { diaryRepository.delete(entry.id) }
    }
}
