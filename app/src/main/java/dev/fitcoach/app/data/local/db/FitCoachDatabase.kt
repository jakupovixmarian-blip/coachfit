package dev.fitcoach.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import dev.fitcoach.app.data.local.dao.AiConfigDao
import dev.fitcoach.app.data.local.dao.CoachDao
import dev.fitcoach.app.data.local.dao.DiaryDao
import dev.fitcoach.app.data.local.dao.ExerciseDao
import dev.fitcoach.app.data.local.dao.FoodDao
import dev.fitcoach.app.data.local.dao.LoggedSetDao
import dev.fitcoach.app.data.local.dao.MealItemDao
import dev.fitcoach.app.data.local.dao.MealPlanDao
import dev.fitcoach.app.data.local.dao.MeasurementDao
import dev.fitcoach.app.data.local.dao.OnboardingDao
import dev.fitcoach.app.data.local.dao.PhotoDao
import dev.fitcoach.app.data.local.dao.PlanDayDao
import dev.fitcoach.app.data.local.dao.ProfileDao
import dev.fitcoach.app.data.local.dao.RoutineDao
import dev.fitcoach.app.data.local.dao.RoutineExerciseDao
import dev.fitcoach.app.data.local.dao.SessionDao
import dev.fitcoach.app.data.local.dao.ShoppingDao
import dev.fitcoach.app.data.local.entity.AiConfigEntity
import dev.fitcoach.app.data.local.entity.CoachProposalEntity
import dev.fitcoach.app.data.local.entity.DiaryEntryEntity
import dev.fitcoach.app.data.local.entity.ExerciseEntity
import dev.fitcoach.app.data.local.entity.FoodEntity
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

@Database(
    entities = [
        ProfileEntity::class,
        OnboardingAnswerEntity::class,
        MeasurementEntity::class,
        PhotoEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        LoggedSetEntity::class,
        FoodEntity::class,
        MealPlanEntity::class,
        PlanDayEntity::class,
        MealItemEntity::class,
        DiaryEntryEntity::class,
        ShoppingItemEntity::class,
        CoachProposalEntity::class,
        AiConfigEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FitCoachDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun onboardingDao(): OnboardingDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun photoDao(): PhotoDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun loggedSetDao(): LoggedSetDao
    abstract fun foodDao(): FoodDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun planDayDao(): PlanDayDao
    abstract fun mealItemDao(): MealItemDao
    abstract fun diaryDao(): DiaryDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun coachDao(): CoachDao
    abstract fun aiConfigDao(): AiConfigDao

    companion object {
        const val NAME = "fitcoach.db"

        /**
         * Adds the program grouping columns to the routine table. Existing rows
         * keep programId = 0, so they remain standalone single sessions.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine ADD COLUMN programId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE routine ADD COLUMN dayIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE routine ADD COLUMN dayLabel TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_programId ON routine(programId)")
            }
        }
    }
}
