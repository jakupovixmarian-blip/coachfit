package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.dao.CheckInDao
import com.fitcoach.app.data.local.dao.ProgressDao
import com.fitcoach.app.data.local.dao.WorkoutSessionDao
import com.fitcoach.app.data.local.dao.WorkoutSetDao
import com.fitcoach.app.data.local.dao.ExercisePerformanceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Esportazione dei dati dell'utente in formato CSV.
 * I file sono pensati per l'archiviazione locale o la condivisione volontaria.
 */
@Singleton
class ExportRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val checkInDao: CheckInDao,
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
    private val performanceDao: ExercisePerformanceDao
) {

    suspend fun exportWeightsCsv(): String = withContext(Dispatchers.IO) {
        val rows = progressDao.getWeights()
        buildCsv(
            header = listOf("data_epoch_day", "peso_kg", "massa_grassa_percent"),
            rows = rows.map { listOf(it.dateEpochDay.toString(), numOrEmpty(it.weightKg), numOrEmpty(it.bodyFatPercent)) }
        )
    }

    suspend fun exportCircumferencesCsv(): String = withContext(Dispatchers.IO) {
        val rows = progressDao.getCircumferences()
        buildCsv(
            header = listOf("data_epoch_day", "sito", "valore_cm"),
            rows = rows.map { listOf(it.dateEpochDay.toString(), it.site, numOrEmpty(it.valueCm)) }
        )
    }

    suspend fun exportSessionsCsv(): String = withContext(Dispatchers.IO) {
        val sessions = sessionDao.getAll()
        buildCsv(
            header = listOf(
                "id", "data_epoch_day", "titolo", "stato", "durata_min", "rpe", "sonno", "dolore", "volume_kg"
            ),
            rows = sessions.map {
                val sets = setDao.getForSession(it.id).filter { s -> s.completed && !s.isWarmup }
                val volume = sets.sumOf { s -> s.weightKg * s.actualReps }
                listOf(
                    it.id.toString(), it.dateEpochDay.toString(), it.dayTitle, it.status.name,
                    it.durationMinutes?.toString() ?: "", numOrEmpty(it.perceivedEffort),
                    it.sleepQuality?.toString() ?: "", it.painReported.toString(), num(volume)
                )
            }
        )
    }

    suspend fun exportPerformanceCsv(): String = withContext(Dispatchers.IO) {
        val rows = performanceDao.getAll()
        buildCsv(
            header = listOf("esercizio", "data_epoch_day", "top_peso_kg", "top_reps", "serie", "reps_totali", "volume_kg", "rir_medio", "1rm_stimato"),
            rows = rows.map {
                listOf(
                    it.exerciseName, it.dateEpochDay.toString(), numOrEmpty(it.topWeightKg),
                    it.topReps.toString(), it.totalSets.toString(), it.totalReps.toString(),
                    num(it.volumeKg), numOrEmpty(it.avgRir), numOrEmpty(it.estimated1Rm)
                )
            }
        )
    }

    suspend fun exportCheckInsCsv(): String = withContext(Dispatchers.IO) {
        val rows = checkInDao.getAll()
        buildCsv(
            header = listOf("settimana_epoch_day", "peso_medio_kg", "trend_kg_settimana", "calorie_medie", "aderenza_percent", "sedute_fatte", "sedute_previste", "performance", "sonno_medio"),
            rows = rows.map {
                listOf(
                    it.weekStartEpochDay.toString(), numOrEmpty(it.avgWeightKg),
                    numOrEmpty(it.weightTrendKgPerWeek), numOrEmpty(it.avgCalories),
                    numOrEmpty(it.dietAdherencePercent),
                    it.trainingSessionsCompleted.toString(), it.trainingSessionsPlanned.toString(),
                    it.performanceTrend, numOrEmpty(it.sleepQualityAvg)
                )
            }
        )
    }

    /** Esporta tutto in un unico documento JSON. */
    suspend fun exportAllJson(): String = withContext(Dispatchers.IO) {
        val weights = progressDao.getWeights()
        val sessions = sessionDao.getAll()
        val checkIns = checkInDao.getAll()
        buildString {
            append("{\n")
            append("  \"app\": \"FitCoach\",\n")
            append("  \"generatedAt\": ${System.currentTimeMillis()},\n")
            append("  \"weights\": [\n")
            weights.forEachIndexed { i, w ->
                append("    {\"epochDay\": ${w.dateEpochDay}, \"weightKg\": ${jsonNum(w.weightKg)}, \"bodyFatPercent\": ${jsonNum(w.bodyFatPercent)}}")
                if (i < weights.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")
            append("  \"sessions\": [\n")
            sessions.forEachIndexed { i, s ->
                append("    {\"id\": ${s.id}, \"epochDay\": ${s.dateEpochDay}, \"title\": \"${jsonEscape(s.dayTitle)}\", \"status\": \"${s.status.name}\"}")
                if (i < sessions.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")
            append("  \"checkIns\": [\n")
            checkIns.forEachIndexed { i, c ->
                append("    {\"weekStartEpochDay\": ${c.weekStartEpochDay}, \"avgWeightKg\": ${jsonNum(c.avgWeightKg)}, \"trendKgPerWeek\": ${jsonNum(c.weightTrendKgPerWeek)}}")
                if (i < checkIns.size - 1) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}\n")
        }
    }

    private fun buildCsv(header: List<String>, rows: List<List<String>>): String = buildString {
        append(header.joinToString(","))
        append("\n")
        rows.forEach { row ->
            append(row.joinToString(",") { escape(it) })
            append("\n")
        }
    }

    /**
     * Formattazione numerica indipendente dalla locale. Con `"%.1f".format()`
     * su un telefono italiano il volume diventava "1218,0" e, non essendo
     * quotato, spezzava la riga in due campi: il foglio di calcolo leggeva i
     * dati sfalsati di una colonna.
     */
    private fun num(value: Double): String =
        if (value.isFinite()) String.format(Locale.ROOT, "%.1f", value) else ""

    /**
     * SQLite accetta Infinity ma non NaN. Senza questo filtro un peso infinito
     * (da un import o da un calcolo guasto) finirebbe nel CSV come testo
     * "Infinity" in una colonna numerica, e il foglio di calcolo sommerebbe
     * zero righe senza avvisare.
     */
    private fun numOrEmpty(value: Double?): String =
        if (value != null && value.isFinite()) value.toString() else ""

    private fun escape(value: String): String {
        val protetto = defuseFormula(value)
        return if (protetto.contains(',') || protetto.contains('"') ||
            protetto.contains('\n') || protetto.contains('\r')
        ) {
            "\"" + protetto.replace("\"", "\"\"") + "\""
        } else protetto
    }

    /**
     * Un testo che inizia con "=", "+", "-" o "@" viene eseguito come formula
     * da Excel e LibreOffice: aprendo l'export partirebbe un comando invece di
     * mostrare il titolo dell'allenamento. Il prefisso apostrofo obbliga il
     * foglio di calcolo a trattare la cella come testo.
     *
     * I numeri veri non vengono toccati, altrimenti "-5.0" diventerebbe testo
     * e le colonne numeriche non sarebbero piu' sommarie.
     */
    private fun defuseFormula(value: String): String {
        if (value.isEmpty()) return value
        val primo = value[0]
        if (primo != '=' && primo != '+' && primo != '-' && primo != '@') return value
        if (value.toDoubleOrNull() != null) return value
        return "'" + value
    }

    /**
     * Escaping JSON, diverso da quello CSV: qui il backslash e le virgolette
     * singole di controllo vanno preceduti da backslash, mentre raddoppiare le
     * virgolette (regola CSV) produce un documento non parsabile.
     */
    /** In JSON "NaN" e "Infinity" non esistono: un valore assente e' null. */
    private fun jsonNum(value: Double?): String =
        if (value != null && value.isFinite()) value.toString() else "null"

    private fun jsonEscape(value: String): String = buildString {
        value.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }
}
