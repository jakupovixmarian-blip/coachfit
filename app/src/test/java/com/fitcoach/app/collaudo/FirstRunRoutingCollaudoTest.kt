package com.fitcoach.app.collaudo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.fitcoach.app.MainActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Ordine del primo avvio: **provider AI, poi le domande, poi la Dashboard**.
 *
 * Il provider viene per primo perche' senza di lui nessun piano si puo'
 * generare: chiederlo a valle significava raccogliere eta', peso, allergie e
 * limitazioni per poi bloccare l'utente alla fine. Il difetto segnalato
 * ("l'app chiede subito l'anamnesi") era esattamente questo ordine sbagliato:
 * il passo provider esisteva, ma dopo tutte le domande.
 *
 * Qui si usa l'Application reale (nessun seme): il database e' vuoto, come su
 * un telefono appena installato.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class FirstRunRoutingCollaudoTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    // Il database sopravvive tra le classi di test nella stessa JVM: senza questa
    // cancellazione il "primo avvio" trovava il profilo seminato da un altro test.
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(CleanDatabaseRule()).around(composeRule)

    @Test
    fun `al primo avvio si apre la configurazione del provider, non le domande`() {
        // La destinazione iniziale si decide dopo una lettura asincrona del
        // profilo: `waitForIdle` non attende quella coroutine.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Provider AI").fetchSemanticsNodes().isNotEmpty()
        }

        val providerTitle = composeRule.onAllNodesWithText("Provider AI").fetchSemanticsNodes()
        assertThat(providerTitle).isNotEmpty()

        // Le domande anagrafiche NON devono precedere il provider.
        val nomeField = composeRule.onAllNodesWithText("Nome").fetchSemanticsNodes()
        assertThat(nomeField).isEmpty()
    }

    @Test
    fun `il setup del provider e saltabile`() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Provider AI").fetchSemanticsNodes().isNotEmpty()
        }
        // La conferma sta in fondo alla lista: LazyColumn non compone gli item
        // fuori schermo, quindi senza scroll il nodo non esiste proprio.
        composeRule.onAllNodes(hasScrollAction())[0]
            .performScrollToNode(hasText("Configura più tardi"))

        // Nessun provider obbligatorio: senza chiave si deve poter comunque
        // proseguire, altrimenti l'app e' inutilizzabile per chi non ne ha una.
        val skip = composeRule.onAllNodesWithText("Configura più tardi").fetchSemanticsNodes()
        assertThat(skip).isNotEmpty()
    }

    @Test
    fun `durante la configurazione iniziale la barra di navigazione inferiore non e mostrata`() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Provider AI").fetchSemanticsNodes().isNotEmpty()
        }
        // Le tab portano a schermate che presuppongono un profilo: mostrarle qui
        // permetterebbe di aggirare la configurazione iniziale.
        val homeTab = composeRule.onAllNodesWithText("Home").fetchSemanticsNodes()
        assertThat(homeTab).isEmpty()
    }

    @Test
    fun `saltando il provider si prosegue con le domande, non alla Dashboard`() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Provider AI").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasScrollAction())[0]
            .performScrollToNode(hasText("Configura più tardi"))
        composeRule.onAllNodesWithText("Configura più tardi")[0].performClick()

        // Senza dati il piano non esiste: la Dashboard mostrerebbe solo zeri.
        // Chi salta il provider deve comunque compilare il profilo.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Configura il tuo profilo")
                .fetchSemanticsNodes().isNotEmpty()
        }
        val onboarding = composeRule.onAllNodesWithText("Configura il tuo profilo")
            .fetchSemanticsNodes()
        assertThat(onboarding).isNotEmpty()
    }
}
