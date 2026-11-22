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
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.EquipmentFit
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Verifica contro un MODELLO REALE che il vincolo sull'attrezzatura non sia
 * solo una frase nel prompt.
 *
 * I test con `ScriptedApi` dimostrano che il prompt contiene la regola, non che
 * un modello la rispetti: un LLM puo' ignorare qualunque istruzione. Questo
 * test chiude il dubbio su due fronti:
 *
 * 1. il modello riceve una lista di esercizi GIA' filtrata (non puo' vedere
 *    esercizi incompatibili, quindi non puo' prescriverli);
 * 2. se ne inventa uno comunque, la consegna viene RIFIUTATA: il programma non
 *    arriva nel database.
 *
 * E' opt-in: senza `OPENROUTER_API_KEY` il test viene saltato, cosi' la suite
 * resta eseguibile offline e in CI senza credenziali. Per eseguirlo:
 *
 *   OPENROUTER_API_KEY=sk-or-... ./gradlew :app:testDebugUnitTest \
 *     --tests "*RealModelEquipmentFidelityTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RealModelEquipmentFidelityTest {

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
    private lateinit var secureKeyStore: SecureKeyStore

    /** Profilo con attrezzatura MINIMA: solo manubri e panca. */
    private val minimalGearProfile = UserProfile(
        id = 1L, name = "Reale", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 3, mealsPerDay = 5,
        availableEquipment = setOf(Equipment.DUMBBELL, Equipment.BENCH, Equipment.BODYWEIGHT),
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata",
        // Senza questo il riepilogo anamnesi rende OGNI campo "NON_DISPONIBILE":
        // il modello riceverebbe un profilo vuoto e genererebbe per un utente
        // inesistente, rendendo verde un test che non prova nulla di reale.
        answeredQuestionIds = setOf(
            "age", "sex", "heightCm", "weightKg", "bodyFatPercent", "goal", "experience",
            "trainingStatus", "trainingDaysPerWeek", "technicalLevel", "availableEquipment",
            "mealsPerDay", "breakfastPreference", "recoveryCapacity", "sleepQuality",
            "stressLevel", "dailyEnergy"
        )
    )

    private val apiKey: String? =
        (System.getenv("OPENROUTER_API_KEY") ?: System.getProperty("OPENROUTER_API_KEY"))
            ?.takeIf { it.isNotBlank() }

    /** Scope da testare: BOTH (default) o WORKOUT/NUTRITION, per isolare i fallimenti. */
    private val scopeName: String =
        (System.getenv("REAL_AI_SCOPE") ?: System.getProperty("REAL_AI_SCOPE"))
            ?.takeIf { it.isNotBlank() } ?: "BOTH"

    private val model: String =
        (System.getenv("REAL_AI_MODEL") ?: System.getProperty("REAL_AI_MODEL"))
            ?.takeIf { it.isNotBlank() } ?: "openai/gpt-4o-mini"

    @Before
    fun setUp() {
        runBlocking {
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
        userRepository.save(minimalGearProfile)
        chatRepository.saveAiSettings(
            AiProviderSettings(
                provider = AiProvider.OPENROUTER,
                baseUrl = "https://openrouter.ai/api/v1",
                model = model,
                useBackendProxy = false
            )
        )
        // Preferenze in memoria: l'EncryptedSharedPreferences non e'
        // disponibile in una JVM, e senza questa istanza la chiave non veniva
        // mai letta (il test falliva con "Provider AI non configurato").
        val prefs = context.getSharedPreferences("real-model-fidelity", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        secureKeyStore = SecureKeyStore(context, prefs)
        apiKey?.let { secureKeyStore.putApiKey(AiProvider.OPENROUTER.name, it) }
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun service(): AiProgramGenerationService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val api = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(com.fitcoach.app.data.remote.AiApi::class.java)

        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val registry = CoachToolRegistry(setOf(submitTool, ConsultInvictusTool()))
        val orchestrator = CoachOrchestrator(AiClient(api, json), registry, json)
        return AiProgramGenerationService(
            orchestrator, chatRepository, secureKeyStore, json,
            foodRepository, exerciseRepository
        )
    }

    @Test
    fun `il modello vede solo esercizi compatibili con la sua attrezzatura`() {
        // Nessuna chiamata di rete: verifica il testo esatto che il modello
        // riceve. Gira sempre, anche senza chiave.
        runBlocking {
        val allowed = EquipmentFit.allowedFor(minimalGearProfile, exerciseRepository.getAll())
        val context = service().buildGenerationContext(
            minimalGearProfile,
            FoodFilter.allowedFoods(foodRepository.getAll(), minimalGearProfile),
            allowed,
            ProgramGenerationEngine.Scope.BOTH
        )

        // Nessun esercizio che richieda attrezzi assenti deve comparire nel testo
        // che il modello legge.
        val forbidden = ExerciseSeedData.exercises.filter {
            !EquipmentFit.isAllowed(it, minimalGearProfile)
        }
        assertThat(forbidden).isNotEmpty()

        val leaked = forbidden.filter { context.contains(it.name) }
        assertThat(leaked.map { it.name }).isEmpty()

        // Il contesto deve trasportare i dati REALI del profilo: con
        // answeredQuestionIds vuoto il riepilogo rende tutto "NON_DISPONIBILE"
        // e il modello genererebbe per un utente inesistente, facendo passare
        // il test senza aver provato nulla.
        assertThat(context).doesNotContain("eta=NON_DISPONIBILE")
        assertThat(context).contains("eta=30")
        assertThat(context).contains("peso=80")
        }
    }

    @Test
    fun `la generazione reale produce una scheda eseguibile con l'attrezzatura dichiarata`() {
        assumeTrue("OPENROUTER_API_KEY assente: test reale saltato", !apiKey.isNullOrBlank())
        runBlocking {
            val scope = ProgramGenerationEngine.Scope.valueOf(scopeName)
            val result = service().generate(minimalGearProfile, scope)

            if (result is AiProgramGenerationService.Result.Failure) {
                println(
                    "Generazione fallita: ${result.message} | finish=${result.lastFinishReason} " +
                        "| strumenti=${result.toolTrace}"
                )
            }
            assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Success::class.java)
            val success = result as AiProgramGenerationService.Result.Success

            val days = workoutRepository.getActiveDays()
            assertThat(days).isNotEmpty()

            val planned = db.plannedExerciseDao().getForDays(days.map { it.id })
            assertThat(planned).isNotEmpty()

            // Ogni esercizio prescritto deve essere eseguibile con l'attrezzatura
            // dichiarata: e' il punto che il modello non puo' aggirare.
            val byName = ExerciseSeedData.exercises.associateBy { it.name }
            val violations = planned.mapNotNull { p ->
                val ex = byName[p.exerciseName] ?: return@mapNotNull "${p.exerciseName}: fuori catalogo"
                if (!EquipmentFit.isAllowed(ex, minimalGearProfile)) {
                    "${p.exerciseName}: incompatibile (${ex.equipment.joinToString()})"
                } else null
            }
            assertThat(violations).isEmpty()

            assertThat(success.workoutPlanId).isGreaterThan(0L)
        }
    }
}
