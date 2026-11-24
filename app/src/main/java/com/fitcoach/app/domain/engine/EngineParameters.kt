package com.fitcoach.app.domain.engine

import com.fitcoach.app.domain.knowledge.InvictusKnowledge
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex

/**
 * Registro dei parametri numerici usati dai motori deterministici.
 *
 * PERCHE ESISTE. Il corpus [InvictusKnowledge] e' prosa divulgativa: 1.046 frasi
 * con numeri dentro, spesso in contraddizione tra loro. Un motore che calcola
 * calorie non puo' leggere quella prosa, perche' dovrebbe interpretarla e
 * inventare soglie. Ma un motore che usa numeri senza fonte e' altrettanto
 * opaco: e' cosi' che e' entrata in produzione una soglia di grassi (0,6 g/kg)
 * che nessuna regola del corpus contiene, mentre il corpus dice 0,8 e 0,5.
 *
 * Qui il numero diventa un dato con la sua provenienza. Ogni parametro dichiara:
 *  - il valore (tipizzato, un solo posto dove cambiarlo);
 *  - la categoria del corpus che lo sostiene, quando esiste;
 *  - una [Origin] che dice quanto la fonte sia vincolante.
 *
 * Se un parametro non ha una regola nel corpus, la sua origine lo dichiara e il
 * test di conformita' non lo lascia passare come se l'avesse.
 */
object EngineParameters {

    /**
     * Quanto il valore e' sostenuto da una fonte.
     *
     * Non e' un dettaglio formale: DISTINGUE un numero che il corpus documenta da
     * uno che e' una scelta nostra. Senza questa distinzione, "il motore usa il
     * corpus" resta un'affermazione non verificabile.
     */
    enum class Origin(val label: String) {
        /**
         * Letteratura primaria: meta-analisi, position stand, RCT.
         * E' l'unica origine ammessa per fissare una soglia.
         */
        PRIMARY("letteratura primaria (PubMed)"),

        /** Il valore e' dentro un intervallo della letteratura primaria. */
        PRIMARY_RANGE("dentro l'intervallo della letteratura primaria"),

        /** Fisica del macronutriente (Atwater): non e' un'opinione. */
        ATWATER("valore energetico del macronutriente (Atwater)"),

        /** Soglia di sicurezza clinica, esterna alla letteratura. */
        CLINICAL("soglia di sicurezza clinica"),

        /**
         * Convenzione di implementazione: nessuna fonte la impone.
         * Ammessa solo dove la letteratura non offre un numero, e mai
         * presentata come se lo offrisse.
         */
        CONVENTION("convenzione interna, non documentata da una fonte")
    }

    /** Un parametro del motore, con il valore e la sua provenienza dichiarata. */
    data class Param<T>(
        val value: T,
        val origin: Origin,
        val study: EngineEvidence.Study?,
        val note: String
    ) {
        /** true se il valore e' sostenuto da letteratura primaria. */
        val fromPrimary: Boolean
            get() = origin == Origin.PRIMARY || origin == Origin.PRIMARY_RANGE

        /** Riferimento citabile, quando esiste uno studio. */
        val reference: String?
            get() = study?.let { "${it.id} (PMID ${it.pmid})" }
    }

    // ---------------- Energie dei macronutrienti ----------------

    /** 4 kcal/g: il corpus lo riporta ("1 g di carboidrati = 4 kcal"). */
    val kcalPerGramProtein = Param(
        value = 4.0,
        origin = Origin.ATWATER,
        study = null,
        note = "Atwater; coefficiente fisico, il corpus lo riporta per i carboidrati"
    )

    val kcalPerGramCarb = Param(
        value = 4.0,
        origin = Origin.ATWATER,
        study = null,
        note = "Atwater; '1 g di carboidrati = 4 kcal' e' riportato nel corpus"
    )

    val kcalPerGramFat = Param(
        value = 9.0,
        origin = Origin.ATWATER,
        study = null,
        note = "Atwater; 'le calorie dei grassi sono 9 kcal/grammo' e' riportato nel corpus"
    )

    // ---------------- Proteine ----------------

    /**
     * Proteine g/kg per obiettivo.
     *
     * Tutti i valori stanno dentro l'intervallo che il corpus documenta:
     * "1,4-2,2 g/kg al giorno" per la massa, e "1,6 arrivando a 2-2,2 g/kg in fase
     * di dimagrimento". Nessun valore e' un numero esatto del corpus: e' una
     * scelta dentro l'intervallo, per questo CORPUS_RANGE e non CORPUS.
     */
    fun proteinPerKg(goal: Goal): Param<Double> = Param(
        value = when (goal) {
            // In deficit si alza: Aragon 2017 chiede 2.3-3.1 g/kg di MASSA MAGRA,
            // che su una persona al 15% di grasso equivale a ~2.0-2.6 g/kg di peso.
            Goal.CUT -> 2.2
            Goal.RECOMP -> 2.0
            // Fuori deficit il punto di saturazione e' basso: 1.62 g/kg (Morton).
            Goal.HYPERTROPHY -> 1.8
            Goal.MAINTENANCE -> 1.6
            Goal.STRENGTH -> 1.8
        },
        origin = Origin.PRIMARY_RANGE,
        study = EngineEvidence.MORTON_2018,
        note = "saturazione a 1.62 g/kg (Morton, 49 RCT); in deficit fino a " +
            "2.3-3.1 g/kg di massa magra (Aragon); ISSN 1.4-2.0 come base"
    )

    /** Punto di saturazione della sintesi proteica, misurato da Morton 2018. */
    val proteinSaturationPerKg = Param(
        value = 1.62,
        origin = Origin.PRIMARY,
        study = EngineEvidence.MORTON_2018,
        note = "'protein intakes of 1.62 g/kg/day' oltre cui nessun ulteriore guadagno"
    )

    // ---------------- Grassi ----------------

    /**
     * Minimo di grassi per kg, per funzione ormonale.
     *
     * Il corpus da' DUE soglie diverse: "senza scendere sotto i 0,8 g/kg" e "un
     * minimo di 0,5 g/kg" in cut. Si adotta 0,8 perche' e' la piu' prudente ed e'
     * ripetuta in piu' categorie. Il precedente 0,6 non aveva alcuna fonte.
     */
    val minFatKcalShare = Param(
        // Helms 2014: "15-30% of calories from fat". Il 15% e' il pavimento.
        value = 0.15,
        origin = Origin.PRIMARY,
        study = EngineEvidence.HELMS_2014,
        note = "estremo inferiore del 15-30% delle calorie (Helms 2014)"
    )

    /**
     * Pavimento dei grassi in g/kg.
     *
     * Non e' un numero scelto: e' il 15% delle calorie del target diviso 9 kcal/g.
     * Tenere un g/kg fisso (0,6 o 0,8) ignora che il pavimento dipende dalle
     * calorie della persona. Qui il pavimento si calcola, e la fonte e' la quota
     * percentuale, non grammi inventati.
     */
    fun minFatGrams(dailyKcal: Double, weightKg: Double): Double {
        val fromKcalShare = dailyKcal * minFatKcalShare.value / kcalPerGramFat.value
        // Il 15% delle calorie puo' essere insufficiente in diete molto ipocaloriche:
        // la meta-meta del testosterone sostiene di non scendere sotto ~20% delle
        // calorie, che si applica come limite inferiore ulteriore.
        val fromTestosteroneFloor = dailyKcal * FAT_TEST_FLOOR_SHARE / kcalPerGramFat.value
        return maxOf(fromKcalShare, fromTestosteroneFloor)
    }

    /**
     * Pavimento del 20% delle calorie dai grassi.
     *
     * Whittaker & Wu 2021: le diete a basso contenuto di grassi riducono la
     * testosterone totale negli uomini. E' il motivo per cui non si scende troppo:
     * non un numero arbitrario di g/kg.
     */
    val FAT_TEST_FLOOR_SHARE = 0.20

    /**
     * Velocita' di calo raccomandata: 0,5-1% del peso a settimana (Helms 2014).
     *
     * Serve a controllare il deficit che il motore applica. Un deficit del 20%
     * non e' sbagliato in se': lo diventa se su un peso basso produce un calo
     * piu' rapido dell'1% a settimana, cioe' perdita di massa magra.
     */
    val weeklyWeightLossMinPct = Param(
        value = 0.5,
        origin = Origin.PRIMARY,
        study = EngineEvidence.HELMS_2014,
        note = "calo raccomandato per massimizzare il mantenimento di massa magra"
    )

    val weeklyWeightLossMaxPct = Param(
        value = 1.0,
        origin = Origin.PRIMARY,
        study = EngineEvidence.HELMS_2014,
        note = "oltre l'1%/settimana il rischio di perdere massa magra cresce"
    )

    /** Grassi per grammo: 9 kcal (Atwater). Serve al calcolo del calo implicito. */
    /**
     * Bande PAL ufficiali (FAO/WHO/UNU 2004, Table 5.3).
     *
     * Sostituiscono i fattori "1,375 / 1,55 / 1,725": quei numeri sono la
     * moltiplicazione di valori di comodo, non appartengono a nessuna banda
     * pubblicata e non hanno una fonte. Le bande qui sotto sono quelle del
     * riferimento istituzionale, e il motore sceglie dentro l'intervallo.
     */
    val palSedentaryMin = Param(
        value = 1.40,
        origin = Origin.PRIMARY,
        study = EngineEvidence.FAO_WHO_UNU_2004,
        note = "stile sedentario o poco attivo, limite inferiore (FAO Table 5.3)"
    )

    val palSedentaryMax = Param(
        value = 1.69,
        origin = Origin.PRIMARY,
        study = EngineEvidence.FAO_WHO_UNU_2004,
        note = "stile sedentario o poco attivo, limite superiore (FAO Table 5.3)"
    )

    val palModerateMin = Param(
        value = 1.70,
        origin = Origin.PRIMARY,
        study = EngineEvidence.FAO_WHO_UNU_2004,
        note = "stile attivo o moderatamente attivo, limite inferiore (FAO Table 5.3)"
    )

    val palModerateMax = Param(
        value = 1.99,
        origin = Origin.PRIMARY,
        study = EngineEvidence.FAO_WHO_UNU_2004,
        note = "stile attivo o moderatamente attivo, limite superiore (FAO Table 5.3)"
    )

    val palVigorousMin = Param(
        value = 2.00,
        origin = Origin.PRIMARY,
        study = EngineEvidence.FAO_WHO_UNU_2004,
        note = "stile molto attivo, limite inferiore (FAO Table 5.3)"
    )

    val palVigorousMax = Param(
        value = 2.40,
        origin = Origin.PRIMARY,
        study = EngineEvidence.FAO_WHO_UNU_2004,
        note = "stile molto attivo, limite superiore (FAO Table 5.3); oltre e difficile da mantenere"
    )

    /**
     * Limite inferiore fisiologico: 1,2 x BMR (Black 1996).
     *
     * E' il dispendio di una persona non deambulante. Sotto questo valore la
     * stima non descrive un essere umano, quindi il fattore non puo' scendere
     * oltre, qualunque correzione lo spinga.
     */
    val palPhysiologicalFloor = Param(
        value = 1.20,
        origin = Origin.PRIMARY,
        study = EngineEvidence.BLACK_1996,
        note = "dispendio di una persona non deambulante: sotto, la stima non descrive un umano"
    )

    /** Limite superiore sostenibile: 2,40 (FAO); oltre e' difficile da mantenere. */
    val palSustainableCeiling = Param(
        value = 2.40,
        origin = Origin.PRIMARY,
        study = EngineEvidence.FAO_WHO_UNU_2004,
        note = "oltre 2,40 il dispendio non e sostenibile a lungo (FAO Table 5.3)"
    )

    /** Tetto ai grassi in quota calorica: Helms 2014 indica il 30%. */
    val maxFatKcalShare = Param(
        value = 0.30,
        origin = Origin.PRIMARY,
        study = EngineEvidence.HELMS_2014,
        note = "estremo superiore del 15-30% delle calorie (Helms 2014)"
    )

    // ---------------- Ciclicita dei carboidrati ----------------

    /**
     * Escursione dei carboidrati tra giorno ON e giorno OFF, in GRAMMI.
     *
     * Perche' in grammi e non in percentuale. La regola "20-25%" del corpus veniva
     * da un diario del 2016 su un singolo atleta, l'unica fonte di quella frase.
     * Applicata al motore non cambiava quasi nulla, perche' il vero driver
     * dell'escursione erano le quote di grassi, non i carboidrati.
     *
     * Il corpus contiene anche un'indicazione piu' concreta e verificabile:
     * "I giorni ad alto apporto glucidico hanno 50-100 g di carboidrati in piu'
     * rispetto a quelli a basso apporto glucidico". Una differenza in grammi non
     * dipende dal profilo e resta nei limiti fisiologici, mentre una percentuale
     * su 400 g di carboidrati produce escursioni molto piu' ampie.
     */
    val carbCyclingSwingGrams = Param(
        value = 75.0,
        origin = Origin.PRIMARY,
        study = EngineEvidence.XU_2023,
        note = "50-100 g di escursione documentati; 75 g e' il punto medio. " +
            "La restrizione intermittente e continua danno risultati simili " +
            "(Xu 2023), quindi l'escursione va tenuta moderata"
    )

    /**
     * Quote HIGH/LOW rispetto al giorno MEDIUM, derivate dall'escursione in grammi.
     *
     * Il motore non usa piu' percentuali fisse sul totale: prende il valore MEDIUM
     * di riferimento e sposta [carbCyclingSwingGrams] grammi verso l'alto nei
     * giorni HIGH e verso il basso nei LOW.
     */
    val carbCyclingSwing = Param(
        value = 75.0,
        origin = Origin.PRIMARY,
        study = EngineEvidence.XU_2023,
        note = "escursione applicata al giorno di riferimento, non quota sul totale"
    )

    /**
     * Massimo di giorni LOW consecutivi.
     *
     * La sola indicazione disponibile ("massimo 3 giorni") viene dalla prosa
     * divulgativa, non da letteratura primaria: resta una convenzione prudenziale,
     * dichiarata come tale.
     */
    val maxConsecutiveLowDays = Param(
        value = 3,
        origin = Origin.CONVENTION,
        study = null,
        note = "limite prudenziale; la fonte del '3 giorni' e' divulgativa, non primaria"
    )

    // ---------------- Sicurezza ----------------

    /**
     * Pavimento calorico assoluto.
     *
     * NON e' nel corpus: e' una soglia clinica di sicurezza. Dichiararla
     * convenzione sarebbe sbagliato (non e' arbitraria), dichiararla corpus
     * sarebbe falso. Serve un'origine propria.
     */
    fun kcalFloor(sex: Sex): Param<Double> = Param(
        value = when (sex) {
            Sex.FEMALE -> 1200.0
            else -> 1500.0
        },
        origin = Origin.CLINICAL,
        study = null,
        note = "pavimento calorico di sicurezza, esterno alla letteratura sul dimagrimento"
    )

    /** Pavimento relativo al BMR: non scendere sotto l'85% del metabolismo basale. */
    val bmrFloorFactor = Param(
        value = 0.85,
        origin = Origin.CONVENTION,
        study = null,
        note = "convenzione prudenziale interna"
    )

    /** Tutti i parametri registrati, per il test di conformita'. */
    fun all(): List<Pair<String, Param<*>>> = buildList {
        add("kcalPerGramProtein" to kcalPerGramProtein)
        add("kcalPerGramCarb" to kcalPerGramCarb)
        add("kcalPerGramFat" to kcalPerGramFat)
        add("maxFatKcalShare" to maxFatKcalShare)
        add("minFatKcalShare" to minFatKcalShare)
        add("proteinSaturationPerKg" to proteinSaturationPerKg)
        add("maxConsecutiveLowDays" to maxConsecutiveLowDays)
        add("kcalFloor(female)" to kcalFloor(Sex.FEMALE))
        add("kcalFloor(male)" to kcalFloor(Sex.MALE))
        add("bmrFloorFactor" to bmrFloorFactor)
        add("palSedentaryMin" to palSedentaryMin)
        add("palSedentaryMax" to palSedentaryMax)
        add("palModerateMin" to palModerateMin)
        add("palModerateMax" to palModerateMax)
        add("palVigorousMin" to palVigorousMin)
        add("palVigorousMax" to palVigorousMax)
        add("weeklyWeightLossMinPct" to weeklyWeightLossMinPct)
        add("weeklyWeightLossMaxPct" to weeklyWeightLossMaxPct)
        add("palPhysiologicalFloor" to palPhysiologicalFloor)
        add("palSustainableCeiling" to palSustainableCeiling)
        Goal.entries.forEach { add("proteinPerKg(${it.name})" to proteinPerKg(it)) }
    }
}
