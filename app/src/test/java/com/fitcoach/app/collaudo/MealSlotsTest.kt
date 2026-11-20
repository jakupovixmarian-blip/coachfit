package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.MealSlots
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Gli slot pasto devono dipendere dal profilo e coincidere in prompt e validatore.
 */
class MealSlotsTest {

    @Test
    fun `tre pasti sono i tre principali`() {
        assertThat(MealSlots.forMealsPerDay(3))
            .containsExactly(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER)
            .inOrder()
    }

    @Test
    fun `cinque pasti aggiungono i due spuntini diurni`() {
        assertThat(MealSlots.forMealsPerDay(5)).containsExactly(
            MealSlot.BREAKFAST, MealSlot.MORNING_SNACK, MealSlot.LUNCH,
            MealSlot.AFTERNOON_SNACK, MealSlot.DINNER
        ).inOrder()
    }

    @Test
    fun `sei pasti usano tutti gli slot`() {
        assertThat(MealSlots.forMealsPerDay(6)).hasSize(6)
        assertThat(MealSlots.forMealsPerDay(6)).containsExactlyElementsIn(MealSlot.entries)
    }

    @Test
    fun `il numero di slot coincide sempre con i pasti richiesti`() {
        (3..6).forEach { meals ->
            assertThat(MealSlots.forMealsPerDay(meals)).hasSize(meals)
        }
    }

    @Test
    fun `l elenco per il prompt non contiene slot non richiesti`() {
        val described = MealSlots.describe(5)
        assertThat(described).contains("BREAKFAST")
        assertThat(described).doesNotContain("EVENING_SNACK")
        assertThat(described.split(", ")).hasSize(5)
    }
}
