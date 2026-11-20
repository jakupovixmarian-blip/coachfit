package com.fitcoach.app.ui.anamnesis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fitcoach.app.diagnostics.CrashLog
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
    /** True quando la generazione (riuscita o fallita) e terminata: si puo tornare alla Home. */
    val generationFinished: Boolean = false,
    val generationMessage: String? = null,
    val generationWarnings: List<String> = emptyList(),
    val aiEnabled: Boolean = false,
    /** True quando si sta modificando un'anamnesi gia confermata (flusso completo, precompilato). */
    val editing: Boolean = false,
    /** Sezione corrente dell'editor a sezioni (modalita modifica). */
    val editSection: String? = null,
    /** True dopo un reset riuscito: l'utente sta ricompilando da zero. */
    val resetDone: Boolean = false
)

@HiltViewModel
class AnamnesisViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val aiProgramGeneration: AiProgramGenerationService,
    @ApplicationContext private val context: android.content.Context
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

    /** Avvia la modifica dell'anamnesi confermata: flusso completo e precompilato dalla prima sezione. */
    fun startEdit() {
        val profile = _state.value.profile
        val first = AnamnesisEngine.questions.firstOrNull() ?: return
        _state.value = _state.value.copy(
            editing = true,
            resetDone = false,
            editSection = first.section,
            showSummary = false,
            error = null,
            current = first,
            input = AnamnesisEngine.answerText(profile, first.id),
            selected = AnamnesisEngine.answerSelection(profile, first.id)
        )
        refreshDerived()
    }

    /** Apre direttamente una sezione specifica per la modifica. */
    fun editSection(section: String) {
        val profile = _state.value.profile
        val first = AnamnesisEngine.questionsOf(section).firstOrNull() ?: return
        _state.value = _state.value.copy(
            editing = true,
            resetDone = false,
            editSection = section,
            showSummary = false,
            error = null,
            current = first,
            input = AnamnesisEngine.answerText(profile, first.id),
            selected = AnamnesisEngine.answerSelection(profile, first.id)
        )
        refreshDerived()
    }

    /** Esce dalla modalita modifica senza rigenerare (le modifiche gia salvate restano). */
    fun stopEdit() {
        _state.value = _state.value.copy(
            editing = false,
            editSection = null,
            showSummary = false,
            error = null
        )
        refreshDerived()
    }

    /**
     * Azzera completamente l'anamnesi per ricompilarla da zero. Non tocca account,
     * chiavi API ne storico allenamenti: azzera solo i dati dell'anamnesi.
     */
    fun resetAnamnesis() {
        val profile = _state.value.profile
        val reset = AnamnesisEngine.resetAnamnesis(profile)
        viewModelScope.launch {
            userRepository.save(reset)
            val first = AnamnesisEngine.questions.firstOrNull()
            _state.value = _state.value.copy(
                profile = reset,
                confirmed = false,
                editing = false,
                resetDone = true,
                editSection = null,
                showSummary = false,
                summary = "",
                current = first,
                input = "",
                selected = emptySet(),
                error = null,
                generationMessage = "Anamnesi azzerata. Ricompila da zero: account, chiavi API e storico allenamenti sono stati conservati.",
                generationWarnings = emptyList()
            )
            refreshDerived()
        }
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
        if (q.required && raw.isBlank() && !_state.value.editing) {
            _state.value = _state.value.copy(error = "Rispondi alla domanda per proseguire.")
            return
        }
        var profile = AnamnesisEngine.applyAnswer(_state.value.profile, q.id, raw)
        // Registra la provenienza: senza questo passaggio un default del profilo
        // (obiettivo RECOMP, lavoro SEDENTARY, 5 pasti) sarebbe indistinguibile
        // da una risposta e finirebbe nel contesto dell'AI come dato reale.
        if (raw.isNotBlank()) {
            profile = profile.copy(answeredQuestionIds = profile.answeredQuestionIds + q.id)
        }
        profile = profile.copy(updatedAt = System.currentTimeMillis())
        val next = if (_state.value.editing) {
            AnamnesisEngine.nextQuestionFull(q.id)
        } else {
            AnamnesisEngine.nextQuestionAfter(profile, q.id)
        }
        _state.value = _state.value.copy(
            profile = profile, error = null,
            current = next,
            input = next?.let { AnamnesisEngine.answerText(profile, it.id) }.orEmpty(),
            selected = next?.let { AnamnesisEngine.answerSelection(profile, it.id) } ?: emptySet(),
            answeredCount = AnamnesisEngine.questions.count { AnamnesisEngine.isAnswered(profile, it.id) }
        )
        refreshDerived()
        persist(profile)
    }

    /** Salta una domanda facoltativa. */
    fun skip() {
        val q = _state.value.current ?: return
        if (q.required && !_state.value.editing) {
            _state.value = _state.value.copy(error = "Questa domanda è obbligatoria.")
            return
        }
        val profile = _state.value.profile
        val next = if (_state.value.editing) AnamnesisEngine.nextQuestionFull(q.id)
        else AnamnesisEngine.nextQuestionAfter(profile, q.id)
        _state.value = _state.value.copy(
            current = next,
            input = next?.let { AnamnesisEngine.answerText(profile, it.id) }.orEmpty(),
            selected = next?.let { AnamnesisEngine.answerSelection(profile, it.id) } ?: emptySet(),
            error = null
        )
        refreshDerived()
    }

    fun goBack() {
        val q = _state.value.current ?: return
        val prev = if (_state.value.editing) AnamnesisEngine.previousQuestion(q.id)
        else AnamnesisEngine.questions.take(
            AnamnesisEngine.questions.indexOfFirst { it.id == q.id }
        ).lastOrNull() ?: return
        prev ?: return
        _state.value = _state.value.copy(
            current = prev,
            input = AnamnesisEngine.answerText(_state.value.profile, prev.id),
            selected = AnamnesisEngine.answerSelection(_state.value.profile, prev.id),
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
            // La conferma SALVA soltanto. La generazione non parte da sola: prima
            // va scelto il provider (API key, base URL, modello) e l'utente deve
            // poter decidere quando lanciarla. Avviarla qui significava scoprire
            // un provider mancante solo dopo aver compilato tutta l'anamnesi, e
            // con il provider assente il turno restava appeso.
            _state.value = _state.value.copy(
                editing = false,
                editSection = null,
                resetDone = false,
                confirmed = true,
                showSummary = false,
                error = null,
                generatingPrograms = false,
                generationFinished = false,
                generationMessage = "Anamnesi confermata e salvata. Genera ora il piano alimentare e " +
                    "la scheda dal Coach AI, oppure fallo più tardi: i dati restano salvati."
            )
        }
    }

    /** Permette di riprovare quando il provider era offline o ha restituito JSON invalido. */
    fun retryGeneration() {
        val profile = _state.value.profile
        if (!profile.isAnamnesisReady || _state.value.generatingPrograms) return
        viewModelScope.launch { runAiGeneration(profile) }
    }

    /** Generazione esplicita richiesta dall'utente dopo la conferma dell'anamnesi. */
    fun generateNow() {
        val profile = _state.value.profile
        if (!profile.isAnamnesisReady || _state.value.generatingPrograms) return
        viewModelScope.launch {
            // Il provider va verificato prima del turno: senza chiave la
            // generazione non puo riuscire, e mostrare un'attesa infinita al
            // posto dell'errore e' il difetto da evitare.
            if (!aiProgramGeneration.isProviderReady()) {
                _state.value = _state.value.copy(
                    generatingPrograms = false,
                    generationFinished = false,
                    error = "Nessun provider AI configurato. Apri Impostazioni > Provider AI, " +
                        "inserisci la chiave e salva: poi torna qui e genera il percorso.",
                    generationMessage = "Provider AI non configurato."
                )
                return@launch
            }
            runAiGeneration(profile)
        }
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
        // La generazione tocca rete, database e motori matematici: un errore qui
        // non deve chiudere l'app. Viene registrato e mostrato come messaggio.
        val result = try {
            // Fuori dal main thread: la generazione costruisce prompt enormi e
            // risolve le grammature, bloccando la UI porterebbe all'ANR.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                aiProgramGeneration.generate(profile)
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            CrashLog.record(context, t, "Generazione percorso dopo anamnesi")
            AiProgramGenerationService.Result.Failure(
                "Generazione non riuscita: ${t.message ?: t::class.simpleName}. " +
                    "Dettagli in Impostazioni > Diagnostica."
            )
        }
        when (result) {
            is AiProgramGenerationService.Result.Success -> _state.value = _state.value.copy(
                generatingPrograms = false,
                generationFinished = true,
                generationMessage = "Percorso generato dal Coach AI e validato: piano alimentare di 7 giorni e scheda salvati.",
                generationWarnings = result.warnings
            )
            // L'AI e l'unica autorita decisionale: se non e disponibile NON si
            // sostituisce con un motore deterministico. Si spiega l'errore.
            is AiProgramGenerationService.Result.Failure -> _state.value = _state.value.copy(
                generatingPrograms = false,
                generationMessage = "Il Coach AI non ha potuto generare il percorso.",
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
}
