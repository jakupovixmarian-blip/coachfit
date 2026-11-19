package com.fitcoach.app.diagnostics

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registro degli errori non gestiti, scritto su file nell'archivio interno.
 *
 * Perche esiste: senza un log recuperabile dal dispositivo, una segnalazione
 * "l'app va in crash" non e diagnosticabile e ogni correzione diventa un
 * tentativo alla cieca. Il file e leggibile e condivisibile dalle impostazioni,
 * cosi lo stack trace reale arriva senza collegare il telefono al PC.
 */
object CrashLog {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(context, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun read(context: Context): String? =
        runCatching { file(context).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY).format(Date())
        val body = buildString {
            appendLine("FitCoach — errore non gestito")
            appendLine("Data: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} " +
                "(Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine(Log.getStackTraceString(throwable))
        }
        file(context).writeText(body)
    }
}
