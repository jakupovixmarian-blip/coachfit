package dev.fitcoach.app.core.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RestTimerState(
    val exerciseId: Long = 0L,
    val exerciseName: String = "",
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val running: Boolean = false
) {
    val active: Boolean get() = totalSeconds > 0 && (running || remainingSeconds > 0)
}

/**
 * Foreground service that keeps the rest timer alive with the screen off. The
 * remaining time is derived from the realtime clock, so it stays correct if the
 * process is recreated.
 */
class RestTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var endRealtime: Long = 0L

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val action = intent?.action
        // Stopping an idle service must not promote it to foreground.
        if (action == ACTION_STOP) {
            stopTimer()
            stopSelf()
            return START_NOT_STICKY
        }
        // Pause/resume/skip arriving while no timer is active are no-ops.
        if (action != ACTION_START && !_state.value.active) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        when (action) {
            ACTION_START -> startTimer(
                intent.getIntExtra(EXTRA_SECONDS, 60),
                intent.getLongExtra(EXTRA_EXERCISE_ID, 0L),
                intent.getStringExtra(EXTRA_NAME).orEmpty()
            )
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_SKIP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int, exerciseId: Long, name: String) {
        endRealtime = SystemClock.elapsedRealtime() + seconds * 1000L
        _state.value = RestTimerState(
            exerciseId = exerciseId,
            exerciseName = name,
            totalSeconds = seconds,
            remainingSeconds = seconds,
            running = true
        )
        launchTicker()
    }

    private fun launchTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (_state.value.remainingSeconds > 0 && _state.value.running) {
                delay(500)
                val remaining = ((endRealtime - SystemClock.elapsedRealtime()) / 1000L).toInt().coerceAtLeast(0)
                _state.value = _state.value.copy(remainingSeconds = remaining, running = remaining > 0)
                updateNotification()
            }
            if (_state.value.remainingSeconds <= 0) onFinished()
        }
    }

    private fun pauseTimer() {
        if (!_state.value.active) return
        tickJob?.cancel()
        _state.value = _state.value.copy(running = false)
        updateNotification()
    }

    private fun resumeTimer() {
        if (!_state.value.active) return
        endRealtime = SystemClock.elapsedRealtime() + _state.value.remainingSeconds * 1000L
        _state.value = _state.value.copy(running = true)
        launchTicker()
    }

    private fun stopTimer() {
        tickJob?.cancel()
        endRealtime = 0L
        _state.value = RestTimerState()
    }

    private fun onFinished() {
        _state.value = _state.value.copy(running = false, remainingSeconds = 0)
        updateNotification()
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val s = _state.value
        val text = if (s.active) "${s.remainingSeconds}s - ${s.exerciseName}" else "Recupero completato"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recupero")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                manager?.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Timer recupero", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "fitcoach_rest_timer"
        const val NOTIFICATION_ID = 4711
        const val ACTION_START = "dev.fitcoach.app.START"
        const val ACTION_PAUSE = "dev.fitcoach.app.PAUSE"
        const val ACTION_RESUME = "dev.fitcoach.app.RESUME"
        const val ACTION_SKIP = "dev.fitcoach.app.SKIP"
        const val ACTION_STOP = "dev.fitcoach.app.STOP"
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_EXERCISE_ID = "exerciseId"
        const val EXTRA_NAME = "name"

        private val _state = MutableStateFlow(RestTimerState())
        val state: StateFlow<RestTimerState> = _state.asStateFlow()

        fun start(context: Context, seconds: Int, exerciseId: Long, name: String) {
            startServiceCompat(
                context,
                Intent(context, RestTimerService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_SECONDS, seconds)
                    putExtra(EXTRA_EXERCISE_ID, exerciseId)
                    putExtra(EXTRA_NAME, name)
                }
            )
        }

        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun resume(context: Context) = send(context, ACTION_RESUME)
        fun skip(context: Context) = send(context, ACTION_SKIP)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            startServiceCompat(context, Intent(context, RestTimerService::class.java).apply { this.action = action })
        }

        private fun startServiceCompat(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
