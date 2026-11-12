package dev.fitcoach.app.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.ChatRole
import dev.fitcoach.app.domain.model.CoachChatMessage
import dev.fitcoach.app.domain.model.MealPlan
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.repository.CoachChatRepository
import dev.fitcoach.app.domain.repository.MealPlanRepository
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.repository.RoutineRepository
import dev.fitcoach.app.domain.usecase.CoachAction
import dev.fitcoach.app.domain.usecase.CoachAgent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachChatState(
    val messages: List<CoachChatMessage> = emptyList(),
    val profile: UserProfile? = null,
    val program: List<Routine> = emptyList(),
    val plan: MealPlan? = null,
    val configured: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null,
    val appliedActions: List<String> = emptyList()
)

/**
 * The chat through which the agent manages the whole program. The user writes in
 * plain language; the agent answers and asks the app to run real actions, which
 * are applied here and reported back in the conversation.
 */
@HiltViewModel
class CoachViewModel @Inject constructor(
    private val coachChatRepository: CoachChatRepository,
    private val coachAgent: CoachAgent,
    private val profileRepository: ProfileRepository,
    private val routineRepository: RoutineRepository,
    private val mealPlanRepository: MealPlanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CoachChatState())
    val state: StateFlow<CoachChatState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            coachChatRepository.observeMessages().collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            routineRepository.observeActiveProgram().collect { program ->
                _state.update { it.copy(program = program) }
            }
        }
        viewModelScope.launch {
            mealPlanRepository.observeActive().collect { plan ->
                _state.update { it.copy(plan = plan) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    profile = profileRepository.getProfile(),
                    configured = coachAgent.isConfigured()
                )
            }
        }
    }

    fun send(text: String) {
        val content = text.trim()
        if (content.isEmpty() || _state.value.sending) return
        val history = _state.value.messages
        viewModelScope.launch {
            coachChatRepository.add(CoachChatMessage(role = ChatRole.USER, content = content, createdAt = DateUtils.nowMillis()))
            _state.update { it.copy(sending = true, error = null, appliedActions = emptyList()) }
            try {
                val reply = coachAgent.chat(history, content)
                val applied = mutableListOf<String>()
                reply.actions.forEach { action ->
                    runCatching { coachAgent.applyAction(action) }
                        .getOrNull()
                        ?.let { applied += it }
                }
                coachChatRepository.add(
                    CoachChatMessage(
                        role = ChatRole.ASSISTANT,
                        content = reply.reply.ifBlank { "Fatto." },
                        createdAt = DateUtils.nowMillis(),
                        actionSummary = applied.joinToString(" ")
                    )
                )
                _state.update { it.copy(sending = false, appliedActions = applied) }
            } catch (error: AppError) {
                _state.update { it.copy(sending = false, error = error.message) }
            } catch (t: Throwable) {
                _state.update { it.copy(sending = false, error = t.message ?: "Errore imprevisto") }
            }
        }
    }

    /** Direct shortcut: ask the agent to regenerate the program. */
    fun regenerateProgram() = runAction(CoachAction.GenerateProgram(""))

    /** Direct shortcut: ask the agent to regenerate the meal plan. */
    fun regenerateMealPlan() = runAction(CoachAction.GenerateMealPlan(""))

    private fun runAction(action: CoachAction) {
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            try {
                val result = coachAgent.applyAction(action)
                val text = result ?: "Azione non applicata: dati insufficienti."
                coachChatRepository.add(
                    CoachChatMessage(role = ChatRole.ASSISTANT, content = text, createdAt = DateUtils.nowMillis(), actionSummary = text)
                )
                _state.update { it.copy(sending = false) }
            } catch (error: AppError) {
                _state.update { it.copy(sending = false, error = error.message) }
            } catch (t: Throwable) {
                _state.update { it.copy(sending = false, error = t.message ?: "Errore imprevisto") }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch { coachChatRepository.clear() }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
