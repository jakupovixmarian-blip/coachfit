package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.ChangeDomain
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Azioni strutturate che il coach AI puo proporre. L'AI NON applica mai
 * direttamente le modifiche: propone, l'app valida e l'utente conferma.
 */
@Serializable
data class CoachAction(
    val action: String,
    val domain: String = "NUTRITION",
    val title: String = "",
    val situation: String = "",
    val problem: String = "",
    @SerialName("data_used") val dataUsed: String = "",
    @SerialName("proposed_change") val proposedChange: String = "",
    val rationale: String = "",
    @SerialName("expected_outcome") val expectedOutcome: String = "",
    val contraindications: String = "",
    val payload: Map<String, String> = emptyMap(),
    val revertPayload: Map<String, String> = emptyMap()
) {
    val changeDomain: ChangeDomain
        get() = runCatching { ChangeDomain.valueOf(domain.uppercase()) }.getOrDefault(ChangeDomain.NUTRITION)

    companion object {
        const val ACTION_SUBSTITUTE_FOOD = "SUBSTITUTE_FOOD"
        const val ACTION_SUBSTITUTE_EXERCISE = "SUBSTITUTE_EXERCISE"
        /**
         * Cambia solo la tecnica d'intensita' di un esercizio gia' in scheda,
         * lasciando esercizio, serie e carico invariati. Prima per applicare una
         * tecnica bisognava sostituire l'esercizio: la chat chiedeva di
         * aggiungere un drop set e l'unico strumento disponibile cambiava anche
         * l'esercizio, oppure non faceva nulla.
         */
        const val ACTION_SET_INTENSITY = "SET_INTENSITY_TECHNIQUE"
        const val ACTION_MOVE_WORKOUT = "MOVE_WORKOUT"
        const val ACTION_ADJUST_CALORIES = "ADJUST_CALORIES"
        const val ACTION_ADJUST_VOLUME = "ADJUST_VOLUME"
        const val ACTION_ADJUST_FREQUENCY = "ADJUST_FREQUENCY"
        const val ACTION_DELOAD = "DELOAD"

        /**
         * Rigenerazione strutturale di una o entrambe le parti del programma tramite
         * il motore unico (ProgramGenerationCoordinator + submit_full_program). Non e
         * una sostituzione puntuale: serve quando la richiesta dell'utente cambia la
         * struttura (nuova split, nuovi giorni, cambio strategia), non un singolo valore.
         */
        const val ACTION_REGENERATE_PLAN = "REGENERATE_PLAN"

        const val ACTION_NONE = "NONE"

        val VALID = setOf(
            ACTION_SUBSTITUTE_FOOD, ACTION_SUBSTITUTE_EXERCISE, ACTION_SET_INTENSITY,
            ACTION_MOVE_WORKOUT, ACTION_ADJUST_CALORIES, ACTION_ADJUST_VOLUME,
            ACTION_ADJUST_FREQUENCY, ACTION_DELOAD, ACTION_REGENERATE_PLAN
        )
    }
}

/** Risultato dell'elaborazione di un messaggio del coach. */
data class CoachReply(
    val text: String,
    val proposedAction: CoachAction? = null,
    val providerError: String? = null,
    /** Proposta gia registrata (PROPOSED) da uno strumento di scrittura, da confermare. */
    val pendingChange: com.fitcoach.app.domain.model.ProgramChange? = null,
    /**
     * Tutte le proposte registrate nel turno. Un turno puo applicare piu
     * strumenti di scrittura: senza questo elenco le proposte oltre la prima
     * restavano registrate ma non mostrate, quindi impossibili da confermare.
     */
    val pendingChanges: List<com.fitcoach.app.domain.model.ProgramChange> = emptyList(),
    /**
     * `finish_reason` dell'ultima risposta del provider e strumenti eseguiti nel
     * turno. Servono a distinguere una risposta troncata da una risposta che il
     * modello ha deciso di chiudere senza chiamare alcuno strumento.
     */
    val lastFinishReason: String? = null,
    val toolTrace: List<String> = emptyList()
)
