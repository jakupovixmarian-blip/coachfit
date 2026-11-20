package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.coach.AiFoodEntry
import com.fitcoach.app.domain.coach.AiMeal
import com.fitcoach.app.domain.coach.AiNutritionDay
import com.fitcoach.app.domain.coach.AiNutritionProposal
import com.fitcoach.app.domain.model.CarbCyclingPlan
import com.fitcoach.app.domain.model.CarbDayTarget
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.FoodItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * Il caso reale osservato su OpenRouter: l'AI sceglie alimenti sensati ma
 * sbaglia i grammi a mano (fino a -900 kcal). Il ricalcolo deve riportare la
 * giornata sul target senza cambiare gli alimenti scelti.
 */
class PortionResolutionTest {

    private fun food(name: String, kcal: Double, p: Double, c: Double, f: Double) = FoodItem(
        id = name.hashCode().toLong(),
        name = name,
        category = "test",
        kcalPer100g = kcal,
        proteinPer100g = p,
        carbsPer100g = c,
        fatPer100g = f,
        source = "test"
    )

    private val foods = listOf(
        food("Petto di pollo", 165.0, 31.0, 0.0, 3.6),
        food("Riso bianco", 130.0, 2.7, 28.0, 0.3),
        food("Olio di oliva", 884.0, 0.0, 0.0, 100.0)
    )

    private val cycling = CarbCyclingPlan(
        targets = mapOf(
            CarbDayType.HIGH to CarbDayTarget(
                CarbDayType.HIGH, kcal = 2600.0, protein = 170.0, carbs = 300.0, fat = 70.0
            )
        ),
        highDays = 1,
        mediumDays = 0,
        lowDays = 0,
        weeklyKcal = 18200.0,
        weeklyKcalFromTarget = 18200.0,
        weeklyProtein = 1190.0,
        weeklyCarbs = 2100.0,
        weeklyFat = 490.0
    )

    /** Grammature volutamente sbagliate: circa meta' del necessario. */
    private fun wrongDay() = AiNutritionDay(
        dayIndex = 0,
        dayType = "HIGH",
        meals = listOf(
            AiMeal("BREAKFAST", "08:00", listOf(AiFoodEntry("Petto di pollo", 50.0))),
            AiMeal("LUNCH", "13:00", listOf(AiFoodEntry("Riso bianco", 60.0))),
            AiMeal("DINNER", "20:00", listOf(AiFoodEntry("Olio di oliva", 5.0)))
        )
    )

    @Test
    fun `le grammature sbagliate dall'AI vengono riportate sul target`() {
        val original = AiNutritionProposal(days = listOf(wrongDay()))

        val (resolved, notes) = PortionResolution.resolve(original, foods, cycling)

        val day = resolved.days.single()
        val names = day.meals.flatMap { m -> m.entries.map { it.foodName } }
        // Gli alimenti scelti dall'AI non cambiano: cambiano solo i grammi.
        assertThat(names).containsExactly("Petto di pollo", "Riso bianco", "Olio di oliva").inOrder()

        val grams = day.meals.flatMap { m -> m.entries }.associate { it.foodName to it.grams }
        assertThat(grams.getValue("Petto di pollo")).isGreaterThan(50.0)
        assertThat(grams.getValue("Riso bianco")).isGreaterThan(60.0)

        val totals = day.meals.flatMap { it.entries }.sumOf { e ->
            val f = foods.first { it.name == e.foodName }
            f.kcalPer100g * e.grams / 100.0
        }
        // Il solver mira a +-3%: il piano deve essere centrato, non "piu' vicino".
        assertThat(abs(totals - 2600.0) / 2600.0).isLessThan(0.03)
        assertThat(notes).isEmpty()
    }

    @Test
    fun `una giornata con soli grassi non centra le proteine e lo dichiara`() {
        // Solo olio: nessuna fonte proteica ne' di carboidrati. Il solver non puo'
        // centrare il target e deve dirlo, invece di restituire un piano che
        // sembra valido ma non lo e'.
        val impossible = AiNutritionProposal(
            days = listOf(
                AiNutritionDay(
                    dayIndex = 0,
                    dayType = "HIGH",
                    meals = listOf(AiMeal("LUNCH", "13:00", listOf(AiFoodEntry("Olio di oliva", 10.0))))
                )
            )
        )

        val (_, notes) = PortionResolution.resolve(impossible, foods, cycling)

        assertThat(notes.joinToString()).contains("giorno 0")
    }

    @Test
    fun `gli alimenti sconosciuti non vengono inventati`() {
        val original = AiNutritionProposal(
            days = listOf(
                AiNutritionDay(
                    dayIndex = 0,
                    dayType = "HIGH",
                    meals = listOf(
                        AiMeal("LUNCH", "13:00", listOf(AiFoodEntry("Cibo inesistente", 100.0)))
                    )
                )
            )
        )

        val (resolved, notes) = PortionResolution.resolve(original, foods, cycling)

        // Nessun alimento aggiunto di nascosto: la giornata resta al validatore.
        assertThat(resolved.days).isEqualTo(original.days)
        assertThat(notes.joinToString()).contains("nessun alimento riconosciuto")
    }
}
