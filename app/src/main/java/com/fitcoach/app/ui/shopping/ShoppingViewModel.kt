package com.fitcoach.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.ShoppingRepository
import com.fitcoach.app.domain.model.ShoppingCategory
import com.fitcoach.app.domain.model.ShoppingItem
import com.fitcoach.app.domain.nutrition.ShoppingListEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingUiState(
    val loading: Boolean = true,
    val weekStart: Long = 0,
    val items: List<ShoppingItem> = emptyList(),
    val message: String? = null,
    val addDialog: Boolean = false,
    /** Voci gia chiuse come acquistate nella settimana (fuori dalla lista attiva). */
    val completedCount: Int = 0
) {
    val grouped: Map<ShoppingCategory, List<ShoppingItem>>
        get() = items.groupBy { it.category }.toSortedMap(compareBy { it.ordinal })

    val total: Int get() = ShoppingListEngine.totalItems(items)
    val purchased: Int get() = ShoppingListEngine.purchasedCount(items)
    val progress: Float get() = ShoppingListEngine.progress(items)
    val checkedCount: Int get() = items.count { it.checked }
    val shareText: String get() = ShoppingListEngine.toShareText(items)
}

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        ShoppingUiState(
            weekStart = NutritionDiaryRepository.weekStart(NutritionDiaryRepository.todayEpochDay())
        )
    )
    val state: StateFlow<ShoppingUiState> = _state.asStateFlow()

    init {
        // La lista e derivata dal piano attivo: reagisce automaticamente quando il
        // piano viene generato o modificato, cosi non resta mai vuota se il piano
        // esiste (il tab Spesa puo essere aperto prima della generazione).
        observePlan()
    }

    private fun observePlan() {
        viewModelScope.launch {
            // Carica anche lo stato già persistito prima di attendere le invalidazioni Room.
            if (nutritionRepository.getActivePlan() != null) regenerateAndRefresh()
            nutritionRepository.activePlanFlow.collect { plan ->
                if (plan == null) {
                    _state.value = _state.value.copy(loading = false, items = emptyList())
                } else {
                    regenerateAndRefresh()
                }
            }
        }
    }

    private suspend fun regenerateAndRefresh() {
        val weekStart = _state.value.weekStart
        val result = shoppingRepository.regenerateAndRead(weekStart)
        _state.value = _state.value.copy(
            loading = false,
            items = result.items,
            completedCount = shoppingRepository.purchasedCount(weekStart),
            message = if (result.invalidated.isNotEmpty()) {
                "Sostituzione rilevata: ricontrolla ${result.invalidated.joinToString()}."
            } else _state.value.message
        )
    }

    fun load() {
        viewModelScope.launch { regenerateAndRefresh() }
    }

    fun toggleChecked(id: Long, checked: Boolean) {
        viewModelScope.launch {
            shoppingRepository.setChecked(id, checked)
            refresh()
        }
    }

    fun toggleAlreadyHave(id: Long, alreadyHave: Boolean) {
        viewModelScope.launch {
            shoppingRepository.setAlreadyHave(id, alreadyHave)
            refresh()
        }
    }

    fun setGrams(id: Long, grams: Double) {
        viewModelScope.launch {
            shoppingRepository.setGrams(id, grams)
            refresh()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            shoppingRepository.delete(id)
            refresh()
        }
    }

    fun showAddDialog(show: Boolean) {
        _state.value = _state.value.copy(addDialog = show)
    }

    fun addManual(name: String, grams: Double, category: ShoppingCategory?) {
        viewModelScope.launch {
            if (name.isBlank() || grams <= 0) {
                _state.value = _state.value.copy(message = "Inserisci un nome e una quantita valida.")
                return@launch
            }
            shoppingRepository.addManual(_state.value.weekStart, name.trim(), grams, category?.name)
            _state.value = _state.value.copy(addDialog = false, message = "Alimento aggiunto alla lista.")
            refresh()
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    /**
     * Chiude la spesa: gli alimenti spuntati escono dalla lista e restano
     * registrati come acquistati per la settimana.
     */
    fun completeShopping() {
        viewModelScope.launch {
            val checked = _state.value.checkedCount
            if (checked == 0) {
                _state.value = _state.value.copy(
                    message = "Spunta prima gli alimenti che hai comprato."
                )
                return@launch
            }
            val closed = shoppingRepository.completeShopping(_state.value.weekStart)
            _state.value = _state.value.copy(
                message = "Spesa effettuata: $closed alimenti rimossi dalla lista."
            )
            refresh()
        }
    }

    /** Riporta in lista tutte le voci chiuse come acquistate in questa settimana. */
    fun reopenShopping() {
        viewModelScope.launch {
            val reopened = shoppingRepository.reopenShopping(_state.value.weekStart)
            _state.value = _state.value.copy(
                message = if (reopened > 0) "Spesa riaperta: $reopened alimenti rimessi in lista."
                else "Nessun alimento acquistato da ripristinare."
            )
            refresh()
        }
    }

    private suspend fun refresh() {
        val weekStart = _state.value.weekStart
        _state.value = _state.value.copy(
            items = shoppingRepository.itemsForWeek(weekStart),
            completedCount = shoppingRepository.purchasedCount(weekStart)
        )
    }
}
