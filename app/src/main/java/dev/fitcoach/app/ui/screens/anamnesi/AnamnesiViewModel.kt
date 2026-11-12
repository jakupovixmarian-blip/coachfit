package dev.fitcoach.app.ui.screens.anamnesi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.ChatRole
import dev.fitcoach.app.domain.model.CoachChatMessage
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.repository.ProfileRepository
import dev.fitcoach.app.domain.usecase.CoachAgent
import dev.fitcoach.app.domain.usecase.CoachAgentPrompts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnamnesiState(
    val messages: List<CoachChatMessage> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val configured: Boolean = false,
    val checking: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null,
    val finished: Boolean = false
) {
    val canSend: Boolean get() = configured && !sending
}

/**
 * Conversational intake. The agent asks the questions and extracts the profile
 * fields turn by turn; the app only stores what the agent understood. There is no
 * manual form: when the provider is missing the screen asks to configure it first.
 */
@HiltViewModel
class AnamnesiViewModel @Inject constructor(
    private val coachAgent: CoachAgent,
    private val profileRepository: ProfileRepository,
    private val prompts: CoachAgentPrompts
) : ViewModel() {

    private val _state = MutableStateFlow(AnamnesiState())
    val state: StateFlow<AnamnesiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = profileRepository.getProfile()
            val configured = coachAgent.isConfigured()
            val opening = if (configured && existing == null) {
                listOf(
                    CoachChatMessage(
                        role = ChatRole.ASSISTANT,
                        content = "Ciao! Sono il tuo coach. Raccontami chi sei e cosa vuoi " +
                            "ottenere: mi servono alcuni dati per costruire la tua scheda e il " +
                            "tuo piano alimentare. Iniziamo: come ti chiami e quanti anni hai?",
                        createdAt = DateUtils.nowMillis()
                    )
                )
            } else {
                emptyList()
            }
            _state.update {
                it.copy(
                    profile = existing ?: UserProfile(),
                    configured = configured,
                    checking = false,
                    messages = opening
                )
            }
        }
    }

    fun refreshConfiguration() {
        viewModelScope.launch { _state.update { it.copy(configured = coachAgent.isConfigured()) } }
    }

    fun send(text: String) {
        val content = text.trim()
        if (content.isEmpty() || _state.value.sending) return
        val userMessage = CoachChatMessage(role = ChatRole.USER, content = content, createdAt = DateUtils.nowMillis())
        val conversation = _state.value.messages + userMessage
        _state.update { it.copy(messages = conversation, sending = true, error = null) }

        viewModelScope.launch {
            try {
                val reply = coachAgent.anamnesiReply(_state.value.profile, conversation)
                val patched = prompts.applyProfilePatch(_state.value.profile, reply.profilePatch)
                profileRepository.saveProfile(patched)
                val assistantMessage = CoachChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = reply.reply.ifBlank { "Grazie, ho aggiornato i tuoi dati." },
                    createdAt = DateUtils.nowMillis()
                )
                _state.update {
                    it.copy(messages = it.messages + assistantMessage, profile = patched, sending = false)
                }
                if (reply.complete) finish()
            } catch (error: AppError) {
                _state.update { it.copy(sending = false, error = error.message) }
            } catch (t: Throwable) {
                _state.update { it.copy(sending = false, error = t.message ?: "Errore imprevisto") }
            }
        }
    }

    /** Marks the intake as complete so the app can move on to the home screen. */
    fun finish() {
        viewModelScope.launch {
            val profile = _state.value.profile.copy(
                onboardingCompleted = true,
                updatedAt = DateUtils.nowMillis()
            )
            profileRepository.saveProfile(profile)
            _state.update { it.copy(profile = profile, finished = true) }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
