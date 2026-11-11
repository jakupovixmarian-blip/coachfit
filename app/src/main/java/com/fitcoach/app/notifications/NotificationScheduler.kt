package com.fitcoach.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fitcoach.app.MainActivity
import com.fitcoach.app.R
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pianificazione notifiche con AlarmManager (orari esatti) e canali Android O+.
 * Le notifiche vengono ripristinate al reboot da [BootReceiver].
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            Triple(ReminderReceiver.CHANNEL_MEALS, R.string.channel_meals, R.string.channel_meals_desc),
            Triple(ReminderReceiver.CHANNEL_WORKOUT, R.string.channel_workout, R.string.channel_workout_desc),
            Triple(ReminderReceiver.CHANNEL_HYDRATION, R.string.channel_hydration, R.string.channel_hydration_desc),
            Triple(ReminderReceiver.CHANNEL_CHECKIN, R.string.channel_checkin, R.string.channel_checkin_desc)
        )
        channels.forEach { (id, nameRes, descRes) ->
            val channel = NotificationChannel(
                id,
                context.getString(nameRes),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(descRes)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Riprogramma tutte le notifiche in base alle preferenze. */
    fun scheduleAll(prefs: UserPreferences) {
        cancelAll()
        if (!prefs.notificationsEnabled) return

        var requestCode = 100
        prefs.mealSchedules.forEach { schedule ->
            if (schedule.enabled) {
                scheduleDaily(
                    requestCode = requestCode++,
                    time = schedule.time,
                    kind = "MEAL_${schedule.slot.name}",
                    title = "FitCoach – ${schedule.slot.label}",
                    body = bodyForMeal(schedule.slot),
                    channel = ReminderReceiver.CHANNEL_MEALS
                )
            }
        }
        scheduleDaily(
            requestCode = requestCode++,
            time = prefs.workoutTime,
            kind = "WORKOUT",
            title = "FitCoach – Allenamento",
            body = "E' ora di allenarti. Apri l'app per vedere la scheda di oggi.",
            channel = ReminderReceiver.CHANNEL_WORKOUT
        )
        if (prefs.hydrationRemindersEnabled) {
            scheduleHydration(prefs, requestCode)
            requestCode += 12
        }
        if (prefs.weeklyCheckInEnabled) {
            scheduleWeekly(
                requestCode = requestCode++,
                dayOfWeek = prefs.weeklyCheckInDay,
                time = prefs.weeklyCheckInTime,
                kind = "CHECKIN",
                title = "FitCoach – Check-in settimanale",
                body = "Registra peso e misure per aggiornare il tuo programma.",
                channel = ReminderReceiver.CHANNEL_CHECKIN
            )
        }
        if (prefs.weightReminderEnabled) {
            scheduleWeekly(
                requestCode = requestCode++,
                dayOfWeek = prefs.weightReminderDay,
                time = prefs.weightReminderTime,
                kind = "WEIGHT",
                title = "FitCoach – Valutazione del peso",
                body = "Pesati a digiuno e registra il peso per il monitoraggio.",
                channel = ReminderReceiver.CHANNEL_CHECKIN
            )
        }
    }

    private fun bodyForMeal(slot: MealSlot): String = when (slot) {
        MealSlot.BREAKFAST -> "E' il momento della colazione prevista dal tuo piano."
        MealSlot.MORNING_SNACK -> "Spuntino mattutino: non saltarlo, aiuta il controllo della fame."
        MealSlot.LUNCH -> "E' ora di pranzo secondo il tuo piano alimentare."
        MealSlot.AFTERNOON_SNACK -> "Spuntino pomeridiano previsto dal piano."
        MealSlot.DINNER -> "E' il momento della cena prevista dal tuo piano."
    }

    fun scheduleDaily(
        requestCode: Int,
        time: String,
        kind: String,
        title: String,
        body: String,
        channel: String
    ) {
        val (hour, minute) = parseTime(time)
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        setAlarm(requestCode, trigger, kind, title, body, channel, repeatingDaily = true)
    }

    private fun scheduleWeekly(
        requestCode: Int,
        dayOfWeek: Int,
        time: String,
        kind: String,
        title: String,
        body: String,
        channel: String
    ) {
        val (hour, minute) = parseTime(time)
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // dayOfWeek 0=Lunedi .. 6=Domenica -> Calendar: 2=Lunedi .. 1=Domenica
            set(Calendar.DAY_OF_WEEK, calendarDayOfWeek(dayOfWeek))
            if (before(Calendar.getInstance())) add(Calendar.WEEK_OF_YEAR, 1)
        }.timeInMillis
        setAlarm(requestCode, trigger, kind, title, body, channel, repeatingDaily = false, weekly = true, dayOfWeek = dayOfWeek)
    }

    private fun scheduleHydration(prefs: UserPreferences, baseCode: Int) {
        val (startHour, _) = parseTime(prefs.hydrationStartTime)
        val (endHour, _) = parseTime(prefs.hydrationEndTime)
        val interval = prefs.hydrationIntervalHours.coerceIn(1, 6)
        var code = baseCode
        var hour = startHour
        while (hour <= endHour) {
            scheduleDaily(
                requestCode = code++,
                time = "%02d:00".format(hour),
                kind = "HYDRATION",
                title = "FitCoach – Idratazione",
                body = "Bevi un bicchiere d'acqua: l'idratazione supporta performance e recupero.",
                channel = ReminderReceiver.CHANNEL_HYDRATION
            )
            hour += interval
        }
    }

    fun cancelAll() {
        for (code in 100..150) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun setAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        kind: String,
        title: String,
        body: String,
        channel: String,
        repeatingDaily: Boolean,
        weekly: Boolean = false,
        dayOfWeek: Int = 0
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_KIND, kind)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_CHANNEL, channel)
            putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
            putExtra(ReminderReceiver.EXTRA_REPEAT_DAILY, repeatingDaily)
            putExtra(ReminderReceiver.EXTRA_WEEKLY, weekly)
            putExtra(ReminderReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
        }
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    fun mainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return hour to minute
    }

    private fun calendarDayOfWeek(day: Int): Int = when (day) {
        0 -> Calendar.MONDAY
        1 -> Calendar.TUESDAY
        2 -> Calendar.WEDNESDAY
        3 -> Calendar.THURSDAY
        4 -> Calendar.FRIDAY
        5 -> Calendar.SATURDAY
        else -> Calendar.SUNDAY
    }
}
