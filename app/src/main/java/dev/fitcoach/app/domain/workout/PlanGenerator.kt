package dev.fitcoach.app.domain.workout

/** One session shape: a name and the muscles it must cover. */
data class DayTemplate(val name: String, val muscles: List<MuscleGroup>)

/** Splits by weekly frequency, from 2 to 6 days. No single fixed scheme is imposed. */
object SplitTemplates {

    private val fullA = DayTemplate("Full body A", listOf(MuscleGroup.QUADS, MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.CORE))
    private val fullB = DayTemplate("Full body B", listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.BACK, MuscleGroup.CHEST, MuscleGroup.GLUTES, MuscleGroup.CORE))
    private val fullC = DayTemplate("Full body C", listOf(MuscleGroup.QUADS, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES))

    private val upperA = DayTemplate("Upper A", listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS))
    private val upperB = DayTemplate("Upper B", listOf(MuscleGroup.BACK, MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, MuscleGroup.BICEPS))
    private val lowerA = DayTemplate("Lower A", listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.CORE))
    private val lowerB = DayTemplate("Lower B", listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CORE, MuscleGroup.CALVES))

    private val pushA = DayTemplate("Push A", listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS))
    private val pullA = DayTemplate("Pull A", listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.CORE))
    private val legsA = DayTemplate("Legs A", listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES))
    private val pushB = DayTemplate("Push B", listOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST, MuscleGroup.TRICEPS))
    private val pullB = DayTemplate("Pull B", listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.CORE))
    private val legsB = DayTemplate("Legs B", listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.QUADS, MuscleGroup.CALVES))

    fun forDays(days: Int): List<DayTemplate> = when (days) {
        2 -> listOf(fullA, fullB)
        3 -> listOf(fullA, fullB, fullC)
        4 -> listOf(upperA, lowerA, upperB, lowerB)
        5 -> listOf(pushA, pullA, legsA, upperB, lowerB)
        else -> listOf(pushA, pullA, legsA, pushB, pullB, legsB)
    }
}

/**
 * Builds a weekly plan from the anamnesis and the exercise catalogue.
 *
 * The result is deterministic: the same profile always produces the same plan, so a
 * proposal can be compared with the previous one. Exercises that need equipment the
 * user does not have, that the user excluded, or that load an injured joint are
 * filtered out before selection, and the validator judges the outcome afterwards.
 */
class PlanGenerator(
    private val catalog: List<Exercise> = DefaultExerciseCatalog.exercises,
    private val validator: PlanValidator = PlanValidator()
) {

    fun generate(profile: TraineeProfile): WorkoutPlan {
        val warnings = mutableListOf<String>()
        val blockers = mutableListOf<String>()

        val requestedDays = profile.daysPerWeek
        val daysPerWeek = requestedDays.coerceIn(2, 6)
        if (daysPerWeek != requestedDays) {
            warnings += "Frequenza richiesta di $requestedDays giorni: generata con $daysPerWeek, l'intervallo supportato è 2-6."
        }

        val equipment = profile.equipment + Equipment.BODYWEIGHT
        val excluded = profile.excludedExercises.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        val limitations = profile.limitations.map { it.trim().lowercase() }.filter { it.isNotEmpty() }

        val usable = catalog.filter { exercise ->
            exercise.equipment in equipment &&
                exercise.id.lowercase() !in excluded &&
                exercise.name.lowercase() !in excluded &&
                exercise.contraindications.none { keyword ->
                    val key = keyword.lowercase()
                    limitations.any { limitation -> limitation.contains(key) || key.contains(limitation) }
                }
        }

        val templates = SplitTemplates.forDays(daysPerWeek)
        val perDay = (profile.sessionMinutes / 12).coerceIn(4, 7)

        val days = templates.map { template ->
            template.muscles
                .filter { muscle -> usable.none { it.primary == muscle } }
                .forEach { muscle ->
                    warnings += "Nessun esercizio disponibile per $muscle in ${template.name} con l'attrezzatura e le limitazioni indicate."
                }

            val used = mutableSetOf<String>()
            val planned = mutableListOf<PlannedExercise>()
            var cursor = 0
            val guard = template.muscles.size * 4

            while (planned.size < perDay && cursor < guard) {
                val muscle = template.muscles[cursor % template.muscles.size]
                cursor++
                val candidate = usable
                    .asSequence()
                    .filter { it.primary == muscle && it.id !in used }
                    .sortedWith(compareByDescending<Exercise> { it.compound }.thenBy { it.technicalDemand })
                    .firstOrNull()
                if (candidate != null) {
                    used += candidate.id
                    planned += PlannedExercise(
                        exercise = candidate,
                        prescription = prescriptionFor(profile.experience, candidate),
                        note = noteFor(candidate)
                    )
                }
            }

            if (planned.isEmpty()) {
                blockers += "Nessun esercizio utilizzabile per ${template.name}: controlla attrezzatura ed esercizi esclusi."
            }

            WorkoutDay(
                name = template.name,
                focus = template.muscles,
                exercises = planned,
                estimatedMinutes = planned.sumOf { minutesFor(it) }
            )
        }

        val weekly = mutableMapOf<MuscleGroup, Double>()
        days.forEach { day ->
            day.exercises.forEach { planned ->
                weekly.merge(planned.exercise.primary, planned.prescription.sets.toDouble(), Double::plus)
                planned.exercise.secondary.forEach { secondary ->
                    weekly.merge(secondary, planned.prescription.sets * 0.5, Double::plus)
                }
            }
        }

        val draft = WorkoutPlan(
            days = days,
            weeklySetsPerMuscle = weekly,
            warnings = warnings,
            blockers = blockers,
            issues = emptyList(),
            totalWeeklySets = days.sumOf { day -> day.exercises.sumOf { it.prescription.sets } }
        )

        return draft.copy(issues = validator.validate(draft, profile))
    }

    private fun prescriptionFor(experience: Experience, exercise: Exercise): PlannedSet = when {
        exercise.compound && experience == Experience.BEGINNER ->
            PlannedSet(sets = 3, repsLow = 8, repsHigh = 10, rir = 3, restSeconds = 120)

        exercise.compound ->
            PlannedSet(sets = 4, repsLow = 6, repsHigh = 8, rir = 2, restSeconds = 150)

        experience == Experience.BEGINNER ->
            PlannedSet(sets = 2, repsLow = 12, repsHigh = 15, rir = 2, restSeconds = 60)

        else ->
            PlannedSet(sets = 3, repsLow = 10, repsHigh = 15, rir = 1, restSeconds = 75)
    }

    private fun noteFor(exercise: Exercise): String? =
        if (exercise.technicalDemand >= 3) "Tecnica da consolidare prima di aumentare il carico." else null

    private fun minutesFor(planned: PlannedExercise): Int {
        val setSeconds = if (planned.exercise.compound) 40 else 30
        val total = planned.prescription.sets * (planned.prescription.restSeconds + setSeconds)
        return (total + 59) / 60
    }
}
