package com.fitcoach.app.domain.nutrition

import com.fitcoach.app.domain.model.FoodItem
import java.io.BufferedReader

/**
 * Importa un database alimentare fornito come asset TSV all'interno dell'app.
 *
 * Formato (una riga per alimento, tab-separated, con header):
 * `name, category, kcal, protein, carbs, fat, fiber, sourceId` — valori per 100 g.
 *
 * Estratto come oggetto puro (nessuna dipendenza da Android) per essere
 * verificabile con i test unitari sull'asset reale confezionato nell'APK.
 */
object FoodAssetImporter {

    const val USDA_SOURCE = "USDA FoodData Central (SR Legacy)"

    fun parseTsv(reader: BufferedReader, source: String): List<FoodItem> =
        reader.useLines { lines ->
            lines.drop(1) // header
                .mapNotNull { parseLine(it, source) }
                .toList()
        }

    private fun parseLine(line: String, source: String): FoodItem? {
        if (line.isBlank()) return null
        val cols = line.split('\t')
        if (cols.size < 8) return null
        val name = cols[0].trim()
        if (name.isEmpty()) return null
        val kcal = cols[2].toDoubleOrNull() ?: return null
        val protein = cols[3].toDoubleOrNull() ?: return null
        val carbs = cols[4].toDoubleOrNull() ?: return null
        val fat = cols[5].toDoubleOrNull() ?: return null
        val fiber = cols[6].toDoubleOrNull() ?: 0.0
        return FoodItem(
            name = name,
            category = cols[1].trim().ifEmpty { "Altro" },
            kcalPer100g = kcal,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            fiberPer100g = fiber,
            source = source,
            sourceId = cols[7].trim().ifBlank { null }
        )
    }
}
