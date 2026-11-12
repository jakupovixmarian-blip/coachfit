package dev.fitcoach.app.ui.screens.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.domain.model.KnowledgeNote
import dev.fitcoach.app.domain.model.KnowledgeTopic
import dev.fitcoach.app.domain.repository.KnowledgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgeUiState(
    val notes: List<KnowledgeNote> = emptyList(),
    val message: String? = null
)

/**
 * Manages the sources the agent is allowed to reason on: the built-in, cited
 * training principles plus the notes the user adds (for example from Project
 * Invictus). Disabling a note removes it from the agent context.
 */
@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val repository: KnowledgeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(KnowledgeUiState())
    val state: StateFlow<KnowledgeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { notes ->
                _state.value = _state.value.copy(notes = notes)
            }
        }
    }

    fun addNote(topic: KnowledgeTopic, title: String, content: String, source: String) {
        if (title.isBlank() || content.isBlank()) {
            _state.value = _state.value.copy(message = "Titolo e contenuto sono obbligatori.")
            return
        }
        viewModelScope.launch {
            repository.add(
                KnowledgeNote(
                    topic = topic,
                    title = title.trim(),
                    content = content.trim(),
                    source = source.trim().ifBlank { "Nota dell'utente" },
                    builtIn = false
                )
            )
            _state.value = _state.value.copy(message = "Nota aggiunta")
        }
    }

    fun toggle(note: KnowledgeNote) {
        viewModelScope.launch { repository.update(note.copy(enabled = !note.enabled)) }
    }

    fun delete(note: KnowledgeNote) {
        if (note.builtIn) {
            _state.value = _state.value.copy(message = "Le fonti predefinite si possono disattivare, non eliminare.")
            return
        }
        viewModelScope.launch {
            repository.delete(note.id)
            _state.value = _state.value.copy(message = "Nota eliminata")
        }
    }

    fun dismissMessage() { _state.value = _state.value.copy(message = null) }
}
