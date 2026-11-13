package dev.fitcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import dev.fitcoach.app.data.local.entity.AiConfigEntity
import dev.fitcoach.app.data.local.entity.CoachMessageEntity
import dev.fitcoach.app.data.local.entity.CoachProposalEntity
import dev.fitcoach.app.data.local.entity.DiaryEntryEntity
import dev.fitcoach.app.data.local.entity.ExerciseEntity
import dev.fitcoach.app.data.local.entity.FoodEntity
import dev.fitcoach.app.data.local.entity.KnowledgeNoteEntity
import dev.fitcoach.app.data.local.entity.LoggedSetEntity
import dev.fitcoach.app.data.local.entity.MealItemEntity
import dev.fitcoach.app.data.local.entity.MealPlanEntity
import dev.fitcoach.app.data.local.entity.MeasurementEntity
import dev.fitcoach.app.data.local.entity.OnboardingAnswerEntity
import dev.fitcoach.app.data.local.entity.PhotoEntity
import dev.fitcoach.app.data.local.entity.PlanDayEntity
import dev.fitcoach.app.data.local.entity.ProfileEntity
import dev.fitcoach.app.data.local.entity.RoutineEntity
import dev.fitcoach.app.data.local.entity.RoutineExerciseEntity
import dev.fitcoach.app.data.local.entity.ShoppingItemEntity
import dev.fitcoach.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(entity: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clear()
}

@Dao
interface OnboardingDao {
    @Query("SELECT * FROM onboarding_answer")
    fun observeAll(): Flow<List<OnboardingAnswerEntity>>

    @Query("SELECT * FROM onboarding_answer WHERE `key` = :key")
    suspend fun get(key: String): OnboardingAnswerEntity?

    @Upsert
    suspend fun upsert(answer: OnboardingAnswerEntity)

    @Query("DELETE FROM onboarding_answer")
    suspend fun clear()
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurement ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurement WHERE type = :type ORDER BY dateEpochDay ASC")
    fun observeByType(type: String): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurement ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAll(): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MeasurementEntity): Long

    @Update
    suspend fun update(entity: MeasurementEntity)

    @Query("DELETE FROM measurement WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM measurement")
    suspend fun clear()
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photo ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAll(): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhotoEntity): Long

    @Query("UPDATE photo SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    @Query("UPDATE photo SET analysis = :analysis, analysisAt = :at WHERE id = :id")
    suspend fun updateAnalysis(id: Long, analysis: String, at: Long)

    @Query("DELETE FROM photo WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM photo")
    suspend fun clear()
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise ORDER BY name ASC")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<ExerciseEntity>)

    @Update
    suspend fun update(entity: ExerciseEntity)

    @Query("UPDATE exercise SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("UPDATE exercise SET defaultRestSeconds = :seconds WHERE id = :id")
    suspend fun setDefaultRest(id: Long, seconds: Int)

    @Query("DELETE FROM exercise WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: Long)

    @Query("DELETE FROM exercise")
    suspend fun clear()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<RoutineEntity?>

    @Query("SELECT * FROM routine WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): RoutineEntity?

    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT * FROM routine WHERE programId = :programId AND programId != 0 ORDER BY dayIndex ASC")
    fun observeProgram(programId: Long): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine WHERE programId = :programId AND programId != 0 ORDER BY dayIndex ASC")
    suspend fun getProgram(programId: Long): List<RoutineEntity>

    @Query("SELECT programId FROM routine WHERE isActive = 1 AND programId != 0 LIMIT 1")
    suspend fun getActiveProgramId(): Long?

    @Query("SELECT programId FROM routine WHERE isActive = 1 AND programId != 0 LIMIT 1")
    fun observeActiveProgramId(): Flow<Long?>

    @Query("SELECT COALESCE(MAX(programId), 0) FROM routine")
    suspend fun maxProgramId(): Long

    @Query("SELECT * FROM routine")
    suspend fun getAll(): List<RoutineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RoutineEntity): Long

    @Update
    suspend fun update(entity: RoutineEntity)

    @Query("UPDATE routine SET isActive = 0")
    suspend fun deactivateAll()

    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM routine WHERE programId = :programId AND programId != 0")
    suspend fun deleteProgram(programId: Long)

    @Query("DELETE FROM routine")
    suspend fun clear()
}

@Dao
interface RoutineExerciseDao {
    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun observeForRoutine(routineId: Long): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getForRoutine(routineId: Long): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercise WHERE id = :id")
    suspend fun getById(id: Long): RoutineExerciseEntity?

    @Query("SELECT * FROM routine_exercise")
    suspend fun getAll(): List<RoutineExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RoutineExerciseEntity): Long

    @Update
    suspend fun update(entity: RoutineExerciseEntity)

    @Query("DELETE FROM routine_exercise WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM routine_exercise WHERE routineId = :routineId")
    suspend fun deleteForRoutine(routineId: Long)

    @Query("DELETE FROM routine_exercise")
    suspend fun clear()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM workout_session ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session WHERE dateEpochDay = :epochDay ORDER BY id ASC")
    fun observeByDate(epochDay: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session WHERE dateEpochDay >= :from AND dateEpochDay <= :to ORDER BY dateEpochDay ASC")
    fun observeRange(from: Long, to: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getInProgress(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_session ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAll(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_session WHERE status = 'COMPLETED' ORDER BY dateEpochDay DESC, id DESC LIMIT 1")
    suspend fun getLastCompleted(): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkoutSessionEntity): Long

    @Update
    suspend fun update(entity: WorkoutSessionEntity)

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM workout_session WHERE routineId IN (:routineIds) AND status = 'PLANNED'")
    suspend fun deletePlannedForRoutines(routineIds: List<Long>)

    @Query("DELETE FROM workout_session")
    suspend fun clear()
}

@Dao
interface LoggedSetDao {
    @Query("SELECT * FROM logged_set WHERE sessionId = :sessionId ORDER BY orderIndex ASC, setIndex ASC")
    fun observeForSession(sessionId: Long): Flow<List<LoggedSetEntity>>

    @Query("SELECT * FROM logged_set WHERE sessionId = :sessionId ORDER BY orderIndex ASC, setIndex ASC")
    suspend fun getForSession(sessionId: Long): List<LoggedSetEntity>

    @Query("SELECT * FROM logged_set WHERE exerciseId = :exerciseId AND completed = 1 ORDER BY loggedAt DESC LIMIT 40")
    suspend fun getRecentForExercise(exerciseId: Long): List<LoggedSetEntity>

    @Query("SELECT * FROM logged_set WHERE exerciseId = :exerciseId AND completed = 1 ORDER BY loggedAt ASC")
    suspend fun getAllForExercise(exerciseId: Long): List<LoggedSetEntity>

    @Query("SELECT * FROM logged_set")
    suspend fun getAll(): List<LoggedSetEntity>

    @Query("SELECT MAX(loggedAt) FROM logged_set WHERE exerciseId = :exerciseId AND completed = 1")
    suspend fun lastLoggedAt(exerciseId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LoggedSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LoggedSetEntity>)

    @Update
    suspend fun update(entity: LoggedSetEntity)

    @Query("DELETE FROM logged_set WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM logged_set WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Query("DELETE FROM logged_set")
    suspend fun clear()
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food ORDER BY name ASC")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food ORDER BY name ASC")
    suspend fun getAll(): List<FoodEntity>

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM food WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): FoodEntity?

    @Query("SELECT COUNT(*) FROM food")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FoodEntity>)

    @Update
    suspend fun update(entity: FoodEntity)

    @Query("DELETE FROM food WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM food")
    suspend fun clear()

    /**
     * Deduplication key for imports: the same record from the same source must
     * update in place instead of being inserted again.
     */
    @Query("SELECT * FROM food WHERE source = :source AND sourceId = :sourceId LIMIT 1")
    suspend fun bySourceRef(source: String, sourceId: String): FoodEntity?

    @Query("SELECT * FROM food WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit")
    suspend fun searchByName(query: String, limit: Int): List<FoodEntity>

    @Query(
        "UPDATE food SET verification = :verification, limitations = :limitations, " +
            "reliability = :reliability, acquiredAt = :acquiredAt WHERE id = :id"
    )
    suspend fun setProvenance(
        id: Long,
        verification: String,
        limitations: String,
        reliability: Double,
        acquiredAt: Long
    )
}

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plan ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plan WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<MealPlanEntity?>

    @Query("SELECT * FROM meal_plan WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): MealPlanEntity?

    @Query("SELECT * FROM meal_plan WHERE id = :id")
    suspend fun getById(id: Long): MealPlanEntity?

    @Query("SELECT * FROM meal_plan")
    suspend fun getAll(): List<MealPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MealPlanEntity): Long

    @Update
    suspend fun update(entity: MealPlanEntity)

    @Query("UPDATE meal_plan SET isActive = 0")
    suspend fun deactivateAll()

    @Query("DELETE FROM meal_plan WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM meal_plan")
    suspend fun clear()
}

@Dao
interface PlanDayDao {
    @Query("SELECT * FROM plan_day WHERE planId = :planId ORDER BY dayIndex ASC")
    fun observeForPlan(planId: Long): Flow<List<PlanDayEntity>>

    @Query("SELECT * FROM plan_day WHERE planId = :planId ORDER BY dayIndex ASC")
    suspend fun getForPlan(planId: Long): List<PlanDayEntity>

    @Query("SELECT * FROM plan_day")
    suspend fun getAll(): List<PlanDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlanDayEntity): Long

    @Query("DELETE FROM plan_day WHERE planId = :planId")
    suspend fun deleteForPlan(planId: Long)

    @Query("DELETE FROM plan_day")
    suspend fun clear()
}

@Dao
interface MealItemDao {
    @Query("SELECT * FROM meal_item WHERE planDayId = :planDayId ORDER BY mealSlot ASC, id ASC")
    fun observeForDay(planDayId: Long): Flow<List<MealItemEntity>>

    @Query("SELECT * FROM meal_item WHERE planDayId = :planDayId ORDER BY mealSlot ASC, id ASC")
    suspend fun getForDay(planDayId: Long): List<MealItemEntity>

    @Query("SELECT * FROM meal_item")
    suspend fun getAll(): List<MealItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MealItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MealItemEntity>)

    @Update
    suspend fun update(entity: MealItemEntity)

    @Query("DELETE FROM meal_item WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM meal_item WHERE planDayId = :planDayId")
    suspend fun deleteForDay(planDayId: Long)

    @Query("DELETE FROM meal_item")
    suspend fun clear()
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entry WHERE dateEpochDay = :epochDay ORDER BY loggedAt ASC")
    fun observeByDate(epochDay: Long): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entry WHERE dateEpochDay = :epochDay ORDER BY loggedAt ASC")
    suspend fun getByDate(epochDay: Long): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entry ORDER BY dateEpochDay DESC, loggedAt ASC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entry")
    suspend fun getAll(): List<DiaryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DiaryEntryEntity): Long

    @Update
    suspend fun update(entity: DiaryEntryEntity)

    @Query("DELETE FROM diary_entry WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM diary_entry WHERE dateEpochDay = :epochDay AND mealSlot = :slot AND planItemId = :planItemId")
    suspend fun deleteByPlanItem(epochDay: Long, slot: String, planItemId: Long)

    @Query("DELETE FROM diary_entry")
    suspend fun clear()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_item WHERE weekStartEpochDay = :weekStart ORDER BY category ASC, name ASC")
    fun observeWeek(weekStart: Long): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_item WHERE weekStartEpochDay = :weekStart ORDER BY category ASC, name ASC")
    suspend fun getWeek(weekStart: Long): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_item")
    suspend fun getAll(): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ShoppingItemEntity>)

    @Update
    suspend fun update(entity: ShoppingItemEntity)

    @Query("UPDATE shopping_item SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_item WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_item WHERE weekStartEpochDay = :weekStart")
    suspend fun deleteWeek(weekStart: Long)

    @Query("DELETE FROM shopping_item")
    suspend fun clear()
}

@Dao
interface CoachDao {
    @Query("SELECT * FROM coach_proposal ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CoachProposalEntity>>

    @Query("SELECT * FROM coach_proposal ORDER BY createdAt DESC")
    suspend fun getAll(): List<CoachProposalEntity>

    @Query("SELECT * FROM coach_proposal WHERE id = :id")
    suspend fun getById(id: Long): CoachProposalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CoachProposalEntity): Long

    @Update
    suspend fun update(entity: CoachProposalEntity)

    @Query("DELETE FROM coach_proposal WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM coach_proposal")
    suspend fun clear()
}

@Dao
interface CoachChatDao {
    @Query("SELECT * FROM coach_message ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<CoachMessageEntity>>

    @Query("SELECT * FROM coach_message ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<CoachMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CoachMessageEntity): Long

    @Query("DELETE FROM coach_message WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM coach_message")
    suspend fun clear()
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_note ORDER BY topic ASC, title ASC")
    fun observeAll(): Flow<List<KnowledgeNoteEntity>>

    @Query("SELECT * FROM knowledge_note WHERE enabled = 1 ORDER BY topic ASC, title ASC")
    suspend fun getEnabled(): List<KnowledgeNoteEntity>

    @Query("SELECT * FROM knowledge_note ORDER BY topic ASC, title ASC")
    suspend fun getAll(): List<KnowledgeNoteEntity>

    @Query("SELECT COUNT(*) FROM knowledge_note WHERE builtIn = 1")
    suspend fun builtInCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KnowledgeNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<KnowledgeNoteEntity>)

    @Update
    suspend fun update(entity: KnowledgeNoteEntity)

    @Query("DELETE FROM knowledge_note WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM knowledge_note WHERE builtIn = 0")
    suspend fun clearUserNotes()

    @Query("DELETE FROM knowledge_note")
    suspend fun clear()
}

@Dao
interface AiConfigDao {
    @Query("SELECT * FROM ai_config WHERE id = 1")
    fun observe(): Flow<AiConfigEntity?>

    @Query("SELECT * FROM ai_config WHERE id = 1")
    suspend fun get(): AiConfigEntity?

    @Upsert
    suspend fun upsert(entity: AiConfigEntity)
}
