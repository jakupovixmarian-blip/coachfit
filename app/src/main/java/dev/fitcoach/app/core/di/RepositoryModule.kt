package dev.fitcoach.app.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.fitcoach.app.data.ai.AiGatewayImpl
import dev.fitcoach.app.data.backup.BackupRepositoryImpl
import dev.fitcoach.app.data.repository.*
import dev.fitcoach.app.domain.repository.*
import dev.fitcoach.app.domain.usecase.AiGateway
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds @Singleton
    abstract fun bindMeasurementRepository(impl: MeasurementRepositoryImpl): MeasurementRepository

    @Binds @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds @Singleton
    abstract fun bindMealPlanRepository(impl: MealPlanRepositoryImpl): MealPlanRepository

    @Binds @Singleton
    abstract fun bindDiaryRepository(impl: DiaryRepositoryImpl): DiaryRepository

    @Binds @Singleton
    abstract fun bindShoppingRepository(impl: ShoppingRepositoryImpl): ShoppingRepository

    @Binds @Singleton
    abstract fun bindCoachRepository(impl: CoachRepositoryImpl): CoachRepository

    @Binds @Singleton
    abstract fun bindCoachChatRepository(impl: CoachChatRepositoryImpl): CoachChatRepository

    @Binds @Singleton
    abstract fun bindKnowledgeRepository(impl: KnowledgeRepositoryImpl): KnowledgeRepository

    @Binds @Singleton
    abstract fun bindAiConfigRepository(impl: AiConfigRepositoryImpl): AiConfigRepository

    @Binds @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds @Singleton
    abstract fun bindAiGateway(impl: AiGatewayImpl): AiGateway
}
