package com.fitcoach.app.collaudo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiConfigResolver
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.WeeklyCheckInService
import com.fitcoach.app.domain.coach.tools.AnalyzeWeeklyCheckInTool
import com.fitcoach.app.domain.coach.tools.AdjustCaloriesTool
import com.fitcoach.app.domain.coach.tools.AdjustVolumeTool
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ProposeDeloadTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import dagger.Lazy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Cablaggio condiviso del collaudo: costruisce i servizi REALI (check-in,
 * esecutore delle azioni, coordinatore) con lo STESSO provider AI scriptato,
 * senza mock dei motori. Serve a riprodurre fedelmente il comportamento in app.
 */
object CollaudoCheckInWiring {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun checkInService(
        db: FitCoachDatabase,
        userRepository: UserRepository,
        progressRepository: ProgressRepository,
        nutritionRepository: NutritionRepository,
        diaryRepository: NutritionDiaryRepository,
        workoutRepository: WorkoutRepository,
        chatRepository: ChatRepository,
        changeRepository: ChangeRepository,
        api: AiApi
    ): WeeklyCheckInService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executor = actionExecutor(
            db, changeRepository, userRepository, chatRepository,
            null, null, nutritionRepository, workoutRepository, null
        )
        val tools: Set<CoachTool> = setOf(
            AnalyzeWeeklyCheckInTool(
                userRepository, progressRepository, nutritionRepository, diaryRepository, workoutRepository
            ),
            AdjustCaloriesTool(executor),
            AdjustVolumeTool(executor),
            ProposeDeloadTool(executor)
        )
        val orchestrator = CoachOrchestrator(AiClient(api, json), CoachToolRegistry(tools), json)
        return WeeklyCheckInService(
            userRepository, progressRepository, nutritionRepository, diaryRepository,
            workoutRepository, chatRepository, changeRepository,
            AiConfigResolver(chatRepository, SecureKeyStore(context)), orchestrator
        )
    }

    fun actionExecutor(
        db: FitCoachDatabase,
        changeRepository: ChangeRepository,
        userRepository: UserRepository,
        chatRepository: ChatRepository,
        foodRepository: FoodRepository?,
        exerciseRepository: ExerciseRepository?,
        nutritionRepository: NutritionRepository,
        workoutRepository: WorkoutRepository,
        planVersionRepository: PlanVersionRepository?
    ): CoachActionExecutor {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coordinator = Lazy {
            ProgramGenerationCoordinator(
                aiGeneration = AiProgramGenerationService(
                    CoachOrchestrator(
                        AiClient(UnconfiguredApi, json),
                        CoachToolRegistry(emptySet()),
                        json
                    ),
                    chatRepository,
                    SecureKeyStore(context),
                    json,
                    foodRepository ?: FoodRepository(db.foodDao()),
                    exerciseRepository ?: ExerciseRepository(db.exerciseDao())
                )
            )
        }
        return CoachActionExecutor(
            db = db,
            changeRepository = changeRepository,
            userRepository = userRepository,
            generationCoordinator = coordinator,
            json = json
        )
    }

    fun calorieArgs(planId: Long, weeklyKcal: Double) = buildJsonObject {
        put("planId", JsonPrimitive(planId))
        put("weeklyKcal", JsonPrimitive(weeklyKcal))
        put("title", JsonPrimitive("Aggiustamento calorico dal check-in"))
        put("situation", JsonPrimitive("Peso stabile su 4 settimane."))
        put("problem", JsonPrimitive("L'obiettivo richiede una variazione."))
        put("dataUsed", JsonPrimitive("Trend peso e aderenza delle ultime settimane."))
        put("rationale", JsonPrimitive("Variazione contenuta entro il 10%."))
        put("expectedOutcome", JsonPrimitive("Avvicinarsi al trend obiettivo."))
        put("contraindications", JsonPrimitive("Non applicare con aderenza bassa."))
    }.toString()

    private object UnconfiguredApi : AiApi {
        override suspend fun chatCompletions(
            url: String, authorization: String?, contentType: String,
            referer: String?, title: String?, body: com.fitcoach.app.data.remote.ChatCompletionRequest
        ): retrofit2.Response<com.fitcoach.app.data.remote.ChatCompletionResponse> =
            retrofit2.Response.error(
                503,
                "".toResponseBody("text/plain".toMediaType())
            )
    }
}
