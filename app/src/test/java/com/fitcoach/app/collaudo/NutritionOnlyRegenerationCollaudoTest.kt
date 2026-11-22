package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiMeal
import com.fitcoach.app.domain.coach.AiNutritionDay
import com.fitcoach.app.domain.coach.AiNutritionProposal
import com.fitcoach.app.domain.coach.AiProgramProposal
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.coach.ProposalChecker
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.FoodFilter
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Rigenerare il solo piano alimentare non deve fallire.
 *
 * Difetto segnalato: l'utente cancella il piano e chiede in chat di rigenerarlo;
 * la richiesta va in errore. Causa: il controllo del calendario di allenamento
 * girava anche in scope NUTRITION, dove il modello non ha motivo di compilare
 * `workout.days`. L'elenco restava vuoto, il numero di giorni non corrispondeva
 * a quelli del profilo e la proposta veniva bocciata come se fosse una scheda
 * incompleta, pur essendo un piano alimentare.
 */
class NutritionOnlyRegenerationCollaudoTest {

    private val profile = MarjanProfile.build()

    private val foods = FoodFilter.allowedFoods(FoodSeedData.foods, profile)

    /** Un giorno nutrizionale valido per gli slot previsti dal profilo. */
    private fun nutritionDays(): List<AiNutritionDay> =
        (0..6).map { dayIndex ->
            AiNutritionDay(
                dayIndex = dayIndex,
                dayType = if (dayIndex % 2 == 0) "HIGH" else "LOW",
                meals = com.fitcoach.app.domain.nutrition.MealSlots
                    .forMealsPerDay(profile.mealsPerDay)
                    .map { slot -> AiMeal(slot = slot.name, entries = emptyList()) }
            )
        }

    @Test
    fun `scope NUTRITION non richiede i giorni di allenamento`() {
        // Nessun giorno di allenamento: e' esattamente cio' che accade quando il
        // modello risponde a una richiesta che riguarda solo il cibo.
        val proposal = AiProgramProposal(
            nutrition = AiNutritionProposal(days = nutritionDays())
        )

        val report = ProposalChecker.check(
            profile = profile,
            proposal = proposal,
            foods = foods,
            exercises = emptyList(),
            scope = ProgramGenerationEngine.Scope.NUTRITION
        )

        val calendarViolations = report.violations.filter { it.contains("allenamento") || it.contains("giorni") }
        assertThat(calendarViolations).isEmpty()
    }

    @Test
    fun `scope WORKOUT continua a verificare il calendario`() {
        // La guardia non deve indebolire il controllo dove serve davvero: una
        // scheda senza i giorni dichiarati dal profilo resta un errore.
        val proposal = AiProgramProposal()

        val report = ProposalChecker.check(
            profile = profile,
            proposal = proposal,
            foods = foods,
            exercises = emptyList(),
            scope = ProgramGenerationEngine.Scope.WORKOUT
        )

        assertThat(report.violations.any { it.contains("allenamento") }).isTrue()
    }
}
