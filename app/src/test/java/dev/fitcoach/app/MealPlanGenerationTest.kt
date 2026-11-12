package dev.fitcoach.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import dev.fitcoach.app.data.repository.FoodRepositoryImpl
import dev.fitcoach.app.data.repository.KnowledgeRepositoryImpl
import dev.fitcoach.app.data.repository.MealPlanRepositoryImpl
import dev.fitcoach.app.data.repository.ProfileRepositoryImpl
import dev.fitcoach.app.data.seed.FoodSeed
import dev.fitcoach.app.domain.model.CoachChatMessage
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.usecase.AiGateway
import dev.fitcoach.app.domain.usecase.CoachAgent
import dev.fitcoach.app.domain.usecase.CoachAgentPrompts
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression test for the reported bug: "il programma va tutto tranne che non fa
 * il piano alimentare".
 *
 * Root cause: the whole week was asked in one answer. A seven-day plan is far
 * larger than a training program, so a model with a small output budget returned
 * a truncated JSON and the app rejected it. The agent now asks one day at a time,
 * so every answer stays small.
 */
@RunWith(RobolectricTestRunner::class)
class MealPlanGenerationTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var foodRepo: FoodRepositoryImpl
    private lateinit var planRepo: MealPlanRepositoryImpl
    private lateinit var profileRepo: ProfileRepositoryImpl

    /**
     * Fake provider that mimics a model with a hard output ceiling: any answer
     * longer than [maxChars] comes back cut in half, exactly like a truncated
     * response. It also refuses to answer more than one day per call, so a
     * single-call implementation would receive a broken answer.
     */
    private class TruncatingGateway(private val maxChars: Int = 700) : AiGateway {
        var calls = 0
            private set

        override suspend fun isConfigured(): Boolean = true

        override suspend fun complete(systemPrompt: String, userPrompt: String, jsonMode: Boolean): String {
            calls++
            val requestedDay = Regex("""dayIndex deve valere esattamente (\d+)""")
                .find(userPrompt)?.groupValues?.get(1)?.toInt() ?: 0
            val full = """
                {"name":"Settimana tipo","rationale":"prova","dayIndex":$requestedDay,
                 "meals":[
                  {"slot":"BREAKFAST","foodId":4,"grams":80},
                  {"slot":"LUNCH","foodId":12,"grams":150},
                  {"slot":"DINNER","foodId":1,"grams":200}]}
            """.trimIndent()
            // A whole-week answer would be much longer than this and get cut.
            return if (full.length > maxChars) full.take(maxChars / 2) else full
        }

        override suspend fun completeWithHistory(
            systemPrompt: String,
            history: List<CoachChatMessage>,
            userPrompt: String,
            jsonMode: Boolean
        ): String = complete(systemPrompt, userPrompt, jsonMode)
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        foodRepo = FoodRepositoryImpl(db)
        planRepo = MealPlanRepositoryImpl(db)
        profileRepo = ProfileRepositoryImpl(db)
    }

    @After
    fun tearDown() = db.close()

    private fun agent(gateway: AiGateway) = CoachAgent(
        gateway = gateway,
        prompts = CoachAgentPrompts(Json { ignoreUnknownKeys = true; isLenient = true }),
        profileRepository = profileRepo,
        exerciseRepository = dev.fitcoach.app.data.repository.ExerciseRepositoryImpl(db),
        routineRepository = dev.fitcoach.app.data.repository.RoutineRepositoryImpl(db),
        sessionRepository = dev.fitcoach.app.data.repository.SessionRepositoryImpl(db),
        foodRepository = foodRepo,
        mealPlanRepository = planRepo,
        knowledgeRepository = KnowledgeRepositoryImpl(db)
    )

    @Test
    fun `the week is built even when the model truncates long answers`() = runBlocking {
        foodRepo.ensureSeeded()
        profileRepo.saveProfile(UserProfile(name = "Marco", age = 30, weightKg = 80.0, heightCm = 180.0))

        val gateway = TruncatingGateway()
        val outcome = agent(gateway).generateMealPlan()

        // One call per day: that is what keeps each answer inside the budget.
        assertEquals(7, gateway.calls)
        assertEquals(7, outcome.dayCount)
        assertTrue(outcome.itemCount >= 7)

        val active = planRepo.getActive()
        assertTrue(active != null)
        val days = planRepo.getDays(active!!.id)
        assertEquals(7, days.size)
        // Every day must carry real meals, referencing seeded foods.
        val knownIds = foodRepo.getAll().map { it.id }.toSet()
        days.forEach { day ->
            val items = planRepo.getItems(day.id)
            assertTrue("giorno ${day.dayIndex} senza pasti", items.isNotEmpty())
            assertTrue(items.all { it.foodId in knownIds })
        }
    }

    @Test
    fun `an unknown food id is dropped instead of being stored`() = runBlocking {
        foodRepo.ensureSeeded()
        profileRepo.saveProfile(UserProfile(name = "Marco", age = 30, weightKg = 80.0, heightCm = 180.0))

        val gateway = object : AiGateway {
            override suspend fun isConfigured() = true
            override suspend fun complete(systemPrompt: String, userPrompt: String, jsonMode: Boolean): String {
                val day = Regex("""esattamente (\d+)""").find(userPrompt)?.groupValues?.get(1) ?: "0"
                return """{"name":"X","rationale":"y","dayIndex":$day,"meals":[
                    {"slot":"LUNCH","foodId":999999,"grams":100},
                    {"slot":"DINNER","foodId":1,"grams":200}]}"""
            }
            override suspend fun completeWithHistory(
                systemPrompt: String, history: List<CoachChatMessage>, userPrompt: String, jsonMode: Boolean
            ) = complete(systemPrompt, userPrompt, jsonMode)
        }

        val outcome = agent(gateway).generateMealPlan()

        // The invented id must never reach the database.
        val knownIds = foodRepo.getAll().map { it.id }.toSet()
        val active = planRepo.getActive()!!
        planRepo.getDays(active.id).forEach { day ->
            planRepo.getItems(day.id).forEach { item ->
                assertTrue("foodId inventato salvato: ${item.foodId}", item.foodId in knownIds)
            }
        }
        assertEquals(7, outcome.dayCount)
    }
}
