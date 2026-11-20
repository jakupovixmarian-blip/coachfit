import json
arts=json.load(open("invictus_raw.json"))
idx={a["title"].strip().lower():a for a in arts}
R=[]
def add(area,statement,title,limits):
    a=idx.get(title.strip().lower())
    assert a, "titolo non trovato: "+title
    R.append(dict(area=area,statement=statement,title=a["title"],url=a["url"],limits=limits))

add("TRAINING","Il volume settimanale si conta in ripetizioni per gruppo muscolare, non in serie: gruppi grandi 100-160 ripetizioni, gruppi piccoli 90-140, specializzazione 180-230 riducendo gli altri distretti. L'individualita del soggetto prevale su qualsiasi range.","Volume di allenamento in palestra","Range divulgativi, non soglie cliniche.")
add("TRAINING","I muscoli grandi vanno allenati almeno 2 volte a settimana: la sintesi proteica indotta dalla seduta dura al massimo 72 ore.","Il culturismo: cos’è, dieta e allenamento","Adattare ai giorni disponibili e al recupero.")
add("TRAINING","Recuperi di 2-3 minuti sui fondamentali: recuperi piu brevi non aumentano l'ipertrofia e riducono la tensione meccanica nella serie successiva.","La Forza nel Bodybuilding è veramente utile?","Recuperi brevi restano leciti su isolamento e tecniche metaboliche.")
add("TRAINING","Il cedimento non e obbligatorio: il lavoro a buffer (RIR maggiore di zero) produce ipertrofia. Alzare il cedimento obbliga ad abbassare volume o carico.","Cedimento Muscolare | Sempre e comunque utile?","Posizioni diverse tra tecnici: il RIR e una variabile, non un dogma.")
add("TRAINING","Lo scarico serve a smaltire la fatica quando il volume supera la capacita di supercompensazione; senza scarico si arriva al sovrallenamento.","Programmare la fase di scarico","Programmare su segnali reali, non a calendario fisso.")
add("TRAINING","Nella stessa settimana si possono combinare sedute a buffer con carichi alti (tensione meccanica) e sedute a cedimento con carichi medi (stress metabolico).","Il culturismo: cos’è, dieta e allenamento","Richiede esperienza per gestire la fatica accumulata.")
add("NUTRITION","Per la massa serve un surplus di 300-500 kcal al giorno sul mantenimento: l'accumulo di grasso in questa fase e fisiologico.","Ipertrofia muscolare: la guida completa","Surplus maggiori non accelerano l'ipertrofia.")
add("NUTRITION","Per la definizione serve un deficit medio di circa 500 kcal al giorno per gli uomini e 300 per le donne, sostenuto per mesi.","Ipertrofia muscolare: la guida completa","Deficit piu aggressivi non sono indicati senza supervisione.")
add("NUTRITION","I grassi non scendono sotto 0,5 g/kg: il range di riferimento va da 0,5 a 1,5 g/kg, piu alto in iperalimentazione e piu basso in ipoalimentazione.","Il mio percorso bodybuilding (Nicolò Liani)","Il minimo protegge la funzione ormonale.")
add("NUTRITION","Le proteine vanno distribuite in modo uniforme nelle 24 ore, con 20-40 g per pasto.","Quando assumere proteine: prima o dopo allenamento?","Da adattare al numero di pasti del profilo.")
add("NUTRITION","La gestione di calorie e macronutrienti determina la maggior parte del risultato; gli integratori sono marginali.","Bodybuilding: la guida completa","Nessun integratore sostituisce il bilancio energetico.")
add("NUTRITION","La ricomposizione si imposta per fascia di grasso corporeo: uomini sotto il 14-15% e donne sotto il 22-25% seguono un approccio diverso da chi ha piu massa grassa.","Ricomposizione corporea: la guida completa","Richiede la misurazione del grasso corporeo, altrimenti non e applicabile.")

json.dump(R,open("/tmp/rules_final.json","w"),ensure_ascii=False,indent=1)
print("regole:",len(R))

# --- generazione Kotlin ---
def k(s):  # stringa Kotlin sicura
    return '"'+s.replace('\\','\\\\').replace('"','\\"')+'"'
def wrap(s, ind="                "):
    words = s.split()
    lines, cur = [], ""
    for w in words:
        if len(cur) + len(w) + 1 > 82:
            lines.append(cur)
            cur = w
        else:
            cur = (cur + " " + w).strip()
    if cur:
        lines.append(cur)
    # ogni riga porta lo spazio finale: la concatenazione non incolla le parole
    return (" +\n" + ind).join(k(l + " ") for l in lines)

out=[]
out.append('''package com.fitcoach.app.domain.knowledge

import com.fitcoach.app.domain.model.SourceKind

/**
 * Corpus Project InVictus: regole di metodo estratte dagli articoli pubblicati
 * su projectinvictus.it, con il link alla fonte per ogni affermazione.
 *
 * Perche esiste: le soglie di volume e le scelte di programmazione non possono
 * nascere da un motore di calcolo. Dieci serie di polpacci per un avanzato non
 * sono ne giuste ne sbagliate in assoluto: dipendono dalla metodologia. Questo
 * file porta quella metodologia dentro l'app, citata.
 *
 * Cosa NON e: non e una copia degli articoli (sono protetti da copyright) e non
 * e una fonte scientifica primaria. E una sintesi di regole divulgative, quindi
 * [SourceKind.DIVULGATIVE]: dove serve autorita si confronta con la letteratura
 * indicizzata (PubMed, OpenAlex) e con [ScientificSourceSeedData].
 *
 * Come si aggiorna: `tools/corpus/build_invictus_corpus.py` rilegge gli articoli
 * via REST API pubblica di WordPress e rigenera la tabella. Ogni regola cita
 * titolo e URL, cosi la si puo verificare alla fonte.
 */
object InvictusCorpus {

    data class Rule(
        val area: KnowledgeCorpus.Area,
        val statement: String,
        val articleTitle: String,
        val url: String,
        val limits: String,
        val kind: SourceKind = SourceKind.DIVULGATIVE
    )

    val rules: List<Rule> = listOf(''')
for i,r in enumerate(R):
    area="KnowledgeCorpus.Area."+r["area"]
    out.append(f"        Rule(\n            area = {area},\n            statement =\n                {wrap(r['statement'],'                ')},\n            articleTitle = {k(r['title'])},\n            url = {k(r['url'])},\n            limits =\n                {wrap(r['limits'],'                ')}\n        ){',' if i<len(R)-1 else ''}")
out.append('''    )

    fun forArea(area: KnowledgeCorpus.Area): List<Rule> = rules.filter { it.area == area }

    /** Blocco per il prompt: regola, fonte e limite, pronto da citare. */
    fun promptBlock(): String = buildString {
        appendLine("METODOLOGIA PROJECT INVICTUS (fonte divulgativa, citare l'articolo quando la si usa).")
        rules.forEach { r ->
            appendLine("- [${r.area.label}] ${r.statement}")
            appendLine("  Fonte: ${r.articleTitle} - ${r.url}")
            appendLine("  Limiti: ${r.limits}")
        }
    }.trimEnd()
}
''')
open("/workspace/project/app/src/main/java/com/fitcoach/app/domain/knowledge/InvictusCorpus.kt","w").write("\n".join(out))
print("scritto InvictusCorpus.kt")
