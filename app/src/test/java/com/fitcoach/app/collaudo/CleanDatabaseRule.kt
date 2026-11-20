package com.fitcoach.app.collaudo

import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.ui.navigation.UserProfileEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Cancella database e preferenze prima del lancio dell'Activity.
 *
 * Perche esiste: `fitcoach.db` e' un file che sopravvive tra le classi di test
 * nella stessa JVM. Un test che semina un profilo lo lasciava agli altri, e il
 * test di primo avvio trovava un utente gia' registrato: falliva a intermittenza
 * a seconda dell'ordine di esecuzione. Serve solo ai test che devono partire da
 * un'installazione pulita; i test con `SeededProfileApplication` non lo usano,
 * perche' il loro seme verrebbe cancellato.
 */
class CleanDatabaseRule : ExternalResource() {

    override fun before() {
        val app = RuntimeEnvironment.getApplication()
        app.deleteDatabase(FitCoachDatabase.DB_NAME)
        // Anche i DataStore sopravvivono tra i test: il flag "setup rimandato"
        // scritto da un test faceva partire i successivi dall'onboarding invece
        // che dal provider. Cancellare il file non basta, perche' l'istanza
        // DataStore e' un singleton gia' in memoria: il flag va riportato a
        // false tramite lo store stesso.
        File(app.filesDir, "datastore").deleteRecursively()
        val store = EntryPointAccessors
            .fromApplication(app, UserProfileEntryPoint::class.java)
            .setupStateStore()
        runBlocking { store.setAiSetupSkipped(false) }
    }
}
