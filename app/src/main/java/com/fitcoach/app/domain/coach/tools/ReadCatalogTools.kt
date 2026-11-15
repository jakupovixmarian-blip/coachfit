package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.ExerciseRepository
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.domain.model.FoodItem
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accesso GLOBALE al catalogo alimenti. L'AI puo filtrare per categoria, tag,
 * testo e vincoli (allergie/esclusioni) e ottenere i valori nutrizionali reali
 * per 100 g con la fonte dichiarata. Nessun valore nutrizionale e inventato.
 */
@Singleton
class ListFoodsTool @Inject constructor(
    private val foodRepository: FoodRepository
) : CoachTool {
    override val name = "list_foods"

    override val description =
        "Elenca o cerca nel catalogo alimentare GLOBALE (valori reali per 100 g, con fonte). " +
            "Filtri opzionali: query testuale, categoria, tag, limite. Usalo per scegliere gli " +
            "alimenti del piano. Per il piano preferisci alimenti non esclusi e non allergeni."

    override val parameters: JsonObject = Schema.obj(
        "query" to Schema.string("Testo da cercare nel nome dell'alimento (opzionale)."),
        "categoria" to Schema.string("Categoria merceologica esatta (opzionale)."),
        "tag" to Schema.string("Tag da cercare (opzionale), es. 'proteina', 'vegetale'."),
        "limit" to Schema.integer("Numero massimo di alimenti da restituire (default 40, max 200).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val args = JsonArgs(arguments)
        val query = args.string("query")?.trim()?.lowercase()
        val category = args.string("categoria")?.trim()?.lowercase()
        val tag = args.string("tag")?.trim()?.lowercase()
        val limit = (args.int("limit") ?: 40).coerceIn(1, 200)

        var foods: List<FoodItem> = foodRepository.getAll()
        if (!query.isNullOrBlank()) {
            foods = foods.filter { it.name.lowercase().contains(query) || (it.brand?.lowercase()?.contains(query) == true) }
        }
        if (!category.isNullOrBlank()) foods = foods.filter { it.category.lowercase() == category }
        if (!tag.isNullOrBlank()) foods = foods.filter { f -> f.tags.any { it.lowercase().contains(tag) } }

        // Senza query esplicita, dai priorita al catalogo curato italiano (CREA,
        // con sostituti definiti) invece che alle migliaia di voci USDA generiche:
        // l'AI costruisce cosi piani con alimenti adatti all'utente italiano.
        if (query.isNullOrBlank()) {
            foods = foods.sortedWith(
                compareByDescending<FoodItem> { it.substituteNames.isNotEmpty() }
                    .thenByDescending { it.source.contains("CREA") }
                    .thenBy { it.name }
            )
        }

        val total = foods.size
        val items = foods.take(limit).map { food ->
            JsonOut.obj(
                "id" to JsonOut.num(food.id),
                "nome" to JsonOut.str(food.name),
                "categoria" to JsonOut.str(food.category),
                "kcal100g" to JsonOut.num(food.kcalPer100g),
                "proteine100g" to JsonOut.num(food.proteinPer100g),
                "carboidrati100g" to JsonOut.num(food.carbsPer100g),
                "grassi100g" to JsonOut.num(food.fatPer100g),
                "fibra100g" to JsonOut.num(food.fiberPer100g),
                "unita" to JsonOut.str(food.defaultUnit.name),
                "fonte" to JsonOut.str(food.source),
                "prezzoKgEur" to JsonOut.num(food.pricePerKgEur)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "totaleTrovati" to JsonOut.num(total),
                "restituiti" to JsonOut.num(items.size),
                "alimenti" to JsonOut.arr(items)
            )
        )
    }
}

/**
 * Accesso GLOBALE al catalogo esercizi. L'AI vede gli esercizi esistenti con
 * gruppo muscolare, attrezzatura, pattern, difficolta e sostituti: cosi puo
 * scegliere gli esercizi giusti per il soggetto e l'attrezzatura disponibile.
 */
@Singleton
class ListExercisesTool @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : CoachTool {
    override val name = "list_exercises"

    override val description =
        "Elenca o cerca nel catalogo esercizi GLOBALE. Filtri opzionali: gruppo muscolare, " +
            "attrezzatura, pattern di movimento, difficolta massima, query testuale. " +
            "Usalo per costruire la scheda scegliendo esercizi compatibili con l'attrezzatura."

    override val parameters: JsonObject = Schema.obj(
        "gruppoMuscolare" to Schema.enumOf(
            "Gruppo muscolare principale.",
            "CHEST", "BACK", "SHOULDERS", "BICEPS", "TRICEPS",
            "QUADS", "HAMSTRINGS", "GLUTES", "CALVES", "CORE"
        ),
        "attrezzatura" to Schema.string("Attrezzatura richiesta (nome enum, es. BARBELL)."),
        "pattern" to Schema.string("Pattern di movimento (nome enum, es. HORIZONTAL_PUSH)."),
        "difficoltaMax" to Schema.integer("Difficolta tecnica massima 1..3."),
        "query" to Schema.string("Testo da cercare nel nome (opzionale)."),
        "limit" to Schema.integer("Numero massimo di esercizi (default 60, max 200).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val args = JsonArgs(arguments)
        val group = args.string("gruppoMuscolare")?.uppercase()
        val equipment = args.string("attrezzatura")?.uppercase()
        val pattern = args.string("pattern")?.uppercase()
        val maxDiff = args.int("difficoltaMax")
        val query = args.string("query")?.trim()?.lowercase()
        val limit = (args.int("limit") ?: 60).coerceIn(1, 200)

        var exercises = exerciseRepository.getAll()
        if (!group.isNullOrBlank()) exercises = exercises.filter { it.primaryMuscle.name == group }
        if (!equipment.isNullOrBlank()) exercises = exercises.filter { ex -> ex.equipment.any { it.name == equipment } }
        if (!pattern.isNullOrBlank()) exercises = exercises.filter { it.movementPattern.name == pattern }
        if (maxDiff != null) exercises = exercises.filter { it.difficulty <= maxDiff }
        if (!query.isNullOrBlank()) exercises = exercises.filter { it.name.lowercase().contains(query) }

        val total = exercises.size
        val items = exercises.take(limit).map { ex ->
            JsonOut.obj(
                "id" to JsonOut.num(ex.id),
                "nome" to JsonOut.str(ex.name),
                "gruppoPrincipale" to JsonOut.str(ex.primaryMuscle.name),
                "muscoliSecondari" to JsonOut.strings(ex.secondaryMuscles.map { it.name }),
                "categoria" to JsonOut.str(ex.category.name),
                "attrezzatura" to JsonOut.strings(ex.equipment.map { it.name }),
                "pattern" to JsonOut.str(ex.movementPattern.name),
                "serieDefault" to JsonOut.num(ex.defaultSets),
                "ripMin" to JsonOut.num(ex.minReps),
                "ripMax" to JsonOut.num(ex.maxReps),
                "recuperoDefaultSec" to JsonOut.num(ex.restSeconds),
                "rirTarget" to JsonOut.num(ex.targetRir),
                "difficolta" to JsonOut.num(ex.difficulty),
                "corpoLibero" to JsonOut.bool(ex.bodyweightOnly),
                "sostituti" to JsonOut.strings(ex.substituteNames),
                "tecnica" to JsonOut.str(ex.technique)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "totaleTrovati" to JsonOut.num(total),
                "restituiti" to JsonOut.num(items.size),
                "esercizi" to JsonOut.arr(items)
            )
        )
    }
}
