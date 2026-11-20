package com.fitcoach.app.domain.knowledge

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

        VOLUME("Volume"),
        FREQUENCY("Frequenza"),
        RECOVERY("Recupero"),
        INTENSITY("Intensita e RIR"),
        PERIODIZATION("Periodizzazione"),
        PROGRESSION("Progressione dei carichi"),
        TECHNIQUES("Tecniche d'intensita"),
        EXERCISE("Esecuzione"),
        PROTEIN("Proteine"),
        CARBS("Carboidrati"),
        FAT("Grassi"),
        CALORIES("Calorie e bilancio"),
        TIMING("Timing dei pasti"),
        BODYCOMP("Composizione corporea"),
        SAFETY("Sicurezza");

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

    val rules: List<Rule> = listOf(
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Per questo è opportuno inserire dei brevi periodi in cui alzare le calorie e tornare " +
                "in normocalorica (almeno 3 giorni) per poi ricominciare l’ipocalorica, in modo da " +
                "non abituare l’organismo. ",
            articleTitle = "Come sbloccare il metabolismo e riattivarlo?",
            url = "https://www.projectinvictus.it/come-sbloccare-il-metabolismo/",
            publishedDate = "2023-07-18"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Il consiglio è quello di aumentare di circa il 10-15% le calorie in entrata. ",
            articleTitle = "Cosa mangiare per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/cosa-mangiare-per-aumentare-la-massa-muscolare/",
            publishedDate = "2023-10-09"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Bene siamo arrivati a concludere che lo sgarro di Giuseppe sommato alle calorie già " +
                "precedentemente consumate, gli è costato circa 3750 kcal di eccesso calorico sopra " +
                "al proprio fabbisogno calorico giornaliero. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Ora queste 3750 kcal non verranno stoccate tutte quante negli adipociti, per capire " +
                "però in che % circa verranno depositate in questi ultimi, dobbiamo analizzare il " +
                "contesto in cui Giuseppe si trova. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Fase di massa Per aumentare la massa muscolare è necessario essere in surplus " +
                "calorico , mediamente tra le 300 e 500 kcal giornaliere rispetto all’apporto " +
                "calorico di mantenimento. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Colazione Costituisce, di solito, circa il 15% delle calorie giornaliere. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Spuntini Sono generalmente 2 e, di solito, contengono circa il 5% delle calorie " +
                "giornaliere. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Pranzo Dovrebbe essere il pasto più abbondante, con circa il 40% delle calorie. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Cena Dovrebbe costituire il secondo pasto più abbondante, con circa il 35% delle " +
                "calorie. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Partendo dal presupposto che, in linea di massima, questa è una scelta che deve " +
                "pianificare il nutrizionista o il medico , la dieta Longo prevede mediamente di " +
                "seguire un digiuno-semidigiuno fino a 800 kcal/die per 3-5 giorni. ",
            articleTitle = "Dieta Mima Digiuno: come funziona, benefici e cosa mangiare",
            url = "https://www.projectinvictus.it/dieta-mima-digiuno/",
            publishedDate = "2024-07-30"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Dieta Sirt: come si fa e come funziona La dieta Sirt prevede una prima fase (1 " +
                "settimana) , in cui l’apporto calorico è molto basso, pari a circa 1000 kcal al " +
                "giorno , e che comporta una forte perdita di peso iniziale , che sicuramente dà una " +
                "forte motivazione. ",
            articleTitle = "Dieta Sirt: come funziona e quali cibi mangiare",
            url = "https://www.projectinvictus.it/dieta-sirt/",
            publishedDate = "2024-01-30"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "La seconda fase (2 settimane) è di mantenimento , e punta a mantenere il peso perso " +
                "nella prima fase. ",
            articleTitle = "Dieta Sirt: come funziona e quali cibi mangiare",
            url = "https://www.projectinvictus.it/dieta-sirt/",
            publishedDate = "2024-01-30"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "La dieta dei 3 giorni funziona perché fa mangiare quantità molto ridotte di cibo " +
                "(circa 1000 kcal / giorno), rendendo la settimana equilibrata anche se negli altri 4 " +
                "giorni mangi un po’ di più. ",
            articleTitle = "Dieta dei 3 giorni per perdere peso",
            url = "https://www.projectinvictus.it/dieta-dei-3-giorni/",
            publishedDate = "2024-05-19"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Se rimane stazionaria 1800kcal sono realmente il suo TDEE altrimenti si aggiusterà " +
                "fino a trovare la quota calorica che le permetterà di non variare di peso nella " +
                "settimana. ",
            articleTitle = "Dieta dimagrante: come farla in modo efficace",
            url = "https://www.projectinvictus.it/dieta-dimagrante/",
            publishedDate = "2018-10-02"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Questi numeri valgono se la persona prima di mettersi a dieta avevamo un buon TDEE " +
                "(almeno intorno alle 30 kcal/kg di peso corporeo), ovvero assumeva un buon " +
                "quantitativo di calorie. ",
            articleTitle = "Dieta ferrea per dimagrire",
            url = "https://www.projectinvictus.it/dieta-ferrea/",
            publishedDate = "2025-05-29"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Le diete ipocaloriche dovrebbero mediamente creare un deficit calorico del 15-20% " +
                "rispetto al tuo fabbisogno. ",
            articleTitle = "Dieta ipocalorica per dimagrire: la guida completa",
            url = "https://www.projectinvictus.it/dieta-ipocalorica/",
            publishedDate = "2025-01-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CALORIES,
            statement =
                "Fai attenzione alle calorie nascoste Quanti cucchiaini di zucchero e quanti di olio " +
                "spesso non vengono considerati, quando invece apportano molte calorie, soprattutto " +
                "l’ olio che apporta circa 100 kcal per 10 ml (1 cucchiaio da minestra). ",
            articleTitle = "Dimagrire senza dieta in modo definitivo",
            url = "https://www.projectinvictus.it/dieta-senza-dieta/",
            publishedDate = "2024-01-30"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "Questo è infatti uno dei principali motivi per cui si sosteneva che assumere molti " +
                "carboidrati (fino a 1.2 g/kg nella singola assunzione) all’interno della finestra " +
                "anabolica (le prime 2 ore) fosse vantaggioso per ottenere una maggiore risintesi del " +
                "glicogeno muscolare (1,3). ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "Non ci sono altri particolari segreti se non tenere una quota proteica intorno a 1,5 " +
                "– 2,5 g/kg e di variare tra i grassi e i carboidrati. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "Le 4 fasi della dieta Atkins Fase 1 – Induzione La prima fase limita l’introduzione " +
                "di carboidrati ad un massimo di 20g/die per un periodo di tempo di 2 settimane, " +
                "ottenuti principalmente da fonti vegetali. ",
            articleTitle = "Dieta Atkins: come funziona, quanto si perde, menù",
            url = "https://www.projectinvictus.it/dieta-atkins/",
            publishedDate = "2023-06-20"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "Dieta metabolica: come funziona La dieta metabolica è una dieta chetogenica ciclica " +
                "in cui i carboidrati sono tenuti al minimo (mediamente 30-50 g al giorno) per 4-5 " +
                "giorni a settimana, intervallati da ricariche glucidiche nel weekend o il mercoledì. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "L’approccio della ricarica il mercoledì è molto più sensato, puoi anche farlo il " +
                "giovedì togliendo i carboidrati per massimo 3 giorni. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "Ad esempio, se mangi molti carboidrati e poi ti pesi, noterai subito che l’ago della " +
                "bilancia indica un peso corporeo maggiore rispetto al solito: infatti, ad ogni " +
                "grammo di glucosio immagazzinato sotto forma di glicogeno corrispondono circa 3 g di " +
                "acqua. ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "Nel breve periodo si può assistere a: Perdita di peso rapida : si assiste ad un " +
                "cospicuo calo ponderale sia per aumento della diuresi legata all’esaurimento del " +
                "glicogeno epatico (1g di glicogeno trattiene 3g di acqua circa) sia per l’aumento " +
                "della lipolisi e dell’ossidazione degli acidi grassi. ",
            articleTitle = "Dieta povera di carboidrati: cos’è, vantaggi e menù",
            url = "https://www.projectinvictus.it/dieta-povera-di-carboidrati/",
            publishedDate = "2023-03-05"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "Le linee guida consigliano di consumare al massimo il 10% di zuccheri nella propria " +
                "dieta all’interno del fabbisogno di carboidrati. ",
            articleTitle = "Dieta senza zuccheri",
            url = "https://www.projectinvictus.it/dieta-senza-zuccheri/",
            publishedDate = "2023-12-08"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "A fine ipertrofico è tuttavia utile preferire il consumo dei carboidrati rispetto a " +
                "quello dei grassi, il cui fabbisogno può quindi essere diminuito fino a 0.5 g/kg " +
                "senza scendere verso valori inferiori. ",
            articleTitle = "Grassi buoni: quali sono e a cosa servono?",
            url = "https://www.projectinvictus.it/grassi-buoni/",
            publishedDate = "2021-06-23"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.CARBS,
            statement =
                "Ma non solo, anche a livello di salute generale e prospettiva di vita, i carboidrati " +
                "dovrebbero occupare il 45-60% del tuo fabbisogno calorico giornaliero. ",
            articleTitle = "Paleo dieta: come funziona e cosa mangiare",
            url = "https://www.projectinvictus.it/la-paleo-dieta/",
            publishedDate = "2024-02-20"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Puoi ricavare i grassi anche dalla carne e formaggi ma i grassi saturi non " +
                "dovrebbero superare il 10% delle calorie giornaliere. ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Molti atleti, per preservare la massa contrattile preferiscono tagliare questo " +
                "macronutriente ed in media andiamo da quantitativi da 0,4-1,2g/kg di grassi . ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            publishedDate = "2022-12-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "I grassi in fase di cut coprono il 15-25% del fabbisogno calorico totale, con un " +
                "minimo di 0,5 g/kg per assicurare la corretta funzionalità dell’organismo. ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Per gli sportivi , i grassi dovrebbero coprire circa il 30% del fabbisogno " +
                "energetico, ma nel periodo di dimagrimento questa quota può scendere al 20%. ",
            articleTitle = "Dieta ipolipidica: cos’è e come funziona?",
            url = "https://www.projectinvictus.it/dieta-ipolipidica/",
            publishedDate = "2025-12-20"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Perdere 1 kg di grasso richiede un deficit calorico di circa 7.000 calorie , e le " +
                "diete lampo solitamente non consentono di raggiungere un tale deficit in così poco " +
                "tempo. ",
            articleTitle = "Dieta lampo per dimagrire in poco tempo",
            url = "https://www.projectinvictus.it/dieta-lampo/",
            publishedDate = "2025-06-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Secondo le linee guida LARN , una dieta ottimale prevede che il 20-35% dell’energia " +
                "giornaliera provenga dai grassi. ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Non è così, però, per la dieta low-fat, in cui i grassi ricoprono al massimo il 15% " +
                "dell’introito energetico giornaliero. ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Inoltre, le donne tendenzialmente avranno bisogno di una quantità maggiore di grassi " +
                "rispetto agli uomini (minimo 0,8 g/kg). ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "L’effetto che si prefigge la dieta dovrebbe portare a migliorare l’assetto " +
                "metabolico , portando il corpo a diventare una macchina brucia grassi per 4-6 giorni " +
                "a settimana, sfruttando successivamente l’effetto anabolico, anticatabolico " +
                "dell’insulina il weekend. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Ad esempio, il colesterolo è parte integrante delle membrane di tutte le cellule ed " +
                "è un precursore ormonale, l’apporto di grassi saturi non dovrebbe superare il 10% " +
                "del fabbisogno energetico, mentre quelli idrogenati e trans è bene ridurli al " +
                "minimo. ",
            articleTitle = "Grassi buoni e cattivi: benefici, rischi e dove trovarli?",
            url = "https://www.projectinvictus.it/grassi-buoni-e-cattivi/",
            publishedDate = "2021-06-27"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "L’apporto di grassi trans non dovrebbe superare l’1% del fabbisogno energetico – " +
                "quantità facilmente raggiungibile e superabile se questi alimenti non vengono " +
                "moderati. ",
            articleTitle = "Grassi buoni e cattivi: benefici, rischi e dove trovarli?",
            url = "https://www.projectinvictus.it/grassi-buoni-e-cattivi/",
            publishedDate = "2021-06-27"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "La restante percentuale (7-10% massimo) viene data dai grassi saturi. ",
            articleTitle = "Grassi buoni: quali sono e a cosa servono?",
            url = "https://www.projectinvictus.it/grassi-buoni/",
            publishedDate = "2021-06-23"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Non che di per sé i “cibi non sani” siano un problema quando occasionali (come il " +
                "comprenderli nel 20%), ma se molto consumati facilmente fanno sforare dalle quantità " +
                "consigliate: sia zuccheri che grassi saturi non dovrebbero superare il 10% " +
                "dell’apporto calorico giornaliero, mentre i trans l’1%. ",
            articleTitle = "IIFYM: che cos’è e come funziona?",
            url = "https://www.projectinvictus.it/iifym/",
            publishedDate = "2021-01-27"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Il mio obiettivo è quello di incrementare di circa 200g il mio peso medio " +
                "settimanale, di settimana in settimana, così da aumentare la massa muscolare e " +
                "migliorare i miei massimali senza accumulare eccessivamente grasso. ",
            articleTitle = "L’autoregolazione nell’allenamento della forza",
            url = "https://www.projectinvictus.it/autoregolazione-allenamento-della-forza/",
            publishedDate = "2017-01-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Il 95% del grasso nell’olio di cocco è saturo e circa la metà è acido laurico. ",
            articleTitle = "Mountain Dog Diet: cos’è e come farla",
            url = "https://www.projectinvictus.it/dieta-mountain-dog/",
            publishedDate = "2022-05-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Secondo le indicazioni dell’autore, suggerite anche da diversi esperti sui lipidi, " +
                "la dieta dovrebbe apportare circa l’1.5% delle sue calorie sotto forma di acidi " +
                "grassi polinsaturi. ",
            articleTitle = "Mountain Dog Diet: cos’è e come farla",
            url = "https://www.projectinvictus.it/dieta-mountain-dog/",
            publishedDate = "2022-05-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.FAT,
            statement =
                "Il grasso nel burro contiene più grassi monoinsaturi di quanto si potrebbe pensare, " +
                "circa il 30%. ",
            articleTitle = "Mountain Dog Diet: cos’è e come farla",
            url = "https://www.projectinvictus.it/dieta-mountain-dog/",
            publishedDate = "2022-05-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "I vantaggi della multifrequenza sono quelli di stimolare un muscolo almeno 2 volte a " +
                "settimana , invece che una sola volta come succede se ti alleni in monofrequenza . ",
            articleTitle = "Allenamento in multifrequenza: 5 schede di allenamento",
            url = "https://www.projectinvictus.it/allenamento-in-multifrequenza/",
            publishedDate = "2024-04-01"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Frequenza ideale: 2 volte parte alta e 2 volte gambe. ",
            articleTitle = "Allenamento in multifrequenza: 5 schede di allenamento",
            url = "https://www.projectinvictus.it/allenamento-in-multifrequenza/",
            publishedDate = "2024-04-01"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "REP RANGE : 10-12 Quante volte allenare le spalle a settimana Puoi partire con una " +
                "frequenza di allenamento di 2 volte settimanali , per poi muoverti se necessario " +
                "verso frequenze più spinte o conservative in base alle progressioni, ai restanti " +
                "parametri allenanti e di recupero soggettivi. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Assicurati di dormire almeno 7 ore per notte ed evita di bere alcolici se non in " +
                "modo moderato 1 – 2 volte a settimana . ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Innanzitutto prendi in considerazione un esercizio che verrà ripetuto per più volte " +
                "durante la settimana ( lavoro in multifrequenza ) e crea una modulazione su almeno 2 " +
                "giorni che preveda l’utilizzo di questo esercizio su più rep range . ",
            articleTitle = "Come programmare l’allenamento",
            url = "https://www.projectinvictus.it/programmazione-allenamento-palestra/",
            publishedDate = "2026-09-16"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Per questo conviene allenarsi 3 – 4 volte a settimana , anche inserendo allenamenti " +
                "metabolici o cardio o HIIT 1-3 volte / settimana, facendo attenzione a non fare " +
                "troppo per non rischiare il sovrallenamento. ",
            articleTitle = "Definizione in palestra: come definire i muscoli",
            url = "https://www.projectinvictus.it/definizione-palestra/",
            publishedDate = "2024-01-15"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "La frequenza di allenamento va da 5 – 7 giorni a settimana per Arthur Jones a 3 per " +
                "Mentzer. ",
            articleTitle = "Heavy Duty: principi e scheda di allenamento",
            url = "https://www.projectinvictus.it/heavy-duty-allenamento/",
            publishedDate = "2022-07-31"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "I quadricipiti hanno una frequenza ideale di 1 fino a 3 volte a settimana. ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Nel caso invece in cui il gruppo sia carente puoi aumentare la frequenza fino a 3 " +
                "volte a settimana . ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Fare squat 3 volte a settimana in un protocollo tipico di bodybuilding può essere " +
                "efficace ma non sempre è la soluzione ideale. ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Puoi allenare le spalle direttamente da 1-4 volte a settimana. ",
            articleTitle = "I migliori esercizi per le spalle",
            url = "https://www.projectinvictus.it/esercizi-spalle-la-guida-completa/",
            publishedDate = "2025-01-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "La frequenza di allenamento dello stesso gruppo muscolare è ogni 5 giorni circa. ",
            articleTitle = "Il metodo Hatfield",
            url = "https://www.projectinvictus.it/metodo-hatfield-ipertrofia/",
            publishedDate = "2025-05-10"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Secondo la mia personale esperienza, un gruppo da specializzare andrebbe allenato " +
                "almeno 3 o 4 volte a settimana per un periodo dalle 4 alle 8 settimana. ",
            articleTitle = "La periodizzazione nel bodybuilding",
            url = "https://www.projectinvictus.it/periodizzazione-nel-bodybuilding/",
            publishedDate = "2018-03-19"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "I vantaggi della multifrequenza sono quelli di stimolare un muscolo almeno 2 volte a " +
                "settimana . ",
            articleTitle = "Monofrequenza o multifrequenza?",
            url = "https://www.projectinvictus.it/monofrequenza-o-multifrequenza/",
            publishedDate = "2026-02-26"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Esempio 1 (livello principiante) Ti alleni 3 volte a settimana, sono un principiante " +
                "(ho iniziato da 1 mese) e voglio migliorare la mia composizione corporea . ",
            articleTitle = "Parametri allenanti: quali sono e come impostarli",
            url = "https://www.projectinvictus.it/parametri-allenanti/",
            publishedDate = "2022-10-29"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Frequenza : il deltoide mediale recupera molto velocemente , partite da 2 sessioni " +
                "settimanali e vedete come vi trovate, possiamo arrivare ad una frequenza anche di " +
                "4-5 volte settimanali in alcuni periodi e atleti. ",
            articleTitle = "Tirate al petto: come si eseguono",
            url = "https://www.projectinvictus.it/tirate-al-petto/",
            publishedDate = "2021-02-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Eseguire gli esercizi seguenti 2-3 volte a settimana a fine allenamento, è il minimo " +
                "sindacale per stimolare il retto dell’addome e gli obliq ui. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Allenamento 1 Allenamento da ripetere almeno 3 volte a settimana. ",
            articleTitle = "Calf Raise: esercizi efficaci per i polpacci e scheda di allenamento",
            url = "https://www.projectinvictus.it/calf-raise/",
            publishedDate = "2026-07-17"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Assolutamente consigliato da fare, almeno 3-7 volte a settimana per almeno 10′ . ",
            articleTitle = "I migliori esercizi fitness",
            url = "https://www.projectinvictus.it/esercizi-fitness/",
            publishedDate = "2024-07-28"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "La frequenza settimanale ottimale è su 4-6 giorni. ",
            articleTitle = "Impariamo a programmare alcuni allenamenti da palestra (1xMax)",
            url = "https://www.projectinvictus.it/impariamo-a-programmare-part-1-1xmax/",
            publishedDate = "2013-10-08"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Data la natura dell’esercizio, è consigliato eseguirlo in frequenza media (2 volte a " +
                "settimana) alternando possibilmente tra le diverse varianti. ",
            articleTitle = "Step up: esecuzione, scheda e varianti",
            url = "https://www.projectinvictus.it/step-up-esecuzione-corretta/",
            publishedDate = "2025-09-09"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Una frequenza di allenamento ottimale si attesta sulle 1 o 2 volte a settimana , " +
                "lasciando spazio ad altri esercizi più complessi che devono avere il maggior focus " +
                "in un programma di allenamento come, ad esempio, la panca piana, il lento avanti o i " +
                "dip alle parallele. ",
            articleTitle = "Diamond push up (o Piegamenti a diamante): esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/piegamenti-a-diamante/",
            publishedDate = "2024-08-01"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.FREQUENCY,
            statement =
                "Il consiglio per un atleta principiante è di allenarsi almeno 3 volte a settimana. ",
            articleTitle = "Street workout: cos’è, come iniziare e allenamento",
            url = "https://www.projectinvictus.it/street-workout/",
            publishedDate = "2023-03-09"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Si può calcolare moltiplicando le serie per le ripetizioni allenanti (ovvero con un " +
                "buffer massimo di 3, se il nostro cedimento arriva a 10 ripetizioni, contano solo le " +
                "serie dove arriviamo almeno a 7). ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            publishedDate = "2022-12-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Con carichi pesanti o in esercizi multiarticolari portati a cedimento tempi di 3-5 " +
                "minuti consentono di mantenere livelli di forza e concentrazione adeguati generando " +
                "così più lavoro di qualità. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "2 Serie a cedimento: tutte le serie vanno portate a cedimento tecnico nel range di " +
                "ripetizioni prefissato. ",
            articleTitle = "La periodizzazione nel bodybuilding",
            url = "https://www.projectinvictus.it/periodizzazione-nel-bodybuilding/",
            publishedDate = "2018-03-19"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Nell’ultimo set si inserisce un dropset: si scala il peso del 20% e si prosegue fino " +
                "a cedimento. ",
            articleTitle = "La periodizzazione nel bodybuilding",
            url = "https://www.projectinvictus.it/periodizzazione-nel-bodybuilding/",
            publishedDate = "2018-03-19"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Una serie allenante deve avere minimo un RPE di 7,5 (3 ripetizioni di margine dal " +
                "cedimento). ",
            articleTitle = "RPE: cos’è, misura, utilizzo",
            url = "https://www.projectinvictus.it/rpe-palestra/",
            publishedDate = "2026-09-16"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Se le serie vengono portate al cedimento o quasi, con un buffer massimo di 1 – 3 " +
                "ripetizioni, sei sicuro di portarti a casa uno dei parametri allenanti più " +
                "importanti. ",
            articleTitle = "Ricomposizione corporea: la guida completa",
            url = "https://www.projectinvictus.it/ricomposizione-corporea/",
            publishedDate = "2026-08-18"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Stripping : prevede il raggiungimento del cedimento concentrico con un determinato " +
                "carico per poi, nel minor tempo possibile, effettuare un “drop” pari solitamente al " +
                "5-15% del carico, per raggiungere un secondo cedimento concentrico, e così via per " +
                "il numero di volte prefissate (almeno 3). ",
            articleTitle = "Tecniche intensità nel bodybuilding",
            url = "https://www.projectinvictus.it/tecniche-intensita-bodybuilding/",
            publishedDate = "2025-08-27"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Nella sua versione originale prevedeva l’applicazione di carichi del 90% o " +
                "superiori, coi quali venivano eseguite una serie di singole intervallate da circa 10 " +
                "secondi di pausa fino ad arrivare a cedimento muscolare. ",
            articleTitle = "Tecniche intensità nel bodybuilding",
            url = "https://www.projectinvictus.it/tecniche-intensita-bodybuilding/",
            publishedDate = "2025-08-27"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Sembra che le intensità minime per provocare questo adattamento corrispondano a " +
                "circa il 30% 1-RM, e che a bassissime intensità si necessiti di portare lo sforzo " +
                "alla massima fatica (cedimento), ma sono necessarie ulteriori ricerche per poter " +
                "trarre conclusioni più esaustive[21]. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 1)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-1/",
            publishedDate = "2014-06-05"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.INTENSITY,
            statement =
                "Fai massimo 6-8 ripetizioni senza accumulare acido lattico, lavorando a buffer (con " +
                "margine). ",
            articleTitle = "Esercizi per il riscaldamento: perchè e quali fare",
            url = "https://www.projectinvictus.it/esercizi-di-riscaldamento/",
            publishedDate = "2024-06-19"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.PROGRESSION,
            statement =
                "Qualora si riesca a completare le 3 serie da 5 con 100 kg la sessione successiva si " +
                "può provare ad aumentare il carico. ",
            articleTitle = "5×5: di Bill Starr, Reg Park, Stronglift",
            url = "https://www.projectinvictus.it/5x5/",
            publishedDate = "2018-08-23"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.PROGRESSION,
            statement =
                "Se riesci ad eseguire correttamente >12-15 ripetizioni è meglio piuttosto, se scegli " +
                "questa variante, utilizzare la versione con sovraccarico. ",
            articleTitle = "Calf in piedi: esecuzione corretta",
            url = "https://www.projectinvictus.it/calf-in-piedi/",
            publishedDate = "2023-05-17"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "La quota ottimale di proteine per la massa muscolare è di 1,4 – 2,2 g / kg al giorno " +
                ". ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "dieta a zona 40% carbo, 30% proteine, 30% grassi): è sempre meglio basarsi su g / kg " +
                "riferiti al peso ideale. ",
            articleTitle = "Definizione in palestra: come definire i muscoli",
            url = "https://www.projectinvictus.it/definizione-palestra/",
            publishedDate = "2024-01-15"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Come noterete la mia media calorica si è abbassata di 200 kcal circa da quella di " +
                "partenza, ed ho aumentato in proporzione l’intake proteico semplicemente per un " +
                "discorso di maggiore sazietà non per altro. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Più il cut è aggressivo e più la dieta ipocalorica viene protratta nel tempo, più la " +
                "quantità di proteine può avvicinarsi al valore massimo del range proposto (3,1 " +
                "g/kg). ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "La fonte proteica va scelta sulla base dell’introito di amminoacidi: conviene, " +
                "quindi, scegliere una fonte ed un quantitativo di proteine che contenga circa " +
                "(almeno) 10 g di aminoacidi essenziali di cui 0.7-3 g di leucina. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Ad ogni modo, l’assunzione di 20-40 g di proteine (a seconda della qualità proteica " +
                "e della resistenza all’anabolismo) per pasto (uno di questi da far coincidere come " +
                "post-workout) durante la giornata sono in grado di stimolare al massimo la sintesi " +
                "proteica. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Alcuni alimenti proteici magri, soprattutto i derivati del latte light, possono " +
                "assolvere una funzione simile – ma con una dilatazione dei tempi digestivi di circa " +
                "il +50% rispetto ai precedenti. ",
            articleTitle = "Pre workout bodybuilding: cosa mangiare e cosa assumere prima della palestra?",
            url = "https://www.projectinvictus.it/pre-workout-palestra-bodybuilding/",
            publishedDate = "2022-01-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Le fonti proteiche è meglio distribuirle in modo uniforme nell’arco delle 24 ore in " +
                "modo che ciascun pasto contenga 20-40 g di proteine (da alimenti o in alternativa " +
                "proteine in polvere) per volta. ",
            articleTitle = "Quando assumere proteine: prima o dopo allenamento?",
            url = "https://www.projectinvictus.it/proteine-prima-o-dopo-allenamento/",
            publishedDate = "2023-09-24"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Ai reni Alla base della preoccupazione circa i danni renali indotti da una dieta " +
                "chetogenica c’è la convinzione che un apporto proteico maggiore rispetto alle RDA " +
                "per soggetti sedentari (0,8-1g/kg) possa provocare danni al sistema renale e " +
                "progressiva perdita di capacità funzionale dei reni. ",
            articleTitle = "Dieta chetogenica: pro e contro",
            url = "https://www.projectinvictus.it/dieta-chetogenica-fa-male/",
            publishedDate = "2024-05-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Questi alimenti apportano una notevole quantità di carboidrati complessi, circa un " +
                "10-12% di proteine ma anche fibre alimentari, vitamine del gruppo B e sali minerali " +
                "come magnesio, zinco e ferro. ",
            articleTitle = "Dieta flexitariana: cos’è, cosa mangiare, menù",
            url = "https://www.projectinvictus.it/flexitariana/",
            publishedDate = "2023-06-16"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "I cereali sono molto poveri di grassi e sono costituiti per circa il 70% da " +
                "carboidrati e per il 12% da proteine. ",
            articleTitle = "Dieta ipolipidica: cos’è e come funziona?",
            url = "https://www.projectinvictus.it/dieta-ipolipidica/",
            publishedDate = "2025-12-20"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Il range di proteine consigliato in questa fase è 2.2-2.6 g proteine/kg corporeo al " +
                "giorno . ",
            articleTitle = "Dieta proteica: la guida completa",
            url = "https://www.projectinvictus.it/dieta-proteica/",
            publishedDate = "2025-05-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "In genere si impostano prima proteine e grassi , destinando ai carboidrati le " +
                "calorie rimanenti: Per le proteine , negli sportivi in normo o ipercalorica si " +
                "consiglia un apporto tra 1,6, arrivando a 2-2,2 g/kg in fase di dimagrimento per " +
                "aiutare a preservare la massa muscolare. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Per definizione, una dieta è iperproteica quando le proteine ricoprono almeno il 35% " +
                "dell’introito calorico totale o che la quota proteica superi il valore RDA di 0,8 g " +
                "proteine/kg peso corporeo . ",
            articleTitle = "La dieta iperproteica per il dimagrimento",
            url = "https://www.projectinvictus.it/dieta-iperproteica-dimagrimento/",
            publishedDate = "2025-05-08"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Ogni giorno circa il 2.5% delle proteine corporee viene completamente rimpiazzato da " +
                "quelle neosintetizzate a partire dai singoli aminoacidi assunti o ricavati da altre " +
                "sostanze endogene. ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Nell’albume c’è la proteina nobile per eccellenza: l’albumina, che infatti ha il " +
                "massimo valore biologico con la presenza di tutti gli aminoacidi e che costituisce " +
                "il 60-70% delle proteine dell’albume. ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Non ci sono infatti riserve di amminoacidi nell’organismo, come è il glicogeno per " +
                "il glucosio o l’organo adiposo per gli acidi grassi, e il muscolo è quello che " +
                "presenta un contenuto proteico più elevato (circa il 20% del suo peso). ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Le proteine sono una componente fondamentale del muscolo (di cui costituiscono circa " +
                "il 20%) e la funzione più nota è sicuramente quella di garantire la contrazione , " +
                "che avviene tramite lo scivolamento dei filamenti proteici di miosina rispetto a " +
                "quelli di actina. ",
            articleTitle = "Proteine palestra: quali prendere e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-palestra/",
            publishedDate = "2023-09-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.PROTEIN,
            statement =
                "Le proteine post-workout (circa 25 g di proteine o 10-12 g di aminoacidi essenziali) " +
                "sono utili specialmente nel caso in cui non hai fatto un pasto pre-allenamento o " +
                "vieni da abbastanza ore di digiuno. ",
            articleTitle = "Proteine palestra: quali prendere e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-palestra/",
            publishedDate = "2023-09-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Puoi fare 4-6 serie da 6-8 ripetizioni con un recupero di 2′. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Esegui 4-6 serie da 6-8 ripetizioni con un recupero di 2′. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "L’autore consiglia di lavorare su range di 3-5 ripetizioni per 3-5 set con recuperi " +
                "abbondanti e completi. ",
            articleTitle = "P.H.A.T. Power Hypertrophy Adaptive Training",
            url = "https://www.projectinvictus.it/phat-power-hypertrophy-adaptive-training/",
            publishedDate = "2022-07-04"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Questo perchè ogni volta che si porta il muscolo allo sfinimento: 1 aumentano di " +
                "molti i tempi di recupero tra i giorni (invece qui dobbiamo allenare la panca 3 " +
                "volte a settima), 2 si deteriora la tecnica perchè si pensa esclusivamente a " +
                "concludere l’alzata perdendo completamente l’assetto. ",
            articleTitle = "5×5 per migliorare la tua Panca Piana (versione inVictus)",
            url = "https://www.projectinvictus.it/5x5-per-migliorare-la-tua-panca-piana-versione-invictus/",
            publishedDate = "2014-09-09"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Questo significa che nei programmi tipici dell’ipertrofia, che normalmente prevedono " +
                "recuperi tra le serie di 60-90 secondi[45], se lo sforzo viene portato al cedimento " +
                "il numero di ripetizioni massime cala ad ogni serie. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 2)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-2/",
            publishedDate = "2014-06-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Nei programmi di forza massimale questo effetto è ancora meno pronunciato, grazie a " +
                "tempi di recupero tra le serie più lunghi[47], tra 3 e 5 minuti, che permettono un " +
                "maggiore recupero e mantenimento della forza[45]. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 2)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-2/",
            publishedDate = "2014-06-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "In questo caso i tempi di recupero ammontano a 2 minuti tra le serie. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 4)",
            url = "https://www.projectinvictus.it/chiarimenti-incomprensioni-sullintensita-nellesercizio-i-pesi-parte-4/",
            publishedDate = "2014-11-17"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Esegui questi esercizi per 3 serie, con 45” di pausa tra le serie. ",
            articleTitle = "Crunch laterali: come farli correttamente",
            url = "https://www.projectinvictus.it/crunch-obliqui/",
            publishedDate = "2024-07-10"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Datti un tempo di almeno 4-5 settimane , se recuperi e i parametri allenanti " +
                "migliorano, così come la circonferenza dei tuoi polpacci, potrai scegliere se " +
                "aumentare il numero di serie o rimanere in questo range fino ai primi stalli. ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Tale metodo può essere applicato con un approccio all-out (tutto a cedimento) " +
                "mantenendo un tempo di recupero completo tra le serie (3/4 minuti), o con un " +
                "approccio in buffer, mantenendo però tempi di recupero incompleti (circa 1-2 " +
                "minuti). ",
            articleTitle = "L’importanza della tecnica e di non scappare dai punti difficili",
            url = "https://www.projectinvictus.it/importanza-tecnica/",
            publishedDate = "2017-01-23"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "Una volta che 1′ sarà facile da tenere, puoi fare più serie man mano diminuendo il " +
                "tempo di pausa tra le serie oppure incrementare il tempo di tenuta, puntando ad " +
                "esempio a mantenere i 5′ – se ti sembrano tanti, ricorda che il record del mondo è " +
                "di più di 10 ore! ",
            articleTitle = "Plank: la guida completa",
            url = "https://www.projectinvictus.it/plank-addominali/",
            publishedDate = "2024-09-24"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.RECOVERY,
            statement =
                "La proposta nell’applicazione dell’endurance muscolare per le scrollate inverse è " +
                "quella di eseguire le ripetizioni alla massima fatica (cedimento) secondo quanto " +
                "permesso dal peso corporeo, e impostare pause di 30 secondi tra le serie multiple. ",
            articleTitle = "Scrollate inverse per il riequilibrio scapolare",
            url = "https://www.projectinvictus.it/scrollate-inverse-per-riequilibrio-scapolare/",
            publishedDate = "2014-11-06"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Serie : in linea generale, considerando questo volume come esclusivo per il deltoide " +
                "mediale, si consiglia di iniziare con 10-12 serie allenanti , da cui poi muoverti " +
                "verso un loro aumento o diminuzione in base alle progressioni e ai parametri di " +
                "recupero soggettivi. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "REP RANGE : 5-6 4 serie: ESERCIZIO DI ISOLAMENTO MULTIARTICOLARE. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "REP RANGE : 7-9 3-4 serie : ESERCIZIO DI ISOLAMENTO MONOARTICOLARE. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Le braccia rispondono molto bene al volume allenante : appena inizi a metterli come " +
                "gruppo muscolare primario e a fare almeno 15-20 serie allenanti , vedrai già nel " +
                "giro di 4 settimane dei risultati (quantomeno aumenterà la ritenzione sulle " +
                "braccia). ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Come allenare le gambe e i quadricipiti in palestra I quadricipiti hanno bisogno " +
                "almeno di 8/10 serie allenanti settimanali per un mantenimento della massa " +
                "muscolare. ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Passai dunque ad una media di 3050kcal circa, introducendo allo stesso tempo a " +
                "livello di training, lavori più lattacidi, aumentando quindi il volume totale " +
                "settimanale in concomitanza di un abbassamento calorico . ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Naturalmente puoi decidere di ripartire tale volume in modo più produttivo " +
                "spalmandolo su una frequenza di allenamento settimanale superiore, ad esempio 18 " +
                "serie totali a settimana per i pettorali distribuite in 2 allenamenti da 9 serie " +
                "l’una. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "La letteratura scientifica mostra che l’ipertrofia si sviluppa tra le 10 e le 25 " +
                "serie allenanti a settimana . ",
            articleTitle = "RPE: cos’è, misura, utilizzo",
            url = "https://www.projectinvictus.it/rpe-palestra/",
            publishedDate = "2026-09-16"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Durante la fase di ricomposizione conviene tenere un volume leggermente inferiore " +
                "intorno alle 12 – 20 serie a settimana . ",
            articleTitle = "Ricomposizione corporea: la guida completa",
            url = "https://www.projectinvictus.it/ricomposizione-corporea/",
            publishedDate = "2026-08-18"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Se vuoi dare enfasi al pettorale puoi aggiungere un esercizio e/o aumentare i set " +
                "fino ad una massimo di 25 serie a settimana. ",
            articleTitle = "Scheda pettorali in palestra",
            url = "https://www.projectinvictus.it/scheda-pettorali/",
            publishedDate = "2026-07-07"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Nella singola seduta puoi eseguire almeno 3-4 serie di questo esercizio. ",
            articleTitle = "Alzate laterali: esecuzione corretta, varianti e muscoli coinvolti",
            url = "https://www.projectinvictus.it/alzate-laterali/",
            publishedDate = "2025-04-09"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Il volume di lavoro sul petto può spaziare molto ( 12-20 serie settimanali) ed è " +
                "consigliato allenarli da 1 a 3 volte a settimana . ",
            articleTitle = "Esercizi pettorali alti per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/pettorali-alti/",
            publishedDate = "2024-01-26"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Se non hai mai allenato i polpacci parti da 8-9 serie settimanali distribuite in 2 " +
                "sessioni allenanti , suddiviso metà su movimenti a gamba distesa e metà a ginocchio " +
                "flesso e vedi come rispondi. ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Possiamo utilizzare un range di intensità che va dalle 8 alle 30 ripetizioni , " +
                "facendo però delle considerazioni: Il range che va dalle 8 alle 20 ripetizioni è in " +
                "genere il più utilizzato perchè permette un buon rapporto tra volume , intensità di " +
                "carico e fatica accumulata. ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "PANCA: 5 serie da 5 ripetizioni Peso usato la settimana prima. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "MILITARY PRESS: 5 serie da 5 ripetizioni Peso usato la settimana prima. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "PANCA: serie da 5 ripetizioni Peso usato la settimana prima cercando di rendere " +
                "molto esplosivo ogni colpo. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Peso usato la settimana prima + 10% MILITARY PRESS: 5 serie da 5 ripetizioni Peso " +
                "usato la settimana prima cercando di rendere molto esplosivo ogni colpo. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "PANCA: 5 serie da 3 ripetizioni Peso usato la settimana prima + 10% Pausa 4-5 minuti " +
                "fra le serie. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "PANCA: 5 serie da 4 ripetizioni Peso usato la settimana prima. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "MILITARY PRESS: 5 serie da 4 ripetizioni Peso usato la settimana prima. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "PANCA: 5 serie da 4 ripetizioni Peso usato la settimana prima + 5% Pausa 4-5 minuti " +
                "fra le serie. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Peso usato la settimana prima + 5% MILITARY PRESS: 5 serie da 4 ripetizioni Peso " +
                "usato la settimana prima + 5% Pausa 4-5 minuti fra le serie. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "In questo test di 8 settimane ho strutturato l’allenamento in 2 blocchi da 3 " +
                "settimane seguiti da una settimana di Deload e test (ho già eseguito una prima " +
                "settimana di test degli RPE a volume limitato, una sorta di Acclimation). ",
            articleTitle = "L’autoregolazione nell’allenamento della forza",
            url = "https://www.projectinvictus.it/autoregolazione-allenamento-della-forza/",
            publishedDate = "2017-01-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Come allenare il Pulley basso: ripetizioni, serie, carichi e progressioni " +
                "Sicuramente il Pulley risulta un esercizio che ben si presta a lavori a range di " +
                "ripetizioni medio-alti , il cosiddetto “range ipertrofico” sulle 8-15 ripetizioni. ",
            articleTitle = "Pulley basso: tutorial ed esecuzione corretta",
            url = "https://www.projectinvictus.it/pulley-basso/",
            publishedDate = "2025-04-09"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.VOLUME,
            statement =
                "Allenali un minimo di 2 volte a settimana: la scarsa richiesta neurale ti permette " +
                "di sopportare molto volume settimanale. ",
            articleTitle = "Flessioni per allenare i pettorali",
            url = "https://www.projectinvictus.it/flessioni-pettorali/",
            publishedDate = "2022-03-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.BODYCOMP,
            statement =
                "Dieta per aumentare la massa muscolare senza ingrassare Prima di tutto, per evitare " +
                "di ingrassare troppo è bene evitare di iniziare una fase di massa se hai percentuale " +
                "di massa grassa superiore a circa l’11% per gli uomini e 16% per le donne . ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.BODYCOMP,
            statement =
                "Se hai messo su troppa massa grassa, puoi continuare con il mini-cut fino a quando " +
                "non torni ad una percentuale di massa grassa accettabile (massimo circa 13% per gli " +
                "uomini, 20% per le donne). ",
            articleTitle = "Mini cut: cos’è e come impostarlo in fase di massa",
            url = "https://www.projectinvictus.it/mini-cut/",
            publishedDate = "2024-11-16"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.BODYCOMP,
            statement =
                "In questo contesto, però, non sempre è conveniente non incrementare il tessuto " +
                "adiposo in quando potresti trovarti a fine definizione con una percentuale molto " +
                "bassa (circa 5%) di massa grassa , che è sottofisiologica . ",
            articleTitle = "Reverse diet: come impostarla",
            url = "https://www.projectinvictus.it/reverse-diet/",
            publishedDate = "2024-03-07"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.BODYCOMP,
            statement =
                "Un dimagrimento equilibrato e sano , che ti permette di bruciare davvero massa " +
                "grassa deve essere graduale : è ottimale perdere lo 0,5-1% del proprio peso corporeo " +
                "a settimana. ",
            articleTitle = "Dieta Plank: come funziona e controindicazioni",
            url = "https://www.projectinvictus.it/dieta-plank/",
            publishedDate = "2026-01-18"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.BODYCOMP,
            statement =
                "I valori di riferimento di massa grassa da mantenere sono circa il 15% per gli " +
                "uomini e il 22% per le donne. ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.BODYCOMP,
            statement =
                "Nel caso di ipocalorica più o meno marcata e bassa massa grassa, l’introito può " +
                "aumentare fino a 3 g/kg in quanto macronutriente che dà sazietà e che limita la " +
                "perdita di tessuto muscolare. ",
            articleTitle = "Proteine palestra: quali prendere e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-palestra/",
            publishedDate = "2023-09-21"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "Il range ipertrofico ( 6-12 ripetizioni ) è il miglior compromesso tra carichi medi " +
                "e tempi medi sotto tensione ( TUT ). ",
            articleTitle = "Bodybuilding: la guida completa",
            url = "https://www.projectinvictus.it/bodybuilding/",
            publishedDate = "2024-01-05"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "In alcuni esercizi può essere utile un fermo di 1-2 secondi prima della fase " +
                "concentrica oppure una fase isometrica in determinati punti del ROM – utile, ad " +
                "esempio, per enfatizzare il massimo accorciamento e lo stretch muscolare. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "Se non hai una buona tecnica di movimento di eseguire questo esercizio almeno in due " +
                "sessioni settimanali per un minimo di 3 serie in ogni sessione . ",
            articleTitle = "Tirate al petto: come si eseguono",
            url = "https://www.projectinvictus.it/tirate-al-petto/",
            publishedDate = "2021-02-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "Dovrebbe essere ovvio che l’esecuzione di 2 ripetizioni al 85% del massimle e 5 " +
                "ripetizioni al 85% del massimale consistono in un livello di sforzo/difficoltà molto " +
                "diverso, anche se l’intensità del carico è identica. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 3)",
            url = "https://www.projectinvictus.it/chiarimenti-incomprensioni-sullintensita-nellesercizio-i-pesi-parte-3/",
            publishedDate = "2014-10-16"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "Quando i risultati rallentano , piuttosto che continuare a soffrire e rinunciare " +
                "improvvisamente alla dieta, conviene fare 2 settimane di break diet , in cui torni " +
                "al tuo TDEE (il fabbisogno energetico giornaliero) e “interrompi” il regime " +
                "ipocalorico. ",
            articleTitle = "Dieta ipocalorica per dimagrire: la guida completa",
            url = "https://www.projectinvictus.it/dieta-ipocalorica/",
            publishedDate = "2025-01-13"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "Il carico che utilizzi sarà una conseguenza del numero di ripetizioni eseguito e " +
                "dell’intensità di sforzo, tempo sotto tensione scelto, range di movimento ecc… La " +
                "pressa si presta bene ai più svariati lavori, indicativamente dalle 5-6 ripetizioni " +
                "fino alle 30 ripetizioni . ",
            articleTitle = "Leg press in palestra e nel bodybuilding: esecuzione corretta",
            url = "https://www.projectinvictus.it/leg-press-bodybuilding/",
            publishedDate = "2024-06-11"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "Come indicazione le mani vanno tenute almeno ad una larghezza di 1.5 volte le spalle " +
                "e in caso di ridotta mobilità scapolo-omerale, il consiglio è quello di allargare " +
                "anche a 2 volte. ",
            articleTitle = "Lento dietro: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/lento-dietro-lento-avanti/",
            publishedDate = "2022-12-15"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "Esecuzione corretta del Military press Per eseguire al meglio il Military press con " +
                "bilanciere la presa deve essere poco più larga delle spalle (da 1.2 a 1.5 volte le " +
                "spalle). ",
            articleTitle = "Military Press: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/military-press/",
            publishedDate = "2026-04-29"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "L’esecuzione corretta prevede di posizionarsi a terra con le mani circa ad una " +
                "larghezza di 1.5 volte le spalle. ",
            articleTitle = "Flessioni per allenare i pettorali",
            url = "https://www.projectinvictus.it/flessioni-pettorali/",
            publishedDate = "2022-03-12"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.EXERCISE,
            statement =
                "Una volta raggiunte almeno 12 ripetizioni con esecuzione impeccabile, aumenta la " +
                "difficoltà dei V-push up posizionando i piedi su un rialzo come uno step, fino ad " +
                "arrivare ad un box. ",
            articleTitle = "V-push up: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/v-push-up/",
            publishedDate = "2022-12-01"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.PERIODIZATION,
            statement =
                "In questo periodo di scarico mangi prevalentemente grassi e proteine, mentre i " +
                "carboidrati sono ridotti al minimo (30 g), preferibilmente da verdura fibrosa, per " +
                "assumere il giusto quantitativo di fibre alimentari. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03"
        ),
        Rule(
            area = KnowledgeCorpus.Area.TRAINING,
            topic = Topic.PERIODIZATION,
            statement =
                "È a questo punto che entra il gioco il periodo di scarico, anche chiamato settimana " +
                "di scarico , dove si opterà per una riduzione del carico di allenamento della durata " +
                "che varia generalmente da 2 a 6 settimane , mediamente 4. ",
            articleTitle = "Mesocicli e settimana di scarico: facciamo chiarezza",
            url = "https://www.projectinvictus.it/mesocicli-di-scarico-perche-farli/",
            publishedDate = "2015-02-09"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.TIMING,
            statement =
                "Se l’obbiettivo è estetico , il post-workout può comprendere circa il 30% dei " +
                "glucidi giornalieri, con il restante 70% suddiviso negli altri pasti. ",
            articleTitle = "Carboidrati dopo l’allenamento in palestra",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-palestra/",
            publishedDate = "2024-03-26"
        ),
        Rule(
            area = KnowledgeCorpus.Area.NUTRITION,
            topic = Topic.TIMING,
            statement =
                "Ovviamente, vale la pena domandarsi se abbia senso farlo, in quando l’ assunzione di " +
                "caffeina va solitamente collocata circa un’oretta prima della seduta di allenamento, " +
                "in un range utile di 30-60 minuti, mentre di creatina post-workout insieme ad un " +
                "pasto prevalentemente glucidico. ",
            articleTitle = "Creatina e caffeina: effetti positivi e negativi",
            url = "https://www.projectinvictus.it/creatina-caffeina/",
            publishedDate = "2021-12-27"
        ),
        Rule(
            area = KnowledgeCorpus.Area.SAFETY,
            topic = Topic.SAFETY,
            statement =
                "Anche se difficile da diagnosticare con precisione, si ritiene che le articolazioni " +
                "sacro-iliache siano la fonte di dolore in circa il 25% dei pazienti affetti da " +
                "lombalgia cronica , sebbene la letteratura non abbia ancora trovato un accordo " +
                "unanime a riguardo. ",
            articleTitle = "Bacino: anatomia e funzioni",
            url = "https://www.projectinvictus.it/bacino-anatomia-funzioni/",
            publishedDate = "2020-05-27"
        ),
        Rule(
            area = KnowledgeCorpus.Area.SAFETY,
            topic = Topic.SAFETY,
            statement =
                "Da un punto di vista della prognosi, nell’85% circa dei casi il dolore svanisce " +
                "spontaneamente entro un anno ed è caratterizzato da frequenti recidive e " +
                "riacutizzazioni del dolore. ",
            articleTitle = "Gomito: anatomia e dolore",
            url = "https://www.projectinvictus.it/gomito-anatomia-dolore/",
            publishedDate = "2019-10-25"
        ),
        Rule(
            area = KnowledgeCorpus.Area.SAFETY,
            topic = Topic.SAFETY,
            statement =
                "Dolore al coccige: cause e trattamento Il dolore al coccige, detto coccigodinia, " +
                "rappresenta un problema scarsamente diffuso nella popolazione affetta da disordini " +
                "muscoloscheletrici, con una prevalenza di circa l’1%. ",
            articleTitle = "Il coccige: dolore ed anatomia",
            url = "https://www.projectinvictus.it/coccige/",
            publishedDate = "2020-03-01"
        )

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
