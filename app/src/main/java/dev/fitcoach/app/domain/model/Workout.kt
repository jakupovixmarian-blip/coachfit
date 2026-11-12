package dev.fitcoach.app.domain.model

enum class MuscleGroup(val label: String) {
    CHEST("Petto"), BACK("Schiena"), SHOULDERS("Spalle"), BICEPS("Bicipiti"),
    TRICEPS("Tricipiti"), FOREARMS("Avambracci"), QUADS("Quadricipiti"),
    HAMSTRINGS("Femorali"), GLUTES("Glutei"), CALVES("Polpacci"), CORE("Core"),
    CARDIO("Cardio"), FULL_BODY("Total body")
}

enum class Equipment(val label: String) {
    BARBELL("Bilanciere"), DUMBBELL("Manubri"), MACHINE("Macchina"),
    CABLE("Cavi"), BODYWEIGHT("Corpo libero"), BAND("Elastici"),
    KETTLEBELL("Kettlebell"), SMITH("Smith machine"), CARDIO_MACHINE("Cardio"), OTHER("Altro")
}

enum class MovementPattern(val label: String) {
    HORIZONTAL_PUSH("Spinta orizzontale"), VERTICAL_PUSH("Spinta verticale"),
    HORIZONTAL_PULL("Trazione orizzontale"), VERTICAL_PULL("Trazione verticale"),
    SQUAT("Squat"), HINGE("Hinge"), LUNGE("Affondo"), CARRY("Carry"),
    CORE("Core"), ISOLATION("Isolamento"), CARDIO("Cardio")
}

data class Exercise(
    val id: Long = 0L,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: Equipment = Equipment.BODYWEIGHT,
    val pattern: MovementPattern = MovementPattern.ISOLATION,
    val execution: String = "",
    val setup: String = "",
    val commonMistakes: String = "",
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false
)

enum class SetType(val label: String) {
    WARMUP("Riscaldamento"), WORKING("Allenante"), FAILURE("Cedimento"),
    DROP_SET("Drop set"), REST_PAUSE("Rest-pause"), MYO_REPS("Myo-reps"),
    BACK_OFF("Back-off"), CLUSTER("Cluster")
}

data class RoutineExercise(
    val id: Long = 0L,
    val routineId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int = 3,
    val targetRepsMin: Int = 8,
    val targetRepsMax: Int = 12,
    val targetRir: Int = 2,
    val restSeconds: Int = 90,
    val notes: String = ""
)

data class Routine(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val isActive: Boolean = false,
    val createdAt: Long = 0L,
    /**
     * Groups the routines that belong to the same program. 0 means a standalone
     * routine created by hand, which is always a single session.
     */
    val programId: Long = 0L,
    /** Position of this session inside its program, used for ordering. */
    val dayIndex: Int = 0,
    /** Short label of the session, e.g. "Upper 1" or "Spinta". */
    val dayLabel: String = ""
) {
    val isPartOfProgram: Boolean get() = programId != 0L
}

/** A program is the set of sessions generated for a user, ordered by day. */
data class WorkoutProgram(
    val programId: Long,
    val routines: List<Routine>
) {
    val sessions: List<Routine> get() = routines.sortedBy { it.dayIndex }
    val dayCount: Int get() = routines.size
}
