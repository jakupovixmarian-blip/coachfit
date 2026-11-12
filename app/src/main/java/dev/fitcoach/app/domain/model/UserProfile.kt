package dev.fitcoach.app.domain.model

data class UserProfile(
    val id: Long = 1L,
    val name: String = "",
    val age: Int? = null,
    val sex: Sex = Sex.UNSPECIFIED,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val goal: Goal = Goal.RECOMP,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val trainingYears: Double? = null,
    val consistencyWeeks: Int? = null,
    val knownLoads: String = "",
    val jobActivity: String = "",
    val dailySteps: Int? = null,
    val sleepHours: Double? = null,
    val stressLevel: Int? = null,
    val recoveryLevel: Int? = null,
    val injuries: String = "",
    val equipment: String = "",
    val availableDays: Int? = null,
    val sessionMinutes: Int? = null,
    val dietPreference: String = "",
    val allergies: String = "",
    val mealsPerDay: Int? = null,
    val mealTimes: String = "",
    val weeklyBudget: Double? = null,
    val cookingMinutes: Int? = null,
    val likedFoods: String = "",
    val dislikedFoods: String = "",
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val onboardingCompleted: Boolean = false,
    val updatedAt: Long = 0L
) {
    /** Mifflin-St Jeor BMR. Returns null when data is insufficient. */
    fun bmr(): Double? {
        val h = heightCm ?: return null
        val w = weightKg ?: return null
        val a = age ?: return null
        val base = 10 * w + 6.25 * h - 5 * a
        return when (sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
            Sex.UNSPECIFIED -> base - 78
        }
    }

    fun activityFactor(): Double = when (experience) {
        ExperienceLevel.BEGINNER -> 1.375
        ExperienceLevel.INTERMEDIATE -> 1.55
        ExperienceLevel.ADVANCED -> 1.725
    }

    /** Estimated maintenance calories. Null when profile is incomplete. */
    fun maintenanceCalories(): Double? = bmr()?.let { it * activityFactor() }

    fun targetCalories(): Double? = maintenanceCalories()?.let { maintenance ->
        when (goal) {
            Goal.FAT_LOSS -> maintenance * 0.80
            Goal.RECOMP -> maintenance * 0.95
            Goal.MUSCLE_GAIN -> maintenance * 1.10
            Goal.STRENGTH -> maintenance * 1.05
            Goal.MAINTENANCE, Goal.HEALTH -> maintenance
        }
    }

    fun proteinTarget(): Double? = weightKg?.let { w ->
        val perKg = when (goal) {
            Goal.FAT_LOSS -> 2.2
            Goal.RECOMP -> 2.0
            Goal.MUSCLE_GAIN -> 1.8
            Goal.STRENGTH -> 1.8
            Goal.MAINTENANCE, Goal.HEALTH -> 1.6
        }
        w * perKg
    }

    fun fatTarget(): Double? = weightKg?.let { it * 0.8 }

    fun carbsTarget(): Double? {
        val kcal = targetCalories() ?: return null
        val p = proteinTarget() ?: return null
        val f = fatTarget() ?: return null
        val remaining = kcal - (p * 4) - (f * 9)
        return (remaining / 4).coerceAtLeast(0.0)
    }
}
