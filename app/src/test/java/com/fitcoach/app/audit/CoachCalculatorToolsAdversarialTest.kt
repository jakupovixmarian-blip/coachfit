package com.fitcoach.app.audit

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.coach.tools.ComputeCarbCyclingTool
import com.fitcoach.app.domain.coach.tools.ComputeEnergyTargetsTool
import com.fitcoach.app.domain.coach.tools.EstimateOneRepMaxTool
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gli strumenti di calcolo sono il punto in cui un numero sbagliato diventa un
 * consiglio sbagliato. Il modello li consulta e si fida: se uno strumento
 * dichiara di fare una cosa e non la fa, l'errore non e' visibile.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoachCalculatorToolsAdversarialTest {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var db: FitCoachDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun out(raw: String): JsonObject = json.parseToJsonElement(raw).jsonObject

    private fun profilo() = UserProfile(
        id = 1L, name = "Luca", ageYears = 30, sex = Sex.MALE, heightCm = 175.0,
        weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L
    )

    // ---------- estimate_1rm ----------

    @Test
    fun `la stima del massimale segue Epley`() = runTest {
        val r = out(EstimateOneRepMaxTool().execute(
            buildJsonObject {
                put("caricoKg", kotlinx.serialization.json.JsonPrimitive(100.0))
                put("ripetizioni", kotlinx.serialization.json.JsonPrimitive(5))
            }
        ))
        assertTrue(r["ok"]!!.jsonPrimitive.boolean)
        // 100 * (1 + 5/30) = 116.67
        assertEquals(116.67, r["stima1Rm"]!!.jsonPrimitive.double, 0.05)
    }

    @Test
    fun `il carico proposto per un target torna al massimale`() = runTest {
        val r = out(EstimateOneRepMaxTool().execute(
            buildJsonObject {
                put("caricoKg", kotlinx.serialization.json.JsonPrimitive(100.0))
                put("ripetizioni", kotlinx.serialization.json.JsonPrimitive(5))
                put("ripTarget", kotlinx.serialization.json.JsonPrimitive(8))
            }
        ))
        // 116.67 / (1 + 8/30) = 92.1
        assertEquals(92.11, r["caricoProposto"]!!.jsonPrimitive.double, 0.05)
    }

    @Test
    fun `un carico nullo non produce una stima inventata`() = runTest {
        val r = out(EstimateOneRepMaxTool().execute(
            buildJsonObject {
                put("caricoKg", kotlinx.serialization.json.JsonPrimitive(0.0))
                put("ripetizioni", kotlinx.serialization.json.JsonPrimitive(5))
            }
        ))
        assertFalse("Zero kg non e' un massimale", r["ok"]!!.jsonPrimitive.boolean)
        assertNotNull("Deve spiegare cosa manca", r["error"])
    }

    @Test
    fun `ripetizioni negative vengono rifiutate`() = runTest {
        val r = out(EstimateOneRepMaxTool().execute(
            buildJsonObject {
                put("caricoKg", kotlinx.serialization.json.JsonPrimitive(100.0))
                put("ripetizioni", kotlinx.serialization.json.JsonPrimitive(-3))
            }
        ))
        assertFalse(r["ok"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `gli argomenti mancanti non fanno crashare lo strumento`() = runTest {
        val r = out(EstimateOneRepMaxTool().execute(buildJsonObject { }))
        assertFalse("Senza argomenti non si stima nulla", r["ok"]!!.jsonPrimitive.boolean)
    }

    // ---------- compute_energy_targets ----------

    @Test
    fun `senza profilo lo strumento lo dichiara`() = runTest {
        val tool = ComputeEnergyTargetsTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(buildJsonObject { }))
        assertFalse(r["ok"]!!.jsonPrimitive.boolean)
        assertEquals("Nessun profilo.", r["error"]!!.jsonPrimitive.content)
    }

    @Test
    fun `i target energetici sono calcolati e non stimati per difetto`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val tool = ComputeEnergyTargetsTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(buildJsonObject { }))
        assertTrue(r["ok"]!!.jsonPrimitive.boolean)
        val bmr = r["bmr"]!!.jsonPrimitive.double
        val tdee = r["tdee"]!!.jsonPrimitive.double
        assertTrue("Un BMR umano plausibile per un uomo di 80 kg: $bmr", bmr in 1500.0..2200.0)
        assertTrue("Il TDEE non puo' essere sotto il BMR: bmr=$bmr tdee=$tdee", tdee > bmr)
        assertTrue("Deve dichiarare la formula usata", r["formulaBmr"]!!.jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun `un obiettivo esplicito prevale su quello del profilo`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val tool = ComputeEnergyTargetsTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(
            buildJsonObject { put("obiettivo", kotlinx.serialization.json.JsonPrimitive("CUT")) }
        ))
        assertEquals("CUT", r["obiettivoUsato"]!!.jsonPrimitive.content)
    }

    @Test
    fun `un obiettivo che non esiste non rompe il calcolo`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val tool = ComputeEnergyTargetsTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(
            buildJsonObject { put("obiettivo", kotlinx.serialization.json.JsonPrimitive("BOH")) }
        ))
        assertTrue("Si ricade sull'obiettivo del profilo", r["ok"]!!.jsonPrimitive.boolean)
        assertEquals("HYPERTROPHY", r["obiettivoUsato"]!!.jsonPrimitive.content)
    }

    @Test
    fun `i dati mancanti sono elencati e non sostituiti da default`() = runTest {
        db.userProfileDao().upsert(
            Mappers.run { profilo().copy(weightKg = null).toEntity() }
        )
        val tool = ComputeEnergyTargetsTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(buildJsonObject { }))
        assertFalse("Senza peso non si calcola", r["ok"]!!.jsonPrimitive.boolean)
        assertNotNull("Deve dire quali dati mancano", r["datiMancanti"])
    }

    // ---------- compute_carb_cycling ----------

    @Test
    fun `il carb cycling riporta una distribuzione coerente`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val tool = ComputeCarbCyclingTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(buildJsonObject { }))
        assertTrue(r["ok"]!!.jsonPrimitive.boolean)
        val high = r["giornateHigh"]!!.jsonPrimitive.int
        val med = r["giornateMedium"]!!.jsonPrimitive.int
        val low = r["giornateLow"]!!.jsonPrimitive.int
        assertEquals("Le giornate devono coprire la settimana", 7, high + med + low)
    }

    @Test
    fun `le giornate richieste dal modello vengono davvero applicate`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val tool = ComputeCarbCyclingTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(
            buildJsonObject {
                put("giornateHigh", kotlinx.serialization.json.JsonPrimitive(3))
                put("giornateMedium", kotlinx.serialization.json.JsonPrimitive(2))
                put("giornateLow", kotlinx.serialization.json.JsonPrimitive(2))
            }
        ))
        assertEquals(
            "Lo strumento dichiara di accettare le giornate: deve usarle, non solo ripeterle",
            3, r["giornateHigh"]!!.jsonPrimitive.int
        )
        assertEquals(2, r["giornateMedium"]!!.jsonPrimitive.int)
        assertEquals(2, r["giornateLow"]!!.jsonPrimitive.int)
        assertEquals(
            "Deve dichiarare che la personalizzazione e' stata applicata",
            true,
            r["richiestaPersonalizzazione"]!!.jsonObject["applicata"]!!.jsonPrimitive.boolean
        )
    }

    @Test
    fun `giornate che non sommano a sette vengono rifiutate`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val tool = ComputeCarbCyclingTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(
            buildJsonObject {
                put("giornateHigh", kotlinx.serialization.json.JsonPrimitive(5))
                put("giornateMedium", kotlinx.serialization.json.JsonPrimitive(5))
                put("giornateLow", kotlinx.serialization.json.JsonPrimitive(5))
            }
        ))
        assertTrue("Deve comunque rispondere", r["ok"]!!.jsonPrimitive.boolean)
        assertEquals(
            "Una settimana di 15 giorni non esiste: non va applicata",
            false,
            r["richiestaPersonalizzazione"]!!.jsonObject["applicata"]!!.jsonPrimitive.boolean
        )
        val tot = r["giornateHigh"]!!.jsonPrimitive.int +
            r["giornateMedium"]!!.jsonPrimitive.int + r["giornateLow"]!!.jsonPrimitive.int
        assertEquals("Deve restare una settimana vera", 7, tot)
    }

    @Test
    fun `ogni giornata riporta i macro e sommando tornano le calorie`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val tool = ComputeCarbCyclingTool(UserRepository(db.userProfileDao()))
        val r = out(tool.execute(buildJsonObject { }))
        val targets = r["target"]!!.jsonArray
        assertTrue("Devono esserci i target", targets.isNotEmpty())
        targets.forEach { t ->
            val o = t.jsonObject
            val kcal = o["kcal"]!!.jsonPrimitive.double
            val p = o["proteine"]!!.jsonPrimitive.double
            val c = o["carboidrati"]!!.jsonPrimitive.double
            val f = o["grassi"]!!.jsonPrimitive.double
            val calcolate = p * 4 + c * 4 + f * 9
            assertEquals(
                "I macro devono tornare con le calorie dichiarate (${o["tipo"]})",
                kcal, calcolate, 25.0
            )
        }
    }
}
