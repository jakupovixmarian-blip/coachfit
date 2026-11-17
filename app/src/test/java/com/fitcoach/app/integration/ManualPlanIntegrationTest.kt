package com.fitcoach.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Piano alimentare MANUALE (senza AI) e scelta del piano attivo.
 *
 * Verifica su Room reale che:
 *  - il piano manuale venga creato con 7 giorni, tipi di giornata (carb cycling)
 *    e target coerenti con i motori deterministici (nessun valore inventato);
 *  - i pasti siano VUOTI: l'utente li compila a mano;
 *  - l'utente possa compilare un pasto (aggiunta alimento) e registrare un EXTRA
 *    nel diario, con l'eccesso rispetto al target calcolato correttamente;
 *  - si possa passare da un piano all'altro rendendo attivo quello scelto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ManualPlanIntegrationTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var diaryRepository: NutritionDiaryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepository: FoodRepository

    private val profile = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 178.0, weightKg = 80.0, trainingDaysPerWeek = 4, mealsPerDay = 5
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        nutritionRepository = NutritionRepository(db)
        diaryRepository = NutritionDiaryRepository(db)
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        userRepository.save(profile)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun createManualPlan(): Long {
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)
        val dayTypes = CarbCyclingEngine.assignDayTypes(
            highDays = cycling.highDays,
            mediumDays = cycling.mediumDays,
            lowDays = cycling.lowDays,
            trainingDayIndices = (0 until profile.trainingDaysPerWeek).toList()
        )
        return nutritionRepository.createManualPlan(
            profile = profile,
            energy = energy,
            cycling = cycling,
            dayTypes = dayTypes,
            trainingDayIndices = (0 until profile.trainingDaysPerWeek).toList()
        )
    }

    @Test
    fun `il piano manuale ha sette giorni con carb cycling e pasti vuoti`() = runTest {
        val planId = createManualPlan()
        val plan = nutritionRepository.getActivePlan()!!

        assertThat(plan.id).isEqualTo(planId)
        assertThat(plan.days).hasSize(7)
        assertThat(plan.days.map { it.dayType }).containsAtLeast(
            com.fitcoach.app.domain.model.CarbDayType.HIGH,
            com.fitcoach.app.domain.model.CarbDayType.LOW
        )
        // I pasti sono creati ma VUOTI: nessun alimento inventato.
        plan.days.forEach { day ->
            assertThat(day.meals).isNotEmpty()
            assertThat(day.meals.all { it.entries.isEmpty() }).isTrue()
        }
        // I target sono calcolati (non zero) e coerenti con i macro entro tolleranza.
        assertThat(plan.days.all { it.targetKcal > 0.0 }).isTrue()
        val day = plan.days.first()
        val macroKcal = day.targetProtein * 4 + day.targetCarbs * 4 + day.targetFat * 9
        assertThat(kotlin.math.abs(macroKcal - day.targetKcal) / day.targetKcal).isLessThan(0.10)
    }

    @Test
    fun `compilare un pasto e registrare un extra produce il bilancio corretto`() = runTest {
        createManualPlan()
        val plan = nutritionRepository.getActivePlan()!!
        val today = NutritionDiaryRepository.todayEpochDay()
        val dayIndex = com.fitcoach.app.domain.nutrition.NutritionDiaryEngine.dayIndexFor(today)
        val day = plan.days.first { it.dayIndex == dayIndex }
        val meal = day.meals.first()

        // L'utente compila a mano il primo pasto con un alimento reale.
        val food = foodRepository.searchNormalized("pollo").first()
        nutritionRepository.addEntryToMeal(meal.id, food, grams = 200.0, unit = FoodUnit.RAW)

        // E registra un EXTRA fuori piano nel diario.
        diaryRepository.addExtraFood(food, grams = 100.0, slot = meal.slot, time = meal.time, epochDay = today)

        val balance = diaryRepository.balanceForDay(today)
        val expectedExtra = food.kcalPer100g * 1.0
        assertThat(balance.consumedKcal).isWithin(1.0).of(expectedExtra)
        // Il target resta quello del piano (nessuna cancellazione, nessun valore inventato).
        assertThat(balance.targetKcal).isEqualTo(day.targetKcal)
    }

    @Test
    fun `l eccesso rispetto al target non viene nascosto`() = runTest {
        createManualPlan()
        val plan = nutritionRepository.getActivePlan()!!
        val today = NutritionDiaryRepository.todayEpochDay()
        val dayIndex = com.fitcoach.app.domain.nutrition.NutritionDiaryEngine.dayIndexFor(today)
        val day = plan.days.first { it.dayIndex == dayIndex }
        val food = foodRepository.searchNormalized("olio").first()
        // 1000 g di un alimento molto calorico: si supera di sicuro il target giornaliero.
        diaryRepository.addExtraFood(food, grams = 1000.0, slot = day.meals.first().slot, time = "10:00", epochDay = today)
        val balance = diaryRepository.balanceForDay(today)
        assertThat(balance.consumedKcal).isGreaterThan(day.targetKcal)
        assertThat(balance.kcalExceeded).isGreaterThan(0.0)
        assertThat(balance.kcalWithinTarget).isFalse()
    }

    @Test
    fun `si puo passare da un piano manuale a uno generato e viceversa`() = runTest {
        val manualId = createManualPlan()
        // Secondo piano (simula una rigenerazione AI): creato con lo stesso percorso manuale
        // per isolare la sola logica di attivazione.
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)
        val secondId = nutritionRepository.createManualPlan(
            profile = profile,
            energy = energy,
            cycling = cycling,
            dayTypes = CarbCyclingEngine.assignDayTypes(cycling.highDays, cycling.mediumDays, cycling.lowDays, listOf(0, 1, 2, 3)),
            trainingDayIndices = listOf(0, 1, 2, 3)
        )

        assertThat(nutritionRepository.getAllPlans()).hasSize(2)
        assertThat(nutritionRepository.getActivePlan()!!.id).isEqualTo(secondId)

        // Torno al primo piano: deve diventare l'unico attivo.
        nutritionRepository.setActivePlan(manualId)
        val active = nutritionRepository.getActivePlan()!!
        assertThat(active.id).isEqualTo(manualId)
        assertThat(nutritionRepository.getAllPlans().count { it.isActive }).isEqualTo(1)
    }
}
