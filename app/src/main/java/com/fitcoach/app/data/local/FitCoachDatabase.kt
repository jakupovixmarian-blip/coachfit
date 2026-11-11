package com.fitcoach.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fitcoach.app.data.local.dao.AiSettingsDao
import com.fitcoach.app.data.local.dao.CheckInDao
import com.fitcoach.app.data.local.dao.ChatDao
import com.fitcoach.app.data.local.dao.DiaryEntryDao
import com.fitcoach.app.data.local.dao.ExerciseDao
import com.fitcoach.app.data.local.dao.ExercisePerformanceDao
import com.fitcoach.app.data.local.dao.FoodDao
import com.fitcoach.app.data.local.dao.MealDao
import com.fitcoach.app.data.local.dao.MealEntryDao
import com.fitcoach.app.data.local.dao.MesocycleDao
import com.fitcoach.app.data.local.dao.NotificationDao
import com.fitcoach.app.data.local.dao.NutritionDayDao
import com.fitcoach.app.data.local.dao.NutritionPlanDao
import com.fitcoach.app.data.local.dao.PlanVersionDao
import com.fitcoach.app.data.local.dao.PlannedExerciseDao
import com.fitcoach.app.data.local.dao.ProgramChangeDao
import com.fitcoach.app.data.local.dao.ProgressDao
import com.fitcoach.app.data.local.dao.ScientificSourceDao
import com.fitcoach.app.data.local.dao.ShoppingItemDao
import com.fitcoach.app.data.local.dao.SubstitutionLogDao
import com.fitcoach.app.data.local.dao.UserPreferencesDao
import com.fitcoach.app.data.local.dao.UserProfileDao
import com.fitcoach.app.data.local.dao.WorkoutDayDao
import com.fitcoach.app.data.local.dao.WorkoutPlanDao
import com.fitcoach.app.data.local.dao.WorkoutSessionDao
import com.fitcoach.app.data.local.dao.WorkoutSetDao
import com.fitcoach.app.data.local.entity.AiProviderSettingsEntity
import com.fitcoach.app.data.local.entity.CheckInEntity
import com.fitcoach.app.data.local.entity.ChatMessageEntity
import com.fitcoach.app.data.local.entity.CircumferenceRecordEntity
import com.fitcoach.app.data.local.entity.DiaryEntryEntity
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
import com.fitcoach.app.data.local.entity.ShoppingItemEntity
import com.fitcoach.app.data.local.entity.SubstitutionLogEntity
import com.fitcoach.app.data.local.entity.UserPreferencesEntity
import com.fitcoach.app.data.local.entity.UserProfileEntity
import com.fitcoach.app.data.local.entity.WeightRecordEntity
import com.fitcoach.app.data.local.entity.WorkoutDayEntity
import com.fitcoach.app.data.local.entity.WorkoutPlanEntity
import com.fitcoach.app.data.local.entity.WorkoutSessionEntity
import com.fitcoach.app.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        UserProfileEntity::class,
        UserPreferencesEntity::class,
        FoodItemEntity::class,
        NutritionPlanEntity::class,
        NutritionDayEntity::class,
        MealEntity::class,
        MealEntryEntity::class,
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        MesocycleEntity::class,
        WorkoutDayEntity::class,
        PlannedExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        ExercisePerformanceEntity::class,
        WeightRecordEntity::class,
        CircumferenceRecordEntity::class,
        ProgressRecordEntity::class,
        CheckInEntity::class,
        ProgramChangeEntity::class,
        ChatMessageEntity::class,
        AiProviderSettingsEntity::class,
        NotificationRecordEntity::class,
        ScientificSourceEntity::class,
        PlanVersionEntity::class,
        DiaryEntryEntity::class,
        ShoppingItemEntity::class,
        SubstitutionLogEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FitCoachDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun foodDao(): FoodDao
    abstract fun nutritionPlanDao(): NutritionPlanDao
    abstract fun nutritionDayDao(): NutritionDayDao
    abstract fun mealDao(): MealDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun mesocycleDao(): MesocycleDao
    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun plannedExerciseDao(): PlannedExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun exercisePerformanceDao(): ExercisePerformanceDao
    abstract fun progressDao(): ProgressDao
    abstract fun checkInDao(): CheckInDao
    abstract fun programChangeDao(): ProgramChangeDao
    abstract fun chatDao(): ChatDao
    abstract fun aiSettingsDao(): AiSettingsDao
    abstract fun notificationDao(): NotificationDao
    abstract fun scientificSourceDao(): ScientificSourceDao
    abstract fun planVersionDao(): PlanVersionDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun substitutionLogDao(): SubstitutionLogDao

    companion object {
        const val DB_NAME = "fitcoach.db"
    }
}
