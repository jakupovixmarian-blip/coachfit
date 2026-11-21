package com.fitcoach.app.integration

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
import com.fitcoach.app.domain.coach.tools.ConsultInvictusTool
import com.fitcoach.app.domain.coach.tools.CoachToolRegistry
import com.fitcoach.app.domain.coach.tools.SubmitFullProgramTool
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.MealSlots
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.fitcoach.app.support.GenerationScript
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.SimulatedAiPrograms
import com.fitcoach.app.support.textResponse
import com.fitcoach.app.support.toolCallResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Anamnesi compilata per intero -> generazione -> verifica di coerenza.
 *
 * E' la catena che l'utente percorre davvero: 44 risposte, poi il Coach AI
 * produce nutrizione e allenamento, e ogni numero viene ricontrollato sui dati
 * reali (catalogo alimenti, catalogo esercizi, ciclicita dei carboidrati).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FullAnamnesisToProgramIntegrationTest {

    private val json = Json {
        ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false
    }

    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var planVersionRepository: PlanVersionRepository
    private lateinit var chatRepository: ChatRepository

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

    /** Il profilo nasce SOLO dalle risposte, come nell'onboarding reale. */
    private fun profileFromAnamnesis(): UserProfile {
        var p = UserProfile(id = 1L, name = "Prova")
        answers.forEach { (id, v) -> p = AnamnesisEngine.applyAnswer(p, id, v) }
        return p.copy(
            anamnesisCompleted = true,
            anamnesisConfirmedAt = 1L,
            anamnesisSummary = AnamnesisEngine.summary(p),
            answeredQuestionIds = answers.keys
        )
    }

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        exerciseRepository = ExerciseRepository(db.exerciseDao())
        nutritionRepository = NutritionRepository(db)
        workoutRepository = WorkoutRepository(db)
        planVersionRepository = PlanVersionRepository(db.planVersionDao())
        chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())

        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        chatRepository.saveAiSettings(
            AiProviderSettings(useBackendProxy = true, backendBaseUrl = "https://proxy.test/v1", model = "test-model")
        )
    }

    @After
    fun tearDown() = db.close()

    private fun service(api: AiApi, profile: UserProfile): AiProgramGenerationService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val submitTool: CoachTool = SubmitFullProgramTool(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, planVersionRepository, db, json
        )
        val orchestrator = CoachOrchestrator(
            AiClient(api, json), CoachToolRegistry(setOf(submitTool, ConsultInvictusTool())), json
        )
        return AiProgramGenerationService(
            orchestrator, chatRepository, SecureKeyStore(context), json, foodRepository, exerciseRepository
        )
    }

    private suspend fun proposalFor(profile: UserProfile): String {
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val exercises = exerciseRepository.getAll()
        val built = SimulatedAiPrograms.proposal(
            profile, foods, exercises,
            WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
        )
        return SimulatedAiPrograms.json(json, built)
    }

    @Test
    fun `anamnesi completa genera nutrizione e allenamento coerenti`() = runTest {
        val profile = profileFromAnamnesis()
        userRepository.save(profile)

        val api = ScriptedApi(
            GenerationScript.consultationsThen(
                toolCallResponse("submit_full_program", proposalFor(profile)),
                textResponse("Programma generato.")
            )
        )
        val result = service(api, profile).generate(profile)
        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Success::class.java)

        val plan = nutritionRepository.getActivePlan()!!
        val workout = workoutRepository.getActivePlan()!!

        // --- Nutrizione: struttura imposta dal profilo ---
        assertThat(plan.days).hasSize(7)
        val expectedSlots = MealSlots.forMealsPerDay(profile.mealsPerDay).toSet()
        plan.days.forEach { day ->
            assertThat(day.meals.map { it.slot }.toSet()).isEqualTo(expectedSlots)
            assertThat(day.meals).hasSize(profile.mealsPerDay)
            day.meals.forEach { meal ->
                assertThat(meal.entries).isNotEmpty()
                meal.entries.forEach { assertThat(it.grams).isGreaterThan(0.0) }
            }
            // Coerenza energetica: totale del giorno vicino al target calcolato.
            assertThat(kotlin.math.abs(day.actualKcal - day.targetKcal))
                .isAtMost(0.10 * day.targetKcal)
            // Le proteine sono il vincolo che non si negozia mai al ribasso.
            assertThat(day.actualProtein).isAtLeast(0.85 * day.targetProtein)
        }

        // --- Ciclicita dei carboidrati: i giorni di allenamento sono i piu carichi ---
        val trainingDayIndices = workout.days.filterNot { it.isRestDay }.map { it.dayIndex }.toSet()
        assertThat(trainingDayIndices).hasSize(profile.trainingDaysPerWeek)
        val kcalByDay = plan.days.associate { it.dayIndex to it.actualKcal }
        val trainingKcal = trainingDayIndices.map { kcalByDay[it]!! }.average()
        val restKcal = plan.days.filterNot { it.dayIndex in trainingDayIndices }
            .map { it.actualKcal }.average()
        assertThat(trainingKcal).isGreaterThan(restKcal)

        // --- Allenamento: struttura e copertura muscolare diretta ---
        val sessions = workout.days.filterNot { it.isRestDay }
        assertThat(sessions).hasSize(profile.trainingDaysPerWeek)
        val catalog = exerciseRepository.getAll().associateBy { it.id }
        val allPlanned = sessions.flatMap { it.exercises }
        assertThat(allPlanned).isNotEmpty()
        allPlanned.forEach { planned ->
            assertThat(catalog).containsKey(planned.exerciseId)
            assertThat(planned.sets).isIn(1..8)
            assertThat(planned.minReps).isAtMost(planned.maxReps)
        }
        val directSets = allPlanned.groupBy { it.primaryMuscle }.mapValues { (_, l) -> l.sumOf { it.sets } }
        assertThat(directSets[com.fitcoach.app.domain.model.MuscleGroup.BICEPS] ?: 0).isAtLeast(3)
        assertThat(directSets[com.fitcoach.app.domain.model.MuscleGroup.TRICEPS] ?: 0).isAtLeast(3)

        // --- Vincoli alimentari del profilo ---
        val excluded = profile.excludedFoods.map { it.lowercase() }
        val allergens = profile.allergies.map { it.lowercase() }
        val servedNames = plan.days.flatMap { it.meals }.flatMap { it.entries }.map { it.foodName.lowercase() }
        assertThat(servedNames.filter { name -> excluded.any { name.contains(it) } }).isEmpty()
        assertThat(servedNames.filter { name -> allergens.any { name.contains(it) } }).isEmpty()
    }

    @Test
    fun `un piano che ignora il numero di pasti dichiarato non viene salvato`() = runTest {
        val profile = profileFromAnamnesis()
        userRepository.save(profile)

        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), profile)
        val base = SimulatedAiPrograms.proposal(
            profile, foods, exerciseRepository.getAll(),
            WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
        )
        // L'AI consegna 4 pasti su un profilo da 5: deve essere respinto.
        val wrong = base.copy(
            nutrition = base.nutrition.copy(
                days = base.nutrition.days.map { d ->
                    d.copy(meals = d.meals.filterNot { it.slot == "EVENING_SNACK" || it.slot == "AFTERNOON_SNACK" })
                }
            )
        )
        val api = ScriptedApi(
            GenerationScript.consultationsThen(
                toolCallResponse("submit_full_program", SimulatedAiPrograms.json(json, wrong)),
                textResponse("Correggo il numero di pasti.")
            )
        )
        val result = service(api, profile).generate(profile)

        assertThat(result).isInstanceOf(AiProgramGenerationService.Result.Failure::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNull()
        assertThat(workoutRepository.getActivePlan()).isNull()
    }
}
