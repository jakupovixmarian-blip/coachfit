package com.fitcoach.app.data.health

/**
 * Stato di un singolo dato letto da Health Connect.
 *
 * Un valore assente non viene mai sostituito da zero: "0 passi" e "nessun dato
 * sui passi" sono informazioni diverse e l'utente deve poterle distinguere.
 */
enum class HealthMetricState(val label: String) {
    /** Dato letto e disponibile. */
    AVAILABLE("Disponibile"),
    /** Permesso concesso ma nessun dato nel periodo: la sorgente non ha scritto nulla. */
    NO_DATA("Nessun dato nel periodo"),
    /** Permesso non concesso dall'utente. */
    PERMISSION_MISSING("Permesso non concesso"),
    /** La piattaforma o la sorgente non forniscono questo dato. */
    NOT_SUPPORTED("Non supportato"),
    /** Errore di lettura (provider, IO). */
    ERROR("Errore di lettura")
}

/**
 * Misure di una giornata, con la provenienza dichiarata per ciascun gruppo.
 *
 * Ogni valore e' null quando la sorgente non lo fornisce.
 */
data class DailyHealthSnapshot(
    val date: String,
    val steps: Int? = null,
    val activeKcal: Double? = null,
    val totalKcal: Double? = null,
    val distanceKm: Double? = null,
    val exerciseMinutes: Int? = null,
    val exerciseSessions: Int = 0,
    val sleepMinutes: Int? = null,
    val sleepStages: Map<String, Int> = emptyMap(),
    val restingHeartRate: Int? = null,
    val heartRateAvg: Int? = null,
    val heartRateMin: Int? = null,
    val heartRateMax: Int? = null,
    val hrvMs: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val leanMassKg: Double? = null,
    val bodyWaterKg: Double? = null,
    val boneMassKg: Double? = null,
    val source: String,
    /** Perche alcuni campi mancano, in chiaro per l'utente. */
    val notes: List<String> = emptyList(),
    /** Stato per singolo dato, cosi' la UI distingue assente da non autorizzato. */
    val states: Map<String, HealthMetricState> = emptyMap(),
    /** Momento dell'ultima lettura effettiva (epoch millis). */
    val readAtMillis: Long = 0L,
    /** Ultima modifica del dato piu' recente secondo Health Connect (epoch millis). */
    val dataUpdatedAtMillis: Long? = null,
    /** Pacchetti che hanno scritto i dati letti (es. Samsung Health). */
    val dataOrigins: List<String> = emptyList()
) {
    val hasAnything: Boolean
        get() = steps != null || activeKcal != null || sleepMinutes != null ||
            restingHeartRate != null || hrvMs != null || weightKg != null ||
            bodyFatPercent != null || distanceKm != null || exerciseMinutes != null ||
            heartRateAvg != null || totalKcal != null

    fun stateOf(metric: String): HealthMetricState = states[metric] ?: HealthMetricState.NO_DATA
}

/**
 * Un valore letto, con la sua provenienza.
 *
 * Serve a evitare il doppio conteggio: quando due app scrivono lo stesso dato
 * (Samsung Health e Google Fit insieme) la somma raddoppia. Si tiene la
 * sorgente con il dato piu' recente invece di sommare le fonti.
 */
data class SourcedValue<T>(
    val value: T,
    val origin: String,
    val lastModifiedMillis: Long
)
