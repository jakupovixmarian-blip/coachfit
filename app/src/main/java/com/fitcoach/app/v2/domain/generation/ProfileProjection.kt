package com.fitcoach.app.v2.domain.generation

import com.fitcoach.app.v2.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisField
import com.fitcoach.app.v2.domain.anamnesis.AnamnesisSession

/** Profilo tipizzato per la generazione. I dati opzionali restano esplicitamente assenti. */
data class GenerationProfile(
    val ageYears: Int,
    val biologicalSex: String,
    val heightCm: Double,
    val weightKg: Double,
    val bodyFatPercent: Double?,
    val primaryGoal: String,
    val trainingExperience: String,
    val equipment: String,
    val trainingDays: Int,
    val answers: Map<AnamnesisField, String?>
) {
    fun promptContext(): String = buildString {
        appendLine("PROFILO CONFERMATO — usa esclusivamente i dati presenti.")
        appendLine("età_anni=$ageYears")
        appendLine("sesso_biologico=$biologicalSex")
        appendLine("altezza_cm=$heightCm")
        appendLine("peso_kg=$weightKg")
        appendLine("massa_grassa_percentuale=${bodyFatPercent ?: "NON DISPONIBILE"}")
        appendLine("obiettivo=$primaryGoal")
        appendLine("esperienza=$trainingExperience")
        appendLine("attrezzatura=$equipment")
        appendLine("giorni_allenamento=$trainingDays")
        appendLine("altri_dati:")
        answers.forEach { (field, value) -> appendLine("${field.name}=${value ?: "NON DISPONIBILE"}") }
    }
}

sealed interface ProfileProjectionResult {
    data class Success(val profile: GenerationProfile) : ProfileProjectionResult
    data class Failure(val missingOrInvalid: List<String>) : ProfileProjectionResult
}

object ProfileProjection {
    fun from(session: AnamnesisSession): ProfileProjectionResult {
        val review = AnamnesisEngine.review(session)
        if (!review.canConfirm || session.confirmedAt == null) {
            return ProfileProjectionResult.Failure(
                review.missingAnswers.map { it.name } +
                    review.unresolvedRequired.map { "${it.name}: non disponibile" } +
                    review.contradictions
            )
        }
        val failures = mutableListOf<String>()
        fun raw(field: AnamnesisField): String? = session.answers[field]?.value
        fun required(field: AnamnesisField): String = raw(field) ?: field.name.also { failures += "$it: non disponibile" }
        fun decimal(field: AnamnesisField): Double? = raw(field)?.replace(',', '.')?.toDoubleOrNull()
        val age = decimal(AnamnesisField.AGE)?.toInt() ?: run { failures += "AGE: valore non numerico"; 0 }
        val height = decimal(AnamnesisField.HEIGHT_CM) ?: run { failures += "HEIGHT_CM: valore non numerico"; 0.0 }
        val weight = decimal(AnamnesisField.WEIGHT_KG) ?: run { failures += "WEIGHT_KG: valore non numerico"; 0.0 }
        val days = decimal(AnamnesisField.TRAINING_DAYS)?.toInt() ?: run { failures += "TRAINING_DAYS: valore non numerico"; 0 }
        val values = session.answers.mapValues { (_, answer) -> answer.value }
        if (failures.isNotEmpty()) return ProfileProjectionResult.Failure(failures.distinct())
        return ProfileProjectionResult.Success(
            GenerationProfile(
                ageYears = age,
                biologicalSex = required(AnamnesisField.BIOLOGICAL_SEX),
                heightCm = height,
                weightKg = weight,
                bodyFatPercent = decimal(AnamnesisField.BODY_FAT_PERCENT),
                primaryGoal = required(AnamnesisField.PRIMARY_GOAL),
                trainingExperience = required(AnamnesisField.TRAINING_EXPERIENCE),
                equipment = required(AnamnesisField.EQUIPMENT),
                trainingDays = days,
                answers = values
            )
        )
    }
}
