package com.fitcoach.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fitcoach.app.data.repository.DatabaseBackupRepository
import com.fitcoach.app.diagnostics.CrashLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Backup automatico periodico del database.
 *
 * Perche' WorkManager e non AlarmManager: il backup deve avvenire "prima o poi",
 * non a un'ora precisa. WorkManager sopravvive al riavvio, rimanda se il
 * dispositivo e' in Doze e ritenta con backoff. Un allarme esatto per
 * un'operazione non time-critical sarebbe la scelta sbagliata.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: DatabaseBackupRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val file = backupRepository.createBackup()
            pruneOldBackups(file.parentFile)
            Result.success()
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            // Disco pieno o database non ancora creato: un errore transitorio
            // non deve disattivare il backup per sempre, quindi retry.
            CrashLog.record(applicationContext, t, "Backup automatico")
            if (runAttemptCount >= MAX_RETRIES) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "fitcoach_auto_backup"
        internal const val BACKUP_PREFIX = "fitcoach_backup_"
        internal const val MAX_BACKUPS = 5
        private const val MAX_RETRIES = 3

        /**
         * Tiene solo gli ultimi [MAX_BACKUPS] file. Senza questo il backup
         * periodico riempirebbe la cache fino a far fallire le scritture.
         */
        internal fun pruneOldBackups(dir: File?) {
            val backups = dir?.listFiles { f -> f.name.startsWith(BACKUP_PREFIX) }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            backups.drop(MAX_BACKUPS).forEach { it.delete() }
        }

        /** Ogni quanto gira il backup. Giornaliero: il DB cambia lentamente. */
        private const val PERIOD_HOURS = 24L

        fun enqueue(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(PERIOD_HOURS, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP: riaccodare a ogni avvio azzererebbe il conteggio e il
                // backup non partirebbe mai prima del periodo successivo.
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
