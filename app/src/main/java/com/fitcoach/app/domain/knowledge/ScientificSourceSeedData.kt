package com.fitcoach.app.domain.knowledge

import com.fitcoach.app.data.local.entity.ScientificSourceEntity
import com.fitcoach.app.domain.model.SourceKind

/**
 * Base di conoscenza delle fonti scientifiche.
 *
 * REGOLE DI ONESTA INTELLETTUALE:
 *  - Ogni voce ha un URL verificabile e un tipo di evidenza dichiarato.
 *  - Le regole derivate sono le indicazioni APPLICATE dai motori dell'app.
 *  - I limiti di applicabilita sono sempre esplicitati: indicazioni studiate su
 *    soggetti sani non vengono applicate automaticamente a chi ha limitazioni.
 *  - Project Invictus e classificato come fonte tecnico-divulgativa, NON come
 *    autorita scientifica istituzionale.
 *  - I riferimenti bibliografici forniti dall'utente senza titolo verificabile
 *    sono marcati come "da verificare alla fonte": non ne inventiamo il contenuto.
 */
object ScientificSourceSeedData {

    val sources: List<ScientificSourceEntity> = listOf(
        ScientificSourceEntity(
            kind = SourceKind.OFFICIAL_GUIDELINE,
            title = "WHO guidelines on physical activity and sedentary behaviour",
            authors = "World Health Organization",
            organization = "WHO",
            url = "https://www.who.int/publications/i/item/9789240015128",
            publishedDate = "2020",
            lastUpdated = "2020",
            topic = "Attivita fisica e stile di vita",
            evidence = "Linea guida ufficiale: gli adulti dovrebbero svolgere 150-300 min/settimana " +
                "di attivita aerobica moderata o 75-150 min vigorosa, piu 2+ sedute di " +
                "allenamento di forza a settimana, e ridurre il tempo sedentario.",
            derivedRule = "Il TDEE include il NEAT da lavoro, passi e attivita quotidiana; il tempo " +
                "sedentario riduce il fattore di attivita. L'app suggerisce 2+ sedute di forza.",
            applicabilityLimits = "Indicazioni per la popolazione generale. Con limitazioni fisiche " +
                "o patologie serve una valutazione individuale."
        ),
        ScientificSourceEntity(
            kind = SourceKind.OFFICIAL_GUIDELINE,
            title = "ACSM Position Stand: Progression Models in Resistance Training for Healthy Adults",
            authors = "American College of Sports Medicine",
            organization = "ACSM",
            url = "https://pubmed.ncbi.nlm.nih.gov/19204579/",
            publishedDate = "2009",
            lastUpdated = "2009",
            topic = "Progressione dell'allenamento contro resistenza",
            evidence = "Position stand: la progressione va applicata gradualmente, aumentando prima " +
                "il volume per i principianti e poi l'intensita per gli avanzati; il carico aumenta " +
                "quando si completa il range di ripetizioni con tecnica corretta.",
            derivedRule = "Doppia progressione: si sale di carico solo al raggiungimento del top del " +
                "range su tutte le serie con RIR adeguato. Il volume cresce gradualmente nel mesociclo.",
            applicabilityLimits = "Soggetti sani. Non applicare incrementi automatici in presenza di " +
                "dolore o tecnica scorretta."
        ),
        ScientificSourceEntity(
            kind = SourceKind.SYSTEMATIC_REVIEW,
            title = "Dose-response relationship between weekly resistance training volume and increases in muscle mass",
            authors = "Schoenfeld BJ, Ogborn D, Krieger JW",
            organization = "J Sports Sci (meta-analisi)",
            url = "https://pubmed.ncbi.nlm.nih.gov/27433992/",
            publishedDate = "2017",
            lastUpdated = "2017",
            topic = "Volume settimanale e ipertrofia",
            evidence = "Meta-analisi: esiste una relazione dose-risposta tra volume settimanale e " +
                "crescita muscolare; volumi maggiori (>10 serie/gruppo/settimana) danno piu ipertrofia, " +
                "con rendimenti decrescenti.",
            derivedRule = "Il volume per gruppo e individualizzato (range ~10-20 serie/settimana) in " +
                "base a esperienza, recupero e priorita, non fisso per tutti.",
            applicabilityLimits = "Uomini giovani allenati. Volumi molto alti non sono indicati con " +
                "recupero scarso o principianti."
        ),
        ScientificSourceEntity(
            kind = SourceKind.SYSTEMATIC_REVIEW,
            title = "The effect of training volume and intensity on improvements in muscular strength and size",
            authors = "Ralston GW, et al.",
            organization = "J Sports Sci (revisione)",
            url = "https://pubmed.ncbi.nlm.nih.gov/28366013/",
            publishedDate = "2017",
            lastUpdated = "2017",
            topic = "Volume, intensita e forza",
            evidence = "Revisione: sia volume sia intensita contribuiscono alla forza; la frequenza " +
                "di allenamento distribuisce meglio il volume settimanale.",
            derivedRule = "Distribuzione del volume su piu sedute (frequenza muscolare 2x) quando i " +
                "giorni disponibili lo consentono.",
            applicabilityLimits = "Popolazione allenata; la frequenza va adattata alla disponibilita."
        ),
        ScientificSourceEntity(
            kind = SourceKind.SYSTEMATIC_REVIEW,
            title = "ISSN exercise & sports nutrition review update: research & recommendations",
            authors = "Kerksick CM, et al.",
            organization = "Journal of the International Society of Sports Nutrition",
            url = "https://pubmed.ncbi.nlm.nih.gov/30068354/",
            publishedDate = "2018",
            lastUpdated = "2018",
            topic = "Nutrizione sportiva e macronutrienti",
            evidence = "Revisione ISSN: per l'ipertrofia un apporto proteico di ~1.6-2.2 g/kg/die e " +
                "sufficiente; i carboidrati supportano il volume di allenamento; i grassi non scendono " +
                "sotto ~0.5-0.6 g/kg per la funzione ormonale.",
            derivedRule = "Proteine 1.6-2.2 g/kg in base all'obiettivo; grassi minimo 0.6 g/kg; " +
                "carboidrati a riempimento delle calorie residue con quota maggiore nei giorni di allenamento.",
            applicabilityLimits = "Atleti/soggetti attivi. Con patologie renali o metaboliche serve " +
                "valutazione medica prima di aumentare le proteine."
        ),
        ScientificSourceEntity(
            kind = SourceKind.SYSTEMATIC_REVIEW,
            title = "International Society of Sports Nutrition Position Stand: protein and exercise",
            authors = "Jager R, et al.",
            organization = "Journal of the International Society of Sports Nutrition",
            url = "https://pubmed.ncbi.nlm.nih.gov/28615996/",
            publishedDate = "2017",
            lastUpdated = "2017",
            topic = "Proteine e allenamento",
            evidence = "Position stand ISSN: 1.4-2.0 g/kg/die di proteine supportano il recupero e " +
                "l'adattamento; dosi per pasto ~0.25-0.4 g/kg distribuite nella giornata.",
            derivedRule = "Le proteine sono costanti tra i giorni e distribuite sui pasti secondo le " +
                "quote per slot.",
            applicabilityLimits = "Soggetti sani e attivi."
        ),
        ScientificSourceEntity(
            kind = SourceKind.DIVULGATIVE,
            title = "Project Invictus - approfondimenti su bodybuilding, biomeccanica e programmazione",
            authors = "Project Invictus",
            organization = "Project Invictus (fonte tecnico-divulgativa)",
            url = "https://www.projectinvictus.it/",
            publishedDate = "continuativo",
            lastUpdated = "continuativo",
            topic = "Metodologia di allenamento e tecniche d'intensita",
            evidence = "Contenuti tecnico-divulgativi su split, volume, frequenza, tecniche d'intensita " +
                "(drop set, rest-pause, myo-reps, cluster) e recuperi. NON e una fonte istituzionale.",
            derivedRule = "Le tecniche d'intensita sono proposte solo con giustificazione (esperienza, " +
                "esercizio, recupero, volume) e mai come obbligatorie per l'ipertrofia.",
            applicabilityLimits = "Fonte divulgativa: usata come supporto metodologico, non come " +
                "autorita scientifica. Le affermazioni vanno verificate su letteratura primaria."
        ),
        ScientificSourceEntity(
            kind = SourceKind.OFFICIAL_GUIDELINE,
            title = "Ministero della Salute - Attivita fisica e stili di vita",
            authors = "Ministero della Salute (Italia)",
            organization = "Ministero della Salute",
            url = "https://www.salute.gov.it/",
            publishedDate = "continuativo",
            lastUpdated = "continuativo",
            topic = "Salute, attivita fisica e alimentazione",
            evidence = "Indicazioni istituzionali italiane su attivita fisica regolare, alimentazione " +
                "equilibrata e riduzione della sedentarieta.",
            derivedRule = "Controlli di sicurezza: nessun deficit calorico estremo, promozione di " +
                "attivita quotidiana e idratazione.",
            applicabilityLimits = "Indicazioni generali per la popolazione."
        ),
        ScientificSourceEntity(
            kind = SourceKind.INDIVIDUAL_STUDY,
            title = "Riferimento bibliografico fornito dall'utente (PubMed 41843416)",
            authors = "",
            organization = "",
            url = "https://pubmed.ncbi.nlm.nih.gov/41843416/",
            publishedDate = "",
            lastUpdated = "",
            topic = "Da verificare alla fonte",
            evidence = "Riferimento fornito dall'utente. Il contenuto specifico NON e stato verificato " +
                "e non viene inventato: va letto alla fonte prima di derivarne regole.",
            derivedRule = "Nessuna regola derivata finche la fonte non e verificata.",
            applicabilityLimits = "Da determinare leggendo il testo originale."
        ),
        ScientificSourceEntity(
            kind = SourceKind.INDIVIDUAL_STUDY,
            title = "Riferimento bibliografico fornito dall'utente (PMC12965823)",
            authors = "",
            organization = "",
            url = "https://pmc.ncbi.nlm.nih.gov/articles/PMC12965823/",
            publishedDate = "",
            lastUpdated = "",
            topic = "Da verificare alla fonte",
            evidence = "Riferimento fornito dall'utente. Il contenuto specifico NON e stato verificato " +
                "e non viene inventato: va letto alla fonte prima di derivarne regole.",
            derivedRule = "Nessuna regola derivata finche la fonte non e verificata.",
            applicabilityLimits = "Da determinare leggendo il testo originale."
        ),
        ScientificSourceEntity(
            kind = SourceKind.SYSTEMATIC_REVIEW,
            title = "Riferimento bibliografico fornito dall'utente (PMC5477153)",
            authors = "",
            organization = "",
            url = "https://pmc.ncbi.nlm.nih.gov/articles/PMC5477153/",
            publishedDate = "",
            lastUpdated = "",
            topic = "Da verificare alla fonte",
            evidence = "Riferimento fornito dall'utente. Il contenuto specifico NON e stato verificato " +
                "e non viene inventato: va letto alla fonte prima di derivarne regole.",
            derivedRule = "Nessuna regola derivata finche la fonte non e verificata.",
            applicabilityLimits = "Da determinare leggendo il testo originale."
        ),
        ScientificSourceEntity(
            kind = SourceKind.OFFICIAL_GUIDELINE,
            title = "CREA - Tabelle di composizione degli alimenti",
            authors = "CREA (Consiglio per la ricerca in agricoltura e l'analisi dell'economia agraria)",
            organization = "CREA (ex INRAN)",
            url = "https://www.crea.gov.it/web/alimenti-e-nutrizione/-/tabelle-di-composizione-degli-alimenti",
            publishedDate = "2019",
            lastUpdated = "2019",
            topic = "Composizione degli alimenti",
            evidence = "Tabelle ufficiali italiane di composizione degli alimenti (valori per 100 g " +
                "di parte edibile).",
            derivedRule = "I valori nutrizionali del database alimentare provengono da CREA (o USDA " +
                "quando assenti) e sono dichiarati per voce; nessun valore inventato.",
            applicabilityLimits = "Valori medi di riferimento; possono variare tra prodotti."
        )
    )
}
