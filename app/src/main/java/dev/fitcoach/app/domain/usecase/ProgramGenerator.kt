package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.MovementPattern
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.model.UserProfile
import kotlin.math.roundToInt

/** A single day of the generated program. */
data class ProgramDay(
    val dayIndex: Int,
    val label: String,
    val focus: List<MuscleGroup>,
    val exercises: List<PlannedExercise>
)

data class PlannedExercise(
    val exercise: Exercise,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val rir: Int,
    val restSeconds: Int
)

data class GeneratedProgram(
    val days: List<ProgramDay>,
    val rationale: String,
    val warnings: List<String> = emptyList()
) {
    val exerciseCount: Int get() = days.sumOf { it.exercises.size }
    val totalSets: Int get() = days.sumOf { day -> day.exercises.sumOf { it.sets } }
}

/** Equipment the user can actually access, derived from the onboarding answer. */
private fun UserProfile.availableEquipment(): Set<Equipment> =
    equipment.split(",", ";", "|").mapNotNull { token ->
        val clean = token.trim()
        if (clean.isBlank()) return@mapNotNull null
        Equipment.entries.firstOrNull { it.name.equals(clean, ignoreCase = true) }
    }.toSet()

/**
 * Builds a real multi-session program from the onboarding answers.
 *
 * The number of sessions is exactly [UserProfile.availableDays]. Every session is
 * a different routine with its own split, and the weekly volume per muscle is kept
 * inside the budget for the user's experience level, so a plan can never turn into
 * four identical full-body days or into 30-45 sets for a single muscle.
 */
object ProgramGenerator {

    private const val MIN_SETS_PER_EXERCISE = 2
    private const val MAX_SETS_PER_EXERCISE = 5

    fun generate(
        profile: UserProfile,
        catalog: List<Exercise>
    ): GeneratedProgram {
        val days = (profile.availableDays ?: 3).coerceIn(1, 6)
        val level = profile.experience
        val goal = profile.goal
        val equipment = profile.availableEquipment()
        val injuries = profile.injuries.lowercase()

        val available = catalog
            .filter { !it.isCustom }
            .filter { equipment.isEmpty() || it.equipment in equipment }
            .filter { !isContraindicated(it, injuries) }

        if (available.isEmpty()) {
            return GeneratedProgram(emptyList(), "Nessun esercizio compatibile con attrezzatura e limitazioni.")
        }

        val split = splitFor(days, level)
        val restSeconds = restFor(goal)
        val baseRir = rirFor(level)
        val perSessionBudget = sessionBudget(profile.sessionMinutes ?: 60, restSeconds)

        val sessions = mutableListOf<List<ExerciseWithSets>>()
        val programDays = mutableListOf<ProgramDay>()
        val usedExercises = mutableSetOf<Exercise>()

        val usageCount = mutableMapOf<Exercise, Int>()
        var reuseNote: String? = null

        split.forEach { (dayIndex, focus) ->
            var picks = pickSession(
                available = available,
                focus = focus,
                maxExercises = perSessionBudget,
                level = level,
                alreadyUsed = usedExercises,
                sessionsSoFar = sessions
            )
            // The declared number of days is the hard requirement. When the
            // compatible catalog is too thin to fill a new day with unused
            // exercises, the day is filled by reusing the least-used ones rather
            // than silently dropping the session.
            if (picks.isEmpty()) {
                picks = pickSession(
                    available = available,
                    focus = focus,
                    maxExercises = perSessionBudget,
                    level = level,
                    alreadyUsed = emptySet(),
                    sessionsSoFar = sessions,
                    preferLeastUsed = usageCount
                )
                if (picks.isNotEmpty()) {
                    reuseNote = "Attrezzatura limitata: alcuni esercizi si ripetono tra le sedute."
                }
            }
            if (picks.isNotEmpty()) {
                picks.forEach { usageCount.merge(it.exercise, 1) { a, b -> a + b } }
                usedExercises += picks.map { it.exercise }
                sessions += picks
                programDays += ProgramDay(
                    dayIndex = dayIndex,
                    label = labelFor(focus, dayIndex, split),
                    focus = focus,
                    exercises = picks.map { planned ->
                        PlannedExercise(
                            exercise = planned.exercise,
                            sets = planned.sets,
                            repsMin = repsMin(planned.exercise, goal),
                            repsMax = repsMax(planned.exercise, goal),
                            rir = baseRir,
                            restSeconds = restFor(goal, planned.exercise)
                        )
                    }
                )
            }
        }

        val warnings = buildWarnings(programDays, level) + listOfNotNull(reuseNote)
        return GeneratedProgram(
            days = programDays,
            rationale = buildRationale(profile, programDays, split, level),
            warnings = warnings
        )
    }

    /** Turns the generated days into routines ready to be persisted. */
    fun toRoutines(
        programId: Long,
        programName: String,
        program: GeneratedProgram,
        createdAt: Long
    ): Pair<List<Routine>, List<RoutineExercise>> {
        val routines = program.days.map { day ->
            Routine(
                name = day.label,
                description = "${programName} - ${day.focus.joinToString(", ") { it.label }}",
                isActive = false,
                createdAt = createdAt,
                programId = programId,
                dayIndex = day.dayIndex,
                dayLabel = day.label
            )
        }
        val exercises = program.days.flatMap { day ->
            day.exercises.mapIndexed { index, planned ->
                RoutineExercise(
                    routineId = 0L,
                    exerciseId = planned.exercise.id,
                    orderIndex = index,
                    targetSets = planned.sets,
                    targetRepsMin = planned.repsMin,
                    targetRepsMax = planned.repsMax,
                    targetRir = planned.rir,
                    restSeconds = planned.restSeconds
                )
            }
        }
        return routines to exercises
    }

    // ---- split selection -------------------------------------------------

    /**
     * Muscle focus for each day. The split is chosen from the number of days and
     * the experience level, so four days never collapse into a single full body.
     */
    fun splitFor(days: Int, level: ExperienceLevel): Map<Int, List<MuscleGroup>> = when (days) {
        1 -> mapOf(0 to listOf(MuscleGroup.FULL_BODY, MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADS, MuscleGroup.CORE))
        2 -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, MuscleGroup.BICEPS),
            1 to listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.CORE)
        )
        3 -> if (level == ExperienceLevel.BEGINNER) {
            mapOf(
                0 to listOf(MuscleGroup.FULL_BODY, MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADS, MuscleGroup.CORE),
                1 to listOf(MuscleGroup.FULL_BODY, MuscleGroup.SHOULDERS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                2 to listOf(MuscleGroup.FULL_BODY, MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.GLUTES, MuscleGroup.CALVES)
            )
        } else {
            mapOf(
                0 to listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, MuscleGroup.CORE),
                1 to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS, MuscleGroup.CORE),
                2 to listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
            )
        }
        4 -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
            1 to listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
            2 to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            3 to listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE)
        )
        5 -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            1 to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
            2 to listOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
            3 to listOf(MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            4 to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
        )
        else -> mapOf(
            0 to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            1 to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
            2 to listOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
            3 to listOf(MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            4 to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            5 to listOf(MuscleGroup.FULL_BODY, MuscleGroup.CORE, MuscleGroup.FOREARMS)
        )
    }

    private fun labelFor(focus: List<MuscleGroup>, dayIndex: Int, split: Map<Int, List<MuscleGroup>>): String {
        val hasUpper = focus.any { it in UPPER }
        val hasLower = focus.any { it in LOWER }
        val dayLetter = ('A' + dayIndex)
        return when {
            focus.contains(MuscleGroup.FULL_BODY) -> "Giorno $dayLetter - Total body"
            hasUpper && hasLower -> "Giorno $dayLetter - Total body"
            hasUpper -> "Giorno $dayLetter - Upper ${ordinalOf(focus, split, UPPER)}"
            hasLower -> "Giorno $dayLetter - Lower ${ordinalOf(focus, split, LOWER)}"
            else -> "Giorno $dayLetter - ${focus.firstOrNull()?.label ?: "Seduta"}"
        }
    }

    private fun ordinalOf(
        focus: List<MuscleGroup>,
        split: Map<Int, List<MuscleGroup>>,
        family: Set<MuscleGroup>
    ): Int {
        val matching = split.entries
            .filter { entry -> entry.value.any { it in family } && entry.value.none { it in otherFamily(family) } }
            .sortedBy { it.key }
        val position = matching.indexOfFirst { it.value == focus }
        return if (position >= 0) position + 1 else 1
    }

    private fun otherFamily(family: Set<MuscleGroup>) = if (family == UPPER) LOWER else UPPER

    private val UPPER = setOf(
        MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
        MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS
    )
    private val LOWER = setOf(
        MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES
    )

    // ---- exercise selection ---------------------------------------------

    /**
     * Chooses the exercises for one session: the largest compound movement of each
     * focus muscle first, then supporting work, never repeating an exercise that is
     * already used in the program and never exceeding the effective volume ceiling.
     */
    private fun pickSession(
        available: List<Exercise>,
        focus: List<MuscleGroup>,
        maxExercises: Int,
        level: ExperienceLevel,
        alreadyUsed: Set<Exercise>,
        sessionsSoFar: List<List<ExerciseWithSets>>,
        preferLeastUsed: Map<Exercise, Int> = emptyMap()
    ): List<ExerciseWithSets> {
        val selected = mutableListOf<ExerciseWithSets>()
        val usedHere = mutableSetOf<Exercise>()
        val usedPatterns = mutableSetOf<MovementPattern>()
        val effective = mutableMapOf<MuscleGroup, Double>()
        val ceiling = VolumeCalculator.sessionEffectiveCeiling(level)
        val weeklyDirect = VolumeCalculator.weeklyDirectSets(sessionsSoFar)
        val weeklyCeiling = VolumeCalculator.weeklyDirectCeiling(level)

        val candidates = available
            .filter { it.primaryMuscle in focus }
            .sortedWith(
                compareByDescending<Exercise> { it.secondaryMuscles.size }
                    .thenBy { preferLeastUsed[it] ?: 0 }
                    .thenByDescending { it.primaryMuscle in focus }
            )

        // Round one: one compound exercise per focus muscle, in focus order.
        focus.forEach { muscle ->
            val candidate = candidates.firstOrNull { ex ->
                ex.primaryMuscle == muscle &&
                    ex !in usedHere &&
                    ex !in alreadyUsed &&
                    ex.pattern !in usedPatterns
            } ?: return@forEach
            addCandidate(candidate, selected, usedHere, usedPatterns, effective, ceiling, weeklyDirect, weeklyCeiling, level)
        }

        // Round two: fill the remaining slots with any focus muscle, preferring
        // movement patterns that are not used yet in this session.
        val remaining = candidates
            .filter { it !in usedHere && it !in alreadyUsed }
            .sortedWith(compareBy<Exercise> { it.pattern in usedPatterns }.thenBy { it.primaryMuscle.ordinal })
        remaining.forEach { candidate ->
            if (selected.size >= maxExercises) return@forEach
            addCandidate(candidate, selected, usedHere, usedPatterns, effective, ceiling, weeklyDirect, weeklyCeiling, level)
        }
        return selected
    }

    private fun addCandidate(
        exercise: Exercise,
        selected: MutableList<ExerciseWithSets>,
        usedHere: MutableSet<Exercise>,
        usedPatterns: MutableSet<MovementPattern>,
        effective: MutableMap<MuscleGroup, Double>,
        ceiling: Double,
        weeklyDirect: Map<MuscleGroup, Int>,
        weeklyCeiling: Int,
        level: ExperienceLevel
    ) {
        val projected = effective.toMutableMap()
        projected.merge(exercise.primaryMuscle, 1.0) { a, b -> a + b }
        exercise.secondaryMuscles.forEach { muscle ->
            projected.merge(muscle, VolumeCalculator.INDIRECT_FRACTION) { a, b -> a + b }
        }
        // A session may not push any muscle above the effective ceiling.
        if (projected.values.any { it > ceiling + 0.001 }) return
        // And the week may not push direct sets above the weekly ceiling.
        val directAfter = (weeklyDirect[exercise.primaryMuscle] ?: 0) + 1
        if (directAfter > weeklyCeiling) return

        val sets = setsForExercise(exercise, level)
        selected += ExerciseWithSets(exercise, sets)
        usedHere += exercise
        usedPatterns += exercise.pattern
        effective.clear()
        effective.putAll(projected)
    }

    private fun setsForExercise(exercise: Exercise, level: ExperienceLevel): Int {
        val base = when (exercise.pattern) {
            MovementPattern.ISOLATION, MovementPattern.CORE -> 3
            MovementPattern.CARDIO -> 1
            else -> 4
        }
        return when (level) {
            ExperienceLevel.BEGINNER -> (base - 1).coerceAtLeast(MIN_SETS_PER_EXERCISE)
            ExperienceLevel.INTERMEDIATE -> base
            ExperienceLevel.ADVANCED -> (base + 1).coerceAtMost(MAX_SETS_PER_EXERCISE)
        }
    }

    /** Exercises that fit the declared time budget, leaving room for warm-up. */
    fun sessionBudget(sessionMinutes: Int, restSeconds: Int): Int {
        val usable = (sessionMinutes - 10).coerceAtLeast(20)
        val perExercise = 3.5 * VolumeCalculator.minutesPerSet(restSeconds)
        return (usable / perExercise).roundToInt().coerceIn(3, 9)
    }

    private fun repsMin(exercise: Exercise, goal: Goal): Int = when {
        exercise.pattern == MovementPattern.CARDIO -> 1
        goal == Goal.STRENGTH && exercise.pattern != MovementPattern.ISOLATION -> 3
        exercise.pattern == MovementPattern.ISOLATION || exercise.pattern == MovementPattern.CORE -> 10
        else -> 6
    }

    private fun repsMax(exercise: Exercise, goal: Goal): Int = when {
        exercise.pattern == MovementPattern.CARDIO -> 1
        goal == Goal.STRENGTH && exercise.pattern != MovementPattern.ISOLATION -> 5
        exercise.pattern == MovementPattern.ISOLATION || exercise.pattern == MovementPattern.CORE -> 15
        goal == Goal.MUSCLE_GAIN -> 12
        else -> 10
    }

    private fun restFor(goal: Goal, exercise: Exercise? = null): Int {
        val base = when (goal) {
            Goal.STRENGTH -> 180
            Goal.MUSCLE_GAIN -> 120
            Goal.FAT_LOSS -> 75
            else -> 90
        }
        return if (exercise != null && exercise.pattern == MovementPattern.ISOLATION) {
            (base * 0.7).roundToInt()
        } else {
            base
        }
    }

    private fun rirFor(level: ExperienceLevel): Int = when (level) {
        ExperienceLevel.BEGINNER -> 3
        ExperienceLevel.INTERMEDIATE -> 2
        ExperienceLevel.ADVANCED -> 1
    }

    // ---- reporting -------------------------------------------------------

    private fun buildWarnings(days: List<ProgramDay>, level: ExperienceLevel): List<String> {
        val warnings = mutableListOf<String>()
        val sessions = days.map { day -> day.exercises.map { ExerciseWithSets(it.exercise, it.sets) } }
        VolumeCalculator.weeklyDirectSets(sessions).forEach { (muscle, sets) ->
            if (sets > VolumeCalculator.weeklyDirectCeiling(level)) {
                warnings += "${muscle.label}: $sets serie settimanali, oltre il tetto per il tuo livello."
            }
        }
        val validation = ProgramValidator.validate(days)
        warnings += validation.issues.map { it.message }
        return warnings.distinct()
    }

    private fun buildRationale(
        profile: UserProfile,
        days: List<ProgramDay>,
        split: Map<Int, List<MuscleGroup>>,
        level: ExperienceLevel
    ): String {
        val perMuscle = VolumeCalculator.weeklyDirectSets(
            days.map { day -> day.exercises.map { ExerciseWithSets(it.exercise, it.sets) } }
        )
        val volumeText = perMuscle.entries.sortedByDescending { it.value }
            .take(6)
            .joinToString(", ") { "${it.key.label} ${it.value}" }
        return "Programma ${days.size} giorni per ${goalLabel(profile.goal)}, livello ${levelLabel(level)}. " +
            "Serie settimanali dirette: $volumeText. " +
            "Split: " + split.entries.joinToString(" | ") { entry ->
                val day = days.firstOrNull { it.dayIndex == entry.key }
                (day?.label ?: "Giorno ${entry.key + 1}") + " = " + entry.value.joinToString("/") { it.label }
            }
    }

    private fun goalLabel(goal: Goal): String = when (goal) {
        Goal.FAT_LOSS -> "perdita di grasso"
        Goal.RECOMP -> "ricomposizione"
        Goal.MUSCLE_GAIN -> "ipertrofia"
        Goal.STRENGTH -> "forza"
        Goal.MAINTENANCE -> "mantenimento"
        Goal.HEALTH -> "salute"
    }

    private fun levelLabel(level: ExperienceLevel): String = when (level) {
        ExperienceLevel.BEGINNER -> "principiante"
        ExperienceLevel.INTERMEDIATE -> "intermedio"
        ExperienceLevel.ADVANCED -> "avanzato"
    }

    private fun isContraindicated(exercise: Exercise, injuries: String): Boolean {
        if (injuries.isBlank()) return false
        val rules = mapOf(
            "spalla" to setOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST),
            "ginocchio" to setOf(MuscleGroup.QUADS, MuscleGroup.CALVES),
            "schiena" to setOf(MuscleGroup.BACK),
            "lombare" to setOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            "gomito" to setOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            "caviglia" to setOf(MuscleGroup.CALVES)
        )
        return rules.any { (keyword, groups) ->
            injuries.contains(keyword) &&
                (exercise.primaryMuscle in groups || exercise.secondaryMuscles.any { it in groups })
        }
    }
}
