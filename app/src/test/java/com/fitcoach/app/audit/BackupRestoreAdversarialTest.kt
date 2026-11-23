package com.fitcoach.app.audit

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.WeightRecordEntity
import com.fitcoach.app.data.repository.DatabaseBackupRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Il ripristino e' l'unica operazione dell'app che puo' distruggere dati in
 * modo irreversibile: chiude il database, poi sovrascrive il file. Se il file
 * scelto dall'utente non e' un database FitCoach valido, i dati originali sono
 * gia' stati chiusi e persi. Questi test provano a passare file sbagliati.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BackupRestoreAdversarialTest {

    private lateinit var context: Context
    private lateinit var db: FitCoachDatabase
    private lateinit var backup: DatabaseBackupRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Il ripristino lavora sui file veri del database: serve un nome su disco.
        db = Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.DB_NAME)
            .allowMainThreadQueries()
            .build()
        backup = DatabaseBackupRepository(context, db)
    }

    @After
    fun tearDown() {
        runCatching { db.close() }
        context.getDatabasePath(FitCoachDatabase.DB_NAME).delete()
    }

    private fun uriDi(file: File): Uri = Uri.fromFile(file)

    /** Un file di testo qualsiasi: l'utente ha scelto il documento sbagliato. */
    @Test
    fun `un file non database viene rifiutato senza distruggere i dati`() = runTest {
        db.progressDao().insertWeight(WeightRecordEntity(dateEpochDay = 20000, weightKg = 80.0))

        val intruso = File(context.cacheDir, "documento.txt")
        intruso.writeText("questo non e' un database")

        try {
            backup.restoreBackup(uriDi(intruso))
            fail("Un file non database deve essere rifiutato, non copiato sopra i dati")
        } catch (expected: Exception) {
            // atteso
        }

        // I dati originali devono essere ancora leggibili.
        val riaperto = Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.DB_NAME)
            .allowMainThreadQueries()
            .build()
        try {
            val pesi = riaperto.progressDao().getWeights()
            assertTrue("I pesi originali devono sopravvivere a un ripristino fallito", pesi.isNotEmpty())
            assertTrue("Il peso registrato prima del ripristino deve essere intatto",
                pesi.any { it.weightKg == 80.0 })
        } finally {
            riaperto.close()
        }
    }

    /** Un file troncato: e' un SQLite valido in intestazione ma incompleto. */
    @Test
    fun `un database troncato non fa perdere i dati`() = runTest {
        db.progressDao().insertWeight(WeightRecordEntity(dateEpochDay = 20000, weightKg = 80.0))
        val valido = backup.createBackup()

        val troncato = File(context.cacheDir, "troncato.db")
        val bytes = valido.readBytes()
        troncato.writeBytes(bytes.copyOf(bytes.size / 3))

        try {
            backup.restoreBackup(uriDi(troncato))
            // Se accettato, il database deve almeno essere apribile e coerente.
            val riaperto = Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.DB_NAME)
                .allowMainThreadQueries()
                .build()
            try {
                riaperto.progressDao().getWeights()
            } finally {
                riaperto.close()
            }
        } catch (expected: Exception) {
            val riaperto = Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.DB_NAME)
                .allowMainThreadQueries()
                .build()
            try {
                assertTrue(
                    "Dopo un ripristino fallito i dati originali devono essere ancora presenti",
                    riaperto.progressDao().getWeights().any { it.weightKg == 80.0 }
                )
            } finally {
                riaperto.close()
            }
        }
    }

    @Test
    fun `un backup valido viene ripristinato`() = runTest {
        db.progressDao().insertWeight(WeightRecordEntity(dateEpochDay = 20000, weightKg = 80.0))
        val valido = backup.createBackup()
        assertNotNull("Il backup deve essere creato", valido)
        assertTrue("Il file di backup deve esistere", valido.exists())
        assertTrue("Il backup non deve essere vuoto", valido.length() > 0)

        db.progressDao().insertWeight(WeightRecordEntity(dateEpochDay = 20001, weightKg = 90.0))

        backup.restoreBackup(uriDi(valido))

        val riaperto = Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.DB_NAME)
            .allowMainThreadQueries()
            .build()
        try {
            val pesi = riaperto.progressDao().getWeights()
            assertTrue("Il peso del backup deve essere presente", pesi.any { it.weightKg == 80.0 })
            assertTrue("Il peso aggiunto dopo il backup non deve esserci",
                pesi.none { it.weightKg == 90.0 })
        } finally {
            riaperto.close()
        }
    }

    /** Un uri che non esiste piu' non deve lasciare il database chiuso. */
    @Test
    fun `un uri illeggibile non lascia il database inutilizzabile`() = runTest {
        db.progressDao().insertWeight(WeightRecordEntity(dateEpochDay = 20000, weightKg = 80.0))
        val mancante = Uri.fromFile(File(context.cacheDir, "non_esiste.db"))

        try {
            backup.restoreBackup(mancante)
            fail("Un uri illeggibile deve produrre un errore")
        } catch (expected: Exception) {
            // atteso
        }

        // Il database non deve essere stato chiuso prima di sapere che il file era leggibile.
        val riaperto = Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.DB_NAME)
            .allowMainThreadQueries()
            .build()
        try {
            assertTrue(
                "Il database deve restare integro dopo un uri illeggibile",
                riaperto.progressDao().getWeights().any { it.weightKg == 80.0 }
            )
        } finally {
            riaperto.close()
        }
    }
}
