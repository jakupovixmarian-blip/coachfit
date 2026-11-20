package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiMeal
import com.fitcoach.app.domain.coach.AiNutritionDay
import com.fitcoach.app.domain.coach.AiNutritionProposal
import com.fitcoach.app.domain.coach.AiProgramProposal
import com.fitcoach.app.domain.coach.AiFoodEntry
import com.fitcoach.app.domain.coach.AiPlannedExercise
import com.fitcoach.app.domain.coach.AiWorkoutDay
import com.fitcoach.app.domain.coach.AiWorkoutProposal
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CatalogNameResolver
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Riproduce il programma realmente respinto dall'app dell'utente:
 *
 *   giorno 1: alimento 'Uova intere' non presente nel database
 *   giorno 1: alimento 'Riso' non presente nel database
 *   giorno 2: alimento 'Fiocchi di latte' non presente nel database
 *   giorno 4: alimento 'Yogurt greco' non presente nel database
 *   giorno 5: alimento 'Tonno al naturale' non presente nel database
 *
 * Tutti questi alimenti ESISTONO nel catalogo con un nome più lungo. Il test usa
 * il catalogo reale (FoodSeedData), non un elenco inventato: se il risolutore
 * funziona qui, funziona con i dati che l'app consegna davvero all'AI.
 */
class RejectedProgramRealCatalogTest {

    private val foods = FoodSeedData.foods
    private val exercises = ExerciseSeedData.exercises

    private val profile = UserProfile(
        name = "Test",
        ageYears = 30,
        sex = Sex.MALE,
        heightCm = 180.0,
        weightKg = 80.0,
        goal = Goal.RECOMP,
        trainingDaysPerWeek = 4,
        mealsPerDay = 5,
        experience = com.fitcoach.app.domain.model.TrainingExperience.INTERMEDIATE,
        availableEquipment = Equipment.entries.toSet()
    )

    @Test
    fun `ogni alimento rifiutato dall'app esiste davvero nel catalogo`() {
        val rifiutati = listOf(
            "Uova intere", "Riso", "Fiocchi di latte", "Grana padano", "Yogurt greco",
            "Frutti di bosco", "Tonno al naturale", "Trancio di salmone", "Riso venere",
            "Marmellata", "Tagliata di manzo", "Mango", "Cottage cheese", "Spigola",
            "Quinoa", "Petto di pollo"
        )
        val risolvibili = rifiutati.filter { CatalogNameResolver.resolveFood(it, foods) != null }
        // Marmellata, mango e frutti di bosco NON sono in catalogo: e' corretto
        // che restino irrisolti, ma tutto il resto deve trovare il suo alimento.
        assertThat(risolvibili).containsAtLeast(
            "Uova intere", "Riso", "Fiocchi di latte", "Grana padano", "Yogurt greco",
            "Tonno al naturale", "Trancio di salmone", "Riso venere", "Tagliata di manzo",
            "Cottage cheese", "Spigola", "Quinoa", "Petto di pollo"
        )
    }

    @Test
    fun `la diagnostica non segnala piu come assenti gli alimenti che esistono`() {
        val proposta = AiProgramProposal(
            nutrition = AiNutritionProposal(
                days = (0..6).map { day ->
                    AiNutritionDay(
                        dayIndex = day,
                        dayType = "MEDIUM",
                        meals = (1..5).map { meal ->
                            AiMeal(
                                slot = listOf(
                                    "BREAKFAST", "MORNING_SNACK", "LUNCH",
                                    "AFTERNOON_SNACK", "DINNER"
                                )[meal - 1],
                                entries = listOf(
                                    AiFoodEntry("Uova intere", 100.0),
                                    AiFoodEntry("Riso", 80.0)
                                )
                            )
                        }
                    )
                }
            ),
            workout = AiWorkoutProposal(
                days = (0..6).take(4).map { day ->
                    AiWorkoutDay(
                        dayIndex = day,
                        title = "Seduta ${day + 1}",
                        exercises = listOf(
                            AiPlannedExercise("Panca piana con bilanciere", 4, 6, 10, 150, 2),
                            AiPlannedExercise("Croci ai cavi", 3, 10, 15, 60, 1)
                        )
                    )
                }
            ),
            calendar = com.fitcoach.app.domain.coach.AiCalendarProposal(
                workoutDayIndices = listOf(0, 1, 2, 3),
                restDayIndices = listOf(4, 5, 6)
            )
        )

        val violazioni = ProgramGenerationEngine.diagnose(profile, proposta, foods, exercises)
        val nomiAssenti = violazioni.filter { it.contains("non presente nel database") }
        assertThat(nomiAssenti).isEmpty()
    }

    @Test
    fun `un esercizio senza nome produce un messaggio che dice cosa manca`() {
        val proposta = AiProgramProposal(
            workout = AiWorkoutProposal(
                days = (0..6).take(4).map { day ->
                    AiWorkoutDay(
                        dayIndex = day,
                        exercises = listOf(AiPlannedExercise("", 3, 0, 0, 0, 0))
                    )
                }
            ),
            calendar = com.fitcoach.app.domain.coach.AiCalendarProposal(
                workoutDayIndices = listOf(0, 1, 2, 3),
                restDayIndices = listOf(4, 5, 6)
            )
        )
        val violazioni = ProgramGenerationEngine.diagnose(
            profile, proposta, foods, exercises, ProgramGenerationEngine.Scope.WORKOUT
        )
        // Prima il messaggio era "esercizio '' non presente nel database", che non
        // diceva nulla su come correggere.
        assertThat(violazioni.any { it.contains("senza nome") }).isTrue()
    }
}
