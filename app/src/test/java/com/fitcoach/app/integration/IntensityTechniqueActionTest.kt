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
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.SetIntensityTechniqueTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.IntensityTechnique
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Percorso completo di una tecnica d'intensita' proposta in chat.
 *
 * La chat poteva dichiarare "ho aggiunto il drop set" senza che nulla venisse
 * registrato: non esisteva alcun strumento per le tecniche. Questo test verifica
 * che la proposta venga registrata, applicata sulla scheda reale e annullata.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IntensityTechniqueActionTest {

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
    private lateinit var changeRepository: ChangeRepository
    private lateinit var executor: CoachActionExecutor
    private lateinit var tool: SetIntensityTechniqueTool

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
        changeRepository = ChangeRepository(db.programChangeDao())

        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        userRepository.save(profile)
        chatRepository.saveAiSettings(
            AiProviderSettings(
                useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1",
                model = "test-model"
            )
        )
        executor = CoachActionExecutor(
            db = db,
            changeRepository = changeRepository,
            userRepository = userRepository,
            generationCoordinator = Lazy { coordinator() },
            json = json
        )
        tool = SetIntensityTechniqueTool(executor)
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
        val aiGeneration = AiProgramGenerationService(
            orchestrator, chatRepository, SecureKeyStore(context), json,
            foodRepository, exerciseRepository
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
        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Success::class.java)
    }

    private val plannedDao get() = db.plannedExerciseDao()

    /** Primo esercizio pianificato del piano attivo. */
    private suspend fun firstPlanned(): com.fitcoach.app.data.local.entity.PlannedExerciseEntity {
        val days = workoutRepository.getActiveDays()
        check(days.isNotEmpty()) { "nessun giorno attivo" }
        return plannedDao.getForDay(days.first().id).first()
    }

    /** Id della proposta in attesa piu recente. */
    private suspend fun pendingId(): Long =
        changeRepository.getAll().first { it.status == ChangeStatus.PROPOSED }.id

    private fun args(plannedId: Long, technique: String, reason: String = "Cedimento locale") =
        buildJsonObject {
            put("plannedId", plannedId)
            put("intensityTechnique", technique)
            put("techniqueReason", reason)
        }

    @Test
    fun `la proposta registrata non modifica la scheda finche non e confermata`() = runTest {
        val planned = firstPlanned()
        assertThat(planned.intensityTechnique).isEqualTo(IntensityTechnique.NONE)

        val output = tool.execute(args(planned.id, "DROP_SET"))
        assertThat(output).contains("PROPOSTA_REGISTRATA")
        assertThat(output).contains("NON e applicata")

        // Il DB non e' cambiato: la chat non scrive mai direttamente.
        assertThat(plannedDao.getById(planned.id)!!.intensityTechnique)
            .isEqualTo(IntensityTechnique.NONE)
    }

    @Test
    fun `la conferma applica la tecnica sulla scheda reale`() = runTest {
        val planned = firstPlanned()
        tool.execute(args(planned.id, "DROP_SET", "Ultima serie in drop per il petto"))

        val changeId = pendingId()
        val result = executor.apply(changeId)
        assertThat(result).isInstanceOf(ApplyResult.Applied::class.java)

        val updated = plannedDao.getById(planned.id)!!
        assertThat(updated.intensityTechnique).isEqualTo(IntensityTechnique.DROP_SET)
        assertThat(updated.techniqueReason).contains("drop")
        // L'esercizio non e' stato sostituito: la tecnica non tocca la scelta.
        assertThat(updated.exerciseId).isEqualTo(planned.exerciseId)
        assertThat(updated.exerciseName).isEqualTo(planned.exerciseName)
        assertThat(updated.sets).isEqualTo(planned.sets)
    }

    @Test
    fun `annullare la modifica riporta la tecnica a NONE`() = runTest {
        val planned = firstPlanned()
        tool.execute(args(planned.id, "REST_PAUSE"))
        val changeId = pendingId()
        executor.apply(changeId)

        assertThat(plannedDao.getById(planned.id)!!.intensityTechnique)
            .isEqualTo(IntensityTechnique.REST_PAUSE)

        val revert = executor.revert(changeId)
        assertThat(revert).isInstanceOf(ApplyResult.Reverted::class.java)
        assertThat(plannedDao.getById(planned.id)!!.intensityTechnique)
            .isEqualTo(IntensityTechnique.NONE)
    }

    @Test
    fun `una tecnica inesistente viene rifiutata senza registrare proposte`() = runTest {
        val planned = firstPlanned()

        val output = tool.execute(args(planned.id, "SUPER_TECHNIQUE"))

        assertThat(output).contains("\"ok\":false")
        assertThat(changeRepository.getAll().filter { it.status == ChangeStatus.PROPOSED }).isEmpty()
    }

    @Test
    fun `senza motivazione la proposta viene rifiutata`() = runTest {
        val planned = firstPlanned()

        val output = tool.execute(args(planned.id, "DROP_SET", reason = "  "))

        assertThat(output).contains("\"ok\":false")
        assertThat(changeRepository.getAll().filter { it.status == ChangeStatus.PROPOSED }).isEmpty()
    }

    @Test
    fun `un plannedId inesistente non applica nulla`() = runTest {
        tool.execute(args(999_999L, "DROP_SET"))
        val changeId = pendingId()

        val result = executor.apply(changeId)

        assertThat(result).isInstanceOf(ApplyResult.Failed::class.java)
    }
}
