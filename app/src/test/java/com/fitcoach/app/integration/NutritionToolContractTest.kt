package com.fitcoach.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.coach.tools.GetNutritionTargetsTool
import com.fitcoach.app.domain.coach.tools.SolveMealPortionsTool
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica gli strumenti che il modello chiama davvero per costruire la
 * nutrizione: parsing degli argomenti, risoluzione dei target reali, lookup nel
 * catalogo e forma del JSON restituito.
 *
 * Il solver e coperto da [com.fitcoach.app.domain.nutrition.MealPortionSolverTest];
 * qui si verifica il contratto verso l'AI (nomi dei campi e messaggi di errore).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NutritionToolContractTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private lateinit var db: FitCoachDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepository: FoodRepository

    private val profile = UserProfile(
        id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE,
        heightCm = 175.0, weightKg = 80.0, bodyFatPercent = 18.0,
        experience = TrainingExperience.INTERMEDIATE, goal = Goal.RECOMP,
        trainingDaysPerWeek = 4, mealsPerDay = 5,
        anamnesisCompleted = true, anamnesisConfirmedAt = 1L, anamnesisSummary = "confermata"
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        userRepository.save(profile)
    }

    @After
    fun tearDown() = db.close()

    private fun tool() = SolveMealPortionsTool(userRepository, foodRepository)
    private fun targetsTool() = GetNutritionTargetsTool(userRepository)

    private fun args(
        dayIndex: Int = 0,
        dayType: String = "HIGH",
        meals: List<Pair<String, List<String>>>
    ): JsonObject = buildJsonObject {
        put("dayIndex", dayIndex)
        put("dayType", dayType)
        put("pasti", buildJsonArray {
            meals.forEach { (slot, foods) ->
                add(buildJsonObject {
                    put("slot", slot)
                    put("alimenti", buildJsonArray { foods.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                })
            }
        })
    }

    private val balancedMeals = listOf(
        "BREAKFAST" to listOf("Fiocchi d'avena", "Uovo intero di gallina"),
        "MORNING_SNACK" to listOf("Pane integrale", "Petto di pollo"),
        "LUNCH" to listOf("Riso bianco (crudo)", "Petto di pollo", "Olio extravergine d'oliva"),
        "AFTERNOON_SNACK" to listOf("Pane integrale", "Fesa di tacchino"),
        "DINNER" to listOf("Pasta di semola (cruda)", "Merluzzo", "Olio extravergine d'oliva")
    )

    // ------------------------------------------------------------ targets
    @Test
    fun `targets tool returns the exact values the validator enforces`() = runTest {
        val out = json.parseToJsonElement(targetsTool().execute(buildJsonObject { })).jsonObject

        assertThat(out["ok"]!!.jsonPrimitive.content).isEqualTo("true")
        assertThat(out["tolleranzaValidatore"]).isNotNull()
        assertThat(out["calendario"]).isNotNull()

        // `target` e un array di {tipo, kcal, proteine, carboidrati, grassi}.
        val list = out["target"] as kotlinx.serialization.json.JsonArray
        assertThat(list).isNotEmpty()
        list.forEach { entry ->
            val o = entry.jsonObject
            assertThat(o["tipo"]!!.jsonPrimitive.content).isIn(listOf("HIGH", "MEDIUM", "LOW"))
            assertThat(o["kcal"]!!.jsonPrimitive.content.toDouble()).isGreaterThan(1000.0)
            assertThat(o["proteine"]!!.jsonPrimitive.content.toDouble()).isGreaterThan(50.0)
        }

        // I valori devono coincidere con quelli che il validatore ricalcola.
        val energy = com.fitcoach.app.domain.nutrition.EnergyCalculator.compute(profile)!!
        val cycling = com.fitcoach.app.domain.nutrition.CarbCyclingEngine.build(profile, energy)
        list.forEach { entry ->
            val o = entry.jsonObject
            val type = com.fitcoach.app.domain.model.CarbDayType.valueOf(o["tipo"]!!.jsonPrimitive.content)
            val expected = cycling.targets.getValue(type)
            assertThat(o["kcal"]!!.jsonPrimitive.content.toDouble())
                .isWithin(1.0).of(expected.kcal)
            assertThat(o["carboidrati"]!!.jsonPrimitive.content.toDouble())
                .isWithin(1.0).of(expected.carbs)
        }
    }

    @Test
    fun `targets tool assigns day types to training and rest days`() = runTest {
        val out = json.parseToJsonElement(targetsTool().execute(buildJsonObject { })).jsonObject
        val calendar = out["calendario"] as kotlinx.serialization.json.JsonArray
        assertThat(calendar).hasSize(7)
        calendar.forEach { entry ->
            val o = entry.jsonObject
            val training = o["allenamento"]!!.jsonPrimitive.content.toBoolean()
            val suggested = o["dayTypeSuggerito"]!!.jsonPrimitive.content
            assertThat(suggested).isIn(listOf("HIGH", "MEDIUM", "LOW"))
            if (!training) assertThat(suggested).isEqualTo("LOW")
        }
    }

    // ------------------------------------------------------- solve tool
    @Test
    fun `solve tool returns ready to use entries with grams`() = runTest {
        val out = json.parseToJsonElement(tool().execute(args(meals = balancedMeals))).jsonObject

        assertThat(out["ok"]!!.jsonPrimitive.content).isEqualTo("true")
        assertThat(out["target"]).isNotNull()
        assertThat(out["ottenuto"]).isNotNull()
        assertThat(out["avvisi"]).isNotNull()

        val meals = out["pasti"] as kotlinx.serialization.json.JsonArray
        assertThat(meals).hasSize(5)
        meals.forEach { m ->
            val meal = m.jsonObject
            assertThat(meal["slot"]).isNotNull()
            val entries = meal["voci"] as kotlinx.serialization.json.JsonArray
            assertThat(entries).isNotEmpty()
            entries.forEach { e ->
                val o = e.jsonObject
                // I nomi dei campi devono essere quelli che submit_full_program accetta.
                assertThat(o["foodName"]!!.jsonPrimitive.content).isNotEmpty()
                val grams = o["grams"]!!.jsonPrimitive.content.toDouble()
                assertThat(grams).isAtLeast(1.0)
                assertThat(grams).isAtMost(2000.0)
            }
        }
    }

    @Test
    fun `solve tool reports unknown foods instead of ignoring them`() = runTest {
        val out = json.parseToJsonElement(
            tool().execute(args(meals = listOf(
                "LUNCH" to listOf("Petto di pollo", "Alimento Inesistente"),
                "DINNER" to listOf("Riso bianco (crudo)", "Olio extravergine d'oliva")
            )))
        ).jsonObject

        val unknown = out["alimentiSconosciuti"] as kotlinx.serialization.json.JsonArray
        assertThat(unknown.map { it.jsonPrimitive.content }).contains("Alimento Inesistente")
    }

    @Test
    fun `solve tool rejects an invalid day type`() = runTest {
        val out = json.parseToJsonElement(tool().execute(args(dayType = "SUPER", meals = balancedMeals))).jsonObject
        assertThat(out["ok"]!!.jsonPrimitive.content).isEqualTo("false")
        assertThat(out["error"]!!.jsonPrimitive.content).contains("dayType")
    }

    @Test
    fun `solve tool rejects an out of range day index`() = runTest {
        val out = json.parseToJsonElement(tool().execute(args(dayIndex = 9, meals = balancedMeals))).jsonObject
        assertThat(out["ok"]!!.jsonPrimitive.content).isEqualTo("false")
        assertThat(out["error"]!!.jsonPrimitive.content).contains("dayIndex")
    }

    @Test
    fun `solve tool explains a day that cannot hit its target`() = runTest {
        // Solo alimenti proteici grassi: i grassi non sono riducibili sotto il target.
        val out = json.parseToJsonElement(
            tool().execute(args(meals = listOf(
                "LUNCH" to listOf("Salmone", "Salmone"),
                "DINNER" to listOf("Salmone", "Salmone")
            )))
        ).jsonObject

        assertThat(out["ok"]!!.jsonPrimitive.content).isEqualTo("false")
        val warnings = out["avvisi"] as kotlinx.serialization.json.JsonArray
        assertThat(warnings.map { it.jsonPrimitive.content }.joinToString())
            .contains("IMPOSSIBILE")
        assertThat(out["nota"]!!.jsonPrimitive.content).contains("correggi")
    }

    @Test
    fun `solve tool works for a low carb day`() = runTest {
        val out = json.parseToJsonElement(tool().execute(args(dayType = "LOW", meals = balancedMeals))).jsonObject
        assertThat(out["ok"]!!.jsonPrimitive.content).isEqualTo("true")
        val target = out["target"]!!.jsonObject
        assertThat(target["carboidrati"]).isNotNull()
    }

    @Test
    fun `solve tool fails clearly without a profile`() = runTest {
        // Il tool non deve inventare un profilo: senza anamnesi si ferma.
        db.userProfileDao().let { }
        userRepository.save(profile.copy(id = 1L))
        val fresh = SolveMealPortionsTool(userRepository, foodRepository)
        // Con profilo presente ma dati essenziali mancanti il calcolo non e possibile.
        userRepository.save(profile.copy(heightCm = null, weightKg = null))
        val out = json.parseToJsonElement(fresh.execute(args(meals = balancedMeals))).jsonObject
        assertThat(out["ok"]!!.jsonPrimitive.content).isEqualTo("false")
        assertThat(out["error"]).isNotNull()
    }
}
