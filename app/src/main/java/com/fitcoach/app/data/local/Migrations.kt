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
            "targetTimelineWeeks INTEGER"
        )
        // NB: activityFactorOverride e calorieTargetOverride esistono gia in v1:
        // NON vanno ri-aggiunte (causerebbe "duplicate column name" in upgrade).
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

/**
 * Migrazione schema v2 -> v3.
 *
 * Aggiunge il diario nutrizionale (diary_entries), la lista della spesa
 * (shopping_items) e lo storico delle sostituzioni (substitution_log). Queste
 * tabelle sono indipendenti: nessuna perdita dei dati esistenti.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `diary_entries` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dateEpochDay` INTEGER NOT NULL, `mealSlot` TEXT NOT NULL, " +
                "`time` TEXT NOT NULL DEFAULT '', `foodItemId` INTEGER NOT NULL, " +
                "`foodName` TEXT NOT NULL, `grams` REAL NOT NULL, " +
                "`unit` TEXT NOT NULL DEFAULT 'RAW', `kcal` REAL NOT NULL, " +
                "`protein` REAL NOT NULL, `carbs` REAL NOT NULL, `fat` REAL NOT NULL, " +
                "`source` TEXT NOT NULL DEFAULT 'EXTRA', `planEntryId` INTEGER, " +
                "`active` INTEGER NOT NULL DEFAULT 1, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_dateEpochDay` ON `diary_entries` (`dateEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_planEntryId` ON `diary_entries` (`planEntryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_mealSlot` ON `diary_entries` (`mealSlot`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `shopping_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`weekStartEpochDay` INTEGER NOT NULL, `foodItemId` INTEGER NOT NULL, " +
                "`foodName` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                "`totalGrams` REAL NOT NULL, `unit` TEXT NOT NULL DEFAULT 'RAW', " +
                "`checked` INTEGER NOT NULL DEFAULT 0, `alreadyHave` INTEGER NOT NULL DEFAULT 0, " +
                "`isCustom` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_weekStartEpochDay` ON `shopping_items` (`weekStartEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_foodItemId` ON `shopping_items` (`foodItemId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `substitution_log` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `dateEpochDay` INTEGER NOT NULL, " +
                "`mealEntryId` INTEGER NOT NULL, `mealSlot` TEXT NOT NULL, " +
                "`originalFoodId` INTEGER NOT NULL, `originalName` TEXT NOT NULL, " +
                "`originalGrams` REAL NOT NULL, `newFoodId` INTEGER NOT NULL, " +
                "`newName` TEXT NOT NULL, `newGrams` REAL NOT NULL, " +
                "`kcalBefore` REAL NOT NULL, `kcalAfter` REAL NOT NULL, " +
                "`proteinBefore` REAL NOT NULL, `proteinAfter` REAL NOT NULL, " +
                "`carbsBefore` REAL NOT NULL, `carbsAfter` REAL NOT NULL, " +
                "`fatBefore` REAL NOT NULL, `fatAfter` REAL NOT NULL, " +
                "`status` TEXT NOT NULL, `note` TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_substitution_log_createdAt` ON `substitution_log` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_substitution_log_dateEpochDay` ON `substitution_log` (`dateEpochDay`)")
    }
}

/**
 * Migrazione schema v3 -> v4.
 *
 * Aggiunge: budget mensile e gate anamnesi al profilo; campi di provenienza,
 * deduplicazione e prezzo al database alimentare (per la ricerca progressiva
 * su fonti esterne e il motore del budget). Nessun dato esistente viene perso.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- user_profile: budget mensile + anamnesi ---
        db.execSQL("ALTER TABLE user_profile ADD COLUMN monthlyFoodBudgetEur REAL")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN budgetScope TEXT NOT NULL DEFAULT 'PERSONAL_ONLY'")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN budgetPersons INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN anamnesisCompleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN anamnesisConfirmedAt INTEGER")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN anamnesisSummary TEXT NOT NULL DEFAULT ''")

        // --- food_database: provenienza, dedup, prezzo ---
        db.execSQL("ALTER TABLE food_database ADD COLUMN sourceId TEXT")
        db.execSQL("ALTER TABLE food_database ADD COLUMN normalizedName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE food_database ADD COLUMN brand TEXT")
        db.execSQL("ALTER TABLE food_database ADD COLUMN pricePerKgEur REAL")
        db.execSQL("ALTER TABLE food_database ADD COLUMN acquiredAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_database ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        // Backfill del nome normalizzato per gli alimenti esistenti.
        db.execSQL(
            "UPDATE food_database SET normalizedName = " +
                "trim(replace(replace(replace(replace(replace(replace(lower(name), " +
                "'à','a'),'è','e'),'é','e'),'ì','i'),'ò','o'),'ù','u')) " +
                "WHERE normalizedName = ''"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_database_normalizedName` ON `food_database` (`normalizedName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_database_sourceId` ON `food_database` (`sourceId`)")
    }
}

/**
 * Migrazione schema v4 -> v5.
 *
 * Impedisce duplicati della stessa voce di piano nello stesso giorno, senza
 * impedire la registrazione della voce negli altri giorni del calendario.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM diary_entries WHERE planEntryId IS NOT NULL AND id NOT IN (" +
                "SELECT MIN(id) FROM diary_entries WHERE planEntryId IS NOT NULL " +
                "GROUP BY dateEpochDay, planEntryId)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_diary_entries_dateEpochDay_planEntryId` " +
                "ON `diary_entries` (`dateEpochDay`, `planEntryId`)"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE user_profile ADD COLUMN trainingStatus TEXT"
        )
    }
}

/** Migrazione schema v6 -> v7: override calendario e riferimenti foto locali. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_schedule_overrides` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `planId` INTEGER NOT NULL, `workoutDayId` INTEGER NOT NULL, `weekStartEpochDay` INTEGER NOT NULL, `targetDayIndex` INTEGER NOT NULL, `permanent` INTEGER NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_schedule_overrides_weekStartEpochDay` ON `workout_schedule_overrides` (`weekStartEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_schedule_overrides_planId` ON `workout_schedule_overrides` (`planId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `progress_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `uri` TEXT NOT NULL, `capturedAt` INTEGER NOT NULL, `pose` TEXT NOT NULL, `note` TEXT NOT NULL, `consentForAi` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_progress_photos_capturedAt` ON `progress_photos` (`capturedAt`)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN coachAuthorizationMode TEXT NOT NULL DEFAULT 'CONFIRMATION'")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `free_session_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `exerciseName` TEXT NOT NULL, `primaryMuscle` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `sets` INTEGER NOT NULL, `minReps` INTEGER NOT NULL, `maxReps` INTEGER NOT NULL, `restSeconds` INTEGER NOT NULL, `targetRir` INTEGER NOT NULL, `technique` TEXT NOT NULL, `category` TEXT NOT NULL, `movementPattern` TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_free_session_exercises_sessionId` ON `free_session_exercises` (`sessionId`)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE program_changes ADD COLUMN outcomeStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE program_changes ADD COLUMN evaluatedAt INTEGER")
        db.execSQL("ALTER TABLE program_changes ADD COLUMN actualOutcome TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE program_changes ADD COLUMN followUpAction TEXT NOT NULL DEFAULT ''")
    }
}

/** Migrazione schema v10 -> v11: persistenza isolata per anamnesi e generazione 2.0. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `v2_anamnesis_sessions` (`id` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `confirmedAt` INTEGER, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_v2_anamnesis_sessions_updatedAt` ON `v2_anamnesis_sessions` (`updatedAt`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `v2_anamnesis_answers` (`sessionId` TEXT NOT NULL, `field` TEXT NOT NULL, `status` TEXT NOT NULL, `value` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`sessionId`, `field`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_v2_anamnesis_answers_sessionId` ON `v2_anamnesis_answers` (`sessionId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `v2_generation_jobs` (`id` TEXT NOT NULL, `profileVersion` INTEGER NOT NULL, `stateJson` TEXT NOT NULL, `committed` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_v2_generation_jobs_updatedAt` ON `v2_generation_jobs` (`updatedAt`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `v2_generation_payloads` (`jobId` TEXT NOT NULL, `phase` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`jobId`, `phase`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_v2_generation_payloads_jobId` ON `v2_generation_payloads` (`jobId`)")
    }
}

/** Migrazione schema v11 -> v12: stato "acquistato" per la lista della spesa. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shopping_items ADD COLUMN purchased INTEGER NOT NULL DEFAULT 0")
    }
}

/** Migrazione schema v12 -> v13: identita esplicita del piano nel diario e vincoli attivi. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE diary_entries ADD COLUMN nutritionPlanId INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_nutritionPlanId` ON `diary_entries` (`nutritionPlanId`)")
        db.execSQL(
            """
            UPDATE diary_entries
            SET nutritionPlanId = (
                SELECT nutrition_days.planId
                FROM meal_entries
                INNER JOIN meals ON meals.id = meal_entries.mealId
                INNER JOIN nutrition_days ON nutrition_days.id = meals.nutritionDayId
                WHERE meal_entries.id = diary_entries.planEntryId
            )
            WHERE source = 'PLAN' AND planEntryId IS NOT NULL
            """.trimIndent()
        )

        // Ripara i database creati dalla versione con piu piani attivi prima di
        // installare i trigger che rendono invalido quello stato.
        db.execSQL(
            """
            UPDATE nutrition_plans
            SET isActive = 0
            WHERE isActive = 1 AND id NOT IN (
                SELECT MAX(id) FROM nutrition_plans
                WHERE isActive = 1 GROUP BY profileId
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE workout_plans
            SET isActive = 0
            WHERE isActive = 1 AND id NOT IN (
                SELECT MAX(id) FROM workout_plans
                WHERE isActive = 1 GROUP BY profileId
            )
            """.trimIndent()
        )
        createActivePlanTriggers(db)
    }
}

/**
 * Aggiunge la provenienza delle risposte dell'anamnesi.
 *
 * I database esistenti hanno un'anamnesi gia confermata, quindi tutte le
 * domande obbligatorie erano state poste: si marcano come risposte per non
 * degradare i piani gia generati. Le facoltative restano non dichiarate
 * (non e possibile sapere a posteriori se l'utente le ha compilate).
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN answeredQuestionIds TEXT NOT NULL DEFAULT ''")
        val mandatory = listOf(
            "age", "sex", "heightCm", "weightKg", "goal", "workType", "transportMode",
            "sleepHours", "sleepQuality", "stressLevel", "dailyEnergy", "recoveryCapacity",
            "mealsPerDay", "breakfastPreference", "dietaryPreferences", "cookingTimeAvailable",
            "mealPrepPossible", "trainingStatus", "experience", "trainingDaysPerWeek",
            "technicalLevel", "availableEquipment"
        ).joinToString("\u001F")
        db.execSQL(
            "UPDATE user_profile SET answeredQuestionIds = ? WHERE anamnesisCompleted = 1",
            arrayOf(mandatory)
        )
    }
}

/**
 * Ricette create dall'utente. Nuova tabella: nessun dato esistente da migrare,
 * le ricette precedenti non esistono.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_recipes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `slot` TEXT NOT NULL,
                `dayIndex` INTEGER NOT NULL,
                `style` TEXT NOT NULL,
                `ingredients` TEXT NOT NULL,
                `steps` TEXT NOT NULL,
                `kcal` REAL NOT NULL,
                `protein` REAL NOT NULL,
                `carbs` REAL NOT NULL,
                `fat` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_recipes_createdAt` ON `saved_recipes` (`createdAt`)")
    }
}

/**
 * Preferenze dell'avviso di fine recupero. Le colonne nascono con DEFAULT 1,
 * quindi chi aggiorna trova l'avviso attivo come prima dell'aggiornamento.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN restAlarmSound INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN restAlarmVibration INTEGER NOT NULL DEFAULT 1")
    }
}

/**
 * Inventario dettagliato dell'attrezzatura (spec §4).
 *
 * Colonna aggiunta con default vuoto: i profili esistenti restano leggibili e
 * l'inventario risulta da compilare, invece di essere inventato.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN gearInventoryJson TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Personalizzazione delle giornate carb cycling (sez. 5.2) e interruttore del
 * backup automatico. Un solo gradino: la v18 non e' mai stata pubblicata, quindi
 * non esiste un database installato da aggiornare a meta' strada.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN carbHighDaysOverride INTEGER")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN carbMediumDaysOverride INTEGER")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN carbLowDaysOverride INTEGER")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN autoBackupEnabled INTEGER NOT NULL DEFAULT 1")
    }
}
