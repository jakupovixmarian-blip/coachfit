package com.fitcoach.app.domain.coach

import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.MacroTotals
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.BreakfastConstraint
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.CatalogNameResolver
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.MealSlots
import com.fitcoach.app.domain.workout.CatalogBounds
import com.fitcoach.app.domain.workout.EquipmentFit
import com.fitcoach.app.domain.workout.VolumeGuidelines
import kotlin.math.abs

/**
 * Unica autorita di validazione della proposta AI.
 *
 * Perche esiste: `ProgramGenerationEngine.validate` (che decide il salvataggio)
 * e `AiProposalDiagnostics.analyze` (che spiega l'errore al modello) erano due
 * implementazioni della stessa regola, e sono divergite piu' volte. Il vincolo
 * dolce/salato della colazione esisteva solo nella diagnostica, quindi il piano
 * veniva respinto con un messaggio chiaro e poi **salvato se il modello
 * insisteva**. Le soglie di volume erano `6..24` fisse nel cancello e dinamiche
 * nella diagnostica: due verdetti diversi sullo stesso piano.
 *
 * Ora c'e' un solo passaggio che produce sia l'esito sia i motivi. Il cancello
 * e' `violations.isEmpty()`: non puo' piu' accettare cio' che la diagnostica
 * condanna, perche' e' la stessa funzione.
 */
object ProposalChecker {

    /** Messaggio usato solo se nessun controllo noto spiega la bocciatura. */
    const val GENERIC_FALLBACK =
        "nessun vincolo elementare violato: verificare struttura JSON e schema dei campi"

    data class Report(val violations: List<String>) {
        val isValid: Boolean get() = violations.isEmpty()
    }

    fun check(
        profile: UserProfile,
        proposal: AiProgramProposal,
        foods: List<FoodItem>,
        exercises: List<Exercise>,
        scope: ProgramGenerationEngine.Scope = ProgramGenerationEngine.Scope.BOTH
    ): Report {
        val violations = mutableListOf<String>()
        val includeNutrition = scope != ProgramGenerationEngine.Scope.WORKOUT
        val includeWorkout = scope != ProgramGenerationEngine.Scope.NUTRITION

        val trainingDays = proposal.workout.days.map { it.dayIndex }.distinct().sorted()
        checkTrainingCalendar(profile, trainingDays, violations)

        if (includeNutrition) {
            checkNutrition(profile, proposal, foods, violations)
        }
        if (includeWorkout) {
            checkWorkout(profile, proposal, exercises, violations)
        }

        return Report(violations)
    }

    private fun checkTrainingCalendar(
        profile: UserProfile,
        trainingDays: List<Int>,
        violations: MutableList<String>
    ) {
        if (trainingDays.size != profile.trainingDaysPerWeek) {
            violations += "giorni allenamento: attesi ${profile.trainingDaysPerWeek}, " +
                "ricevuti ${trainingDays.size} (${trainingDays.joinToString()}). " +
                "days deve elencare solo le sedute: i giorni di riposo non vanno inseriti."
        }
        if (trainingDays.any { it !in 0..6 }) {
            violations += "giorni allenamento: indici fuori intervallo 0-6 " +
                "(${trainingDays.filterNot { it in 0..6 }.joinToString()})"
        }
        // Il calendario non si controlla: e' derivato da workout.days in
        // SubmitFullProgramTool, quindi non puo' divergere.
    }

    private fun checkNutrition(
        profile: UserProfile,
        proposal: AiProgramProposal,
        foods: List<FoodItem>,
        violations: MutableList<String>
    ) {
        val nutritionIndices = proposal.nutrition.days.map { it.dayIndex }.sorted()
        if (nutritionIndices != (0..6).toList()) {
            violations += "giorni nutrizionali: attesi 0..6, ricevuti ${nutritionIndices.joinToString()}"
        }

        val energy = EnergyCalculator.compute(profile)
        if (energy == null) {
            violations += "energia: profilo non calcolabile (dati essenziali mancanti)"
            return
        }

        val cycling = if (CarbCyclingEngine.shouldApplyCarbCycling(profile)) {
            CarbCyclingEngine.build(profile, energy)
        } else {
            CarbCyclingEngine.buildUniform(profile, energy)
        }
        // Stesso elenco che il prompt impone al modello: numero E identita'
        // degli slot, cosi' un pasto sostituito da un altro non passa.
        val expectedSlots = MealSlots.forMealsPerDay(profile.mealsPerDay).toSet()

        var weeklyActual = 0.0
        proposal.nutrition.days.forEach { day ->
            val type = parseDayType(day.dayType)
            val target = type?.let { cycling.targets[it] }
            if (target == null) {
                violations += "giorno ${day.dayIndex}: tipo nutrizionale '${day.dayType}' non valido " +
                    "(attesi HIGH, MEDIUM o LOW)"
                return@forEach
            }
            if (day.meals.size != profile.mealsPerDay) {
                violations += "giorno ${day.dayIndex}: ${day.meals.size} pasti, " +
                    "il profilo ne prevede ${profile.mealsPerDay}"
            }
            var actual = MacroTotals.ZERO
            day.meals.forEach { meal ->
                val slot = parseMealSlot(meal.slot)
                if (slot == null) {
                    violations += "giorno ${day.dayIndex}: slot pasto '${meal.slot}' non valido " +
                        "(attesi BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER, EVENING_SNACK)"
                } else if (slot !in expectedSlots) {
                    violations += "giorno ${day.dayIndex}: slot '${meal.slot}' fuori dagli slot " +
                        "previsti dal profilo (${expectedSlots.joinToString { it.name }})"
                }
                if (meal.entries.isEmpty()) {
                    violations += "giorno ${day.dayIndex}, pasto '${meal.slot}': nessun alimento"
                }
                meal.entries.forEach { entry ->
                    if (entry.grams !in 1.0..2_000.0) {
                        violations += "giorno ${day.dayIndex}: '${entry.foodName}' con " +
                            "%.0f g, attesi da 1 a 2000 g".format(entry.grams)
                    }
                    // Confronto tollerante: l'AI scrive "Uova intere" dove il
                    // catalogo dice "Uovo intero di gallina".
                    val food = CatalogNameResolver.resolveFood(entry.foodName, foods)
                    if (food == null) {
                        val hint = CatalogNameResolver.foodSuggestions(entry.foodName, foods)
                        violations += "giorno ${day.dayIndex}: alimento '${entry.foodName}' non presente nel database" +
                            if (hint.isEmpty()) {
                                " (nessun nome simile: scegli un alimento dall'elenco alimenti_validi)"
                            } else {
                                " (usa esattamente uno di questi nomi: ${hint.joinToString()})"
                            }
                    } else {
                        if (slot != null) {
                            BreakfastConstraint.violations(
                                profile.breakfastPreference, slot, listOf(food)
                            ).firstOrNull()?.let { offending ->
                                val actualFlavor = BreakfastConstraint.flavorOf(offending)
                                val wanted = if (actualFlavor == BreakfastConstraint.Flavor.SAVORY) "dolce" else "salata"
                                val suitable = BreakfastConstraint.suitable(profile.breakfastPreference, foods)
                                violations += "giorno ${day.dayIndex}, colazione: " +
                                    "'${offending.name}' e ${actualFlavor.name.lowercase()}, ma la colazione " +
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
                violations += "giorno ${day.dayIndex} kcal: attese %.0f, ricevute %.0f"
                    .format(target.kcal, actual.kcal)
            }
            if (!close(actual.protein, target.protein, 0.25)) {
                violations += "giorno ${day.dayIndex} proteine: attese %.0f g, ricevute %.0f g"
                    .format(target.protein, actual.protein)
            }
            if (!close(actual.carbs, target.carbs, 0.30)) {
                violations += "giorno ${day.dayIndex} carboidrati: attesi %.0f g, ricevuti %.0f g"
                    .format(target.carbs, actual.carbs)
            }
            if (!close(actual.fat, target.fat, 0.30)) {
                violations += "giorno ${day.dayIndex} grassi: attesi %.0f g, ricevuti %.0f g"
                    .format(target.fat, actual.fat)
            }
        }
        if (!close(weeklyActual, cycling.weeklyKcal, 0.20)) {
            violations += "totale settimanale kcal: attese %.0f, ricevute %.0f"
                .format(cycling.weeklyKcal, weeklyActual)
        }
    }

    private fun checkWorkout(
        profile: UserProfile,
        proposal: AiProgramProposal,
        exercises: List<Exercise>,
        violations: MutableList<String>
    ) {
        fun findExercise(rawName: String): Exercise? =
            CatalogNameResolver.resolveExercise(rawName, exercises)

        val weeklySetsPerExercise = mutableMapOf<Long, Int>()
        proposal.workout.days.forEach { day ->
            if (day.exercises.isEmpty()) {
                violations += "giorno workout ${day.dayIndex}: nessun esercizio (seduta vuota)"
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
                violations += "giorno workout ${day.dayIndex}: $sessionSets serie, " +
                    "massimo per questi esercizi $sessionCeiling"
            } else if (sessionSets < VolumeGuidelines.MIN_SETS_PER_SESSION) {
                violations += "giorno workout ${day.dayIndex}: $sessionSets serie, " +
                    "minimo ${VolumeGuidelines.MIN_SETS_PER_SESSION} per seduta"
            }
            // Una seduta di soli multiarticolari lascia scoperti i distretti che
            // il multiarticolare non stimola (deltoide laterale/posteriore,
            // bicipiti, tricipiti, femorali, polpacci). Prima passava la
            // validazione: il modello non veniva mai richiamato sull'isolamento.
            // L'obbligo scatta solo se il catalogo ha un isolamento eseguibile
            // con l'attrezzatura dichiarata: chi ha solo corpo libero non ha
            // alternative e resterebbe bloccato senza poter correggere nulla.
            val resolved = day.exercises.mapNotNull { findExercise(it.exerciseName) }
            val hasIsolation = resolved.any { it.category == ExerciseCategory.ISOLATION }
            val hasCompound = resolved.any { it.category == ExerciseCategory.COMPOUND }
            val isolationAvailable = exercises.any {
                it.category == ExerciseCategory.ISOLATION && allowedExercise(it, profile)
            }
            if (hasCompound && !hasIsolation && resolved.size >= 3 && isolationAvailable) {
                violations += "giorno workout ${day.dayIndex}: nessun esercizio di isolamento " +
                    "(${resolved.size} esercizi, tutti multiarticolari). Aggiungi almeno un " +
                    "isolamento per i gruppi che restano scoperti."
            }
            day.exercises.forEach { item ->
                // Un JSON con nomi di campo diversi produceva esercizi con nome
                // vuoto e 0 ripetizioni: la diagnosi parlava di "esercizio ''".
                if (item.exerciseName.isBlank()) {
                    violations += "giorno workout ${day.dayIndex}: esercizio senza nome " +
                        "(campo 'exerciseName' mancante o vuoto)"
                    return@forEach
                }
                if (item.sets < 1) {
                    violations += "giorno workout ${day.dayIndex}: '${item.exerciseName}' senza serie " +
                        "(campo 'sets' mancante o zero)"
                    return@forEach
                }
                if (item.minReps < 1 || item.maxReps < item.minReps) {
                    violations += "giorno workout ${day.dayIndex}: '${item.exerciseName}' con ripetizioni " +
                        "non valide (${item.minReps}-${item.maxReps}): indica minReps e maxReps"
                    return@forEach
                }
                val exercise = findExercise(item.exerciseName)
                if (exercise == null) {
                    val hint = CatalogNameResolver.exerciseSuggestions(item.exerciseName, exercises)
                    violations += "giorno workout ${day.dayIndex}: esercizio '${item.exerciseName}' non presente nel database" +
                        if (hint.isEmpty()) {
                            " (nessun nome simile: scegli dall'elenco esercizi_validi)"
                        } else {
                            " (usa esattamente uno di questi nomi: ${hint.joinToString()})"
                        }
                } else {
                    weeklySetsPerExercise.merge(exercise.id, item.sets, Int::plus)
                    if (!allowedExercise(exercise, profile)) {
                        violations += "giorno workout ${day.dayIndex}: esercizio '${item.exerciseName}' " +
                            "vietato o incompatibile con attrezzatura/limitazioni"
                    }
                    // Serie, ripetizioni, recupero e RIR si verificano contro il
                    // record del catalogo: Plank e' prescritto a 30-60 s e un
                    // tetto fisso rendeva impossibile salvarlo.
                    violations += CatalogBounds.violations(
                        exercise, item.sets, item.minReps, item.maxReps,
                        item.restSeconds, item.targetRir
                    )
                }
            }
        }
        weeklySetsPerExercise.forEach { (exerciseId, sets) ->
            val exercise = exercises.firstOrNull { it.id == exerciseId } ?: return@forEach
            val perExercise = VolumeGuidelines.maxSetsPerExercise(exercise.category)
            val weeklyCeiling = perExercise * profile.trainingDaysPerWeek.coerceAtLeast(1)
            if (sets > weeklyCeiling) {
                violations += "esercizio '${exercise.name}': $sets serie settimanali, " +
                    "massimo $weeklyCeiling ($perExercise per seduta su " +
                    "${profile.trainingDaysPerWeek} sedute)"
            }
        }
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
}
