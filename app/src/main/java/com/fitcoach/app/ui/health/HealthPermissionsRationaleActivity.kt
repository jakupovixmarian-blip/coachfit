package com.fitcoach.app.ui.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitcoach.app.ui.theme.FitCoachTheme
import com.fitcoach.app.ui.theme.TextSecondary

/**
 * Schermata di motivazione richiesta da Health Connect.
 *
 * Senza un'activity che risponda a ACTION_SHOW_PERMISSIONS_RATIONALE, Health
 * Connect non considera FitCoach un'app salute: la richiesta di permessi si
 * chiude senza mostrare nulla, che e' esattamente il "clicco e non succede
 * niente" segnalato. La stessa activity e' dichiarata con i due filtri
 * VIEW_PERMISSION_USAGE e HEALTH_PERMISSIONS, che abilitano la voce
 * "Dati e accesso" nella scheda dell'app dentro Health Connect.
 */
class HealthPermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitCoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RationaleContent()
                }
            }
        }
    }
}

@Composable
private fun RationaleContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Dati che FitCoach legge",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "FitCoach legge da Health Connect solo i dati che servono a " +
                "calibrare allenamento e nutrizione sul tuo dispendio reale:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        listOf(
            "Passi: stima dell'attivita' quotidiana e del dispendio energetico.",
            "Calorie attive: correzione del target calorico giornaliero.",
            "Sonno: valutazione del recupero e gestione del volume di allenamento.",
            "Frequenza cardiaca a riposo: indicatore di affaticamento.",
            "Variabilita' cardiaca (HRV): indicatore di recupero."
        ).forEach { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "FitCoach non scrive dati in Health Connect e non condivide le " +
                "tue misure con terzi. Puoi revocare l'accesso in qualsiasi momento " +
                "da Health Connect o dalle impostazioni di sistema.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "FitCoach non e' un dispositivo medico e non formula diagnosi.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
