package com.fitcoach.app.domain.model

/**
 * Profilo utente. I campi nullable rappresentano dati non ancora forniti:
 * non vengono mai sostituiti con valori fittizi.
 */
data class UserProfile(
    val id: Long = 1L,
    val name: String = "",
    val ageYears: Int? = null,
    val sex: Sex = Sex.UNSPECIFIED,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val experience: TrainingExperience = TrainingExperience.BEGINNER,
    val trainingStatus: TrainingStatus? = null,
    val goal: Goal = Goal.RECOMP,
    val trainingDaysPerWeek: Int = 3,
    val availableEquipment: Set<Equipment> = emptySet(),
    val dietaryPreferences: Set<DietaryPreference> = emptySet(),
    val excludedFoods: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val physicalLimitations: List<String> = emptyList(),
    val movementsToAvoid: List<String> = emptyList(),
    val weeklyFoodBudgetEur: Double? = null,
    val activityFactorOverride: Double? = null,
    val calorieTargetOverride: Double? = null,
    val mealsPerDay: Int = 5,

    // ---- Budget mensile ----
    /** Budget alimentare mensile in EUR (il vincolo effettivo richiesto dalla spec). */
    val monthlyFoodBudgetEur: Double? = null,
    /** Ambito del budget: personale, condiviso o totale. */
    val budgetScope: BudgetScope = BudgetScope.PERSONAL_ONLY,
    /** Numero di persone coperte dal budget (>=1). */
    val budgetPersons: Int = 1,

    // ---- Stile di vita ----
    val workType: WorkType = WorkType.SEDENTARY,
    val workShift: WorkShift = WorkShift.NONE,
    val workHoursPerDay: Double? = null,
    val seatedHoursPerDay: Double? = null,
    val dailySteps: Int? = null,
    val transportMode: TransportMode = TransportMode.CAR,
    val extraSportSessionsPerWeek: Int = 0,
    val walkingMinutesPerWeek: Int = 0,
    val maxSessionMinutes: Int? = null,
    val maxMealPrepMinutes: Int? = null,

    // ---- Recupero e abitudini ----
    val sleepHours: Double? = null,
    val sleepQuality: Level3 = Level3.MEDIUM,
    val bedtime: String = "",
    val wakeTime: String = "",
    val stressLevel: Level3 = Level3.MEDIUM,
    val dailyEnergy: Level3 = Level3.MEDIUM,
    val recoveryCapacity: Level3 = Level3.MEDIUM,
    val hydrationLitersPerDay: Double? = null,
    val caffeineCupsPerDay: Int? = null,
    val alcoholUnitsPerWeek: Int? = null,

    // ---- Alimentazione e preferenze ----
    val breakfastPreference: BreakfastPreference = BreakfastPreference.SWEET,
    val likedFoods: List<String> = emptyList(),
    val dislikedFoods: List<String> = emptyList(),
    val dietaryRestrictions: List<String> = emptyList(),
    val cookingTimeAvailable: CookingTimeAvailable = CookingTimeAvailable.MEDIUM,
    val mealPrepPossible: Boolean = false,
    val prefersSimpleFoods: Boolean = true,
    val dietDifficulties: String = "",

    // ---- Esperienza di allenamento (dettagli) ----
    val trainingYears: Double? = null,
    val trainingFrequencyPerWeek: Int? = null,
    val preferredExercises: List<String> = emptyList(),
    val dislikedExercises: List<String> = emptyList(),
    val unknownExercises: List<String> = emptyList(),
    val technicalLevel: TechnicalLevel = TechnicalLevel.NOVICE,
    val priorityMuscles: Set<MuscleGroup> = emptySet(),
    val currentLoads: Map<String, Double> = emptyMap(),

    // ---- Obiettivi estesi ----
    val secondaryGoals: Set<Goal> = emptySet(),
    val desiredOutcome: String = "",
    val targetTimelineWeeks: Int? = null,

    // ---- Anamnesi (gate di generazione) ----
    /** True quando l'anamnesi guidata e stata completata e confermata dall'utente. */
    val anamnesisCompleted: Boolean = false,
    /** Timestamp della conferma dell'anamnesi (null se non confermata). */
    val anamnesisConfirmedAt: Long? = null,
    /** Riepilogo testuale dell'anamnesi mostrato e confermato dall'utente. */
    val anamnesisSummary: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isComplete: Boolean
        get() = ageYears != null && heightCm != null && weightKg != null

    /**
     * Anamnesi pronta: la generazione dei programmi e consentita solo dopo la
     * conferma esplicita dell'anamnesi (spec. §3.7). I dati essenziali devono
     * essere presenti e non devono esserci contraddizioni bloccanti.
     */
    val isAnamnesisReady: Boolean
        get() = anamnesisCompleted && missingEssentialData.isEmpty()

    /** Contraddizioni rilevate nell'anamnesi (da correggere prima di confermare). */
    val anamnesisContradictions: List<String>
        get() = buildList {
            val days = trainingDaysPerWeek
            if (days > 7) add("I giorni di allenamento non possono superare 7.")
            val freq = trainingFrequencyPerWeek
            if (freq != null && freq > days && days > 0) {
                add("La frequenza dichiarata ($freq) supera i giorni disponibili ($days).")
            }
            val age = ageYears
            if (age != null && age < 14) add("Età inferiore a 14 anni: richiedere supervisione medica.")
            val sleep = sleepHours
            if (sleep != null && sleep <= 0) add("Le ore di sonno devono essere positive.")
            val bf = bodyFatPercent
            if (bf != null && (bf < 3 || bf > 60)) add("Percentuale di massa grassa fuori intervallo plausibile.")
        }

    /** Campi dell'anamnesi ancora mancanti (oltre ai dati essenziali). */
    val missingAnamnesisData: List<String>
        get() = buildList {
            addAll(missingEssentialData)
            if (goal == null) add("obiettivo")
            if (availableEquipment.isEmpty()) add("attrezzatura")
            if (breakfastPreference == BreakfastPreference.SWEET && likedFoods.isEmpty() &&
                dislikedFoods.isEmpty()
            ) {
                // informativo, non bloccante
            }
        }

    /**
     * Dati indispensabili per generare programmi personalizzati. Se mancano,
     * non viene generato un piano definitivo (evita piani su dati incompleti).
     */
    val missingEssentialData: List<String>
        get() = buildList {
            if (ageYears == null) add("età")
            if (heightCm == null) add("altezza")
            if (weightKg == null) add("peso")
            if (trainingDaysPerWeek <= 0) add("giorni di allenamento")
        }

    val bmi: Double?
        get() {
            val h = heightCm ?: return null
            val w = weightKg ?: return null
            if (h <= 0) return null
            val m = h / 100.0
            return w / (m * m)
        }
}

data class MealSchedule(
    val slot: MealSlot,
    val time: String,
    val enabled: Boolean = true
)

data class UserPreferences(
    val profileId: Long = 1L,
    val mealSchedules: List<MealSchedule> = MealSlot.entries.map {
        MealSchedule(it, it.defaultTime)
    },
    val workoutTime: String = "18:00",
    val hydrationRemindersEnabled: Boolean = true,
    val hydrationIntervalHours: Int = 3,
    val hydrationStartTime: String = "08:00",
    val hydrationEndTime: String = "22:00",
    val weeklyCheckInEnabled: Boolean = true,
    val weeklyCheckInDay: Int = 1, // 0=Lun .. 6=Dom
    val weeklyCheckInTime: String = "09:00",
    val weightReminderEnabled: Boolean = true,
    val weightReminderDay: Int = 1,
    val weightReminderTime: String = "07:00",
    val notificationsEnabled: Boolean = true,
    val deferMinutes: Int = 15,
    val locale: String = "it",
    val unitsMetric: Boolean = true
)
