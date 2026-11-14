package com.fitcoach.app.v2.ai

import com.fitcoach.app.v2.ai.provider.AndroidCredentialStore
import com.fitcoach.app.v2.ai.provider.CredentialStore
import com.fitcoach.app.v2.ai.transport.AiTransport
import com.fitcoach.app.v2.ai.transport.OkHttpAiTransport
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class V2AiModule {
    @Binds
    abstract fun bindAiTransport(transport: OkHttpAiTransport): AiTransport

    @Binds
    abstract fun bindCredentialStore(store: AndroidCredentialStore): CredentialStore
}
