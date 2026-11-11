package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toDomain
import com.fitcoach.app.data.local.dao.ExerciseDao
import com.fitcoach.app.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val dao: ExerciseDao
) {
    val exercisesFlow: Flow<List<Exercise>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Exercise> = dao.getAll().map { it.toDomain() }

    suspend fun getById(id: Long): Exercise? = dao.getById(id)?.toDomain()

    suspend fun getByName(name: String): Exercise? = dao.getByName(name)?.toDomain()

    suspend fun count(): Int = dao.count()
}
