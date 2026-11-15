package com.fitcoach.app.domain.usecase

import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.UserProfile
import javax.inject.Inject

/**
 * Unico punto di ingresso per la creazione/rigenerazione dei programmi.
 *
 * Non esiste un motore decisionale deterministico: la generazione passa sempre
 * dal Coach AI (l'unica autorita), che propone la struttura e la consegna con
 * submit_full_program. I calcoli matematici e i vincoli di sicurezza verificano
 * la proposta e possono bloccarla, ma non ne generano una alternativa.
 */
class ProgramGenerationCoordinator @Inject constructor(
    private val aiGeneration: AiProgramGenerationService
) {
    enum class Scope { BOTH, NUTRITION, WORKOUT }

    suspend fun generate(
        profile: UserProfile,
        scope: Scope = Scope.BOTH
    ): ProgramGenerationResult {
        if (!profile.isAnamnesisReady) {
            return ProgramGenerationResult.AnamnesisNotConfirmed(
                missing = profile.missingAnamnesisData,
                contradictions = profile.anamnesisContradictions
            )
        }
        val missing = buildList {
            if (profile.ageYears == null) add("eta")
            if (profile.heightCm == null) add("altezza")
            if (profile.weightKg == null) add("peso")
            if (profile.trainingDaysPerWeek <= 0) add("giorni di allenamento")
        }
        if (missing.isNotEmpty()) return ProgramGenerationResult.MissingData(missing)

        return when (val result = aiGeneration.generate(profile, scope.toEngineScope())) {
            is AiProgramGenerationService.Result.Success -> ProgramGenerationResult.Success(
                nutritionPlanId = result.nutritionPlanId,
                workoutPlanId = result.workoutPlanId,
                warnings = result.warnings
            )
            is AiProgramGenerationService.Result.Failure ->
                ProgramGenerationResult.Error(result.message)
        }
    }

    private fun Scope.toEngineScope(): ProgramGenerationEngine.Scope = when (this) {
        Scope.BOTH -> ProgramGenerationEngine.Scope.BOTH
        Scope.NUTRITION -> ProgramGenerationEngine.Scope.NUTRITION
        Scope.WORKOUT -> ProgramGenerationEngine.Scope.WORKOUT
    }
}
