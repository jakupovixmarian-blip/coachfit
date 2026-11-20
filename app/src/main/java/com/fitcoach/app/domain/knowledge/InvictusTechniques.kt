package com.fitcoach.app.domain.knowledge

import com.fitcoach.app.domain.model.SourceKind

/**
 * Tecniche d'intensita di Project InVictus: come si eseguono, con i parametri
 * numerici dichiarati nell'articolo (mini-serie, percentuali di 1RM, recuperi).
 *
 * Servono al motore quando deve proporre una tecnica invece di aggiungere serie:
 * un drop set al 10-25% di calo del carico e una scelta diversa da "altre 4 serie".
 *
 * Classificazione: [SourceKind.DIVULGATIVE].
 */
object InvictusTechniques {

    data class Technique(
        val name: String,
        val articleTitle: String,
        val url: String,
        val publishedDate: String,
        val rules: List<String>,
        val kind: SourceKind = SourceKind.DIVULGATIVE
    )

    val techniques: List<Technique> = listOf(
        Technique(
            name = "Drop set",
            articleTitle = "Drop set bodybuilding: cos’è e come si fa",
            url = "https://www.projectinvictus.it/drop-set-bodybuilding/",
            publishedDate = "2022-11-16",
            rules = listOf(
                "Non solo, ma il carico potrà essere calato di un ulteriore 10-25% per continuare a " +
                "sfinire le fibre rimaste, in questo caso non si parla più di doppio Drop Set ma di " +
                "triplo Drop Set . "
            )
        ),
        Technique(
            name = "Giant set",
            articleTitle = "Giant set: cos’è e come impostarlo",
            url = "https://www.projectinvictus.it/giant-set/",
            publishedDate = "2022-07-22",
            rules = listOf(
                "Il Giant Set (o Set Gigante ) nel bodybuilding segue le stesse logiche di superserie " +
                "e triset ma porta all’estremo il concetto utilizzando più di 3 esercizi in serie . ",

                "Giant Set: esempio di scheda di allenamento Scheda giant set pre-congestione Il " +
                "primo, il secondo o il terzo esercizio sono di isolamento per il gruppo muscolare (1 " +
                "– 2 o 1 – 3), mentre il secondo, il terzo o il quarto esercizio (2 – 4 o 3 – 4) sono " +
                "un multiarticolare con target il muscolo pre-affaticato. "
            )
        ),
        Technique(
            name = "Heavy Duty",
            articleTitle = "Heavy Duty: principi e scheda di allenamento",
            url = "https://www.projectinvictus.it/heavy-duty-allenamento/",
            publishedDate = "2022-07-31",
            rules = listOf(
                "L’allenamento Heavy Duty nasce in un periodo in cui la maggior parte dei Bodybuilder " +
                "si allenavano con schede ad alto volume (10 – 12 split a settimana). ",

                "La preparazione per la monoserie dell’Heavy Duty ed il fatto che si inseriscano " +
                "spesso superserie o giant set, lo possiamo anche considerare come se fossero 3 – 6 " +
                "serie, ma rimanendo sotto del 50 % per ottenere da natural i migliori benefici. "
            )
        ),
        Technique(
            name = "Isometria",
            articleTitle = "Trazioni isometriche e trazioni frenchy",
            url = "https://www.projectinvictus.it/trazioni-isometriche-frenchy/",
            publishedDate = "2022-03-31",
            rules = listOf(
                "Un esempio potrebbe essere il seguente: trazioni con fermo alla fronte in salita 2” " +
                "3 x 5 serie risultano molto più fruttuose rispetto all’allenare la sola isometria " +
                "alla fronte 10” x 5 serie. "
            )
        ),
        Technique(
            name = "Metodo Hatfield",
            articleTitle = "Il metodo Hatfield",
            url = "https://www.projectinvictus.it/metodo-hatfield-ipertrofia/",
            publishedDate = "2025-05-10",
            rules = listOf(
                "In questo contesto, quindi, la scelta del tipo di esercizio è di fondamentale " +
                "importanza affinché l’applicazione del Metodo Hatfield sia efficace: 1° esercizio: " +
                "esercizio multiarticolare (T.U.T. "
            )
        ),
        Technique(
            name = "Piramidale",
            articleTitle = "Allenamento piramidale",
            url = "https://www.projectinvictus.it/allenamento-piramidale/",
            publishedDate = "2023-03-27",
            rules = listOf(
                "Un piramidale crescente classico prevede che la prima serie sia eseguita al 70% di " +
                "1RM per 12 ripetizioni per poi arrivare all’ultima serie eseguita con il 90-100 % di " +
                "1RM. ",

                "L’ obiettivo di questo tipo di piramidale è quello di andare ad eseguire una o più " +
                "ripetizioni con carichi pesanti (in questo caso il 100 % dell’1RM) dopo un " +
                "affaticamento dovuto ad una scalata di carico. ",

                "RIPETIZIONI % 1RM 1 100 2 95 3 90 5 85 8 80 10 75 12 70 Piramidale doppio Il metodo " +
                "piramidale doppio è una fusione tra piramidale crescente e decrescente: si parte da " +
                "leggero a pesante (o da pesante a leggero), si procede a ritroso . "
            )
        ),
        Technique(
            name = "Rest pause",
            articleTitle = "Rest pause: come e quando usarlo",
            url = "https://www.projectinvictus.it/rest-pause/",
            publishedDate = "2025-08-28",
            rules = listOf(
                "Il Rest pause prevede di fare più mini-serie da poche ripetizioni (2-3 o anche 4-6 e " +
                "non di più) con un carico medio-alto e una pausa breve tra le serie ( 15-30” ), fino " +
                "ad arrivare al numero di ripetizioni prefissato nell’allenamento (20-30). "
            )
        )
    )

    fun byName(name: String): Technique? =
        techniques.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
