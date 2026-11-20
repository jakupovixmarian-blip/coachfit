package com.fitcoach.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fitcoach.app.data.repository.NotificationRepository
import com.fitcoach.app.diagnostics.CrashLog
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.data.repository.SeedInitializer
import com.fitcoach.app.notifications.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
// `open` per consentire alle Application di test di seminare uno stato iniziale.
open class FitCoachApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var seedInitializer: SeedInitializer

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        notificationScheduler.createChannels()
        appScope.launch {
            // Un errore qui (seme, notifiche, permessi allarme) non deve chiudere
            // l'app all'avvio: va registrato e l'avvio prosegue.
            runCatching {
                seedInitializer.seedIfNeeded()
                val prefs = preferencesRepository.getOrCreate()
                val records = notificationRepository.getAll()
                notificationScheduler.scheduleAll(prefs, records)
            }.onFailure { Log.e("FitCoachApp", "Avvio non completato", it) }
        }
    }
}
