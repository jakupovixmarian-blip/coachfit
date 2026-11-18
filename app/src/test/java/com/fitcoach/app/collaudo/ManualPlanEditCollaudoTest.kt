package com.fitcoach.app.collaudo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.entity.MealEntity
import com.fitcoach.app.data.local.entity.NutritionDayEntity
import com.fitcoach.app.data.local.entity.NutritionPlanEntity
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.MealSlot
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Collaudo della modifica manuale del piano alimentare (richiesta utente):
 * l'utente deve poter aggiungere e rimuovere alimenti senza rigenerare il piano.
 *
 * Verifica che i macronutrienti vengano ricalcolati dai grammi reali e che il
 * pasto rifletta immediatamente le modifiche.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ManualPlanEditCollaudoTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var repo: NutritionRepository

    private val pollo = FoodItem(
        id = 0, name = "Petto di pollo", category = "Carne",
        kcalPer100g = 110.0, proteinPer100g = 23.3, carbsPer100g = 0.0,
        fatPer100g = 1.7, source = "CREA",
    )

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = NutritionRepository(db)
        db.foodDao().insertAll(listOf(pollo.toEntity()))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seedPlanWithMeal(): Pair<Long, Long> {
        val planId = db.nutritionPlanDao().insert(
            NutritionPlanEntity(
                profileId = 1L, name = "Test", isActive = true,
                bmr = 1700.0, tdee = 2500.0, weeklyCalories = 17500.0,
                weeklyKcalBalance = 0.0, bmrFormula = "Mifflin", tdeeFormula = "Katch",
                highCarbDays = 2, mediumCarbDays = 3, lowCarbDays = 2, proteinGPerKg = 2.0
            )
        )
        val dayId = db.nutritionDayDao().insertAll(
            listOf(
                NutritionDayEntity(
                    planId = planId, dayIndex = 0, dayType = CarbDayType.MEDIUM,
                    targetKcal = 2000.0, targetProtein = 160.0, targetCarbs = 200.0, targetFat = 60.0
                )
            )
        ).first()
        val mealId = db.mealDao().insertAll(
            listOf(MealEntity(nutritionDayId = dayId, slot = MealSlot.LUNCH, time = "13:00"))
        ).first()
        return dayId to mealId
    }

    @Test
    fun `aggiungere un alimento calcola i macro dai grammi`() = runBlocking {
        val (dayId, mealId) = seedPlanWithMeal()
        val food = db.foodDao().getByName("Petto di pollo")!!.let { repoFood(it.id) }

        repo.addEntryToMeal(mealId, food, grams = 200.0, unit = FoodUnit.RAW)

        val day = repo.getDayWithMeals(dayId)!!
        val meal = day.meals.first { it.slot == MealSlot.LUNCH }
        assertThat(meal.entries).hasSize(1)
        val entry = meal.entries.first()
        assertThat(entry.grams).isEqualTo(200.0)
        // 110 kcal/100 g * 2 = 220 kcal
        assertThat(entry.kcal).isWithin(0.5).of(220.0)
        // 23.3 g proteine/100 g * 2 = 46.6 g
        assertThat(entry.protein).isWithin(0.1).of(46.6)
        assertThat(entry.fat).isWithin(0.1).of(3.4)
    }

    @Test
    fun `modificare i grammi ricalcola i macro dai valori dell alimento`() = runBlocking {
        val (dayId, mealId) = seedPlanWithMeal()
        val food = repoFood(db.foodDao().getByName("Petto di pollo")!!.id)
        repo.addEntryToMeal(mealId, food, grams = 150.0, unit = FoodUnit.RAW)

        val entry = repo.getDayWithMeals(dayId)!!.meals.first { it.slot == MealSlot.LUNCH }.entries.first()
        repo.replaceMealEntry(
            entryId = entry.id,
            grams = 250.0,
            kcal = food.kcalPer100g * 2.5,
            protein = food.proteinPer100g * 2.5,
            carbs = food.carbsPer100g * 2.5,
            fat = food.fatPer100g * 2.5
        )

        val updated = repo.getDayWithMeals(dayId)!!.meals.first { it.slot == MealSlot.LUNCH }.entries.first()
        assertThat(updated.grams).isEqualTo(250.0)
        assertThat(updated.kcal).isWithin(0.5).of(275.0)
        assertThat(updated.protein).isWithin(0.1).of(58.25)
    }

    @Test
    fun `rimuovere un alimento lo elimina dal pasto`() = runBlocking {
        val (dayId, mealId) = seedPlanWithMeal()
        val food = repoFood(db.foodDao().getByName("Petto di pollo")!!.id)
        repo.addEntryToMeal(mealId, food, grams = 150.0, unit = FoodUnit.RAW)

        val before = repo.getDayWithMeals(dayId)!!.meals.first { it.slot == MealSlot.LUNCH }
        assertThat(before.entries).hasSize(1)

        repo.removeEntry(before.entries.first())

        val after = repo.getDayWithMeals(dayId)!!.meals.first { it.slot == MealSlot.LUNCH }
        assertThat(after.entries).isEmpty()
    }

    private suspend fun repoFood(id: Long): FoodItem =
        db.foodDao().getById(id)!!.toDomain()
}
