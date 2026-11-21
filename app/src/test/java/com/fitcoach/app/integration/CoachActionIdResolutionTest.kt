package com.fitcoach.app.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ApplyResult
import com.fitcoach.app.data.repository.ChangeRepository
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.data.repository.CoachActionExecutor
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.AdjustCaloriesTool
import com.fitcoach.app.domain.coach.tools.AdjustVolumeTool
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.ProposeDeloadTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.TestProgramGenerator
import com.google.common.truth.Truth.assertThat
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regressione: l'AI puo citare un id di piano inesistente (o non citarne alcuno)
 * mentre il piano attivo esiste davvero. In quel caso "Approva e applica" non deve
 * fallire con "Piano non trovato": la proposta va applicata al piano attivo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoachActionIdResolutionTest {

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
    private lateinit var progressRepository: ProgressRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var changeRepository: ChangeRepository
    private lateinit var executor: CoachActionExecutor

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
        progressRepository = ProgressRepository(db.progressDao(), db.checkInDao())
        chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())
        changeRepository = ChangeRepository(db.programChangeDao())

        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        userRepository.save(profile)
        chatRepository.saveAiSettings(
            AiProviderSettings(useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1", model = "test-model")
        )
        executor = CoachActionExecutor(
            db = db,
            changeRepository = changeRepository,
            userRepository = userRepository,
            generationCoordinator = Lazy { coordinator() },
            json = json
        )
        seedProgram()
    }

    @After
    fun tearDown() = db.close()

    private fun coordinator(): ProgramGenerationCoordinator {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val orchestrator = CoachOrchestrator(
            AiClient(ScriptedApi(mutableListOf()), json),
            CoachToolRegistry(setOf(submitTool, ConsultInvictusTool())), json
        )
        val aiGeneration = com.fitcoach.app.domain.coach.AiProgramGenerationService(
            orchestrator, chatRepository, SecureKeyStore(context), json, foodRepository, exerciseRepository
        )
        return ProgramGenerationCoordinator(aiGeneration = aiGeneration)
    }

    private suspend fun seedProgram() {
        val result = TestProgramGenerator.generate(
            json = json,
            userRepository = userRepository,
            foodRepository = foodRepository,
            exerciseRepository = exerciseRepository,
            nutritionRepository = nutritionRepository,
            workoutRepository = workoutRepository,
            planVersionRepository = planVersionRepository,
            chatRepository = chatRepository,
            db = db,
            profile = profile
        )
        assertThat(result).isInstanceOf(
            com.fitcoach.app.domain.coach.AiProgramGenerationService.Result.Success::class.java
        )
    }

    private fun action(
        type: String,
        payload: Map<String, String>,
        domain: String = "NUTRITION"
    ) = CoachAction(
        action = type,
        domain = domain,
        title = "Proposta di test",
        situation = "situazione",
        problem = "problema",
        dataUsed = "dati",
        proposedChange = "modifica",
        rationale = "motivo",
        expectedOutcome = "esito",
        contraindications = "nessuna",
        payload = payload
    )

    @Test
    fun `calorie con planId inesistente vengono applicate al piano attivo`() = runTest {
        val active = db.nutritionPlanDao().getActive()!!
        val before = active.weeklyCalories
        val changeId = executor.registerProposal(
            action(
                CoachAction.ACTION_ADJUST_CALORIES,
                mapOf("planId" to "999999", "weeklyKcal" to (before * 1.05).toString())
            )
        )

        val result = executor.apply(changeId)

        assertThat(result).isInstanceOf(ApplyResult.Applied::class.java)
        assertThat(db.nutritionPlanDao().getById(active.id)!!.weeklyCalories)
            .isWithin(0.01).of(before * 1.05)
    }

    @Test
    fun `calorie senza planId vengono applicate al piano attivo`() = runTest {
        val active = db.nutritionPlanDao().getActive()!!
        val before = active.weeklyCalories
        val changeId = executor.registerProposal(
            action(
                CoachAction.ACTION_ADJUST_CALORIES,
                mapOf("weeklyKcal" to (before * 0.97).toString())
            )
        )

        val result = executor.apply(changeId)

        assertThat(result).isInstanceOf(ApplyResult.Applied::class.java)
        assertThat(db.nutritionPlanDao().getById(active.id)!!.weeklyCalories)
            .isWithin(0.01).of(before * 0.97)
    }

    @Test
    fun `un planId valido resta quello usato`() = runTest {
        val active = db.nutritionPlanDao().getActive()!!
        val changeId = executor.registerProposal(
            action(
                CoachAction.ACTION_ADJUST_CALORIES,
                mapOf("planId" to active.id.toString(), "weeklyKcal" to (active.weeklyCalories * 1.02).toString())
            )
        )

        assertThat(executor.apply(changeId)).isInstanceOf(ApplyResult.Applied::class.java)
    }

    @Test
    fun `la modifica applicata e annullabile`() = runTest {
        val active = db.nutritionPlanDao().getActive()!!
        val before = active.weeklyCalories
        val changeId = executor.registerProposal(
            action(
                CoachAction.ACTION_ADJUST_CALORIES,
                mapOf("planId" to "123456", "weeklyKcal" to (before * 1.05).toString())
            )
        )
        assertThat(executor.apply(changeId)).isInstanceOf(ApplyResult.Applied::class.java)

        val revert = executor.revert(changeId)

        assertThat(revert).isInstanceOf(ApplyResult.Reverted::class.java)
        assertThat(db.nutritionPlanDao().getById(active.id)!!.weeklyCalories)
            .isWithin(0.01).of(before)
        assertThat(changeRepository.getById(changeId)!!.status).isEqualTo(ChangeStatus.REVERTED)
    }

    @Test
    fun `volume con mesocycleId inesistente usa il mesociclo attivo`() = runTest {
        val workout = db.workoutPlanDao().getActive()!!
        val meso = db.mesocycleDao().getForPlan(workout.id).first()
        val setsBefore = db.workoutDayDao().getForMesocycle(meso.id)
            .flatMap { db.plannedExerciseDao().getForDay(it.id) }
            .associate { it.id to it.sets }
        assertThat(setsBefore).isNotEmpty()

        val changeId = executor.registerProposal(
            action(
                CoachAction.ACTION_ADJUST_VOLUME,
                mapOf("mesocycleId" to "424242", "volumeReductionPercent" to "20"),
                domain = "WORKOUT"
            )
        )

        val result = executor.apply(changeId)

        assertThat(result).isInstanceOf(ApplyResult.Applied::class.java)
        val setsAfter = db.workoutDayDao().getForMesocycle(meso.id)
            .flatMap { db.plannedExerciseDao().getForDay(it.id) }
            .associate { it.id to it.sets }
        assertThat(setsAfter.values.sum()).isLessThan(setsBefore.values.sum())
    }

    @Test
    fun `una proposta su un piano non piu attivo viene applicata a quello attivo`() = runTest {
        val active = db.nutritionPlanDao().getActive()!!
        // Versione precedente del piano: esiste nel database ma non e' quella seguita.
        val oldId = db.nutritionPlanDao().insert(
            active.copy(id = 0, isActive = false, version = active.version - 1)
        )
        val activeBefore = active.weeklyCalories
        val oldBefore = db.nutritionPlanDao().getById(oldId)!!.weeklyCalories

        val changeId = executor.registerProposal(
            action(
                CoachAction.ACTION_ADJUST_CALORIES,
                mapOf("planId" to oldId.toString(), "weeklyKcal" to (oldBefore * 1.04).toString())
            )
        )

        assertThat(executor.apply(changeId)).isInstanceOf(ApplyResult.Applied::class.java)
        assertThat(db.nutritionPlanDao().getById(active.id)!!.weeklyCalories)
            .isWithin(0.01).of(oldBefore * 1.04)
        assertThat(db.nutritionPlanDao().getById(active.id)!!.weeklyCalories)
            .isNotWithin(0.01).of(activeBefore)
        assertThat(db.nutritionPlanDao().getById(oldId)!!.weeklyCalories)
            .isWithin(0.01).of(oldBefore)
    }

    @Test
    fun `un piano alimentare davvero assente produce un messaggio comprensibile`() = runTest {
        db.nutritionPlanDao().deactivateAll()
        val changeId = executor.registerProposal(
            action(
                CoachAction.ACTION_ADJUST_CALORIES,
                mapOf("planId" to "1", "weeklyKcal" to "2000")
            )
        )

        val result = executor.apply(changeId)

        assertThat(result).isInstanceOf(ApplyResult.Failed::class.java)
        val message = (result as ApplyResult.Failed).message
        assertThat(message).contains("Nessun piano alimentare attivo")
    }
}
