package com.fitcoach.app.live

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.remote.AiApi
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
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SolveMealPortionsTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

/**
 * Prova REALE contro OpenRouter, con il codice di produzione.
 *
 * Non fa parte della suite automatica: richiede rete e una chiave in
 * `FITCOACH_LIVE_KEY`. Si esegue a mano:
 *
 *   FITCOACH_LIVE_KEY=sk-or-... ./gradlew testDebugUnitTest \
 *     --tests "com.fitcoach.app.live.OpenRouterLiveProbe"
 *
 * Verifica cio' che i test con provider scriptato non possono verificare: se il
 * modello reale consegna il programma entro i limiti dell'app (iterazioni,
 * timeout, budget token).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OpenRouterLiveProbe {

    private val model = System.getenv("FITCOACH_LIVE_MODEL") ?: "qwen/qwen3.5-35b-a3b"
    private val key = System.getenv("FITCOACH_LIVE_KEY") ?: ""

    private val json = Json {
        ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false
    }

    private val profile = UserProfile(
        id = 1L, name = "Prova", ageYears = 32, sex = Sex.MALE,
        heightCm = 180.0, weightKg = 82.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.RECOMP,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L,
        anamnesisSummary = "Anamnesi confermata per la prova live."
    )

    @Test
    fun `generazione reale su OpenRouter`() = runTest(timeout = 12.minutes) {
        assumeTrue(
            "Serve FITCOACH_LIVE_KEY: prova manuale, non parte nella suite automatica.",
            key.isNotBlank()
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val userRepo = UserRepository(db.userProfileDao())
            val foodRepo = FoodRepository(db.foodDao())
            val exerciseRepo = ExerciseRepository(db.exerciseDao())
            val nutritionRepo = NutritionRepository(db)
            val workoutRepo = WorkoutRepository(db)
            val planVersionRepo = PlanVersionRepository(db.planVersionDao())
            val chatRepo = ChatRepository(db.chatDao(), db.aiSettingsDao())

            db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
            db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
            userRepo.save(profile)
            chatRepo.saveAiSettings(
                AiProviderSettings(
                    provider = AiProvider.OPENROUTER,
                    baseUrl = "https://openrouter.ai/api/v1",
                    model = model,
                    maxTokens = 32_000,
                    useBackendProxy = false
                )
            )
            // Costruttore di test di SecureKeyStore: in Robolectric non esiste
            // l'Android Keystore, quindi l'archivio cifrato non e' utilizzabile.
            val prefs = context.getSharedPreferences("live_probe_keys", Context.MODE_PRIVATE)
            val keys = SecureKeyStore(context, prefs)
            keys.putApiKey(AiProvider.OPENROUTER.name, key)

            // Stessi timeout di produzione, ma la lettura e' alzata al tetto che
            // l'app concede al turno: serve a distinguere "lento" da "appeso".
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            val api = Retrofit.Builder()
                .baseUrl("https://openrouter.ai/api/v1/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AiApi::class.java)

            val tools: Set<CoachTool> = setOf(
                SubmitFullProgramTool(
                    userRepo, foodRepo, exerciseRepo, nutritionRepo,
                    workoutRepo, planVersionRepo, db, json
                ),
                SolveMealPortionsTool(userRepo, foodRepo)
            )
            val service = AiProgramGenerationService(
                CoachOrchestrator(AiClient(api, json), CoachToolRegistry(tools), json),
                chatRepo, keys, json, foodRepo, exerciseRepo
            )

            val started = System.currentTimeMillis()
            val result = service.generate(profile)
            val elapsed = (System.currentTimeMillis() - started) / 1000.0
            println("=== RISULTATO in ${elapsed}s: $result")
            when (result) {
                is AiProgramGenerationService.Result.Success -> {
                    println("=== piano=${result.nutritionPlanId} scheda=${result.workoutPlanId}")
                    println("=== giorni nutrizione=${nutritionRepo.getActivePlan()?.days?.size}")
                    println("=== sedute=${workoutRepo.getActivePlan()?.days?.count { !it.isRestDay }}")
                    println("=== avvisi=${result.warnings}")
                }
                is AiProgramGenerationService.Result.Failure ->
                    println("=== FALLIMENTO: ${result.message}")
            }
        } finally {
            db.close()
        }
    }
}
