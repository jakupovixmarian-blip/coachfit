package dev.fitcoach.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep(val title: String) {
    BASICS("Dati di base"),
    GOAL("Obiettivo"),
    EXPERIENCE("Esperienza"),
    LIFESTYLE("Vita quotidiana"),
    HEALTH("Salute e limitazioni"),
    TRAINING("Allenamento"),
    NUTRITION("Alimentazione"),
    SUMMARY("Riepilogo")
}

data class OnboardingState(
    val stepIndex: Int = 0,
    val profile: UserProfile = UserProfile(),
    val savedMessage: String? = null,
    val contradictions: List<String> = emptyList(),
    val finished: Boolean = false
) {
    val step: OnboardingStep get() = OnboardingStep.entries[stepIndex]
    val isLast: Boolean get() = stepIndex == OnboardingStep.entries.lastIndex
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.getProfile()?.let { existing ->
                _state.update { it.copy(profile = existing) }
            }
        }
    }

    fun update(transform: (UserProfile) -> UserProfile) {
        _state.update { it.copy(profile = transform(it.profile)) }
    }

    fun next() {
        val current = _state.value
        val issues = detectContradictions(current.profile)
        if (issues.isNotEmpty() && !current.isLast) {
            _state.update { it.copy(contradictions = issues) }
            return
        }
        persist()
        _state.update {
            it.copy(
                stepIndex = (it.stepIndex + 1).coerceAtMost(OnboardingStep.entries.lastIndex),
                contradictions = emptyList(),
                savedMessage = "Bozza salvata"
            )
        }
    }

    fun back() {
        persist()
        _state.update { it.copy(stepIndex = (it.stepIndex - 1).coerceAtLeast(0), contradictions = emptyList()) }
    }

    fun goTo(index: Int) {
        _state.update { it.copy(stepIndex = index.coerceIn(0, OnboardingStep.entries.lastIndex)) }
    }

    fun confirmContradictions() {
        persist()
        _state.update {
            it.copy(
                stepIndex = (it.stepIndex + 1).coerceAtMost(OnboardingStep.entries.lastIndex),
                contradictions = emptyList()
            )
        }
    }

    fun dismissMessage() = _state.update { it.copy(savedMessage = null) }

    /** Saves the current draft so the wizard can be resumed later. */
    fun persist() {
        val profile = _state.value.profile
        viewModelScope.launch {
            profileRepository.saveProfile(profile.copy(updatedAt = DateUtils.nowMillis()))
        }
    }

    fun finish() {
        viewModelScope.launch {
            val profile = _state.value.profile.copy(
                onboardingCompleted = true,
                updatedAt = DateUtils.nowMillis()
            )
            profileRepository.saveProfile(profile)
            _state.update { it.copy(finished = true) }
        }
    }

    private fun detectContradictions(profile: UserProfile): List<String> {
        val issues = mutableListOf<String>()
        val age = profile.age
        if (age != null && (age < 12 || age > 100)) issues.add("Eta non plausibile: $age anni.")
        if (profile.heightCm != null && (profile.heightCm < 120 || profile.heightCm > 230)) {
            issues.add("Altezza non plausibile: ${profile.heightCm} cm.")
        }
        if (profile.weightKg != null && (profile.weightKg < 30 || profile.weightKg > 300)) {
            issues.add("Peso non plausibile: ${profile.weightKg} kg.")
        }
        if (profile.bodyFatPercent != null && (profile.bodyFatPercent < 3 || profile.bodyFatPercent > 60)) {
            issues.add("Percentuale di grasso non plausibile: ${profile.bodyFatPercent}%.")
        }
        if (profile.experience == ExperienceLevel.BEGINNER && (profile.trainingYears ?: 0.0) > 2.0) {
            issues.add("Livello principiante con oltre 2 anni di allenamento: confermi?")
        }
        if (profile.experience == ExperienceLevel.ADVANCED && (profile.trainingYears ?: 0.0) < 1.0) {
            issues.add("Livello avanzato con meno di 1 anno di allenamento: confermi?")
        }
        if (profile.goal == Goal.FAT_LOSS && profile.dietPreference.contains("surplus", ignoreCase = true)) {
            issues.add("Obiettivo dimagrimento con preferenza dichiarata per il surplus calorico: confermi?")
        }
        if ((profile.availableDays ?: 0) > 7) issues.add("Giorni disponibili superiori a 7.")
        if ((profile.mealsPerDay ?: 0) > 8) issues.add("Numero di pasti elevato: ${profile.mealsPerDay}.")
        return issues
    }
}
