package com.fitcoach.app.ui.screens.profile

/* ============================================================================
 * Modifica profilo (design/screens.md §14) — dati di sola presentazione.
 * I valori sono stringhe perché sono campi in modifica: la validazione e la
 * conversione restano nel ViewModel, che poi delega al motore deterministico.
 * ========================================================================== */

enum class ProfileField {
    NAME,
    WEIGHT,
    HEIGHT,
    AGE,
    MEALS_PER_DAY,
    TRAINING_DAYS,
    EQUIPMENT,
    ALLERGIES,
    LIMITATIONS,
    PAINS,
}

data class EditProfileUiState(
    val initials: String = "",
    val name: String = "",
    val goalLabel: String = "",
    val weight: String = "",
    val height: String = "",
    val age: String = "",
    val sexLabel: String = "",
    val mealsPerDay: String = "",
    val trainingDays: String = "",
    val equipment: String = "",
    val allergies: String = "",
    val limitations: String = "",
    val pains: String = "",
    val goalOptions: List<String> = emptyList(),
    val selectedGoalIndex: Int = 0,
    val errors: Map<ProfileField, String> = emptyMap(),
    val planImpactWarning: Boolean = false,
    val isSaving: Boolean = false,
    val savedMessage: String? = null,
)
