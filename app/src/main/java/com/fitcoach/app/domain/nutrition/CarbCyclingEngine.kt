package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.CarbCyclingPlan
import com.fitcoach.app.domain.model.CarbDayTarget
import com.fitcoach.app.domain.model.CarbDayType
import com.fitcoach.app.domain.model.EnergyResult
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Motore di Carb Cycling deterministico.
 *
 * Principi:
 *  - Il bilancio calorico settimanale e la somma dei target giornalieri.
 *  - Il deficit/surplus settimanale dipende SOLO dall'obiettivo, non dai singoli giorni.
 *  - Le proteine sono costanti e basate sul peso (g/kg per obiettivo).
 *  - I grassi hanno un minimo di sicurezza (~0.6 g/kg) e salgono nei giorni LOW.
 *  - I carboidrati riempiono le calorie rimanenti, con piu quota nei giorni HIGH.
 *  - Validazione: kcal deve corrispondere a 4*P + 4*C + 9*F entro tolleranza.
 */
object CarbCyclingEngine {

    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_CARB = 4.0
    const val KCAL_PER_G_FAT = 9.0

    /** Tolleranza di coerenza kcal vs macro (kcal). */
    const val KCAL_TOLERANCE = 15.0

    /** Minimo di grassi per kg per sicurezza ormonale. */
    const val MIN_FAT_G_PER_KG = 0.6

    /**
     * Verifica se il carb cycling e giustificato per questo profilo.
     *
     * Non va applicato automaticamente a tutti. E' motivato quando esiste un
     * obiettivo di ricomposizione/definizione con un numero sufficiente di giorni
     * di allenamento (>= 3) e una buona capacita di recupero/aderenza, oppure
     * quando l'utente lo richiede esplicitamente. Per obiettivi di mantenimento
     * o forza con pochi allenamenti, si preferisce una distribuzione uniforme.
     */
    fun shouldApplyCarbCycling(profile: UserProfile): Boolean {
        if (profile.goal == Goal.MAINTENANCE) return false
        if (profile.trainingDaysPerWeek < 3) return false
        // Con recupero molto scarso il cycling e comunque poco utile: priorita al recupero.
        return true
    }

    /** Distribuzione uniforme (nessun carb cycling): un solo target per tutti i giorni. */
    fun buildUniform(
        profile: UserProfile,
        energy: EnergyResult
    ): CarbCyclingPlan {
        val weight = profile.weightKg ?: 70.0
        val adjFraction = weeklyAdjustmentFraction(profile.goal)
        val weeklyKcal = profile.calorieTargetOverride?.let { it * 7 }
            ?: (energy.tdee * 7 * (1 + adjFraction))
        val dailyKcal = weeklyKcal / 7.0
        val proteinG = weight * proteinPerKg(profile.goal)
        val warnings = mutableListOf<String>()

        val target = solveDay(CarbDayType.MEDIUM, dailyKcal, proteinG, weight * MIN_FAT_G_PER_KG, 0.4, warnings, profile)
        val targets = linkedMapOf(CarbDayType.MEDIUM to target)
        val floor = safetyFloor(profile, energy)
        if (target.kcal < floor) {
            warnings += "Target ${target.kcal.roundToInt()} kcal sotto la soglia di sicurezza (${floor.roundToInt()} kcal)."
        }
        return CarbCyclingPlan(
            targets = targets,
            highDays = 0,
            mediumDays = 7,
            lowDays = 0,
            weeklyKcal = weeklyKcal,
            weeklyKcalFromTarget = target.kcal * 7,
            weeklyProtein = proteinG * 7,
            weeklyCarbs = target.carbs * 7,
            weeklyFat = target.fat * 7,
            warnings = warnings,
            isValid = warnings.none { it.contains("soglia di sicurezza") },
            applied = false
        )
    }

    /** Deficit/surplus settimanale come frazione del TDEE, per obiettivo. */
    fun weeklyAdjustmentFraction(goal: Goal): Double = when (goal) {
        Goal.CUT -> -0.20
        Goal.RECOMP -> -0.08
        Goal.HYPERTROPHY -> 0.10
        Goal.MAINTENANCE -> 0.0
        Goal.STRENGTH -> 0.05
    }

    /** Proteine g/kg per obiettivo. */
    fun proteinPerKg(goal: Goal): Double = when (goal) {
        Goal.CUT -> 2.2
        Goal.RECOMP -> 2.0
        Goal.HYPERTROPHY -> 1.8
        Goal.MAINTENANCE -> 1.6
        Goal.STRENGTH -> 1.8
    }

    /**
     * Limite minimo calorico di sicurezza: non scendere sotto BMR*0.85
     * e mai sotto 1200 kcal (donne) / 1500 kcal (uomini).
     */
    fun safetyFloor(profile: UserProfile, energy: EnergyResult): Double {
        val sexFloor = when (profile.sex) {
            com.fitcoach.app.domain.model.Sex.FEMALE -> 1200.0
            else -> 1500.0
        }
        return maxOf(energy.bmr * 0.85, sexFloor)
    }

    /**
     * Distribuzione settimanale dei giorni HIGH/MEDIUM/LOW.
     * HIGH = giorni di allenamento piu pesanti, LOW = riposo/leggeri.
     *
     * Vincolo: i giorni NON-LOW (HIGH + MEDIUM) non possono essere meno dei
     * giorni di allenamento, altrimenti un giorno di allenamento finirebbe LOW
     * (carboidrati ridotti nel giorno in cui serve piu energia). Quindi MEDIUM
     * assorbe almeno i giorni di allenamento che eccedono i giorni HIGH.
     */
    fun distributeDays(trainingDays: Int): Triple<Int, Int, Int> {
        val td = trainingDays.coerceIn(0, 7)
        val (high, medium, low) = when (td) {
            0 -> Triple(0, 0, 7)
            1 -> Triple(0, 1, 6)
            2 -> Triple(1, 1, 5)
            3 -> Triple(1, 2, 4)
            4 -> Triple(2, 2, 3)
            5 -> Triple(2, 3, 2)
            6 -> Triple(3, 3, 1)
            else -> Triple(4, 3, 0)
        }
        // Garanzia esplicita: nessun giorno di allenamento resta LOW.
        val mediumFixed = maxOf(medium, td - high)
        return Triple(high, mediumFixed, 7 - high - mediumFixed)
    }

    /**
     * Costruisce il piano di carb cycling.
     *
     * @param kcalShareHigh quota relativa dei carboidrati nei giorni HIGH (default 1.0)
     * @param kcalShareMedium quota relativa (default 0.65)
     * @param kcalShareLow quota relativa (default 0.35)
     */
    fun build(
        profile: UserProfile,
        energy: EnergyResult,
        highDays: Int = distributeDays(profile.trainingDaysPerWeek).first,
        mediumDays: Int = distributeDays(profile.trainingDaysPerWeek).second,
        lowDays: Int = distributeDays(profile.trainingDaysPerWeek).third,
        carbShareHigh: Double = 1.0,
        carbShareMedium: Double = 0.65,
        carbShareLow: Double = 0.35,
        proteinPerKgOverride: Double? = null,
        weeklyAdjustmentOverride: Double? = null
    ): CarbCyclingPlan {
        val weight = profile.weightKg ?: 70.0
        val warnings = mutableListOf<String>()

        // 1) Bilancio calorico settimanale
        val adjFraction = weeklyAdjustmentOverride ?: weeklyAdjustmentFraction(profile.goal)
        val weeklyKcal = profile.calorieTargetOverride?.let { it * 7 }
            ?: (energy.tdee * 7 * (1 + adjFraction))

        val totalDays = (highDays + mediumDays + lowDays).coerceAtLeast(1)
        val dailyAvg = weeklyKcal / totalDays

        // 2) Proteine costanti
        val proteinPerKg = proteinPerKgOverride ?: proteinPerKg(profile.goal)
        val proteinG = weight * proteinPerKg

        // 3) Grassi minimi di sicurezza
        val minFatG = weight * MIN_FAT_G_PER_KG

        // 4) Calorie giornaliere per tipo, poi risoluzione carboidrati.
        //    Le calorie dei giorni HIGH sono sopra la media, LOW sotto, mantenendo
        //    esattamente il totale settimanale.
        val highKcal = dailyAvg * (1 + 0.15 * carbShareHigh)
        val lowKcal = dailyAvg * (1 - 0.18 * carbShareLow)
        val mediumKcal = computeBalancingKcal(
            weeklyKcal, highKcal, highDays, lowKcal, lowDays, mediumDays
        )

        val targets = linkedMapOf<CarbDayType, CarbDayTarget>()
        targets[CarbDayType.HIGH] = solveDay(
            CarbDayType.HIGH, highKcal, proteinG, minFatG, carbShareHigh, warnings, profile
        )
        targets[CarbDayType.MEDIUM] = solveDay(
            CarbDayType.MEDIUM, mediumKcal, proteinG, minFatG, carbShareMedium, warnings, profile
        )
        targets[CarbDayType.LOW] = solveDay(
            CarbDayType.LOW, lowKcal, proteinG, minFatG, carbShareLow, warnings, profile
        )

        // 5) Validazione bilancio settimanale e floor di sicurezza
        val weeklyFromTarget = targets[CarbDayType.HIGH]!!.kcal * highDays +
            targets[CarbDayType.MEDIUM]!!.kcal * mediumDays +
            targets[CarbDayType.LOW]!!.kcal * lowDays

        val floor = safetyFloor(profile, energy)
        targets.forEach { (type, t) ->
            if (t.kcal < floor) {
                warnings += "${type.label}: ${t.kcal.roundToInt()} kcal sotto la soglia di sicurezza " +
                    "(${floor.roundToInt()} kcal). Verifica il target calorico."
            }
        }

        val balanceDiff = abs(weeklyFromTarget - weeklyKcal)
        if (balanceDiff > 50) {
            warnings += "Il totale settimanale dei giorni (${weeklyFromTarget.roundToInt()} kcal) " +
                "differisce dal bilancio pianificato (${weeklyKcal.roundToInt()} kcal)."
        }

        val isValid = warnings.none {
            it.contains("sotto la soglia di sicurezza") || it.contains("differisce")
        }

        return CarbCyclingPlan(
            targets = targets,
            highDays = highDays,
            mediumDays = mediumDays,
            lowDays = lowDays,
            weeklyKcal = weeklyKcal,
            weeklyKcalFromTarget = weeklyFromTarget,
            weeklyProtein = proteinG * totalDays,
            weeklyCarbs = targets[CarbDayType.HIGH]!!.carbs * highDays +
                targets[CarbDayType.MEDIUM]!!.carbs * mediumDays +
                targets[CarbDayType.LOW]!!.carbs * lowDays,
            weeklyFat = targets[CarbDayType.HIGH]!!.fat * highDays +
                targets[CarbDayType.MEDIUM]!!.fat * mediumDays +
                targets[CarbDayType.LOW]!!.fat * lowDays,
            warnings = warnings,
            isValid = isValid
        )
    }

    /** Calcola le kcal dei giorni MEDIUM in modo che il totale settimanale torni. */
    private fun computeBalancingKcal(
        weeklyKcal: Double,
        highKcal: Double,
        highDays: Int,
        lowKcal: Double,
        lowDays: Int,
        mediumDays: Int
    ): Double {
        if (mediumDays <= 0) return 0.0
        val remaining = weeklyKcal - highKcal * highDays - lowKcal * lowDays
        return remaining / mediumDays
    }

    /**
     * Risolve i macro di un giorno dati kcal, proteine e grassi minimi.
     * I grassi partono dal minimo e vengono aumentati nei giorni LOW per compensare
     * la riduzione dei carboidrati (senza superare il 40% delle kcal).
     */
    private fun solveDay(
        type: CarbDayType,
        kcal: Double,
        proteinG: Double,
        minFatG: Double,
        carbShare: Double,
        warnings: MutableList<String>,
        profile: UserProfile
    ): CarbDayTarget {
        val proteinKcal = proteinG * KCAL_PER_G_PROTEIN
        val remainingKcal = (kcal - proteinKcal).coerceAtLeast(0.0)

        // Nei giorni LOW i grassi salgono, nei giorni HIGH restano al minimo.
        val fatShareOfRemaining = when (type) {
            CarbDayType.HIGH -> 0.28
            CarbDayType.MEDIUM -> 0.34
            CarbDayType.LOW -> 0.52
        }
        var fatG = (remainingKcal * fatShareOfRemaining / KCAL_PER_G_FAT)
            .coerceAtLeast(minFatG)

        // Cap grassi al 45% delle kcal totali per evitare diete sbilanciate.
        val maxFatG = kcal * 0.45 / KCAL_PER_G_FAT
        if (fatG > maxFatG) fatG = maxFatG

        var carbKcal = kcal - proteinKcal - fatG * KCAL_PER_G_FAT
        if (carbKcal < 0) {
            // Riduci i grassi per fare spazio ai carboidrati minimi.
            fatG = minFatG
            carbKcal = (kcal - proteinKcal - fatG * KCAL_PER_G_FAT).coerceAtLeast(0.0)
        }
        var carbG = carbKcal / KCAL_PER_G_CARB

        // Nei giorni LOW garantiamo comunque un minimo di carboidrati per il cervello.
        val minCarbG = profile.weightKg?.let { it * 0.5 } ?: 40.0
        if (type == CarbDayType.LOW && carbG < minCarbG) {
            carbG = minCarbG
        }

        // Ricalcolo kcal esatte dai macro arrotondati
        val pR = proteinG.roundToInt().toDouble()
        val cR = carbG.roundToInt().toDouble()
        val fR = fatG.roundToInt().toDouble()
        val kcalFromMacros = pR * KCAL_PER_G_PROTEIN + cR * KCAL_PER_G_CARB + fR * KCAL_PER_G_FAT

        if (abs(kcalFromMacros - kcal) > KCAL_TOLERANCE) {
            warnings += "${type.label}: coerenza kcal/macro fuori tolleranza " +
                "(${kcalFromMacros.roundToInt()} vs ${kcal.roundToInt()} kcal)."
        }

        return CarbDayTarget(
            type = type,
            kcal = kcalFromMacros,
            protein = pR,
            carbs = cR,
            fat = fR
        )
    }

    /** Verifica la coerenza matematica di un target. Ritorna l'errore in kcal. */
    fun validateCoherence(target: CarbDayTarget): Double {
        val expected = target.protein * KCAL_PER_G_PROTEIN +
            target.carbs * KCAL_PER_G_CARB +
            target.fat * KCAL_PER_G_FAT
        return abs(expected - target.kcal)
    }

    /** Distribuzione dei tipi di giornata sui 7 giorni (index 0=Lun .. 6=Dom). */
    fun assignDayTypes(highDays: Int, mediumDays: Int, lowDays: Int, trainingDayIndices: List<Int>): List<CarbDayType> {
        val types = MutableList(7) { CarbDayType.LOW }
        val sortedTraining = trainingDayIndices.sorted()
        // I primi `highDays` giorni di allenamento diventano HIGH, i successivi MEDIUM.
        sortedTraining.take(highDays).forEach { if (it in 0..6) types[it] = CarbDayType.HIGH }
        sortedTraining.drop(highDays).take(mediumDays).forEach { if (it in 0..6) types[it] = CarbDayType.MEDIUM }
        // Se restano giorni MEDIUM non assegnati a giorni di allenamento, usali sui giorni non-LOW.
        var remainingMedium = mediumDays - sortedTraining.drop(highDays).take(mediumDays).size
        var idx = 0
        while (remainingMedium > 0 && idx < 7) {
            if (types[idx] == CarbDayType.LOW) {
                types[idx] = CarbDayType.MEDIUM
                remainingMedium--
            }
            idx++
        }
        return types
    }
}
