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
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProviderSettings
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
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Il deload esiste per ridurre l'affaticamento. Se la percentuale arriva
 * negativa o fuori scala, il "deload" aumenta il volume: esattamente il
 * contrario di cio' che serve, e nel momento in cui l'utente e' piu' stanco.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeloadAdversarialTest {

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

    private suspend fun mesocicloAttivo(): Long =
        workoutRepository.getActivePlan()!!.mesocycles.first().id

    private suspend fun serieTotali(mesocycleId: Long): Int {
        val giorni = db.workoutDayDao().getForMesocycle(mesocycleId)
        return giorni.sumOf { giorno ->
            db.plannedExerciseDao().getForDay(giorno.id).sumOf { it.sets }
        }
    }

    private fun deload(mesocycleId: Long, percentuale: Double) = CoachAction(
        action = CoachAction.ACTION_DELOAD,
        domain = "WORKOUT",
        title = "Deload",
        situation = "", problem = "", dataUsed = "",
        proposedChange = "Deload al $percentuale%",
        rationale = "", expectedOutcome = "", contraindications = "",
        payload = mutableMapOf(
            "mesocycleId" to mesocycleId.toString(),
            "volumeReductionPercent" to percentuale.toString()
        )
    )

    @Test
    fun `il deload non deve cancellare la scheda`() = runTest {
        val meso = mesocicloAttivo()
        val prima = serieTotali(meso)
        assertTrue("Il fixture deve avere sedute: $prima", prima > 0)

        executor.apply(executor.registerProposal(deload(meso, 40.0)))

        val giorni = db.workoutDayDao().getForMesocycle(meso)
        assertTrue(
            "Dopo il deload il mesociclo $meso non ha piu' sedute: i giorni di allenamento " +
                "sono stati cancellati a cascata (mesocycles e' @Insert(REPLACE) e workout_days " +
                "ha onDelete CASCADE). L'utente perde la scheda per un deload.",
            giorni.isNotEmpty()
        )
    }

    @Test
    fun `un deload normale riduce il volume`() = runTest {
        val meso = mesocicloAttivo()
        val prima = serieTotali(meso)
        val esito = executor.apply(executor.registerProposal(deload(meso, 40.0)))
        assertTrue("Un deload al 40% deve applicarsi: $esito", esito is ApplyResult.Applied)
        assertTrue(
            "Il volume deve scendere: prima $prima, dopo ${serieTotali(meso)}",
            serieTotali(meso) < prima
        )
    }

    @Test
    fun `una percentuale negativa non deve aumentare il volume`() = runTest {
        val meso = mesocicloAttivo()
        val prima = serieTotali(meso)
        val esito = executor.apply(executor.registerProposal(deload(meso, -50.0)))
        val dopo = serieTotali(meso)
        assertTrue(
            "Un deload al -50% aumenterebbe il volume da $prima a $dopo: nel momento in " +
                "cui l'utente e' piu' affaticato. Esito: $esito",
            dopo <= prima || esito !is ApplyResult.Applied
        )
    }

    @Test
    fun `una percentuale oltre 100 non azzera la scheda`() = runTest {
        val meso = mesocicloAttivo()
        val esito = executor.apply(executor.registerProposal(deload(meso, 250.0)))
        if (esito is ApplyResult.Applied) {
            val giorni = db.workoutDayDao().getForMesocycle(meso)
            val esercizi = giorni.flatMap { db.plannedExerciseDao().getForDay(it.id) }
            assertTrue(
                "Con una riduzione oltre il 100% ogni esercizio deve restare allenabile",
                esercizi.isEmpty() || esercizi.all { it.sets >= 1 }
            )
            assertTrue(
                "La scheda non deve svuotarsi: ${esercizi.size} esercizi",
                esercizi.isNotEmpty()
            )
        }
    }

    @Test
    fun `il deload si puo' annullare e riporta le serie originali`() = runTest {
        val meso = mesocicloAttivo()
        val prima = serieTotali(meso)
        val id = executor.registerProposal(deload(meso, 40.0))
        executor.apply(id)
        val esito = executor.revert(id)
        assertTrue("L'annullamento deve riuscire: $esito", esito is ApplyResult.Reverted)
        assertTrue(
            "Annullando il deload il volume torna a $prima, trovato ${serieTotali(meso)}",
            serieTotali(meso) == prima
        )
    }
}
