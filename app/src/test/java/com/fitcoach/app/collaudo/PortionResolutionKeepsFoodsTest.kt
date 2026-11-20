package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.coach.AiFoodEntry
import com.fitcoach.app.domain.coach.AiMeal
import com.fitcoach.app.domain.coach.AiNutritionDay
import com.fitcoach.app.domain.coach.AiNutritionProposal
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.PortionResolution
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * L'errore riportato dall'utente conteneva anche:
 *
 *   giorno 3 kcal: attese 1891, ricevute 1463
 *   giorno 3 carboidrati: attesi 148 g, ricevuti 48 g
 *
 * Il giorno perdeva calorie perche il solver delle porzioni scartava in silenzio
 * gli alimenti il cui nome non coincideva esattamente col catalogo. Con i nomi
 * risolti, gli alimenti restano nel pasto.
 */
class PortionResolutionKeepsFoodsTest {

    private val profile = UserProfile(
        name = "Test",
        ageYears = 30,
        sex = Sex.MALE,
        heightCm = 180.0,
        weightKg = 80.0,
        goal = Goal.RECOMP,
        trainingDaysPerWeek = 4,
        mealsPerDay = 5,
        experience = com.fitcoach.app.domain.model.TrainingExperience.INTERMEDIATE
    )

    @Test
    fun `il solver non perde gli alimenti scritti con un nome diverso dal catalogo`() {
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)

        val proposta = AiNutritionProposal(
            days = listOf(
                AiNutritionDay(
                    dayIndex = 0,
                    dayType = CarbDayType.MEDIUM.name,
                    meals = listOf(
                        AiMeal("BREAKFAST", entries = listOf(
                            AiFoodEntry("Uova intere", 150.0),
                            AiFoodEntry("Fiocchi d'avena", 80.0)
                        )),
                        AiMeal("LUNCH", entries = listOf(
                            AiFoodEntry("Riso", 100.0),
                            AiFoodEntry("Petto di pollo", 180.0),
                            AiFoodEntry("Olio extravergine d'oliva", 10.0)
                        )),
                        AiMeal("DINNER", entries = listOf(
                            AiFoodEntry("Trancio di salmone", 180.0),
                            AiFoodEntry("Quinoa", 90.0)
                        ))
                    )
                )
            )
        )

        val (risolto, _) = PortionResolution.resolve(proposta, FoodSeedData.foods, cycling)
        val pasti = risolto.days.first().meals

        // Nessun pasto deve restare vuoto e nessun alimento deve sparire.
        assertThat(pasti.map { it.entries.size }).isEqualTo(listOf(2, 3, 2))
        // Ogni alimento deve avere grammi positivi: se il nome non fosse stato
        // risolto, la grammatura sarebbe rimasta quella originale o nulla.
        pasti.flatMap { it.entries }.forEach { entry ->
            assertThat(entry.grams).isGreaterThan(0.0)
        }
    }
}
