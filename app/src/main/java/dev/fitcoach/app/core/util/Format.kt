package dev.fitcoach.app.core.util

import kotlin.math.roundToInt

object Format {
    fun kcal(value: Double): String = "${value.roundToInt()} kcal"
    fun grams(value: Double): String = "${value.roundToInt()} g"
    fun kg(value: Double): String = if (value == value.roundToInt().toDouble()) "${value.roundToInt()} kg" else "%.1f kg".format(value)
    fun oneDecimal(value: Double): String = "%.1f".format(value)
    fun money(value: Double): String = "%.2f EUR".format(value)
    fun percent(value: Double): String = "${value.roundToInt()}%"
}
