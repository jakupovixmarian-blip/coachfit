package dev.fitcoach.app.domain.workout

/** Weekly set thresholds used to judge a plan, per experience level. */
object VolumeRules {
    const val MIN_WEEKLY_SETS = 6.0

    fun maxWeeklySets(experience: Experience): Double = when (experience) {
        Experience.BEGINNER -> 18.0
        Experience.INTERMEDIATE -> 24.0
        Experience.ADVANCED -> 28.0
    }
}

/**
 * Checks a plan against the rules the app promises to respect: enough weekly volume
 * per muscle, no excessive volume, no repeated exercise inside the same session and
 * a session that fits the time the user actually has.
 */
class PlanValidator {

    fun validate(plan: WorkoutPlan, profile: TraineeProfile): List<PlanIssue> {
        val issues = mutableListOf<PlanIssue>()
        val maxSets = VolumeRules.maxWeeklySets(profile.experience)

        plan.weeklySetsPerMuscle.forEach { (muscle, sets) ->
            if (sets < VolumeRules.MIN_WEEKLY_SETS) {
                issues += PlanIssue(
                    PlanSeverity.WARNING,
                    "Volume settimanale basso per $muscle: ${format(sets)} serie."
                )
            }
            if (sets > maxSets) {
                issues += PlanIssue(
                    PlanSeverity.ERROR,
                    "Volume settimanale eccessivo per $muscle: ${format(sets)} serie (massimo ${format(maxSets)})."
                )
            }
        }

        plan.days.forEach { day ->
            val ids = day.exercises.map { it.exercise.id }
            if (ids.size != ids.toSet().size) {
                issues += PlanIssue(
                    PlanSeverity.ERROR,
                    "Esercizi duplicati nella seduta ${day.name}."
                )
            }
            if (day.estimatedMinutes > profile.sessionMinutes + 20) {
                issues += PlanIssue(
                    PlanSeverity.WARNING,
                    "La seduta ${day.name} dura circa ${day.estimatedMinutes} minuti, oltre i ${profile.sessionMinutes} disponibili."
                )
            }
            if (day.exercises.isEmpty()) {
                issues += PlanIssue(
                    PlanSeverity.ERROR,
                    "La seduta ${day.name} non contiene esercizi."
                )
            }
        }

        return issues
    }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
}
