package com.fitcoach.app.collaudo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.MealPlanGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * L'utente segnala che dopo aver cambiato la distribuzione HIGH/MEDIUM/LOW
 * l'app dice "Distribuzione applicata al piano" ma i pasti restano identici.
 *
 * I test precedenti usavano piani con i pasti VUOTI (createManualPlan), quindi
 * verificavano solo il target del giorno: con zero alimenti non c'e' nulla da
 * riscalare e il difetto restava invisibile. Qui il piano contiene alimenti
 * veri, come un piano generato dal Coach AI.
 */
@RunWith(RobolectricTestRunner::class)
class CarbCyclingMealsRescaleTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var nutrition: NutritionRepository
    private lateinit var users: UserRepository

    private val profilo = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 178.0, weightKg = 78.0, bodyFatPercent = 15.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "ok"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        nutrition = NutritionRepository(db)
        users = UserRepository(db.userProfileDao())
    }

    @After
    fun tearDown() = db.close()

    private fun entry(
        nome: String, grammi: Double, kcal: Double, p: Double, c: Double, f: Double, id: Long
    ) =
        MealEntry(
            foodItemId = id, foodName = nome, grams = grammi, unit = FoodUnit.RAW,
            kcal = kcal, protein = p, carbs = c, fat = f
        )

    /** Piano con alimenti reali, come quelli prodotti dal Coach AI. */
    private suspend fun creaPianoConAlimenti(): Long {
        db.userProfileDao().upsert(Mappers.run { profilo.toEntity() })

        // Gli alimenti devono esistere nel catalogo: il riscalamento lavora sui
        // valori nutrizionali reali, non su quelli scritti nella voce del pasto.
        val alimenti = listOf(
            com.fitcoach.app.domain.model.FoodItem(
                id = 1L, name = "Avena", category = "Cereali",
                kcalPer100g = 375.0, proteinPer100g = 12.5, carbsPer100g = 62.5, fatPer100g = 7.5,
                source = "CREA"
            ),
            com.fitcoach.app.domain.model.FoodItem(
                id = 2L, name = "Albumi", category = "Uova",
                kcalPer100g = 50.0, proteinPer100g = 11.0, carbsPer100g = 0.5, fatPer100g = 0.2,
                source = "CREA"
            ),
            com.fitcoach.app.domain.model.FoodItem(
                id = 3L, name = "Riso", category = "Cereali",
                kcalPer100g = 350.0, proteinPer100g = 7.0, carbsPer100g = 78.0, fatPer100g = 1.0,
                source = "CREA"
            ),
            com.fitcoach.app.domain.model.FoodItem(
                id = 4L, name = "Petto di pollo", category = "Carni",
                kcalPer100g = 110.0, proteinPer100g = 23.0, carbsPer100g = 0.0, fatPer100g = 1.5,
                source = "CREA"
            ),
            com.fitcoach.app.domain.model.FoodItem(
                id = 5L, name = "Olio extravergine di oliva", category = "Condimenti",
                kcalPer100g = 899.0, proteinPer100g = 0.0, carbsPer100g = 0.0, fatPer100g = 99.9,
                source = "CREA"
            )
        )
        db.foodDao().insertAll(alimenti.map { Mappers.run { it.toEntity() } })

        val energy = EnergyCalculator.compute(profilo)!!
        val cycling = CarbCyclingEngine.build(profilo, energy)
        val pasti = listOf(
            MealPlanGenerator.GeneratedMeal(
                slot = MealSlot.BREAKFAST,
                entries = listOf(
                    entry("Avena", 80.0, 300.0, 10.0, 50.0, 6.0, 1L),
                    entry("Albumi", 200.0, 100.0, 22.0, 1.0, 0.0, 2L),
                    entry("Olio extravergine di oliva", 10.0, 90.0, 0.0, 0.0, 10.0, 5L)
                )
            ),
            MealPlanGenerator.GeneratedMeal(
                slot = MealSlot.MORNING_SNACK,
                entries = listOf(
                    entry("Albumi", 150.0, 75.0, 16.5, 0.8, 0.3, 2L)
                )
            ),
            MealPlanGenerator.GeneratedMeal(
                slot = MealSlot.LUNCH,
                entries = listOf(
                    entry("Riso", 100.0, 350.0, 7.0, 78.0, 1.0, 3L),
                    entry("Petto di pollo", 180.0, 200.0, 42.0, 0.0, 3.0, 4L),
                    entry("Olio extravergine di oliva", 15.0, 135.0, 0.0, 0.0, 15.0, 5L)
                )
            ),
            MealPlanGenerator.GeneratedMeal(
                slot = MealSlot.AFTERNOON_SNACK,
                entries = listOf(
                    entry("Avena", 40.0, 150.0, 5.0, 25.0, 3.0, 1L)
                )
            ),
            MealPlanGenerator.GeneratedMeal(
                slot = MealSlot.DINNER,
                entries = listOf(
                    entry("Petto di pollo", 200.0, 220.0, 46.0, 0.0, 3.0, 4L),
                    entry("Riso", 80.0, 280.0, 5.6, 62.4, 0.8, 3L),
                    entry("Olio extravergine di oliva", 10.0, 90.0, 0.0, 0.0, 10.0, 5L)
                )
            )
        )
        val giorni = (0..6).map { idx ->
            val tipo = CarbDayType.MEDIUM
            val target = cycling.targets[tipo]!!
            MealPlanGenerator.GeneratedDay(
                dayIndex = idx, dayType = tipo, target = target, meals = pasti,
                actual = com.fitcoach.app.domain.model.MacroTotals.ZERO,
                deviations = com.fitcoach.app.domain.model.MacroTotals.ZERO,
                isCoherent = true
            )
        }
        return nutrition.savePlan(
            profile = profilo, energy = energy, cycling = cycling,
            generatedDays = giorni, dayTypes = List(7) { CarbDayType.MEDIUM },
            trainingDayIndices = (0..3).toList()
        )
    }

    private fun grammiPerGiorno(plan: com.fitcoach.app.domain.model.NutritionPlan): List<Double> =
        plan.days.map { d -> d.meals.flatMap { it.entries }.sumOf { it.grams } }

    @Test
    fun `cambiare distribuzione riscala gli alimenti dei giorni`() = runTest {
        val planId = creaPianoConAlimenti()
        val prima = nutrition.getActivePlan()!!
        val grammiPrima = grammiPerGiorno(prima)
        assertThat(grammiPrima.sum()).isGreaterThan(0.0)

        val custom = profilo.copy(
            carbHighDaysOverride = 4, carbMediumDaysOverride = 3, carbLowDaysOverride = 0
        )
        users.save(custom)
        val energy = EnergyCalculator.compute(custom)!!
        val cycling = CarbCyclingEngine.build(custom, energy)
        nutrition.applyCarbCyclingAsNewPlanVersion(
            planId = planId,
            cycling = cycling,
            dayTypes = CarbCyclingEngine.assignDayTypes(4, 3, 0, (0..3).toList()),
            tdee = energy.tdee,
            diaryEpochDay = 20_000L
        )

        val dopo = nutrition.getActivePlan()!!
        val grammiDopo = grammiPerGiorno(dopo)

        // I pasti devono esistere ancora (non azzerati)...
        assertThat(dopo.days.sumOf { d -> d.meals.sumOf { it.entries.size } })
            .isEqualTo(prima.days.sumOf { d -> d.meals.sumOf { it.entries.size } })

        // ...e le grammature NON devono essere identiche giorno per giorno quando
        // i target cambiano: se restano uguali il piano e' "uguale al precedente"
        // ed e' esattamente la lamentela dell'utente.
        val giorniCambiati = prima.days.zip(dopo.days).count { (a, b) ->
            a.targetKcal != b.targetKcal &&
                a.meals.flatMap { it.entries }.sumOf { it.grams } !=
                b.meals.flatMap { it.entries }.sumOf { it.grams }
        }
        assertThat(giorniCambiati)
            .isAtLeast(1)

        // Il riscalamento deve essere sostanziale, non un grammo di differenza:
        // altrimenti il piano resta di fatto "uguale al precedente".
        val scartoMassimo = prima.days.zip(dopo.days).maxOf { (a, b) ->
            kotlin.math.abs(
                a.meals.flatMap { it.entries }.sumOf { it.grams } -
                    b.meals.flatMap { it.entries }.sumOf { it.grams }
            )
        }
        assertThat(scartoMassimo).isGreaterThan(20.0)

        // E le calorie degli alimenti devono avvicinarsi al nuovo target del
        // giorno, non restare quelle di prima.
        val giornoHigh = dopo.days.first { it.dayType == CarbDayType.HIGH }
        val kcalAlimenti = giornoHigh.meals.flatMap { it.entries }.sumOf { it.kcal }
        val scarto = kotlin.math.abs(kcalAlimenti - giornoHigh.targetKcal) / giornoHigh.targetKcal
        assertThat(scarto).isLessThan(0.10)
    }

    @Test
    fun `modificare a mano il target di un giorno riscala i suoi alimenti`() = runTest {
        val planId = creaPianoConAlimenti()
        val prima = nutrition.getActivePlan()!!
        val giorno = prima.days.first()
        val kcalPrima = giorno.meals.flatMap { it.entries }.sumOf { it.kcal }

        // Stesso tipo di giornata, target ridotto di 300 kcal.
        val nuovoTarget = giorno.targetKcal - 300.0
        nutrition.updateDayTargetsAsNewPlanVersion(
            planId = planId,
            dayId = giorno.id,
            kcal = nuovoTarget,
            protein = giorno.targetProtein,
            carbs = giorno.targetCarbs - 75.0,
            fat = giorno.targetFat,
            diaryEpochDay = 20_000L
        )

        val dopo = nutrition.getActivePlan()!!.days.first { it.dayIndex == giorno.dayIndex }
        val kcalDopo = dopo.meals.flatMap { it.entries }.sumOf { it.kcal }
        // Le grammature seguono il nuovo target: senza il riscalamento restavano
        // ferme a quelle di prima e il giorno risultava "modificato" solo in testa.
        assertThat(kcalDopo).isNotWithin(1.0).of(kcalPrima)
        assertThat(kotlin.math.abs(kcalDopo - nuovoTarget) / nuovoTarget).isLessThan(0.10)
    }
}
