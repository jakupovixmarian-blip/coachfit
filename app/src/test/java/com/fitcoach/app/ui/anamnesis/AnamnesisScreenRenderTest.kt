package com.fitcoach.app.ui.anamnesis

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.ui.theme.FitCoachTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Verifica VISIVA della schermata finale dell'anamnesi (41/41). Rende il vero
 * composable su JVM (Robolectric + grafica nativa) e salva uno screenshot PNG,
 * cosi il layout e controllabile senza emulatore.
 *
 * Output: app/build/screenshots/
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AnamnesisScreenRenderTest {

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

    private fun render(name: String) {
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
        val view = composeRule.activity.window.decorView
        val width = if (view.width > 0) view.width else 1080
        val height = if (view.height > 0) view.height else 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun `screenshot final anamnesis screen`() {
        render("anamnesis_final")
    }
}