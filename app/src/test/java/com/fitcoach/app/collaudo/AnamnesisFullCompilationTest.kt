package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * L'anamnesi deve poter essere compilata per intero e ogni risposta deve
 * arrivare davvero nel profilo.
 *
 * Il difetto che questo test sorveglia: `applyAnswer` ignora in silenzio i
 * valori che non riesce a interpretare. Una domanda "compilata" con un valore
 * non valido lascia il profilo al default, indistinguibile da una risposta
 * reale, e la generazione decide su dati mai forniti.
 */
class AnamnesisFullCompilationTest {

    /** Risposte realistiche e valide per tutte le 44 domande del flusso. */
    private val answers = linkedMapOf(
        "age" to "34",
        "sex" to "MALE",
        "heightCm" to "178",
        "weightKg" to "81.5",
        "bodyFatPercent" to "19",
        "goal" to "HYPERTROPHY",
        "secondaryGoals" to "CUT",
        "targetTimelineWeeks" to "20",
        "workType" to "SEDENTARY",
        "workHoursPerDay" to "8",
        "dailySteps" to "7000",
        "transportMode" to "CAR",
        "workShift" to "NONE",
        "extraSportSessionsPerWeek" to "1",
        "maxSessionMinutes" to "75",
        "sleepHours" to "7",
        "sleepQuality" to "MEDIUM",
        "stressLevel" to "HIGH",
        "dailyEnergy" to "MEDIUM",
        "recoveryCapacity" to "MEDIUM",
        "mealsPerDay" to "5",
        "breakfastPreference" to "SWEET",
        "dietaryPreferences" to "MEDITERRANEAN",
        "likedFoods" to "Yogurt greco, Avena, Salmone",
        "dislikedFoods" to "Fegato",
        "allergies" to "Arachidi",
        "excludedFoods" to "Maiale",
        "cookingTimeAvailable" to "MEDIUM",
        "mealPrepPossible" to "true",
        "monthlyFoodBudgetEur" to "320",
        "budgetScope" to "PERSONAL_ONLY",
        "budgetPersons" to "1",
        "trainingStatus" to "CURRENTLY_TRAINING",
        "experience" to "INTERMEDIATE",
        "trainingYears" to "4",
        "currentLoads" to "Panca piana=80, Squat=110, Stacco=140",
        "trainingDaysPerWeek" to "4",
        "technicalLevel" to "COMPETENT",
        "availableEquipment" to "BARBELL, DUMBBELL, MACHINE, CABLE, BENCH",
        "preferredExercises" to "Panca piana, Trazioni",
        "dislikedExercises" to "Burpees",
        "priorityMuscles" to "CHEST, BACK",
        "physicalLimitations" to "Lombalgia lieve",
        "movementsToAvoid" to "Stacco da terra con schiena curva"
    )

    private fun compile(): UserProfile {
        var profile = UserProfile()
        answers.forEach { (id, value) -> profile = AnamnesisEngine.applyAnswer(profile, id, value) }
        return profile
    }

    @Test
    fun `il test copre ogni domanda del flusso`() {
        val declared = AnamnesisEngine.questions.map { it.id }
        assertThat(answers.keys).containsExactlyElementsIn(declared)
    }

    @Test
    fun `nessuna risposta viene ignorata in silenzio`() {
        val profile = compile()
        val lost = AnamnesisEngine.questions
            .filter { it.id in answers.keys }
            .filterNot { AnamnesisEngine.isAnswered(profile, it.id) }
            .map { it.id }
        assertThat(lost).isEmpty()
    }

    @Test
    fun `i valori arrivano nel profilo e non restano ai default`() {
        val profile = compile()
        assertThat(profile.ageYears).isEqualTo(34)
        assertThat(profile.heightCm).isEqualTo(178.0)
        assertThat(profile.weightKg).isEqualTo(81.5)
        assertThat(profile.bodyFatPercent).isEqualTo(19.0)
        assertThat(profile.mealsPerDay).isEqualTo(5)
        assertThat(profile.trainingDaysPerWeek).isEqualTo(4)
        assertThat(profile.monthlyFoodBudgetEur).isEqualTo(320.0)
        assertThat(profile.currentLoads["Panca piana"]).isEqualTo(80.0)
        assertThat(profile.currentLoads["Squat"]).isEqualTo(110.0)
        assertThat(profile.allergies).containsExactly("Arachidi")
        assertThat(profile.physicalLimitations).containsExactly("Lombalgia lieve")
        assertThat(profile.availableEquipment).hasSize(5)
        assertThat(profile.priorityMuscles).hasSize(2)
    }

    @Test
    fun `un valore non valido non viene scambiato per una risposta data`() {
        // Stesso percorso della UI: se il testo non e' interpretabile il campo
        // resta vuoto, e isAnswered deve dirlo apertamente.
        val profile = AnamnesisEngine.applyAnswer(UserProfile(), "age", "trentaquattro")
        assertThat(profile.ageYears).isNull()
        assertThat(AnamnesisEngine.isAnswered(profile, "age")).isFalse()
    }

    @Test
    fun `il riepilogo riporta i dati dichiarati`() {
        val summary = AnamnesisEngine.summary(compile())
        assertThat(summary).contains("34")
        assertThat(summary).contains("81.5")
    }
}
