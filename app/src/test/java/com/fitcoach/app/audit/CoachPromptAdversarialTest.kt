package com.fitcoach.app.audit

import com.fitcoach.app.domain.coach.CoachContext
import com.fitcoach.app.domain.coach.CoachPromptBuilder
import com.fitcoach.app.domain.model.BudgetScope
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il prompt e' il contratto fra app e modello: quello che non c'e' scritto non
 * esiste, e quello che e' scritto sbagliato viene creduto. Un `%.0f` non
 * sostituito non produce un errore: produce un modello che legge "budget
 * %.0f EUR" e risponde a caso.
 */
class CoachPromptAdversarialTest {

    private fun profilo() = UserProfile(
        name = "Luca", ageYears = 30, sex = Sex.MALE, heightCm = 175.0, weightKg = 80.0,
        bodyFatPercent = 18.0, experience = TrainingExperience.INTERMEDIATE,
        goal = Goal.HYPERTROPHY, trainingDaysPerWeek = 4, mealsPerDay = 5,
        monthlyFoodBudgetEur = 320.0, budgetScope = BudgetScope.PERSONAL_ONLY,
        budgetPersons = 1, anamnesisCompleted = true
    )

    private fun contesto(profile: UserProfile? = profilo()) = CoachContext(
        profile = profile,
        nutritionPlan = null,
        workoutPlan = null,
        recentPerformances = emptyList(),
        weightTrend = null,
        recentChanges = emptyList(),
        history = emptyList()
    )

    @Test
    fun `nessun segnaposto di formato resta nel contesto`() {
        val testo = CoachPromptBuilder.buildContext(contesto())
        assertFalse(
            "Un %.0f non sostituito finisce sotto gli occhi del modello:\n$testo",
            testo.contains("%.0f")
        )
        assertFalse("Nessun %.1f non sostituito", testo.contains("%.1f"))
        assertFalse("Nessun %.2f non sostituito", testo.contains("%.2f"))
        assertFalse("Nessun %d non sostituito", testo.contains("%d"))
        assertFalse("Nessun %s non sostituito", testo.contains("%s"))
    }

    @Test
    fun `il budget mensile arriva al modello come numero`() {
        val testo = CoachPromptBuilder.buildContext(contesto())
        assertTrue(
            "Il budget deve comparire con il suo valore:\n$testo",
            testo.contains("320 EUR")
        )
    }

    @Test
    fun `il budget indica l'ambito e le persone`() {
        val testo = CoachPromptBuilder.buildContext(contesto())
        val riga = testo.lines().firstOrNull { it.contains("Budget alimentare mensile") }
        assertTrue("La riga del budget deve esistere", riga != null)
        assertTrue("Deve dire quante persone copre: $riga", riga!!.contains("1 persone"))
    }

    @Test
    fun `le prestazioni recenti riportano il volume come numero`() {
        val perf = ExercisePerformance(
            exerciseId = 1L, exerciseName = "Panca piana",
            sessionId = 1L, dateEpochDay = 20_000L, topWeightKg = 80.0, topReps = 8,
            totalSets = 4, totalReps = 32, volumeKg = 2560.0, estimated1Rm = 101.3
        )
        val testo = CoachPromptBuilder.buildContext(
            contesto().copy(recentPerformances = listOf(perf))
        )
        assertFalse("Il volume non deve restare un segnaposto:\n$testo", testo.contains("%.0f"))
        assertTrue(
            "Il volume reale deve comparire:\n$testo",
            testo.contains("2560 kg") || testo.contains("2561 kg") ||
                testo.contains("2559 kg") || testo.contains("2560.0 kg")
        )
    }

    @Test
    fun `senza budget il contesto non inventa una cifra`() {
        val senzaBudget = profilo().copy(monthlyFoodBudgetEur = null)
        val testo = CoachPromptBuilder.buildContext(contesto(senzaBudget))
        assertFalse(
            "Senza budget non deve comparire la riga del budget mensile",
            testo.contains("Budget alimentare mensile")
        )
    }

    @Test
    fun `senza profilo il contesto chiede di completare l'onboarding`() {
        val testo = CoachPromptBuilder.buildContext(contesto(profile = null))
        assertTrue(
            "Deve dire che il profilo manca invece di lasciare il blocco vuoto",
            testo.contains("NON configurato", ignoreCase = true)
        )
        assertFalse("Nessun segnaposto nemmeno senza profilo", testo.contains("%.0f"))
    }

    @Test
    fun `il contesto dichiara il calendario per non far chiedere che giorno sia`() {
        val testo = CoachPromptBuilder.buildContext(contesto())
        assertTrue("Serve l'indice del giorno di oggi", testo.contains("indice"))
        assertTrue("Serve dire cos'e' domani", testo.contains("Domani"))
    }

    @Test
    fun `il prompt di sistema vieta diagnosi e farmaci`() {
        val s = CoachPromptBuilder.systemPrompt.lowercase()
        assertTrue("Deve vietare le diagnosi", s.contains("diagnosi"))
        assertTrue("Deve vietare farmaci e ormoni", s.contains("farmaci") && s.contains("ormoni"))
        assertTrue("Deve citare il dolore", s.contains("dolore"))
    }

    @Test
    fun `il prompt di sistema impone l'italiano`() {
        assertTrue(
            "Il prompt deve imporre la lingua italiana",
            CoachPromptBuilder.systemPrompt.contains("italiano", ignoreCase = true)
        )
    }

    @Test
    fun `il prompt del check-in vieta di cambiare le calorie su una sola settimana`() {
        val s = CoachPromptBuilder.weeklyCheckInSystemPrompt.lowercase()
        assertTrue(
            "Serve la regola sulle 2-3 settimane di dati",
            s.contains("2 settimane") || s.contains("tre settimane") || s.contains("3 settimane")
        )
        assertTrue("Deve vietare di alzare il volume se le prestazioni calano",
            s.contains("volume") && s.contains("peggiorano"))
    }
}
