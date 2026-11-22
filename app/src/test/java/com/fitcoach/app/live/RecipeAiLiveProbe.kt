package com.fitcoach.app.live

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.remote.AiApi
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.domain.coach.AiConfigResolver
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.AiProviderSettings
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.RecipeAiResult
import com.fitcoach.app.domain.nutrition.RecipeAiService
import com.fitcoach.app.domain.nutrition.RecipeStyle
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Prova REALE della generazione ricette contro OpenRouter, con il codice di
 * produzione (`RecipeAiService` -> `AiClient` -> provider).
 *
 * Non fa parte della suite automatica: richiede rete e una chiave in
 * `FITCOACH_LIVE_KEY`. Si esegue a mano:
 *
 *   FITCOACH_LIVE_KEY=sk-or-... ./gradlew testDebugUnitTest \
 *     --tests "com.fitcoach.app.live.RecipeAiLiveProbe"
 *
 * Opzionale: `FITCOACH_LIVE_MODEL` per cambiare modello.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeAiLiveProbe {

    private val model = System.getenv("FITCOACH_LIVE_MODEL") ?: "qwen/qwen3.5-35b-a3b"
    private val key = System.getenv("FITCOACH_LIVE_KEY") ?: ""

    private val json = Json {
        ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false
    }

    /**
     * Un pasto reale del piano: pollo, riso, zucchine e olio. I nomi sono quelli
     * del database alimentare, con la virgola che il piano usa davvero, perche'
     * era proprio quel formato a far scartare la risposta come "non
     * interpretabile".
     */
    private val entries = listOf(
        MealEntry(
            id = 1, mealId = 1, foodItemId = 1, foodName = "Pollo, petto",
            grams = 180.0, unit = FoodUnit.RAW, kcal = 198.0,
            protein = 43.2, carbs = 0.0, fat = 1.8
        ),
        MealEntry(
            id = 2, mealId = 1, foodItemId = 2, foodName = "Riso, bianco",
            grams = 80.0, unit = FoodUnit.RAW, kcal = 277.0,
            protein = 5.7, carbs = 60.6, fat = 0.7
        ),
        MealEntry(
            id = 3, mealId = 1, foodItemId = 3, foodName = "Zucchine",
            grams = 200.0, unit = FoodUnit.RAW, kcal = 32.0,
            protein = 2.6, carbs = 3.2, fat = 0.4
        ),
        MealEntry(
            id = 4, mealId = 1, foodItemId = 4, foodName = "Olio, extravergine di oliva",
            grams = 10.0, unit = FoodUnit.RAW, kcal = 90.0,
            protein = 0.0, carbs = 0.0, fat = 10.0
        )
    )

    @Test
    fun `generazione reale di una ricetta su OpenRouter`() = runBlocking {
        assumeTrue(
            "Serve FITCOACH_LIVE_KEY: prova manuale, non parte nella suite automatica.",
            key.isNotBlank()
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val chatRepo = ChatRepository(db.chatDao(), db.aiSettingsDao())
            chatRepo.saveAiSettings(
                AiProviderSettings(
                    provider = AiProvider.OPENROUTER,
                    baseUrl = "https://openrouter.ai/api/v1",
                    model = model,
                    maxTokens = 2_000,
                    useBackendProxy = false
                )
            )
            val keys = SecureKeyStore(
                context,
                context.getSharedPreferences("recipe_live_keys", Context.MODE_PRIVATE)
            )
            keys.putApiKey(AiProvider.OPENROUTER.name, key)

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            val api = Retrofit.Builder()
                .baseUrl("https://openrouter.ai/api/v1/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AiApi::class.java)

            val service = RecipeAiService(
                AiClient(api, json),
                AiConfigResolver(chatRepo, keys),
                json
            )

            val started = System.currentTimeMillis()
            val result = service.generate(
                entries = entries,
                slot = MealSlot.LUNCH,
                style = RecipeStyle.CLASSIC,
                profile = null
            )
            val elapsed = (System.currentTimeMillis() - started) / 1000.0

            when (result) {
                is RecipeAiResult.Success -> {
                    val r = result.recipe
                    println("=== RISULTATO in ${elapsed}s, modello ${result.model}")
                    println("=== NOME: ${r.name}")
                    r.ingredients.forEach { println("--- ingrediente: ${it.foodName} ${it.grams} g") }
                    r.steps.forEachIndexed { i, s -> println("--- passo ${i + 1}: $s") }
                    println("=== MACRO: ${r.kcal.toInt()} kcal, P ${r.protein}, C ${r.carbs}, G ${r.fat}")

                    // Le grammature devono essere quelle del piano, non riscritte.
                    assertWithMessage("gli ingredienti devono essere quelli del pasto")
                        .that(r.ingredients.map { it.foodName })
                        .containsExactlyElementsIn(entries.map { it.foodName })
                    assertWithMessage("le grammature non devono cambiare")
                        .that(r.ingredients.map { it.grams })
                        .containsExactlyElementsIn(entries.map { it.grams })
                    assertWithMessage("i passi devono essere reali")
                        .that(r.steps.size).isAtLeast(2)
                }
                is RecipeAiResult.Failed -> {
                    println("=== FALLITA in ${elapsed}s: ${result.message}")
                    println("=== DETTAGLIO: ${result.detail}")
                    assertWithMessage(
                        "la generazione reale e' fallita: ${result.message} / ${result.detail}"
                    ).fail()
                }
            }
        } finally {
            db.close()
        }
    }
}
