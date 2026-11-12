package com.fitcoach.app.v2.domain

import com.fitcoach.app.v2.domain.anamnesis.AnamnesisStore
import com.fitcoach.app.v2.domain.anamnesis.RoomAnamnesisStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class V2DomainModule {
    @Binds
    abstract fun bindAnamnesisStore(store: RoomAnamnesisStore): AnamnesisStore
}
