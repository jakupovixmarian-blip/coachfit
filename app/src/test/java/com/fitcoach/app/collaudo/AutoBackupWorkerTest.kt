package com.fitcoach.app.collaudo

import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.work.AutoBackupWorker
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.Executors

/**
 * Contratto di scheduling del backup automatico.
 *
 * Il worker vero richiede un database su disco: qui si verifica la parte che
 * puo' rompersi in silenzio, cioe' che l'interruttore accodi o annulli davvero
 * il lavoro e che la pulizia non lasci crescere la cache.
 */
@RunWith(RobolectricTestRunner::class)
class AutoBackupWorkerTest {

    private lateinit var context: android.content.Context

    companion object {
        private var inizializzato = false
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // WorkManager e' un singleton statico: inizializzarlo in ogni test
        // solleva "already initialized". Gli esecutori devono stare fuori dal
        // main thread, altrimenti il database interno di WorkManager rifiuta
        // le query ("Cannot access database on the main thread").
        if (!inizializzato) {
            val executor = Executors.newSingleThreadExecutor()
            WorkManager.initialize(
                context,
                Configuration.Builder()
                    .setExecutor(executor)
                    .setTaskExecutor(executor)
                    .build()
            )
            inizializzato = true
        }
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    private fun lavori(): List<WorkInfo> {
        val wm = WorkManager.getInstance(context)
        return wm.getWorkInfosForUniqueWork(AutoBackupWorker.WORK_NAME).get()
    }

    @Test
    fun `attivare l'interruttore accoda un lavoro periodico unico`() {
        AutoBackupWorker.enqueue(context, enabled = true)

        val lavori = lavori()
        assertThat(lavori).hasSize(1)
        assertThat(lavori.first().state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun `accodare due volte non duplica il lavoro`() {
        AutoBackupWorker.enqueue(context, enabled = true)
        AutoBackupWorker.enqueue(context, enabled = true)

        assertThat(lavori()).hasSize(1)
    }

    @Test
    fun `disattivare l'interruttore annulla il lavoro`() {
        AutoBackupWorker.enqueue(context, enabled = true)
        AutoBackupWorker.enqueue(context, enabled = false)

        // WorkManager conserva la riga storica con stato CANCELLED: il lavoro
        // non e' piu' schedulato, ma la voce non sparisce dalla tabella.
        assertThat(lavori().map { it.state }).containsExactly(WorkInfo.State.CANCELLED)
        assertThat(lavori().none { it.state == WorkInfo.State.ENQUEUED }).isTrue()
    }

    @Test
    fun `la pulizia tiene solo gli ultimi backup`() {
        val dir = File(context.cacheDir, "backups-test").apply { mkdirs() }
        repeat(AutoBackupWorker.MAX_BACKUPS + 3) { i ->
            File(dir, "${AutoBackupWorker.BACKUP_PREFIX}$i.db").apply {
                writeText("x")
                // lastModified ha risoluzione variabile: forzala per un ordine stabile.
                setLastModified(1_000_000L + i * 10_000L)
            }
        }

        AutoBackupWorker.pruneOldBackups(dir)

        val rimasti = dir.listFiles()!!.sortedBy { it.name }
        assertThat(rimasti).hasSize(AutoBackupWorker.MAX_BACKUPS)
        // Restano i piu' recenti, non i primi trovati.
        assertThat(rimasti.map { it.name })
            .containsExactly(
                "${AutoBackupWorker.BACKUP_PREFIX}3.db",
                "${AutoBackupWorker.BACKUP_PREFIX}4.db",
                "${AutoBackupWorker.BACKUP_PREFIX}5.db",
                "${AutoBackupWorker.BACKUP_PREFIX}6.db",
                "${AutoBackupWorker.BACKUP_PREFIX}7.db"
            )
    }

    @Test
    fun `la pulizia ignora file che non sono backup`() {
        val dir = File(context.cacheDir, "backups-misti").apply { mkdirs() }
        File(dir, "altro.db").writeText("x")
        repeat(AutoBackupWorker.MAX_BACKUPS + 1) { i ->
            File(dir, "${AutoBackupWorker.BACKUP_PREFIX}$i.db").writeText("x")
        }

        AutoBackupWorker.pruneOldBackups(dir)

        assertThat(dir.listFiles()!!.any { it.name == "altro.db" }).isTrue()
        assertThat(dir.listFiles()!!.count { it.name.startsWith(AutoBackupWorker.BACKUP_PREFIX) })
            .isEqualTo(AutoBackupWorker.MAX_BACKUPS)
    }

}
