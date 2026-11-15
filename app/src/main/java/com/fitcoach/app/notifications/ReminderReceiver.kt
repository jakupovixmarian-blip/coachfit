package com.fitcoach.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fitcoach.app.MainActivity
import com.fitcoach.app.R

/**
 * Riceve gli allarmi dei promemoria e mostra la notifica.
 * Supporta azioni: Posticipa (snooze) e Conferma (apre l'app).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_SNOOZE) {
            handleSnooze(context, intent)
            return
        }

        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "FitCoach"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: CHANNEL_MEALS
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)

        showNotification(context, requestCode, title, body, channel, kind)

        // Riprogramma l'allarme per il giorno/settimana successivo.
        reschedule(context, intent, requestCode)
    }

    private fun showNotification(
        context: Context,
        requestCode: Int,
        title: String,
        body: String,
        channel: String,
        kind: String
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val contentIntent = PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context, requestCode + 1000,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_CHANNEL, channel)
                putExtra(EXTRA_KIND, kind)
                putExtra(EXTRA_REQUEST_CODE, requestCode)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.notif_action_defer), snoozeIntent)
            .addAction(0, context.getString(R.string.notif_action_confirm), contentIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                manager.notify(requestCode, notification)
            } catch (e: SecurityException) {
                // Permesso notifiche non concesso: ignora silenziosamente.
            }
        } else {
            manager.notify(requestCode, notification)
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: CHANNEL_MEALS
        val kind = intent.getStringExtra(EXTRA_KIND) ?: ""
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)

        // Posticipo di 15 minuti tramite WorkManager one-shot (semplice e affidabile).
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val trigger = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        val retry = PendingIntent.getBroadcast(
            context, requestCode + 2000,
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_KIND, kind)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_CHANNEL, channel)
                putExtra(EXTRA_REQUEST_CODE, requestCode)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, trigger, retry)
    }

    private fun reschedule(context: Context, intent: Intent, requestCode: Int) {
        val scheduler = NotificationScheduler(context)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: CHANNEL_MEALS
        val kind = intent.getStringExtra(EXTRA_KIND) ?: ""
        val repeatDaily = intent.getBooleanExtra(EXTRA_REPEAT_DAILY, false)
        val weekly = intent.getBooleanExtra(EXTRA_WEEKLY, false)
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, 0)

        // L'allarme originale non e ripetitivo: lo ripianifichiamo per il periodo successivo.
        if (repeatDaily || weekly) {
            // Riprogramma a orario fisso per il giorno successivo / settimana successiva.
            val hour = intent.getIntExtra(EXTRA_HOUR, -1)
            val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
            if (hour >= 0 && minute >= 0) {
                val time = "%02d:%02d".format(hour, minute)
                if (weekly) {
                    scheduler.scheduleWeekly(requestCode, dayOfWeek, time, kind, title, body, channel)
                } else {
                    scheduler.scheduleDaily(requestCode, time, kind, title, body, channel)
                }
            }
        }
    }

    companion object {
        const val CHANNEL_MEALS = "fitcoach_meals"
        const val CHANNEL_WORKOUT = "fitcoach_workout"
        const val CHANNEL_HYDRATION = "fitcoach_hydration"
        const val CHANNEL_CHECKIN = "fitcoach_checkin"
        const val CHANNEL_TIMER = "fitcoach_timer"

        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_CHANNEL = "extra_channel"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
        const val EXTRA_REPEAT_DAILY = "extra_repeat_daily"
        const val EXTRA_WEEKLY = "extra_weekly"
        const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"

        const val ACTION_SNOOZE = "com.fitcoach.app.action.SNOOZE"
        const val SNOOZE_MINUTES = 15
    }
}
