package com.fitcoach.app.collaudo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.EnergyResult
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.notifications.ReminderReceiver
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Collaudo delle azioni delle notifiche: deep link e conferma reale del pasto. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationActionCollaudoTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var nutrition: NutritionRepository
    private lateinit var diary: NutritionDiaryRepository
    private lateinit var profile: UserProfile

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        nutrition = NutritionRepository(db)
        diary = NutritionDiaryRepository(db)
        profile = UserProfile(
            id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
            heightCm = 178.0, weightKg = 80.0, goal = Goal.RECOMP,
            trainingDaysPerWeek = 4, mealsPerDay = 5
        )
        UserRepository(db.userProfileDao()).save(profile)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `ogni tipo di promemoria apre la schermata corretta`() {
        assertThat(ReminderReceiver.deepLinkRouteFor("MEAL_BREAKFAST")).isEqualTo("diary")
        assertThat(ReminderReceiver.deepLinkRouteFor("MEAL_DINNER")).isEqualTo("diary")
        assertThat(ReminderReceiver.deepLinkRouteFor("WORKOUT")).isEqualTo("workout")
        assertThat(ReminderReceiver.deepLinkRouteFor("CHECKIN")).isEqualTo("progress")
        assertThat(ReminderReceiver.deepLinkRouteFor("WEIGHT")).isEqualTo("progress")
        assertThat(ReminderReceiver.deepLinkRouteFor("HYDRATION")).isEqualTo("dashboard")
    }

    @Test
    fun `confermare il pasto dalla notifica registra le voci previste nel diario`() = runTest {
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)
        nutrition.createManualPlan(
            profile = profile,
            energy = energy,
            cycling = cycling,
            dayTypes = CarbCyclingEngine.assignDayTypes(
                cycling.highDays, cycling.mediumDays, cycling.lowDays, listOf(0, 1, 2, 3)
            ),
            trainingDayIndices = listOf(0, 1, 2, 3)
        )
        val plan = nutrition.getActivePlan()!!
        val day = plan.days.first()
        val meal = day.meals.first { it.slot == MealSlot.BREAKFAST }
        val food = FoodSeedData.foods.first()
        db.foodDao().insertAll(listOf(food.toEntity()))
        nutrition.addEntryToMeal(meal.id, food, 100.0, com.fitcoach.app.domain.model.FoodUnit.RAW)
        val entry = nutrition.getDayWithMeals(day.id)!!.meals
            .first { it.slot == MealSlot.BREAKFAST }.entries.first()

        val epochDay = NutritionDiaryRepository.todayEpochDay()
        diary.confirmPlanEntry(entry, MealSlot.BREAKFAST, meal.time, epochDay)

        val logged = diary.entriesForDay(epochDay)
        assertThat(logged.map { it.planEntryId }).containsExactly(entry.id)

        // Confermare due volte non duplica la voce ne inverte lo stato.
        diary.confirmPlanEntry(entry, MealSlot.BREAKFAST, meal.time, epochDay)
        assertThat(diary.entriesForDay(epochDay)).hasSize(1)
    }
}
