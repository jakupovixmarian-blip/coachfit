package com.fitcoach.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fitcoach.app.data.repository.NotificationRepository
import com.fitcoach.app.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ripristina le notifiche programmate dopo il riavvio del dispositivo
 * o dopo un aggiornamento dell'app.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationScheduler: NotificationScheduler
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var notificationRepository: NotificationRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reschedule()
            } finally {
                // goAsync() puo restituire null: chiamare finish() su null
                // solleverebbe dentro la coroutine e chiuderebbe il processo.
                runCatching { pendingResult?.finish() }
            }
        }
    }

    /**
     * Ricalcola e riprogramma tutti i promemoria.
     *
     * Non propaga eccezioni: gira in un contesto senza UI (riavvio o
     * aggiornamento dell'app) e un errore non gestito qui chiude il processo,
     * cioe l'utente vede l'app andare in crash appena la apre.
     */
    internal suspend fun reschedule() {
        try {
            notificationScheduler.createChannels()
            val prefs = preferencesRepository.getOrCreate()
            val records = notificationRepository.getAll()
            notificationScheduler.scheduleAll(prefs, records)
        } catch (t: Throwable) {
            Log.e("BootReceiver", "Ripristino notifiche non riuscito", t)
        }
    }
}
