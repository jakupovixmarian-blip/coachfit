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
        const val ACTION_MOVE_WORKOUT = "MOVE_WORKOUT"
        const val ACTION_ADJUST_CALORIES = "ADJUST_CALORIES"
        const val ACTION_ADJUST_VOLUME = "ADJUST_VOLUME"
        const val ACTION_ADJUST_FREQUENCY = "ADJUST_FREQUENCY"
        const val ACTION_DELOAD = "DELOAD"
        const val ACTION_NONE = "NONE"

        val VALID = setOf(
            ACTION_SUBSTITUTE_FOOD, ACTION_SUBSTITUTE_EXERCISE, ACTION_MOVE_WORKOUT,
            ACTION_ADJUST_CALORIES, ACTION_ADJUST_VOLUME, ACTION_ADJUST_FREQUENCY, ACTION_DELOAD
        )
    }
}

/** Risultato dell'elaborazione di un messaggio del coach. */
data class CoachReply(
    val text: String,
    val proposedAction: CoachAction? = null,
    val providerError: String? = null
)
