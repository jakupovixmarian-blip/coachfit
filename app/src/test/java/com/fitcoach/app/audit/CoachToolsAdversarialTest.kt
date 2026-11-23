package com.fitcoach.app.audit

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.coach.tools.EstimateOneRepMaxTool
import com.fitcoach.app.domain.coach.tools.JsonOut
import com.fitcoach.app.domain.coach.tools.ValidateWorkoutPlanTool
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gli strumenti che il modello chiama durante una conversazione. Il loro
 * input arriva da una macchina: puo' avere campi mancanti, tipi sbagliati,
 * liste vuote o valori fuori scala. Un errore qui non si vede in chat, si
 * vede come un consiglio sbagliato.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoachToolsAdversarialTest {

    private lateinit var db: FitCoachDatabase
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FitCoachDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun args(raw: String): JsonObject = json.parseToJsonElement(raw).jsonObject

    private fun risultato(raw: String): JsonObject =
        json.parseToJsonElement(raw).jsonObject

    private fun profilo() = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.HYPERTROPHY,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    // ---------- estimate_1rm ----------

    private val rm = EstimateOneRepMaxTool()

    @Test
    fun `il massimale si calcola con la formula di Epley`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": 100, "ripetizioni": 10}""")))
        assertEquals(true, r["ok"]!!.jsonPrimitive.content.toBoolean())
        // 100 * (1 + 10/30) = 133.3
        assertEquals(133.3, r["stima1Rm"]!!.jsonPrimitive.content.toDouble(), 0.1)
    }

    @Test
    fun `un carico a zero viene rifiutato`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": 0, "ripetizioni": 10}""")))
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
        assertNotNull("Il rifiuto deve spiegare perche'", r["error"])
    }

    @Test
    fun `un carico negativo viene rifiutato`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": -50, "ripetizioni": 10}""")))
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `zero ripetizioni viene rifiutato`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": 100, "ripetizioni": 0}""")))
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `un campo mancante non produce un massimale inventato`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": 100}""")))
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
        assertNull("Non deve esserci una stima senza ripetizioni", r["stima1Rm"])
    }

    @Test
    fun `un testo al posto del numero viene rifiutato`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": "pesante", "ripetizioni": 10}""")))
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `il carico proposto per un target si calcola dal massimale`() = runTest {
        // 1RM 133.3 a 10 ripetizioni; per 5 ripetizioni: 133.3 / (1+5/30) = 114.3
        val r = risultato(rm.execute(args("""{"caricoKg": 100, "ripetizioni": 10, "ripTarget": 5}""")))
        assertEquals(114.3, r["caricoProposto"]!!.jsonPrimitive.content.toDouble(), 0.2)
    }

    @Test
    fun `un target a zero non produce un carico proposto`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": 100, "ripetizioni": 10, "ripTarget": 0}""")))
        assertEquals(true, r["ok"]!!.jsonPrimitive.content.toBoolean())
        assertNull("Zero ripetizioni target non ha senso", r["caricoProposto"])
    }

    @Test
    fun `un target negativo non produce un carico proposto`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": 100, "ripetizioni": 10, "ripTarget": -3}""")))
        assertNull(r["caricoProposto"])
    }

    @Test
    fun `una singola ripetizione coincide col massimale`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": 120, "ripetizioni": 1}""")))
        assertEquals(124.0, r["stima1Rm"]!!.jsonPrimitive.content.toDouble(), 0.1)
    }

    @Test
    fun `un numero enorme non produce un valore non finito`() = runTest {
        val r = risultato(rm.execute(args("""{"caricoKg": 1e308, "ripetizioni": 30}""")))
        val valore = r["stima1Rm"]?.jsonPrimitive?.content
        assertTrue(
            "Un valore non finito non deve finire nel JSON: era $valore",
            valore == null || valore.toDoubleOrNull()?.isFinite() == true
        )
    }

    // ---------- validate_workout_plan ----------

    private fun validator() = ValidateWorkoutPlanTool(
        UserRepository(db.userProfileDao()),
        ExerciseRepository(db.exerciseDao())
    )

    @Test
    fun `senza profilo la validazione rifiuta con un motivo`() = runTest {
        val r = risultato(validator().execute(args("""{"giornate": []}""")))
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
        assertNotNull("Deve dire che manca il profilo", r["error"])
    }

    @Test
    fun `una lista di giornate mancante viene rifiutata`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val r = risultato(validator().execute(args("""{}""")))
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
        assertNotNull(r["error"])
    }

    @Test
    fun `una giornata vuota non lancia e produce un esito`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val r = risultato(validator().execute(args("""{"giornate": [{}]}""")))
        assertNotNull("Deve comunque rispondere", r["ok"])
    }

    @Test
    fun `una giornata senza dayIndex viene scartata senza crash`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val r = risultato(
            validator().execute(args("""{"giornate": [{"gruppiMuscolari": ["CHEST"], "esercizi": []}]}"""))
        )
        assertNotNull(r["ok"])
    }

    @Test
    fun `un esercizio inesistente viene segnalato come violazione`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val r = risultato(
            validator().execute(
                args("""{"giornate": [{"dayIndex": 0, "gruppiMuscolari": ["CHEST"],
                        "esercizi": [{"exerciseId": 999999, "serie": 3}]}]}""")
            )
        )
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
        val violazioni = r["violazioni"].toString()
        assertTrue(
            "Deve dire che l'esercizio non esiste: $violazioni",
            violazioni.contains("inesistente")
        )
    }

    @Test
    fun `un numero di serie fuori scala viene segnalato`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val r = risultato(
            validator().execute(
                args("""{"giornate": [{"dayIndex": 0, "gruppiMuscolari": ["CHEST"],
                        "esercizi": [{"exerciseId": 999999, "serie": 50}]}]}""")
            )
        )
        assertEquals(false, r["ok"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `un gruppo muscolare sconosciuto non fa crashare la validazione`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val r = risultato(
            validator().execute(
                args("""{"giornate": [{"dayIndex": 0, "gruppiMuscolari": ["NON_ESISTE"],
                        "esercizi": []}]}""")
            )
        )
        assertNotNull("Un gruppo ignoto va ignorato, non deve lanciare", r["ok"])
    }

    @Test
    fun `il risultato contiene sempre volume e gruppi coperti`() = runTest {
        db.userProfileDao().upsert(Mappers.run { profilo().toEntity() })
        val r = risultato(validator().execute(args("""{"giornate": []}""")))
        assertNotNull("Il volume effettivo deve esserci", r["volumeEffettivoPerGruppo"])
        assertNotNull("I gruppi coperti devono esserci", r["gruppiCoperti"])
    }

    // ---------- JsonOut ----------

    @Test
    fun `un valore non finito diventa zero e non rompe il json`() {
        val out = JsonOut.encode(JsonOut.obj("v" to JsonOut.num(Double.POSITIVE_INFINITY)))
        val v = risultato(out)["v"]!!.jsonPrimitive.content.toDouble()
        assertTrue("Il valore deve essere finito: era $v", v.isFinite())
    }

    @Test
    fun `i campi nulli vengono omessi e non scritti come null`() {
        val out = JsonOut.encode(JsonOut.obj("a" to JsonOut.str(null), "b" to JsonOut.str("x")))
        val o = risultato(out)
        assertNull("Un campo nullo non deve comparire", o["a"])
        assertNotNull(o["b"])
    }

    @Test
    fun `i numeri vengono arrotondati a un decimale`() {
        val out = JsonOut.encode(JsonOut.obj("v" to JsonOut.num(1.23456)))
        assertEquals(1.2, risultato(out)["v"]!!.jsonPrimitive.content.toDouble(), 0.0001)
    }
}
