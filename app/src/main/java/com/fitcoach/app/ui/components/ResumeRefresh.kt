package com.fitcoach.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * Esegue [onResume] ogni volta che la schermata torna visibile.
 *
 * I ViewModel di questa app sono legati alle destinazioni della bottom bar e
 * restano vivi quando si cambia scheda: un `load()` chiamato solo in `init`
 * non ricarica nulla al ritorno. Il risultato era che una modifica applicata
 * dal coach restava invisibile finche l'app non veniva chiusa e riaperta.
 *
 * Nessun flag "primo avvio": la navigazione distrugge la composizione, quindi
 * uno stato tenuto con `remember` si perderebbe e il refresh salterebbe proprio
 * al ritorno sulla scheda. Il costo di una lettura in piu' all'avvio e'
 * trascurabile, mentre un refresh mancato e' un difetto visibile.
 */
@Composable
fun OnResumeRefresh(onResume: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateAsState()
    val callback by rememberUpdatedState(onResume)

    LaunchedEffect(state) {
        if (state.isAtLeast(Lifecycle.State.RESUMED)) callback()
    }
}
