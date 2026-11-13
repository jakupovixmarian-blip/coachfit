package dev.fitcoach.app

import dev.fitcoach.app.domain.engine.EnergyEngine
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EnergyEngineTest {

    private fun profile(
        weightKg: Double? = 80.0,
        heightCm: Double? = 180.0,
        age: Int? = 30,
        sex: Sex = Sex.MALE,
        bodyFat: Double? = null,
        goal: Goal = Goal.FAT_LOSS,
        steps: Int? = null,
        job: String = "",
        days: Int? = null,
        minutes: Int? = null
    ) = UserProfile(
        name = "Test",
        age = age,
        sex = sex,
        heightCm = heightCm,
        weightKg = weightKg,
        bodyFatPercent = bodyFat,
        goal = goal,
        jobActivity = job,
        dailySteps = steps,
        availableDays = days,
        sessionMinutes = minutes
    )

    @Test
    fun `mifflin st jeor bmr for a male`() {
        assertEquals(1780.0, EnergyEngine.bmr(profile())!!, 0.001)
    }

    @Test
    fun `katch mcardle is used when body fat is known`() {
        val bmr = EnergyEngine.bmr(profile(bodyFat = 20.0))!!
        assertEquals(370.0 + 21.6 * 64.0, bmr, 0.001)
    }

    @Test
    fun `bmr needs weight and either body fat or height and age`() {
        assertNull(EnergyEngine.bmr(profile(weightKg = null)))
        assertNull(EnergyEngine.bmr(profile(heightCm = null)))
        assertNull(EnergyEngine.bmr(profile(age = null)))
        assertNotNull(EnergyEngine.bmr(profile(weightKg = 80.0, bodyFat = 18.0, heightCm = null, age = null)))
    }

    @Test
    fun `activity factor grows with steps work and training`() {
        val sedentary = EnergyEngine.activityFactor(profile())
        val active = EnergyEngine.activityFactor(
            profile(steps = 12000, job = "lavoro pesante", days = 4, minutes = 60)
        )
        assertEquals(1.2, sedentary, 0.001)
        assertEquals(1.59, active, 0.001)
        assertTrue(active > sedentary)
    }

    @Test
    fun `activity factor is clamped to a plausible range`() {
        val extreme = EnergyEngine.activityFactor(
            profile(steps = 30000, job = "molto pesante in cantiere", days = 7, minutes = 120)
        )
        assertTrue(extreme <= 1.95)
        assertTrue(extreme >= 1.2)
    }

    @Test
    fun `fat loss target never falls below resting metabolism`() {
        val targets = EnergyEngine.targets(profile())!!
        assertTrue(targets.targetKcal >= targets.bmrKcal)
        assertTrue(targets.warnings.isNotEmpty())
    }

    @Test
    fun `macro split matches the calorie target`() {
        val targets = EnergyEngine.targets(profile())!!
        assertTrue(abs(targets.macroEnergyDriftKcal) <= 10.0)
    }

    @Test
    fun `protein is capped at forty percent of calories`() {
        val targets = EnergyEngine.targets(profile(weightKg = 120.0, bodyFat = 8.0, goal = Goal.FAT_LOSS))!!
        assertTrue(targets.proteinG * 4.0 <= targets.targetKcal * 0.40 + 0.5)
    }

    @Test
    fun `carbs are never negative across profiles`() {
        val profiles = listOf(
            profile(goal = Goal.FAT_LOSS),
            profile(goal = Goal.MUSCLE_GAIN),
            profile(weightKg = 55.0, heightCm = 155.0, age = 70, sex = Sex.FEMALE, goal = Goal.HEALTH),
            profile(weightKg = 140.0, bodyFat = 45.0, goal = Goal.FAT_LOSS)
        )
        for (candidate in profiles) {
            val targets = EnergyEngine.targets(candidate) ?: continue
            assertTrue("carboidrati negativi per $candidate", targets.carbsG >= 0.0)
            assertTrue("proteine negative per $candidate", targets.proteinG >= 0.0)
            assertTrue("grassi negativi per $candidate", targets.fatG >= 0.0)
        }
    }

    @Test
    fun `maintenance uses the computed activity factor`() {
        val targets = EnergyEngine.targets(profile(goal = Goal.MAINTENANCE))!!
        assertEquals(targets.bmrKcal * targets.activityFactor, targets.maintenanceKcal, 0.001)
    }
}
