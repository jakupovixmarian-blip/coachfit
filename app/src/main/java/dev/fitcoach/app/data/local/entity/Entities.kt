package dev.fitcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String,
    val age: Int?,
    val sex: String,
    val heightCm: Double?,
    val weightKg: Double?,
    val bodyFatPercent: Double?,
    val goal: String,
    val experience: String,
    val trainingYears: Double?,
    val consistencyWeeks: Int?,
    val knownLoads: String,
    val jobActivity: String,
    val dailySteps: Int?,
    val sleepHours: Double?,
    val stressLevel: Int?,
    val recoveryLevel: Int?,
    val injuries: String,
    val equipment: String,
    val availableDays: Int?,
    val sessionMinutes: Int?,
    val dietPreference: String,
    val allergies: String,
    val mealsPerDay: Int?,
    val mealTimes: String,
    val weeklyBudget: Double?,
    val cookingMinutes: Int?,
    val likedFoods: String,
    val dislikedFoods: String,
    val unitSystem: String,
    val onboardingCompleted: Boolean,
    val updatedAt: Long
)

@Entity(tableName = "measurement", indices = [Index("type"), Index("dateEpochDay")])
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val customLabel: String,
    val dateEpochDay: Long,
    val value: Double,
    val notes: String,
    val createdAt: Long
)

@Entity(tableName = "photo", indices = [Index("dateEpochDay")])
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val pose: String,
    val customLabel: String,
    val filePath: String,
    val notes: String,
    val createdAt: Long,
    /** Analysis returned by the AI coach for this photo, empty until requested. */
    val analysis: String = "",
    val analysisAt: Long? = null
)

@Entity(tableName = "exercise", indices = [Index(value = ["name"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: String,
    val equipment: String,
    val pattern: String,
    val execution: String,
    val setup: String,
    val commonMistakes: String,
    val isCustom: Boolean,
    val isFavorite: Boolean,
    /** Rest the user wants between sets of this exercise, adjustable per exercise. */
    val defaultRestSeconds: Int = 90
)

@Entity(tableName = "routine", indices = [Index("programId")])
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val isActive: Boolean,
    val createdAt: Long,
    /** Groups sessions of the same generated program. 0 = standalone routine. */
    val programId: Long = 0L,
    val dayIndex: Int = 0,
    val dayLabel: String = ""
)

@Entity(
    tableName = "routine_exercise",
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetRir: Int,
    val restSeconds: Int,
    val notes: String
)

@Entity(tableName = "workout_session", indices = [Index("dateEpochDay"), Index("routineId")])
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long?,
    val name: String,
    val dateEpochDay: Long,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: String,
    val notes: String
)

@Entity(tableName = "logged_set", indices = [Index("sessionId"), Index("exerciseId")])
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val setIndex: Int,
    val setType: String,
    val weightKg: Double?,
    val reps: Int?,
    val rir: Int?,
    val rpe: Double?,
    val restSeconds: Int?,
    val pain: Boolean,
    val completed: Boolean,
    val notes: String,
    val loggedAt: Long
)

@Entity(
    tableName = "food",
    indices = [Index("name"), Index("barcode"), Index(value = ["source", "sourceId"])]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val brand: String,
    val category: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double?,
    val source: String,
    val barcode: String?,
    val state: String,
    val pricePerKg: Double?,
    val isCustom: Boolean,
    /** Identifier inside the source, so a refresh updates instead of duplicating. */
    val sourceId: String = "",
    val acquiredAt: Long = 0L,
    val sourceUpdatedAt: Long? = null,
    val reliability: Double = 0.0,
    /** Name of [dev.fitcoach.app.domain.model.VerificationStatus]. */
    val verification: String = "MANUAL",
    /** What the record does not state, instead of a guessed value. */
    val limitations: String = ""
)

@Entity(tableName = "meal_plan")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val isActive: Boolean,
    val targetKcal: Double,
    val targetProtein: Double,
    val targetCarbs: Double,
    val targetFat: Double,
    val createdAt: Long,
    val source: String
)

@Entity(tableName = "plan_day", indices = [Index("planId")])
data class PlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val planId: Long,
    val dayIndex: Int
)

@Entity(tableName = "meal_item", indices = [Index("planDayId"), Index("foodId")])
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val planDayId: Long,
    val mealSlot: String,
    val foodId: Long,
    val grams: Double,
    val alternativeGroup: Int?,
    val preparation: String,
    val timeLabel: String
)

@Entity(tableName = "diary_entry", indices = [Index("dateEpochDay"), Index("foodId")])
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val mealSlot: String,
    val foodId: Long,
    val grams: Double,
    val consumed: Boolean,
    val planItemId: Long?,
    val loggedAt: Long
)

@Entity(tableName = "shopping_item", indices = [Index("weekStartEpochDay")])
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val quantityGrams: Double,
    val category: String,
    val estimatedPrice: Double?,
    val checked: Boolean,
    val weekStartEpochDay: Long
)

@Entity(tableName = "coach_proposal", indices = [Index("status")])
data class CoachProposalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val createdAt: Long,
    val changeType: String,
    val rationale: String,
    val previousValue: String,
    val newValue: String,
    val origin: String,
    val status: String,
    val appliedAt: Long?,
    val appliedTo: String
)

@Entity(tableName = "ai_config")
data class AiConfigEntity(
    @PrimaryKey val id: Long = 1L,
    val provider: String,
    val baseUrl: String,
    val modelId: String,
    val maxOutputTokens: Int,
    val timeoutSeconds: Int,
    val temperature: Double,
    val topP: Double,
    val useJsonMode: Boolean,
    val sendPhotosToAi: Boolean
)

@Entity(tableName = "onboarding_answer")
data class OnboardingAnswerEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long
)

/** One turn of the coach chat. Turns are kept so the model has the full context. */
@Entity(tableName = "coach_message", indices = [Index("createdAt")])
data class CoachMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String,
    val content: String,
    val createdAt: Long,
    /** Optional structured summary of an action the assistant applied. */
    val actionSummary: String = ""
)

/**
 * A source the coach may use. The app ships verifiable training principles and the
 * user can add their own notes (for example from Project Invictus), so the coach
 * reasons on declared references instead of invented claims.
 */
@Entity(tableName = "knowledge_note", indices = [Index("topic")])
data class KnowledgeNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val topic: String,
    val title: String,
    val content: String,
    val source: String,
    val builtIn: Boolean,
    val enabled: Boolean = true,
    val createdAt: Long = 0L
)
