package com.fitcoach.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legge i dati del giorno da Health Connect.
 *
 * Health Connect non espone lo stress: Samsung lo misura ma non lo esporta.
 * Si leggono quindi i proxy fisiologici reali (FC a riposo e HRV) e lo si dice
 * all'utente invece di mostrare un campo vuoto.
 */
@Singleton
class HealthConnectReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    )

    /** 0 = non disponibile, 1 = aggiornamento richiesto, 2 = disponibile, 3 = non supportato. */
    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.UPDATE_REQUIRED
        else -> Availability.NOT_SUPPORTED
    }

    private fun client(): HealthConnectClient? =
        if (availability() == Availability.AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    suspend fun grantedPermissions(): Set<String> =
        client()?.permissionController?.getGrantedPermissions().orEmpty()

    /**
     * Snapshot del giorno [date]. Restituisce null solo se Health Connect non e'
     * utilizzabile: in quel caso il chiamante mostra lo stato, non dei numeri.
     */
    suspend fun read(date: LocalDate): DailyHealthSnapshot? {
        val c = client() ?: return null
        val granted = c.permissionController.getGrantedPermissions()
        if (granted.isEmpty()) {
            return DailyHealthSnapshot(
                date = date.toString(),
                source = "Health Connect",
                notes = listOf("Permessi non concessi: nessun dato disponibile.")
            )
        }
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = start.plus(Duration.ofDays(1))
        val filter = TimeRangeFilter.between(start, end)
        val notes = mutableListOf<String>()

        val steps = runCatching {
            if (HealthPermission.getReadPermission(StepsRecord::class) in granted) {
                c.aggregate(
                    AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter = filter)
                ).let { it[StepsRecord.COUNT_TOTAL]?.toInt() }
            } else null
        }.getOrElse { notes += "Passi non leggibili: ${it.message ?: "errore"}"; null }

        val activeKcal = runCatching {
            if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) {
                c.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = filter
                    )
                ).let { it[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories }
            } else null
        }.getOrElse { notes += "Calorie attive non leggibili."; null }

        val sleepMinutes = runCatching {
            if (HealthPermission.getReadPermission(SleepSessionRecord::class) in granted) {
                // Il sonno puo iniziare il giorno prima: si allarga la finestra.
                val sleepFilter = TimeRangeFilter.between(
                    date.minusDays(1).atStartOfDay(zone).toInstant(),
                    end
                )
                val sessions = c.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = sleepFilter
                    )
                ).records
                val total = sessions.sumOf { s ->
                    Duration.between(s.startTime, s.endTime).toMinutes()
                }
                if (sessions.isEmpty()) null else total.toInt()
            } else null
        }.getOrElse { notes += "Sonno non leggibile."; null }

        val restingHr = runCatching {
            if (HealthPermission.getReadPermission(RestingHeartRateRecord::class) in granted) {
                val recs = c.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = filter
                    )
                ).records
                recs.maxByOrNull { it.time }?.beatsPerMinute?.toInt()
            } else null
        }.getOrElse { notes += "FC a riposo non leggibile."; null }

        val hrv = runCatching {
            if (HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class) in granted) {
                val recs = c.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateVariabilityRmssdRecord::class,
                        timeRangeFilter = filter
                    )
                ).records
                recs.maxByOrNull { it.time }?.heartRateVariabilityMillis
            } else null
        }.getOrElse { notes += "HRV non leggibile."; null }

        if (restingHr == null && hrv == null) {
            notes += "Lo stress non e' esportabile da Health Connect: " +
                "Samsung lo misura ma non lo condivide. FC a riposo e HRV sono i proxy disponibili."
        }

        return DailyHealthSnapshot(
            date = date.toString(),
            steps = steps,
            activeKcal = activeKcal,
            sleepMinutes = sleepMinutes,
            restingHeartRate = restingHr,
            hrvMs = hrv,
            source = "Health Connect",
            notes = notes
        )
    }

    enum class Availability { AVAILABLE, UPDATE_REQUIRED, NOT_SUPPORTED }

    /** Finestra leggibile per la UI. */
    fun todayStart(): Instant = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
}
