import json
from collections import defaultdict

rules = json.load(open("/tmp/claims_final.json"))

AREA = {
    "volume":"TRAINING","frequency":"TRAINING","recovery":"TRAINING",
    "intensity":"TRAINING","periodization":"TRAINING","progression":"TRAINING",
    "techniques":"TRAINING","exercise":"TRAINING",
    "protein":"NUTRITION","carbs":"NUTRITION","fat":"NUTRITION",
    "calories":"NUTRITION","timing":"NUTRITION","bodycomp":"NUTRITION",
    "safety":"SAFETY",
}
TOPIC_LABEL = {
    "volume":"Volume","frequency":"Frequenza","recovery":"Recupero",
    "intensity":"Intensita e RIR","periodization":"Periodizzazione",
    "progression":"Progressione dei carichi","techniques":"Tecniche d'intensita",
    "exercise":"Esecuzione","protein":"Proteine","carbs":"Carboidrati",
    "fat":"Grassi","calories":"Calorie e bilancio","timing":"Timing dei pasti",
    "bodycomp":"Composizione corporea","safety":"Sicurezza",
}

def k(s):
    s = s.replace("\\","\\\\").replace('"','\\"')
    return '"' + s + '"'

def wrap(s, ind="                "):
    words = s.split(); lines=[]; cur=""
    for w in words:
        if len(cur)+len(w)+1 > 84: lines.append(cur); cur=w
        else: cur=(cur+" "+w).strip()
    if cur: lines.append(cur)
    return (" +\n"+ind).join(k(l+" ") for l in lines)

groups = defaultdict(list)
for r in rules:
    groups[r["topic"]].append(r)

out = ['''package com.fitcoach.app.domain.knowledge

import com.fitcoach.app.domain.model.SourceKind

/**
 * Corpus Project InVictus: regole di metodo estratte dagli articoli di
 * projectinvictus.it, ognuna con titolo e URL della fonte.
 *
 * Ogni voce e una frase prescrittiva con un numero verificabile (serie,
 * ripetizioni, grammi, kcal, percentuali), filtrata dalle categorie
 * metodologiche del sito. Non sono copie di paragrafi: sono le affermazioni
 * utili al motore, con il rimando all'articolo per verificarle nel contesto.
 *
 * Classificazione: [SourceKind.DIVULGATIVE]. E una fonte tecnica divulgativa,
 * non una linea guida. Dove serve autorita si confronta con la letteratura
 * indicizzata (PubMed, OpenAlex, Crossref) e con [ScientificSourceSeedData].
 *
 * Rigenerazione: `tools/corpus/build_invictus_corpus.py` rilegge gli articoli
 * via REST API pubblica di WordPress e riscrive questo file.
 */
object InvictusCorpus {

    /** Argomento della regola: serve a selezionare solo cio che serve al prompt. */
    enum class Topic(val label: String) {
''']

for t in TOPIC_LABEL:
    out.append(f'        {t.upper()}("{TOPIC_LABEL[t]}"),')
out[-1] = out[-1].rstrip(",") + ";"

out.append('''
        companion object {
            fun of(raw: String): Topic? = entries.firstOrNull { it.name == raw.uppercase() }
        }
    }

    data class Rule(
        val area: KnowledgeCorpus.Area,
        val topic: Topic,
        val statement: String,
        val articleTitle: String,
        val url: String,
        val publishedDate: String,
        val kind: SourceKind = SourceKind.DIVULGATIVE
    )

    val rules: List<Rule> = listOf(''')

items = []
for t, lst in groups.items():
    lst.sort(key=lambda x: (-x["score"], x["title"]))
    for r in lst:
        items.append(
            f"        Rule(\n"
            f"            area = KnowledgeCorpus.Area.{AREA[t]},\n"
            f"            topic = Topic.{t.upper()},\n"
            f"            statement =\n                {wrap(r['statement'])},\n"
            f"            articleTitle = {k(r['title'])},\n"
            f"            url = {k(r['url'])},\n"
            f"            publishedDate = {k(r['date'])}\n"
            f"        )"
        )
out.append(",\n".join(items))

out.append('''
    )

    fun forArea(area: KnowledgeCorpus.Area): List<Rule> =
        rules.filter { it.area == area }

    fun forTopic(topic: Topic): List<Rule> =
        rules.filter { it.topic == topic }

    /** Regole piu pertinenti per argomento, per non gonfiare il prompt. */
    fun top(topic: Topic, limit: Int): List<Rule> = forTopic(topic).take(limit)

    /**
     * Blocco per il prompt. Con [maxPerTopic] si limita ogni argomento, cosi il
     * contesto resta utile invece di diventare un elenco interminabile.
     */
    fun promptBlock(maxPerTopic: Int = 4): String = buildString {
        appendLine("METODOLOGIA PROJECT INVICTUS (fonte divulgativa: cita l'articolo quando la usi).")
        Topic.entries.forEach { topic ->
            val list = top(topic, maxPerTopic)
            if (list.isEmpty()) return@forEach
            appendLine("[${topic.label}]")
            list.forEach { r ->
                appendLine("- ${r.statement}")
                appendLine("  Fonte: ${r.articleTitle} - ${r.url}")
            }
        }
    }.trimEnd()

    /** Tutte le regole in forma testuale, per la consultazione in app. */
    fun fullText(): String = buildString {
        Topic.entries.forEach { topic ->
            val list = forTopic(topic)
            if (list.isEmpty()) return@forEach
            appendLine("== ${topic.label} (${list.size}) ==")
            list.forEach { r ->
                appendLine("- ${r.statement}")
                appendLine("  ${r.articleTitle} - ${r.url} (${r.publishedDate})")
            }
            appendLine()
        }
    }.trimEnd()
}
''')

open("/workspace/project/app/src/main/java/com/fitcoach/app/domain/knowledge/InvictusCorpus.kt","w").write("\n".join(out))
print("regole scritte:", len(items))
print("argomenti:", {t: len(l) for t,l in groups.items()})
