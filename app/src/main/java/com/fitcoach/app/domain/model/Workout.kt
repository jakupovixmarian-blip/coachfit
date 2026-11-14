package com.fitcoach.app.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: Set<MuscleGroup> = emptySet(),
    val category: ExerciseCategory,
    val equipment: Set<Equipment> = emptySet(),
    val technique: String,
    val defaultSets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int,
    val targetRir: Int,
    val isUnilateral: Boolean = false,
    val substituteNames: List<String> = emptyList(),
    val bodyweightOnly: Boolean = false,
    val movementPattern: MovementPattern = MovementPattern.OTHER,
    /** Difficolta tecnica 1 (semplice) .. 3 (avanzata). */
    val difficulty: Int = 1
)

/** Esercizio programmato in una giornata di allenamento. */
data class PlannedExercise(
    val id: Long = 0,
    val workoutDayId: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val primaryMuscle: MuscleGroup,
    val orderIndex: Int,
    val sets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int,
    val targetRir: Int,
    val targetWeightKg: Double? = null,
    val targetReps: Int? = null,
    val notes: String = "",
    val technique: String = "",
    val category: ExerciseCategory = ExerciseCategory.COMPOUND,
    /** Spiegazione del perche questo esercizio e stato scelto. */
    val selectionReason: String = "",
    /** Spiegazione del recupero assegnato. */
    val restReason: String = "",
    /** Tecnica d'intensita applicata (NONE se nessuna). */
    val intensityTechnique: IntensityTechnique = IntensityTechnique.NONE,
    /** Spiegazione della tecnica d'intensita, se applicata. */
    val techniqueReason: String = "",
    val movementPattern: MovementPattern = MovementPattern.OTHER
)

data class WorkoutDay(
    val id: Long = 0,
    val mesocycleId: Long = 0,
    val dayIndex: Int, // 0..6 day of week
    val title: String,
    val muscleGroups: Set<MuscleGroup> = emptySet(),
    val exercises: List<PlannedExercise> = emptyList(),
    val isRestDay: Boolean = false
) {
    val totalSets: Int get() = exercises.sumOf { it.sets }
}

data class Mesocycle(
    val id: Long = 0,
    val planId: Long = 0,
    val name: String,
    val focus: String,
    val weekCount: Int,
    val startWeekIndex: Int = 0,
    val deloadWeekIndex: Int? = null,
    val isDeload: Boolean = false,
    val progressionModel: String = "Double Progression"
)

data class WorkoutPlan(
    val id: Long = 0,
    val profileId: Long = 1L,
    val name: String,
    val splitType: SplitType,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val version: Int = 1,
    val mesocycles: List<Mesocycle> = emptyList(),
    val days: List<WorkoutDay> = emptyList(),
    val weeklyVolumePerGroup: Map<MuscleGroup, Int> = emptyMap()
)

data class WorkoutSet(
    val id: Long = 0,
    val sessionId: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val setIndex: Int,
    val plannedReps: Int,
    val actualReps: Int,
    val weightKg: Double,
    val rir: Int? = null,
    val rpe: Double? = null,
    val completed: Boolean = true,
    val isWarmup: Boolean = false,
    val notes: String = ""
)

data class WorkoutSession(
    val id: Long = 0,
    val planId: Long = 0,
    val workoutDayId: Long? = null,
    val dayTitle: String,
    val dateEpochDay: Long,
    val status: SessionStatus,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val durationMinutes: Int? = null,
    val perceivedEffort: Double? = null,
    val sleepQuality: Int? = null, // 1..5
    val painReported: Boolean = false,
    val painNotes: String = "",
    val sets: List<WorkoutSet> = emptyList()
) {
    val totalVolumeKg: Double
        get() = sets.filter { !it.isWarmup && it.completed }
            .sumOf { it.weightKg * it.actualReps }
    val completedSets: Int get() = sets.count { it.completed && !it.isWarmup }
}

/** Aggregato storico per un esercizio, usato dal motore di progressione. */
data class ExercisePerformance(
    val id: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val sessionId: Long,
    val dateEpochDay: Long,
    val topWeightKg: Double,
    val topReps: Int,
    val totalSets: Int,
    val totalReps: Int,
    val volumeKg: Double,
    val avgRir: Double? = null,
    val estimated1Rm: Double? = null
)
