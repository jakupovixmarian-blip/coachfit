package dev.fitcoach.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migrazione 4 -> 5 su un database reale (Robolectric).
 *
 * Aggiunge routine_exercise.targetWeightKg senza perdere le righe esistenti. Lo schema v4 non e'
 * mai stato esportato dal progetto, quindi si parte dallo schema v3 (esportato) e si applica la
 * catena intera 3 -> 4 -> 5: la 4 -> 5 e' l'ultimo anello e viene cosi' eseguita davvero.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationToV5Test {

    private val dbName = "migration-v5-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FitCoachDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun `catena 3 a 5 aggiunge targetWeightKg e conserva le righe`() {
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "INSERT INTO routine (id, name, description, isActive, createdAt, programId, dayIndex, dayLabel) " +
                    "VALUES (1, 'Scheda v3', 'prima della migrazione', 1, 100, 0, 0, '')"
            )
            execSQL(
                "INSERT INTO routine_exercise " +
                    "(id, routineId, exerciseId, orderIndex, targetSets, targetRepsMin, targetRepsMax, targetRir, restSeconds, notes) " +
                    "VALUES (1, 1, 5, 0, 4, 8, 10, 2, 90, 'nota preesistente')"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            dbName,
            5,
            true,
            FitCoachDatabase.MIGRATION_3_4,
            FitCoachDatabase.MIGRATION_4_5
        )

        migrated.query(
            "SELECT targetSets, targetRepsMin, targetRepsMax, restSeconds, notes, targetWeightKg " +
                "FROM routine_exercise WHERE id = 1"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(4, cursor.getInt(0))
            assertEquals(8, cursor.getInt(1))
            assertEquals(10, cursor.getInt(2))
            assertEquals(90, cursor.getInt(3))
            assertEquals("nota preesistente", cursor.getString(4))
            assertEquals(true, cursor.isNull(5))
        }

        migrated.close()
    }

    @Test
    fun `dopo la migrazione il peso target e scrivibile e rileggibile`() {
        helper.createDatabase(dbName, 3).close()
        val migrated = helper.runMigrationsAndValidate(
            dbName,
            5,
            true,
            FitCoachDatabase.MIGRATION_3_4,
            FitCoachDatabase.MIGRATION_4_5
        )

        migrated.execSQL(
            "INSERT INTO routine (id, name, description, isActive, createdAt, programId, dayIndex, dayLabel) " +
                "VALUES (2, 'Scheda nuova', 'post migrazione', 1, 200, 0, 0, '')"
        )
        migrated.execSQL(
            "INSERT INTO routine_exercise " +
                "(id, routineId, exerciseId, orderIndex, targetSets, targetRepsMin, targetRepsMax, targetRir, restSeconds, notes, targetWeightKg) " +
                "VALUES (2, 2, 5, 0, 3, 6, 8, 1, 120, '', 42.5)"
        )

        migrated.query("SELECT targetWeightKg FROM routine_exercise WHERE id = 2").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(42.5, cursor.getDouble(0), 0.0001)
        }

        migrated.close()
    }
}
