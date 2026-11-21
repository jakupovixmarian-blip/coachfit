package com.fitcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitcoach.app.data.local.entity.SavedRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRecipeDao {

    @Query("SELECT * FROM saved_recipes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedRecipeEntity>>

    @Query("SELECT * FROM saved_recipes ORDER BY createdAt DESC")
    suspend fun getAll(): List<SavedRecipeEntity>

    @Insert
    suspend fun insert(recipe: SavedRecipeEntity): Long

    @Query("DELETE FROM saved_recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM saved_recipes")
    suspend fun count(): Int
}
