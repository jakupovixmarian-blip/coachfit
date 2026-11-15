package com.fitcoach.app.support

import com.fitcoach.app.domain.coach.AiCalendarProposal
import com.fitcoach.app.domain.coach.AiFoodEntry
import com.fitcoach.app.domain.coach.AiMeal
import com.fitcoach.app.domain.coach.AiNutritionDay
import com.fitcoach.app.domain.coach.AiNutritionProposal
import com.fitcoach.app.domain.coach.AiPlannedExercise
import com.fitcoach.app.domain.coach.AiProgramProposal
import com.fitcoach.app.domain.coach.AiWorkoutDay
import com.fitcoach.app.domain.coach.AiWorkoutProposal
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.MealPlanGenerator
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import com.fitcoach.app.domain.workout.WorkoutPlanValidator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Costruisce una PROPOSTA AI valida a partire dai motori matematici rimasti
 * (calcoli energetici e carb cycling) e dai cataloghi reali di alimenti/esercizi.
 *
 * Serve SOLO ai test: non esiste piu un generatore deterministico nel codice di
 * produzione. Qui simuliamo cio che farebbe il modello (l'unica autorita
 * decisionale) componendo una struttura conforme allo schema `submit_full_program`,
 * cosi da poter verificare la pipeline reale (validazione + persistenza) senza un
 * provider LLM. I target calorici e i macro provengono dai motori reali: non sono
 * numeri inventati.
 */
object SimulatedAiPrograms {

    private val fiveMealShares = listOf(
        MealSlot.BREAKFAST to Triple(0.20, 0.25, 0.30),
        MealSlot.MORNING_SNACK to Triple(0.15, 0.10, 0.10),
        MealSlot.LUNCH to Triple(0.30, 0.30, 0.25),
        MealSlot.AFTERNOON_SNACK to Triple(0.15, 0.10, 0.10),
        MealSlot.DINNER to Triple(0.20, 0.25, 0.25)
    )

    fun proposal(
        profile: UserProfile,
        foods: List<FoodItem>,
        exercises: List<Exercise>,
        trainingDays: List<Int> = WorkoutPlanBuilder.defaultTrainingDays(profile.trainingDaysPerWeek)
    ): AiProgramProposal {
        val energy = EnergyCalculator.compute(profile)
            ?: error("Profilo senza dati energetici sufficienti: il profilo di test deve essere completo.")
        val applied = CarbCyclingEngine.shouldApplyCarbCycling(profile)
        val cycling = if (applied) CarbCyclingEngine.build(profile, energy)
        else CarbCyclingEngine.buildUniform(profile, energy)
        val dayTypes = if (applied) {
            CarbCyclingEngine.assignDayTypes(
                cycling.highDays, cycling.mediumDays, cycling.lowDays, trainingDays
            )
        } else {
            List(7) { CarbDayType.MEDIUM }
        }

        val byName = foods.associateBy { it.name }
        val bank = buildBank(foods)
        val nutritionDays = (0 until 7).map { dayIndex ->
            val type = dayTypes.getOrElse(dayIndex) { CarbDayType.MEDIUM }
            val target = cycling.targets[type] ?: cycling.targets[CarbDayType.MEDIUM]!!
            val meals = fiveMealShares.mapIndexed { mealIndex, (slot, shares) ->
                buildMeal(byName, bank, slot, shares, target.protein, target.carbs, target.fat, dayIndex, mealIndex)
            }
            AiNutritionDay(dayIndex = dayIndex, dayType = type.name, meals = meals)
        }

        val split = splitFor(trainingDays.size)
        val workoutDays = trainingDays.sorted().map { dayIndex ->
            val groups = groupsFor(split, trainingDays, dayIndex)
            val chosen = chooseExercises(groups, exercises, profile, dayIndex)
            AiWorkoutDay(
                dayIndex = dayIndex,
                title = titleFor(split),
                muscleGroups = chosen.map { it.primaryMuscle.name }.distinct(),
                exercises = chosen.map { ex ->
                    AiPlannedExercise(
                        exerciseName = ex.name,
                        sets = 4,
                        minReps = ex.minReps.coerceIn(1, 30),
                        maxReps = ex.maxReps.coerceIn(ex.minReps.coerceIn(1, 30), 30),
                        restSeconds = ex.restSeconds.coerceIn(15, 600),
                        targetRir = ex.targetRir.coerceIn(0, 5),
                        technique = "NONE",
                        techniqueReason = ""
                    )
                }
            )
        }

        return AiProgramProposal(
            strategy = "Percorso personalizzato simulato per i test (numeri dai motori matematici reali).",
            nutrition = AiNutritionProposal(days = nutritionDays),
            workout = AiWorkoutProposal(
                split = split.name,
                rationale = "Split scelto in base ai giorni disponibili.",
                days = workoutDays
            ),
            calendar = AiCalendarProposal(
                workoutDayIndices = trainingDays.sorted(),
                restDayIndices = (0..6).filterNot { it in trainingDays }
            )
        )
    }

    private data class Bank(
        val proteins: List<FoodItem>,
        val carbs: List<FoodItem>,
        val fats: List<FoodItem>,
        val veggies: List<FoodItem>,
        val fruits: List<FoodItem>
    )

    private fun buildBank(foods: List<FoodItem>): Bank {
        fun cat(vararg c: String) = foods.filter { it.category in c }
        val leanProteins = foods.filter {
            it.proteinPer100g >= 15.0 && it.carbsPer100g <= 15.0 && it.fatPer100g <= 10.0
        }
        val carbPool = cat("Cereali", "Tuberi")
        val fatPool = cat("Grassi", "Frutta secca")
        return Bank(
            proteins = (leanProteins.ifEmpty { foods.filter { it.proteinPer100g >= 10.0 } })
                .sortedByDescending { it.proteinPer100g },
            carbs = carbPool.filter { it.carbsPer100g >= 30.0 }.ifEmpty { carbPool }
                .sortedByDescending { it.carbsPer100g },
            fats = fatPool.filter { it.fatPer100g >= 60.0 }.ifEmpty { fatPool }
                .sortedByDescending { it.fatPer100g },
            veggies = cat("Verdura"),
            fruits = cat("Frutta")
        )
    }

    private fun <T> pick(list: List<T>, seed: Int): T? =
        if (list.isEmpty()) null else list[((seed % list.size) + list.size) % list.size]

    private fun buildMeal(
        byName: Map<String, FoodItem>,
        bank: Bank,
        slot: MealSlot,
        shares: Triple<Double, Double, Double>,
        proteinTarget: Double,
        carbsTarget: Double,
        fatTarget: Double,
        dayIndex: Int,
        mealIndex: Int
    ): AiMeal {
        val (pShare, cShare, fShare) = shares
        val entries = mutableListOf<AiFoodEntry>()
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0

        fun add(food: FoodItem, grams: Double) {
            entries += AiFoodEntry(foodName = food.name, grams = grams.toInt().toDouble(), unit = food.defaultUnit.name)
            val f = grams / 100.0
            protein += food.proteinPer100g * f
            carbs += food.carbsPer100g * f
            fat += food.fatPer100g * f
        }

        val isBreakfast = slot == MealSlot.BREAKFAST
        val veg = if (isBreakfast) pick(bank.fruits, dayIndex + mealIndex)
        else pick(bank.veggies, dayIndex + mealIndex)
        veg?.let { add(it, if (isBreakfast) 100.0 else 150.0) }

        val carb = pick(bank.carbs, dayIndex * 3 + mealIndex)
        val remainingCarbs = (carbsTarget * cShare - carbs).coerceAtLeast(0.0)
        if (carb != null && remainingCarbs >= 5.0) {
            add(carb, MealPlanGenerator.gramsForMacro(remainingCarbs, carb.carbsPer100g).coerceIn(15.0, 350.0))
        }

        val proteinFood = pick(bank.proteins, dayIndex * 5 + mealIndex)
        val remainingProtein = (proteinTarget * pShare - protein).coerceAtLeast(0.0)
        if (proteinFood != null && remainingProtein > 1.0) {
            add(proteinFood, MealPlanGenerator.gramsForMacro(remainingProtein, proteinFood.proteinPer100g).coerceIn(20.0, 400.0))
        }

        val fatFood = pick(bank.fats, dayIndex * 2 + mealIndex)
        val remainingFat = (fatTarget * fShare - fat).coerceAtLeast(0.0)
        if (fatFood != null && remainingFat >= 1.0) {
            add(fatFood, MealPlanGenerator.gramsForMacro(remainingFat, fatFood.fatPer100g).coerceIn(2.0, 60.0))
        }

        return AiMeal(slot = slot.name, time = slot.defaultTime, entries = entries)
    }

    fun splitFor(daysPerWeek: Int): SplitType = when {
        daysPerWeek <= 3 -> SplitType.FULL_BODY
        daysPerWeek == 4 -> SplitType.UPPER_LOWER
        else -> SplitType.PUSH_PULL_LEGS
    }

    private fun titleFor(split: SplitType): String = when (split) {
        SplitType.FULL_BODY -> "Full Body"
        SplitType.UPPER_LOWER -> "Upper / Lower"
        SplitType.PUSH_PULL_LEGS -> "Push / Pull / Legs"
        SplitType.CUSTOM -> "Split personalizzata"
    }

    private fun groupsFor(split: SplitType, trainingDays: List<Int>, dayIndex: Int): Set<MuscleGroup> {
        val position = trainingDays.sorted().indexOf(dayIndex)
        val upper = setOf(
            MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
            MuscleGroup.BICEPS, MuscleGroup.TRICEPS
        )
        val lower = setOf(
            MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
            MuscleGroup.CALVES, MuscleGroup.CORE
        )
        return when (split) {
            SplitType.FULL_BODY -> setOf(
                MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
                MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
                MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.CORE
            )
            SplitType.UPPER_LOWER -> if (position % 2 == 0) upper else lower
            SplitType.PUSH_PULL_LEGS -> when (position % 3) {
                0 -> setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
                1 -> setOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.CORE)
                else -> setOf(
                    MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES
                )
            }
            SplitType.CUSTOM -> if (position % 2 == 0) upper else lower
        }
    }

    /** Seleziona esercizi compatibili coprendo i gruppi previsti, con volume controllato. */
    private fun chooseExercises(
        groups: Set<MuscleGroup>,
        exercises: List<Exercise>,
        profile: UserProfile,
        dayIndex: Int
    ): List<Exercise> {
        val compatible = exercises.filter { WorkoutPlanValidator.isCompatible(it, profile) }
        val chosen = mutableListOf<Exercise>()
        val used = mutableSetOf<Long>()

        fun rotated(list: List<Exercise>): List<Exercise> {
            if (list.isEmpty()) return list
            val k = dayIndex % list.size
            return list.drop(k) + list.take(k)
        }

        val ordered = groups.sortedByDescending { it.isLargeGroup }
        for (g in ordered) {
            if (chosen.size >= 6) break
            val candidate = rotated(
                compatible.filter { it.primaryMuscle == g && it.id !in used }
                    .sortedWith(
                        compareByDescending<Exercise> { it.category == ExerciseCategory.COMPOUND }
                            .thenBy { it.difficulty }
                    )
            ).firstOrNull() ?: continue
            chosen += candidate
            used += candidate.id
        }
        for (g in ordered.filter { it.isLargeGroup }) {
            if (chosen.size >= 6) break
            val candidate = compatible
                .filter { it.primaryMuscle == g && it.id !in used }
                .sortedWith(compareByDescending<Exercise> { it.category == ExerciseCategory.COMPOUND })
                .firstOrNull() ?: continue
            chosen += candidate
            used += candidate.id
        }
        return chosen
    }

    /** Serializza la proposta aggiungendo l'ambito richiesto dallo strumento. */
    fun json(json: Json, proposal: AiProgramProposal, scope: String? = null): String {
        val base = json.encodeToString(AiProgramProposal.serializer(), proposal)
        if (scope == null) return base
        val obj = json.parseToJsonElement(base) as JsonObject
        return json.encodeToString(
            JsonObject.serializer(),
            JsonObject(obj + ("scope" to JsonPrimitive(scope)))
        )
    }
}
