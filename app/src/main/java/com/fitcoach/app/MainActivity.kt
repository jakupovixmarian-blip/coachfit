package com.fitcoach.app

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fitcoach.app.data.repository.NotificationRepository
import com.fitcoach.app.data.repository.PreferencesRepository
import com.fitcoach.app.notifications.NotificationScheduler
import com.fitcoach.app.notifications.ReminderReceiver
import com.fitcoach.app.ui.navigation.FitCoachNavHost
import com.fitcoach.app.ui.theme.FitCoachTheme
import com.fitcoach.app.ui.theme.HyperionBackground
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingRoute by mutableStateOf<String?>(null)

    @Inject lateinit var notificationScheduler: NotificationScheduler
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var notificationRepository: NotificationRepository

    private val rearmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
        pendingRoute = intent.deepLinkRoute()
        setContent {
            FitCoachTheme {
                HyperionBackground {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color.Transparent,
                    ) {
                        val navController = rememberNavController()
                        FitCoachNavHost(
                            navController = navController,
                            pendingRoute = pendingRoute,
                            onRouteConsumed = { pendingRoute = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.deepLinkRoute()
    }

    /**
     * Gli allarmi si perdono quando il sistema forza l'app in stop, quando il
     * permesso per gli allarmi esatti viene concesso dopo il primo avvio o per
     * le ottimizzazioni batteria dei produttori. Riaromarli a ogni ritorno in
     * primo piano evita che le notifiche restino mute finche' l'app non viene
     * chiusa e riaperta del tutto.
     */
    override fun onResume() {
        super.onResume()
        rearmScope.launch {
            runCatching {
                val prefs = preferencesRepository.getOrCreate()
                val records = notificationRepository.getAll()
                notificationScheduler.scheduleAll(prefs, records)
            }
        }
    }

    private fun Intent.deepLinkRoute(): String? =
        getStringExtra(ReminderReceiver.EXTRA_DEEP_LINK_ROUTE)

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }
}
