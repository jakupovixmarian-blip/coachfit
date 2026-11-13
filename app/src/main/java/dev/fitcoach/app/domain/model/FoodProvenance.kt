package dev.fitcoach.app.domain.model

import kotlin.math.roundToInt

/**
 * How much a stored value can be trusted.
 *
 * The status belongs to the record, not to the check that produced it: an
 * imported food stays imported even when the validator finds no issue, and a
 * value typed by hand stays manual even when it happens to be correct.
 */
enum class VerificationStatus(val label: String) {
    VERIFIED("Verificato su fonte ufficiale"),
    IMPORTED("Importato da fonte esterna"),
    MANUAL("Inserito a mano"),
    ESTIMATED("Stimato")
}

/**
 * Where a food record comes from.
 *
 * Every field is recorded at import time and never recomputed from the name:
 * two foods that look similar are not the same record, and a value without its
 * source cannot be checked later.
 */
data class FoodProvenance(
    /** Identifier used by the source, so the same food can be refreshed, not duplicated. */
    val sourceId: String = "",
    /** When this app stored the record (epoch millis). */
    val acquiredAt: Long = 0L,
    /** When the source last changed the record, when the source says so. */
    val sourceUpdatedAt: Long? = null,
    /** 0.0 to 1.0, from [dev.fitcoach.app.domain.validation.FoodValidator.reliabilityOf]. */
    val reliability: Double = 0.0,
    val verification: VerificationStatus = VerificationStatus.ESTIMATED,
    /** What the record does not say, in plain words, instead of a guessed value. */
    val limitations: String = ""
) {
    val hasSource: Boolean get() = sourceId.isNotBlank()

    val reliabilityPercent: Int get() = (reliability.coerceIn(0.0, 1.0) * 100).roundToInt()

    /** One line a screen or the coach can show without interpreting anything. */
    val summary: String
        get() = buildString {
            append(verification.label)
            if (hasSource) append(" · id ").append(sourceId)
            append(" · affidabilità ").append(reliabilityPercent).append('%')
            if (limitations.isNotBlank()) append(" · ").append(limitations)
        }

    companion object {
        val NONE = FoodProvenance()
    }
}
