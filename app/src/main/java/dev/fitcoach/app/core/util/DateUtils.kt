package dev.fitcoach.app.core.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY)
    private val shortFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ITALY)

    fun today(): LocalDate = LocalDate.now(zone)

    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()

    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun nowMillis(): Long = Instant.now().toEpochMilli()

    fun startOfWeek(date: LocalDate = today()): LocalDate =
        date.with(DayOfWeek.MONDAY)

    fun format(date: LocalDate): String = date.format(dayFormatter)

    fun formatShort(date: LocalDate): String = date.format(shortFormatter)

    fun dayName(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Lunedi"
        DayOfWeek.TUESDAY -> "Martedi"
        DayOfWeek.WEDNESDAY -> "Mercoledi"
        DayOfWeek.THURSDAY -> "Giovedi"
        DayOfWeek.FRIDAY -> "Venerdi"
        DayOfWeek.SATURDAY -> "Sabato"
        DayOfWeek.SUNDAY -> "Domenica"
    }

    fun weekDays(start: LocalDate): List<LocalDate> = (0..6).map { start.plusDays(it.toLong()) }
}
