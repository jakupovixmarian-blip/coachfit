package com.fitcoach.app.ui.screens.coach

/**
 * Stato di presentazione del Coach AI.
 *
 * Il Coach ha identità visiva separata dal resto dell'app (accento AiAccent),
 * ma NON è una finestra su un LLM: riceve già il contesto del programma dal
 * CoachOrchestrator. Qui non si decide nulla e non si scrive sul database:
 * approvare o rifiutare una proposta chiama la funzione di conferma/rollback
 * già esistente.
 *
 * Mapping entità -> campo:
 *   messages        <- ChatMessage (role, content, timestamp)
 *   status          <- stato del provider configurato (AiProviderSettings)
 *   history         <- conversazioni precedenti
 *   proposal        <- ProgramChange PROPOSED con snapshot degli id
 *   comparison      <- confronto piano precedente / nuovo piano
 */
data class CoachUiState(
    val messages: List<CoachMessageUi> = emptyList(),
    val status: CoachStatus = CoachStatus.READY,
    val providerLabel: String = "",
    val modelLabel: String = "",
    val inputText: String = "",
    val isSending: Boolean = false,
    val history: List<CoachSessionUi> = emptyList(),
    val errorMessage: String? = null,
) {
    val canSend: Boolean get() = inputText.isNotBlank() && !isSending && status != CoachStatus.NO_PROVIDER
    val hasMessages: Boolean get() = messages.isNotEmpty()
    val pendingProposal: CoachMessageUi?
        get() = messages.lastOrNull { it.proposal != null && it.proposal.status == CoachProposalStatus.PROPOSED }
}

/** Stato del motore AI, mostrato come indicatore nella testata. */
enum class CoachStatus { READY, THINKING, OFFLINE, NO_PROVIDER }

enum class CoachRole { USER, COACH, SYSTEM }

/** Messaggio della conversazione. `proposal` non null = scheda di proposta. */
data class CoachMessageUi(
    val id: Long,
    val role: CoachRole,
    val text: String,
    val timeLabel: String,
    val proposal: CoachProposalUi? = null,
    val isError: Boolean = false,
)

/**
 * Proposta di modifica del programma.
 * Deve restare visivamente distinta da un normale messaggio e contenere
 * tutti i campi richiesti: situazione, problema, dati usati, modifica,
 * motivazione, risultato atteso, controindicazioni.
 */
data class CoachProposalUi(
    val id: Long,
    val title: String,
    val situation: String,
    val problem: String,
    val dataUsed: String,
    val change: String,
    val rationale: String,
    val expected: String,
    val cautions: String?,
    val comparison: List<ProgramDiffUi> = emptyList(),
    val status: CoachProposalStatus = CoachProposalStatus.PROPOSED,
    val isApplying: Boolean = false,
)

enum class CoachProposalStatus { PROPOSED, APPROVED, REJECTED }

/** Riga del confronto programma precedente / nuovo. */
data class ProgramDiffUi(
    val label: String,
    val before: String,
    val after: String,
)

/** Voce della cronologia conversazioni. */
data class CoachSessionUi(
    val id: Long,
    val title: String,
    val timeLabel: String,
    val isActive: Boolean = false,
)
