package com.fitcoach.app.live

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.AiProviderConfig
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
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.CoachService
import com.fitcoach.app.domain.coach.tools.AdjustCaloriesTool
import com.fitcoach.app.domain.coach.tools.AdjustVolumeTool
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.ComputeCarbCyclingTool
import com.fitcoach.app.domain.coach.tools.ComputeEnergyTargetsTool
import com.fitcoach.app.domain.coach.tools.ComputeWeeklyVolumeTool
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.GetActiveNutritionPlanTool
import com.fitcoach.app.domain.coach.tools.GetActiveWorkoutPlanTool
import com.fitcoach.app.domain.coach.tools.GetAnamnesisTool
import com.fitcoach.app.domain.coach.tools.GetCatalogSummaryTool
import com.fitcoach.app.domain.coach.tools.GetNutritionDiaryTool
import com.fitcoach.app.domain.coach.tools.GetNutritionTargetsTool
import com.fitcoach.app.domain.coach.tools.GetProgressTool
import com.fitcoach.app.domain.coach.tools.GetWorkoutHistoryTool
import com.fitcoach.app.domain.coach.tools.ListExercisesTool
import com.fitcoach.app.domain.coach.tools.ListFoodsTool
import com.fitcoach.app.domain.coach.tools.MoveWorkoutTool
import com.fitcoach.app.domain.coach.tools.ProposeDeloadTool
import com.fitcoach.app.domain.coach.tools.SetIntensityTechniqueTool
import com.fitcoach.app.domain.coach.tools.SubstituteExerciseTool
import com.fitcoach.app.domain.coach.tools.SubstituteFoodTool
import com.fitcoach.app.domain.coach.CoachAction
import com.fitcoach.app.domain.coach.tools.ValidateNutritionDayTool
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.support.TestProgramGenerator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Collaudo LIVE del Coach AI su un provider reale (OpenRouter).
 *
 * I test con provider scriptato verificano il codice, non il modello. Qui si
 * misura cio' che l'utente vede davvero: se il Coach, davanti a una richiesta
 * di modifica, produce una PROPOSTA registrata e se, una volta confermata,
 * i dati nel database cambiano.
 *
 * Nasce da due segnalazioni reali:
 *  1. "L'AI non riesce a modificare i pasti: dice che un deficit del 10% non
 *     e' consentito."
 *  2. "Per la scheda dice di aver fatto le modifiche, ma la scheda non cambia."
 *
 * La prova e' manuale, perche' richiede rete e una chiave:
 *
 *   FITCOACH_LIVE_KEY=sk-or-... FITCOACH_LIVE_MODEL=qwen/qwen3.5-35b-a3b \
 *     ./gradlew :app:testDebugUnitTest --offline \
 *     --tests "com.fitcoach.app.live.CoachToolsLiveProbe"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoachToolsLiveProbe {

    private val model = System.getenv("FITCOACH_LIVE_MODEL") ?: "qwen/qwen3.5-35b-a3b"
    private val key = System.getenv("FITCOACH_LIVE_KEY") ?: ""

    private val json = Json {
        ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false
    }

    /** Profilo realistico: uomo in ricomposizione, 4 sedute, 5 pasti. */
    private fun profile(): UserProfile = UserProfile(
        id = 1L,
        name = "Collaudo",
        ageYears = 34,
        sex = Sex.MALE,
        heightCm = 178.0,
        weightKg = 81.5,
        bodyFatPercent = 19.0,
        goal = Goal.RECOMP,
        experience = TrainingExperience.INTERMEDIATE,
        trainingDaysPerWeek = 4,
        mealsPerDay = 5,
        anamnesisCompleted = true,
        anamnesisConfirmedAt = 1L
    )

    private class Env(
        val db: FitCoachDatabase,
        val coach: CoachService,
        val executor: CoachActionExecutor,
        val changeRepository: ChangeRepository,
        val nutritionRepository: NutritionRepository,
        val workoutRepository: WorkoutRepository
    )

    private suspend fun setup(): Env {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()

        val userRepo = UserRepository(db.userProfileDao())
        val foodRepo = FoodRepository(db.foodDao())
        val exerciseRepo = ExerciseRepository(db.exerciseDao())
        val nutritionRepo = NutritionRepository(db)
        val workoutRepo = WorkoutRepository(db)
        val planVersionRepo = PlanVersionRepository(db.planVersionDao())
        val chatRepo = ChatRepository(db.chatDao(), db.aiSettingsDao())
        val changeRepo = ChangeRepository(db.programChangeDao())
        val progressRepo = ProgressRepository(db.progressDao(), db.checkInDao())
        val diaryRepo = NutritionDiaryRepository(db)
        val shoppingRepo = ShoppingRepository(db, nutritionRepo, foodRepo)
        val substitutionRepo = SubstitutionRepository(db, nutritionRepo, foodRepo)

        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        val prof = profile()
        userRepo.save(prof)

        chatRepo.saveAiSettings(
            AiProviderSettings(
                provider = AiProvider.OPENROUTER,
                baseUrl = "https://openrouter.ai/api/v1",
                model = model,
                maxTokens = 32_000,
                useBackendProxy = false
            )
        )
        val prefs = context.getSharedPreferences("live_coach_keys", Context.MODE_PRIVATE)
        val keys = SecureKeyStore(context, prefs)
        keys.putApiKey(AiProvider.OPENROUTER.name, key)

        // Programma reale attraverso l'unico motore di produzione (provider scriptato):
        // serve un piano attivo su cui il modello possa proporre modifiche.
        val gen = TestProgramGenerator.generate(
            json = json, userRepository = userRepo, foodRepository = foodRepo,
            exerciseRepository = exerciseRepo, nutritionRepository = nutritionRepo,
            workoutRepository = workoutRepo, planVersionRepository = planVersionRepo,
            chatRepository = chatRepo, db = db, profile = prof, keyStore = keys
        )
        println("=== SETUP generazione: $gen")
        if (gen !is AiProgramGenerationService.Result.Success) {
            println("=== SETUP FALLITO: ${(gen as? AiProgramGenerationService.Result.Failure)?.message}")
        }
        println("=== SETUP piano attivo: ${nutritionRepo.getActivePlan()?.id} / workout: ${workoutRepo.getActivePlan()?.id}")

        val executor = CoachActionExecutor(
            db, changeRepo, userRepo,
            object : dagger.Lazy<ProgramGenerationCoordinator> {
                override fun get(): ProgramGenerationCoordinator =
                    error("rigenerazione non esercitata in questa prova")
            },
            json
        )

        val tools: Set<CoachTool> = setOf(
            GetAnamnesisTool(userRepo, foodRepo, exerciseRepo),
            GetCatalogSummaryTool(foodRepo, exerciseRepo),
            ListFoodsTool(foodRepo), ListExercisesTool(exerciseRepo),
            GetActiveNutritionPlanTool(nutritionRepo),
            GetActiveWorkoutPlanTool(workoutRepo),
            GetNutritionTargetsTool(userRepo),
            GetWorkoutHistoryTool(workoutRepo), GetProgressTool(progressRepo),
            GetNutritionDiaryTool(diaryRepo),
            ComputeEnergyTargetsTool(userRepo), ComputeCarbCyclingTool(userRepo),
            ComputeWeeklyVolumeTool(userRepo, workoutRepo),
            ValidateNutritionDayTool(foodRepo, userRepo),
            SubstituteFoodTool(executor), SubstituteExerciseTool(executor),
            SetIntensityTechniqueTool(executor), MoveWorkoutTool(executor),
            AdjustCaloriesTool(executor), AdjustVolumeTool(executor),
            ProposeDeloadTool(executor), ConsultInvictusTool()
        )
        val client = AiClient(openRouterApi(), json)
        val orchestrator = CoachOrchestrator(client, CoachToolRegistry(tools), json)
        val coach = CoachService(
            userRepo, nutritionRepo, workoutRepo, progressRepo, changeRepo, chatRepo,
            diaryRepo, shoppingRepo, substitutionRepo, foodRepo, client, keys, orchestrator
        )
        return Env(db, coach, executor, changeRepo, nutritionRepo, workoutRepo)
    }

    private fun openRouterApi(): AiApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AiApi::class.java)
    }

    private suspend fun pendingChanges(env: Env) =
        env.changeRepository.getAll().filter { it.status == ChangeStatus.PROPOSED }

    /** L'azione non e' una colonna: vive dentro il payload JSON della proposta. */
    private fun actionOf(change: com.fitcoach.app.domain.model.ProgramChange): String =
        runCatching {
            json.decodeFromString(CoachAction.serializer(), change.payloadJson).action
        }.getOrDefault("")

    /**
     * Bug 1 — il Coach deve poter proporre una riduzione calorica del 10%.
     *
     * Il rifiuto nasceva dal confronto fra il nuovo totale e il piano corrente
     * con un margine rigido, piu' un floor sul BMR. Un deficit del 10% chiesto
     * dall'utente e' legittimo e deve produrre una proposta applicabile.
     */
    @Test
    fun `il coach propone e applica una riduzione calorica del 10 percento`() = runBlocking {
        assumeTrue("Serve FITCOACH_LIVE_KEY", key.isNotBlank())
        val env = setup()
        try {
            val plan = env.nutritionRepository.getActivePlan()
            println("=== PIANO: id=${plan?.id} weekly=${plan?.weeklyCalories} bmr=${plan?.bmr} tdee=${plan?.tdee}")

            val reply = env.coach.send(
                "Il peso e' fermo da tre settimane. Riduci le calorie del piano attuale del 10%."
            )
            println("=== RISPOSTA COACH:\n${reply.text}")
            reply.providerError?.let { println("=== ERRORE PROVIDER: $it") }

            val proposals = pendingChanges(env)
            println("=== PROPOSTE IN ATTESA: ${proposals.map { actionOf(it) }}")

            val calorieProposal = proposals.firstOrNull { actionOf(it) == CoachAction.ACTION_ADJUST_CALORIES }
            if (calorieProposal != null) {
                val result = env.executor.apply(calorieProposal.id)
                println("=== APPLICAZIONE: $result")
            } else {
                println("=== NESSUNA PROPOSTA CALORICA: il modello ha risposto senza proporre")
            }
        } finally {
            env.db.close()
        }
    }

    /** Bug 2 — la sostituzione di un esercizio deve cambiare davvero la scheda. */
    @Test
    fun `il coach sostituisce un esercizio e la scheda cambia davvero`() = runBlocking {
        assumeTrue("Serve FITCOACH_LIVE_KEY", key.isNotBlank())
        val env = setup()
        try {
            val days = env.workoutRepository.getActiveDays()
            val before = env.db.plannedExerciseDao().getForDay(days.first().id)
            println("=== SCHEDA PRIMA: ${before.map { "${it.id}:${it.exerciseName}" }}")

            val reply = env.coach.send(
                "Non ho la panca inclinata. Sostituisci il primo esercizio del petto con un'altra " +
                    "variante di spinta che uso con manubri e panca."
            )
            println("=== RISPOSTA COACH:\n${reply.text}")

            val proposals = pendingChanges(env)
            println("=== PROPOSTE: ${proposals.map { "${actionOf(it)}:${it.title}" }}")

            val sub = proposals.firstOrNull { actionOf(it) == CoachAction.ACTION_SUBSTITUTE_EXERCISE }
            if (sub != null) {
                val result = env.executor.apply(sub.id)
                println("=== APPLICAZIONE: $result")
                val after = env.db.plannedExerciseDao().getForDay(days.first().id)
                println("=== SCHEDA DOPO: ${after.map { "${it.id}:${it.exerciseName}" }}")
                val changed = before.map { it.exerciseName } != after.map { it.exerciseName }
                println("=== SCHEDA CAMBIATA: $changed")
            } else {
                println("=== NESSUNA PROPOSTA DI SOSTITUZIONE")
            }
        } finally {
            env.db.close()
        }
    }

    /** Giro completo su tutti gli strumenti di scrittura: ogni richiesta deve produrre una proposta. */
    @Test
    fun `ogni tipo di modifica produce una proposta registrata`() = runBlocking {
        assumeTrue("Serve FITCOACH_LIVE_KEY", key.isNotBlank())
        val env = setup()
        try {
            val richieste = listOf(
                "Togli il riso dal pranzo e metti le patate, stessa quantita di carboidrati." to
                    CoachAction.ACTION_SUBSTITUTE_FOOD,
                "Sposta l'allenamento di domani a dopodomani." to CoachAction.ACTION_MOVE_WORKOUT,
                "Aggiungi un drop set sulla lat machine." to CoachAction.ACTION_SET_INTENSITY,
                "Il volume e' troppo alto, riducilo del 20%." to CoachAction.ACTION_ADJUST_VOLUME,
                "Mi sento stanco e le prestazioni calano: programmiamo un deload." to CoachAction.ACTION_DELOAD
            )
            richieste.forEach { (msg, atteso) ->
                val before = pendingChanges(env).map { it.id }.toSet()
                val reply = env.coach.send(msg)
                val nuove = pendingChanges(env).filterNot { it.id in before }
                val verdict = if (nuove.any { actionOf(it) == atteso }) "OK" else "MANCATA"
                println("=== [$verdict] atteso=$atteso nuove=${nuove.map { actionOf(it) }}")
                println("    testoVuoto=${reply.text.isBlank()} errore=${reply.providerError}")
                println("    finishReason=${reply.lastFinishReason} toolTrace=${reply.toolTrace}")
                println("    risposta: ${reply.text.take(400)}")
            }
        } finally {
            env.db.close()
        }
    }
}
