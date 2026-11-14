package com.fitcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fitcoach.app.data.local.entity.V2AnamnesisAnswerEntity
import com.fitcoach.app.data.local.entity.V2AnamnesisSessionEntity

@Dao
abstract class V2AnamnesisDao {
    @Query("SELECT * FROM v2_anamnesis_sessions ORDER BY updatedAt DESC LIMIT 1")
    abstract suspend fun latestSession(): V2AnamnesisSessionEntity?

    @Query("SELECT * FROM v2_anamnesis_answers WHERE sessionId = :sessionId")
    abstract suspend fun answers(sessionId: String): List<V2AnamnesisAnswerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSession(session: V2AnamnesisSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAnswers(answers: List<V2AnamnesisAnswerEntity>)

    @Query("DELETE FROM v2_anamnesis_answers WHERE sessionId = :sessionId")
    protected abstract suspend fun deleteAnswers(sessionId: String)

    @Transaction
    open suspend fun replace(session: V2AnamnesisSessionEntity, answers: List<V2AnamnesisAnswerEntity>) {
        insertSession(session)
        deleteAnswers(session.id)
        insertAnswers(answers)
    }
}
