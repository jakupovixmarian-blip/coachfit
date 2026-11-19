package com.fitcoach.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fitcoach.app.MainActivity
import com.fitcoach.app.R
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.NutritionDiaryEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Riceve gli allarmi dei promemoria e mostra la notifica.
 * Azioni: Posticipa (snooze), Conferma (registra il pasto o apre l'app),
 * apertura della schermata pertinente tramite deep link.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var nutritionRepository: NutritionRepository
    @Inject lateinit var diaryRepository: NutritionDiaryRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_SNOOZE) {
            handleSnooze(context, intent)
            return
        }
        if (action == ACTION_CONFIRM) {
            handleConfirm(context, intent)
            return
        }

        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "FitCoach"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: CHANNEL_MEALS
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)

        val deferMinutes = intent.getIntExtra(EXTRA_DEFER_MINUTES, SNOOZE_MINUTES).coerceIn(5, 120)
        showNotification(context, requestCode, title, body, channel, kind, deferMinutes)

        // Riprogramma l'allarme per il giorno/settimana successivo.
        reschedule(context, intent, requestCode)
    }

    private fun showNotification(
        context: Context,
        requestCode: Int,
        title: String,
        body: String,
        channel: String,
        kind: String,
        deferMinutes: Int
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val contentIntent = deepLinkPendingIntent(context, kind, requestCode)

        val snoozeIntent = PendingIntent.getBroadcast(
            context, requestCode + 1000,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_CHANNEL, channel)
                putExtra(EXTRA_KIND, kind)
                putExtra(EXTRA_REQUEST_CODE, requestCode)
                putExtra(EXTRA_DEFER_MINUTES, deferMinutes)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confirmIntent = PendingIntent.getBroadcast(
            context, requestCode + 3000,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_CONFIRM
                putExtra(EXTRA_KIND, kind)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_CHANNEL, channel)
                putExtra(EXTRA_REQUEST_CODE, requestCode)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confirmLabel = when {
            kind.startsWith("MEAL_") -> context.getString(R.string.notif_action_confirm_meal)
            kind == "WORKOUT" -> context.getString(R.string.notif_action_confirm_workout)
            else -> context.getString(R.string.notif_action_confirm)
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.notif_action_defer), snoozeIntent)
            .addAction(0, confirmLabel, confirmIntent)
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

    private fun deepLinkPendingIntent(context: Context, kind: String, requestCode: Int): PendingIntent {
        val route = deepLinkRouteFor(kind)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Conferma reale: per i pasti registra il pasto previsto del giorno (spunta le
     * voci di piano dello slot) cosi il diario riflette quanto previsto; per gli
     * altri promemoria apre direttamente la schermata pertinente.
     */
    private fun handleConfirm(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND).orEmpty()
        if (!kind.startsWith("MEAL_")) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.cancel(intent.getIntExtra(EXTRA_REQUEST_CODE, 0))
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_DEEP_LINK_ROUTE, deepLinkRouteFor(kind))
                }
            )
            return
        }
        val slot = kind.removePrefix("MEAL_").let { name ->
            MealSlot.entries.firstOrNull { it.name == name }
        } ?: return
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val epochDay = NutritionDiaryRepository.todayEpochDay()
                val dayIndex = NutritionDiaryEngine.dayIndexFor(epochDay)
                val day = nutritionRepository.getActivePlan()?.days?.firstOrNull { it.dayIndex == dayIndex }
                val meal = day?.meals?.firstOrNull { it.slot == slot }
                meal?.entries?.forEach { entry ->
                    diaryRepository.confirmPlanEntry(entry, slot, meal.time, epochDay)
                }
                nutritionRepository.getActivePlan()?.days
                    ?.firstOrNull { it.dayIndex == dayIndex }
                    ?.meals?.firstOrNull { it.slot == slot }?.let { confirmed ->
                        nutritionRepository.confirmMeal(confirmed.id, true)
                    }
            } catch (t: Throwable) {
                // La conferma dal promemoria gira fuori da qualsiasi UI: senza
                // questa cattura l'errore chiude il processo dell'app.
                Log.e("ReminderReceiver", "Conferma pasto non riuscita", t)
            } finally {
                context.getSystemService(NotificationManager::class.java)?.cancel(requestCode)
                runCatching { pendingResult?.finish() }
            }
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: CHANNEL_MEALS
        val kind = intent.getStringExtra(EXTRA_KIND) ?: ""
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)
        val deferMinutes = intent.getIntExtra(EXTRA_DEFER_MINUTES, SNOOZE_MINUTES).coerceIn(5, 120)

        // Posticipo configurabile tramite AlarmManager one-shot.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val trigger = System.currentTimeMillis() + deferMinutes * 60_000L
        val retry = PendingIntent.getBroadcast(
            context, requestCode + 2000,
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_KIND, kind)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_CHANNEL, channel)
                putExtra(EXTRA_REQUEST_CODE, requestCode)
                putExtra(EXTRA_DEFER_MINUTES, deferMinutes)
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
        val deferMinutes = intent.getIntExtra(EXTRA_DEFER_MINUTES, SNOOZE_MINUTES).coerceIn(5, 120)

        // L'allarme originale non e ripetitivo: lo ripianifichiamo per il periodo successivo.
        if (repeatDaily || weekly) {
            // Riprogramma a orario fisso per il giorno successivo / settimana successiva.
            val hour = intent.getIntExtra(EXTRA_HOUR, -1)
            val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
            if (hour >= 0 && minute >= 0) {
                val time = "%02d:%02d".format(hour, minute)
                if (weekly) {
                    scheduler.scheduleWeekly(requestCode, dayOfWeek, time, kind, title, body, channel, deferMinutes)
                } else {
                    scheduler.scheduleDaily(requestCode, time, kind, title, body, channel, deferMinutes)
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
        const val EXTRA_DEFER_MINUTES = "extra_defer_minutes"
        const val EXTRA_DEEP_LINK_ROUTE = "extra_deep_link_route"

        const val ACTION_SNOOZE = "com.fitcoach.app.action.SNOOZE"
        const val ACTION_CONFIRM = "com.fitcoach.app.action.CONFIRM"
        const val SNOOZE_MINUTES = 15

        /** Destinazione aperta toccando la notifica, in base al tipo di promemoria. */
        fun deepLinkRouteFor(kind: String): String = when {
            kind.startsWith("MEAL_") -> "diary"
            kind == "WORKOUT" -> "workout"
            kind == "CHECKIN" || kind == "WEIGHT" -> "progress"
            else -> "dashboard"
        }
    }
}
