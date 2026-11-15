package com.fitcoach.app.domain.usecase

/**
 * Esito della creazione/rigenerazione dei programmi.
 *
 * Non esiste piu un percorso di generazione deterministico: l'unica autorita
 * decisionale e il Coach AI (vedi [com.fitcoach.app.domain.coach.AiProgramGenerationService]).
 * Questo esito descrive solo l'esito del turno agentico.
 */
sealed interface ProgramGenerationResult {
    data class Success(
        val nutritionPlanId: Long,
        val workoutPlanId: Long,
        val warnings: List<String>
    ) : ProgramGenerationResult

    data class MissingData(val missing: List<String>) : ProgramGenerationResult

    /** Anamnesi non completata/confermata: la generazione e bloccata. */
    data class AnamnesisNotConfirmed(
        val missing: List<String>,
        val contradictions: List<String>
    ) : ProgramGenerationResult

    data class Error(val message: String) : ProgramGenerationResult
}
