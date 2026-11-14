package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.dao.CheckInDao
import com.fitcoach.app.data.local.dao.ProgressDao
import com.fitcoach.app.data.local.dao.WorkoutSessionDao
import com.fitcoach.app.data.local.dao.WorkoutSetDao
import com.fitcoach.app.data.local.dao.ExercisePerformanceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            rows = rows.map { listOf(it.dateEpochDay.toString(), it.weightKg.toString(), it.bodyFatPercent?.toString() ?: "") }
        )
    }

    suspend fun exportCircumferencesCsv(): String = withContext(Dispatchers.IO) {
        val rows = progressDao.getCircumferences()
        buildCsv(
            header = listOf("data_epoch_day", "sito", "valore_cm"),
            rows = rows.map { listOf(it.dateEpochDay.toString(), it.site, it.valueCm.toString()) }
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
                    it.durationMinutes?.toString() ?: "", it.perceivedEffort?.toString() ?: "",
                    it.sleepQuality?.toString() ?: "", it.painReported.toString(), "%.1f".format(volume)
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
                    it.exerciseName, it.dateEpochDay.toString(), it.topWeightKg.toString(),
                    it.topReps.toString(), it.totalSets.toString(), it.totalReps.toString(),
                    "%.1f".format(it.volumeKg), it.avgRir?.toString() ?: "", it.estimated1Rm?.toString() ?: ""
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
                    it.weekStartEpochDay.toString(), it.avgWeightKg?.toString() ?: "",
                    it.weightTrendKgPerWeek?.toString() ?: "", it.avgCalories?.toString() ?: "",
                    it.dietAdherencePercent?.toString() ?: "",
                    it.trainingSessionsCompleted.toString(), it.trainingSessionsPlanned.toString(),
                    it.performanceTrend, it.sleepQualityAvg?.toString() ?: ""
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
                append("    {\"epochDay\": ${w.dateEpochDay}, \"weightKg\": ${w.weightKg}, \"bodyFatPercent\": ${w.bodyFatPercent ?: "null"}}")
                if (i < weights.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")
            append("  \"sessions\": [\n")
            sessions.forEachIndexed { i, s ->
                append("    {\"id\": ${s.id}, \"epochDay\": ${s.dateEpochDay}, \"title\": \"${escape(s.dayTitle)}\", \"status\": \"${s.status.name}\"}")
                if (i < sessions.size - 1) append(",")
                append("\n")
            }
            append("  ],\n")
            append("  \"checkIns\": [\n")
            checkIns.forEachIndexed { i, c ->
                append("    {\"weekStartEpochDay\": ${c.weekStartEpochDay}, \"avgWeightKg\": ${c.avgWeightKg ?: "null"}, \"trendKgPerWeek\": ${c.weightTrendKgPerWeek ?: "null"}}")
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

    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
}
