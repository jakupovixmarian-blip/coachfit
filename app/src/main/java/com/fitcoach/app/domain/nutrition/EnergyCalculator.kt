package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.engine.EngineParameters
import com.fitcoach.app.domain.model.EnergyResult
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import kotlin.math.roundToInt

/**
 * Calcoli energetici deterministici.
 *
 * Fonti formule:
 *  - BMR Mifflin-St Jeor (1990): uomini 10*kg + 6.25*cm - 5*eta + 5; donne -161.
 *  - BMR Katch-McArdle: 370 + 21.6 * massa magra (richiede % grasso).
 *  - TDEE = BMR * fattore di attivita (Harris-Benedict activity multipliers).
 *
 * I risultati sono STIME. Se l'utente fornisce un override, viene usato quello.
 */
object EnergyCalculator {

    /** Spostamento di PAL per ogni 2.500 passi sopra o sotto i 5.000 di riferimento. */
    private const val STEPS_TO_PAL = 0.03

    /**
     * Fattore di attivita (PAL) del profilo.
     *
     * Le bande sono quelle FAO/WHO/UNU 2004 (Table 5.3): 1,40-1,69 per uno stile
     * sedentario o poco attivo, 1,70-1,99 per uno attivo o moderatamente attivo,
     * 2,00-2,40 per uno molto attivo.
     *
     * ATTENZIONE al mappaggio: la tabella classifica lo STILE DI VITA, non il
     * numero di allenamenti. Un impiegato che si allena 4 volte a settimana e
     * cammina 0 passi ha uno stile sedentario; chi lavora in cantiere e non si
     * allena ha uno stile molto attivo. Per questo il numero di allenamenti NON
     * puo' decidere da solo la banda: sceglie il punto di partenza, e la
     * correzione NEAT lo sposta dentro la banda.
     *
     * Il caso 4-5 usava [1,99, 2,00], largo 0,01: la correzione restava sempre
     * schiacciata a 1,99 e un lavoro d'ufficio senza passi non aveva alcun
     * effetto. Il TDEE di un impiegato sedentario usciva a 3.348 kcal. La banda
     * ora e' 1,70-2,00, cioe' "attivo" nel senso FAO (si allena), con il lavoro
     * d'ufficio che abbassa il valore verso il limite moderato.
     *
     * Prima qui c'erano 1,2 / 1,375 / 1,55 / 1,725: valori di comodo senza fonte,
     * che non corrispondono a nessuna banda pubblicata. In particolare 1,375 sta
     * sotto il minimo FAO per una persona che si allena 2-3 volte a settimana, e
     * quindi sottostimava il fabbisogno.
     */
    fun activityFactor(profile: UserProfile): Double {
        profile.activityFactorOverride?.let { return it }

        val min: Double
        val max: Double
        when (profile.trainingDaysPerWeek) {
            0, 1 -> {
                min = EngineParameters.palSedentaryMin.value
                max = EngineParameters.palSedentaryMax.value
            }
            2, 3 -> {
                min = EngineParameters.palModerateMin.value
                max = EngineParameters.palModerateMax.value
            }
            4, 5 -> {
                min = EngineParameters.palModerateMin.value
                max = EngineParameters.palVigorousMin.value
            }
            else -> {
                min = EngineParameters.palModerateMax.value
                max = EngineParameters.palVigorousMax.value
            }
        }

        // Punto di partenza: centro della banda. Le correzioni spostano dentro.
        val base = (min + max) / 2.0
        return (base + neatAdjustment(profile)).coerceIn(min, max)
    }

    /**
     * Correzione NEAT (attivita' non sportiva) dentro la banda PAL.
     *
     * Restituisce un delta da sommare al centro della banda. Il valore resta
     * comunque limitato dalla banda stessa in [activityFactor]: qui non si esce
     * mai dall'intervallo pubblicato.
     *
     * Le quantificazioni per passi e trasporto NON hanno una fonte primaria: sono
     * `CONVENTION`, e il registro lo dichiara. Servono a distinguere due persone
     * con gli stessi allenamenti ma uno stile di vita diverso, non a stimare il
     * dispendio con precisione.
     */
    fun neatAdjustment(profile: UserProfile): Double {
        var adj = profile.workType.neatAdjustment

        profile.dailySteps?.let { steps ->
            // 5.000 passi = riferimento neutro; ogni 2.500 passi muove di 0,03.
            val stepDelta = (steps - 5000) / 2500.0 * STEPS_TO_PAL
            adj += stepDelta.coerceIn(-0.10, 0.15)
        }

        adj += when (profile.transportMode) {
            com.fitcoach.app.domain.model.TransportMode.WALK -> 0.06
            com.fitcoach.app.domain.model.TransportMode.BIKE -> 0.05
            com.fitcoach.app.domain.model.TransportMode.PUBLIC -> 0.02
            com.fitcoach.app.domain.model.TransportMode.CAR -> 0.0
            com.fitcoach.app.domain.model.TransportMode.NONE -> -0.01
        }

        if (profile.walkingMinutesPerWeek > 0) {
            adj += (profile.walkingMinutesPerWeek / 60.0 / 7.0 * 0.02).coerceAtMost(0.06)
        }

        adj += (profile.extraSportSessionsPerWeek * 0.015).coerceAtMost(0.08)

        return adj
    }

    fun bmrMifflin(profile: UserProfile): Double? {
        val w = profile.weightKg ?: return null
        val h = profile.heightCm ?: return null
        val a = profile.ageYears ?: return null
        val sexTerm = when (profile.sex) {
            Sex.MALE -> 5.0
            Sex.FEMALE -> -161.0
            Sex.UNSPECIFIED -> -78.0 // media dei due termini, documentata
        }
        return 10 * w + 6.25 * h - 5 * a + sexTerm
    }

    fun bmrKatchMcArdle(profile: UserProfile): Double? {
        val w = profile.weightKg ?: return null
        val bf = profile.bodyFatPercent ?: return null
        if (bf <= 0 || bf >= 70) return null
        val leanMass = w * (1 - bf / 100.0)
        return 370 + 21.6 * leanMass
    }

    /**
     * Calcola BMR/TDEE scegliendo la formula piu accurata disponibile.
     * Se e presente la % di grasso usa Katch-McArdle, altrimenti Mifflin-St Jeor.
     */
    fun compute(profile: UserProfile): EnergyResult? {
        val mifflin = bmrMifflin(profile)
        val katch = bmrKatchMcArdle(profile)

        val (bmr, formula) = when {
            katch != null -> katch to "Katch-McArdle"
            mifflin != null -> mifflin to "Mifflin-St Jeor"
            else -> return null
        }

        val leanMass = profile.weightKg?.let { w ->
            profile.bodyFatPercent?.let { bf -> w * (1 - bf / 100.0) }
        }

        val factor = activityFactor(profile)
        val tdee = bmr * factor
        val weekly = profile.calorieTargetOverride?.let { it * 7 } ?: (tdee * 7)

        val notes = mutableListOf<String>()
        notes += "BMR stimato con $formula: ${bmr.roundToInt()} kcal."
        notes += "TDEE = BMR x ${"%.3f".format(factor)} = ${tdee.roundToInt()} kcal (stima)."
        val neat = neatAdjustment(profile)
        if (neat != 0.0) {
            notes += "Fattore attivita corretto per stile di vita (lavoro ${profile.workType.label}" +
                (profile.dailySteps?.let { ", ~$it passi/giorno" } ?: "") +
                "): ${if (neat > 0) "+" else ""}${"%.3f".format(neat)}."
        }
        if (profile.calorieTargetOverride != null) {
            notes += "Target calorico sovrascritto manualmente dall'utente."
        }
        if (mifflin != null && katch == null) {
            notes += "Inserisci la % di grasso per una stima piu precisa (Katch-McArdle)."
        }

        return EnergyResult(
            bmr = bmr,
            tdee = tdee,
            bmrFormula = formula,
            tdeeFormula = "BMR x fattore attivita (${"%.3f".format(factor)})",
            leanMassKg = leanMass,
            weeklyCalories = weekly,
            activityFactor = factor,
            notes = notes
        )
    }
}
