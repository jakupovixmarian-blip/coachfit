package com.fitcoach.app.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Navigazione canonica verso una destinazione di primo livello (tab).
 *
 * TUTTE le destinazioni di primo livello (Dashboard, Alimentazione, Spesa,
 * Allenamento, Progressi, Coach) devono essere aperte con le stesse opzioni,
 * sia dalla barra inferiore sia dalle card della Dashboard.
 *
 * Perche: mescolare navigazioni "bare" (senza opzioni) e navigazioni con
 * `popUpTo(start) { saveState }` + `restoreState` corrompe lo stato di ripristino
 * dei tab. Il sintomo reale segnalato dall'utente era che, toccando la Home dopo
 * aver aperto una card, l'app ripristinava Allenamento/Alimentazione e la Home
 * sembrava sparita e irraggiungibile.
 */
fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
