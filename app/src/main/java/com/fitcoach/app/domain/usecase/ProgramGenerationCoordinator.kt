package com.fitcoach.app.domain.usecase

import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.UserProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    /**
     * La generazione costruisce prompt molto grandi (catalogo alimenti ed
     * esercizi), serializza payload di migliaia di token e risolve le
     * grammature in un ciclo iterativo. Eseguirla sul dispatcher del chiamante
     * (il main thread dei ViewModel) blocca la UI e porta all'ANR, che Android
     * presenta all'utente come chiusura dell'app. Il lavoro viene quindi
     * spostato su IO; i ViewModel possono chiamare da qualsiasi dispatcher.
     */
    suspend fun generate(
        profile: UserProfile,
        scope: Scope = Scope.BOTH,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): ProgramGenerationResult = try {
        withContext(dispatcher) { generateBlocking(profile, scope) }
    } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
        // Rete di sicurezza unica per ogni chiamante (schermate, chat, azioni
        // del Coach): un guasto imprevisto diventa un esito leggibile invece di
        // un'eccezione che chiude il processo.
        ProgramGenerationResult.Error(
            "Generazione non riuscita: ${t.message ?: t::class.simpleName}."
        )
    }

    private suspend fun generateBlocking(
        profile: UserProfile,
        scope: Scope
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
