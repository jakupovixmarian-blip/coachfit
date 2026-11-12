package com.fitcoach.app.di

import android.content.Context
import androidx.room.Room
import com.fitcoach.data.RoomProfileRepository
import com.fitcoach.database.FitCoachDatabase
import com.fitcoach.database.ProfileDao
import com.fitcoach.domain.ProfileRepository
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
        Room.databaseBuilder(context, FitCoachDatabase::class.java, "fitcoach.db")
            .addMigrations(FitCoachDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideProfileDao(database: FitCoachDatabase): ProfileDao = database.profileDao()

    @Provides
    @Singleton
    fun provideProfileRepository(dao: ProfileDao): ProfileRepository = RoomProfileRepository(dao)
}
