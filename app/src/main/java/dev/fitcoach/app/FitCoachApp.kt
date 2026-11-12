package dev.fitcoach.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.fitcoach.app.data.bootstrap.CatalogInitializer
import javax.inject.Inject

@HiltAndroidApp
class FitCoachApp : Application() {

    @Inject lateinit var catalogInitializer: CatalogInitializer

    override fun onCreate() {
        super.onCreate()
        catalogInitializer.initialize()
    }
}
