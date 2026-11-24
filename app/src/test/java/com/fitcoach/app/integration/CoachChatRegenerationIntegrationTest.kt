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
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ProposeRegeneratePlanTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.fitcoach.app.support.GenerationScript
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.SimulatedAiPrograms
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
 * Verifica end-to-end del collegamento CHAT -> RIGENERAZIONE PER AMBITO.
 *
 * L'utente chiede al Coach AI di cambiare il programma; l'AI sceglie lo strumento
 * adatto (propose_regenerate_plan con ambito WORKOUT/NUTRITION/BOTH) e registra una
 * proposta. Solo dopo la conferma dell'utente il [CoachActionExecutor] delega la
 * rigenerazione all'UNICO motore ([ProgramGenerationCoordinator] -> submit_full_program),
 * esattamente lo stesso usato per la generazione iniziale. Non esiste un secondo motore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoachChatRegenerationIntegrationTest {

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
            AiProviderSettings(useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1", model = "test-model")
        )
        actionExecutor = buildExecutor()
    }

    @After
    fun tearDown() = db.close()

    // ---- Cablaggio dei servizi reali con provider AI "scriptato". ----

    private fun aiGenerationService(api: AiApi): AiProgramGenerationService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val registry = CoachToolRegistry(setOf(submitTool, ConsultInvictusTool()))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)
        return AiProgramGenerationService(
            orchestrator, chatRepository, SecureKeyStore(context), json, foodRepository, exerciseRepository
        )
    }

    private fun coordinator(api: AiApi): ProgramGenerationCoordinator =
        ProgramGenerationCoordinator(aiGeneration = aiGenerationService(api))

    private fun buildExecutor(api: AiApi = ScriptedApi(mutableListOf())): CoachActionExecutor =
        CoachActionExecutor(
            db = db,
            changeRepository = changeRepository,
            userRepository = userRepository,
            generationCoordinator = Lazy { coordinator(api) },
            json = json
        )

    /**
     * CoachService completo: usa lo STESSO provider scriptato sia per il turno di chat
     * (che propone la rigenerazione) sia per il turno di generazione (submit_full_program).
     */
    private fun coachService(api: AiApi): CoachService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tools = mutableSetOf<CoachTool>(
            ProposeRegeneratePlanTool(actionExecutor),
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

    private suspend fun proposal(
        scope: String,
        setsPerExercise: Int = 4,
        technique: String = "NONE"
    ): String {
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        val built = SimulatedAiPrograms.proposal(
            profile, foods, exercises,
            WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek),
            setsPerExercise = setsPerExercise,
            technique = technique
        )
        return SimulatedAiPrograms.json(json, built, scope)
    }

    private fun regenerateArgs(
        scope: String,
        directive: String = "Aggiungi una tecnica d'intensita' per ogni gruppo muscolare, " +
            "inserisci almeno un esercizio di isolamento per gruppo e riduci il volume settimanale."
    ) = json.encodeToString(
        JsonObject.serializer(),
        JsonObject(
            mapOf(
                "scope" to JsonPrimitive(scope),
                "directive" to JsonPrimitive(directive),
                "title" to JsonPrimitive("Rigenerazione $scope"),
                "problem" to JsonPrimitive("Richiesta dell'utente di cambiare la struttura.")
            )
        )
    )

    /** Simula la chat: l'AI propone la rigenerazione e il coach registra la proposta. */
    private suspend fun askCoachToRegenerate(scope: String): Long {
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("propose_regenerate_plan", regenerateArgs(scope)),
                textResponse("Ho preparato la proposta di rigenerazione: confermala per applicarla.")
            )
        )
        val reply = coachService(api).send("Voglio cambiare la struttura del programma ($scope).")
        val pending = reply.pendingChange
        assertThat(pending).isNotNull()
        assertThat(pending!!.status).isEqualTo(ChangeStatus.PROPOSED)
        return pending.id
    }

    // ---------------------------------------------------------------- TESTS

    @Test
    fun `chat propone la rigenerazione e non applica nulla finche non si conferma`() = runTest {
        val changeId = askCoachToRegenerate("WORKOUT")
        assertThat(changeRepository.getById(changeId)!!.status).isEqualTo(ChangeStatus.PROPOSED)
        assertThat(nutritionRepository.getActivePlan()).isNull()
        assertThat(workoutRepository.getActivePlan()).isNull()
    }

    @Test
    fun `conferma rigenera solo la scheda quando l'ambito e WORKOUT`() = runTest {
        assertThat(coordinator(ScriptedApi(GenerationScript.consultationsThen(
            toolCallResponse("submit_full_program", proposal("BOTH")),
            textResponse("Programma generato.")
        ))).generate(profile))
            .isInstanceOf(com.fitcoach.app.domain.usecase.ProgramGenerationResult.Success::class.java)
        val originalNutritionId = nutritionRepository.getActivePlan()!!.id
        val originalWorkoutId = workoutRepository.getActivePlan()!!.id

        val changeId = askCoachToRegenerate("WORKOUT")
        val exec = buildExecutor(
            ScriptedApi(GenerationScript.consultationsThen(
                // 5 serie e una tecnica: la richiesta dell'utente ha avuto effetto,
                // quindi la scheda DEVE risultare diversa da quella precedente.
                toolCallResponse("submit_full_program", proposal("WORKOUT", setsPerExercise = 3, technique = "DROP_SET")),
                textResponse("Scheda rigenerata.")
            ))
        )
        val result = exec.apply(changeId)

        assertThat(result).isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Applied::class.java)
        assertThat(workoutRepository.getActivePlan()!!.id).isNotEqualTo(originalWorkoutId)
        assertThat(nutritionRepository.getActivePlan()!!.id).isEqualTo(originalNutritionId)
        // La scheda nuova deve contenere davvero le serie e le tecniche richieste.
        val activeWorkout = workoutRepository.getActivePlan()!!
        val days = db.workoutDayDao().getForMesocycles(
            db.mesocycleDao().getForPlan(activeWorkout.id).map { it.id }
        )
        val planned = db.plannedExerciseDao().getForDays(days.map { it.id })
        assertThat(planned).isNotEmpty()
        assertThat(planned.map { it.sets }.distinct()).containsExactly(3)
        assertThat(planned.map { it.intensityTechnique.name }.distinct())
            .containsExactly("DROP_SET")
    }

    @Test
    fun `conferma rigenera solo la nutrizione quando l'ambito e NUTRITION`() = runTest {
        assertThat(coordinator(ScriptedApi(GenerationScript.consultationsThen(
            toolCallResponse("submit_full_program", proposal("BOTH")),
            textResponse("Programma generato.")
        ))).generate(profile))
            .isInstanceOf(com.fitcoach.app.domain.usecase.ProgramGenerationResult.Success::class.java)
        val originalNutritionId = nutritionRepository.getActivePlan()!!.id
        val originalWorkoutId = workoutRepository.getActivePlan()!!.id

        val changeId = askCoachToRegenerate("NUTRITION")
        val exec = buildExecutor(
            ScriptedApi(GenerationScript.consultationsThen(
                toolCallResponse("submit_full_program", proposal("NUTRITION")),
                textResponse("Piano alimentare rigenerato.")
            ))
        )
        val result = exec.apply(changeId)

        assertThat(result).isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Applied::class.java)
        assertThat(nutritionRepository.getActivePlan()!!.id).isNotEqualTo(originalNutritionId)
        assertThat(workoutRepository.getActivePlan()!!.id).isEqualTo(originalWorkoutId)
    }

    @Test
    fun `ambito BOTH rigenera entrambe le parti`() = runTest {
        coordinator(ScriptedApi(GenerationScript.consultationsThen(
            toolCallResponse("submit_full_program", proposal("BOTH")),
            textResponse("Programma generato.")
        ))).generate(profile)
        val originalNutritionId = nutritionRepository.getActivePlan()!!.id
        val originalWorkoutId = workoutRepository.getActivePlan()!!.id

        val changeId = askCoachToRegenerate("BOTH")
        val exec = buildExecutor(
            ScriptedApi(GenerationScript.consultationsThen(
                toolCallResponse("submit_full_program", proposal("BOTH")),
                textResponse("Programma rigenerato.")
            ))
        )
        exec.apply(changeId)

        assertThat(nutritionRepository.getActivePlan()!!.id).isNotEqualTo(originalNutritionId)
        assertThat(workoutRepository.getActivePlan()!!.id).isNotEqualTo(originalWorkoutId)
    }

    @Test
    fun `la direttiva dell'utente arriva al motore di generazione`() = runTest {
        coordinator(ScriptedApi(GenerationScript.consultationsThen(
            toolCallResponse("submit_full_program", proposal("BOTH")),
            textResponse("Programma generato.")
        ))).generate(profile)

        // Una rigenerazione che riceve la stessa scheda dell'ultima volta deve
        // essere segnalata come NON applicata: prima l'utente leggeva "modifica
        // applicata" e ritrovava la scheda identica.
        val changeId = askCoachToRegenerate("WORKOUT")
        val exec = buildExecutor(
            ScriptedApi(GenerationScript.consultationsThen(
                toolCallResponse("submit_full_program", proposal("WORKOUT")),
                textResponse("Scheda rigenerata.")
            ))
        )
        // Il proposal("WORKOUT") e' deterministico: stessa struttura di prima.
        val result = exec.apply(changeId)

        assertThat(result)
            .isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Failed::class.java)
        val messaggio = (result as com.fitcoach.app.data.repository.ApplyResult.Failed).message
        assertThat(messaggio).contains("identica")
    }

    @Test
    fun `la direttiva compare nel testo che il modello riceve`() = runTest {
        // Verifica il canale vero, non il risultato finale: la richiesta
        // dell'utente deve essere presente nel prompt di generazione. Senza
        // questo passaggio il modello rigenerava dagli stessi dati di anamnesi.
        val richiesta = "Aggiungi tecniche d'intensita' e un esercizio di isolamento per gruppo"
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val context = aiGenerationService(ScriptedApi(mutableListOf())).buildGenerationContext(
            profile, foods, exerciseRepository.getAll(),
            ProgramGenerationEngine.Scope.WORKOUT, richiesta
        )
        assertThat(context).contains(richiesta)
        assertThat(context).contains("RICHIESTA ESPLICITA DELL'UTENTE")

        // Controprova: senza direttiva il blocco non compare.
        val senza = aiGenerationService(ScriptedApi(mutableListOf())).buildGenerationContext(
            profile, foods, exerciseRepository.getAll(),
            ProgramGenerationEngine.Scope.WORKOUT, null
        )
        assertThat(senza).doesNotContain("RICHIESTA ESPLICITA DELL'UTENTE")
    }

    @Test
    fun `una proposta senza direttiva non viene registrata`() = runTest {
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse(
                    "propose_regenerate_plan",
                    json.encodeToString(
                        JsonObject.serializer(),
                        JsonObject(mapOf("scope" to JsonPrimitive("WORKOUT")))
                    )
                ),
                textResponse("Fatto.")
            )
        )
        val reply = coachService(api).send("Cambia la scheda.")
        // Senza direttiva il tool risponde con errore: nessuna proposta in attesa.
        assertThat(reply.pendingChange).isNull()
    }

    @Test
    fun `il rollback riattiva i piani precedenti dopo una rigenerazione`() = runTest {
        coordinator(ScriptedApi(GenerationScript.consultationsThen(
            toolCallResponse("submit_full_program", proposal("BOTH")),
            textResponse("Programma generato.")
        ))).generate(profile)
        val originalNutritionId = nutritionRepository.getActivePlan()!!.id
        val originalWorkoutId = workoutRepository.getActivePlan()!!.id

        val changeId = askCoachToRegenerate("BOTH")
        val exec = buildExecutor(
            ScriptedApi(GenerationScript.consultationsThen(
                toolCallResponse("submit_full_program", proposal("BOTH")),
                textResponse("Programma rigenerato.")
            ))
        )
        exec.apply(changeId)
        assertThat(nutritionRepository.getActivePlan()!!.id).isNotEqualTo(originalNutritionId)

        val revert = exec.revert(changeId)

        assertThat(revert).isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Reverted::class.java)
        assertThat(nutritionRepository.getActivePlan()!!.id).isEqualTo(originalNutritionId)
        assertThat(workoutRepository.getActivePlan()!!.id).isEqualTo(originalWorkoutId)
        assertThat(changeRepository.getById(changeId)!!.status).isEqualTo(ChangeStatus.REVERTED)
    }

    @Test
    fun `provider irraggiungibile non lascia piani incoerenti`() = runTest {
        coordinator(ScriptedApi(GenerationScript.consultationsThen(
            toolCallResponse("submit_full_program", proposal("BOTH")),
            textResponse("Programma generato.")
        ))).generate(profile)
        val originalNutritionId = nutritionRepository.getActivePlan()!!.id
        val originalWorkoutId = workoutRepository.getActivePlan()!!.id

        val changeId = askCoachToRegenerate("BOTH")
        // Nessuna risposta dal provider alla conferma: la rigenerazione fallisce.
        val exec = buildExecutor(ScriptedApi(mutableListOf()))
        val result = exec.apply(changeId)

        assertThat(result).isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Failed::class.java)
        assertThat(nutritionRepository.getActivePlan()!!.id).isEqualTo(originalNutritionId)
        assertThat(workoutRepository.getActivePlan()!!.id).isEqualTo(originalWorkoutId)
        assertThat(changeRepository.getById(changeId)!!.status).isEqualTo(ChangeStatus.PROPOSED)
    }

    @Test
    fun `una proposta non valida dell'AI viene bloccata e non cambia il programma`() = runTest {
        coordinator(ScriptedApi(GenerationScript.consultationsThen(
            toolCallResponse("submit_full_program", proposal("BOTH")),
            textResponse("Programma generato.")
        ))).generate(profile)
        val originalNutritionId = nutritionRepository.getActivePlan()!!.id
        val originalWorkoutId = workoutRepository.getActivePlan()!!.id

        // Proposta invalida: solo 2 giorni di allenamento invece dei 3 dichiarati.
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

        val changeId = askCoachToRegenerate("WORKOUT")
        val exec = buildExecutor(
            ScriptedApi(GenerationScript.consultationsThen(
                toolCallResponse("submit_full_program", SimulatedAiPrograms.json(json, invalid, "WORKOUT")),
                textResponse("Devo correggere la scheda.")
            ))
        )
        val result = exec.apply(changeId)

        assertThat(result).isInstanceOf(com.fitcoach.app.data.repository.ApplyResult.Failed::class.java)
        assertThat(workoutRepository.getActivePlan()!!.id).isEqualTo(originalWorkoutId)
        assertThat(nutritionRepository.getActivePlan()!!.id).isEqualTo(originalNutritionId)
    }
}
