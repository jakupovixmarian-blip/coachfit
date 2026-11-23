package com.fitcoach.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
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
import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.SubstituteFoodTool
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
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Il write path del coach: quello che l'AI propone e l'utente conferma finisce
 * davvero nel piano. La proposta e' gia' un atto dell'utente (ha premuto
 * conferma), ma i NUMERI dentro la proposta li ha scritti il modello: se non
 * sono validati, l'utente conferma qualcosa che non ha mai visto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoachWriteToolsAdversarialTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var changeRepository: ChangeRepository
    private lateinit var executor: CoachActionExecutor

    private val profilo = UserProfile(
        id = 1L, name = "Luca", ageYears = 30, sex = Sex.MALE, heightCm = 175.0,
        weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        userRepository = UserRepository(db.userProfileDao())
        val foodRepository = FoodRepository(db.foodDao())
        val exerciseRepository = ExerciseRepository(db.exerciseDao())
        nutritionRepository = NutritionRepository(db)
        val workoutRepository = WorkoutRepository(db)
        val planVersionRepository = PlanVersionRepository(db.planVersionDao())
        val chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())
        changeRepository = ChangeRepository(db.programChangeDao())

        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        userRepository.save(profilo)
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
            generationCoordinator = Lazy { coordinator(context) },
            json = json
        )
        TestProgramGenerator.generate(
            json = json,
            userRepository = userRepository,
            foodRepository = foodRepository,
            exerciseRepository = exerciseRepository,
            nutritionRepository = nutritionRepository,
            workoutRepository = workoutRepository,
            planVersionRepository = planVersionRepository,
            chatRepository = chatRepository,
            db = db,
            profile = profilo
        )
    }

    @After
    fun tearDown() = db.close()

    private fun coordinator(context: Context): ProgramGenerationCoordinator {
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, FoodRepository(db.foodDao()), ExerciseRepository(db.exerciseDao()),
            nutritionRepository, WorkoutRepository(db), PlanVersionRepository(db.planVersionDao()),
            db, json
        )
        val orchestrator = CoachOrchestrator(
            AiClient(ScriptedApi(mutableListOf()), json),
            CoachToolRegistry(setOf(submitTool, ConsultInvictusTool())), json
        )
        val aiGeneration = AiProgramGenerationService(
            orchestrator, ChatRepository(db.chatDao(), db.aiSettingsDao()),
            SecureKeyStore(context), json,
            FoodRepository(db.foodDao()), ExerciseRepository(db.exerciseDao())
        )
        return ProgramGenerationCoordinator(aiGeneration = aiGeneration)
    }

    /** Prima voce di pasto del piano attivo: il bersaglio delle sostituzioni. */
    private suspend fun primaVoce(): Long {
        val piano = nutritionRepository.getActivePlan()!!
        val giorno = piano.days.first()
        return giorno.meals.flatMap { it.entries }.first().id
    }

    private suspend fun voce(id: Long) = db.mealEntryDao().getById(id)!!

    private fun proposta(
        entryId: Long, grams: Double, kcal: Double,
        protein: Double, carbs: Double, fat: Double
    ) = CoachAction(
        action = CoachAction.ACTION_SUBSTITUTE_FOOD,
        domain = "NUTRITION",
        title = "Sostituzione",
        situation = "", problem = "", dataUsed = "",
        proposedChange = "sostituzione", rationale = "",
        expectedOutcome = "", contraindications = "",
        payload = mutableMapOf(
            "entryId" to entryId.toString(),
            "newFoodName" to "Alimento di prova",
            "grams" to grams.toString(),
            "kcal" to kcal.toString(),
            "protein" to protein.toString(),
            "carbs" to carbs.toString(),
            "fat" to fat.toString()
        )
    )

    @Test
    fun `una sostituzione normale viene applicata`() = runTest {
        val id = primaVoce()
        val propostaId = executor.registerProposal(
            proposta(id, grams = 150.0, kcal = 165.0, protein = 20.0, carbs = 10.0, fat = 5.0)
        )
        val esito = executor.apply(propostaId)
        assertTrue("Un sostituto plausibile deve applicarsi: $esito", esito is com.fitcoach.app.data.repository.ApplyResult.Applied)
        val v = voce(id)
        assertTrue(v.kcal > 0.0)
    }

    @Test
    fun `grammi negativi non devono entrare nel piano`() = runTest {
        val id = primaVoce()
        val propostaId = executor.registerProposal(
            proposta(id, grams = -250.0, kcal = 200.0, protein = 20.0, carbs = 10.0, fat = 5.0)
        )
        val esito = executor.apply(propostaId)
        assertTrue(
            "Una quantita' negativa e' impossibile: la conferma deve fallire, non scrivere -250 g. " +
                "Esito: $esito",
            esito !is com.fitcoach.app.data.repository.ApplyResult.Applied
        )
    }

    @Test
    fun `calorie negative non devono entrare nel piano`() = runTest {
        val id = primaVoce()
        val propostaId = executor.registerProposal(
            proposta(id, grams = 150.0, kcal = -500.0, protein = 20.0, carbs = 10.0, fat = 5.0)
        )
        val esito = executor.apply(propostaId)
        assertTrue("Un alimento da -500 kcal non esiste. Esito: $esito",
            esito !is com.fitcoach.app.data.repository.ApplyResult.Applied)
    }

    @Test
    fun `grammi non finiti non devono entrare nel piano`() = runTest {
        val id = primaVoce()
        val propostaId = executor.registerProposal(
            proposta(id, grams = Double.POSITIVE_INFINITY, kcal = 200.0,
                protein = 20.0, carbs = 10.0, fat = 5.0)
        )
        val esito = executor.apply(propostaId)
        assertTrue("Infinity non e' una grammatura. Esito: $esito",
            esito !is com.fitcoach.app.data.repository.ApplyResult.Applied)
    }

    @Test
    fun `una sostituzione con calorie incompatibili coi macro viene rifiutata`() = runTest {
        val id = primaVoce()
        // 0 g di macro ma 500 kcal: i numeri non tornano, il modello ha inventato.
        val propostaId = executor.registerProposal(
            proposta(id, grams = 100.0, kcal = 500.0, protein = 0.0, carbs = 0.0, fat = 0.0)
        )
        val esito = executor.apply(propostaId)
        assertTrue(
            "500 kcal da zero grammi di macro e' un dato incoerente. Esito: $esito",
            esito !is com.fitcoach.app.data.repository.ApplyResult.Applied
        )
    }

    @Test
    fun `una quantita' assurda viene rifiutata`() = runTest {
        val id = primaVoce()
        val propostaId = executor.registerProposal(
            proposta(id, grams = 50_000.0, kcal = 30_000.0, protein = 2500.0,
                carbs = 3000.0, fat = 900.0)
        )
        val esito = executor.apply(propostaId)
        assertTrue("50 kg in una voce di pasto non e' una porzione. Esito: $esito",
            esito !is com.fitcoach.app.data.repository.ApplyResult.Applied)
    }

    @Test
    fun `una sostituzione con un allergene viene rifiutata`() = runTest {
        // Profilo allergico al lattosio: il coach in chat propone uno yogurt.
        userRepository.save(profilo.copy(allergies = listOf("lattosio")))
        val id = primaVoce()
        val yogurt = db.foodDao().getAll().first { it.name.contains("Yogurt", ignoreCase = true) }
        val propostaId = executor.registerProposal(
            CoachAction(
                action = CoachAction.ACTION_SUBSTITUTE_FOOD,
                domain = "NUTRITION", title = "Sostituzione con yogurt",
                situation = "", problem = "", dataUsed = "",
                proposedChange = "yogurt", rationale = "",
                expectedOutcome = "", contraindications = "",
                payload = mutableMapOf(
                    "entryId" to id.toString(),
                    "newFoodId" to yogurt.id.toString(),
                    "newFoodName" to yogurt.name,
                    "grams" to "170",
                    "kcal" to (yogurt.kcalPer100g * 1.7).toString(),
                    "protein" to (yogurt.proteinPer100g * 1.7).toString(),
                    "carbs" to (yogurt.carbsPer100g * 1.7).toString(),
                    "fat" to (yogurt.fatPer100g * 1.7).toString()
                )
            )
        )
        val esito = executor.apply(propostaId)
        assertTrue(
            "Un allergico al lattosio non deve poter ricevere lo yogurt, nemmeno via chat. Esito: $esito",
            esito !is com.fitcoach.app.data.repository.ApplyResult.Applied
        )
    }

    @Test
    fun `una voce inesistente non viene applicata`() = runTest {
        val propostaId = executor.registerProposal(
            proposta(999_999L, grams = 100.0, kcal = 200.0, protein = 20.0, carbs = 10.0, fat = 5.0)
        )
        val esito = executor.apply(propostaId)
        assertTrue("Non esiste la voce 999999. Esito: $esito",
            esito !is com.fitcoach.app.data.repository.ApplyResult.Applied)
    }

    @Test
    fun `la proposta registrata risulta in attesa e non applicata`() = runTest {
        val id = primaVoce()
        val propostaId = executor.registerProposal(
            proposta(id, grams = 150.0, kcal = 200.0, protein = 20.0, carbs = 10.0, fat = 5.0)
        )
        val change = changeRepository.getById(propostaId)
        assertNotNull("La proposta deve essere registrata", change)
        assertTrue(
            "Deve restare PROPOSED finche' l'utente non conferma: ${change!!.status}",
            change.status == ChangeStatus.PROPOSED
        )
        val v = voce(id)
        assertTrue("Nessuna scrittura prima della conferma", v.grams > 0.0)
    }

    @Test
    fun `lo strumento rifiuta gli argomenti obbligatori mancanti`() = runTest {
        val tool = SubstituteFoodTool(executor)
        val r = tool.execute(buildJsonObject { })
        assertTrue("Senza entryId non si propone nulla: $r", r.contains("\"ok\":false"))
    }

    @Test
    fun `lo strumento non applica mai la modifica`() = runTest {
        val id = primaVoce()
        val tool = SubstituteFoodTool(executor)
        val prima = voce(id)
        val r = tool.execute(
            buildJsonObject {
                put("entryId", id)
                put("newFoodName", "Riso")
                put("grams", 120.0)
                put("kcal", 420.0)
                put("protein", 9.0)
                put("carbs", 90.0)
                put("fat", 2.0)
            }
        )
        assertTrue("Deve registrare una proposta: $r", r.contains("PROPOSTA_REGISTRATA"))
        val dopo = voce(id)
        assertTrue(
            "Lo strumento di scrittura NON deve toccare il piano: e' una proposta",
            prima.grams == dopo.grams && prima.kcal == dopo.kcal
        )
    }
}
