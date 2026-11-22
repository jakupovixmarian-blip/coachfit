package com.fitcoach.app.support

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import kotlinx.serialization.json.Json

/**
 * Genera un programma attraverso l'UNICO percorso reale di produzione: il Coach AI
 * (qui simulato con [SimulatedAiPrograms]) consegna la struttura con lo strumento
 * `submit_full_program`, che la valida con i motori matematici e la persiste.
 *
 * Serve ai test di integrazione/collaudo per riprodurre fedelmente cio che accade
 * in app senza un provider LLM: nessun motore deterministico nascosto.
 */
object TestProgramGenerator {

    suspend fun generate(
        json: Json,
        userRepository: UserRepository,
        foodRepository: FoodRepository,
        exerciseRepository: ExerciseRepository,
        nutritionRepository: NutritionRepository,
        workoutRepository: WorkoutRepository,
        planVersionRepository: PlanVersionRepository,
        chatRepository: ChatRepository,
        db: FitCoachDatabase,
        profile: UserProfile,
        scope: ProgramGenerationEngine.Scope = ProgramGenerationEngine.Scope.BOTH,
        trainingDays: List<Int> = WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
    ): AiProgramGenerationService.Result {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        val proposal = SimulatedAiPrograms.proposal(profile, foods, exercises, trainingDays)
        // Il fixture deve imitare il modello REALE: la generazione esige che la
        // metodologia sia consultata prima della consegna (Prerequisite.consultations),
        // quindi le due consultazioni fanno parte dello script, non sono un extra.
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("consult_invictus", """{"area":"allenamento"}"""),
                toolCallResponse("consult_invictus", """{"area":"nutrizione"}"""),
                toolCallResponse("consult_invictus", """{"area":"tecniche"}"""),
                toolCallResponse("submit_full_program", SimulatedAiPrograms.json(json, proposal, scope.name)),
                textResponse("Programma generato e verificato.")
            )
        )
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val service = AiProgramGenerationService(
            CoachOrchestrator(
                AiClient(api, json),
                CoachToolRegistry(setOf(submitTool, ConsultInvictusTool())),
                json
            ),
            chatRepository, SecureKeyStore(context), json, foodRepository, exerciseRepository
        )
        return service.generate(profile, scope)
    }
}
