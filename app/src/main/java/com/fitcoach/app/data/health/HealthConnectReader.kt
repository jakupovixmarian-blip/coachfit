package com.fitcoach.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * Legge i dati del giorno da Health Connect.
 *
 * Health Connect non espone lo stress: Samsung lo misura ma non lo esporta.
 * Si leggono quindi i proxy fisiologici reali (FC a riposo e HRV) e lo si dice
 * all'utente invece di mostrare un campo vuoto.
 *
 * Deduplicazione: quando due app scrivono lo stesso dato (Samsung Health e
 * Google Fit insieme) sommare le fonti raddoppia il valore. Per i dati
 * cumulativi si aggrega per singola sorgente e si tiene la sorgente con il
 * valore piu' alto, non la somma: due app che registrano gli stessi passi
 * descrivono la stessa camminata.
 */
@Singleton
class HealthConnectReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(BodyWaterMassRecord::class),
        HealthPermission.getReadPermission(BoneMassRecord::class)
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
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = start.plus(Duration.ofDays(1))
        val filter = TimeRangeFilter.between(start, end)
        val notes = mutableListOf<String>()
        val states = mutableMapOf<String, HealthMetricState>()
        val origins = mutableSetOf<String>()

        val steps = runCatching {
            if (!granted.canRead(StepsRecord::class)) null else
                sumPerOrigin(c, filter, StepsRecord::class, origins) { it.count.toDouble() }?.toInt()
        }.getOrElse { notes += "Passi non leggibili: ${it.message ?: "errore"}"; null }

        val activeKcal = runCatching {
            if (!granted.canRead(ActiveCaloriesBurnedRecord::class)) null else
                sumPerOrigin(c, filter, ActiveCaloriesBurnedRecord::class, origins) {
                    it.energy.inKilocalories
                }
        }.getOrElse { notes += "Calorie attive non leggibili."; null }

        val totalKcal = runCatching {
            if (!granted.canRead(TotalCaloriesBurnedRecord::class)) null else
                sumPerOrigin(c, filter, TotalCaloriesBurnedRecord::class, origins) {
                    it.energy.inKilocalories
                }
        }.getOrElse { notes += "Calorie totali non leggibili."; null }

        val distanceKm = runCatching {
            if (!granted.canRead(DistanceRecord::class)) null else
                sumPerOrigin(c, filter, DistanceRecord::class, origins) {
                    it.distance.inMeters
                }?.let { it / 1000.0 }
        }.getOrElse { notes += "Distanza non leggibile."; null }

        val sessions = runCatching {
            if (!granted.canRead(ExerciseSessionRecord::class)) emptyList() else
                c.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = filter
                    )
                ).records
        }.getOrElse { notes += "Attivita' fisica non leggibile."; emptyList() }

        sessions.forEach { origins += it.metadata.dataOrigin.packageName }
        // Lo stesso allenamento puo' essere scritto da due app: si conta una volta.
        val distinctSessions = sessions.distinctBy { "${it.startTime}|${it.endTime}|${it.exerciseType}" }
        val exerciseMinutes = distinctSessions
            .takeIf { it.isNotEmpty() }
            ?.sumOf { Duration.between(it.startTime, it.endTime).toMinutes().toInt() }

        val sleepSessions = runCatching {
            if (!granted.canRead(SleepSessionRecord::class)) emptyList() else
                c.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(
                            date.minusDays(1).atStartOfDay(zone).toInstant(),
                            end
                        )
                    )
                ).records
        }.getOrElse { notes += "Sonno non leggibile."; emptyList() }

        sleepSessions.forEach { origins += it.metadata.dataOrigin.packageName }
        // Il sonno e' un intervallo: se due app scrivono la stessa notte, la
        // sessione piu' lunga e' quella reale, la somma conterebbe due volte.
        val mainSleep = sleepSessions.maxByOrNull {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }
        val sleepMinutes = mainSleep?.let {
            Duration.between(it.startTime, it.endTime).toMinutes().toInt()
        }
        val sleepStages = mainSleep?.stages
            ?.groupBy { stageLabel(it.stage) }
            ?.mapValues { (_, v) ->
                v.sumOf { Duration.between(it.startTime, it.endTime).toMinutes().toInt() }
            }
            .orEmpty()

        val hrSamples = runCatching {
            if (!granted.canRead(HeartRateRecord::class)) emptyList() else
                c.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = filter
                    )
                ).records.flatMap { it.samples }
        }.getOrElse { notes += "Frequenza cardiaca non leggibile."; emptyList() }

        val heartRateAvg = hrSamples.takeIf { it.isNotEmpty() }
            ?.map { it.beatsPerMinute }?.average()?.toInt()
        val heartRateMin = hrSamples.takeIf { it.isNotEmpty() }
            ?.minOf { it.beatsPerMinute }?.toInt()
        val heartRateMax = hrSamples.takeIf { it.isNotEmpty() }
            ?.maxOf { it.beatsPerMinute }?.toInt()

        val restingHr = runCatching {
            if (!granted.canRead(RestingHeartRateRecord::class)) null else
                c.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = filter
                    )
                ).records.maxByOrNull { it.time }?.beatsPerMinute?.toInt()
        }.getOrElse { notes += "FC a riposo non leggibile."; null }

        val hrv = runCatching {
            if (!granted.canRead(HeartRateVariabilityRmssdRecord::class)) null else
                c.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateVariabilityRmssdRecord::class,
                        timeRangeFilter = filter
                    )
                ).records.maxByOrNull { it.time }?.heartRateVariabilityMillis
        }.getOrElse { notes += "HRV non leggibile."; null }

        // Composizione corporea: si cerca indietro di 30 giorni perche' la
        // bilancia non scrive ogni giorno.
        val weight = latest<WeightRecord>(c, granted, date, origins, WeightRecord::class) { it.time }
        val bodyFat = latest<BodyFatRecord>(c, granted, date, origins, BodyFatRecord::class) { it.time }
        val leanMass = latest<LeanBodyMassRecord>(
            c, granted, date, origins, LeanBodyMassRecord::class
        ) { it.time }
        val bodyWater = latest<BodyWaterMassRecord>(
            c, granted, date, origins, BodyWaterMassRecord::class
        ) { it.time }
        val boneMass = latest<BoneMassRecord>(
            c, granted, date, origins, BoneMassRecord::class
        ) { it.time }

        fun mark(key: String, value: Any?) {
            states[key] = when {
                value != null -> HealthMetricState.AVAILABLE
                !granted.canReadKey(key) -> HealthMetricState.PERMISSION_MISSING
                else -> HealthMetricState.NO_DATA
            }
        }
        mark("steps", steps)
        mark("activeKcal", activeKcal)
        mark("totalKcal", totalKcal)
        mark("distance", distanceKm)
        mark("exercise", exerciseMinutes)
        mark("sleep", sleepMinutes)
        mark("heartRate", heartRateAvg)
        mark("restingHeartRate", restingHr)
        mark("hrv", hrv)
        mark("weight", weight)
        mark("bodyFat", bodyFat)
        mark("leanMass", leanMass)
        mark("bodyWater", bodyWater)
        mark("boneMass", boneMass)
        states["stress"] = HealthMetricState.NOT_SUPPORTED

        notes += coverageNotes(
            granted = granted,
            hasActivityData = steps != null || sleepMinutes != null || activeKcal != null,
            restingHeartRate = restingHr,
            heartRateVariability = hrv
        )

        val lastModified = listOfNotNull(
            weight?.metadata?.lastModifiedTime,
            bodyFat?.metadata?.lastModifiedTime,
            mainSleep?.metadata?.lastModifiedTime,
            sessions.maxByOrNull { it.metadata.lastModifiedTime }?.metadata?.lastModifiedTime
        ).maxOrNull()

        return DailyHealthSnapshot(
            date = date.toString(),
            steps = steps,
            activeKcal = activeKcal,
            totalKcal = totalKcal,
            distanceKm = distanceKm,
            exerciseMinutes = exerciseMinutes,
            exerciseSessions = distinctSessions.size,
            sleepMinutes = sleepMinutes,
            sleepStages = sleepStages,
            restingHeartRate = restingHr,
            heartRateAvg = heartRateAvg,
            heartRateMin = heartRateMin,
            heartRateMax = heartRateMax,
            hrvMs = hrv,
            weightKg = weight?.weight?.inKilograms,
            bodyFatPercent = bodyFat?.percentage?.value,
            leanMassKg = leanMass?.mass?.inKilograms,
            bodyWaterKg = bodyWater?.mass?.inKilograms,
            boneMassKg = boneMass?.mass?.inKilograms,
            source = "Health Connect",
            notes = notes,
            states = states,
            readAtMillis = System.currentTimeMillis(),
            dataUpdatedAtMillis = lastModified?.toEpochMilli(),
            dataOrigins = origins.toList().sorted()
        )
    }

    /**
     * Spiegazioni su cosa manca e perche'. Funzione pura perche' il caso piu'
     * importante ("permesso concesso ma nessun dato") dipende dal ritardo con cui
     * Samsung Health scrive in Health Connect: senza una spiegazione l'utente
     * conclude che l'app non funziona.
     */
    internal fun coverageNotes(
        granted: Set<String>,
        hasActivityData: Boolean,
        restingHeartRate: Int?,
        heartRateVariability: Double?
    ): List<String> = buildList {
        if (restingHeartRate == null && heartRateVariability == null) {
            add(
                "Lo stress non e' esportabile da Health Connect: " +
                    "Samsung lo misura ma non lo condivide. FC a riposo e HRV sono i proxy disponibili."
            )
        }
        if (granted.isEmpty()) {
            add("Permessi non concessi: nessun dato disponibile.")
        } else if (!hasActivityData) {
            add(
                "Nessun dato per oggi: Samsung Health puo' impiegare alcuni minuti " +
                    "a sincronizzare con Health Connect. Apri Samsung Health, verifica che la " +
                    "sincronizzazione sia attiva, poi tocca di nuovo Aggiorna report."
            )
        }
    }

    private fun Set<String>.canRead(type: KClass<out Record>): Boolean =
        HealthPermission.getReadPermission(type) in this

    private fun Set<String>.canReadKey(key: String): Boolean = when (key) {
        "steps" -> canRead(StepsRecord::class)
        "activeKcal" -> canRead(ActiveCaloriesBurnedRecord::class)
        "totalKcal" -> canRead(TotalCaloriesBurnedRecord::class)
        "distance" -> canRead(DistanceRecord::class)
        "exercise" -> canRead(ExerciseSessionRecord::class)
        "sleep" -> canRead(SleepSessionRecord::class)
        "heartRate" -> canRead(HeartRateRecord::class)
        "restingHeartRate" -> canRead(RestingHeartRateRecord::class)
        "hrv" -> canRead(HeartRateVariabilityRmssdRecord::class)
        "weight" -> canRead(WeightRecord::class)
        "bodyFat" -> canRead(BodyFatRecord::class)
        "leanMass" -> canRead(LeanBodyMassRecord::class)
        "bodyWater" -> canRead(BodyWaterMassRecord::class)
        "boneMass" -> canRead(BoneMassRecord::class)
        else -> false
    }

    /**
     * Somma i record raggruppandoli per sorgente e restituisce il totale della
     * sorgente piu' alta. Evita che due app che descrivono la stessa attivita'
     * facciano raddoppiare passi, distanza e calorie.
     */
    private suspend fun <T : Record> sumPerOrigin(
        client: HealthConnectClient,
        filter: TimeRangeFilter,
        type: KClass<T>,
        origins: MutableSet<String>,
        sum: (T) -> Double
    ): Double? {
        val recs = runCatching {
            client.readRecords(ReadRecordsRequest(recordType = type, timeRangeFilter = filter)).records
        }.getOrDefault(emptyList())
        if (recs.isEmpty()) return null
        val byOrigin = recs.groupBy { it.metadata.dataOrigin.packageName }
        origins += byOrigin.keys
        return byOrigin.values.map { list -> list.sumOf(sum) }.maxOrNull()
    }

    /** Ultimo record del tipo indicato, cercando indietro di 30 giorni. */
    private suspend fun <T : Record> latest(
        client: HealthConnectClient,
        granted: Set<String>,
        date: LocalDate,
        origins: MutableSet<String>,
        type: KClass<T>,
        timeOf: (T) -> Instant
    ): T? {
        if (!granted.canRead(type)) return null
        return runCatching {
            val zone = ZoneId.systemDefault()
            val from = date.minusDays(30).atStartOfDay(zone).toInstant()
            val to = date.plusDays(1).atStartOfDay(zone).toInstant()
            val recs = client.readRecords(
                ReadRecordsRequest(recordType = type, timeRangeFilter = TimeRangeFilter.between(from, to))
            ).records
            recs.forEach { origins += it.metadata.dataOrigin.packageName }
            recs.maxByOrNull(timeOf)
        }.getOrNull()
    }

    private fun stageLabel(stage: Int): String = when (stage) {
        SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> "Sveglio"
        SleepSessionRecord.STAGE_TYPE_LIGHT -> "Leggero"
        SleepSessionRecord.STAGE_TYPE_DEEP -> "Profondo"
        SleepSessionRecord.STAGE_TYPE_REM -> "REM"
        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "Fuori letto"
        else -> "Altro"
    }

    enum class Availability { AVAILABLE, UPDATE_REQUIRED, NOT_SUPPORTED }

    /** Finestra leggibile per la UI. */
    fun todayStart(): Instant = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
}
