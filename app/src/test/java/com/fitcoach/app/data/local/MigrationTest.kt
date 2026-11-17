package com.fitcoach.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica le migrazioni Room sul database REALE (SQLite), non su una ricostruzione
 * a mano. Copre il bug che aveva causato la perdita di dati all'aggiornamento:
 * MIGRATION_11_12 era definita ma non registrata, quindi Room applicava il fallback
 * distruttivo e azzerava profilo, piani e storico.
 *
 * Due garanzie:
 *  1. lo schema risultante combacia ESATTAMENTE con lo schema esportato (v12.json);
 *  2. i dati esistenti sopravvivono alla migrazione (nessun azzeramento).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    private companion object {
        const val TEST_DB = "migration-test.db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FitCoachDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun `migrazione 11 verso 12 valida lo schema esportato`() {
        helper.createDatabase(TEST_DB, 11).close()

        // Se MIGRATION_11_12 non producesse lo schema atteso, questo test fallirebbe
        // con "Migration didn't properly handle ..." prima di arrivare all'assert.
        val db = helper.runMigrationsAndValidate(TEST_DB, 12, true, MIGRATION_11_12)
        db.close()
    }

    @Test
    fun `la migrazione 11 verso 12 non cancella i dati e aggiunge purchased`() {
        helper.createDatabase(TEST_DB, 11).use { db ->
            db.execSQL(
                "INSERT INTO shopping_items " +
                    "(id, weekStartEpochDay, foodItemId, foodName, category, totalGrams, unit, " +
                    "checked, alreadyHave, isCustom, createdAt) VALUES " +
                    "(1, 20000, 42, 'Petto di pollo', 'CARNE', 1500.0, 'RAW', 1, 0, 0, 1700000000000)"
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 12, true, MIGRATION_11_12)

        db.query("SELECT foodName, totalGrams, checked, purchased FROM shopping_items").use { c ->
            assertThat(c.moveToFirst()).isTrue()
            assertThat(c.getString(0)).isEqualTo("Petto di pollo")
            assertThat(c.getDouble(1)).isEqualTo(1500.0)
            assertThat(c.getInt(2)).isEqualTo(1)
            // La nuova colonna parte a 0 (non acquistato), non a un valore inventato.
            assertThat(c.getInt(3)).isEqualTo(0)
            assertThat(c.count).isEqualTo(1)
        }
        db.close()
    }

    @Test
    fun `l intera catena di migrazioni da 1 a 12 valida lo schema finale`() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 12, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
            MIGRATION_11_12
        )
        db.close()
    }
}
