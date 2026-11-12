package com.fitcoach.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_database")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceCitation: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val rawOrCooked: String,
    val verifiedAt: Long
)

@Entity(tableName = "nutrition_days")
data class NutritionDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val carbType: String,
    val targetKcal: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nutritionDayId: Long,
    val name: String,
    val scheduledMinutes: Int,
    val completed: Boolean = false
)

@Entity(tableName = "meal_foods")
data class MealFoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodId: Long,
    val grams: Double,
    val rawOrCooked: String
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: String,
    val equipment: String,
    val techniqueNotes: String,
    val alternatives: String
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val title: String,
    val completed: Boolean = false,
    val notes: String? = null
)

@Entity(tableName = "exercise_performances")
data class ExercisePerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val loadKg: Double,
    val reps: Int,
    val rir: Double? = null,
    val rpe: Double? = null,
    val painReported: Boolean = false,
    val formWasGood: Boolean = true
)

@Entity(tableName = "weight_records")
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measuredAt: Long,
    val weightKg: Double,
    val source: String = "USER"
)

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStartEpochDay: Long,
    val adherencePercent: Int? = null,
    val sleepScore: Int? = null,
    val recoveryScore: Int? = null,
    val notes: String? = null
)

@Entity(tableName = "program_changes")
data class ProgramChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val situation: String,
    val problem: String,
    val dataUsed: String,
    val suggestion: String,
    val rationale: String,
    val expectedResult: String,
    val contraindications: String,
    val status: String = "PENDING"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val role: String,
    val content: String,
    val provider: String? = null
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val scheduledAt: Long,
    val enabled: Boolean = true,
    val snoozedUntil: Long? = null
)
