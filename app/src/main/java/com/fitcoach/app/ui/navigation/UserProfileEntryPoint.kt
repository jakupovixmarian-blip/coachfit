package com.fitcoach.app.ui.navigation

import com.fitcoach.app.data.repository.UserRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Punto d'accesso Hilt per decidere la destinazione iniziale prima che il grafo
 * di navigazione esista: `startDestination` va scelto quando il `NavHost` viene
 * composto, quindi non e' possibile leggere il profilo da un ViewModel.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserProfileEntryPoint {
    fun userRepository(): UserRepository
}
