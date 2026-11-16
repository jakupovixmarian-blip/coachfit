package com.fitcoach.app.ui.screens.anamnesis

/**
 * Stato dell'Anamnesi (primo avvio e modifica successiva).
 *
 * I valori sono stringhe perché provengono dai campi di testo: la conversione e
 * la validazione di dominio restano nel ViewModel esistente, che chiama
 * buildFromAnamnesis(...). Qui non si calcola nulla di nutrizionale.
 */
data class AnamnesisUiState(
    val step: Int = 0,
    val stepCount: Int = 5,
    val sex: SexOption = SexOption.MALE,
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val goal: GoalOption = GoalOption.RECOMPOSITION,
    val experience: ExperienceOption = ExperienceOption.INTERMEDIATE,
    val daysPerWeek: Int = 4,
    val equipment: EquipmentOption = EquipmentOption.GYM,
    val activity: ActivityOption = ActivityOption.MODERATE,
    val allergies: String = "",
    val limitations: String = "",
    val sleepHours: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Avanzamento mostrato dall'indicatore a passi. */
    val progress: Float get() = if (stepCount > 0) (step + 1f) / stepCount else 0f

    /** Il primo passo richiede i dati antropometrici minimi. */
    val canGoNext: Boolean
        get() = when (step) {
            0 -> age.isNotBlank() && heightCm.isNotBlank() && weightKg.isNotBlank()
            else -> true
        }

    val isLastStep: Boolean get() = step >= stepCount - 1
}

enum class SexOption(val label: String) {
    MALE("Uomo"),
    FEMALE("Donna"),
}

enum class GoalOption(val label: String, val description: String) {
    HYPERTROPHY("Ipertrofia", "Aumento della massa muscolare"),
    RECOMPOSITION("Ricomposizione", "Massa magra su, grasso giù"),
    FAT_LOSS("Definizione", "Riduzione della massa grassa"),
    STRENGTH("Forza", "Prestazione sui grandi alzati"),
}

enum class ExperienceOption(val label: String) {
    BEGINNER("Principiante"),
    INTERMEDIATE("Intermedio"),
    ADVANCED("Avanzato"),
}

enum class EquipmentOption(val label: String) {
    GYM("Palestra completa"),
    HOME("Casa"),
    BODYWEIGHT("Corpo libero"),
}

enum class ActivityOption(val label: String) {
    SEDENTARY("Sedentario"),
    LIGHT("Leggero"),
    MODERATE("Moderato"),
    HIGH("Elevato"),
}
