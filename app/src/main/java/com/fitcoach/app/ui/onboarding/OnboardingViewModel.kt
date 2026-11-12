package com.fitcoach.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.domain.ProfileRepository
import com.fitcoach.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingState(
    val age: String = "",
    val biologicalSex: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val bodyFatPercent: String = "",
    val goal: String = "",
    val trainingExperience: String = "",
    val trainingDays: String = "",
    val error: String? = null,
    val saving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun update(transform: (OnboardingState) -> OnboardingState) {
        _state.value = transform(_state.value).copy(error = null)
    }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        val age = current.age.toIntOrNull()
        val height = current.heightCm.toDoubleOrNull()
        val weight = current.weightKg.toDoubleOrNull()
        if (age == null || height == null || weight == null || current.biologicalSex.isBlank() || current.goal.isBlank()) {
            _state.value = current.copy(error = "Compila età, sesso, altezza, peso e obiettivo.")
            return
        }
        if (age !in 13..100 || height !in 100.0..250.0 || weight !in 30.0..300.0) {
            _state.value = current.copy(error = "Controlla i valori inseriti.")
            return
        }
        _state.value = current.copy(saving = true)
        viewModelScope.launch {
            repository.saveProfile(
                UserProfile(
                    age = age,
                    biologicalSex = current.biologicalSex,
                    heightCm = height,
                    weightKg = weight,
                    bodyFatPercent = current.bodyFatPercent.toDoubleOrNull(),
                    goal = current.goal,
                    trainingExperience = current.trainingExperience.ifBlank { null },
                    availableTrainingDays = current.trainingDays.toIntOrNull()
                )
            )
            _state.value = _state.value.copy(saving = false)
            onSaved()
        }
    }
}
