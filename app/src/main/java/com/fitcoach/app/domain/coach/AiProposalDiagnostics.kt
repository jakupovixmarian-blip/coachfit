package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import java.util.Locale
import kotlin.math.abs

/**
 * Diagnostica deterministica della proposta AI. Per ogni vincolo fallito
 * riporta il valore atteso e il valore realmente restituito dal modello, così
 * l'errore mostrato all'utente indica la causa esatta e non un messaggio generico.
 *
 * INVARIANTE: questa diagnostica deve coprire OGNI controllo di
 * [ProgramGenerationEngine.validate]. Se validate boccia una proposta, analyze
 * deve spiegarne il motivo: non deve mai cadere nel messaggio generico di
 * fallback. Il test `diagnose_spiega_ogni_bocciatura_di_validate` verifica
 * l'invariante.
 */
object AiProposalDiagnostics {

    /** Messaggio usato solo se nessun controllo noto spiega la bocciatura. */
    const val GENERIC_FALLBACK =
        "nessun vincolo elementare violato: verificare struttura JSON e schema dei campi"

    fun analyze(
        profile: UserProfile,
        proposal: AiProgramProposal,
        foods: List<FoodItem>,
        exercises: List<Exercise>,
        scope: ProgramGenerationEngine.Scope = ProgramGenerationEngine.Scope.BOTH
    ): List<String> {
        val errors = mutableListOf<String>()
        val includeNutrition = scope != ProgramGenerationEngine.Scope.WORKOUT
        val includeWorkout = scope != ProgramGenerationEngine.Scope.NUTRITION

        val trainingDays = proposal.workout.days.map { it.dayIndex }.distinct().sorted()
        if (trainingDays.size != profile.trainingDaysPerWeek) {
            errors += "giorni allenamento: attesi ${profile.trainingDaysPerWeek}, " +
                "ricevuti ${trainingDays.size} (${trainingDays.joinToString()})"
        }
        if (trainingDays.any { it !in 0..6 }) {
            errors += "giorni allenamento: indici fuori intervallo 0-6 " +
                "(${trainingDays.filterNot { it in 0..6 }.joinToString()})"
        }
        val expectedRest = (0..6).filterNot { it in trainingDays }
        val calendarWorkout = proposal.calendar.workoutDayIndices.distinct().sorted()
        if (calendarWorkout != trainingDays) {
            errors += "calendario workout: attesi ${trainingDays.joinToString()}, " +
                "ricevuti ${calendarWorkout.joinToString()}"
        }
        val calendarRest = proposal.calendar.restDayIndices.distinct().sorted()
        if (calendarRest != expectedRest) {
            errors += "calendario riposo: attesi ${expectedRest.joinToString()}, " +
                "ricevuti ${calendarRest.joinToString()}"
        }

        val nutritionIndices = proposal.nutrition.days.map { it.dayIndex }.sorted()
        if (nutritionIndices != (0..6).toList()) {
            errors += "giorni nutrizionali: attesi 0..6, ricevuti ${nutritionIndices.joinToString()}"
        }

        val energy = EnergyCalculator.compute(profile)
        if (energy == null) {
            errors += "energia: profilo non calcolabile (dati essenziali mancanti)"
        } else if (includeNutrition) {
            val cycling = if (CarbCyclingEngine.shouldApplyCarbCycling(profile)) {
                CarbCyclingEngine.build(profile, energy)
            } else {
                CarbCyclingEngine.buildUniform(profile, energy)
            }
            var weeklyActual = 0.0
            proposal.nutrition.days.forEach { day ->
                val type = parseDayType(day.dayType)
                val target = type?.let { cycling.targets[it] }
                if (target == null) {
                    errors += "giorno ${day.dayIndex}: tipo nutrizionale '${day.dayType}' non valido " +
                        "(attesi HIGH, MEDIUM o LOW)"
                    return@forEach
                }
                if (day.meals.isEmpty() || day.meals.size !in 3..6) {
                    errors += "giorno ${day.dayIndex}: ${day.meals.size} pasti, attesi da 3 a 6"
                }
                var actual = MacroTotals.ZERO
                day.meals.forEach { meal ->
                    if (parseMealSlot(meal.slot) == null) {
                        errors += "giorno ${day.dayIndex}: slot pasto '${meal.slot}' non valido " +
                            "(attesi BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER, EVENING_SNACK)"
                    }
                    if (meal.entries.isEmpty()) {
                        errors += "giorno ${day.dayIndex}, pasto '${meal.slot}': nessun alimento"
                    }
                    meal.entries.forEach { entry ->
                        if (entry.grams !in 1.0..2_000.0) {
                            errors += "giorno ${day.dayIndex}: '${entry.foodName}' con " +
                                "%.0f g, attesi da 1 a 2000 g".format(entry.grams)
                        }
                        val food = foods.firstOrNull {
                            it.normalizedName == FoodItem.normalizeName(entry.foodName)
                        }
                        if (food == null) {
                            errors += "giorno ${day.dayIndex}: alimento '${entry.foodName}' non presente nel database"
                        } else {
                            val factor = entry.grams / 100.0
                            actual += MacroTotals(
                                food.kcalPer100g * factor,
                                food.proteinPer100g * factor,
                                food.carbsPer100g * factor,
                                food.fatPer100g * factor
                            )
                        }
                    }
                }
                weeklyActual += actual.kcal
                if (!close(actual.kcal, target.kcal, 0.20)) {
                    errors += "giorno ${day.dayIndex} kcal: attese %.0f, ricevute %.0f"
                        .format(target.kcal, actual.kcal)
                }
                if (!close(actual.protein, target.protein, 0.25)) {
                    errors += "giorno ${day.dayIndex} proteine: attese %.0f g, ricevute %.0f g"
                        .format(target.protein, actual.protein)
                }
                if (!close(actual.carbs, target.carbs, 0.30)) {
                    errors += "giorno ${day.dayIndex} carboidrati: attesi %.0f g, ricevuti %.0f g"
                        .format(target.carbs, actual.carbs)
                }
                if (!close(actual.fat, target.fat, 0.30)) {
                    errors += "giorno ${day.dayIndex} grassi: attesi %.0f g, ricevuti %.0f g"
                        .format(target.fat, actual.fat)
                }
            }
            if (!close(weeklyActual, cycling.weeklyKcal, 0.20)) {
                errors += "totale settimanale kcal: attese %.0f, ricevute %.0f"
                    .format(cycling.weeklyKcal, weeklyActual)
            }
        }

        if (includeWorkout) {
            val known = exercises.associateBy { normalize(it.name) }
            val weeklySetsPerExercise = mutableMapOf<Long, Int>()
            proposal.workout.days.forEach { day ->
                if (day.exercises.isEmpty()) {
                    errors += "giorno workout ${day.dayIndex}: nessun esercizio (seduta vuota)"
                }
                val sessionSets = day.exercises.sumOf { it.sets }
                if (sessionSets > 24) {
                    errors += "giorno workout ${day.dayIndex}: $sessionSets serie, massimo sicuro 24"
                } else if (sessionSets in 1..5) {
                    errors += "giorno workout ${day.dayIndex}: $sessionSets serie, minimo 6 per seduta"
                }
                day.exercises.forEach { item ->
                    val exercise = known[normalize(item.exerciseName)]
                    if (exercise == null) {
                        errors += "giorno workout ${day.dayIndex}: esercizio '${item.exerciseName}' non presente nel database"
                    } else {
                        weeklySetsPerExercise.merge(exercise.id, item.sets, Int::plus)
                        if (!allowedExercise(exercise, profile)) {
                            errors += "giorno workout ${day.dayIndex}: esercizio '${item.exerciseName}' " +
                                "vietato o incompatibile con attrezzatura/limitazioni"
                        }
                    }
                    if (item.sets !in 1..8 || item.minReps !in 1..30 || item.maxReps !in item.minReps..30) {
                        errors += "${item.exerciseName}: serie/ripetizioni non valide " +
                            "(${item.sets} serie, ${item.minReps}-${item.maxReps} rip.)"
                    }
                    if (item.restSeconds !in 15..600) {
                        errors += "${item.exerciseName}: recupero ${item.restSeconds}s, atteso da 15 a 600s"
                    }
                    if (item.targetRir !in 0..5) {
                        errors += "${item.exerciseName}: RIR ${item.targetRir}, atteso da 0 a 5"
                    }
                }
            }
            weeklySetsPerExercise.filterValues { it > 24 }.forEach { (exerciseId, sets) ->
                val name = exercises.firstOrNull { it.id == exerciseId }?.name ?: "id=$exerciseId"
                errors += "esercizio '$name': $sets serie settimanali, massimo sicuro 24"
            }
        }

        return errors.ifEmpty { listOf(GENERIC_FALLBACK) }
    }

    private fun allowedExercise(exercise: Exercise, profile: UserProfile): Boolean {
        val equipmentOk = profile.availableEquipment.isEmpty() || exercise.bodyweightOnly ||
            exercise.equipment.isEmpty() || exercise.equipment.any { it in profile.availableEquipment }
        val avoided = profile.movementsToAvoid.any {
            exercise.name.contains(it, true) || exercise.primaryMuscle.label.contains(it, true)
        }
        return equipmentOk && !avoided && exercise.name !in profile.dislikedExercises
    }

    private fun close(actual: Double, target: Double, tolerance: Double): Boolean =
        target > 0 && abs(actual - target) / target <= tolerance

    private fun parseDayType(raw: String): CarbDayType? = runCatching {
        CarbDayType.valueOf(raw.trim().uppercase().replace(" ", "_"))
    }.getOrNull()

    private fun parseMealSlot(raw: String): MealSlot? = runCatching {
        MealSlot.valueOf(raw.trim().uppercase().replace(" ", "_"))
    }.getOrNull()

    private fun normalize(raw: String): String = raw.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9àèéìòù]+"), " ").trim()
}
