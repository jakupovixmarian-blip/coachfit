package com.fitcoach.app.data.repository

import android.content.Context
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import com.fitcoach.app.data.local.FitCoachDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
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

    /**
     * Il ripristino e' l'unica operazione irreversibile dell'app: sovrascrive il
     * database e poi riavvia. Se il file scelto non e' un database FitCoach,
     * i dati originali sono gia' chiusi e persi. Per questo il file viene
     * copiato in un'area temporanea e VALIDATO prima di toccare l'originale:
     * la chiusura del database avviene solo a validazione superata.
     */
    suspend fun restoreBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val target = context.getDatabasePath(FitCoachDatabase.DB_NAME)
        val temp = File(context.cacheDir, "restore_${System.currentTimeMillis()}.db")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Impossibile leggere il file di backup.")

            validateBackupFile(temp)

            database.close()
            temp.copyTo(target, overwrite = true)
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
        } finally {
            temp.delete()
        }
    }

    /**
     * Un backup accettabile deve essere un database SQLite leggibile e deve
     * contenere le tabelle di FitCoach. Il solo magic number non basta: un
     * file troncato lo conserva ma non e' interrogabile.
     */
    private fun validateBackupFile(file: File) {
        if (file.length() < 100) {
            throw IOException("Il file scelto non e' un backup valido (troppo piccolo).")
        }
        val magic = file.inputStream().use { input ->
            val buffer = ByteArray(16)
            val letti = input.read(buffer)
            if (letti < 16) ByteArray(0) else buffer
        }
        val atteso = "SQLite format 3".toByteArray(StandardCharsets.US_ASCII)
        if (!magic.copyOfRange(0, 15).contentEquals(atteso)) {
            throw IOException("Il file scelto non e' un database FitCoach.")
        }

        val sonda = SQLiteDatabase.openDatabase(
            file.path, null, SQLiteDatabase.OPEN_READONLY
        )
        try {
            val tabelle = mutableSetOf<String>()
            sonda.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'", null
            ).use { cursor ->
                while (cursor.moveToNext()) tabelle.add(cursor.getString(0))
            }
            val richieste = listOf("user_profile", "weight_records")
            val mancanti = richieste.filterNot { tabelle.contains(it) }
            if (mancanti.isNotEmpty()) {
                throw IOException(
                    "Il file non e' un backup FitCoach: mancano le tabelle ${mancanti.joinToString(", ")}."
                )
            }
        } finally {
            sonda.close()
        }
    }

    private fun checkpointWal() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }
}
