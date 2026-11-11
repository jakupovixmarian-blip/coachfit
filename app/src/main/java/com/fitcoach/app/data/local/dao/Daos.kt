package com.fitcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fitcoach.app.data.local.entity.AiProviderSettingsEntity
import com.fitcoach.app.data.local.entity.CheckInEntity
import com.fitcoach.app.data.local.entity.ChatMessageEntity
import com.fitcoach.app.data.local.entity.CircumferenceRecordEntity
import com.fitcoach.app.data.local.entity.ExerciseEntity
import com.fitcoach.app.data.local.entity.ExercisePerformanceEntity
import com.fitcoach.app.data.local.entity.FoodItemEntity
import com.fitcoach.app.data.local.entity.MealEntity
import com.fitcoach.app.data.local.entity.MealEntryEntity
import com.fitcoach.app.data.local.entity.MesocycleEntity
import com.fitcoach.app.data.local.entity.NotificationRecordEntity
import com.fitcoach.app.data.local.entity.NutritionDayEntity
import com.fitcoach.app.data.local.entity.NutritionPlanEntity
import com.fitcoach.app.data.local.entity.PlannedExerciseEntity
import com.fitcoach.app.data.local.entity.PlanVersionEntity
import com.fitcoach.app.data.local.entity.ProgramChangeEntity
import com.fitcoach.app.data.local.entity.ProgressRecordEntity
import com.fitcoach.app.data.local.entity.ScientificSourceEntity
import com.fitcoach.app.data.local.entity.UserPreferencesEntity
import com.fitcoach.app.data.local.entity.UserProfileEntity
import com.fitcoach.app.data.local.entity.WeightRecordEntity
import com.fitcoach.app.data.local.entity.WorkoutDayEntity
import com.fitcoach.app.data.local.entity.WorkoutPlanEntity
import com.fitcoach.app.data.local.entity.WorkoutSessionEntity
import com.fitcoach.app.data.local.entity.WorkoutSetEntity
import com.fitcoach.app.domain.model.ChangeDomain
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserProfileEntity)

    @Query("SELECT COUNT(*) FROM user_profile")
    suspend fun count(): Int
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE profileId = 1")
    fun observe(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE profileId = 1")
    suspend fun get(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserPreferencesEntity)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_database ORDER BY name")
    fun observeAll(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_database ORDER BY name")
    suspend fun getAll(): List<FoodItemEntity>

    @Query("SELECT * FROM food_database WHERE id = :id")
    suspend fun getById(id: Long): FoodItemEntity?

    @Query("SELECT * FROM food_database WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): FoodItemEntity?

    @Query("SELECT * FROM food_database WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name")
    suspend fun search(query: String): List<FoodItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FoodItemEntity): Long

    @Query("SELECT COUNT(*) FROM food_database")
    suspend fun count(): Int
}

@Dao
interface NutritionPlanDao {
    @Query("SELECT * FROM nutrition_plans WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun observeActive(): Flow<NutritionPlanEntity?>

    @Query("SELECT * FROM nutrition_plans WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActive(): NutritionPlanEntity?

    @Query("SELECT * FROM nutrition_plans ORDER BY createdAt DESC")
    suspend fun getAll(): List<NutritionPlanEntity>

    @Query("SELECT * FROM nutrition_plans WHERE id = :id")
    suspend fun getById(id: Long): NutritionPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: NutritionPlanEntity): Long

    @Update
    suspend fun update(plan: NutritionPlanEntity)

    @Query("UPDATE nutrition_plans SET isActive = 0")
    suspend fun deactivateAll()

    @Query("DELETE FROM nutrition_plans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM nutrition_plans")
    suspend fun count(): Int
}

@Dao
interface NutritionDayDao {
    @Query("SELECT * FROM nutrition_days WHERE planId = :planId ORDER BY dayIndex")
    fun observeForPlan(planId: Long): Flow<List<NutritionDayEntity>>

    @Query("SELECT * FROM nutrition_days WHERE planId = :planId ORDER BY dayIndex")
    suspend fun getForPlan(planId: Long): List<NutritionDayEntity>

    @Query("SELECT * FROM nutrition_days WHERE id = :id")
    suspend fun getById(id: Long): NutritionDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<NutritionDayEntity>): List<Long>

    @Query("UPDATE nutrition_days SET targetKcal = :kcal, targetProtein = :protein, targetCarbs = :carbs, targetFat = :fat WHERE id = :id")
    suspend fun updateTargets(id: Long, kcal: Double, protein: Double, carbs: Double, fat: Double)

    @Query("UPDATE nutrition_days SET workoutDayIndex = :workoutDayIndex WHERE id = :id")
    suspend fun updateWorkoutDayIndex(id: Long, workoutDayIndex: Int?)

    @Query("DELETE FROM nutrition_days WHERE planId = :planId")
    suspend fun deleteForPlan(planId: Long)
}

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE nutritionDayId = :dayId ORDER BY time")
    fun observeForDay(dayId: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE nutritionDayId = :dayId ORDER BY time")
    suspend fun getForDay(dayId: Long): List<MealEntity>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getById(id: Long): MealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meals: List<MealEntity>): List<Long>

    @Query("UPDATE meals SET confirmed = :confirmed WHERE id = :id")
    suspend fun setConfirmed(id: Long, confirmed: Boolean)

    @Query("DELETE FROM meals WHERE nutritionDayId = :dayId")
    suspend fun deleteForDay(dayId: Long)
}

@Dao
interface MealEntryDao {
    @Query("SELECT * FROM meal_entries WHERE mealId = :mealId")
    fun observeForMeal(mealId: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE mealId = :mealId")
    suspend fun getForMeal(mealId: Long): List<MealEntryEntity>

    @Query("SELECT * FROM meal_entries WHERE mealId IN (:mealIds)")
    suspend fun getForMeals(mealIds: List<Long>): List<MealEntryEntity>

    @Query("SELECT * FROM meal_entries WHERE id = :id")
    suspend fun getById(id: Long): MealEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<MealEntryEntity>): List<Long>

    @Query("UPDATE meal_entries SET foodItemId = :foodItemId, foodName = :foodName, grams = :grams, kcal = :kcal, protein = :protein, carbs = :carbs, fat = :fat WHERE id = :id")
    suspend fun replaceEntry(
        id: Long, foodItemId: Long, foodName: String, grams: Double,
        kcal: Double, protein: Double, carbs: Double, fat: Double
    )

    @Query("UPDATE meal_entries SET grams = :grams, kcal = :kcal, protein = :protein, carbs = :carbs, fat = :fat WHERE id = :id")
    suspend fun updateEntry(id: Long, grams: Double, kcal: Double, protein: Double, carbs: Double, fat: Double)

    @Delete
    suspend fun delete(entry: MealEntryEntity)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}

@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutPlanEntity?>

    @Query("SELECT * FROM workout_plans WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActive(): WorkoutPlanEntity?

    @Query("SELECT * FROM workout_plans ORDER BY createdAt DESC")
    suspend fun getAll(): List<WorkoutPlanEntity>

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    suspend fun getById(id: Long): WorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutPlanEntity): Long

    @Query("UPDATE workout_plans SET isActive = 0")
    suspend fun deactivateAll()

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM workout_plans")
    suspend fun count(): Int
}

@Dao
interface MesocycleDao {
    @Query("SELECT * FROM mesocycles WHERE planId = :planId ORDER BY startWeekIndex")
    fun observeForPlan(planId: Long): Flow<List<MesocycleEntity>>

    @Query("SELECT * FROM mesocycles WHERE planId = :planId ORDER BY startWeekIndex")
    suspend fun getForPlan(planId: Long): List<MesocycleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mesocycle: MesocycleEntity): Long

    @Query("DELETE FROM mesocycles WHERE planId = :planId")
    suspend fun deleteForPlan(planId: Long)
}

@Dao
interface WorkoutDayDao {
    @Query("SELECT * FROM workout_days WHERE mesocycleId = :mesocycleId ORDER BY dayIndex")
    fun observeForMesocycle(mesocycleId: Long): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM workout_days WHERE mesocycleId = :mesocycleId ORDER BY dayIndex")
    suspend fun getForMesocycle(mesocycleId: Long): List<WorkoutDayEntity>

    @Query("SELECT * FROM workout_days WHERE mesocycleId IN (:mesocycleIds) ORDER BY dayIndex")
    suspend fun getForMesocycles(mesocycleIds: List<Long>): List<WorkoutDayEntity>

    @Query("SELECT * FROM workout_days WHERE id = :id")
    suspend fun getById(id: Long): WorkoutDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<WorkoutDayEntity>): List<Long>

    @Query("DELETE FROM workout_days WHERE mesocycleId = :mesocycleId")
    suspend fun deleteForMesocycle(mesocycleId: Long)
}

@Dao
interface PlannedExerciseDao {
    @Query("SELECT * FROM planned_exercises WHERE workoutDayId = :dayId ORDER BY orderIndex")
    fun observeForDay(dayId: Long): Flow<List<PlannedExerciseEntity>>

    @Query("SELECT * FROM planned_exercises WHERE workoutDayId = :dayId ORDER BY orderIndex")
    suspend fun getForDay(dayId: Long): List<PlannedExerciseEntity>

    @Query("SELECT * FROM planned_exercises WHERE workoutDayId IN (:dayIds) ORDER BY workoutDayId, orderIndex")
    suspend fun getForDays(dayIds: List<Long>): List<PlannedExerciseEntity>

    @Query("SELECT * FROM planned_exercises WHERE id = :id")
    suspend fun getById(id: Long): PlannedExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlannedExerciseEntity>): List<Long>

    @Query("UPDATE planned_exercises SET targetWeightKg = :weight, targetReps = :reps WHERE id = :id")
    suspend fun updateTarget(id: Long, weight: Double?, reps: Int?)

    @Query("UPDATE planned_exercises SET exerciseId = :exerciseId, exerciseName = :name, technique = :technique WHERE id = :id")
    suspend fun substituteExercise(id: Long, exerciseId: Long, name: String, technique: String)

    @Query("UPDATE planned_exercises SET sets = :sets WHERE id = :id")
    suspend fun updateSets(id: Long, sets: Int)

    @Update
    suspend fun update(item: PlannedExerciseEntity)

    @Delete
    suspend fun delete(item: PlannedExerciseEntity)
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateEpochDay DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY dateEpochDay DESC")
    suspend fun getAll(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE dateEpochDay = :epochDay ORDER BY id DESC LIMIT 1")
    suspend fun getForDay(epochDay: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSessionEntity): Long

    @Update
    suspend fun update(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE dateEpochDay BETWEEN :from AND :to")
    suspend fun getBetween(from: Long, to: Long): List<WorkoutSessionEntity>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int
}

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    fun observeForSession(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    suspend fun getForSession(sessionId: Long): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<WorkoutSetEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: WorkoutSetEntity): Long

    @Delete
    suspend fun delete(set: WorkoutSetEntity)
}

@Dao
interface ExercisePerformanceDao {
    @Query("SELECT * FROM exercise_performance WHERE exerciseId = :exerciseId ORDER BY dateEpochDay DESC")
    suspend fun getForExercise(exerciseId: Long): List<ExercisePerformanceEntity>

    @Query("SELECT * FROM exercise_performance WHERE exerciseId = :exerciseId ORDER BY dateEpochDay DESC LIMIT 1")
    suspend fun getLatest(exerciseId: Long): ExercisePerformanceEntity?

    @Query("SELECT * FROM exercise_performance ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<ExercisePerformanceEntity>>

    @Query("SELECT * FROM exercise_performance ORDER BY dateEpochDay DESC")
    suspend fun getAll(): List<ExercisePerformanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(perf: ExercisePerformanceEntity): Long
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM weight_records ORDER BY dateEpochDay DESC")
    fun observeWeights(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records ORDER BY dateEpochDay DESC")
    suspend fun getWeights(): List<WeightRecordEntity>

    @Query("SELECT * FROM weight_records ORDER BY dateEpochDay DESC LIMIT 1")
    suspend fun getLatestWeight(): WeightRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(record: WeightRecordEntity): Long

    @Delete
    suspend fun deleteWeight(record: WeightRecordEntity)

    @Query("SELECT * FROM circumference_records ORDER BY dateEpochDay DESC")
    fun observeCircumferences(): Flow<List<CircumferenceRecordEntity>>

    @Query("SELECT * FROM circumference_records ORDER BY dateEpochDay DESC")
    suspend fun getCircumferences(): List<CircumferenceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCircumference(record: CircumferenceRecordEntity): Long

    @Query("SELECT * FROM progress_records ORDER BY dateEpochDay DESC")
    fun observeProgress(): Flow<List<ProgressRecordEntity>>

    @Query("SELECT * FROM progress_records ORDER BY dateEpochDay DESC")
    suspend fun getProgress(): List<ProgressRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(record: ProgressRecordEntity): Long
}

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY weekStartEpochDay DESC")
    fun observeAll(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins ORDER BY weekStartEpochDay DESC")
    suspend fun getAll(): List<CheckInEntity>

    @Query("SELECT * FROM check_ins ORDER BY weekStartEpochDay DESC LIMIT 1")
    suspend fun getLatest(): CheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: CheckInEntity): Long
}

@Dao
interface ProgramChangeDao {
    @Query("SELECT * FROM program_changes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProgramChangeEntity>>

    @Query("SELECT * FROM program_changes WHERE status = 'PROPOSED' ORDER BY createdAt DESC")
    fun observeProposed(): Flow<List<ProgramChangeEntity>>

    @Query("SELECT * FROM program_changes ORDER BY createdAt DESC")
    suspend fun getAll(): List<ProgramChangeEntity>

    @Query("SELECT * FROM program_changes WHERE id = :id")
    suspend fun getById(id: Long): ProgramChangeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(change: ProgramChangeEntity): Long

    @Update
    suspend fun update(change: ProgramChangeEntity)

    @Query("UPDATE program_changes SET status = :status, appliedAt = :appliedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: com.fitcoach.app.domain.model.ChangeStatus, appliedAt: Long?)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clear()

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AiSettingsDao {
    @Query("SELECT * FROM ai_provider_settings WHERE id = 1")
    fun observe(): Flow<AiProviderSettingsEntity?>

    @Query("SELECT * FROM ai_provider_settings WHERE id = 1")
    suspend fun get(): AiProviderSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AiProviderSettingsEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_records ORDER BY kind, triggerTime")
    fun observeAll(): Flow<List<NotificationRecordEntity>>

    @Query("SELECT * FROM notification_records WHERE enabled = 1")
    suspend fun getEnabled(): List<NotificationRecordEntity>

    @Query("SELECT * FROM notification_records")
    suspend fun getAll(): List<NotificationRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NotificationRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: NotificationRecordEntity)

    @Query("UPDATE notification_records SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE notification_records SET triggerTime = :time WHERE id = :id")
    suspend fun setTime(id: Long, time: String)
}

@Dao
interface ScientificSourceDao {
    @Query("SELECT * FROM scientific_sources ORDER BY kind, topic")
    fun observeAll(): Flow<List<ScientificSourceEntity>>

    @Query("SELECT * FROM scientific_sources ORDER BY kind, topic")
    suspend fun getAll(): List<ScientificSourceEntity>

    @Query("SELECT COUNT(*) FROM scientific_sources")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScientificSourceEntity>)
}

@Dao
interface PlanVersionDao {
    @Query("SELECT * FROM plan_versions WHERE domain = :domain ORDER BY createdAt DESC")
    suspend fun getForDomain(domain: ChangeDomain): List<PlanVersionEntity>

    @Query("SELECT * FROM plan_versions ORDER BY createdAt DESC")
    suspend fun getAll(): List<PlanVersionEntity>

    @Query("SELECT * FROM plan_versions WHERE id = :id")
    suspend fun getById(id: Long): PlanVersionEntity?

    @Query("SELECT MAX(version) FROM plan_versions WHERE domain = :domain")
    suspend fun latestVersion(domain: ChangeDomain): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlanVersionEntity): Long

    @Query("UPDATE plan_versions SET isActive = 0 WHERE domain = :domain")
    suspend fun clearActiveForDomain(domain: ChangeDomain)

    @Query("UPDATE plan_versions SET isActive = 0")
    suspend fun clearActive()
}
