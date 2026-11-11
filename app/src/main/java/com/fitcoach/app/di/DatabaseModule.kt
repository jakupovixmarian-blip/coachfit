package com.fitcoach.app.di

import android.content.Context
import androidx.room.Room
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.MIGRATION_1_2
import com.fitcoach.app.data.local.MIGRATION_2_3
import com.fitcoach.app.data.local.MIGRATION_3_4
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitCoachDatabase =
        Room.databaseBuilder(context, FitCoachDatabase::class.java, FitCoachDatabase.DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun userProfileDao(db: FitCoachDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun userPreferencesDao(db: FitCoachDatabase): UserPreferencesDao = db.userPreferencesDao()
    @Provides fun foodDao(db: FitCoachDatabase): FoodDao = db.foodDao()
    @Provides fun nutritionPlanDao(db: FitCoachDatabase): NutritionPlanDao = db.nutritionPlanDao()
    @Provides fun nutritionDayDao(db: FitCoachDatabase): NutritionDayDao = db.nutritionDayDao()
    @Provides fun mealDao(db: FitCoachDatabase): MealDao = db.mealDao()
    @Provides fun mealEntryDao(db: FitCoachDatabase): MealEntryDao = db.mealEntryDao()
    @Provides fun exerciseDao(db: FitCoachDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun workoutPlanDao(db: FitCoachDatabase): WorkoutPlanDao = db.workoutPlanDao()
    @Provides fun mesocycleDao(db: FitCoachDatabase): MesocycleDao = db.mesocycleDao()
    @Provides fun workoutDayDao(db: FitCoachDatabase): WorkoutDayDao = db.workoutDayDao()
    @Provides fun plannedExerciseDao(db: FitCoachDatabase): PlannedExerciseDao = db.plannedExerciseDao()
    @Provides fun workoutSessionDao(db: FitCoachDatabase): WorkoutSessionDao = db.workoutSessionDao()
    @Provides fun workoutSetDao(db: FitCoachDatabase): WorkoutSetDao = db.workoutSetDao()
    @Provides fun exercisePerformanceDao(db: FitCoachDatabase): ExercisePerformanceDao = db.exercisePerformanceDao()
    @Provides fun progressDao(db: FitCoachDatabase): ProgressDao = db.progressDao()
    @Provides fun checkInDao(db: FitCoachDatabase): CheckInDao = db.checkInDao()
    @Provides fun programChangeDao(db: FitCoachDatabase): ProgramChangeDao = db.programChangeDao()
    @Provides fun chatDao(db: FitCoachDatabase): ChatDao = db.chatDao()
    @Provides fun aiSettingsDao(db: FitCoachDatabase): AiSettingsDao = db.aiSettingsDao()
    @Provides fun notificationDao(db: FitCoachDatabase): NotificationDao = db.notificationDao()
    @Provides fun scientificSourceDao(db: FitCoachDatabase): ScientificSourceDao = db.scientificSourceDao()
    @Provides fun planVersionDao(db: FitCoachDatabase): PlanVersionDao = db.planVersionDao()
    @Provides fun diaryEntryDao(db: FitCoachDatabase): DiaryEntryDao = db.diaryEntryDao()
    @Provides fun shoppingItemDao(db: FitCoachDatabase): ShoppingItemDao = db.shoppingItemDao()
    @Provides fun substitutionLogDao(db: FitCoachDatabase): SubstitutionLogDao = db.substitutionLogDao()
}
