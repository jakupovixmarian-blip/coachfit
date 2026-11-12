package dev.fitcoach.app.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.data.ai.OpenFoodFactsClient
import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState
import dev.fitcoach.app.domain.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodSearchState(
    val query: String = "",
    val remoteResults: List<Food> = emptyList(),
    val loading: Boolean = false,
    val info: String? = null
)

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val openFoodFacts: OpenFoodFactsClient
) : ViewModel() {

    val foods: StateFlow<List<Food>> =
        foodRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _search = MutableStateFlow(FoodSearchState())
    val search: StateFlow<FoodSearchState> = _search.asStateFlow()

    fun onQueryChange(query: String) { _search.value = _search.value.copy(query = query) }

    fun searchOpenFoodFacts() {
        val query = _search.value.query.trim()
        if (query.length < 3) {
            _search.value = _search.value.copy(info = "Inserisci almeno 3 caratteri")
            return
        }
        viewModelScope.launch {
            _search.value = _search.value.copy(loading = true, info = null)
            val results = openFoodFacts.searchByName(query)
            _search.value = _search.value.copy(
                loading = false,
                remoteResults = results,
                info = if (results.isEmpty()) "Nessun risultato da Open Food Facts" else "${results.size} risultati reali da Open Food Facts"
            )
        }
    }

    fun clearRemote() { _search.value = _search.value.copy(remoteResults = emptyList(), info = null) }

    fun addManualFood(name: String, kcal: Double, protein: Double, carbs: Double, fat: Double, gramsPerUnit: Double? = null) {
        if (name.isBlank() || kcal < 0) return
        viewModelScope.launch {
            foodRepository.add(
                Food(
                    name = name.trim(),
                    kcalPer100g = kcal,
                    proteinPer100g = protein,
                    carbsPer100g = carbs,
                    fatPer100g = fat,
                    source = FoodSource.MANUAL,
                    state = FoodState.RAW,
                    isCustom = true
                )
            )
            _search.value = _search.value.copy(info = "Alimento creato")
        }
    }

    fun importFood(food: Food) {
        viewModelScope.launch {
            foodRepository.add(food.copy(id = 0L, isCustom = false))
            _search.value = _search.value.copy(info = "Alimento importato in archivio")
        }
    }

    fun deleteFood(food: Food) {
        viewModelScope.launch { foodRepository.delete(food.id) }
    }
}
