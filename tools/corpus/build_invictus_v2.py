#!/usr/bin/env python3
"""Genera InvictusKnowledge.kt dal corpus categorizzato."""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import taxonomy

OUT = ("/workspace/project/app/src/main/java/com/fitcoach/app/domain/"
       "knowledge/InvictusKnowledge.kt")

def k(s):
    s = s.replace("\\", "\\\\").replace('"', '\\"')
    return '"' + s + '"'

def wrap(s, ind="                ", width=88):
    words, lines, cur = s.split(), [], ""
    for w in words:
        if len(cur) + len(w) + 1 > width:
            lines.append(cur); cur = w
        else:
            cur = (cur + " " + w).strip()
    if cur: lines.append(cur)
    return (" +\n" + ind).join(k(l + " ") for l in lines)

data = json.load(open("/tmp/rules_by_category.json"))

enum_lines = []
for code, (area, label) in taxonomy.ALL.items():
    enum_lines.append(f'        {code}("{label}", Area.{area.upper()})')

items = []
total = 0
for code, (area, label) in taxonomy.ALL.items():
    for r in data.get(code, []):
        total += 1
        items.append(
            f"        Rule(\n"
            f"            category = Category.{code},\n"
            f"            statement =\n                {wrap(r['statement'])},\n"
            f"            articleTitle = {k(r['articleTitle'])},\n"
            f"            url = {k(r['url'])},\n"
            f"            publishedDate = {k(r['publishedDate'])}\n"
            f"        )"
        )

src = '''package com.fitcoach.app.domain.knowledge

import com.fitcoach.app.domain.model.SourceKind

/**
 * Metodologia Project InVictus, organizzata per parametro di generazione.
 *
 * Ogni categoria corrisponde a una decisione che il motore deve prendere quando
 * costruisce un piano alimentare o una scheda: split, frequenza, volume,
 * recuperi, progressione, periodizzazione, macro, timing, ricariche. Non ci
 * sono categorie che non servano a generare.
 *
 * Ogni regola e una frase prescrittiva con un numero verificabile e il link
 * all'articolo. Non sono copie di paragrafi: sono le affermazioni utili, con il
 * rimando alla fonte per leggerle nel contesto.
 *
 * Il corpus NON va nel prompt di generazione: su un modello piccolo un contesto
 * gonfiato produce programmi non validi. Si consulta con lo strumento
 * `consult_invictus`, che filtra per area e categoria.
 *
 * Classificazione: [SourceKind.DIVULGATIVE]. E una fonte tecnica divulgativa,
 * non una linea guida. Dove serve autorita si confronta con la letteratura
 * indicizzata e con [ScientificSourceSeedData].
 *
 * Rigenerazione: `tools/corpus/build_invictus_v2.py`.
 */
object InvictusKnowledge {

    /** Macro-area: decide quale parte del generatore consulta il corpus. */
    enum class Area { ALLENAMENTO, NUTRIZIONE }

    /**
     * Categoria = parametro di generazione. Una regola puo' informare piu'
     * categorie: "12-20 serie a settimana in ricomposizione" dice qualcosa sul
     * volume e sul contesto insieme.
     */
    enum class Category(val label: String, val area: Area) {
''' + ",\n".join(enum_lines) + ''';
        companion object {
            fun of(raw: String): Category? =
                entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
    }

    data class Rule(
        val category: Category,
        val statement: String,
        val articleTitle: String,
        val url: String,
        val publishedDate: String,
        val kind: SourceKind = SourceKind.DIVULGATIVE
    )

    val rules: List<Rule> = listOf(
''' + ",\n".join(items) + '''
    )

    fun forCategory(category: Category): List<Rule> =
        rules.filter { it.category == category }

    fun forArea(area: Area): List<Rule> =
        rules.filter { it.category.area == area }

    /** Categorie di un'area che hanno almeno una regola. */
    fun categoriesOf(area: Area): List<Category> =
        Category.entries.filter { it.area == area && forCategory(it).isNotEmpty() }

    /**
     * Cerca nelle regole per testo, categoria o titolo dell'articolo.
     * [area] restringe la ricerca a una macro-area.
     */
    fun search(query: String, area: Area? = null, limit: Int = 20): List<Rule> {
        val q = query.trim().lowercase()
        val pool = if (area != null) forArea(area) else rules
        if (q.isEmpty()) return pool.take(limit)
        return pool.filter {
            it.statement.lowercase().contains(q) ||
                it.category.label.lowercase().contains(q) ||
                it.category.name.lowercase().contains(q) ||
                it.articleTitle.lowercase().contains(q)
        }.take(limit)
    }
}
'''

os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, "w").write(src)
print(f"scritte {total} regole in {len(taxonomy.ALL)} categorie")
print(f"-> {OUT}")
