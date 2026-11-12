package com.fitcoach.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.ShoppingRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.ShoppingListEngine
import com.fitcoach.app.domain.usecase.GenerateProgramsUseCase
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.fitcoach.app.ui.shopping.ShoppingViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test d'integrazione REALE su database Room in-memory: esegue l'intera catena
 * generazione piano -> persistenza -> lista della spesa -> diario, senza mock.
 * Riproduce il bug segnalato ("Spesa: 0 di 0 alimenti" con un piano attivo).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NutritionChainIntegrationTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var shoppingRepository: ShoppingRepository
    private lateinit var diaryRepository: NutritionDiaryRepository
    private lateinit var generatePrograms: GenerateProgramsUseCase

    private val profile = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.BEGINNER, goal = Goal.RECOMP,
        trainingDaysPerWeek = 3
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        exerciseRepository = ExerciseRepository(db.exerciseDao())
        nutritionRepository = NutritionRepository(db)
        workoutRepository = WorkoutRepository(db)
        shoppingRepository = ShoppingRepository(db, nutritionRepository, foodRepository)
        diaryRepository = NutritionDiaryRepository(db)
        generatePrograms = GenerateProgramsUseCase(
            userRepository = userRepository,
            foodRepository = foodRepository,
            exerciseRepository = exerciseRepository,
            nutritionRepository = nutritionRepository,
            workoutRepository = workoutRepository,
            planVersionRepository = PlanVersionRepository(db.planVersionDao())
        )
        // Seed reale degli alimenti ed esercizi.
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        userRepository.save(profile)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `genera piano, persiste 7 giorni e la spesa NON e vuota`() = runTest {
        val result = generatePrograms(
            profile = profile,
            trainingDayIndices = WorkoutPlanBuilder.defaultTrainingDays(3),
            regenerateNutrition = true,
            regenerateWorkout = true,
            requireAnamnesis = false
        )
        assertThat(result).isInstanceOf(GenerateProgramsUseCase.Result.Success::class.java)

        val plan = nutritionRepository.getActivePlan()
        assertThat(plan).isNotNull()
        assertThat(plan!!.days).hasSize(7)
        assertThat(plan.days.all { it.meals.isNotEmpty() }).isTrue()

        val weekStart = NutritionDiaryRepository.weekStart(NutritionDiaryRepository.todayEpochDay())
        shoppingRepository.regenerate(weekStart)
        val items = shoppingRepository.itemsForWeek(weekStart)
        assertThat(items).isNotEmpty()

        // La somma delle quantita in spesa deve corrispondere agli alimenti del piano.
        val foodsById = foodRepository.getAll().associateBy { it.id }
        val expected = ShoppingListEngine.aggregate(plan, weekStart, foodsById)
        assertThat(items.map { it.foodItemId }.toSet())
            .isEqualTo(expected.map { it.foodItemId }.toSet())
    }

    @Test
    fun `profilo anamnesi confermata genera automaticamente entrambi i programmi`() = runTest {
        val confirmed = profile.copy(
            anamnesisCompleted = true,
            anamnesisConfirmedAt = System.currentTimeMillis(),
            anamnesisSummary = "Profilo confermato"
        )
        val result = generatePrograms(
            profile = confirmed,
            regenerateNutrition = true,
            regenerateWorkout = true,
            requireAnamnesis = true
        )

        assertThat(result).isInstanceOf(GenerateProgramsUseCase.Result.Success::class.java)
        assertThat(nutritionRepository.getActivePlan()).isNotNull()
        assertThat(workoutRepository.getActivePlan()).isNotNull()
    }


    @Test
    fun `la spesa conserva lo stato spuntato dopo la rigenerazione`() = runTest {
        generatePrograms(profile, regenerateNutrition = true, regenerateWorkout = false, requireAnamnesis = false)
        val weekStart = NutritionDiaryRepository.weekStart(NutritionDiaryRepository.todayEpochDay())
        shoppingRepository.regenerate(weekStart)
        val items = shoppingRepository.itemsForWeek(weekStart)
        val first = items.first()
        shoppingRepository.setChecked(first.id, true)

        shoppingRepository.regenerate(weekStart)
        val after = shoppingRepository.itemsForWeek(weekStart)
        val same = after.first { it.foodItemId == first.foodItemId }
        assertThat(same.checked).isTrue()
    }

    @Test
    fun `diario separa la stessa voce di piano tra date e impedisce duplicati`() = runTest {
        generatePrograms(profile, regenerateNutrition = true, regenerateWorkout = false, requireAnamnesis = false)
        val plan = nutritionRepository.getActivePlan()!!
        val mealEntry = plan.days.first { it.meals.isNotEmpty() }.meals.first().entries.first()
        val firstDay = NutritionDiaryRepository.todayEpochDay()
        val nextWeekSameDay = firstDay + 7

        diaryRepository.togglePlanEntry(mealEntry, MealSlot.BREAKFAST, "08:00", firstDay)
        diaryRepository.togglePlanEntry(mealEntry, MealSlot.BREAKFAST, "08:00", firstDay)
        diaryRepository.togglePlanEntry(mealEntry, MealSlot.BREAKFAST, "08:00", nextWeekSameDay)

        val firstEntries = diaryRepository.entriesForDay(firstDay)
        val nextEntries = diaryRepository.entriesForDay(nextWeekSameDay)
        assertThat(firstEntries).hasSize(1)
        assertThat(firstEntries.single().active).isFalse()
        assertThat(nextEntries).hasSize(1)
        assertThat(nextEntries.single().active).isTrue()
    }

    @Test
    fun `diario ricalcola quantita e bilancio solo sui consumi attivi`() = runTest {
        generatePrograms(profile, regenerateNutrition = true, regenerateWorkout = false, requireAnamnesis = false)
        val plan = nutritionRepository.getActivePlan()!!
        val meal = plan.days.first { it.meals.isNotEmpty() }.meals.first()
        val mealEntry = meal.entries.first()
        val date = NutritionDiaryRepository.todayEpochDay()

        diaryRepository.togglePlanEntry(mealEntry, meal.slot, meal.time, date)
        val original = diaryRepository.entriesForDay(date).single()
        diaryRepository.updateGrams(original.id, original.grams * 2)

        val updated = diaryRepository.entriesForDay(date).single()
        assertThat(updated.kcal).isWithin(0.001).of(original.kcal * 2)
        assertThat(updated.protein).isWithin(0.001).of(original.protein * 2)
        assertThat(diaryRepository.balanceForDay(date).consumedKcal)
            .isWithin(0.001).of(original.kcal * 2)

        diaryRepository.setActive(updated.id, false)
        assertThat(diaryRepository.balanceForDay(date).consumedKcal).isEqualTo(0.0)
    }


    @Test
    fun `il ViewModel Spesa si aggiorna quando il piano viene generato dopo l'apertura del tab`() = runTest {
        // Scenario del bug: l'utente apre il tab Spesa PRIMA di generare il piano.
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = ShoppingViewModel(shoppingRepository, nutritionRepository)
            advanceUntilIdle()
            assertThat(vm.state.value.items).isEmpty()

            // Ora genera il piano: il ViewModel deve reagire e popolare la lista.
            generatePrograms(profile, regenerateNutrition = true, regenerateWorkout = false, requireAnamnesis = false)
            advanceUntilIdle()

            assertThat(vm.state.value.items).isNotEmpty()
            assertThat(vm.state.value.total).isGreaterThan(0)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
