package dev.fitcoach.app.data.repository

import dev.fitcoach.app.data.local.db.FitCoachDatabase
import dev.fitcoach.app.data.local.mapper.toDomain
import dev.fitcoach.app.data.local.mapper.toEntity
import dev.fitcoach.app.domain.model.LoggedSet
import dev.fitcoach.app.domain.model.RoutineExercise
import dev.fitcoach.app.domain.usecase.ProgressionPort
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room adapter for the progression approval flow.
 *
 * History comes from the sets actually logged for the exercise, so a proposal is
 * always built on real data; nothing here writes unless the user approved.
 */
@Singleton
class ProgressionPortImpl @Inject constructor(
    private val db: FitCoachDatabase
) : ProgressionPort {

    private val exerciseDao = db.routineExerciseDao()
    private val setDao = db.loggedSetDao()

    override suspend fun routineExercise(id: Long): RoutineExercise? =
        exerciseDao.getById(id)?.toDomain()

    override suspend fun history(exerciseId: Long): List<LoggedSet> =
        setDao.getRecentForExercise(exerciseId).map { it.toDomain() }

    override suspend fun updateRoutineExercise(exercise: RoutineExercise) {
        exerciseDao.update(exercise.toEntity())
    }
}
