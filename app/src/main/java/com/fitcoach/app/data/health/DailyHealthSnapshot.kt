package com.fitcoach.app.data.health

/**
 * Misure di una giornata, con la provenienza dichiarata.
 *
 * Ogni valore e' null quando la sorgente non lo fornisce: un campo assente non
 * viene mai riempito con uno zero, perche zero passi e "nessun dato sui passi"
 * sono cose diverse e l'utente deve poterle distinguere.
 */
data class DailyHealthSnapshot(
    val date: String,
    val steps: Int? = null,
    val activeKcal: Double? = null,
    val sleepMinutes: Int? = null,
    val restingHeartRate: Int? = null,
    val hrvMs: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    /** Sorgente effettiva del dato: "Health Connect", "Bilancia", ... */
    val source: String,
    /** Perche alcuni campi mancano, in chiaro per l'utente. */
    val notes: List<String> = emptyList()
) {
    val hasAnything: Boolean
        get() = steps != null || activeKcal != null || sleepMinutes != null ||
            restingHeartRate != null || hrvMs != null || weightKg != null ||
            bodyFatPercent != null
}
