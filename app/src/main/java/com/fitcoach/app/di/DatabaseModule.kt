package com.fitcoach.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.createActivePlanTriggers
import com.fitcoach.app.data.local.MIGRATION_1_2
import com.fitcoach.app.data.local.MIGRATION_2_3
import com.fitcoach.app.data.local.MIGRATION_3_4
import com.fitcoach.app.data.local.MIGRATION_4_5
import com.fitcoach.app.data.local.MIGRATION_5_6
import com.fitcoach.app.data.local.MIGRATION_6_7
import com.fitcoach.app.data.local.MIGRATION_7_8
import com.fitcoach.app.data.local.MIGRATION_8_9
import com.fitcoach.app.data.local.MIGRATION_9_10
import com.fitcoach.app.data.local.MIGRATION_10_11
import com.fitcoach.app.data.local.MIGRATION_11_12
import com.fitcoach.app.data.local.MIGRATION_12_13
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
import com.fitcoach.app.data.local.dao.V2AnamnesisDao
import com.fitcoach.app.data.local.dao.V2GenerationDao
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
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13
            )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    createActivePlanTriggers(db)
                }
            })
            // Nessun fallback distruttivo: aggiornare l'app non deve mai azzerare
            // profilo, piani e storico. Una migrazione mancante deve fallire in modo
            // esplicito (e va aggiunta), non cancellare i dati dell'utente.
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
    @Provides fun v2AnamnesisDao(db: FitCoachDatabase): V2AnamnesisDao = db.v2AnamnesisDao()
    @Provides fun v2GenerationDao(db: FitCoachDatabase): V2GenerationDao = db.v2GenerationDao()
}
