package com.fitcoach.app.collaudo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.AiProviderSettingsEntity
import com.fitcoach.app.data.remote.AiClient
import com.fitcoach.app.data.remote.SecureKeyStore
import com.fitcoach.app.data.repository.ChatRepository
import com.fitcoach.app.domain.coach.AiConfigResolver
import com.fitcoach.app.domain.model.AiProvider
import com.fitcoach.app.domain.model.FoodUnit
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.RecipeAiResult
import com.fitcoach.app.domain.nutrition.RecipeAiService
import com.fitcoach.app.domain.nutrition.RecipeStyle
import com.fitcoach.app.support.ScriptedApi
import com.fitcoach.app.support.textResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La ricetta deve essere scritta dall'AI o non esistere.
 *
 * Il difetto segnalato era "Ricetta composta senza AI (Risposta AI non
 * interpretabile)": il servizio ripiegava su un compositore deterministico
 * quando il parsing falliva. Il ripiego e' stato eliminato e il parsing ora
 * accetta le forme reali che i modelli producono (JSON dentro markdown, nomi di
 * alimento riscritti) e chiede una correzione quando la risposta non va bene.
 *
 * Il database e' quello vero, in memoria: l'unico doppio e' il provider LLM,
 * che in un test unitario non esiste.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeAiGenerationCollaudoTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var chatRepository: ChatRepository
    private lateinit var keys: SecureKeyStore

    private val json = Json {
        ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false
    }

    /** Il piano reale scrive "Pollo, petto": il modello risponde "petto di pollo". */
    private val entries = listOf(
        MealEntry(
            id = 1,
            mealId = 10,
            foodItemId = 1,
            foodName = "Pollo, petto",
            grams = 180.0,
            unit = FoodUnit.RAW,
            kcal = 198.0,
            protein = 43.2,
            carbs = 0.0,
            fat = 1.8
        ),
        MealEntry(
            id = 2,
            mealId = 10,
            foodItemId = 2,
            foodName = "Riso, bianco",
            grams = 70.0,
            unit = FoodUnit.RAW,
            kcal = 242.0,
            protein = 5.0,
            carbs = 53.0,
            fat = 0.6
        )
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        chatRepository = ChatRepository(db.chatDao(), db.aiSettingsDao())
        keys = SecureKeyStore(
            context,
            context.getSharedPreferences("recipe_ai_test_keys", Context.MODE_PRIVATE)
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun configureProvider(baseUrl: String = "https://provider.test/v1", model: String = "test-model") {
        db.aiSettingsDao().upsert(
            AiProviderSettingsEntity(
                id = 1L,
                provider = AiProvider.CUSTOM,
                baseUrl = baseUrl,
                model = model
            )
        )
        keys.putApiKey(AiProvider.CUSTOM.name, "test-key")
    }

    private fun service(api: ScriptedApi) = RecipeAiService(
        AiClient(api, json),
        AiConfigResolver(chatRepository, keys),
        json
    )

    private suspend fun generate(api: ScriptedApi): RecipeAiResult = service(api).generate(
        entries = entries,
        slot = MealSlot.LUNCH,
        style = RecipeStyle.CLASSIC,
        profile = null
    )

    @Test
    fun `una risposta valida produce la ricetta`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                textResponse(
                    """{"nome":"Pollo e riso in padella","ingredienti":["Pollo, petto","Riso, bianco"],""" +
                        """"passi":["Cuoci il riso in acqua bollente salata per 15 minuti.",""" +
                        """"Scalda la padella a fuoco medio e rosola il pollo 6 minuti per lato."]}"""
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Success::class.java)
        val recipe = (result as RecipeAiResult.Success).recipe
        assertThat(recipe.name).isEqualTo("Pollo e riso in padella")
        assertThat(recipe.steps).hasSize(2)
        // I macro sono quelli del piano, non quelli eventualmente inventati dal modello.
        assertThat(recipe.kcal).isWithin(0.01).of(440.0)
        assertThat(recipe.protein).isWithin(0.01).of(48.2)
    }

    @Test
    fun `il JSON dentro un blocco markdown viene comunque letto`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                textResponse(
                    "Ecco la ricetta:\n```json\n" +
                        """{"nome":"Insalata di pollo e riso","ingredienti":["pollo","riso"],""" +
                        """"passi":["Lessa il riso in acqua salata.","Griglia il pollo e unisci."]}""" +
                        "\n```\nBuon appetito."
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Success::class.java)
        assertThat((result as RecipeAiResult.Success).recipe.name)
            .isEqualTo("Insalata di pollo e riso")
    }

    @Test
    fun `un nome di alimento riscritto in modo diverso non invalida la ricetta`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                textResponse(
                    """{"nome":"Petto di pollo con riso","ingredienti":["petto di pollo","riso bianco"],""" +
                        """"passi":["Cuoci il riso 15 minuti.","Rosola il petto di pollo in padella."]}"""
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Success::class.java)
    }

    @Test
    fun `un alimento estraneo viene corretto chiedendo una nuova risposta`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                // Primo tentativo: aggiunge olio e parmigiano, non previsti dal pasto.
                textResponse(
                    """{"nome":"Pollo al parmigiano","ingredienti":["pollo","riso","olio","parmigiano"],""" +
                        """"passi":["Cuoci il riso in acqua.","Rosola il pollo con olio e parmigiano."]}"""
                ),
                textResponse(
                    """{"nome":"Pollo e riso","ingredienti":["Pollo, petto","Riso, bianco"],""" +
                        """"passi":["Cuoci il riso 15 minuti.","Rosola il pollo in padella."]}"""
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Success::class.java)
        assertThat((result as RecipeAiResult.Success).recipe.name).isEqualTo("Pollo e riso")
        // La correzione deve essere stata chiesta al modello, non solo tentata.
        assertThat(api.sent).hasSize(2)
        val correction = api.sent[1].messages.last().content
        assertThat(correction).contains("olio")
        assertThat(correction).contains("parmigiano")
    }

    @Test
    fun `se il modello sbaglia sempre si restituisce un errore esplicito`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                textResponse("Non posso scrivere ricette."),
                textResponse("Mi dispiace, non ho capito."),
                textResponse("Riprova piu' tardi.")
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Failed::class.java)
        val failed = result as RecipeAiResult.Failed
        assertThat(failed.message).contains("non ha prodotto una ricetta valida")
        assertThat(failed.detail).contains("JSON")
        assertThat(api.sent).hasSize(3)
    }

    @Test
    fun `senza provider configurato non si inventa una ricetta`() = runTest {
        // Nessuna riga di impostazioni: provider non configurato.
        val api = ScriptedApi(mutableListOf())

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Failed::class.java)
        assertThat((result as RecipeAiResult.Failed).message)
            .contains("Nessun provider AI configurato")
        assertThat(api.sent).isEmpty()
    }

    @Test
    fun `un errore del provider viene riportato senza ripiego`() = runTest {
        configureProvider()
        // ScriptedApi senza risposte: simula un provider non raggiungibile.
        val api = ScriptedApi(mutableListOf())

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Failed::class.java)
        assertThat((result as RecipeAiResult.Failed).message).isNotEmpty()
    }

    @Test
    fun `la ricetta incapsulata in un oggetto esterno viene letta`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                textResponse(
                    """{"ricetta":{"nome":"Pollo e riso","ingredienti":["Pollo, petto","Riso, bianco"],""" +
                        """"passi":["Cuoci il riso 15 minuti.","Rosola il pollo in padella."]}}"""
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Success::class.java)
        assertThat((result as RecipeAiResult.Success).recipe.name).isEqualTo("Pollo e riso")
        // Nessuna correzione richiesta: la forma era accettabile.
        assertThat(api.sent).hasSize(1)
    }

    @Test
    fun `i campi in inglese vengono accettati`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                textResponse(
                    """{"name":"Pollo e riso in padella","ingredients":["Pollo, petto","Riso, bianco"],""" +
                        """"steps":["Cuoci il riso in acqua bollente 15 minuti.","Rosola il pollo 6 minuti per lato."]}"""
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Success::class.java)
        assertThat((result as RecipeAiResult.Success).recipe.steps).hasSize(2)
        assertThat(api.sent).hasSize(1)
    }

    @Test
    fun `un ingrediente tradotto in un'altra lingua viene rifiutato`() = runTest {
        configureProvider()
        // Il modello traduce "Pollo, petto" in "chicken breast": non e' un
        // alimento del piano e non e' verificabile, quindi va corretto.
        val api = ScriptedApi(
            mutableListOf(
                textResponse(
                    """{"nome":"Chicken and rice","ingredienti":["chicken breast","white rice"],""" +
                        """"passi":["Boil the rice for 15 minutes.","Pan-sear the chicken 6 minutes per side."]}"""
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Failed::class.java)
        // La correzione deve citare gli alimenti estranei.
        val correction = api.sent.last().messages.last().content
        assertThat(correction).contains("chicken breast")
        assertThat(correction).contains("white rice")
    }

    @Test
    fun `i passi scritti come unica stringa numerata vengono divisi`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                textResponse(
                    """{"nome":"Pollo e riso","ingredienti":["Pollo, petto","Riso, bianco"],""" +
                        """"passi":"1. Cuoci il riso in acqua bollente salata per 15 minuti. """ +
                        """2. Rosola il pollo in padella 6 minuti per lato."}"""
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Success::class.java)
        val steps = (result as RecipeAiResult.Success).recipe.steps
        assertThat(steps).hasSize(2)
        assertThat(steps[0]).startsWith("Cuoci il riso")
        assertThat(steps[1]).startsWith("Rosola il pollo")
        assertThat(api.sent).hasSize(1)
    }

    @Test
    fun `i passi insufficienti vengono corretti`() = runTest {
        configureProvider()
        val api = ScriptedApi(
            mutableListOf(
                textResponse(
                    """{"nome":"Pollo e riso","ingredienti":["Pollo, petto","Riso, bianco"],""" +
                        """"passi":["Cuoci tutto."]}"""
                ),
                textResponse(
                    """{"nome":"Pollo e riso","ingredienti":["Pollo, petto","Riso, bianco"],""" +
                        """"passi":["Cuoci il riso 15 minuti in acqua bollente.","Rosola il pollo 6 minuti per lato."]}"""
                )
            )
        )

        val result = generate(api)

        assertThat(result).isInstanceOf(RecipeAiResult.Success::class.java)
        assertThat(api.sent).hasSize(2)
    }
}
