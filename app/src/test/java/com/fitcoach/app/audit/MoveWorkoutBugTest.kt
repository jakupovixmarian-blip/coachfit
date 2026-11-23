package com.fitcoach.app.audit

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
import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.MoveWorkoutTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.TestProgramGenerator
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * "Domani non posso allenarmi, sposta la seduta" e' una richiesta d'esempio del
 * prodotto. Se lo spostamento scrive su un campo che nessuna schermata legge,
 * l'utente riceve una conferma e non succede nulla.
 *
 * Qui si verifica l'esito VISIBILE: dopo la conferma il giorno della scheda e'
 * cambiato davvero, e un giorno impossibile viene rifiutato.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MoveWorkoutBugTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var db: FitCoachDatabase
    private lateinit var executor: CoachActionExecutor
    private lateinit var workoutRepository: WorkoutRepository

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
        val userRepository = UserRepository(db.userProfileDao())
        val foodRepository = FoodRepository(db.foodDao())
        val exerciseRepository = ExerciseRepository(db.exerciseDao())
        val nutritionRepository = NutritionRepository(db)
        workoutRepository = WorkoutRepository(db)
        val planVersionRepository = PlanVersionRepository(db.planVersionDao())
        val chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())

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
            changeRepository = ChangeRepository(db.programChangeDao()),
            userRepository = userRepository,
            generationCoordinator = Lazy { coordinator(context) },
            json = json
        )
        TestProgramGenerator.generate(
            json = json, userRepository = userRepository,
            foodRepository = foodRepository, exerciseRepository = exerciseRepository,
            nutritionRepository = nutritionRepository, workoutRepository = workoutRepository,
            planVersionRepository = planVersionRepository, chatRepository = chatRepository,
            db = db, profile = profilo
        )
    }

    @After
    fun tearDown() = db.close()

    private fun coordinator(context: Context): ProgramGenerationCoordinator {
        val submitTool: CoachTool = SubmitFullProgramTool(
            UserRepository(db.userProfileDao()), FoodRepository(db.foodDao()),
            ExerciseRepository(db.exerciseDao()), NutritionRepository(db), WorkoutRepository(db),
            PlanVersionRepository(db.planVersionDao()), db, json
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

    private suspend fun primaSeduta(): WorkoutDay {
        val piano = workoutRepository.getActivePlan()!!
        return piano.days.first { !it.isRestDay }
    }

    private fun proposta(dayId: Long, nuovoGiorno: Int) = CoachAction(
        action = CoachAction.ACTION_MOVE_WORKOUT,
        domain = "WORKOUT",
        title = "Spostamento seduta",
        situation = "", problem = "", dataUsed = "",
        proposedChange = "Spostare la seduta al giorno $nuovoGiorno",
        rationale = "", expectedOutcome = "", contraindications = "",
        payload = mutableMapOf(
            "dayId" to dayId.toString(),
            "newDayIndex" to nuovoGiorno.toString()
        )
    )

    @Test
    fun `dopo la conferma la seduta e' davvero in un altro giorno`() = runTest {
        val seduta = primaSeduta()
        val nuovo = (seduta.dayIndex + 2) % 7
        val id = executor.registerProposal(proposta(seduta.id, nuovo))
        val esito = executor.apply(id)
        assertTrue("Lo spostamento deve applicarsi: $esito", esito is ApplyResult.Applied)

        val dopo = workoutRepository.getActivePlan()!!.days.first { it.id == seduta.id }
        assertNotEquals(
            "La conferma dice 'spostata' ma la scheda mostra ancora il giorno ${dopo.dayIndex}",
            seduta.dayIndex, dopo.dayIndex
        )
    }

    @Test
    fun `il giorno mostrato dalla scheda e' quello nuovo`() = runTest {
        val seduta = primaSeduta()
        val nuovo = (seduta.dayIndex + 3) % 7
        executor.apply(executor.registerProposal(proposta(seduta.id, nuovo)))

        val giorni = workoutRepository.getActivePlan()!!.days.filter { !it.isRestDay }
        assertTrue(
            "Nessuna seduta nel giorno $nuovo: la UI mostra la scheda vecchia. " +
                "Giorni attuali: ${giorni.map { it.dayIndex to it.title }}",
            giorni.any { it.dayIndex == nuovo }
        )
    }

    @Test
    fun `un giorno fuori 0-6 viene rifiutato`() = runTest {
        val seduta = primaSeduta()
        val id = executor.registerProposal(proposta(seduta.id, 99))
        val esito = executor.apply(id)
        assertTrue(
            "Un giorno 99 non esiste nella settimana: la conferma deve fallire. Esito: $esito",
            esito !is ApplyResult.Applied
        )
        val dopo = workoutRepository.getActivePlan()!!.days.first { it.id == seduta.id }
        assertTrue(
            "Il giorno non deve essere corrotto: ${dopo.dayIndex}",
            dopo.dayIndex in 0..6
        )
    }

    @Test
    fun `un giorno negativo viene rifiutato`() = runTest {
        val seduta = primaSeduta()
        val id = executor.registerProposal(proposta(seduta.id, -3))
        val esito = executor.apply(id)
        assertTrue("Giorno -3 non esiste. Esito: $esito", esito !is ApplyResult.Applied)
    }

    @Test
    fun `spostare su un giorno occupato non crea due sedute nello stesso giorno`() = runTest {
        val giorni = workoutRepository.getActivePlan()!!.days.filter { !it.isRestDay }
        val seduta = giorni.first()
        val occupato = giorni.first { it.dayIndex != seduta.dayIndex }

        executor.apply(executor.registerProposal(proposta(seduta.id, occupato.dayIndex)))

        val doppi = workoutRepository.getActivePlan()!!.days
            .filter { !it.isRestDay }
            .groupBy { it.dayIndex }
            .filterValues { it.size > 1 }
            .keys
        assertTrue("Due sedute nello stesso giorno: $doppi", doppi.isEmpty())
    }

    @Test
    fun `lo spostamento si puo' annullare`() = runTest {
        val seduta = primaSeduta()
        val originale = seduta.dayIndex
        val nuovo = (originale + 2) % 7
        val id = executor.registerProposal(proposta(seduta.id, nuovo))
        executor.apply(id)
        val esito = executor.revert(id)
        assertTrue("L'annullamento deve riuscire: $esito", esito is ApplyResult.Reverted)
        val dopo = workoutRepository.getActivePlan()!!.days.first { it.id == seduta.id }
        assertTrue(
            "Annullando, la seduta deve tornare al giorno $originale, trovato ${dopo.dayIndex}",
            dopo.dayIndex == originale
        )
    }

    @Test
    fun `il tool propone usando il giorno della scheda`() = runTest {
        val seduta = primaSeduta()
        val tool = MoveWorkoutTool(executor)
        val esito = tool.execute(
            buildJsonObject {
                put("dayId", JsonPrimitive(seduta.id))
                put("newDayIndex", JsonPrimitive((seduta.dayIndex + 2) % 7))
            }
        )
        assertTrue("Il tool deve accettare l'id della seduta: $esito", esito.contains("\"ok\":true"))
    }
}
