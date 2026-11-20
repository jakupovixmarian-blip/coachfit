package com.fitcoach.app.domain.knowledge

import com.fitcoach.app.domain.model.SourceKind

/**
 * Regole nutrizionali di Project InVictus: proteine, carboidrati, grassi,
 * bilancio calorico, idratazione, timing dei pasti.
 *
 * Ogni regola e una prescrizione con un numero verificabile e il link
 * all'articolo. Completa [InvictusCorpus], che copre la metodologia di
 * allenamento.
 *
 * Classificazione: [SourceKind.DIVULGATIVE].
 */
object InvictusNutrition {

    enum class Topic(val label: String) {
        PROTEIN("Proteine"),
        CARBS("Carboidrati"),
        FAT("Grassi"),
        CALORIES("Calorie e bilancio"),
        HYDRATION("Idratazione"),
        FIBER("Fibre"),
        TIMING("Timing"),
        ALCOHOL("Alcol"),
        SUPPLEMENT("Integratori"),
        BODYCOMP("Composizione corporea"),
        SAFETY("Sicurezza")
    }

    data class Rule(
        val topic: Topic,
        val statement: String,
        val articleTitle: String,
        val url: String,
        val publishedDate: String,
        val kind: SourceKind = SourceKind.DIVULGATIVE
    )

    val rules: List<Rule> = listOf(
        Rule(
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
            topic = Topic.PROTEIN,
            statement =
                "Se hai un fabbisogno proteico alto, ti conviene inserire proteine in buona quantità " +
                "(circa 15-30 g) anche nel primo pasto della giornata. ",
            articleTitle = "Colazione per dimagrire: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-per-dimagrire/",
            publishedDate = "2025-10-28"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Poi imposta la quantità di proteine, che è più alta rispetto a quando vuoi mantenere " +
                "o aumentare il peso: 2 – 2,6 g/kg . ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "E’ più importante la quantità totale giornaliera di proteine che il timing, è " +
                "consigliato dividere la quantità proteica in 4-5 volte al giorno , che possono " +
                "coincidere con i normali pasti che fai nell’arco della giornata. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "In questo ultimo caso, ti conviene fare uno spuntino o un pasto, a seconda " +
                "dell’orario, che contenga almeno 20 g di proteine. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Una volta si dava come indicazione massimo 30 g di proteine per pasto perché si " +
                "pensava che una quota aggiuntiva andasse sprecata, visto che la massima sintesi " +
                "proteica si raggiunge con questo quantitativo. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Se assumi meno di 20 g di proteine in uno o più pasti non c’è nessun problema: " +
                "focalizzati sul raggiungere il fabbisogno proteico della giornata e otterrai i tuoi " +
                "risultati. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Le linee guida sostengono che 0,8 g proteine/kg peso corporeo sia un quantitativo " +
                "sufficiente per ricoprire il fabbisogno proteico giornaliero . ",
            articleTitle = "Dieta proteica: la guida completa",
            url = "https://www.projectinvictus.it/dieta-proteica/",
            publishedDate = "2025-05-13"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Il range di proteine consigliato in questa fase è 2.2-2.6 g proteine/kg corporeo al " +
                "giorno . ",
            articleTitle = "Dieta proteica: la guida completa",
            url = "https://www.projectinvictus.it/dieta-proteica/",
            publishedDate = "2025-05-13"
        ),
        Rule(
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
            topic = Topic.PROTEIN,
            statement =
                "Per assumere un quantitativo più alto di proteine, consuma o aggiungi l’ albume : " +
                "100 g contengono circa 11 g di proteine e non ha tracce di carboidrati e grassi. ",
            articleTitle = "Colazione proteica: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-proteica/",
            publishedDate = "2025-04-08"
        ),
        Rule(
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
            topic = Topic.PROTEIN,
            statement =
                "In realtà, questa finestra anabolica dura più tempo: l’importante non è fare un " +
                "preciso pasto post workout ma assicurarsi di assumere abbastanza carboidrati (oltre " +
                "che energia, proteine, grassi) nel corso delle 24 ore . ",
            articleTitle = "Carboidrati dopo l’allenamento in palestra",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-palestra/",
            publishedDate = "2024-03-26"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Il range è ampissimo, quindi non c’è nemmeno bisogno di “rielaborare” le linee guida " +
                "specificamente per i vegani, semplicemente questi ultimi tenderanno a posizionarsi " +
                "verso l’estremo alto (dai 2 g/kg in poi) aiutandosi anche con gli integratori di " +
                "proteine o tramite l’assunzione di EAA (aminoacidi essenziali). ",
            articleTitle = "Dieta vegetariana: la guida completa",
            url = "https://www.projectinvictus.it/dieta-vegetariana/",
            publishedDate = "2024-03-17"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Ad esempio, per quanto riguarda le proteine , è consigliato consumarle ogni 3-4 ore " +
                "per creare un ambiente anabolico favorevole (in un contesto energetico opportuno). ",
            articleTitle = "Finestra anabolica: cos’è e cosa mangiare",
            url = "https://www.projectinvictus.it/finestra-anabolica/",
            publishedDate = "2024-03-13"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "In questo periodo di scarico mangi prevalentemente grassi e proteine, mentre i " +
                "carboidrati sono ridotti al minimo (30 g), preferibilmente da verdura fibrosa, per " +
                "assumere il giusto quantitativo di fibre alimentari. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Al contrario, se stai seguendo un regime ipocalorico aumentare le proteine fino a " +
                "1.6 – 2.6 g/kg peso corporeo potrebbe consentire un ottimo vantaggio, sia per " +
                "proteggere la massa magra, sia per sopportare meglio la fame. ",
            articleTitle = "Le proteine fanno ingrassare?",
            url = "https://www.projectinvictus.it/le-proteine-fanno-ingrassare/",
            publishedDate = "2024-01-21"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Questo viene aiutato da una dieta opportuna, che deve: farti perdere peso in modo " +
                "graduale (0.5-1% del tuo peso corporeo a settimana), non essere drastica ma " +
                "prevedere un deficit calorico moderato, farti assumere sufficienti proteine , non " +
                "escludere carboidrati e grassi, che devono essere presenti. ",
            articleTitle = "Definizione in palestra: come definire i muscoli",
            url = "https://www.projectinvictus.it/definizione-palestra/",
            publishedDate = "2024-01-15"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "La quota ottimale di proteine per la massa muscolare è di 1,4 – 2,2 g / kg al giorno " +
                ". ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Aspetto a cui prestare attenzione, per assicurarti di soddisfare il fabbisogno " +
                "proteico, è essenziale avere una dieta varia e incrementare del 10-20% la quantità " +
                "di proteine assunte rispetto alle linee guida consigliate per gli onnivori. ",
            articleTitle = "Dieta vegana bodybuilding: come funziona ed esempio",
            url = "https://www.projectinvictus.it/dieta-vegana-e-bodybuilding/",
            publishedDate = "2022-05-24"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Per soggetti che si allenano molto e che sono in forte ipocalorica, il fabbisogno " +
                "proteico è pari a 2,3-3,1 g proteine/kg peso corporeo . ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03"
        ),
        Rule(
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
            topic = Topic.PROTEIN,
            statement =
                "Se ci si allena a stomaco vuoto, per ridurre l’effetto catabolico muscolare, si " +
                "consiglia di assumere HMB in associazione a 20-40 g di proteine ad alto valore " +
                "biologico nel post-workout . ",
            articleTitle = "HMB integratore: cos’è, a cosa serve, funziona?",
            url = "https://www.projectinvictus.it/hmb-integratore/",
            publishedDate = "2022-01-31"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Per quanto le proteine vegetali abbiano un valore biologico complessivamente " +
                "inferiore a quelle animali, compensando gli amminoacidi limitanti, anche il vegano " +
                "può fare a meno degli integratori di EAA – nonostante il suo fabbisogno proteico sia " +
                "del 20% superiore rispetto all’onnivoro. ",
            articleTitle = "Aminoacidi essenziali (EAA): come e quando prenderli? Prima o dopo l’allenamento?",
            url = "https://www.projectinvictus.it/fabbisogno-aminoacidi-essenziali/",
            publishedDate = "2022-01-08"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "La quota proteica sufficiente per massimizzare la sintesi proteica è di 20-40 g (di " +
                "cui almeno 10 g di aminoacidi essenziali) di proteine ad elevato valore biologico, " +
                "ovvero complete di tutti gli aminoacidi e soprattutto che contengono quelli " +
                "essenziali. ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Ad esempio, atleti di resistenza con una richiesta energetica molto elevata possono " +
                "spingersi anche oltre il 60% di carboidrati oppure soggetti in forte dieta " +
                "ipocalorica avranno un apporto proteico maggiore. ",
            articleTitle = "Calorie macronutrienti",
            url = "https://www.projectinvictus.it/calorie-macronutrienti/",
            publishedDate = "2021-09-13"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Benefici delle proteine vegane Le proteine vegane sono digeribili al 60-80% , mentre " +
                "quelle animali al 90%: se da una parte è un aspetto negativo, dall’altro permette di " +
                "assumere meno calorie a parità di introito. ",
            articleTitle = "Proteine vegane: come assumerle?",
            url = "https://www.projectinvictus.it/proteine-vegane/",
            publishedDate = "2021-06-22"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Solitamente, la dose di proteine consigliata da assumere per massimizzare la sintesi " +
                "proteica è di 25 g. ",
            articleTitle = "Proteine vegane: come assumerle?",
            url = "https://www.projectinvictus.it/proteine-vegane/",
            publishedDate = "2021-06-22"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "E’ quindi utile assumere 20-40 g di proteine ad elevato valore biologico, una " +
                "quantità che è sufficiente a massimizzare la proteosintesi. ",
            articleTitle = "Aminoacidi e proteine: che differenza c’è?",
            url = "https://www.projectinvictus.it/aminoacidi-o-proteine/",
            publishedDate = "2021-06-14"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Le proteine, infatti, in caso di carenza energetica, vengono prelevate soprattutto " +
                "dal tessuto muscolare poichè è quello più ricco di proteine (20% del suo peso) – lo " +
                "stesso tessuto che è importante da mantenere per un dimagrimento efficace. ",
            articleTitle = "Proteine magre: quali sono i cibi proteici con pochi grassi?",
            url = "https://www.projectinvictus.it/proteine-magre/",
            publishedDate = "2021-06-05"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Colazione Per una sintesi proteica più efficace, è consigliato assumere una quota " +
                "proteica (0.25 g/kg) ogni 3 ore, perciò circa ad ogni pasto per assicurare un " +
                "costante pool ematico aminoacidico e soddisfare il fabbisogno proteico giornaliero. ",
            articleTitle = "Proteine idrolizzate: benefici e controindicazioni",
            url = "https://www.projectinvictus.it/proteine-idrolizzate/",
            publishedDate = "2021-04-22"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "In termini percentuali sulle calorie totali, contestualizzato in una dieta " +
                "ipercalorica, il fabbisogno proteico sarebbe compreso tra 25-30% kcal/die – aumenta " +
                "quando le calorie scendono. ",
            articleTitle = "Proteine: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-cosa-sono-a-cosa-servono/",
            publishedDate = "2021-04-14"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Il concetto di “limite proteico massimo assorbibile = 30 g per pasto” è stato in " +
                "parte superato. ",
            articleTitle = "Proteine: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-cosa-sono-a-cosa-servono/",
            publishedDate = "2021-04-14"
        ),
        Rule(
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
            topic = Topic.PROTEIN,
            statement =
                "Questo anche perchè le proteine vegetali sono meno facilmente assimilabili e " +
                "digeribili: in caso di alimentazione vegetariana e vegana è quindi utile " +
                "incrementare del 15-20% il fabbisogno proteico rispetto a quello consigliato per i " +
                "non vegetariani . ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Per chi fa palestra o bodybuilding è consigliato consumare circa 3-4 pasti al giorno " +
                "per massimizzare i guadagni ipertrofici soprattutto con pasti proteici (almeno 20 g " +
                "di proteine a pasto). ",
            articleTitle = "Ogni quanto mangiare per dimagrire?",
            url = "https://www.projectinvictus.it/frequenza-dei-pasti/",
            publishedDate = "2020-07-13"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Fabbisogno proteico giornaliero secondo l’OMS Nelle tabelle indicate dall’OMS per " +
                "una persona adulta (> 18 anni) sono indicati 0.66 g proteine per ogni chilo di peso " +
                "corporeo, sia per gli uomini che per le donne. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "Con l’ avanzare dell’età per le donne è bene aumentare l’apporto proteico : da circa " +
                "0.70 g/kg peso corporeo passiamo a una dose consigliata maggiore di 0.84 g di " +
                "proteine. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "225 sono i grammi di proteine da assumere al giorno (pari a circa 2.8 g/kg di peso " +
                "corporeo). ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03"
        ),
        Rule(
            topic = Topic.PROTEIN,
            statement =
                "C’è un range indicativo molto ampio all’interno del quale puoi muoverti, a partire " +
                "dai 0.66 g proteine/kg massa corporea. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Per i carboidrati , se sei attivo e ti alleni, soprattutto se ha una buona massa " +
                "muscolare, può beneficiare di un maggiore apporto di carboidrati, che mediamente " +
                "varia tra 3 e 7 g/kg. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Perdere peso a carico della massa magra non è consigliato e basarsi solamente sul " +
                "numero segnato sulla bilancia non è una strategia ottimale: veder calare il proprio " +
                "peso di 1 kg è facile, è sufficiente mangiare di meno (soprattutto carboidrati), " +
                "bere di meno, introdurre poco sale per qualche giorno. ",
            articleTitle = "Deficit calorico: cos’è e come calcolarlo",
            url = "https://www.projectinvictus.it/deficit-calorico-calcolo/",
            publishedDate = "2025-01-15"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Secondo i LARN, ogni giorno dovrebbero essere consumati 25-30 g di fibra alimentare, " +
                "ottenibile proprio tramite le porzioni e gli alimenti sopra indicati. ",
            articleTitle = "Dimagrire velocemente e perdere peso",
            url = "https://www.projectinvictus.it/dimagrire-velocemente/",
            publishedDate = "2025-01-12"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Se non ingrassi, continua pure ad aumentare i carboidrati la settimana successiva, " +
                "altrimenti , non appena la massa grassa inizia a beneficiare del surplus calorico " +
                "più della massa magra, inserisci una fase di 4 settimane di ipocalorica, per poi " +
                "ricominciare a re-introdurre 20-25 g di carboidrati ogni settimana. ",
            articleTitle = "Come accelerare il metabolismo",
            url = "https://www.projectinvictus.it/come-accelerare-il-metabolismo/",
            publishedDate = "2024-10-15"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Attenzione a non eccedere con grassi saturi (formaggi e carni grasse), sale negli " +
                "alimenti e assicurati di assumere dal cibo almeno 25 – 30 g di fibre alimentari al " +
                "giorno (tramite frutta e verdura). ",
            articleTitle = "Dieta ormonale: cos’è e come funziona",
            url = "https://www.projectinvictus.it/dieta-ormonale/",
            publishedDate = "2024-04-02"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Ma non solo, anche a livello di salute generale e prospettiva di vita, i carboidrati " +
                "dovrebbero occupare il 45-60% del tuo fabbisogno calorico giornaliero. ",
            articleTitle = "Paleo dieta: come funziona e cosa mangiare",
            url = "https://www.projectinvictus.it/la-paleo-dieta/",
            publishedDate = "2024-02-20"
        ),
        Rule(
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
            topic = Topic.CARBS,
            statement =
                "Dopo la prima fase di 14-21 giorni, a seconda di quanto ci metti ad abituarti alla " +
                "chetosi, Di Pasquale consiglia i seguenti accorgimenti che devono essere adattati " +
                "sulla persona: Durante il periodo di scarico settimanale (da lunedì a venerdì) trova " +
                "il quantitativo minimo di carboidrati con cui ti trovi bene. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "L’approccio della ricarica il mercoledì è molto più sensato, puoi anche farlo il " +
                "giovedì togliendo i carboidrati per massimo 3 giorni. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "I giorni ad alto apporto glucidico hanno 50-100 g d i carboidrati in più rispetto a " +
                "quelli a basso apporto glucidico . ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Le linee guida consigliano di consumare al massimo il 10% di zuccheri nella propria " +
                "dieta all’interno del fabbisogno di carboidrati. ",
            articleTitle = "Dieta senza zuccheri",
            url = "https://www.projectinvictus.it/dieta-senza-zuccheri/",
            publishedDate = "2023-12-08"
        ),
        Rule(
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
            topic = Topic.CARBS,
            statement =
                "Quelle ipotoniche hanno una minore concentrazione di carboidrati (< 6 g/100 ml) e " +
                "sono utili quando l’idratazione è più necessaria rispetto all’apporto glucidico. ",
            articleTitle = "Bevande energetiche per lo sport",
            url = "https://www.projectinvictus.it/bevande-energetiche/",
            publishedDate = "2023-06-07"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Per capire se la pasta, il riso o il pane che stai comprando sono davvero integrali, " +
                "guarda l’ etichetta nutrizionale : se per 100 g ci sono almeno 7 g di fibre e 2 g di " +
                "lipidi allora è vero integrale ! ",
            articleTitle = "Cibi integrali: quali sono?",
            url = "https://www.projectinvictus.it/cibi-integrali/",
            publishedDate = "2023-05-22"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Una dieta povera di carboidrati è una dieta in cui l’apporto di questo " +
                "macronutriente è minore rispetto alle linee guida internazionali che si aggirano sul " +
                "45-65% dell’energia totale. ",
            articleTitle = "Dieta povera di carboidrati: cos’è, vantaggi e menù",
            url = "https://www.projectinvictus.it/dieta-povera-di-carboidrati/",
            publishedDate = "2023-03-05"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Infine per i carboidrati il quantitativo raccomandato minimo è di 2.5 g/kg (3 per " +
                "gli sportivi) . ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Superata infatti una determinata quota di carboidrati (350-400 g) l’apporto di fibre " +
                "è assicurato anche mangiando fonti raffinate. ",
            articleTitle = "Calorie macronutrienti",
            url = "https://www.projectinvictus.it/calorie-macronutrienti/",
            publishedDate = "2021-09-13"
        ),
        Rule(
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
            topic = Topic.CARBS,
            statement =
                "La percentuale di carboidrati consigliata corrisponde a 45-60% del fabbisogno " +
                "energetico totale giornaliero ed è abbastanza ampio proprio perché puoi “muoverti” " +
                "in questo intervallo a seconda della tua soggettività. ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Gli sportivi rispetto ai sedentari: è chiaro che una persona attiva necessita di una " +
                "quota maggiore di energia, con un quantitativo glucidico anche doppio rispetto ad un " +
                "sedentario e che può arrivare fino a 10-12g di carboidrati per kg di peso corporeo " +
                "al giorno! ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Quanti carboidrati semplici Al giorno al massimo il 10% dell’energia deve provenire " +
                "dagli zuccheri, il che significa che è bene limitarne il consumo. ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Una volta risolta l’equazione, sai quante calorie assumere con i carboidrati ; per " +
                "ottenere i grammi sarà sufficiente dividere questo numero per 4 (dato che 1 g di " +
                "carboidrati corrispondono a 4 kcal). ",
            articleTitle = "Quanti carboidrati per dimagrire?",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno-per-dimagrire/",
            publishedDate = "2020-04-02"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Gli zuccheri (i carboidrati semplici) non sono da escludere ma sicuramente da " +
                "limitare: l’ideale è ricavare al massimo il 10-12% del fabbisogno energetico " +
                "giornaliero dagli zuccheri . ",
            articleTitle = "Quanti carboidrati per dimagrire?",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno-per-dimagrire/",
            publishedDate = "2020-04-02"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "In ogni caso, ai fini del calcolo calorico è inutile perdersi in questo conteggio e " +
                "considerare comunque come valore medio 4 kcal/g per tutti i carboidrati, perché " +
                "l’errore dovuto a questa approssimazione è molto trascurabile rispetto al totale " +
                "delle calorie che vai ad assumere in un giorno o nella settimana. ",
            articleTitle = "Fruttosio e glucosio, differenze: che cosa cambia?",
            url = "https://www.projectinvictus.it/fruttosio-glucosio-differenze/",
            publishedDate = "2020-03-31"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Idealmente, per gli atleti è consigliato il consumo di 1.0-1.5 g di carboidrati/kg " +
                "di peso corporeo ogni 2 ore nelle 6 ore dopo l’allenamento. ",
            articleTitle = "Destrosio in palestra: che cos’è e a cosa serve",
            url = "https://www.projectinvictus.it/destrosio-bodybuilding-palestra/",
            publishedDate = "2020-03-25"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Carboidrati 45-60% Nonostante i carboidrati alimentari non siano essenziali alla " +
                "sopravvivenza, il glucosio lo è; in natura il modo più conveniente, sostenibile e " +
                "pratico di soddisfare il fabbisogno di glucosio è consumare le fonti amidacee. ",
            articleTitle = "Dieta dimagrante: come farla in modo efficace",
            url = "https://www.projectinvictus.it/dieta-dimagrante/",
            publishedDate = "2018-10-02"
        ),
        Rule(
            topic = Topic.CARBS,
            statement =
                "Di conseguenza la NPWHC dovrebbe sostenere di evitare qualsiasi fonte di carboidrati " +
                "che mantiene la glicemia elevata anche subito dopo l’esercizio, come un pasto misto " +
                "a base di carboidrati mangiato 5-6 ore prima. ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04"
        ),
        Rule(
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
            topic = Topic.CARBS,
            statement =
                "Ciò che contraddice questa ipotesi è che la sovraregolazione di questi processi " +
                "avviene in particolar modo nelle prime 2 ore post-allenamento (1,3), che su queste " +
                "basi sembra essere uno dei momenti in cui può essere relativamente vantaggioso " +
                "assumere carboidrati in grandi quantità. ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Secondo i LARN, punto di riferimento per l’assunzione dei nutrienti in Italia, la " +
                "quantità di grassi da assumere per la popolazione italiana corrisponde al 20-35% del " +
                "totale calorico giornaliero oppure fino a 1,5 g di grassi/kg peso corporeo. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Bisogna, però, differenziare la quantità dei grassi a seconda della loro qualità: i " +
                "grassi saturi vanno limitati al massimo al 10% del tuo fabbisogno calorico " +
                "giornaliero totale. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "In fase di massa , le calorie sono più alte e quindi anche i grassi potranno essere " +
                "rialzati, sempre stando nel range 20-35% o alzando al massimo al 40%. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Secondo le linee guida LARN , una dieta ottimale prevede che il 20-35% dell’energia " +
                "giornaliera provenga dai grassi. ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Non è così, però, per la dieta low-fat, in cui i grassi ricoprono al massimo il 15% " +
                "dell’introito energetico giornaliero. ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Inoltre, le donne tendenzialmente avranno bisogno di una quantità maggiore di grassi " +
                "rispetto agli uomini (minimo 0,8 g/kg). ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Per gli sportivi , i grassi dovrebbero coprire circa il 30% del fabbisogno " +
                "energetico, ma nel periodo di dimagrimento questa quota può scendere al 20%. ",
            articleTitle = "Dieta ipolipidica: cos’è e come funziona?",
            url = "https://www.projectinvictus.it/dieta-ipolipidica/",
            publishedDate = "2025-12-20"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Nella dieta non possono mancare: frutta e verdura pesce grassi buoni (olio, frutta " +
                "secca) legumi cereali integrali e non carne fresca Anche l’ acqua , soprattutto nei " +
                "periodi caldi e durante l’allenamento, non è da dimenticare per restare " +
                "correttamente idratati: va consumata almeno 1 ml per ogni chilocaloria ingerita. ",
            articleTitle = "Dieta per avere gli addominali",
            url = "https://www.projectinvictus.it/dieta-per-addominali/",
            publishedDate = "2025-08-08"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Inoltre, anche se olio, frutta secca, avocado, salmone sono fonti “buone” di grassi, " +
                "sono comunque da limitare perché le calorie dei grassi sono sempre 9 kcal/grammo a " +
                "prescindere dall’alimento e che facciano bene o male alla salute. ",
            articleTitle = "Dieta per avere gli addominali",
            url = "https://www.projectinvictus.it/dieta-per-addominali/",
            publishedDate = "2025-08-08"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Questo stato di “carenza energetica volontaria” (e necessaria) non dev’essere troppo " +
                "marcata ma moderata: – 350-500 kcal al giorno rispetto al fabbisogno, in modo da " +
                "poter essere sostenibile nel tempo e perdere grasso piuttosto che massa magra. ",
            articleTitle = "Non riesco a dimagrire: rimedi",
            url = "https://www.projectinvictus.it/non-riesco-a-dimagrire/",
            publishedDate = "2025-06-03"
        ),
        Rule(
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
            topic = Topic.FAT,
            statement =
                "Molti autori consigliano range tra 1.5 – 2.1 g/kg peso corporeo : eccedendo con le " +
                "calorie ti ritroverai non solo più grosso ma anche più grasso . ",
            articleTitle = "Le proteine fanno ingrassare?",
            url = "https://www.projectinvictus.it/le-proteine-fanno-ingrassare/",
            publishedDate = "2024-01-21"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Attenzione a non esagerare con i grassi saturi e consuma formaggi grassi massimo 1-2 " +
                "volte alla settimana. ",
            articleTitle = "Cosa mangiare per ingrassare in modo sano",
            url = "https://www.projectinvictus.it/cosa-mangiare-per-ingrassare/",
            publishedDate = "2023-07-27"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Puoi ricavare i grassi anche dalla carne e formaggi ma i grassi saturi non " +
                "dovrebbero superare il 10% delle calorie giornaliere. ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Considera il tuo fabbisogno energetico totale della giornata : se più del 10% " +
                "dell’energia che ti serve deriva dagli acidi grassi saturi è meglio iniziare a " +
                "ridurli! ",
            articleTitle = "Gli acidi grassi saturi fanno male?",
            url = "https://www.projectinvictus.it/acidi-grassi-saturi-fanno-bene-o-male/",
            publishedDate = "2022-05-31"
        ),
        Rule(
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
            topic = Topic.FAT,
            statement =
                "I grassi in fase di cut coprono il 15-25% del fabbisogno calorico totale, con un " +
                "minimo di 0,5 g/kg per assicurare la corretta funzionalità dell’organismo. ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Per i grassi si consiglia un range intorno al 25-30% delle calorie giornaliere. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Si consiglia di non superare il 10% delle calorie dai grassi saturi. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Le vitamine si dividono in liposolubili : Vitamina A, vitamina D , Vitamina E, " +
                "Vitamina K, Sono veicolate dai grassi alimentari (almeno 30 g al giorno) e si " +
                "depositano nelle riserve lipidiche del nostro organismo, nelle membrane cellulari e " +
                "nel fegato. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Come gli altri grassi, contribuiscono a mantenere il bilancio energetico positivo " +
                "quando assunti nella dieta e sono molto calorici: apportano 9 kcal per grammo . ",
            articleTitle = "Grassi monoinsaturi: fanno male o fanno bene? I benefici",
            url = "https://www.projectinvictus.it/grassi-monoinsaturi/",
            publishedDate = "2021-07-08"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Oltre al quanti grassi assumere al giorno , parametro importante per il bilancio " +
                "energetico e la salute, anche la qualità lipidica è da considerare: la quantità di " +
                "grassi saturi e monoinsaturi trans possono ricoprire al massimo il 10% del " +
                "fabbisogno energetico, i polinsaturi il 5-10%. ",
            articleTitle = "Grassi monoinsaturi: fanno male o fanno bene? I benefici",
            url = "https://www.projectinvictus.it/grassi-monoinsaturi/",
            publishedDate = "2021-07-08"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Dosaggio L’apporto di grassi essenziali consigliato è pari al 4-5% del fabbisogno " +
                "calorico giornaliero, da comprendere nella quota lipidica che generalmente in una " +
                "dieta equilibrata corrisponde al 25-30% – per approfondire, leggi quanti grassi " +
                "assumere al giorno . ",
            articleTitle = "Acidi grassi essenziali: quali sono e perché sono importanti?",
            url = "https://www.projectinvictus.it/acidi-grassi-essenziali/",
            publishedDate = "2021-07-06"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Sono in generale grassi “buoni” , da preferire rispetto a quelli “cattivi” per le " +
                "loro proprietà e utilità, Gli AGE contribuiscono al fabbisogno energetico quotidiano " +
                "per il 4-5% e derivano sia da fonti vegetali ( frutta secca, oli di semi, …) che " +
                "animali ( pesce ). ",
            articleTitle = "Acidi grassi essenziali: quali sono e perché sono importanti?",
            url = "https://www.projectinvictus.it/acidi-grassi-essenziali/",
            publishedDate = "2021-07-06"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Alcuni esempi per come assumere grassi essenziali: mangiare pesce grasso almeno 2 " +
                "volte a settimana, utilizzare delle noci o altra frutta secca come spuntino, " +
                "aggiungere semi o oli vegetali di semi (di lino, di arachide,…) sulla verdura. ",
            articleTitle = "Acidi grassi essenziali: quali sono e perché sono importanti?",
            url = "https://www.projectinvictus.it/acidi-grassi-essenziali/",
            publishedDate = "2021-07-06"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Ti traduco due estratti che, secondo me, sono significativi: “…che il processo di " +
                "idrogenazione dei grassi industriali può portare al 60% di TFAs tra i grassi totali " +
                "(di un alimento), quando nei grassi dei ruminanti il livello di TFAs non supera il 5 " +
                "o 6%.” (Pag. ",
            articleTitle = "Acidi grassi trans, salute e rischi: cosa sono e perché è meglio evitarli?",
            url = "https://www.projectinvictus.it/acidi-grassi-trans/",
            publishedDate = "2021-06-30"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Questi grassi coprono la maggior parte del fabbisogno lipidico giornaliero (> 20%), " +
                "mentre il restante 10% (valore limite massimo) è fornito da grassi saturi. ",
            articleTitle = "Acidi grassi trans, salute e rischi: cosa sono e perché è meglio evitarli?",
            url = "https://www.projectinvictus.it/acidi-grassi-trans/",
            publishedDate = "2021-06-30"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "La percentuale ideale di grassi nella dieta equilibrata di un adulto sano (normo o " +
                "ipocalorica) potrebbe essere del 25% circa sulle calorie totali; ai bambini si " +
                "consiglia invece il 30% . ",
            articleTitle = "Grassi: cosa sono, quali sono e che funzione hanno?",
            url = "https://www.projectinvictus.it/grassi/",
            publishedDate = "2021-06-25"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "La quantità di grassi “ cattivi ” , ovvero quelli saturi e monoinsaturi in " +
                "conformazione trans, non dovrebbe superare il 10% delle calorie totali nella dieta. ",
            articleTitle = "Grassi: cosa sono, quali sono e che funzione hanno?",
            url = "https://www.projectinvictus.it/grassi/",
            publishedDate = "2021-06-25"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "Per una dieta salutare ed equilibrata, i grassi saturi non dovrebbero superare il " +
                "10% del fabbisogno energetico a prescindere dalla fonte da cui questi derivino. ",
            articleTitle = "Grassi vegetali: caratteristiche e benefici",
            url = "https://www.projectinvictus.it/grassi-vegetali/",
            publishedDate = "2021-06-24"
        ),
        Rule(
            topic = Topic.FAT,
            statement =
                "La restante percentuale (7-10% massimo) viene data dai grassi saturi. ",
            articleTitle = "Grassi buoni: quali sono e a cosa servono?",
            url = "https://www.projectinvictus.it/grassi-buoni/",
            publishedDate = "2021-06-23"
        ),
        Rule(
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
            topic = Topic.CALORIES,
            statement =
                "Indicando un range più ampio, tra le 150 e le 500 kcal . ",
            articleTitle = "Calorie bruciate in palestra facendo pesi e bodybuilding",
            url = "https://www.projectinvictus.it/calorie-palestra-bodybuilding/",
            publishedDate = "2026-08-10"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Se 3 allenamenti a settimana sono sufficienti per notare cambiamenti della " +
                "composizione corporea , considera che poi ogni allenamento aggiunto contribuisce a " +
                "creare una maggiore spesa energetica, specialmente se ogni sessione allenante " +
                "consuma almeno 300 kcal. ",
            articleTitle = "Il migliore allenamento per dimagrire",
            url = "https://www.projectinvictus.it/allenamento-per-dimagrire/",
            publishedDate = "2026-05-15"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "L’indicazione delle calorie da assumere solo a colazione non è sufficiente per " +
                "garantire un effettivo dimagrimento: puoi ingerire 100 kcal o 500 kcal di mattina e " +
                "dimagrire o ingrassare in entrambi i casi. ",
            articleTitle = "Colazione per dimagrire: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-per-dimagrire/",
            publishedDate = "2025-10-28"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "L’ Organizzazione Mondiale della Sanit à (OMS) consiglia di limitare l’ assunzione " +
                "di zuccheri liberi a meno del 10% delle calorie totali giornaliere . ",
            articleTitle = "Il fruttosio fa male?",
            url = "https://www.projectinvictus.it/fruttosio-fa-male/",
            publishedDate = "2025-10-16"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Una condizione di bassa disponibilità energetica (<30 kcal/kg massa magra) è da " +
                "evitare sia per la salute sia per la perdita di peso. ",
            articleTitle = "Non riesco a dimagrire: rimedi",
            url = "https://www.projectinvictus.it/non-riesco-a-dimagrire/",
            publishedDate = "2025-06-03"
        ),
        Rule(
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
            topic = Topic.CALORIES,
            statement =
                "L’uso del tapis roulant per almeno 3 volte alla settimana, insieme al deficit " +
                "calorico creato con la dieta, ti permetterà di aumentare il dispendio energetico " +
                "favorendo il processo di dimagrimento. ",
            articleTitle = "Tapis roulant per dimagrire",
            url = "https://www.projectinvictus.it/tapis-roulant-per-dimagrire/",
            publishedDate = "2024-10-13"
        ),
        Rule(
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
            topic = Topic.CALORIES,
            statement =
                "Dieta L’ectomorfo puro deve seguire una dieta ipercalorica continuando ad alzare le " +
                "calorie finchè non vede il peso aumentare gradualmente ( 0,5 – 1 % del proprio peso " +
                "corporeo in un mese). ",
            articleTitle = "Ectomorfo: caratteristiche, dieta ed allenamento",
            url = "https://www.projectinvictus.it/ectomorfo/",
            publishedDate = "2024-03-08"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Soggetto Surplus calorico Donna +200 kcal Uomo +300 kcal Non aumentare in modo " +
                "eccessivo l’input calorico: a più calorie introdotte non corrisponde un maggior " +
                "sviluppo muscolare, in quanto la sintesi proteica è comunque limitata. ",
            articleTitle = "Dieta ipercalorica per prendere peso e aumentare la massa magra",
            url = "https://www.projectinvictus.it/dieta-ipercalorica/",
            publishedDate = "2024-01-31"
        ),
        Rule(
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
            topic = Topic.CALORIES,
            statement =
                "Il consiglio è quello di aumentare di circa il 10-15% le calorie in entrata. ",
            articleTitle = "Cosa mangiare per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/cosa-mangiare-per-aumentare-la-massa-muscolare/",
            publishedDate = "2023-10-09"
        ),
        Rule(
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
            topic = Topic.CALORIES,
            statement =
                "Fornendo 9 kcal/g , se in aggiunta alla dieta consuetudinaria, gli MCT non possono " +
                "far altro che aumentare il bilancio energetico . ",
            articleTitle = "Acidi grassi a media catena (MCT): cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/mct-trigliceridi-grassi-media-catena/",
            publishedDate = "2022-01-28"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Così come per il BMI dare una dieta di 2500 kcal dà un’indicazione statistica " +
                "generale di quanto più o meno uno dovrebbe mangiare, è un dato anche questo " +
                "immediatamente spendibile e facilmente comprensibile. ",
            articleTitle = "Le calorie fanno ingrassare veramente?",
            url = "https://www.projectinvictus.it/una-calorie-e-un-caloria-vediamo-di-fare-chiarezza/",
            publishedDate = "2021-11-01"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Stimando la media di quella che trovi tutti i giorni nel banco frutta, l’apporto " +
                "calorico si aggira intorno alle 50 kcal/100g. ",
            articleTitle = "Calorie frutta: tabella",
            url = "https://www.projectinvictus.it/calorie-frutta-tabella/",
            publishedDate = "2021-10-20"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "L’OMS consiglia per gli zuccheri semplici di non superare il 10% delle calorie " +
                "giornaliere, pertanto è bene, quando possibile, limitare tutte e due questi zuccheri " +
                "semplici. ",
            articleTitle = "Fruttosio o zucchero? Qual è il migliore?",
            url = "https://www.projectinvictus.it/fruttosio-o-zucchero-qual-e-il-migliore/",
            publishedDate = "2021-10-08"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Tra queste troviamo anche l’ acqua e l’ alcol , il quale apporta 7kcal/g ma non ha " +
                "altre funzioni ed è tossico per l’organismo, quindi normalmente non viene annoverato " +
                "tra i nutrienti . ",
            articleTitle = "Sostanze nutritive: cosa sono e quali sono?",
            url = "https://www.projectinvictus.it/sostanze-nutritive/",
            publishedDate = "2021-09-30"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Pranzo Dovrebbe essere il pasto più abbondante, con circa il 40% delle calorie. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Cena Dovrebbe costituire il secondo pasto più abbondante, con circa il 35% delle " +
                "calorie. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "I pasti principali della giornata (pranzo e cena) dovrebbero rappresentare la quota " +
                "calorica maggiore, ciascuno circa il 35% delle calorie totali giornaliere ed è utile " +
                "che si completino a vicenda dal punto di vista nutrizionale. ",
            articleTitle = "Cosa mangiare a cena per dimagrire?",
            url = "https://www.projectinvictus.it/cena-per-dimagrire/",
            publishedDate = "2021-04-06"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Abbuffata notturna di dolci Tra i disturbi dell’alimentazione c’è anche la sindrome " +
                "dei mangiatori notturni : il soggetto interrompe il sonno, tende a fare uno spuntino " +
                "prima di riaddormentarsi, mangia almeno il 25% delle calorie dopo cena e durante la " +
                "notte. ",
            articleTitle = "Abbuffate di dolci e cibo: ingrasso tanto?",
            url = "https://www.projectinvictus.it/abbuffate-dolci-natalizie-sopravvivere/",
            publishedDate = "2020-09-10"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Fai uno sgarro da 1500 kcal, quantità che per alcune ragazze potrebbe corrispondere " +
                "addirittura ad un intero fabbisogno calorico di una giornata. ",
            articleTitle = "Abbuffate di dolci e cibo: ingrasso tanto?",
            url = "https://www.projectinvictus.it/abbuffate-dolci-natalizie-sopravvivere/",
            publishedDate = "2020-09-10"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Tuttavia, è sempre il bilancio calorico che fa da padrone quando vuoi dimagrire o " +
                "ingrassare: se in un giorno mangi 1000 kcal di dolci in un colpo solo ma poi non " +
                "mangi nient’altro e sei al di sotto del tuo fabbisogno energetico non ingrasserai! ",
            articleTitle = "Grassi e carboidrati nello stesso pasto fanno ingrassare?",
            url = "https://www.projectinvictus.it/grassi-ed-carboidrati-vanno-mangiati-assieme/",
            publishedDate = "2020-08-31"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Questo come caso estremo, ma è per farti capire che una volta impostato il deficit " +
                "puoi benissimo inserire gli alimenti che preferisci e come ti va meglio : puoi " +
                "assumere molte poche calorie da lunedì a venerdì, per poi sgarrare liberamente nel " +
                "weekend… Ma restando sempre nel limite delle 12.000 kcal! ",
            articleTitle = "Sgarro nella dieta: come gestirlo?",
            url = "https://www.projectinvictus.it/sgarro-dieta-bodybuilding/",
            publishedDate = "2020-08-13"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Fare più pasti ravvicinati tra loro (ogni 1, 2, 3 ore) non aumenta il metabolismo e " +
                "quindi non fa dimagrire di più : a meno che questo non ti aiuti, in regime " +
                "ipocalorico, a mantenere più aderenza alla dieta. ",
            articleTitle = "Ogni quanto mangiare per dimagrire?",
            url = "https://www.projectinvictus.it/frequenza-dei-pasti/",
            publishedDate = "2020-07-13"
        ),
        Rule(
            topic = Topic.CALORIES,
            statement =
                "Un grammo di destrosio corrisponde a 3.74 kcal e, tra i vari parametri nutrizionali, " +
                "è classificato come glucide . ",
            articleTitle = "Destrosio in palestra: che cos’è e a cosa serve",
            url = "https://www.projectinvictus.it/destrosio-bodybuilding-palestra/",
            publishedDate = "2020-03-25"
        ),
        Rule(
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
            topic = Topic.CALORIES,
            statement =
                "Decidiamo d’aumentare la nostra quota proteica, sforando il nostri equilibrio " +
                "energetico di 500kcal. ",
            articleTitle = "Calorie per dimagrire: capiamo Ormoni, macronutrienti e calorie",
            url = "https://www.projectinvictus.it/ormoni-macronutrienti-e-calorie/",
            publishedDate = "2016-03-10"
        ),
        Rule(
            topic = Topic.HYDRATION,
            statement =
                "Per le donne l’apporto consigliato è di circa 1,5-2 litri, mentre per gli uomini " +
                "2-2,5 litri – entrambe quantità da personalizzare in base a quanti liquidi si " +
                "assumono tramite gli alimenti, al peso corporeo e all’attività fisica svolta. ",
            articleTitle = "Voglio dimagrire 20 kg: come perderli in modo definitivo",
            url = "https://www.projectinvictus.it/voglio-dimagrire-20-kg/",
            publishedDate = "2024-08-24"
        ),
        Rule(
            topic = Topic.TIMING,
            statement =
                "Alcuni esperti asseriscono infatti che il “timing” possa influire al massimo per il " +
                "5-10% circa nel totale della dieta . ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02"
        ),
        Rule(
            topic = Topic.SUPPLEMENT,
            statement =
                "Dose giornaliera di vitamina D: fabbisogno giornaliero La concentrazione sierica di " +
                "vitamina D è dipendente sia dalla quantità prodotta dalla cute grazie ai raggi " +
                "solare che da quella introdotta con la dieta; la sua emivita è di circa 15 giorni. ",
            articleTitle = "Vitamina D per l’allenamento",
            url = "https://www.projectinvictus.it/vitamina-d/",
            publishedDate = "2021-08-16"
        ),
        Rule(
            topic = Topic.BODYCOMP,
            statement =
                "Dieta per aumentare la massa muscolare senza ingrassare Prima di tutto, per evitare " +
                "di ingrassare troppo è bene evitare di iniziare una fase di massa se hai percentuale " +
                "di massa grassa superiore a circa l’11% per gli uomini e 16% per le donne . ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12"
        )
    )

    fun forTopic(topic: Topic): List<Rule> = rules.filter { it.topic == topic }
}
