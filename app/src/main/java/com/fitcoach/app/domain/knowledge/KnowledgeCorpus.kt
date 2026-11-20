package com.fitcoach.app.domain.knowledge

import com.fitcoach.app.domain.model.SourceKind

/**
 * Corpus delle regole applicate dal Coach AI, con la fonte dichiarata.
 *
 * Ogni regola deriva da una voce di [ScientificSourceSeedData] e viene citata nel
 * prompt: il Coach deve motivare le scelte su basi dichiarate, non su soglie
 * inventate. Le regole sono indicazioni di metodo, non cancelli: i controlli
 * bloccanti restano solo su sicurezza e coerenza verificabile.
 *
 * ONESTA INTELLETTUALE: nessuna regola viene attribuita a una fonte il cui
 * contenuto non e stato letto. Project Invictus e una fonte tecnico-divulgativa:
 * se ne usa solo la regola metodologica gia' dichiarata nel seed, e le tecniche
 * d'intensita' restano facoltative e motivate.
 */
object KnowledgeCorpus {

    /** Ambito in cui una regola si applica. */
    enum class Area(val label: String) {
        TRAINING("Allenamento"),
        NUTRITION("Nutrizione"),
        SAFETY("Sicurezza")
    }

    data class Rule(
        val area: Area,
        val statement: String,
        val source: String,
        val kind: SourceKind,
        val limits: String
    )

    val rules: List<Rule> = listOf(
        // ---------------- Allenamento ----------------
        Rule(
            area = Area.TRAINING,
            statement = "Il volume settimanale per gruppo e' individualizzato su esperienza, recupero e " +
                "priorita: ~10-20 serie effettive/settimana, con rendimenti decrescenti oltre. " +
                "Non esiste un numero valido per tutti, e gruppi piccoli (polpacci, braccia, core) " +
                "non seguono le stesse soglie dei grandi.",
            source = "Schoenfeld BJ, Ogborn D, Krieger JW - Dose-response relationship between weekly " +
                "resistance training volume and increases in muscle mass (J Sports Sci, 2017)",
            kind = SourceKind.SYSTEMATIC_REVIEW,
            limits = "Dati su uomini giovani allenati; volumi alti non sono indicati con recupero scarso."
        ),
        Rule(
            area = Area.TRAINING,
            statement = "Il volume settimanale si distribuisce su piu' sedute quando i giorni disponibili " +
                "lo consentono: frequenza muscolare ~2x/settimana distribuisce meglio il lavoro.",
            source = "Ralston GW, et al. - The effect of training volume and intensity on improvements " +
                "in muscular strength and size (2017)",
            kind = SourceKind.SYSTEMATIC_REVIEW,
            limits = "La frequenza va adattata ai giorni realmente disponibili."
        ),
        Rule(
            area = Area.TRAINING,
            statement = "Progressione a doppia progressione: si sale di carico solo dopo aver completato " +
                "il top del range di ripetizioni su tutte le serie con tecnica corretta e RIR adeguato. " +
                "Per i principianti cresce prima il volume, per gli avanzati prima l'intensita'.",
            source = "ACSM Position Stand - Progression Models in Resistance Training for Healthy Adults (2009)",
            kind = SourceKind.OFFICIAL_GUIDELINE,
            limits = "Nessun incremento automatico con dolore, tecnica scorretta o RIR insufficiente."
        ),
        Rule(
            area = Area.TRAINING,
            statement = "Le tecniche d'intensita' (drop set, rest-pause, myo-reps, cluster) sono opzioni " +
                "metodologiche facoltative: si propongono solo con una motivazione legata a esperienza, " +
                "esercizio, recupero e volume, mai come obbligatorie per l'ipertrofia.",
            source = "Project Invictus - metodologia di allenamento e tecniche d'intensita' " +
                "(fonte tecnico-divulgativa)",
            kind = SourceKind.DIVULGATIVE,
            limits = "Fonte divulgativa: supporto metodologico, non autorita' scientifica. " +
                "Le affermazioni vanno confrontate con la letteratura primaria."
        ),
        Rule(
            area = Area.TRAINING,
            statement = "L'attivita' di forza va mantenuta su almeno 2 sedute settimanali, insieme a " +
                "un'attivita' quotidiana non sedentaria.",
            source = "WHO guidelines on physical activity and sedentary behaviour (2020)",
            kind = SourceKind.OFFICIAL_GUIDELINE,
            limits = "Indicazioni di popolazione generale; con limitazioni serve valutazione individuale."
        ),

        // ---------------- Nutrizione ----------------
        Rule(
            area = Area.NUTRITION,
            statement = "Proteine 1.6-2.2 g/kg/die per l'ipertrofia, costanti tra i giorni e distribuite " +
                "sui pasti (~0.25-0.4 g/kg per pasto).",
            source = "ISSN exercise & sports nutrition review update (Kerksick CM, et al., 2018) e " +
                "ISSN Position Stand: protein and exercise (Jager R, et al., 2017)",
            kind = SourceKind.SYSTEMATIC_REVIEW,
            limits = "Con patologie renali o metaboliche serve valutazione medica."
        ),
        Rule(
            area = Area.NUTRITION,
            statement = "I grassi non scendono sotto ~0.6 g/kg/die per la funzione ormonale; i carboidrati " +
                "riempiono le calorie residue, con quota maggiore nei giorni di allenamento piu' intenso.",
            source = "ISSN exercise & sports nutrition review update (Kerksick CM, et al., 2018)",
            kind = SourceKind.SYSTEMATIC_REVIEW,
            limits = "Soggetti sani e attivi."
        ),
        Rule(
            area = Area.NUTRITION,
            statement = "La ciclicita' dei carboidrati modula i carboidrati tra i giorni, non le proteine: " +
                "il deficit settimanale resta nel range fisiologico e non si crea un deficit eccessivo " +
                "nei giorni LOW.",
            source = "ISSN exercise & sports nutrition review update (Kerksick CM, et al., 2018) - " +
                "i carboidrati supportano il volume di allenamento",
            kind = SourceKind.SYSTEMATIC_REVIEW,
            limits = "Adattare al volume reale e al recupero, non a un modello astratto."
        ),
        Rule(
            area = Area.NUTRITION,
            statement = "I valori nutrizionali degli alimenti provengono da banche dati riconosciute " +
                "(CREA); le grammature si calcolano su quei valori, non a stima.",
            source = "CREA - Tabelle di composizione degli alimenti (2019)",
            kind = SourceKind.OFFICIAL_GUIDELINE,
            limits = "I valori sono medi: cottura e varieta' del prodotto cambiano il contenuto."
        ),

        // ---------------- Sicurezza ----------------
        Rule(
            area = Area.SAFETY,
            statement = "Nessun deficit calorico estremo, nessun volume irragionevole, nessuna diagnosi e " +
                "nessuna prescrizione di farmaci, ormoni o sostanze dopanti.",
            source = "Ministero della Salute - Attivita' fisica e stili di vita; WHO (2020)",
            kind = SourceKind.OFFICIAL_GUIDELINE,
            limits = "Indicazioni generali: non sostituiscono il parere medico."
        ),
        Rule(
            area = Area.SAFETY,
            statement = "Allergie, intolleranze, alimenti esclusi ed esercizi vietati da limitazioni " +
                "fisiche sono vincoli assoluti, non preferenze da bilanciare.",
            source = "Ministero della Salute - indicazioni su alimentazione e sicurezza",
            kind = SourceKind.OFFICIAL_GUIDELINE,
            limits = "In presenza di dolore l'utente interrompe l'esercizio e lo segnala."
        )
    )

    /** Regole di una singola area. */
    fun forArea(area: Area): List<Rule> = rules.filter { it.area == area }

    /**
     * Blocco compatto per il prompt: ogni regola con la sua fonte.
     * Formato leggibile per il modello, senza elenchi ridondanti.
     */
    fun promptBlock(areas: List<Area> = Area.entries): String = buildString {
        appendLine("CORPUS DI CONOSCENZA (regole di metodo con fonte dichiarata).")
        appendLine("Sono indicazioni, non cancelli: se te ne discosti, spiega perche' citando la fonte.")
        areas.forEach { area ->
            val list = forArea(area)
            if (list.isEmpty()) return@forEach
            appendLine("[${area.label}]")
            list.forEach { r ->
                appendLine("- ${r.statement}")
                appendLine("  Fonte: ${r.source} (${r.kind.label})")
                appendLine("  Limiti: ${r.limits}")
            }
        }
    }.trimEnd()
}
