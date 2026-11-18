package com.fitcoach.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

/** Crea i vincoli SQLite che impediscono piu piani attivi per profilo. */
fun createActivePlanTriggers(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS nutrition_plans_one_active_insert
        BEFORE INSERT ON nutrition_plans
        WHEN NEW.isActive = 1 AND EXISTS (
            SELECT 1 FROM nutrition_plans
            WHERE profileId = NEW.profileId AND isActive = 1
        )
        BEGIN
            SELECT RAISE(ABORT, 'one active nutrition plan per profile');
        END
        """.trimIndent()
    )
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS nutrition_plans_one_active_update
        BEFORE UPDATE OF profileId, isActive ON nutrition_plans
        WHEN NEW.isActive = 1 AND EXISTS (
            SELECT 1 FROM nutrition_plans
            WHERE profileId = NEW.profileId AND isActive = 1 AND id != NEW.id
        )
        BEGIN
            SELECT RAISE(ABORT, 'one active nutrition plan per profile');
        END
        """.trimIndent()
    )
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS workout_plans_one_active_insert
        BEFORE INSERT ON workout_plans
        WHEN NEW.isActive = 1 AND EXISTS (
            SELECT 1 FROM workout_plans
            WHERE profileId = NEW.profileId AND isActive = 1
        )
        BEGIN
            SELECT RAISE(ABORT, 'one active workout plan per profile');
        END
        """.trimIndent()
    )
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS workout_plans_one_active_update
        BEFORE UPDATE OF profileId, isActive ON workout_plans
        WHEN NEW.isActive = 1 AND EXISTS (
            SELECT 1 FROM workout_plans
            WHERE profileId = NEW.profileId AND isActive = 1 AND id != NEW.id
        )
        BEGIN
            SELECT RAISE(ABORT, 'one active workout plan per profile');
        END
        """.trimIndent()
    )
}
