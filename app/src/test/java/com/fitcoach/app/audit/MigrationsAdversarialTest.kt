package com.fitcoach.app.audit

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.MIGRATION_1_2
import com.fitcoach.app.data.local.MIGRATION_2_3
import com.fitcoach.app.data.local.MIGRATION_3_4
import com.fitcoach.app.data.local.MIGRATION_4_5
import com.fitcoach.app.data.local.MIGRATION_5_6
import com.fitcoach.app.data.local.MIGRATION_6_7
import com.fitcoach.app.data.local.MIGRATION_7_8
import com.fitcoach.app.data.local.MIGRATION_8_9
import com.fitcoach.app.data.local.MIGRATION_9_10
import com.fitcoach.app.data.local.MIGRATION_10_11
import com.fitcoach.app.data.local.MIGRATION_11_12
import com.fitcoach.app.data.local.MIGRATION_12_13
import com.fitcoach.app.data.local.MIGRATION_13_14
import com.fitcoach.app.data.local.MIGRATION_14_15
import com.fitcoach.app.data.local.MIGRATION_15_16
import com.fitcoach.app.data.local.MIGRATION_16_17
import com.fitcoach.app.data.local.MIGRATION_17_18
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Una migrazione rotta distrugge i dati dell'utente a ogni aggiornamento
 * dell'app, e il crash arriva solo sul telefono, non in sviluppo.
 *
 * Ogni gradino si verifica con lo schema esportato: Room confronta la tabella
 * risultante con quella attesa dalla versione dichiarata.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationsAdversarialTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FitCoachDatabase::class.java
    )

    /** Tutti i gradini, nell'ordine in cui l'app li applica. */
    private val migrazioni = listOf(
        MIGRATION_1_2 to (1 to 2),
        MIGRATION_2_3 to (2 to 3),
        MIGRATION_3_4 to (3 to 4),
        MIGRATION_4_5 to (4 to 5),
        MIGRATION_5_6 to (5 to 6),
        MIGRATION_6_7 to (6 to 7),
        MIGRATION_7_8 to (7 to 8),
        MIGRATION_8_9 to (8 to 9),
        MIGRATION_9_10 to (9 to 10),
        MIGRATION_10_11 to (10 to 11),
        MIGRATION_11_12 to (11 to 12),
        MIGRATION_12_13 to (12 to 13),
        MIGRATION_13_14 to (13 to 14),
        MIGRATION_14_15 to (14 to 15),
        MIGRATION_15_16 to (15 to 16),
        MIGRATION_16_17 to (16 to 17),
        MIGRATION_17_18 to (17 to 18)
    )

    @Test
    fun `ogni migrazione dichiara il gradino che collega`() {
        migrazioni.forEachIndexed { i, (m, atteso) ->
            assertEquals(
                "La migrazione in posizione $i non parte dalla versione attesa",
                atteso.first, m.startVersion
            )
            assertEquals(
                "La migrazione in posizione $i non arriva alla versione attesa",
                atteso.second, m.endVersion
            )
        }
    }

    @Test
    fun `la catena e' continua e senza buchi`() {
        migrazioni.forEachIndexed { i, (_, v) ->
            assertEquals("Buco nella catena alla posizione $i", i + 1, v.first)
            assertEquals("Gradino non consecutivo alla posizione $i", v.first + 1, v.second)
        }
        // Se la versione dichiarata in @Database salisse senza una nuova
        // migrazione, l'app crasherebbe all'avvio su ogni telefono aggiornato.
        // Invece di riflettere sull'annotazione, si apre il database come fa
        // l'app: se manca un gradino Room solleva IllegalStateException.
        val ultima = migrazioni.last().second.second
        helper.createDatabase(dbName, ultima).close()
        val aperto = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitCoachDatabase::class.java,
            dbName
        ).addMigrations(*migrazioni.map { it.first }.toTypedArray()).build()
        aperto.openHelper.readableDatabase
        aperto.close()
    }

    @Test
    fun `ogni gradino passa la verifica dello schema esportato`() {
        migrazioni.forEach { (migrazione, versioni) ->
            val (da, a) = versioni
            helper.createDatabase(dbName, da).close()
            val db = helper.runMigrationsAndValidate(dbName, a, true, migrazione)
            db.close()
        }
    }

    @Test
    fun `la migrazione completa da 1 a 17 conserva una tabella reale`() {
        helper.createDatabase(dbName, 1).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 17, true, *migrazioni.map { it.first }.toTypedArray()
        )
        // Il database migrato deve essere interrogabile: uno schema che valida
        // ma non risponde alle query dell'app non serve a nulla.
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            val tabelle = mutableListOf<String>()
            while (c.moveToNext()) tabelle += c.getString(0)
            assertTrue("Nessuna tabella dopo la migrazione", tabelle.isNotEmpty())
            assertTrue(
                "Manca weight_records dopo la migrazione: $tabelle",
                tabelle.contains("weight_records")
            )
            assertTrue(
                "Manca user_profile dopo la migrazione: $tabelle",
                tabelle.contains("user_profile")
            )
        }
        db.close()
    }

    @Test
    fun `il database migrato accetta una scrittura e la rilegge`() {
        helper.createDatabase(dbName, 1).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 17, true, *migrazioni.map { it.first }.toTypedArray()
        )
        db.execSQL(
            "INSERT INTO weight_records (profileId, dateEpochDay, weightKg, note) " +
                "VALUES (1, 20000, 81.5, '')"
        )
        db.query("SELECT weightKg FROM weight_records").use { c ->
            assertTrue("La pesata scritta non si rilegge", c.moveToFirst())
            assertEquals(81.5, c.getDouble(0), 0.001)
        }
        db.close()
    }

    @Test
    fun `un database aperto all'ultima versione e' gia' aggiornato`() {
        // Nessuna migrazione applicabile: se il builder pretendesse una
        // migrazione per l'ultima versione, ogni installazione nuova fallirebbe.
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FitCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        db.query("SELECT 1", null).use { c -> assertTrue(c.moveToFirst()) }
        db.close()
    }
}
