package dev.fitcoach.app.domain.engine

import dev.fitcoach.app.domain.model.Equipment
import dev.fitcoach.app.domain.model.Exercise
import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.MuscleGroup
import dev.fitcoach.app.domain.model.RoutineExercise
import kotlin.math.roundToInt

enum class VolumeStatus(val label: String) {
    BELOW_MINIMUM("Sotto il minimo"),
    OPTIMAL("Ottimale"),
    HIGH("Alto"),
    EXCESSIVE("Eccessivo")
}

data class MuscleVolume(
    val muscle: MuscleGroup,
    val directSets: Double,
    val indirectSets: Double,
    val frequency: Int,
    val status: VolumeStatus
) {
    /**
     * Indirect work counts half: a set of bench press trains the chest directly and
     * the triceps indirectly, but it is not the same stimulus as a triceps set.
     */
    val effectiveSets: Double get() = directSets + indirectSets * INDIRECT_FACTOR

    companion object {
        const val INDIRECT_FACTOR = 0.5
    }
}

data class VolumeReport(
    val perMuscle: List<MuscleVolume>,
    val totalWeeklySets: Double,
    val minutesPerSession: List<Int>,
    val issues: List<String>
) {
    val blockingIssues: List<String> get() = issues.filter { it.startsWith("!") }
    val isCoherent: Boolean get() = blockingIssues.isEmpty()
}

/**
 * Checks a generated program before it reaches the user.
 *
 * The rules are the ones that actually make a program wrong, not stylistic
 * preferences: a muscle trained with no direct work, weekly volume far outside the
 * useful range for the experience level, the same exercise repeated inside one
 * session, a session longer than the user has time for, or an exercise the user
 * cannot perform with the equipment they own.
 *
 * Issues prefixed with "!" are blocking: the generator must fix the program before
 * saving it.
 */
object VolumeAnalyzer {

    private const val SET_SECONDS = 45
    private const val MAX_SESSION_MINUTES = 95

    private val MAJOR_MUSCLES = listOf(
        MuscleGroup.CHEST,
        MuscleGroup.BACK,
        MuscleGroup.QUADS,
        MuscleGroup.HAMSTRINGS,
        MuscleGroup.GLUTES,
        MuscleGroup.SHOULDERS
    )

    /** Useful weekly set range per muscle, by experience. */
    fun thresholds(experience: ExperienceLevel): IntRange = when (experience) {
        ExperienceLevel.BEGINNER -> 8..16
        ExperienceLevel.INTERMEDIATE -> 10..20
        ExperienceLevel.ADVANCED -> 12..22
    }

    fun analyze(
        sessions: List<List<RoutineExercise>>,
        exercises: Map<Long, Exercise>,
        experience: ExperienceLevel,
        availableEquipment: Set<Equipment>? = null
    ): VolumeReport {
        val direct = mutableMapOf<MuscleGroup, Double>()
        val indirect = mutableMapOf<MuscleGroup, Double>()
        val frequency = mutableMapOf<MuscleGroup, Int>()
        val issues = mutableListOf<String>()
        val minutes = mutableListOf<Int>()

        sessions.forEachIndexed { sessionIndex, session ->
            var sessionSeconds = 0
            val touched = mutableSetOf<MuscleGroup>()
            val repeated = mutableMapOf<Long, Int>()

            for (routineExercise in session) {
                repeated[routineExercise.exerciseId] = (repeated[routineExercise.exerciseId] ?: 0) + 1
                val exercise = exercises[routineExercise.exerciseId] ?: continue
                val sets = routineExercise.targetSets.coerceAtLeast(0).toDouble()

                direct[exercise.primaryMuscle] = (direct[exercise.primaryMuscle] ?: 0.0) + sets
                for (muscle in exercise.secondaryMuscles) {
                    indirect[muscle] = (indirect[muscle] ?: 0.0) + sets
                }

                touched += exercise.primaryMuscle
                touched.addAll(exercise.secondaryMuscles)
                sessionSeconds += routineExercise.targetSets * (SET_SECONDS + routineExercise.restSeconds)

                if (availableEquipment != null && exercise.equipment !in availableEquipment &&
                    exercise.equipment != Equipment.BODYWEIGHT
                ) {
                    issues += "!Esercizio ${exercise.name}: richiede ${exercise.equipment.label}, " +
                        "non presente nell'attrezzatura dichiarata."
                }
            }

            for (muscle in touched) frequency[muscle] = (frequency[muscle] ?: 0) + 1

            for ((exerciseId, count) in repeated) {
                if (count > 1) {
                    val name = exercises[exerciseId]?.name ?: "esercizio #$exerciseId"
                    issues += "!Esercizio ripetuto nella seduta ${sessionIndex + 1}: $name ($count volte)."
                }
            }

            val sessionMinutes = (sessionSeconds / 60.0).roundToInt()
            minutes += sessionMinutes
            if (sessionMinutes > MAX_SESSION_MINUTES) {
                issues += "La seduta ${sessionIndex + 1} dura circa $sessionMinutes minuti: " +
                    "oltre i $MAX_SESSION_MINUTES minuti dichiarati."
            }
        }

        val range = thresholds(experience)
        val muscles = (direct.keys + indirect.keys).distinct()
            .sortedWith(compareByDescending<MuscleGroup> { direct[it] ?: 0.0 }.thenBy { it.label })

        val perMuscle = muscles.map { muscle ->
            val directSets = direct[muscle] ?: 0.0
            val indirectSets = indirect[muscle] ?: 0.0
            val effective = directSets + indirectSets * MuscleVolume.INDIRECT_FACTOR
            MuscleVolume(
                muscle = muscle,
                directSets = directSets,
                indirectSets = indirectSets,
                frequency = frequency[muscle] ?: 0,
                status = statusFor(effective, range)
            )
        }

        for (volume in perMuscle) {
            when (volume.status) {
                VolumeStatus.BELOW_MINIMUM -> {
                    issues += "Volume sotto il minimo per ${volume.muscle.label}: " +
                        "${volume.effectiveSets.roundToInt()} serie utili (minimo ${range.first})."
                }
                VolumeStatus.EXCESSIVE -> {
                    issues += "Volume eccessivo per ${volume.muscle.label}: " +
                        "${volume.effectiveSets.roundToInt()} serie utili (massimo ${range.last + 5})."
                }
                else -> Unit
            }
        }

        for (muscle in MAJOR_MUSCLES) {
            val volume = perMuscle.firstOrNull { it.muscle == muscle }
            if (volume == null || volume.directSets <= 0.0) {
                issues += "!Nessun esercizio diretto per ${muscle.label}."
            }
        }

        return VolumeReport(
            perMuscle = perMuscle,
            totalWeeklySets = direct.values.sum(),
            minutesPerSession = minutes,
            issues = issues
        )
    }

    fun statusFor(effectiveSets: Double, range: IntRange): VolumeStatus = when {
        effectiveSets < range.first -> VolumeStatus.BELOW_MINIMUM
        effectiveSets <= range.last -> VolumeStatus.OPTIMAL
        effectiveSets <= range.last + 5 -> VolumeStatus.HIGH
        else -> VolumeStatus.EXCESSIVE
    }
}
