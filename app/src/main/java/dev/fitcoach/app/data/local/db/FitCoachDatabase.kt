package dev.fitcoach.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import dev.fitcoach.app.data.local.dao.AiConfigDao
import dev.fitcoach.app.data.local.dao.CoachChatDao
import dev.fitcoach.app.data.local.dao.CoachDao
import dev.fitcoach.app.data.local.dao.DiaryDao
import dev.fitcoach.app.data.local.dao.ExerciseDao
import dev.fitcoach.app.data.local.dao.FoodDao
import dev.fitcoach.app.data.local.dao.KnowledgeDao
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
        AiConfigEntity::class,
        CoachMessageEntity::class,
        KnowledgeNoteEntity::class
    ],
    version = 3,
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
    abstract fun coachChatDao(): CoachChatDao
    abstract fun knowledgeDao(): KnowledgeDao
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

        /**
         * Adds the coach chat, the knowledge notes the coach reasons on, the
         * per-exercise rest timer and the photo analysis column.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS coach_message (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "role TEXT NOT NULL, content TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, actionSummary TEXT NOT NULL DEFAULT '')"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_coach_message_createdAt ON coach_message(createdAt)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS knowledge_note (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "topic TEXT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, " +
                        "source TEXT NOT NULL, builtIn INTEGER NOT NULL, " +
                        "enabled INTEGER NOT NULL DEFAULT 1, createdAt INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_note_topic ON knowledge_note(topic)")

                db.execSQL("ALTER TABLE photo ADD COLUMN analysis TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE photo ADD COLUMN analysisAt INTEGER")

                db.execSQL("ALTER TABLE exercise ADD COLUMN defaultRestSeconds INTEGER NOT NULL DEFAULT 90")
            }
        }

        /** Every migration, in order, so the database builder can never miss one. */
        val ALL: Array<Migration> get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
