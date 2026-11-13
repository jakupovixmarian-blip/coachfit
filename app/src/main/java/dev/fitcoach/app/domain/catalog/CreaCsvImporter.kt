package dev.fitcoach.app.domain.catalog

import dev.fitcoach.app.domain.model.Food
import dev.fitcoach.app.domain.model.FoodSource
import dev.fitcoach.app.domain.model.FoodState

/** A line of the dataset that could not be imported, with the reason. */
data class CreaRejectedRow(
    val lineNumber: Int,
    val reason: String
)

data class CreaImportReport(
    val total: Int,
    val created: Int,
    val updated: Int,
    val rejected: List<CreaRejectedRow>
) {
    val stored: Int get() = created + updated
}

/**
 * Imports the CREA food composition tables from a delimited file.
 *
 * CREA does not publish an API, so the tables are imported from the file the user
 * supplies. The expected column order is documented and enforced instead of
 * guessed: a line that does not match is rejected with its number, never stored
 * with invented values.
 *
 * Columns: nome; categoria; kcal; proteine; carboidrati; grassi; fibre
 * Delimiter is detected from the first line (semicolon or comma). Decimal comma is
 * accepted, because the Italian tables use it.
 */
class CreaCsvImporter(
    private val catalog: FoodCatalog
) {

    suspend fun import(text: String): CreaImportReport {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return CreaImportReport(total = 0, created = 0, updated = 0, rejected = emptyList())

        val delimiter = if (lines.first().contains(';')) ';' else ','
        var total = 0
        var created = 0
        var updated = 0
        val rejected = mutableListOf<CreaRejectedRow>()

        lines.forEachIndexed { index, raw ->
            val cells = raw.split(delimiter).map { it.trim().trim('"') }
            if (isHeader(cells)) return@forEachIndexed

            total++
            val food = toFood(cells)
            if (food == null) {
                rejected += CreaRejectedRow(index + 1, describeProblem(cells))
                return@forEachIndexed
            }

            val hit = RemoteFoodHit(
                food = food,
                sourceId = sourceIdFor(food),
                sourceName = "CREA — Tabelle di composizione degli alimenti",
                sourceUpdatedAt = null,
                sourceNote = "Tabella CREA: valori per 100 g di parte edibile."
            )
            val result = catalog.import(hit)
            if (!result.stored) {
                rejected += CreaRejectedRow(index + 1, result.rejection ?: "Record rifiutato.")
            } else if (result.created) {
                created++
            } else {
                updated++
            }
        }

        return CreaImportReport(total = total, created = created, updated = updated, rejected = rejected)
    }

    private fun isHeader(cells: List<String>): Boolean {
        val first = cells.firstOrNull()?.lowercase().orEmpty()
        return first == "nome" || first == "alimento" || first == "name"
    }

    private fun toFood(cells: List<String>): Food? {
        if (cells.size < 6) return null
        val name = cells[0].takeIf { it.isNotBlank() } ?: return null
        val kcal = number(cells[2]) ?: return null
        if (kcal <= 0.0) return null
        val protein = number(cells[3]) ?: return null
        val carbs = number(cells[4]) ?: return null
        val fat = number(cells[5]) ?: return null
        val fiber = if (cells.size >= 7) number(cells[6]) else null

        return Food(
            name = name,
            brand = "",
            category = cells[1],
            kcalPer100g = kcal,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            fiberPer100g = fiber,
            source = FoodSource.CREA,
            state = FoodState.RAW
        )
    }

    private fun describeProblem(cells: List<String>): String = when {
        cells.size < 6 ->
            "Colonne insufficienti (${cells.size}): attese nome; categoria; kcal; proteine; carboidrati; grassi; fibre."
        cells[0].isBlank() -> "Nome dell'alimento mancante."
        number(cells[2]) == null -> "Calorie non numeriche: '${cells[2]}'."
        number(cells[3]) == null -> "Proteine non numeriche: '${cells[3]}'."
        number(cells[4]) == null -> "Carboidrati non numerici: '${cells[4]}'."
        number(cells[5]) == null -> "Grassi non numerici: '${cells[5]}'."
        else -> "Valori non validi."
    }

    /** Accepts Italian decimal comma and the placeholders the tables use for missing data. */
    private fun number(cell: String): Double? {
        val cleaned = cell.trim()
        if (cleaned.isEmpty()) return null
        val lower = cleaned.lowercase()
        if (lower == "n.d." || lower == "nd" || lower == "-" || lower == "tr" || lower == "n/a") return null
        return cleaned.replace(',', '.').toDoubleOrNull()
    }

    private fun sourceIdFor(food: Food): String {
        val key = (food.name + "-" + food.category).lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return "crea:" + key
    }
}
