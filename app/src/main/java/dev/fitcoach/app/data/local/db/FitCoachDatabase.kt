package dev.fitcoach.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
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
    }
}
