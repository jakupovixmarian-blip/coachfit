package com.fitcoach.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun schedule(id: String, title: String, body: String, delayMinutes: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes.coerceAtLeast(0), TimeUnit.MINUTES)
            .setInputData(ReminderWorker.data(title, body))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(id, ExistingWorkPolicy.REPLACE, request)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "FitCoach", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(inputData.getString(KEY_TITLE).orEmpty())
            .setContentText(inputData.getString(KEY_BODY).orEmpty())
            .setAutoCancel(true)
            .build()
        manager.notify(inputData.getString(KEY_TITLE).hashCode(), notification)
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "fitcoach_reminders"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        fun data(title: String, body: String) = androidx.work.workDataOf(KEY_TITLE to title, KEY_BODY to body)
    }
}
