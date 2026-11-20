package com.fitcoach.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.setupDataStore by preferencesDataStore(name = "fitcoach_setup")
private val AI_SETUP_SKIPPED = booleanPreferencesKey("ai_setup_skipped")

/**
 * Ricorda che l'utente ha scelto "Configura più tardi" nel setup iniziale.
 *
 * Senza questo flag l'app riproporrebbe il setup a ogni avvio, perche' la
 * configurazione manca ancora: un blocco in uscita, non un promemoria. Con il
 * flag il setup si chiede una volta sola e resta raggiungibile da Impostazioni.
 */
@Singleton
class SetupStateStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun isAiSetupSkipped(): Boolean =
        context.setupDataStore.data.first()[AI_SETUP_SKIPPED] ?: false

    suspend fun setAiSetupSkipped(skipped: Boolean) {
        context.setupDataStore.edit { it[AI_SETUP_SKIPPED] = skipped }
    }
}
