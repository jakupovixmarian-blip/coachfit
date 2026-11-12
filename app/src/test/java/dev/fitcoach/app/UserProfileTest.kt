package dev.fitcoach.app

import dev.fitcoach.app.domain.model.ExperienceLevel
import dev.fitcoach.app.domain.model.Goal
import dev.fitcoach.app.domain.model.Sex
import dev.fitcoach.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileTest {

    private val male = UserProfile(
        name = "Test", age = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        goal = Goal.MUSCLE_GAIN, experience = ExperienceLevel.INTERMEDIATE
    )

    @Test
    fun `mifflin st jeor bmr is correct for a male`() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 1780
        assertEquals(1780.0, male.bmr()!!, 0.001)
    }

    @Test
    fun `bmr is null without body data`() {
        assertNull(UserProfile(name = "X", sex = Sex.MALE).bmr())
    }

    @Test
    fun `maintenance and targets are derived from the same data`() {
        val maintenance = male.maintenanceCalories()!!
        assertEquals(1780.0 * 1.55, maintenance, 0.001)
        assertEquals(maintenance * 1.10, male.targetCalories()!!, 0.001)
        assertEquals(80.0 * 1.8, male.proteinTarget()!!, 0.001)
        assertEquals(80.0 * 0.8, male.fatTarget()!!, 0.001)
    }

    @Test
    fun `carbs fill the remaining calories`() {
        val kcal = male.targetCalories()!!
        val expected = (kcal - male.proteinTarget()!! * 4 - male.fatTarget()!! * 9) / 4
        assertEquals(expected, male.carbsTarget()!!, 0.001)
    }

    @Test
    fun `targets are null when profile is incomplete`() {
        assertNull(UserProfile(name = "X").targetCalories())
        assertNull(UserProfile(name = "X").proteinTarget())
    }
}
