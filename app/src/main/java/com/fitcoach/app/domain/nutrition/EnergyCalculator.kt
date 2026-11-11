package com.fitcoach.app.domain.nutrition

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

    /** Fattori di attivita per livello di esperienza e giorni di allenamento. */
    fun activityFactor(profile: UserProfile): Double {
        profile.activityFactorOverride?.let { return it }
        val base = when (profile.trainingDaysPerWeek) {
            0, 1 -> 1.2
            2, 3 -> 1.375
            4, 5 -> 1.55
            else -> 1.725
        }
        // L'esperienza avanzata tende ad avere NEAT piu alto; correzione piccola e documentata.
        val experienceAdj = when (profile.experience) {
            TrainingExperience.BEGINNER -> 0.0
            TrainingExperience.INTERMEDIATE -> 0.025
            TrainingExperience.ADVANCED -> 0.05
        }
        return base + experienceAdj
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
            notes = notes
        )
    }
}
