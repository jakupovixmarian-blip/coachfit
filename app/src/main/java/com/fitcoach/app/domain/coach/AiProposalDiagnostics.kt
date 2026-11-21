package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.workout.EquipmentFit
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.BreakfastConstraint
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.CatalogNameResolver
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.workout.CatalogBounds
import com.fitcoach.app.domain.workout.VolumeGuidelines
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
                // Il numero di pasti e' un dato del profilo, non una costante: il
                // validatore accettava 3..6 ignorando la preferenza dichiarata.
                if (day.meals.size != profile.mealsPerDay) {
                    errors += "giorno ${day.dayIndex}: ${day.meals.size} pasti, " +
                        "il profilo ne prevede ${profile.mealsPerDay}"
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
                        // Confronto tollerante: l'AI scrive "Uova intere" dove il
                        // catalogo dice "Uovo intero di gallina". Il match esatto
                        // bocciava programmi corretti.
                        val food = CatalogNameResolver.resolveFood(entry.foodName, foods)
                        if (food == null) {
                            val hint = CatalogNameResolver.foodSuggestions(entry.foodName, foods)
                            errors += "giorno ${day.dayIndex}: alimento '${entry.foodName}' non presente nel database" +
                                if (hint.isEmpty()) {
                                    " (nessun nome simile: scegli un alimento dall'elenco alimenti_validi)"
                                } else {
                                    " (usa esattamente uno di questi nomi: ${hint.joinToString()})"
                                }
                        } else {
                            // La preferenza dolce/salata a colazione non era
                            // verificata da nulla: il piano usciva con pollo e
                            // riso a colazione pur essendo stata scelta "dolce".
                            val slotOfMeal = parseMealSlot(meal.slot)
                            if (slotOfMeal != null) {
                                BreakfastConstraint.violations(
                                    profile.breakfastPreference, slotOfMeal, listOf(food)
                                ).firstOrNull()?.let { offending ->
                                    val actual = BreakfastConstraint.flavorOf(offending)
                                    val wanted = if (actual == BreakfastConstraint.Flavor.SAVORY) "dolce" else "salata"
                                    val suitable = BreakfastConstraint
                                        .suitable(profile.breakfastPreference, foods)
                                    errors += "giorno ${day.dayIndex}, colazione: " +
                                        "'${offending.name}' e ${actual.name.lowercase()}, ma la colazione " +
                                        "scelta e $wanted: sostituiscilo" +
                                        if (suitable.isEmpty()) " con alimenti adatti"
                                        else " con uno di questi (nome esatto): ${suitable.joinToString()}"
                                }
                            }
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
            fun findExercise(rawName: String): Exercise? =
                CatalogNameResolver.resolveExercise(rawName, exercises)
            val weeklySetsPerExercise = mutableMapOf<Long, Int>()
            proposal.workout.days.forEach { day ->
                if (day.exercises.isEmpty()) {
                    errors += "giorno workout ${day.dayIndex}: nessun esercizio (seduta vuota)"
                }
                // Il tetto di seduta dipende dagli esercizi scelti: la somma dei
                // massimi per categoria, non un numero universale valido anche per
                // una seduta di soli multiarticolari.
                val sessionSets = day.exercises.sumOf { it.sets }
                val sessionCeiling = VolumeGuidelines.sessionCeiling(
                    day.exercises.map { item ->
                        findExercise(item.exerciseName)?.category ?: ExerciseCategory.ISOLATION
                    }
                )
                if (sessionSets > sessionCeiling) {
                    errors += "giorno workout ${day.dayIndex}: $sessionSets serie, " +
                        "massimo per questi esercizi $sessionCeiling"
                } else if (sessionSets < VolumeGuidelines.MIN_SETS_PER_SESSION) {
                    errors += "giorno workout ${day.dayIndex}: $sessionSets serie, " +
                        "minimo ${VolumeGuidelines.MIN_SETS_PER_SESSION} per seduta"
                }
                day.exercises.forEach { item ->
                    // Un JSON con nomi di campo diversi produceva esercizi con
                    // nome vuoto e 0 ripetizioni: la diagnosi parlava di
                    // "esercizio ''", inutile per correggere. Qui il messaggio
                    // dice esattamente quale campo manca.
                    if (item.exerciseName.isBlank()) {
                        errors += "giorno workout ${day.dayIndex}: esercizio senza nome " +
                            "(campo 'exerciseName' mancante o vuoto)"
                        return@forEach
                    }
                    if (item.sets < 1) {
                        errors += "giorno workout ${day.dayIndex}: '${item.exerciseName}' senza serie " +
                            "(campo 'sets' mancante o zero)"
                        return@forEach
                    }
                    if (item.minReps < 1 || item.maxReps < item.minReps) {
                        errors += "giorno workout ${day.dayIndex}: '${item.exerciseName}' con ripetizioni " +
                            "non valide (${item.minReps}-${item.maxReps}): indica minReps e maxReps"
                        return@forEach
                    }
                    val exercise = findExercise(item.exerciseName)
                    if (exercise == null) {
                        val hint = CatalogNameResolver.exerciseSuggestions(item.exerciseName, exercises)
                        errors += "giorno workout ${day.dayIndex}: esercizio '${item.exerciseName}' non presente nel database" +
                            if (hint.isEmpty()) {
                                " (nessun nome simile: scegli dall'elenco esercizi_validi)"
                            } else {
                                " (usa esattamente uno di questi nomi: ${hint.joinToString()})"
                            }
                    } else {
                        weeklySetsPerExercise.merge(exercise.id, item.sets, Int::plus)
                        if (!allowedExercise(exercise, profile)) {
                            errors += "giorno workout ${day.dayIndex}: esercizio '${item.exerciseName}' " +
                                "vietato o incompatibile con attrezzatura/limitazioni"
                        }
                    }
                    // Serie, ripetizioni, recupero e RIR si verificano contro il
                    // record del catalogo, non contro soglie fisse: Plank e' prescritto
                    // a 30-60 s e un tetto fisso a 30 rendeva impossibile salvarlo.
                    if (exercise != null) {
                        errors += CatalogBounds.violations(
                            exercise, item.sets, item.minReps, item.maxReps,
                            item.restSeconds, item.targetRir
                        )
                    } else if (item.sets < 1 || item.minReps < 1 || item.maxReps < item.minReps) {
                        errors += "${item.exerciseName}: serie/ripetizioni non valide " +
                            "(${item.sets} serie, ${item.minReps}-${item.maxReps} rip.)"
                    }
                }
            }
            weeklySetsPerExercise.forEach { (exerciseId, sets) ->
                val exercise = exercises.firstOrNull { it.id == exerciseId } ?: return@forEach
                val perExercise = VolumeGuidelines.maxSetsPerExercise(exercise.category)
                val weeklyCeiling = perExercise * profile.trainingDaysPerWeek.coerceAtLeast(1)
                if (sets > weeklyCeiling) {
                    errors += "esercizio '${exercise.name}': $sets serie settimanali, " +
                        "massimo $weeklyCeiling ($perExercise per seduta su " +
                        "${profile.trainingDaysPerWeek} sedute)"
                }
            }
        }

        return errors.ifEmpty { listOf(GENERIC_FALLBACK) }
    }

    private fun allowedExercise(exercise: Exercise, profile: UserProfile): Boolean {
        val equipmentOk = EquipmentFit.isAvailable(exercise, profile)
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
