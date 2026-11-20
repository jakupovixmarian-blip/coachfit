package com.fitcoach.app.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.remote.AiApi
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
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
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
 * Verifica end-to-end del percorso UNIFICATO: la generazione iniziale del programma
 * passa dallo stesso motore agentico della chat (CoachOrchestrator + strumento
 * submit_full_program). Usa un database Room reale e un provider AI "scriptato":
 * il modello propone la struttura, lo strumento la verifica con i calcoli
 * deterministici e la salva solo se valida. Non esiste un secondo motore.
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
        chatRepository.saveAiSettings(
            AiProviderSettings(useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1", model = "test-model")
        )
    }

    @After
    fun tearDown() = db.close()

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

    private suspend fun proposal(scope: String? = null): String {
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        val built = SimulatedAiPrograms.proposal(
            profile, foods, exercises,
            WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
        )
        return SimulatedAiPrograms.json(json, built, scope)
    }

    @Test
    fun `generazione iniziale passa dal motore AI unico e persiste piano e scheda`() = runTest {
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit_full_program", proposal()),
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
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        val valid = SimulatedAiPrograms.proposal(
            profile, foods, exercises, WorkoutPlanBuilder.defaultTrainingDays(3)
        )
        val kept = valid.workout.days.take(2)
        val invalid = valid.copy(
            workout = valid.workout.copy(days = kept),
            calendar = valid.calendar.copy(
                workoutDayIndices = kept.map { it.dayIndex },
                restDayIndices = (0..6).filterNot { it in kept.map { d -> d.dayIndex } }
            )
        )
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit_full_program", SimulatedAiPrograms.json(json, invalid)),
                textResponse("Devo correggere il programma.")
            )
        )
        val result = service(api).generate(profile)

        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Failure::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNull()
        assertThat(workoutRepository.getActivePlan()).isNull()
    }

    @Test
    fun `grammature sbagliate dall'AI vengono ricalcolate e il programma viene salvato`() = runTest {
        // Riproduce la prova reale su OpenRouter: il modello sceglie alimenti
        // sensati ma dimezza i grammi e calcola i macro a mente. Prima il piano
        // veniva respinto dal validatore; ora il codice ricalcola i grammi.
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        val correct = SimulatedAiPrograms.proposal(
            profile, foods, exercises, WorkoutPlanBuilder.defaultTrainingDays(3)
        )
        val halved = correct.copy(
            nutrition = correct.nutrition.copy(
                days = correct.nutrition.days.map { day ->
                    day.copy(
                        meals = day.meals.map { meal ->
                            meal.copy(
                                entries = meal.entries.map { it.copy(grams = it.grams / 2.0) }
                            )
                        }
                    )
                }
            )
        )
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit_full_program", SimulatedAiPrograms.json(json, halved)),
                textResponse("Programma generato.")
            )
        )

        val result = service(api).generate(profile)

        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Success::class.java)
        val saved = nutritionRepository.getActivePlan()
        assertThat(saved).isNotNull()

        // Le grammature salvate non sono quelle dimezzate: sono state ricalcolate.
        val savedGrams = saved!!.days.flatMap { it.meals }.flatMap { it.entries }.map { it.grams }
        val submittedGrams = halved.nutrition.days.flatMap { it.meals }.flatMap { it.entries }.map { it.grams }
        assertThat(savedGrams).isNotEqualTo(submittedGrams)
        assertThat(savedGrams.all { it > 0.0 }).isTrue()
    }

    @Test
    fun `ambito nutrizione rigenera solo il piano alimentare`() = runTest {
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("submit_full_program", proposal(scope = "NUTRITION")),
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
                toolCallResponse("submit_full_program", proposal(scope = "WORKOUT")),
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
    fun `il coordinatore non genera nulla se il provider AI non risponde`() = runTest {
        // Provider non raggiungibile: nessuna sostituzione automatica, nessun piano.
        val coordinator = ProgramGenerationCoordinator(aiGeneration = service(ScriptedApi(mutableListOf())))

        val result = coordinator.generate(profile)

        assertThat(result).isInstanceOf(com.fitcoach.app.domain.usecase.ProgramGenerationResult.Error::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNull()
        assertThat(workoutRepository.getActivePlan()).isNull()
    }

    @Test
    fun `il coordinatore blocca la generazione senza anamnesi confermata`() = runTest {
        val coordinator = ProgramGenerationCoordinator(aiGeneration = service(ScriptedApi(mutableListOf())))
        val notConfirmed = profile.copy(anamnesisCompleted = false, anamnesisConfirmedAt = null)

        val result = coordinator.generate(notConfirmed)

        assertThat(result)
            .isInstanceOf(com.fitcoach.app.domain.usecase.ProgramGenerationResult.AnamnesisNotConfirmed::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNull()
        assertThat(workoutRepository.getActivePlan()).isNull()
    }

    @Test
    fun `il coordinatore blocca la generazione senza provider configurato`() = runTest {
        // Nessun proxy e nessuna chiave: il turno non puo riuscire, quindi non
        // deve nemmeno partire. Prima la schermata restava su "Generazione..."
        // fino alla deadline.
        chatRepository.saveAiSettings(
            AiProviderSettings(provider = AiProvider.OPENAI, model = "test-model", useBackendProxy = false)
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        SecureKeyStore(context).clear(AiProvider.OPENAI.name)

        val coordinator = ProgramGenerationCoordinator(aiGeneration = service(ScriptedApi(mutableListOf())))
        val result = coordinator.generate(profile)

        assertThat(result)
            .isInstanceOf(com.fitcoach.app.domain.usecase.ProgramGenerationResult.Error::class.java)
        assertThat((result as com.fitcoach.app.domain.usecase.ProgramGenerationResult.Error).message)
            .contains("Nessun provider AI configurato")
        assertThat(nutritionRepository.getActivePlan()).isNull()
        assertThat(workoutRepository.getActivePlan()).isNull()
    }
}
