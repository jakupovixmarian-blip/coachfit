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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isComplete: Boolean
        get() = ageYears != null && heightCm != null && weightKg != null

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
