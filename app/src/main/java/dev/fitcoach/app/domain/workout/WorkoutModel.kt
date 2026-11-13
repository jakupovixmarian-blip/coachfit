package dev.fitcoach.app.domain.workout

enum class Sex { MALE, FEMALE }

enum class Experience { BEGINNER, INTERMEDIATE, ADVANCED }

enum class Goal { HYPERTROPHY, FAT_LOSS, RECOMPOSITION, STRENGTH, ENDURANCE, MAINTENANCE }

enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, KETTLEBELL, BANDS }

enum class MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE }

/**
 * What the generator is allowed to use. Every field comes from the anamnesis, so the
 * plan is built on the user's real availability instead of an idealised trainee.
 */
data class TraineeProfile(
    val name: String = "",
    val age: Int = 30,
    val sex: Sex = Sex.MALE,
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val targetWeightKg: Double? = null,
    val experience: Experience,
    val daysPerWeek: Int,
    val sessionMinutes: Int = 60,
    val equipment: Set<Equipment>,
    val goal: Goal,
    val limitations: List<String> = emptyList(),
    val excludedExercises: Set<String> = emptySet()
)

/**
 * An exercise with the data the generator needs to filter it.
 *
 * [contraindications] are plain keywords ("spalla", "ginocchio", "lombare") matched
 * against the user's limitations: a keyword match excludes the exercise instead of
 * trusting the model to remember the injury.
 */
data class Exercise(
    val id: String,
    val name: String,
    val primary: MuscleGroup,
    val secondary: List<MuscleGroup> = emptyList(),
    val equipment: Equipment,
    val compound: Boolean,
    val technicalDemand: Int = 1,
    val contraindications: List<String> = emptyList()
)

data class PlannedSet(
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val rir: Int,
    val restSeconds: Int
)

data class PlannedExercise(
    val exercise: Exercise,
    val prescription: PlannedSet,
    val note: String? = null
)

data class WorkoutDay(
    val name: String,
    val focus: List<MuscleGroup>,
    val exercises: List<PlannedExercise>,
    val estimatedMinutes: Int
)

enum class PlanSeverity { INFO, WARNING, ERROR }

data class PlanIssue(
    val severity: PlanSeverity,
    val message: String
)

data class WorkoutPlan(
    val days: List<WorkoutDay>,
    val weeklySetsPerMuscle: Map<MuscleGroup, Double>,
    val warnings: List<String>,
    val blockers: List<String>,
    val issues: List<PlanIssue>,
    val totalWeeklySets: Int
) {
    val hasErrors: Boolean get() = issues.any { it.severity == PlanSeverity.ERROR }
}
