package dev.fitcoach.app

import dev.fitcoach.app.domain.workout.Equipment
import dev.fitcoach.app.domain.workout.Experience
import dev.fitcoach.app.domain.workout.Goal
import dev.fitcoach.app.domain.workout.MuscleGroup
import dev.fitcoach.app.domain.workout.PlanGenerator
import dev.fitcoach.app.domain.workout.PlanSeverity
import dev.fitcoach.app.domain.workout.TraineeProfile
import dev.fitcoach.app.domain.workout.VolumeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator must produce a plan that respects the user's real constraints, not
 * just a plausible-looking list of exercises.
 */
class PlanGeneratorTest {

    private val fullGym = setOf(
        Equipment.BARBELL,
        Equipment.DUMBBELL,
        Equipment.MACHINE,
        Equipment.CABLE
    )

    private fun profile(
        experience: Experience = Experience.INTERMEDIATE,
        days: Int = 4,
        minutes: Int = 60,
        equipment: Set<Equipment> = fullGym,
        limitations: List<String> = emptyList(),
        excluded: Set<String> = emptySet()
    ) = TraineeProfile(
        experience = experience,
        daysPerWeek = days,
        sessionMinutes = minutes,
        equipment = equipment,
        goal = Goal.HYPERTROPHY,
        limitations = limitations,
        excludedExercises = excluded
    )

    @Test
    fun `genera una seduta per ogni giorno richiesto`() {
        val plan = PlanGenerator().generate(profile(days = 4))

        assertEquals(4, plan.days.size)
        assertEquals(4, plan.days.map { it.name }.toSet().size)
        assertTrue(plan.days.all { it.exercises.isNotEmpty() })
        assertTrue(plan.blockers.isEmpty())
    }

    @Test
    fun `nessun esercizio si ripete nella stessa seduta`() {
        val plan = PlanGenerator().generate(profile(days = 5))

        plan.days.forEach { day ->
            val ids = day.exercises.map { it.exercise.id }
            assertEquals("duplicati in ${day.name}", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `il volume settimanale resta nella forbice del livello`() {
        val plan = PlanGenerator().generate(profile(experience = Experience.INTERMEDIATE))
        val max = VolumeRules.maxWeeklySets(Experience.INTERMEDIATE)

        plan.weeklySetsPerMuscle.forEach { (muscle, sets) ->
            assertTrue("$muscle sotto il minimo: $sets", sets >= VolumeRules.MIN_WEEKLY_SETS)
            assertTrue("$muscle sopra il massimo: $sets", sets <= max)
        }
        assertFalse(plan.hasErrors)
    }

    @Test
    fun `con sola attrezzatura a corpo libero non compare alcun attrezzo`() {
        val plan = PlanGenerator().generate(profile(equipment = setOf(Equipment.BODYWEIGHT)))

        val used = plan.days.flatMap { it.exercises }.map { it.exercise.equipment }.toSet()
        assertEquals(setOf(Equipment.BODYWEIGHT), used)
        assertTrue(plan.warnings.any { it.contains("SHOULDERS") })
    }

    @Test
    fun `una limitazione alla spalla esclude gli esercizi che la caricano`() {
        val plan = PlanGenerator().generate(profile(limitations = listOf("spalla")))

        val loaded = plan.days.flatMap { it.exercises }
            .filter { exercise -> exercise.exercise.contraindications.any { it.equals("spalla", ignoreCase = true) } }
        assertTrue("esercizi vietati ancora presenti: ${loaded.map { it.exercise.name }}", loaded.isEmpty())
        assertTrue(plan.days.all { it.exercises.isNotEmpty() })
    }

    @Test
    fun `un esercizio escluso dall'utente non compare`() {
        val plan = PlanGenerator().generate(profile(excluded = setOf("Back squat")))

        val names = plan.days.flatMap { it.exercises }.map { it.exercise.name }
        assertFalse(names.contains("Squat bilanciere"))
    }

    @Test
    fun `una frequenza fuori intervallo viene corretta e segnalata`() {
        val plan = PlanGenerator().generate(profile(days = 9))

        assertEquals(6, plan.days.size)
        assertTrue(plan.warnings.any { it.contains("2-6") })
    }

    @Test
    fun `un principiante riceve meno serie di un avanzato`() {
        val beginner = PlanGenerator().generate(profile(experience = Experience.BEGINNER))
        val advanced = PlanGenerator().generate(profile(experience = Experience.ADVANCED))

        assertTrue(beginner.totalWeeklySets < advanced.totalWeeklySets)
        assertTrue(beginner.days.all { day -> day.exercises.all { it.prescription.rir >= 2 } })
    }

    @Test
    fun `la durata stimata resta vicina al tempo disponibile`() {
        val plan = PlanGenerator().generate(profile(minutes = 60))

        plan.days.forEach { day ->
            assertTrue(
                "${day.name} dura ${day.estimatedMinutes} minuti",
                day.estimatedMinutes <= 80
            )
        }
        assertTrue(plan.issues.none { it.severity == PlanSeverity.ERROR })
    }

    @Test
    fun `i gruppi muscolari allenati coprono il piano`() {
        val plan = PlanGenerator().generate(profile(days = 4))
        val trained = plan.weeklySetsPerMuscle.keys

        assertTrue(trained.containsAll(listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS)))
    }
}
