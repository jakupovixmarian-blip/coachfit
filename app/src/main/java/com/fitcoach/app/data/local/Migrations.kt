package com.fitcoach.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrazione schema v1 -> v2.
 *
 * Aggiunge i campi dell'anamnesi estesa (stile di vita, recupero, alimentazione,
 * allenamento), la versione dei piani, il carb cycling condizionale e le nuove
 * tabelle per fonti scientifiche e storico versioni. I nuovi campi hanno valori
 * di default non fittizi (l'utente li imposta in onboarding/modifica profilo).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- user_profile: nuovi campi anamnesi ---
        val profileCols = listOf(
            "workType TEXT NOT NULL DEFAULT 'SEDENTARY'",
            "workShift TEXT NOT NULL DEFAULT 'NONE'",
            "workHoursPerDay REAL",
            "seatedHoursPerDay REAL",
            "dailySteps INTEGER",
            "transportMode TEXT NOT NULL DEFAULT 'CAR'",
            "extraSportSessionsPerWeek INTEGER NOT NULL DEFAULT 0",
            "walkingMinutesPerWeek INTEGER NOT NULL DEFAULT 0",
            "maxSessionMinutes INTEGER",
            "maxMealPrepMinutes INTEGER",
            "sleepHours REAL",
            "sleepQuality TEXT NOT NULL DEFAULT 'MEDIUM'",
            "bedtime TEXT NOT NULL DEFAULT ''",
            "wakeTime TEXT NOT NULL DEFAULT ''",
            "stressLevel TEXT NOT NULL DEFAULT 'MEDIUM'",
            "dailyEnergy TEXT NOT NULL DEFAULT 'MEDIUM'",
            "recoveryCapacity TEXT NOT NULL DEFAULT 'MEDIUM'",
            "hydrationLitersPerDay REAL",
            "caffeineCupsPerDay INTEGER",
            "alcoholUnitsPerWeek INTEGER",
            "breakfastPreference TEXT NOT NULL DEFAULT 'SWEET'",
            "likedFoods TEXT NOT NULL DEFAULT ''",
            "dislikedFoods TEXT NOT NULL DEFAULT ''",
            "dietaryRestrictions TEXT NOT NULL DEFAULT ''",
            "cookingTimeAvailable TEXT NOT NULL DEFAULT 'MEDIUM'",
            "mealPrepPossible INTEGER NOT NULL DEFAULT 0",
            "prefersSimpleFoods INTEGER NOT NULL DEFAULT 1",
            "dietDifficulties TEXT NOT NULL DEFAULT ''",
            "trainingYears REAL",
            "trainingFrequencyPerWeek INTEGER",
            "preferredExercises TEXT NOT NULL DEFAULT ''",
            "dislikedExercises TEXT NOT NULL DEFAULT ''",
            "unknownExercises TEXT NOT NULL DEFAULT ''",
            "technicalLevel TEXT NOT NULL DEFAULT 'NOVICE'",
            "priorityMuscles TEXT NOT NULL DEFAULT ''",
            "currentLoadsJson TEXT NOT NULL DEFAULT ''",
            "secondaryGoals TEXT NOT NULL DEFAULT ''",
            "desiredOutcome TEXT NOT NULL DEFAULT ''",
            "targetTimelineWeeks INTEGER",
            "activityFactorOverride REAL",
            "calorieTargetOverride REAL"
        )
        profileCols.forEach { db.execSQL("ALTER TABLE user_profile ADD COLUMN $it") }

        // --- exercises: pattern di movimento e difficolta ---
        db.execSQL("ALTER TABLE exercises ADD COLUMN movementPattern TEXT NOT NULL DEFAULT 'OTHER'")
        db.execSQL("ALTER TABLE exercises ADD COLUMN difficulty INTEGER NOT NULL DEFAULT 1")

        // --- planned_exercises: spiegazioni, tecnica d'intensita, pattern ---
        db.execSQL("ALTER TABLE planned_exercises ADD COLUMN selectionReason TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE planned_exercises ADD COLUMN restReason TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE planned_exercises ADD COLUMN intensityTechnique TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE planned_exercises ADD COLUMN techniqueReason TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE planned_exercises ADD COLUMN movementPattern TEXT NOT NULL DEFAULT 'OTHER'")

        // --- piani: versione + carb cycling condizionale ---
        db.execSQL("ALTER TABLE nutrition_plans ADD COLUMN carbCyclingApplied INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE nutrition_plans ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE workout_plans ADD COLUMN version INTEGER NOT NULL DEFAULT 1")

        // --- nuove tabelle ---
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `scientific_sources` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`kind` TEXT NOT NULL, `title` TEXT NOT NULL, `authors` TEXT NOT NULL DEFAULT '', " +
                "`organization` TEXT NOT NULL DEFAULT '', `url` TEXT NOT NULL, " +
                "`publishedDate` TEXT NOT NULL DEFAULT '', `lastUpdated` TEXT NOT NULL DEFAULT '', " +
                "`topic` TEXT NOT NULL, `evidence` TEXT NOT NULL, `derivedRule` TEXT NOT NULL, " +
                "`applicabilityLimits` TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scientific_sources_url` ON `scientific_sources` (`url`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plan_versions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`profileId` INTEGER NOT NULL DEFAULT 1, `domain` TEXT NOT NULL, " +
                "`planId` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `label` TEXT NOT NULL, `reason` TEXT NOT NULL, " +
                "`snapshotJson` TEXT NOT NULL, `isActive` INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_versions_createdAt` ON `plan_versions` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_versions_domain` ON `plan_versions` (`domain`)")
    }
}
