package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.domain.knowledge.InvictusCorpus
import com.fitcoach.app.domain.knowledge.KnowledgeCorpus
import com.fitcoach.app.domain.knowledge.InvictusExercises
import com.fitcoach.app.domain.knowledge.InvictusNutrition
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

    private fun training(topic: String?, limit: Int): String {
        val all = InvictusCorpus.rules.filter { it.area == KnowledgeCorpus.Area.TRAINING }
        val filtered = if (topic.isNullOrBlank()) {
            // Senza argomento: la prima regola di ogni tema, per dare il quadro.
            all.groupBy { it.topic }.values.mapNotNull { it.firstOrNull() }
        } else {
            all.filter { it.matches(topic) }
        }
        if (filtered.isEmpty()) return fail("Nessuna regola di allenamento per '$topic'. Temi disponibili: ${topics()}.")

        val items = filtered.take(limit).map { r ->
            JsonOut.obj(
                "tema" to JsonOut.str(r.topic.label),
                "regola" to JsonOut.str(r.statement),
                "fonte" to JsonOut.str(r.articleTitle),
                "url" to JsonOut.str(r.url)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "area" to JsonOut.str("allenamento"),
                "totaleTrovate" to JsonOut.num(filtered.size),
                "regole" to JsonOut.arr(items)
            )
        )
    }

    private fun nutrition(topic: String?, limit: Int): String {
        val all = InvictusNutrition.rules
        val filtered = if (topic.isNullOrBlank()) {
            all.groupBy { it.topic }.values.mapNotNull { it.firstOrNull() }
        } else {
            all.filter {
                it.statement.contains(topic, ignoreCase = true) ||
                    it.topic.label.contains(topic, ignoreCase = true) ||
                    it.articleTitle.contains(topic, ignoreCase = true)
            }
        }
        if (filtered.isEmpty()) return fail("Nessuna regola nutrizionale per '$topic'.")

        val items = filtered.take(limit).map { r ->
            JsonOut.obj(
                "tema" to JsonOut.str(r.topic.label),
                "regola" to JsonOut.str(r.statement),
                "fonte" to JsonOut.str(r.articleTitle),
                "url" to JsonOut.str(r.url)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "area" to JsonOut.str("nutrizione"),
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
            JsonOut.obj(
                "nome" to JsonOut.str(e.name),
                "muscoli" to JsonOut.strings(e.primaryMuscles),
                "esecuzione" to JsonOut.strings(e.execution),
                "errori" to JsonOut.strings(e.errors),
                "varianti" to JsonOut.strings(e.variants),
                "fonte" to JsonOut.str(e.articleTitle),
                "url" to JsonOut.str(e.url)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "area" to JsonOut.str("esercizi"),
                "totaleTrovati" to JsonOut.num(filtered.size),
                "esercizi" to JsonOut.arr(items)
            )
        )
    }

    private fun InvictusCorpus.Rule.matches(topic: String): Boolean =
        statement.contains(topic, ignoreCase = true) ||
            this.topic.label.contains(topic, ignoreCase = true) ||
            articleTitle.contains(topic, ignoreCase = true)

    private fun topics(): String = InvictusCorpus.Topic.entries.joinToString { it.label }

    private fun fail(message: String): String =
        JsonOut.encode(JsonOut.obj("ok" to JsonOut.bool(false), "errore" to JsonOut.str(message)))
}
