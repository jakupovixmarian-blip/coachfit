package com.fitcoach.app.collaudo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.PlanVersionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.usecase.GenerateProgramsUseCase
import com.fitcoach.app.domain.workout.ExerciseSeedData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest

/**
 * Sonda di sola lettura: genera il programma per il profilo REALE di collaudo
 * (Marjan) e stampa i valori effettivi, per documentare il comportamento reale
 * prima di fissare le asserzioni del collaudo. Nessuna asserzione: solo output.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CollaudoExplorationTest {

    @Test
    fun `stampa i valori reali generati per il profilo Marjan`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        val userRepository = UserRepository(db.userProfileDao())
        val foodRepository = FoodRepository(db.foodDao())
        val exerciseRepository = ExerciseRepository(db.exerciseDao())
        val nutritionRepository = NutritionRepository(db)
        val workoutRepository = WorkoutRepository(db)
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        db.exerciseDao().insertAll(ExerciseSeedData.exercises.map { it.toEntity() })

        val marjan = MarjanProfile.build()
        userRepository.save(marjan)

        val engine = GenerateProgramsUseCase(
            userRepository, foodRepository, exerciseRepository,
            nutritionRepository, workoutRepository, PlanVersionRepository(db.planVersionDao())
        )
        val result = engine(marjan, requireAnamnesis = false)
        println("=== GENERAZIONE: $result ===")

        val plan = nutritionRepository.getActivePlan()!!
        println("BMR=${plan.bmr} TDEE=${plan.tdee} formulaBmr=${plan.bmrFormula} formulaTdee=${plan.tdeeFormula}")
        println("weeklyCalories=${plan.weeklyCalories} weeklyBalance=${plan.weeklyKcalBalance}")
        println("HIGH=${plan.highCarbDays} MEDIUM=${plan.mediumCarbDays} LOW=${plan.lowCarbDays} applied=${plan.carbCyclingApplied}")
        println("proteinGPerKg=${plan.proteinGPerKg}")
        plan.days.forEach { d ->
            println(
                "giorno ${d.dayIndex} [${d.dayType}] target=${d.targetKcal.toInt()} " +
                    "P=${d.targetProtein.toInt()} C=${d.targetCarbs.toInt()} F=${d.targetFat.toInt()} " +
                    "| effettive=${d.actualKcal.toInt()} P=${d.actualProtein.toInt()} " +
                    "C=${d.actualCarbs.toInt()} F=${d.actualFat.toInt()} | pasti=${d.meals.size}"
            )
            d.meals.forEach { m ->
                println("    ${m.slot} ${m.time}: ${m.entries.joinToString { "${it.foodName} ${it.grams.toInt()}g (${it.unit})" }}")
            }
        }

        val workout = workoutRepository.getActivePlan()!!
        println("SPLIT=${workout.splitType} meso=${workout.mesocycles.firstOrNull()}")
        println("volumeSettimanale=${workout.weeklyVolumePerGroup}")
        workout.days.forEach { d ->
            println("  giorno ${d.dayIndex} ${d.title} riposo=${d.isRestDay} gruppi=${d.muscleGroups} set=${d.totalSets}")
            d.exercises.forEach { e ->
                println(
                    "     ${e.exerciseName} ${e.sets}x${e.minReps}-${e.maxReps} rec=${e.restSeconds}s " +
                        "RIR=${e.targetRir} cat=${e.category} tecnica=${e.intensityTechnique}"
                )
            }
        }
        db.close()
    }
}
