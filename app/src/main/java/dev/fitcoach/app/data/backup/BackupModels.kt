package dev.fitcoach.app.data.backup

import kotlinx.serialization.Serializable

/** Full-fidelity snapshot of the locally stored data. */
@Serializable
data class BackupSnapshot(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val profile: ProfileBackup? = null,
    val measurements: List<MeasurementBackup> = emptyList(),
    val photos: List<PhotoBackup> = emptyList(),
    val exercises: List<ExerciseBackup> = emptyList(),
    val routines: List<RoutineBackup> = emptyList(),
    val routineExercises: List<RoutineExerciseBackup> = emptyList(),
    val sessions: List<SessionBackup> = emptyList(),
    val loggedSets: List<LoggedSetBackup> = emptyList(),
    val foods: List<FoodBackup> = emptyList(),
    val mealPlans: List<MealPlanBackup> = emptyList(),
    val planDays: List<PlanDayBackup> = emptyList(),
    val mealItems: List<MealItemBackup> = emptyList(),
    val diaryEntries: List<DiaryBackup> = emptyList(),
    val shoppingItems: List<ShoppingBackup> = emptyList(),
    val coachProposals: List<CoachProposalBackup> = emptyList()
)

@Serializable
data class ProfileBackup(
    val name: String, val age: Int?, val sex: String, val heightCm: Double?, val weightKg: Double?,
    val bodyFatPercent: Double?, val goal: String, val experience: String, val trainingYears: Double?,
    val consistencyWeeks: Int?, val knownLoads: String, val jobActivity: String, val dailySteps: Int?,
    val sleepHours: Double?, val stressLevel: Int?, val recoveryLevel: Int?, val injuries: String,
    val equipment: String, val availableDays: Int?, val sessionMinutes: Int?, val dietPreference: String,
    val allergies: String, val mealsPerDay: Int?, val mealTimes: String, val weeklyBudget: Double?,
    val cookingMinutes: Int?, val likedFoods: String, val dislikedFoods: String, val unitSystem: String,
    val onboardingCompleted: Boolean, val updatedAt: Long
)

@Serializable
data class MeasurementBackup(
    val type: String, val customLabel: String, val dateEpochDay: Long, val value: Double,
    val notes: String, val createdAt: Long
)

@Serializable
data class PhotoBackup(
    val dateEpochDay: Long, val pose: String, val customLabel: String, val filePath: String,
    val notes: String, val createdAt: Long
)

@Serializable
data class ExerciseBackup(
    val name: String, val primaryMuscle: String, val secondaryMuscles: String, val equipment: String,
    val pattern: String, val execution: String, val setup: String, val commonMistakes: String,
    val isCustom: Boolean, val isFavorite: Boolean
)

@Serializable
data class RoutineBackup(
    val localId: Long, val name: String, val description: String, val isActive: Boolean, val createdAt: Long
)

@Serializable
data class RoutineExerciseBackup(
    val routineLocalId: Long, val exerciseName: String, val orderIndex: Int, val targetSets: Int,
    val targetRepsMin: Int, val targetRepsMax: Int, val targetRir: Int, val restSeconds: Int, val notes: String
)

@Serializable
data class SessionBackup(
    val routineLocalId: Long?, val name: String, val dateEpochDay: Long, val startedAt: Long,
    val finishedAt: Long?, val status: String, val notes: String
)

@Serializable
data class LoggedSetBackup(
    val sessionIndex: Int, val exerciseName: String, val orderIndex: Int, val setIndex: Int,
    val setType: String, val weightKg: Double?, val reps: Int?, val rir: Int?, val rpe: Double?,
    val restSeconds: Int?, val pain: Boolean, val completed: Boolean, val notes: String, val loggedAt: Long
)

@Serializable
data class FoodBackup(
    val name: String, val brand: String, val category: String, val kcalPer100g: Double,
    val proteinPer100g: Double, val carbsPer100g: Double, val fatPer100g: Double, val fiberPer100g: Double?,
    val source: String, val barcode: String?, val state: String, val pricePerKg: Double?, val isCustom: Boolean
)

@Serializable
data class MealPlanBackup(
    val localId: Long, val name: String, val isActive: Boolean, val targetKcal: Double,
    val targetProtein: Double, val targetCarbs: Double, val targetFat: Double, val createdAt: Long, val source: String
)

@Serializable
data class PlanDayBackup(val planLocalId: Long, val dayIndex: Int)

@Serializable
data class MealItemBackup(
    val planLocalId: Long, val dayIndex: Int, val mealSlot: String, val foodName: String,
    val grams: Double, val preparation: String, val timeLabel: String
)

@Serializable
data class DiaryBackup(
    val dateEpochDay: Long, val mealSlot: String, val foodName: String, val grams: Double,
    val consumed: Boolean, val loggedAt: Long
)

@Serializable
data class ShoppingBackup(
    val name: String, val quantityGrams: Double, val category: String, val estimatedPrice: Double?,
    val checked: Boolean, val weekStartEpochDay: Long
)

@Serializable
data class CoachProposalBackup(
    val createdAt: Long, val changeType: String, val rationale: String, val previousValue: String,
    val newValue: String, val origin: String, val status: String, val appliedAt: Long?, val appliedTo: String
)
