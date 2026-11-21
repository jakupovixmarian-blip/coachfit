package com.fitcoach.app.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ApplyResult
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.domain.coach.CoachService
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.ChatMessage
import com.fitcoach.app.domain.model.ChatRole
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.ProgramChange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachUiState(
    val loading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val pendingChange: ProgramChange? = null,
    /** Modifiche gia applicate, con possibilita di annullamento (rollback). */
    val appliedChanges: List<ProgramChange> = emptyList(),
    val message: String? = null,
    val providerConfigured: Boolean = false,
    val providerLabel: String = "",
    /** Ultimo messaggio dell'utente, per riprovare dopo un errore di rete. */
    val lastUserMessage: String? = null,
    val canRetry: Boolean = false,
    /**
     * Vero mentre una proposta viene applicata. L'applicazione puo includere una
     * rigenerazione con chiamata al provider, quindi dura decine di secondi:
     * senza questo stato il pulsante resta cliccabile e sembra bloccato.
     */
    val applying: Boolean = false,
    /** Avanzamento leggibile dell'applicazione in corso. */
    val applyingProgress: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val changeRepository: ChangeRepository,
    private val preferencesRepository: PreferencesRepository,
    private val coachService: CoachService,
    private val actionExecutor: CoachActionExecutor
) : ViewModel() {

    private val _state = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val messages = chatRepository.getRecent(limit = 100)
            val settings = chatRepository.getAiSettings()
            val allChanges = changeRepository.getAll()
            val pending = allChanges
                .filter { it.status == ChangeStatus.PROPOSED }
                .maxByOrNull { it.createdAt }
            val applied = allChanges
                .filter { it.status == ChangeStatus.APPLIED }
                .sortedByDescending { it.appliedAt ?: it.createdAt }
                .take(10)
            _state.value = _state.value.copy(
                loading = false,
                messages = messages,
                pendingChange = pending,
                appliedChanges = applied,
                providerConfigured = settings.provider != AiProvider.OPENAI || settings.model.isNotBlank(),
                providerLabel = "${settings.provider.label} · ${settings.model.ifBlank { settings.provider.defaultModel }}"
            )
        }
    }

    fun updateInput(v: String) { _state.value = _state.value.copy(input = v) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.sending) return
        _state.value = _state.value.copy(input = "")
        dispatch(text, echoUser = true)
    }

    /** Riprova l'ultimo messaggio dopo un errore transitorio, senza duplicarlo in chat. */
    fun retry() {
        val text = _state.value.lastUserMessage ?: return
        if (_state.value.sending) return
        viewModelScope.launch {
            // Rimuovi il messaggio di errore prima di riprovare.
            _state.value.messages.filter { it.isError }.forEach { chatRepository.delete(it.id) }
            load()
        }
        dispatch(text, echoUser = false)
    }

    private fun dispatch(text: String, echoUser: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                sending = true, canRetry = false, lastUserMessage = text
            )
            if (echoUser) chatRepository.add(ChatMessage(role = ChatRole.USER, content = text))
            load()

            val reply = coachService.send(text)

            if (reply.providerError != null) {
                chatRepository.add(
                    ChatMessage(
                        role = ChatRole.ASSISTANT,
                        content = "Impossibile contattare il provider AI: ${reply.providerError}",
                        isError = true
                    )
                )
                _state.value = _state.value.copy(
                    sending = false, message = reply.providerError, canRetry = true
                )
                load()
                return@launch
            }

            val assistantText = reply.text.ifBlank { "Ho elaborato la richiesta." }
            chatRepository.add(ChatMessage(role = ChatRole.ASSISTANT, content = assistantText))

            // Le proposte degli strumenti di scrittura arrivano gia registrate (PROPOSED).
            val pendingFromTool = reply.pendingChange
            if (pendingFromTool != null) {
                _state.value = _state.value.copy(pendingChange = pendingFromTool)
            } else {
                reply.proposedAction?.let { action ->
                    val changeId = actionExecutor.registerProposal(action)
                    val mode = preferencesRepository.getOrCreate().coachAuthorizationMode
                    val ordinary = action.action in setOf(
                        com.fitcoach.app.domain.coach.CoachAction.ACTION_ADJUST_CALORIES,
                        com.fitcoach.app.domain.coach.CoachAction.ACTION_SUBSTITUTE_FOOD
                    )
                    if (mode == com.fitcoach.app.domain.model.CoachAuthorizationMode.ADAPTIVE && ordinary) {
                        when (val result = actionExecutor.apply(changeId)) {
                            is ApplyResult.Applied -> _state.value = _state.value.copy(message = "Applicata automaticamente: ${result.message}")
                            is ApplyResult.Failed -> _state.value = _state.value.copy(message = result.message)
                            else -> Unit
                        }
                    } else {
                        _state.value = _state.value.copy(pendingChange = changeRepository.getById(changeId))
                    }
                }
            }

            _state.value = _state.value.copy(sending = false, canRetry = false, lastUserMessage = null)
            load()
        }
    }

    fun approvePending() {
        val change = _state.value.pendingChange ?: return
        if (_state.value.applying) return
        _state.value = _state.value.copy(
            applying = true,
            applyingProgress = "Applico la modifica...",
            message = null
        )
        viewModelScope.launch {
            val result = try {
                actionExecutor.apply(change.id)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ApplyResult.Failed("Applicazione non riuscita: ${e.message ?: e::class.simpleName}")
            }
            _state.value = when (result) {
                is ApplyResult.Applied -> _state.value.copy(
                    applying = false,
                    applyingProgress = null,
                    message = "Modifica applicata: ${result.message}",
                    pendingChange = null
                )
                is ApplyResult.Failed -> _state.value.copy(
                    applying = false,
                    applyingProgress = null,
                    message = result.message
                )
                // Un esito non previsto non deve lasciare la schermata in attesa.
                is ApplyResult.Reverted -> _state.value.copy(
                    applying = false, applyingProgress = null
                )
            }
            load()
        }
    }

    fun rejectPending() {
        val change = _state.value.pendingChange ?: return
        viewModelScope.launch {
            changeRepository.updateStatus(change.id, ChangeStatus.REJECTED)
            _state.value = _state.value.copy(pendingChange = null, message = "Proposta rifiutata.")
            load()
        }
    }

    fun revert(changeId: Long) {
        viewModelScope.launch {
            when (val result = actionExecutor.revert(changeId)) {
                is ApplyResult.Reverted -> _state.value = _state.value.copy(message = result.message)
                is ApplyResult.Failed -> _state.value = _state.value.copy(message = result.message)
                else -> Unit
            }
            load()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clear()
            load()
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
