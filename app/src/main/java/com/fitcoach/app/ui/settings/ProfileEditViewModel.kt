package com.fitcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.usecase.ProgramGenerationResult
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProfileEditUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val weightKg: String = "",
    val bodyFat: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val regenerate: Boolean = true
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository,
    private val generationCoordinator: ProgramGenerationCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val p = userRepository.get()
            _state.value = ProfileEditUiState(
                loading = false,
                profile = p,
                weightKg = p?.weightKg?.let { "%.1f".format(it) }.orEmpty(),
                bodyFat = p?.bodyFatPercent?.let { "%.1f".format(it) }.orEmpty()
            )
        }
    }

    fun update(transform: (UserProfile) -> UserProfile) {
        val p = _state.value.profile ?: return
        _state.value = _state.value.copy(profile = transform(p))
    }

    fun updateWeight(v: String) { _state.value = _state.value.copy(weightKg = v) }
    fun updateBodyFat(v: String) { _state.value = _state.value.copy(bodyFat = v) }
    fun setRegenerate(v: Boolean) { _state.value = _state.value.copy(regenerate = v) }
    fun setGoal(g: Goal) = update { it.copy(goal = g) }
    fun setExperience(e: TrainingExperience) = update { it.copy(experience = e) }
    fun setSex(s: Sex) = update { it.copy(sex = s) }
    fun setTrainingDays(d: Int) = update { it.copy(trainingDaysPerWeek = d) }
    fun toggleEquipment(e: Equipment) = update {
        it.copy(availableEquipment = if (e in it.availableEquipment) it.availableEquipment - e
        else it.availableEquipment + e)
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value.profile ?: return@launch
            val newWeight = _state.value.weightKg.replace(',', '.').toDoubleOrNull()
            val newBf = _state.value.bodyFat.replace(',', '.').toDoubleOrNull()?.takeIf { it in 1.0..70.0 }
            val updated = current.copy(
                weightKg = newWeight ?: current.weightKg,
                bodyFatPercent = newBf ?: current.bodyFatPercent,
                updatedAt = System.currentTimeMillis()
            )
            _state.value = _state.value.copy(saving = true)
            userRepository.save(updated)

            if (newWeight != null && newWeight != current.weightKg) {
                progressRepository.addWeight(
                    WeightRecord(dateEpochDay = LocalDate.now().toEpochDay(), weightKg = newWeight)
                )
            }

            val message = if (_state.value.regenerate) {
                when (val result = generationCoordinator.generate(
                    profile = updated,
                    scope = ProgramGenerationCoordinator.Scope.BOTH
                )) {
                    is ProgramGenerationResult.Success -> "Profilo aggiornato e programmi rigenerati."
                    is ProgramGenerationResult.MissingData ->
                        "Profilo salvato, ma dati mancanti: ${result.missing.joinToString()}."
                    is ProgramGenerationResult.AnamnesisNotConfirmed ->
                        "Profilo salvato. Completa l'anamnesi per rigenerare i programmi."
                    is ProgramGenerationResult.Error -> "Profilo salvato. ${result.message}"
                }
            } else {
                "Profilo aggiornato."
            }
            _state.value = _state.value.copy(saving = false, profile = updated, message = message)
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
