package com.fitcoach.app.domain.knowledge

import com.fitcoach.app.domain.model.SourceKind

/**
 * Esercizi documentati da Project InVictus: muscoli coinvolti, esecuzione
 * corretta, errori da evitare, varianti.
 *
 * Serve al motore per due cose: scegliere l'esercizio giusto per il gruppo
 * muscolare, e spiegare all'utente come eseguirlo quando registra la serie.
 *
 * Classificazione: [SourceKind.DIVULGATIVE].
 */
object InvictusExercises {

    data class Exercise(
        val name: String,
        val primaryMuscles: List<String>,
        val articleTitle: String,
        val url: String,
        val execution: List<String>,
        val errors: List<String>,
        val variants: List<String>,
        val kind: SourceKind = SourceKind.DIVULGATIVE
    )

    val exercises: List<Exercise> = listOf(
        Exercise(
            name = "Squat bulgaro",
            primaryMuscles = listOf("quadricipiti", "glutei", "femorali", "addominali"),
            articleTitle = "Squat bulgaro: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/squat-bulgaro/",
            execution = emptyList(),
            errors = listOf(
                "Target muscolare Variante esecutiva Glutei Posiziona il piede a terra più distante " +
                "rispetto alla panca e in modo che il carico sul piede a terra sia più verso il " +
                "tallone. ",

                "Mentre scendi fletti il busto anteriormente mantenendo le curve fisiologiche della " +
                "schiena. ",

                "Scendi più in basso che riesci, finché riesci a mantenere la curva lombare. "
            ),
            variants = listOf(
                "Varianti di squat bulgaro Con manubri o kettlebell : dato il carico relativamente " +
                "basso, è più adatto a medio-alte ripetizioni (10-15). ",

                "Può essere eseguito con due manubri o con un solo manubrio per lato (utile a livello " +
                "coordinativo). "
            )
        ),
        Exercise(
            name = "Calf Raise",
            primaryMuscles = listOf("polpacci"),
            articleTitle = "Calf Raise: esercizi efficaci per i polpacci e scheda di allenamento",
            url = "https://www.projectinvictus.it/calf-raise/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Calf Raise in piedi Il Calf Raise in piedi coinvolge soprattutto il gastrocnemio " +
                "(gemello mediale e laterale). ",

                "Il Calf in piedi può essere eseguito a corpo libero o con uno o due manubri in mano " +
                ", con entrambi i piedi o unilateralmente , stimolando un polpaccio alla volta. "
            )
        ),
        Exercise(
            name = "Piegamenti sulle braccia",
            primaryMuscles = listOf("pettorali", "tricipiti"),
            articleTitle = "Piegamenti sulle braccia: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/piegamenti-sulle-braccia-esecuzione-e-muscoli-coinvolti/",
            execution = listOf(
                "Tecnica corretta L’esecuzione corretta dei Piegamenti sulle braccia prevede di " +
                "posizionarsi in un set-up iniziale di un Plank sulle mani : mani a terra, gomiti in " +
                "blocco articolare, glutei tesi, addome teso. ",

                "Da questa posizione di partenza assicurati di allontanare le spalle dalle orecchie e " +
                "di avvicinare leggermente le scapole tra loro: occhio a non addurle completamente, " +
                "perché questo probabilmente ti farà inarcare troppo, diminuendo il ROM effettivo del " +
                "push up. ",

                "Una volta settate le spalle, piega i gomiti e arriva a sfiorare il suolo. "
            ),
            errors = listOf(
                "Errori comuni da evitare Non settare correttamente le spalle : partire con le " +
                "scapole in avanti e alte non mette in sicurezza l’articolazione e riduce " +
                "l’attivazione muscolare nei distretti che ti interessano maggiormente da allenare. ",

                "Dare troppa enfasi alla contrazione addominale : i Piegamenti non sono eseguiti in " +
                "hollow position , che aumenta di gran lunga la stabilità del busto, ma riduce la " +
                "percezione delle scapole e non fa assettare correttamente le spalle. ",

                "Dare troppa poca enfasi alla contrazione addominale : il rischio è di lasciare il " +
                "sedere in alto , riducendo la leva e quindi il carico percepito durante il " +
                "piegamento. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Military Press",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "trapezio"),
            articleTitle = "Military Press: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/military-press/",
            execution = listOf(
                "Esecuzione corretta del Military press Per eseguire al meglio il Military press con " +
                "bilanciere la presa deve essere poco più larga delle spalle (da 1.2 a 1.5 volte le " +
                "spalle). ",

                "In generale, più il corpo è verticale più la presa deve essere stretta, nell’ottica " +
                "del rispetto del piano di scorrimento scapolare. ",

                "Ecco perché nella Panca piana, dove sei orizzontale, la presa massima suggerita è " +
                "più larga (da 1.5 a 2 volte circa la larghezza delle spalle). "
            ),
            errors = listOf(
                "Errori da evitare nel Military press Muovere le ginocchia : utilizzare il leg drive " +
                "(molleggio o spinta delle gambe) è un errore che trasforma l’esercizio in un Push " +
                "press, un esercizio totalmente differente. ",

                "Cambia la percezione dell’esercizio, l’attivazione muscolare e la modulazione dei " +
                "carichi. ",

                "Tenere i piedi troppo larghi, troppo stretti o storti : la base di appoggio è " +
                "fondamentale per riuscire ad applicare forza nel modo corretto al bilanciere o ai " +
                "manubri. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Lat Machine",
            primaryMuscles = listOf("dorsali", "pettorali", "trapezio", "romboidi", "bicipiti"),
            articleTitle = "Lat Machine: esecuzione corretta e allenamento",
            url = "https://www.projectinvictus.it/lat-machine/",
            execution = listOf(
                "Esecuzione corretta della Lat machine L’esecuzione della Lat machine prevede i " +
                "seguenti step da seguire: Posizionati seduto esattamente al di sotto del cavo. ",

                "Il busto va tenuto sull’attenti, col petto in fuori . ",

                "Schiena inarcata (non iperestesa), che va eventualmente a rilassarsi nella fase " +
                "finale del movimento con le braccia in alto. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Sissy Squat",
            primaryMuscles = listOf("quadricipiti"),
            articleTitle = "Sissy Squat: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/sissy-squat-esecuzione/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Ci sono alcuni errori da evitare nel Sissy squat: “Spezzarsi” con " +
                "la schiena e perdere le curve fisiologiche (che devi cercare di mantenere). ",

                "Caricare troppo potrebbe causarti problematiche articolari: ricorda che è un " +
                "esercizio monoarticolare e che il corpo funziona già da “sovraccarico”. ",

                "Eseguire questo esercizio anche se non si ha la mobilità per farlo. "
            ),
            variants = listOf(
                "Varianti del Sissy Squat Esistono delle varianti del Sissy Squat che ti permettono " +
                "di facilitare l’esercizio : Sissy Squat a terra o Reverse Nordic Curl , che prevede " +
                "di posizionare le ginocchia a terra, alla distanza delle spalle, con le gambe " +
                "perpendicolari a terra e le mani lungo i fianchi. ",

                "Da questa posizione vai indietro senza spezzare la linea della schiena (ginocchia e " +
                "schiena devono rimanere su un’unica linea) e scendi fino a quando i glutei non " +
                "toccano i talloni (se hai la forza e mobilità per farlo). "
            )
        ),
        Exercise(
            name = "Hollow position",
            primaryMuscles = listOf("glutei", "lombari", "addominali"),
            articleTitle = "Hollow position: esecuzione corretta e progressioni",
            url = "https://www.projectinvictus.it/hollow-position/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Good morning per glutei e femorali",
            primaryMuscles = listOf("femorali", "glutei"),
            articleTitle = "Good morning per glutei e femorali",
            url = "https://www.projectinvictus.it/good-morning-esercizio/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nel Good Morning Posizionare il bilanciere troppo in alto sui " +
                "trapezi potrebbe essere davvero rischioso perché tutto il peso va sulla cervicale, " +
                "soprattutto se il carico è molto pesante. ",

                "Perdere le curve fisiologiche della colonna è un errore comune soprattutto per chi " +
                "ha poca mobilità e propriocezione del bacino. ",

                "Flettere troppo o troppo poco le ginocchia : le ginocchia vanno mantenute circa allo " +
                "stesso angolo di flessione durante tutto l’esercizio per mantenere le giuste " +
                "attivazioni muscolari. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Leg extension",
            primaryMuscles = listOf("quadricipiti"),
            articleTitle = "Leg extension: esecuzione corretta e benefici",
            url = "https://www.projectinvictus.it/leg-extension/",
            execution = listOf(
                "Come eseguire la Leg extension Vediamo i passaggi chiave per eseguire correttamente " +
                "la Leg extension: Posiziona il cuscinetto anteriore sulle tibie vicino alla caviglia " +
                "e non a contatto coi piedi. ",

                "Le ginocchia devono posizionarsi al limite della seduta , in modo che il ginocchio " +
                "non sporga eccessivamente dalla stessa ma non sia nemmeno troppo indietro: in questo " +
                "modo crei una leva sicura a livello articolare e anche il lavoro muscolare ne " +
                "beneficia. ",

                "I gradi del movimento vanno decisi in base anche ad eventuali problematiche che " +
                "possiedi. "
            ),
            errors = listOf(
                "Errori da evitare Staccare il ginocchio dalla seduta : ciò compromette la stabilità " +
                "articolare, favorendo l’insorgenza di eventuali problematiche all’articolazione del " +
                "ginocchio. ",

                "“Palleggiare” col cuscinetto anteriore durante l’esecuzione dell’esercizio: è un " +
                "atteggiamento altamente lesivo nel tempo per le ginocchia. ",

                "Settarsi troppo avanti con la seduta (come nella foto sottostante a sinistra) crea " +
                "una leva pericolosa sulle ginocchia. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Chin up",
            primaryMuscles = listOf("dorsali", "spalle", "tricipiti", "trapezio", "romboidi"),
            articleTitle = "Chin up: esecuzione corretta e scheda",
            url = "https://www.projectinvictus.it/chin-up/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nei Chin up Non superare la sbarra con il mento, rendendo la " +
                "trazione incompleta. ",

                "Non avvicinare le scapole alla fine del movimento, con le spalle che tendono ad " +
                "avvicinarsi alle orecchie e ad andare in avanti, che succede spesso se la postura " +
                "iniziale non è corretta. ",

                "Portare le gambe in avanti rispetto al busto e alla sbarra. "
            ),
            variants = listOf(
                "Varianti di Chin up e progressioni Come tutti gli esercizi, anche i Chin up " +
                "prevedono delle varianti e delle progressioni da eseguire, da utilizzare in base al " +
                "tuo obbiettivo come il correggere un preciso errore esecutivo. ",

                "Chin up mezzo ROM : l’esecuzione a mezzo ROM (Range Of Motion) prevede di fermarsi a " +
                "metà durante la salita, senza arrivare con il mento sopra la sbarra, ma fermandosi " +
                "quando la fronte arriva a livello della sbarra. "
            )
        ),
        Exercise(
            name = "Curl manubri panca inclinata",
            primaryMuscles = listOf("bicipiti"),
            articleTitle = "Curl manubri panca inclinata",
            url = "https://www.projectinvictus.it/curl-manubri-panca-inclinata/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nel Curl con manubri su panca inclinata Sono due gli errori più " +
                "comuni nell’esecuzione del Curl su panca. ",

                "Il primo è quello di portare i gomiti eccessivamente avanti durante il sollevamento " +
                "dei manubri . ",

                "Questo è un compenso che subentra con un carico eccessivo o con l’affaticamento. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Step up",
            primaryMuscles = listOf("glutei", "quadricipiti", "femorali"),
            articleTitle = "Step up: esecuzione, scheda e varianti",
            url = "https://www.projectinvictus.it/step-up-esecuzione-corretta/",
            execution = emptyList(),
            errors = listOf(
                "Errori comuni nello step up Nonostante il pattern motorio relativamente semplice, lo " +
                "Step up presenta alcuni errori che sarebbe meglio evitare per una corretta " +
                "attivazione dei glutei e la riduzione del rischio di infortuni . ",

                "Quando esegui una salita sullo step fai attenzione a: Mantenere costante la velocità " +
                "e il ritmo di esecuzione , per avere una distribuzione uniforme dello sforzo ed " +
                "evitare di arrivare a fine serie con eccessivo affaticamento o di lavorare in modo " +
                "differente tra destra e sinistra. ",

                "Evitare di avere troppa fretta a portare la gamba posteriore sul gradino: è " +
                "importante completare il movimento a carico di quella anteriore . "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni alla sbarra",
            primaryMuscles = listOf("pettorali", "dorsali", "spalle", "bicipiti", "tricipiti"),
            articleTitle = "Trazioni alla sbarra: esecuzione corretta e scheda",
            url = "https://www.projectinvictus.it/trazioni-alla-sbarra/",
            execution = listOf(
                "Le Trazioni alla sbarra sono un esercizio complesso che richiede un attento s tudio " +
                "durante tutto il percorso di allenamento man mano che progredisci. ",

                "Per iniziare appenditi alla sbarra mettendo le mani ad una distanza pari a circa " +
                "larghezza delle spalle con il pollice sopra alla sbarra. ",

                "Una volta staccati i piedi tira rivolgendo il petto verso l’alto e arrivando fino a " +
                "mento sopra alla sbarra. "
            ),
            errors = listOf(
                "Elevare le spalle durante la tirata, ingaggiando i flessori del gomito. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Esercizio French press",
            primaryMuscles = listOf("tricipiti"),
            articleTitle = "Esercizio French press: guida completa",
            url = "https://www.projectinvictus.it/french-press-bilanciere/",
            execution = emptyList(),
            errors = listOf(
                "Apertura dei gomiti durante l’esecuzione , che causa un compenso articolare e la " +
                "fuoriuscita da una corretta linea di spinta. ",

                "Flessione di spalla La flessione di spalla trasforma l’esercizio da monoarticolare a " +
                "multiarticolare, in quanto la spalla interviene nel riportare l’omero perpendicolare " +
                "a terra, togliendo carico al tricipite. ",

                "Ciononostante, sfruttare l’ intervento della spalla non è sbagliato dal punto di " +
                "vista anatomo-funzionale quando eseguito con consapevolezza e con escursione " +
                "completa: il tricipite grazie al suo capo lungo è estensore di spalla vista la sua " +
                "inserzione posteriore scapolare. "
            ),
            variants = listOf(
                "Varianti esecutive del French press con bilanciere L’ esercizio French press può " +
                "essere svolto: con bilanciere dritto, con bilanciere sagomato. ",

                "Il bilanciere dritto per ragioni articolari favorisce l’apertura dei gomiti e non " +
                "garantisce i dovuti snodi articolari al gomito a causa della posizione supina. "
            )
        ),
        Exercise(
            name = "Leg curl",
            primaryMuscles = listOf("femorali"),
            articleTitle = "Leg curl: come eseguirla",
            url = "https://www.projectinvictus.it/leg-curl/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Hack Squat",
            primaryMuscles = listOf("quadricipiti", "femorali", "glutei"),
            articleTitle = "Hack Squat: esecuzione corretta",
            url = "https://www.projectinvictus.it/hack-squat/",
            execution = listOf(
                "In tal modo, si porrà maggior enfasi sull’estensione della tibia. ",

                "I talloni devono mantenere sempre il contatto con la pedana. ",

                "Quest’ultima variante determina un maggiore stress sul ginocchio e, in quanto tale, " +
                "andrà gestita. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni orizzontali",
            primaryMuscles = listOf("dorsali", "spalle", "tricipiti", "trapezio", "romboidi"),
            articleTitle = "Trazioni orizzontali: come si eseguono?",
            url = "https://www.projectinvictus.it/trazioni-orizzontali-australian-pull-up/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Squat al multipower",
            primaryMuscles = listOf("quadricipiti"),
            articleTitle = "Squat al multipower: esecuzione, fa male, allenamento",
            url = "https://www.projectinvictus.it/squat-multipower/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Alzate laterali",
            primaryMuscles = listOf("spalle", "trapezio", "glutei"),
            articleTitle = "Alzate laterali: esecuzione corretta, varianti e muscoli coinvolti",
            url = "https://www.projectinvictus.it/alzate-laterali/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Varianti esecutive delle alzate laterali Inclinate : con la variante con busto " +
                "inclinato dalla parte opposta rispetto alla spalla che lavoro oppure con il busto " +
                "inclinato dalla parte della spalla che lavora. ",

                "Isometriche : puoi usarle come pre-attivazione eseguendo 2-3 serie submassimali " +
                "mantenendo il carico nel punto in cui percepiamo al massimo il deltoide mediale " +
                "attivo; oppure ad esaurimento. "
            )
        ),
        Exercise(
            name = "Pulley basso",
            primaryMuscles = listOf("dorsali", "trapezio", "romboidi", "spalle"),
            articleTitle = "Pulley basso: tutorial ed esecuzione corretta",
            url = "https://www.projectinvictus.it/pulley-basso/",
            execution = listOf(
                "L’esecuzione corretta del Pulley prevede una partenza da seduti ma con le gambe in " +
                "avanti, i piedi appoggiati ai supporti e il tronco perpendicolare al terreno. ",

                "Il consiglio generale, per la stragrande maggioranza dei soggetti in ambito fitness, " +
                "è quello di mantenere la lordosi in posizione neutra durante l’esercizio, evitando " +
                "di inclinare il tronco in avanti come spesso veniva insegnato in passato. ",

                "Ciò garantisce una buona stabilità vertebrale e la concomitante co-contrazione di " +
                "muscoli fondamentali come trasverso dell’addome e multifido, muscoli spesso deboli " +
                "nei sedentari e in chi soffre di dolori alla schiena ricorrenti. "
            ),
            errors = emptyList(),
            variants = listOf(
                "Varianti esecutive del Pulley basso Ci sono diverse varianti di questo esercizio, " +
                "oltre a qualche esempio trovi anche questo articolo di approfondimento su una " +
                "variante interessante del Pulley. ",

                "Pulley con maniglie: ad una maniglia, prevede un lavoro unilaterale ed evita " +
                "qualsiasi tipo di asimmetria di contrazione. "
            )
        ),
        Exercise(
            name = "Floor press",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti"),
            articleTitle = "Floor press: esecuzione, muscoli coinvolti, scheda",
            url = "https://www.projectinvictus.it/floor-press/",
            execution = listOf(
                "Esecuzione corretta Floor Press Innanzitutto imposta un ottimo set-up scapolare : " +
                "spalle lontane dalle orecchie (depresse) e scapole leggermente addotte. ",

                "Questi due movimenti genereranno un leggero tilt posteriore della spalla (la spalla " +
                "ruota in senso antiorario in visione laterale). ",

                "Il mantenimento di queste posizioni durante l’esecuzione dell’esercizio ti assicura " +
                "una corretta stabilizzazione articolare e un corretto ingaggio della muscolatura . "
            ),
            errors = listOf(
                "Errori da evitare nel Floor Press L’errore più comune da evitare è perdere il " +
                "corretto setup scapolare . ",

                "Floor press setup corretto e scorretto Un altro errore abbastanza comune che si può " +
                "notare è utilizzare un carico troppo elevato per le capacità dell’atleta, " +
                "giustificandolo con il ROM ridotto dell’esercizio. ",

                "Ricordati sempre che il tuo focus principale deve essere quello di muoverti " +
                "correttamente e i parametri allenanti vanno modulati soprattutto in base a questo " +
                "fattore. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Renegade row",
            primaryMuscles = listOf("dorsali"),
            articleTitle = "Renegade row: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/renegade-row/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Arnold Press",
            primaryMuscles = listOf("spalle", "tricipiti", "trapezio"),
            articleTitle = "Arnold Press: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/arnold-press-esecuzione/",
            execution = listOf(
                "Esecuzione corretta L’esecuzione dell’Arnold Press si divide in due fasi : Prima " +
                "fase : distensione con manubri sopra la testa, per cui valgono le stesse " +
                "interpretazioni del ritmo scapolo-omerale in termini di reclutamento dei muscoli " +
                "deltoide e trapezio. ",

                "Seconda fase : conclusa la distensione, riporta i manubri al punto di partenza e " +
                "portali davanti agli occhi per poi spingerli ancora verso l’alto di pochi " +
                "centimetri. ",

                "La seconda fase è quella che differenzia l’Arnold Press da una distensione classica " +
                "sopra la testa, per questo motivo è bene parlarne in maniera più approfondita. "
            ),
            errors = listOf(
                "Errori da evitare nell’Arnold press Gli errori da evitare durante l’esecuzione di " +
                "questo esercizio sono gli stessi da evitare durante l’esecuzione delle distensioni " +
                "coi manubri sopra la testa: 1. ",

                "Non impostare un corretto set-up articolare : le spalle anteposte e alte, vicino " +
                "alle orecchie in fase di partenza, rendono l’esercizio molto insicuro dal punto di " +
                "vista articolare soprattutto se eseguito con carichi alti. ",

                "Portare i gomiti troppo verso l’esterno e indietro , quando porti i manubri sopra la " +
                "testa come in una distensione (prima fase), non permette un corretto allineamento " +
                "articolare tra testa dell’omero e cavità glenoidea, esponendoti a problematiche " +
                "articolari. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Kick back",
            primaryMuscles = listOf("tricipiti", "spalle"),
            articleTitle = "Kick back: come si esegue e cosa allena",
            url = "https://www.projectinvictus.it/kick-back/",
            execution = listOf(
                "Appoggia un ginocchio e una mano (dallo stesso lato) su una panca, in modo da avere " +
                "il busto quasi parallelo a terra e mantenendo le curve fisiologiche della schiena. ",

                "Estendi il gomito , tenendo ferme le braccia il più possibile, per selezionare " +
                "solamente la contrazione del tricipite. ",

                "Una volta arrivato in massimo accorciamento il consiglio è quello di fare un mini " +
                "fermo per assicurarti di essere arrivato completamente in blocco articolare del " +
                "gomito. "
            ),
            errors = listOf(
                "Errori da evitare nel Kick back Ci sono parecchi possibili errori nel Kick back che " +
                "possono rendere vano l’esercizio: Non tenere il braccio lungo il corpo : mantenere " +
                "le braccia lungo i fianchi è dispendioso, essendo contro gravità. ",

                "Di conseguenza chi ha poca mobilità e poca forza nel deltoide posteriore potrebbe " +
                "far fatica a raggiungere la posizione ottimale di partenza. ",

                "Non stendere i gomiti : arrivare in blocco articolare è una parte importante " +
                "dell’esercizio che permette di contrarre al massimo il tricipite. "
            ),
            variants = listOf(
                "Eseguito in questo modo rende molto più agevole il mantenimento delle posizioni e " +
                "permette di avere il massimo focus sul gesto. ",

                "Con il cavo , così puoi creare una tensione costante anche a fine della fase " +
                "eccentrica (quando l’avambraccio è perpendicolare a terra). "
            )
        ),
        Exercise(
            name = "Hip Thrust con bilanciere",
            primaryMuscles = listOf("glutei", "femorali", "quadricipiti"),
            articleTitle = "Hip Thrust con bilanciere: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/hip-thrust-tecnica-ed-accorgimenti/",
            execution = emptyList(),
            errors = listOf(
                "Direzione della spinta L’ errore più comune nell’esecuzione di Hip Thrust è di " +
                "spingere verso dietro anziché verso l’alto, coinvolgendo eccessivamente gli " +
                "estensori del ginocchio (quadricipite) anziché gli estensori dell’anca (grande " +
                "gluteo). ",

                "Spesso, quando il reclutamento del quadricipite diventa eccessivo, la panca sulla " +
                "quale siamo appoggiati tende a “scivolare” indietro e/o i piedi possono slittare in " +
                "avanti. ",

                "Per evitare di commettere questo errore è sufficiente immaginare un “filo " +
                "invisibile” che tira il bacino verso il soffitto. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Tirate al mento",
            primaryMuscles = listOf("spalle", "trapezio"),
            articleTitle = "Tirate al mento: esecuzione corretta e scheda",
            url = "https://www.projectinvictus.it/tirate-al-mento/",
            execution = listOf(
                "Esecuzione corretta Le Tirate al mento si eseguono principalmente con un bilanciere " +
                "EZ e gli step esecutivi da seguire sono i seguenti: 1. ",

                "Impugna il bilanciere con una presa stretta , in modo che le mani si tocchino oppure " +
                "con una presa leggermente più larga (come nella foto sottostante). ",

                "Stai in piedi , quindi tieni l’ addome contratto e non inarcare mai la schiena . "
            ),
            errors = listOf(
                "Errori da evitare nelle Tirate al mento Caricare troppo peso sul bilanciere (o sui " +
                "manubri). ",

                "Portare in avanti il bilanciere e staccarlo dal corpo: questo genererà una leva " +
                "sulla spalla con omero in intrarotazione che non gioverà per nulla alla salute di " +
                "questa articolazione. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Rematore con manubrio",
            primaryMuscles = listOf("dorsali", "trapezio", "romboidi", "bicipiti"),
            articleTitle = "Rematore con manubrio: esecuzione, errori, scheda",
            url = "https://www.projectinvictus.it/rematore-manubrio-esecuzione-ed-errori-comuni/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Ponte per i glutei",
            primaryMuscles = listOf("femorali", "glutei", "lombari", "addominali"),
            articleTitle = "Ponte per i glutei: tutorial completo",
            url = "https://www.projectinvictus.it/ponte-glutei-esecuzione-corretta/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Nonostante la sua apparente semplicità è possibile effettuare " +
                "degli errori. ",

                "I più comuni sono: Inarcare la schiena , che porta ad iniziare il movimento " +
                "sollevando prima la gabbia toracica, non permettendo una completa estensione " +
                "dell’anca e ciò causerà una minima attivazione dei glutei. ",

                "Per evitarlo è utile contrarre la muscolatura addominale durante l’intera " +
                "esecuzione. "
            ),
            variants = listOf(
                "Varianti esecutive del ponte glutei Come per ogni esecuzione, c’è una fase in cui ti " +
                "abitui allo sforzo percepito: di settimana in settimana, la difficoltà percepita " +
                "sarà minore e anche i benefici diminuiranno. ",

                "Entrano in gioco le possibili varianti del Ponte per i Glutei che creeranno una " +
                "progressione della difficoltà incrementando di conseguenza i benefici primari " +
                "dell’esercizio. "
            )
        ),
        Exercise(
            name = "Lento avanti con manubri",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "trapezio"),
            articleTitle = "Lento avanti con manubri: esecuzione corretta, varianti e scheda",
            url = "https://www.projectinvictus.it/lento-manubri/",
            execution = listOf(
                "Come eseguire il lento avanti con manubri Dalla letteratura scientifica sappiamo che " +
                "per stimolare al meglio i principali muscoli coinvolti occorre lavorare su un ROM di " +
                "movimento completo. ",

                "La corretta esecuzione prevede, quindi, un movimento di abduzione completa di omero " +
                "fino a 180° per stimolare al meglio il deltoide , muscolo che si attiva soprattutto " +
                "nei primi 90° di escursione articolare. ",

                "Oltre ai 90° di abduzione, gran parte del lavoro è attribuito al muscolo trapezio , " +
                "quindi partire dai 90° di abduzione risulta poco efficace . "
            ),
            errors = listOf(
                "Errori da evitare nel lento avanti Il lento con manubri, specie da seduti, è un " +
                "esercizio abbastanza sicuro . ",

                "Ciò non toglie che rispettare la corretta esecuzione diminuirà il rischio infortuni. ",

                "S e mantieni la fisiologica lordosi lombare ed eviti l’iperestensione del tratto " +
                "cervicale, abbassi di molto la probabilità di accusare una lombalgia o una " +
                "cervicalgia dovuta all’esercizio, specie nell’esecuzione da in piedi. "
            ),
            variants = listOf(
                "Varianti di lento avanti con manubri Se consideriamo che il lento con manubri viene " +
                "svolto principalmente da seduti, la variante più classica è il Lento avanti in piedi " +
                ", svolto con manubri o con bilanciere. ",

                "Un’altra variante è l’ Arnold press che prevede, oltre al movimento di spinta con " +
                "manubri, un movimento successivo di flessione di spalla a gomito flesso così da " +
                "mantenere una tensione continua sul deltoide anteriore. "
            )
        ),
        Exercise(
            name = "Push down",
            primaryMuscles = listOf("tricipiti", "addominali"),
            articleTitle = "Push down: esecuzione corretta ed errori da evitare",
            url = "https://www.projectinvictus.it/push-down-tricipiti/",
            execution = emptyList(),
            errors = listOf(
                "Errori comuni da evitare nel Push down Partire con le spalle vicine alle orecchie " +
                "non garantisce stabilità al movimento e alla spalla in generale. ",

                "Partire col gomito già mezzo in estensione non permette il lavoro lungo il ROM più " +
                "adatto e proficuo per stimolare correttamente il tricipite. ",

                "Spostare il gomito in avanti o indietro mentre si esegue l’esercizio. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Handstand",
            primaryMuscles = listOf("glutei", "addominali", "lombari"),
            articleTitle = "Handstand: progressioni per riuscire a farla",
            url = "https://www.projectinvictus.it/handstand-progressioni-per-riuscire-a-farla/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nell’Handstand I primi due errori più comuni sono: Allenare la " +
                "verticale senza una buona mobilità scapolo omerale. ",

                "Bruciare le tappe ricercando solamente la messa in equilibrio senza studiare la " +
                "forma. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Dip per allenare i tricipiti",
            primaryMuscles = listOf("tricipiti", "pettorali"),
            articleTitle = "Dip per allenare i tricipiti",
            url = "https://www.projectinvictus.it/dip-tricipiti/",
            execution = listOf(
                "Come eseguire i Dip per allenare i tricipiti Distinguiamo due fasi di uguale " +
                "importanza: set-up e discesa. ",

                "Nell set-up posizionati in modo da essere più alto rispetto alle parallele e " +
                "impugnale utilizzandole per portare in depressione e adduzione le spalle . ",

                "Le scapole non sono mai totalmente addotte: questo ti porterebbe poi durante la " +
                "discesa a perdere tensione sulla spalla, con un eccessivo allungamento dei muscoli. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni presa neutra",
            primaryMuscles = listOf("pettorali", "dorsali", "spalle", "bicipiti", "tricipiti"),
            articleTitle = "Trazioni presa neutra: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/trazioni-presa-neutra/",
            execution = listOf(
                "Le trazioni a presa neutra si eseguono su una coppia di sbarre parallele in modo da " +
                "avere sempre i pollici rivolti verso il volto, con una presa definita a martello. ",

                "Una volta appeso alla sbarra , tira facendo partire il movimento dai gomiti, " +
                "deprimendo le spalle e salendo piegando il più possibile i gomiti. ",

                "Dato che le sbarre non sono parallele, non avrai un punto di riferimento e di " +
                "conseguenza dovrai tirare fino a portare i gomiti completamente lungo i fianchi: la " +
                "testa dovrà spuntare completamente dalle sbarre. "
            ),
            errors = listOf(
                "Tuttavia, ricordati che nella trazione l’intervento delle braccia deve crescere in " +
                "modo esponenziale mano a mano che tiri. ",

                "Presa troppo stretta o troppo larga : trovare la presa giusta per tue leve ti " +
                "permette di percepire molto più fluido e comodo il movimento. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Dip tra panche",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti"),
            articleTitle = "Dip tra panche: esecuzione corretta",
            url = "https://www.projectinvictus.it/dip-tra-panche/",
            execution = emptyList(),
            errors = listOf(
                "Partire con le spalle alte e in avanti : partire con le spalle attaccate alle " +
                "orecchie e ruotate in avanti causa un pre-accorciamento del muscolo pettorale che " +
                "non riesce ad esprimere forza in discesa. ",

                "Posizionare i piedi troppo in alto o troppo in basso : se hai i piedi troppo in alto " +
                "riduci il ROM (range of movement) e fai meno lavoro muscolare. ",

                "Al contrario, con i piedi troppo in basso è più difficile mantenere le giuste " +
                "attivazioni a livello delle spalle dato che avrai molto peso appoggiato sui piedi. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Handstand push up",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "trapezio"),
            articleTitle = "Handstand push up: progressione ed esecuzione corretta",
            url = "https://www.projectinvictus.it/handstand-push-up/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare I principali errori da evitare in un Handstand push up sono: " +
                "Ridurre il ROM : a volte, presi dalla paura di non salire o dalla fretta di " +
                "concludere l’esercizio, si vedono persone che non arrivano a sfiorare con la fronte " +
                "il pavimento. ",

                "Chiaramente questo è un errore dato che come parametri di riferimento per dichiarare " +
                "valida una ripetizione, si tiene il gomito teso e il raggiungimento del pavimento " +
                "con la testa. ",

                "Non stendere i gomiti : errore collegato al punto precedente, che oltre a non dare " +
                "un parametro di riferimento non permette di mantenerti in una posizione di " +
                "sicurezza. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni a presa stretta",
            primaryMuscles = listOf("trapezio", "romboidi"),
            articleTitle = "Trazioni a presa stretta: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/trazioni-strette/",
            execution = listOf(
                "Per trazione stretta si intende una trazione con le mani che hanno una distanza da " +
                "0.8 volte a 1.2 volte le spalle . ",

                "Per trazioni larghe si intende una presa che va da 1.2 a 1.8 volte le spalle . ",

                "Non sono quindi prese in considerazione in questa trattazione, quelle trazioni con " +
                "le mani praticamente unite che venivano denominate ”strette”. "
            ),
            errors = listOf(
                "Stringere troppo la presa Il range va da 0.8 ad 1.2 volte le spalle, questo perché " +
                "non tutti hanno le stesse leve e di conseguenza ci si potrebbe trovare meglio con " +
                "una certa larghezza piuttosto che un’altra. ",

                "Una presa troppo stretta per una persona con leve inadatte, porta ad un’elevazione " +
                "prematura delle spalle con un conseguente arrivo a mento sopra passivo. ",

                "Si arriva a chiudere la ripetizione con poca tensione sul dorso e con le spalle " +
                "molto intraruotate, fattori da evitare per migliorare nel lungo periodo. "
            ),
            variants = listOf(
                "Questo accade perchè in presa supina, l’atleta principiante non riesce ad ingaggiare " +
                "correttamente i muscoli target, scaricando la maggior parte delle tensioni sui " +
                "flessori di gomito . ",

                "Questi muscoli, come bicipite brachiale, brachioradiale e brachiale sono messi nelle " +
                "migliori condizioni per esprimere forza grazie alla supinazione. "
            )
        ),
        Exercise(
            name = "Muscoli coinvolti nei Push up",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti"),
            articleTitle = "Muscoli coinvolti nei Push up",
            url = "https://www.projectinvictus.it/push-up-muscoli-coinvolti/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Estensioni tricipiti",
            primaryMuscles = listOf("tricipiti"),
            articleTitle = "Estensioni tricipiti: French press e altri esercizi",
            url = "https://www.projectinvictus.it/estensioni-tricipiti/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Analizziamo ora le principali varianti di estensioni per i tricipiti con i manubri, " +
                "oltre al French press visto sopra. ",

                "Kick back Il Kick back è un esercizio in cui i tricipiti cominciano a lavorare in " +
                "pre-allungamento, con una gran tensione isometrica al capo lungo. "
            )
        ),
        Exercise(
            name = "Chest press",
            primaryMuscles = listOf("pettorali", "dorsali", "spalle", "tricipiti"),
            articleTitle = "Chest press: come si esegue, varianti, scheda",
            url = "https://www.projectinvictus.it/chest-press/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nella Chest press Evita di tenere i gomiti alti (foto a sinistra) " +
                "e di anteporre le spalle quando spingi (foto a destra). ",

                "Tenere i gomiti alti è un errore da evitare . ",

                "Tecnicamente significa eseguire l’esercizio con l’omero a 90° o anche più di " +
                "abduzione. "
            ),
            variants = listOf(
                "Chest press orizzontale Garantisce un movimento su piano ibrido " +
                "sagittale/trasversale simile a quello di una Panca Piana classica 2. ",

                "Chest press verticale Avvicina i gomiti ai fianchi e sposta il movimento più verso " +
                "un piano sagittale favorendo maggiormente l’attivazione del petto alto. "
            )
        ),
        Exercise(
            name = "Spinte in alto con bilanciere",
            primaryMuscles = listOf("addominali", "pettorali", "spalle", "tricipiti"),
            articleTitle = "Spinte in alto con bilanciere: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/spinte-in-alto/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nelle Spinte in alto Ci sono alcuni errori da evitare: Sollevare " +
                "il bilanciere da terra quando devi spostare tanti chili, da evitare perché " +
                "difficilmente troveremo il setup giusto e rischi di farti male nel tentativo di " +
                "portarli sui fasci alti del petto. ",

                "Non posizionarsi correttamente sotto il rack per staccare il bilanciere: il " +
                "posizionamento è veramente importante se si vogliono gestire tanti chili e se si " +
                "vuole iniziare l’esercizio nel modo più corretto. ",

                "Non stendere le ginocchia nel tentativo di “salvare” la schiena, cercando magari di " +
                "appiattirla in zona lombare . "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Pulley",
            primaryMuscles = listOf("dorsali"),
            articleTitle = "Pulley: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/pulley-tutorial-ed-esecuzione/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Pull down",
            primaryMuscles = listOf("dorsali", "spalle", "tricipiti"),
            articleTitle = "Pull down: esecuzione corretta, muscoli coinvolti, scheda",
            url = "https://www.projectinvictus.it/pull-down-come-eseguirlo/",
            execution = listOf(
                "Esecuzione corretta Il Pull down è un esercizio che prevede un’ estensione di spalla " +
                "partendo da una massima flessione sopra alla testa . ",

                "Il movimento deve partire dai gomiti in modo da dare la massima tensione possibile " +
                "al muscolo gran dorsale, Una volta che le spalle sono completamente estese lungo i " +
                "fianchi, ritorna in posizione di partenza con le braccia sopra alla testa, lasciando " +
                "che le scapole tornino in posizione neutra. ",

                "Le scapole non vanno forzate in adduzione e depressione prima di iniziare il " +
                "movimento perché questo porta la muscolatura target in una situazione di " +
                "pre-accorciamento, limitando l’espressione di forza. "
            ),
            errors = listOf(
                "Errori da evitare nel Pull down Non posizionarti contro gravità : il set up del Pull " +
                "down va studiato in base a come sono posizionati i cavi che hai a disposizione. ",

                "Più i cavi sono alti più dovrai stare verticale con il busto, più i cavi sono bassi " +
                "più dovrai stare inclinato in avanti con il busto. ",

                "Muovere i gomiti durante l’esecuzione : i gomiti vanno mantenuti più fissi possibile " +
                "per avere un parametro fisso di valutazione nel tempo, in modo da impostare un " +
                "programma di miglioramento basato sul sovraccarico progressivo, e per non facilitare " +
                "l’esecuzione. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Rematore con bilanciere per i dorsali",
            primaryMuscles = listOf("dorsali", "spalle", "lombari", "trapezio"),
            articleTitle = "Rematore con bilanciere per i dorsali: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/rematore-bilanciere/",
            execution = listOf(
                "Il Rematore con bilanciere va eseguito a busto flesso : più fletti il busto in " +
                "avanti e più ampli il ROM (range di movimento) dell’omero, lavorando di più. ",

                "Al contrario, meno ti fletti e più accorci il ROM, coinvolgendo molto il trapezio " +
                "superiore . ",

                "Sono importanti due punti fondamentali nell’esecuzione del rematore con bilanciere: " +
                "colonna lombare che rispetti le curve fisiologiche (lordosi), petto aperto e spalle " +
                "basse soprattutto in chiusura. "
            ),
            errors = listOf(
                "Errori comuni da evitare nel Rematore Gli errori più comuni sono: flettere poco il " +
                "busto, soprattutto quando carichi molti kg, muovere il busto durante l’esecuzione, " +
                "chiudere il petto ed alzare le spalle, perdere la curva lombare, piega leggermente " +
                "le ginocchia per mantenerla. ",

                "Tutte queste esecuzioni scorrette portano ad aumentare il rischio di infortuni ed a " +
                "ridurre il ROM di lavoro del gran dorsale. ",

                "Un altro accorgimento è non fare il rematore con bilanciere dopo lo stacco da terra " +
                "o esercizi che hanno affaticato i muscoli del rachide, altrimenti rischi di " +
                "sovraccaricare la spina dorsale più che i muscoli target. "
            ),
            variants = listOf(
                "A corpo libero l’esercizio che sostituisce il rematore con bilanciere è il body row " +
                "(australian pull up o trazioni orizzontali ). ",

                "Puoi farle con una sbarra bassa (anche un bilanciere sul rack), un tavolo da cucina " +
                "(scomodo), con gli anelli o il TRX. "
            )
        ),
        Exercise(
            name = "Squat",
            primaryMuscles = listOf("femorali", "glutei", "polpacci", "quadricipiti"),
            articleTitle = "Squat: la guida completa",
            url = "https://www.projectinvictus.it/squat-la-guida-completa/",
            execution = emptyList(),
            errors = listOf(
                "Per maggiori dettagli esecutivi, ti rimandiamo al nostro articolo più approfondito " +
                "sull’ esecuzione dello squat . "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Curl con Manubri",
            primaryMuscles = listOf("bicipiti", "tricipiti"),
            articleTitle = "Curl con Manubri",
            url = "https://www.projectinvictus.it/curl-manubri/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Varianti del Curl con manubri Eseguire il Curl nelle innumerevoli varianti offre due " +
                "grossi vantaggi : Porre maggiore enfasi su un determinato tratto di range of motion " +
                "(ROM); Rendere l’esercizio più funzionale al ruolo che ha nella scheda. ",

                "Possiamo dividere i Curl in due gruppi, a seconda che il bicipite parta in " +
                "pre-accorciamento o pre-allungamento. "
            )
        ),
        Exercise(
            name = "Squat a corpo libero (o Air squat)",
            primaryMuscles = listOf("quadricipiti", "femorali", "glutei", "polpacci"),
            articleTitle = "Squat a corpo libero (o Air squat)",
            url = "https://www.projectinvictus.it/squat-a-corpo-libero/",
            execution = listOf(
                "Focalizzati sulla pressione che il tuo piede esercita a terra, Scendi e inizia la " +
                "discesa, portando le ginocchia leggermente in fuori e cercando di non cambiare la " +
                "pressione sul piede, così come mantenendo le curve fisiologiche della schiena " +
                "Mantieni le ginocchia rivolte verso l’esterno. ",

                "I muscoli che vengono coinvolti in modo secondario sono gli erettori della colonna, " +
                "l’addome e i polpacci. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Curl a martello",
            primaryMuscles = listOf("bicipiti", "avambracci"),
            articleTitle = "Curl a martello: come farlo e che muscoli allena",
            url = "https://www.projectinvictus.it/curl-a-martello/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Oltre all’esecuzione classica dell’ Hammer curl appena analizzata, esiste una " +
                "seconda variante, in cui il manubrio viene portato in linea col braccio, ma davanti " +
                "al tronco . ",

                "L’esercizio è il medesimo a livello del gomito mentre la spalla si sposta e mantiene " +
                "una rotazione interna durante il sollevamento del manubrio, che si ritrova così di " +
                "fronte al torace. "
            )
        ),
        Exercise(
            name = "Leg press in palestra e nel bodybuilding",
            primaryMuscles = listOf("quadricipiti", "femorali", "glutei"),
            articleTitle = "Leg press in palestra e nel bodybuilding: esecuzione corretta",
            url = "https://www.projectinvictus.it/leg-press-bodybuilding/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Varianti esecutive di leg press La Leg press è un esercizio che permette di lavorare " +
                "con ottimi carichi anche quando eseguita in versione monopodalica , che è " +
                "sconsigliata a soggetti principianti. ",

                "In soggetti avanzati potrebbe avere senso in caso di asimmetrie tra gli arti " +
                "inferiori. "
            )
        ),
        Exercise(
            name = "Esercizi petto alto",
            primaryMuscles = listOf("pettorali", "spalle"),
            articleTitle = "Esercizi petto alto: quali sono i migliori",
            url = "https://www.projectinvictus.it/esercizi-petto-alto/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Push up",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti"),
            articleTitle = "Push up: come si fanno, errori e varianti",
            url = "https://www.projectinvictus.it/push-up/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nel Push up Contrarre troppo l’addome : aumenta la stabilità del " +
                "tronco, ma non fa settare correttamente le scapole. ",

                "Contrarre troppo poco l’addome : partire con il bacino in alto riduce la leva e " +
                "quindi il carico percepito durante il piegamento. ",

                "Non settare correttamente le scapole e le spalle : partire con le scapole non " +
                "addotte non mette in sicurezza l’articolazione e riduce l’attivazione dei muscoli " +
                "interessati nell’esercizio. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Stacchi rumeni",
            primaryMuscles = listOf("glutei", "femorali"),
            articleTitle = "Stacchi rumeni: esecuzione, tecnica corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/stacchi-rumeni/",
            execution = emptyList(),
            errors = listOf(
                "Errori comuni negli Stacchi rumeni La difficoltà dello Stacco rumeno è imparare a " +
                "coordinare correttamente ginocchio ed anca, mantenendo il bacino antiverso durante " +
                "tutto il movimento . ",

                "In aggiunta devi controllare anche il bilanciere o i manubri mantenendolo vicino a " +
                "te e a contatto con le tibie al di sotto del ginocchio. ",

                "I possibili errori sono: Chinarsi nella discesa a gambe distese senza controllo. "
            ),
            variants = listOf(
                "Varianti di Stacchi rumeni Stacchi rumeni con il bilanciere : è la variante " +
                "principale descritta nell’articolo. ",

                "Il bilanciere ha il vantaggio di consentire l’utilizzo di grossi carichi in " +
                "stabilità e sicurezza. "
            )
        ),
        Exercise(
            name = "Spider curl",
            primaryMuscles = listOf("bicipiti", "avambracci"),
            articleTitle = "Spider curl: esecuzione corretta",
            url = "https://www.projectinvictus.it/spider-curl/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nello Spider Curl Lo Spider curl è un esercizio che, per la tipica " +
                "posizione di appoggio del torso sulla panca, limita parecchio i compensi " +
                "d’esecuzione – ad es. ",

                "L’ errore più comune dello Spider curl è quello “classico” di tutti i curl, ovvero " +
                "di portare i gomiti indietro , estendendo la spalla. ",

                "Questo “compenso” è dovuto all’insorgenza di cedimento tecnico o, se presente fin da " +
                "subito, all’errata stima (per eccesso) del sovraccarico utilizzato, o ancora alla " +
                "non padronanza tecnico-specifica. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Shoulder press",
            primaryMuscles = listOf("spalle", "bicipiti", "tricipiti", "trapezio"),
            articleTitle = "Shoulder press: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/shoulder-press/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nello Shoulder press Avere una postura scorretta sulla seduta : il " +
                "bacino va mantenuto neutro o leggermente aperto in anteroversione per permettere di " +
                "respirare correttamente. ",

                "Una postura in retroversione graverebbe troppo sulla colonna. ",

                "Spingere in avanti o indietro : la spinta deve essere più verticale possibile. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Squat con manubri",
            primaryMuscles = listOf("quadricipiti"),
            articleTitle = "Squat con manubri: esecuzione, varianti, benefici",
            url = "https://www.projectinvictus.it/squat-con-manubri/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Retrovertere il bacino durante la discesa : il bacino nello squat " +
                "va mantenuto in posizione neutra o leggermente antiversa per garantire un massimo " +
                "allungamento dei muscoli ischiocrurali durante la discesa, in modo che stabilizzino " +
                "il movimento. ",

                "Inoltre, la retroversione del bacino sotto carico, che comporta un movimento della " +
                "colonna vertebrale, è un fattore di rischio da non sottovalutare per la schiena. ",

                "Sculare : il bacino non deve mai salire prima delle spalle. "
            ),
            variants = listOf(
                "Varianti di Squat con manubri I manubri possono essere posizionati in diverso modo " +
                "per eseguire lo Squat: Goblet squat : questa è la variante sopra descritta, in cui " +
                "il manubrio è posto davanti a noi appoggiato allo sterno. ",

                "Goblet squat doppio manubrio : in questa variante i manubri sono due e sono posti " +
                "sopra alle spalle, mantenendo i gomiti alti. "
            )
        ),
        Exercise(
            name = "Pettorali con manubri",
            primaryMuscles = listOf("tricipiti"),
            articleTitle = "Pettorali con manubri: quali esercizi fare",
            url = "https://www.projectinvictus.it/pettorali-con-manubri/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Deltoide posteriore",
            primaryMuscles = listOf("spalle", "trapezio"),
            articleTitle = "Deltoide posteriore: i migliori esercizi",
            url = "https://www.projectinvictus.it/esercizi-deltoide-posteriore/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Squat con elastico",
            primaryMuscles = listOf("quadricipiti"),
            articleTitle = "Squat con elastico: come si fa e a cosa serve",
            url = "https://www.projectinvictus.it/squat-con-elastico/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Gli errori da evitare sono: Perdere le curve della schiena mentre " +
                "si scende o addirittura ancora prima di scendere. ",

                "In fase di spinta portare le ginocchia in dentro : cerca sempre di focalizzarti " +
                "sulla pressione del piede a terra che deve essere omogenea in tutti i suoi punti, " +
                "siano essi laterali, mediali, anteriori o posteriori. ",

                "Ovviamente posizionare male l’elastico rischiando di perderlo durante l’esecuzione. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Goblet squat",
            primaryMuscles = listOf("quadricipiti", "spalle"),
            articleTitle = "Goblet squat: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/goblet-squat/",
            execution = emptyList(),
            errors = listOf(
                "Posizionarsi troppo sull’ avampiede all’inizio. ",

                "Utilizzare un carico troppo elevato , che non riesci a gestire a livello di presa. ",

                "Arrotondare la schiena , soprattutto in zona lombare, durante la fase di discesa o " +
                "addirittura ancora prima. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Alzate posteriori",
            primaryMuscles = listOf("spalle", "trapezio", "romboidi"),
            articleTitle = "Alzate posteriori: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/alzate-posteriori/",
            execution = listOf(
                "Più il busto rimane immobile e più isoli il lavoro sul deltoide posteriore. ",

                "Al contrario, se ti aiuti estendendo il busto togli il lavoro alla zona interessata. ",

                "Le braccia devono rimanere tese o semitese a seconda di come riusciamo a percepire " +
                "meglio il lavoro sulla zona posteriore del deltoide. "
            ),
            errors = listOf(
                "Errori comuni da evitare Tenere il busto meno flesso di 90° e di portare i gomiti " +
                "vicino al busto. ",

                "Questo posta il lavoro dal deltoide posteriore al trapezio o gran dorsale (a seconda " +
                "della linea di lavoro). ",

                "Muovere il busto per aiutarsi perdendo completamente il lavoro sulla spalla " +
                "posteriore. "
            ),
            variants = listOf(
                "Varianti alzate posteriori Vediamo in questi due video come eseguire delle varianti " +
                "di alzate posteriori per i deltoidi posteriori . ",

                "Partenza con petto in appoggio su panca inclinata, manubri verso il terreno, presa " +
                "neutra. "
            )
        ),
        Exercise(
            name = "Pulley alto",
            primaryMuscles = listOf("dorsali", "trapezio", "romboidi"),
            articleTitle = "Pulley alto: esecuzione, errori, muscoli coinvolti",
            url = "https://www.projectinvictus.it/pulley-alto/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nel Pulley alto Gli errori da evitare nel Pulley alto sono: " +
                "Perdere o non avere il corretto setup scapolare : esso ti mantiene salda " +
                "l’articolazione della spalla, prevenendo spiacevoli infortuni e indirizza il lavoro " +
                "nei giusti gruppi muscolari. ",

                "Allargare i gomiti verso l’esterno durante la fase di tirata: in quel modo il gran " +
                "dorsale perderà tensione. ",

                "Inarcare troppo la zona lombare durante la fase di tirata. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Tate press",
            primaryMuscles = listOf("tricipiti"),
            articleTitle = "Tate press: esecuzione, muscoli coinvolti, scheda",
            url = "https://www.projectinvictus.it/tate-press/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nel Tate press Non trovare un set up scapolare adeguato : il set " +
                "up delle spalle è importantissimo per garantire stabilità all’articolazione e per " +
                "salvaguardarla da eventuali stress articolari inutili. ",

                "Fare poco ROM : potrebbe essere che i manubri siano molto grandi con conseguenza di " +
                "un contatto prematuro tra loro durante la discesa. ",

                "Per sopperire a questo problema, basta allargare un po’ i gomiti, quel tanto da " +
                "permettere un range di discesa onorevole. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Back squat",
            primaryMuscles = listOf("quadricipiti", "femorali", "glutei", "polpacci"),
            articleTitle = "Back squat: cos’è, come si fa e cosa allena",
            url = "https://www.projectinvictus.it/back-squat/",
            execution = listOf(
                "Esecuzione corretta Ecco i principali passaggi per eseguire correttamente il Back " +
                "squat in palestra. ",

                "Unrack del bilanciere : posizionati sotto al bilanciere e impugnalo in modo da non " +
                "sentire pressioni anomale sulle spalle. ",

                "Appoggia il bilanciere sui trapezi in modo che non ti dia fastidio alla cervicale. "
            ),
            errors = listOf(
                "Errori da evitare nel Back squat Scendere sul tallone : spostare il baricentro sul " +
                "tallone durante la discesa porta inevitabilmente ad un crollo anteriore del carico, " +
                "che si traduce in uno slittamento del bilanciere in avanti. ",

                "La “sculata” : la “sculata” è un classico compenso di chi non riesce a mantenere un " +
                "punto sul piede ottimale durante tutto il movimento. ",

                "Si porta erroneamente in alto il sedere durante la spinta in fase concentrica. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Pectoral machine",
            primaryMuscles = listOf("pettorali"),
            articleTitle = "Pectoral machine: esecuzione, muscoli coinvolti, scheda",
            url = "https://www.projectinvictus.it/pectoral-machine/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Ecco gli errori da evitare sulla Pectoral machine: Sedersi sulla " +
                "macchina ad un’altezza per la quale hai i gomiti sopra la linea delle spalle , in " +
                "quel modo metterai a rischio l’articolazione della spalla, stai attento a questo " +
                "dettaglio. ",

                "Avere un atteggiamento di chiusura ovvero di anteposizione delle spalle durante " +
                "tutto l’arco di movimento: anche in questo caso metterai a rischio l’articolazione " +
                "della spalla. ",

                "Utilizzare troppo carico e darti lo slancio per la chiusura nella parte finale " +
                "dell’esercizio: non è un esercizio sul quale devi maneggiare carichi importanti! "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Squat sumo",
            primaryMuscles = listOf("glutei"),
            articleTitle = "Squat sumo: esecuzione corretta, varianti, muscoli coinvolti",
            url = "https://www.projectinvictus.it/squat-sumo/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nello Squat sumo Ecco gli errori che non devi fare per minimizzare " +
                "il rischio di infortunio e attivare correttamente i muscoli coinvolti: Non tenere i " +
                "piedi paralleli tra loro ma extraruotati. ",

                "Non mantenere costante la pressione sul piede ma indirizzarla, ad esempio, sul " +
                "tallone. ",

                "Ciò ti porterà a non essere in equilibrio quando inverti il movimento e di " +
                "conseguenza, anche grazie al carico, ti sposterai in avanti, aumentando le " +
                "probabilità di “sculare”, ovvero di salire prima col sedere rispetto alle spalle. "
            ),
            variants = listOf(
                "Varianti Squat sumo Lo Squat Sumo può essere eseguito a corpo libero, con manubri (o " +
                "con kettlebell), col bilanciere libero, al multipower oppure con dei dischi " +
                "attaccati ad una cintura (come quelle che si usano nello streetlifting). ",

                "La variante coi manubri prevede di impugnare un manubrio ad una delle sue estremità " +
                "con tutte e due le mani. "
            )
        ),
        Exercise(
            name = "Air squat",
            primaryMuscles = listOf("quadricipiti"),
            articleTitle = "Air squat: come si fanno e cosa allenano",
            url = "https://www.projectinvictus.it/air-squat/",
            execution = emptyList(),
            errors = listOf(
                "Retrovertere eccessivamente il bacino perdendo la curva lombare. ",

                "“Sculare” alzando prima il sedere e poi le spalle. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Hindu push up",
            primaryMuscles = listOf("pettorali", "spalle"),
            articleTitle = "Hindu push up: esecuzione, muscoli coinvolti, benefici",
            url = "https://www.projectinvictus.it/hindu-push-up/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Spezzare il timing di spinta : durante la discesa il passaggio da " +
                "v-push up a push up deve avvenire come una singola fase. ",

                "Spanciare troppo durante la discesa : non esasperare il concetto di Hindu, " +
                "ricercando un estensione della colonna a fine della fase eccentrica, contorcendosi " +
                "come dei serpenti. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Squat jump",
            primaryMuscles = listOf("quadricipiti"),
            articleTitle = "Squat jump: come si fa, errori da evitare, benefici",
            url = "https://www.projectinvictus.it/squat-jump/",
            execution = listOf(
                "Esecuzione corretta Squat jump fasi Lo Squat Jump è un esercizio che richiede una " +
                "generazione di energia dagli arti inferiori tale per cui ti sollevi da terra. ",

                "Ecco come si esegue: Parti coi piedi leggermente più larghi delle spalle . ",

                "Posizione piedi Il ginocchio parte sempre in estensione (non partire con le " +
                "ginocchia piegate). "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Bench press",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "bicipiti"),
            articleTitle = "Bench press: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/bench-press/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Non impostare correttamente le spalle : se le spalle rimangono " +
                "alte e non depresse durante la panca, ci sono instabilità, un coinvolgimento del " +
                "pettorale non ottimale e un maggior rischio di infortunio. ",

                "Non mantenere il set-up dopo l’un-rack del bilanciere : staccare il bilanciere senza " +
                "perdere il set-up è difficile e per questo spesso è utile avere uno spotter. ",

                "Assicurati anche di posizionare il bilanciere ad un’altezza giusta per le tue " +
                "braccia. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Calf in piedi",
            primaryMuscles = listOf("tricipiti", "polpacci"),
            articleTitle = "Calf in piedi: esecuzione corretta",
            url = "https://www.projectinvictus.it/calf-in-piedi/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nel Calf in piedi Non incurvarti durante l’esecuzione " +
                "dell’esercizio, ma mantieni una postura eretta. ",

                "Non fare velocemente o di “rimbalzo” il Calf in piedi: esegui un movimento " +
                "controllato. ",

                "Non fare l’esercizio in apnea , ma inspira mentre scendi ed espira durante la fase " +
                "di sollevamento. "
            ),
            variants = listOf(
                "Varianti esecutive di Calf in piedi Calf con bilanciere Il Calf con bilanciere è " +
                "consigliata farla utilizzando il multipower e con l’ausilio di un gradino di rialzo " +
                "su cui poggiare i piedi, sufficientemente alto per poter consentire un range di " +
                "movimento adeguato. ",

                "Si sconsiglia l’utilizzo del bilanciere libero, questo perchè vi sarà una forte " +
                "componente di instabilità che condizionerà probabilmente l’efficacia del lavoro sul " +
                "distretto da allenare. "
            )
        ),
        Exercise(
            name = "Bicipiti ai cavi",
            primaryMuscles = listOf("bicipiti"),
            articleTitle = "Bicipiti ai cavi: i migliori esercizi",
            url = "https://www.projectinvictus.it/bicipiti-ai-cavi/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Panca stretta",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti"),
            articleTitle = "Panca stretta: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/panca-stretta-tricipiti/",
            execution = listOf(
                "La soluzione migliore per la presa, sia in termini articolari che muscolari per " +
                "attivare il tricipite, è adottare un presa che ti permetta di arrivare coi gomiti " +
                "perpendicolari a terra quando il bilanciere è al petto . ",

                "In questo modo, il gomito sarà posto in quasi massima flessione e potrà esprimersi " +
                "in estensione e il lavoro sui fasci medi del gran pettorale sarà ridotto (favorendo " +
                "però il lavoro sui fasci claveari). ",

                "Non è possibile focalizzare unicamente il lavoro sui tricipiti su un esercizio che è " +
                "multiarticolare ma è possibile, adottando un set-up corretto, ripartire il lavoro su " +
                "muscoli differenti. "
            ),
            errors = listOf(
                "Errori da evitare Gli errori da evitare nell’esecuzione della Panca stretta sono: " +
                "Non lavorare con un corretto set-up scapolare. ",

                "Caricare troppo peso e non riuscire a mantenere un corretto set-up. ",

                "Adottare una presa troppo stretta (con le mani che quasi si toccano). "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Torsioni del busto",
            primaryMuscles = listOf("addominali"),
            articleTitle = "Torsioni del busto: cosa sono e a cosa servono",
            url = "https://www.projectinvictus.it/torsioni-del-busto/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Panca piana manubri",
            primaryMuscles = listOf("pettorali"),
            articleTitle = "Panca piana manubri: esecuzione corretta",
            url = "https://www.projectinvictus.it/panca-manubri/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nella panca piana manubri Abdurre eccessivamente l’omero , quindi " +
                "portare i gomiti troppo verso l’esterno durante la fase eccentrica: in quel modo il " +
                "petto lavora comunque ma la spalla non è compatta e la salute articolare è " +
                "compromessa. ",

                "Non ottenere e mantenere un corretto setup scapolare durante tutta l’alzata. ",

                "Cercare di appiattire la zona lombare per “preservare la schiena” da eventuali " +
                "infortuni. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Zottman curl",
            primaryMuscles = listOf("bicipiti", "avambracci"),
            articleTitle = "Zottman curl: esecuzione, muscoli coinvolti, allenamento",
            url = "https://www.projectinvictus.it/zottman-curl/",
            execution = listOf(
                "Esegui un curl arrivando in supinazione quando il gomito è flesso al massimo. ",

                "Ruota il manubrio rimanendo in massima flessione di gomito. ",

                "Una volta che hai pronato la presa effettua la negativa, tornando in blocco " +
                "articolare. "
            ),
            errors = listOf(
                "Errori da evitare nello Zottman curl Muovere i gomiti durante l’esecuzione : se i " +
                "gomiti si muovono avanti o indietro significa che stai utilizzando troppo carico e " +
                "che attivi altri muscoli come ad esempio il deltoide anteriore. ",

                "Perdere il set up delle spalle : una posizione con le spalle molto anteposte ed " +
                "intraruotate durante il curl ti espone ad un maggior rischio infortuni al bicipite, " +
                "quindi stai in allerta. ",

                "Molleggiarsi con le ginocchia : le ginocchia devono rimanere in blocco articolare " +
                "per evitare compensi al sopraggiungere della fatica. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Face pull",
            primaryMuscles = listOf("spalle"),
            articleTitle = "Face pull: esecuzione corretta, errori, muscoli coinvolti",
            url = "https://www.projectinvictus.it/face-pull/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare I principali errori da evitare nel Face pull sono: Elevare le " +
                "spalle durante la tirata : questo scarica molta della tensione sui fasci più alti " +
                "del trapezio superiore. ",

                "L’esercizio si presta molto bene all’allenamento dei fasci medi ed inferiori del " +
                "trapezio e di conseguenza il consiglio è di non sprecare i suoi benefici scaricando " +
                "tensione altrove. ",

                "Non arrivare al massimo ROM attivo : portando in massimo stretch i muscoli che " +
                "adducono le scapole, riuscirai a dare molta più tensione a quel distretto muscolare. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Alzate laterali ai cavi",
            primaryMuscles = listOf("spalle"),
            articleTitle = "Alzate laterali ai cavi: esecuzione, muscoli coinvolti, scheda",
            url = "https://www.projectinvictus.it/alzate-laterali-ai-cavi/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nelle Alzate laterali ai cavi Traiettoria errata : la traiettoria " +
                "non deve essere proiettata troppo in avanti in quanto verrebbe data molta più enfasi " +
                "alla contrazione del deltoide anteriore e nemmeno troppo laterale per non andare a " +
                "stressare inutilmente le strutture della spalla. ",

                "La traiettoria di salita assume poi un ruolo ancora più importante in chi ha " +
                "problematiche tipo spalle anteposte ed intraruotate, dove un’errata traiettoria " +
                "potrebbe dare stress a strutture come il sovraspinato o il capoluogo del bicipite. ",

                "Rispetta sempre il piano scapolare durante l’abduzione dell’omero Gomiti troppo " +
                "piegati : una flessione di gomito pronunciata anche fino a 90° porta un " +
                "miglioramento effettivo solo nell’ego del praticante dato che potrà sollevare molti " +
                "più kili rispetto al normale. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Maniglie per flessioni",
            primaryMuscles = listOf("pettorali", "spalle"),
            articleTitle = "Maniglie per flessioni: perché usarle ed esercizi",
            url = "https://www.projectinvictus.it/maniglie-per-flessioni/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Pullover palestra",
            primaryMuscles = listOf("pettorali", "dorsali"),
            articleTitle = "Pullover palestra: tutto quello che devi sapere",
            url = "https://www.projectinvictus.it/pullover-palestra/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Pullover manubrio",
            primaryMuscles = listOf("tricipiti", "pettorali", "dorsali"),
            articleTitle = "Pullover manubrio: esecuzione, muscoli coinvolti, scheda",
            url = "https://www.projectinvictus.it/pullover-manubrio/",
            execution = listOf(
                "Sdraiati sulla panca piana col manubrio in mano. ",

                "Imposta il corretto set-up scapolare (spalle lontane dalle orecchie, scapole addotte " +
                "e petto in fuori). ",

                "I piedi sono sempre appoggiati a terra in quanto aiutano a mantenere stabilità sul " +
                "tronco. "
            ),
            errors = listOf(
                "Errori da evitare nel Pullover manubrio Portare le spalle in anteposizione e perdere " +
                "il set-up scapolare: ciò renderà instabile l’articolazione della spalla. ",

                "Utilizzare troppo carico : ciò ti porterà a piegare (flettere) il gomito per " +
                "compensare. ",

                "Portare il manubrio troppo verso i piedi è inutile per questioni di tensione " +
                "muscolare: come detto prima fermati ad altezza occhi. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Esercizi per le braccia senza pesi",
            primaryMuscles = listOf("bicipiti", "avambracci", "tricipiti", "pettorali", "spalle"),
            articleTitle = "Esercizi per le braccia senza pesi",
            url = "https://www.projectinvictus.it/esercizi-braccia-senza-pesi/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Landmine press",
            primaryMuscles = listOf("pettorali", "spalle"),
            articleTitle = "Landmine press: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/landmine-press/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Gli errori più comuni e da evitare in questo esercizio sono: " +
                "Mancato ingaggio corretto dei muscoli del core che causa l’inclinazione laterale e " +
                "la rotazione del busto. ",

                "Landmine Press inclinazione laterale e rotazione busto Mancato corretto set-up " +
                "scapolare : esso serve per rendere sicuro l’esercizio e per permettere ai muscoli " +
                "target di lavorare correttamente. ",

                "Se non mantieni il corretto setup l’esercizio perde di efficacia. "
            ),
            variants = listOf(
                "Alternative al Landmine press Per trovare alternative al Landmine Press devi " +
                "ragionare sulle peculiarità di questo esercizio: Attiva tantissimo il core, Lavora " +
                "monoarto, Lavora i muscoli della spalla come muscoli motori principali in un piano " +
                "di spinta non completo. ",

                "L’alternativa potrebbe quindi essere svolgere un lento avanti monoarto con manubrio " +
                "da in piedi, per cercare di includere tutte le condizioni nell’elenco esposto prima. "
            )
        ),
        Exercise(
            name = "Panca inclinata",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "bicipiti"),
            articleTitle = "Panca inclinata: esecuzione, muscoli coinvolti, scheda",
            url = "https://www.projectinvictus.it/panca-inclinata/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nella Panca inclinata I principali errori da evitare durante " +
                "l’esecuzione della Panca inclinata sono: Non inclinare abbastanza la panca : se vuoi " +
                "utilizzare la stessa presa che utilizzi anche in panca piana per la panca inclinata, " +
                "devi assolutamente inclinarla almeno a 45°. ",

                "Inclinare poco la panca (meno di 45°) è un errore da evitare. ",

                "Utilizzare una presa troppo larga col bilanciere : limita la componente di " +
                "flesso-estensione del movimento, riducendo l’attivazione dei fasci alti del " +
                "pettorale. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Front squat",
            primaryMuscles = listOf("dorsali", "quadricipiti"),
            articleTitle = "Front squat: come si fa e a cosa serve",
            url = "https://www.projectinvictus.it/front-squat/",
            execution = listOf(
                "Esecuzione corretta Per fare correttamente il Front Squat è importante impostarsi " +
                "correttamente fin da subito: Posiziona il bilanciere sul rack, ad altezza spalle. ",

                "Vai sotto al bilanciere, avendo cura di posizionarlo sul deltoide anteriore e " +
                "impugnalo portando i gomiti in avanti , afferrando il bilanciere con i palmi delle " +
                "mani rivolti verso l’alto. ",

                "Posizione di partenza del Front squat: bilanciere appoggiato sul deltoide anteriore " +
                "e gomiti in avanti. "
            ),
            errors = listOf(
                "Errori comuni da evitare nel Front squat Posizionarsi male sotto al bilanciere, " +
                "appoggiandolo ad esempio sul collo o troppo distante dalla spalla . ",

                "Se ti fanno male i polsi, non costringerti ad una presa che metta troppo in " +
                "estensione i polsi ma opta per l’utilizzo delle fascette o per la presa in cui le " +
                "braccia sono incrociate. ",

                "Front Squat presa facilitata con fascette Caricare troppo se non hai troppa " +
                "confidenza con questo esercizio. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Pendlay row",
            primaryMuscles = listOf("dorsali", "spalle", "bicipiti", "lombari", "trapezio"),
            articleTitle = "Pendlay row: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/pendlay-row/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare Dovresti assolutamente evitare di: Verticalizzare il busto durante " +
                "la tirata: l’esercizio perderebbe di efficacia sui muscoli target. ",

                "Fare questo esercizio se non riesci a mantenere un corretto set-up della zona " +
                "lombare. ",

                "Pendlay Row setup scorretto e corretto Caricare troppo rispetto al tuo potenziale. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Lat Machine presa inversa",
            primaryMuscles = listOf("bicipiti", "avambracci"),
            articleTitle = "Lat Machine presa inversa: come si fa e cosa allena",
            url = "https://www.projectinvictus.it/lat-machine-presa-inversa/",
            execution = listOf(
                "Come eseguire la Lat machine presa inversa L’esecuzione della Lat machine prevede di " +
                "posizionati seduto sotto al cavo, con il busto tenuto sull’attenti, col petto in " +
                "fuori . ",

                "La presa è supina (inversa) : i palmi delle mani sono rivolte verso di te, mentre la " +
                "schiena è inarcata (non iperestesa) e va eventualmente a rilassarsi nella fase " +
                "finale del movimento con le braccia in alto. ",

                "Le scapole possono essere sia tenute sempre depresse oppure quando le braccia si " +
                "distendono elevarle e deprimerle prima di tirare la sbarra verso di te. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Stacchi a gambe tese per glutei di marmo",
            primaryMuscles = listOf("polpacci", "romboidi", "femorali", "glutei"),
            articleTitle = "Stacchi a gambe tese per glutei di marmo",
            url = "https://www.projectinvictus.it/stacchi-gambe-tese/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Varianti di stacchi a gambe tese Stacchi da terra a gambe tese con il bilanciere : " +
                "utilizza delle fascette per la presa qualora risulti un fattore limitante che porta " +
                "a fermarsi per via del fatto che ti “scivola” il bilanciere dalle mani. ",

                "Stacchi da terra a gambe tese con manubri e kettlebell : si adattano bene in genere " +
                "a un rep range indicativo dalle 8 alle 15 ripetizioni. "
            )
        ),
        Exercise(
            name = "Affondi",
            primaryMuscles = listOf("quadricipiti", "glutei"),
            articleTitle = "Affondi: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/affondi/",
            execution = listOf(
                "Focalizzati sulla pressione che il piede esercita a terra mentre stai affondando col " +
                "movimento: se ad esempio senti una pressione predominante all’interno del piede, " +
                "probabilmente hai le ginocchia che stanno collassando verso l’interno. ",

                "Un altro fattore molto importante è la verticalità del busto : cerca sempre di " +
                "mantenere l’ inclinazione del busto in una posizione neutra , né troppo proiettato " +
                "in avanti, né troppo proiettato in verticale. ",

                "Il ginocchio della gamba posteriore (quella che non spinge e non è “attiva”) sfiora " +
                "sempre il terreno. "
            ),
            errors = listOf(
                "Errori comuni da evitare Gli errori da evitare su questo esercizio sono: Non " +
                "appoggiare il peso del corpo su tutto il piede quando si arriva in affondo. ",

                "Ad esempio, hai il tallone alzato da terra oppure la zona mediale del piede " +
                "completamente schiacciata a discapito di quella laterale. ",

                "Fare collassare le ginocchia all’interno durante la discesa e/o durante la salita. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Alzate frontali con manubri",
            primaryMuscles = listOf("spalle", "bicipiti"),
            articleTitle = "Alzate frontali con manubri: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/alzate-frontali/",
            execution = emptyList(),
            errors = listOf(
                "Errori comuni da evitare nelle alzate frontali L’errore comune da evitare " +
                "maggiormente è quello di caricare troppo rispetto al proprio potenziale. ",

                "Questo è un esercizio monoarticolare che crea una leva molto svantaggiosa sui " +
                "muscoli che partecipano al movimento. ",

                "Ciò significa che in presenza di un carico eccessivamente alto, si potrebbero creare " +
                "dei compensi, soprattutto a livello di zona lombare che si iperestenderà per " +
                "facilitare il sollevamento del peso. "
            ),
            variants = listOf(
                "Varianti esecutive alzate frontali Come hai visto, esistono diverse varianti " +
                "esecutive delle alzate frontali, in particolare: Presa neutra : in questa presa hai " +
                "i palmi delle mani rivolti verso il busto nella posizione iniziale e così nella " +
                "posizione finale. ",

                "Presa leggera intrarotazione : in questa presa hai i palmi delle mani rivolti verso " +
                "il basso quando arrivi nella posizione finale. "
            )
        ),
        Exercise(
            name = "Esercizi Panca",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "bicipiti", "dorsali"),
            articleTitle = "Esercizi Panca: quali sono i migliori",
            url = "https://www.projectinvictus.it/esercizi-panca/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Muscle Up",
            primaryMuscles = listOf("pettorali", "dorsali", "spalle", "bicipiti", "tricipiti"),
            articleTitle = "Muscle Up: esecuzione corretta e come iniziare",
            url = "https://www.projectinvictus.it/muscle-up-la-guida-completa/",
            execution = emptyList(),
            errors = listOf(
                "Errori comuni da evitare nel Muscle up Il Muscle up è un gesto complesso che " +
                "nasconde molte insidie e molti errori, in questo paragrafo vedremo i principali e i " +
                "più comuni. ",

                "Stance delle mani: troppo larghe o troppo strette. ",

                "Questo è un errore comune nell’atleta principiante, che solitamente non sa a che " +
                "larghezza mantenere le mani e cambia lo stance di serie in serie o di allenamento in " +
                "allenamento. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Body row",
            primaryMuscles = listOf("dorsali", "spalle", "bicipiti", "trapezio", "avambracci"),
            articleTitle = "Body row: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/body-row/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare I principali errori da evitare sono: Elevare le spalle : alzare le " +
                "spalle durante l’esecuzione non permette un ottimale reclutamento del gran dorsale e " +
                "scarica molta tensione sui flessori di gomito. ",

                "Con un’esecuzione del genere è molto probabile che tu senta principalmente lavorare " +
                "le braccia piuttosto che la schiena. ",

                "Non mantenere core e set-up stabili : questo si nota quando l’atleta durante la " +
                "trazione si piega con il busto in avanti e non mantiene il corpo completamente teso. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Lento dietro",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "trapezio"),
            articleTitle = "Lento dietro: esecuzione corretta e muscoli coinvolti",
            url = "https://www.projectinvictus.it/lento-dietro-lento-avanti/",
            execution = listOf(
                "Il lento dietro si esegue riproducendo i principali aspetti esecutivi del lento " +
                "avanti . ",

                "Viene eseguito sempre con il bilanciere, con i manubri non risulterebbe possibile a " +
                "differenza del lento avanti. ",

                "Il lento dietro prevede un’ abduzione dell’omero che per sua natura (dato che il " +
                "bilanciere deve essere portato dietro la testa) è eseguita su un piano di movimento " +
                "frontale puro. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Dip alle parallele",
            primaryMuscles = listOf("pettorali", "dorsali", "spalle", "tricipiti", "addominali"),
            articleTitle = "Dip alle parallele: tutorial, esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/dip-parallele/",
            execution = listOf(
                "Fase 2: discesa, fino a quando la spalla non è più bassa del gomito. ",

                "Distinguiamo due fasi di uguale importanza: setting up e discesa. ",

                "Il setting up è il modo in cui ti approcci alle parallele: posizionati in modo da " +
                "essere più alto rispetto alle parallele; impugna le parallele e utilizzale per " +
                "portare in depressione e adduzione le spalle. "
            ),
            errors = emptyList(),
            variants = listOf(
                "Varianti di dip alle parallele A differenza di molti altri esercizi i dip alle " +
                "parallele non vedono l’utilizzo di vere e proprie varianti. ",

                "Le uniche varianti di dip alle parallele sono le stesse dip lavorate ad angoli " +
                "specifici, con fermi e variazioni di TUT , come ad esempio dip fino a 90° , dip fino " +
                "a 120° e dip da gara. "
            )
        ),
        Exercise(
            name = "V-push up",
            primaryMuscles = listOf("spalle"),
            articleTitle = "V-push up: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/v-push-up/",
            execution = emptyList(),
            errors = listOf(
                "Errori comuni da evitare Allargare troppo i gomiti durante la salita, che porta ad " +
                "aumentare il rischio di infortunio. ",

                "Questo può essere causato da una mancanza di forza, data dal fatto che si sta in " +
                "qualche modo cercando di ridurre la leva rendendola più favorevole per spingere. ",

                "Può sembrare strano, ma questo errore non è dipendente dallo stance delle mani, " +
                "infatti anche con un posizionamento molto largo, se hai ben chiaro come deprimere le " +
                "spalle, porterai i gomiti in un giusto posizionamento per esprimere forza. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Panca piana",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "bicipiti"),
            articleTitle = "Panca piana: come eseguirla",
            url = "https://www.projectinvictus.it/la-panca-piana-nel-bodybuilding/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Varianti di trazioni alla sbarra",
            primaryMuscles = listOf("dorsali", "trapezio", "romboidi", "bicipiti", "spalle"),
            articleTitle = "Varianti di trazioni alla sbarra: esempi ed esercizi",
            url = "https://www.projectinvictus.it/varianti-di-trazioni/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Bicipiti a corpo libero (anche a casa)",
            primaryMuscles = listOf("bicipiti"),
            articleTitle = "Bicipiti a corpo libero (anche a casa)",
            url = "https://www.projectinvictus.it/bicipiti-a-corpo-libero/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Presa prona, supina, neutra",
            primaryMuscles = listOf("dorsali", "romboidi"),
            articleTitle = "Presa prona, supina, neutra: quale usare",
            url = "https://www.projectinvictus.it/presa-prona-supina/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Superman push up",
            primaryMuscles = listOf("pettorali", "spalle", "tricipiti", "femorali", "glutei"),
            articleTitle = "Superman push up: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/superman-push-up/",
            execution = listOf(
                "Il superman push up è una variante molto complessa di piegamento a terra che " +
                "richiede una grande forza in tutta la muscolatura di spinta dell’upper body e " +
                "un’ottima coordinazione tra upper e lower body. ",

                "L’esecuzione corretta prevede di eseguire un push up in cui si spinge a terra con la " +
                "massima potenza possibile accelerando il gesto tanto per staccare le mani e portare " +
                "le braccia tese sopra alla testa . ",

                "Ma questo non basta, dovrai anche staccare i piedi da terra mantenendo le gambe tese " +
                "ed in linea con tutto il corpo. "
            ),
            errors = listOf(
                "Errori comuni da evitare Gli errori più comuni che puoi riscontrare nel Superman " +
                "push up sono: Dare poca spinta . ",

                "Con poca accelerazione non riuscirai a portare le spalle in massima flessione sopra " +
                "alla testa. ",

                "Le ripetizioni non saranno quindi complete e l’esecuzione sarà incompleta. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Pettorali a corpo libero",
            primaryMuscles = listOf("pettorali"),
            articleTitle = "Pettorali a corpo libero",
            url = "https://www.projectinvictus.it/pettorali-corpo-libero-esercizi/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Rematore con kettlebell",
            primaryMuscles = listOf("avambracci", "dorsali", "bicipiti", "trapezio", "romboidi"),
            articleTitle = "Rematore con kettlebell",
            url = "https://www.projectinvictus.it/rematore-kettlebell/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Alzate frontali ai cavi",
            primaryMuscles = listOf("pettorali", "spalle", "bicipiti"),
            articleTitle = "Alzate frontali ai cavi: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/alzate-frontali-cavi/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Ha il “vantaggio” di poter intraruotare il braccio durante la flessione della spalla " +
                "anche se, come abbiamo detto, si tratta di un accorgimento pressoché inutile sul " +
                "piano dell’efficacia. ",

                "Le alzate frontali ai cavi possono essere eseguite a una mano, alternate o assieme, " +
                "e ad impugnatura doppia. "
            )
        ),
        Exercise(
            name = "V-sit",
            primaryMuscles = listOf("femorali", "dorsali", "lombari", "glutei", "quadricipiti"),
            articleTitle = "V-sit: cos’è e come si esegue correttamente?",
            url = "https://www.projectinvictus.it/v-sit-guida/",
            execution = listOf(
                "Tutorial Posizione corretta L’esecuzione corretta della skill consiste nel portare " +
                "le gambe perpendicolari al suolo da una posizione di appoggio delle mani al suolo o " +
                "alle parallele. ",

                "Questo avviene attivamente grazie alla forza del retto addominale e dei muscoli " +
                "flessori d’anca, ma anche grazie all’elasticità dei muscoli estensori dell’anca e " +
                "flessori di ginocchio, come femorali e glutei. ",

                "Inoltre, è bene ricordare che la posizione viene mantenuta grazie alla forza in " +
                "estensione attiva delle spalle che tramite le mani applicano forza a terra. "
            ),
            errors = listOf(
                "Questi errori sono tutti dovuti ad una mancanza di forza , propriocezione o mobilità " +
                "(passiva ed attiva). ",

                "Proprio per questo per allenare il V-sit è importante un periodo di condizionamento " +
                "lungo e meticoloso che permetta di approcciare la skill nel modo più sicuro " +
                "possibile. "
            ),
            variants = listOf(
                "Alternative al V-sit Ottime alternative all’allenamento della posizione di V-sit " +
                "possono essere tutte le varianti di compressione eseguite alla sbarra come per " +
                "esempio i leg raise . ",

                "La versione vista in precedenza prevede di portare le gambe fino a 90° da appesi " +
                "alla sbarra ma questa non è la versione più complessa. "
            )
        ),
        Exercise(
            name = "Muscle up agli anelli",
            primaryMuscles = listOf("pettorali", "dorsali", "spalle", "bicipiti", "tricipiti"),
            articleTitle = "Muscle up agli anelli: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/tutorial-muscle-up-agli-anelli/",
            execution = listOf(
                "Il muscle up agli anelli è un gesto di trazione dinamico che porta l’atleta da sotto " +
                "a sopra il supporto. ",

                "Il muscle up agli anelli si può quindi dividere in 3 fasi distinte: Trazione " +
                "Transizione Spinta e lock-out articolare L’esecuzione corretta del muscle up agli " +
                "anelli prevede di posizionarti appeso agli anelli mantenendo i gomiti tesi in blocco " +
                "articolare e le gambe distese. ",

                "Da questa posizione, trazionati portando gli anelli fino a toccare i pettorali, per " +
                "poi buttarti avanti con le spalle , portando gli anelli a livello dei fianchi. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Planche",
            primaryMuscles = listOf("glutei", "addominali"),
            articleTitle = "Planche: esecuzione, propedeutiche e muscoli coinvolti",
            url = "https://www.projectinvictus.it/planche-tutorial-ed-esecuzione/",
            execution = listOf(
                "Tutorial Innanzitutto, la posizione corretta dei gomiti durante la planche è in " +
                "blocco articolare , per riuscire a reclutare al meglio determinati muscoli (ad " +
                "esempio, il gran dentato) e per scaricare la troppa tensione da altri (ad esempio, " +
                "il bicipite brachiale). ",

                "Altrimenti, la fatica muscolare aumenterebbe in modo spropositato mantenendo i " +
                "gomiti semiflessi e difficilmente riusciresti ad avanzare nel tempo. ",

                "Per quanto riguarda la posizione delle scapole , esse dovranno essere protratte e " +
                "depresse. "
            ),
            errors = listOf(
                "Errori da evitare Non perdere il corretto set-up scapolare e dei gomiti durante " +
                "l’esecuzione! ",

                "Tra gli errori da evitare per una corretta tecnica esecutiva ci sono: perdere " +
                "l’estetica e l’armonia del gesto, mantenere i gomiti semiflessi , perdere " +
                "protrazione e depressione scapolare , lavorare la planche senza una buona mobilità " +
                "del carpo , esasperare gli assetti posturali . ",

                "Errore comune ai primi approcci nella planche lean , è quello di tenere il sedere " +
                "allo stesso livello delle spalle. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Affondi laterali",
            primaryMuscles = listOf("quadricipiti", "femorali", "glutei", "polpacci"),
            articleTitle = "Affondi laterali: tecnica corretta, muscoli coinvolti e varianti",
            url = "https://www.projectinvictus.it/affondi-laterali/",
            execution = listOf(
                "Tecnica ed esecuzione L’esecuzione dell’affondo laterale è riassumibile in questi " +
                "punti: Partenza: parti dalla posizione eretta con i piedi uniti. ",

                "Movimento: esegui un passo laterale lungo circa due volte e mezzo la larghezza delle " +
                "spalle con entrambe le punte dei piedi dritte rivolte in avanti (o leggermente in " +
                "fuori). ",

                "Da qui, fletti contemporaneamente anca, ginocchio e caviglia della gamba che hai " +
                "spostato, scendendo fino a formare un angolo di poco meno di 90° tra la gamba e la " +
                "coscia. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni larghe",
            primaryMuscles = listOf("dorsali"),
            articleTitle = "Trazioni larghe: come eseguirle?",
            url = "https://www.projectinvictus.it/trazioni-larghe/",
            execution = listOf(
                "Una trazione viene considerata a presa larga quando le mani sono posizionate almeno " +
                "ad una larghezza di una volta e mezzo le spalle , fino ad arrivare a due volte. ",

                "Posizionati sotto alla sbarra con gli omeri lungo i fianchi come se volessi fare un " +
                "military press. ",

                "Da questa posizione alza le braccia arrivando con le mani alla sbarra. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni agli anelli",
            primaryMuscles = listOf("pettorali", "dorsali", "spalle", "bicipiti", "tricipiti"),
            articleTitle = "Trazioni agli anelli: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/trazioni-agli-anelli/",
            execution = listOf(
                "L’ esecuzione corretta prevede pochi e semplici passi: Appenditi agli anelli con una " +
                "presa prona Lascia le spalle in una posizione neutra mentre sei appeso Tira " +
                "abbassando le spalle e piegando i gomiti, mantenendo il petto alto Arriva a toccare " +
                "il petto con gli anelli. ",

                "Le trazioni agli anelli sono una variante affascinante e complessa delle trazioni " +
                "alla sbarra. ",

                "Per eseguire una trazione agli anelli in ottima forma esecutiva è infatti richiesta " +
                "un po’ più di forza rispetto che alla sbarra. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni al mento o al petto?",
            primaryMuscles = listOf("dorsali"),
            articleTitle = "Trazioni al mento o al petto?",
            url = "https://www.projectinvictus.it/trazioni-mento-trazioni-petto/",
            execution = listOf(
                "Un errore comune è, infatti, quello di sopperire alla mancanza di forza tramite un’ " +
                "estensione forzata della colonna (tratto toracico) che permette di recuperare quei " +
                "pochi centimetri mancanti per toccare il petto con la sbarra. ",

                "Se non stai realmente creando forza, è solo un compenso che non ti porterà a nulla, " +
                "se non a farti male. ",

                "Trazioni in L-sit al petto Le trazioni in L-sit al petto sono una variante di " +
                "trazione esplosiva che non porta a nessun beneficio in più rispetto a quelli che " +
                "puoi ottenere nelle trazioni al petto da fermo e in quelle in modalità muscle up. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Peck deck",
            primaryMuscles = listOf("pettorali", "spalle", "trapezio", "tricipiti"),
            articleTitle = "Peck deck: esecuzione corretta",
            url = "https://www.projectinvictus.it/peck-deck/",
            execution = emptyList(),
            errors = emptyList(),
            variants = listOf(
                "Varianti della peck deck Ci sono più varianti rispetto alle versioni appena " +
                "analizzate, ciascuna che porta ad un coinvolgimento muscolare differente a seconda " +
                "dell’opzione scelta. ",

                "Conclusioni sulla peck deck In conclusione, la peck deck orizzontale in quanto " +
                "macchinario vincola la traiettoria di movimento e difficilmente permette un " +
                "movimento articolare fisiologico durante gli esercizi. "
            )
        ),
        Exercise(
            name = "Scrollate con manubri per i trapezi",
            primaryMuscles = listOf("trapezio", "romboidi"),
            articleTitle = "Scrollate con manubri per i trapezi",
            url = "https://www.projectinvictus.it/shrugs-scrollate/",
            execution = listOf(
                "Alcuni errori da evitare sono: Aiutarti con le braccia nella fase di sollevamento: " +
                "le braccia dovranno solamente sostenere il peso del manubrio, mentre l’elevazione " +
                "sarà favorita esclusivamente dalle scapole. ",

                "Questo errore accade specialmente quando si usano carichi elevati. ",

                "Lasciarti cadere con le scapole durante la discesa senza accentuare la negativa, " +
                "perdendo la tensione sui muscoli. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Box Squat",
            primaryMuscles = listOf("quadricipiti", "femorali", "glutei", "polpacci", "addominali"),
            articleTitle = "Box Squat: come si esegue",
            url = "https://www.projectinvictus.it/box-squat/",
            execution = emptyList(),
            errors = listOf(
                "Errori comuni nel box squat Gli errori più comuni durante l’esecuzione del box squat " +
                "sono principalmente 4 : Altezza sbagliata per il soggetto: l’altezza deve consentire " +
                "un’esecuzione ottimale e corretta, non avere fretta di arrivare subito ad una " +
                "determinata profondità. ",

                "Perdita di tensione muscolare sul box: sedersi disattivando i muscoli degli arti " +
                "inferiori crea problemi nella fase di risalita nel momento in cui il carico " +
                "utilizzato è consistente. ",

                "Non mantenere le curve fisiologiche durante il movimento. "
            ),
            variants = listOf(
                "Varianti di box squat Box squat monopodalico Questa variante potrebbe avere senso " +
                "eseguirla qualora non si disponga di sovraccarichi , andando ad eseguire in realtà " +
                "un pistol squat con box di riferimento. ",

                "I problemi in questa variante sono 2: Mancanza di un sovraccarico importante. "
            )
        ),
        Exercise(
            name = "Croci ai cavi",
            primaryMuscles = listOf("pettorali", "spalle"),
            articleTitle = "Croci ai cavi: esecuzione e muscoli coinvolti",
            url = "https://www.projectinvictus.it/croci-ai-cavi/",
            execution = emptyList(),
            errors = listOf(
                "In sintesi è quindi importante controllare lo scivolamento scapolare durante la fase " +
                "di chiusura dell’esercizio, assicurandosi che le scapole non si aprano troppo, cosa " +
                "che porterà il gran pettorale in svantaggio meccanico. ",

                "Le braccia ipertese durante lo svolgimento dell’esercizio sono atteggiamento che " +
                "porterà ad un maggior rischio di insulti articolari a carico dell’articolazione " +
                "della spalla. ",

                "E’ consigliabile mantenere qualche grado di flessione dei gomiti durante la fase di " +
                "allungamento muscolare, per poi estendere i gomiti durante la fase di accorciamento " +
                "o chiusura. "
            ),
            variants = listOf(
                "Varianti delle Croci ai cavi Esistono diverse varianti esecutive dell’esercizio " +
                "croci ai cavi, ed ognuna di esse è vittima di accorgimenti tecnici importanti da " +
                "rispettare. ",

                "Vediamole quindi assieme: Croci in piedi ai cavi alti È la variante classicamente " +
                "più utilizzata, che permette un ottimo isolamento muscolare del gran pettorale a " +
                "scapito del deltoide anteriore. "
            )
        ),
        Exercise(
            name = "Croci con manubri su panca",
            primaryMuscles = listOf("pettorali"),
            articleTitle = "Croci con manubri su panca: esecuzione corretta",
            url = "https://www.projectinvictus.it/croci-manubri/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare nelle croci su panca Due accorgimenti importanti riguardano " +
                "l’escursione di movimento e lo schema motorio: è importante nel sollevare i manubri " +
                "pensare di avvicinare i gomiti allo sterno , spostandoli verso il tronco anziché " +
                "pensare di far partire il movimento dai polsi. ",

                "In questo modo selezioni il movimento dell’ omero , importante per veicolare al " +
                "meglio lo stimolo meccanico sul pettorale. ",

                "Sempre durante il sollevamento è consigliabile estendere i gomiti e non avvicinare i " +
                "manubri: ciò garantisce il mantenimento di una tensione costante sul muscolo – evita " +
                "dunque di portate il gomito oltre la linea della spalla. "
            ),
            variants = emptyList()
        ),
        Exercise(
            name = "Vertical row",
            primaryMuscles = listOf("dorsali", "spalle", "bicipiti", "trapezio", "romboidi"),
            articleTitle = "Vertical row: come si esegue e a cosa serve?",
            url = "https://www.projectinvictus.it/vertical-row/",
            execution = emptyList(),
            errors = listOf(
                "Gli errori più frequenti sono: Tirare senza muovere le scapole (mentre tiro non mi " +
                "apro con il petto, ma rimango nella posizione di partenza, eseguendo molto lavoro " +
                "con le braccia e poco con la schiena). ",

                "Tirare senza muovere le scapole e rimanere in chiusura (mentre tiro non mi apro con " +
                "il petto, ma mi chiudo maggiormente rispetto alla posizione di partenza andando " +
                "avanti con le spalle e tirando solo con le braccia). "
            ),
            variants = listOf(
                "Vertical row basso bilaterale o singolo Generalmente eseguito alla macchina “low " +
                "row”, qui il movimento diviene un’estensione d’omero prevalentemente sul piano " +
                "sagittale con coinvolgimento prevalentemente (ma parziale) del gran dorsale. ",

                "Questo perché la spalla parte da una posizione in flessione minore di 90°, il " +
                "braccio è meno orizzontale al terreno, quindi il gran dorsale interviene per meno " +
                "gradi di movimento, oltre che non partire da una posizione di allungamento completo. "
            )
        ),
        Exercise(
            name = "Dolore alla spalla in palestra",
            primaryMuscles = listOf("spalle", "trapezio"),
            articleTitle = "Dolore alla spalla in palestra: cause e rimedi",
            url = "https://www.projectinvictus.it/dolore-alla-spalla-consigli-pratici-palestra/",
            execution = listOf(
                "Saranno invece da prediligere lavori con ampio buffer e una progressione graduale " +
                "dei carichi. ",

                "L’inserimento di esercizi mirati alla correzione delle possibili disfunzioni " +
                "muscolari e articolari (esercizi distretching capsulare e muscolare ed esercizi di " +
                "rinforzo per i muscoli stabilizzatori della scapola e per la cuffia dei rotatori) " +
                "potranno risultare molto utili. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Esercizi trapezio superiore",
            primaryMuscles = listOf("trapezio", "romboidi"),
            articleTitle = "Esercizi trapezio superiore: lo Shrug",
            url = "https://www.projectinvictus.it/esercizi-trapezio-superiore-lo-shrug/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Bicipiti alla Panca Scott",
            primaryMuscles = listOf("bicipiti"),
            articleTitle = "Bicipiti alla Panca Scott",
            url = "https://www.projectinvictus.it/bicipiti-alla-panca-scott/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni presa inversa",
            primaryMuscles = listOf("dorsali", "bicipiti", "avambracci"),
            articleTitle = "Trazioni presa inversa: muscoli attivi e rischi articolari",
            url = "https://www.projectinvictus.it/trazioni-presa-inversa/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Distensioni con manubri sopra la testa",
            primaryMuscles = listOf("spalle", "trapezio", "lombari"),
            articleTitle = "Distensioni con manubri sopra la testa: come farle correttamente.",
            url = "https://www.projectinvictus.it/distensioni-con-manubri-sopra-la-testa/",
            execution = listOf(
                "Vediamo in pochi punti, attraverso l’applicazione pratica di materie scientifiche, " +
                "come sia possibile guidare a un esecuzione più performante e consapevole. ",

                "Distensioni con manubri sopra la testa: escursione di movimento e attivazione " +
                "muscolare Distensioni con manubri sopra la testa partendo da 90° Un primo dubbio che " +
                "sorge riguarda l’escursione di movimento. ",

                "Quasi sempre si vede eseguire le distensioni manubri sopra la testa con " +
                "un’escursione che va dai 90° ai 180° di abduzione , ossia con il gomito che parte " +
                "all’altezza della spalla. "
            ),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Crunch addominali",
            primaryMuscles = listOf("addominali"),
            articleTitle = "Crunch addominali",
            url = "https://www.projectinvictus.it/crunch-addominali/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Side Bending per gli addominali",
            primaryMuscles = listOf("addominali"),
            articleTitle = "Side Bending per gli addominali",
            url = "https://www.projectinvictus.it/side-bending/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Esercizi addominali bassi",
            primaryMuscles = listOf("addominali"),
            articleTitle = "Esercizi addominali bassi",
            url = "https://www.projectinvictus.it/esercizi-addominali-bassi/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Esercizi esterno coscia",
            primaryMuscles = listOf("glutei"),
            articleTitle = "Esercizi esterno coscia",
            url = "https://www.projectinvictus.it/esercizi-esterno-coscia/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Esercizi deltoidi",
            primaryMuscles = listOf("spalle", "trapezio"),
            articleTitle = "Esercizi deltoidi: quali sono i migliori e scheda",
            url = "https://www.projectinvictus.it/allenamento-deltoidi-rapporto-spalle-trapezio/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "French Press",
            primaryMuscles = listOf("pettorali", "tricipiti"),
            articleTitle = "French Press: esecuzione e varianti",
            url = "https://www.projectinvictus.it/french-press/",
            execution = emptyList(),
            errors = listOf(
                "Errori da evitare La french press può essere un esercizio che da fastidio ai gomiti " +
                ", soprattutto col bilanciere dritto, meno con quello EZ o coi manubri. ",

                "Può comunque infiammare il gomito ed il tendine del tricipite. ",

                "Se avvertiamo fastidio o dolore è meglio evitarle. "
            ),
            variants = listOf(
                "Varianti sulla base del ROM Pre-accorciamento Pre-allungamento Kickback French Press " +
                "da seduti, Pushdown al cavo alto Estensioni dietro la nuca Come anche il bicipite , " +
                "il tricipite è un muscolo multiarticolare. ",

                "Per raggiungere un massimo allungamento o un massimo accorciamento sarà determinante " +
                "la posizione della spalla. "
            )
        ),
        Exercise(
            name = "Allenare le spalle da in piedi o da seduto?",
            primaryMuscles = listOf("addominali", "lombari"),
            articleTitle = "Allenare le spalle da in piedi o da seduto?",
            url = "https://www.projectinvictus.it/allenare-le-spalle-piedi-seduto/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Errori d’esecuzione nella lat machine",
            primaryMuscles = listOf("dorsali", "spalle"),
            articleTitle = "Errori d’esecuzione nella lat machine: la schienata",
            url = "https://www.projectinvictus.it/errori-nella-lat-machine/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Quadra Bar",
            primaryMuscles = listOf("lombari"),
            articleTitle = "Quadra Bar: cos’è, che vantaggi ha e come utilizzarla",
            url = "https://www.projectinvictus.it/quadra-bar-cose-che-vantaggi-ha-e-come-utilizzarla/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Distensioni manubri su panca piana",
            primaryMuscles = listOf("pettorali"),
            articleTitle = "Distensioni manubri su panca piana: in intra o extrarotazione?",
            url = "https://www.projectinvictus.it/distensioni-manubri-su-panca-piana-in-intra-o-extrarotazione/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Lat Machine avanti o dietro la testa?",
            primaryMuscles = listOf("avambracci", "spalle", "pettorali", "dorsali"),
            articleTitle = "Lat Machine avanti o dietro la testa?",
            url = "https://www.projectinvictus.it/lat-machine-avanti-dietro-la-testa/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Biomeccanica della spalla in palestra",
            primaryMuscles = listOf("spalle", "trapezio"),
            articleTitle = "Biomeccanica della spalla in palestra",
            url = "https://www.projectinvictus.it/spalla-biomeccanica-ed-esercizi-in-palestra/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Verticale d’impostazione",
            primaryMuscles = listOf("glutei", "lombari"),
            articleTitle = "Verticale d’impostazione: guida e tutorial",
            url = "https://www.projectinvictus.it/verticale-dimpostazione-guida-e-tutorial/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Front Lever",
            primaryMuscles = listOf("dorsali", "spalle", "bicipiti", "trapezio", "romboidi"),
            articleTitle = "Front Lever: Tutorial, propedeutica e biomeccanica",
            url = "https://www.projectinvictus.it/biomeccanica-front-lever/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Trazioni con l’asciugamano. Binomio perfetto",
            primaryMuscles = listOf("dorsali", "spalle", "trapezio", "romboidi", "bicipiti"),
            articleTitle = "Trazioni con l’asciugamano. Binomio perfetto",
            url = "https://www.projectinvictus.it/trazioni-con-lasciugamano-binomio-perfetto/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Squat, pressa e problemi all’anca",
            primaryMuscles = listOf("femorali"),
            articleTitle = "Squat, pressa e problemi all’anca",
            url = "https://www.projectinvictus.it/squat-pressa-e-problemi-allanca/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Esercizi spalle",
            primaryMuscles = listOf("spalle", "trapezio", "bicipiti"),
            articleTitle = "Esercizi spalle: allenamento, anatomia e dolori",
            url = "https://www.projectinvictus.it/spalla-esercizi-dolore-e-biomeccanica/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Lat fly",
            primaryMuscles = listOf("spalle"),
            articleTitle = "Lat fly: un esercizio per il grande dorsale inedito",
            url = "https://www.projectinvictus.it/lat-fly-un-esercizio-per-il-grande-dorsale-inedito/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Scrollate inverse per il riequilibrio scapolare",
            primaryMuscles = listOf("pettorali", "trapezio", "romboidi", "dorsali", "spalle"),
            articleTitle = "Scrollate inverse per il riequilibrio scapolare",
            url = "https://www.projectinvictus.it/scrollate-inverse-per-riequilibrio-scapolare/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "Push down ed esecuzioni per i tricipiti",
            primaryMuscles = listOf("tricipiti", "avambracci"),
            articleTitle = "Push down ed esecuzioni per i tricipiti",
            url = "https://www.projectinvictus.it/push-down-e-french-press-tutorial-ed-esecuzione/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        ),
        Exercise(
            name = "spalla in palestra",
            primaryMuscles = listOf("avambracci", "spalle"),
            articleTitle = "La spalla in palestra: analisi fisiologica e biomeccanica per un esercizio sicuro",
            url = "https://www.projectinvictus.it/la-spalla-in-palestra-analisi-e-biomeccanica-per-un-esercizio-sicuro/",
            execution = emptyList(),
            errors = emptyList(),
            variants = emptyList()
        )
    )

    fun byName(name: String): Exercise? =
        exercises.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun forMuscle(muscle: String): List<Exercise> =
        exercises.filter { e -> e.primaryMuscles.any { it.equals(muscle, ignoreCase = true) } }
}
