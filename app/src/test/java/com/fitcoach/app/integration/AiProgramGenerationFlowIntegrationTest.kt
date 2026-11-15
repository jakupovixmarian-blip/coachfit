package com.fitcoach.app.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.ChatCompletionRequest
import com.fitcoach.app.data.remote.ChatCompletionResponse
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiFoodEntry
import com.fitcoach.app.domain.coach.AiCalendarProposal
import com.fitcoach.app.domain.coach.AiMeal
import com.fitcoach.app.domain.coach.AiNutritionDay
import com.fitcoach.app.domain.coach.AiNutritionProposal
import com.fitcoach.app.domain.coach.AiPlannedExercise
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.coach.AiProgramProposal
import com.fitcoach.app.domain.coach.AiWorkoutDay
import com.fitcoach.app.domain.coach.AiWorkoutProposal
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.MealPlanGenerator
import com.fitcoach.app.domain.usecase.GenerateProgramsUseCase
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/**
 * Verifica end-to-end del percorso UNIFICATO: la generazione iniziale del programma
 * passa dallo stesso motore agentico della chat (CoachOrchestrator + strumento
 * submit_full_program). Usa un database Room reale e un provider AI "scriptato":
 * il modello propone la struttura, lo strumento la verifica con i calcoli
 * deterministici e la salva solo se valida.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiProgramGenerationFlowIntegrationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var planVersionRepository: PlanVersionRepository
    private lateinit var chatRepository: ChatRepository

    private val profile = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.BEGINNER, goal = Goal.RECOMP,
        trainingDaysPerWeek = 3, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        exerciseRepository = ExerciseRepository(db.exerciseDao())
        nutritionRepository = NutritionRepository(db)
        workoutRepository = WorkoutRepository(db)
        planVersionRepository = PlanVersionRepository(db.planVersionDao())
        chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())

        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        userRepository.save(profile)
        // Backend proxy: nessuna chiave sul dispositivo, ma il provider e configurato.
        chatRepository.saveAiSettings(
            AiProviderSettings(useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1", model = "test-model")
        )
    }

    @After
    fun tearDown() = db.close()

    // ---- API AI scriptata: risponde con una tool call e poi con il testo finale. ----

    private class ScriptedApi(
        private val responses: MutableList<Response<ChatCompletionResponse>>
    ) : AiApi {
        val sent = mutableListOf<ChatCompletionRequest>()
        override suspend fun chatCompletions(
            url: String, authorization: String?, contentType: String,
            referer: String?, title: String?, body: ChatCompletionRequest
        ): Response<ChatCompletionResponse> {
            sent += body
            return responses.removeAt(0)
        }
    }

    private fun toolCallResponse(name: String, args: String) = Response.success(
        ChatCompletionResponse(
            choices = listOf(
                ChatCompletionResponse.Choice(
                    index = 0,
                    message = ChatCompletionResponse.Message(
                        content = null,
                        toolCalls = listOf(
                            ChatCompletionResponse.ToolCall(
                                id = "c1", type = "function",
                                function = ChatCompletionResponse.FunctionCall(name = name, arguments = args)
                            )
                        )
                    ),
                    finishReason = "tool_calls"
                )
            )
        )
    )

    private fun textResponse(text: String) = Response.success(
        ChatCompletionResponse(
            choices = listOf(
                ChatCompletionResponse.Choice(
                    index = 0,
                    message = ChatCompletionResponse.Message(content = JsonPrimitive(text)),
                    finishReason = "stop"
                )
            )
        )
    )

    private fun service(api: AiApi): AiProgramGenerationService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val registry = CoachToolRegistry(setOf(submitTool))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)
        return AiProgramGenerationService(
            orchestrator, chatRepository, SecureKeyStore(context), json, foodRepository, exerciseRepository
        )
    }

    /**
     * Costruisce una proposta conforme allo schema AI a partire dai motori
     * deterministici: simula una risposta valida del modello e serve a verificare
     * che il percorso unificato la accetti, la verifichi e la persista.
     */
    private suspend fun buildValidProposal(): AiProgramProposal {
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        val energy = EnergyCalculator.compute(profile)!!
        val applied = CarbCyclingEngine.shouldApplyCarbCycling(profile)
        val cycling = if (applied) CarbCyclingEngine.build(profile, energy)
        else CarbCyclingEngine.buildUniform(profile, energy)
        val training = WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
        val dayTypes = if (applied) {
            CarbCyclingEngine.assignDayTypes(cycling.highDays, cycling.mediumDays, cycling.lowDays, training)
        } else List(7) { CarbDayType.MEDIUM }
        val days = MealPlanGenerator.generate(profile, cycling, foods, dayTypes)
        val built = WorkoutPlanBuilder.build(profile, exercises, training)

        return AiProgramProposal(
            strategy = "Ricomposizione con carb cycling",
            nutrition = AiNutritionProposal(days = days.map { d ->
                AiNutritionDay(
                    dayIndex = d.dayIndex, dayType = d.dayType.name,
                    meals = d.meals.map { m ->
                        AiMeal(slot = m.slot.name, entries = m.entries.map { e ->
                            AiFoodEntry(foodName = e.foodName, grams = e.grams, unit = e.unit.name)
                        })
                    }
                )
            }),
            workout = AiWorkoutProposal(
                split = built.splitType.name, rationale = "Split scelto dai motori",
                days = built.days.filterNot { it.isRestDay }.map { d ->
                    AiWorkoutDay(
                        dayIndex = d.dayIndex, title = d.title,
                        muscleGroups = d.muscleGroups.map { it.name },
                        exercises = d.exercises.map { e ->
                            AiPlannedExercise(
                                exerciseName = e.exerciseName, sets = e.sets,
                                minReps = e.minReps, maxReps = e.maxReps,
                                restSeconds = e.restSeconds, targetRir = e.targetRir,
                                technique = e.intensityTechnique.name, techniqueReason = e.techniqueReason
                            )
                        }
                    )
                }
            ),
            calendar = AiCalendarProposal(
                workoutDayIndices = training, restDayIndices = (0..6).filterNot { it in training }
            )
        )
    }

    private fun proposalJson(p: AiProgramProposal, scope: String? = null): String {
        val base = json.encodeToString(AiProgramProposal.serializer(), p)
        if (scope == null) return base
        val obj = json.parseToJsonElement(base) as kotlinx.serialization.json.JsonObject
        return json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.JsonObject(obj + ("scope" to JsonPrimitive(scope)))
        )
    }

    @Test
    fun `generazione iniziale passa dal motore AI unico e persiste piano e scheda`() = runTest {
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit_full_program", proposalJson(buildValidProposal())),
                textResponse("Programma generato e salvato.")
            )
        )
        val result = service(api).generate(profile)

        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Success::class.java)
        val nutrition = nutritionRepository.getActivePlan()
        assertThat(nutrition).isNotNull()
        assertThat(nutrition!!.days).hasSize(7)
        val workout = workoutRepository.getActivePlan()
        assertThat(workout).isNotNull()
        assertThat(workout!!.days.count { !it.isRestDay }).isEqualTo(3)
        // Le versioni dei piani vengono fotografate: la modifica e tracciata.
        assertThat(planVersionRepository.getAll()).isNotEmpty()
    }

    @Test
    fun `proposta AI non valida viene bloccata dai controlli e non salva nulla`() = runTest {
        // Solo 2 giorni di allenamento invece dei 3 dichiarati: violazione bloccante.
        val invalid = buildValidProposal().let { p ->
            val kept = p.workout.days.take(2)
            p.copy(
                workout = p.workout.copy(days = kept),
                calendar = p.calendar.copy(
                    workoutDayIndices = kept.map { it.dayIndex },
                    restDayIndices = (0..6).filterNot { it in kept.map { d -> d.dayIndex } }
                )
            )
        }
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit_full_program", proposalJson(invalid)),
                textResponse("Devo correggere il programma.")
            )
        )
        val result = service(api).generate(profile)

        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Failure::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNull()
        assertThat(workoutRepository.getActivePlan()).isNull()
    }

    @Test
    fun `ambito nutrizione rigenera solo il piano alimentare`() = runTest {
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit_full_program", proposalJson(buildValidProposal(), scope = "NUTRITION")),
                textResponse("Piano alimentare aggiornato.")
            )
        )
        val result = service(api).generate(profile, ProgramGenerationEngine.Scope.NUTRITION)
        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Success::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNotNull()
        // Ambito NUTRIZIONE: la scheda non viene toccata.
        assertThat(workoutRepository.getActivePlan()).isNull()
    }

    @Test
    fun `ambito allenamento rigenera solo la scheda`() = runTest {
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit_full_program", proposalJson(buildValidProposal(), scope = "WORKOUT")),
                textResponse("Scheda aggiornata.")
            )
        )
        val result = service(api).generate(profile, ProgramGenerationEngine.Scope.WORKOUT)
        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Success::class.java)
        assertThat(workoutRepository.getActivePlan()).isNotNull()
        // Ambito ALLENAMENTO: il piano alimentare non viene toccato.
        assertThat(nutritionRepository.getActivePlan()).isNull()
    }

    @Test
    fun `il coordinatore usa il motore offline solo su scelta esplicita`() = runTest {
        val offlineEngine = GenerateProgramsUseCase(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository
        )
        // Provider non raggiungibile: il percorso predefinito fallisce senza sostituzioni automatiche.
        val coordinator = ProgramGenerationCoordinator(
            aiGeneration = service(ScriptedApi(mutableListOf())),
            offlineEngine = offlineEngine
        )

        val aiDefault = coordinator.generate(profile)
        assertThat(aiDefault).isInstanceOf(GenerateProgramsUseCase.Result.Error::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNull()
        assertThat(workoutRepository.getActivePlan()).isNull()

        // Su scelta esplicita il motore offline genera e persiste entrambi i programmi.
        val offline = coordinator.generate(profile, useOfflineEngine = true)
        assertThat(offline).isInstanceOf(GenerateProgramsUseCase.Result.Success::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNotNull()
        assertThat(workoutRepository.getActivePlan()).isNotNull()
    }
}
