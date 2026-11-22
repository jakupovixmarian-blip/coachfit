package com.fitcoach.app.collaudo

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.domain.coach.AiProgramProposalJson
import com.fitcoach.app.domain.coach.ProgramGenerationEngine
import com.fitcoach.app.domain.nutrition.CarbCyclingEngine
import com.fitcoach.app.domain.nutrition.EnergyCalculator
import com.fitcoach.app.domain.nutrition.FoodCatalogSelector
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.nutrition.PortionResolution
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Il difetto segnalato, riprodotto end-to-end.
 *
 * Il modello consegnava 7 giorni con gli slot giusti e le voci sotto la chiave
 * "voci" (la forma stampata da solve_meal_portions). Il parser le scartava, le
 * grammature non venivano ricalcolate ("nessun alimento riconosciuto") e la
 * validazione respingeva tutti i giorni con 0 kcal e 0 g di macro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VociKeyEndToEndCollaudoTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var foodRepository: FoodRepository
    private lateinit var userRepository: UserRepository
    private val profile = MarjanProfile.build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        foodRepository = FoodRepository(db.foodDao())
        userRepository = UserRepository(db.userProfileDao())
        db.foodDao().insertAll(FoodSeedData.foods.map { it.toEntity() })
        userRepository.save(profile)
    }

    @After
    fun tearDown() = db.close()

    /** Una giornata scritta come la scriverebbe il modello copiando il solver. */
    private fun dayAsModelWrites(dayIndex: Int, dayType: String) = """
        { "dayIndex": $dayIndex, "dayType": "$dayType", "meals": [
          { "slot": "BREAKFAST", "voci": [
              { "foodName": "Yogurt greco 0% grassi", "grams": 200 },
              { "foodName": "Fiocchi d'avena", "grams": 80 },
              { "foodName": "Banana", "grams": 120 } ] },
          { "slot": "MORNING_SNACK", "voci": [ { "foodName": "Mela", "grams": 170 } ] },
          { "slot": "LUNCH", "voci": [
              { "foodName": "Petto di pollo", "grams": 180 },
              { "foodName": "Riso bianco (crudo)", "grams": 90 } ] },
          { "slot": "AFTERNOON_SNACK", "voci": [ { "foodName": "Noci", "grams": 25 } ] },
          { "slot": "DINNER", "voci": [
              { "foodName": "Merluzzo", "grams": 200 },
              { "foodName": "Olio extravergine d'oliva", "grams": 10 },
              { "foodName": "Broccoli", "grams": 200 } ] } ] }
    """.trimIndent()

    private fun fullProposal(): String {
        val days = listOf("HIGH", "MEDIUM", "LOW", "MEDIUM", "HIGH", "LOW", "LOW")
            .mapIndexed { i, t -> dayAsModelWrites(i, t) }
        return """
            { "scope": "NUTRITION",
              "nutrition": { "days": [ ${days.joinToString(",")} ] },
              "workout": { "split": "CUSTOM", "days": [] } }
        """.trimIndent()
    }

    @Test
    fun `un piano con le voci sotto la chiave voci supera la validazione`() = runBlocking {
        val foods = FoodCatalogSelector.forGeneration(foodRepository.getAll(), profile)
        val energy = EnergyCalculator.compute(profile)!!
        val cycling = CarbCyclingEngine.build(profile, energy)

        val decoded = AiProgramProposalJson.decode(json, fullProposal())
        assertThat(decoded).isNotNull()

        // Le voci devono essere arrivate: era il punto del difetto.
        val firstMeal = decoded!!.nutrition.days.first().meals.first()
        assertThat(firstMeal.entries).hasSize(3)

        val (nutrition, notes) = PortionResolution.resolve(decoded.nutrition, foods, cycling)

        // Nessun "nessun alimento riconosciuto": tutte le giornate ricalcolate.
        assertThat(notes.filter { it.contains("nessun alimento riconosciuto") }).isEmpty()

        // Ogni pasto mantiene i suoi alimenti dopo il ricalcolo.
        nutrition.days.forEach { day ->
            day.meals.forEach { meal ->
                assertThat(meal.entries).isNotEmpty()
            }
        }

        val validated = ProgramGenerationEngine.validate(
            profile = profile,
            proposal = decoded.copy(nutrition = nutrition),
            foods = foods,
            exercises = emptyList(),
            scope = ProgramGenerationEngine.Scope.NUTRITION
        )
        assertThat(validated).isNotNull()
        val day0 = validated!!.nutrition!!.days.first()
        // Non piu 0 kcal: il giorno HIGH deve stare vicino al target.
        assertThat(day0.actual.kcal).isGreaterThan(1500.0)
        assertThat(day0.actual.protein).isGreaterThan(100.0)
    }
}
