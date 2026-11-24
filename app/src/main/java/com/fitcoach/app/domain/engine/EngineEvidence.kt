package com.fitcoach.app.domain.engine

/**
 * Fonti primarie per i parametri dei motori deterministici.
 *
 * PERCHE SEPARATO DAL CORPUS. Il corpus [com.fitcoach.app.domain.knowledge.InvictusKnowledge]
 * e' fatto di 1046 regole, di cui 1000 DIVULGATIVE: articoli divulgativi, diari di
 * atleti, resoconti personali. Va benissimo per spiegare all'utente il perche' di una
 * scelta, ma non e' una base su cui un motore che calcola calorie puo' fissare
 * soglie. Lo si e' visto con la regola "20-25% di ciclizzazione": veniva da un
 * diario del 2016 su un singolo atleta, ed era l'unica fonte di quella frase.
 *
 * Qui stanno gli studi verificabili: meta-analisi, position stand, RCT. Ogni voce
 * riporta il PMID, la citazione completa e la frase esatta da cui viene il numero.
 * I numeri sono stati letti dagli abstract tramite l'API E-utilities di PubMed, non
 * citati a memoria.
 */
object EngineEvidence {

    enum class Kind(val label: String) {
        META_ANALYSIS("meta-analisi"),
        POSITION_STAND("position stand di società scientifica"),
        RCT("studio randomizzato controllato"),
        REVIEW("revisione")
    }

    /**
     * Uno studio verificabile.
     *
     * @param quote frase esatta dell'abstract da cui deriva il parametro: senza
     *   questo campo, "il motore cita la letteratura" non sarebbe controllabile.
     */
    data class Study(
        val id: String,
        val pmid: String,
        val citation: String,
        val kind: Kind,
        val participants: Int?,
        val quote: String,
        /**
         * Link alla fonte. Se null si usa PubMed; serve per i riferimenti
         * istituzionali che non hanno un PMID (es. FAO/WHO/UNU).
         */
        val sourceUrl: String? = null
    ) {
        val url: String get() = sourceUrl ?: "https://pubmed.ncbi.nlm.nih.gov/$pmid/"
    }

    /**
     * Morton 2018: il punto di saturazione delle proteine per la massa magra.
     * 49 RCT, 1863 partecipanti. E' il riferimento piu' solido disponibile.
     */
    val MORTON_2018 = Study(
        id = "Morton2018",
        pmid = "28698222",
        citation = "Morton RW et al. A systematic review, meta-analysis and meta-regression " +
            "of the effect of protein supplementation on resistance training-induced gains in " +
            "muscle mass and strength in healthy adults. Br J Sports Med. 2018;52(6):376-384.",
        kind = Kind.META_ANALYSIS,
        participants = 1863,
        quote = "Protein supplementation beyond total protein intakes of 1.62 g/kg/day " +
            "resulted in no further RET-induced gains in FFM."
    )

    /** Jäger 2017 (ISSN): intervallo di riferimento per chi si allena. */
    val JAEGER_2017 = Study(
        id = "Jaeger2017",
        pmid = "28642676",
        citation = "Jäger R et al. International Society of Sports Nutrition Position Stand: " +
            "Protein and Exercise. J Int Soc Sports Nutr. 2017;14:20.",
        kind = Kind.POSITION_STAND,
        participants = null,
        quote = "1.4-2.0 g protein/kg body weight/day (g/kg/d) is sufficient for most " +
            "exercising individuals."
    )

    /**
     * Aragon 2017 (ISSN): nelle diete ipocaloriche, nei soggetti magri e allenati,
     * servono apporti proteici piu' alti per conservare la massa magra.
     */
    val ARAGON_2017 = Study(
        id = "Aragon2017",
        pmid = "28630601",
        citation = "Aragon AA et al. International Society of Sports Nutrition position stand: " +
            "Diets and body composition. J Int Soc Sports Nutr. 2017;14:16.",
        kind = Kind.POSITION_STAND,
        participants = null,
        quote = "Higher protein intakes (2.3-3.1 g/kg FFM) may be required to maximize " +
            "muscle retention in lean, resistance-trained subjects under hypocaloric conditions."
    )

    /**
     * Helms 2014: riferimento per la preparazione al contest. Da' insieme il ritmo di
     * calo (0,5-1%/settimana), le proteine per MASSA MAGRA e la quota grassi
     * (15-30% delle calorie). E' la fonte del pavimento e del tetto dei grassi.
     */
    val HELMS_2014 = Study(
        id = "Helms2014",
        pmid = "24864135",
        citation = "Helms ER, Aragon AA, Fitschen PJ. Evidence-based recommendations for " +
            "natural bodybuilding contest preparation: nutrition and supplementation. " +
            "J Int Soc Sports Nutr. 2014;11:20.",
        kind = Kind.REVIEW,
        participants = null,
        quote = "Caloric intake should be set at a level that results in bodyweight losses of " +
            "approximately 0.5 to 1%/wk to maximize muscle retention. Within this caloric " +
            "intake, most but not all bodybuilders will respond best to consuming 2.3-3.1 g/kg " +
            "of lean body mass per day of protein, 15-30% of calories from fat, and the " +
            "reminder of calories from carbohydrate."
    )

    /**
     * Xu 2023: restrizione energetica intermittente contro continua. 20 trial,
     * 1511 partecipanti. E' la prova piu' diretta sulla ciclizzazione: le differenze
     * esistono ma sono piccole.
     */
    val XU_2023 = Study(
        id = "Xu2023",
        pmid = "37229479",
        citation = "Xu Z et al. Intermittent energy restriction vs continuous energy " +
            "restriction on metabolic outcomes: a systematic review and meta-analysis. " +
            "Front Nutr. 2023;10:1090792.",
        kind = Kind.META_ANALYSIS,
        participants = 1511,
        quote = "IER and CER intervention equally improved MetS risk factors... BW " +
            "(MD = -0.8 kg, 95% CI [-1.26, -0.33]), BF (MD = -0.75 kg) and FFM " +
            "(MD = -0.49 kg) were also significant improved in IER."
    )

    /**
     * Meta-analisi 2021 sui grassi: le diete a basso contenuto di grassi riducono la
     * testosterone negli uomini. Sostiene il pavimento dei grassi, cioe' il motivo per
     * cui non si scende oltre un certo apporto.
     */
    val FAT_TESTOSTERONE_2021 = Study(
        id = "FatTestosterone2021",
        pmid = "33741447",
        citation = "Whittaker J, Wu K. Low-fat diets and testosterone in men: Systematic review " +
            "and meta-analysis of intervention studies. J Steroid Biochem Mol Biol. " +
            "2021;210:105878. doi:10.1016/j.jsbmb.2021.105878.",
        kind = Kind.META_ANALYSIS,
        participants = 206,
        quote = "Low-fat diets appear to decrease testosterone levels in men... significant " +
            "decreases in total testosterone [-0.38 (95% CI -0.75 to -0.01) P = 0.04]."
    )

    /**
     * Black 1996: 574 misure di dispendio energetico con acqua doppiamente
     * marcata (DLW), 2-95 anni. E' la base dei valori PAL e fissa i limiti
     * fisiologici: circa 1,2 x BMR per chi non deambula e 4,5 x BMR per atleti
     * di endurance.
     */
    val BLACK_1996 = Study(
        id = "Black1996",
        pmid = "8641250",
        citation = "Black AE, Coward WA, Cole TJ, Prentice AM. Human energy expenditure in " +
            "affluent societies: an analysis of 574 doubly-labelled water measurements. " +
            "Eur J Clin Nutr. 1996;50(2):72-92.",
        kind = Kind.META_ANALYSIS,
        participants = 574,
        quote = "A separate analysis of data from non-ambulant subjects, and from elite " +
            "endurance athletes (all excluded from the main dataset) established the limits " +
            "of human daily energy expenditure at around 1.2 x BMR and 4.5 x BMR."
    )

    /**
     * FAO/WHO/UNU 2004: bande PAL ufficiali per categoria di stile di vita.
     *
     * Non e' uno studio ma il riferimento istituzionale che definisce i valori
     * usati per stimare il fabbisogno energetico. Sostituisce i fattori
     * "1,375 / 1,55 / 1,725", che non appartengono a nessuna di queste bande e
     * non hanno fonte.
     */
    val FAO_WHO_UNU_2004 = Study(
        id = "FaoWhoUnu2004",
        pmid = "",
        citation = "FAO/WHO/UNU. Human energy requirements: Report of a Joint FAO/WHO/UNU " +
            "Expert Consultation. Rome, 2004. Chapter 5, Table 5.3.",
        kind = Kind.REVIEW,
        participants = null,
        quote = "Classification of lifestyles in relation to the intensity of habitual " +
            "physical activity: Sedentary or light activity lifestyle 1.40-1.69; Active or " +
            "moderately active lifestyle 1.70-1.99; Vigorous or vigorously active lifestyle " +
            "2.00-2.40. PAL values > 2.40 are difficult to maintain over a long period of time.",
        sourceUrl = "https://www.fao.org/4/y5686e/y5686e07.htm"
    )

    fun all(): List<Study> = listOf(
        MORTON_2018, JAEGER_2017, ARAGON_2017, HELMS_2014, XU_2023, FAT_TESTOSTERONE_2021,
        BLACK_1996, FAO_WHO_UNU_2004
    )

    fun byId(id: String): Study? = all().firstOrNull { it.id == id }
}
