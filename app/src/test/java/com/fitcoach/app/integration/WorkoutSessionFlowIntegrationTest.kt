package com.fitcoach.app.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
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
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutSet
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.ProgressiveOverloadEngine
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.fitcoach.app.support.GenerationScript
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.SimulatedAiPrograms
import com.fitcoach.app.support.textResponse
import com.fitcoach.app.support.toolCallResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica funzionale del ciclo di allenamento su database Room reale:
 * generazione scheda (dal Coach AI) -> dettaglio seduta (esercizi, serie, rep,
 * recupero, RIR) -> registrazione serie (carico/rep/RIR) -> completamento ->
 * persistenza e aggregazione delle prestazioni -> progressione dei carichi.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorkoutSessionFlowIntegrationTest {

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
        chatRepository.saveAiSettings(
            AiProviderSettings(useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1", model = "test-model")
        )
    }

    @After
    fun tearDown() = db.close()

    /** Genera la scheda attraverso l'UNICO motore (Coach AI + submit_full_program). */
    private suspend fun generateWorkout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        val proposal = SimulatedAiPrograms.proposal(
            profile, foods, exercises,
            WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
        )
        val api = ScriptedApi(
            GenerationScript.consultationsThen(
                toolCallResponse("submit_full_program", SimulatedAiPrograms.json(json, proposal, "WORKOUT")),
                textResponse("Scheda generata.")
            )
        )
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val service = AiProgramGenerationService(
            CoachOrchestrator(AiClient(api, json), CoachToolRegistry(setOf(submitTool, ConsultInvictusTool())), json),
            chatRepository, SecureKeyStore(context), json, foodRepository, exerciseRepository
        )
        val result = service.generate(profile, ProgramGenerationEngine.Scope.WORKOUT)
        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Success::class.java)
    }

    @Test
    fun `la scheda generata espone esercizi con serie rep recupero e RIR`() = runTest {
        generateWorkout()
        val plan = workoutRepository.getActivePlan()
        assertThat(plan).isNotNull()
        val trainingDays = plan!!.days.filterNot { it.isRestDay }
        assertThat(trainingDays).hasSize(3)
        trainingDays.forEach { day ->
            assertThat(day.exercises).isNotEmpty()
            day.exercises.forEach { e ->
                assertThat(e.sets).isAtLeast(1)
                assertThat(e.minReps).isAtLeast(1)
                assertThat(e.maxReps).isAtLeast(e.minReps)
                assertThat(e.restSeconds).isAtLeast(15)
                assertThat(e.targetRir).isAtLeast(0)
                assertThat(e.exerciseName).isNotEmpty()
            }
        }
    }

    @Test
    fun `registro serie completa la sessione e aggrega le prestazioni`() = runTest {
        generateWorkout()
        val day = workoutRepository.getActivePlan()!!.days.first { !it.isRestDay }
        val epochDay = 20_000L

        val sessionId = workoutRepository.startSession(day, epochDay)
        assertThat(sessionId).isGreaterThan(0L)
        val exercise = day.exercises.first()

        assertThat(workoutRepository.saveSet(sessionId, WorkoutSet(
            exerciseId = exercise.exerciseId, exerciseName = exercise.exerciseName,
            setIndex = 0, plannedReps = exercise.maxReps, actualReps = 8, weightKg = 60.0, rir = 2
        ))).isGreaterThan(0L)
        assertThat(workoutRepository.saveSet(sessionId, WorkoutSet(
            exerciseId = exercise.exerciseId, exerciseName = exercise.exerciseName,
            setIndex = 1, plannedReps = exercise.maxReps, actualReps = 9, weightKg = 62.5, rir = 1
        ))).isGreaterThan(0L)

        // Una serie con dati non validi NON viene registrata.
        assertThat(workoutRepository.saveSet(sessionId, WorkoutSet(
            exerciseId = exercise.exerciseId, exerciseName = exercise.exerciseName,
            setIndex = 2, plannedReps = exercise.maxReps, actualReps = 0, weightKg = 70.0
        ))).isEqualTo(0L)

        assertThat(workoutRepository.completeSession(sessionId, 55, 8.0, 4, false, "")).isTrue()

        val reloaded = workoutRepository.getSessionForDay(epochDay)
        assertThat(reloaded?.status?.name).isEqualTo("COMPLETED")
        assertThat(reloaded?.completedSets).isEqualTo(2)

        val perf = workoutRepository.getLatestPerformance(exercise.exerciseId)
        assertThat(perf).isNotNull()
        assertThat(perf!!.totalSets).isEqualTo(2)
        assertThat(perf.topWeightKg).isEqualTo(62.5)
        assertThat(perf.topReps).isEqualTo(9)
        assertThat(perf.volumeKg).isEqualTo(60.0 * 8 + 62.5 * 9)
        assertThat(perf.estimated1Rm).isNotNull()
    }

    @Test
    fun `la doppia progressione avanza le ripetizioni prima del carico`() = runTest {
        val min = 8
        val max = 10
        // 70 kg x 8 (RIR 2): mantieni il carico e prova una ripetizione in piu.
        val first = ProgressiveOverloadEngine.decide(
            lastWeightKg = 70.0,
            lastSets = listOf(set(reps = 8, weight = 70.0, rir = 2)),
            minReps = min, maxReps = max, category = com.fitcoach.app.domain.model.ExerciseCategory.COMPOUND
        )
        assertThat(first.recommendedWeightKg).isEqualTo(70.0)
        assertThat(first.recommendedReps).isEqualTo(9)

        // 70 kg x 10 in tutte le serie: ora si aumenta il carico.
        val progressed = ProgressiveOverloadEngine.decide(
            lastWeightKg = 70.0,
            lastSets = listOf(
                set(reps = 10, weight = 70.0, rir = 1),
                set(reps = 10, weight = 70.0, rir = 1)
            ),
            minReps = min, maxReps = max, category = com.fitcoach.app.domain.model.ExerciseCategory.COMPOUND
        )
        assertThat(progressed.increaseApplied).isTrue()
        assertThat(progressed.recommendedWeightKg).isGreaterThan(70.0)

        // Con dolore segnalato il carico NON aumenta.
        val pain = ProgressiveOverloadEngine.decide(
            lastWeightKg = 70.0,
            lastSets = listOf(set(reps = 10, weight = 70.0, rir = 1)),
            minReps = min, maxReps = max,
            category = com.fitcoach.app.domain.model.ExerciseCategory.COMPOUND,
            painReported = true
        )
        assertThat(pain.increaseApplied).isFalse()
    }

    private fun set(reps: Int, weight: Double, rir: Int?) = WorkoutSet(
        exerciseId = 1L, exerciseName = "Test", setIndex = 0,
        plannedReps = reps, actualReps = reps, weightKg = weight, rir = rir, completed = true
    )
}
