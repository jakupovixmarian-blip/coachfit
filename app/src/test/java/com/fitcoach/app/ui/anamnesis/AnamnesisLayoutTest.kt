package com.fitcoach.app.ui.anamnesis

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.ui.theme.FitCoachTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifica il LAYOUT della schermata finale dell'anamnesi (41/41) in modo
 * oggettivo, misurando i riquadri dei componenti (semantica Compose):
 *  - il pulsante "Vai alla Dashboard" esiste una sola volta;
 *  - non si sovrappone al testo di conferma;
 *  - il testo della card e il pulsante restano dentro la card.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AnamnesisLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun finalState(): AnamnesisUiState {
        val total = AnamnesisEngine.questions.size
        val profile = UserProfile(
            name = "Marco", ageYears = 32, heightCm = 180.0, weightKg = 82.0,
            anamnesisCompleted = true, anamnesisConfirmedAt = 1L
        )
        return AnamnesisUiState(
            loading = false,
            profile = profile,
            current = null,
            answeredCount = total,
            totalRequired = AnamnesisEngine.questions.count { it.required },
            confirmed = true,
            summary = AnamnesisEngine.summary(profile)
        )
    }

    private fun setContent() {
        composeRule.setContent {
            FitCoachTheme {
                Surface(Modifier.fillMaxSize()) {
                    AnamnesisContent(
                        state = finalState(),
                        onNavigateDashboard = {},
                        onOpenSummary = {},
                        onCloseSummary = {},
                        onConfirm = {},
                        onSubmit = {},
                        onBack = {},
                        onSkip = {},
                        onToggleOption = { _, _ -> },
                        onUpdateInput = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun dashboardButton_appearsExactlyOnce() {
        setContent()
        composeRule.onAllNodesWithText("Vai alla Dashboard").assertCountEquals(1)
    }

    @Test
    fun dashboardButton_doesNotOverlapConfirmationText() {
        setContent()
        val button = composeRule.onNodeWithText("Vai alla Dashboard").getUnclippedBoundsInRoot()
        val confirmation = composeRule.onNodeWithText("Anamnesi confermata").getUnclippedBoundsInRoot()

        // Il pulsante deve stare SOTTO il titolo della card, senza sovrapporsi.
        assertTrue(
            "Il pulsante 'Vai alla Dashboard' (top=${button.top}) si sovrappone al testo " +
                "di conferma (bottom=${confirmation.bottom}).",
            button.top >= confirmation.bottom
        )
    }

    @Test
    fun confirmationCard_titleAboveButton() {
        setContent()
        val title = composeRule.onNodeWithText("Anamnesi confermata").getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithText("Vai alla Dashboard").getUnclippedBoundsInRoot()
        val review = composeRule.onNodeWithText("Rivedi il riepilogo").getUnclippedBoundsInRoot()

        assertTrue("Titolo card non sopra il pulsante", title.top < button.top)
        assertTrue("Pulsante non sopra 'Rivedi il riepilogo'", button.bottom <= review.top)
    }
}
