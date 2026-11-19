package com.fitcoach.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodCatalogSelector
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.MealPortionSolver
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.fitcoach.app.support.SimulatedAiPrograms
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
 * Regressione del bug "il programma AI non e valido: giorno N kcal: attese X,
 * ricevute ~0.6X".
 *
 * Causa: l'AI non riceveva i target e doveva centrare quattro macro a mano su
 * ~35 pasti; sbagliava sistematicamente per difetto. Ora sceglie solo gli
 * alimenti e [MealPortionSolver] calcola le grammature.
 *
 * Il test verifica che il percorso completo (scelta alimenti -> solver ->
 * validatore reale) produca un piano VALIDO per tutti i 7 giorni, su piu
 * profili (carb cycling attivo e non).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NutritionTargetCoherenceTest {

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

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        exerciseRepository = ExerciseRepository(db.exerciseDao())
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun profile(goal: Goal, days: Int, weight: Double = 80.0) = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = weight, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = goal,
        trainingDaysPerWeek = days, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    /** Verifica che ogni giornata della proposta centri il target del proprio dayType. */
    private suspend fun assertProposalIsValid(p: UserProfile) {
        userRepository.save(p)
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), p)
        val exercises = exerciseRepository.getAll()
        val trainingDays = WorkoutPlanBuilder.defaultTrainingDays(p.trainingDaysPerWeek)
        val proposal = SimulatedAiPrograms.proposal(p, foods, exercises, trainingDays)

        // Il validatore reale: se la proposta non e coerente restituisce null.
        val validated = ProgramGenerationEngine.validate(
            p, proposal, foods, exercises, ProgramGenerationEngine.Scope.BOTH
        )
        assertThat(validated).isNotNull()

        // E la diagnosi non deve riportare alcuno scostamento di macro.
        val errors = ProgramGenerationEngine.diagnose(p, proposal, foods, exercises)
        assertThat(errors.filter { it.contains("kcal") || it.contains("proteine") ||
            it.contains("carboidrati") || it.contains("grassi") }).isEmpty()
    }

    @Test
    fun `recomp with carb cycling produces a valid 7 day plan`() = runTest {
        assertProposalIsValid(profile(Goal.RECOMP, 4))
    }

    @Test
    fun `hypertrophy produces a valid 7 day plan`() = runTest {
        assertProposalIsValid(profile(Goal.HYPERTROPHY, 4))
    }

    @Test
    fun `cut produces a valid 7 day plan`() = runTest {
        assertProposalIsValid(profile(Goal.CUT, 3))
    }

    @Test
    fun `maintenance without carb cycling produces a valid plan`() = runTest {
        assertProposalIsValid(profile(Goal.MAINTENANCE, 3))
    }

    @Test
    fun `five training days produces a valid plan`() = runTest {
        assertProposalIsValid(profile(Goal.RECOMP, 5))
    }

    @Test
    fun `every day type present in the plan hits its own target`() = runTest {
        val p = profile(Goal.RECOMP, 4)
        userRepository.save(p)
        val foods = FoodFilter.allowedFoods(foodRepository.getAll(), p)
        val energy = EnergyCalculator.compute(p)!!
        val cycling = CarbCyclingEngine.build(p, energy)

        val proposal = SimulatedAiPrograms.proposal(
            p, foods, exerciseRepository.getAll(), WorkoutPlanBuilder.defaultTrainingDays(4)
        )

        proposal.nutrition.days.forEach { day ->
            val type = CarbDayType.valueOf(day.dayType)
            val target = cycling.targets[type]!!
            var kcal = 0.0
            var protein = 0.0
            day.meals.flatMap { it.entries }.forEach { e ->
                val food = foods.first { it.name == e.foodName }
                val f = e.grams / 100.0
                kcal += food.kcalPer100g * f
                protein += food.proteinPer100g * f
            }
            // Molto piu stretto della tolleranza del validatore (20-30%).
            assertThat(kotlin.math.abs(kcal - target.kcal) / target.kcal).isLessThan(0.05)
            assertThat(kotlin.math.abs(protein - target.protein) / target.protein).isLessThan(0.05)
        }
    }

    @Test
    fun `solver covers targets across all realistic body sizes`() = runTest {
        // Il solver non deve funzionare solo per il profilo di esempio.
        listOf(55.0, 65.0, 80.0, 95.0, 110.0).forEach { weight ->
            val p = profile(Goal.RECOMP, 4, weight)
            userRepository.save(p)
            val energy = EnergyCalculator.compute(p)!!
            val cycling = CarbCyclingEngine.build(p, energy)
            val foods = FoodCatalogSelector.forGeneration(foodRepository.getAll(), p)
            val carbs = foods.filter { MealPortionSolver.roleOf(it) == MealPortionSolver.Role.CARBS }
            val proteins = foods.filter { MealPortionSolver.roleOf(it) == MealPortionSolver.Role.PROTEIN }
            val fats = foods.filter { MealPortionSolver.roleOf(it) == MealPortionSolver.Role.FAT }

            assertThat(carbs).isNotEmpty()
            assertThat(proteins).isNotEmpty()
            assertThat(fats).isNotEmpty()

            cycling.targets.values.forEach { target ->
                val structure = (0 until 5).map { i ->
                    "MEAL$i" to listOf(carbs[i % carbs.size], proteins[i % proteins.size], fats[i % fats.size])
                }
                val solved = MealPortionSolver.solve(target, structure)
                assertThat(solved.ok).isTrue()
            }
        }
    }
}
