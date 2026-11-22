package com.fitcoach.app.domain.device

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fitcoach.app.MainActivity
import com.fitcoach.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tiene vivo il collegamento BLE quando l'app non e' in primo piano.
 *
 * Senza un foreground service Android sospende il processo pochi istanti dopo
 * lo spegnimento dello schermo: il GATT cade e la fascia cardio smette di
 * inviare. Il servizio non fa calcoli, mostra solo una notifica persistente
 * (obbligatoria per legge) e tiene in vita [BleSessionManager].
 */
@AndroidEntryPoint
class BleSessionService : Service() {

    companion object {
        const val CHANNEL_ID = "ble_session"
        const val NOTIFICATION_ID = 4211

        const val ACTION_START = "com.fitcoach.app.action.BLE_START"
        const val ACTION_STOP = "com.fitcoach.app.action.BLE_STOP"

        const val EXTRA_ADDRESS = "address"
        const val EXTRA_NAME = "name"
        const val EXTRA_KIND = "kind"

        private const val PREFS = "ble_session"
        private const val KEY_ADDRESS = "address"
        private const val KEY_NAME = "name"
        private const val KEY_KIND = "kind"

        /** Salva l'ultimo dispositivo per poter riprendere dopo un riavvio del processo. */
        private fun remember(context: Context, address: String, name: String, kind: DeviceKind) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_ADDRESS, address)
                .putString(KEY_NAME, name)
                .putString(KEY_KIND, kind.name)
                .apply()
        }

        private fun forget(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        }

        private data class Saved(val address: String, val name: String, val kind: DeviceKind)

        private fun recall(context: Context): Saved? {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val address = p.getString(KEY_ADDRESS, null) ?: return null
            val kind = p.getString(KEY_KIND, null)
                ?.let { runCatching { DeviceKind.valueOf(it) }.getOrNull() }
                ?: DeviceKind.UNKNOWN
            return Saved(address, p.getString(KEY_NAME, null) ?: "Dispositivo", kind)
        }

        /**
         * Avvia il servizio. Da Android 12 l'avvio in background solleva
         * ForegroundServiceStartNotAllowedException: qui si propaga come
         * esito, non come crash, cosi' la UI puo' spiegarlo.
         */
        fun start(context: Context, address: String, name: String, kind: DeviceKind): Boolean {
            val intent = Intent(context, BleSessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_KIND, kind.name)
            }
            return runCatching {
                remember(context, address, name, kind)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.isSuccess
        }

        /**
         * Riprende l'ultimo collegamento salvato, se c'era. Serve al riavvio del
         * telefono: i servizi non vengono riavviati da soli.
         */
        fun resumeIfSaved(context: Context) {
            val saved = recall(context) ?: return
            start(context, saved.address, saved.name, saved.kind)
        }

        fun stop(context: Context) {
            forget(context)
            runCatching {
                context.startService(
                    Intent(context, BleSessionService::class.java).apply { action = ACTION_STOP }
                )
            }
        }
    }

    @Inject
    lateinit var sessionManager: BleSessionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            forget(this)
            sessionManager.stop()
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        // Dopo un riavvio del processo Android richiama il servizio con un
        // intent nullo: senza i dati salvati il collegamento andrebbe perso.
        val saved = if (intent?.getStringExtra(EXTRA_ADDRESS) == null) recall(this) else null
        val address = intent?.getStringExtra(EXTRA_ADDRESS) ?: saved?.address
        val name = intent?.getStringExtra(EXTRA_NAME) ?: saved?.name ?: "Dispositivo"
        val kind = intent?.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { DeviceKind.valueOf(it) }.getOrNull() }
            ?: saved?.kind
            ?: DeviceKind.UNKNOWN

        if (address == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // La notifica va mostrata subito: Android chiude il servizio se non
        // diventa foreground entro pochi secondi dall'avvio.
        startForegroundCompat(buildNotification(name, "Collegamento in corso..."))
        sessionManager.start(address, name, kind)
        observeState(name)
        return START_STICKY
    }

    private fun observeState(fallbackName: String) {
        observeJob?.cancel()
        observeJob = scope.launch {
            sessionManager.state.collectLatest { state ->
                if (state.status == BleSessionStatus.IDLE) return@collectLatest
                val text = when (state.status) {
                    BleSessionStatus.CONNECTING -> "Collegamento in corso..."
                    BleSessionStatus.CONNECTED -> "Connesso"
                    BleSessionStatus.STREAMING -> "Ricezione dati attiva"
                    BleSessionStatus.RECONNECTING -> "Fuori portata, riprovo..."
                    BleSessionStatus.DISCONNECTED -> "Disconnesso"
                    BleSessionStatus.FAILED -> state.detail ?: "Collegamento non riuscito"
                    BleSessionStatus.IDLE -> return@collectLatest
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(
                    NOTIFICATION_ID,
                    buildNotification(state.name ?: fallbackName, text)
                )
            }
        }
    }

    private fun buildNotification(name: String, status: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = if (notificationsAllowed()) {
            status
        } else {
            "$status (concedi le notifiche per vedere questo stato)"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(name)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_ble_session),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_ble_session_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * Da Android 13 senza POST_NOTIFICATIONS la notifica non compare, ma il
     * servizio resta valido: si segnala nella notifica stessa invece di
     * rinunciare al collegamento.
     */
    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
