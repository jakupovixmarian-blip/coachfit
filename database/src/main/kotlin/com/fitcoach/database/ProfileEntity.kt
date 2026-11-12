package com.fitcoach.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1,
    val age: Int? = null,
    val biologicalSex: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val goal: String? = null,
    val trainingExperience: String? = null,
    val availableTrainingDays: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
