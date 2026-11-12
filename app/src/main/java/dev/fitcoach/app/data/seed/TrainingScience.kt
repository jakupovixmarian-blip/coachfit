package dev.fitcoach.app.data.seed

import dev.fitcoach.app.domain.model.KnowledgeNote
import dev.fitcoach.app.domain.model.KnowledgeTopic

/**
 * Training principles the coach reasons on, each with a verifiable source.
 *
 * These notes summarise what the cited literature reports. They are deliberately
 * short and conservative, and the coach is told to prefer the user's own notes
 * when the two disagree. No numerical value is stated here that the cited source
 * does not support, and no source is cited that does not exist.
 */
object TrainingScience {

    fun builtIn(): List<KnowledgeNote> = listOf(
        KnowledgeNote(
            topic = KnowledgeTopic.VOLUME,
            title = "Volume settimanale e ipertrofia: relazione dose-risposta",
            content = "Il volume settimanale per gruppo muscolare e il principale fattore " +
                "modificabile dell'ipertrofia. La meta-analisi dose-risposta indica un " +
                "aumento della crescita fino a circa 10 serie settimanali per gruppo, con " +
                "ulteriori guadagni piu incerti oltre quel valore e rendimenti decrescenti.",
            source = "Schoenfeld BJ, Ogborn D, Krieger JW. Dose-response relationship between " +
                "weekly resistance training volume and increases in muscle mass: A systematic " +
                "review and meta-analysis. J Sports Sci. 2017;35(11):1073-1082."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.VOLUME,
            title = "Volume alto contro volume basso: la meta-analisi piu ampia",
            content = "Confrontando volumi di serie diversi, un volume maggiore produce in media " +
                "piu ipertrofia, con una differenza media di circa il 2-3 percento per ogni " +
                "serie settimanale aggiuntiva per gruppo. L'effetto e piccolo e va bilanciato " +
                "con il recupero disponibile, non massimizzato a ogni costo.",
            source = "Schoenfeld BJ, Grgic J, Krieger J. How many times per week should a " +
                "muscle be trained to maximize hypertrophy? A systematic review and " +
                "meta-analysis. J Sports Sci. 2019;37(11):1286-1295."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.VOLUME,
            title = "Il volume conta piu dell'intensita di carico per l'ipertrofia",
            content = "Quando il volume di serie e equiparato, carichi bassi (circa 30 percento " +
                "del massimale) e carichi alti producono ipertrofia simile, a condizione di " +
                "arrivare vicino al cedimento. Per la forza massimale, invece, i carichi alti " +
                "restano superiori.",
            source = "Schoenfeld BJ, Grgic J, Ogborn D, Krieger JW. Strength and hypertrophy " +
                "adaptations between low- vs. high-load resistance training: A systematic " +
                "review and meta-analysis. J Strength Cond Res. 2017;31(12):3508-3523."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.FREQUENCY,
            title = "Frequenza: due sedute per gruppo sono sufficienti se il volume e pari",
            content = "Quando il volume settimanale e equiparato, distribuire il lavoro su due " +
                "sedute per gruppo muscolare non e inferiore a una sola seduta, e risulta " +
                "spesso piu praticabile per raggiungere volumi alti con serie di qualita. " +
                "Non emerge un vantaggio automatico oltre le due-tre sedute per gruppo.",
            source = "Schoenfeld BJ, Grgic J, Krieger J. How many times per week should a " +
                "muscle be trained to maximize hypertrophy? A systematic review and " +
                "meta-analysis. J Sports Sci. 2019;37(11):1286-1295."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.INTENSITY,
            title = "Vicinanza al cedimento: l'RIR come parametro pratico",
            content = "Le serie portate a poche ripetizioni dal cedimento (RIR 0-3) stimolano " +
                "l'ipertrofia in modo simile. Il cedimento assoluto su ogni serie aggiunge " +
                "fatica e non e necessario; e piu utile tenere un RIR basso ma non nullo " +
                "sulla maggior parte delle serie.",
            source = "Zourdos MC, Klemp A, Dolan C, et al. Novel resistance training-specific " +
                "RPE scale measuring repetitions in reserve. J Strength Cond Res. " +
                "2016;30(1):267-275."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.PROGRESSION,
            title = "Sovraccarico progressivo: aumento di carico o di ripetizioni",
            content = "Il miglioramento della forza e guidato dall'aumento progressivo del " +
                "carico o delle ripetizioni a parita di carico. Un modello semplice e " +
                "affidabile e il doppio progresso: si sale di ripetizioni dentro un intervallo " +
                "e solo quando l'estremo superiore e raggiunto su tutte le serie si aumenta il " +
                "carico.",
            source = "American College of Sports Medicine. Progression models in resistance " +
                "training for healthy adults. Med Sci Sports Exerc. 2009;41(3):687-708."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.TECHNIQUE,
            title = "Tecnica: range completo e controllo del carico",
            content = "Escursioni articolari complete producono in genere piu ipertrofia " +
                "rispetto a escursioni parziali, in particolare per il lavoro in allungamento. " +
                "Il carico va scelto per poter controllare la fase eccentrica, non per " +
                "spostare il peso con slancio.",
            source = "Schoenfeld BJ, Grgic J. Effects of range of motion on muscle development " +
                "during resistance training interventions: A systematic review. SAGE Open " +
                "Medicine. 2020;8:2050312120901559."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.WARMUP,
            title = "Riscaldamento specifico prima delle serie allenanti",
            content = "Un riscaldamento specifico con serie leggere progressive sullo stesso " +
                "esercizio migliora la prestazione e riduce il rischio di infortunio. Per la " +
                "maggior parte degli esercizi sono sufficienti due o tre serie di avvicinamento " +
                "a carico crescente e ripetizioni decrescenti.",
            source = "Fradkin AJ, Zazryn TR, Smoliga JM. Effects of warming-up on physical " +
                "performance: a systematic review with meta-analysis. J Strength Cond Res. " +
                "2010;24(1):140-148."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.DELOAD,
            title = "Scarico periodico quando la prestazione cala",
            content = "Una settimana di scarico, con volume ridotto di circa la meta e stesso " +
                "carico, serve a smaltire la fatica accumulata. Va inserita quando le " +
                "prestazioni calano su piu esercizi o quando il dolore articolare e il " +
                "sonno peggiorano, non a calendario fisso per tutti.",
            source = "Coleman M, Burke R, Augustin F, et al. Gaining more from doing less? " +
                "The effects of a one-week deload period during supervised resistance " +
                "training. PeerJ. 2024;12:e16777."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.RECOVERY,
            title = "Sonno e recupero: la variabile che limita il volume utile",
            content = "La restrizione di sonno riduce la prestazione di forza e peggiora il " +
                "bilancio energetico. Prima di aumentare il volume, conviene verificare sonno " +
                "e recupero percepito: il volume che non si recupera non produce adattamento.",
            source = "Craven J, McCartney D, Desbrow B, et al. Effects of acute sleep loss on " +
                "physical performance: a systematic and meta-analytical review. Sports Med. " +
                "2022;52(11):2669-2690."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.CARDIO,
            title = "Cardio e allenamento con i pesi: interferenza contenuta",
            content = "L'allenamento aerobico concomitante puo attenuare l'aumento di forza se " +
                "e molto voluminoso o eseguito subito prima dei pesi. Un volume aerobico " +
                "moderato, collocato in giorni o momenti separati, non compromette in modo " +
                "rilevante l'ipertrofia.",
            source = "Wilson JM, Marin PJ, Rhea MR, et al. Concurrent training: a meta-analysis " +
                "examining interference of aerobic and resistance exercises. J Strength Cond " +
                "Res. 2012;26(8):2293-2307."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.NUTRITION,
            title = "Proteine giornaliere per l'ipertrofia",
            content = "La meta-analisi sulla supplementazione proteica indica che un apporto " +
                "totale di circa 1,6 g per kg di peso corporeo al giorno e sufficiente per " +
                "massimizzare i guadagni di massa nella maggior parte delle persone, con un " +
                "limite superiore di confidenza intorno a 2,2 g per kg al giorno.",
            source = "Morton RW, Murphy KT, McKellar SR, et al. A systematic review, " +
                "meta-analysis and meta-regression of the effect of protein supplementation " +
                "on resistance training-induced gains in muscle mass and strength in healthy " +
                "adults. Br J Sports Med. 2018;52(6):376-384."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.NUTRITION,
            title = "Distribuzione delle proteine nella giornata",
            content = "Distribuire le proteine su piu pasti, con circa 0,4 g per kg di peso per " +
                "pasto su almeno quattro pasti, stimola la sintesi proteica in modo piu " +
                "uniforme rispetto a concentrarle in un unico pasto serale.",
            source = "Mamerow MM, Mettler JA, English KL, et al. Dietary protein distribution " +
                "positively influences 24-h muscle protein synthesis in healthy adults. J " +
                "Nutr. 2014;144(6):876-880."
        ),
        KnowledgeNote(
            topic = KnowledgeTopic.NUTRITION,
            title = "Velocita di perdita di peso per preservare la massa",
            content = "Un deficit calorico moderato, con una perdita di circa lo 0,5-1 percento " +
                "del peso corporeo a settimana, preserva meglio la massa magra rispetto a " +
                "deficit aggressivi, soprattutto se l'apporto proteico e adeguato e il lavoro " +
                "con i pesi continua.",
            source = "Helms ER, Aragon AA, Fitschen PJ. Evidence-based recommendations for " +
                "natural bodybuilding contest preparation: nutrition and supplementation. " +
                "J Int Soc Sports Nutr. 2014;11:20."
        )
    )
}
