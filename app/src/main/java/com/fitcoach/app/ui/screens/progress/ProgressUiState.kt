package com.fitcoach.app.ui.screens.progress

/**
 * Stato di presentazione di Progressi.
 *
 * REGOLA DI PRODOTTO: qui non si inventa nulla. Ogni serie mostrata proviene da
 * ProgressRecord / WeightRecord / CircumferenceRecord / ExercisePerformance /
 * WorkoutSession già presenti nel database. Se non ci sono record, la sezione
 * espone `hasData = false` e la UI mostra lo stato vuoto invece di un grafico
 * finto. I confronti tra periodi sono calcolati dal motore di analisi (domain/).
 *
 * Mapping entità -> campo:
 *   weightSeries      <- WeightRecord (media settimanale, non il singolo giorno)
 *   composition       <- ProgressRecord.fatMassKg / leanMassKg
 *   circumferences    <- CircumferenceRecord
 *   strength          <- ExercisePerformance (miglior serie per esercizio)
 *   volume            <- WorkoutSession + WorkoutSet (volume settimanale)
 *   checkIn           <- CheckIn (peso medio, aderenza, performance, sonno)
 *   proposals         <- ProgramChange con stato PROPOSED
 */
data class ProgressUiState(
    val range: RangeFilter = RangeFilter.MONTH,
    val weightSeries: List<MetricPoint> = emptyList(),
    val weightUnit: String = "kg",
    val weightDeltaLabel: String? = null,
    val weightTrendDown: Boolean? = null,
    val composition: CompositionUi? = null,
    val circumferences: List<CircumferenceUi> = emptyList(),
    val strength: List<StrengthUi> = emptyList(),
    val volumeSeries: List<MetricPoint> = emptyList(),
    val volumeTotalLabel: String? = null,
    val adherencePercent: Int? = null,
    val checkIn: CheckInUi? = null,
    val proposals: List<ProposalUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasWeight: Boolean get() = weightSeries.size >= 2
    val hasComposition: Boolean get() = composition != null
    val hasCircumferences: Boolean get() = circumferences.isNotEmpty()
    val hasStrength: Boolean get() = strength.isNotEmpty()
    val hasVolume: Boolean get() = volumeSeries.isNotEmpty()
    val hasAnything: Boolean
        get() = hasWeight || hasComposition || hasCircumferences || hasStrength || hasVolume
    val pendingProposals: List<ProposalUi> get() = proposals.filter { it.status == ProposalStatus.PROPOSED }
}

/** Filtro temporale dei grafici. */
enum class RangeFilter(val label: String, val days: Int) {
    WEEK("7g", 7),
    MONTH("30g", 30),
    QUARTER("90g", 90),
    YEAR("1a", 365),
}

/** Punto di una serie: etichetta sull'asse X e valore reale. */
data class MetricPoint(val label: String, val value: Float)

/** Massa grassa / massa magra con variazione nel periodo. */
data class CompositionUi(
    val fatMassKg: Double,
    val leanMassKg: Double,
    val fatPercent: Double,
    val fatDeltaKg: Double,
    val leanDeltaKg: Double,
)

/** Circonferenza corporea registrata. */
data class CircumferenceUi(
    val name: String,
    val valueCm: Double,
    val deltaCm: Double,
)

/** Progressione dei carichi su un esercizio. */
data class StrengthUi(
    val exerciseName: String,
    val topSetLabel: String,
    val deltaLabel: String?,
    val series: List<MetricPoint> = emptyList(),
)

/** Check-in settimanale con i valori registrati. */
data class CheckInUi(
    val weekLabel: String,
    val weightAvgLabel: String,
    val adherenceLabel: String,
    val performanceLabel: String,
    val sleepLabel: String?,
    val note: String? = null,
)

/**
 * Proposta di modifica del programma.
 * Deve contenere tutti i campi richiesti dal contratto: situazione, problema,
 * dati usati, modifica, motivazione, risultato atteso, controindicazioni.
 * Resta PROPOSED finché l'utente non approva o rifiuta (rollback lato dominio).
 */
data class ProposalUi(
    val id: Long,
    val title: String,
    val situation: String,
    val problem: String,
    val dataUsed: String,
    val change: String,
    val rationale: String,
    val expected: String,
    val cautions: String?,
    val status: ProposalStatus = ProposalStatus.PROPOSED,
    val isApplying: Boolean = false,
)

enum class ProposalStatus { PROPOSED, APPROVED, REJECTED }
