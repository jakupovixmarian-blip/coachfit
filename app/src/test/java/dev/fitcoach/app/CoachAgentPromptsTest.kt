package dev.fitcoach.app

import dev.fitcoach.app.core.result.AppError
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.UserProfile
import dev.fitcoach.app.domain.usecase.CoachAction
import dev.fitcoach.app.domain.usecase.CoachAgentPrompts
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The agent decides every number, so the app-side parsers must accept any coherent
 * answer and reject only what cannot be trusted: unknown catalog ids, impossible
 * numbers and malformed JSON. These tests pin exactly that boundary.
 */
class CoachAgentPromptsTest {

    private val prompts = CoachAgentPrompts(Json { ignoreUnknownKeys = true; isLenient = true })

    // ---- training program ----------------------------------------------

    @Test
    fun `program parser keeps real exercises and drops unknown ids`() {
        val raw = """
            {"name":"Push Pull Legs","rationale":"3 sedute",
             "days":[
               {"label":"Spinta","exercises":[
                 {"exerciseId":1,"sets":4,"repsMin":6,"repsMax":8,"rir":2,"restSeconds":150},
                 {"exerciseId":999,"sets":3,"repsMin":8,"repsMax":12,"restSeconds":90}]},
               {"label":"Trazione","exercises":[
                 {"exerciseId":2,"sets":4,"repsMin":8,"repsMax":10,"rir":1,"restSeconds":120}]}]}
        """.trimIndent()

        val draft = prompts.parseProgram(raw, setOf(1L, 2L))

        assertEquals("Push Pull Legs", draft.name)
        assertEquals(2, draft.days.size)
        // The unknown id is dropped, the coherent one survives untouched.
        assertEquals(listOf(1L), draft.days.first().exercises.map { it.exerciseId })
        assertEquals(4, draft.days.first().exercises.first().sets)
        assertEquals(150, draft.days.first().exercises.first().restSeconds)
    }

    @Test
    fun `program parser rejects absurd set counts instead of clamping them`() {
        val raw = """
            {"name":"X","days":[{"label":"A","exercises":[
              {"exerciseId":1,"sets":500,"repsMin":8,"repsMax":12,"restSeconds":90}]}]}
        """.trimIndent()

        try {
            prompts.parseProgram(raw, setOf(1L))
            fail("Expected InvalidResponse")
        } catch (error: AppError) {
            assertTrue(error is AppError.InvalidResponse)
        }
    }

    @Test
    fun `program parser throws when no day is usable`() {
        val raw = """{"name":"X","days":[{"label":"A","exercises":[{"exerciseId":42,"sets":3}]}]}"""
        try {
            prompts.parseProgram(raw, setOf(1L))
            fail("Expected InvalidResponse")
        } catch (error: AppError) {
            assertTrue(error is AppError.InvalidResponse)
        }
    }

    // ---- meal plan ------------------------------------------------------

    @Test
    fun `meal plan parser only accepts catalog foods and positive grams`() {
        val raw = """
            {"name":"Piano","rationale":"4 pasti","days":[
              {"dayIndex":0,"meals":[
                {"slot":"BREAKFAST","foodId":10,"grams":80,"preparation":"cotto"},
                {"slot":"LUNCH","foodId":77,"grams":200},
                {"slot":"DINNER","foodId":11,"grams":-5}]}]}
        """.trimIndent()

        val draft = prompts.parseMealPlan(raw, setOf(10L, 11L))

        assertEquals(1, draft.days.size)
        // Unknown food and negative grams are both dropped.
        assertEquals(1, draft.days.first().items.size)
        assertEquals(10L, draft.days.first().items.first().foodId)
        assertEquals(80.0, draft.days.first().items.first().grams, 0.001)
    }

    // ---- anamnesi -------------------------------------------------------

    @Test
    fun `anamnesi parser reads the learned fields and the completion flag`() {
        val raw = """
            {"reply":"Perfetto, ho tutto.","complete":true,
             "profile":{"name":"Marco","age":34,"sex":"MALE","heightCm":181,"weightKg":82.5,
             "goal":"FAT_LOSS","equipment":"BARBELL,DUMBBELL","availableDays":4}}
        """.trimIndent()

        val reply = prompts.parseAnamnesi(raw)

        assertTrue(reply.complete)
        assertEquals("Marco", reply.profilePatch["name"])
        assertEquals("181", reply.profilePatch["heightCm"])
        assertEquals("FAT_LOSS", reply.profilePatch["goal"])
    }

    @Test
    fun `profile patch applies learned fields and leaves the rest untouched`() {
        val base = UserProfile(name = "Vecchio", age = 40, sex = Sex.FEMALE, weightKg = 70.0)

        val patched = prompts.applyProfilePatch(
            base,
            mapOf(
                "name" to "Marco",
                "age" to "34",
                "sex" to "MALE",
                "weightKg" to "82.5",
                "goal" to "MUSCLE_GAIN",
                "availableDays" to "non-un-numero"
            )
        )

        assertEquals("Marco", patched.name)
        assertEquals(34, patched.age)
        assertEquals(Sex.MALE, patched.sex)
        assertEquals(82.5, patched.weightKg!!, 0.001)
        assertEquals(Goal.MUSCLE_GAIN, patched.goal)
        // A value that cannot be parsed is ignored, never guessed.
        assertNull(patched.availableDays)
    }

    // ---- chat -----------------------------------------------------------

    @Test
    fun `chat parser maps the actions the agent asked for`() {
        val raw = """
            {"reply":"Ho aumentato il recupero e rifatto la scheda.",
             "actions":[
               {"type":"set_rest","exercise":"Panca piana","seconds":180},
               {"type":"generate_program","notes":"solo manubri"},
               {"type":"add_exercise","routine":"Spinta","exercise":"Croci","sets":3},
               {"type":"update_profile","fields":{"equipment":"DUMBBELL"}},
               {"type":"unknown_action"}]}
        """.trimIndent()

        val reply = prompts.parseAgentReply(raw)

        assertEquals(4, reply.actions.size)
        assertTrue(reply.actions[0] is CoachAction.SetRest)
        assertEquals(180, (reply.actions[0] as CoachAction.SetRest).seconds)
        assertEquals("solo manubri", (reply.actions[1] as CoachAction.GenerateProgram).notes)
        assertEquals("Croci", (reply.actions[2] as CoachAction.AddExercise).exerciseName)
        assertEquals("DUMBBELL", (reply.actions[3] as CoachAction.UpdateProfile).patch["equipment"])
    }

    @Test
    fun `chat parser drops a set_rest without a valid duration`() {
        val raw = """{"reply":"ok","actions":[{"type":"set_rest","exercise":"Panca","seconds":0}]}"""
        val reply = prompts.parseAgentReply(raw)
        assertTrue(reply.actions.isEmpty())
    }
}
