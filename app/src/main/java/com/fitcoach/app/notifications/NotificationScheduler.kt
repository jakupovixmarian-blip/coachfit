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
import com.fitcoach.app.domain.model.NotificationRecord
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
    private var deferMinutesForSchedule: Int = 15

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            Triple(ReminderReceiver.CHANNEL_MEALS, R.string.channel_meals, R.string.channel_meals_desc),
            Triple(ReminderReceiver.CHANNEL_WORKOUT, R.string.channel_workout, R.string.channel_workout_desc),
            Triple(ReminderReceiver.CHANNEL_HYDRATION, R.string.channel_hydration, R.string.channel_hydration_desc),
            Triple(ReminderReceiver.CHANNEL_CHECKIN, R.string.channel_checkin, R.string.channel_checkin_desc),
            Triple(ReminderReceiver.CHANNEL_TIMER, R.string.channel_timer, R.string.channel_timer_desc)
        )
        channels.forEach { (id, nameRes, descRes) ->
            val importance = if (id == ReminderReceiver.CHANNEL_TIMER) {
                NotificationManager.IMPORTANCE_HIGH
            } else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                id,
                context.getString(nameRes),
                importance
            ).apply {
                description = context.getString(descRes)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Riprogramma tutte le notifiche in base alle preferenze e alle impostazioni salvate. */
    fun scheduleAll(prefs: UserPreferences, records: List<NotificationRecord> = emptyList()) {
        deferMinutesForSchedule = prefs.deferMinutes.coerceIn(5, 120)
        cancelAll()
        if (!prefs.notificationsEnabled) return

        val byKind = records.associateBy { it.kind }
        var requestCode = 100
        fun nextCode(record: NotificationRecord?): Int {
            val code = record?.requestCode?.takeIf { it > 0 } ?: requestCode
            requestCode++
            return code
        }
        prefs.mealSchedules.forEach { schedule ->
            val record = byKind["MEAL_${schedule.slot.name}"]
            if (record?.enabled ?: schedule.enabled) {
                scheduleDaily(
                    requestCode = nextCode(record),
                    time = record?.triggerTime ?: schedule.time,
                    kind = record?.kind ?: "MEAL_${schedule.slot.name}",
                    title = record?.title ?: "FitCoach – ${schedule.slot.label}",
                    body = record?.body ?: bodyForMeal(schedule.slot),
                    channel = ReminderReceiver.CHANNEL_MEALS
                )
            }
        }

        val workout = byKind["WORKOUT"]
        if (workout?.enabled ?: true) {
            scheduleDaily(
                requestCode = nextCode(workout),
                time = workout?.triggerTime ?: prefs.workoutTime,
                kind = "WORKOUT",
                title = workout?.title ?: "FitCoach – Allenamento",
                body = workout?.body ?: "E' ora di allenarti. Apri l'app per vedere la scheda di oggi.",
                channel = ReminderReceiver.CHANNEL_WORKOUT
            )
        }

        val hydration = byKind["HYDRATION"]
        if (hydration?.enabled ?: prefs.hydrationRemindersEnabled) {
            scheduleHydration(prefs, requestCode)
            requestCode += 12
        }

        val checkIn = byKind["CHECKIN"]
        if (checkIn?.enabled ?: prefs.weeklyCheckInEnabled) {
            scheduleWeekly(
                requestCode = nextCode(checkIn),
                dayOfWeek = prefs.weeklyCheckInDay,
                time = checkIn?.triggerTime ?: prefs.weeklyCheckInTime,
                kind = "CHECKIN",
                title = checkIn?.title ?: "FitCoach – Check-in settimanale",
                body = checkIn?.body ?: "Registra peso e misure per aggiornare il tuo programma.",
                channel = ReminderReceiver.CHANNEL_CHECKIN
            )
        }

        val weight = byKind["WEIGHT"]
        if (weight?.enabled ?: prefs.weightReminderEnabled) {
            scheduleWeekly(
                requestCode = nextCode(weight),
                dayOfWeek = prefs.weightReminderDay,
                time = weight?.triggerTime ?: prefs.weightReminderTime,
                kind = "WEIGHT",
                title = weight?.title ?: "FitCoach – Valutazione del peso",
                body = weight?.body ?: "Pesati a digiuno e registra il peso per il monitoraggio.",
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
        MealSlot.EVENING_SNACK -> "Spuntino serale previsto dal piano."
    }

    fun scheduleDaily(
        requestCode: Int,
        time: String,
        kind: String,
        title: String,
        body: String,
        channel: String,
        deferMinutes: Int = deferMinutesForSchedule
    ) {
        val (hour, minute) = parseTime(time)
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        setAlarm(requestCode, trigger, kind, title, body, channel, repeatingDaily = true, deferMinutes = deferMinutes)
    }

    internal fun scheduleWeekly(
        requestCode: Int,
        dayOfWeek: Int,
        time: String,
        kind: String,
        title: String,
        body: String,
        channel: String,
        deferMinutes: Int = deferMinutesForSchedule
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
        setAlarm(requestCode, trigger, kind, title, body, channel, repeatingDaily = false, weekly = true, dayOfWeek = dayOfWeek, deferMinutes = deferMinutes)
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

    /**
     * Allarme una-tantum alla fine del recupero: sveglia lo schermo anche se l'app
     * e in background, cosi l'utente sa quando riprendere la serie. Usa un codice
     * dedicato fuori dall'intervallo dei promemoria per non interferire.
     */
    fun scheduleRestAlarm(triggerAtMillis: Long, seconds: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_KIND, "REST_TIMER")
            putExtra(ReminderReceiver.EXTRA_TITLE, context.getString(R.string.notif_rest_title))
            putExtra(ReminderReceiver.EXTRA_BODY, context.getString(R.string.notif_rest_body, seconds))
            putExtra(ReminderReceiver.EXTRA_CHANNEL, ReminderReceiver.CHANNEL_TIMER)
            putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, REST_ALARM_CODE)
        }
        val pending = PendingIntent.getBroadcast(
            context, REST_ALARM_CODE, intent,
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

    fun cancelRestAlarm() {
        val pending = PendingIntent.getBroadcast(
            context, REST_ALARM_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    companion object {
        /** Codice dedicato all'allarme di fine recupero (fuori da 100..150). */
        const val REST_ALARM_CODE = 9001
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
        dayOfWeek: Int = 0,
        deferMinutes: Int = deferMinutesForSchedule
    ) {
        val triggerCalendar = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_KIND, kind)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_CHANNEL, channel)
            putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
            putExtra(ReminderReceiver.EXTRA_REPEAT_DAILY, repeatingDaily)
            putExtra(ReminderReceiver.EXTRA_WEEKLY, weekly)
            putExtra(ReminderReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(ReminderReceiver.EXTRA_HOUR, triggerCalendar.get(Calendar.HOUR_OF_DAY))
            putExtra(ReminderReceiver.EXTRA_MINUTE, triggerCalendar.get(Calendar.MINUTE))
            putExtra(ReminderReceiver.EXTRA_DEFER_MINUTES, deferMinutes.coerceIn(5, 120))
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
