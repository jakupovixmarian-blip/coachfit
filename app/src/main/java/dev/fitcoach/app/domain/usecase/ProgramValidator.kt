package dev.fitcoach.app.domain.usecase

import dev.fitcoach.app.domain.model.MovementPattern
import dev.fitcoach.app.domain.model.MuscleGroup

enum class ProgramIssueType {
    DUPLICATE_IN_SESSION,
    DUPLICATE_ACROSS_SESSIONS,
    PATTERN_OVERUSE,
    MUSCLE_IMBALANCE,
    SESSION_OVERLOAD
}

data class ProgramIssue(
    val type: ProgramIssueType,
    val message: String,
    val dayIndices: List<Int> = emptyList(),
    val exerciseName: String? = null,
    val muscle: MuscleGroup? = null
)

/**
 * Checks a program for the defects that make a plan unusable: the same exercise
 * twice in a session, unintentional repeats across days, one movement pattern
 * dominating, muscle imbalances and sessions that are simply too long.
 *
 * It never removes exercises: it reports, so the caller can decide.
 */
object ProgramValidator {

    /** Patterns that legitimately appear more than once in a well-built session. */
    private val REPEATABLE_PATTERNS = setOf(MovementPattern.CORE, MovementPattern.CARDIO)

    fun validate(days: List<ProgramDay>): ProgramValidation {
        val issues = mutableListOf<ProgramIssue>()
        issues += duplicatesInSession(days)
        issues += duplicatesAcrossSessions(days)
        issues += patternOveruse(days)
        issues += muscleImbalance(days)
        issues += sessionOverload(days)
        return ProgramValidation(issues)
    }

    private fun duplicatesInSession(days: List<ProgramDay>): List<ProgramIssue> {
        val issues = mutableListOf<ProgramIssue>()
        days.forEach { day ->
            day.exercises.groupBy { it.exercise.id }
                .filter { it.value.size > 1 }
                .forEach { (_, repeated) ->
                    issues += ProgramIssue(
                        type = ProgramIssueType.DUPLICATE_IN_SESSION,
                        message = "${repeated.first().exercise.name} compare ${repeated.size} volte nel ${day.label}.",
                        dayIndices = listOf(day.dayIndex),
                        exerciseName = repeated.first().exercise.name
                    )
                }
        }
        return issues
    }

    private fun duplicatesAcrossSessions(days: List<ProgramDay>): List<ProgramIssue> {
        val issues = mutableListOf<ProgramIssue>()
        val byExercise = mutableMapOf<Long, MutableList<Pair<Int, ProgramDay>>>()
        days.forEach { day ->
            day.exercises.forEach { planned ->
                byExercise.getOrPut(planned.exercise.id) { mutableListOf() } += day.dayIndex to day
            }
        }
        byExercise.forEach { (_, occurrences) ->
            if (occurrences.size > 1) {
                val distinctDays = occurrences.map { it.first }.distinct()
                if (distinctDays.size > 1) {
                    val name = occurrences.first().second.exercises
                        .first { true }.exercise.name
                    issues += ProgramIssue(
                        type = ProgramIssueType.DUPLICATE_ACROSS_SESSIONS,
                        message = "$name ripetuto in ${distinctDays.size} sedute diverse.",
                        dayIndices = distinctDays,
                        exerciseName = name
                    )
                }
            }
        }
        return issues
    }

    private fun patternOveruse(days: List<ProgramDay>): List<ProgramIssue> {
        val issues = mutableListOf<ProgramIssue>()
        days.forEach { day ->
            day.exercises.filter { it.exercise.pattern !in REPEATABLE_PATTERNS }
                .groupBy { it.exercise.pattern }
                .filter { it.value.size > 2 }
                .forEach { (pattern, repeated) ->
                    issues += ProgramIssue(
                        type = ProgramIssueType.PATTERN_OVERUSE,
                        message = "${repeated.size} esercizi con schema ${pattern.label} nel ${day.label}: schema troppo ripetuto.",
                        dayIndices = listOf(day.dayIndex)
                    )
                }
        }
        return issues
    }

    /**
     * Compares the weekly direct volume of opposing muscles. A push/pull or
     * quad/hamstring ratio outside 0.5-2.0 is a real imbalance, not a preference.
     */
    private fun muscleImbalance(days: List<ProgramDay>): List<ProgramIssue> {
        val sessions = days.map { day -> day.exercises.map { ExerciseWithSets(it.exercise, it.sets) } }
        val weekly = VolumeCalculator.weeklyDirectSets(sessions)
        val issues = mutableListOf<ProgramIssue>()

        val pairs = listOf(
            MuscleGroup.CHEST to MuscleGroup.BACK,
            MuscleGroup.QUADS to MuscleGroup.HAMSTRINGS,
            MuscleGroup.BICEPS to MuscleGroup.TRICEPS
        )
        pairs.forEach { (a, b) ->
            val setsA = weekly[a] ?: 0
            val setsB = weekly[b] ?: 0
            if (setsA == 0 && setsB == 0) return@forEach
            val ratio = if (setsB == 0) Double.MAX_VALUE else setsA.toDouble() / setsB
            if (ratio > 2.0 || ratio < 0.5) {
                val weaker = if (ratio > 2.0) b else a
                val stronger = if (ratio > 2.0) a else b
                issues += ProgramIssue(
                    type = ProgramIssueType.MUSCLE_IMBALANCE,
                    message = "Squilibrio ${stronger.label}/${weaker.label}: ${weekly[stronger] ?: 0} contro ${weekly[weaker] ?: 0} serie settimanali.",
                    muscle = weaker
                )
            }
        }
        return issues
    }

    private fun sessionOverload(days: List<ProgramDay>): List<ProgramIssue> {
        val issues = mutableListOf<ProgramIssue>()
        days.forEach { day ->
            val sets = day.exercises.sumOf { it.sets }
            if (sets > MAX_SETS_PER_SESSION) {
                issues += ProgramIssue(
                    type = ProgramIssueType.SESSION_OVERLOAD,
                    message = "${day.label}: $sets serie in una seduta, oltre il limite di $MAX_SETS_PER_SESSION.",
                    dayIndices = listOf(day.dayIndex)
                )
            }
        }
        return issues
    }

    const val MAX_SETS_PER_SESSION = 26
}

data class ProgramValidation(val issues: List<ProgramIssue>) {
    val hasIssues: Boolean get() = issues.isNotEmpty()
    val blockingIssues: List<ProgramIssue>
        get() = issues.filter {
            it.type == ProgramIssueType.DUPLICATE_IN_SESSION || it.type == ProgramIssueType.SESSION_OVERLOAD
        }
}
