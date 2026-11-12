package com.fitcoach.domain

data class UserProfile(
    val age: Int? = null,
    val biologicalSex: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val goal: String? = null,
    val trainingExperience: String? = null,
    val availableTrainingDays: Int? = null
) {
    val isComplete: Boolean
        get() = age != null && biologicalSex != null && heightCm != null && weightKg != null && goal != null
}
