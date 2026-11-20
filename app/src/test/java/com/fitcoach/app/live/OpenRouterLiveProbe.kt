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
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.CoachOrchestrator
import com.fitcoach.app.domain.coach.tools.CoachTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SolveMealPortionsTool
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.BreakfastConstraint
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import com.google.common.truth.Truth.assertWithMessage
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

    /**
     * Profilo costruito dalle 44 risposte dell'anamnesi, come nell'onboarding
     * reale: la prova live deve partire dagli stessi dati dell'utente.
     */
    private val answers = linkedMapOf(
        "age" to "34", "sex" to "MALE", "heightCm" to "178", "weightKg" to "81.5",
        "bodyFatPercent" to "19", "goal" to "HYPERTROPHY", "secondaryGoals" to "CUT",
        "targetTimelineWeeks" to "20", "workType" to "SEDENTARY", "workHoursPerDay" to "8",
        "dailySteps" to "7000", "transportMode" to "CAR", "workShift" to "NONE",
        "extraSportSessionsPerWeek" to "1", "maxSessionMinutes" to "75", "sleepHours" to "7",
        "sleepQuality" to "MEDIUM", "stressLevel" to "HIGH", "dailyEnergy" to "MEDIUM",
        "recoveryCapacity" to "MEDIUM", "mealsPerDay" to "5", "breakfastPreference" to "SWEET",
        "dietaryPreferences" to "MEDITERRANEAN", "likedFoods" to "Yogurt greco, Avena",
        "dislikedFoods" to "Fegato", "allergies" to "Arachidi", "excludedFoods" to "Maiale",
        "cookingTimeAvailable" to "MEDIUM", "mealPrepPossible" to "true",
        "monthlyFoodBudgetEur" to "320", "budgetScope" to "PERSONAL_ONLY", "budgetPersons" to "1",
        "trainingStatus" to "CURRENTLY_TRAINING", "experience" to "INTERMEDIATE",
        "trainingYears" to "4", "currentLoads" to "Panca piana=80, Squat=110",
        "trainingDaysPerWeek" to "4", "technicalLevel" to "COMPETENT",
        "availableEquipment" to "BARBELL, DUMBBELL, MACHINE, CABLE, BENCH",
        "preferredExercises" to "Panca piana", "dislikedExercises" to "Burpees",
        "priorityMuscles" to "CHEST, BACK", "physicalLimitations" to "Lombalgia lieve",
        "movementsToAvoid" to "Stacco con schiena curva"
    )

    private val profile: UserProfile = run {
        var p = UserProfile(id = 1L, name = "Prova")
        answers.forEach { (id, v) -> p = AnamnesisEngine.applyAnswer(p, id, v) }
        p.copy(
            anamnesisCompleted = true, anamnesisConfirmedAt = 1L,
            anamnesisSummary = AnamnesisEngine.summary(p),
            answeredQuestionIds = answers.keys
        )
    }

    @Test
    fun `generazione reale su OpenRouter`() = runBlocking {
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
            val failure = (result as? AiProgramGenerationService.Result.Failure)?.message
            assertWithMessage("generazione reale fallita: $failure")
                .that(result)
                .isInstanceOf(AiProgramGenerationService.Result.Success::class.java)

            val plan = nutritionRepo.getActivePlan()
            assertWithMessage("nessun piano nutrizionale salvato").that(plan).isNotNull()
            val days = plan!!.days
            assertWithMessage("giorni del piano").that(days).hasSize(7)
            assertWithMessage("pasti per giorno").that(days.map { it.meals.size }.toSet()).isEqualTo(setOf(5))

            // 1. Ogni pasto deve contenere alimenti reali con grammature positive.
            days.forEach { day ->
                day.meals.forEach { meal ->
                    assertWithMessage("pasto vuoto: giorno ${day.dayIndex} ${meal.slot}")
                        .that(meal.entries).isNotEmpty()
                    meal.entries.forEach { e ->
                        assertWithMessage("grammi non positivi: ${e.foodName}")
                            .that(e.grams).isGreaterThan(0.0)
                    }
                }
            }

            // 2. Coerenza energetica: i macro dichiarati devono tornare con le kcal.
            days.forEach { day ->
                day.meals.forEach { meal ->
                    meal.entries.forEach { e ->
                        // Le kcal da database non coincidono con 4-4-9 quando
                        // l'alimento ha fibre o alcoli: la tolleranza e' sul
                        // valore assoluto, non una percentuale stretta.
                        val fromMacros = e.protein * 4 + e.carbs * 4 + e.fat * 9
                        assertWithMessage("kcal incoerenti per ${e.foodName}: ${e.kcal} vs $fromMacros")
                            .that(kotlin.math.abs(fromMacros - e.kcal)).isLessThan(0.25 * e.kcal + 15.0)
                    }
                }
                // 3. Il totale del giorno deve restare vicino al target dichiarato.
                val delta = kotlin.math.abs(day.actualKcal - day.targetKcal)
                assertWithMessage(
                    "giorno ${day.dayIndex}: totale ${day.actualKcal} lontano dal target ${day.targetKcal}"
                ).that(delta).isAtMost(0.10 * day.targetKcal)
                assertWithMessage("giorno ${day.dayIndex}: proteine sotto il target")
                    .that(day.actualProtein).isAtLeast(0.85 * day.targetProtein)
            }

            // 4. La preferenza di colazione dichiarata deve essere rispettata.
            if (profile.breakfastPreference == BreakfastPreference.SWEET) {
                days.forEach { day ->
                    val breakfast = day.meals.first { it.slot == MealSlot.BREAKFAST }
                    val offending = breakfast.entries.filter { entry ->
                        val food = foodRepo.getAll().firstOrNull { it.name == entry.foodName }
                        food != null && BreakfastConstraint.violations(
                            BreakfastPreference.SWEET, MealSlot.BREAKFAST, listOf(food)
                        ).isNotEmpty()
                    }
                    assertWithMessage(
                        "colazione dolce violata il giorno ${day.dayIndex}: " +
                            offending.joinToString { it.foodName }
                    ).that(offending).isEmpty()
                }
            }

            // 5. La scheda deve coprire i gruppi senza lasciarli al solo stimolo indiretto.
            val workout = workoutRepo.getActivePlan()
            assertWithMessage("nessuna scheda salvata").that(workout).isNotNull()
            val sessions = workout!!.days.filter { !it.isRestDay }
            assertWithMessage("sedute attese ${profile.trainingDaysPerWeek}, trovate ${sessions.size}")
                .that(sessions).hasSize(profile.trainingDaysPerWeek)
            val catalog = exerciseRepo.getAll().associateBy { it.id }
            val direct = sessions.flatMap { it.exercises }
                .groupBy { it.primaryMuscle }
                .mapValues { (_, list) -> list.sumOf { it.sets } }
            listOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS).forEach { group ->
                assertWithMessage("nessun lavoro diretto per $group")
                    .that(direct[group] ?: 0).isAtLeast(3)
            }
            assertWithMessage("esercizi inesistenti nel catalogo")
                .that(sessions.flatMap { it.exercises }.filter { it.exerciseId !in catalog }).isEmpty()

            println("=== COERENZA VERIFICATA: 7 giorni, ${sessions.size} sedute, " +
                "dirette=$direct")
        } finally {
            db.close()
        }
    }
}
