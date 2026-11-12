package com.fitcoach.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class,
        FoodEntity::class,
        NutritionDayEntity::class,
        MealEntity::class,
        MealFoodEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        ExercisePerformanceEntity::class,
        WeightRecordEntity::class,
        CheckInEntity::class,
        ProgramChangeEntity::class,
        ChatMessageEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class FitCoachDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS food_database (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sourceCitation TEXT NOT NULL, kcalPer100g REAL NOT NULL, proteinPer100g REAL NOT NULL, carbsPer100g REAL NOT NULL, fatPer100g REAL NOT NULL, rawOrCooked TEXT NOT NULL, verifiedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS nutrition_days (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dateEpochDay INTEGER NOT NULL, carbType TEXT NOT NULL, targetKcal INTEGER NOT NULL, targetProteinG INTEGER NOT NULL, targetCarbsG INTEGER NOT NULL, targetFatG INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS meals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nutritionDayId INTEGER NOT NULL, name TEXT NOT NULL, scheduledMinutes INTEGER NOT NULL, completed INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS meal_foods (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mealId INTEGER NOT NULL, foodId INTEGER NOT NULL, grams REAL NOT NULL, rawOrCooked TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, primaryMuscle TEXT NOT NULL, secondaryMuscles TEXT NOT NULL, equipment TEXT NOT NULL, techniqueNotes TEXT NOT NULL, alternatives TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dateEpochDay INTEGER NOT NULL, title TEXT NOT NULL, completed INTEGER NOT NULL, notes TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS exercise_performances (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, setNumber INTEGER NOT NULL, loadKg REAL NOT NULL, reps INTEGER NOT NULL, rir REAL, rpe REAL, painReported INTEGER NOT NULL, formWasGood INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS weight_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, measuredAt INTEGER NOT NULL, weightKg REAL NOT NULL, source TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS check_ins (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, weekStartEpochDay INTEGER NOT NULL, adherencePercent INTEGER, sleepScore INTEGER, recoveryScore INTEGER, notes TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS program_changes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, createdAt INTEGER NOT NULL, situation TEXT NOT NULL, problem TEXT NOT NULL, dataUsed TEXT NOT NULL, suggestion TEXT NOT NULL, rationale TEXT NOT NULL, expectedResult TEXT NOT NULL, contraindications TEXT NOT NULL, status TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, createdAt INTEGER NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, provider TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, scheduledAt INTEGER NOT NULL, enabled INTEGER NOT NULL, snoozedUntil INTEGER)")
            }
        }
    }
}

