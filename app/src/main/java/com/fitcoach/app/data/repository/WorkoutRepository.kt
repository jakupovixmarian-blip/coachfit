package com.fitcoach.app.data.repository

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.entity.ExercisePerformanceEntity
import com.fitcoach.app.data.local.entity.MesocycleEntity
import com.fitcoach.app.data.local.entity.PlannedExerciseEntity
import com.fitcoach.app.data.local.entity.WorkoutDayEntity
import com.fitcoach.app.data.local.entity.WorkoutPlanEntity
import com.fitcoach.app.data.local.entity.WorkoutSessionEntity
import com.fitcoach.app.data.local.entity.WorkoutSetEntity
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.Mesocycle
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.model.WorkoutPlan
import com.fitcoach.app.domain.model.WorkoutSession
import com.fitcoach.app.domain.model.WorkoutSet
import com.fitcoach.app.domain.workout.PeriodizationEngine
import com.fitcoach.app.domain.workout.ProgressiveOverloadEngine
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val db: FitCoachDatabase
) {
    private val planDao = db.workoutPlanDao()
    private val mesoDao = db.mesocycleDao()
    private val dayDao = db.workoutDayDao()
    private val plannedDao = db.plannedExerciseDao()
    private val sessionDao = db.workoutSessionDao()
    private val setDao = db.workoutSetDao()
    private val perfDao = db.exercisePerformanceDao()

    val activePlanFlow: Flow<WorkoutPlan?> = planDao.observeActive().map { entity ->
        entity?.let { loadFullPlan(it.id) }
    }

    suspend fun getActivePlan(): WorkoutPlan? {
        val entity = planDao.getActive() ?: return null
        return loadFullPlan(entity.id)
    }

    suspend fun savePlan(
        profile: UserProfile,
        built: WorkoutPlanBuilder.BuiltPlan,
        mesocycleWeeks: Int = 6
    ): Long = db.withTransaction {
        planDao.deactivateAll()
        val planId = planDao.insert(
            WorkoutPlanEntity(
                profileId = profile.id,
                name = "Scheda ${built.splitType.label}",
                splitType = built.splitType,
                isActive = true
            )
        )
        val meso = PeriodizationEngine.defaultMesocycle(planId, profile, mesocycleWeeks)
        val mesoId = mesoDao.insert(
            MesocycleEntity(
                planId = planId,
                name = meso.name,
                focus = meso.focus,
                weekCount = meso.weekCount,
                startWeekIndex = meso.startWeekIndex,
                deloadWeekIndex = meso.deloadWeekIndex,
                progressionModel = meso.progressionModel
            )
        )

        built.days.forEach { day ->
            val dayId = dayDao.insertAll(
                listOf(
                    WorkoutDayEntity(
                        mesocycleId = mesoId,
                        dayIndex = day.dayIndex,
                        title = day.title,
                        muscleGroups = day.muscleGroups,
                        isRestDay = day.isRestDay
                    )
                )
            ).first()
            if (!day.isRestDay) {
                plannedDao.insertAll(
                    day.exercises.map { pe ->
                        PlannedExerciseEntity(
                            workoutDayId = dayId,
                            exerciseId = pe.exerciseId,
                            exerciseName = pe.exerciseName,
                            primaryMuscle = pe.primaryMuscle,
                            orderIndex = pe.orderIndex,
                            sets = pe.sets,
                            minReps = pe.minReps,
                            maxReps = pe.maxReps,
                            restSeconds = pe.restSeconds,
                            targetRir = pe.targetRir,
                            technique = pe.technique,
                            category = pe.category
                        )
                    }
                )
            }
        }
        planId
    }

    suspend fun getDay(dayId: Long): WorkoutDay? {
        val day = dayDao.getById(dayId) ?: return null
        val planned = plannedDao.getForDay(dayId)
        return day.toDomain(planned.map { it.toDomain() })
    }

    suspend fun getActiveDays(): List<WorkoutDay> {
        val plan = planDao.getActive() ?: return emptyList()
        val mesos = mesoDao.getForPlan(plan.id)
        if (mesos.isEmpty()) return emptyList()
        val mesoIds = mesos.map { it.id }
        val days = dayDao.getForMesocycles(mesoIds)
        val planned = plannedDao.getForDays(days.map { it.id })
        val byDay = planned.groupBy { it.workoutDayId }
        return days.map { it.toDomain(byDay[it.id]?.map { p -> p.toDomain() } ?: emptyList()) }
    }

    suspend fun substituteExercise(plannedId: Long, exerciseId: Long, name: String, technique: String) =
        plannedDao.substituteExercise(plannedId, exerciseId, name, technique)

    suspend fun updateTarget(plannedId: Long, weight: Double?, reps: Int?) =
        plannedDao.updateTarget(plannedId, weight, reps)

    // ---------------- Sessioni ----------------

    suspend fun startSession(workoutDay: WorkoutDay, epochDay: Long): Long {
        val existing = sessionDao.getForDay(epochDay)
        if (existing != null) return existing.id
        val planId = planDao.getActive()?.id ?: 0L
        return sessionDao.insert(
            WorkoutSessionEntity(
                planId = planId,
                workoutDayId = workoutDay.id.takeIf { it > 0 },
                dayTitle = workoutDay.title,
                dateEpochDay = epochDay,
                status = SessionStatus.IN_PROGRESS,
                startedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getSessionForDay(epochDay: Long): WorkoutSession? {
        val entity = sessionDao.getForDay(epochDay) ?: return null
        return entity.toDomainWithSets()
    }

    suspend fun getRecentSessions(limit: Int = 20): List<WorkoutSession> =
        sessionDao.getRecent(limit).map { it.toDomainWithSets() }

    val sessionsFlow: Flow<List<WorkoutSession>> = sessionDao.observeAll().map { list ->
        list.map { it.toDomainWithSets() }
    }

    suspend fun saveSet(sessionId: Long, set: WorkoutSet): Long =
        setDao.insert(
            WorkoutSetEntity(
                id = set.id,
                sessionId = sessionId,
                exerciseId = set.exerciseId,
                exerciseName = set.exerciseName,
                setIndex = set.setIndex,
                plannedReps = set.plannedReps,
                actualReps = set.actualReps,
                weightKg = set.weightKg,
                rir = set.rir,
                rpe = set.rpe,
                completed = set.completed,
                isWarmup = set.isWarmup,
                notes = set.notes
            )
        )

    suspend fun completeSession(
        sessionId: Long,
        durationMinutes: Int?,
        perceivedEffort: Double?,
        sleepQuality: Int?,
        painReported: Boolean,
        painNotes: String
    ) {
        val entity = sessionDao.getById(sessionId) ?: return
        sessionDao.update(
            entity.copy(
                status = SessionStatus.COMPLETED,
                completedAt = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                perceivedEffort = perceivedEffort,
                sleepQuality = sleepQuality,
                painReported = painReported,
                painNotes = painNotes
            )
        )
        aggregatePerformance(sessionId)
    }

    /** Calcola e salva l'aggregato di performance per ogni esercizio della sessione. */
    private suspend fun aggregatePerformance(sessionId: Long) {
        val sets = setDao.getForSession(sessionId).filter { it.completed && !it.isWarmup }
        val session = sessionDao.getById(sessionId) ?: return
        sets.groupBy { it.exerciseId }.forEach { (exerciseId, exSets) ->
            val top = exSets.maxByOrNull { it.weightKg * 1000 + it.actualReps } ?: return@forEach
            val rirs = exSets.mapNotNull { it.rir }
            perfDao.insert(
                ExercisePerformanceEntity(
                    exerciseId = exerciseId,
                    exerciseName = top.exerciseName,
                    sessionId = sessionId,
                    dateEpochDay = session.dateEpochDay,
                    topWeightKg = top.weightKg,
                    topReps = top.actualReps,
                    totalSets = exSets.size,
                    totalReps = exSets.sumOf { it.actualReps },
                    volumeKg = exSets.sumOf { it.weightKg * it.actualReps },
                    avgRir = if (rirs.isEmpty()) null else rirs.average(),
                    estimated1Rm = ProgressiveOverloadEngine.estimated1Rm(top.weightKg, top.actualReps)
                )
            )
        }
    }

    suspend fun getPerformanceFor(exerciseId: Long): List<ExercisePerformance> =
        perfDao.getForExercise(exerciseId).map { it.toDomain() }

    suspend fun getLatestPerformance(exerciseId: Long): ExercisePerformance? =
        perfDao.getLatest(exerciseId)?.toDomain()

    val performanceFlow: Flow<List<ExercisePerformance>> = perfDao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun completedSessionCount(): Int = sessionDao.completedCount()

    suspend fun getSessionsBetween(from: Long, to: Long): List<WorkoutSession> =
        sessionDao.getBetween(from, to).map { it.toDomainWithSets() }

    // ---------------- helpers ----------------

    private suspend fun loadFullPlan(planId: Long): WorkoutPlan? {
        val entity = planDao.getById(planId) ?: return null
        val mesos = mesoDao.getForPlan(planId)
        val mesoIds = mesos.map { it.id }
        val days = if (mesoIds.isEmpty()) emptyList() else dayDao.getForMesocycles(mesoIds)
        val planned = if (days.isEmpty()) emptyList() else plannedDao.getForDays(days.map { it.id })
        val byDay = planned.groupBy { it.workoutDayId }
        val volume = mutableMapOf<MuscleGroup, Int>()
        planned.forEach { volume[it.primaryMuscle] = (volume[it.primaryMuscle] ?: 0) + it.sets }

        return WorkoutPlan(
            id = entity.id,
            profileId = entity.profileId,
            name = entity.name,
            splitType = entity.splitType,
            createdAt = entity.createdAt,
            isActive = entity.isActive,
            mesocycles = mesos.map {
                Mesocycle(
                    id = it.id, planId = it.planId, name = it.name, focus = it.focus,
                    weekCount = it.weekCount, startWeekIndex = it.startWeekIndex,
                    deloadWeekIndex = it.deloadWeekIndex, isDeload = it.isDeload,
                    progressionModel = it.progressionModel
                )
            },
            days = days.map { it.toDomain(byDay[it.id]?.map { p -> p.toDomain() } ?: emptyList()) },
            weeklyVolumePerGroup = volume
        )
    }

    private suspend fun WorkoutSessionEntity.toDomainWithSets(): WorkoutSession {
        val sets = setDao.getForSession(id).map { it.toDomain() }
        return WorkoutSession(
            id = id, planId = planId, workoutDayId = workoutDayId, dayTitle = dayTitle,
            dateEpochDay = dateEpochDay, status = status, startedAt = startedAt,
            completedAt = completedAt, durationMinutes = durationMinutes,
            perceivedEffort = perceivedEffort, sleepQuality = sleepQuality,
            painReported = painReported, painNotes = painNotes, sets = sets
        )
    }

    private fun ExercisePerformanceEntity.toDomain() = ExercisePerformance(
        id = id, exerciseId = exerciseId, exerciseName = exerciseName, sessionId = sessionId,
        dateEpochDay = dateEpochDay, topWeightKg = topWeightKg, topReps = topReps,
        totalSets = totalSets, totalReps = totalReps, volumeKg = volumeKg,
        avgRir = avgRir, estimated1Rm = estimated1Rm
    )
}
