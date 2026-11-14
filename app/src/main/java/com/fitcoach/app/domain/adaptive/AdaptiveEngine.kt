package com.fitcoach.app.domain.adaptive

import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.CheckIn
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.ProgramChange
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Motore di analisi deterministico.
 *
 * Legge i dati reali dell'utente (peso, aderenza, performance) e produce
 * PROPOSTE di modifica strutturate e motivate. Non applica nulla
 * automaticamente: le modifiche importanti richiedono approvazione.
 *
 * Ogni proposta contiene: situazione attuale, problema, dati usati, modifica,
 * motivazione, risultato atteso, controindicazioni.
 */
object AdaptiveEngine {

    /** Aderenza minima accettabile per valutare il piano (%). */
    const val MIN_ADHERENCE_FOR_CHANGE = 75.0

    /** Variazione calorica massima per singola modifica (frazione). */
    const val MAX_KCAL_STEP = 0.10

    /** Fabbisogno minimo di settimane per una modifica nutrizionale. */
    const val MIN_WEEKS_BEFORE_CHANGE = 2

    data class NutritionContext(
        val profile: UserProfile,
        val currentWeeklyKcal: Double,
        val currentDailyKcal: Double,
        val weightTrendKgPerWeek: Double?,
        val weeksOfData: Int,
        val avgAdherencePercent: Double?,
        val avgCaloriesConsumed: Double?
    )

    /**
     * Valuta se il piano nutrizionale necessita di modifiche.
     */
    fun evaluateNutrition(ctx: NutritionContext): List<ProgramChange> {
        val changes = mutableListOf<ProgramChange>()
        val target = targetRate(ctx.profile.goal)
        val trend = ctx.weightTrendKgPerWeek

        // 1) Dati insufficienti: nessuna modifica.
        if (trend == null || ctx.weeksOfData < MIN_WEEKS_BEFORE_CHANGE) {
            return emptyList()
        }

        // 2) Aderenza troppo bassa: prima di modificare il piano va verificata l'aderenza.
        if (ctx.avgAdherencePercent != null && ctx.avgAdherencePercent < MIN_ADHERENCE_FOR_CHANGE) {
            changes += buildChange(
                domain = ChangeDomain.NUTRITION,
                title = "Aderenza al piano insufficiente",
                situation = "Aderenza media ${ctx.avgAdherencePercent.roundToInt()}% nelle ultime settimane.",
                problem = "L'aderenza e sotto la soglia del ${MIN_ADHERENCE_FOR_CHANGE.roundToInt()}%: " +
                    "i dati sul peso non riflettono il piano.",
                dataUsed = "Aderenza ${ctx.avgAdherencePercent.roundToInt()}%, " +
                    "media kcal consumate ${ctx.avgCaloriesConsumed?.roundToInt() ?: "n/d"} kcal.",
                proposedChange = "Nessuna modifica calorica. Proposta di semplificare il piano e " +
                    "migliorare la logistica dei pasti.",
                rationale = "Modificare le calorie con aderenza bassa renderebbe i dati inaffidabili.",
                expectedOutcome = "Aderenza >= ${MIN_ADHERENCE_FOR_CHANGE.roundToInt()}% per 2 settimane.",
                contraindications = "Nessuna modifica strutturale applicata."
            )
            return changes
        }

        val diff = trend - target

        // 3) Stagnazione in cut/recomp: riduci le calorie (max -10%).
        if ((ctx.profile.goal == Goal.CUT || ctx.profile.goal == Goal.RECOMP) &&
            abs(trend) < 0.1 && target < -0.1
        ) {
            val step = -ctx.currentDailyKcal * 0.07
            val newWeekly = ctx.currentWeeklyKcal + step * 7
            changes += buildChange(
                domain = ChangeDomain.NUTRITION,
                title = "Peso stabile: riduzione calorica",
                situation = "Peso medio stabile (${fmt(trend)} kg/sett) da ${ctx.weeksOfData} settimane.",
                problem = "Obiettivo ${ctx.profile.goal.label}: atteso ${fmt(target)} kg/sett, riscontrato stagnazione.",
                dataUsed = "Trend ${fmt(trend)} kg/sett su ${ctx.weeksOfData} settimane, " +
                    "media kcal ${ctx.avgCaloriesConsumed?.roundToInt() ?: "n/d"}.",
                proposedChange = "Ridurre il target di circa ${abs(step).roundToInt()} kcal/giorno " +
                    "(${step.roundToInt()} kcal/giorno, circa il 7%).",
                rationale = "Un deficit leggermente maggiore riattiva la perdita di peso mantenendo " +
                    "le proteine elevate e un deficit non estremo.",
                expectedOutcome = "Perdita di circa ${fmt(target)} kg/sett nelle prossime 2 settimane.",
                contraindications = "Non applicare se il target scende sotto la soglia di sicurezza " +
                    "(BMR x 0.85). Non superare il 10% di variazione per volta."
            )
        }

        // 4) Perdita troppo rapida: aumenta le calorie.
        if ((ctx.profile.goal == Goal.CUT || ctx.profile.goal == Goal.RECOMP) && trend < target - 0.2) {
            val step = ctx.currentDailyKcal * 0.06
            changes += buildChange(
                domain = ChangeDomain.NUTRITION,
                title = "Perdita di peso troppo rapida",
                situation = "Perdita di ${fmt(trend)} kg/sett, superiore al previsto (${fmt(target)}).",
                problem = "Rischio di perdita di massa magra e calo delle prestazioni.",
                dataUsed = "Trend ${fmt(trend)} kg/sett su ${ctx.weeksOfData} settimane.",
                proposedChange = "Aumentare il target di circa ${step.roundToInt()} kcal/giorno (+6%).",
                rationale = "Rallentare la perdita protegge la massa muscolare e le performance.",
                expectedOutcome = "Perdita piu graduale, vicina a ${fmt(target)} kg/sett.",
                contraindications = "Verifica che l'aumento non annulli il deficit."
            )
        }

        // 5) Ipertrofia: peso non sale.
        if (ctx.profile.goal == Goal.HYPERTROPHY && trend < 0.1) {
            val step = ctx.currentDailyKcal * 0.08
            changes += buildChange(
                domain = ChangeDomain.NUTRITION,
                title = "Surplus insufficiente per l'ipertrofia",
                situation = "Variazione peso ${fmt(trend)} kg/sett, inferiore all'obiettivo di crescita.",
                problem = "Senza un surplus adeguato la crescita muscolare e rallentata.",
                dataUsed = "Trend ${fmt(trend)} kg/sett su ${ctx.weeksOfData} settimane.",
                proposedChange = "Aumentare il target di circa ${step.roundToInt()} kcal/giorno (+8%).",
                rationale = "Un surplus controllato (0.25-0.5% del peso a settimana) supporta la sintesi proteica.",
                expectedOutcome = "Crescita di circa ${fmt(target)} kg/sett.",
                contraindications = "Se l'aumento di peso e troppo rapido aumenta il grasso: monitora le circonferenze."
            )
        }

        // 6) Surplus eccessivo in ipertrofia.
        if (ctx.profile.goal == Goal.HYPERTROPHY && trend > target + 0.25) {
            val step = -ctx.currentDailyKcal * 0.06
            changes += buildChange(
                domain = ChangeDomain.NUTRITION,
                title = "Aumento di peso troppo rapido",
                situation = "Aumento di ${fmt(trend)} kg/sett.",
                problem = "Surplus eccessivo: probabile accumulo di grasso.",
                dataUsed = "Trend ${fmt(trend)} kg/sett su ${ctx.weeksOfData} settimane.",
                proposedChange = "Ridurre il target di circa ${abs(step).roundToInt()} kcal/giorno (-6%).",
                rationale = "Ridurre il surplus mantiene il rapporto massa magra/grassa favorevole.",
                expectedOutcome = "Crescita piu pulita, vicina a ${fmt(target)} kg/sett.",
                contraindications = "Non scendere sotto il TDEE di mantenimento."
            )
        }

        return changes
    }

    /** Valuta se serve un deload in base all'affaticamento. */
    fun evaluateTraining(
        profile: UserProfile,
        recoverySeverity: Int,
        painSessions: Int,
        weeksInMesocycle: Int
    ): List<ProgramChange> {
        val changes = mutableListOf<ProgramChange>()
        if (recoverySeverity >= 3 || painSessions >= 2) {
            changes += buildChange(
                domain = ChangeDomain.WORKOUT,
                title = "Deload consigliato",
                situation = "Segnali di affaticamento accumulato " +
                    "(severita recupero $recoverySeverity/3, dolore in $painSessions sedute).",
                problem = "Recupero insufficiente o dolore ricorrente.",
                dataUsed = "Sonno/RPE recenti, dolore segnalato.",
                proposedChange = "Inserire una settimana di deload: volume -40%, intensita -10%.",
                rationale = "Il deload consente il recupero e previene infortuni, senza perdere forza.",
                expectedOutcome = "Ripresa delle prestazioni dopo la settimana di scarico.",
                contraindications = "Se il dolore persiste, valuta una consulenza medica."
            )
        } else if (weeksInMesocycle >= 6) {
            changes += buildChange(
                domain = ChangeDomain.WORKOUT,
                title = "Rivalutazione fine mesociclo",
                situation = "Sei arrivato a $weeksInMesocycle settimane del mesociclo.",
                problem = "Fine del blocco di accumulo: e il momento di rivalutare.",
                dataUsed = "Settimane di mesociclo, andamento carichi.",
                proposedChange = "Concludere il mesociclo con una settimana di deload e ricalibrare i carichi.",
                rationale = "La periodizzazione a blocchi prevede una rivalutazione periodica.",
                expectedOutcome = "Nuovo mesociclo con carichi aggiornati.",
                contraindications = "Nessuna, se le prestazioni sono in crescita puoi estendere il blocco di 1 settimana."
            )
        }
        return changes
    }

    /**
     * Analizza l'andamento REALE delle prestazioni per esercizio e propone
     * interventi mirati. Non interpreta ogni stagnazione come necessita di
     * aumentare il volume: distingue regressione, stagnazione e under-stimulus.
     */
    fun evaluateExerciseTrends(
        performancesByExercise: Map<String, List<ExercisePerformance>>
    ): List<ProgramChange> {
        val changes = mutableListOf<ProgramChange>()
        performancesByExercise.forEach { (exerciseName, perfs) ->
            if (perfs.size < 6) return@forEach
            val ordered = perfs.sortedByDescending { it.dateEpochDay }
            val recent = ordered.take(3).map { it.estimated1Rm ?: 0.0 }.average()
            val previous = ordered.drop(3).take(3).map { it.estimated1Rm ?: 0.0 }.average()
            if (previous <= 0.0) return@forEach
            val delta = (recent - previous) / previous
            when {
                delta <= -0.05 -> changes += buildChange(
                    domain = ChangeDomain.WORKOUT,
                    title = "Calo di prestazione: $exerciseName",
                    situation = "1RM stimato in calo del ${"%.1f".format(-delta * 100)}% nelle ultime 3 sedute.",
                    problem = "Regressione su un esercizio chiave: possibile affaticamento accumulato.",
                    dataUsed = "Storico 1RM stimato di $exerciseName (${perfs.size} sedute).",
                    proposedChange = "Mantieni il carico e riduci il volume del 20-30% per 1-2 settimane " +
                        "su questo esercizio.",
                    rationale = "Il calo di prestazione spesso indica affaticamento, non necessita di piu volume.",
                    expectedOutcome = "Ripresa dei carichi precedenti dopo il recupero.",
                    contraindications = "Se il calo persiste nonostante il recupero, verifica alimentazione e sonno."
                )
                delta >= 0.05 -> changes += buildChange(
                    domain = ChangeDomain.WORKOUT,
                    title = "Progressione solida: $exerciseName",
                    situation = "1RM stimato in crescita del ${"%.1f".format(delta * 100)}%.",
                    problem = "Nessun problema: la progressione funziona.",
                    dataUsed = "Storico 1RM stimato di $exerciseName.",
                    proposedChange = "Prosegui con la double progression senza cambiare l'esercizio.",
                    rationale = "Se il progresso e costante, modificare lo stimolo e controproducente.",
                    expectedOutcome = "Ulteriore crescita nelle prossime settimane.",
                    contraindications = "Nessuna."
                )
            }
        }
        return changes
    }

    private fun targetRate(goal: Goal): Double = when (goal) {
        Goal.CUT -> -0.5
        Goal.RECOMP -> -0.25
        Goal.HYPERTROPHY -> 0.3
        Goal.MAINTENANCE -> 0.0
        Goal.STRENGTH -> 0.15
    }

    private fun buildChange(
        domain: ChangeDomain,
        title: String,
        situation: String,
        problem: String,
        dataUsed: String,
        proposedChange: String,
        rationale: String,
        expectedOutcome: String,
        contraindications: String
    ) = ProgramChange(
        domain = domain,
        status = ChangeStatus.PROPOSED,
        title = title,
        situation = situation,
        problem = problem,
        dataUsed = dataUsed,
        proposedChange = proposedChange,
        rationale = rationale,
        expectedOutcome = expectedOutcome,
        contraindications = contraindications,
        source = "ADAPTIVE_ENGINE"
    )

    private fun fmt(v: Double): String = "%+.2f".format(v)

    /** Costruisce un CheckIn aggregando i dati settimanali. */
    fun buildCheckIn(
        profile: UserProfile,
        weekStartEpochDay: Long,
        avgWeight: Double?,
        trend: Double?,
        avgCalories: Double?,
        adherence: Double?,
        sessionsCompleted: Int,
        sessionsPlanned: Int,
        performanceTrend: String,
        sleepAvg: Double?,
        painNotes: String
    ): CheckIn {
        val summary = buildString {
            append("Peso medio: ${avgWeight?.let { "%.1f".format(it) } ?: "n/d"} kg. ")
            append("Trend: ${trend?.let { "%+.2f".format(it) } ?: "n/d"} kg/sett. ")
            append("Aderenza: ${adherence?.roundToInt() ?: "n/d"}%. ")
            append("Allenamenti: $sessionsCompleted/$sessionsPlanned. ")
            if (performanceTrend.isNotBlank()) append(performanceTrend)
        }
        return CheckIn(
            profileId = profile.id,
            weekStartEpochDay = weekStartEpochDay,
            avgWeightKg = avgWeight,
            weightTrendKgPerWeek = trend,
            avgCalories = avgCalories,
            dietAdherencePercent = adherence,
            trainingSessionsCompleted = sessionsCompleted,
            trainingSessionsPlanned = sessionsPlanned,
            performanceTrend = performanceTrend,
            sleepQualityAvg = sleepAvg,
            painNotes = painNotes,
            summary = summary
        )
    }
}
