package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.dao.UserPreferencesDao
import com.fitcoach.app.data.local.entity.UserPreferencesEntity
import com.fitcoach.app.domain.model.CoachAuthorizationMode
import com.fitcoach.app.domain.model.MealSchedule
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dao: UserPreferencesDao
) {
    val preferencesFlow: Flow<UserPreferences> = dao.observe().map { it?.toDomain() ?: default() }

    suspend fun getOrCreate(): UserPreferences {
        val existing = dao.get()
        if (existing == null) {
            val def = default()
            dao.upsert(def.toEntity())
            return def
        }
        return existing.toDomain()
    }

    suspend fun save(prefs: UserPreferences) = dao.upsert(prefs.toEntity())

    private fun default() = UserPreferences()

    private fun UserPreferencesEntity.toDomain() = UserPreferences(
        profileId = profileId,
        mealSchedules = decodeSchedules(mealScheduleJson),
        workoutTime = workoutTime,
        hydrationRemindersEnabled = hydrationRemindersEnabled,
        hydrationIntervalHours = hydrationIntervalHours,
        hydrationStartTime = hydrationStartTime,
        hydrationEndTime = hydrationEndTime,
        weeklyCheckInEnabled = weeklyCheckInEnabled,
        weeklyCheckInDay = weeklyCheckInDay,
        weeklyCheckInTime = weeklyCheckInTime,
        weightReminderEnabled = weightReminderEnabled,
        weightReminderDay = weightReminderDay,
        weightReminderTime = weightReminderTime,
        notificationsEnabled = notificationsEnabled,
        deferMinutes = deferMinutes,
        coachAuthorizationMode = runCatching { CoachAuthorizationMode.valueOf(coachAuthorizationMode) }
            .getOrDefault(CoachAuthorizationMode.CONFIRMATION),
        locale = locale,
        unitsMetric = unitsMetric
    )

    private fun UserPreferences.toEntity() = UserPreferencesEntity(
        profileId = profileId,
        mealScheduleJson = encodeSchedules(mealSchedules),
        workoutTime = workoutTime,
        hydrationRemindersEnabled = hydrationRemindersEnabled,
        hydrationIntervalHours = hydrationIntervalHours,
        hydrationStartTime = hydrationStartTime,
        hydrationEndTime = hydrationEndTime,
        weeklyCheckInEnabled = weeklyCheckInEnabled,
        weeklyCheckInDay = weeklyCheckInDay,
        weeklyCheckInTime = weeklyCheckInTime,
        weightReminderEnabled = weightReminderEnabled,
        weightReminderDay = weightReminderDay,
        weightReminderTime = weightReminderTime,
        notificationsEnabled = notificationsEnabled,
        deferMinutes = deferMinutes,
        coachAuthorizationMode = coachAuthorizationMode.name,
        locale = locale,
        unitsMetric = unitsMetric
    )

    private fun encodeSchedules(schedules: List<MealSchedule>): String =
        schedules.joinToString(";") { "${it.slot.name},${it.time},${it.enabled}" }

    private fun decodeSchedules(raw: String): List<MealSchedule> {
        if (raw.isBlank()) return MealSlot.entries.map { MealSchedule(it, it.defaultTime) }
        return raw.split(";").mapNotNull { part ->
            val tokens = part.split(",")
            val slot = runCatching { MealSlot.valueOf(tokens[0]) }.getOrNull() ?: return@mapNotNull null
            MealSchedule(
                slot = slot,
                time = tokens.getOrNull(1) ?: slot.defaultTime,
                enabled = tokens.getOrNull(2)?.toBooleanStrictOrNull() ?: true
            )
        }.ifEmpty { MealSlot.entries.map { MealSchedule(it, it.defaultTime) } }
    }
}
