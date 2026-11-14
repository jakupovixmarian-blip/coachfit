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
import kotlinx.coroutines.async
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
    fun `checkbox piano persiste il consumo e aggiorna il bilancio macro`() = runTest {
        generatePrograms(profile, regenerateNutrition = true, regenerateWorkout = false, requireAnamnesis = false)
        val plan = nutritionRepository.getActivePlan()!!
        val meal = plan.days.first { it.meals.isNotEmpty() }.meals.first()
        val entry = meal.entries.first()
        val date = NutritionDiaryRepository.todayEpochDay()

        diaryRepository.togglePlanEntry(entry, meal.slot, meal.time, date)
        val checked = diaryRepository.entriesForDay(date).single()
        assertThat(checked.active).isTrue()
        assertThat(diaryRepository.balanceForDay(date).consumedKcal)
            .isWithin(0.001).of(entry.kcal)
        assertThat(diaryRepository.balanceForDay(date).consumedProtein)
            .isWithin(0.001).of(entry.protein)

        diaryRepository.togglePlanEntry(entry, meal.slot, meal.time, date)
        assertThat(diaryRepository.entriesForDay(date).single().active).isFalse()
        assertThat(diaryRepository.balanceForDay(date).consumedKcal).isEqualTo(0.0)
    }

    @Test
    fun `calendario scambia seduta e riposo senza rigenerare il piano`() = runTest {
        generatePrograms(
            profile,
            trainingDayIndices = listOf(0, 2, 4),
            regenerateNutrition = false,
            regenerateWorkout = true,
            requireAnamnesis = false
        )
        val before = workoutRepository.getActiveDays()
        val monday = before.first { it.dayIndex == 0 }
        val wednesday = before.first { it.dayIndex == 2 }
        val planId = workoutRepository.getActivePlan()!!.id

        assertThat(workoutRepository.moveWorkoutDay(monday.id, 6)).isTrue()
        val after = workoutRepository.getActiveDays()
        assertThat(after.first { it.id == monday.id }.dayIndex).isEqualTo(6)
        assertThat(after.first { it.id == wednesday.id }.dayIndex).isEqualTo(2)
        assertThat(workoutRepository.getActivePlan()!!.id).isEqualTo(planId)
    }



    @Test
    fun `tre profili producono percorsi diversi in base a esperienza obiettivo e disponibilita`() = runTest {
        val beginner = profile.copy(
            experience = TrainingExperience.BEGINNER,
            goal = Goal.RECOMP,
            trainingDaysPerWeek = 2,
            availableEquipment = emptySet()
        )
        generatePrograms(beginner, regenerateNutrition = true, regenerateWorkout = true, requireAnamnesis = false)
        val beginnerWorkout = workoutRepository.getActivePlan()!!
        val beginnerNutrition = nutritionRepository.getActivePlan()!!

        val advanced = profile.copy(
            experience = TrainingExperience.ADVANCED,
            goal = Goal.HYPERTROPHY,
            trainingDaysPerWeek = 5,
            availableEquipment = com.fitcoach.app.domain.model.Equipment.entries.toSet(),
            priorityMuscles = setOf(com.fitcoach.app.domain.model.MuscleGroup.CHEST)
        )
        generatePrograms(advanced, regenerateNutrition = true, regenerateWorkout = true, requireAnamnesis = false)
        val advancedWorkout = workoutRepository.getActivePlan()!!
        val advancedNutrition = nutritionRepository.getActivePlan()!!

        assertThat(beginnerWorkout.splitType).isNotEqualTo(advancedWorkout.splitType)
        assertThat(beginnerWorkout.days.count { !it.isRestDay }).isEqualTo(2)
        assertThat(advancedWorkout.days.count { !it.isRestDay }).isEqualTo(5)
        assertThat(advancedNutrition.weeklyCalories).isGreaterThan(beginnerNutrition.weeklyCalories)
        assertThat(advancedWorkout.days.filter { !it.isRestDay }.sumOf { it.totalSets })
            .isGreaterThan(beginnerWorkout.days.filter { !it.isRestDay }.sumOf { it.totalSets })
    }



    @Test
    fun `rigenerazioni concorrenti non lasciano la lista vuota`() = runTest {
        generatePrograms(profile, regenerateNutrition = true, regenerateWorkout = false, requireAnamnesis = false)
        val weekStart = NutritionDiaryRepository.weekStart(NutritionDiaryRepository.todayEpochDay())

        // Il bug: due rigenerazioni sovrapposte (collect del piano + load del
        // ViewModel) potevano lasciare la lista vuota per una corsa tra delete e insert.
        repeat(20) {
            val results = List(4) {
                async { shoppingRepository.regenerateAndRead(weekStart) }
            }.map { it.await() }
            results.forEach { result ->
                assertThat(result.items).isNotEmpty()
            }
            assertThat(shoppingRepository.itemsForWeek(weekStart)).isNotEmpty()
        }
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
            shoppingRepository.regenerate(NutritionDiaryRepository.weekStart(NutritionDiaryRepository.todayEpochDay()))
            vm.load()
            advanceUntilIdle()

            assertThat(vm.state.value.items).isNotEmpty()
            assertThat(vm.state.value.total).isGreaterThan(0)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `libreria aggiunge modifica riordina e rimuove esercizi senza rigenerare la scheda`() = runTest {
        generatePrograms(profile, regenerateNutrition = false, regenerateWorkout = true, requireAnamnesis = false)
        val day = workoutRepository.getActiveDays().first { !it.isRestDay }
        val before = day.exercises
        val exercise = exerciseRepository.getAll().first { candidate -> before.none { it.exerciseId == candidate.id } }

        assertThat(workoutRepository.addExerciseToDay(day.id, exercise)).isTrue()
        val added = workoutRepository.getDay(day.id)!!.exercises.last()
        assertThat(added.exerciseName).isEqualTo(exercise.name)
        assertThat(workoutRepository.updateExerciseTemplate(added.id, 4, 8, 12, 120, 2,
            com.fitcoach.app.domain.model.IntensityTechnique.DROP_SET)).isTrue()
        val updated = workoutRepository.getDay(day.id)!!.exercises.last()
        assertThat(updated.sets).isEqualTo(4)
        assertThat(updated.intensityTechnique).isEqualTo(com.fitcoach.app.domain.model.IntensityTechnique.DROP_SET)

        val originalOrder = workoutRepository.getDay(day.id)!!.exercises.map { it.id }
        val reversedOrder = originalOrder.reversed()
        workoutRepository.reorderExercises(day.id, reversedOrder)
        assertThat(workoutRepository.getDay(day.id)!!.exercises.map { it.id }).isEqualTo(reversedOrder)
        workoutRepository.removeExercise(added.id)
        assertThat(workoutRepository.getDay(day.id)!!.exercises.any { it.id == added.id }).isFalse()
    }


    @Test
    fun `override temporaneo scambia le sedute solo nella settimana corrente`() = runTest {
        generatePrograms(profile, regenerateNutrition = false, regenerateWorkout = true, requireAnamnesis = false)
        val days = workoutRepository.getActiveDays().sortedBy { it.dayIndex }
        val source = days.first { !it.isRestDay }
        val targetIndex = (source.dayIndex + 1) % 7
        val originalTarget = days.first { it.dayIndex == targetIndex }
        val weekStart = 2_000L

        assertThat(workoutRepository.saveScheduleOverride(source.id, targetIndex, weekStart, permanent = false)).isTrue()
        val current = workoutRepository.getActiveDays(weekStart)
        assertThat(current.first { it.id == source.id }.dayIndex).isEqualTo(targetIndex)
        assertThat(current.first { it.id == originalTarget.id }.dayIndex).isEqualTo(source.dayIndex)
        assertThat(workoutRepository.getActiveDays(weekStart + 7).first { it.id == source.id }.dayIndex)
            .isEqualTo(source.dayIndex)
    }

    @Test
    fun `sessione libera persiste esercizi e supporta rimozione`() = runTest {
        val sessionId = workoutRepository.startFreeSession(3_000L)
        val exercise = exerciseRepository.getAll().first()
        val added = workoutRepository.addExerciseToFreeSession(sessionId, exercise)

        assertThat(workoutRepository.getFreeExercises(sessionId)).containsExactly(added)
        assertThat(workoutRepository.updateExerciseTemplate(added.id, 4, 8, 12, 90, 2, com.fitcoach.app.domain.model.IntensityTechnique.NONE)).isTrue()
        workoutRepository.removeExercise(added.id)
        assertThat(workoutRepository.getFreeExercises(sessionId)).isEmpty()
    }


}
