package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.domain.knowledge.InvictusKnowledge
import com.fitcoach.app.domain.knowledge.InvictusExercises
import com.fitcoach.app.domain.knowledge.InvictusTechniques
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Consultazione della metodologia Project InVictus.
 *
 * Il corpus (159 regole di allenamento, 134 di nutrizione, 7 tecniche, 141
 * esercizi) resta nei dati dell'app: qui il modello ne chiede solo la parte che
 * gli serve, quando gli serve. Metterlo nel prompt lo renderebbe un elenco
 * interminabile da leggere a ogni generazione.
 *
 * Ogni risposta cita articolo e URL, cosi la proposta del coach e verificabile.
 */
@Singleton
class ConsultInvictusTool @Inject constructor() : CoachTool {

    override val name = "consult_invictus"

    override val description =
        "Consulta la metodologia Project InVictus su allenamento e nutrizione. " +
            "Usalo PRIMA di decidere volume, frequenza, recuperi, RIR, tecniche di intensita, " +
            "ripartizione dei macro o scelta degli esercizi: restituisce le regole con il numero " +
            "e il link all'articolo. Filtra per area e per argomento invece di chiedere tutto."

    override val parameters: JsonObject = Schema.obj(
        "area" to Schema.enumOf(
            "Area da consultare.",
            "allenamento", "nutrizione", "tecniche", "esercizi"
        ),
        "argomento" to Schema.string(
            "Argomento o parola chiave (opzionale). Es. 'volume', 'frequenza', 'proteine', " +
                "'drop set', 'petto'. Senza argomento restituisce la prima regola di ogni tema."
        ),
        "limite" to Schema.integer("Numero massimo di voci (default 12, max 40).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val args = JsonArgs(arguments)
        val area = args.string("area")?.trim()?.lowercase() ?: "allenamento"
        val topic = args.string("argomento")?.trim()?.lowercase()
        val limit = (args.int("limite") ?: 12).coerceIn(1, 40)

        return when (area) {
            "allenamento" -> training(topic, limit)
            "nutrizione" -> nutrition(topic, limit)
            "tecniche" -> techniques(topic, limit)
            "esercizi" -> exercises(topic, limit)
            else -> fail("Area sconosciuta: '$area'. Valori ammessi: allenamento, nutrizione, tecniche, esercizi.")
        }
    }

    private fun training(topic: String?, limit: Int): String =
        fromKnowledge(InvictusKnowledge.Area.ALLENAMENTO, "allenamento", topic, limit)

    private fun nutrition(topic: String?, limit: Int): String =
        fromKnowledge(InvictusKnowledge.Area.NUTRIZIONE, "nutrizione", topic, limit)

    /**
     * Consulta [InvictusKnowledge]. Senza argomento da' una regola per
     * categoria: serve a orientarsi tra i parametri, non a leggere tutto.
     */
    private fun fromKnowledge(
        area: InvictusKnowledge.Area,
        areaName: String,
        topic: String?,
        limit: Int
    ): String {
        val pool = InvictusKnowledge.forArea(area)
        val filtered = if (topic.isNullOrBlank()) {
            InvictusKnowledge.categoriesOf(area).mapNotNull { c ->
                InvictusKnowledge.forCategory(c).firstOrNull()
            }
        } else {
            InvictusKnowledge.search(topic, area, limit = Int.MAX_VALUE)
        }
        if (filtered.isEmpty()) {
            val cats = InvictusKnowledge.categoriesOf(area).joinToString { it.label }
            return fail("Nessuna regola di $areaName per '$topic'. Categorie disponibili: $cats.")
        }

        val items = filtered.take(limit).map { r ->
            JsonOut.obj(
                "categoria" to JsonOut.str(r.category.label),
                "regola" to JsonOut.str(r.statement),
                "fonte" to JsonOut.str(r.articleTitle),
                // Il modello deve sapere se un numero viene da una meta-analisi
                // o da un articolo divulgativo: non hanno lo stesso peso.
                "tipo_fonte" to JsonOut.str(r.kind.label),
                "url" to JsonOut.str(r.url)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "area" to JsonOut.str(areaName),
                "totaleTrovate" to JsonOut.num(filtered.size),
                "regole" to JsonOut.arr(items)
            )
        )
    }

    private fun techniques(topic: String?, limit: Int): String {
        val filtered = InvictusTechniques.techniques.filter { t ->
            topic.isNullOrBlank() ||
                t.name.contains(topic, ignoreCase = true) ||
                t.articleTitle.contains(topic, ignoreCase = true)
        }
        if (filtered.isEmpty()) {
            return fail("Nessuna tecnica per '$topic'. Disponibili: ${InvictusTechniques.techniques.joinToString { it.name }}.")
        }
        val items = filtered.take(limit).map { t ->
            JsonOut.obj(
                "tecnica" to JsonOut.str(t.name),
                "come" to JsonOut.strings(t.rules),
                "fonte" to JsonOut.str(t.articleTitle),
                "url" to JsonOut.str(t.url)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "area" to JsonOut.str("tecniche"),
                "tecniche" to JsonOut.arr(items)
            )
        )
    }

    private fun exercises(topic: String?, limit: Int): String {
        val filtered = if (topic.isNullOrBlank()) {
            InvictusExercises.exercises
        } else {
            InvictusExercises.exercises.filter { e ->
                e.name.contains(topic, ignoreCase = true) ||
                    e.primaryMuscles.any { it.contains(topic, ignoreCase = true) }
            }
        }
        if (filtered.isEmpty()) return fail("Nessun esercizio documentato per '$topic'.")

        val items = filtered.take(limit).map { e ->
            // Alcune voci dell'archivio sono solo il titolo di un articolo:
            // esecuzione, errori e varianti sono vuoti. Restituire tre array
            // vuoti senza dirlo invita il modello a inventare la tecnica.
            val senzaContenuto = e.execution.isEmpty() && e.errors.isEmpty() && e.variants.isEmpty()
            JsonOut.obj(
                "nome" to JsonOut.str(e.name),
                "muscoli" to JsonOut.strings(e.primaryMuscles),
                "esecuzione" to JsonOut.strings(e.execution),
                "errori" to JsonOut.strings(e.errors),
                "varianti" to JsonOut.strings(e.variants),
                "fonte" to JsonOut.str(e.articleTitle),
                "url" to JsonOut.str(e.url),
                "contenutoDisponibile" to JsonOut.bool(!senzaContenuto)
            )
        }
        val senzaContenuto = filtered.take(limit).count {
            it.execution.isEmpty() && it.errors.isEmpty() && it.variants.isEmpty()
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "area" to JsonOut.str("esercizi"),
                "totaleTrovati" to JsonOut.num(filtered.size),
                "senzaContenuto" to JsonOut.num(senzaContenuto),
                "nota" to JsonOut.str(
                    if (senzaContenuto == 0) ""
                    else "$senzaContenuto voci su ${filtered.take(limit).size} hanno solo il " +
                        "rimando all'articolo: esecuzione, errori e varianti non sono " +
                        "documentati. Non inventarli: cita la fonte o dichiara che il " +
                        "dettaglio tecnico non e' disponibile nell'archivio."
                ),
                "esercizi" to JsonOut.arr(items)
            )
        )
    }

    private fun fail(message: String): String =
        JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "errore" to JsonOut.str(message)))
}
