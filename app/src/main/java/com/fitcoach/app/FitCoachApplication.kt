package com.fitcoach.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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
class FitCoachApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var seedInitializer: SeedInitializer

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationScheduler.createChannels()
        appScope.launch {
            seedInitializer.seedIfNeeded()
            val prefs = preferencesRepository.getOrCreate()
            notificationScheduler.scheduleAll(prefs)
        }
    }
}
