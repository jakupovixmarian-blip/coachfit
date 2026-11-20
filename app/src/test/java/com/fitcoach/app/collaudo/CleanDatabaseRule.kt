package com.fitcoach.app.collaudo

import com.fitcoach.app.data.local.FitCoachDatabase
import org.junit.rules.ExternalResource
import org.robolectric.RuntimeEnvironment

/**
 * Cancella il database prima del lancio dell'Activity.
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
        RuntimeEnvironment.getApplication().deleteDatabase(FitCoachDatabase.DB_NAME)
    }
}
