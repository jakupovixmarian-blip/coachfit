package com.fitcoach.app.data.repository

import androidx.room.withTransaction
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.entity.ExercisePerformanceEntity
import com.fitcoach.app.data.local.entity.FreeSessionExerciseEntity
import com.fitcoach.app.data.local.entity.MesocycleEntity
import com.fitcoach.app.data.local.entity.PlannedExerciseEntity
import com.fitcoach.app.data.local.entity.WorkoutDayEntity
import com.fitcoach.app.data.local.entity.WorkoutPlanEntity
import com.fitcoach.app.data.local.entity.WorkoutSessionEntity
import com.fitcoach.app.data.local.entity.WorkoutSetEntity
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExercisePerformance
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Mesocycle
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.PlannedExercise
import com.fitcoach.app.domain.model.SessionStatus
import com.fitcoach.app.domain.model.SplitType
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkoutDay
import com.fitcoach.app.domain.model.WorkoutPlan
import com.fitcoach.app.domain.model.WorkoutSession
import com.fitcoach.app.domain.model.WorkoutSet
import com.fitcoach.app.domain.workout.ProgressiveOverloadEngine
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
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
    private val freeExerciseDao = db.freeSessionExerciseDao()
    private val sessionDao = db.workoutSessionDao()
    private val setDao = db.workoutSetDao()
    private val perfDao = db.exercisePerformanceDao()
    private val scheduleOverrideDao = db.workoutScheduleOverrideDao()

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
        val previousVersion = planDao.getActive()?.version ?: 0
        planDao.deactivateAll()
        val planId = planDao.insert(
            WorkoutPlanEntity(
                profileId = profile.id,
                name = "Scheda ${built.splitType.label}",
                splitType = built.splitType,
                isActive = true,
                version = previousVersion + 1
            )
        )
        val meso = defaultMesocycle(planId, profile, mesocycleWeeks)
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
                            category = pe.category,
                            selectionReason = pe.selectionReason,
                            restReason = pe.restReason,
                            intensityTechnique = pe.intensityTechnique,
                            techniqueReason = pe.techniqueReason,
                            movementPattern = pe.movementPattern
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

    suspend fun getActiveDays(weekStartEpochDay: Long? = null): List<WorkoutDay> {
        val plan = planDao.getActive() ?: return emptyList()
        val mesos = mesoDao.getForPlan(plan.id)
        if (mesos.isEmpty()) return emptyList()
        val mesoIds = mesos.map { it.id }
        val days = dayDao.getForMesocycles(mesoIds)
        val planned = plannedDao.getForDays(days.map { it.id })
        val byDay = planned.groupBy { it.workoutDayId }
        val base = days.map { it.toDomain(byDay[it.id]?.map { p -> p.toDomain() } ?: emptyList()) }
        val week = weekStartEpochDay ?: LocalDate.now().toEpochDay().let { it - Math.floorMod(it + 3, 7) }
        val temporary = scheduleOverrideDao.getForWeek(week).filter { !it.permanent }
        val permanent = scheduleOverrideDao.getPermanent()
        val targets = (permanent + temporary).associate { it.workoutDayId to it.targetDayIndex }
        return base.map { day -> day.copy(dayIndex = targets[day.id] ?: day.dayIndex) }
    }

    suspend fun substituteExercise(plannedId: Long, exerciseId: Long, name: String, technique: String) =
        plannedDao.substituteExercise(plannedId, exerciseId, name, technique)

    /**
     * Sostituisce un esercizio mantenendo, quando possibile, lo stimolo previsto
     * (serie, range ripetizioni, recupero, RIR, categoria, tecnica d'intensita).
     */
    suspend fun substituteExercisePreservingStimulus(plannedId: Long, replacement: Exercise) {
        val current = plannedDao.getById(plannedId)
        if (current != null) {
            plannedDao.update(
                current.copy(
                    exerciseId = replacement.id,
                    exerciseName = replacement.name,
                    primaryMuscle = replacement.primaryMuscle,
                    technique = replacement.technique,
                    category = replacement.category,
                    movementPattern = replacement.movementPattern,
                    minReps = replacement.minReps,
                    maxReps = replacement.maxReps,
                    targetRir = replacement.targetRir
                )
            )
        } else {
            freeExerciseDao.substitute(
                id = plannedId,
                exerciseId = replacement.id,
                name = replacement.name,
                primaryMuscle = replacement.primaryMuscle,
                minReps = replacement.minReps,
                maxReps = replacement.maxReps,
                targetRir = replacement.targetRir,
                technique = replacement.technique,
                category = replacement.category,
                movementPattern = replacement.movementPattern
            )
        }
    }

    /** Sposta una seduta nel calendario senza rigenerare il piano. */
    suspend fun moveWorkoutDay(dayId: Long, newDayIndex: Int): Boolean = db.withTransaction {
        if (newDayIndex !in 0..6) return@withTransaction false
        val day = dayDao.getById(dayId) ?: return@withTransaction false
        val siblings = dayDao.getForMesocycle(day.mesocycleId)
        val target = siblings.firstOrNull { it.dayIndex == newDayIndex && it.id != dayId }
        if (target != null) {
            dayDao.insertAll(listOf(day.copy(dayIndex = 7), target.copy(dayIndex = day.dayIndex)))
        }
        dayDao.insertAll(listOf(day.copy(dayIndex = newDayIndex)))
        true
    }

    /** Registra uno spostamento temporaneo senza alterare il piano storico. */
    suspend fun saveScheduleOverride(
        dayId: Long,
        targetDayIndex: Int,
        weekStartEpochDay: Long,
        permanent: Boolean,
        note: String = ""
    ): Boolean = db.withTransaction {
        if (targetDayIndex !in 0..6) return@withTransaction false
        val day = dayDao.getById(dayId) ?: return@withTransaction false
        val mesocycle = mesoDao.getForPlan(planDao.getActive()?.id ?: 0L).firstOrNull { it.id == day.mesocycleId }
            ?: return@withTransaction false
        val plan = planDao.getById(mesocycle.planId) ?: return@withTransaction false
        val sibling = dayDao.getForMesocycle(day.mesocycleId)
            .firstOrNull { it.dayIndex == targetDayIndex && it.id != dayId }
        if (!permanent) {
            scheduleOverrideDao.deleteTemporary(dayId, weekStartEpochDay)
            sibling?.let { scheduleOverrideDao.deleteTemporary(it.id, weekStartEpochDay) }
            sibling?.let {
                scheduleOverrideDao.insert(
                    com.fitcoach.app.data.local.entity.WorkoutScheduleOverrideEntity(
                        planId = plan.id,
                        workoutDayId = it.id,
                        weekStartEpochDay = weekStartEpochDay,
                        targetDayIndex = day.dayIndex,
                        permanent = false,
                        note = "Scambio settimana corrente"
                    )
                )
            }
        } else {
            // Una modifica permanente scambia anche il giorno occupato, evitando due sedute nello stesso giorno.
            scheduleOverrideDao.deletePermanent(dayId)
            sibling?.let {
                scheduleOverrideDao.deletePermanent(it.id)
                moveWorkoutDay(it.id, day.dayIndex)
                scheduleOverrideDao.insert(
                    com.fitcoach.app.data.local.entity.WorkoutScheduleOverrideEntity(
                        planId = plan.id,
                        workoutDayId = it.id,
                        weekStartEpochDay = weekStartEpochDay,
                        targetDayIndex = day.dayIndex,
                        permanent = true,
                        note = "Scambio permanente"
                    )
                )
            }
            moveWorkoutDay(dayId, targetDayIndex)
        }
        scheduleOverrideDao.insert(
            com.fitcoach.app.data.local.entity.WorkoutScheduleOverrideEntity(
                planId = plan.id,
                workoutDayId = dayId,
                weekStartEpochDay = weekStartEpochDay,
                targetDayIndex = targetDayIndex,
                permanent = permanent,
                note = note
            )
        )
        true
    }

    suspend fun updateTarget(plannedId: Long, weight: Double?, reps: Int?) =
        plannedDao.updateTarget(plannedId, weight, reps)

    /** Aggiunge un esercizio alla seduta corrente senza rigenerare il programma. */
    suspend fun addExerciseToDay(dayId: Long, exercise: Exercise): Boolean = db.withTransaction {
        val day = dayDao.getById(dayId) ?: return@withTransaction false
        if (day.isRestDay) return@withTransaction false
        val current = plannedDao.getForDay(dayId)
        plannedDao.insertAll(
            listOf(
                PlannedExerciseEntity(
                    workoutDayId = dayId,
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    primaryMuscle = exercise.primaryMuscle,
                    orderIndex = (current.maxOfOrNull { it.orderIndex } ?: -1) + 1,
                    sets = exercise.defaultSets,
                    minReps = exercise.minReps,
                    maxReps = exercise.maxReps,
                    restSeconds = exercise.restSeconds,
                    targetRir = exercise.targetRir,
                    technique = exercise.technique,
                    category = exercise.category,
                    selectionReason = "Aggiunto manualmente dall'utente.",
                    restReason = "Recupero standard dell'esercizio.",
                    movementPattern = exercise.movementPattern
                )
            )
        )
        true
    }

    suspend fun removeExercise(plannedId: Long) {
        plannedDao.getById(plannedId)?.let { plannedDao.delete(it) }
        freeExerciseDao.deleteById(plannedId)
    }

    suspend fun updateExerciseTemplate(
        plannedId: Long,
        sets: Int,
        minReps: Int,
        maxReps: Int,
        restSeconds: Int,
        targetRir: Int,
        technique: com.fitcoach.app.domain.model.IntensityTechnique
    ): Boolean {
        val current = plannedDao.getById(plannedId)
        if (sets !in 1..20 || minReps !in 1..50 || maxReps !in minReps..50 ||
            restSeconds !in 15..900 || targetRir !in 0..5
        ) return false
        if (current == null) {
            return freeExerciseDao.updateTemplate(
                plannedId, sets, minReps, maxReps, restSeconds, targetRir, technique.name
            ) > 0
        }
        plannedDao.update(
            current.copy(
                sets = sets,
                minReps = minReps,
                maxReps = maxReps,
                restSeconds = restSeconds,
                targetRir = targetRir,
                intensityTechnique = technique
            )
        )
        return true
    }

    suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>) = db.withTransaction {
        val existing = plannedDao.getForDay(dayId).associateBy { it.id }
        orderedIds.distinct().forEachIndexed { index, id ->
            if (id in existing) plannedDao.updateOrder(id, index)
        }
    }

    // ---------------- Sessioni ----------------

    suspend fun startSession(workoutDay: WorkoutDay, epochDay: Long): Long {
        val existing = sessionDao.getForDay(epochDay)
        if (existing != null) {
            // Una seduta completata resta immutabile: evitare duplicati e riaperture implicite.
            return existing.id
        }
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


    suspend fun startFreeSession(epochDay: Long): Long {
        val existing = sessionDao.getForDay(epochDay)
        if (existing != null) {
            return existing.id
        }
        return sessionDao.insert(
            WorkoutSessionEntity(
                planId = planDao.getActive()?.id ?: 0L,
                workoutDayId = null,
                dayTitle = "Allenamento libero",
                dateEpochDay = epochDay,
                status = SessionStatus.IN_PROGRESS,
                startedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getFreeExercises(sessionId: Long): List<PlannedExercise> =
        freeExerciseDao.getForSession(sessionId).map { it.toPlannedExercise() }

    suspend fun addExerciseToFreeSession(sessionId: Long, exercise: Exercise): PlannedExercise {
        val current = freeExerciseDao.getForSession(sessionId)
        val item = FreeSessionExerciseEntity(
            sessionId = sessionId,
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            primaryMuscle = exercise.primaryMuscle,
            orderIndex = current.size,
            sets = exercise.defaultSets,
            minReps = exercise.minReps,
            maxReps = exercise.maxReps,
            restSeconds = exercise.restSeconds,
            targetRir = exercise.targetRir,
            technique = exercise.technique,
            category = exercise.category,
            movementPattern = exercise.movementPattern
        )
        val id = freeExerciseDao.insert(item)
        return item.copy(id = id).toPlannedExercise()
    }

    suspend fun convertFreeSessionToRoutine(sessionId: Long): Boolean = db.withTransaction {
        val exercises = freeExerciseDao.getForSession(sessionId)
        if (exercises.isEmpty()) return@withTransaction false
        val plan = planDao.getActive() ?: return@withTransaction false
        val meso = mesoDao.getForPlan(plan.id).firstOrNull() ?: return@withTransaction false
        val rest = dayDao.getForMesocycle(meso.id).firstOrNull { it.isRestDay }
            ?: return@withTransaction false
        val newDayId = dayDao.insertAll(listOf(rest.copy(title = "Routine da sessione libera", isRestDay = false))).first()
        plannedDao.insertAll(exercises.map {
            PlannedExerciseEntity(
                workoutDayId = newDayId,
                exerciseId = it.exerciseId,
                exerciseName = it.exerciseName,
                primaryMuscle = it.primaryMuscle,
                orderIndex = it.orderIndex,
                sets = it.sets,
                minReps = it.minReps,
                maxReps = it.maxReps,
                restSeconds = it.restSeconds,
                targetRir = it.targetRir,
                technique = it.technique,
                category = it.category,
                selectionReason = "Convertito da sessione libera.",
                restReason = "Recupero standard dell'esercizio.",
                movementPattern = it.movementPattern
            )
        })
        true
    }

    private fun FreeSessionExerciseEntity.toPlannedExercise() = PlannedExercise(
        id = id,
        workoutDayId = -sessionId,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        primaryMuscle = primaryMuscle,
        orderIndex = orderIndex,
        sets = sets,
        minReps = minReps,
        maxReps = maxReps,
        restSeconds = restSeconds,
        targetRir = targetRir,
        technique = technique,
        category = category,
        movementPattern = movementPattern,
        selectionReason = "Aggiunto durante una sessione libera.",
        restReason = "Recupero standard dell'esercizio."
    )

    /**
     * Riapre le sessioni marcate COMPLETED ma prive di serie allenanti valide:
     * erano sessioni chiuse per errore. Non elimina dati e lascia intatte le
     * sessioni realmente completate. Ritorna il numero di sessioni riaperte.
     */
    suspend fun repairInvalidCompletedSessions(): Int {
        var repaired = 0
        sessionDao.getAll().filter { it.status == SessionStatus.COMPLETED }.forEach { session ->
            val validSets = setDao.getForSession(session.id).count {
                it.completed && !it.isWarmup && it.actualReps > 0 && it.weightKg >= 0.0
            }
            if (validSets == 0) {
                sessionDao.update(
                    session.copy(status = SessionStatus.IN_PROGRESS, completedAt = null)
                )
                repaired++
            }
        }
        return repaired
    }

    suspend fun getSessionForDay(epochDay: Long): WorkoutSession? {
        repairInvalidCompletedSessions()
        val entity = sessionDao.getForDay(epochDay) ?: return null
        return entity.toDomainWithSets()
    }

    suspend fun getRecentSessions(limit: Int = 20): List<WorkoutSession> {
        repairInvalidCompletedSessions()
        return sessionDao.getRecent(limit).map { it.toDomainWithSets() }
    }

    suspend fun getSessionById(id: Long): WorkoutSession? {
        val entity = sessionDao.getById(id) ?: return null
        return entity.toDomainWithSets()
    }

    val sessionsFlow: Flow<List<WorkoutSession>> = sessionDao.observeAll().map { list ->
        list.map { it.toDomainWithSets() }
    }

    suspend fun saveSet(sessionId: Long, set: WorkoutSet): Long {
        val session = sessionDao.getById(sessionId) ?: return 0L
        if (session.status != SessionStatus.IN_PROGRESS || !set.completed ||
            set.actualReps <= 0 || set.weightKg < 0.0 || set.setIndex < 0
        ) return 0L
        val existing = setDao.getForSession(sessionId).firstOrNull {
            it.exerciseId == set.exerciseId && it.setIndex == set.setIndex
        }
        return setDao.insert(
            WorkoutSetEntity(
                id = existing?.id ?: set.id,
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
    }

    suspend fun completeSession(
        sessionId: Long,
        durationMinutes: Int?,
        perceivedEffort: Double?,
        sleepQuality: Int?,
        painReported: Boolean,
        painNotes: String
    ): Boolean = db.withTransaction {
        val entity = sessionDao.getById(sessionId) ?: return@withTransaction false
        if (entity.status != SessionStatus.IN_PROGRESS) return@withTransaction false
        val validSets = setDao.getForSession(sessionId).filter {
            it.completed && !it.isWarmup && it.actualReps > 0 && it.weightKg >= 0.0
        }
        if (validSets.isEmpty()) return@withTransaction false
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
        true
    }

    /**
     * Chiude una seduta avviata e poi abbandonata senza serie valide.
     *
     * Serve al caso reale segnalato: l'utente apre l'allenamento, magari non
     * completa nulla e non riesce piu a uscire dalla seduta (resta IN_PROGRESS
     * per sempre). La seduta viene marcata [SessionStatus.SKIPPED] e non
     * [SessionStatus.COMPLETED]: non e un allenamento svolto, quindi non deve
     * contare nel volume settimanale ne generare performance. Se esistono serie
     * valide la sessione viene COMPLETATA come prima.
     */
    suspend fun abandonSession(sessionId: Long): Boolean = db.withTransaction {
        val entity = sessionDao.getById(sessionId) ?: return@withTransaction false
        if (entity.status != SessionStatus.IN_PROGRESS) return@withTransaction false
        val validSets = setDao.getForSession(sessionId).count {
            it.completed && !it.isWarmup && it.actualReps > 0 && it.weightKg >= 0.0
        }
        if (validSets > 0) {
            sessionDao.update(
                entity.copy(status = SessionStatus.COMPLETED, completedAt = System.currentTimeMillis())
            )
            aggregatePerformance(sessionId)
        } else {
            sessionDao.update(
                entity.copy(status = SessionStatus.SKIPPED, completedAt = System.currentTimeMillis())
            )
        }
        true
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

    /** Tutte le performance raggruppate per nome esercizio (per l'analisi adattiva). */
    suspend fun getPerformanceByExercise(): Map<String, List<ExercisePerformance>> =
        perfDao.getAll().map { it.toDomain() }.groupBy { it.exerciseName }

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
            version = entity.version,
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

    /**
     * Mesociclo di riferimento per la persistenza: registra durata, focus e
     * settimana di deload. Non pianifica la progressione (che appartiene al
     * Coach AI): descrive solo l'inquadramento del blocco corrente.
     */
    private fun defaultMesocycle(planId: Long, profile: UserProfile, weeks: Int = 6): Mesocycle {
        val focus = when (profile.goal) {
            Goal.HYPERTROPHY -> "Ipertrofia"
            Goal.CUT -> "Definizione (mantenimento forza)"
            Goal.RECOMP -> "Ricomposizione"
            Goal.MAINTENANCE -> "Mantenimento"
            Goal.STRENGTH -> "Forza"
        }
        return Mesocycle(
            planId = planId,
            name = "Mesociclo $focus",
            focus = focus,
            weekCount = weeks,
            startWeekIndex = 0,
            deloadWeekIndex = when (profile.experience) {
                TrainingExperience.BEGINNER -> null
                else -> weeks - 1
            },
            isDeload = false,
            progressionModel = "Double Progression"
        )
    }
}
