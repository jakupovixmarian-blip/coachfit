package com.fitcoach.app.ui.screens.onboarding

/* =============================================================================
 * FitCoach 2.0 — Onboarding · OnboardingUiState
 * -----------------------------------------------------------------------------
 * Stato di PRESENTAZIONE del flusso guidato iniziale.
 *
 * L'onboarding reale NON è una lista generica di pagine: ha 8 passi nominati,
 * ognuno con i propri campi e la propria validazione. Questo stato li
 * rispecchia 1:1, così la grafica può essere agganciata al ViewModel senza
 * perdere nessun campo.
 *
 * Nessun calcolo: qui si raccolgono i dati che alimentano l'anamnesi e la
 * generazione AI. Le regole di validazione restano nel ViewModel/logica.
 * ========================================================================== */

/** Gli 8 passi reali del flusso. */
enum class OnboardingStep(val title: String, val subtitle: String) {
    ANAGRAFICA(
        title = "Dati personali",
        subtitle = "Nome, età, altezza, peso e composizione corporea",
    ),
    OBIETTIVO(
        title = "Obiettivo e allenamento",
        subtitle = "Cosa vuoi ottenere e quanto tempo hai",
    ),
    DETTAGLI_ALLENAMENTO(
        title = "Dettagli allenamento",
        subtitle = "Durata seduta, orario preferito e frequenza",
    ),
    ATTREZZATURA(
        title = "Attrezzatura disponibile",
        subtitle = "Palestra completa, home gym o solo corpo libero",
    ),
    NUTRIZIONE(
        title = "Preferenze alimentari",
        subtitle = "Stile alimentare, alimenti esclusi, allergie",
    ),
    STILE_VITA(
        title = "Stile di vita e recupero",
        subtitle = "Sonno, stress, attività quotidiana e orari",
    ),
    SALUTE(
        title = "Salute e limitazioni",
        subtitle = "Limitazioni fisiche, dolori e movimenti da evitare",
    ),
    RIEPILOGO(
        title = "Riepilogo",
        subtitle = "Controlla i dati prima di iniziare l'anamnesi",
    );

    val isLast: Boolean get() = this == RIEPILOGO
    val isFirst: Boolean get() = this == ANAGRAFICA
}

/** Sesso biologico richiesto dai calcoli energetici. */
enum class SexOption(val label: String) {
    MALE("Uomo"),
    FEMALE("Donna"),
    UNSPECIFIED("Non specificato"),
}

/** Obiettivo primario. */
enum class GoalOption(val label: String) {
    HYPERTROPHY("Ipertrofia"),
    RECOMPOSITION("Ricomposizione"),
    FAT_LOSS("Definizione"),
    STRENGTH("Forza"),
    MAINTENANCE("Mantenimento"),
}

/** Esperienza di allenamento. */
enum class ExperienceOption(val label: String) {
    BEGINNER("Principiante"),
    INTERMEDIATE("Intermedio"),
    ADVANCED("Avanzato"),
}

/** Attrezzatura selezionabile. */
enum class EquipmentOption(val label: String) {
    BARBELL("Bilanciere e dischi"),
    DUMBBELLS("Manubri"),
    MACHINES("Macchine"),
    CABLES("Cavi"),
    RACK("Rack / gabbia"),
    BENCH("Panca"),
    BODYWEIGHT("Corpo libero"),
    BANDS("Elastici"),
}

/** Stile alimentare. */
enum class DietPreference(val label: String) {
    OMNIVORE("Onnivoro"),
    MEDITERRANEAN("Mediterraneo"),
    VEGETARIAN("Vegetariano"),
    VEGAN("Vegano"),
    PESCATARIAN("Pescatariano"),
}

/** Livello di attività quotidiana oltre all'allenamento. */
enum class ActivityLevel(val label: String) {
    SEDENTARY("Sedentario"),
    LIGHT("Poco attivo"),
    MODERATE("Moderatamente attivo"),
    HIGH("Molto attivo"),
}

/** Qualità percepita del sonno. */
enum class SleepQuality(val label: String) {
    POOR("Scarso"),
    FAIR("Discreto"),
    GOOD("Buono"),
    EXCELLENT("Ottimo"),
}

/** Una voce del riepilogo finale (etichetta + valore già formattato). */
data class OnboardingSummaryRow(
    val label: String,
    val value: String,
    val step: OnboardingStep,
    val isMissing: Boolean = false,
)

/** Attrezzatura con stato di selezione, per i chip. */
data class EquipmentChoice(
    val option: EquipmentOption,
    val isSelected: Boolean,
)

/** Area di dolore/limitazione selezionabile. */
data class LimitationChoice(
    val label: String,
    val isSelected: Boolean,
)

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // --- Navigazione del flusso -------------------------------------------------
    val step: OnboardingStep = OnboardingStep.ANAGRAFICA,
    val stepIndex: Int = 0,
    val stepCount: Int = OnboardingStep.entries.size,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,

    // --- Step 1 · Dati personali ------------------------------------------------
    val name: String = "",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val bodyFatPercent: String = "",
    val sex: SexOption = SexOption.UNSPECIFIED,

    // --- Step 2 · Obiettivo e allenamento --------------------------------------
    val goal: GoalOption = GoalOption.HYPERTROPHY,
    val experience: ExperienceOption = ExperienceOption.INTERMEDIATE,

    // --- Step 3 · Dettagli allenamento -----------------------------------------
    val daysPerWeek: String = "4",
    val sessionMinutes: String = "60",
    val preferredTime: String = "",
    val trainingLocation: String = "",

    // --- Step 4 · Attrezzatura --------------------------------------------------
    val equipment: List<EquipmentChoice> = emptyList(),

    // --- Step 5 · Preferenze alimentari ----------------------------------------
    val diet: DietPreference = DietPreference.OMNIVORE,
    val excludedFoods: String = "",
    val allergies: String = "",
    val intolerances: String = "",
    val mealsPerDay: String = "5",

    // --- Step 6 · Stile di vita e recupero -------------------------------------
    val sleepHours: String = "",
    val sleepQuality: SleepQuality = SleepQuality.GOOD,
    val stressLevel: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val workSchedule: String = "",
    val waterLiters: String = "",

    // --- Step 7 · Salute e limitazioni -----------------------------------------
    val limitations: List<LimitationChoice> = emptyList(),
    val avoidMovements: String = "",
    val medicalNotes: String = "",

    // --- Step 8 · Riepilogo -----------------------------------------------------
    val summary: List<OnboardingSummaryRow> = emptyList(),
    val warnings: List<String> = emptyList(),

    // --- Generazione iniziale ---------------------------------------------------
    val isStartingAnamnesis: Boolean = false,
    val generationError: String? = null,
    val canStartAnamnesis: Boolean = false,
) {
    /** Frazione 0..1 per la barra di avanzamento. */
    val progress: Float
        get() = if (stepCount <= 1) 1f else stepIndex.toFloat() / (stepCount - 1)

    val stepLabel: String
        get() = "Passo ${stepIndex + 1} di $stepCount"

    val isLastStep: Boolean
        get() = step.isLast

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.ANAGRAFICA ->
                name.isNotBlank() && age.toIntOrNull() != null &&
                    heightCm.toIntOrNull() != null && weightKg.toDoubleOrNull() != null

            OnboardingStep.OBIETTIVO -> true
            OnboardingStep.DETTAGLI_ALLENAMENTO ->
                daysPerWeek.toIntOrNull()?.let { it in 1..7 } == true

            OnboardingStep.ATTREZZATURA -> equipment.any { it.isSelected }
            OnboardingStep.NUTRIZIONE -> true
            OnboardingStep.STILE_VITA -> true
            OnboardingStep.SALUTE -> true
            OnboardingStep.RIEPILOGO -> canStartAnamnesis
        }
}
