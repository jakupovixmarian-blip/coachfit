package com.fitcoach.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.domain.ProfileRepository
import com.fitcoach.nutrition.BiologicalSex
import com.fitcoach.nutrition.EnergyCalculator
import com.fitcoach.nutrition.EnergyEstimate
import com.fitcoach.nutrition.NutritionGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {
    private val _estimate = MutableStateFlow<EnergyEstimate?>(null)
    val estimate: StateFlow<EnergyEstimate?> = _estimate.asStateFlow()

    fun calculate() {
        viewModelScope.launch {
            val profile = repository.observeProfile().first() ?: return@launch
            val age = profile.age ?: return@launch
            val height = profile.heightCm ?: return@launch
            val weight = profile.weightKg ?: return@launch
            val sex = when (profile.biologicalSex?.trim()?.lowercase()) {
                "m", "male", "uomo", "maschio" -> BiologicalSex.MALE
                "f", "female", "donna", "femmina" -> BiologicalSex.FEMALE
                else -> return@launch
            }
            val goal = when (profile.goal?.trim()?.lowercase()) {
                "ipertrofia", "hypertrophy" -> NutritionGoal.HYPERTROPHY
                "definizione", "cut" -> NutritionGoal.DEFINITION
                "mantenimento", "maintenance" -> NutritionGoal.MAINTENANCE
                else -> NutritionGoal.RECOMPOSITION
            }
            _estimate.value = EnergyCalculator.estimate(age, sex, height, weight, 1.4, goal)
        }
    }
}
