#!/usr/bin/env python3
"""
Estrae dal corpus di articoli le regole per categoria, una categoria per
parametro che il generatore deve decidere.

Criterio per una frase valida:
  - prescrittiva (dice cosa fare, non cosa e' accaduto)
  - contiene un numero con unita' di misura riconoscibile
  - parla di allenamento o nutrizione, non di un singolo alimento o ricetta
  - sta in un articolo pertinente alla categoria

L'output e' JSON con fonte (titolo + URL + data) per ogni regola.
"""
import json, re, sys, os
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import taxonomy

ARTS = "/tmp/invictus_full.json"

# ---------------------------------------------------------------- pattern
NUM = re.compile(r"\d+\s*(?:%|g/kg|g\b|kcal|kg|litri|ml|mg|ore|ora|giorni|giorno|"
                 r"settiman[ae]|volte|serie|ripetizioni|minuti|min|secondi|sec|"
                 r"x\s*\d|cm|mesi|mesocicl|blocch)", re.I)

# Deve parlare di allenamento o nutrizione.
CTX = re.compile(r"serie|ripetizioni|carico|recuper|allenament|palestra|esercizi|"
                 r"calorie|kcal|proteine|carboidrat|grassi|pasto|dieta|aliment|"
                 r"peso|massa|grasso|volume|frequenza|sedut|mesocicl|settiman", re.I)

# Rumore: ricette, singoli alimenti, tabelle scheda, somatotipi.
NOISE = re.compile(r"ricetta|ingredienti|cucchiaio|forno|padella|lessa|frulla|"
                   r"questa pianta|questa bacca|sapore|profum|croccante|"
                   r"100 g di \w+ contiene|un etto|porzione da|"
                   r"somatotipo|ectomorf|mesomorf|endomorf|"
                   r"Serie\s+Ripetizioni|Ripetizioni\s+Recupero|"
                   r"clikka|clicca|iscriviti|abbonament|corso online|"
                   r"costo|prezzo|sconto|offerta|€|eur\b", re.I)

# Prescrizione: verbo o forma che indica una regola.
PRESC = re.compile(r"\b(dovrebbe|dovrebbero|consigliat|si consiglia|conviene|"
                   r"mantenere|evitare|minimo|massimo|non superare|ideale|ottimale|"
                   r"range|apporto|fabbisogno|assumere|limitare|aumentare|ridurre|"
                   r"almeno|fino a|tra i|tra le|partire da|si parte|prevede|"
                   r"e' possibile|puoi|puo|bisogna|occorre|serve|utile|"
                   r"programmare|inserire|eseguire|tenere|considerare)\b", re.I)

# Cadenza o intervallo: "una settimana ogni 3-5", "2-3 volte a settimana",
# "dal 70 all'80%". Sono regole anche senza verbo modale.
CADENCE = re.compile(
    r"\bogni\s+\d|\d+\s*(?:-|a|\/)\s*\d+\s*(?:volte|settiman|giorni|serie|ripetizioni|minuti|ore|%)|"
    r"\d+\s*%\s*(?:di|del|dell)|\d+\s*(?:volte)\s*(?:a|alla)\s*settiman|"
    r"una\s+volta\s+ogni|ogni\s+\d+\s*-\s*\d+|"
    r"da\s+\d+\s+a\s+\d+|tra\s+\d+\s+e\s+\d+", re.I)

# ---------------------------------------------------------------- keywords
KW = {
 "SPLIT": r"\bsplit\b|upper.?lower|push.?pull|full.?body|abcd|abed|monofrequenza|multifrequenza|divisione",
 "FREQUENCY": r"frequenza|volte a settimana|2x|3x|due volte|tre volte|sessioni settimanali",
 "EXERCISE_SELECT": r"scegliere l'esercizio|scelta dell'esercizio|ordine degli esercizi|"
                    r"prima gli esercizi|prima i multiarticolari|priorit[aà] muscolare|"
                    r"esercizi fondamentali|esercizi di isolamento|complementari",
 "VOLUME": r"serie settimanal|volume settimanal|volume di allenamento|numero di serie|serie totali|serie per gruppo|volume",
 "INTENSITY": r"\bRIR\b|\bRPE\b|intensit[aà]|a cedimento|vicino al cedimento|sforzo percepito",
 "REPS": r"range di ripetizioni|rep range|\d+\s*(?:-|a|–)\s*\d+\s*ripetizion|"
        r"tra \d+ e \d+ ripetizion|dalle \d+ alle \d+ ripetizion|"
        r"serie da \d+ ripetizion|\d+ ripetizioni per serie|per \d+-\d+ ripetizion",
 "REST": r"recuper[oi]|pausa tra le serie|secondi di pausa|minuti di recupero",
 "PROGRESSION": r"progressive overload|sovraccarico|progressione|aumentare il carico|double progression|aumento del carico|progredire",
 "PERIODIZATION": r"periodizzazione|mesociclo|macrociclo|microciclo|blocco di allenamento|fase di",
 "DELOAD": r"deload|scarico|settimana di scarico|alleggerire",
 "FATIGUE": r"affaticamento|fatica|overtraining|recupero insufficiente|stallo|stagnazione|sovrallenamento",
 "TECHNIQUE": r"drop ?set|rest ?pause|superserie|giant ?set|stripping|myo.?reps|cluster|piramidale|cheating|negative|isometri",
 "EXECUTION": r"esecuzione|tecnica corretta|errori da evitare|range di movimento|\bROM\b",
 "WARMUP": r"riscaldamento|warm.?up|serie di avvicinamento|ramping|"
            r"pre.?riscaldamento|fase 0|mobilizzazione",
 "CARDIO": r"cardio|attivit[aà] aerobica|\bLISS\b|\bHIIT\b|passi giornalieri|\bNEAT\b",
 "LEVEL": r"principiante|intermedio|avanzat[oi]|anni di allenamento",
 "GOAL_TRAIN": r"ipertrofia|forza massimale|massa muscolare|definizione|ricomposizione",
 "SAFETY_TRAIN": r"dolore|infortuni|controindicazion|sicurezza|articolazion|patolog|tecnica scorretta",
 "CALORIES": r"calorie|kcal|deficit|surplus|bilancio energetico|fabbisogno calorico",
 "METABOLISM": r"metabolismo basale|\bBMR\b|\bTDEE\b|dispendio|termogenesi|\bNEAT\b|metabolismo",
 "PROTEIN": r"proteine|proteic[oi]|g/kg|amminoacid",
 "CARBS": r"carboidrat|glucid|glicogen|zuccher",
 "FAT": r"\bgrass[io]\b|lipid|omega|saturi|insaturi",
 "FIBER": r"fibra|fibre|vegetal|verdur|frutta",
 "MEALS": r"pasti|colazione|pranzo|cena|spuntino|frequenza dei pasti|numero di pasti",
 "TIMING": r"timing|pre.?workout|post.?workout|finestra anabolica|prima di dormire|distribuzione",
 "CARB_CYCLING": r"ciclicit[aà]|carb cycling|ricarica|refeed|cheat meal|"
                r"giornat[ae] (?:high|low|medio)|high carb|low carb|"
                r"giorni? (?:on|off)|ricomposizione",
 "HYDRATION": r"acqua|idratazione|litri|bere|disidratazione",
 "ALCOHOL": r"alcol|alcolic|birra|vino|superalcolic",
 "SUBSTITUTION": r"sostituz|alternativ|equivalenz|scambio|flessibilit|"
                r"alimento equivalente|puoi scambiare|in alternativa",
 "ADHERENCE": r"aderenza|sostenibilit|dieta flessibile|\bIIFYM\b|abbuffat|dieta rigida|"
              r"dieta deve essere|regime alimentare|mantenere nel tempo|lungo periodo",
 "WEIGHT": r"peso corporeo|media settimanale|bilancia|oscillazion|pesarsi|variazione di peso",
 "BODYCOMP": r"massa grassa|percentuale di grasso|composizione corporea|plicometr|bioimpedenz",
 "SUPPLEMENT": r"creatina|caffeina|beta.?alanina|omega 3|vitamina d|integratore|whey|proteine in polvere",
 "SAFETY_NUT": r"allergi|intolleranz|celiac|diabet|patolog|controindicazion|"
               r"disturbo alimentare|reazione avversa|lattosio|glutine|frutta a guscio",
}
KW = {k: re.compile(v, re.I) for k, v in KW.items()}

# Quanto una categoria e' specifica: a parita' di match vince la piu' alta.
# Le categorie di contesto (LEVEL, GOAL_TRAIN) perdono contro quelle di parametro.
SPECIFICITY = {
    "TECHNIQUE": 10, "CARB_CYCLING": 10, "DELOAD": 10, "PERIODIZATION": 9,
    "PROGRESSION": 9, "VOLUME": 8, "INTENSITY": 8, "REPS": 8, "REST": 8,
    "SPLIT": 8, "FREQUENCY": 8, "EXERCISE_SELECT": 8, "WARMUP": 8,
    "PROTEIN": 8, "CARBS": 8, "FAT": 8, "FIBER": 8, "CALORIES": 8,
    "METABOLISM": 8, "MEALS": 8, "TIMING": 8, "HYDRATION": 8, "ALCOHOL": 8,
    "WEIGHT": 8, "BODYCOMP": 8, "SUPPLEMENT": 8, "SUBSTITUTION": 8,
    "ADHERENCE": 7, "EXECUTION": 6, "FATIGUE": 6, "CARDIO": 6,
    "SAFETY_TRAIN": 6, "SAFETY_NUT": 6, "LEVEL": 4, "GOAL_TRAIN": 3,
}

# Parole che tradiscono la categoria vera: se compaiono, la frase appartiene
# a quell'area e non a quella dell'articolo che la contiene.
DOMINANT = [
    ("FAT",         re.compile(r"\bgrass[io]\b|lipid|omega.?3|saturi", re.I)),
    ("PROTEIN",     re.compile(r"proteine|proteic[oi]|g/kg", re.I)),
    ("CARBS",       re.compile(r"carboidrat|glucid|glicogen", re.I)),
    ("CALORIES",    re.compile(r"kcal|calorie|deficit|surplus", re.I)),
    ("VOLUME",      re.compile(r"serie (?:a|per) settiman|volume settimanal|serie allenanti", re.I)),
    ("REST",        re.compile(r"recuper[oi]|pausa tra le serie|secondi di pausa", re.I)),
    ("INTENSITY",   re.compile(r"\bRIR\b|\bRPE\b|cedimento", re.I)),
    ("REPS",        re.compile(r"\d+\s*(?:-|a)\s*\d+\s*ripetizion|ripetizioni per serie", re.I)),
    ("FREQUENCY",   re.compile(r"volte a settimana|frequenza|2x|3x", re.I)),
    ("HYDRATION",   re.compile(r"acqua|idratazione|litri|bere", re.I)),
    ("TECHNIQUE",   re.compile(r"drop ?set|rest ?pause|superserie|giant ?set|stripping|piramidale", re.I)),
    ("DELOAD",      re.compile(r"scarico|deload", re.I)),
    ("CARB_CYCLING",re.compile(r"ricarica|refeed|ciclicit|giornat[ae] (?:high|low)", re.I)),
]

# Articoli da escludere del tutto: singolo alimento, dieta di moda, patologia.
BAD_TITLE = re.compile(
    r"valori nutrizionali|ricetta|proprietà e benefici|quante calorie ha|quante calorie|"
    r"dieta militare|dieta del |dieta dukan|dieta lampo|dieta dei |"
    r"collagene|topinambur|avena|quinoa|farro|orzo|miele|cioccolat|gelato|"
    r"quanto ne sai|curiosità|classifica|storia del bodybuilding|"
    r"celiachia|diabete|colesterolo|ipertensione|anemia|tiroid|"
    r"cellulite|ritenzione|stretch|mobilità|yoga|pilates", re.I)


def sentences(text):
    """Frasi con abbastanza contesto per stare in piedi da sole."""
    out = []
    for s in re.split(r"(?<=[.!?])\s+", text):
        s = s.strip()
        if not (55 <= len(s) <= 300):
            continue
        if s.endswith("?"):
            continue
        if re.match(r"^[a-zàèéìòù]", s):          # frammento di elenco
            continue
        if s.count("/") >= 2 or s.count("|") >= 1:
            continue
        if len(re.findall(r"\d", s)) > 7:          # tabella travestita
            continue
        out.append(s)
    return out


def valid(sentence, rx):
    if not NUM.search(sentence):
        return False
    if not CTX.search(sentence):
        return False
    if NOISE.search(sentence):
        return False
    if not (PRESC.search(sentence) or CADENCE.search(sentence)):
        return False
    if not rx.search(sentence):
        return False
    return True


def normalize(s):
    s = re.sub(r"\s+", " ", s).strip()
    return s


def main():
    arts = json.load(open(ARTS))
    by_cat = defaultdict(list)
    seen = defaultdict(set)

    for a in arts:
        if BAD_TITLE.search(a["title"]):
            continue
        sents = sentences(a["text"])
        if not sents:
            continue
        # Categorie pertinenti a questo articolo.
        candidates = [
            code for code, rx in KW.items()
            if rx.search(a["title"]) or len(rx.findall(a["text"])) >= 3
        ]
        if not candidates:
            continue

        for s in sents:
            # Una frase puo' informare piu' parametri: "12-20 serie a settimana
            # in ricomposizione" dice qualcosa sul volume E sul contesto. La si
            # tiene in entrambe, perche' lo strumento filtra per area.
            for code in candidates:
                if not valid(s, KW[code]):
                    continue
                key = re.sub(r"\W+", "", s.lower())[:110]
                if key in seen[code]:
                    continue
                seen[code].add(key)
                by_cat[code].append({
                    "statement": normalize(s),
                    "articleTitle": a["title"],
                    "url": a["url"],
                    "publishedDate": a["date"],
                })

    out = {}
    for code, rules in by_cat.items():
        # Un articolo non deve dominare la categoria: il tetto per fonte evita
        # che un solo testo riempia una sezione. Se pero' le fonti sono poche,
        # il tetto strozza la categoria: in quel caso lo si alza.
        fonts = len({r["articleTitle"] for r in rules})
        cap = 4 if fonts >= 5 else (8 if fonts >= 3 else 12)
        per_article = defaultdict(int)
        kept = []
        for r in rules:
            if per_article[r["articleTitle"]] >= cap:
                continue
            per_article[r["articleTitle"]] += 1
            kept.append(r)
        out[code] = kept

    json.dump(out, open("/tmp/rules_by_category.json", "w"), ensure_ascii=False, indent=1)

    total = sum(len(v) for v in out.values())
    print(f"{'CATEGORIA':18s} {'regole':>6s}  {'fonti':>5s}")
    print("-" * 36)
    weak = []
    for code in taxonomy.ALL:
        rules = out.get(code, [])
        fonts = len({r["articleTitle"] for r in rules})
        mark = "" if len(rules) >= 5 else "   <-- DA RINFORZARE"
        if len(rules) < 5:
            weak.append(code)
        print(f"{code:18s} {len(rules):6d}  {fonts:5d}{mark}")
    print("-" * 36)
    print(f"{'TOTALE':18s} {total:6d}")
    if weak:
        print(f"\nCategorie sotto 5 regole: {', '.join(weak)}")


if __name__ == "__main__":
    main()
