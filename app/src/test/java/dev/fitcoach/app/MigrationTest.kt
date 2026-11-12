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
 * Runs the real migration on a v1 database and checks that the new program columns
 * are added without losing the existing routines.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FitCoachDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun `migration from 1 to 2 adds the program columns and keeps the rows`() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO routine (name, description, isActive, createdAt) " +
                    "VALUES ('Scheda vecchia', 'creata prima dell aggiornamento', 1, 100)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            FitCoachDatabase.MIGRATION_1_2
        )

        migrated.query("SELECT name, isActive, programId, dayIndex, dayLabel FROM routine").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Scheda vecchia", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            // A routine created before the update stays a standalone single session.
            assertEquals(0L, cursor.getLong(2))
            assertEquals(0, cursor.getInt(3))
            assertEquals("", cursor.getString(4))
        }
        migrated.close()
    }

    @Test
    fun `a program saved after the migration is readable by the new query`() {
        helper.createDatabase(dbName, 1).close()
        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, FitCoachDatabase.MIGRATION_1_2)

        migrated.execSQL(
            "INSERT INTO routine (name, description, isActive, createdAt, programId, dayIndex, dayLabel) " +
                "VALUES ('Giorno A - Upper 1', 'programma', 1, 200, 7, 0, 'Giorno A - Upper 1')"
        )
        migrated.execSQL(
            "INSERT INTO routine (name, description, isActive, createdAt, programId, dayIndex, dayLabel) " +
                "VALUES ('Giorno B - Lower 1', 'programma', 1, 200, 7, 1, 'Giorno B - Lower 1')"
        )

        migrated.query(
            "SELECT name FROM routine WHERE programId = 7 AND programId != 0 ORDER BY dayIndex ASC"
        ).use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals("Giorno A - Upper 1", cursor.getString(0))
            cursor.moveToNext()
            assertEquals("Giorno B - Lower 1", cursor.getString(0))
        }
        migrated.close()
    }
}
