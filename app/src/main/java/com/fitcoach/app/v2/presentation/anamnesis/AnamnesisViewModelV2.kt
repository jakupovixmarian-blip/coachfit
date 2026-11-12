package com.fitcoach.app.v2.presentation.anamnesis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisAnswer
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisField
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisReview
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisSession
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AnamnesisV2State(
    val loading: Boolean = true,
    val session: AnamnesisSession? = null,
    val currentField: AnamnesisField? = null,
    val input: String = "",
    val review: AnamnesisReview? = null,
    val showReview: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AnamnesisViewModelV2 @Inject constructor(
    private val store: AnamnesisStore
) : ViewModel() {
    private val _state = MutableStateFlow(AnamnesisV2State())
    val state: StateFlow<AnamnesisV2State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = store.load() ?: AnamnesisSession(UUID.randomUUID().toString())
            _state.value = AnamnesisV2State(
                loading = false,
                session = session,
                currentField = AnamnesisEngine.nextQuestion(session),
                review = AnamnesisEngine.review(session)
            )
        }
    }

    fun updateInput(value: String) {
        _state.value = _state.value.copy(input = value, error = null)
    }

    fun submit() {
        val state = _state.value
        val session = state.session ?: return
        val field = state.currentField ?: run {
            openReview()
            return
        }
        val value = state.input.trim()
        if (value.isBlank()) {
            _state.value = state.copy(error = "Inserisci una risposta oppure usa Non lo so.")
            return
        }
        answer(session, field, AnamnesisAnswer.provided(value, System.currentTimeMillis()))
    }

    fun answerUnknown() {
        val state = _state.value
        val session = state.session ?: return
        val field = state.currentField ?: return
        answer(session, field, AnamnesisAnswer.unknown(System.currentTimeMillis()))
    }

    fun openReview() {
        val session = _state.value.session ?: return
        _state.value = _state.value.copy(review = AnamnesisEngine.review(session), showReview = true)
    }

    fun closeReview() {
        _state.value = _state.value.copy(showReview = false)
    }

    fun confirm() {
        val state = _state.value
        val session = state.session ?: return
        runCatching { AnamnesisEngine.confirm(session, System.currentTimeMillis()) }
            .onSuccess { confirmed ->
                viewModelScope.launch {
                    store.save(confirmed)
                    _state.value = state.copy(
                        session = confirmed,
                        review = AnamnesisEngine.review(confirmed),
                        showReview = false,
                        error = null
                    )
                }
            }
            .onFailure { error ->
                _state.value = state.copy(error = error.message ?: "Anamnesi non confermabile.")
            }
    }

    private fun answer(session: AnamnesisSession, field: AnamnesisField, answer: AnamnesisAnswer) {
        val nextSession = AnamnesisEngine.answer(session, field, answer)
        _state.value = _state.value.copy(
            session = nextSession,
            currentField = AnamnesisEngine.nextQuestion(nextSession),
            input = "",
            review = AnamnesisEngine.review(nextSession),
            error = null
        )
        viewModelScope.launch { store.save(nextSession) }
    }
}
