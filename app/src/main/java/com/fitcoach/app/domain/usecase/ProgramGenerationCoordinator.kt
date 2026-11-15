package com.fitcoach.app.domain.usecase

import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.UserProfile
import javax.inject.Inject

/**
 * Unico punto di ingresso per la creazione/rigenerazione dei programmi.
 *
 * Per impostazione predefinita la generazione passa dal motore AI (l'autorita
 * decisionale del coach): l'AI propone la struttura e i motori matematici la
 * verificano e la persistono. Il motore deterministico offline resta disponibile
 * solo come scelta esplicita dell'utente, quando non c'e connessione.
 */
class ProgramGenerationCoordinator @Inject constructor(
    private val aiGeneration: AiProgramGenerationService,
    private val offlineEngine: GenerateProgramsUseCase
) {
    enum class Scope { BOTH, NUTRITION, WORKOUT }

    suspend fun generate(
        profile: UserProfile,
        scope: Scope = Scope.BOTH,
        useOfflineEngine: Boolean = false
    ): GenerateProgramsUseCase.Result {
        if (!profile.isAnamnesisReady) {
            return GenerateProgramsUseCase.Result.AnamnesisNotConfirmed(
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
        if (missing.isNotEmpty()) return GenerateProgramsUseCase.Result.MissingData(missing)

        if (useOfflineEngine) {
            return offlineEngine(
                profile = profile,
                regenerateNutrition = scope != Scope.WORKOUT,
                regenerateWorkout = scope != Scope.NUTRITION,
                requireAnamnesis = true
            )
        }

        return when (val result = aiGeneration.generate(profile, scope.toEngineScope())) {
            is AiProgramGenerationService.Result.Success -> GenerateProgramsUseCase.Result.Success(
                nutritionPlanId = result.nutritionPlanId,
                workoutPlanId = result.workoutPlanId,
                warnings = result.warnings
            )
            is AiProgramGenerationService.Result.Failure ->
                GenerateProgramsUseCase.Result.Error(result.message)
        }
    }

    private fun Scope.toEngineScope(): ProgramGenerationEngine.Scope = when (this) {
        Scope.BOTH -> ProgramGenerationEngine.Scope.BOTH
        Scope.NUTRITION -> ProgramGenerationEngine.Scope.NUTRITION
        Scope.WORKOUT -> ProgramGenerationEngine.Scope.WORKOUT
    }
}
