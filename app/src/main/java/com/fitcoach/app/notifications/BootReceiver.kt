package com.fitcoach.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.data.repository.UserRepository
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
    @Inject lateinit var userRepository: UserRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationScheduler.createChannels()
                val prefs = preferencesRepository.getOrCreate()
                notificationScheduler.scheduleAll(prefs)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
