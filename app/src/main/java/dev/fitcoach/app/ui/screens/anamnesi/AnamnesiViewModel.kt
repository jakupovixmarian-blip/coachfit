package dev.fitcoach.app.ui.screens.anamnesi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.ChatRole
import dev.fitcoach.app.domain.model.CoachChatMessage
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.repository.CoachChatRepository
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
 * fields turn by turn; the app only stores what the agent understood.
 *
 * The turns are persisted through [CoachChatRepository], so every message has a
 * real, unique id (the list needs stable keys) and the intake becomes the opening
 * of the same conversation the Coach tab continues.
 */
@HiltViewModel
class AnamnesiViewModel @Inject constructor(
    private val coachAgent: CoachAgent,
    private val profileRepository: ProfileRepository,
    private val chatRepository: CoachChatRepository,
    private val prompts: CoachAgentPrompts
) : ViewModel() {

    private val _state = MutableStateFlow(AnamnesiState())
    val state: StateFlow<AnamnesiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = profileRepository.getProfile()
            val configured = coachAgent.isConfigured()
            val messages = chatRepository.getMessages()
            _state.update {
                it.copy(
                    messages = messages,
                    profile = existing ?: UserProfile(),
                    configured = configured,
                    checking = false
                )
            }
            if (configured && messages.isEmpty()) seedOpeningQuestion()
        }
    }

    /** Re-reads the provider state; seeds the first question once it is ready. */
    fun refreshConfiguration() {
        viewModelScope.launch {
            val configured = coachAgent.isConfigured()
            _state.update { it.copy(configured = configured) }
            if (configured && _state.value.messages.isEmpty()) seedOpeningQuestion()
        }
    }

    /**
     * Writes the agent's first question only when the conversation is still empty.
     * Re-reading the stored turns right before writing keeps two concurrent seeds
     * (screen init and "ricontrolla") from inserting the question twice.
     */
    private suspend fun seedOpeningQuestion() {
        if (chatRepository.getMessages().isNotEmpty()) return
        chatRepository.add(
            CoachChatMessage(
                role = ChatRole.ASSISTANT,
                content = "Ciao! Sono il tuo coach. Raccontami chi sei e cosa vuoi " +
                    "ottenere: mi servono alcuni dati per costruire la tua scheda e il " +
                    "tuo piano alimentare. Iniziamo: come ti chiami e quanti anni hai?",
                createdAt = DateUtils.nowMillis()
            )
        )
        _state.update { it.copy(messages = chatRepository.getMessages()) }
    }

    fun send(text: String) {
        val content = text.trim()
        if (content.isEmpty() || _state.value.sending) return
        _state.update { it.copy(sending = true, error = null) }

        viewModelScope.launch {
            try {
                chatRepository.add(
                    CoachChatMessage(role = ChatRole.USER, content = content, createdAt = DateUtils.nowMillis())
                )
                // Show the user's turn immediately, while the agent is answering.
                _state.update { it.copy(messages = chatRepository.getMessages()) }
                val history = chatRepository.getMessages()
                val reply = coachAgent.anamnesiReply(_state.value.profile, history)
                val patched = prompts.applyProfilePatch(_state.value.profile, reply.profilePatch)
                profileRepository.saveProfile(patched)
                chatRepository.add(
                    CoachChatMessage(
                        role = ChatRole.ASSISTANT,
                        content = reply.reply.ifBlank { "Grazie, ho aggiornato i tuoi dati." },
                        createdAt = DateUtils.nowMillis()
                    )
                )
                _state.update {
                    it.copy(messages = chatRepository.getMessages(), profile = patched, sending = false)
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
