package com.fitcoach.app.domain.knowledge

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

    val rules: List<Rule> = listOf(
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            statement =
                "Il volume settimanale si conta in ripetizioni per gruppo muscolare, non in serie: " +
                "gruppi grandi 100-160 ripetizioni, gruppi piccoli 90-140, specializzazione 180-230 " +
                "riducendo gli altri distretti. L'individualita del soggetto prevale su qualsiasi " +
                "range. ",
            articleTitle = "Volume di allenamento in palestra",
            url = "https://www.projectinvictus.it/volume-allenamento-bodybuilding/",
            limits =
                "Range divulgativi, non soglie cliniche. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            statement =
                "I muscoli grandi vanno allenati almeno 2 volte a settimana: la sintesi proteica " +
                "indotta dalla seduta dura al massimo 72 ore. ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            limits =
                "Adattare ai giorni disponibili e al recupero. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            statement =
                "Recuperi di 2-3 minuti sui fondamentali: recuperi piu brevi non aumentano " +
                "l'ipertrofia e riducono la tensione meccanica nella serie successiva. ",
            articleTitle = "La Forza nel Bodybuilding è veramente utile?",
            url = "https://www.projectinvictus.it/forza-bodybuilding/",
            limits =
                "Recuperi brevi restano leciti su isolamento e tecniche metaboliche. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            statement =
                "Il cedimento non e obbligatorio: il lavoro a buffer (RIR maggiore di zero) produce " +
                "ipertrofia. Alzare il cedimento obbliga ad abbassare volume o carico. ",
            articleTitle = "Cedimento Muscolare | Sempre e comunque utile?",
            url = "https://www.projectinvictus.it/cedimento-muscolare-sempre-e-comunque-utile/",
            limits =
                "Posizioni diverse tra tecnici: il RIR e una variabile, non un dogma. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            statement =
                "Lo scarico serve a smaltire la fatica quando il volume supera la capacita di " +
                "supercompensazione; senza scarico si arriva al sovrallenamento. ",
            articleTitle = "Programmare la fase di scarico",
            url = "https://www.projectinvictus.it/programmare-fase-scarico/",
            limits =
                "Programmare su segnali reali, non a calendario fisso. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            statement =
                "Nella stessa settimana si possono combinare sedute a buffer con carichi alti " +
                "(tensione meccanica) e sedute a cedimento con carichi medi (stress metabolico). ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            limits =
                "Richiede esperienza per gestire la fatica accumulata. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            statement =
                "Per la massa serve un surplus di 300-500 kcal al giorno sul mantenimento: " +
                "l'accumulo di grasso in questa fase e fisiologico. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            limits =
                "Surplus maggiori non accelerano l'ipertrofia. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            statement =
                "Per la definizione serve un deficit medio di circa 500 kcal al giorno per gli " +
                "uomini e 300 per le donne, sostenuto per mesi. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            limits =
                "Deficit piu aggressivi non sono indicati senza supervisione. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            statement =
                "I grassi non scendono sotto 0,5 g/kg: il range di riferimento va da 0,5 a 1,5 " +
                "g/kg, piu alto in iperalimentazione e piu basso in ipoalimentazione. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            limits =
                "Il minimo protegge la funzione ormonale. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            statement =
                "Le proteine vanno distribuite in modo uniforme nelle 24 ore, con 20-40 g per " +
                "pasto. ",
            articleTitle = "Quando assumere proteine: prima o dopo allenamento?",
            url = "https://www.projectinvictus.it/proteine-prima-o-dopo-allenamento/",
            limits =
                "Da adattare al numero di pasti del profilo. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            statement =
                "La gestione di calorie e macronutrienti determina la maggior parte del risultato; " +
                "gli integratori sono marginali. ",
            articleTitle = "Bodybuilding: la guida completa",
            url = "https://www.projectinvictus.it/bodybuilding/",
            limits =
                "Nessun integratore sostituisce il bilancio energetico. "
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            statement =
                "La ricomposizione si imposta per fascia di grasso corporeo: uomini sotto il 14-15% " +
                "e donne sotto il 22-25% seguono un approccio diverso da chi ha piu massa grassa. ",
            articleTitle = "Ricomposizione corporea: la guida completa",
            url = "https://www.projectinvictus.it/ricomposizione-corporea/",
            limits =
                "Richiede la misurazione del grasso corporeo, altrimenti non e applicabile. "
        )
    )

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
