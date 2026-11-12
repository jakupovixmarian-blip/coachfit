package dev.fitcoach.app

import dev.fitcoach.app.ui.screens.calendar.CalendarUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarLogicTest {

    @Test
    fun `grid starts on monday and covers the whole month`() {
        val state = CalendarUiState(month = YearMonth.of(2026, 2))
        val days = state.gridDays()
        assertEquals(42, days.size)
        assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
        assertTrue(days.any { it == LocalDate.of(2026, 2, 28) })
        assertTrue(days.any { it == LocalDate.of(2026, 2, 1) })
    }

    @Test
    fun `grid aligns to the monday of the first week`() {
        // 1 March 2026 is a Sunday, so the grid starts on 23 February.
        val state = CalendarUiState(month = YearMonth.of(2026, 3))
        assertEquals(LocalDate.of(2026, 2, 23), state.gridDays().first())
    }
}
