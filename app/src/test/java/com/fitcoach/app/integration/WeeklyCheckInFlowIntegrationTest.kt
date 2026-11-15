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
import com.fitcoach.app.data.repository.ApplyResult
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
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.usecase.RunWeeklyCheckInUseCase
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.TestProgramGenerator
import com.fitcoach.app.support.textResponse
import com.fitcoach.app.support.toolCallResponse
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
import java.time.LocalDate

/**
 * Verifica end-to-end del CHECK-IN SETTIMANALE attraverso l'UNICA autorita
 * decisionale (CoachOrchestrator). Usa un database Room reale e un provider AI
 * "scriptato": l'AI interpreta i dati e registra PROPOSTE con gli strumenti
 * "propose_*"; solo la conferma dell'utente le applica.
 *
 * Copre: proposta nutrizionale, proposta allenamento, rifiuto, conferma,
 * rollback e persistenza. Nessun secondo decisore, nessuna modifica automatica.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WeeklyCheckInFlowIntegrationTest {

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
    private lateinit var diaryRepository: NutritionDiaryRepository
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
        progressRepository = ProgressRepository(db.progressDao(), db.checkInDao())
        diaryRepository = NutritionDiaryRepository(db)
        chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())
        changeRepository = ChangeRepository(db.programChangeDao())

        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        userRepository.save(profile)
        chatRepository.saveAiSettings(
            AiProviderSettings(useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1", model = "test-model")
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

    // ---------------------------------------------------------------- provider

    private fun toolCallResponse(name: String, args: String) = com.fitcoach.app.support.toolCallResponse(name, args)

    private fun textResponse(text: String) = com.fitcoach.app.support.textResponse(text)

    // ---------------------------------------------------------------- wiring

    private fun coordinator(api: AiApi): ProgramGenerationCoordinator {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val orchestrator = CoachOrchestrator(AiClient(api, json), CoachToolRegistry(setOf(submitTool)), json)
        val aiGeneration = AiProgramGenerationService(
            orchestrator, chatRepository, SecureKeyStore(context), json, foodRepository, exerciseRepository
        )
        return ProgramGenerationCoordinator(aiGeneration = aiGeneration)
    }

    /** Costruisce il servizio di check-in con lo STESSO provider scriptato per il turno AI. */
    private fun checkInService(api: AiApi): WeeklyCheckInService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tools: Set<CoachTool> = setOf(
            AnalyzeWeeklyCheckInTool(
                userRepository, progressRepository, nutritionRepository, diaryRepository, workoutRepository
            ),
            AdjustCaloriesTool(actionExecutor),
            AdjustVolumeTool(actionExecutor),
            ProposeDeloadTool(actionExecutor)
        )
        val orchestrator = CoachOrchestrator(AiClient(api, json), CoachToolRegistry(tools), json)
        return WeeklyCheckInService(
            userRepository, progressRepository, nutritionRepository, diaryRepository,
            workoutRepository, chatRepository, changeRepository,
            AiConfigResolver(chatRepository, SecureKeyStore(context)), orchestrator
        )
    }

    // ---------------------------------------------------------------- data setup

    private suspend fun seedProgram() {
        // Programma iniziale generato dall'UNICO percorso (Coach AI + submit_full_program).
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
        assertThat(result).isInstanceOf(com.fitcoach.app.domain.coach.AiProgramGenerationService.Result.Success::class.java)
    }

    private suspend fun seedWeightsAndDiary() {
        val today = LocalDate.now().toEpochDay()
        // 4 settimane di pesate (media ~80 kg, trend leggermente calante).
        for (week in 0 until 4) {
            for (d in 0 until 3) {
                val day = today - week * 7L - d
                progressRepository.addWeight(
                    WeightRecord(dateEpochDay = day, weightKg = 80.0 + week * 0.2)
                )
            }
        }
        // 2 settimane di diario nutrizionale (aderenza reale vs piano).
        val food = foodRepository.getAll().first { it.proteinPer100g >= 20.0 }
        for (week in 0 until 2) {
            for (d in 0 until 7) {
                val day = today - week * 7L - d
                diaryRepository.addExtraFood(food, 150.0, MealSlot.LUNCH, "13:00", day)
            }
        }
    }

    private fun calorieArgs(planId: Long, weeklyKcal: Double) = buildJsonObject {
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

    private fun volumeArgs(mesocycleId: Long) = buildJsonObject {
        put("mesocycleId", JsonPrimitive(mesocycleId))
        put("volumeReductionPercent", JsonPrimitive(20.0))
        put("title", JsonPrimitive("Riduzione volume dal check-in"))
        put("rationale", JsonPrimitive("Regressione su piu esercizi."))
    }.toString()

    // ---------------------------------------------------------------- tests

    @Test
    fun `check-in salva la fotografia e chiede dati se insufficienti`() = runTest {
        seedProgram()
        // Nessun peso, nessun diario: dati insufficienti.
        val api = ScriptedApi(mutableListOf(textResponse("Servono piu dati per una valutazione affidabile.")))
        val outcome = RunWeeklyCheckInUseCase(checkInService(api))()

        assertThat(outcome).isNotNull()
        assertThat(outcome!!.report.dataSufficient).isFalse()
        assertThat(outcome.proposals).isEmpty()
        // Il check-in e stato persistito comunque (fotografia della settimana).
        assertThat(progressRepository.getCheckIns()).hasSize(1)
        // Il contesto inviato all'AI dichiara esplicitamente l'insufficienza dei dati.
        val prompt = api.sent.first().messages.joinToString { it.content ?: "" }
        assertThat(prompt).contains("Dati sufficienti per una modifica: NO")
    }

    @Test
    fun `proposta nutrizionale dell'AI resta in attesa e si applica solo dopo conferma`() = runTest {
        seedProgram()
        seedWeightsAndDiary()
        val planId = nutritionRepository.getActivePlan()!!.id
        val originalWeekly = nutritionRepository.getActivePlan()!!.weeklyCalories
        val target = originalWeekly * 0.95

        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("propose_calorie_adjustment", calorieArgs(planId, target)),
                textResponse("Ho preparato una proposta di aggiustamento calorico: confermala per applicarla.")
            )
        )
        val outcome = RunWeeklyCheckInUseCase(checkInService(api))()

        assertThat(outcome!!.proposals).hasSize(1)
        val change = outcome.proposals.single()
        assertThat(change.status).isEqualTo(ChangeStatus.PROPOSED)
        // NON applicata: il piano e invariato.
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isWithin(0.5).of(originalWeekly)

        // Conferma: ora si applica.
        val applied = actionExecutor.apply(change.id)
        assertThat(applied).isInstanceOf(ApplyResult.Applied::class.java)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isWithin(1.0).of(target)
        assertThat(changeRepository.getById(change.id)!!.status).isEqualTo(ChangeStatus.APPLIED)
    }

    @Test
    fun `proposta allenamento (volume) resta in attesa e si applica solo dopo conferma`() = runTest {
        seedProgram()
        seedWeightsAndDiary()
        val mesoId = workoutRepository.getActivePlan()!!.mesocycles.first().id

        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("propose_volume_adjustment", volumeArgs(mesoId)),
                textResponse("Propongo una riduzione del volume: conferma o rifiuta.")
            )
        )
        val outcome = RunWeeklyCheckInUseCase(checkInService(api))()

        val change = outcome!!.proposals.single()
        assertThat(change.domain.name).isEqualTo("WORKOUT")
        assertThat(change.status).isEqualTo(ChangeStatus.PROPOSED)
        assertThat(changeRepository.getById(change.id)!!.status).isEqualTo(ChangeStatus.PROPOSED)

        val applied = actionExecutor.apply(change.id)
        assertThat(applied).isInstanceOf(ApplyResult.Applied::class.java)
        assertThat(changeRepository.getById(change.id)!!.status).isEqualTo(ChangeStatus.APPLIED)
    }

    @Test
    fun `il rifiuto non modifica il programma`() = runTest {
        seedProgram()
        seedWeightsAndDiary()
        val planId = nutritionRepository.getActivePlan()!!.id
        val originalWeekly = nutritionRepository.getActivePlan()!!.weeklyCalories

        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("propose_calorie_adjustment", calorieArgs(planId, originalWeekly * 0.95)),
                textResponse("Proposta pronta.")
            )
        )
        val change = RunWeeklyCheckInUseCase(checkInService(api))()!!.proposals.single()

        changeRepository.updateStatus(change.id, ChangeStatus.REJECTED)

        assertThat(changeRepository.getById(change.id)!!.status).isEqualTo(ChangeStatus.REJECTED)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isWithin(0.5).of(originalWeekly)
        // Una proposta rifiutata non e applicabile.
        assertThat(actionExecutor.apply(change.id)).isInstanceOf(ApplyResult.Failed::class.java)
    }

    @Test
    fun `rollback ripristina il programma precedente dopo la conferma`() = runTest {
        seedProgram()
        seedWeightsAndDiary()
        val planId = nutritionRepository.getActivePlan()!!.id
        val originalWeekly = nutritionRepository.getActivePlan()!!.weeklyCalories

        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("propose_calorie_adjustment", calorieArgs(planId, originalWeekly * 0.95)),
                textResponse("Proposta pronta.")
            )
        )
        val change = RunWeeklyCheckInUseCase(checkInService(api))()!!.proposals.single()
        actionExecutor.apply(change.id)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isNotWithin(0.5).of(originalWeekly)

        val revert = actionExecutor.revert(change.id)

        assertThat(revert).isInstanceOf(ApplyResult.Reverted::class.java)
        assertThat(nutritionRepository.getActivePlan()!!.weeklyCalories).isWithin(0.5).of(originalWeekly)
        assertThat(changeRepository.getById(change.id)!!.status).isEqualTo(ChangeStatus.REVERTED)
    }

    @Test
    fun `senza provider configurato il check-in salva i dati e segnala l'errore`() = runTest {
        seedProgram()
        seedWeightsAndDiary()
        // Provider non configurato (nessun backend proxy, nessuna chiave).
        chatRepository.saveAiSettings(AiProviderSettings())

        val outcome = RunWeeklyCheckInUseCase(checkInService(ScriptedApi(mutableListOf())))()

        assertThat(outcome).isNotNull()
        assertThat(outcome!!.providerError).isNotNull()
        assertThat(outcome.proposals).isEmpty()
        assertThat(progressRepository.getCheckIns()).hasSize(1)
    }

    @Test
    fun `provider irraggiungibile non applica nulla e non inventa proposte`() = runTest {
        seedProgram()
        seedWeightsAndDiary()
        // Nessuna risposta programmata: il provider risponde 503.
        val api = ScriptedApi(mutableListOf())
        val outcome = RunWeeklyCheckInUseCase(checkInService(api))()

        assertThat(outcome!!.proposals).isEmpty()
        assertThat(outcome.providerError).isNotNull()
        assertThat(changeRepository.getAll().filter { it.status == ChangeStatus.APPLIED }).isEmpty()
    }

    @Test
    fun `la proposta del check-in e ritrovabile nella chat`() = runTest {
        seedProgram()
        seedWeightsAndDiary()
        val planId = nutritionRepository.getActivePlan()!!.id
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("propose_calorie_adjustment", calorieArgs(planId, nutritionRepository.getActivePlan()!!.weeklyCalories * 0.95)),
                textResponse("Ecco la mia valutazione della settimana.")
            )
        )
        RunWeeklyCheckInUseCase(checkInService(api))()

        val messages = chatRepository.getRecent(limit = 20)
        assertThat(messages.any { it.content.contains("Check-in settimanale") }).isTrue()
        assertThat(messages.any { it.content.contains("valutazione della settimana") }).isTrue()
    }

    @Test
    fun `l'AI usa lo strumento di analisi e il suo esito torna al modello`() = runTest {
        seedProgram()
        seedWeightsAndDiary()
        val api = ScriptedApi(
            mutableListOf(
                toolCallResponse("analyze_weekly_checkin", "{}"),
                textResponse("Valutazione completata senza modifiche.")
            )
        )

        RunWeeklyCheckInUseCase(checkInService(api))()

        // Due chiamate: la prima innesca lo strumento, la seconda riceve il suo esito.
        assertThat(api.sent).hasSize(2)
        val toolNames = api.sent.first().tools?.map { it.function.name } ?: emptyList()
        assertThat(toolNames).contains("analyze_weekly_checkin")
        val toolMessage = api.sent[1].messages.last { it.role == "tool" }
        assertThat(toolMessage.content).contains("datiSufficienti")
    }
}
