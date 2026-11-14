package com.fitcoach.app.ui.anamnesis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Stato dell'intervista di anamnesi. L'intervista e progressiva: una domanda
 * per volta, con approfondimenti. La logica di completezza e in
 * [AnamnesisEngine]; qui si gestisce solo la presentazione e la conferma.
 */
data class AnamnesisUiState(
    val loading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val current: AnamnesisEngine.Question? = null,
    val input: String = "",
    val selected: Set<String> = emptySet(),
    val answeredCount: Int = 0,
    val totalRequired: Int = 0,
    val missingRequired: List<String> = emptyList(),
    val contradictions: List<String> = emptyList(),
    val summary: String = "",
    val showSummary: Boolean = false,
    val confirmed: Boolean = false,
    val error: String? = null,
    val generatingPrograms: Boolean = false,
    val generationMessage: String? = null,
    val generationWarnings: List<String> = emptyList(),
    val aiEnabled: Boolean = false
)

@HiltViewModel
class AnamnesisViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val aiProgramGeneration: AiProgramGenerationService
) : ViewModel() {

    private val _state = MutableStateFlow(AnamnesisUiState())
    val state: StateFlow<AnamnesisUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = userRepository.get() ?: UserProfile()
            _state.value = _state.value.copy(
                loading = false,
                profile = profile,
                current = AnamnesisEngine.nextQuestion(profile),
                answeredCount = AnamnesisEngine.questions.count { AnamnesisEngine.isAnswered(profile, it.id) },
                totalRequired = AnamnesisEngine.questions.count { it.required },
                confirmed = profile.anamnesisCompleted
            )
            refreshDerived()
        }
    }

    fun updateInput(v: String) {
        _state.value = _state.value.copy(input = v, error = null)
    }

    fun toggleOption(option: String, multi: Boolean) {
        val current = _state.value.selected
        val next = if (multi) {
            if (option in current) current - option else current + option
        } else {
            setOf(option)
        }
        _state.value = _state.value.copy(selected = next, input = next.joinToString(","), error = null)
    }

    /** Applica la risposta alla domanda corrente e passa alla successiva. */
    fun submit() {
        val q = _state.value.current ?: return
        val raw = _state.value.input.trim()
        if (q.required && raw.isBlank()) {
            _state.value = _state.value.copy(error = "Rispondi alla domanda per proseguire.")
            return
        }
        var profile = AnamnesisEngine.applyAnswer(_state.value.profile, q.id, raw)
        profile = profile.copy(updatedAt = System.currentTimeMillis())
        _state.value = _state.value.copy(
            profile = profile, input = "", selected = emptySet(), error = null,
            current = AnamnesisEngine.nextQuestionAfter(profile, q.id),
            answeredCount = AnamnesisEngine.questions.count { AnamnesisEngine.isAnswered(profile, it.id) }
        )
        refreshDerived()
        persist(profile)
    }

    /** Salta una domanda facoltativa. */
    fun skip() {
        val q = _state.value.current ?: return
        if (q.required) {
            _state.value = _state.value.copy(error = "Questa domanda è obbligatoria.")
            return
        }
        val profile = _state.value.profile
        _state.value = _state.value.copy(
            current = AnamnesisEngine.nextQuestionAfter(profile, q.id),
            input = "", selected = emptySet(), error = null
        )
        refreshDerived()
    }

    fun goBack() {
        val q = _state.value.current ?: return
        val idx = AnamnesisEngine.questions.indexOfFirst { it.id == q.id }
        val prev = AnamnesisEngine.questions.take(idx).lastOrNull() ?: return
        _state.value = _state.value.copy(
            current = prev,
            input = currentAnswerFor(prev.id),
            selected = selectedFor(prev.id),
            error = null
        )
    }

    fun openSummary() {
        refreshDerived()
        _state.value = _state.value.copy(showSummary = true, summary = AnamnesisEngine.summary(_state.value.profile))
    }

    fun closeSummary() { _state.value = _state.value.copy(showSummary = false) }

    /** Conferma l'anamnesi e avvia una generazione reale tramite il provider AI configurato. */
    fun confirm() {
        val profile = _state.value.profile
        if (AnamnesisEngine.missingRequired(profile).isNotEmpty()) {
            _state.value = _state.value.copy(
                showSummary = false,
                error = "Completa le domande obbligatorie prima di confermare.",
                missingRequired = AnamnesisEngine.missingRequired(profile).map { it.prompt }
            )
            return
        }
        val contradictions = profile.anamnesisContradictions
        if (contradictions.isNotEmpty()) {
            _state.value = _state.value.copy(
                showSummary = false,
                contradictions = contradictions,
                error = "Correggi le incongruenze prima di confermare."
            )
            return
        }
        val confirmed = profile.copy(
            anamnesisCompleted = true,
            anamnesisConfirmedAt = System.currentTimeMillis(),
            anamnesisSummary = AnamnesisEngine.summary(profile),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            userRepository.save(confirmed)
            runAiGeneration(confirmed)
        }
    }

    /** Permette di riprovare quando il provider era offline o ha restituito JSON invalido. */
    fun retryGeneration() {
        val profile = _state.value.profile
        if (!profile.isAnamnesisReady || _state.value.generatingPrograms) return
        viewModelScope.launch { runAiGeneration(profile) }
    }

    private suspend fun runAiGeneration(profile: UserProfile) {
        _state.value = _state.value.copy(
            profile = profile,
            confirmed = true,
            showSummary = false,
            error = null,
            generatingPrograms = true,
            generationMessage = "Anamnesi confermata. Il Coach AI sta generando il percorso...",
            generationWarnings = emptyList()
        )
        when (val result = aiProgramGeneration.generate(profile)) {
            is AiProgramGenerationService.Result.Success -> _state.value = _state.value.copy(
                generatingPrograms = false,
                generationMessage = "Percorso generato dal Coach AI e validato: piano alimentare di 7 giorni e scheda salvati.",
                generationWarnings = result.warnings
            )
            is AiProgramGenerationService.Result.Failure -> _state.value = _state.value.copy(
                generatingPrograms = false,
                generationMessage = "Generazione AI non completata. Nessun programma è stato salvato.",
                error = result.message
            )
        }
    }

    private fun persist(profile: UserProfile) {
        viewModelScope.launch { userRepository.save(profile) }
    }

    private fun refreshDerived() {
        val profile = _state.value.profile
        _state.value = _state.value.copy(
            missingRequired = AnamnesisEngine.missingRequired(profile).map { it.prompt },
            contradictions = profile.anamnesisContradictions
        )
    }

    private fun currentAnswerFor(id: String): String = when (id) {
        "age" -> _state.value.profile.ageYears?.toString().orEmpty()
        "heightCm" -> _state.value.profile.heightCm?.toString().orEmpty()
        "weightKg" -> _state.value.profile.weightKg?.toString().orEmpty()
        "sleepHours" -> _state.value.profile.sleepHours?.toString().orEmpty()
        "trainingStatus" -> _state.value.profile.trainingStatus?.name.orEmpty()
        "currentLoads" -> _state.value.profile.currentLoads.entries
            .joinToString(", ") { "${it.key}=${it.value}" }
        else -> ""
    }

    private fun selectedFor(id: String): Set<String> = when (id) {
        "availableEquipment" -> _state.value.profile.availableEquipment.map { it.name }.toSet()
        "priorityMuscles" -> _state.value.profile.priorityMuscles.map { it.name }.toSet()
        "dietaryPreferences" -> _state.value.profile.dietaryPreferences.map { it.name }.toSet()
        else -> emptySet()
    }
}
