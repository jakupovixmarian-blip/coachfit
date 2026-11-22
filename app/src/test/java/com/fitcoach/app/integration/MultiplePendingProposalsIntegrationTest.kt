package com.fitcoach.app.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
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
import com.fitcoach.app.data.repository.ShoppingRepository
import com.fitcoach.app.data.repository.SubstitutionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.CoachService
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.SetIntensityTechniqueTool
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
import com.fitcoach.app.support.textResponse
import com.fitcoach.app.support.toolCallResponse
import com.google.common.truth.Truth.assertThat
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Un turno della chat puo registrare piu proposte: tutte devono restare
 * confermabili.
 *
 * Difetto segnalato: chiedendo una modifica alla scheda la chat rispondeva
 * "programma modificato" senza che nulla cambiasse. Una causa strutturale era
 * che [CoachService] restituiva solo l'ULTIMA proposta del turno: le altre
 * restavano in stato PROPOSED nel database ma non arrivavano all'interfaccia,
 * quindi non erano ne mostrate ne confermabili, e non venivano mai applicate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MultiplePendingProposalsIntegrationTest {

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
    private lateinit var actionExecutor: CoachActionExecutor

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
                useBackendProxy = true,
                backendBaseUrl = "https://proxy.test/v1",
                model = "test-model"
            )
        )
        actionExecutor = CoachActionExecutor(
            db = db,
            changeRepository = changeRepository,
            userRepository = userRepository,
            generationCoordinator = Lazy { coordinator(ScriptedApi(mutableListOf())) },
            json = json
        )
    }

    @After
    fun tearDown() = db.close()

    private fun aiGenerationService(api: AiApi): AiProgramGenerationService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val orchestrator = CoachOrchestrator(
            AiClient(api, json),
            CoachToolRegistry(setOf(submitTool, ConsultInvictusTool())),
            json
        )
        return AiProgramGenerationService(
            orchestrator, chatRepository, SecureKeyStore(context), json,
            foodRepository, exerciseRepository
        )
    }

    private fun coordinator(api: AiApi): ProgramGenerationCoordinator =
        ProgramGenerationCoordinator(aiGeneration = aiGenerationService(api))

    private fun coachService(api: AiApi): CoachService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tools = mutableSetOf<CoachTool>(
            SetIntensityTechniqueTool(actionExecutor),
            SubmitFullProgramTool(
                userRepository, foodRepository, exerciseRepository,
                nutritionRepository, workoutRepository, planVersionRepository, db, json
            )
        )
        val orchestrator = CoachOrchestrator(AiClient(api, json), CoachToolRegistry(tools), json)
        return CoachService(
            userRepository, nutritionRepository, workoutRepository,
            ProgressRepository(db.progressDao(), db.checkInDao()),
            changeRepository, chatRepository,
            NutritionDiaryRepository(db),
            ShoppingRepository(db, nutritionRepository, foodRepository),
            SubstitutionRepository(db, nutritionRepository, foodRepository),
            foodRepository, AiClient(api, json), SecureKeyStore(context), orchestrator
        )
    }

    private fun techniqueArgs(plannedId: Long, technique: String) = json.encodeToString(
        JsonObject.serializer(),
        JsonObject(
            mapOf(
                "plannedId" to JsonPrimitive(plannedId),
                "intensityTechnique" to JsonPrimitive(technique),
                "techniqueReason" to JsonPrimitive("Serie finale a cedimento controllato."),
                "title" to JsonPrimitive("Tecnica su esercizio $plannedId")
            )
        )
    )

    @Test
    fun `due proposte nello stesso turno arrivano tutte al flusso di conferma`() = runTest {
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("propose_intensity_technique", techniqueArgs(101L, "DROP_SET")),
                toolCallResponse("propose_intensity_technique", techniqueArgs(102L, "REST_PAUSE")),
                textResponse("Ho preparato due proposte da confermare.")
            )
        )

        val reply = coachService(api).send("Aggiungi un drop set e un rest-pause.")

        // Prima della correzione questo elenco esisteva solo con l'ultimo
        // elemento: la prima proposta restava orfana nel database.
        assertThat(reply.pendingChanges).hasSize(2)
        assertThat(reply.pendingChanges.map { it.id }).hasSize(2)
        assertThat(reply.pendingChanges.all { it.status == ChangeStatus.PROPOSED }).isTrue()
        // Il campo singolo resta valorizzato per il flusso di conferma esistente.
        assertThat(reply.pendingChange?.id).isEqualTo(reply.pendingChanges.last().id)
    }
}
