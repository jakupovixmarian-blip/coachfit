package com.fitcoach.app.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.coach.tools.ComputePortionTool
import com.fitcoach.app.domain.coach.tools.ComputeSubstitutionTool
import com.fitcoach.app.domain.coach.tools.ValidateNutritionDayTool
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica gli strumenti con cui l'AI costruisce i pasti da sola (senza piano
 * preimpostato): calcolo porzioni, sostituzioni equivalenti e validazione di
 * una giornata alimentare sui valori reali del catalogo seed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NutritionBuildToolsIntegrationTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var foodRepository: FoodRepository
    private lateinit var userRepository: UserRepository

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        foodRepository = FoodRepository(db.foodDao())
        userRepository = UserRepository(db.userProfileDao())
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        userRepository.save(UserProfile(id = 1L, name = "Test", ageYears = 30, sex = Sex.MALE, heightCm = 175.0, weightKg = 80.0))
    }

    @After
    fun tearDown() = db.close()

    private fun args(vararg pairs: Pair<String, Any>): JsonObject =
        JsonObject(pairs.associate { (k, v) ->
            k to when (v) {
                is String -> kotlinx.serialization.json.JsonPrimitive(v)
                is Double -> kotlinx.serialization.json.JsonPrimitive(v)
                is Int -> kotlinx.serialization.json.JsonPrimitive(v)
                else -> error("tipo non supportato")
            }
        })

    @Test
    fun `compute_portion calcola i grammi per centrare le proteine`() = runTest {
        val tool = ComputePortionTool(foodRepository)
        val out = json.parseToJsonElement(
            tool.execute(args("foodName" to "Petto di pollo", "target" to "PROTEIN", "grammi" to 46.6))
        ).let { it as JsonObject }

        assertThat(out["ok"].toString()).isEqualTo("true")
        // 23.3 g di proteine per 100 g -> ~200 g per 46.6 g.
        val grams = out["grammi"]!!.toString().toDouble()
        assertThat(grams).isWithin(2.0).of(200.0)
        val protein = out["proteine"]!!.toString().toDouble()
        assertThat(protein).isWithin(1.0).of(46.6)
    }

    @Test
    fun `compute_portion segnala un alimento inesistente senza inventare dati`() = runTest {
        val tool = ComputePortionTool(foodRepository)
        val out = json.parseToJsonElement(
            tool.execute(args("foodName" to "Polvere di unicorno", "target" to "PROTEIN", "grammi" to 30.0))
        ) as JsonObject
        assertThat(out["ok"].toString()).isEqualTo("false")
        assertThat(out["error"]).isNotNull()
    }

    @Test
    fun `compute_substitution propone una quantita equivalente per macro dominante`() = runTest {
        val tool = ComputeSubstitutionTool(foodRepository, userRepository)
        val out = json.parseToJsonElement(
            tool.execute(
                args(
                    "alimentoOriginale" to "Petto di pollo",
                    "grammiOriginali" to 200.0,
                    "alimentoSostituto" to "Fesa di tacchino"
                )
            )
        ) as JsonObject
        assertThat(out["ok"].toString()).isEqualTo("true")
        val grams = out["grammiSostituto"]!!.toString().toDouble()
        assertThat(grams).isGreaterThan(0.0)
        // Il sostituto deve mantenere il macro dominante (proteine) ~ quella dei 200 g di pollo.
        val protein = out["proteine"]!!.toString().toDouble()
        assertThat(protein).isWithin(6.0).of(46.6)
    }

    @Test
    fun `validate_nutrition_day somma le voci e conferma la coerenza col target`() = runTest {
        val tool = ValidateNutritionDayTool(foodRepository, userRepository)
        val entries = kotlinx.serialization.json.buildJsonArray {
            add(
                kotlinx.serialization.json.buildJsonObject {
                    put("alimento", kotlinx.serialization.json.JsonPrimitive("Petto di pollo"))
                    put("grammi", kotlinx.serialization.json.JsonPrimitive(200.0))
                }
            )
        }
        val payload = JsonObject(
            mapOf(
                "targetKcal" to kotlinx.serialization.json.JsonPrimitive(220.0),
                "targetProteine" to kotlinx.serialization.json.JsonPrimitive(46.6),
                "voci" to entries
            )
        )
        val out = json.parseToJsonElement(tool.execute(payload)) as JsonObject
        assertThat(out["ok"].toString()).isEqualTo("true")
        val kcal = out["kcal"]!!.toString().toDouble()
        assertThat(kcal).isWithin(2.0).of(220.0)
    }

    @Test
    fun `validate_nutrition_day segnala uno scostamento fuori tolleranza`() = runTest {
        val tool = ValidateNutritionDayTool(foodRepository, userRepository)
        val entries = kotlinx.serialization.json.buildJsonArray {
            add(
                kotlinx.serialization.json.buildJsonObject {
                    put("alimento", kotlinx.serialization.json.JsonPrimitive("Petto di pollo"))
                    put("grammi", kotlinx.serialization.json.JsonPrimitive(100.0))
                }
            )
        }
        val payload = JsonObject(
            mapOf(
                "targetKcal" to kotlinx.serialization.json.JsonPrimitive(1000.0),
                "voci" to entries
            )
        )
        val out = json.parseToJsonElement(tool.execute(payload)) as JsonObject
        assertThat(out["ok"].toString()).isEqualTo("false")
        assertThat(out["violazioni"].toString()).contains("kcal")
    }
}
