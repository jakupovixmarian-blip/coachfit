package com.fitcoach.app.data.repository

import android.content.Context
import android.net.Uri
import com.fitcoach.app.data.local.FitCoachDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Crea e ripristina una copia locale del database Room. */
@Singleton
class DatabaseBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FitCoachDatabase
) {
    suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        checkpointWal()
        val source = context.getDatabasePath(FitCoachDatabase.DB_NAME)
        if (!source.exists()) throw IOException("Database FitCoach non trovato.")
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val target = File(dir, "fitcoach_backup_${System.currentTimeMillis()}.db")
        source.copyTo(target, overwrite = true)
        target
    }

    suspend fun restoreBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val target = context.getDatabasePath(FitCoachDatabase.DB_NAME)
        val temp = File(context.cacheDir, "restore_${System.currentTimeMillis()}.db")
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Impossibile leggere il file di backup.")

        database.close()
        temp.copyTo(target, overwrite = true)
        File(target.path + "-wal").delete()
        File(target.path + "-shm").delete()
        temp.delete()
    }

    private fun checkpointWal() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }
}
