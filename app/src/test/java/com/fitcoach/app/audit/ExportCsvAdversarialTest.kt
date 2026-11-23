package com.fitcoach.app.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.CheckInEntity
import com.fitcoach.app.data.local.entity.CircumferenceRecordEntity
import com.fitcoach.app.data.local.entity.ExercisePerformanceEntity
import com.fitcoach.app.data.local.entity.WeightRecordEntity
import com.fitcoach.app.data.local.entity.WorkoutSessionEntity
import com.fitcoach.app.data.local.entity.WorkoutSetEntity
import com.fitcoach.app.data.repository.ExportRepository
import com.fitcoach.app.domain.model.SessionStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * L'export e' una funzione distruttiva al contrario: se il file e' malformato
 * l'utente se ne accorge quando prova a riaprirlo, quando i dati originali
 * possono non esserci piu'. Questi test provano a rompere il formato con i casi
 * che il codice attuale non copre: separatore decimale italiano, caratteri
 * speciali nei titoli, valori non finiti, database vuoto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExportCsvAdversarialTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var export: ExportRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        export = ExportRepository(
            progressDao = db.progressDao(),
            checkInDao = db.checkInDao(),
            sessionDao = db.workoutSessionDao(),
            setDao = db.workoutSetDao(),
            performanceDao = db.exercisePerformanceDao()
        )
    }

    @After
    fun tearDown() = db.close()

    /**
     * Record logici del CSV: un campo quotato puo' contenere un a capo, quindi
     * la riga fisica non coincide col record. Spezzare su '\n' produrrebbe
     * falsi allarmi su un file corretto.
     */
    private fun dataLines(csv: String): List<String> {
        val record = mutableListOf<String>()
        val corrente = StringBuilder()
        var inQuote = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            when {
                c == '"' -> {
                    if (inQuote && i + 1 < csv.length && csv[i + 1] == '"') {
                        corrente.append("\"\""); i++
                    } else {
                        inQuote = !inQuote; corrente.append(c)
                    }
                }
                c == '\n' && !inQuote -> { record.add(corrente.toString()); corrente.clear() }
                else -> corrente.append(c)
            }
            i++
        }
        if (corrente.isNotEmpty()) record.add(corrente.toString())
        return record
    }

    /** Numero di colonne atteso: verifica che ogni riga abbia lo stesso numero di campi. */
    private fun assertRettangolare(csv: String, colonne: Int, nome: String) {
        dataLines(csv).forEachIndexed { i, riga ->
            assertEquals(
                "$nome: la riga $i ha un numero di campi diverso da $colonne (riga: $riga)",
                colonne,
                contaCampi(riga)
            )
        }
    }

    /** Parser RFC 4180 minimo: conta i campi rispettando le virgolette. */
    private fun contaCampi(riga: String): Int {
        var campi = 1
        var inQuote = false
        var i = 0
        while (i < riga.length) {
            val c = riga[i]
            when {
                c == '"' -> if (inQuote && i + 1 < riga.length && riga[i + 1] == '"') i++ else inQuote = !inQuote
                c == ',' && !inQuote -> campi++
            }
            i++
        }
        return campi
    }

    @Test
    fun `database vuoto produce solo intestazione, non righe fantasma`() = runTest {
        assertEquals(1, dataLines(export.exportWeightsCsv()).size)
        assertEquals(1, dataLines(export.exportCircumferencesCsv()).size)
        assertEquals(1, dataLines(export.exportSessionsCsv()).size)
        assertEquals(1, dataLines(export.exportPerformanceCsv()).size)
        assertEquals(1, dataLines(export.exportCheckInsCsv()).size)
    }

    /**
     * Su un telefono in italiano `"%.1f".format(x)` scrive "1234,5". Il valore
     * finisce in una colonna numerica non quotata, quindi il CSV si spezza in
     * due campi e il foglio di calcolo legge i dati sfalsati.
     */
    @Test
    fun `il separatore decimale resta il punto anche con locale italiano`() = runTest {
        val originale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            val sessionId = db.workoutSessionDao().insert(
                WorkoutSessionEntity(
                    dayTitle = "Gambe", dateEpochDay = 20000,
                    status = SessionStatus.COMPLETED
                )
            )
            db.workoutSetDao().insert(
                WorkoutSetEntity(
                    sessionId = sessionId, exerciseId = 1, exerciseName = "Squat",
                    setIndex = 0, plannedReps = 10, actualReps = 8,
                    weightKg = 152.25, completed = true
                )
            )

            val csv = export.exportSessionsCsv()
            assertRettangolare(csv, 9, "sessions")
            assertTrue("Il volume deve usare il punto decimale: $csv", csv.contains("1218.0"))
            assertFalse("Il volume non deve usare la virgola decimale: $csv", csv.contains("1218,0"))
        } finally {
            Locale.setDefault(originale)
        }
    }

    /** Stessa insidia sulla colonna 1RM e sul volume delle performance. */
    @Test
    fun `le performance usano il punto decimale con locale italiano`() = runTest {
        val originale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            db.exercisePerformanceDao().insert(
                ExercisePerformanceEntity(
                    exerciseId = 1, exerciseName = "Panca", sessionId = 1,
                    dateEpochDay = 20000, topWeightKg = 80.5, topReps = 8,
                    totalSets = 4, totalReps = 32, volumeKg = 2576.75,
                    avgRir = 1.5, estimated1Rm = 101.4
                )
            )
            val csv = export.exportPerformanceCsv()
            assertRettangolare(csv, 9, "performance")
            assertFalse("Nessuna virgola decimale nelle colonne numeriche: $csv", csv.contains("2576,75"))
        } finally {
            Locale.setDefault(originale)
        }
    }

    /**
     * Un titolo che inizia con "=" o "+" viene interpretato come formula da
     * Excel e da LibreOffice. Aprendo l'export, la cella esegue un comando
     * invece di mostrare il testo.
     */
    @Test
    fun `un titolo che sembra una formula non viene eseguito dal foglio di calcolo`() = runTest {
        val payload = "=cmd|'/c calc'!A0"
        db.workoutSessionDao().insert(
            WorkoutSessionEntity(
                dayTitle = payload, dateEpochDay = 20000, status = SessionStatus.COMPLETED
            )
        )

        val csv = export.exportSessionsCsv()
        val cella = dataLines(csv)[1].split(",")[2]
        assertFalse(
            "La cella non deve iniziare con '=' perche' il foglio di calcolo la eseguirebbe: $cella",
            cella.startsWith("=")
        )
    }

    @Test
    fun `virgolette virgole e a capo nel titolo non rompono le colonne`() = runTest {
        db.workoutSessionDao().insert(
            WorkoutSessionEntity(
                dayTitle = "Spinta \"pesante\", con\nseconda riga",
                dateEpochDay = 20000, status = SessionStatus.COMPLETED
            )
        )
        val csv = export.exportSessionsCsv()
        assertRettangolare(csv, 9, "sessions con titolo ostile")
        assertTrue("Le virgolette devono essere raddoppiate: $csv", csv.contains("\"\""))
    }

    /**
     * SQLite rifiuta NaN (NOT NULL) ma accetta Infinity. Un peso infinito,
     * arrivato da un import o da un calcolo guasto, finirebbe nel CSV come
     * testo in una colonna numerica: il foglio di calcolo salterebbe la riga
     * senza avvisare. L'export deve scrivere una cella vuota.
     */
    @Test
    fun `un peso non finito non corrompe il file`() = runTest {
        runCatching {
            db.progressDao().insertWeight(
                WeightRecordEntity(dateEpochDay = 20000, weightKg = Double.POSITIVE_INFINITY)
            )
        }
        runCatching {
            db.progressDao().insertWeight(
                WeightRecordEntity(dateEpochDay = 20001, weightKg = Double.NaN)
            )
        }

        val csv = export.exportWeightsCsv()
        assertRettangolare(csv, 3, "weights")
        assertFalse("NaN non e' un numero leggibile da un foglio di calcolo: $csv", csv.contains("NaN"))
        assertFalse("Infinity non e' un numero leggibile: $csv", csv.contains("Infinity"))
    }

    @Test
    fun `il JSON di export completo e valido anche con caratteri speciali`() = runTest {
        db.workoutSessionDao().insert(
            WorkoutSessionEntity(
                dayTitle = "Percorso C:\\nuovo \"ciclo\"",
                dateEpochDay = 20000, status = SessionStatus.COMPLETED
            )
        )
        val json = export.exportAllJson()

        // Un JSON valido non deve contenere un backslash non escapato.
        val indice = json.indexOf("Percorso C:")
        assertTrue("Il titolo deve essere presente", indice >= 0)
        val frammento = json.substring(indice, minOf(json.length, indice + 40))
        assertFalse(
            "Il backslash deve essere escapato, altrimenti il JSON non e' parsabile: $frammento",
            frammento.contains("C:\\n")
        )
    }

    @Test
    fun `il JSON di export completo non contiene valori non finiti`() = runTest {
        runCatching {
            db.progressDao().insertWeight(
                WeightRecordEntity(dateEpochDay = 20000, weightKg = Double.NaN)
            )
        }
        val json = export.exportAllJson()
        assertFalse("NaN non e' un valore JSON valido: $json", json.contains("NaN"))
        assertFalse("Infinity non e' un valore JSON valido: $json", json.contains("Infinity"))
    }

    @Test
    fun `le circonferenze con caratteri speciali restano su una riga`() = runTest {
        db.progressDao().insertCircumference(
            CircumferenceRecordEntity(dateEpochDay = 20000, site = "vita, addome", valueCm = 82.5)
        )
        val csv = export.exportCircumferencesCsv()
        assertRettangolare(csv, 3, "circonferenze")
        assertTrue("La sede va quotata: $csv", csv.contains("\"vita, addome\""))
    }

    @Test
    fun `i campi mancanti restano vuoti e non diventano la stringa null`() = runTest {
        db.checkInDao().insert(
            CheckInEntity(weekStartEpochDay = 20000)
        )
        val csv = export.exportCheckInsCsv()
        assertRettangolare(csv, 9, "check-in")
        assertFalse("Un campo assente deve restare vuoto, non 'null': $csv", csv.contains("null"))
    }
}
