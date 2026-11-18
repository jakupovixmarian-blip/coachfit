package com.fitcoach.app.collaudo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.CarbCyclingPlan
import com.fitcoach.app.domain.model.EnergyResult
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Collaudo di selezione, isolamento e CRUD dei piani salvati. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlanManagementCollaudoTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var nutrition: NutritionRepository
    private lateinit var diary: NutritionDiaryRepository
    private lateinit var workout: WorkoutRepository
    private lateinit var profile: UserProfile

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        nutrition = NutritionRepository(db)
        diary = NutritionDiaryRepository(db)
        workout = WorkoutRepository(db)
        profile = UserProfile(
            id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
            heightCm = 178.0, weightKg = 80.0, goal = Goal.RECOMP,
            trainingDaysPerWeek = 4, mealsPerDay = 5
        )
        UserRepository(db.userProfileDao()).save(profile)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun createNutritionPlan(): Long {
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)
        return nutrition.createManualPlan(
            profile = profile,
            energy = energy,
            cycling = cycling,
            dayTypes = CarbCyclingEngine.assignDayTypes(
                cycling.highDays, cycling.mediumDays, cycling.lowDays, listOf(0, 1, 2, 3)
            ),
            trainingDayIndices = listOf(0, 1, 2, 3)
        )
    }

    @Test
    fun `un solo piano alimentare attivo e il diario nasconde gli altri piani`() = runTest {
        val firstId = createNutritionPlan()
        val first = nutrition.getActivePlan()!!
        val firstMeal = first.days.first().meals.first()
        val food = com.fitcoach.app.domain.nutrition.FoodSeedData.foods.first()
        db.foodDao().insertAll(listOf(food.toEntity()))
        nutrition.addEntryToMeal(firstMeal.id, food, 100.0, FoodUnit.RAW)
        val firstEntry = nutrition.getDayWithMeals(first.days.first().id)!!.meals.first().entries.first()
        diary.togglePlanEntry(firstEntry, firstMeal.slot, firstMeal.time, NutritionDiaryRepository.todayEpochDay())

        val secondId = createNutritionPlan()
        assertThat(nutrition.getAllPlans().count { it.isActive }).isEqualTo(1)
        assertThat(nutrition.getActivePlan()!!.id).isEqualTo(secondId)
        assertThat(diary.entriesForDay(NutritionDiaryRepository.todayEpochDay())).isEmpty()

        nutrition.setActivePlan(firstId)
        assertThat(diary.entriesForDay(NutritionDiaryRepository.todayEpochDay()).map { it.planEntryId })
            .containsExactly(firstEntry.id)
    }

    @Test
    fun `piano alimentare si puo rinominare ed eliminare mantenendo un attivo`() = runTest {
        val firstId = createNutritionPlan()
        val secondId = createNutritionPlan()
        assertThat(nutrition.renamePlan(firstId, "Definizione estiva")).isTrue()
        assertThat(nutrition.getAllPlans().first { it.id == firstId }.name).isEqualTo("Definizione estiva")
        assertThat(nutrition.deletePlan(secondId)).isTrue()
        assertThat(nutrition.getAllPlans()).hasSize(1)
        assertThat(nutrition.getActivePlan()!!.id).isEqualTo(firstId)
    }

    @Test
    fun `scheda workout si puo modificare rinominare selezionare ed eliminare`() = runTest {
        val firstId = workout.savePlan(profile, builtWorkout("Prima"))
        val secondId = workout.savePlan(profile, builtWorkout("Seconda"))
        assertThat(workout.getAllPlans().count { it.isActive }).isEqualTo(1)
        assertThat(workout.getActivePlan()!!.id).isEqualTo(secondId)

        assertThat(workout.updatePlan(firstId, profile, builtWorkout("Aggiornata"))).isTrue()
        assertThat(workout.getActivePlan()!!.id).isEqualTo(firstId)
        assertThat(workout.getActivePlan()!!.days.first { !it.isRestDay }.exercises).hasSize(1)
        assertThat(workout.renamePlan(firstId, "Scheda forza")).isTrue()
        assertThat(workout.getAllPlans().first { it.id == firstId }.name).isEqualTo("Scheda forza")
        assertThat(workout.deletePlan(firstId)).isTrue()
        assertThat(workout.getAllPlans()).hasSize(1)
        assertThat(workout.getActivePlan()!!.id).isEqualTo(secondId)
    }

    private fun builtWorkout(label: String): WorkoutPlanBuilder.BuiltPlan {
        val exercise = PlannedExercise(
            exerciseId = 1L,
            exerciseName = label,
            primaryMuscle = MuscleGroup.CHEST,
            orderIndex = 0,
            sets = 3,
            minReps = 8,
            maxReps = 12,
            restSeconds = 120,
            targetRir = 2,
            category = ExerciseCategory.COMPOUND
        )
        return WorkoutPlanBuilder.BuiltPlan(
            splitType = SplitType.CUSTOM,
            days = listOf(
                WorkoutDay(dayIndex = 0, title = label, isRestDay = false, exercises = listOf(exercise)),
                WorkoutDay(dayIndex = 1, title = "Riposo", isRestDay = true)
            ),
            weeklyVolumePerGroup = mapOf(MuscleGroup.CHEST to 3),
            notes = emptyList()
        )
    }
}
