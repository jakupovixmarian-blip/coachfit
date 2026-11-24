package com.fitcoach.app.domain.knowledge

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
        SPLIT("Split e scelta della divisione", Area.ALLENAMENTO),
        FREQUENCY("Frequenza per gruppo muscolare", Area.ALLENAMENTO),
        EXERCISE_SELECT("Selezione e ordine degli esercizi", Area.ALLENAMENTO),
        VOLUME("Volume settimanale e per seduta", Area.ALLENAMENTO),
        INTENSITY("Intensita, RIR e RPE", Area.ALLENAMENTO),
        REPS("Range di ripetizioni", Area.ALLENAMENTO),
        REST("Recuperi tra serie", Area.ALLENAMENTO),
        PROGRESSION("Progressive overload", Area.ALLENAMENTO),
        PERIODIZATION("Periodizzazione e mesocicli", Area.ALLENAMENTO),
        DELOAD("Deload e scarico", Area.ALLENAMENTO),
        FATIGUE("Gestione dell'affaticamento", Area.ALLENAMENTO),
        TECHNIQUE("Tecniche d'intensita", Area.ALLENAMENTO),
        EXECUTION("Esecuzione e tecnica", Area.ALLENAMENTO),
        WARMUP("Riscaldamento", Area.ALLENAMENTO),
        CARDIO("Cardio e attivita accessoria", Area.ALLENAMENTO),
        LEVEL("Differenze per livello di esperienza", Area.ALLENAMENTO),
        GOAL_TRAIN("Obiettivi di allenamento", Area.ALLENAMENTO),
        SAFETY_TRAIN("Sicurezza e dolore", Area.ALLENAMENTO),
        CALORIES("Calorie, deficit e surplus", Area.NUTRIZIONE),
        METABOLISM("Metabolismo e dispendio", Area.NUTRIZIONE),
        PROTEIN("Proteine", Area.NUTRIZIONE),
        CARBS("Carboidrati", Area.NUTRIZIONE),
        FAT("Grassi", Area.NUTRIZIONE),
        FIBER("Fibre", Area.NUTRIZIONE),
        MEALS("Numero e composizione dei pasti", Area.NUTRIZIONE),
        TIMING("Timing e distribuzione", Area.NUTRIZIONE),
        CARB_CYCLING("Ciclicita dei carboidrati e ricariche", Area.NUTRIZIONE),
        HYDRATION("Idratazione", Area.NUTRIZIONE),
        ALCOHOL("Alcol", Area.NUTRIZIONE),
        SUBSTITUTION("Sostituzioni e flessibilita", Area.NUTRIZIONE),
        ADHERENCE("Aderenza e sostenibilita", Area.NUTRIZIONE),
        WEIGHT("Peso e sua lettura", Area.NUTRIZIONE),
        BODYCOMP("Composizione corporea", Area.NUTRIZIONE),
        SUPPLEMENT("Integratori", Area.NUTRIZIONE),
        SAFETY_NUT("Sicurezza, allergie, patologie", Area.NUTRIZIONE);
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
        Rule(
            category = Category.SPLIT,
            statement =
                "Scheda pettorali in multifrequenza Vediamo una scheda per il petto che lo alleni 2 volte " +
                "a settimana (18 serie). ",
            articleTitle = "Scheda pettorali in palestra",
            url = "https://www.projectinvictus.it/scheda-pettorali/",
            publishedDate = "2026-07-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SPLIT,
            statement =
                "In ottica di periodizzazione puoi anche provare a fare 6 – 9 mesi in multifrequenza e 6 " +
                "– 3 mesi in monofrequenza, con l’obiettivo di cambiare gli stimoli allenanti. ",
            articleTitle = "Monofrequenza o multifrequenza?",
            url = "https://www.projectinvictus.it/monofrequenza-o-multifrequenza/",
            publishedDate = "2026-02-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SPLIT,
            statement =
                "I vantaggi della multifrequenza sono quelli di stimolare un muscolo almeno 2 volte a " +
                "settimana . ",
            articleTitle = "Monofrequenza o multifrequenza?",
            url = "https://www.projectinvictus.it/monofrequenza-o-multifrequenza/",
            publishedDate = "2026-02-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SPLIT,
            statement =
                "I vantaggi della multifrequenza sono quelli di stimolare un muscolo almeno 2 volte a " +
                "settimana , invece che una sola volta come succede se ti alleni in monofrequenza . ",
            articleTitle = "Allenamento in multifrequenza: 5 schede di allenamento",
            url = "https://www.projectinvictus.it/allenamento-in-multifrequenza/",
            publishedDate = "2024-04-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SPLIT,
            statement =
                "Un’ultima versione prevede per chi si allena in monofrequenza di allenare un gruppo " +
                "muscolare singolarmente, allenandosi 5 – 6 giorni a settimana. ",
            articleTitle = "Gruppi muscolari: quali sono e come allenarli in palestra",
            url = "https://www.projectinvictus.it/gruppi-muscolari/",
            publishedDate = "2024-01-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SPLIT,
            statement =
                "Anche la diatriba tra monofrequenza (allenare uno stesso muscolo ogni 7 giorni) e " +
                "multifrequenza (allenarlo più volte nell’arco della settimana) non avrà mai fine in " +
                "quanto ci saranno sempre persone cresciute con un modo o l’altro. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SPLIT,
            statement =
                "Chi invece si allena in multifrequenza , stimolando lo stesso muscolo 2-3 volte nella " +
                "stessa settimana, più facilmente può sentire il bisogno di staccare la spina. ",
            articleTitle = "Programmare la fase di scarico",
            url = "https://www.projectinvictus.it/programmare-fase-scarico/",
            publishedDate = "2017-02-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Scheda pettorali in multifrequenza Vediamo una scheda per il petto che lo alleni 2 volte " +
                "a settimana (18 serie). ",
            articleTitle = "Scheda pettorali in palestra",
            url = "https://www.projectinvictus.it/scheda-pettorali/",
            publishedDate = "2026-07-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "In ottica di periodizzazione puoi anche provare a fare 6 – 9 mesi in multifrequenza e 6 " +
                "– 3 mesi in monofrequenza, con l’obiettivo di cambiare gli stimoli allenanti. ",
            articleTitle = "Monofrequenza o multifrequenza?",
            url = "https://www.projectinvictus.it/monofrequenza-o-multifrequenza/",
            publishedDate = "2026-02-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "I vantaggi della multifrequenza sono quelli di stimolare un muscolo almeno 2 volte a " +
                "settimana . ",
            articleTitle = "Monofrequenza o multifrequenza?",
            url = "https://www.projectinvictus.it/monofrequenza-o-multifrequenza/",
            publishedDate = "2026-02-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Ora, benché con frequenze di 3-4 volte a settimana si può riuscire a progredire " +
                "comunque, questo ci porterebbe a dover sostenere sedute più pesanti. ",
            articleTitle = "Quante volte a settimana andare in palestra?",
            url = "https://www.projectinvictus.it/quante-volte-andare-in-palestra/",
            publishedDate = "2026-02-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "In media tuttavia, non sono molte le persone che si allenano volentieri più di 3-4 volte " +
                "a settimana; sono davvero poche quelle che raggiungono e mantengono una frequenza di 5 " +
                "volta a settimana. ",
            articleTitle = "Quante volte a settimana andare in palestra?",
            url = "https://www.projectinvictus.it/quante-volte-andare-in-palestra/",
            publishedDate = "2026-02-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Allenali 2-3 volte a settimana Per sviluppare il petto devi allenarlo 2 o 3 volte a " +
                "settimana : questa frequenza ti permetterà di farlo crescere più rapidamente. ",
            articleTitle = "Pettorali scolpiti in poco tempo",
            url = "https://www.projectinvictus.it/pettorali-scolpiti/",
            publishedDate = "2025-10-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Non allenare il petto più di 3 volte a settimana perchè se riesci a farlo vuol dire che " +
                "non ti stai allenando abbastanza intensamente e che stai facendo volume a vuoto. ",
            articleTitle = "Pettorali scolpiti in poco tempo",
            url = "https://www.projectinvictus.it/pettorali-scolpiti/",
            publishedDate = "2025-10-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Data la natura dell’esercizio, è consigliato eseguirlo in frequenza media (2 volte a " +
                "settimana) alternando possibilmente tra le diverse varianti. ",
            articleTitle = "Step up: esecuzione, scheda e varianti",
            url = "https://www.projectinvictus.it/step-up-esecuzione-corretta/",
            publishedDate = "2025-09-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Come iniziare a camminare per dimagrire Se sei molto sedentario, anche per un’attività " +
                "semplice come la camminata è utile cominciare in modo graduale e progressivo , con pochi " +
                "chilometri a passo moderato 2-3 volte a settimana. ",
            articleTitle = "Camminare fa dimagrire?",
            url = "https://www.projectinvictus.it/camminare-fa-dimagrire/",
            publishedDate = "2025-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Chi si allena nel Calisthenics inizia ad stimolare i muscoli almeno 3 volte a settimana, " +
                "aumentando di volume, intensità e molto spesso la densità. ",
            articleTitle = "Quale fisico puoi ottenere con il Calisthenics",
            url = "https://www.projectinvictus.it/fisico-calisthenics/",
            publishedDate = "2025-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "I quadricipiti hanno una frequenza ideale di 1 fino a 3 volte a settimana. ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Nel caso invece in cui il gruppo sia carente puoi aumentare la frequenza fino a 3 volte " +
                "a settimana . ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Fare squat 3 volte a settimana in un protocollo tipico di bodybuilding può essere " +
                "efficace ma non sempre è la soluzione ideale. ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Una frequenza di allenamento ottimale si attesta sulle 1 o 2 volte a settimana , " +
                "lasciando spazio ad altri esercizi più complessi che devono avere il maggior focus in un " +
                "programma di allenamento come, ad esempio, la panca piana, il lento avanti o i dip alle " +
                "parallele. ",
            articleTitle = "Diamond push up (o Piegamenti a diamante): esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/piegamenti-a-diamante/",
            publishedDate = "2024-08-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Scheda Panca piana nella home gym Lacertosus La Panca piana è un esercizio da allenare " +
                "con costanza e con una buona frequenza, come 3 volte a settimana . ",
            articleTitle = "Allenamento panca piana nella home gym Lacertosus",
            url = "https://www.projectinvictus.it/allenamento-panca-piana-nella-home-gym-lacertosus/",
            publishedDate = "2024-05-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Ti proponiamo una scheda di allenamento con una frequenza di 3 volte a settimana e con " +
                "una progressione su 4 settimane. ",
            articleTitle = "Allenamento panca piana nella home gym Lacertosus",
            url = "https://www.projectinvictus.it/allenamento-panca-piana-nella-home-gym-lacertosus/",
            publishedDate = "2024-05-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "I vantaggi della multifrequenza sono quelli di stimolare un muscolo almeno 2 volte a " +
                "settimana , invece che una sola volta come succede se ti alleni in monofrequenza . ",
            articleTitle = "Allenamento in multifrequenza: 5 schede di allenamento",
            url = "https://www.projectinvictus.it/allenamento-in-multifrequenza/",
            publishedDate = "2024-04-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Programma corsa per sedentari (BMI > 26) Puoi allenarti 2 – 3 volte a settimana " +
                "ripetendo il programma. ",
            articleTitle = "Come iniziare a correre senza farsi male",
            url = "https://www.projectinvictus.it/come-iniziare-a-correre/",
            publishedDate = "2024-03-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Programma corsa per sedentari (BMI < 25) Puoi allenarti 2 – 3 volte a settimana " +
                "ripetendo il programma. ",
            articleTitle = "Come iniziare a correre senza farsi male",
            url = "https://www.projectinvictus.it/come-iniziare-a-correre/",
            publishedDate = "2024-03-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Programma corsa per sportivi che non corrono Puoi allenarti 3 volte a settimana facendo " +
                "1 allenamento A e due allenamenti B. ",
            articleTitle = "Come iniziare a correre senza farsi male",
            url = "https://www.projectinvictus.it/come-iniziare-a-correre/",
            publishedDate = "2024-03-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Il volume di lavoro sul petto può spaziare molto ( 12-20 serie settimanali) ed è " +
                "consigliato allenarli da 1 a 3 volte a settimana . ",
            articleTitle = "Esercizi pettorali alti per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/pettorali-alti/",
            publishedDate = "2024-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Naturalmente puoi decidere di ripartire tale volume in modo più produttivo spalmandolo " +
                "su una frequenza di allenamento settimanale superiore, ad esempio 18 serie totali a " +
                "settimana per i pettorali distribuite in 2 allenamenti da 9 serie l’una. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Farlo con una frequenza di 4-5 volte a settimana può rappresentare un’indicazione " +
                "mediamente valida. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Anche la diatriba tra monofrequenza (allenare uno stesso muscolo ogni 7 giorni) e " +
                "multifrequenza (allenarlo più volte nell’arco della settimana) non avrà mai fine in " +
                "quanto ci saranno sempre persone cresciute con un modo o l’altro. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Il consiglio per un atleta principiante è di allenarsi almeno 3 volte a settimana. ",
            articleTitle = "Street workout: cos’è, come iniziare e allenamento",
            url = "https://www.projectinvictus.it/street-workout/",
            publishedDate = "2023-03-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Il consiglio è quello di aumentare la frequenza di allenamento a 5 volte settimanali e " +
                "di trovare un modo per aggiungere dei sovraccarichi graduali nel tempo, ad esempio " +
                "acquistando un bilanciere e qualche disco. ",
            articleTitle = "Street workout: cos’è, come iniziare e allenamento",
            url = "https://www.projectinvictus.it/street-workout/",
            publishedDate = "2023-03-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Esempio 1 (livello principiante) Ti alleni 3 volte a settimana, sono un principiante (ho " +
                "iniziato da 1 mese) e voglio migliorare la mia composizione corporea . ",
            articleTitle = "Parametri allenanti: quali sono e come impostarli",
            url = "https://www.projectinvictus.it/parametri-allenanti/",
            publishedDate = "2022-10-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Esempio 2 (livello avanzato) Ti alleni 4 volte a settimana, sei un atleta avanzato (ti " +
                "alleni da anni) e vuoi migliorare il tuo fisico e anche le tue prestazioni. ",
            articleTitle = "Parametri allenanti: quali sono e come impostarli",
            url = "https://www.projectinvictus.it/parametri-allenanti/",
            publishedDate = "2022-10-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Assicurati di dormire almeno 7 ore per notte ed evita di bere alcolici se non in modo " +
                "moderato 1 – 2 volte a settimana . ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "L’ High Intensity Training ha le seguenti basi teoriche: Un esercizio condotto a " +
                "frequenza cardiaca elevata (80-90% della FCMAX) modifica la composizione corporea " +
                "(Brayner). ",
            articleTitle = "Tabata training: come funziona",
            url = "https://www.projectinvictus.it/tabata-training-come-funziona/",
            publishedDate = "2022-07-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Generalmente un Powerlifter si può allenare dalle 3 alle 5 volte a settimana . ",
            articleTitle = "Powerlifting: metodo di allenamento, esercizi e scheda",
            url = "https://www.projectinvictus.it/powerlifting-allenamento-scheda/",
            publishedDate = "2022-07-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Frequenza settimanale 2-4 volte in base anche al numero totale dei tuoi allenamenti " +
                "settimanali. ",
            articleTitle = "Glutei: anatomia, esercizi, allenamento",
            url = "https://www.projectinvictus.it/glutei/",
            publishedDate = "2022-05-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Anche il solo allenamento sembra promuovere la sintesi proteica nel post-workout fino a " +
                "due volte i livelli normali soprattutto a partire dalle 2 ore successive fino a " +
                "perdurare anche 48 ore. ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Idealmente, qualora sia contemplato all’interno nel tuo allenamento, una buona frequenza " +
                "potrebbe essere di svolgere questo esercizio 1-2 volte a settimana , come visto nel " +
                "paragrafo precedente. ",
            articleTitle = "Box Squat: come si esegue",
            url = "https://www.projectinvictus.it/box-squat/",
            publishedDate = "2021-04-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "REP RANGE : 10-12 Quante volte allenare le spalle a settimana Puoi partire con una " +
                "frequenza di allenamento di 2 volte settimanali , per poi muoverti se necessario verso " +
                "frequenze più spinte o conservative in base alle progressioni, ai restanti parametri " +
                "allenanti e di recupero soggettivi. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Se non hai una buona tecnica di movimento di eseguire questo esercizio almeno in due " +
                "sessioni settimanali per un minimo di 3 serie in ogni sessione . ",
            articleTitle = "Tirate al petto: come si eseguono",
            url = "https://www.projectinvictus.it/tirate-al-petto/",
            publishedDate = "2021-02-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Frequenza : il deltoide mediale recupera molto velocemente , partite da 2 sessioni " +
                "settimanali e vedete come vi trovate, possiamo arrivare ad una frequenza anche di 4-5 " +
                "volte settimanali in alcuni periodi e atleti. ",
            articleTitle = "Tirate al petto: come si eseguono",
            url = "https://www.projectinvictus.it/tirate-al-petto/",
            publishedDate = "2021-02-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Che tu sollevi pesanti bilancieri 5 volte a settimana, faccia chilometri in bici nei " +
                "weekend o vada semplicemente a camminare qualche chilometro al giorno ti fa e farà stare " +
                "bene. ",
            articleTitle = "Miochine: che cosa sono e che funzione svolgono?",
            url = "https://www.projectinvictus.it/miochine/",
            publishedDate = "2019-12-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Ovvero: 4 minuti di HIIT effettuati 4 volte a settimana hanno gli stessi benefici di un " +
                "training aerobico prolungato svolto 5 volte a settimana (non solo: i lavori scientifici " +
                "dimostravano che solo il “gruppo Tabata” otteneva un incremento della capacità " +
                "anaerobica). ",
            articleTitle = "HIIT: High Intensity Interval Training – Che cos’è? Esempi pratici",
            url = "https://www.projectinvictus.it/hiit-high-intensity-interval-training-aspetti-scientifici-e-pratici/",
            publishedDate = "2019-11-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Il protocollo Timmons ha dimostrato di poter indurre drammatici miglioramenti nella " +
                "sensibilità insulinica (incrementi del 24%) se effettuato 3 volte a settimana, per un " +
                "totale settimanale di soli 21 minuti! ",
            articleTitle = "HIIT: High Intensity Interval Training – Che cos’è? Esempi pratici",
            url = "https://www.projectinvictus.it/hiit-high-intensity-interval-training-aspetti-scientifici-e-pratici/",
            publishedDate = "2019-11-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Proprio a causa dell’ elevato stress imposto all’ organismo da una seduta di HIIT, " +
                "ritengo sia consigliabile non effettuarlo più di 3-4 volte a settimana (non più di 2-3 " +
                "volte a settimana se si svolge anche un’ altra attività). ",
            articleTitle = "HIIT: High Intensity Interval Training – Che cos’è? Esempi pratici",
            url = "https://www.projectinvictus.it/hiit-high-intensity-interval-training-aspetti-scientifici-e-pratici/",
            publishedDate = "2019-11-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Il recupero finisce quando la frequenza cardiaca scende al 60% della FCMAX 30 burpees Il " +
                "numero di ripetute aumenterà naturalmente ad ogni inizio di mesociclo in base al " +
                "risultato dello sprint test. ",
            articleTitle = "Spartan Ultra Race: come prepararla",
            url = "https://www.projectinvictus.it/spartan-ultra-race-preparazione/",
            publishedDate = "2019-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "I 65 giorni sono ipotetici, se decidiamo che 1-2 volte a settimana Maria potrà mangiare " +
                "di più ridurremmo il deficit settimanale ed aumenteremo il tempo stimato per raggiungere " +
                "l’obiettivo. ",
            articleTitle = "Dieta dimagrante: come farla in modo efficace",
            url = "https://www.projectinvictus.it/dieta-dimagrante/",
            publishedDate = "2018-10-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "In soldoni se faccio lavorare un buon atleta in buffer, per portarlo in overreaching sul " +
                "volume mi ritrovo a fargli fare sedute da 40-50 set allenanti 6 volte a settimana. ",
            articleTitle = "Cedimento Muscolare | Sempre e comunque utile?",
            url = "https://www.projectinvictus.it/cedimento-muscolare-sempre-e-comunque-utile/",
            publishedDate = "2018-03-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Si consiglia anche qui di eseguire 10 ripetizioni due volte al giorno. ",
            articleTitle = "Esercizi per la cervicale",
            url = "https://www.projectinvictus.it/esercizi-cervicale/",
            publishedDate = "2018-03-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Secondo la mia personale esperienza, un gruppo da specializzare andrebbe allenato almeno " +
                "3 o 4 volte a settimana per un periodo dalle 4 alle 8 settimana. ",
            articleTitle = "La periodizzazione nel bodybuilding",
            url = "https://www.projectinvictus.it/periodizzazione-nel-bodybuilding/",
            publishedDate = "2018-03-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Chi invece si allena in multifrequenza , stimolando lo stesso muscolo 2-3 volte nella " +
                "stessa settimana, più facilmente può sentire il bisogno di staccare la spina. ",
            articleTitle = "Programmare la fase di scarico",
            url = "https://www.projectinvictus.it/programmare-fase-scarico/",
            publishedDate = "2017-02-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Lo stesso muscolo allenato 4 volte a settimana (da scomunica per l’epoca) iniziava a " +
                "crescere. ",
            articleTitle = "Ipertrofia e forza: le logiche che li correlano",
            url = "https://www.projectinvictus.it/ipertrofia-forza/",
            publishedDate = "2016-12-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Inoltre, sempre a causa del lavoro, non riuscirà ad allenarsi più di 3 volte a " +
                "settimana. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "CONCLUSIONS: When comparing studies that investigated training muscle groups between 1 " +
                "to 3 days per week on a volume-equated basis, the current body of evidence indicates " +
                "that frequencies of training twice a week promote superior hypertrophic outcomes to once " +
                "a week. ",
            articleTitle = "Effects of Resistance Training Frequency on Measures of Muscle Hypertrophy: A Systematic Review and Meta-Analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/27102172/",
            publishedDate = "2016",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "POPULATION: healthy adults of any sex and age; Intervention: supervised concurrent " +
                "aerobic and strength training for at least 4 weeks; Comparison: identical strength " +
                "training prescription, with no aerobic training; Outcome: maximal strength, explosive " +
                "strength, and muscle hypertrophy. ",
            articleTitle = "Compatibility of Concurrent Aerobic and Strength Training for Skeletal Muscle Size and Function: An Updated Systematic Review and Meta-Analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/34757594/",
            publishedDate = "2022",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Frequenza ideale: 2 volte parte alta e 2 volte gambe. ",
            articleTitle = "Allenamento in multifrequenza: 5 schede di allenamento",
            url = "https://www.projectinvictus.it/allenamento-in-multifrequenza/",
            publishedDate = "2024-04-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Innanzitutto prendi in considerazione un esercizio che verrà ripetuto per più volte " +
                "durante la settimana ( lavoro in multifrequenza ) e crea una modulazione su almeno 2 " +
                "giorni che preveda l’utilizzo di questo esercizio su più rep range . ",
            articleTitle = "Come programmare l’allenamento",
            url = "https://www.projectinvictus.it/programmazione-allenamento-palestra/",
            publishedDate = "2026-09-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Per questo conviene allenarsi 3 – 4 volte a settimana , anche inserendo allenamenti " +
                "metabolici o cardio o HIIT 1-3 volte / settimana, facendo attenzione a non fare troppo " +
                "per non rischiare il sovrallenamento. ",
            articleTitle = "Definizione in palestra: come definire i muscoli",
            url = "https://www.projectinvictus.it/definizione-palestra/",
            publishedDate = "2024-01-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "La frequenza di allenamento va da 5 – 7 giorni a settimana per Arthur Jones a 3 per " +
                "Mentzer. ",
            articleTitle = "Heavy Duty: principi e scheda di allenamento",
            url = "https://www.projectinvictus.it/heavy-duty-allenamento/",
            publishedDate = "2022-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Puoi allenare le spalle direttamente da 1-4 volte a settimana. ",
            articleTitle = "I migliori esercizi per le spalle",
            url = "https://www.projectinvictus.it/esercizi-spalle-la-guida-completa/",
            publishedDate = "2025-01-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "La frequenza di allenamento dello stesso gruppo muscolare è ogni 5 giorni circa. ",
            articleTitle = "Il metodo Hatfield",
            url = "https://www.projectinvictus.it/metodo-hatfield-ipertrofia/",
            publishedDate = "2025-05-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Eseguire gli esercizi seguenti 2-3 volte a settimana a fine allenamento, è il minimo " +
                "sindacale per stimolare il retto dell’addome e gli obliq ui. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Allenamento 1 Allenamento da ripetere almeno 3 volte a settimana. ",
            articleTitle = "Calf Raise: esercizi efficaci per i polpacci e scheda di allenamento",
            url = "https://www.projectinvictus.it/calf-raise/",
            publishedDate = "2026-07-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "Assolutamente consigliato da fare, almeno 3-7 volte a settimana per almeno 10′ . ",
            articleTitle = "I migliori esercizi fitness",
            url = "https://www.projectinvictus.it/esercizi-fitness/",
            publishedDate = "2024-07-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FREQUENCY,
            statement =
                "La frequenza settimanale ottimale è su 4-6 giorni. ",
            articleTitle = "Impariamo a programmare alcuni allenamenti da palestra (1xMax)",
            url = "https://www.projectinvictus.it/impariamo-a-programmare-part-1-1xmax/",
            publishedDate = "2013-10-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXERCISE_SELECT,
            statement =
                "Esercizi complementari glutei uomo I complementari servono per andare a completare il " +
                "lavoro iniziato coi fondamentali e i principali sono: affondi step up Di solito si " +
                "eseguono 2-3 serie con un range di ripetizioni tra 8-12. ",
            articleTitle = "Glutei uomini: esercizi per allenarli",
            url = "https://www.projectinvictus.it/esercizi-glutei-uomo/",
            publishedDate = "2025-04-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXERCISE_SELECT,
            statement =
                "Metteremo per primi gli esercizi fondamentali da allenare per 3-4 serie su 6-10 " +
                "ripetizioni, e per secondi gli esercizi complementari da eseguire su alte ripetizioni " +
                "(anche a tempo o a circuito) per completare il lavoro metabolico sul distretto muscolare " +
                "scelto. ",
            articleTitle = "Tonificazione muscolare: come si fa ed esercizi",
            url = "https://www.projectinvictus.it/tonificazione/",
            publishedDate = "2024-04-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXERCISE_SELECT,
            statement =
                "I complementari che puoi abbinare sono: varianti curl (panca inclinata, spider curl) " +
                "push down (se sei in palestra) estensioni tricipiti tricipiti sopra la testa Fai un " +
                "altro esercizio per i bicipiti 2-3 serie 12-15 ripetizioni ed uno uguale per i " +
                "tricipiti. ",
            articleTitle = "Tonificazione muscolare: come si fa ed esercizi",
            url = "https://www.projectinvictus.it/tonificazione/",
            publishedDate = "2024-04-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXERCISE_SELECT,
            statement =
                "Ciascun allenamento è composto da: Due esercizi fondamentali in modalità “mantenimento " +
                "forza” a volume crescente (da 6 a 9 serie nel corso delle 4 settimane) con il 60-65% del " +
                "massimale. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Durante la fase di ricomposizione conviene tenere un volume leggermente inferiore " +
                "intorno alle 12 – 20 serie a settimana . ",
            articleTitle = "Ricomposizione corporea: la guida completa",
            url = "https://www.projectinvictus.it/ricomposizione-corporea/",
            publishedDate = "2026-08-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Chi si allena nel Calisthenics inizia ad stimolare i muscoli almeno 3 volte a settimana, " +
                "aumentando di volume, intensità e molto spesso la densità. ",
            articleTitle = "Quale fisico puoi ottenere con il Calisthenics",
            url = "https://www.projectinvictus.it/fisico-calisthenics/",
            publishedDate = "2025-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Consiglio di inserire 3-5 serie allenanti di Pulley basso a settimana , progredendo in " +
                "un primo momento sul carico, nel rispetto delle tecnica di esecuzione, per poi virare " +
                "l’attenzione sul volume. ",
            articleTitle = "Pulley basso: tutorial ed esecuzione corretta",
            url = "https://www.projectinvictus.it/pulley-basso/",
            publishedDate = "2025-04-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Nel caso invece essi rappresentino un punto carente, puoi arrivare sulle 20/22 serie " +
                "settimanali . ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Per quanto riguarda il numero di serie, consiglio di stare tra le 3-4, in base al volume " +
                "della piramide: se la piramide completa è circa il tuo massimale di ripetizioni, allora " +
                "puoi stare sulle 4 serie totali, se è maggiore di questo, consiglio di non superare le " +
                "3. ",
            articleTitle = "Calisthenic Workout: schede e programmi per il calisthenics",
            url = "https://www.projectinvictus.it/calisthenic-workout-programmi/",
            publishedDate = "2024-10-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Il volume di lavoro sul petto può spaziare molto ( 12-20 serie settimanali) ed è " +
                "consigliato allenarli da 1 a 3 volte a settimana . ",
            articleTitle = "Esercizi pettorali alti per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/pettorali-alti/",
            publishedDate = "2024-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Una buona indicazione generale potrebbe essere quella di svolgere, in termini di volume " +
                "settimanale per distretto muscolare, nel corso del macrociclo annuale: 50-70% del lavoro " +
                "nel range che va dalle 6 alle 12 ripetizioni. ",
            articleTitle = "Come aumentare la massa muscolare?",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare/",
            publishedDate = "2023-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Naturalmente puoi decidere di ripartire tale volume in modo più produttivo spalmandolo " +
                "su una frequenza di allenamento settimanale superiore, ad esempio 18 serie totali a " +
                "settimana per i pettorali distribuite in 2 allenamenti da 9 serie l’una. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "In linea generale, allenamenti della durata di 90-120 minuti sono un buon compromesso " +
                "che permette di generare un quantitativo di lavoro adeguato senza perdere " +
                "concentrazione, forza e rischiare di generare volume a vuoto. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Naturalmente, aumenta gradualmente un parametro alla volta e non tutti insieme, ad " +
                "esempio partendo da 80 kg x 8 x 3 potresti prima aumentare il volume, ovvero le " +
                "ripetizioni a parità di serie o numero di serie totali. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Se non hai mai allenato i polpacci parti da 8-9 serie settimanali distribuite in 2 " +
                "sessioni allenanti , suddiviso metà su movimenti a gamba distesa e metà a ginocchio " +
                "flesso e vedi come rispondi. ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Datti un tempo di almeno 4-5 settimane , se recuperi e i parametri allenanti migliorano, " +
                "così come la circonferenza dei tuoi polpacci, potrai scegliere se aumentare il numero di " +
                "serie o rimanere in questo range fino ai primi stalli. ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Possiamo utilizzare un range di intensità che va dalle 8 alle 30 ripetizioni , facendo " +
                "però delle considerazioni: Il range che va dalle 8 alle 20 ripetizioni è in genere il " +
                "più utilizzato perchè permette un buon rapporto tra volume , intensità di carico e " +
                "fatica accumulata. ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Nel Giorno 2 svolgerai andrai ad accumulare volume nelle settimane fino ad arrivare in " +
                "Settimana 3 ad eseguire 5 serie da 10 ripetizioni (50 ripetizioni in totale). ",
            articleTitle = "Pendlay row: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/pendlay-row/",
            publishedDate = "2023-02-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Decidi così di aumentare il volume su questo gruppo muscolare ma ti ritrovi a stare 3 " +
                "ore in palestra. ",
            articleTitle = "Parametri allenanti: quali sono e come impostarli",
            url = "https://www.projectinvictus.it/parametri-allenanti/",
            publishedDate = "2022-10-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Procedi nelle prime 3 settimane con un aumento del volume di allenamento, aumentando le " +
                "serie di 1 ogni settimana, arrivando quindi ad eseguire 6 serie su tutti gli esercizi. ",
            articleTitle = "Come ingrossare le gambe",
            url = "https://www.projectinvictus.it/come-ingrossare-le-gambe/",
            publishedDate = "2022-09-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Il volume allenante per le braccia dovrà salire e saremo intorno alle 15-20 serie " +
                "allenanti per bicipiti ed altrettante per i tricipiti. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Le braccia rispondono molto bene al volume allenante : appena inizi a metterli come " +
                "gruppo muscolare primario e a fare almeno 15-20 serie allenanti , vedrai già nel giro di " +
                "4 settimane dei risultati (quantomeno aumenterà la ritenzione sulle braccia). ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Allenali un minimo di 2 volte a settimana: la scarsa richiesta neurale ti permette di " +
                "sopportare molto volume settimanale. ",
            articleTitle = "Flessioni per allenare i pettorali",
            url = "https://www.projectinvictus.it/flessioni-pettorali/",
            publishedDate = "2022-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Se vuoi lavorare più sulla forza prediligi un volume basso e un carico intenso (> 85% di " +
                "1RM) con un recupero alto (>180”) . ",
            articleTitle = "Pettorali nella donna per tonificare il seno",
            url = "https://www.projectinvictus.it/esercizi-pettorali-donne-aumentare-seno/",
            publishedDate = "2021-03-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Serie : in linea generale, considerando questo volume come esclusivo per il deltoide " +
                "mediale, si consiglia di iniziare con 10-12 serie allenanti , da cui poi muoverti verso " +
                "un loro aumento o diminuzione in base alle progressioni e ai parametri di recupero " +
                "soggettivi. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Per quanto riguarda il numero di serie, consiglio di stare tra 3 – 4, in base al volume " +
                "della piramide: se la piramide completa è circa il nostro massimale di ripetizioni, " +
                "allora possiamo stare sulle 4 serie totali, se è maggiore di questo, consiglio di non " +
                "superare le 3. ",
            articleTitle = "Calisthenic Workout: schede e programmi per il calisthenics",
            url = "https://www.projectinvictus.it/calisthenic-workout-schede-e-programmi-per-il-calisthenics/",
            publishedDate = "2020-01-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Anche il volume plasmatico subisce un aumento del 12-20% dovuto ad un aumento " +
                "dell’albumina, tale volume ritorna al valore pre allenamento entro una settimana " +
                "dall’ultima sessione allenante. ",
            articleTitle = "Allenamento aerobico",
            url = "https://www.projectinvictus.it/allenamento-aerobico/",
            publishedDate = "2018-08-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Sia sedute di HIIT (vedi il relativo articolo), sia serie lattacide sopra le 15-20 " +
                "ripetizioni coi pesi (bilancieri, kettlebell , manubri, ecc), aumentano la " +
                "capilarizzazione muscolare, la densità mitocondriale, il volume plasmatico. ",
            articleTitle = "Allenamento aerobico",
            url = "https://www.projectinvictus.it/allenamento-aerobico/",
            publishedDate = "2018-08-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Su un volume cosi alto in genere è consigliato di almeno 2 minuti ma poi può variare in " +
                "base all’intensita di lavoro scelta. ",
            articleTitle = "Metodo Set x Rep + 1xmax nel calisthenics",
            url = "https://www.projectinvictus.it/programma-set-x-rep-1xmax-calisthenics/",
            publishedDate = "2018-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "In soldoni se faccio lavorare un buon atleta in buffer, per portarlo in overreaching sul " +
                "volume mi ritrovo a fargli fare sedute da 40-50 set allenanti 6 volte a settimana. ",
            articleTitle = "Cedimento Muscolare | Sempre e comunque utile?",
            url = "https://www.projectinvictus.it/cedimento-muscolare-sempre-e-comunque-utile/",
            publishedDate = "2018-03-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Un buon approccio potrebbe essere di aumentare gradatamente carico e volume per 1-2 " +
                "settimane, a seconda di quanto siete stati fermi. ",
            articleTitle = "Riprendere ad allenarsi dopo una pausa, come fare",
            url = "https://www.projectinvictus.it/riprendere-allenarsi/",
            publishedDate = "2017-09-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Partiamo con un paio di allenamenti con il 50% del volume e il 70% dei carichi " +
                "utilizzati nell’ultima scheda. ",
            articleTitle = "Riprendere ad allenarsi dopo una pausa, come fare",
            url = "https://www.projectinvictus.it/riprendere-allenarsi/",
            publishedDate = "2017-09-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "A partire da questo peso calerò di una certa percentuale (che varierà nel corso delle 8 " +
                "settimane) per andare ad accumulare volume con un gesto tecnico superiore fin tanto che " +
                "non raggiungo nuovamente l’RPE target della giornata. ",
            articleTitle = "L’autoregolazione nell’allenamento della forza",
            url = "https://www.projectinvictus.it/autoregolazione-allenamento-della-forza/",
            publishedDate = "2017-01-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "The posterior probability of the marginal slope exceeding zero for the effect of volume " +
                "on both hypertrophy and strength was 100%, indicating that gains in muscle size and " +
                "strength increase as volume increases. ",
            articleTitle = "The Resistance Training Dose Response: Meta-Regressions Exploring the Effects of Weekly Volume and Frequency on Muscle Hypertrophy and Strength Gains",
            url = "https://pubmed.ncbi.nlm.nih.gov/41343037/",
            publishedDate = "2026",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "The posterior probability of the marginal slope exceeding zero for frequency's effect on " +
                "hypertrophy was less than 100%, indicating compatibility with negligible effects. ",
            articleTitle = "The Resistance Training Dose Response: Meta-Regressions Exploring the Effects of Weekly Volume and Frequency on Muscle Hypertrophy and Strength Gains",
            url = "https://pubmed.ncbi.nlm.nih.gov/41343037/",
            publishedDate = "2026",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "In contrast, the posterior probability for strength was 100%, suggesting strength gains " +
                "increase with increasing frequency, albeit with diminishing returns. ",
            articleTitle = "The Resistance Training Dose Response: Meta-Regressions Exploring the Effects of Weekly Volume and Frequency on Muscle Hypertrophy and Strength Gains",
            url = "https://pubmed.ncbi.nlm.nih.gov/41343037/",
            publishedDate = "2026",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Full-body RT elicited greater losses compared to split-body in whole-body fat mass " +
                "(-0.775 ± 1.120 kg vs. ",
            articleTitle = "Full-body resistance training promotes greater fat mass loss than a split-body routine in well-trained males: A randomized trial",
            url = "https://pubmed.ncbi.nlm.nih.gov/38874955/",
            publishedDate = "2024",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "This study investigated the effects of increasing previous resistance training (RT) " +
                "weekly set volume by 30% (G30) and 60% (G60) on muscle hypertrophy and strength. ",
            articleTitle = "Training volume increases or maintenance based on previous volume: the effects on muscular adaptations in trained males",
            url = "https://pubmed.ncbi.nlm.nih.gov/39665246/",
            publishedDate = "2025",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "We assessed pre- and poststudy thigh region-of-interest fat-free mass (ROI-FFM), " +
                "anterior thigh muscle thickness (MT) at two sites: proximal (PMT) and distal (DMT) and " +
                "their sum (ΣMT), one-repetition maximum (1RM), and strength-endurance via repetitions to " +
                "failure (RTF) at 70% of 1RM. ",
            articleTitle = "Training volume increases or maintenance based on previous volume: the effects on muscular adaptations in trained males",
            url = "https://pubmed.ncbi.nlm.nih.gov/39665246/",
            publishedDate = "2025",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Each additional set was associated with an increase in effect size (ES) of 0.023 " +
                "corresponding to an increase in the percentage gain by 0.37%. ",
            articleTitle = "Dose-response relationship between weekly resistance training volume and increases in muscle mass: A systematic review and meta-analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/27433992/",
            publishedDate = "2017",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Eighty-five older individuals [41M/44F, age = 68 ± 4 yr; body mass index (BMI) = 26.4 ± " +
                "3.7 kg/m2] had one leg randomly allocated to a single (1)-set and the contralateral leg " +
                "allocated to four sets of unilateral knee-extension RT at 8-15 repetition maximum (RM) " +
                "for 10-wk 2 days/wk. ",
            articleTitle = "Higher resistance training volume offsets muscle hypertrophy nonresponsiveness in older individuals",
            url = "https://pubmed.ncbi.nlm.nih.gov/38174375/",
            publishedDate = "2024",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Passai dunque ad una media di 3050kcal circa, introducendo allo stesso tempo a livello " +
                "di training, lavori più lattacidi, aumentando quindi il volume totale settimanale in " +
                "concomitanza di un abbassamento calorico . ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Se vuoi dare enfasi al pettorale puoi aggiungere un esercizio e/o aumentare i set fino " +
                "ad una massimo di 25 serie a settimana. ",
            articleTitle = "Scheda pettorali in palestra",
            url = "https://www.projectinvictus.it/scheda-pettorali/",
            publishedDate = "2026-07-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Nella singola seduta puoi eseguire almeno 3-4 serie di questo esercizio. ",
            articleTitle = "Alzate laterali: esecuzione corretta, varianti e muscoli coinvolti",
            url = "https://www.projectinvictus.it/alzate-laterali/",
            publishedDate = "2025-04-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "PANCA: 5 serie da 5 ripetizioni Peso usato la settimana prima. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "MILITARY PRESS: 5 serie da 5 ripetizioni Peso usato la settimana prima. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "PANCA: serie da 5 ripetizioni Peso usato la settimana prima cercando di rendere molto " +
                "esplosivo ogni colpo. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "Peso usato la settimana prima + 10% MILITARY PRESS: 5 serie da 5 ripetizioni Peso usato " +
                "la settimana prima cercando di rendere molto esplosivo ogni colpo. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "PANCA: 5 serie da 4 ripetizioni Peso usato la settimana prima. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "MILITARY PRESS: 5 serie da 4 ripetizioni Peso usato la settimana prima. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.VOLUME,
            statement =
                "In questo test di 8 settimane ho strutturato l’allenamento in 2 blocchi da 3 settimane " +
                "seguiti da una settimana di Deload e test (ho già eseguito una prima settimana di test " +
                "degli RPE a volume limitato, una sorta di Acclimation). ",
            articleTitle = "L’autoregolazione nell’allenamento della forza",
            url = "https://www.projectinvictus.it/autoregolazione-allenamento-della-forza/",
            publishedDate = "2017-01-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Una serie allenante deve avere minimo un RPE di 7,5 (3 ripetizioni di margine dal " +
                "cedimento). ",
            articleTitle = "RPE: cos’è, misura, utilizzo",
            url = "https://www.projectinvictus.it/rpe-palestra/",
            publishedDate = "2026-09-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Solitamente come attività aerobica per consumare grassi viene consigliata quella a bassa " +
                "intensità, ma non è una proposta valida se sei già ben allenato e sportivo: in questo " +
                "caso, meglio preferire esercizi ad alta intensità di una certa durata (30-60 minuti) . ",
            articleTitle = "Il migliore allenamento per dimagrire",
            url = "https://www.projectinvictus.it/allenamento-per-dimagrire/",
            publishedDate = "2026-05-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Chi si allena nel Calisthenics inizia ad stimolare i muscoli almeno 3 volte a settimana, " +
                "aumentando di volume, intensità e molto spesso la densità. ",
            articleTitle = "Quale fisico puoi ottenere con il Calisthenics",
            url = "https://www.projectinvictus.it/fisico-calisthenics/",
            publishedDate = "2025-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Il numero di ripetizioni viene deciso in base all’intensità , che si aggira tra il 60 e " +
                "il 75% del massimale . ",
            articleTitle = "Allenamento PHA: cos’è, benefici, come impostarlo",
            url = "https://www.projectinvictus.it/allenamento-pha/",
            publishedDate = "2025-03-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Il carico che utilizzi sarà una conseguenza del numero di ripetizioni eseguito e " +
                "dell’intensità di sforzo, tempo sotto tensione scelto, range di movimento ecc… La pressa " +
                "si presta bene ai più svariati lavori, indicativamente dalle 5-6 ripetizioni fino alle " +
                "30 ripetizioni . ",
            articleTitle = "Leg press in palestra e nel bodybuilding: esecuzione corretta",
            url = "https://www.projectinvictus.it/leg-press-bodybuilding/",
            publishedDate = "2024-06-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "L’ intensità in genere adatta a questo tipo di esercizio è medio-elevata ( 75-85% ), " +
                "facendo dalle 4 alle 10 ripetizioni per “n” serie. ",
            articleTitle = "Stacchi rumeni: esecuzione, tecnica corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/stacchi-rumeni/",
            publishedDate = "2023-11-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Con carichi pesanti o in esercizi multiarticolari portati a cedimento tempi di 3-5 " +
                "minuti consentono di mantenere livelli di forza e concentrazione adeguati generando così " +
                "più lavoro di qualità. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Possiamo utilizzare un range di intensità che va dalle 8 alle 30 ripetizioni , facendo " +
                "però delle considerazioni: Il range che va dalle 8 alle 20 ripetizioni è in genere il " +
                "più utilizzato perchè permette un buon rapporto tra volume , intensità di carico e " +
                "fatica accumulata. ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Scheda di allenamento stacchi a gambe tese In base alla variante scelta puoi utilizzare " +
                "uno spettro di intensità davvero ampio ( 6 – 15 ripetizioni ). ",
            articleTitle = "Stacchi a gambe tese per glutei di marmo",
            url = "https://www.projectinvictus.it/stacchi-gambe-tese/",
            publishedDate = "2023-01-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Nel Giorno 2 andrai ad inserire una tecnica di intensità, lavorando prima con un singolo " +
                "drop set del 20% ovvero alla fine delle prime 16 ripetizioni, calerai il carico del 20% " +
                "e continuerai ad eseguire gli affondi fino al cedimento. ",
            articleTitle = "Affondi: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/affondi/",
            publishedDate = "2023-01-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Noradrenalina La noradrenalina (NA) viene secreta in risposta a stimoli di intensità " +
                "maggiore come situazioni di grave pericolo, traumi fisici, shock ed esercizi fisici al " +
                "70-75% del VO2 max . ",
            articleTitle = "Catecolamine: sintesi, funzione, come abbassarle",
            url = "https://www.projectinvictus.it/catecolamine/",
            publishedDate = "2022-09-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Il Protocollo TDR prevedeva carichi del 70%rm sollevati per 4 serie da 8-12 ripetizioni " +
                "di distensioni su panca piana, leg press,military press, lat machine, curl per i " +
                "bicipiti brachiali, estensioni tricipiti brachiali, leg curl e sit up (a cedimento) con " +
                "un minuto di recupero. ",
            articleTitle = "EPOC: cos’è e come aumentarlo",
            url = "https://www.projectinvictus.it/epoc-calorie-metabolismo-studi/",
            publishedDate = "2022-08-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Inoltre, i vari cicli sono stati ben tollerati dai soggetti ed hanno comportato un " +
                "aumento della spesa calorica rispetto a quello che normalmente è stato rilevato col " +
                "completamento di un normale protocollo da 4 minuti (questo ci fa intuire che il mix tra " +
                "intensità e volume è forse il metodo ideale). ",
            articleTitle = "Tabata training: come funziona",
            url = "https://www.projectinvictus.it/tabata-training-come-funziona/",
            publishedDate = "2022-07-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Negli studi le partecipanti seguivano questo protocollo per 3 volte a settimana e " +
                "l’intensità era compresa tra il 90 e il 95% della FC max. ",
            articleTitle = "Sindrome da ovaio policistico: dieta ed allenamento",
            url = "https://www.projectinvictus.it/sindrome-ovaio-policistico-dieta-allenamento/",
            publishedDate = "2020-08-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Su un volume cosi alto in genere è consigliato di almeno 2 minuti ma poi può variare in " +
                "base all’intensita di lavoro scelta. ",
            articleTitle = "Metodo Set x Rep + 1xmax nel calisthenics",
            url = "https://www.projectinvictus.it/programma-set-x-rep-1xmax-calisthenics/",
            publishedDate = "2018-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "2 Serie a cedimento: tutte le serie vanno portate a cedimento tecnico nel range di " +
                "ripetizioni prefissato. ",
            articleTitle = "La periodizzazione nel bodybuilding",
            url = "https://www.projectinvictus.it/periodizzazione-nel-bodybuilding/",
            publishedDate = "2018-03-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Nell’ultimo set si inserisce un dropset: si scala il peso del 20% e si prosegue fino a " +
                "cedimento. ",
            articleTitle = "La periodizzazione nel bodybuilding",
            url = "https://www.projectinvictus.it/periodizzazione-nel-bodybuilding/",
            publishedDate = "2018-03-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "A partire da questo peso calerò di una certa percentuale (che varierà nel corso delle 8 " +
                "settimane) per andare ad accumulare volume con un gesto tecnico superiore fin tanto che " +
                "non raggiungo nuovamente l’RPE target della giornata. ",
            articleTitle = "L’autoregolazione nell’allenamento della forza",
            url = "https://www.projectinvictus.it/autoregolazione-allenamento-della-forza/",
            publishedDate = "2017-01-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Il pre-riscaldamento , la “fase 0” di questo processo, dovrebbe durare intorno ai 5 " +
                "minuti, nei quali si eseguirà un esercizio aerobico a scelta, dipendentemente dalla " +
                "disponibilità di attrezzature e spazio, a intensità moderata (è pur sempre " +
                "riscaldamento!), cui seguirà il riscaldamento specifico. ",
            articleTitle = "Come effettuare il riscaldamento per l’allenamento?",
            url = "https://www.projectinvictus.it/come-effettuare-il-riscaldamento/",
            publishedDate = "2016-05-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Quindi parlando come al bar, fare un circuito funzionale con la Mask per 10-15-20 minuti " +
                "non solo non vi farà ottenere gli adattamenti fisiologici ricercati e tipici del vero " +
                "allenamento in quota, ma vi farà ridurre anche l’intensità dello sforzo perché per forza " +
                "di cose avrete meno fiato. ",
            articleTitle = "Elevation Training Mask: serve o non serve?",
            url = "https://www.projectinvictus.it/elevation-training-mask-serve-o-non-serve/",
            publishedDate = "2015-03-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Dovrebbe essere ovvio che l’esecuzione di 2 ripetizioni al 85% del massimle e 5 " +
                "ripetizioni al 85% del massimale consistono in un livello di sforzo/difficoltà molto " +
                "diverso, anche se l’intensità del carico è identica. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 3)",
            url = "https://www.projectinvictus.it/chiarimenti-incomprensioni-sullintensita-nellesercizio-i-pesi-parte-3/",
            publishedDate = "2014-10-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Tipicamente, un carico del 30-60% del massimale (bassa intensità di carico) può essere " +
                "sollevato per un numero di ripetizioni molto sub-massimale. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 3)",
            url = "https://www.projectinvictus.it/chiarimenti-incomprensioni-sullintensita-nellesercizio-i-pesi-parte-3/",
            publishedDate = "2014-10-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Si può calcolare moltiplicando le serie per le ripetizioni allenanti (ovvero con un " +
                "buffer massimo di 3, se il nostro cedimento arriva a 10 ripetizioni, contano solo le " +
                "serie dove arriviamo almeno a 7). ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            publishedDate = "2022-12-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Se le serie vengono portate al cedimento o quasi, con un buffer massimo di 1 – 3 " +
                "ripetizioni, sei sicuro di portarti a casa uno dei parametri allenanti più importanti. ",
            articleTitle = "Ricomposizione corporea: la guida completa",
            url = "https://www.projectinvictus.it/ricomposizione-corporea/",
            publishedDate = "2026-08-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Nella sua versione originale prevedeva l’applicazione di carichi del 90% o superiori, " +
                "coi quali venivano eseguite una serie di singole intervallate da circa 10 secondi di " +
                "pausa fino ad arrivare a cedimento muscolare. ",
            articleTitle = "Tecniche intensità nel bodybuilding",
            url = "https://www.projectinvictus.it/tecniche-intensita-bodybuilding/",
            publishedDate = "2025-08-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Sembra che le intensità minime per provocare questo adattamento corrispondano a circa il " +
                "30% 1-RM, e che a bassissime intensità si necessiti di portare lo sforzo alla massima " +
                "fatica (cedimento), ma sono necessarie ulteriori ricerche per poter trarre conclusioni " +
                "più esaustive[21]. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 1)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-1/",
            publishedDate = "2014-06-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.INTENSITY,
            statement =
                "Fai massimo 6-8 ripetizioni senza accumulare acido lattico, lavorando a buffer (con " +
                "margine). ",
            articleTitle = "Esercizi per il riscaldamento: perchè e quali fare",
            url = "https://www.projectinvictus.it/esercizi-di-riscaldamento/",
            publishedDate = "2024-06-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "La panca piana con manubri è più indicata per esecuzioni che prevedano rep range " +
                "maggiori (8-12 ripetizioni) con carichi più bassi. ",
            articleTitle = "Panca per allenare pettorali",
            url = "https://www.projectinvictus.it/panca-pettorali/",
            publishedDate = "2026-08-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Fai 3-4 serie x 12-15 ripetizioni lente e controllate 2. ",
            articleTitle = "I 10 migliori esercizi per gli addominali",
            url = "https://www.projectinvictus.it/esercizi-addominali/",
            publishedDate = "2026-06-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Una volta raggiunte le 30-40 ripetizioni , un buon metodo per aumentare la difficoltà " +
                "allenante è quello di utilizzare delle zavorre . ",
            articleTitle = "Piegamenti sulle braccia: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/piegamenti-sulle-braccia-esecuzione-e-muscoli-coinvolti/",
            publishedDate = "2026-06-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Dopo aver raggiunto le 30-40 ripetizioni di fila, aggiungi una zavorra per aumentare la " +
                "difficoltà dell’esercizio. ",
            articleTitle = "Piegamenti sulle braccia: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/piegamenti-sulle-braccia-esecuzione-e-muscoli-coinvolti/",
            publishedDate = "2026-06-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Ad esempio, se hai un massimale di 30 ripetizioni negli allenamenti puoi fare 10-15 " +
                "ripetizioni per più serie oppure meno ripetizioni con varianti più intense (fermi al " +
                "petto, isocinetiche, discese o salite lente ecc..). ",
            articleTitle = "Piegamenti sulle braccia: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/piegamenti-sulle-braccia-esecuzione-e-muscoli-coinvolti/",
            publishedDate = "2026-06-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Crunch Per il Crunch fai 3-4 serie per 12-15 ripetizioni 3. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Crunch inverso Per allenare il Crunch inverso fai 2-3 serie per 8-12 ripetizioni 4. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Leg raise Per il Leg raise , fai 3-4 serie per 6-10 ripetizioni. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Ab wheel Per l’ AB Wheel esegui 2-3 serie per 6-10 ripetizioni In generale, non esiste " +
                "un esercizio completo per gli addominali, variare e stimolarli 2-3 volte a settimana è " +
                "la cosa migliore da fare. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Sia la versione base di questi esercizi, sia le varianti vanno benissimo, basta caricare " +
                "pesi importanti e fare dalle 4-6 serie per 4-8 ripetizioni. ",
            articleTitle = "Glutei uomini: esercizi per allenarli",
            url = "https://www.projectinvictus.it/esercizi-glutei-uomo/",
            publishedDate = "2025-04-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Esercizi complementari glutei uomo I complementari servono per andare a completare il " +
                "lavoro iniziato coi fondamentali e i principali sono: affondi step up Di solito si " +
                "eseguono 2-3 serie con un range di ripetizioni tra 8-12. ",
            articleTitle = "Glutei uomini: esercizi per allenarli",
            url = "https://www.projectinvictus.it/esercizi-glutei-uomo/",
            publishedDate = "2025-04-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Si eseguono 0-2 serie ad alte o altissime ripetizioni (più di 12 – 15 ripetizioni). ",
            articleTitle = "Glutei uomini: esercizi per allenarli",
            url = "https://www.projectinvictus.it/esercizi-glutei-uomo/",
            publishedDate = "2025-04-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Come allenare il Pulley basso: ripetizioni, serie, carichi e progressioni Sicuramente il " +
                "Pulley risulta un esercizio che ben si presta a lavori a range di ripetizioni medio-alti " +
                ", il cosiddetto “range ipertrofico” sulle 8-15 ripetizioni. ",
            articleTitle = "Pulley basso: tutorial ed esecuzione corretta",
            url = "https://www.projectinvictus.it/pulley-basso/",
            publishedDate = "2025-04-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Metteremo per primi gli esercizi fondamentali da allenare per 3-4 serie su 6-10 " +
                "ripetizioni, e per secondi gli esercizi complementari da eseguire su alte ripetizioni " +
                "(anche a tempo o a circuito) per completare il lavoro metabolico sul distretto muscolare " +
                "scelto. ",
            articleTitle = "Tonificazione muscolare: come si fa ed esercizi",
            url = "https://www.projectinvictus.it/tonificazione/",
            publishedDate = "2024-04-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Gli esercizi principali per le braccia sono: curl manubri /bilanciere EZ french press " +
                "(piegamenti sulle ginocchia braccia strette se ti alleni a casa) Anche qui per ogni " +
                "esercizio fai 3-4 serie per 8-10 ripetizioni. ",
            articleTitle = "Tonificazione muscolare: come si fa ed esercizi",
            url = "https://www.projectinvictus.it/tonificazione/",
            publishedDate = "2024-04-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "I complementari che puoi abbinare sono: varianti curl (panca inclinata, spider curl) " +
                "push down (se sei in palestra) estensioni tricipiti tricipiti sopra la testa Fai un " +
                "altro esercizio per i bicipiti 2-3 serie 12-15 ripetizioni ed uno uguale per i " +
                "tricipiti. ",
            articleTitle = "Tonificazione muscolare: come si fa ed esercizi",
            url = "https://www.projectinvictus.it/tonificazione/",
            publishedDate = "2024-04-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Per gli addominali ecco alcuni esercizi che puoi fare a tempo (stile Tabata ) o con " +
                "serie da 15-20 ripetizioni: crunch , crunch inverso plank , plank laterale barchetta " +
                "addominali Per approfondire guarda questo nostro articolo sui 10 migliori esercizi per " +
                "gli addominali . ",
            articleTitle = "Tonificazione muscolare: come si fa ed esercizi",
            url = "https://www.projectinvictus.it/tonificazione/",
            publishedDate = "2024-04-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Il carico utilizzabile è limitato rispetto alle precedenti varianti, qui è sconsigliato " +
                "lavorare a rep range minori di 8 ripetizioni, ovviamente però va valutato anche con " +
                "quale tempo sotto tensione decidi di eseguire ogni ripetizione. ",
            articleTitle = "Esercizi pettorali alti per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/pettorali-alti/",
            publishedDate = "2024-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "E’ una tipologia di esercizio che non fornisce un grande stress in allungamento , ma può " +
                "trovare una sua collocazione in scheda in un rep range che va in linea generale dalle 8 " +
                "alle 15-20 ripetizioni. ",
            articleTitle = "Esercizi pettorali alti per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/pettorali-alti/",
            publishedDate = "2024-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Concentrati maggiormente sul rep range che va dalle 5 alle 15 ripetizioni . ",
            articleTitle = "Come aumentare la massa muscolare?",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare/",
            publishedDate = "2023-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Una buona indicazione generale potrebbe essere quella di svolgere, in termini di volume " +
                "settimanale per distretto muscolare, nel corso del macrociclo annuale: 50-70% del lavoro " +
                "nel range che va dalle 6 alle 12 ripetizioni. ",
            articleTitle = "Come aumentare la massa muscolare?",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare/",
            publishedDate = "2023-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "15-25% del lavoro nel range dalle 3 alle 5 ripetizioni. ",
            articleTitle = "Come aumentare la massa muscolare?",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare/",
            publishedDate = "2023-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Distretti come bicipiti, tricipiti, deltoidi, polpacci e addome vedranno adattarsi il " +
                "lavoro in termini % in rep range adatti, evitando lavori a 3-5 ripetizioni di curl " +
                "alternato. ",
            articleTitle = "Come aumentare la massa muscolare?",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare/",
            publishedDate = "2023-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "L’ intensità in genere adatta a questo tipo di esercizio è medio-elevata ( 75-85% ), " +
                "facendo dalle 4 alle 10 ripetizioni per “n” serie. ",
            articleTitle = "Stacchi rumeni: esecuzione, tecnica corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/stacchi-rumeni/",
            publishedDate = "2023-11-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "A tal proposito, secondo il principio delle dimensioni di Henneman già con carichi di " +
                "circa l’80% (6-8 ripetizioni massimali) vengono reclutate tutte le fibre muscolari. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "In pratica è ciò che sostanzialmente avviene quando sposti un peso per 6-15 ripetizioni. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Possiamo utilizzare un range di intensità che va dalle 8 alle 30 ripetizioni , facendo " +
                "però delle considerazioni: Il range che va dalle 8 alle 20 ripetizioni è in genere il " +
                "più utilizzato perchè permette un buon rapporto tra volume , intensità di carico e " +
                "fatica accumulata. ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Soggetti novizi-principianti ha più senso che si concentrino inizialmente su un range " +
                "che va dalle 8 alle 12 ripetizioni . ",
            articleTitle = "I migliori esercizi per i polpacci",
            url = "https://www.projectinvictus.it/esercizi-polpacci/",
            publishedDate = "2023-06-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Stacchi da terra a gambe tese con manubri e kettlebell : si adattano bene in genere a un " +
                "rep range indicativo dalle 8 alle 15 ripetizioni. ",
            articleTitle = "Stacchi a gambe tese per glutei di marmo",
            url = "https://www.projectinvictus.it/stacchi-gambe-tese/",
            publishedDate = "2023-01-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Scheda di allenamento stacchi a gambe tese In base alla variante scelta puoi utilizzare " +
                "uno spettro di intensità davvero ampio ( 6 – 15 ripetizioni ). ",
            articleTitle = "Stacchi a gambe tese per glutei di marmo",
            url = "https://www.projectinvictus.it/stacchi-gambe-tese/",
            publishedDate = "2023-01-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Ciò si traduce che una grossa percentuale dei tuoi allenamenti sarà intorno a rep range " +
                "tra 1 e 10/12 ripetizioni , piuttosto che dalle 15 ripetizioni in su. ",
            articleTitle = "Fibre rosse e bianche",
            url = "https://www.projectinvictus.it/fibre-rosse-bianche/",
            publishedDate = "2022-11-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Diventa più forte sulle 6-8 ripetizioni ed hai risolto il 90% dei problemi. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Puoi fare 4-6 serie da 6-8 ripetizioni con un recupero di 2′. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Esegui 4-6 serie da 6-8 ripetizioni con un recupero di 2′. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Per avere effetti importanti aumenta i carichi con cui fai 6-8 ripetizioni (con la " +
                "stessa tecnica e velocità). ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "12-15 ripetizioni per 3-5 serie Se fai bene il crunch e le sue varianti ( crunch inverso " +
                ", addominali per gli obliqui ) hai già fatto un terzo del lavoro. ",
            articleTitle = "Esercizi addome uomo",
            url = "https://www.projectinvictus.it/esercizi-addominali-uomo/",
            publishedDate = "2022-09-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "8-12 ripetizioni per 2-3 serie Il dragon flag , invece, va fatto solo se sei molto forte " +
                "e hai un livello avanzato, può essere messo anche come primo esercizio. ",
            articleTitle = "Esercizi addome uomo",
            url = "https://www.projectinvictus.it/esercizi-addominali-uomo/",
            publishedDate = "2022-09-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Il Protocollo TDR prevedeva carichi del 70%rm sollevati per 4 serie da 8-12 ripetizioni " +
                "di distensioni su panca piana, leg press,military press, lat machine, curl per i " +
                "bicipiti brachiali, estensioni tricipiti brachiali, leg curl e sit up (a cedimento) con " +
                "un minuto di recupero. ",
            articleTitle = "EPOC: cos’è e come aumentarlo",
            url = "https://www.projectinvictus.it/epoc-calorie-metabolismo-studi/",
            publishedDate = "2022-08-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "L’autore consiglia di lavorare su range di 3-5 ripetizioni per 3-5 set con recuperi " +
                "abbondanti e completi. ",
            articleTitle = "P.H.A.T. Power Hypertrophy Adaptive Training",
            url = "https://www.projectinvictus.it/phat-power-hypertrophy-adaptive-training/",
            publishedDate = "2022-07-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Per le loro caratteristiche i glutei rispondono bene a: lavori di forza : carichi alti " +
                "(80%-90% del massimale) e basse ripetizioni (1-5 ripetizioni); lavori in massimo " +
                "accorciamento (es. ",
            articleTitle = "Glutei: anatomia, esercizi, allenamento",
            url = "https://www.projectinvictus.it/glutei/",
            publishedDate = "2022-05-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Ha senso lavorare a rep range medi per ripetere il gesto più volte all’interno della " +
                "serie: un range che può andare dalle 8 alle 10 ripetizioni . ",
            articleTitle = "Box Squat: come si esegue",
            url = "https://www.projectinvictus.it/box-squat/",
            publishedDate = "2021-04-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Rep range: mediamente il rep range in cui si svolge gran parte del lavoro, considerando " +
                "che la porzione stressata maggiormente per l’obbiettivo è quella mediale, va dalle 8 " +
                "alle 12/15 ripetizioni. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Puoi comunque svolgere lavori anche in rep range inferiori nel range delle 5-6 " +
                "ripetizioni. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "REP RANGE : 5-6 4 serie: ESERCIZIO DI ISOLAMENTO MULTIARTICOLARE. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "REP RANGE : 7-9 3-4 serie : ESERCIZIO DI ISOLAMENTO MONOARTICOLARE. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Soggetto intermedio Soggetto in grado di eseguire il vacuum in piedi con mani dietro la " +
                "nuca per 10-15 secondi per 2-3 serie da 6 ripetizioni. ",
            articleTitle = "Vacuum addominale: esecuzione corretta",
            url = "https://www.projectinvictus.it/vacuum-addominale/",
            publishedDate = "2021-01-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Svolgeva per ogni esercizio sei serie da 10-15 ripetizioni, per terminare poi il workout " +
                "per i polpacci con un completo stretching statico di circa 1 minuto. ",
            articleTitle = "Polpacci Carenti: mito o realtà?",
            url = "https://www.projectinvictus.it/polpacci-carenti/",
            publishedDate = "2020-09-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Come la più classica delle progressioni lineare, quando non riuscite più a progredire " +
                "avete due alternative: Passate da 8 a 6 ripetizioni, infine a 5-4, quindi/oppure: " +
                "Scalate del 10% e ricominciate a salire. ",
            articleTitle = "Come imparare a fare la prima trazione alla sbarra",
            url = "https://www.projectinvictus.it/come-imparare-a-fare-la-prima-trazione-alla-sbarra/",
            publishedDate = "2019-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "PANCA: 5 serie da 3 ripetizioni Peso usato la settimana prima + 10% Pausa 4-5 minuti fra " +
                "le serie. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "PANCA: 5 serie da 4 ripetizioni Peso usato la settimana prima + 5% Pausa 4-5 minuti fra " +
                "le serie. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Peso usato la settimana prima + 5% MILITARY PRESS: 5 serie da 4 ripetizioni Peso usato " +
                "la settimana prima + 5% Pausa 4-5 minuti fra le serie. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Faremo quindi la prima serie da 2 ripetizioni, la seconda serie da 4 ripetizioni, la " +
                "terza da 6 ripetizioni fino al massimo unbroken e poi torniamo indietro. ",
            articleTitle = "Metodo ad incremento",
            url = "https://www.projectinvictus.it/metodo-ad-incremento/",
            publishedDate = "2018-04-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Se ho un massimale ipotetico di 4-5 ripetizioni difficilmente userò un RT troppo " +
                "voluminoso . ",
            articleTitle = "Il Metodo Ripetizioni Totali nel Calisthenics",
            url = "https://www.projectinvictus.it/il-metodo-ripetizioni-totali-nel-calisthenics/",
            publishedDate = "2018-03-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Ovviamente se uno ha un massimale anche solo di 7-10 ripetizioni non è il top. ",
            articleTitle = "Il Metodo Ripetizioni Totali nel Calisthenics",
            url = "https://www.projectinvictus.it/il-metodo-ripetizioni-totali-nel-calisthenics/",
            publishedDate = "2018-03-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Se cercate l’IPERTROFIA dovete incentrare una parte cospicua delle vostre sedute con " +
                "carichi che vi permettano di fare da 8 a 12 ripetizioni . ",
            articleTitle = "Ipertrofia e forza: le logiche che li correlano",
            url = "https://www.projectinvictus.it/ipertrofia-forza/",
            publishedDate = "2016-12-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Negative a muro gambe divaricate Appreso bene l’ esercizio precedente in modo da " +
                "eseguire 4 serie da 10 ripetizioni possiamo passare alla fase negativa a gambe " +
                "divaricate. ",
            articleTitle = "Verticale d’impostazione: guida e tutorial",
            url = "https://www.projectinvictus.it/verticale-dimpostazione-guida-e-tutorial/",
            publishedDate = "2016-09-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Consiglio all’inizio di eseguire qualche serie con poche ripetizioni, per poi aumentare " +
                "fino a raggiungere le 4 serie da 10 ripetizioni, per poi proseguire con la propedeutica. ",
            articleTitle = "Verticale d’impostazione: guida e tutorial",
            url = "https://www.projectinvictus.it/verticale-dimpostazione-guida-e-tutorial/",
            publishedDate = "2016-09-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Cosniglio di scendere pochi gradi fino a effettare 4 serie da 5 ripetizioni per poi " +
                "progredire fino a terra. ",
            articleTitle = "Verticale d’impostazione: guida e tutorial",
            url = "https://www.projectinvictus.it/verticale-dimpostazione-guida-e-tutorial/",
            publishedDate = "2016-09-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Un buon risultato in questo esercizio per passare alle versioni successive è quello di " +
                "completare 5 serie da 15 ripetizioni con 1-1,30′ di recupero tra le serie. ",
            articleTitle = "Trazioni senza sovraccarico: alternativa alle zavorre",
            url = "https://www.projectinvictus.it/trazioni-alternativa-alle-zavorre/",
            publishedDate = "2016-03-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Questo esercizio va allenato fino a completare 5 serie da 5 ripetizioni con 1-2′ di " +
                "recupero. ",
            articleTitle = "Trazioni senza sovraccarico: alternativa alle zavorre",
            url = "https://www.projectinvictus.it/trazioni-alternativa-alle-zavorre/",
            publishedDate = "2016-03-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Un buon obbiettivo da completare prima di passare allo step successivo è quello di " +
                "riuscire ad eseguire 5 serie da 8 ripetizioni di questo esercizio con un minuto, un " +
                "minuto e mezzo di riposo tra le serie. ",
            articleTitle = "Trazioni con l’asciugamano. Binomio perfetto",
            url = "https://www.projectinvictus.it/trazioni-con-lasciugamano-binomio-perfetto/",
            publishedDate = "2015-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Anche qui l’obbiettivo sarà quello di completare 5 serie da 5 ripetizioni per braccio, " +
                "con 2′ di riposo tra le serie inizialmente, poi potremo ridurre a piacimento fino ad 1′ " +
                "o 30”. ",
            articleTitle = "Trazioni con l’asciugamano. Binomio perfetto",
            url = "https://www.projectinvictus.it/trazioni-con-lasciugamano-binomio-perfetto/",
            publishedDate = "2015-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Nella prima settimana andremo ad eseguire 5 serie da 3 ripetizioni per braccio. ",
            articleTitle = "Trazioni con l’asciugamano. Binomio perfetto",
            url = "https://www.projectinvictus.it/trazioni-con-lasciugamano-binomio-perfetto/",
            publishedDate = "2015-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Sembra che i vari esercizi vadano effettuati per 3 serie di 12-15 ripetizioni 2 volte al " +
                "giorno per almeno la prima settimana. ",
            articleTitle = "Riabilitazione e prevenzione della spalla: impingement sub-acromiale",
            url = "https://www.projectinvictus.it/riabilitazione-e-prevenzione-della-spalla-impingement-sub-acromiale/",
            publishedDate = "2015-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Eseguire 3 serie da 8-10 ripetizioni per lato in modo lento e controllato e con " +
                "respirazione diaframmatica (inspirazione durante la rotazione) . ",
            articleTitle = "Riabilitazione e prevenzione della spalla: impingement sub-acromiale",
            url = "https://www.projectinvictus.it/riabilitazione-e-prevenzione-della-spalla-impingement-sub-acromiale/",
            publishedDate = "2015-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Eseguire 3-4 serie da 10 ripetizioni per ogni movimento articolare. ",
            articleTitle = "Riabilitazione e prevenzione della spalla: impingement sub-acromiale",
            url = "https://www.projectinvictus.it/riabilitazione-e-prevenzione-della-spalla-impingement-sub-acromiale/",
            publishedDate = "2015-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Nello studio è stata proposta una serie da 10 ripetizioni per 10 secondi nella posizione " +
                "del massimo allungamento raggiunto. ",
            articleTitle = "Riabilitazione e prevenzione della spalla: impingement sub-acromiale",
            url = "https://www.projectinvictus.it/riabilitazione-e-prevenzione-della-spalla-impingement-sub-acromiale/",
            publishedDate = "2015-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "8-12 ripetizioni x 4-5 serie Sit ups Sit ups, in questo caso alla spalliera. ",
            articleTitle = "10 Esercizi base per gli Addominali nella Ginnastica",
            url = "https://www.projectinvictus.it/10-esercizi-base-per-gli-addominali-nella-ginnastica/",
            publishedDate = "2014-06-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "10-15 ripetizioni x 3-4 serie Sit ups completi Versione avanzata dei sit ups . ",
            articleTitle = "10 Esercizi base per gli Addominali nella Ginnastica",
            url = "https://www.projectinvictus.it/10-esercizi-base-per-gli-addominali-nella-ginnastica/",
            publishedDate = "2014-06-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "6-10 ripetizioni x 2-3 serie Addominali obliqui Da sdraiati su un lato, alziamo " +
                "contemporaneamente le gambe ed il busto, cercando di aiutarci il meno possibile col " +
                "braccio a terra. ",
            articleTitle = "10 Esercizi base per gli Addominali nella Ginnastica",
            url = "https://www.projectinvictus.it/10-esercizi-base-per-gli-addominali-nella-ginnastica/",
            publishedDate = "2014-06-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "10-15 ripetizioni x 3-4 volte Addominali ballerina Partiamo da una posizione seduta con " +
                "gambe tese, le alziamo flettendole ripetutamente. ",
            articleTitle = "10 Esercizi base per gli Addominali nella Ginnastica",
            url = "https://www.projectinvictus.it/10-esercizi-base-per-gli-addominali-nella-ginnastica/",
            publishedDate = "2014-06-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Se un atleta esegue 4-8 ripetizioni in buffer con un carico che gli permetterebbe " +
                "l’esecuzione di 10 o più ripetizioni ( ≤ 75% 1-RM), egli non si sta allenando nel “ " +
                "range di forza”, e il massimo sviluppo della forza viene penalizzato. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 2)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-2/",
            publishedDate = "2014-06-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REPS,
            statement =
                "When training at a specific RM load, it is recommended that 2-10% increase in load be " +
                "applied when the individual can perform the current workload for one to two repetitions " +
                "over the desired number. ",
            articleTitle = "American College of Sports Medicine position stand. Progression models in resistance training for healthy adults",
            url = "https://pubmed.ncbi.nlm.nih.gov/19204579/",
            publishedDate = "2009",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Triceps brachii (TB) muscle thickness was assessed using ultrasonography at 60% (TB60) " +
                "and 70% (TB70) of the upper arm length, and the sum of both sites was calculated (∑TB). ",
            articleTitle = "Progressive Overload Affects the Magnitude of Muscle Hypertrophy",
            url = "https://pubmed.ncbi.nlm.nih.gov/41718594/",
            publishedDate = "2026",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Subjects were tested pre- and post-study for: 1 repetition maximum (RM) strength in the " +
                "bench press and squat, upper body muscle endurance, and muscle thickness of the elbow " +
                "flexors, elbow extensors, and lateral thigh. ",
            articleTitle = "Differential Effects of Heavy Versus Moderate Loads on Measures of Strength and Hypertrophy in Resistance-Trained Men",
            url = "https://pubmed.ncbi.nlm.nih.gov/27928218/",
            publishedDate = "2016",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Higher-load (>80% of single repetition maximum) prescriptions maximised strength gains, " +
                "and all prescriptions comparably promoted muscle hypertrophy. ",
            articleTitle = "Resistance training prescription for muscle strength and hypertrophy in healthy adults: a systematic review and Bayesian network meta-analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/37414459/",
            publishedDate = "2023",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Our results revealed that during both low-load training and walking, the addition of BFR " +
                "elicits significantly greater improvements in muscular strength with pooled effect sizes " +
                "(ES) of 2.16 (95% CI 1.61 to 2.70) and 3.09 (95% CI 2.04 to 4.14), respectively. ",
            articleTitle = "Effects of Blood Flow Restriction Training on Muscular Strength and Hypertrophy in Older Individuals: A Systematic Review and Meta-Analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/30306467/",
            publishedDate = "2019",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.REPS,
            statement =
                "Muscle mass was also increased when comparing walking with and without BFR [ES 1.82 (95% " +
                "CI 1.32 to 2.32)]. ",
            articleTitle = "Effects of Blood Flow Restriction Training on Muscular Strength and Hypertrophy in Older Individuals: A Systematic Review and Meta-Analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/30306467/",
            publishedDate = "2019",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.REPS,
            statement =
                "In comparison with high-load training, LL-BFR promotes similar muscle hypertrophy [ES " +
                "0.21 (95% CI - 0.14 to 0.56)] but lower strength gains [ES - 0.42 (95% CI - 0.70 to - " +
                "0.14)]. ",
            articleTitle = "Effects of Blood Flow Restriction Training on Muscular Strength and Hypertrophy in Older Individuals: A Systematic Review and Meta-Analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/30306467/",
            publishedDate = "2019",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.REST,
            statement =
                "Puoi consumare una porzione di 20-30g di cioccolato fondente dopo gli allenamenti per " +
                "fornire un apporto immediato di energia grazie ai carboidrati e ai grassi presenti nel " +
                "cioccolato, contribuendo al recupero post-esercizio. ",
            articleTitle = "I 10 migliori alimenti per la massa muscolare",
            url = "https://www.projectinvictus.it/alimenti-per-la-massa-muscolare/",
            publishedDate = "2025-07-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Per tale ragione è raccomandabile inserire un recupero di 2-3 giorni tra le attività di " +
                "carico pensante sul tendine ogni volta che si aumentano in modo importante i parametri " +
                "di allenamento. ",
            articleTitle = "Epicondilite (gomito del tennista): cause, rimedi ed esercizi efficaci",
            url = "https://www.projectinvictus.it/epicondilite-rimedi-esercizi-palestra/",
            publishedDate = "2025-02-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Puoi partire con circa il 30% del proprio massimale e andare spedito con 30” di pausa " +
                "tra le serie, per poi diminuire le reps appena cominci ad avvertire molta fatica. ",
            articleTitle = "Calisthenic Workout: schede e programmi per il calisthenics",
            url = "https://www.projectinvictus.it/calisthenic-workout-programmi/",
            publishedDate = "2024-10-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Se ancora non ci riesci, puoi seguire questo programma : 4 serie x max secondi, recupero " +
                "30″ . ",
            articleTitle = "Plank: la guida completa",
            url = "https://www.projectinvictus.it/plank-addominali/",
            publishedDate = "2024-09-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Una volta che 1′ sarà facile da tenere, puoi fare più serie man mano diminuendo il tempo " +
                "di pausa tra le serie oppure incrementare il tempo di tenuta, puntando ad esempio a " +
                "mantenere i 5′ – se ti sembrano tanti, ricorda che il record del mondo è di più di 10 " +
                "ore! ",
            articleTitle = "Plank: la guida completa",
            url = "https://www.projectinvictus.it/plank-addominali/",
            publishedDate = "2024-09-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Il recupero è soggettivo , il fattore più limitante è più il sistema nervoso rispetto al " +
                "recupero muscolare, dovremo quindi considerare in linea generale il recupero di 3 " +
                "sistemi: Muscoli: in genere recuperano in 48-72 ore circa. ",
            articleTitle = "Come aumentare la massa muscolare?",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare/",
            publishedDate = "2023-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Puoi fare 4-6 serie da 6-8 ripetizioni con un recupero di 2′. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Esegui 4-6 serie da 6-8 ripetizioni con un recupero di 2′. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Esegui 0-2 serie da > 12 ripetizioni con un recupero di 1’30. ",
            articleTitle = "Come ingrossare le braccia",
            url = "https://www.projectinvictus.it/come-ingrossare-le-braccia/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Il Protocollo TDR prevedeva carichi del 70%rm sollevati per 4 serie da 8-12 ripetizioni " +
                "di distensioni su panca piana, leg press,military press, lat machine, curl per i " +
                "bicipiti brachiali, estensioni tricipiti brachiali, leg curl e sit up (a cedimento) con " +
                "un minuto di recupero. ",
            articleTitle = "EPOC: cos’è e come aumentarlo",
            url = "https://www.projectinvictus.it/epoc-calorie-metabolismo-studi/",
            publishedDate = "2022-08-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Rambod consiglia sempre le solite 8-12 ripetizioni , con un peso più leggero di un 8-12 " +
                "RM considerato il recupero incompleto. ",
            articleTitle = "FST-7 : cos’è e funziona veramente?",
            url = "https://www.projectinvictus.it/fst-7-cose-e-funziona-veramente/",
            publishedDate = "2022-07-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "L’autore consiglia di lavorare su range di 3-5 ripetizioni per 3-5 set con recuperi " +
                "abbondanti e completi. ",
            articleTitle = "P.H.A.T. Power Hypertrophy Adaptive Training",
            url = "https://www.projectinvictus.it/phat-power-hypertrophy-adaptive-training/",
            publishedDate = "2022-07-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Non si può dire di aver fatto un recupero completo se non si è portato avanti questa " +
                "fase per almeno 3-6 mesi . ",
            articleTitle = "Conflitto subacromiale della spalla: cause e rimedi",
            url = "https://www.projectinvictus.it/conflitto-spalla-cause-rimedi/",
            publishedDate = "2021-09-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Alcuni ricercatori infatti sottolineano che i BCAA potrebbero agire positivamente sul " +
                "recupero se l’apporto proteico è al di sotto di 1.6 g/kg di proteine (16), cioè " +
                "inferiore alle soglie minime ottimali per chi si allena con i pesi (1,7). ",
            articleTitle = "BCAA – aminoacidi ramificati: cosa sono e quali scegliere?",
            url = "https://www.projectinvictus.it/aminoacidi-ramificati-bcaa/",
            publishedDate = "2021-03-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Se vuoi lavorare più sulla forza prediligi un volume basso e un carico intenso (> 85% di " +
                "1RM) con un recupero alto (>180”) . ",
            articleTitle = "Pettorali nella donna per tonificare il seno",
            url = "https://www.projectinvictus.it/esercizi-pettorali-donne-aumentare-seno/",
            publishedDate = "2021-03-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Serie : in linea generale, considerando questo volume come esclusivo per il deltoide " +
                "mediale, si consiglia di iniziare con 10-12 serie allenanti , da cui poi muoverti verso " +
                "un loro aumento o diminuzione in base alle progressioni e ai parametri di recupero " +
                "soggettivi. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "REP RANGE : 10-12 Quante volte allenare le spalle a settimana Puoi partire con una " +
                "frequenza di allenamento di 2 volte settimanali , per poi muoverti se necessario verso " +
                "frequenze più spinte o conservative in base alle progressioni, ai restanti parametri " +
                "allenanti e di recupero soggettivi. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Io consiglio di partire con circa il 30% del proprio massimale e andare spediti con 30” " +
                "di pausa tra le serie, per poi diminuire le reps come si comincia ad avvertire molta " +
                "fatica. ",
            articleTitle = "Calisthenic Workout: schede e programmi per il calisthenics",
            url = "https://www.projectinvictus.it/calisthenic-workout-schede-e-programmi-per-il-calisthenics/",
            publishedDate = "2020-01-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Recupero tra le serie: almeno 2 minuti Per lo stacco da terra si usa lo stesso schema " +
                "serie / ripetizioni. ",
            articleTitle = "Spartan Ultra Race: come prepararla",
            url = "https://www.projectinvictus.it/spartan-ultra-race-preparazione/",
            publishedDate = "2019-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Il recupero finisce quando la frequenza cardiaca scende al 60% della FCMAX 30 burpees Il " +
                "numero di ripetute aumenterà naturalmente ad ogni inizio di mesociclo in base al " +
                "risultato dello sprint test. ",
            articleTitle = "Spartan Ultra Race: come prepararla",
            url = "https://www.projectinvictus.it/spartan-ultra-race-preparazione/",
            publishedDate = "2019-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Scarico Ogni 5-6 settimane di allenamento fatene una di recupero attivo. ",
            articleTitle = "Spartan Ultra Race: come prepararla",
            url = "https://www.projectinvictus.it/spartan-ultra-race-preparazione/",
            publishedDate = "2019-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "A quel punto ci concediamo un recupero semi-completo superiore ai 2 minuti (anche fino a " +
                "3) e poi ripartiamo. ",
            articleTitle = "Allenare la Planche con l’elastico",
            url = "https://www.projectinvictus.it/planche-con-elastico/",
            publishedDate = "2018-06-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Tale metodo può essere applicato con un approccio all-out (tutto a cedimento) mantenendo " +
                "un tempo di recupero completo tra le serie (3/4 minuti), o con un approccio in buffer, " +
                "mantenendo però tempi di recupero incompleti (circa 1-2 minuti). ",
            articleTitle = "L’importanza della tecnica e di non scappare dai punti difficili",
            url = "https://www.projectinvictus.it/importanza-tecnica/",
            publishedDate = "2017-01-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Una volta che saremo in grado di tenere una buona squadra ed eseguire correttamente le " +
                "trazioni in L-sit, il nostro obbiettivo sarà arrivare a completare 5 serie da 5/6 " +
                "ripetizioni con 1′- 1,30′ di recupero tra le serie. ",
            articleTitle = "Trazioni senza sovraccarico: alternativa alle zavorre",
            url = "https://www.projectinvictus.it/trazioni-alternativa-alle-zavorre/",
            publishedDate = "2016-03-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Un buon risultato in questo esercizio per passare alle versioni successive è quello di " +
                "completare 5 serie da 15 ripetizioni con 1-1,30′ di recupero tra le serie. ",
            articleTitle = "Trazioni senza sovraccarico: alternativa alle zavorre",
            url = "https://www.projectinvictus.it/trazioni-alternativa-alle-zavorre/",
            publishedDate = "2016-03-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Questo esercizio va allenato fino a completare 5 serie da 5 ripetizioni con 1-2′ di " +
                "recupero. ",
            articleTitle = "Trazioni senza sovraccarico: alternativa alle zavorre",
            url = "https://www.projectinvictus.it/trazioni-alternativa-alle-zavorre/",
            publishedDate = "2016-03-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "In questo caso i tempi di recupero ammontano a 2 minuti tra le serie. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 4)",
            url = "https://www.projectinvictus.it/chiarimenti-incomprensioni-sullintensita-nellesercizio-i-pesi-parte-4/",
            publishedDate = "2014-11-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "In questo caso però i tempi di recupero tra le serie sono dimezzati a 1 minuto, con il " +
                "risultato che la densità di allenamento è molto aumentata rispetto alla precedente. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 4)",
            url = "https://www.projectinvictus.it/chiarimenti-incomprensioni-sullintensita-nellesercizio-i-pesi-parte-4/",
            publishedDate = "2014-11-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Questo significa che nei programmi tipici dell’ipertrofia, che normalmente prevedono " +
                "recuperi tra le serie di 60-90 secondi[45], se lo sforzo viene portato al cedimento il " +
                "numero di ripetizioni massime cala ad ogni serie. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 2)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-2/",
            publishedDate = "2014-06-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Nei programmi di forza massimale questo effetto è ancora meno pronunciato, grazie a " +
                "tempi di recupero tra le serie più lunghi[47], tra 3 e 5 minuti, che permettono un " +
                "maggiore recupero e mantenimento della forza[45]. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 2)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-2/",
            publishedDate = "2014-06-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "However, it seems that longer duration RIs (> 2 min) are required to maximize strength " +
                "gains in resistance-trained individuals. ",
            articleTitle = "Effects of Rest Interval Duration in Resistance Training on Measures of Muscular Strength: A Systematic Review",
            url = "https://pubmed.ncbi.nlm.nih.gov/28933024/",
            publishedDate = "2018",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.REST,
            statement =
                "J Strength Cond Res 36(1): 22-30, 2022-This study investigated the effects of different " +
                "volumes of resistance training (RT) (8 weeks of 16, 24, and 32 weekly sets per muscle " +
                "group) on muscular strength and hypertrophy. ",
            articleTitle = "High Resistance-Training Volume Enhances Muscle Thickness in Resistance-Trained Men",
            url = "https://pubmed.ncbi.nlm.nih.gov/31868813/",
            publishedDate = "2022",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REST,
            statement =
                "The magnitude of increase in 1RM and MT of the lower body when training with 32 weekly " +
                "sets per muscle group was higher than for 16 weekly sets per muscle group. ",
            articleTitle = "High Resistance-Training Volume Enhances Muscle Thickness in Resistance-Trained Men",
            url = "https://pubmed.ncbi.nlm.nih.gov/31868813/",
            publishedDate = "2022",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REST,
            statement =
                "The magnitude of the increase in MTTB was higher when training with 32 weekly sets than " +
                "for 16 weekly sets. ",
            articleTitle = "High Resistance-Training Volume Enhances Muscle Thickness in Resistance-Trained Men",
            url = "https://pubmed.ncbi.nlm.nih.gov/31868813/",
            publishedDate = "2022",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REST,
            statement =
                "Overall, the reported improvement in sprint performance (sprint ES = -0.87, mean sprint " +
                "improvement = 3.11 %) resulting from resistance training is of practical relevance for " +
                "coaches and athletes in sport activities requiring high levels of speed. ",
            articleTitle = "Increases in lower-body strength transfer positively to sprint performance: a systematic review with meta-analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/25059334/",
            publishedDate = "2014",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.REST,
            statement =
                "CONCLUSIONS: The use of 2 minutes of rest between sets allowed the performance of a high " +
                "volume-load and density of the session in young women. ",
            articleTitle = "Does rest interval between sets affect resistance training volume, density, and rating of perceived exertion when adopting the crescent pyramid system in young women?",
            url = "https://pubmed.ncbi.nlm.nih.gov/32597616/",
            publishedDate = "2020",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REST,
            statement =
                "Seventeen young men were randomly assigned to either a hypertrophy-type resistance " +
                "training group that performed 3 sets of 10 repetition maximum (RM) with 90 seconds rest " +
                "or a strength-type resistance training (ST) group that performed 7 sets of 3RM with a " +
                "3-minute rest interval. ",
            articleTitle = "Effects of different volume-equated resistance training loading strategies on muscular adaptations in well-trained men",
            url = "https://pubmed.ncbi.nlm.nih.gov/24714538/",
            publishedDate = "2014",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REST,
            statement =
                "After 8 weeks, no significant differences were noted in muscle thickness of the biceps " +
                "brachii. ",
            articleTitle = "Effects of different volume-equated resistance training loading strategies on muscular adaptations in well-trained men",
            url = "https://pubmed.ncbi.nlm.nih.gov/24714538/",
            publishedDate = "2014",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.REST,
            statement =
                "Questo perchè ogni volta che si porta il muscolo allo sfinimento: 1 aumentano di molti i " +
                "tempi di recupero tra i giorni (invece qui dobbiamo allenare la panca 3 volte a " +
                "settima), 2 si deteriora la tecnica perchè si pensa esclusivamente a concludere l’alzata " +
                "perdendo completamente l’assetto. ",
            articleTitle = "5×5 per migliorare la tua Panca Piana (versione inVictus)",
            url = "https://www.projectinvictus.it/5x5-per-migliorare-la-tua-panca-piana-versione-invictus/",
            publishedDate = "2014-09-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.REST,
            statement =
                "Esegui questi esercizi per 3 serie, con 45” di pausa tra le serie. ",
            articleTitle = "Crunch laterali: come farli correttamente",
            url = "https://www.projectinvictus.it/crunch-obliqui/",
            publishedDate = "2024-07-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Nel mio caso quindi si tratterebbe di 22-23 settimane solo per ripristinare tutti quegli " +
                "adattamenti metabolici ed ormonali necessari per poter progredire nel tempo. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "La frequenza di allenamento proposta dal buon caro Bill Starr prevede innanzitutto una " +
                "split in FULL BODY strutturata su 3-4 esercizi su cui vogliamo progredire, i quali " +
                "verranno ripetuti 3 volte alla settimana. ",
            articleTitle = "5×5: di Bill Starr, Reg Park, Stronglift",
            url = "https://www.projectinvictus.it/5x5/",
            publishedDate = "2018-08-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Qualora si riesca a completare le 3 serie da 5 con 100 kg la sessione successiva si può " +
                "provare ad aumentare il carico. ",
            articleTitle = "5×5: di Bill Starr, Reg Park, Stronglift",
            url = "https://www.projectinvictus.it/5x5/",
            publishedDate = "2018-08-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Si puo scegliere, come sempre in base agli altri esercizi, se continuare la progressione " +
                "ogni volta dopo il max tecnico con la stessa regola ( 6 x 4 + 1 x max tecnico/ 4 x 5 + 1 " +
                "x max tecnico ) o decidere di stabilizzare le serie e le ripetizioni. ",
            articleTitle = "Metodo Set x Rep + 1xmax nel calisthenics",
            url = "https://www.projectinvictus.it/programma-set-x-rep-1xmax-calisthenics/",
            publishedDate = "2018-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Consiglio in questo caso di partire usando come ripetizioni di riferimento poco meno del " +
                "50% del massimale, in modo da avere una progressione costante le prime volte che " +
                "affronterete il programma. ",
            articleTitle = "Metodo Set x Rep + 1xmax nel calisthenics",
            url = "https://www.projectinvictus.it/programma-set-x-rep-1xmax-calisthenics/",
            publishedDate = "2018-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Se il peso corporeo rappresenta un sovraccarico troppo elevato (che permette meno di " +
                "12-15 ripetizioni massime), è possibile optare per la variante facilitata da seduti su " +
                "panca, con i piedi appoggiati a terra. ",
            articleTitle = "Scrollate inverse per il riequilibrio scapolare",
            url = "https://www.projectinvictus.it/scrollate-inverse-per-riequilibrio-scapolare/",
            publishedDate = "2014-11-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Both protocols increased 1RM values from pre (LOADprog: 52.90±16.32 kg; REPSprog: " +
                "51.67±15.84 kg) to post (LOADprog: 69.05±18.55 kg, REPSprog: 66.82±17.95 kg), with no " +
                "difference between them (P+>+0.05). ",
            articleTitle = "Effects of Resistance Training Overload Progression Protocols on Strength and Muscle Mass",
            url = "https://pubmed.ncbi.nlm.nih.gov/38286426/",
            publishedDate = "2024",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Alternatively, dynamic strength increases slightly favored LOAD (2.0 kg [-2.4, 7.8]), " +
                "with differences of questionable practical significance. ",
            articleTitle = "Progressive overload without progressing load? The effects of load or repetition progression on muscular adaptations",
            url = "https://pubmed.ncbi.nlm.nih.gov/36199287/",
            publishedDate = "2022",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "No other notable between-group differences were found across outcomes (muscle " +
                "thicknesses, <1 mm; endurance, <1%; countermovement jump, 0.1 cm; body fat, <1%; leg " +
                "segmental lean mass, 0.1 kg), with narrow CIs for most outcomes. ",
            articleTitle = "Progressive overload without progressing load? The effects of load or repetition progression on muscular adaptations",
            url = "https://pubmed.ncbi.nlm.nih.gov/36199287/",
            publishedDate = "2022",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Objective To compare the effects of strength training with load progression after 4 " +
                "weeks on sleep parameters and mental health in college students. ",
            articleTitle = "Examining the Effects of Strength Training with Load Progression on Sleep Parameters and Mental Health in College Students",
            url = "https://pubmed.ncbi.nlm.nih.gov/38846591/",
            publishedDate = "2024",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Discussion Short-term strength training for 4 weeks was effective for improving mental " +
                "health, helping achieve characteristics of a positive mood profile, that is, low values " +
                "for negative factors and a high value for the positive factor. ",
            articleTitle = "Examining the Effects of Strength Training with Load Progression on Sleep Parameters and Mental Health in College Students",
            url = "https://pubmed.ncbi.nlm.nih.gov/38846591/",
            publishedDate = "2024",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "CASE PRESENTATION: Two female recreational athletes (Case A: 27 years old; Case B: 28 " +
                "years old) with subacromial pain were treated with BFRT (50% occlusion) during 3 " +
                "isotonic strengthening exercises of the upper extremity against 20% of the maximal " +
                "isometric strength. ",
            articleTitle = "Blood Flow Restriction Training for Subacromial Pain: Two Case Reports",
            url = "https://pubmed.ncbi.nlm.nih.gov/36582265/",
            publishedDate = "2022",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Thirty volunteers (men = 16 and women = 14; age = 23.3 ± 1.7 years; body mass = 63.9 ± " +
                "13.3 kg; height = 167.9 ± 9.2 cm; body mass index [BMI] = 22.5 ± 3.4 kg·m-2) performed " +
                "14 static back-rows at 7 different lengths of the straps in 2 different elbow positions " +
                "(flexed and extended). ",
            articleTitle = "Evaluation of Training Load During Suspension Exercise",
            url = "https://pubmed.ncbi.nlm.nih.gov/30893278/",
            publishedDate = "2021",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PROGRESSION,
            statement =
                "Se riesci ad eseguire correttamente >12-15 ripetizioni è meglio piuttosto, se scegli " +
                "questa variante, utilizzare la versione con sovraccarico. ",
            articleTitle = "Calf in piedi: esecuzione corretta",
            url = "https://www.projectinvictus.it/calf-in-piedi/",
            publishedDate = "2023-05-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Quanti grassi al giorno nel bodybuilding La quantità consigliata (25-30%) è consigliata " +
                "anche a chi fa bodybuilding, anche se c’è qualche differenza a seconda che tu sia in " +
                "fase di definizione o di massa. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "In fase di massa , le calorie sono più alte e quindi anche i grassi potranno essere " +
                "rialzati, sempre stando nel range 20-35% o alzando al massimo al 40%. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Come impostare la dieta per la massa muscolare Se provieni da una fase di Cut, la prima " +
                "cosa da fare è tornare in normocalorica in modo graduale e stabilizzarti a queste " +
                "calorie per 2-3 settimane , in modo da tornare in una situazione fisiologica e pronta ad " +
                "instaurare una fase anabolica. ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Dieta per aumentare la massa muscolare senza ingrassare Prima di tutto, per evitare di " +
                "ingrassare troppo è bene evitare di iniziare una fase di massa se hai percentuale di " +
                "massa grassa superiore a circa l’11% per gli uomini e 16% per le donne . ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Se la tua percentuale di grasso corporeo attuale è superiore al 20-21%, è consigliato " +
                "vivamente di ridurre la percentuale di grasso corporeo, altrimenti rischi di fare una " +
                "fase di “grassa” e non di “massa”. ",
            articleTitle = "Dieta donna nel bodybuilding e in palestra",
            url = "https://www.projectinvictus.it/dieta-donne-bodybuilding/",
            publishedDate = "2024-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Una volta terminata la fase di definizione, segui una dieta normocalorica di " +
                "mantenimento per 2-3 settimane e poi continua con la fase di massa . ",
            articleTitle = "Definizione in palestra: come definire i muscoli",
            url = "https://www.projectinvictus.it/definizione-palestra/",
            publishedDate = "2024-01-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Fase di massa Per aumentare la massa muscolare è necessario essere in surplus calorico , " +
                "mediamente tra le 300 e 500 kcal giornaliere rispetto all’apporto calorico di " +
                "mantenimento. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Fase di definizione Per rendere visibile la muscolatura dopo una fase di massa è " +
                "necessario essere in deficit calorico per diversi mesi: in media pari a 300 kcal per le " +
                "donne e a 500 kcal per gli uomini rispetto all’apporto calorico giornaliero di " +
                "mantenimento. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Una volta finita questa fase di intensificazione , procederai con una fase di scarico " +
                "(della durata di 2/3 settimane), per drenare tutta la fatica accumulata e poi eseguirai " +
                "il test . ",
            articleTitle = "Forza massimale: cos’è, calcolo e come allenarla",
            url = "https://www.projectinvictus.it/forza-massimale/",
            publishedDate = "2022-10-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "I grassi in fase di cut coprono il 15-25% del fabbisogno calorico totale, con un minimo " +
                "di 0,5 g/kg per assicurare la corretta funzionalità dell’organismo. ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Se volessimo sapere quanto tempo impiegano le scorte muscolari per saturarsi , potremmo " +
                "prendere per buoni i seguenti valori: previa assunzione moderata e continua : 28 giorni; " +
                "mediante fase di carico equivalente a 5-7 giorni, da proseguire con il mantenimento: " +
                "quasi la metà . ",
            articleTitle = "Creatina: assunzione e dosi giornaliere",
            url = "https://www.projectinvictus.it/come-assumere-creatina/",
            publishedDate = "2021-12-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Superata questa fase si entrerà in una “ fase di cicatrizzazione ” della durata della " +
                "durata media di 1-3 settimane. ",
            articleTitle = "Mal di schiena: cause, sintomi e rimedi efficaci",
            url = "https://www.projectinvictus.it/mal-di-schiena-cause-sintomi-e-rimedi-efficaci/",
            publishedDate = "2021-01-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Existing literature shows that exercise performance towards 2-3 sets of 1-2 exercises " +
                "per major muscle group, performing 5-8 repetitions or achieving intensities of 50-80% of " +
                "1RM, 2-3 times per week should be recommended, followed by training principles such as " +
                "periodization and progression. ",
            articleTitle = "A Review on Aging, Sarcopenia, Falls, and Resistance Training in Community-Dwelling Older Adults",
            url = "https://pubmed.ncbi.nlm.nih.gov/35055695/",
            publishedDate = "2022",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "However, voluntary strength was enhanced by lifting heavier loads (≥80% one-repetition " +
                "maximum), through a complete range of motion, for 2-3 sets, at the beginning of training " +
                "sessions, and ≥2 sessions/wk. ",
            articleTitle = "American College of Sports Medicine Position Stand. Resistance Training Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults: An Overview of Reviews",
            url = "https://pubmed.ncbi.nlm.nih.gov/41843416/",
            publishedDate = "2026",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.PERIODIZATION,
            statement =
                "Muscle hypertrophy was enhanced by higher volumes (≥10 sets/wk) and eccentric overload. ",
            articleTitle = "American College of Sports Medicine Position Stand. Resistance Training Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults: An Overview of Reviews",
            url = "https://pubmed.ncbi.nlm.nih.gov/41843416/",
            publishedDate = "2026",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "Il primo può coincidere con: una settimana di scarico attivo ogni 3-5 settimane di " +
                "allenamento. ",
            articleTitle = "Programmare la fase di scarico",
            url = "https://www.projectinvictus.it/programmare-fase-scarico/",
            publishedDate = "2017-02-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "È a questo punto che entra il gioco il periodo di scarico, anche chiamato settimana di " +
                "scarico , dove si opterà per una riduzione del carico di allenamento della durata che " +
                "varia generalmente da 2 a 6 settimane , mediamente 4. ",
            articleTitle = "Mesocicli e settimana di scarico: facciamo chiarezza",
            url = "https://www.projectinvictus.it/mesocicli-di-scarico-perche-farli/",
            publishedDate = "2015-02-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "Most coaches reported decreasing volume during all deloading periods (taper: 89.1% " +
                "[163/183 respondents], competitive season: 70.4% [133/189], tournament: 84.1% [74/88], " +
                "travel: 74.6% [88/118]), with the most common reduction in RT volume reported as 0-25%. ",
            articleTitle = "Resistance Training Prescription During Planned Deloading Periods: A Survey of Strength and Conditioning Coaches Across Varying Sporting Codes",
            url = "https://pubmed.ncbi.nlm.nih.gov/39446750/",
            publishedDate = "2024",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "Most coaches also decreased intensity during a taper (52.9% [82/155]), tournament (54.8% " +
                "[40/73]), and travel (53.6% [52/97]), with a 0-25% reduction the most common. ",
            articleTitle = "Resistance Training Prescription During Planned Deloading Periods: A Survey of Strength and Conditioning Coaches Across Varying Sporting Codes",
            url = "https://pubmed.ncbi.nlm.nih.gov/39446750/",
            publishedDate = "2024",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "In addition, analysis of the relative importance of each week in predicting changes in " +
                "CMJ height indicated that the VLs during deload or taper weeks were the best predictors " +
                "(10%-17%) of changes in CMJ performance. ",
            articleTitle = "Use of Machine Learning to Model Volume Load Effects on Changes in Jump Performance",
            url = "https://pubmed.ncbi.nlm.nih.gov/31094253/",
            publishedDate = "2020",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "This study assessed the effect of unilateral strength training at 80% one repetition " +
                "maximum and of detraining on bone mineral density (BMD, g/cm-2) and bone mineral content " +
                "(sigma BMC, g) in young women. ",
            articleTitle = "Effects of unilateral strength training and detraining on bone mineral density and content in young women: a study of mechanical loading and deloading on human bones",
            url = "https://pubmed.ncbi.nlm.nih.gov/7922791/",
            publishedDate = "1994",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "Major programming challenges identified were managing fatigue (72%), optimizing training " +
                "stimuli (53%), specificity (50%), and adherence (47%). ",
            articleTitle = "Current Periodization, Testing, and Monitoring Practices of Strength and Conditioning Coaches",
            url = "https://pubmed.ncbi.nlm.nih.gov/40681169/",
            publishedDate = "2025",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "Physical performance changes were primarily identified from testing (90%) but also from " +
                "athlete/coach feedback (76%), monitoring (71%), training data (67%), and performance " +
                "data/statistics (62%). ",
            articleTitle = "Current Periodization, Testing, and Monitoring Practices of Strength and Conditioning Coaches",
            url = "https://pubmed.ncbi.nlm.nih.gov/40681169/",
            publishedDate = "2025",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.DELOAD,
            statement =
                "Pretraining readiness was monitored via conversations (71%), wellness tools (46%), and " +
                "performance devices (31%). ",
            articleTitle = "Current Periodization, Testing, and Monitoring Practices of Strength and Conditioning Coaches",
            url = "https://pubmed.ncbi.nlm.nih.gov/40681169/",
            publishedDate = "2025",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.FATIGUE,
            statement =
                "Se si verifica uno stallo del peso , ci sono due strade che puoi seguire a seconda della " +
                "condizione fisica: se la condizione è buona , aggiungi +5% delle calorie ogni volta che " +
                "si verifica uno stallo. ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FATIGUE,
            statement =
                "E’ importante precisare che se ti alleni 3-4 volte in palestra e ti senti affaticato, " +
                "non sei in sovrallenamento . ",
            articleTitle = "Sovrallenamento: cos’è, sintomi e rimedi",
            url = "https://www.projectinvictus.it/sovrallenamento/",
            publishedDate = "2024-11-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FATIGUE,
            statement =
                "La singola posta alla fine di ogni allenamento sarà circa al 90% del massimale reale e " +
                "la affronterete dopo aver accumulato tutta la fatica dell’allenamento,quindi sarà " +
                "percepita come un peso quasi massimale. ",
            articleTitle = "Record panca piana: ecco come migliorarlo",
            url = "https://www.projectinvictus.it/record-panca-piana-ecco-come-migliorarlo/",
            publishedDate = "2017-10-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FATIGUE,
            statement =
                "La proposta nell’applicazione dell’endurance muscolare per le scrollate inverse è quella " +
                "di eseguire le ripetizioni alla massima fatica (cedimento) secondo quanto permesso dal " +
                "peso corporeo, e impostare pause di 30 secondi tra le serie multiple. ",
            articleTitle = "Scrollate inverse per il riequilibrio scapolare",
            url = "https://www.projectinvictus.it/scrollate-inverse-per-riequilibrio-scapolare/",
            publishedDate = "2014-11-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FATIGUE,
            statement =
                "Ovvero, in teoria, un carico che corrisponde al 85% del massimale permette all’atleta di " +
                "compiere 5 ripetizioni, anche se alla fine sarà piuttosto affaticato. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 3)",
            url = "https://www.projectinvictus.it/chiarimenti-incomprensioni-sullintensita-nellesercizio-i-pesi-parte-3/",
            publishedDate = "2014-10-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FATIGUE,
            statement =
                "Basare l’allenamento sulle percentuali può essere fuorviante quando quello che dovrebbe " +
                "essere un carico massimale del 90% è in realtà inferiore o superiore a causa delle " +
                "variazioni della forma fisica o dello stato di affaticamento [4]. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 3)",
            url = "https://www.projectinvictus.it/chiarimenti-incomprensioni-sullintensita-nellesercizio-i-pesi-parte-3/",
            publishedDate = "2014-10-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "Stripping : prevede il raggiungimento del cedimento concentrico con un determinato " +
                "carico per poi, nel minor tempo possibile, effettuare un “drop” pari solitamente al " +
                "5-15% del carico, per raggiungere un secondo cedimento concentrico, e così via per il " +
                "numero di volte prefissate (almeno 3). ",
            articleTitle = "Tecniche intensità nel bodybuilding",
            url = "https://www.projectinvictus.it/tecniche-intensita-bodybuilding/",
            publishedDate = "2025-08-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "In alcuni esercizi può essere utile un fermo di 1-2 secondi prima della fase concentrica " +
                "oppure una fase isometrica in determinati punti del ROM – utile, ad esempio, per " +
                "enfatizzare il massimo accorciamento e lo stretch muscolare. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "L’ obiettivo di questo tipo di piramidale è quello di andare ad eseguire una o più " +
                "ripetizioni con carichi pesanti (in questo caso il 100 % dell’1RM) dopo un affaticamento " +
                "dovuto ad una scalata di carico. ",
            articleTitle = "Allenamento piramidale",
            url = "https://www.projectinvictus.it/allenamento-piramidale/",
            publishedDate = "2023-03-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "Il doppio drop set prevede un ulteriore calo del 20 % del carico dal primo drop set. ",
            articleTitle = "Pullover manubrio: esecuzione, muscoli coinvolti, scheda",
            url = "https://www.projectinvictus.it/pullover-manubrio/",
            publishedDate = "2023-03-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "Ad esempio, potresti utilizzare un rep range attorno alle 15 ripetizioni e per le " +
                "isometrie partire dai 20” e salire modulando di settimana in settimana aumentando ad " +
                "esempio di 5”. ",
            articleTitle = "Esercizi e allenamento adduttori",
            url = "https://www.projectinvictus.it/esercizi-adduttori/",
            publishedDate = "2023-03-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "Nel Giorno 2 andrai ad inserire una tecnica di intensità, lavorando prima con un singolo " +
                "drop set del 20% ovvero alla fine delle prime 16 ripetizioni, calerai il carico del 20% " +
                "e continuerai ad eseguire gli affondi fino al cedimento. ",
            articleTitle = "Affondi: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/affondi/",
            publishedDate = "2023-01-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "Non solo, ma il carico potrà essere calato di un ulteriore 10-25% per continuare a " +
                "sfinire le fibre rimaste, in questo caso non si parla più di doppio Drop Set ma di " +
                "triplo Drop Set . ",
            articleTitle = "Drop set bodybuilding: cos’è e come si fa",
            url = "https://www.projectinvictus.it/drop-set-bodybuilding/",
            publishedDate = "2022-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "Una volta raggiunte circa 8 ripetizioni dinamiche e almeno 20” di isometria si può " +
                "passare alla versione successiva. ",
            articleTitle = "Dragon flag",
            url = "https://www.projectinvictus.it/dragon-flag/",
            publishedDate = "2022-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "Si consiglia di raggiungere almeno 6 ripetizioni dinamiche e almeno 20” di isometria. ",
            articleTitle = "Dragon flag",
            url = "https://www.projectinvictus.it/dragon-flag/",
            publishedDate = "2022-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "La preparazione per la monoserie dell’Heavy Duty ed il fatto che si inseriscano spesso " +
                "superserie o giant set, lo possiamo anche considerare come se fossero 3 – 6 serie, ma " +
                "rimanendo sotto del 50 % per ottenere da natural i migliori benefici. ",
            articleTitle = "Heavy Duty: principi e scheda di allenamento",
            url = "https://www.projectinvictus.it/heavy-duty-allenamento/",
            publishedDate = "2022-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TECHNIQUE,
            statement =
                "Nell’ultimo set si inserisce un dropset: si scala il peso del 20% e si prosegue fino a " +
                "cedimento. ",
            articleTitle = "La periodizzazione nel bodybuilding",
            url = "https://www.projectinvictus.it/periodizzazione-nel-bodybuilding/",
            publishedDate = "2018-03-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Consiglio di inserire 3-5 serie allenanti di Pulley basso a settimana , progredendo in " +
                "un primo momento sul carico, nel rispetto delle tecnica di esecuzione, per poi virare " +
                "l’attenzione sul volume. ",
            articleTitle = "Pulley basso: tutorial ed esecuzione corretta",
            url = "https://www.projectinvictus.it/pulley-basso/",
            publishedDate = "2025-04-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Il carico che utilizzi sarà una conseguenza del numero di ripetizioni eseguito e " +
                "dell’intensità di sforzo, tempo sotto tensione scelto, range di movimento ecc… La pressa " +
                "si presta bene ai più svariati lavori, indicativamente dalle 5-6 ripetizioni fino alle " +
                "30 ripetizioni . ",
            articleTitle = "Leg press in palestra e nel bodybuilding: esecuzione corretta",
            url = "https://www.projectinvictus.it/leg-press-bodybuilding/",
            publishedDate = "2024-06-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "In alcuni esercizi può essere utile un fermo di 1-2 secondi prima della fase concentrica " +
                "oppure una fase isometrica in determinati punti del ROM – utile, ad esempio, per " +
                "enfatizzare il massimo accorciamento e lo stretch muscolare. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Una volta raggiunte almeno 12 ripetizioni con esecuzione impeccabile, aumenta la " +
                "difficoltà dei V-push up posizionando i piedi su un rialzo come uno step, fino ad " +
                "arrivare ad un box. ",
            articleTitle = "V-push up: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/v-push-up/",
            publishedDate = "2022-12-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "In ambito Bodybuilding, quello che ti interessa è mantenere un buon setup durante tutta " +
                "l’esecuzione dell’esercizio, senza forzature estreme e in linea di massima potresti " +
                "pensare di mettere il dito indice sugli 81 cm. ",
            articleTitle = "Panca piana: come eseguirla",
            url = "https://www.projectinvictus.it/la-panca-piana-nel-bodybuilding/",
            publishedDate = "2022-11-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "La sua esecuzione prevede di trovare un carico con difficoltà 9/10 (o comunque molto " +
                "impegnativo) con cui eseguirai un numero prestabilito di ripetizioni, solitamente in un " +
                "rep range medio-alto, tra le 10 e le 15 ripetizioni . ",
            articleTitle = "Drop set bodybuilding: cos’è e come si fa",
            url = "https://www.projectinvictus.it/drop-set-bodybuilding/",
            publishedDate = "2022-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Quando termini la loro esecuzione, solitamente si ha un “drop” del carico , ovvero " +
                "calerai il carico di un 10-25% e andrai ad eseguire il maggior numero di ripetizioni che " +
                "riuscirai ad eseguire con quel nuovo carico. ",
            articleTitle = "Drop set bodybuilding: cos’è e come si fa",
            url = "https://www.projectinvictus.it/drop-set-bodybuilding/",
            publishedDate = "2022-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Una volta raggiunte le 15-20 ripetizioni con la schiena al muro, allena anche le " +
                "versioni a pancia a muro per poi passare alle versioni full rom. ",
            articleTitle = "Planche: esecuzione, propedeutiche e muscoli coinvolti",
            url = "https://www.projectinvictus.it/planche-tutorial-ed-esecuzione/",
            publishedDate = "2022-03-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Dividi, per esempio, queste 10-12 serie in: 3-4 serie : ESERCIZIO VERTICALE FULL ROM o " +
                "ESERCIZIO spinta verticale ROM PARZIALE con manubri /bilanciere. ",
            articleTitle = "Come allargare le spalle? Esercizi efficaci",
            url = "https://www.projectinvictus.it/come-allargare-spalle/",
            publishedDate = "2021-03-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Se un atleta esegue 4-8 ripetizioni in buffer con un carico che gli permetterebbe " +
                "l’esecuzione di 10 o più ripetizioni ( ≤ 75% 1-RM), egli non si sta allenando nel “ " +
                "range di forza”, e il massimo sviluppo della forza viene penalizzato. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 2)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-2/",
            publishedDate = "2014-06-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Il range ipertrofico ( 6-12 ripetizioni ) è il miglior compromesso tra carichi medi e " +
                "tempi medi sotto tensione ( TUT ). ",
            articleTitle = "Bodybuilding: la guida completa",
            url = "https://www.projectinvictus.it/bodybuilding/",
            publishedDate = "2024-01-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Come indicazione le mani vanno tenute almeno ad una larghezza di 1.5 volte le spalle e " +
                "in caso di ridotta mobilità scapolo-omerale, il consiglio è quello di allargare anche a " +
                "2 volte. ",
            articleTitle = "Lento dietro: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/lento-dietro-lento-avanti/",
            publishedDate = "2022-12-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "Esecuzione corretta del Military press Per eseguire al meglio il Military press con " +
                "bilanciere la presa deve essere poco più larga delle spalle (da 1.2 a 1.5 volte le " +
                "spalle). ",
            articleTitle = "Military Press: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/military-press/",
            publishedDate = "2026-04-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.EXECUTION,
            statement =
                "L’esecuzione corretta prevede di posizionarsi a terra con le mani circa ad una larghezza " +
                "di 1.5 volte le spalle. ",
            articleTitle = "Flessioni per allenare i pettorali",
            url = "https://www.projectinvictus.it/flessioni-pettorali/",
            publishedDate = "2022-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WARMUP,
            statement =
                "Nel Giorno 1 trova un carico (tramite ramping) che ti permetta di eseguire 12 " +
                "ripetizioni a margine 2 dal cedimento. ",
            articleTitle = "Military Press: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/military-press/",
            publishedDate = "2026-04-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WARMUP,
            statement =
                "Si tratta di un ramping , una scalata in pratica, a partire da un carico decisamente " +
                "basso, tecnico, fino a raggiungere il massimo carico che riusciamo ad utilizzare per " +
                "svolgere 5 ripetizioni, il nostro 5RM. ",
            articleTitle = "5×5: di Bill Starr, Reg Park, Stronglift",
            url = "https://www.projectinvictus.it/5x5/",
            publishedDate = "2018-08-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WARMUP,
            statement =
                "Il pre-riscaldamento , la “fase 0” di questo processo, dovrebbe durare intorno ai 5 " +
                "minuti, nei quali si eseguirà un esercizio aerobico a scelta, dipendentemente dalla " +
                "disponibilità di attrezzature e spazio, a intensità moderata (è pur sempre " +
                "riscaldamento!), cui seguirà il riscaldamento specifico. ",
            articleTitle = "Come effettuare il riscaldamento per l’allenamento?",
            url = "https://www.projectinvictus.it/come-effettuare-il-riscaldamento/",
            publishedDate = "2016-05-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Solitamente come attività aerobica per consumare grassi viene consigliata quella a bassa " +
                "intensità, ma non è una proposta valida se sei già ben allenato e sportivo: in questo " +
                "caso, meglio preferire esercizi ad alta intensità di una certa durata (30-60 minuti) . ",
            articleTitle = "Il migliore allenamento per dimagrire",
            url = "https://www.projectinvictus.it/allenamento-per-dimagrire/",
            publishedDate = "2026-05-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Ad esempio, mangiare alimenti ricchi di omega 3 in grandi quantità non ti proteggerà al " +
                "100% da malattie cardiovascolari, perché l’alimentazione è solo uno dei fattori che " +
                "influenza e non l’unico (uno tra i più importante e non modificabile, la genetica ). ",
            articleTitle = "Superfood: quali sono e a cosa servono",
            url = "https://www.projectinvictus.it/superfood/",
            publishedDate = "2024-12-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Se hai poco tempo (max 30′ per 3-5 volte settimana) prediligi l’HIIT in modo da bruciare " +
                "più calorie rispetto ad un’attività moderata. ",
            articleTitle = "Attività cardio per dimagrire: schede ed esercizi",
            url = "https://www.projectinvictus.it/cardio-per-dimagrire/",
            publishedDate = "2024-03-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "1-2 ore per 2-3 volte settimane) puoi optare anche per cardio LISS o MISS. ",
            articleTitle = "Attività cardio per dimagrire: schede ed esercizi",
            url = "https://www.projectinvictus.it/cardio-per-dimagrire/",
            publishedDate = "2024-03-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Le linee guida internazionali per stare in salute consigliano di fare almeno 150 minuti " +
                "di attività cardiovascolare moderata alla settimana, oppure 75 minuti di attività " +
                "intensa . ",
            articleTitle = "Attività cardio per dimagrire: schede ed esercizi",
            url = "https://www.projectinvictus.it/cardio-per-dimagrire/",
            publishedDate = "2024-03-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Puoi sia fare sedute separate (1 giorno cardio e 2 giorni pesi) sia includerle nello " +
                "stesso allenamento, anche facendo circuiti HIIT . ",
            articleTitle = "Dimagrire in palestra: cosa fare",
            url = "https://www.projectinvictus.it/dimagrire-in-palestra/",
            publishedDate = "2023-02-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Sono sufficienti 30′ di camminata veloce al giorno per 5 giorni alla settimana per " +
                "raggiungere la quantità di attività fisica consigliata da OMS e AHA per ridurre il " +
                "rischio di cardiopatie, migliorare la qualità del sonno e il generale benessere mentale. ",
            articleTitle = "Le migliori App Fitness",
            url = "https://www.projectinvictus.it/app-fitness-le-10-migliori/",
            publishedDate = "2022-02-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Studi dimostrano che in un lasso di tempo che va dalle 10 alle 12 settimane, " +
                "l’allenamento HIIT consente di: aumentare il colesterolo buono HDL, ridurre il grasso " +
                "viscerale e totale del corpo, aumentare la VO2 max e ridurre l’ insulino-resistenza. ",
            articleTitle = "Sindrome da ovaio policistico: dieta ed allenamento",
            url = "https://www.projectinvictus.it/sindrome-ovaio-policistico-dieta-allenamento/",
            publishedDate = "2020-08-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "In relazione a questi due parametri, la durata di una seduta di HIIT può avere una " +
                "durata variabile tra 4 e 30 minuti . ",
            articleTitle = "HIIT: High Intensity Interval Training – Che cos’è? Esempi pratici",
            url = "https://www.projectinvictus.it/hiit-high-intensity-interval-training-aspetti-scientifici-e-pratici/",
            publishedDate = "2019-11-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Ovvero: 4 minuti di HIIT effettuati 4 volte a settimana hanno gli stessi benefici di un " +
                "training aerobico prolungato svolto 5 volte a settimana (non solo: i lavori scientifici " +
                "dimostravano che solo il “gruppo Tabata” otteneva un incremento della capacità " +
                "anaerobica). ",
            articleTitle = "HIIT: High Intensity Interval Training – Che cos’è? Esempi pratici",
            url = "https://www.projectinvictus.it/hiit-high-intensity-interval-training-aspetti-scientifici-e-pratici/",
            publishedDate = "2019-11-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Proprio a causa dell’ elevato stress imposto all’ organismo da una seduta di HIIT, " +
                "ritengo sia consigliabile non effettuarlo più di 3-4 volte a settimana (non più di 2-3 " +
                "volte a settimana se si svolge anche un’ altra attività). ",
            articleTitle = "HIIT: High Intensity Interval Training – Che cos’è? Esempi pratici",
            url = "https://www.projectinvictus.it/hiit-high-intensity-interval-training-aspetti-scientifici-e-pratici/",
            publishedDate = "2019-11-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Sia sedute di HIIT (vedi il relativo articolo), sia serie lattacide sopra le 15-20 " +
                "ripetizioni coi pesi (bilancieri, kettlebell , manubri, ecc), aumentano la " +
                "capilarizzazione muscolare, la densità mitocondriale, il volume plasmatico. ",
            articleTitle = "Allenamento aerobico",
            url = "https://www.projectinvictus.it/allenamento-aerobico/",
            publishedDate = "2018-08-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARDIO,
            statement =
                "Tra l’altro si è visto che non ci sono particolari benefici, dal punto di vista " +
                "cardiovascolare, se siamo sedentari ma facciamo palestra (pesi) 3 volte a settimana . ",
            articleTitle = "Camminare/ benefici e se serve per dimagrire",
            url = "https://www.projectinvictus.it/camminare/",
            publishedDate = "2018-07-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Il consiglio per un atleta principiante è di allenarsi almeno 3 volte a settimana. ",
            articleTitle = "Street workout: cos’è, come iniziare e allenamento",
            url = "https://www.projectinvictus.it/street-workout/",
            publishedDate = "2023-03-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Esempio 1 (livello principiante) Ti alleni 3 volte a settimana, sono un principiante (ho " +
                "iniziato da 1 mese) e voglio migliorare la mia composizione corporea . ",
            articleTitle = "Parametri allenanti: quali sono e come impostarli",
            url = "https://www.projectinvictus.it/parametri-allenanti/",
            publishedDate = "2022-10-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Esempio 2 (livello avanzato) Ti alleni 4 volte a settimana, sei un atleta avanzato (ti " +
                "alleni da anni) e vuoi migliorare il tuo fisico e anche le tue prestazioni. ",
            articleTitle = "Parametri allenanti: quali sono e come impostarli",
            url = "https://www.projectinvictus.it/parametri-allenanti/",
            publishedDate = "2022-10-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Se sei intermedio e conosci il movimento, ma devi correggere difetti tecnici, fare un " +
                "passo indietro e diminuire il peso per correggere i difetti va benissimo, a patto che " +
                "utilizzi un carico che renda la serie percepita come allenante: un range che spazia " +
                "dalle 5-6 alle 8 ripetizioni . ",
            articleTitle = "Box Squat: come si esegue",
            url = "https://www.projectinvictus.it/box-squat/",
            publishedDate = "2021-04-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Soggetto intermedio Soggetto in grado di eseguire il vacuum in piedi con mani dietro la " +
                "nuca per 10-15 secondi per 2-3 serie da 6 ripetizioni. ",
            articleTitle = "Vacuum addominale: esecuzione corretta",
            url = "https://www.projectinvictus.it/vacuum-addominale/",
            publishedDate = "2021-01-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Il soggetto intermedio può considerare le 100-200 kcal/die. ",
            articleTitle = "Macronutrienti: cosa sono e in quali alimenti si trovano?",
            url = "https://www.projectinvictus.it/macronutrienti-nella-dieta-cosa-dobbiamo-sapere/",
            publishedDate = "2021-01-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Iniziate eseguendo 10 ripetizioni Rinforzo del muscolo multifido: livello semplice Il " +
                "livello intermedio prevede, dalla stessa posizione e nel medesimo assetto posturale, di " +
                "sollevare in simultanea la coscia e il braccio controlaterale, portandoli entrambi " +
                "paralleli al pavimento. ",
            articleTitle = "Esercizi per il mal di schiena",
            url = "https://www.projectinvictus.it/esercizi-mal-di-schiena/",
            publishedDate = "2018-03-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Per progredire bene nelle trazioni è necessario allenarle minimo due volta a settimana " +
                "(frequenza ottimale nel principiante), un intermedio può eseguirle 3 volte, un avanzato " +
                "anche 4-5 (dipende da quel’è la vostra alzata target). ",
            articleTitle = "Migliora le tue trazioni col giro della morte",
            url = "https://www.projectinvictus.it/migliora-tue-trazioni-col-giro-morte/",
            publishedDate = "2014-09-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.LEVEL,
            statement =
                "Chi migliora il suo massimale in 2 mesi con l’1xmax non è un atleta avanzato in " +
                "quell’alzata, per atleta avanzato intendiamo una persona che dedica da almeno 4 anni 3 " +
                "giorni d’allenamento a settimana a quell’esercizio specifico. ",
            articleTitle = "Impariamo a programmare alcuni allenamenti da palestra (1xMax)",
            url = "https://www.projectinvictus.it/impariamo-a-programmare-part-1-1xmax/",
            publishedDate = "2013-10-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "La letteratura scientifica mostra che l’ipertrofia si sviluppa tra le 10 e le 25 serie " +
                "allenanti a settimana . ",
            articleTitle = "RPE: cos’è, misura, utilizzo",
            url = "https://www.projectinvictus.it/rpe-palestra/",
            publishedDate = "2026-09-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Quanti grassi al giorno nel bodybuilding La quantità consigliata (25-30%) è consigliata " +
                "anche a chi fa bodybuilding, anche se c’è qualche differenza a seconda che tu sia in " +
                "fase di definizione o di massa. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Durante la fase di ricomposizione conviene tenere un volume leggermente inferiore " +
                "intorno alle 12 – 20 serie a settimana . ",
            articleTitle = "Ricomposizione corporea: la guida completa",
            url = "https://www.projectinvictus.it/ricomposizione-corporea/",
            publishedDate = "2026-08-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Più sei sedentario, poco attivo e con poca massa muscolare più puoi avvicinarti ai 2 " +
                "g/kg. ",
            articleTitle = "Fabbisogno glucidico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-glucidico-carboidrati/",
            publishedDate = "2026-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "In genere si impostano prima proteine e grassi , destinando ai carboidrati le calorie " +
                "rimanenti: Per le proteine , negli sportivi in normo o ipercalorica si consiglia un " +
                "apporto tra 1,6, arrivando a 2-2,2 g/kg in fase di dimagrimento per aiutare a preservare " +
                "la massa muscolare. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Per i carboidrati , se sei attivo e ti alleni, soprattutto se ha una buona massa " +
                "muscolare, può beneficiare di un maggiore apporto di carboidrati, che mediamente varia " +
                "tra 3 e 7 g/kg. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Come impostare la dieta per la massa muscolare Se provieni da una fase di Cut, la prima " +
                "cosa da fare è tornare in normocalorica in modo graduale e stabilizzarti a queste " +
                "calorie per 2-3 settimane , in modo da tornare in una situazione fisiologica e pronta ad " +
                "instaurare una fase anabolica. ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Dieta per aumentare la massa muscolare senza ingrassare Prima di tutto, per evitare di " +
                "ingrassare troppo è bene evitare di iniziare una fase di massa se hai percentuale di " +
                "massa grassa superiore a circa l’11% per gli uomini e 16% per le donne . ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Come allenare le gambe e i quadricipiti in palestra I quadricipiti hanno bisogno almeno " +
                "di 8/10 serie allenanti settimanali per un mantenimento della massa muscolare. ",
            articleTitle = "I 10 migliori esercizi per allenare le gambe",
            url = "https://www.projectinvictus.it/allenamento-gambe-quadricipiti/",
            publishedDate = "2025-03-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Alcuni punti fondamentali da tenere in considerazione nell’allenamento delle spalle per " +
                "farne crescere la massa muscolare sono: I set di allenamento devono stare " +
                "prevalentemente tra il 60 e l’80% del massimale. ",
            articleTitle = "I migliori esercizi per le spalle",
            url = "https://www.projectinvictus.it/esercizi-spalle-la-guida-completa/",
            publishedDate = "2025-01-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Il r ange di ripetizioni più comodo per l’ipertrofia è tra le 8 e le 20 ripetizioni. ",
            articleTitle = "I migliori esercizi per le spalle",
            url = "https://www.projectinvictus.it/esercizi-spalle-la-guida-completa/",
            publishedDate = "2025-01-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Possiamo, quindi, parlare di periodi di mini-definizione, che possono durare 2-6 " +
                "settimane a seconda del soggetto. ",
            articleTitle = "Mini cut: cos’è e come impostarlo in fase di massa",
            url = "https://www.projectinvictus.it/mini-cut/",
            publishedDate = "2024-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Isolate Per definizione la forma con il maggiore grado di purezza, arrivando a " +
                "percentuali proteiche fino al 90-95%, quantità di lattosio e grassi insignificanti e " +
                "minore presenza di minerali. ",
            articleTitle = "Le 6 migliori proteine in polvere",
            url = "https://www.projectinvictus.it/migliori-proteine-in-polvere/",
            publishedDate = "2024-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "In anni più recenti è emerso che digiunare anche fino a 16-20 ore al giorno non agisce " +
                "negativamente sulla massa muscolare (9,10), mettendo in discussione la necessità di " +
                "assumere proteine per “coprire” il digiuno notturno, data l’assenza di rischio " +
                "catabolico. ",
            articleTitle = "Le 6 migliori proteine in polvere",
            url = "https://www.projectinvictus.it/migliori-proteine-in-polvere/",
            publishedDate = "2024-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Più è abbondante il grasso corporeo, meno massa muscolare c’è, più la persona è poco " +
                "attiva durante la giornata e più sarà necessario tempo per ottenere risultati, anche " +
                "considerando il range prima consigliato di 0.5-1 kg a settimana. ",
            articleTitle = "Fisico asciutto: come ottenerlo",
            url = "https://www.projectinvictus.it/dieta-asciugare-fisico/",
            publishedDate = "2024-04-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Una volta terminata la fase di definizione, segui una dieta normocalorica di " +
                "mantenimento per 2-3 settimane e poi continua con la fase di massa . ",
            articleTitle = "Definizione in palestra: come definire i muscoli",
            url = "https://www.projectinvictus.it/definizione-palestra/",
            publishedDate = "2024-01-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Fase di massa Per aumentare la massa muscolare è necessario essere in surplus calorico , " +
                "mediamente tra le 300 e 500 kcal giornaliere rispetto all’apporto calorico di " +
                "mantenimento. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Fase di definizione Per rendere visibile la muscolatura dopo una fase di massa è " +
                "necessario essere in deficit calorico per diversi mesi: in media pari a 300 kcal per le " +
                "donne e a 500 kcal per gli uomini rispetto all’apporto calorico giornaliero di " +
                "mantenimento. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Il bilancio calorico Per la massa muscolare dobbiamo assumere un 10-15% in più delle " +
                "nostre calorie giornaliere. ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            publishedDate = "2022-12-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "La persona viene spinta a credere, visto che un pasto proteico ha il massimo effetto " +
                "anabolico per 4-6 ore, di dover continuamente mangiare, per evitare la perdita della " +
                "massa muscolare . ",
            articleTitle = "Catabolismo muscolare: cos’è, cause e rimedi",
            url = "https://www.projectinvictus.it/catabolismo-muscolare/",
            publishedDate = "2022-09-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Soggetti sedentari dopo i 30-40 anni iniziano a perdere lo 0,5 – 1% di massa muscolare " +
                "all’anno. ",
            articleTitle = "Massa magra: cos’è, valori ottimali, calcolo",
            url = "https://www.projectinvictus.it/massa-magra/",
            publishedDate = "2022-08-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "La quota ottimale di proteine per la massa muscolare è di 1,4 – 2,2 g / kg al giorno . ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "È pertanto fondamentale essere sicuri di assumere più calorie del proprio TDEE , qual " +
                "ora si vuole aumentare la massa muscolare , introducendo spesso un quantitativo " +
                "superiore a 300 – 500 kcal rispetto al fabbisogno calorico giornaliero . ",
            articleTitle = "Somatotipo: endomorfo, mesomorfo, ectomorfo",
            url = "https://www.projectinvictus.it/somatotipo/",
            publishedDate = "2022-06-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "La dose attualmente consigliata di acido fosfatidico per aumentare la massa muscolare è " +
                "di 750 mg / die ; dosi più basse invece, non hanno mostrato benefici significativi. ",
            articleTitle = "Acido fosfatidico: cos’è, a cosa serve e quando assumerlo?",
            url = "https://www.projectinvictus.it/acido-fosfatidico-integratore-bodybuilding/",
            publishedDate = "2022-02-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "In base ad alcuni modesti approfondimenti, sembra che il supplemento di HMB permetta di " +
                "guadagnare fino a 0,5-1,0 kg di massa muscolare in 3-6 settimane di allenamento . ",
            articleTitle = "HMB integratore: cos’è, a cosa serve, funziona?",
            url = "https://www.projectinvictus.it/hmb-integratore/",
            publishedDate = "2022-01-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Per massimizzare gli adattamenti muscolari (e l’ipertrofia) è consigliato assumere " +
                "0.4-0.5 g di proteine/kg sia 1-2 ore prima l’allenamento (tramite cibo solido o " +
                "integrazione) sia dopo. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Per definizione sono la forma che ha il maggior grado di purezza : la percentuale " +
                "proteica è molto alta (90-95%), mentre tutte le altre componenti (lattosio, grassi, sali " +
                "minerali) sono praticamente assenti – le proteine risultano quindi “isolate” rispetto " +
                "alle altre sostanze presenti nel latte. ",
            articleTitle = "Proteine isolate: benefici e controindicazioni",
            url = "https://www.projectinvictus.it/proteine-isolate/",
            publishedDate = "2021-04-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "La dose solitamente raccomandata è di 25-30 g , in quanto sono sufficienti a garantire " +
                "la massima sintesi proteica al fine di mantenere o incrementare la massa muscolare. ",
            articleTitle = "Proteine isolate: benefici e controindicazioni",
            url = "https://www.projectinvictus.it/proteine-isolate/",
            publishedDate = "2021-04-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Proprio in merito all’ apporto proteico , per chi ha un’assunzione proteica inferiore a " +
                "1,2 g per kg di peso corporeo, è bene che aumenti l’apporto proteico (fino ad anche 1,6 " +
                "g/kg) per preservare o aumentare la massa muscolare. ",
            articleTitle = "Insulino resistenza, sintomi e rimedi: cos’è e come ridurla?",
            url = "https://www.projectinvictus.it/insulino-resistenza-sintomi-e-rimedi/",
            publishedDate = "2019-10-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Attività contro resistenze con un carico inferiore al 30% del massimale sono definite di " +
                "resistenza, mentre quando il carico è superiore si parla di forza (prima forza " +
                "resistente e poi forza massimale). ",
            articleTitle = "Fibrocellula muscolare ed allenamento",
            url = "https://www.projectinvictus.it/fibrocellula-muscolare/",
            publishedDate = "2018-04-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "La stimolazione della sintesi proteica da parte del nucleo cellulare è data da range " +
                "ipertrofici molto ampi, sopra al 40% della forza massimale (l’esercizio in questo caso " +
                "deve essere protratto fino a cedimento muscolare ). ",
            articleTitle = "Fibrocellula muscolare ed allenamento",
            url = "https://www.projectinvictus.it/fibrocellula-muscolare/",
            publishedDate = "2018-04-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Partendo dal presupposto che storicamente il range tra le 8 e le 12 ripetizioni è " +
                "considerato quello ottimale per l’ ipertrofia , cerchiamo di avere un quadro più chiaro " +
                "su questa questione. ",
            articleTitle = "Ipertrofia e forza: le logiche che li correlano",
            url = "https://www.projectinvictus.it/ipertrofia-forza/",
            publishedDate = "2016-12-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Se cercate l’IPERTROFIA dovete incentrare una parte cospicua delle vostre sedute con " +
                "carichi che vi permettano di fare da 8 a 12 ripetizioni . ",
            articleTitle = "Ipertrofia e forza: le logiche che li correlano",
            url = "https://www.projectinvictus.it/ipertrofia-forza/",
            publishedDate = "2016-12-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Questo significa che nei programmi tipici dell’ipertrofia, che normalmente prevedono " +
                "recuperi tra le serie di 60-90 secondi[45], se lo sforzo viene portato al cedimento il " +
                "numero di ripetizioni massime cala ad ogni serie. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 2)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-2/",
            publishedDate = "2014-06-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Nei programmi di forza massimale questo effetto è ancora meno pronunciato, grazie a " +
                "tempi di recupero tra le serie più lunghi[47], tra 3 e 5 minuti, che permettono un " +
                "maggiore recupero e mantenimento della forza[45]. ",
            articleTitle = "Chiarimenti e incomprensioni sull’intensità nell’esercizio con i pesi. (Parte 2)",
            url = "https://www.projectinvictus.it/chiarimenti-e-incomprensioni-sullintensita-nellesercizio-con-i-pesi-parte-2/",
            publishedDate = "2014-06-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.GOAL_TRAIN,
            statement =
                "Oltre a potenziare una dieta ipocalorica di ricomposizione, una dose elevata di BCAA " +
                "durante gli allenamenti (30-40 g) tende a ridurre significativamente il DOMS. ",
            articleTitle = "BCAA sempre e comunque utili?",
            url = "https://www.projectinvictus.it/bcaa-sempre-e-comunque-utili/",
            publishedDate = "2014-03-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_TRAIN,
            statement =
                "Se il dolore è insorto il giorno stesso o il giorno dopo un attività manuale o " +
                "un’attività fisica intensa e anomala è probabile che sia correlato a un sovraccarico del " +
                "muscolo stesso e sarà sufficiente un riposo di 3-4 giorni per attenuare la " +
                "sintomatologia. ",
            articleTitle = "Brachioradiale: anatomia, esercizi, dolori",
            url = "https://www.projectinvictus.it/brachioradiale/",
            publishedDate = "2025-02-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_TRAIN,
            statement =
                "Questo contiene 5 mini box con varietà di barrette, zuppe, bibite e supplementi da " +
                "assumere Controindicazioni ed effetti collaterali della Dieta Mima Digiuno Alcune " +
                "persone accusano una sensazione di debolezza durante i giorni della DMD. ",
            articleTitle = "Dieta Mima Digiuno: come funziona, benefici e cosa mangiare",
            url = "https://www.projectinvictus.it/dieta-mima-digiuno/",
            publishedDate = "2024-07-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_TRAIN,
            statement =
                "Una volta raggiunte 4-6 ripetizioni per almeno 3-4 serie potrai provare in sicurezza il " +
                "tuo primo muscle up agli anelli. ",
            articleTitle = "Muscle up agli anelli: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/tutorial-muscle-up-agli-anelli/",
            publishedDate = "2022-03-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_TRAIN,
            statement =
                "Si pensi che questi, a braccio abdotto a 90°, esprimono forze di compressione " +
                "sull’articolazione della spalla pari a 80-90% del peso corporeo. ",
            articleTitle = "Muscolo infraspinato: anatomia e funzioni",
            url = "https://www.projectinvictus.it/infraspinato-anatomia-e-funzioni/",
            publishedDate = "2021-03-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_TRAIN,
            statement =
                "Il dolore lombare aspecifico di solito si risolve spontaneamente entro 2-6 settimane , " +
                "con una completa risoluzione dei sintomi. ",
            articleTitle = "Mal di schiena: cause, sintomi e rimedi efficaci",
            url = "https://www.projectinvictus.it/mal-di-schiena-cause-sintomi-e-rimedi-efficaci/",
            publishedDate = "2021-01-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_TRAIN,
            statement =
                "Si va dal doloretto al ginocchio che rompe le balle solo 2-3 giorni all’infortunio dal " +
                "quale recuperare non sarà poi così semplice e veloce, sempre per colpa degli anni. ",
            articleTitle = "Lo squat pesante dopo i 40 anni: quando l’età non conta, anche se conta.",
            url = "https://www.projectinvictus.it/lo-squat-pesante-dopo-i-40-anni/",
            publishedDate = "2018-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_TRAIN,
            statement =
                "3) Esercizi per il rinforzo dei muscoli extrarotatori e stabilizzatori della scapola: " +
                "OPPURE 4) Miglioramento della flessibilità della capsula: eseguire 3 serie di 45″ di " +
                "tenuta di Cross Body Stretch e Sleeper’s Stretch ( solo se questo non evoca dolore ). ",
            articleTitle = "Riabilitazione e prevenzione della spalla: impingement sub-acromiale",
            url = "https://www.projectinvictus.it/riabilitazione-e-prevenzione-della-spalla-impingement-sub-acromiale/",
            publishedDate = "2015-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_TRAIN,
            statement =
                "Nel momento in cui il dolore si attenua iniziare con dei blandi esercizi di allungamento " +
                "statico che verranno intensificati nelle settimane a seguire (dalla 2/3 settimana in " +
                "poi). ",
            articleTitle = "Riabilitazione e prevenzione della spalla: impingement sub-acromiale",
            url = "https://www.projectinvictus.it/riabilitazione-e-prevenzione-della-spalla-impingement-sub-acromiale/",
            publishedDate = "2015-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Bisogna, però, differenziare la quantità dei grassi a seconda della loro qualità: i " +
                "grassi saturi vanno limitati al massimo al 10% del tuo fabbisogno calorico giornaliero " +
                "totale. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "In fase di massa , le calorie sono più alte e quindi anche i grassi potranno essere " +
                "rialzati, sempre stando nel range 20-35% o alzando al massimo al 40%. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Indicando un range più ampio, tra le 150 e le 500 kcal . ",
            articleTitle = "Calorie bruciate in palestra facendo pesi e bodybuilding",
            url = "https://www.projectinvictus.it/calorie-palestra-bodybuilding/",
            publishedDate = "2026-08-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Sapendo che 1 g di carboidrati = 4 kcal, dovrà assumere 361 g di carboidrati al giorno " +
                "(4,5 g/kg per il soggetto in questione). ",
            articleTitle = "Fabbisogno glucidico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-glucidico-carboidrati/",
            publishedDate = "2026-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "In genere si impostano prima proteine e grassi , destinando ai carboidrati le calorie " +
                "rimanenti: Per le proteine , negli sportivi in normo o ipercalorica si consiglia un " +
                "apporto tra 1,6, arrivando a 2-2,2 g/kg in fase di dimagrimento per aiutare a preservare " +
                "la massa muscolare. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Se 3 allenamenti a settimana sono sufficienti per notare cambiamenti della composizione " +
                "corporea , considera che poi ogni allenamento aggiunto contribuisce a creare una " +
                "maggiore spesa energetica, specialmente se ogni sessione allenante consuma almeno 300 " +
                "kcal. ",
            articleTitle = "Il migliore allenamento per dimagrire",
            url = "https://www.projectinvictus.it/allenamento-per-dimagrire/",
            publishedDate = "2026-05-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "In base alle kcal consumate è possibile stimare l’intervallo idrico di riferimento " +
                "secondo cui un adulto dovrebbe introdurre almeno 1 mL di acqua per ogni kcal alimentare " +
                "introdotta nel corso della giornata. ",
            articleTitle = "Quanta acqua bere al giorno",
            url = "https://www.projectinvictus.it/quanta-acqua-bere-al-giorno/",
            publishedDate = "2026-02-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "L’indicazione delle calorie da assumere solo a colazione non è sufficiente per garantire " +
                "un effettivo dimagrimento: puoi ingerire 100 kcal o 500 kcal di mattina e dimagrire o " +
                "ingrassare in entrambi i casi. ",
            articleTitle = "Colazione per dimagrire: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-per-dimagrire/",
            publishedDate = "2025-10-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "L’ Organizzazione Mondiale della Sanit à (OMS) consiglia di limitare l’ assunzione di " +
                "zuccheri liberi a meno del 10% delle calorie totali giornaliere . ",
            articleTitle = "Il fruttosio fa male?",
            url = "https://www.projectinvictus.it/fruttosio-fa-male/",
            publishedDate = "2025-10-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Scopri il tuo fabbisogno giornaliero (reale) e da lì fai un taglio calorico di 500 kcal " +
                "nei maschi e di 350 kcal nelle femmine. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Inoltre, anche se olio, frutta secca, avocado, salmone sono fonti “buone” di grassi, " +
                "sono comunque da limitare perché le calorie dei grassi sono sempre 9 kcal/grammo a " +
                "prescindere dall’alimento e che facciano bene o male alla salute. ",
            articleTitle = "Dieta per avere gli addominali",
            url = "https://www.projectinvictus.it/dieta-per-addominali/",
            publishedDate = "2025-08-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Questo stato di “carenza energetica volontaria” (e necessaria) non dev’essere troppo " +
                "marcata ma moderata: – 350-500 kcal al giorno rispetto al fabbisogno, in modo da poter " +
                "essere sostenibile nel tempo e perdere grasso piuttosto che massa magra. ",
            articleTitle = "Non riesco a dimagrire: rimedi",
            url = "https://www.projectinvictus.it/non-riesco-a-dimagrire/",
            publishedDate = "2025-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Una condizione di bassa disponibilità energetica (<30 kcal/kg massa magra) è da evitare " +
                "sia per la salute sia per la perdita di peso. ",
            articleTitle = "Non riesco a dimagrire: rimedi",
            url = "https://www.projectinvictus.it/non-riesco-a-dimagrire/",
            publishedDate = "2025-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Se volessimo sapere quante di queste 600 kcal provengono dai grassi bisogna considerare " +
                "il quoziente respiratorio . ",
            articleTitle = "Corsa brucia grassi per dimagrire",
            url = "https://www.projectinvictus.it/corsa-brucia-grassi/",
            publishedDate = "2025-06-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Un grammo di grasso nel nostro corpo sono 7 kcal (non 9 kcal come nei grassi alimentari) " +
                "e otteniamo quindi 17 g di grasso consumati ogni 10 km. ",
            articleTitle = "Corsa brucia grassi per dimagrire",
            url = "https://www.projectinvictus.it/corsa-brucia-grassi/",
            publishedDate = "2025-06-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Questi numeri valgono se la persona prima di mettersi a dieta avevamo un buon TDEE " +
                "(almeno intorno alle 30 kcal/kg di peso corporeo), ovvero assumeva un buon quantitativo " +
                "di calorie. ",
            articleTitle = "Dieta ferrea per dimagrire",
            url = "https://www.projectinvictus.it/dieta-ferrea/",
            publishedDate = "2025-05-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Un altro modo usato per garantire una buona idratazione è di consumare almeno 1-1.5 L di " +
                "acqua ogni 1000 kcal . ",
            articleTitle = "Bere tanta acqua per dimagrire",
            url = "https://www.projectinvictus.it/bere-acqua-fa-dimagrire/",
            publishedDate = "2025-05-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "In linea generale, ciascuno spuntino dovrebbe apportare non più del 5% dell’energia " +
                "totale giornaliera (200 – 300 kcal). ",
            articleTitle = "10 spuntini sani se sei a dieta (o meno)",
            url = "https://www.projectinvictus.it/spuntini-sani/",
            publishedDate = "2025-04-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Come impostare la dieta per la massa muscolare Se provieni da una fase di Cut, la prima " +
                "cosa da fare è tornare in normocalorica in modo graduale e stabilizzarti a queste " +
                "calorie per 2-3 settimane , in modo da tornare in una situazione fisiologica e pronta ad " +
                "instaurare una fase anabolica. ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Se si verifica uno stallo del peso , ci sono due strade che puoi seguire a seconda della " +
                "condizione fisica: se la condizione è buona , aggiungi +5% delle calorie ogni volta che " +
                "si verifica uno stallo. ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Il consumo calorico varia in base al peso corporeo, ma in media consumi 5 – 8 kcal ogni " +
                "10 burpees. ",
            articleTitle = "10 esercizi per dimagrire",
            url = "https://www.projectinvictus.it/esercizi-per-dimagrire/",
            publishedDate = "2025-03-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Per perdere 5 kg serve un deficit complessivo di 35000 kcal . ",
            articleTitle = "Deficit calorico: cos’è e come calcolarlo",
            url = "https://www.projectinvictus.it/deficit-calorico-calcolo/",
            publishedDate = "2025-01-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Con questo deficit ti puoi aspettare di perdere 350-500 g di grasso a settimana ; se " +
                "vuoi dimagrire più rapidamente devi mangiare di meno e/o muoverti di più. ",
            articleTitle = "Dieta ipocalorica per dimagrire: la guida completa",
            url = "https://www.projectinvictus.it/dieta-ipocalorica/",
            publishedDate = "2025-01-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Anzi, i carboidrati sono così fondamentali che devono ricoprire il 45-60% delle calorie " +
                "che assumi ogni giorno e sono importanti anche quando vuoi dimagrire , indifferentemente " +
                "dal momento della giornata in cui li mangi (anche la sera!). ",
            articleTitle = "Il pane fa ingrassare?",
            url = "https://www.projectinvictus.it/il-pane-fa-ingrassare/",
            publishedDate = "2024-10-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Nel secondo caso, bisogna essere consapevoli che sotto una certa soglia calorica (30 " +
                "kcal/kg massa magra) è meglio non scendere. ",
            articleTitle = "Mangio poco ma non dimagrisco",
            url = "https://www.projectinvictus.it/perche-non-dimagrisco-quando-il-metabolismo-si-blocca/",
            publishedDate = "2024-10-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "L’uso del tapis roulant per almeno 3 volte alla settimana, insieme al deficit calorico " +
                "creato con la dieta, ti permetterà di aumentare il dispendio energetico favorendo il " +
                "processo di dimagrimento. ",
            articleTitle = "Tapis roulant per dimagrire",
            url = "https://www.projectinvictus.it/tapis-roulant-per-dimagrire/",
            publishedDate = "2024-10-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Non bisogna trascurare che i centrifugati possono essere più o meno ricchi di calorie, " +
                "possono arrivare anche fino a 150-200 kcal per porzione ! ",
            articleTitle = "Centrifugati dimagranti: quanto peso fanno perdere?",
            url = "https://www.projectinvictus.it/centrifugati-dimagranti/",
            publishedDate = "2024-09-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Questo avviene perché in un’alimentazione normale e sana (60% delle calorie derivanti da " +
                "carboidrati) noi diventiamo dipendenti da essi. ",
            articleTitle = "Saltare la colazione sì o no? Fa bene o fa male?",
            url = "https://www.projectinvictus.it/saltare-la-colazione/",
            publishedDate = "2024-08-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Dal punto di vista calorico non c’è un valore preciso per una colazione ideale, potresti " +
                "assumere dalle 100 Kcal alle 400 Kcal. ",
            articleTitle = "Colazione ideale: la guida completa",
            url = "https://www.projectinvictus.it/colazione-ideale/",
            publishedDate = "2024-08-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Ogni 2-3 settimane se il peso mantiene aggiungi altre 100 calorie o modula il lavoro " +
                "attraverso l’allenamento. ",
            articleTitle = "Dieta donna nel bodybuilding e in palestra",
            url = "https://www.projectinvictus.it/dieta-donne-bodybuilding/",
            publishedDate = "2024-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Non si dovrebbero superare troppo le +300/500 kcal rispetto alle kcal di mantenimento " +
                "onde evitare di ingrassare troppo. ",
            articleTitle = "Dieta donna nel bodybuilding e in palestra",
            url = "https://www.projectinvictus.it/dieta-donne-bodybuilding/",
            publishedDate = "2024-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Partendo dal presupposto che, in linea di massima, questa è una scelta che deve " +
                "pianificare il nutrizionista o il medico , la dieta Longo prevede mediamente di seguire " +
                "un digiuno-semidigiuno fino a 800 kcal/die per 3-5 giorni. ",
            articleTitle = "Dieta Mima Digiuno: come funziona, benefici e cosa mangiare",
            url = "https://www.projectinvictus.it/dieta-mima-digiuno/",
            publishedDate = "2024-07-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "L’ azione dinamica specifica dei lipidi è la più bassa dal 2-5% : questo vuol dire che " +
                "su 100kcal di grassi il corpo impiega solo 2-5kcal per digerirli (ricordiamo che la loro " +
                "assimilazione al contrario è la più lunga). ",
            articleTitle = "Cosa sono le calorie?",
            url = "https://www.projectinvictus.it/caloria/",
            publishedDate = "2024-07-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Se hai poco tempo (max 30′ per 3-5 volte settimana) prediligi l’HIIT in modo da bruciare " +
                "più calorie rispetto ad un’attività moderata. ",
            articleTitle = "Attività cardio per dimagrire: schede ed esercizi",
            url = "https://www.projectinvictus.it/cardio-per-dimagrire/",
            publishedDate = "2024-03-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Imposta un taglio calorico di 500 kcal per gli uomini e 350 per le donne rispetto al " +
                "fabbisogno calorico. ",
            articleTitle = "Dieta della frutta per dimagrire",
            url = "https://www.projectinvictus.it/dieta-della-frutta/",
            publishedDate = "2024-03-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Per trovare il TDEE il metodo migliore è mettere su un’ app conta calorie tutto ma " +
                "proprio tutto quello che mangi per 3-4 settimane. ",
            articleTitle = "3 diete efficaci che funzionano",
            url = "https://www.projectinvictus.it/3-diete-efficaci/",
            publishedDate = "2024-03-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Ma non solo, anche a livello di salute generale e prospettiva di vita, i carboidrati " +
                "dovrebbero occupare il 45-60% del tuo fabbisogno calorico giornaliero. ",
            articleTitle = "Paleo dieta: come funziona e cosa mangiare",
            url = "https://www.projectinvictus.it/la-paleo-dieta/",
            publishedDate = "2024-02-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Per un dimagrimento non troppo drastico e impostando un deficit di 3500 kcal a settimana " +
                "(circa -0,5 kg/settimana), in un mese puoi perdere circa 2 kg . ",
            articleTitle = "Perdere massa grassa in modo efficace",
            url = "https://www.projectinvictus.it/perdere-massa-grassa/",
            publishedDate = "2024-02-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Soggetto Surplus calorico Donna +200 kcal Uomo +300 kcal Non aumentare in modo eccessivo " +
                "l’input calorico: a più calorie introdotte non corrisponde un maggior sviluppo " +
                "muscolare, in quanto la sintesi proteica è comunque limitata. ",
            articleTitle = "Dieta ipercalorica per prendere peso e aumentare la massa magra",
            url = "https://www.projectinvictus.it/dieta-ipercalorica/",
            publishedDate = "2024-01-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Dieta Sirt: come si fa e come funziona La dieta Sirt prevede una prima fase (1 " +
                "settimana) , in cui l’apporto calorico è molto basso, pari a circa 1000 kcal al giorno , " +
                "e che comporta una forte perdita di peso iniziale , che sicuramente dà una forte " +
                "motivazione. ",
            articleTitle = "Dieta Sirt: come funziona e quali cibi mangiare",
            url = "https://www.projectinvictus.it/dieta-sirt/",
            publishedDate = "2024-01-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Molti autori consigliano range tra 1.5 – 2.1 g/kg peso corporeo : eccedendo con le " +
                "calorie ti ritroverai non solo più grosso ma anche più grasso . ",
            articleTitle = "Le proteine fanno ingrassare?",
            url = "https://www.projectinvictus.it/le-proteine-fanno-ingrassare/",
            publishedDate = "2024-01-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Questo viene aiutato da una dieta opportuna, che deve: farti perdere peso in modo " +
                "graduale (0.5-1% del tuo peso corporeo a settimana), non essere drastica ma prevedere un " +
                "deficit calorico moderato, farti assumere sufficienti proteine , non escludere " +
                "carboidrati e grassi, che devono essere presenti. ",
            articleTitle = "Definizione in palestra: come definire i muscoli",
            url = "https://www.projectinvictus.it/definizione-palestra/",
            publishedDate = "2024-01-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Ciò che conta per avere il 90% del risultato è la gestione delle calorie e dei " +
                "macronutrienti. ",
            articleTitle = "Bodybuilding: la guida completa",
            url = "https://www.projectinvictus.it/bodybuilding/",
            publishedDate = "2024-01-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Un grammo di grasso nel nostro corpo equivale a 7 kcal (non 9 kcal come nei grassi " +
                "alimentari) e otteniamo, quindi, 17 g di grasso consumati ogni 10 km . ",
            articleTitle = "Correre per dimagrire: quanto peso perdi?",
            url = "https://www.projectinvictus.it/correre-per-dimagrire/",
            publishedDate = "2023-12-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Il consiglio è quello di aumentare di circa il 10-15% le calorie in entrata. ",
            articleTitle = "Cosa mangiare per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/cosa-mangiare-per-aumentare-la-massa-muscolare/",
            publishedDate = "2023-10-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Evitare di perdere muscolo in ipocalorica, soprattutto se prolungata, è inevitabile ma " +
                "può essere limitato tramite: adeguato apporto proteico, allenamento contro resistenze " +
                "(con i pesi), deficit calorico graduale e non eccessivo (- 300-500 kcal al giorno). ",
            articleTitle = "Proteine in polvere per dimagrire donne",
            url = "https://www.projectinvictus.it/proteine-in-polvere-per-dimagrire-donne/",
            publishedDate = "2023-09-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Ecco alcuni consigli pratici: Consuma una quantità di calorie leggermente inferiore al " +
                "tuo fabbisogno energetico (200-300kcal) per creare un deficit calorico; Scegli cibi " +
                "ricchi di nutrienti : come proteine magre (pollo, tacchino, pesce), cereali integrali " +
                "(farro, orzo, riso), frutta, verdura. ",
            articleTitle = "Come dimagrire le gambe: esercizi, dieta, consigli",
            url = "https://www.projectinvictus.it/come-dimagrire-le-gambe/",
            publishedDate = "2023-09-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Fase di massa Per aumentare la massa muscolare è necessario essere in surplus calorico , " +
                "mediamente tra le 300 e 500 kcal giornaliere rispetto all’apporto calorico di " +
                "mantenimento. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Fase di definizione Per rendere visibile la muscolatura dopo una fase di massa è " +
                "necessario essere in deficit calorico per diversi mesi: in media pari a 300 kcal per le " +
                "donne e a 500 kcal per gli uomini rispetto all’apporto calorico giornaliero di " +
                "mantenimento. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La chiave per prendere peso in modo sano è seguire una dieta ipercalorica , in cui " +
                "aumenti del 10-15% le calorie giornaliere per instaurare un bilancio energetico positivo " +
                ". ",
            articleTitle = "Cosa mangiare per ingrassare in modo sano",
            url = "https://www.projectinvictus.it/cosa-mangiare-per-ingrassare/",
            publishedDate = "2023-07-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Per questo è opportuno inserire dei brevi periodi in cui alzare le calorie e tornare in " +
                "normocalorica (almeno 3 giorni) per poi ricominciare l’ipocalorica, in modo da non " +
                "abituare l’organismo. ",
            articleTitle = "Come sbloccare il metabolismo e riattivarlo?",
            url = "https://www.projectinvictus.it/come-sbloccare-il-metabolismo/",
            publishedDate = "2023-07-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Il bilancio calorico Per la massa muscolare dobbiamo assumere un 10-15% in più delle " +
                "nostre calorie giornaliere. ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            publishedDate = "2022-12-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Per dimagrire bisognerà instaurare un deficit calorico che comprenda il fabbisogno " +
                "nutrizionale giornaliero (compreso il dispendio dell’allenamento) e da questo creare un " +
                "deficit medio intorno alle 500 – 350 kcal (uomo-donna). ",
            articleTitle = "Il CrossFit fa dimagrire?",
            url = "https://www.projectinvictus.it/crossfit-fa-dimagrire/",
            publishedDate = "2022-11-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Puoi ricavare i grassi anche dalla carne e formaggi ma i grassi saturi non dovrebbero " +
                "superare il 10% delle calorie giornaliere. ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "È pertanto fondamentale essere sicuri di assumere più calorie del proprio TDEE , qual " +
                "ora si vuole aumentare la massa muscolare , introducendo spesso un quantitativo " +
                "superiore a 300 – 500 kcal rispetto al fabbisogno calorico giornaliero . ",
            articleTitle = "Somatotipo: endomorfo, mesomorfo, ectomorfo",
            url = "https://www.projectinvictus.it/somatotipo/",
            publishedDate = "2022-06-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Secondo le indicazioni dell’autore, suggerite anche da diversi esperti sui lipidi, la " +
                "dieta dovrebbe apportare circa l’1.5% delle sue calorie sotto forma di acidi grassi " +
                "polinsaturi. ",
            articleTitle = "Mountain Dog Diet: cos’è e come farla",
            url = "https://www.projectinvictus.it/dieta-mountain-dog/",
            publishedDate = "2022-05-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Riscontrando un deficit >20% della RDA , dopo aver migliorato l’apporto dello stesso con " +
                "gli alimenti, è consigliabile ricorrere al supplemento nutrizionale. ",
            articleTitle = "Integratore di magnesio: qual è il migliore e a cosa serve",
            url = "https://www.projectinvictus.it/magnesio-integratore/",
            publishedDate = "2022-03-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La cosa certa è che bisogna creare un deficit calorico graduale di 350-500 kcal " +
                "giornaliere , in modo da dare tempo al corpo di adattarsi al nuovo regime alimentare e " +
                "ristabilire naturalmente il proprio equilibrio ormonale. ",
            articleTitle = "Amenorrea e aumento di peso",
            url = "https://www.projectinvictus.it/amenorrea-aumento-di-peso/",
            publishedDate = "2022-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Il consiglio che è quello di prevedere un range entro cui muovervi, il cui limite minimo " +
                "è dato dalle 94 kcal calcolate e il limite massimo è dato dal valore corrispondente al " +
                "10% della vostra normocalorica (270 kcal). ",
            articleTitle = "Come andare in surplus calorico per la massa muscolare",
            url = "https://www.projectinvictus.it/surplus-calorico-massa-muscolare/",
            publishedDate = "2022-02-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "I grassi in fase di cut coprono il 15-25% del fabbisogno calorico totale, con un minimo " +
                "di 0,5 g/kg per assicurare la corretta funzionalità dell’organismo. ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Così come per il BMI dare una dieta di 2500 kcal dà un’indicazione statistica generale " +
                "di quanto più o meno uno dovrebbe mangiare, è un dato anche questo immediatamente " +
                "spendibile e facilmente comprensibile. ",
            articleTitle = "Le calorie fanno ingrassare veramente?",
            url = "https://www.projectinvictus.it/una-calorie-e-un-caloria-vediamo-di-fare-chiarezza/",
            publishedDate = "2021-11-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Stimando la media di quella che trovi tutti i giorni nel banco frutta, l’apporto " +
                "calorico si aggira intorno alle 50 kcal/100g. ",
            articleTitle = "Calorie frutta: tabella",
            url = "https://www.projectinvictus.it/calorie-frutta-tabella/",
            publishedDate = "2021-10-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "L’OMS consiglia per gli zuccheri semplici di non superare il 10% delle calorie " +
                "giornaliere, pertanto è bene, quando possibile, limitare tutte e due questi zuccheri " +
                "semplici. ",
            articleTitle = "Fruttosio o zucchero? Qual è il migliore?",
            url = "https://www.projectinvictus.it/fruttosio-o-zucchero-qual-e-il-migliore/",
            publishedDate = "2021-10-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "In generale, puoi continuare a considerare tutte e due gli zuccheri semplici come se " +
                "avessero le canoniche 4 kcal/g. ",
            articleTitle = "Fruttosio o zucchero? Qual è il migliore?",
            url = "https://www.projectinvictus.it/fruttosio-o-zucchero-qual-e-il-migliore/",
            publishedDate = "2021-10-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Per i grassi si consiglia un range intorno al 25-30% delle calorie giornaliere. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Si consiglia di non superare il 10% delle calorie dai grassi saturi. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Le linee guida vedono nei glucidi il principale macronutriente dell’alimentazione con " +
                "una quota che va dal 45-60% delle calorie giornaliere. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Tra queste troviamo anche l’ acqua e l’ alcol , il quale apporta 7kcal/g ma non ha altre " +
                "funzioni ed è tossico per l’organismo, quindi normalmente non viene annoverato tra i " +
                "nutrienti . ",
            articleTitle = "Sostanze nutritive: cosa sono e quali sono?",
            url = "https://www.projectinvictus.it/sostanze-nutritive/",
            publishedDate = "2021-09-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Si dividono in carboidrati complessi e semplici a seconda della loro struttura e, in " +
                "termini pratici, non ci sono differenze sostanziali in termini di calorie a seconda che " +
                "si preferisca i primi o i secondi: possiamo considerare sempre valide le 4 kcal/g. ",
            articleTitle = "Calorie macronutrienti",
            url = "https://www.projectinvictus.it/calorie-macronutrienti/",
            publishedDate = "2021-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Le linee guida considerano un’alimentazione sana e salutare quella che si basa " +
                "principalmente su cereali interi, legumi, frutta, verdura e che apporta dal 45% al 60% " +
                "delle calorie giornaliere introdotte. ",
            articleTitle = "Carboidrati complessi: funzioni, benefici ed esempi",
            url = "https://www.projectinvictus.it/carboidrati-complessi/",
            publishedDate = "2021-09-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Barrette proteiche, proteine in polvere, prodotti “dietetici”, bibite 0 kcal, alimenti " +
                "light ,… sono, nella maggior parte dei casi, presenti nella dieta di chi frequenta la " +
                "palestra e, soprattutto, di chi vuole dimagrire o stare attento a non assumere troppi " +
                "zuccheri e calorie. ",
            articleTitle = "Dolcificanti artificiali: fanno male e fanno ingrassare?",
            url = "https://www.projectinvictus.it/dolcificanti-fanno-male-fanno-ingrassare/",
            publishedDate = "2021-08-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Il corpo impiega il 5% delle calorie degli zuccheri per digerirle ed assimilarle. ",
            articleTitle = "Carboidrati semplici: funzioni, benefici ed esempi",
            url = "https://www.projectinvictus.it/carboidrati-semplici/",
            publishedDate = "2021-08-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Come vedremo, una dieta sana e bilanciata non dovrebbe comprendere più del 5-10% delle " +
                "calorie provenienti dagli zuccheri semplici aggiunti (25-50 g). ",
            articleTitle = "Carboidrati semplici: funzioni, benefici ed esempi",
            url = "https://www.projectinvictus.it/carboidrati-semplici/",
            publishedDate = "2021-08-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "L’OMS consiglia di consumare al massimo il 5-10% delle calorie da zuccheri semplici " +
                "(25-50 g al giorno). ",
            articleTitle = "Carboidrati semplici: funzioni, benefici ed esempi",
            url = "https://www.projectinvictus.it/carboidrati-semplici/",
            publishedDate = "2021-08-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Questo frutto, o meglio, queste bacche hanno circa 46 kcal ogni 100 gr, dovute perlopiù " +
                "a zuccheri semplici ed è quindi assolutamente introducibile nel contesto di una dieta " +
                "dimagrante . ",
            articleTitle = "Mirtillo rosso: proprietà, benefici e controindicazioni",
            url = "https://www.projectinvictus.it/mirtillo-rosso-proprieta-benefici/",
            publishedDate = "2021-07-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Come gli altri grassi, contribuiscono a mantenere il bilancio energetico positivo quando " +
                "assunti nella dieta e sono molto calorici: apportano 9 kcal per grammo . ",
            articleTitle = "Grassi monoinsaturi: fanno male o fanno bene? I benefici",
            url = "https://www.projectinvictus.it/grassi-monoinsaturi/",
            publishedDate = "2021-07-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Oltre al quanti grassi assumere al giorno , parametro importante per il bilancio " +
                "energetico e la salute, anche la qualità lipidica è da considerare: la quantità di " +
                "grassi saturi e monoinsaturi trans possono ricoprire al massimo il 10% del fabbisogno " +
                "energetico, i polinsaturi il 5-10%. ",
            articleTitle = "Grassi monoinsaturi: fanno male o fanno bene? I benefici",
            url = "https://www.projectinvictus.it/grassi-monoinsaturi/",
            publishedDate = "2021-07-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La percentuale ideale di grassi nella dieta equilibrata di un adulto sano (normo o " +
                "ipocalorica) potrebbe essere del 25% circa sulle calorie totali; ai bambini si consiglia " +
                "invece il 30% . ",
            articleTitle = "Grassi: cosa sono, quali sono e che funzione hanno?",
            url = "https://www.projectinvictus.it/grassi/",
            publishedDate = "2021-06-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La quantità di grassi “ cattivi ” , ovvero quelli saturi e monoinsaturi in conformazione " +
                "trans, non dovrebbe superare il 10% delle calorie totali nella dieta. ",
            articleTitle = "Grassi: cosa sono, quali sono e che funzione hanno?",
            url = "https://www.projectinvictus.it/grassi/",
            publishedDate = "2021-06-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La frutta fresca, infatti, è molto idratata (utile come spuntino o colazione soprattutto " +
                "in estate) e presenta fibra alimentare soprattutto nella buccia – questa fornisce meno " +
                "di 2 kcal/g e ha molti benefici sulla salute. ",
            articleTitle = "Frutti proteici: la frutta più proteica",
            url = "https://www.projectinvictus.it/frutta-proteica/",
            publishedDate = "2021-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "(1) È un frutto molto calorico , la sua polpa contiene 358 kcal ogni 100 g provenienti " +
                "prevalentemente da grassi, ed un’ottima fonte di minerali principalmente rame, ferro e " +
                "magnesio. ",
            articleTitle = "Cocco: benefici, proprietà e calorie",
            url = "https://www.projectinvictus.it/cocco-proprieta-benefici/",
            publishedDate = "2021-05-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "A tal proposito, molte istituzioni consigliavano (alcune tutt’ora) di limitarli più " +
                "possibile (10-12% delle calorie totali). ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Colazione Costituisce, di solito, circa il 15% delle calorie giornaliere. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Spuntini Sono generalmente 2 e, di solito, contengono circa il 5% delle calorie " +
                "giornaliere. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Pranzo Dovrebbe essere il pasto più abbondante, con circa il 40% delle calorie. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "In termini percentuali sulle calorie totali, contestualizzato in una dieta ipercalorica, " +
                "il fabbisogno proteico sarebbe compreso tra 25-30% kcal/die – aumenta quando le calorie " +
                "scendono. ",
            articleTitle = "Proteine: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-cosa-sono-a-cosa-servono/",
            publishedDate = "2021-04-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Per limitare l’appetito durante la perdita di peso, non fare un taglio calorico troppo " +
                "drastico (350, massimo 500 kcal circa): meglio un deficit meno marcato e più " +
                "perseguibile nel tempo che uno molto consistente per poco tempo, sia in termini di " +
                "sostenibilità che di risultati. ",
            articleTitle = "Come aumentare o diminuire l’appetito?",
            url = "https://www.projectinvictus.it/appetito/",
            publishedDate = "2021-03-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "A questo punto, per ottenere i grammi dei macronutrienti da raggiungere bisogna " +
                "ricordare che 1 g di carboidrati e 1g di proteine corrispondono a 4 kcal, mentre 1 g di " +
                "grassi a 9 kcal . ",
            articleTitle = "IIFYM: che cos’è e come funziona?",
            url = "https://www.projectinvictus.it/iifym/",
            publishedDate = "2021-01-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Il soggetto intermedio può considerare le 100-200 kcal/die. ",
            articleTitle = "Macronutrienti: cosa sono e in quali alimenti si trovano?",
            url = "https://www.projectinvictus.it/macronutrienti-nella-dieta-cosa-dobbiamo-sapere/",
            publishedDate = "2021-01-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Se con il taglio calorico ti risulta difficile rispettare questi valori, può essere " +
                "utile ridurre il deficit : ad esempio se hai prefissato una perdita di peso dell’1% del " +
                "peso corporeo a settimana, passa allo 0.5%. ",
            articleTitle = "Macronutrienti: cosa sono e in quali alimenti si trovano?",
            url = "https://www.projectinvictus.it/macronutrienti-nella-dieta-cosa-dobbiamo-sapere/",
            publishedDate = "2021-01-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Lo studio è stato condotto per 8 settimane su 59 soggetti obesi a cui è stato chiesto di " +
                "consumare il 25% del proprio fabbisogno calorico giornaliero nei giorni di “digiuno” e " +
                "mangiare ad libitum nei restanti giorni. ",
            articleTitle = "Intermittent fasting: come farlo in pratica?",
            url = "https://www.projectinvictus.it/intermittent-fasting/",
            publishedDate = "2020-12-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Un piatto di pasta (sugo escluso) da 100 g ha circa 365kcal – valore che puoi " +
                "controllare sulla tabella nutrizionale che trovi stampata direttamente sul pacco di " +
                "pasta. ",
            articleTitle = "I carboidrati sono meglio a pranzo o a cena?",
            url = "https://www.projectinvictus.it/carboidrati-giorno-sera/",
            publishedDate = "2020-11-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Abbuffata notturna di dolci Tra i disturbi dell’alimentazione c’è anche la sindrome dei " +
                "mangiatori notturni : il soggetto interrompe il sonno, tende a fare uno spuntino prima " +
                "di riaddormentarsi, mangia almeno il 25% delle calorie dopo cena e durante la notte. ",
            articleTitle = "Abbuffate di dolci e cibo: ingrasso tanto?",
            url = "https://www.projectinvictus.it/abbuffate-dolci-natalizie-sopravvivere/",
            publishedDate = "2020-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Fai uno sgarro da 1500 kcal, quantità che per alcune ragazze potrebbe corrispondere " +
                "addirittura ad un intero fabbisogno calorico di una giornata. ",
            articleTitle = "Abbuffate di dolci e cibo: ingrasso tanto?",
            url = "https://www.projectinvictus.it/abbuffate-dolci-natalizie-sopravvivere/",
            publishedDate = "2020-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Fai un attimo due calcoli (la matematica della prima elementare torna sempre utile): " +
                "pesi 70 kg, quindi per smaltire quelle calorie dovresti camminare per 42 km e qualcosa. ",
            articleTitle = "Abbuffate di dolci e cibo: ingrasso tanto?",
            url = "https://www.projectinvictus.it/abbuffate-dolci-natalizie-sopravvivere/",
            publishedDate = "2020-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "In termini pratici, se fai un past o da non più di 600 kcal e inoltre hai anche una " +
                "buona massa magra puoi mescolare glucidi e lipidi senza il timore di ingrassare. ",
            articleTitle = "Grassi e carboidrati nello stesso pasto fanno ingrassare?",
            url = "https://www.projectinvictus.it/grassi-ed-carboidrati-vanno-mangiati-assieme/",
            publishedDate = "2020-08-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Tuttavia, è sempre il bilancio calorico che fa da padrone quando vuoi dimagrire o " +
                "ingrassare: se in un giorno mangi 1000 kcal di dolci in un colpo solo ma poi non mangi " +
                "nient’altro e sei al di sotto del tuo fabbisogno energetico non ingrasserai! ",
            articleTitle = "Grassi e carboidrati nello stesso pasto fanno ingrassare?",
            url = "https://www.projectinvictus.it/grassi-ed-carboidrati-vanno-mangiati-assieme/",
            publishedDate = "2020-08-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Ovviamente il segreto di queste barrette si trova sull’etichetta nutrizionale: " +
                "solitamente apportano meno di 300 kcal a porzione , riducendo quindi l’apporto calorico " +
                "della giornata. ",
            articleTitle = "Bufale alimentari: le più diffuse",
            url = "https://www.projectinvictus.it/bufale-alimentari/",
            publishedDate = "2020-08-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "A prescindere dalla distribuzione dei macronutrienti che vuoi adottare, hai 12.000 kcal " +
                "a disposizione che puoi raggiungere come vuoi: potenzialmente potresti anche mangiare 10 " +
                "pizze (e basta) in una settimana e dimagriresti – perché sei in deficit calorico! ",
            articleTitle = "Sgarro nella dieta: come gestirlo?",
            url = "https://www.projectinvictus.it/sgarro-dieta-bodybuilding/",
            publishedDate = "2020-08-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La quota raccomandata di zucchero dall’OMS è quella di non superare il 5% delle calorie " +
                "dagli zuccheri aggiunti, stiamo parlando di 25g ovvero 6 cucchiaini da cucina. ",
            articleTitle = "Il glucosio: cos’è e dove si trova",
            url = "https://www.projectinvictus.it/glucosio/",
            publishedDate = "2020-05-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Una volta risolta l’equazione, sai quante calorie assumere con i carboidrati ; per " +
                "ottenere i grammi sarà sufficiente dividere questo numero per 4 (dato che 1 g di " +
                "carboidrati corrispondono a 4 kcal). ",
            articleTitle = "Quanti carboidrati per dimagrire?",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno-per-dimagrire/",
            publishedDate = "2020-04-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Un grammo di destrosio corrisponde a 3.74 kcal e, tra i vari parametri nutrizionali, è " +
                "classificato come glucide . ",
            articleTitle = "Destrosio in palestra: che cos’è e a cosa serve",
            url = "https://www.projectinvictus.it/destrosio-bodybuilding-palestra/",
            publishedDate = "2020-03-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Vado a correre 5 volte a settimana o faccio crossfit 4 volte a settimana e ad ogni " +
                "seduta brucio 500 kcal (numero puramente inventato di fantasia da non prendere come " +
                "riferimento). ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "I carboidrati dovrebbero comprendere il 55-60% del fabbisogno calorico giornaliero, " +
                "mentre per le proteine il valore consigliato è 1.4 – 2 g/kg di peso corporeo . ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La necessaria cottura dei legumi secchi li porta ad aumentare di circa 3 volte il peso " +
                "iniziale e pertanto i valori nutrizionali andranno normalizzati dividendo per il " +
                "coefficiente di idratazione (andranno divise le calorie e tutti i valori nutrizionali " +
                "presenti in etichetta per 3). ",
            articleTitle = "Legumi: quanti sono e quali sono i migliori?",
            url = "https://www.projectinvictus.it/legumi/",
            publishedDate = "2020-02-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La frutta secca può andare da un contenuto calorico medio come quello delle castagne " +
                "(189 kcal) ad un contenuto calorico elevatissimo come le noci di macadamia (718 kcal), " +
                "ma bisogna saper paragonare l’effetto saziante con il contenuto calorico. ",
            articleTitle = "Frutta secca migliore: benefici a dieta e in palestra",
            url = "https://www.projectinvictus.it/frutta-secca-quale-quanta-e-perche/",
            publishedDate = "2020-02-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Se rimane stazionaria 1800kcal sono realmente il suo TDEE altrimenti si aggiusterà fino " +
                "a trovare la quota calorica che le permetterà di non variare di peso nella settimana. ",
            articleTitle = "Dieta dimagrante: come farla in modo efficace",
            url = "https://www.projectinvictus.it/dieta-dimagrante/",
            publishedDate = "2018-10-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "I 65 giorni sono ipotetici, se decidiamo che 1-2 volte a settimana Maria potrà mangiare " +
                "di più ridurremmo il deficit settimanale ed aumenteremo il tempo stimato per raggiungere " +
                "l’obiettivo. ",
            articleTitle = "Dieta dimagrante: come farla in modo efficace",
            url = "https://www.projectinvictus.it/dieta-dimagrante/",
            publishedDate = "2018-10-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Per far si di mantenere quella media calorica settimanale (la più bassa di tutta la mia " +
                "vita), o facevo ancora più la fame gli altri 6 giorni cosi da concedermi più calorie il " +
                "giorno singolo di ricarica, oppure dovevo bilanciare tutto quanto nella maniera che " +
                "ritenevo più accettabile e proficua. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Bene siamo arrivati a concludere che lo sgarro di Giuseppe sommato alle calorie già " +
                "precedentemente consumate, gli è costato circa 3750 kcal di eccesso calorico sopra al " +
                "proprio fabbisogno calorico giornaliero. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Soltanto sovraccarichi superiori al 75% del nostro massimale, permettono di proteggere " +
                "la massa magra durante il catabolismo indotto dal deficit energetico. ",
            articleTitle = "Esercizi per dimagrire/ quali sono?",
            url = "https://www.projectinvictus.it/esercizi-per-dimagrire-esistono/",
            publishedDate = "2017-04-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Contare le calorie e macronutrienti è palloso, per scendere fino al 12-15% non è " +
                "necessario, ma se lo si fa è tutto molto più facile. ",
            articleTitle = "Calorie: perchè contano",
            url = "https://www.projectinvictus.it/calorie-perche-contano/",
            publishedDate = "2017-03-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Non essendoci state, nei 30 giorni di rilevazione, variazioni significative del peso e " +
                "della circonferenza della vita, consideriamo queste calorie come indicative di un " +
                "apporto di mantenimento. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Essendo l’apporto di acqua, complice anche il caldo, arrivato alla soglia dei 10 litri e " +
                "non potendo pertanto agire su quella leva, decidiamo di apportare una piccola modifica " +
                "al piano di Peaking, tagliando altre 150 Kcal medie giornaliere tramite una diminuzione " +
                "dei grassi a 30 g. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Nel medio termine, infatti, a parità di calorie e macronutrienti, otterrete gli stessi " +
                "identici risultati utilizzando una semplice ciclizzazione ON-OFF con un 20-25% di " +
                "differenza nell’apporto dei carboidrati e introducendo un REFEED ogni tanto nelle ultime " +
                "settimane. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Decidiamo d’aumentare la nostra quota proteica, sforando il nostri equilibrio energetico " +
                "di 500kcal. ",
            articleTitle = "Calorie per dimagrire: capiamo Ormoni, macronutrienti e calorie",
            url = "https://www.projectinvictus.it/ormoni-macronutrienti-e-calorie/",
            publishedDate = "2016-03-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Nella mia esperienza, si potrebbero ridurre le calorie giornaliere di circa 300-500 kcal " +
                "e non sacrificare alcuna massa magra se l’assunzione di BCAA è abbastanza alta in " +
                "prossimità degli allenamenti. ",
            articleTitle = "BCAA sempre e comunque utili?",
            url = "https://www.projectinvictus.it/bcaa-sempre-e-comunque-utili/",
            publishedDate = "2014-03-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Gli atomi H che si perdono tolgono intorno al 19% del contenuto energetico delle " +
                "proteine, portando il loro valore effettivo da 5,65 a 4kcal. ",
            articleTitle = "Le proteine fanno male? Impariamo a conoscerle!",
            url = "https://www.projectinvictus.it/proteine-fanno-male/",
            publishedDate = "2013-03-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "È essenziale non avere carenze di questo nutriente, la quota minima raccomandata è di " +
                "0,75g per kg di peso corporeo, in media il 13,5% delle calorie introdotte. ",
            articleTitle = "Le proteine fanno male? Impariamo a conoscerle!",
            url = "https://www.projectinvictus.it/proteine-fanno-male/",
            publishedDate = "2013-03-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Una persona sedentaria può vivere in salute assumendo 0,9-1g , pari al 20% delle calorie " +
                "giornaliere. ",
            articleTitle = "Le proteine fanno male? Impariamo a conoscerle!",
            url = "https://www.projectinvictus.it/proteine-fanno-male/",
            publishedDate = "2013-03-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Ora queste 3750 kcal non verranno stoccate tutte quante negli adipociti, per capire però " +
                "in che % circa verranno depositate in questi ultimi, dobbiamo analizzare il contesto in " +
                "cui Giuseppe si trova. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La seconda fase (2 settimane) è di mantenimento , e punta a mantenere il peso perso " +
                "nella prima fase. ",
            articleTitle = "Dieta Sirt: come funziona e quali cibi mangiare",
            url = "https://www.projectinvictus.it/dieta-sirt/",
            publishedDate = "2024-01-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "La dieta dei 3 giorni funziona perché fa mangiare quantità molto ridotte di cibo (circa " +
                "1000 kcal / giorno), rendendo la settimana equilibrata anche se negli altri 4 giorni " +
                "mangi un po’ di più. ",
            articleTitle = "Dieta dei 3 giorni per perdere peso",
            url = "https://www.projectinvictus.it/dieta-dei-3-giorni/",
            publishedDate = "2024-05-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Le diete ipocaloriche dovrebbero mediamente creare un deficit calorico del 15-20% " +
                "rispetto al tuo fabbisogno. ",
            articleTitle = "Dieta ipocalorica per dimagrire: la guida completa",
            url = "https://www.projectinvictus.it/dieta-ipocalorica/",
            publishedDate = "2025-01-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CALORIES,
            statement =
                "Fai attenzione alle calorie nascoste Quanti cucchiaini di zucchero e quanti di olio " +
                "spesso non vengono considerati, quando invece apportano molte calorie, soprattutto l’ " +
                "olio che apporta circa 100 kcal per 10 ml (1 cucchiaio da minestra). ",
            articleTitle = "Dimagrire senza dieta in modo definitivo",
            url = "https://www.projectinvictus.it/dieta-senza-dieta/",
            publishedDate = "2024-01-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Questi numeri valgono se la persona prima di mettersi a dieta avevamo un buon TDEE " +
                "(almeno intorno alle 30 kcal/kg di peso corporeo), ovvero assumeva un buon quantitativo " +
                "di calorie. ",
            articleTitle = "Dieta ferrea per dimagrire",
            url = "https://www.projectinvictus.it/dieta-ferrea/",
            publishedDate = "2025-05-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Con questo taglio calorico, da fare rispetto al tuo dispendio energetico giornaliero, " +
                "puoi immaginare di perdere circa lo 0,5 – 1% del peso corporeo a settimana . ",
            articleTitle = "Grasso addominale: cos’è, come eliminarlo, rischi",
            url = "https://www.projectinvictus.it/grasso-addominale/",
            publishedDate = "2025-04-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Il restante 30-40% dell’energia spesa è data dalla somma di attività fisica, NEAT , TID " +
                "(Termogenesi Indotta dalla Dieta ). ",
            articleTitle = "Calcolo metabolismo basale",
            url = "https://www.projectinvictus.it/metabolismo-basale/",
            publishedDate = "2025-01-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Ci sono molte variabili da cui dipende il metabolismo basale, anche se alcuni hanno più " +
                "influenza rispetto ad altri: età, sesso, massa magra spiegano circa l’80% del " +
                "metabolismo basale , che infatti sono i tre fattori solitamente inclusi nelle formule di " +
                "calcolo. ",
            articleTitle = "Calcolo metabolismo basale",
            url = "https://www.projectinvictus.it/metabolismo-basale/",
            publishedDate = "2025-01-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Quando i risultati rallentano , piuttosto che continuare a soffrire e rinunciare " +
                "improvvisamente alla dieta, conviene fare 2 settimane di break diet , in cui torni al " +
                "tuo TDEE (il fabbisogno energetico giornaliero) e “interrompi” il regime ipocalorico. ",
            articleTitle = "Dieta ipocalorica per dimagrire: la guida completa",
            url = "https://www.projectinvictus.it/dieta-ipocalorica/",
            publishedDate = "2025-01-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Per trovare il TDEE il metodo migliore è mettere su un’ app conta calorie tutto ma " +
                "proprio tutto quello che mangi per 3-4 settimane. ",
            articleTitle = "3 diete efficaci che funzionano",
            url = "https://www.projectinvictus.it/3-diete-efficaci/",
            publishedDate = "2024-03-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Per dimagrire bisognerà instaurare un deficit calorico che comprenda il fabbisogno " +
                "nutrizionale giornaliero (compreso il dispendio dell’allenamento) e da questo creare un " +
                "deficit medio intorno alle 500 – 350 kcal (uomo-donna). ",
            articleTitle = "Il CrossFit fa dimagrire?",
            url = "https://www.projectinvictus.it/crossfit-fa-dimagrire/",
            publishedDate = "2022-11-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "È pertanto fondamentale essere sicuri di assumere più calorie del proprio TDEE , qual " +
                "ora si vuole aumentare la massa muscolare , introducendo spesso un quantitativo " +
                "superiore a 300 – 500 kcal rispetto al fabbisogno calorico giornaliero . ",
            articleTitle = "Somatotipo: endomorfo, mesomorfo, ectomorfo",
            url = "https://www.projectinvictus.it/somatotipo/",
            publishedDate = "2022-06-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Fare più pasti ravvicinati tra loro (ogni 1, 2, 3 ore) non aumenta il metabolismo e " +
                "quindi non fa dimagrire di più : a meno che questo non ti aiuti, in regime ipocalorico, " +
                "a mantenere più aderenza alla dieta. ",
            articleTitle = "Ogni quanto mangiare per dimagrire?",
            url = "https://www.projectinvictus.it/frequenza-dei-pasti/",
            publishedDate = "2020-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Se rimane stazionaria 1800kcal sono realmente il suo TDEE altrimenti si aggiusterà fino " +
                "a trovare la quota calorica che le permetterà di non variare di peso nella settimana. ",
            articleTitle = "Dieta dimagrante: come farla in modo efficace",
            url = "https://www.projectinvictus.it/dieta-dimagrante/",
            publishedDate = "2018-10-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "I muscoli scheletrici, motore del metabolismo La muscolatura scheletrica rappresenta il " +
                "40% del peso corporeo ed è per questo il più esteso organo di cui disponiamo. ",
            articleTitle = "Cosa succede quando il muscolo incontra il grasso?",
            url = "https://www.projectinvictus.it/muscolo-incontra-grasso/",
            publishedDate = "2017-03-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "In questo modo possiamo mantenere una dieta ipocalorica per mesi, senza vedere il nostro " +
                "metabolismo dopo 3-5 settimane, sprofondare per terra e bloccare il dimagrimento. ",
            articleTitle = "Calorie per dimagrire: capiamo Ormoni, macronutrienti e calorie",
            url = "https://www.projectinvictus.it/ormoni-macronutrienti-e-calorie/",
            publishedDate = "2016-03-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.METABOLISM,
            statement =
                "Tutti gli alimenti per essere digeriti ed assimilati richiedono una spesa energetica , " +
                "in una dieta bilanciata si fa una media del 10% del nostro dispendio giornaliero. ",
            articleTitle = "Calorie per dimagrire: capiamo Ormoni, macronutrienti e calorie",
            url = "https://www.projectinvictus.it/ormoni-macronutrienti-e-calorie/",
            publishedDate = "2016-03-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Inoltre, le donne tendenzialmente avranno bisogno di una quantità maggiore di grassi " +
                "rispetto agli uomini (minimo 0,8 g/kg). ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Più sei sedentario, poco attivo e con poca massa muscolare più puoi avvicinarti ai 2 " +
                "g/kg. ",
            articleTitle = "Fabbisogno glucidico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-glucidico-carboidrati/",
            publishedDate = "2026-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Sapendo che 1 g di carboidrati = 4 kcal, dovrà assumere 361 g di carboidrati al giorno " +
                "(4,5 g/kg per il soggetto in questione). ",
            articleTitle = "Fabbisogno glucidico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-glucidico-carboidrati/",
            publishedDate = "2026-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "In genere si impostano prima proteine e grassi , destinando ai carboidrati le calorie " +
                "rimanenti: Per le proteine , negli sportivi in normo o ipercalorica si consiglia un " +
                "apporto tra 1,6, arrivando a 2-2,2 g/kg in fase di dimagrimento per aiutare a preservare " +
                "la massa muscolare. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per i carboidrati , se sei attivo e ti alleni, soprattutto se ha una buona massa " +
                "muscolare, può beneficiare di un maggiore apporto di carboidrati, che mediamente varia " +
                "tra 3 e 7 g/kg. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Poi imposta la quantità di proteine, che è più alta rispetto a quando vuoi mantenere o " +
                "aumentare il peso: 2 – 2,6 g/kg . ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "E’ più importante la quantità totale giornaliera di proteine che il timing, è " +
                "consigliato dividere la quantità proteica in 4-5 volte al giorno , che possono " +
                "coincidere con i normali pasti che fai nell’arco della giornata. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "In questo ultimo caso, ti conviene fare uno spuntino o un pasto, a seconda dell’orario, " +
                "che contenga almeno 20 g di proteine. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Una volta si dava come indicazione massimo 30 g di proteine per pasto perché si pensava " +
                "che una quota aggiuntiva andasse sprecata, visto che la massima sintesi proteica si " +
                "raggiunge con questo quantitativo. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Una porzione di 100g di pollo fornisce ben 27g di proteine, rendendolo una scelta ideale " +
                "se cerchi un elevato apporto proteico con un ridotto contenuto di grassi. ",
            articleTitle = "I 10 migliori alimenti per la massa muscolare",
            url = "https://www.projectinvictus.it/alimenti-per-la-massa-muscolare/",
            publishedDate = "2025-07-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La carne è costituita per il 15-20% da proteine e presenta soprattutto lisina, leucina, " +
                "fenilalanina e tirosina tra gli aminoacidi essenziali – con una marginale differenza a " +
                "seconda dell’animale da cui deriva. ",
            articleTitle = "Proteine animali e vegetali: quali sono le migliori",
            url = "https://www.projectinvictus.it/proteine-animali-e-vegetali-a-confronto/",
            publishedDate = "2025-05-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Il tuorlo è molto meno proteico e ha una natura più lipidica di cui il colesterolo " +
                "costituisce il 5% del peso dell’uovo intero – da precisare, che questo dà origine a " +
                "lipoproteine meno facilmente ossidabili (e quindi meno dannose) rispetto al colesterolo " +
                "di altri alimenti. ",
            articleTitle = "Proteine animali e vegetali: quali sono le migliori",
            url = "https://www.projectinvictus.it/proteine-animali-e-vegetali-a-confronto/",
            publishedDate = "2025-05-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le linee guida sostengono che 0,8 g proteine/kg peso corporeo sia un quantitativo " +
                "sufficiente per ricoprire il fabbisogno proteico giornaliero . ",
            articleTitle = "Dieta proteica: la guida completa",
            url = "https://www.projectinvictus.it/dieta-proteica/",
            publishedDate = "2025-05-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Il range di proteine consigliato in questa fase è 2.2-2.6 g proteine/kg corporeo al " +
                "giorno . ",
            articleTitle = "Dieta proteica: la guida completa",
            url = "https://www.projectinvictus.it/dieta-proteica/",
            publishedDate = "2025-05-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per definizione, una dieta è iperproteica quando le proteine ricoprono almeno il 35% " +
                "dell’introito calorico totale o che la quota proteica superi il valore RDA di 0,8 g " +
                "proteine/kg peso corporeo . ",
            articleTitle = "La dieta iperproteica per il dimagrimento",
            url = "https://www.projectinvictus.it/dieta-iperproteica-dimagrimento/",
            publishedDate = "2025-05-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per assumere un quantitativo più alto di proteine, consuma o aggiungi l’ albume : 100 g " +
                "contengono circa 11 g di proteine e non ha tracce di carboidrati e grassi. ",
            articleTitle = "Colazione proteica: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-proteica/",
            publishedDate = "2025-04-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Al contrario di cosa si pensa comunemente, il muscolo non è costituito solo da proteine " +
                "anzi: le proteine costituiscono solamente il 20% del tessuto muscolare . ",
            articleTitle = "Proteine per aumentare la massa muscolare",
            url = "https://www.projectinvictus.it/proteine-per-massa-muscolare/",
            publishedDate = "2025-02-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine che contiene sono ad alto valore biologico, soprattutto l’ albumina è " +
                "considerata la proteina nobile per eccellenza data la sua composizione amminoacidica e " +
                "costituisce il 60-70% delle proteine dell’albume . ",
            articleTitle = "Quante proteine contiene un uovo?",
            url = "https://www.projectinvictus.it/quante-proteine-contiene-un-uovo/",
            publishedDate = "2024-11-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Perchè il latte sia conforme alle leggi, deve contenere almeno 28 g di proteine per 1 " +
                "litro di latte. ",
            articleTitle = "Quali sono le proteine del latte?",
            url = "https://www.projectinvictus.it/proteine-del-latte/",
            publishedDate = "2024-11-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Queste rappresentano circa il 20% delle proteine del latte e sono riconosciute per una " +
                "maggiore qualità proteica (come il valore biologico) rispetto all’altra frazione delle " +
                "proteine del latte, le caseine . ",
            articleTitle = "Le 6 migliori proteine in polvere",
            url = "https://www.projectinvictus.it/migliori-proteine-in-polvere/",
            publishedDate = "2024-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Caseine La caseina è la frazione predominante delle proteine del latte , " +
                "rappresentandone circa l’80% contro il 20% delle proteine del siero del latte. ",
            articleTitle = "Le 6 migliori proteine in polvere",
            url = "https://www.projectinvictus.it/migliori-proteine-in-polvere/",
            publishedDate = "2024-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "In anni più recenti è emerso che digiunare anche fino a 16-20 ore al giorno non agisce " +
                "negativamente sulla massa muscolare (9,10), mettendo in discussione la necessità di " +
                "assumere proteine per “coprire” il digiuno notturno, data l’assenza di rischio " +
                "catabolico. ",
            articleTitle = "Le 6 migliori proteine in polvere",
            url = "https://www.projectinvictus.it/migliori-proteine-in-polvere/",
            publishedDate = "2024-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Ai reni Alla base della preoccupazione circa i danni renali indotti da una dieta " +
                "chetogenica c’è la convinzione che un apporto proteico maggiore rispetto alle RDA per " +
                "soggetti sedentari (0,8-1g/kg) possa provocare danni al sistema renale e progressiva " +
                "perdita di capacità funzionale dei reni. ",
            articleTitle = "Dieta chetogenica: pro e contro",
            url = "https://www.projectinvictus.it/dieta-chetogenica-fa-male/",
            publishedDate = "2024-05-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "In realtà, questa finestra anabolica dura più tempo: l’importante non è fare un preciso " +
                "pasto post workout ma assicurarsi di assumere abbastanza carboidrati (oltre che energia, " +
                "proteine, grassi) nel corso delle 24 ore . ",
            articleTitle = "Carboidrati dopo l’allenamento in palestra",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-palestra/",
            publishedDate = "2024-03-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Ad esempio, per quanto riguarda le proteine , è consigliato consumarle ogni 3-4 ore per " +
                "creare un ambiente anabolico favorevole (in un contesto energetico opportuno). ",
            articleTitle = "Finestra anabolica: cos’è e cosa mangiare",
            url = "https://www.projectinvictus.it/finestra-anabolica/",
            publishedDate = "2024-03-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Associare per ogni blocchetto di proteine anche 1 blocchetto di carboidrati e 1 " +
                "blocchetto di grassi ( rapporto 1:1:1 tra i blocchetti ). ",
            articleTitle = "Dieta a zona: come funziona e menù",
            url = "https://www.projectinvictus.it/dieta-a-zona/",
            publishedDate = "2024-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "In questo periodo di scarico mangi prevalentemente grassi e proteine, mentre i " +
                "carboidrati sono ridotti al minimo (30 g), preferibilmente da verdura fibrosa, per " +
                "assumere il giusto quantitativo di fibre alimentari. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Tagliare completamente i carboidrati o i grassi non è una scelta fattibile: i " +
                "carboidrati sono il nutriente principale e i grassi è fondamentale tenerli minimamente " +
                "alti anche in ipocalorica (senza scendere sotto i 0,8 g/kg al giorno per evitare " +
                "squilibri ormonali). ",
            articleTitle = "Aumentare la massa muscolare nelle donne: come fare",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare-donne/",
            publishedDate = "2024-01-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Puoi prevedere una fonte proteica ad ogni pasto, in modo che ogni pasto contenga circa " +
                "0.25 g proteine/kg peso corporeo. ",
            articleTitle = "Proteine prima di dormire per la massa muscolare",
            url = "https://www.projectinvictus.it/proteine-prima-di-dormire/",
            publishedDate = "2024-01-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine idrolizzate sono quelle con il tempo di digestione più rapido ( 10-30 minuti " +
                "), data la lavorazione a cui sono sottoposte e che prevede la formazione di un composto " +
                "“pre-digerito”. ",
            articleTitle = "Proteine prima di dormire per la massa muscolare",
            url = "https://www.projectinvictus.it/proteine-prima-di-dormire/",
            publishedDate = "2024-01-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine isolate , invece, richiedono più tempo: 30-60 minuti . ",
            articleTitle = "Proteine prima di dormire per la massa muscolare",
            url = "https://www.projectinvictus.it/proteine-prima-di-dormire/",
            publishedDate = "2024-01-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Molti autori consigliano range tra 1.5 – 2.1 g/kg peso corporeo : eccedendo con le " +
                "calorie ti ritroverai non solo più grosso ma anche più grasso . ",
            articleTitle = "Le proteine fanno ingrassare?",
            url = "https://www.projectinvictus.it/le-proteine-fanno-ingrassare/",
            publishedDate = "2024-01-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Al contrario, se stai seguendo un regime ipocalorico aumentare le proteine fino a 1.6 – " +
                "2.6 g/kg peso corporeo potrebbe consentire un ottimo vantaggio, sia per proteggere la " +
                "massa magra, sia per sopportare meglio la fame. ",
            articleTitle = "Le proteine fanno ingrassare?",
            url = "https://www.projectinvictus.it/le-proteine-fanno-ingrassare/",
            publishedDate = "2024-01-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Evitare di perdere muscolo in ipocalorica, soprattutto se prolungata, è inevitabile ma " +
                "può essere limitato tramite: adeguato apporto proteico, allenamento contro resistenze " +
                "(con i pesi), deficit calorico graduale e non eccessivo (- 300-500 kcal al giorno). ",
            articleTitle = "Proteine in polvere per dimagrire donne",
            url = "https://www.projectinvictus.it/proteine-in-polvere-per-dimagrire-donne/",
            publishedDate = "2023-09-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Nell’immediato post-workout prendere proteine in polvere non ha grandi utilità, se non " +
                "nei rari casi in cui non assumi cibo o proteine da almeno 4-5 ore , come negli " +
                "allenamenti a digiuno. ",
            articleTitle = "Quando assumere proteine: prima o dopo allenamento?",
            url = "https://www.projectinvictus.it/proteine-prima-o-dopo-allenamento/",
            publishedDate = "2023-09-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Nel caso di ipocalorica più o meno marcata e bassa massa grassa, l’introito può " +
                "aumentare fino a 3 g/kg in quanto macronutriente che dà sazietà e che limita la perdita " +
                "di tessuto muscolare. ",
            articleTitle = "Proteine palestra: quali prendere e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-palestra/",
            publishedDate = "2023-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La sintesi proteica non avviene in poco tempo ma una volta stimolata, ad esempio con un " +
                "pasto proteo-glucidico post-allenamento, può durare anche 24-48 ore: risulta così " +
                "evidente come non siano le proteine post-workout a determinare una proteosintesi " +
                "efficace, ma la loro assunzione nella giornata. ",
            articleTitle = "Proteine palestra: quali prendere e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-palestra/",
            publishedDate = "2023-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Questi alimenti apportano una notevole quantità di carboidrati complessi, circa un " +
                "10-12% di proteine ma anche fibre alimentari, vitamine del gruppo B e sali minerali come " +
                "magnesio, zinco e ferro. ",
            articleTitle = "Dieta flexitariana: cos’è, cosa mangiare, menù",
            url = "https://www.projectinvictus.it/flexitariana/",
            publishedDate = "2023-06-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Con i giusti accostamenti, le uova diventano perfette per una dieta ipocalorica ma il " +
                "consiglio è sempre quello di variare le fonti di proteine e grassi e seguire le linee " +
                "guida che raccomandano di consumare 1 porzione di uova (50 g) massimo 2-4 volte alla " +
                "settimana . ",
            articleTitle = "Dieta delle uova",
            url = "https://www.projectinvictus.it/dieta-delle-uova/",
            publishedDate = "2023-01-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La dieta ipoproteica è intesa come una terapia dietetica conservativa che apporta il " +
                "7-10% di proteine rispetto al 15% di una dieta bilanciata. ",
            articleTitle = "Dieta ipoproteica: cosa mangiare, menù e consigli",
            url = "https://www.projectinvictus.it/dieta-ipoproteica/",
            publishedDate = "2023-01-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La dieta ipoproteica prevede un apporto di 0.6-0.8 g/kg di proteine al giorno e può " +
                "variare sia in positivo che in negativo in base alla funzionalità renale e ad altri " +
                "parametri clinici e antropometrici. ",
            articleTitle = "Dieta ipoproteica: cosa mangiare, menù e consigli",
            url = "https://www.projectinvictus.it/dieta-ipoproteica/",
            publishedDate = "2023-01-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Nei casi più gravi di insufficienza renale, in cui è previsto un apporto di circa 40 " +
                "g/die di proteine totali è possibile sostituire pane o pasta con analoghi artificiali " +
                "privi di proteine. ",
            articleTitle = "Dieta ipoproteica: cosa mangiare, menù e consigli",
            url = "https://www.projectinvictus.it/dieta-ipoproteica/",
            publishedDate = "2023-01-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine sono una classe di molecole biologiche molto presenti in natura : negli " +
                "organismi animali rappresentano circa il 50% del peso secco, mentre in quelli vegetali " +
                "un po’ meno poichè c’è un alto contenuto di cellulosa (una molecola che fa parte della " +
                "famiglia dei carboidrati). ",
            articleTitle = "Aminoacidi: cosa sono e che funzione svolgono?",
            url = "https://www.projectinvictus.it/cosa-sono-gli-aminoacidi/",
            publishedDate = "2022-12-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Quantitativi ben superiori alle quote raccomandate per i sedentari (0,8g/kg) Carboidrati " +
                ": gli atleti natural hanno benefici nel tenere alta la quota di carboidrati. ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            publishedDate = "2022-12-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La quota proteica e l’eventuale surplus calorico scongiurano il catabolismo muscolare, " +
                "anzi promuovono l’anabolismo; se segui un’alimentazione ipocalorica , bilanciata nei " +
                "macronutrienti, con un apporto proteico di 2,3-3,1g/kg di peso corporeo. ",
            articleTitle = "Catabolismo",
            url = "https://www.projectinvictus.it/catabolismo/",
            publishedDate = "2022-09-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La persona viene spinta a credere, visto che un pasto proteico ha il massimo effetto " +
                "anabolico per 4-6 ore, di dover continuamente mangiare, per evitare la perdita della " +
                "massa muscolare . ",
            articleTitle = "Catabolismo muscolare: cos’è, cause e rimedi",
            url = "https://www.projectinvictus.it/catabolismo-muscolare/",
            publishedDate = "2022-09-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La quota ottimale di proteine per la massa muscolare è di 1,4 – 2,2 g / kg al giorno . ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Aspetto a cui prestare attenzione, per assicurarti di soddisfare il fabbisogno proteico, " +
                "è essenziale avere una dieta varia e incrementare del 10-20% la quantità di proteine " +
                "assunte rispetto alle linee guida consigliate per gli onnivori. ",
            articleTitle = "Dieta vegana bodybuilding: come funziona ed esempio",
            url = "https://www.projectinvictus.it/dieta-vegana-e-bodybuilding/",
            publishedDate = "2022-05-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per soggetti che si allenano molto e che sono in forte ipocalorica, il fabbisogno " +
                "proteico è pari a 2,3-3,1 g proteine/kg peso corporeo . ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Più il cut è aggressivo e più la dieta ipocalorica viene protratta nel tempo, più la " +
                "quantità di proteine può avvicinarsi al valore massimo del range proposto (3,1 g/kg). ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "I grassi in fase di cut coprono il 15-25% del fabbisogno calorico totale, con un minimo " +
                "di 0,5 g/kg per assicurare la corretta funzionalità dell’organismo. ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Se ci si allena a stomaco vuoto, per ridurre l’effetto catabolico muscolare, si " +
                "consiglia di assumere HMB in associazione a 20-40 g di proteine ad alto valore biologico " +
                "nel post-workout . ",
            articleTitle = "HMB integratore: cos’è, a cosa serve, funziona?",
            url = "https://www.projectinvictus.it/hmb-integratore/",
            publishedDate = "2022-01-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Di queste, l’ 80% delle proteine totali nel latte è rappresentato dalle caseine (circa " +
                "2,7 g / 100 ml ). ",
            articleTitle = "Caseina ed effetti collaterali",
            url = "https://www.projectinvictus.it/caseina-fa-male/",
            publishedDate = "2022-01-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per quanto le proteine vegetali abbiano un valore biologico complessivamente inferiore a " +
                "quelle animali, compensando gli amminoacidi limitanti, anche il vegano può fare a meno " +
                "degli integratori di EAA – nonostante il suo fabbisogno proteico sia del 20% superiore " +
                "rispetto all’onnivoro. ",
            articleTitle = "Aminoacidi essenziali (EAA): come e quando prenderli? Prima o dopo l’allenamento?",
            url = "https://www.projectinvictus.it/fabbisogno-aminoacidi-essenziali/",
            publishedDate = "2022-01-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La quota proteica sufficiente per massimizzare la sintesi proteica è di 20-40 g (di cui " +
                "almeno 10 g di aminoacidi essenziali) di proteine ad elevato valore biologico, ovvero " +
                "complete di tutti gli aminoacidi e soprattutto che contengono quelli essenziali. ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Distribuire le proteine giornaliere nei vari pasti (idealmente ogni 3-4 ore) è la scelta " +
                "da preferire per influenzare maggiormente la sintesi proteica, la composizione corporea " +
                "e la performance. ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Infine per i carboidrati il quantitativo raccomandato minimo è di 2.5 g/kg (3 per gli " +
                "sportivi) . ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Ad esempio, atleti di resistenza con una richiesta energetica molto elevata possono " +
                "spingersi anche oltre il 60% di carboidrati oppure soggetti in forte dieta ipocalorica " +
                "avranno un apporto proteico maggiore. ",
            articleTitle = "Calorie macronutrienti",
            url = "https://www.projectinvictus.it/calorie-macronutrienti/",
            publishedDate = "2021-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "In totale, i glucidi dovrebbero ricoprire il 45-60% del proprio fabbisogno energetico " +
                "giornaliero oppure almeno 2-3 g/kg di peso corporeo , sempre in relazione alla spesa " +
                "energetica, al proprio obbiettivo e stile di vita. ",
            articleTitle = "Carboidrati semplici e complessi: qual è la differenza?",
            url = "https://www.projectinvictus.it/quali-sono-i-carboidrati/",
            publishedDate = "2021-09-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Una buona scelta è quella di circoscrivere un 50-70% dell’introito glucidico giornaliero " +
                "e comunque non scendere sotto 1.5 g/kg di peso corporeo nei pasti vicini " +
                "all’allenamento, fermo restando che su soggetti in fisiologia l’introito calorico è il " +
                "principale fattore da tenere in considerazione. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La fonte proteica va scelta sulla base dell’introito di amminoacidi: conviene, quindi, " +
                "scegliere una fonte ed un quantitativo di proteine che contenga circa (almeno) 10 g di " +
                "aminoacidi essenziali di cui 0.7-3 g di leucina. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Ad ogni modo, l’assunzione di 20-40 g di proteine (a seconda della qualità proteica e " +
                "della resistenza all’anabolismo) per pasto (uno di questi da far coincidere come " +
                "post-workout) durante la giornata sono in grado di stimolare al massimo la sintesi " +
                "proteica. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per massimizzare gli adattamenti muscolari (e l’ipertrofia) è consigliato assumere " +
                "0.4-0.5 g di proteine/kg sia 1-2 ore prima l’allenamento (tramite cibo solido o " +
                "integrazione) sia dopo. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "I dosaggi raccomandati sono compresi nel range di 3-6 mg/kg di peso corporeo al giorno, " +
                "non necessariamente ogni giorno poichè svolge i suoi effetti in acuto (ad esempio, nei " +
                "giorni di allenamento, prima dell’allenamento). ",
            articleTitle = "Caffeina, allenamento e palestra: effetti e dosaggi",
            url = "https://www.projectinvictus.it/caffeina-effetti-e-dosaggi/",
            publishedDate = "2021-07-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "A fine ipertrofico è tuttavia utile preferire il consumo dei carboidrati rispetto a " +
                "quello dei grassi, il cui fabbisogno può quindi essere diminuito fino a 0.5 g/kg senza " +
                "scendere verso valori inferiori. ",
            articleTitle = "Grassi buoni: quali sono e a cosa servono?",
            url = "https://www.projectinvictus.it/grassi-buoni/",
            publishedDate = "2021-06-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Benefici delle proteine vegane Le proteine vegane sono digeribili al 60-80% , mentre " +
                "quelle animali al 90%: se da una parte è un aspetto negativo, dall’altro permette di " +
                "assumere meno calorie a parità di introito. ",
            articleTitle = "Proteine vegane: come assumerle?",
            url = "https://www.projectinvictus.it/proteine-vegane/",
            publishedDate = "2021-06-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Solitamente, la dose di proteine consigliata da assumere per massimizzare la sintesi " +
                "proteica è di 25 g. ",
            articleTitle = "Proteine vegane: come assumerle?",
            url = "https://www.projectinvictus.it/proteine-vegane/",
            publishedDate = "2021-06-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per dare degli estremi, le caseine impiegano 7-8 ore per essere completamente digerite, " +
                "mentre le proteine idrolizzate (dall’assorbimento più rapido rispetto anche alle " +
                "proteine isolate ) 30 minuti. ",
            articleTitle = "Proteine a lento rilascio: quali sono?",
            url = "https://www.projectinvictus.it/proteine-lento-rilascio/",
            publishedDate = "2021-06-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Il range di proteine apportate dalla carne bianca corrisponde a 23-32 g per 100 g di " +
                "alimento : sono un’ottima fonte di aminoacidi. ",
            articleTitle = "Le carni proteiche con più proteine",
            url = "https://www.projectinvictus.it/carni-piu-proteiche/",
            publishedDate = "2021-06-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "E’ quindi utile assumere 20-40 g di proteine ad elevato valore biologico, una quantità " +
                "che è sufficiente a massimizzare la proteosintesi. ",
            articleTitle = "Aminoacidi e proteine: che differenza c’è?",
            url = "https://www.projectinvictus.it/aminoacidi-o-proteine/",
            publishedDate = "2021-06-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine, infatti, in caso di carenza energetica, vengono prelevate soprattutto dal " +
                "tessuto muscolare poichè è quello più ricco di proteine (20% del suo peso) – lo stesso " +
                "tessuto che è importante da mantenere per un dimagrimento efficace. ",
            articleTitle = "Proteine magre: quali sono i cibi proteici con pochi grassi?",
            url = "https://www.projectinvictus.it/proteine-magre/",
            publishedDate = "2021-06-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per l’adulto, volendo stimarli sul peso fisiologico, il coefficiente dovrebbe essere di " +
                "0,5-1,5 g/kg . ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Diciamo che, per la popolazione generale, se ne raccomanda più o meno 1,0 g/kg di peso " +
                "fisiologico (a seconda della fonte bibliografica consultata); oppure il 13% dell’energia " +
                "normocalorica. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine del siero del latte ( whey ) rappresentano il 20% delle proteine del latte e " +
                "hanno la caratteristica di avere una buona composizione aminoacidica. ",
            articleTitle = "Proteine del siero del latte: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-del-siero-del-latte/",
            publishedDate = "2021-05-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Isolate in polvere Le proteine isolate hanno una percentuale proteica molto alta " +
                "(90-95%) mentre le altre componenti (lattosio, grassi, sali minerali) sono praticamente " +
                "assenti. ",
            articleTitle = "Proteine del siero del latte: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-del-siero-del-latte/",
            publishedDate = "2021-05-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine vegetali hanno una minor digeribilità, il corpo ne assorbe meno del 90% di " +
                "quello che introduci, tuttavia puoi benissimo sostituire le proteine animali con quelle " +
                "vegetali. ",
            articleTitle = "Proteine animali sono dannose?",
            url = "https://www.projectinvictus.it/proteine-animali/",
            publishedDate = "2021-04-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Colazione Per una sintesi proteica più efficace, è consigliato assumere una quota " +
                "proteica (0.25 g/kg) ogni 3 ore, perciò circa ad ogni pasto per assicurare un costante " +
                "pool ematico aminoacidico e soddisfare il fabbisogno proteico giornaliero. ",
            articleTitle = "Proteine idrolizzate: benefici e controindicazioni",
            url = "https://www.projectinvictus.it/proteine-idrolizzate/",
            publishedDate = "2021-04-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per definizione sono la forma che ha il maggior grado di purezza : la percentuale " +
                "proteica è molto alta (90-95%), mentre tutte le altre componenti (lattosio, grassi, sali " +
                "minerali) sono praticamente assenti – le proteine risultano quindi “isolate” rispetto " +
                "alle altre sostanze presenti nel latte. ",
            articleTitle = "Proteine isolate: benefici e controindicazioni",
            url = "https://www.projectinvictus.it/proteine-isolate/",
            publishedDate = "2021-04-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La sintesi proteica miofibrillare e il bilancio proteico sono risultati migliori nel " +
                "caso dei 20 g ogni 3 ore , così come è emerso, soprattutto per i soggetti con un alto " +
                "volume di allenamento, come l’introduzione di 80 g di proteine a distanza di 12 ore sia " +
                "inadeguata. ",
            articleTitle = "Proteine isolate: benefici e controindicazioni",
            url = "https://www.projectinvictus.it/proteine-isolate/",
            publishedDate = "2021-04-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "L’allattamento, seppur meno impegnativo, richiede comunque di proseguire la secrezione " +
                "mammaria – contenente proteine ad alto valore biologico – per almeno 6 mesi. ",
            articleTitle = "Proteine: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-cosa-sono-a-cosa-servono/",
            publishedDate = "2021-04-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "In termini percentuali sulle calorie totali, contestualizzato in una dieta ipercalorica, " +
                "il fabbisogno proteico sarebbe compreso tra 25-30% kcal/die – aumenta quando le calorie " +
                "scendono. ",
            articleTitle = "Proteine: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-cosa-sono-a-cosa-servono/",
            publishedDate = "2021-04-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per di più, come abbiamo visto sul paragrafo della TID, le proteine di origine vegetale " +
                "sono meno digeribili rispetto a quelle animali (60-80% VS 90%) . ",
            articleTitle = "Proteine: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-cosa-sono-a-cosa-servono/",
            publishedDate = "2021-04-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Il concetto di “limite proteico massimo assorbibile = 30 g per pasto” è stato in parte " +
                "superato. ",
            articleTitle = "Proteine: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-cosa-sono-a-cosa-servono/",
            publishedDate = "2021-04-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Ogni giorno circa il 2.5% delle proteine corporee viene completamente rimpiazzato da " +
                "quelle neosintetizzate a partire dai singoli aminoacidi assunti o ricavati da altre " +
                "sostanze endogene. ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Nell’albume c’è la proteina nobile per eccellenza: l’albumina, che infatti ha il massimo " +
                "valore biologico con la presenza di tutti gli aminoacidi e che costituisce il 60-70% " +
                "delle proteine dell’albume. ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Proteine vegetali: cereali I cereali (come il frumento e derivati) hanno un contenuto " +
                "proteico molto variabile (8-20%) e non contengono tutti gli aminoacidi essenziali . ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Non ci sono infatti riserve di amminoacidi nell’organismo, come è il glicogeno per il " +
                "glucosio o l’organo adiposo per gli acidi grassi, e il muscolo è quello che presenta un " +
                "contenuto proteico più elevato (circa il 20% del suo peso). ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Controindicazioni Gli aminoacidi ramificati sono tre aminoacidi essenziali : leucina, " +
                "valina e isoleucina , che insieme costituiscono il 30% del totale delle proteine del " +
                "muscolo e che sono la fonte primaria per la sintesi di glutammina e alanina. ",
            articleTitle = "BCAA fanno male?",
            url = "https://www.projectinvictus.it/bcaa-aminoacidi-ramificati-fanno-male/",
            publishedDate = "2021-04-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Alcuni ricercatori infatti sottolineano che i BCAA potrebbero agire positivamente sul " +
                "recupero se l’apporto proteico è al di sotto di 1.6 g/kg di proteine (16), cioè " +
                "inferiore alle soglie minime ottimali per chi si allena con i pesi (1,7). ",
            articleTitle = "BCAA – aminoacidi ramificati: cosa sono e quali scegliere?",
            url = "https://www.projectinvictus.it/aminoacidi-ramificati-bcaa/",
            publishedDate = "2021-03-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Un dato importante è che se una persona che si allena con i pesi segue una dieta " +
                "iperproteica, raggiungendo le quantità suggerite dalle linee guida ufficiali, e " +
                "ricavando gran parte delle proteine da fonti nobili, normalmente tra il 15 e il 20% di " +
                "queste proteine è rappresentato da BCAA (5). ",
            articleTitle = "BCAA – aminoacidi ramificati: cosa sono e quali scegliere?",
            url = "https://www.projectinvictus.it/aminoacidi-ramificati-bcaa/",
            publishedDate = "2021-03-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Parte degli amminoacidi ricavati dalle proteine alimentari infatti viene trattenuta dal " +
                "piccolo intestino (attorno al 30-50%) per essere rilasciata in seguito, quando il cibo " +
                "non è più disponibile (11). ",
            articleTitle = "BCAA – aminoacidi ramificati: cosa sono e quali scegliere?",
            url = "https://www.projectinvictus.it/aminoacidi-ramificati-bcaa/",
            publishedDate = "2021-03-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Gli ultimi tre sono anche conosciuti come aminoacidi ramificati , i famosi BCAA , che " +
                "costituiscono ben il 40% delle proteine muscolari. ",
            articleTitle = "Aminoacidi per dimagrire: cosa sono? Funzionano veramente?",
            url = "https://www.projectinvictus.it/aminoacidi-per-dimagrire/",
            publishedDate = "2021-02-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "A questo punto, per ottenere i grammi dei macronutrienti da raggiungere bisogna " +
                "ricordare che 1 g di carboidrati e 1g di proteine corrispondono a 4 kcal, mentre 1 g di " +
                "grassi a 9 kcal . ",
            articleTitle = "IIFYM: che cos’è e come funziona?",
            url = "https://www.projectinvictus.it/iifym/",
            publishedDate = "2021-01-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "La donna in dolce attesa dovrebbe prediligere fonti di proteine da cibi di origine " +
                "vegetale come legumi (circa il 57% dell’assunzione giornaliera) seguiti da alimenti di " +
                "origine animale come carne (18%) e latticini (10%). ",
            articleTitle = "Alimentazione in gravidanza: cosa mangiare?",
            url = "https://www.projectinvictus.it/alimentazione-in-gravidanza/",
            publishedDate = "2020-11-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Tuttavia non si dovrebbe mai superare il 25% di energia totale data da proteine visto " +
                "che alcune meta-analisi suggeriscono come un’ingente quota proteica possa portare ad un " +
                "aumentato rischio di SGA, nascita pre-termine, aumento del peso del nascituro, morte nel " +
                "momento del parto e morte neonatale. ",
            articleTitle = "Alimentazione in gravidanza: cosa mangiare?",
            url = "https://www.projectinvictus.it/alimentazione-in-gravidanza/",
            publishedDate = "2020-11-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Senza glutine Gli alimenti che contengono glutine e che sono quindi da escludere dalla " +
                "dieta sono sostanzialmente frumento, kamut, segale, orzo e farro, alimenti che " +
                "contengono proteine per il 10-15% del totale. ",
            articleTitle = "Proteine vegetali per la palestra",
            url = "https://www.projectinvictus.it/proteine-vegetali/",
            publishedDate = "2020-07-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Altro metodo consiste nel tenere conto del fatto che le proteine vegetali hanno circa il " +
                "20-35% di digeribilità in meno (è una stima ovviamente). ",
            articleTitle = "Proteine vegetali per la palestra",
            url = "https://www.projectinvictus.it/proteine-vegetali/",
            publishedDate = "2020-07-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per chi fa palestra o bodybuilding è consigliato consumare circa 3-4 pasti al giorno per " +
                "massimizzare i guadagni ipertrofici soprattutto con pasti proteici (almeno 20 g di " +
                "proteine a pasto). ",
            articleTitle = "Ogni quanto mangiare per dimagrire?",
            url = "https://www.projectinvictus.it/frequenza-dei-pasti/",
            publishedDate = "2020-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le stime “standard” riguardo al fabbisogno proteico, che sentirai pronunciare più o meno " +
                "da ogni medico, corrispondono sempre a circa 0.8 g/kg peso corporeo. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Fabbisogno proteico giornaliero secondo l’OMS Nelle tabelle indicate dall’OMS per una " +
                "persona adulta (> 18 anni) sono indicati 0.66 g proteine per ogni chilo di peso " +
                "corporeo, sia per gli uomini che per le donne. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Con l’ avanzare dell’età per le donne è bene aumentare l’apporto proteico : da circa " +
                "0.70 g/kg peso corporeo passiamo a una dose consigliata maggiore di 0.84 g di proteine. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "225 sono i grammi di proteine da assumere al giorno (pari a circa 2.8 g/kg di peso " +
                "corporeo). ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "I carboidrati dovrebbero comprendere il 55-60% del fabbisogno calorico giornaliero, " +
                "mentre per le proteine il valore consigliato è 1.4 – 2 g/kg di peso corporeo . ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Più del 60% del peso a secco è rappresentato da proteine . ",
            articleTitle = "Spirulina proprietà: cosa sapere su questa alga",
            url = "https://www.projectinvictus.it/spirulina-proprieta/",
            publishedDate = "2020-02-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per valori più precisi puoi sempre fare riferimento alla tabella, in cui sono indicati i " +
                "grammi di proteine per 100 g di prodotto. ",
            articleTitle = "Frutta secca migliore: benefici a dieta e in palestra",
            url = "https://www.projectinvictus.it/frutta-secca-quale-quanta-e-perche/",
            publishedDate = "2020-02-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Ad esempio, i più ricchi di proteine (tra i 26 e i 15 g su 100 g di alimento), in ordine " +
                "decrescente, sono: arachidi, pistacchi, mandorle dolci, nocciole e anacardi. ",
            articleTitle = "Frutta secca migliore: benefici a dieta e in palestra",
            url = "https://www.projectinvictus.it/frutta-secca-quale-quanta-e-perche/",
            publishedDate = "2020-02-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Proprio in merito all’ apporto proteico , per chi ha un’assunzione proteica inferiore a " +
                "1,2 g per kg di peso corporeo, è bene che aumenti l’apporto proteico (fino ad anche 1,6 " +
                "g/kg) per preservare o aumentare la massa muscolare. ",
            articleTitle = "Insulino resistenza, sintomi e rimedi: cos’è e come ridurla?",
            url = "https://www.projectinvictus.it/insulino-resistenza-sintomi-e-rimedi/",
            publishedDate = "2019-10-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Nel sangue la maggior parte degli steroidi sono legati a proteine, nel caso degli " +
                "androgeni le SHBG sono le proteine con maggior affinità di legame (60% del totale), " +
                "mentre l’albumina lega in maniera più labile il testosterone (38% del totale). ",
            articleTitle = "Ormoni maschili: quali sono e come funzionano?",
            url = "https://www.projectinvictus.it/ormoni-maschili/",
            publishedDate = "2019-09-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Il consumo è talmente elevato che per soddisfare le richieste del ciclo di Krebs " +
                "intervengono anche gli aminoacidi (glucogenetici), in particolare gli aminoacidi " +
                "ramificati che costituiscono il 35% delle proteine muscolari. ",
            articleTitle = "I grassi bruciano al fuoco dei carboidrati",
            url = "https://www.projectinvictus.it/i-grassi-bruciano-al-fuoco-dei-carboidrati/",
            publishedDate = "2018-06-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Per limitare questo catabolismo le diete low carb consigliano d’introdurre almeno " +
                "150-200g di proteine. ",
            articleTitle = "I grassi bruciano al fuoco dei carboidrati",
            url = "https://www.projectinvictus.it/i-grassi-bruciano-al-fuoco-dei-carboidrati/",
            publishedDate = "2018-06-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Decidiamo di proseguire l’incremento di carboidrati aggiungendo 10 grammi ogni 3-4 " +
                "giorni e lasciando invariate proteine e grassi. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Questo è infatti uno dei principali motivi per cui si sosteneva che assumere molti " +
                "carboidrati (fino a 1.2 g/kg nella singola assunzione) all’interno della finestra " +
                "anabolica (le prime 2 ore) fosse vantaggioso per ottenere una maggiore risintesi del " +
                "glicogeno muscolare (1,3). ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine rispetto agli altri macronutrienti hanno una ASD del 20-30%. ",
            articleTitle = "Calorie per dimagrire: capiamo Ormoni, macronutrienti e calorie",
            url = "https://www.projectinvictus.it/ormoni-macronutrienti-e-calorie/",
            publishedDate = "2016-03-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine muscolari hanno un ciclo che si chiama turnover, proteico cioè un ricambio " +
                "che per i muscoli si fa ogni 180 giorni circa. ",
            articleTitle = "Aminoacidi essenziali e non essenziali: basi di chimica",
            url = "https://www.projectinvictus.it/aminoacidi-biochimica/",
            publishedDate = "2016-02-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "In realtà, l’aumento del turnover proteico da esercizio fisico è solo circa 2 o 3 volte " +
                "rispetto alle 20-30 volte per carboidrati e grassi. ",
            articleTitle = "BCAA sempre e comunque utili?",
            url = "https://www.projectinvictus.it/bcaa-sempre-e-comunque-utili/",
            publishedDate = "2014-03-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine tipicamente contengono il 15-20% di BCAA, mentre alcune qualità di whey ne " +
                "contengono fino al 25%. ",
            articleTitle = "BCAA sempre e comunque utili?",
            url = "https://www.projectinvictus.it/bcaa-sempre-e-comunque-utili/",
            publishedDate = "2014-03-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Gli atomi H che si perdono tolgono intorno al 19% del contenuto energetico delle " +
                "proteine, portando il loro valore effettivo da 5,65 a 4kcal. ",
            articleTitle = "Le proteine fanno male? Impariamo a conoscerle!",
            url = "https://www.projectinvictus.it/proteine-fanno-male/",
            publishedDate = "2013-03-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Protein supplementation beyond total protein intakes of 1.62 g/kg/day resulted in no " +
                "further RET-induced gains in FFM. ",
            articleTitle = "A systematic review, meta-analysis and meta-regression of the effect of protein supplementation on resistance training-induced gains in muscle mass and strength in healthy adults",
            url = "https://pubmed.ncbi.nlm.nih.gov/28698222/",
            publishedDate = "2018",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "With protein supplementation, protein intakes at amounts greater than ~1.6 g/kg/day do " +
                "not further contribute RET-induced gains in FFM. ",
            articleTitle = "A systematic review, meta-analysis and meta-regression of the effect of protein supplementation on resistance training-induced gains in muscle mass and strength in healthy adults",
            url = "https://pubmed.ncbi.nlm.nih.gov/28698222/",
            publishedDate = "2018",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Protein combined with an exercise intervention significantly reduces fat mass (-0.80 kg; " +
                "95%CI, -1.32 to -0.28). ",
            articleTitle = "Nutritional and exercise interventions in individuals with sarcopenic obesity around retirement age: a systematic review and meta-analysis",
            url = "https://pubmed.ncbi.nlm.nih.gov/36882046/",
            publishedDate = "2023",
            kind = SourceKind.SYSTEMATIC_REVIEW
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "dieta a zona 40% carbo, 30% proteine, 30% grassi): è sempre meglio basarsi su g / kg " +
                "riferiti al peso ideale. ",
            articleTitle = "Definizione in palestra: come definire i muscoli",
            url = "https://www.projectinvictus.it/definizione-palestra/",
            publishedDate = "2024-01-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Come noterete la mia media calorica si è abbassata di 200 kcal circa da quella di " +
                "partenza, ed ho aumentato in proporzione l’intake proteico semplicemente per un discorso " +
                "di maggiore sazietà non per altro. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Alcuni alimenti proteici magri, soprattutto i derivati del latte light, possono " +
                "assolvere una funzione simile – ma con una dilatazione dei tempi digestivi di circa il " +
                "+50% rispetto ai precedenti. ",
            articleTitle = "Pre workout bodybuilding: cosa mangiare e cosa assumere prima della palestra?",
            url = "https://www.projectinvictus.it/pre-workout-palestra-bodybuilding/",
            publishedDate = "2022-01-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le fonti proteiche è meglio distribuirle in modo uniforme nell’arco delle 24 ore in modo " +
                "che ciascun pasto contenga 20-40 g di proteine (da alimenti o in alternativa proteine in " +
                "polvere) per volta. ",
            articleTitle = "Quando assumere proteine: prima o dopo allenamento?",
            url = "https://www.projectinvictus.it/proteine-prima-o-dopo-allenamento/",
            publishedDate = "2023-09-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "I cereali sono molto poveri di grassi e sono costituiti per circa il 70% da carboidrati " +
                "e per il 12% da proteine. ",
            articleTitle = "Dieta ipolipidica: cos’è e come funziona?",
            url = "https://www.projectinvictus.it/dieta-ipolipidica/",
            publishedDate = "2025-12-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine sono una componente fondamentale del muscolo (di cui costituiscono circa il " +
                "20%) e la funzione più nota è sicuramente quella di garantire la contrazione , che " +
                "avviene tramite lo scivolamento dei filamenti proteici di miosina rispetto a quelli di " +
                "actina. ",
            articleTitle = "Proteine palestra: quali prendere e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-palestra/",
            publishedDate = "2023-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.PROTEIN,
            statement =
                "Le proteine post-workout (circa 25 g di proteine o 10-12 g di aminoacidi essenziali) " +
                "sono utili specialmente nel caso in cui non hai fatto un pasto pre-allenamento o vieni " +
                "da abbastanza ore di digiuno. ",
            articleTitle = "Proteine palestra: quali prendere e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-palestra/",
            publishedDate = "2023-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Sapendo che 1 g di carboidrati = 4 kcal, dovrà assumere 361 g di carboidrati al giorno " +
                "(4,5 g/kg per il soggetto in questione). ",
            articleTitle = "Fabbisogno glucidico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-glucidico-carboidrati/",
            publishedDate = "2026-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "La maggior quota glucidica (50-70%) è meglio consumarla nelle ore vicine " +
                "all’allenamento. ",
            articleTitle = "Dieta personalizzata: la dieta che funziona",
            url = "https://www.projectinvictus.it/dieta-personalizzata/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "In genere si impostano prima proteine e grassi , destinando ai carboidrati le calorie " +
                "rimanenti: Per le proteine , negli sportivi in normo o ipercalorica si consiglia un " +
                "apporto tra 1,6, arrivando a 2-2,2 g/kg in fase di dimagrimento per aiutare a preservare " +
                "la massa muscolare. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Per i carboidrati , se sei attivo e ti alleni, soprattutto se ha una buona massa " +
                "muscolare, può beneficiare di un maggiore apporto di carboidrati, che mediamente varia " +
                "tra 3 e 7 g/kg. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "L’ Organizzazione Mondiale della Sanit à (OMS) consiglia di limitare l’ assunzione di " +
                "zuccheri liberi a meno del 10% delle calorie totali giornaliere . ",
            articleTitle = "Il fruttosio fa male?",
            url = "https://www.projectinvictus.it/fruttosio-fa-male/",
            publishedDate = "2025-10-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Non ci sono altri particolari segreti se non tenere una quota proteica intorno a 1,5 – " +
                "2,5 g/kg e di variare tra i grassi e i carboidrati. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Puoi consumare una porzione di 20-30g di cioccolato fondente dopo gli allenamenti per " +
                "fornire un apporto immediato di energia grazie ai carboidrati e ai grassi presenti nel " +
                "cioccolato, contribuendo al recupero post-esercizio. ",
            articleTitle = "I 10 migliori alimenti per la massa muscolare",
            url = "https://www.projectinvictus.it/alimenti-per-la-massa-muscolare/",
            publishedDate = "2025-07-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Per assumere un quantitativo più alto di proteine, consuma o aggiungi l’ albume : 100 g " +
                "contengono circa 11 g di proteine e non ha tracce di carboidrati e grassi. ",
            articleTitle = "Colazione proteica: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-proteica/",
            publishedDate = "2025-04-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Se invece hai già cominciato il periodo di massa, ricorda che dopo 4-10 settimane il " +
                "corpo sembrerà non rispondere più ad una dieta ipercalorica e iper-glucidica: i muscoli " +
                "si svuotano e se ti controlli la glicemia a digiuno sarà più alta di 5-15 punti rispetto " +
                "al tuo standard. ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Quando mangi pochi carboidrati puoi perdere in una settimana anche 2-4 kg , in quanto " +
                "cala l’ acqua intracellulare ed interstiziale. ",
            articleTitle = "Dimagrire velocemente e perdere peso",
            url = "https://www.projectinvictus.it/dimagrire-velocemente/",
            publishedDate = "2025-01-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Anzi, i carboidrati sono così fondamentali che devono ricoprire il 45-60% delle calorie " +
                "che assumi ogni giorno e sono importanti anche quando vuoi dimagrire , indifferentemente " +
                "dal momento della giornata in cui li mangi (anche la sera!). ",
            articleTitle = "Il pane fa ingrassare?",
            url = "https://www.projectinvictus.it/il-pane-fa-ingrassare/",
            publishedDate = "2024-10-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "In questi soggetti è bene porre maggiore attenzione alla quantità di carboidrati " +
                "assunta, senza eliminarli (> 2 g carboidrati / kg peso corporeo), incentivando " +
                "l’attività fisica e il raggiungimento di un peso corporeo ideale, con una buona massa " +
                "magra e muscolare. ",
            articleTitle = "Il pane fa ingrassare?",
            url = "https://www.projectinvictus.it/il-pane-fa-ingrassare/",
            publishedDate = "2024-10-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Questo avviene perché in un’alimentazione normale e sana (60% delle calorie derivanti da " +
                "carboidrati) noi diventiamo dipendenti da essi. ",
            articleTitle = "Saltare la colazione sì o no? Fa bene o fa male?",
            url = "https://www.projectinvictus.it/saltare-la-colazione/",
            publishedDate = "2024-08-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "I glucidi, in soggetti sani e non insulino resistenti , vengono facilmente convertiti in " +
                "calore quando sono in eccesso e si stima che in soggetti sani soltanto il 10% " +
                "dell’eccesso si converta effettivamente in grassi. ",
            articleTitle = "Cosa sono le calorie?",
            url = "https://www.projectinvictus.it/caloria/",
            publishedDate = "2024-07-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Per comprendere meglio la questione, che è spesso fuorviante, facciamo un esempio: la " +
                "carota cotta ha un indice glicemico alto ma al suo interno ha pochi carboidrati, così " +
                "come l’ albicocca , bisogna quindi mangiarne alti quantitativi per raggiungere 50 g. ",
            articleTitle = "Indice Glicemico",
            url = "https://www.projectinvictus.it/indice-glicemico/",
            publishedDate = "2024-05-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Per chi fa palestra la deplezione delle scorte di glicogeno non è un problema: un " +
                "allenamento le riduce solo del 24-40% . ",
            articleTitle = "Carboidrati dopo l’allenamento in palestra",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-palestra/",
            publishedDate = "2024-03-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "In realtà, questa finestra anabolica dura più tempo: l’importante non è fare un preciso " +
                "pasto post workout ma assicurarsi di assumere abbastanza carboidrati (oltre che energia, " +
                "proteine, grassi) nel corso delle 24 ore . ",
            articleTitle = "Carboidrati dopo l’allenamento in palestra",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-palestra/",
            publishedDate = "2024-03-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Associare per ogni blocchetto di proteine anche 1 blocchetto di carboidrati e 1 " +
                "blocchetto di grassi ( rapporto 1:1:1 tra i blocchetti ). ",
            articleTitle = "Dieta a zona: come funziona e menù",
            url = "https://www.projectinvictus.it/dieta-a-zona/",
            publishedDate = "2024-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Ma non solo, anche a livello di salute generale e prospettiva di vita, i carboidrati " +
                "dovrebbero occupare il 45-60% del tuo fabbisogno calorico giornaliero. ",
            articleTitle = "Paleo dieta: come funziona e cosa mangiare",
            url = "https://www.projectinvictus.it/la-paleo-dieta/",
            publishedDate = "2024-02-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Dieta metabolica: come funziona La dieta metabolica è una dieta chetogenica ciclica in " +
                "cui i carboidrati sono tenuti al minimo (mediamente 30-50 g al giorno) per 4-5 giorni a " +
                "settimana, intervallati da ricariche glucidiche nel weekend o il mercoledì. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "In questo periodo di scarico mangi prevalentemente grassi e proteine, mentre i " +
                "carboidrati sono ridotti al minimo (30 g), preferibilmente da verdura fibrosa, per " +
                "assumere il giusto quantitativo di fibre alimentari. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Se dopo 48 ore che non mangi carboidrati hai un calo di energie e di attenzione " +
                "vigoroso, consiglia di inserire una mini ricarica di 12-24 ore il mercoledì. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Puoi così arrivare anche ad avere un approccio chetogenico 4 giorni a settimana (lunedì, " +
                "martedì, giovedì, venerdì) e glucidico 3 giorni (mercoledì, sabato, domenica). ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Tagliare completamente i carboidrati o i grassi non è una scelta fattibile: i " +
                "carboidrati sono il nutriente principale e i grassi è fondamentale tenerli minimamente " +
                "alti anche in ipocalorica (senza scendere sotto i 0,8 g/kg al giorno per evitare " +
                "squilibri ormonali). ",
            articleTitle = "Aumentare la massa muscolare nelle donne: come fare",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare-donne/",
            publishedDate = "2024-01-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "I giorni ad alto apporto glucidico hanno 50-100 g d i carboidrati in più rispetto a " +
                "quelli a basso apporto glucidico . ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Le linee guida consigliano di consumare al massimo il 10% di zuccheri nella propria " +
                "dieta all’interno del fabbisogno di carboidrati. ",
            articleTitle = "Dieta senza zuccheri",
            url = "https://www.projectinvictus.it/dieta-senza-zuccheri/",
            publishedDate = "2023-12-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Le 4 fasi della dieta Atkins Fase 1 – Induzione La prima fase limita l’introduzione di " +
                "carboidrati ad un massimo di 20g/die per un periodo di tempo di 2 settimane, ottenuti " +
                "principalmente da fonti vegetali. ",
            articleTitle = "Dieta Atkins: come funziona, quanto si perde, menù",
            url = "https://www.projectinvictus.it/dieta-atkins/",
            publishedDate = "2023-06-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Fase 2 – Bilanciamento Nella seconda fase, la dieta Atkins prevede l’ aggiunta " +
                "progressiva e controllata di 5 g di carboidrati alla settimana fino a raggiungere circa " +
                "25-50g di glucidi, livello che permette di testare la propria tolleranza glucidica per " +
                "perdere circa 1 kg alla settimana. ",
            articleTitle = "Dieta Atkins: come funziona, quanto si perde, menù",
            url = "https://www.projectinvictus.it/dieta-atkins/",
            publishedDate = "2023-06-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Questi alimenti apportano una notevole quantità di carboidrati complessi, circa un " +
                "10-12% di proteine ma anche fibre alimentari, vitamine del gruppo B e sali minerali come " +
                "magnesio, zinco e ferro. ",
            articleTitle = "Dieta flexitariana: cos’è, cosa mangiare, menù",
            url = "https://www.projectinvictus.it/flexitariana/",
            publishedDate = "2023-06-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Solitamente la concentrazione di carboidrati è del 6-8% ( bevande isotoniche ). ",
            articleTitle = "Bevande energetiche per lo sport",
            url = "https://www.projectinvictus.it/bevande-energetiche/",
            publishedDate = "2023-06-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Quelle ipotoniche hanno una minore concentrazione di carboidrati (< 6 g/100 ml) e sono " +
                "utili quando l’idratazione è più necessaria rispetto all’apporto glucidico. ",
            articleTitle = "Bevande energetiche per lo sport",
            url = "https://www.projectinvictus.it/bevande-energetiche/",
            publishedDate = "2023-06-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Normalmente viene consigliato di integrare le bevande energetiche durante attività " +
                "fisiche continue superiori a 60-90 minuti ma, quando particolarmente intense, puoi " +
                "integrare anche con attività più brevi – in questo caso è opportuno diminuire il " +
                "contenuto di acqua e di carboidrati. ",
            articleTitle = "Bevande energetiche per lo sport",
            url = "https://www.projectinvictus.it/bevande-energetiche/",
            publishedDate = "2023-06-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Infatti, ipotizzando che le scorte di glicogeno siano piene, una singola sessione di " +
                "allenamento riduce il deposito, soprattutto nelle fibre di tipo II, del 25-40% a seconda " +
                "della durata, dell’intensità, del volume. ",
            articleTitle = "Bevande energetiche per lo sport",
            url = "https://www.projectinvictus.it/bevande-energetiche/",
            publishedDate = "2023-06-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Basti pensare che dalle linee guida i carboidrati sono il macronutriente che andrebbe " +
                "mangiato in quantità maggiore nella propria dieta, fino a coprire il 45-60% del proprio " +
                "fabbisogno energetico. ",
            articleTitle = "La pasta fa ingrassare: miti e luoghi comuni",
            url = "https://www.projectinvictus.it/la-pasta-fa-ingrassare/",
            publishedDate = "2023-04-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Per dare alcuni riferimenti, solitamente per la media della popolazione si consiglia di " +
                "non scendere sotto i 2 g di carboidrati per kg di peso corporeo. ",
            articleTitle = "La pasta fa ingrassare: miti e luoghi comuni",
            url = "https://www.projectinvictus.it/la-pasta-fa-ingrassare/",
            publishedDate = "2023-04-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Per avere una dieta equilibrata e non escludere altri gruppi alimentari, sicuramente chi " +
                "ha un apporto glucidico più basso dovrà “accontentarsi” di porzioni minori ( 50-70 g di " +
                "pasta ), mentre chi ce l’ha più alto potrà permettersi porzioni più elevate (anche 150 g " +
                "o più ). ",
            articleTitle = "La pasta fa ingrassare: miti e luoghi comuni",
            url = "https://www.projectinvictus.it/la-pasta-fa-ingrassare/",
            publishedDate = "2023-04-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Questo stato può essere raggiunto attraverso una riduzione drastica dell’apporto di " +
                "glucidi, di solito inferiori a 50 g/die , per più settimane. ",
            articleTitle = "Dieta senza carboidrati: come funziona, cosa mangiare, controindicazioni",
            url = "https://www.projectinvictus.it/dieta-senza-carboidrati/",
            publishedDate = "2023-04-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Una dieta povera di carboidrati è una dieta in cui l’apporto di questo macronutriente è " +
                "minore rispetto alle linee guida internazionali che si aggirano sul 45-65% dell’energia " +
                "totale. ",
            articleTitle = "Dieta povera di carboidrati: cos’è, vantaggi e menù",
            url = "https://www.projectinvictus.it/dieta-povera-di-carboidrati/",
            publishedDate = "2023-03-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Le proteine sono una classe di molecole biologiche molto presenti in natura : negli " +
                "organismi animali rappresentano circa il 50% del peso secco, mentre in quelli vegetali " +
                "un po’ meno poichè c’è un alto contenuto di cellulosa (una molecola che fa parte della " +
                "famiglia dei carboidrati). ",
            articleTitle = "Aminoacidi: cosa sono e che funzione svolgono?",
            url = "https://www.projectinvictus.it/cosa-sono-gli-aminoacidi/",
            publishedDate = "2022-12-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Quantitativi ben superiori alle quote raccomandate per i sedentari (0,8g/kg) Carboidrati " +
                ": gli atleti natural hanno benefici nel tenere alta la quota di carboidrati. ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            publishedDate = "2022-12-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Per quanto riguarda attività intense e prolungate, è anche opportuno assumere una " +
                "sufficiente quantità di carboidrati nell’arco delle 24 ore in modo da integrare le " +
                "scorte di glicogeno epatico e muscolare. ",
            articleTitle = "Allenamento con il caldo nel bodybuilding: benefici, rischi e rimedi",
            url = "https://www.projectinvictus.it/allenarsi-con-il-caldo/",
            publishedDate = "2021-11-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Se corri a digiuno con le scorte di glicogeno depauperate il consumo di grassi aumenta " +
                "mediamente di un 20-30%. ",
            articleTitle = "Corsa a digiuno a stomaco vuoto per dimagrire",
            url = "https://www.projectinvictus.it/correre-a-digiuno/",
            publishedDate = "2021-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Un allenamento con i pesi non esaurisce tutte le riserve di glicogeno e le depleta per " +
                "valori non superiori al 20-40%.[6][7][8] a seconda della durata dell’allenamento, dagli " +
                "esercizi e dall’intensità dell’attività fisica svolta. ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "L’OMS consiglia per gli zuccheri semplici di non superare il 10% delle calorie " +
                "giornaliere, pertanto è bene, quando possibile, limitare tutte e due questi zuccheri " +
                "semplici. ",
            articleTitle = "Fruttosio o zucchero? Qual è il migliore?",
            url = "https://www.projectinvictus.it/fruttosio-o-zucchero-qual-e-il-migliore/",
            publishedDate = "2021-10-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "In generale, puoi continuare a considerare tutte e due gli zuccheri semplici come se " +
                "avessero le canoniche 4 kcal/g. ",
            articleTitle = "Fruttosio o zucchero? Qual è il migliore?",
            url = "https://www.projectinvictus.it/fruttosio-o-zucchero-qual-e-il-migliore/",
            publishedDate = "2021-10-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Infine per i carboidrati il quantitativo raccomandato minimo è di 2.5 g/kg (3 per gli " +
                "sportivi) . ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Le linee guida vedono nei glucidi il principale macronutriente dell’alimentazione con " +
                "una quota che va dal 45-60% delle calorie giornaliere. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Si dividono in carboidrati complessi e semplici a seconda della loro struttura e, in " +
                "termini pratici, non ci sono differenze sostanziali in termini di calorie a seconda che " +
                "si preferisca i primi o i secondi: possiamo considerare sempre valide le 4 kcal/g. ",
            articleTitle = "Calorie macronutrienti",
            url = "https://www.projectinvictus.it/calorie-macronutrienti/",
            publishedDate = "2021-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Ad esempio, atleti di resistenza con una richiesta energetica molto elevata possono " +
                "spingersi anche oltre il 60% di carboidrati oppure soggetti in forte dieta ipocalorica " +
                "avranno un apporto proteico maggiore. ",
            articleTitle = "Calorie macronutrienti",
            url = "https://www.projectinvictus.it/calorie-macronutrienti/",
            publishedDate = "2021-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Superata infatti una determinata quota di carboidrati (350-400 g) l’apporto di fibre è " +
                "assicurato anche mangiando fonti raffinate. ",
            articleTitle = "Calorie macronutrienti",
            url = "https://www.projectinvictus.it/calorie-macronutrienti/",
            publishedDate = "2021-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "In totale, i glucidi dovrebbero ricoprire il 45-60% del proprio fabbisogno energetico " +
                "giornaliero oppure almeno 2-3 g/kg di peso corporeo , sempre in relazione alla spesa " +
                "energetica, al proprio obbiettivo e stile di vita. ",
            articleTitle = "Carboidrati semplici e complessi: qual è la differenza?",
            url = "https://www.projectinvictus.it/quali-sono-i-carboidrati/",
            publishedDate = "2021-09-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Barrette proteiche, proteine in polvere, prodotti “dietetici”, bibite 0 kcal, alimenti " +
                "light ,… sono, nella maggior parte dei casi, presenti nella dieta di chi frequenta la " +
                "palestra e, soprattutto, di chi vuole dimagrire o stare attento a non assumere troppi " +
                "zuccheri e calorie. ",
            articleTitle = "Dolcificanti artificiali: fanno male e fanno ingrassare?",
            url = "https://www.projectinvictus.it/dolcificanti-fanno-male-fanno-ingrassare/",
            publishedDate = "2021-08-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Il corpo impiega il 5% delle calorie degli zuccheri per digerirle ed assimilarle. ",
            articleTitle = "Carboidrati semplici: funzioni, benefici ed esempi",
            url = "https://www.projectinvictus.it/carboidrati-semplici/",
            publishedDate = "2021-08-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Come vedremo, una dieta sana e bilanciata non dovrebbe comprendere più del 5-10% delle " +
                "calorie provenienti dagli zuccheri semplici aggiunti (25-50 g). ",
            articleTitle = "Carboidrati semplici: funzioni, benefici ed esempi",
            url = "https://www.projectinvictus.it/carboidrati-semplici/",
            publishedDate = "2021-08-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "L’OMS consiglia di consumare al massimo il 5-10% delle calorie da zuccheri semplici " +
                "(25-50 g al giorno). ",
            articleTitle = "Carboidrati semplici: funzioni, benefici ed esempi",
            url = "https://www.projectinvictus.it/carboidrati-semplici/",
            publishedDate = "2021-08-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Una buona scelta è quella di circoscrivere un 50-70% dell’introito glucidico giornaliero " +
                "e comunque non scendere sotto 1.5 g/kg di peso corporeo nei pasti vicini " +
                "all’allenamento, fermo restando che su soggetti in fisiologia l’introito calorico è il " +
                "principale fattore da tenere in considerazione. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Tutti noi abbiamo un quantitativo glucidico da assumere nel corso della giornata, che lo " +
                "conti oppure no a fine delle 24 ore hai assunto tot grammi di carboidrati. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Come nel caso dei carboidrati, anche l’integrazione dei sali è più utile quando " +
                "l’allenamento dura molto (>90 minuti) e/o l’ambiente è molto caldo . ",
            articleTitle = "Intra workout per l’allenamento in palestra",
            url = "https://www.projectinvictus.it/intra-workout-cosa-assumere-durante-allenamento/",
            publishedDate = "2021-08-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Questo frutto, o meglio, queste bacche hanno circa 46 kcal ogni 100 gr, dovute perlopiù " +
                "a zuccheri semplici ed è quindi assolutamente introducibile nel contesto di una dieta " +
                "dimagrante . ",
            articleTitle = "Mirtillo rosso: proprietà, benefici e controindicazioni",
            url = "https://www.projectinvictus.it/mirtillo-rosso-proprieta-benefici/",
            publishedDate = "2021-07-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "A fine ipertrofico è tuttavia utile preferire il consumo dei carboidrati rispetto a " +
                "quello dei grassi, il cui fabbisogno può quindi essere diminuito fino a 0.5 g/kg senza " +
                "scendere verso valori inferiori. ",
            articleTitle = "Grassi buoni: quali sono e a cosa servono?",
            url = "https://www.projectinvictus.it/grassi-buoni/",
            publishedDate = "2021-06-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Non ci sono infatti riserve di amminoacidi nell’organismo, come è il glicogeno per il " +
                "glucosio o l’organo adiposo per gli acidi grassi, e il muscolo è quello che presenta un " +
                "contenuto proteico più elevato (circa il 20% del suo peso). ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "A questo punto, per ottenere i grammi dei macronutrienti da raggiungere bisogna " +
                "ricordare che 1 g di carboidrati e 1g di proteine corrispondono a 4 kcal, mentre 1 g di " +
                "grassi a 9 kcal . ",
            articleTitle = "IIFYM: che cos’è e come funziona?",
            url = "https://www.projectinvictus.it/iifym/",
            publishedDate = "2021-01-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Non che di per sé i “cibi non sani” siano un problema quando occasionali (come il " +
                "comprenderli nel 20%), ma se molto consumati facilmente fanno sforare dalle quantità " +
                "consigliate: sia zuccheri che grassi saturi non dovrebbero superare il 10% dell’apporto " +
                "calorico giornaliero, mentre i trans l’1%. ",
            articleTitle = "IIFYM: che cos’è e come funziona?",
            url = "https://www.projectinvictus.it/iifym/",
            publishedDate = "2021-01-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Perché questo avvenga devi fare un pasto glucidico con almeno 80-100g di amido, per " +
                "stimolare correttamente l’ insulina . ",
            articleTitle = "I carboidrati sono meglio a pranzo o a cena?",
            url = "https://www.projectinvictus.it/carboidrati-giorno-sera/",
            publishedDate = "2020-11-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Il quantitativo di glucidici da assumere andrebbe calcolato nelle 24 ore o nella " +
                "settimana . ",
            articleTitle = "I carboidrati sono meglio a pranzo o a cena?",
            url = "https://www.projectinvictus.it/carboidrati-giorno-sera/",
            publishedDate = "2020-11-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "In termini pratici, se fai un past o da non più di 600 kcal e inoltre hai anche una " +
                "buona massa magra puoi mescolare glucidi e lipidi senza il timore di ingrassare. ",
            articleTitle = "Grassi e carboidrati nello stesso pasto fanno ingrassare?",
            url = "https://www.projectinvictus.it/grassi-ed-carboidrati-vanno-mangiati-assieme/",
            publishedDate = "2020-08-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "La quota raccomandata di zucchero dall’OMS è quella di non superare il 5% delle calorie " +
                "dagli zuccheri aggiunti, stiamo parlando di 25g ovvero 6 cucchiaini da cucina. ",
            articleTitle = "Il glucosio: cos’è e dove si trova",
            url = "https://www.projectinvictus.it/glucosio/",
            publishedDate = "2020-05-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Se puoi assumili almeno il 60% della quota glucidica nelle 5h intorno all’allenamento in " +
                "modo da migliorare il partizionamento calorico del glucosio verso il muscolo. ",
            articleTitle = "Il glucosio: cos’è e dove si trova",
            url = "https://www.projectinvictus.it/glucosio/",
            publishedDate = "2020-05-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Se ti alleni, il range temporale in cui è preferibile (non indispensabile) assumere la " +
                "maggior quota glucidica della giornata è nelle 3 ore prima e/o dopo allenamento per " +
                "motivi di richieste energetiche aumentate e conseguente depauperamento delle scorte di " +
                "glicogeno, che andranno ripristinate. ",
            articleTitle = "Carboidrati: la guida completa",
            url = "https://www.projectinvictus.it/carboidrati-guida-completa/",
            publishedDate = "2020-04-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "La percentuale di carboidrati consigliata corrisponde a 45-60% del fabbisogno energetico " +
                "totale giornaliero ed è abbastanza ampio proprio perché puoi “muoverti” in questo " +
                "intervallo a seconda della tua soggettività. ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Gli sportivi rispetto ai sedentari: è chiaro che una persona attiva necessita di una " +
                "quota maggiore di energia, con un quantitativo glucidico anche doppio rispetto ad un " +
                "sedentario e che può arrivare fino a 10-12g di carboidrati per kg di peso corporeo al " +
                "giorno! ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Quanti carboidrati semplici Al giorno al massimo il 10% dell’energia deve provenire " +
                "dagli zuccheri, il che significa che è bene limitarne il consumo. ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Oltre a questo, consideriamo sempre che l’insulina è l’ormone anabolico per eccellenza , " +
                "sfruttarlo è in generale una buona mossa, così come l’assunzione della maggior frazione " +
                "glucidica (50-70%) dei carboidrati giornalieri nelle ore a ridosso dell’allenamento. ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Una volta risolta l’equazione, sai quante calorie assumere con i carboidrati ; per " +
                "ottenere i grammi sarà sufficiente dividere questo numero per 4 (dato che 1 g di " +
                "carboidrati corrispondono a 4 kcal). ",
            articleTitle = "Quanti carboidrati per dimagrire?",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno-per-dimagrire/",
            publishedDate = "2020-04-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Gli zuccheri (i carboidrati semplici) non sono da escludere ma sicuramente da limitare: " +
                "l’ideale è ricavare al massimo il 10-12% del fabbisogno energetico giornaliero dagli " +
                "zuccheri . ",
            articleTitle = "Quanti carboidrati per dimagrire?",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno-per-dimagrire/",
            publishedDate = "2020-04-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Un grammo di destrosio corrisponde a 3.74 kcal e, tra i vari parametri nutrizionali, è " +
                "classificato come glucide . ",
            articleTitle = "Destrosio in palestra: che cos’è e a cosa serve",
            url = "https://www.projectinvictus.it/destrosio-bodybuilding-palestra/",
            publishedDate = "2020-03-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Idealmente, per gli atleti è consigliato il consumo di 1.0-1.5 g di carboidrati/kg di " +
                "peso corporeo ogni 2 ore nelle 6 ore dopo l’allenamento. ",
            articleTitle = "Destrosio in palestra: che cos’è e a cosa serve",
            url = "https://www.projectinvictus.it/destrosio-bodybuilding-palestra/",
            publishedDate = "2020-03-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "I carboidrati dovrebbero comprendere il 55-60% del fabbisogno calorico giornaliero, " +
                "mentre per le proteine il valore consigliato è 1.4 – 2 g/kg di peso corporeo . ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Carboidrati 45-60% Nonostante i carboidrati alimentari non siano essenziali alla " +
                "sopravvivenza, il glucosio lo è; in natura il modo più conveniente, sostenibile e " +
                "pratico di soddisfare il fabbisogno di glucosio è consumare le fonti amidacee. ",
            articleTitle = "Dieta dimagrante: come farla in modo efficace",
            url = "https://www.projectinvictus.it/dieta-dimagrante/",
            publishedDate = "2018-10-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Dopo i pasti l’ azione dell’insulina abbassa i livelli di glucagone , ma questo avviene " +
                "quando seguiamo un’alimentazione normale e bilanciata (in cui i carboidrati sono almeno " +
                "il 50% delle calorie). ",
            articleTitle = "Insulina e glucagone",
            url = "https://www.projectinvictus.it/insulina-e-glucagone/",
            publishedDate = "2018-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Tenete presente che il rapporto ottimale per questi tipi di carboidrati in polvere e " +
                "l’acqua è 1:1, quindi per 50 gr di Vitargo vanno bene 500 ml di acqua. ",
            articleTitle = "Crossfit ed alimentazione (Vademecum per il non agonista)",
            url = "https://www.projectinvictus.it/crossfit-ed-alimentazione-vademecum-non-agonista/",
            publishedDate = "2017-04-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Decidiamo pertanto di intervenire modificando il piano alimentare con un incremento " +
                "dell’apporto di carboidrati di 10 grammi a settimana per le prossime 4 settimane (v . ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Decidiamo di proseguire l’incremento di carboidrati aggiungendo 10 grammi ogni 3-4 " +
                "giorni e lasciando invariate proteine e grassi. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Nel medio termine, infatti, a parità di calorie e macronutrienti, otterrete gli stessi " +
                "identici risultati utilizzando una semplice ciclizzazione ON-OFF con un 20-25% di " +
                "differenza nell’apporto dei carboidrati e introducendo un REFEED ogni tanto nelle ultime " +
                "settimane. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Completamente al di fuori del mondo scientifico, negli ultimi anni alcuni autori " +
                "italiani hanno espresso delle teorie sui presunti svantaggi dell’ingestione di cibo, in " +
                "particolar modo di carboidrati, entro i primi 45-60 minuti post-allenamento. ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Di conseguenza la NPWHC dovrebbe sostenere di evitare qualsiasi fonte di carboidrati che " +
                "mantiene la glicemia elevata anche subito dopo l’esercizio, come un pasto misto a base " +
                "di carboidrati mangiato 5-6 ore prima. ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Questo è infatti uno dei principali motivi per cui si sosteneva che assumere molti " +
                "carboidrati (fino a 1.2 g/kg nella singola assunzione) all’interno della finestra " +
                "anabolica (le prime 2 ore) fosse vantaggioso per ottenere una maggiore risintesi del " +
                "glicogeno muscolare (1,3). ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Ciò che contraddice questa ipotesi è che la sovraregolazione di questi processi avviene " +
                "in particolar modo nelle prime 2 ore post-allenamento (1,3), che su queste basi sembra " +
                "essere uno dei momenti in cui può essere relativamente vantaggioso assumere carboidrati " +
                "in grandi quantità. ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "In realtà, l’aumento del turnover proteico da esercizio fisico è solo circa 2 o 3 volte " +
                "rispetto alle 20-30 volte per carboidrati e grassi. ",
            articleTitle = "BCAA sempre e comunque utili?",
            url = "https://www.projectinvictus.it/bcaa-sempre-e-comunque-utili/",
            publishedDate = "2014-03-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Due grammi di L-arginina possono aumentare del 40% la velocità di sintesi di glicogeno " +
                "post allenamento. ",
            articleTitle = "La sensibilità insulinica (Parte 3)",
            url = "https://www.projectinvictus.it/la-sensibilita-insulinica-parte-3/",
            publishedDate = "2014-01-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Ad esempio, se mangi molti carboidrati e poi ti pesi, noterai subito che l’ago della " +
                "bilancia indica un peso corporeo maggiore rispetto al solito: infatti, ad ogni grammo di " +
                "glucosio immagazzinato sotto forma di glicogeno corrispondono circa 3 g di acqua. ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARBS,
            statement =
                "Nel breve periodo si può assistere a: Perdita di peso rapida : si assiste ad un cospicuo " +
                "calo ponderale sia per aumento della diuresi legata all’esaurimento del glicogeno " +
                "epatico (1g di glicogeno trattiene 3g di acqua circa) sia per l’aumento della lipolisi e " +
                "dell’ossidazione degli acidi grassi. ",
            articleTitle = "Dieta povera di carboidrati: cos’è, vantaggi e menù",
            url = "https://www.projectinvictus.it/dieta-povera-di-carboidrati/",
            publishedDate = "2023-03-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Secondo i LARN, punto di riferimento per l’assunzione dei nutrienti in Italia, la " +
                "quantità di grassi da assumere per la popolazione italiana corrisponde al 20-35% del " +
                "totale calorico giornaliero oppure fino a 1,5 g di grassi/kg peso corporeo. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Bisogna, però, differenziare la quantità dei grassi a seconda della loro qualità: i " +
                "grassi saturi vanno limitati al massimo al 10% del tuo fabbisogno calorico giornaliero " +
                "totale. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Quanti grassi al giorno nel bodybuilding La quantità consigliata (25-30%) è consigliata " +
                "anche a chi fa bodybuilding, anche se c’è qualche differenza a seconda che tu sia in " +
                "fase di definizione o di massa. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "In fase di massa , le calorie sono più alte e quindi anche i grassi potranno essere " +
                "rialzati, sempre stando nel range 20-35% o alzando al massimo al 40%. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Nella dieta ipocalorica le calorie derivanti dai grassi non dovrebbero scendere oltre al " +
                "20% se vuoi limitare l’abbassamento dell’ormone gonadico. ",
            articleTitle = "Come aumentare il testosterone",
            url = "https://www.projectinvictus.it/come-aumentare-il-testosterone/",
            publishedDate = "2026-08-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Secondo le linee guida LARN , una dieta ottimale prevede che il 20-35% dell’energia " +
                "giornaliera provenga dai grassi. ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Non è così, però, per la dieta low-fat, in cui i grassi ricoprono al massimo il 15% " +
                "dell’introito energetico giornaliero. ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Inoltre, le donne tendenzialmente avranno bisogno di una quantità maggiore di grassi " +
                "rispetto agli uomini (minimo 0,8 g/kg). ",
            articleTitle = "Dieta low-fat per dimagrire",
            url = "https://www.projectinvictus.it/dieta-low-fat/",
            publishedDate = "2026-07-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Grassi : 20-30% dell’introito calorico, senza scendere al di sotto dei 0.5g grassi/kg di " +
                "peso corporeo, da consumare preferibilmente lontano dall’allenamento. ",
            articleTitle = "Dieta personalizzata: la dieta che funziona",
            url = "https://www.projectinvictus.it/dieta-personalizzata/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "In genere si impostano prima proteine e grassi , destinando ai carboidrati le calorie " +
                "rimanenti: Per le proteine , negli sportivi in normo o ipercalorica si consiglia un " +
                "apporto tra 1,6, arrivando a 2-2,2 g/kg in fase di dimagrimento per aiutare a preservare " +
                "la massa muscolare. ",
            articleTitle = "La dieta flessibile",
            url = "https://www.projectinvictus.it/dieta-flessibile/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Solitamente come attività aerobica per consumare grassi viene consigliata quella a bassa " +
                "intensità, ma non è una proposta valida se sei già ben allenato e sportivo: in questo " +
                "caso, meglio preferire esercizi ad alta intensità di una certa durata (30-60 minuti) . ",
            articleTitle = "Il migliore allenamento per dimagrire",
            url = "https://www.projectinvictus.it/allenamento-per-dimagrire/",
            publishedDate = "2026-05-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per gli sportivi , i grassi dovrebbero coprire circa il 30% del fabbisogno energetico, " +
                "ma nel periodo di dimagrimento questa quota può scendere al 20%. ",
            articleTitle = "Dieta ipolipidica: cos’è e come funziona?",
            url = "https://www.projectinvictus.it/dieta-ipolipidica/",
            publishedDate = "2025-12-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "I grassi non devono superare il 30% dell’energia giornaliera e non più del 10% deve " +
                "provenire da quelli saturi e trans (8% se le LDL sono elevate). ",
            articleTitle = "Dieta ipolipidica: cos’è e come funziona?",
            url = "https://www.projectinvictus.it/dieta-ipolipidica/",
            publishedDate = "2025-12-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Velocità con cui perdi grasso: 0,5 – 1 % del proprio peso corporeo a settimana. ",
            articleTitle = "Pettorali scolpiti in poco tempo",
            url = "https://www.projectinvictus.it/pettorali-scolpiti/",
            publishedDate = "2025-10-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Non ci sono altri particolari segreti se non tenere una quota proteica intorno a 1,5 – " +
                "2,5 g/kg e di variare tra i grassi e i carboidrati. ",
            articleTitle = "Addominali scolpiti: come ottenerli",
            url = "https://www.projectinvictus.it/addominali-scolpiti/",
            publishedDate = "2025-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Si è visto che soggetti obesi, perdendo il 10% del proprio peso corporeo, riducono in " +
                "modo importante l’accumulo di grasso ectopico. ",
            articleTitle = "Grasso viscerale: cos’è e come eliminarlo",
            url = "https://www.projectinvictus.it/grasso-viscerale/",
            publishedDate = "2025-09-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Inoltre, anche se olio, frutta secca, avocado, salmone sono fonti “buone” di grassi, " +
                "sono comunque da limitare perché le calorie dei grassi sono sempre 9 kcal/grammo a " +
                "prescindere dall’alimento e che facciano bene o male alla salute. ",
            articleTitle = "Dieta per avere gli addominali",
            url = "https://www.projectinvictus.it/dieta-per-addominali/",
            publishedDate = "2025-08-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Una porzione di 100g di pollo fornisce ben 27g di proteine, rendendolo una scelta ideale " +
                "se cerchi un elevato apporto proteico con un ridotto contenuto di grassi. ",
            articleTitle = "I 10 migliori alimenti per la massa muscolare",
            url = "https://www.projectinvictus.it/alimenti-per-la-massa-muscolare/",
            publishedDate = "2025-07-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Puoi consumare una porzione di 20-30g di cioccolato fondente dopo gli allenamenti per " +
                "fornire un apporto immediato di energia grazie ai carboidrati e ai grassi presenti nel " +
                "cioccolato, contribuendo al recupero post-esercizio. ",
            articleTitle = "I 10 migliori alimenti per la massa muscolare",
            url = "https://www.projectinvictus.it/alimenti-per-la-massa-muscolare/",
            publishedDate = "2025-07-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "In media per perdere davvero 1 kg di grasso ci vogliono 2-4 settimane . ",
            articleTitle = "Non riesco a dimagrire: rimedi",
            url = "https://www.projectinvictus.it/non-riesco-a-dimagrire/",
            publishedDate = "2025-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Questo stato di “carenza energetica volontaria” (e necessaria) non dev’essere troppo " +
                "marcata ma moderata: – 350-500 kcal al giorno rispetto al fabbisogno, in modo da poter " +
                "essere sostenibile nel tempo e perdere grasso piuttosto che massa magra. ",
            articleTitle = "Non riesco a dimagrire: rimedi",
            url = "https://www.projectinvictus.it/non-riesco-a-dimagrire/",
            publishedDate = "2025-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Se volessimo sapere quante di queste 600 kcal provengono dai grassi bisogna considerare " +
                "il quoziente respiratorio . ",
            articleTitle = "Corsa brucia grassi per dimagrire",
            url = "https://www.projectinvictus.it/corsa-brucia-grassi/",
            publishedDate = "2025-06-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Un grammo di grasso nel nostro corpo sono 7 kcal (non 9 kcal come nei grassi alimentari) " +
                "e otteniamo quindi 17 g di grasso consumati ogni 10 km. ",
            articleTitle = "Corsa brucia grassi per dimagrire",
            url = "https://www.projectinvictus.it/corsa-brucia-grassi/",
            publishedDate = "2025-06-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Il tuorlo è molto meno proteico e ha una natura più lipidica di cui il colesterolo " +
                "costituisce il 5% del peso dell’uovo intero – da precisare, che questo dà origine a " +
                "lipoproteine meno facilmente ossidabili (e quindi meno dannose) rispetto al colesterolo " +
                "di altri alimenti. ",
            articleTitle = "Proteine animali e vegetali: quali sono le migliori",
            url = "https://www.projectinvictus.it/proteine-animali-e-vegetali-a-confronto/",
            publishedDate = "2025-05-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per una perdita di peso e di grasso efficace e fisiologica è consigliato perdere lo " +
                "0,5-1% del proprio peso corporeo a settimana . ",
            articleTitle = "La dieta iperproteica per il dimagrimento",
            url = "https://www.projectinvictus.it/dieta-iperproteica-dimagrimento/",
            publishedDate = "2025-05-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per assumere un quantitativo più alto di proteine, consuma o aggiungi l’ albume : 100 g " +
                "contengono circa 11 g di proteine e non ha tracce di carboidrati e grassi. ",
            articleTitle = "Colazione proteica: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-proteica/",
            publishedDate = "2025-04-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Con questo deficit ti puoi aspettare di perdere 350-500 g di grasso a settimana ; se " +
                "vuoi dimagrire più rapidamente devi mangiare di meno e/o muoverti di più. ",
            articleTitle = "Dieta ipocalorica per dimagrire: la guida completa",
            url = "https://www.projectinvictus.it/dieta-ipocalorica/",
            publishedDate = "2025-01-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Quanti kg puoi perdere in una settimana In una settimana puoi perdere circa 1-5 kg , a " +
                "seconda del tuo peso di partenza e della tua condizione fisica: più pesi e più hai una " +
                "cattiva composizione corporea (grasso e liquidi in eccesso, massa magra bassa) più " +
                "riesci a perdere kg. ",
            articleTitle = "Come dimagrire in una settimana",
            url = "https://www.projectinvictus.it/dimagrire-in-una-settimana/",
            publishedDate = "2025-01-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per ottenere davvero risultati perdendo grasso è necessario perdere peso in modo " +
                "graduale, circa 0,5-1% del tuo peso corporeo a settimana in media. ",
            articleTitle = "Dieta per dimagrire: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-dimagrire/",
            publishedDate = "2025-01-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Ad esempio, mangiare alimenti ricchi di omega 3 in grandi quantità non ti proteggerà al " +
                "100% da malattie cardiovascolari, perché l’alimentazione è solo uno dei fattori che " +
                "influenza e non l’unico (uno tra i più importante e non modificabile, la genetica ). ",
            articleTitle = "Superfood: quali sono e a cosa servono",
            url = "https://www.projectinvictus.it/superfood/",
            publishedDate = "2024-12-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Isolate Per definizione la forma con il maggiore grado di purezza, arrivando a " +
                "percentuali proteiche fino al 90-95%, quantità di lattosio e grassi insignificanti e " +
                "minore presenza di minerali. ",
            articleTitle = "Le 6 migliori proteine in polvere",
            url = "https://www.projectinvictus.it/migliori-proteine-in-polvere/",
            publishedDate = "2024-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per quanto riguarda i grassi, limita al massimo al 10% del fabbisogno giornaliero quelli " +
                "saturi per evitare problemi di salute ma non eliminarli del tutto. ",
            articleTitle = "Dieta per la pancia piatta",
            url = "https://www.projectinvictus.it/dieta-pancia-piatta/",
            publishedDate = "2024-10-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Se la tua percentuale di grasso corporeo attuale è superiore al 20-21%, è consigliato " +
                "vivamente di ridurre la percentuale di grasso corporeo, altrimenti rischi di fare una " +
                "fase di “grassa” e non di “massa”. ",
            articleTitle = "Dieta donna nel bodybuilding e in palestra",
            url = "https://www.projectinvictus.it/dieta-donne-bodybuilding/",
            publishedDate = "2024-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "I glucidi, in soggetti sani e non insulino resistenti , vengono facilmente convertiti in " +
                "calore quando sono in eccesso e si stima che in soggetti sani soltanto il 10% " +
                "dell’eccesso si converta effettivamente in grassi. ",
            articleTitle = "Cosa sono le calorie?",
            url = "https://www.projectinvictus.it/caloria/",
            publishedDate = "2024-07-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "L’ azione dinamica specifica dei lipidi è la più bassa dal 2-5% : questo vuol dire che " +
                "su 100kcal di grassi il corpo impiega solo 2-5kcal per digerirli (ricordiamo che la loro " +
                "assimilazione al contrario è la più lunga). ",
            articleTitle = "Cosa sono le calorie?",
            url = "https://www.projectinvictus.it/caloria/",
            publishedDate = "2024-07-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Più è abbondante il grasso corporeo, meno massa muscolare c’è, più la persona è poco " +
                "attiva durante la giornata e più sarà necessario tempo per ottenere risultati, anche " +
                "considerando il range prima consigliato di 0.5-1 kg a settimana. ",
            articleTitle = "Fisico asciutto: come ottenerlo",
            url = "https://www.projectinvictus.it/dieta-asciugare-fisico/",
            publishedDate = "2024-04-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Attenzione a non eccedere con grassi saturi (formaggi e carni grasse), sale negli " +
                "alimenti e assicurati di assumere dal cibo almeno 25 – 30 g di fibre alimentari al " +
                "giorno (tramite frutta e verdura). ",
            articleTitle = "Dieta ormonale: cos’è e come funziona",
            url = "https://www.projectinvictus.it/dieta-ormonale/",
            publishedDate = "2024-04-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "In realtà, questa finestra anabolica dura più tempo: l’importante non è fare un preciso " +
                "pasto post workout ma assicurarsi di assumere abbastanza carboidrati (oltre che energia, " +
                "proteine, grassi) nel corso delle 24 ore . ",
            articleTitle = "Carboidrati dopo l’allenamento in palestra",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-palestra/",
            publishedDate = "2024-03-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Associare per ogni blocchetto di proteine anche 1 blocchetto di carboidrati e 1 " +
                "blocchetto di grassi ( rapporto 1:1:1 tra i blocchetti ). ",
            articleTitle = "Dieta a zona: come funziona e menù",
            url = "https://www.projectinvictus.it/dieta-a-zona/",
            publishedDate = "2024-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "In questo periodo di scarico mangi prevalentemente grassi e proteine, mentre i " +
                "carboidrati sono ridotti al minimo (30 g), preferibilmente da verdura fibrosa, per " +
                "assumere il giusto quantitativo di fibre alimentari. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "L’effetto che si prefigge la dieta dovrebbe portare a migliorare l’assetto metabolico , " +
                "portando il corpo a diventare una macchina brucia grassi per 4-6 giorni a settimana, " +
                "sfruttando successivamente l’effetto anabolico, anticatabolico dell’insulina il weekend. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Tagliare completamente i carboidrati o i grassi non è una scelta fattibile: i " +
                "carboidrati sono il nutriente principale e i grassi è fondamentale tenerli minimamente " +
                "alti anche in ipocalorica (senza scendere sotto i 0,8 g/kg al giorno per evitare " +
                "squilibri ormonali). ",
            articleTitle = "Aumentare la massa muscolare nelle donne: come fare",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare-donne/",
            publishedDate = "2024-01-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Molti autori consigliano range tra 1.5 – 2.1 g/kg peso corporeo : eccedendo con le " +
                "calorie ti ritroverai non solo più grosso ma anche più grasso . ",
            articleTitle = "Le proteine fanno ingrassare?",
            url = "https://www.projectinvictus.it/le-proteine-fanno-ingrassare/",
            publishedDate = "2024-01-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Un grammo di grasso nel nostro corpo equivale a 7 kcal (non 9 kcal come nei grassi " +
                "alimentari) e otteniamo, quindi, 17 g di grasso consumati ogni 10 km . ",
            articleTitle = "Correre per dimagrire: quanto peso perdi?",
            url = "https://www.projectinvictus.it/correre-per-dimagrire/",
            publishedDate = "2023-12-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Scendendo sotto al 8-10% di grasso corporeo, la produzione degli androgeni cala " +
                "vistosamente, i bodybuilder natural sul palco hanno livelli di testosterone di una " +
                "persona castrata. ",
            articleTitle = "Natural bodybuilding: cos’è, allenamento, dieta",
            url = "https://www.projectinvictus.it/natural-bodybuilding/",
            publishedDate = "2023-10-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Ci sono persone che geneticamente tutto quello che mangiano lo mettono nei muscoli, " +
                "hanno basse percentuali di grasso corporeo (8-10%) senza stare a dieta ed enormi masse " +
                "muscolari da natural. ",
            articleTitle = "Natural bodybuilding: cos’è, allenamento, dieta",
            url = "https://www.projectinvictus.it/natural-bodybuilding/",
            publishedDate = "2023-10-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Il latte contiene acidi grassi saturi , che possono aumentare la sintesi di ormoni " +
                "steroidei e la concentrazione di insulina, ma che se bilanciati nella dieta (<10% del " +
                "fabbisogno energetico) non sono un problema. ",
            articleTitle = "Latte: fa male o fa bene?",
            url = "https://www.projectinvictus.it/latte-fa-male-o-bene/",
            publishedDate = "2023-08-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Attenzione a non esagerare con i grassi saturi e consuma formaggi grassi massimo 1-2 " +
                "volte alla settimana. ",
            articleTitle = "Cosa mangiare per ingrassare in modo sano",
            url = "https://www.projectinvictus.it/cosa-mangiare-per-ingrassare/",
            publishedDate = "2023-07-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per assicurarti che la pasta sia davvero integrale , puoi controllare sulla confezione " +
                "la tabella nutrizionale: per 100 g dovrebbero esserci almeno 7 g di fibre e 2 g di " +
                "grassi , che sono presenti poiché nella lavorazione viene mantenuto il germe del seme " +
                "che è una riserva di grassi. ",
            articleTitle = "La pasta fa ingrassare: miti e luoghi comuni",
            url = "https://www.projectinvictus.it/la-pasta-fa-ingrassare/",
            publishedDate = "2023-04-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Tuttavia, per il 98% della popolazione è solo una questione di grasso corporeo ostinato " +
                "nella zona sotto ombelicale (grasso tipico nell’uomo). ",
            articleTitle = "Addominali bassi: come allenarli",
            url = "https://www.projectinvictus.it/addominali-bassi/",
            publishedDate = "2023-01-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per quanto riguarda i valori nutrizionali , sono alimenti ricchi di grassi, che " +
                "rappresentano più del 70% dell’energia apportata. ",
            articleTitle = "Noci di macadamia: proprietà, benefici, calorie",
            url = "https://www.projectinvictus.it/noci-di-macadamia/",
            publishedDate = "2022-11-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Un dettaglio importante è che dopo un allenamento ad alta intensità, il corpo continua a " +
                "bruciare grassi e ci sarà una ricostruzione muscolare che può durare tra le 12 e le 24 " +
                "ore, chiamata afterburning. ",
            articleTitle = "Il CrossFit fa dimagrire?",
            url = "https://www.projectinvictus.it/crossfit-fa-dimagrire/",
            publishedDate = "2022-11-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Le cellule possono ottenere il combustibile metabolico sotto forma di acidi grassi da " +
                "tre fonti: i grassi della dieta (30% circa dell’apporto calorico); i grassi di deposito " +
                "(immagazzinati nel tessuto adiposo); i grassi sintetizzati in un organo ed esportati in " +
                "un altro. ",
            articleTitle = "Beta ossidazione",
            url = "https://www.projectinvictus.it/beta-ossidazione/",
            publishedDate = "2022-10-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "La beta-ossidazione è un processo molto importante per il fegato e il cuore , organi in " +
                "cui l’80% dell’energia consumata proviene dall’ossidazione degli acidi grassi. ",
            articleTitle = "Beta ossidazione",
            url = "https://www.projectinvictus.it/beta-ossidazione/",
            publishedDate = "2022-10-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per perdere 1 kg di grasso ci vorranno più di 2-3 settimane. ",
            articleTitle = "Tartaruga addominali: come ottenerla",
            url = "https://www.projectinvictus.it/tartaruga-addominali/",
            publishedDate = "2022-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Se non hai grasso in eccesso (soprattutto il grasso viscerale) e mangi seguendo il tuo " +
                "fabbisogno calorico , puoi anche concederti un’alimentazione basata su: 80% cibo sano e " +
                "salutare, 20% cibo libero (anche junk food). ",
            articleTitle = "Junk food e cibo spazzatura: cosa sono e perché fanno male",
            url = "https://www.projectinvictus.it/junk-food-cibo-spazzatura/",
            publishedDate = "2022-08-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Puoi ricavare i grassi anche dalla carne e formaggi ma i grassi saturi non dovrebbero " +
                "superare il 10% delle calorie giornaliere. ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Considera il tuo fabbisogno energetico totale della giornata : se più del 10% " +
                "dell’energia che ti serve deriva dagli acidi grassi saturi è meglio iniziare a ridurli! ",
            articleTitle = "Gli acidi grassi saturi fanno male?",
            url = "https://www.projectinvictus.it/acidi-grassi-saturi-fanno-bene-o-male/",
            publishedDate = "2022-05-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per avere un altro punto di riferimento, considera che la maggior parte della frutta e " +
                "della verdura contengono pochissimi acidi grassi saturi : tra 0 e 1 g per 100 g di " +
                "prodotto. ",
            articleTitle = "Gli acidi grassi saturi fanno male?",
            url = "https://www.projectinvictus.it/acidi-grassi-saturi-fanno-bene-o-male/",
            publishedDate = "2022-05-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Il 95% del grasso nell’olio di cocco è saturo e circa la metà è acido laurico. ",
            articleTitle = "Mountain Dog Diet: cos’è e come farla",
            url = "https://www.projectinvictus.it/dieta-mountain-dog/",
            publishedDate = "2022-05-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Secondo le indicazioni dell’autore, suggerite anche da diversi esperti sui lipidi, la " +
                "dieta dovrebbe apportare circa l’1.5% delle sue calorie sotto forma di acidi grassi " +
                "polinsaturi. ",
            articleTitle = "Mountain Dog Diet: cos’è e come farla",
            url = "https://www.projectinvictus.it/dieta-mountain-dog/",
            publishedDate = "2022-05-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "I grassi in fase di cut coprono il 15-25% del fabbisogno calorico totale, con un minimo " +
                "di 0,5 g/kg per assicurare la corretta funzionalità dell’organismo. ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "I grassi al 15% sono un valore molto basso , ma bisogna considerare che è una quantità " +
                "che viene mantenuta solamente per un periodo relativamente breve. ",
            articleTitle = "Macronutrienti per la definizione muscolare",
            url = "https://www.projectinvictus.it/macronutrienti-definizione-muscolare/",
            publishedDate = "2022-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Se corri a digiuno con le scorte di glicogeno depauperate il consumo di grassi aumenta " +
                "mediamente di un 20-30%. ",
            articleTitle = "Corsa a digiuno a stomaco vuoto per dimagrire",
            url = "https://www.projectinvictus.it/correre-a-digiuno/",
            publishedDate = "2021-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Ovviamente, questo dato non fa differenza tra massa magra o massa grassa: ad esempio, un " +
                "bodybuilder di 100 kg risulterebbe grasso anche col 5% di FM (Fat Mass, massa grassa). ",
            articleTitle = "Le calorie fanno ingrassare veramente?",
            url = "https://www.projectinvictus.it/una-calorie-e-un-caloria-vediamo-di-fare-chiarezza/",
            publishedDate = "2021-11-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per i grassi si consiglia un range intorno al 25-30% delle calorie giornaliere. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Si consiglia di non superare il 10% delle calorie dai grassi saturi. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Le vitamine si dividono in liposolubili : Vitamina A, vitamina D , Vitamina E, Vitamina " +
                "K, Sono veicolate dai grassi alimentari (almeno 30 g al giorno) e si depositano nelle " +
                "riserve lipidiche del nostro organismo, nelle membrane cellulari e nel fegato. ",
            articleTitle = "Nutrienti: quali sono e cosa sono?",
            url = "https://www.projectinvictus.it/nutrienti/",
            publishedDate = "2021-10-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Di conseguenza, un ampio consumo corrisponde ad una grande introduzione di grassi saturi " +
                "che probabilmente ti farà sforare dalla quantità giornaliera indicata (< 10% fabbisogno " +
                "energetico). ",
            articleTitle = "L’olio di palma fa male alla salute?",
            url = "https://www.projectinvictus.it/olio-di-palma-fa-male/",
            publishedDate = "2021-09-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "E’ composto da un 50% di acidi grassi saturi (palmitico in prevalenza) e un 50% di acidi " +
                "grassi insaturi (oleico in prevalenza) ed è ricco in vitamina E, carotenoidi e " +
                "fitosteroli. ",
            articleTitle = "L’olio di palma fa male alla salute?",
            url = "https://www.projectinvictus.it/olio-di-palma-fa-male/",
            publishedDate = "2021-09-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Come gli altri grassi, contribuiscono a mantenere il bilancio energetico positivo quando " +
                "assunti nella dieta e sono molto calorici: apportano 9 kcal per grammo . ",
            articleTitle = "Grassi monoinsaturi: fanno male o fanno bene? I benefici",
            url = "https://www.projectinvictus.it/grassi-monoinsaturi/",
            publishedDate = "2021-07-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "L’acido oleico è il principale acido grasso monoinsaturo a cui si devono benefici per la " +
                "salute ed è il più presente nei cibi considerati come buone fonti di MUFA, in quanto " +
                "rappresenta il 95-100% dei grassi monoinsaturi contenuti. ",
            articleTitle = "Grassi monoinsaturi: fanno male o fanno bene? I benefici",
            url = "https://www.projectinvictus.it/grassi-monoinsaturi/",
            publishedDate = "2021-07-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Oltre al quanti grassi assumere al giorno , parametro importante per il bilancio " +
                "energetico e la salute, anche la qualità lipidica è da considerare: la quantità di " +
                "grassi saturi e monoinsaturi trans possono ricoprire al massimo il 10% del fabbisogno " +
                "energetico, i polinsaturi il 5-10%. ",
            articleTitle = "Grassi monoinsaturi: fanno male o fanno bene? I benefici",
            url = "https://www.projectinvictus.it/grassi-monoinsaturi/",
            publishedDate = "2021-07-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per differenza, poiché nei LARN non è specificata direttamente la quantità di MUFA, e " +
                "considerando che in una dieta equilibrata per un adulto i grassi coprono il 20-35% " +
                "dell’energia giornaliera, i MUFA buoni contribuiscono per circa il 10% . ",
            articleTitle = "Grassi monoinsaturi: fanno male o fanno bene? I benefici",
            url = "https://www.projectinvictus.it/grassi-monoinsaturi/",
            publishedDate = "2021-07-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Dosaggio L’apporto di grassi essenziali consigliato è pari al 4-5% del fabbisogno " +
                "calorico giornaliero, da comprendere nella quota lipidica che generalmente in una dieta " +
                "equilibrata corrisponde al 25-30% – per approfondire, leggi quanti grassi assumere al " +
                "giorno . ",
            articleTitle = "Acidi grassi essenziali: quali sono e perché sono importanti?",
            url = "https://www.projectinvictus.it/acidi-grassi-essenziali/",
            publishedDate = "2021-07-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Sono in generale grassi “buoni” , da preferire rispetto a quelli “cattivi” per le loro " +
                "proprietà e utilità, Gli AGE contribuiscono al fabbisogno energetico quotidiano per il " +
                "4-5% e derivano sia da fonti vegetali ( frutta secca, oli di semi, …) che animali ( " +
                "pesce ). ",
            articleTitle = "Acidi grassi essenziali: quali sono e perché sono importanti?",
            url = "https://www.projectinvictus.it/acidi-grassi-essenziali/",
            publishedDate = "2021-07-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Alcuni esempi per come assumere grassi essenziali: mangiare pesce grasso almeno 2 volte " +
                "a settimana, utilizzare delle noci o altra frutta secca come spuntino, aggiungere semi o " +
                "oli vegetali di semi (di lino, di arachide,…) sulla verdura. ",
            articleTitle = "Acidi grassi essenziali: quali sono e perché sono importanti?",
            url = "https://www.projectinvictus.it/acidi-grassi-essenziali/",
            publishedDate = "2021-07-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Se la persona ha insulino-resistenza o mangia una buona percentuale di acidi grassi " +
                "(oltre al 25-30% delle calorie) questi si accumulano nel tessuto adiposo . ",
            articleTitle = "Lipogenesi: cos’è, come e dove avviene?",
            url = "https://www.projectinvictus.it/lipogenesi/",
            publishedDate = "2021-07-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Ti traduco due estratti che, secondo me, sono significativi: “…che il processo di " +
                "idrogenazione dei grassi industriali può portare al 60% di TFAs tra i grassi totali (di " +
                "un alimento), quando nei grassi dei ruminanti il livello di TFAs non supera il 5 o 6%.” " +
                "(Pag. ",
            articleTitle = "Acidi grassi trans, salute e rischi: cosa sono e perché è meglio evitarli?",
            url = "https://www.projectinvictus.it/acidi-grassi-trans/",
            publishedDate = "2021-06-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Questi grassi coprono la maggior parte del fabbisogno lipidico giornaliero (> 20%), " +
                "mentre il restante 10% (valore limite massimo) è fornito da grassi saturi. ",
            articleTitle = "Acidi grassi trans, salute e rischi: cosa sono e perché è meglio evitarli?",
            url = "https://www.projectinvictus.it/acidi-grassi-trans/",
            publishedDate = "2021-06-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Ad esempio, il cocco essiccato contiene il 93% di grassi saturi e, per differenza, il 7% " +
                "di insaturi. ",
            articleTitle = "Grassi buoni e cattivi: benefici, rischi e dove trovarli?",
            url = "https://www.projectinvictus.it/grassi-buoni-e-cattivi/",
            publishedDate = "2021-06-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Ad esempio, il colesterolo è parte integrante delle membrane di tutte le cellule ed è un " +
                "precursore ormonale, l’apporto di grassi saturi non dovrebbe superare il 10% del " +
                "fabbisogno energetico, mentre quelli idrogenati e trans è bene ridurli al minimo. ",
            articleTitle = "Grassi buoni e cattivi: benefici, rischi e dove trovarli?",
            url = "https://www.projectinvictus.it/grassi-buoni-e-cattivi/",
            publishedDate = "2021-06-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "L’apporto di grassi trans non dovrebbe superare l’1% del fabbisogno energetico – " +
                "quantità facilmente raggiungibile e superabile se questi alimenti non vengono moderati. ",
            articleTitle = "Grassi buoni e cattivi: benefici, rischi e dove trovarli?",
            url = "https://www.projectinvictus.it/grassi-buoni-e-cattivi/",
            publishedDate = "2021-06-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "La percentuale ideale di grassi nella dieta equilibrata di un adulto sano (normo o " +
                "ipocalorica) potrebbe essere del 25% circa sulle calorie totali; ai bambini si consiglia " +
                "invece il 30% . ",
            articleTitle = "Grassi: cosa sono, quali sono e che funzione hanno?",
            url = "https://www.projectinvictus.it/grassi/",
            publishedDate = "2021-06-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "La quantità di grassi “ cattivi ” , ovvero quelli saturi e monoinsaturi in conformazione " +
                "trans, non dovrebbe superare il 10% delle calorie totali nella dieta. ",
            articleTitle = "Grassi: cosa sono, quali sono e che funzione hanno?",
            url = "https://www.projectinvictus.it/grassi/",
            publishedDate = "2021-06-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Per una dieta salutare ed equilibrata, i grassi saturi non dovrebbero superare il 10% " +
                "del fabbisogno energetico a prescindere dalla fonte da cui questi derivino. ",
            articleTitle = "Grassi vegetali: caratteristiche e benefici",
            url = "https://www.projectinvictus.it/grassi-vegetali/",
            publishedDate = "2021-06-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Quando addizionato agli altri grassi, in Italia l’acido erucico non deve superare il 5% " +
                "del totale degli acidi grassi. ",
            articleTitle = "Grassi vegetali: caratteristiche e benefici",
            url = "https://www.projectinvictus.it/grassi-vegetali/",
            publishedDate = "2021-06-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "La restante percentuale (7-10% massimo) viene data dai grassi saturi. ",
            articleTitle = "Grassi buoni: quali sono e a cosa servono?",
            url = "https://www.projectinvictus.it/grassi-buoni/",
            publishedDate = "2021-06-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "A fine ipertrofico è tuttavia utile preferire il consumo dei carboidrati rispetto a " +
                "quello dei grassi, il cui fabbisogno può quindi essere diminuito fino a 0.5 g/kg senza " +
                "scendere verso valori inferiori. ",
            articleTitle = "Grassi buoni: quali sono e a cosa servono?",
            url = "https://www.projectinvictus.it/grassi-buoni/",
            publishedDate = "2021-06-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "(1) È un frutto molto calorico , la sua polpa contiene 358 kcal ogni 100 g provenienti " +
                "prevalentemente da grassi, ed un’ottima fonte di minerali principalmente rame, ferro e " +
                "magnesio. ",
            articleTitle = "Cocco: benefici, proprietà e calorie",
            url = "https://www.projectinvictus.it/cocco-proprieta-benefici/",
            publishedDate = "2021-05-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Isolate in polvere Le proteine isolate hanno una percentuale proteica molto alta " +
                "(90-95%) mentre le altre componenti (lattosio, grassi, sali minerali) sono praticamente " +
                "assenti. ",
            articleTitle = "Proteine del siero del latte: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-del-siero-del-latte/",
            publishedDate = "2021-05-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Latte e derivati Per i latticini, sono da preferire il latte e yogurt parzialmente " +
                "scremanti, i formaggi freschi e meno grassi in modo da ridurre l’assunzione degli acidi " +
                "grassi saturi – che vanno limitati al 10% del fabbisogno giornaliero e presenti solo " +
                "negli alimenti animali. ",
            articleTitle = "Proteine animali sono dannose?",
            url = "https://www.projectinvictus.it/proteine-animali/",
            publishedDate = "2021-04-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Non ci sono infatti riserve di amminoacidi nell’organismo, come è il glicogeno per il " +
                "glucosio o l’organo adiposo per gli acidi grassi, e il muscolo è quello che presenta un " +
                "contenuto proteico più elevato (circa il 20% del suo peso). ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Il pesce è simile alla carne per composizione, anche se ha un contenuto molto più " +
                "variabile di grassi (0-25%) e più alto di acidi grassi essenziali (EPA, DHA). ",
            articleTitle = "Cosa mangiare a cena per dimagrire?",
            url = "https://www.projectinvictus.it/cena-per-dimagrire/",
            publishedDate = "2021-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "A questo punto, per ottenere i grammi dei macronutrienti da raggiungere bisogna " +
                "ricordare che 1 g di carboidrati e 1g di proteine corrispondono a 4 kcal, mentre 1 g di " +
                "grassi a 9 kcal . ",
            articleTitle = "IIFYM: che cos’è e come funziona?",
            url = "https://www.projectinvictus.it/iifym/",
            publishedDate = "2021-01-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Non che di per sé i “cibi non sani” siano un problema quando occasionali (come il " +
                "comprenderli nel 20%), ma se molto consumati facilmente fanno sforare dalle quantità " +
                "consigliate: sia zuccheri che grassi saturi non dovrebbero superare il 10% dell’apporto " +
                "calorico giornaliero, mentre i trans l’1%. ",
            articleTitle = "IIFYM: che cos’è e come funziona?",
            url = "https://www.projectinvictus.it/iifym/",
            publishedDate = "2021-01-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Fai sempre attenzione: un prodotto è davvero integrale quando contiene almeno 7 g di " +
                "fibre e almeno 2 g di grassi per 100 g di alimento . ",
            articleTitle = "I carboidrati sono meglio a pranzo o a cena?",
            url = "https://www.projectinvictus.it/carboidrati-giorno-sera/",
            publishedDate = "2020-11-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "In termini pratici, se fai un past o da non più di 600 kcal e inoltre hai anche una " +
                "buona massa magra puoi mescolare glucidi e lipidi senza il timore di ingrassare. ",
            articleTitle = "Grassi e carboidrati nello stesso pasto fanno ingrassare?",
            url = "https://www.projectinvictus.it/grassi-ed-carboidrati-vanno-mangiati-assieme/",
            publishedDate = "2020-08-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Studi dimostrano che in un lasso di tempo che va dalle 10 alle 12 settimane, " +
                "l’allenamento HIIT consente di: aumentare il colesterolo buono HDL, ridurre il grasso " +
                "viscerale e totale del corpo, aumentare la VO2 max e ridurre l’ insulino-resistenza. ",
            articleTitle = "Sindrome da ovaio policistico: dieta ed allenamento",
            url = "https://www.projectinvictus.it/sindrome-ovaio-policistico-dieta-allenamento/",
            publishedDate = "2020-08-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Nell’elenco vanno comprese anche le arachidi , scambiate molto spesso per frutta secca , " +
                "che contengono circa il 25% di proteina ma anche il 50% di grassi, caratteristica che li " +
                "differenzia notevolmente dagli altri legumi. ",
            articleTitle = "Legumi: quanti sono e quali sono i migliori?",
            url = "https://www.projectinvictus.it/legumi/",
            publishedDate = "2020-02-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "L’errore standard nella previsione della quantità del grasso corporeo è compreso tra il " +
                "2 e il 3% del peso totale dell’individuo. ",
            articleTitle = "Valutazione composizione corporea",
            url = "https://www.projectinvictus.it/valutazione-composizione-corporea/",
            publishedDate = "2018-06-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Il detto: “ gli addominali si fanno a tavola ” è vero, finché non avete una % di grasso " +
                "corporeo intorno al 10-12% potete fare qualsiasi esercizio completo per gli addominali , " +
                "ma di loro non ne vedrete traccia (se non iniziare ad intravederli sotto al 15%). ",
            articleTitle = "Esercizio completo addominali: qual è il migliore",
            url = "https://www.projectinvictus.it/esercizio-completo-addominali-migliore/",
            publishedDate = "2017-12-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Rimanere così in normopeso con una % di grasso corporeo tra il 10-15% è importante per " +
                "chi vuole salvaguardare i suoi range fisiologici. ",
            articleTitle = "Testosterone: la guida a questo ormone",
            url = "https://www.projectinvictus.it/testosterone-la-guida-ormone/",
            publishedDate = "2017-12-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Durante il digiuno ad esempio l’ossidazione degli acidi grassi ( FAO , Fatty Acid " +
                "Oxidation ) copre oltre il 90% del fabbisogno energetico. ",
            articleTitle = "Cosa succede quando il muscolo incontra il grasso?",
            url = "https://www.projectinvictus.it/muscolo-incontra-grasso/",
            publishedDate = "2017-03-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Essendo l’apporto di acqua, complice anche il caldo, arrivato alla soglia dei 10 litri e " +
                "non potendo pertanto agire su quella leva, decidiamo di apportare una piccola modifica " +
                "al piano di Peaking, tagliando altre 150 Kcal medie giornaliere tramite una diminuzione " +
                "dei grassi a 30 g. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "L’obiettivo del 6% di grasso potrebbe essere raggiunto con un mese o due di anticipo " +
                "rispetto al programma iniziale. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "L’obiettivo del 6% di grasso corporeo è ormai a portata di mano! ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Decidiamo di proseguire l’incremento di carboidrati aggiungendo 10 grammi ogni 3-4 " +
                "giorni e lasciando invariate proteine e grassi. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Sebbene i grassi omega 3 siano più abbondanti nei frutti di mare, essi si trovano anche " +
                "negli animali allevati al pascolo, dove sono presenti tra 2 e 6 volte più omega 3. ",
            articleTitle = "La Dieta Mountain Dog (Parte 2)",
            url = "https://www.projectinvictus.it/la-dieta-mountain-dog-parte-2/",
            publishedDate = "2014-05-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "In realtà, l’aumento del turnover proteico da esercizio fisico è solo circa 2 o 3 volte " +
                "rispetto alle 20-30 volte per carboidrati e grassi. ",
            articleTitle = "BCAA sempre e comunque utili?",
            url = "https://www.projectinvictus.it/bcaa-sempre-e-comunque-utili/",
            publishedDate = "2014-03-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "5) Alto consumo di grassi monoinsaturi che dovrebbero coprire tra il 60 e l’80% del " +
                "vostro consumo giornaliero di grassi. ",
            articleTitle = "La sensibilità insulinica (Parte 3)",
            url = "https://www.projectinvictus.it/la-sensibilita-insulinica-parte-3/",
            publishedDate = "2014-01-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Molti atleti, per preservare la massa contrattile preferiscono tagliare questo " +
                "macronutriente ed in media andiamo da quantitativi da 0,4-1,2g/kg di grassi . ",
            articleTitle = "Il culturismo: cos’è, dieta e allenamento",
            url = "https://www.projectinvictus.it/culturismo/",
            publishedDate = "2022-12-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Perdere 1 kg di grasso richiede un deficit calorico di circa 7.000 calorie , e le diete " +
                "lampo solitamente non consentono di raggiungere un tale deficit in così poco tempo. ",
            articleTitle = "Dieta lampo per dimagrire in poco tempo",
            url = "https://www.projectinvictus.it/dieta-lampo/",
            publishedDate = "2025-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Il mio obiettivo è quello di incrementare di circa 200g il mio peso medio settimanale, " +
                "di settimana in settimana, così da aumentare la massa muscolare e migliorare i miei " +
                "massimali senza accumulare eccessivamente grasso. ",
            articleTitle = "L’autoregolazione nell’allenamento della forza",
            url = "https://www.projectinvictus.it/autoregolazione-allenamento-della-forza/",
            publishedDate = "2017-01-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FAT,
            statement =
                "Il grasso nel burro contiene più grassi monoinsaturi di quanto si potrebbe pensare, " +
                "circa il 30%. ",
            articleTitle = "Mountain Dog Diet: cos’è e come farla",
            url = "https://www.projectinvictus.it/dieta-mountain-dog/",
            publishedDate = "2022-05-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Le linee guida internazionali per una sana alimentazione , a prescindere dal tuo " +
                "obbiettivo, consigliano di consumare almeno 2-3 porzioni da 150 g di frutta al giorno, " +
                "anche se non c’è un vero e proprio limite da non superare. ",
            articleTitle = "Quanta frutta mangiare al giorno?",
            url = "https://www.projectinvictus.it/quanta-frutta-mangiare-al-giorno/",
            publishedDate = "2025-11-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Inoltre, anche se olio, frutta secca, avocado, salmone sono fonti “buone” di grassi, " +
                "sono comunque da limitare perché le calorie dei grassi sono sempre 9 kcal/grammo a " +
                "prescindere dall’alimento e che facciano bene o male alla salute. ",
            articleTitle = "Dieta per avere gli addominali",
            url = "https://www.projectinvictus.it/dieta-per-addominali/",
            publishedDate = "2025-08-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "La porzione di riferimento è di 200 g, condita con un filo d’olio EVO; Frutta : puoi sia " +
                "concludere il pasto con una porzione di frutta che consumarla negli spuntini, la " +
                "porzione di riferimento è di 1 frutto medio o 2 frutti piccoli per 2-3 volte al giorno. ",
            articleTitle = "Dieta estiva per dimagrire",
            url = "https://www.projectinvictus.it/dieta-estiva/",
            publishedDate = "2025-04-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Secondo i LARN, ogni giorno dovrebbero essere consumati 25-30 g di fibra alimentare, " +
                "ottenibile proprio tramite le porzioni e gli alimenti sopra indicati. ",
            articleTitle = "Dimagrire velocemente e perdere peso",
            url = "https://www.projectinvictus.it/dimagrire-velocemente/",
            publishedDate = "2025-01-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Attenzione a non eccedere con grassi saturi (formaggi e carni grasse), sale negli " +
                "alimenti e assicurati di assumere dal cibo almeno 25 – 30 g di fibre alimentari al " +
                "giorno (tramite frutta e verdura). ",
            articleTitle = "Dieta ormonale: cos’è e come funziona",
            url = "https://www.projectinvictus.it/dieta-ormonale/",
            publishedDate = "2024-04-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "In questo periodo di scarico mangi prevalentemente grassi e proteine, mentre i " +
                "carboidrati sono ridotti al minimo (30 g), preferibilmente da verdura fibrosa, per " +
                "assumere il giusto quantitativo di fibre alimentari. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "L’effetto che si prefigge la dieta dovrebbe portare a migliorare l’assetto metabolico , " +
                "portando il corpo a diventare una macchina brucia grassi per 4-6 giorni a settimana, " +
                "sfruttando successivamente l’effetto anabolico, anticatabolico dell’insulina il weekend. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Verdura prima del resto Mangiare una porzione di verdura (200 g) o un piatto di minestra " +
                "o una ciotola di verdura a foglia prima del resto del pasto ti aiuta ad evitare di " +
                "mangiare troppo nel corso del pasto. ",
            articleTitle = "Dimagrire senza dieta in modo definitivo",
            url = "https://www.projectinvictus.it/dieta-senza-dieta/",
            publishedDate = "2024-01-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Ecco alcuni consigli pratici: Consuma una quantità di calorie leggermente inferiore al " +
                "tuo fabbisogno energetico (200-300kcal) per creare un deficit calorico; Scegli cibi " +
                "ricchi di nutrienti : come proteine magre (pollo, tacchino, pesce), cereali integrali " +
                "(farro, orzo, riso), frutta, verdura. ",
            articleTitle = "Come dimagrire le gambe: esercizi, dieta, consigli",
            url = "https://www.projectinvictus.it/come-dimagrire-le-gambe/",
            publishedDate = "2023-09-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "A tal proposito, secondo il principio delle dimensioni di Henneman già con carichi di " +
                "circa l’80% (6-8 ripetizioni massimali) vengono reclutate tutte le fibre muscolari. ",
            articleTitle = "Ipertrofia muscolare: la guida completa",
            url = "https://www.projectinvictus.it/ipertrofia-muscolare-la-guida-completa/",
            publishedDate = "2023-08-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Le 4 fasi della dieta Atkins Fase 1 – Induzione La prima fase limita l’introduzione di " +
                "carboidrati ad un massimo di 20g/die per un periodo di tempo di 2 settimane, ottenuti " +
                "principalmente da fonti vegetali. ",
            articleTitle = "Dieta Atkins: come funziona, quanto si perde, menù",
            url = "https://www.projectinvictus.it/dieta-atkins/",
            publishedDate = "2023-06-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Questi alimenti apportano una notevole quantità di carboidrati complessi, circa un " +
                "10-12% di proteine ma anche fibre alimentari, vitamine del gruppo B e sali minerali come " +
                "magnesio, zinco e ferro. ",
            articleTitle = "Dieta flexitariana: cos’è, cosa mangiare, menù",
            url = "https://www.projectinvictus.it/flexitariana/",
            publishedDate = "2023-06-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Il consumo quotidiano di almeno 5 porzioni di frutta e verdura assicura una giusta " +
                "quantità di vitamine, sali minerali e fibra alimentare (30 g/die). ",
            articleTitle = "Dieta e tumori: relazione e raccomandazioni",
            url = "https://www.projectinvictus.it/dieta-tumori/",
            publishedDate = "2023-06-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Per assicurarti che la pasta sia davvero integrale , puoi controllare sulla confezione " +
                "la tabella nutrizionale: per 100 g dovrebbero esserci almeno 7 g di fibre e 2 g di " +
                "grassi , che sono presenti poiché nella lavorazione viene mantenuto il germe del seme " +
                "che è una riserva di grassi. ",
            articleTitle = "La pasta fa ingrassare: miti e luoghi comuni",
            url = "https://www.projectinvictus.it/la-pasta-fa-ingrassare/",
            publishedDate = "2023-04-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Le proteine sono una classe di molecole biologiche molto presenti in natura : negli " +
                "organismi animali rappresentano circa il 50% del peso secco, mentre in quelli vegetali " +
                "un po’ meno poichè c’è un alto contenuto di cellulosa (una molecola che fa parte della " +
                "famiglia dei carboidrati). ",
            articleTitle = "Aminoacidi: cosa sono e che funzione svolgono?",
            url = "https://www.projectinvictus.it/cosa-sono-gli-aminoacidi/",
            publishedDate = "2022-12-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Secondo le linee guida del CREA, la quantità consigliata è di 30 g di frutta secca a " +
                "guscio massimo 3 volte a settimana . ",
            articleTitle = "Noci di macadamia: proprietà, benefici, calorie",
            url = "https://www.projectinvictus.it/noci-di-macadamia/",
            publishedDate = "2022-11-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Non solo, ma il carico potrà essere calato di un ulteriore 10-25% per continuare a " +
                "sfinire le fibre rimaste, in questo caso non si parla più di doppio Drop Set ma di " +
                "triplo Drop Set . ",
            articleTitle = "Drop set bodybuilding: cos’è e come si fa",
            url = "https://www.projectinvictus.it/drop-set-bodybuilding/",
            publishedDate = "2022-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Per avere un altro punto di riferimento, considera che la maggior parte della frutta e " +
                "della verdura contengono pochissimi acidi grassi saturi : tra 0 e 1 g per 100 g di " +
                "prodotto. ",
            articleTitle = "Gli acidi grassi saturi fanno male?",
            url = "https://www.projectinvictus.it/acidi-grassi-saturi-fanno-bene-o-male/",
            publishedDate = "2022-05-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Il supplemento di nitrati può tuttavia essere tentato bevendo succo di barbabietola o " +
                "altro estratto vegetale a base di ortaggi tra quelli elencati, da assumere 2-3 ore prima " +
                "dell’esercizio fisico. ",
            articleTitle = "Integratore di ossido nitrico",
            url = "https://www.projectinvictus.it/ossido-nitrico-integratore/",
            publishedDate = "2022-02-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Stimando la media di quella che trovi tutti i giorni nel banco frutta, l’apporto " +
                "calorico si aggira intorno alle 50 kcal/100g. ",
            articleTitle = "Calorie frutta: tabella",
            url = "https://www.projectinvictus.it/calorie-frutta-tabella/",
            publishedDate = "2021-10-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Superata infatti una determinata quota di carboidrati (350-400 g) l’apporto di fibre è " +
                "assicurato anche mangiando fonti raffinate. ",
            articleTitle = "Calorie macronutrienti",
            url = "https://www.projectinvictus.it/calorie-macronutrienti/",
            publishedDate = "2021-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Le linee guida considerano un’alimentazione sana e salutare quella che si basa " +
                "principalmente su cereali interi, legumi, frutta, verdura e che apporta dal 45% al 60% " +
                "delle calorie giornaliere introdotte. ",
            articleTitle = "Carboidrati complessi: funzioni, benefici ed esempi",
            url = "https://www.projectinvictus.it/carboidrati-complessi/",
            publishedDate = "2021-09-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Sono in generale grassi “buoni” , da preferire rispetto a quelli “cattivi” per le loro " +
                "proprietà e utilità, Gli AGE contribuiscono al fabbisogno energetico quotidiano per il " +
                "4-5% e derivano sia da fonti vegetali ( frutta secca, oli di semi, …) che animali ( " +
                "pesce ). ",
            articleTitle = "Acidi grassi essenziali: quali sono e perché sono importanti?",
            url = "https://www.projectinvictus.it/acidi-grassi-essenziali/",
            publishedDate = "2021-07-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Alcuni esempi per come assumere grassi essenziali: mangiare pesce grasso almeno 2 volte " +
                "a settimana, utilizzare delle noci o altra frutta secca come spuntino, aggiungere semi o " +
                "oli vegetali di semi (di lino, di arachide,…) sulla verdura. ",
            articleTitle = "Acidi grassi essenziali: quali sono e perché sono importanti?",
            url = "https://www.projectinvictus.it/acidi-grassi-essenziali/",
            publishedDate = "2021-07-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "La frutta fresca, infatti, è molto idratata (utile come spuntino o colazione soprattutto " +
                "in estate) e presenta fibra alimentare soprattutto nella buccia – questa fornisce meno " +
                "di 2 kcal/g e ha molti benefici sulla salute. ",
            articleTitle = "Frutti proteici: la frutta più proteica",
            url = "https://www.projectinvictus.it/frutta-proteica/",
            publishedDate = "2021-06-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Le proteine vegetali hanno una minor digeribilità, il corpo ne assorbe meno del 90% di " +
                "quello che introduci, tuttavia puoi benissimo sostituire le proteine animali con quelle " +
                "vegetali. ",
            articleTitle = "Proteine animali sono dannose?",
            url = "https://www.projectinvictus.it/proteine-animali/",
            publishedDate = "2021-04-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Per di più, come abbiamo visto sul paragrafo della TID, le proteine di origine vegetale " +
                "sono meno digeribili rispetto a quelle animali (60-80% VS 90%) . ",
            articleTitle = "Proteine: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-cosa-sono-a-cosa-servono/",
            publishedDate = "2021-04-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Proteine vegetali: cereali I cereali (come il frumento e derivati) hanno un contenuto " +
                "proteico molto variabile (8-20%) e non contengono tutti gli aminoacidi essenziali . ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Questo anche perchè le proteine vegetali sono meno facilmente assimilabili e digeribili: " +
                "in caso di alimentazione vegetariana e vegana è quindi utile incrementare del 15-20% il " +
                "fabbisogno proteico rispetto a quello consigliato per i non vegetariani . ",
            articleTitle = "Proteine nobili: significato e alimenti ricchi di proteine nobili",
            url = "https://www.projectinvictus.it/proteine-nobili/",
            publishedDate = "2021-04-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Fai sempre attenzione: un prodotto è davvero integrale quando contiene almeno 7 g di " +
                "fibre e almeno 2 g di grassi per 100 g di alimento . ",
            articleTitle = "I carboidrati sono meglio a pranzo o a cena?",
            url = "https://www.projectinvictus.it/carboidrati-giorno-sera/",
            publishedDate = "2020-11-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "La donna in dolce attesa dovrebbe prediligere fonti di proteine da cibi di origine " +
                "vegetale come legumi (circa il 57% dell’assunzione giornaliera) seguiti da alimenti di " +
                "origine animale come carne (18%) e latticini (10%). ",
            articleTitle = "Alimentazione in gravidanza: cosa mangiare?",
            url = "https://www.projectinvictus.it/alimentazione-in-gravidanza/",
            publishedDate = "2020-11-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Altro metodo consiste nel tenere conto del fatto che le proteine vegetali hanno circa il " +
                "20-35% di digeribilità in meno (è una stima ovviamente). ",
            articleTitle = "Proteine vegetali per la palestra",
            url = "https://www.projectinvictus.it/proteine-vegetali/",
            publishedDate = "2020-07-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Oltre a questo, consideriamo sempre che l’insulina è l’ormone anabolico per eccellenza , " +
                "sfruttarlo è in generale una buona mossa, così come l’assunzione della maggior frazione " +
                "glucidica (50-70%) dei carboidrati giornalieri nelle ore a ridosso dell’allenamento. ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Nell’elenco vanno comprese anche le arachidi , scambiate molto spesso per frutta secca , " +
                "che contengono circa il 25% di proteina ma anche il 50% di grassi, caratteristica che li " +
                "differenzia notevolmente dagli altri legumi. ",
            articleTitle = "Legumi: quanti sono e quali sono i migliori?",
            url = "https://www.projectinvictus.it/legumi/",
            publishedDate = "2020-02-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "La frutta secca può andare da un contenuto calorico medio come quello delle castagne " +
                "(189 kcal) ad un contenuto calorico elevatissimo come le noci di macadamia (718 kcal), " +
                "ma bisogna saper paragonare l’effetto saziante con il contenuto calorico. ",
            articleTitle = "Frutta secca migliore: benefici a dieta e in palestra",
            url = "https://www.projectinvictus.it/frutta-secca-quale-quanta-e-perche/",
            publishedDate = "2020-02-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Basta l’80% del carico massimale per reclutare tutte le fibre muscolari. ",
            articleTitle = "Le fibre muscolari",
            url = "https://www.projectinvictus.it/le-fibre-muscolari/",
            publishedDate = "2017-06-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.FIBER,
            statement =
                "Bosco e coll dimostrarono la legge di Henneman , ovvero che intorno all’80% del carico " +
                "massimale si attivano TUTTE le fibre muscolari. ",
            articleTitle = "La Forza nel Bodybuilding è veramente utile?",
            url = "https://www.projectinvictus.it/forza-bodybuilding/",
            publishedDate = "2016-10-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "L’indicazione delle calorie da assumere solo a colazione non è sufficiente per garantire " +
                "un effettivo dimagrimento: puoi ingerire 100 kcal o 500 kcal di mattina e dimagrire o " +
                "ingrassare in entrambi i casi. ",
            articleTitle = "Colazione per dimagrire: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-per-dimagrire/",
            publishedDate = "2025-10-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "E’ più importante la quantità totale giornaliera di proteine che il timing, è " +
                "consigliato dividere la quantità proteica in 4-5 volte al giorno , che possono " +
                "coincidere con i normali pasti che fai nell’arco della giornata. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "In questo ultimo caso, ti conviene fare uno spuntino o un pasto, a seconda dell’orario, " +
                "che contenga almeno 20 g di proteine. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Se assumi meno di 20 g di proteine in uno o più pasti non c’è nessun problema: " +
                "focalizzati sul raggiungere il fabbisogno proteico della giornata e otterrai i tuoi " +
                "risultati. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "In linea generale, ciascuno spuntino dovrebbe apportare non più del 5% dell’energia " +
                "totale giornaliera (200 – 300 kcal). ",
            articleTitle = "10 spuntini sani se sei a dieta (o meno)",
            url = "https://www.projectinvictus.it/spuntini-sani/",
            publishedDate = "2025-04-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Con gli affettati è meglio non esagerare con le quantità o non consumarli ogni giorno, " +
                "ma al massimo due volte a settimana (per una quantità massima di 50 g), considerando " +
                "anche gli altri pasti della giornata. ",
            articleTitle = "Colazione proteica: cosa mangiare ed esempi",
            url = "https://www.projectinvictus.it/colazione-proteica/",
            publishedDate = "2025-04-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Dal punto di vista calorico non c’è un valore preciso per una colazione ideale, potresti " +
                "assumere dalle 100 Kcal alle 400 Kcal. ",
            articleTitle = "Colazione ideale: la guida completa",
            url = "https://www.projectinvictus.it/colazione-ideale/",
            publishedDate = "2024-08-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Normalmente il periodo più lungo senza alimentarsi è tra la cena e la colazione, quindi " +
                "10-12 ore (e molti saltano anche la colazione); gli inglesi infatti chiamano la " +
                "colazione breakfast: breaking a fast, cioè rompi digiuno. ",
            articleTitle = "Digiuno intermittente: cos’è e come funziona?",
            url = "https://www.projectinvictus.it/digiuno-intermittente/",
            publishedDate = "2024-02-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Consumare da 3 a 5 pasti giornalieri e non saltare i pasti: secondo la dieta Atkins è " +
                "necessario evitare di far passare più di 6 ore tra un pasto e l’altro per ottenere un " +
                "maggior controllo sulla fame. ",
            articleTitle = "Dieta Atkins: come funziona, quanto si perde, menù",
            url = "https://www.projectinvictus.it/dieta-atkins/",
            publishedDate = "2023-06-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Distribuire le proteine giornaliere nei vari pasti (idealmente ogni 3-4 ore) è la scelta " +
                "da preferire per influenzare maggiormente la sintesi proteica, la composizione corporea " +
                "e la performance. ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Una buona scelta è quella di circoscrivere un 50-70% dell’introito glucidico giornaliero " +
                "e comunque non scendere sotto 1.5 g/kg di peso corporeo nei pasti vicini " +
                "all’allenamento, fermo restando che su soggetti in fisiologia l’introito calorico è il " +
                "principale fattore da tenere in considerazione. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Colazione Costituisce, di solito, circa il 15% delle calorie giornaliere. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Pranzo Dovrebbe essere il pasto più abbondante, con circa il 40% delle calorie. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Cena Dovrebbe costituire il secondo pasto più abbondante, con circa il 35% delle " +
                "calorie. ",
            articleTitle = "Dieta Equilibrata: le Basi di un’Alimentazione Sana e Bilanciata",
            url = "https://www.projectinvictus.it/dieta-equilibrata-alimentazione-bilanciata/",
            publishedDate = "2021-05-23",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Colazione Per una sintesi proteica più efficace, è consigliato assumere una quota " +
                "proteica (0.25 g/kg) ogni 3 ore, perciò circa ad ogni pasto per assicurare un costante " +
                "pool ematico aminoacidico e soddisfare il fabbisogno proteico giornaliero. ",
            articleTitle = "Proteine idrolizzate: benefici e controindicazioni",
            url = "https://www.projectinvictus.it/proteine-idrolizzate/",
            publishedDate = "2021-04-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "I pasti principali della giornata (pranzo e cena) dovrebbero rappresentare la quota " +
                "calorica maggiore, ciascuno circa il 35% delle calorie totali giornaliere ed è utile che " +
                "si completino a vicenda dal punto di vista nutrizionale. ",
            articleTitle = "Cosa mangiare a cena per dimagrire?",
            url = "https://www.projectinvictus.it/cena-per-dimagrire/",
            publishedDate = "2021-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Si pensi che questi, a braccio abdotto a 90°, esprimono forze di compressione " +
                "sull’articolazione della spalla pari a 80-90% del peso corporeo. ",
            articleTitle = "Muscolo infraspinato: anatomia e funzioni",
            url = "https://www.projectinvictus.it/infraspinato-anatomia-e-funzioni/",
            publishedDate = "2021-03-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Esistono comunque delle teorie, anche se non del tutto confermate in ambito scientifico, " +
                "che affermano che sarebbe meglio consumare circa 3-4 pasti al giorno distanziati di " +
                "circa 5-6 ore per massimizzare i guadagni ipertrofici (la durata media dell’ effetto " +
                "anabolico di un pasto). ",
            articleTitle = "Intermittent fasting: come farlo in pratica?",
            url = "https://www.projectinvictus.it/intermittent-fasting/",
            publishedDate = "2020-12-11",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Abbuffata notturna di dolci Tra i disturbi dell’alimentazione c’è anche la sindrome dei " +
                "mangiatori notturni : il soggetto interrompe il sonno, tende a fare uno spuntino prima " +
                "di riaddormentarsi, mangia almeno il 25% delle calorie dopo cena e durante la notte. ",
            articleTitle = "Abbuffate di dolci e cibo: ingrasso tanto?",
            url = "https://www.projectinvictus.it/abbuffate-dolci-natalizie-sopravvivere/",
            publishedDate = "2020-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Fare più pasti ravvicinati tra loro (ogni 1, 2, 3 ore) non aumenta il metabolismo e " +
                "quindi non fa dimagrire di più : a meno che questo non ti aiuti, in regime ipocalorico, " +
                "a mantenere più aderenza alla dieta. ",
            articleTitle = "Ogni quanto mangiare per dimagrire?",
            url = "https://www.projectinvictus.it/frequenza-dei-pasti/",
            publishedDate = "2020-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Per chi fa palestra o bodybuilding è consigliato consumare circa 3-4 pasti al giorno per " +
                "massimizzare i guadagni ipertrofici soprattutto con pasti proteici (almeno 20 g di " +
                "proteine a pasto). ",
            articleTitle = "Ogni quanto mangiare per dimagrire?",
            url = "https://www.projectinvictus.it/frequenza-dei-pasti/",
            publishedDate = "2020-07-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "Se invece il mio primo evento è a più di 3-4 ore di distanza dal mio primo pasto, avrò " +
                "modo di fare una colazione più corposa. ",
            articleTitle = "Come mangiare prima di una gara",
            url = "https://www.projectinvictus.it/come-mangiare-prima-di-una-gara/",
            publishedDate = "2019-06-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.MEALS,
            statement =
                "I dati del registro rivelarono che a seguito della perdita di peso, circa l’80% delle " +
                "persone dichiarava di consumare regolarmente la prima colazione. ",
            articleTitle = "Colazione e obesità: anche la scienza sbaglia",
            url = "https://www.projectinvictus.it/colazione-e-obesita-anche-la-scienza-sbaglia/",
            publishedDate = "2014-02-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "E’ più importante la quantità totale giornaliera di proteine che il timing, è " +
                "consigliato dividere la quantità proteica in 4-5 volte al giorno , che possono " +
                "coincidere con i normali pasti che fai nell’arco della giornata. ",
            articleTitle = "Quante proteine assumere?",
            url = "https://www.projectinvictus.it/quante-proteine-assumere/",
            publishedDate = "2025-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "In realtà, questa finestra anabolica dura più tempo: l’importante non è fare un preciso " +
                "pasto post workout ma assicurarsi di assumere abbastanza carboidrati (oltre che energia, " +
                "proteine, grassi) nel corso delle 24 ore . ",
            articleTitle = "Carboidrati dopo l’allenamento in palestra",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-palestra/",
            publishedDate = "2024-03-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "Nell’immediato post-workout prendere proteine in polvere non ha grandi utilità, se non " +
                "nei rari casi in cui non assumi cibo o proteine da almeno 4-5 ore , come negli " +
                "allenamenti a digiuno. ",
            articleTitle = "Quando assumere proteine: prima o dopo allenamento?",
            url = "https://www.projectinvictus.it/proteine-prima-o-dopo-allenamento/",
            publishedDate = "2023-09-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "La sintesi proteica non avviene in poco tempo ma una volta stimolata, ad esempio con un " +
                "pasto proteo-glucidico post-allenamento, può durare anche 24-48 ore: risulta così " +
                "evidente come non siano le proteine post-workout a determinare una proteosintesi " +
                "efficace, ma la loro assunzione nella giornata. ",
            articleTitle = "Proteine palestra: quali prendere e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-palestra/",
            publishedDate = "2023-09-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "Se ci si allena a stomaco vuoto, per ridurre l’effetto catabolico muscolare, si " +
                "consiglia di assumere HMB in associazione a 20-40 g di proteine ad alto valore biologico " +
                "nel post-workout . ",
            articleTitle = "HMB integratore: cos’è, a cosa serve, funziona?",
            url = "https://www.projectinvictus.it/hmb-integratore/",
            publishedDate = "2022-01-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "Anche il solo allenamento sembra promuovere la sintesi proteica nel post-workout fino a " +
                "due volte i livelli normali soprattutto a partire dalle 2 ore successive fino a " +
                "perdurare anche 48 ore. ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "Alcuni esperti asseriscono infatti che il “timing” possa influire al massimo per il " +
                "5-10% circa nel totale della dieta . ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "Ad ogni modo, l’assunzione di 20-40 g di proteine (a seconda della qualità proteica e " +
                "della resistenza all’anabolismo) per pasto (uno di questi da far coincidere come " +
                "post-workout) durante la giornata sono in grado di stimolare al massimo la sintesi " +
                "proteica. ",
            articleTitle = "Post workout: cosa assumere dopo l’allenamento in palestra?",
            url = "https://www.projectinvictus.it/post-workout-palestra-cosa-mangiare/",
            publishedDate = "2021-08-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "Questo è infatti uno dei principali motivi per cui si sosteneva che assumere molti " +
                "carboidrati (fino a 1.2 g/kg nella singola assunzione) all’interno della finestra " +
                "anabolica (le prime 2 ore) fosse vantaggioso per ottenere una maggiore risintesi del " +
                "glicogeno muscolare (1,3). ",
            articleTitle = "Carboidrati post-workout: perché no?",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-perche-no/",
            publishedDate = "2016-08-04",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.TIMING,
            statement =
                "Se l’obbiettivo è estetico , il post-workout può comprendere circa il 30% dei glucidi " +
                "giornalieri, con il restante 70% suddiviso negli altri pasti. ",
            articleTitle = "Carboidrati dopo l’allenamento in palestra",
            url = "https://www.projectinvictus.it/carboidrati-post-workout-palestra/",
            publishedDate = "2024-03-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Il Carb cycling più che una “dieta” è una strategia, motivo per cui va applicata " +
                "solamente quando necessario: non è la ciclizzazione dei carboidrati in sé a promuovere " +
                "la ricomposizione corporea, bensì il contesto di allenamento e calorico in cui questa " +
                "strategia viene applicata. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "A parità di taglio calorico, ciclizzare i glucidi non fa “dimagrire” di più rispetto ad " +
                "uno stesso piano alimentare non ciclizzato. Il Carb cycling deve essere, quindi, una " +
                "strategia che va adottata se permette una maggiore aderenza al piano alimentare (sarà " +
                "l’aderenza a far ottenere i risultati). ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Ripartisci le calorie tra i macronutrienti (carboidrati, grassi, proteine), stabilendo " +
                "quanti grammi di ciascun macro devi assumere in media a settimana. Differenzia i giorni " +
                "tra quelli ad alto quantitativo glucidico e medio/alto lipidico, in modo che la media " +
                "settimanale calorica e di macronutrienti sia quella iniziale. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Fai combaciare le giornate con alto apporto calorico e glucidico con quelle di " +
                "allenamento (giorno ON), quando la spesa energetica è maggiore. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "I giorni ad alto apporto glucidico hanno 50-100 g d i carboidrati in più rispetto a " +
                "quelli a basso apporto glucidico. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Giorno ON: sono da preferire alimenti ricchi di carboidrati e quantità maggiori di " +
                "questi, preferendo fonti proteiche povere di grassi e limitando le fonti lipidiche " +
                "(senza eliminarle). ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Giorno OFF: viene dato più spazio agli alimenti lipidici come la frutta secca, il pesce " +
                "grasso e, senza esagerare, uova, latticini. Le fonti glucidiche sono presenti ma in " +
                "quantità più ridotta. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Contro del Carb cycling: non considera che un muscolo si adatta e “cresce” nei giorni " +
                "senza allenamento; può peggiorare il recupero dato i minori introiti nei giorni OFF. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "In fase di massa (bulk), il Carb cycling nel bodybuilding permette di veicolare le alte " +
                "quote glucidiche verso la cellula muscolare piuttosto che verso la cellula grassa, " +
                "portando ad un minor accumulo di massa grassa, a patto che l’allenamento sia ben " +
                "programmato e ti porti a migliorare la prestazione nel tempo. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Se l’introito glucidico e calorico risulta alto anche nei giorni OFF, puoi introdurre " +
                "delle sessioni di attività cardiovascolare, nel caso in cui tu non voglia tenere una " +
                "differenza troppo marcata tra giorni ON e OFF a livello calorico. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Nella dieta in definizione la percentuale di nutrienti dedicata ai glucidi diminuisce, " +
                "più o meno notevolmente, in risposta all’abbassamento calorico e all’incremento " +
                "proteico. ",
            articleTitle = "Cos’è il Carb cycling e come funziona?",
            url = "https://www.projectinvictus.it/carb-cycling-come-applicarla/",
            publishedDate = "2024-01-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Se dopo 48 ore che non mangi carboidrati hai un calo di energie e di attenzione " +
                "vigoroso, consiglia di inserire una mini ricarica di 12-24 ore il mercoledì. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "L’approccio della ricarica il mercoledì è molto più sensato, puoi anche farlo il giovedì " +
                "togliendo i carboidrati per massimo 3 giorni. ",
            articleTitle = "Dieta metabolica: come funziona",
            url = "https://www.projectinvictus.it/dieta-metabolica/",
            publishedDate = "2024-02-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Per far si di mantenere quella media calorica settimanale (la più bassa di tutta la mia " +
                "vita), o facevo ancora più la fame gli altri 6 giorni cosi da concedermi più calorie il " +
                "giorno singolo di ricarica, oppure dovevo bilanciare tutto quanto nella maniera che " +
                "ritenevo più accettabile e proficua. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Per limitare questo catabolismo le diete low carb consigliano d’introdurre almeno " +
                "150-200g di proteine. ",
            articleTitle = "I grassi bruciano al fuoco dei carboidrati",
            url = "https://www.projectinvictus.it/i-grassi-bruciano-al-fuoco-dei-carboidrati/",
            publishedDate = "2018-06-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "Nel medio termine, infatti, a parità di calorie e macronutrienti, otterrete gli stessi " +
                "identici risultati utilizzando una semplice ciclizzazione ON-OFF con un 20-25% di " +
                "differenza nell’apporto dei carboidrati e introducendo un REFEED ogni tanto nelle ultime " +
                "settimane. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.CARB_CYCLING,
            statement =
                "CK almost certainly increased by 77.9±25.4% (0.75±0.19) post-simulation for all players. ",
            articleTitle = "Metabolic demands and replenishment of muscle glycogen after a rugby league match simulation protocol",
            url = "https://pubmed.ncbi.nlm.nih.gov/28238618/",
            publishedDate = "2017",
            kind = SourceKind.INDIVIDUAL_STUDY
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "In generale si consiglia di bere almeno 30-35 mL di acqua per kg di peso corporeo . ",
            articleTitle = "Quanta acqua bere al giorno",
            url = "https://www.projectinvictus.it/quanta-acqua-bere-al-giorno/",
            publishedDate = "2026-02-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "In base alle kcal consumate è possibile stimare l’intervallo idrico di riferimento " +
                "secondo cui un adulto dovrebbe introdurre almeno 1 mL di acqua per ogni kcal alimentare " +
                "introdotta nel corso della giornata. ",
            articleTitle = "Quanta acqua bere al giorno",
            url = "https://www.projectinvictus.it/quanta-acqua-bere-al-giorno/",
            publishedDate = "2026-02-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Mentre si può sopravvivere anche 10 settimane senza mangiare, la morte sopraggiunge solo " +
                "dopo pochi giorni senza assumere acqua. ",
            articleTitle = "Quanta acqua bere al giorno",
            url = "https://www.projectinvictus.it/quanta-acqua-bere-al-giorno/",
            publishedDate = "2026-02-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Un altro modo usato per garantire una buona idratazione è di consumare almeno 1-1.5 L di " +
                "acqua ogni 1000 kcal . ",
            articleTitle = "Bere tanta acqua per dimagrire",
            url = "https://www.projectinvictus.it/bere-acqua-fa-dimagrire/",
            publishedDate = "2025-05-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Quando mangi pochi carboidrati puoi perdere in una settimana anche 2-4 kg , in quanto " +
                "cala l’ acqua intracellulare ed interstiziale. ",
            articleTitle = "Dimagrire velocemente e perdere peso",
            url = "https://www.projectinvictus.it/dimagrire-velocemente/",
            publishedDate = "2025-01-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Per le donne l’apporto consigliato è di circa 1,5-2 litri, mentre per gli uomini 2-2,5 " +
                "litri – entrambe quantità da personalizzare in base a quanti liquidi si assumono tramite " +
                "gli alimenti, al peso corporeo e all’attività fisica svolta. ",
            articleTitle = "Voglio dimagrire 20 kg: come perderli in modo definitivo",
            url = "https://www.projectinvictus.it/voglio-dimagrire-20-kg/",
            publishedDate = "2024-08-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Cetriolo Il cetriolo è considerato un alimento drenante , idratante e antiossidante " +
                "grazie alla quantità di acqua presente (circa il 96.5% del peso) e di vitamine come la " +
                "vitamina C . ",
            articleTitle = "Cibi e alimenti drenanti",
            url = "https://www.projectinvictus.it/cibo-drenante/",
            publishedDate = "2023-08-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Quelle ipotoniche hanno una minore concentrazione di carboidrati (< 6 g/100 ml) e sono " +
                "utili quando l’idratazione è più necessaria rispetto all’apporto glucidico. ",
            articleTitle = "Bevande energetiche per lo sport",
            url = "https://www.projectinvictus.it/bevande-energetiche/",
            publishedDate = "2023-06-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Normalmente viene consigliato di integrare le bevande energetiche durante attività " +
                "fisiche continue superiori a 60-90 minuti ma, quando particolarmente intense, puoi " +
                "integrare anche con attività più brevi – in questo caso è opportuno diminuire il " +
                "contenuto di acqua e di carboidrati. ",
            articleTitle = "Bevande energetiche per lo sport",
            url = "https://www.projectinvictus.it/bevande-energetiche/",
            publishedDate = "2023-06-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Già con una diminuzione del 2% del peso corporeo a carico dei liquidi ( 2% di " +
                "disidratazione ) la performance incomincia ad essere inficiata, al 4% l’abbassamento è " +
                "addirittura del 20-30%. ",
            articleTitle = "Allenamento con il caldo nel bodybuilding: benefici, rischi e rimedi",
            url = "https://www.projectinvictus.it/allenarsi-con-il-caldo/",
            publishedDate = "2021-11-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Tra queste troviamo anche l’ acqua e l’ alcol , il quale apporta 7kcal/g ma non ha altre " +
                "funzioni ed è tossico per l’organismo, quindi normalmente non viene annoverato tra i " +
                "nutrienti . ",
            articleTitle = "Sostanze nutritive: cosa sono e quali sono?",
            url = "https://www.projectinvictus.it/sostanze-nutritive/",
            publishedDate = "2021-09-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "La necessaria cottura dei legumi secchi li porta ad aumentare di circa 3 volte il peso " +
                "iniziale e pertanto i valori nutrizionali andranno normalizzati dividendo per il " +
                "coefficiente di idratazione (andranno divise le calorie e tutti i valori nutrizionali " +
                "presenti in etichetta per 3). ",
            articleTitle = "Legumi: quanti sono e quali sono i migliori?",
            url = "https://www.projectinvictus.it/legumi/",
            publishedDate = "2020-02-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.HYDRATION,
            statement =
                "Essendo l’apporto di acqua, complice anche il caldo, arrivato alla soglia dei 10 litri e " +
                "non potendo pertanto agire su quella leva, decidiamo di apportare una piccola modifica " +
                "al piano di Peaking, tagliando altre 150 Kcal medie giornaliere tramite una diminuzione " +
                "dei grassi a 30 g. ",
            articleTitle = "Natural Peaking: Diario di una trasformazione corporea",
            url = "https://www.projectinvictus.it/natural-peaking-teoria-pratica/",
            publishedDate = "2016-10-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.ALCOHOL,
            statement =
                "Converrebbe limitare l’assunzione di alcol 1-2 volte a settimana , senza superare le " +
                "dosi consigliate (2-3 bicchieri nell’uomo, 1-2 nella donna). ",
            articleTitle = "Come aumentare il testosterone",
            url = "https://www.projectinvictus.it/come-aumentare-il-testosterone/",
            publishedDate = "2026-08-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.ALCOHOL,
            statement =
                "Ci sono molte variabili da cui dipende il metabolismo basale, anche se alcuni hanno più " +
                "influenza rispetto ad altri: età, sesso, massa magra spiegano circa l’80% del " +
                "metabolismo basale , che infatti sono i tre fattori solitamente inclusi nelle formule di " +
                "calcolo. ",
            articleTitle = "Calcolo metabolismo basale",
            url = "https://www.projectinvictus.it/metabolismo-basale/",
            publishedDate = "2025-01-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.ALCOHOL,
            statement =
                "Questo dipende da molti fattori e può essere calcolato: per via teorica tramite delle " +
                "formule, per via pratica, monitorando per 1-2 settimane tutto ciò che introduci (ad es. ",
            articleTitle = "Deficit calorico: cos’è e come calcolarlo",
            url = "https://www.projectinvictus.it/deficit-calorico-calcolo/",
            publishedDate = "2025-01-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.ALCOHOL,
            statement =
                "Se non puoi fare a meno del vino o della birra cerca di limitare le occasioni di consumo " +
                "a 1-2 volte alla settimana e fermarti a 1-2 bicchieri. ",
            articleTitle = "Dimagrire senza sport: come fare",
            url = "https://www.projectinvictus.it/dimagrire-senza-sport/",
            publishedDate = "2023-04-14",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.ALCOHOL,
            statement =
                "Assicurati di dormire almeno 7 ore per notte ed evita di bere alcolici se non in modo " +
                "moderato 1 – 2 volte a settimana . ",
            articleTitle = "Come diventare grossi in poco tempo",
            url = "https://www.projectinvictus.it/come-diventare-grossi/",
            publishedDate = "2022-07-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUBSTITUTION,
            statement =
                "3) Esercizi per il rinforzo dei muscoli extrarotatori e stabilizzatori della scapola: " +
                "OPPURE 4) Miglioramento della flessibilità della capsula: eseguire 3 serie di 45″ di " +
                "tenuta di Cross Body Stretch e Sleeper’s Stretch ( solo se questo non evoca dolore ). ",
            articleTitle = "Riabilitazione e prevenzione della spalla: impingement sub-acromiale",
            url = "https://www.projectinvictus.it/riabilitazione-e-prevenzione-della-spalla-impingement-sub-acromiale/",
            publishedDate = "2015-01-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.ADHERENCE,
            statement =
                "La dieta Plank permette una grande perdita di peso iniziale, che va da 6 a 9 kg nelle " +
                "prime due settimane , in base all’aderenza del soggetto al piano alimentare suggerito. ",
            articleTitle = "Dieta Plank: come funziona e controindicazioni",
            url = "https://www.projectinvictus.it/dieta-plank/",
            publishedDate = "2026-01-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.ADHERENCE,
            statement =
                "Abbuffata notturna di dolci Tra i disturbi dell’alimentazione c’è anche la sindrome dei " +
                "mangiatori notturni : il soggetto interrompe il sonno, tende a fare uno spuntino prima " +
                "di riaddormentarsi, mangia almeno il 25% delle calorie dopo cena e durante la notte. ",
            articleTitle = "Abbuffate di dolci e cibo: ingrasso tanto?",
            url = "https://www.projectinvictus.it/abbuffate-dolci-natalizie-sopravvivere/",
            publishedDate = "2020-09-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Secondo i LARN, punto di riferimento per l’assunzione dei nutrienti in Italia, la " +
                "quantità di grassi da assumere per la popolazione italiana corrisponde al 20-35% del " +
                "totale calorico giornaliero oppure fino a 1,5 g di grassi/kg peso corporeo. ",
            articleTitle = "Quanti grassi assumere?",
            url = "https://www.projectinvictus.it/quanti-grassi-al-giorno/",
            publishedDate = "2026-08-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Grassi : 20-30% dell’introito calorico, senza scendere al di sotto dei 0.5g grassi/kg di " +
                "peso corporeo, da consumare preferibilmente lontano dall’allenamento. ",
            articleTitle = "Dieta personalizzata: la dieta che funziona",
            url = "https://www.projectinvictus.it/dieta-personalizzata/",
            publishedDate = "2026-07-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Poiché la cannella si rivela utile a dosaggi di circa 300 mg / kg di peso corporeo, che " +
                "implicano concentrazioni di cumarina nettamente superiori alla TDI in certe tipologie di " +
                "cannella, è molto importante selezionare accuratamente il tipo di prodotto che si " +
                "intende consumare . ",
            articleTitle = "Cannella: proprietà, controindicazioni, dosi giornaliere",
            url = "https://www.projectinvictus.it/cannella-benefici-proprieta/",
            publishedDate = "2026-03-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "In questo modo, per raggiungere la TDI di 0,1 mg / kg di peso corporeo, un uomo di 100 " +
                "kg dovrebbe ingerire 50 g di Ceylon. ",
            articleTitle = "Cannella: proprietà, controindicazioni, dosi giornaliere",
            url = "https://www.projectinvictus.it/cannella-benefici-proprieta/",
            publishedDate = "2026-03-22",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "In generale si consiglia di bere almeno 30-35 mL di acqua per kg di peso corporeo . ",
            articleTitle = "Quanta acqua bere al giorno",
            url = "https://www.projectinvictus.it/quanta-acqua-bere-al-giorno/",
            publishedDate = "2026-02-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "E’ consigliato perdere lo 0,5 – 1% del proprio peso corporeo a settimana per avere un " +
                "dimagrimento sostenibile, sano e duraturo . ",
            articleTitle = "Dieta per dimagrire 10 kg",
            url = "https://www.projectinvictus.it/dieta-per-dimagrire-10-kg/",
            publishedDate = "2025-09-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Per definizione, una dieta è iperproteica quando le proteine ricoprono almeno il 35% " +
                "dell’introito calorico totale o che la quota proteica superi il valore RDA di 0,8 g " +
                "proteine/kg peso corporeo . ",
            articleTitle = "La dieta iperproteica per il dimagrimento",
            url = "https://www.projectinvictus.it/dieta-iperproteica-dimagrimento/",
            publishedDate = "2025-05-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Per una perdita di peso e di grasso efficace e fisiologica è consigliato perdere lo " +
                "0,5-1% del proprio peso corporeo a settimana . ",
            articleTitle = "La dieta iperproteica per il dimagrimento",
            url = "https://www.projectinvictus.it/dieta-iperproteica-dimagrimento/",
            publishedDate = "2025-05-08",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Con questo taglio calorico, da fare rispetto al tuo dispendio energetico giornaliero, " +
                "puoi immaginare di perdere circa lo 0,5 – 1% del peso corporeo a settimana . ",
            articleTitle = "Grasso addominale: cos’è, come eliminarlo, rischi",
            url = "https://www.projectinvictus.it/grasso-addominale/",
            publishedDate = "2025-04-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Punta a perdere lo 0,5-1% del tuo peso corporeo a settimana (massimo 2% nelle prime " +
                "settimane) . ",
            articleTitle = "Deficit calorico: cos’è e come calcolarlo",
            url = "https://www.projectinvictus.it/deficit-calorico-calcolo/",
            publishedDate = "2025-01-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Per aumentare di 2 kg di peso corporeo sono necessari circa 25-40 giorni . ",
            articleTitle = "Quanto tempo ci vuole ad ingrassare?",
            url = "https://www.projectinvictus.it/quanto-tempo-per-ingrassare/",
            publishedDate = "2024-04-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Per un dimagrimento fisiologico è consigliato perdere lo 0.5-1% del tuo peso corporeo a " +
                "settimana. ",
            articleTitle = "Dieta mediterranea per dimagrire",
            url = "https://www.projectinvictus.it/dieta-mediterranea/",
            publishedDate = "2024-03-29",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Solitamente per un dimagrimento efficace e graduale è consigliato perdere 0.5-1% del " +
                "proprio peso corporeo a settimana . ",
            articleTitle = "Dieta a zona: come funziona e menù",
            url = "https://www.projectinvictus.it/dieta-a-zona/",
            publishedDate = "2024-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Bisogna perdere in una settimana da 0.5 – 1% del peso corporeo . ",
            articleTitle = "Bodybuilding: la guida completa",
            url = "https://www.projectinvictus.it/bodybuilding/",
            publishedDate = "2024-01-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "In generale possiamo fare due considerazioni: Minore è l’anzianità allenante, maggiori " +
                "sono i margini di guadagno in termini muscolari e peso corporeo , in un range di 1-1.5% " +
                "di aumento del peso corporeo mensile. ",
            articleTitle = "Come aumentare la massa muscolare?",
            url = "https://www.projectinvictus.it/aumentare-massa-muscolare/",
            publishedDate = "2023-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Una perdita di peso ritenuta sana e sostenibile può essere compresa tra lo 0.5-1% del " +
                "tuo peso corporeo a settimana . ",
            articleTitle = "Dieta vegetariana per dimagrire",
            url = "https://www.projectinvictus.it/dieta-vegetariana-per-dimagrire/",
            publishedDate = "2023-06-26",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Sarebbe bene porre un ostacolo a circa 60 cm dalla sbarra in modo da dosare " +
                "l’oscillazione entro quel range, non dichiarando valide le ripetizioni in cui l’ostacolo " +
                "viene toccato con i piedi. ",
            articleTitle = "Muscle Up: esecuzione corretta e come iniziare",
            url = "https://www.projectinvictus.it/muscle-up-la-guida-completa/",
            publishedDate = "2023-01-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "La quota proteica e l’eventuale surplus calorico scongiurano il catabolismo muscolare, " +
                "anzi promuovono l’anabolismo; se segui un’alimentazione ipocalorica , bilanciata nei " +
                "macronutrienti, con un apporto proteico di 2,3-3,1g/kg di peso corporeo. ",
            articleTitle = "Catabolismo",
            url = "https://www.projectinvictus.it/catabolismo/",
            publishedDate = "2022-09-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Una perdita di peso corretta dovrebbe essere tra lo 0,5 – 1 % del proprio peso corporeo " +
                "a settimana . ",
            articleTitle = "Il bilancio energetico",
            url = "https://www.projectinvictus.it/bilancio-energetico/",
            publishedDate = "2022-08-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Anche per le donne una perdita di peso corretta dovrebbe essere tra lo 0,5 – 1 % del " +
                "proprio peso corporeo a settimana. ",
            articleTitle = "Il bilancio energetico",
            url = "https://www.projectinvictus.it/bilancio-energetico/",
            publishedDate = "2022-08-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Anche gli esercizi con i pesi sono caldamente raccomandati – un buon 80% del proprio " +
                "peso corporeo in military press è un ottimo obiettivo iniziale. ",
            articleTitle = "Bandiera umana: tutorial e progressioni per impararla",
            url = "https://www.projectinvictus.it/la-bandiera-human-flag-tutorial-ed-esecuzione/",
            publishedDate = "2022-03-19",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Già con una diminuzione del 2% del peso corporeo a carico dei liquidi ( 2% di " +
                "disidratazione ) la performance incomincia ad essere inficiata, al 4% l’abbassamento è " +
                "addirittura del 20-30%. ",
            articleTitle = "Allenamento con il caldo nel bodybuilding: benefici, rischi e rimedi",
            url = "https://www.projectinvictus.it/allenarsi-con-il-caldo/",
            publishedDate = "2021-11-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "I dosaggi raccomandati sono compresi nel range di 3-6 mg/kg di peso corporeo al giorno, " +
                "non necessariamente ogni giorno poichè svolge i suoi effetti in acuto (ad esempio, nei " +
                "giorni di allenamento, prima dell’allenamento). ",
            articleTitle = "Caffeina, allenamento e palestra: effetti e dosaggi",
            url = "https://www.projectinvictus.it/caffeina-effetti-e-dosaggi/",
            publishedDate = "2021-07-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Se con il taglio calorico ti risulta difficile rispettare questi valori, può essere " +
                "utile ridurre il deficit : ad esempio se hai prefissato una perdita di peso dell’1% del " +
                "peso corporeo a settimana, passa allo 0.5%. ",
            articleTitle = "Macronutrienti: cosa sono e in quali alimenti si trovano?",
            url = "https://www.projectinvictus.it/macronutrienti-nella-dieta-cosa-dobbiamo-sapere/",
            publishedDate = "2021-01-17",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Gli sportivi rispetto ai sedentari: è chiaro che una persona attiva necessita di una " +
                "quota maggiore di energia, con un quantitativo glucidico anche doppio rispetto ad un " +
                "sedentario e che può arrivare fino a 10-12g di carboidrati per kg di peso corporeo al " +
                "giorno! ",
            articleTitle = "Quanti carboidrati al giorno",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno/",
            publishedDate = "2020-04-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Per questo motivo, è consigliato perdere 0.5-1% del peso corporeo a settimana . ",
            articleTitle = "Quanti carboidrati per dimagrire?",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno-per-dimagrire/",
            publishedDate = "2020-04-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "È importante, comunque, mantenere un introito minimo pari a 1g/1 kg di peso corporeo . ",
            articleTitle = "Quanti carboidrati per dimagrire?",
            url = "https://www.projectinvictus.it/quanti-carboidrati-al-giorno-per-dimagrire/",
            publishedDate = "2020-04-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Idealmente, per gli atleti è consigliato il consumo di 1.0-1.5 g di carboidrati/kg di " +
                "peso corporeo ogni 2 ore nelle 6 ore dopo l’allenamento. ",
            articleTitle = "Destrosio in palestra: che cos’è e a cosa serve",
            url = "https://www.projectinvictus.it/destrosio-bodybuilding-palestra/",
            publishedDate = "2020-03-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Le stime “standard” riguardo al fabbisogno proteico, che sentirai pronunciare più o meno " +
                "da ogni medico, corrispondono sempre a circa 0.8 g/kg peso corporeo. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Fabbisogno proteico giornaliero secondo l’OMS Nelle tabelle indicate dall’OMS per una " +
                "persona adulta (> 18 anni) sono indicati 0.66 g proteine per ogni chilo di peso " +
                "corporeo, sia per gli uomini che per le donne. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Con l’ avanzare dell’età per le donne è bene aumentare l’apporto proteico : da circa " +
                "0.70 g/kg peso corporeo passiamo a una dose consigliata maggiore di 0.84 g di proteine. ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "225 sono i grammi di proteine da assumere al giorno (pari a circa 2.8 g/kg di peso " +
                "corporeo). ",
            articleTitle = "Fabbisogno proteico: come calcolarlo?",
            url = "https://www.projectinvictus.it/fabbisogno-proteico/",
            publishedDate = "2020-03-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "I carboidrati dovrebbero comprendere il 55-60% del fabbisogno calorico giornaliero, " +
                "mentre per le proteine il valore consigliato è 1.4 – 2 g/kg di peso corporeo . ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Conviene iniziare ad approcciarle quando facciamo le trazioni zavorrate con almeno il " +
                "50-60% del nostro peso corporeo. ",
            articleTitle = "Esercizi calistenici: quali sono, quali fare a casa",
            url = "https://www.projectinvictus.it/esercizi-calistenici/",
            publishedDate = "2019-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Per far si di mantenere quella media calorica settimanale (la più bassa di tutta la mia " +
                "vita), o facevo ancora più la fame gli altri 6 giorni cosi da concedermi più calorie il " +
                "giorno singolo di ricarica, oppure dovevo bilanciare tutto quanto nella maniera che " +
                "ritenevo più accettabile e proficua. ",
            articleTitle = "Il mio percorso bodybuilding (Nicolò Liani)",
            url = "https://www.projectinvictus.it/percorso-nicolo-liani/",
            publishedDate = "2018-09-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "I muscoli scheletrici, motore del metabolismo La muscolatura scheletrica rappresenta il " +
                "40% del peso corporeo ed è per questo il più esteso organo di cui disponiamo. ",
            articleTitle = "Cosa succede quando il muscolo incontra il grasso?",
            url = "https://www.projectinvictus.it/muscolo-incontra-grasso/",
            publishedDate = "2017-03-09",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "La proposta nell’applicazione dell’endurance muscolare per le scrollate inverse è quella " +
                "di eseguire le ripetizioni alla massima fatica (cedimento) secondo quanto permesso dal " +
                "peso corporeo, e impostare pause di 30 secondi tra le serie multiple. ",
            articleTitle = "Scrollate inverse per il riequilibrio scapolare",
            url = "https://www.projectinvictus.it/scrollate-inverse-per-riequilibrio-scapolare/",
            publishedDate = "2014-11-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.WEIGHT,
            statement =
                "Se il peso corporeo rappresenta un sovraccarico troppo elevato (che permette meno di " +
                "12-15 ripetizioni massime), è possibile optare per la variante facilitata da seduti su " +
                "panca, con i piedi appoggiati a terra. ",
            articleTitle = "Scrollate inverse per il riequilibrio scapolare",
            url = "https://www.projectinvictus.it/scrollate-inverse-per-riequilibrio-scapolare/",
            publishedDate = "2014-11-06",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Se 3 allenamenti a settimana sono sufficienti per notare cambiamenti della composizione " +
                "corporea , considera che poi ogni allenamento aggiunto contribuisce a creare una " +
                "maggiore spesa energetica, specialmente se ogni sessione allenante consuma almeno 300 " +
                "kcal. ",
            articleTitle = "Il migliore allenamento per dimagrire",
            url = "https://www.projectinvictus.it/allenamento-per-dimagrire/",
            publishedDate = "2026-05-15",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Un dimagrimento equilibrato e sano , che ti permette di bruciare davvero massa grassa " +
                "deve essere graduale : è ottimale perdere lo 0,5-1% del proprio peso corporeo a " +
                "settimana. ",
            articleTitle = "Dieta Plank: come funziona e controindicazioni",
            url = "https://www.projectinvictus.it/dieta-plank/",
            publishedDate = "2026-01-18",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Dieta per aumentare la massa muscolare senza ingrassare Prima di tutto, per evitare di " +
                "ingrassare troppo è bene evitare di iniziare una fase di massa se hai percentuale di " +
                "massa grassa superiore a circa l’11% per gli uomini e 16% per le donne . ",
            articleTitle = "Dieta per la massa muscolare: la guida completa",
            url = "https://www.projectinvictus.it/dieta-per-la-massa-muscolare/",
            publishedDate = "2025-03-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Quanti kg puoi perdere in una settimana In una settimana puoi perdere circa 1-5 kg , a " +
                "seconda del tuo peso di partenza e della tua condizione fisica: più pesi e più hai una " +
                "cattiva composizione corporea (grasso e liquidi in eccesso, massa magra bassa) più " +
                "riesci a perdere kg. ",
            articleTitle = "Come dimagrire in una settimana",
            url = "https://www.projectinvictus.it/dimagrire-in-una-settimana/",
            publishedDate = "2025-01-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Se hai messo su troppa massa grassa, puoi continuare con il mini-cut fino a quando non " +
                "torni ad una percentuale di massa grassa accettabile (massimo circa 13% per gli uomini, " +
                "20% per le donne). ",
            articleTitle = "Mini cut: cos’è e come impostarlo in fase di massa",
            url = "https://www.projectinvictus.it/mini-cut/",
            publishedDate = "2024-11-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Se la tua percentuale di grasso corporeo attuale è superiore al 20-21%, è consigliato " +
                "vivamente di ridurre la percentuale di grasso corporeo, altrimenti rischi di fare una " +
                "fase di “grassa” e non di “massa”. ",
            articleTitle = "Dieta donna nel bodybuilding e in palestra",
            url = "https://www.projectinvictus.it/dieta-donne-bodybuilding/",
            publishedDate = "2024-07-31",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Se alla fine delle 14 settimane il peso e la massa grassa continuano a non aumentare, " +
                "Luca può continuare con questo schema di incremento. ",
            articleTitle = "Reverse diet: come impostarla",
            url = "https://www.projectinvictus.it/reverse-diet/",
            publishedDate = "2024-03-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Distribuire le proteine giornaliere nei vari pasti (idealmente ogni 3-4 ore) è la scelta " +
                "da preferire per influenzare maggiormente la sintesi proteica, la composizione corporea " +
                "e la performance. ",
            articleTitle = "Nutrient timing dei pasti per la palestra e il bodybuilding",
            url = "https://www.projectinvictus.it/nutrient-timing/",
            publishedDate = "2021-11-02",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "Ovviamente, questo dato non fa differenza tra massa magra o massa grassa: ad esempio, un " +
                "bodybuilder di 100 kg risulterebbe grasso anche col 5% di FM (Fat Mass, massa grassa). ",
            articleTitle = "Le calorie fanno ingrassare veramente?",
            url = "https://www.projectinvictus.it/una-calorie-e-un-caloria-vediamo-di-fare-chiarezza/",
            publishedDate = "2021-11-01",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "I valori di riferimento di massa grassa da mantenere sono circa il 15% per gli uomini e " +
                "il 22% per le donne. ",
            articleTitle = "Dieta per ingrassare velocemente",
            url = "https://www.projectinvictus.it/dieta-per-ingrassare/",
            publishedDate = "2020-02-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "In letteratura gli effetti sono in media neutri (anche se la maggior parte degli studi " +
                "hanno preso in esame il peso e non la composizione corporea) ma con enorme variabilità " +
                "tra donne, si sono osservate perdite fino a 16kg e aumenti fino a 36kg in 12 mesi! ",
            articleTitle = "Ormoni femminili: cosa conoscere sugli estrogeni",
            url = "https://www.projectinvictus.it/ormoni-femminili-cosa-conoscere-sugli-estrogeni/",
            publishedDate = "2019-06-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.BODYCOMP,
            statement =
                "In questo contesto, però, non sempre è conveniente non incrementare il tessuto adiposo " +
                "in quando potresti trovarti a fine definizione con una percentuale molto bassa (circa " +
                "5%) di massa grassa , che è sottofisiologica . ",
            articleTitle = "Reverse diet: come impostarla",
            url = "https://www.projectinvictus.it/reverse-diet/",
            publishedDate = "2024-03-07",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Nell’immediato post-workout prendere proteine in polvere non ha grandi utilità, se non " +
                "nei rari casi in cui non assumi cibo o proteine da almeno 4-5 ore , come negli " +
                "allenamenti a digiuno. ",
            articleTitle = "Quando assumere proteine: prima o dopo allenamento?",
            url = "https://www.projectinvictus.it/proteine-prima-o-dopo-allenamento/",
            publishedDate = "2023-09-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "La creatina viene sintetizzata dall’organismo (nel fegato, a partire da arginina, " +
                "metionina e glicina) e assunta con gli alimenti (carne e pesce), in percentuali del 50% " +
                "e 50%. ",
            articleTitle = "Creatina monoidrato per la palestra",
            url = "https://www.projectinvictus.it/creatina-monoidrato/",
            publishedDate = "2022-01-10",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Ovviamente, vale la pena domandarsi se abbia senso farlo, in quando l’ assunzione di " +
                "caffeina va solitamente collocata circa un’oretta prima della seduta di allenamento, in " +
                "un range utile di 30-60 minuti, mentre di creatina post-workout insieme ad un pasto " +
                "prevalentemente glucidico. ",
            articleTitle = "Creatina e caffeina: effetti positivi e negativi",
            url = "https://www.projectinvictus.it/creatina-caffeina/",
            publishedDate = "2021-12-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "In coloro che seguono una dieta equilibrata ed onnivora , la saturazione delle riserve " +
                "di creatina fosfato raggiunge il 60-80% del totale; nei vegani invece, arriva a malapena " +
                "al 40% . ",
            articleTitle = "Creatina nel bodybuilding: assunzione in fase di definizione muscolare",
            url = "https://www.projectinvictus.it/creatina-definizione-muscolare/",
            publishedDate = "2021-12-12",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Questo protocollo “standard” di integrazione assicura di saturare le scorte muscolari di " +
                "creatina in 3-4 settimane (secondo alcuni studi, mediamente in 28 giorni). ",
            articleTitle = "Creatina in palestra: cos’è, a cosa serve, benefici e controindicazioni",
            url = "https://www.projectinvictus.it/creatina-a-cosa-serve/",
            publishedDate = "2021-11-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Viceversa, un vegano poco attento alla propria alimentazione potrebbe possedere riserve " +
                "muscolari di creatina appena del 40% rispetto al proprio limite massimo. ",
            articleTitle = "Creatina in palestra: cos’è, a cosa serve, benefici e controindicazioni",
            url = "https://www.projectinvictus.it/creatina-a-cosa-serve/",
            publishedDate = "2021-11-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Anche la popolazione dei non responder , pari al 25-35%, può avvantaggiarsi " +
                "dell’integrazione con creatina, scendendo al compromesso di utilizzare dosaggi superiori " +
                "alla norma e col metodo del carico . ",
            articleTitle = "Creatina in palestra: cos’è, a cosa serve, benefici e controindicazioni",
            url = "https://www.projectinvictus.it/creatina-a-cosa-serve/",
            publishedDate = "2021-11-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Dose giornaliera di vitamina D: fabbisogno giornaliero La concentrazione sierica di " +
                "vitamina D è dipendente sia dalla quantità prodotta dalla cute grazie ai raggi solare " +
                "che da quella introdotta con la dieta; la sua emivita è di circa 15 giorni. ",
            articleTitle = "Vitamina D per l’allenamento",
            url = "https://www.projectinvictus.it/vitamina-d/",
            publishedDate = "2021-08-16",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Le proteine del siero del latte ( whey ) rappresentano il 20% delle proteine del latte e " +
                "hanno la caratteristica di avere una buona composizione aminoacidica. ",
            articleTitle = "Proteine del siero del latte: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-del-siero-del-latte/",
            publishedDate = "2021-05-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Sebbene i grassi omega 3 siano più abbondanti nei frutti di mare, essi si trovano anche " +
                "negli animali allevati al pascolo, dove sono presenti tra 2 e 6 volte più omega 3. ",
            articleTitle = "La Dieta Mountain Dog (Parte 2)",
            url = "https://www.projectinvictus.it/la-dieta-mountain-dog-parte-2/",
            publishedDate = "2014-05-05",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SUPPLEMENT,
            statement =
                "Le proteine tipicamente contengono il 15-20% di BCAA, mentre alcune qualità di whey ne " +
                "contengono fino al 25%. ",
            articleTitle = "BCAA sempre e comunque utili?",
            url = "https://www.projectinvictus.it/bcaa-sempre-e-comunque-utili/",
            publishedDate = "2014-03-13",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Isolate Per definizione la forma con il maggiore grado di purezza, arrivando a " +
                "percentuali proteiche fino al 90-95%, quantità di lattosio e grassi insignificanti e " +
                "minore presenza di minerali. ",
            articleTitle = "Le 6 migliori proteine in polvere",
            url = "https://www.projectinvictus.it/migliori-proteine-in-polvere/",
            publishedDate = "2024-11-03",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Questo contiene 5 mini box con varietà di barrette, zuppe, bibite e supplementi da " +
                "assumere Controindicazioni ed effetti collaterali della Dieta Mima Digiuno Alcune " +
                "persone accusano una sensazione di debolezza durante i giorni della DMD. ",
            articleTitle = "Dieta Mima Digiuno: come funziona, benefici e cosa mangiare",
            url = "https://www.projectinvictus.it/dieta-mima-digiuno/",
            publishedDate = "2024-07-30",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Tra i sintomi di intolleranza al glutine più frequenti ci sono: diarrea, steatorrea, " +
                "perdita di peso (per via del malassorbimento ); sintomi extra-intestinali o atipici come " +
                "osteoporosi, anemia, dermatite che affliggono quasi il 50% dei pazienti. ",
            articleTitle = "Glutine: cos’è, dove si trova, perché fa male",
            url = "https://www.projectinvictus.it/glutine/",
            publishedDate = "2023-06-28",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Le allergie alimentari sono risposte avverse agli alimenti, le cui cause dipendono da " +
                "meccanismi immunologici, interessano circa il 4% della popolazione adulta. ",
            articleTitle = "Allergie alimentari: sintomi, cause, rimedi",
            url = "https://www.projectinvictus.it/allergie-alimentari/",
            publishedDate = "2022-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Le manifestazioni cliniche legate ad un’allergia compaiono entro pochi minuti fino ad un " +
                "massimo di 2 ore dall’ingestione dell’ alimento offending . ",
            articleTitle = "Allergie alimentari: sintomi, cause, rimedi",
            url = "https://www.projectinvictus.it/allergie-alimentari/",
            publishedDate = "2022-12-21",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Isolate in polvere Le proteine isolate hanno una percentuale proteica molto alta " +
                "(90-95%) mentre le altre componenti (lattosio, grassi, sali minerali) sono praticamente " +
                "assenti. ",
            articleTitle = "Proteine del siero del latte: cosa sono e a cosa servono?",
            url = "https://www.projectinvictus.it/proteine-del-siero-del-latte/",
            publishedDate = "2021-05-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Per definizione sono la forma che ha il maggior grado di purezza : la percentuale " +
                "proteica è molto alta (90-95%), mentre tutte le altre componenti (lattosio, grassi, sali " +
                "minerali) sono praticamente assenti – le proteine risultano quindi “isolate” rispetto " +
                "alle altre sostanze presenti nel latte. ",
            articleTitle = "Proteine isolate: benefici e controindicazioni",
            url = "https://www.projectinvictus.it/proteine-isolate/",
            publishedDate = "2021-04-20",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Senza glutine Gli alimenti che contengono glutine e che sono quindi da escludere dalla " +
                "dieta sono sostanzialmente frumento, kamut, segale, orzo e farro, alimenti che " +
                "contengono proteine per il 10-15% del totale. ",
            articleTitle = "Proteine vegetali per la palestra",
            url = "https://www.projectinvictus.it/proteine-vegetali/",
            publishedDate = "2020-07-24",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Anche se difficile da diagnosticare con precisione, si ritiene che le articolazioni " +
                "sacro-iliache siano la fonte di dolore in circa il 25% dei pazienti affetti da lombalgia " +
                "cronica , sebbene la letteratura non abbia ancora trovato un accordo unanime a riguardo. ",
            articleTitle = "Bacino: anatomia e funzioni",
            url = "https://www.projectinvictus.it/bacino-anatomia-funzioni/",
            publishedDate = "2020-05-27",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Da un punto di vista della prognosi, nell’85% circa dei casi il dolore svanisce " +
                "spontaneamente entro un anno ed è caratterizzato da frequenti recidive e riacutizzazioni " +
                "del dolore. ",
            articleTitle = "Gomito: anatomia e dolore",
            url = "https://www.projectinvictus.it/gomito-anatomia-dolore/",
            publishedDate = "2019-10-25",
            kind = SourceKind.DIVULGATIVE
        ),
        Rule(
            category = Category.SAFETY_NUT,
            statement =
                "Dolore al coccige: cause e trattamento Il dolore al coccige, detto coccigodinia, " +
                "rappresenta un problema scarsamente diffuso nella popolazione affetta da disordini " +
                "muscoloscheletrici, con una prevalenza di circa l’1%. ",
            articleTitle = "Il coccige: dolore ed anatomia",
            url = "https://www.projectinvictus.it/coccige/",
            publishedDate = "2020-03-01",
            kind = SourceKind.DIVULGATIVE
        )
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
