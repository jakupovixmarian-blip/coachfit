import json, re
from collections import Counter

arts = json.load(open("/tmp/invictus_full.json"))

# Solo contenuti metodologici: fuori enciclopedie alimentari, patologie, ricette.
KEEP = {
    "metodi-e-tecniche":3, "bodybuilding":3, "scheda-palestra":3,
    "per-la-massa-muscolare":3, "ricomposizione-corporea":3,
    "allenamento":3, "allenamento-della-forza":2, "forza":2,
    "programmi-corpo-libero":2, "esercizi-palestra":2, "addominali":2,
    "diete":2, "macronutrienti":2, "dimagrire":2, "nutrizione":2,
    "calisthenics":1, "corpo-libero":1, "anatomia":1, "femminile":1,
}
DROP_TITLE = re.compile(
    r"valori nutrizionali|calorie e proprietà|proprietà e benefici|ricetta|"
    r"quanto ne sai|curiosità|storia del|chi era|classifica|migliori proteine|"
    r"dove si trova|in quali cibi", re.I)

TOPICS = [
    ("volume",        r"\bserie\b.*\b(gruppo|settimana|seduta|esercizio)|\bvolume\b", 3),
    ("frequency",     r"frequenza|volte a settimana|monofrequenza|multifrequenza", 3),
    ("recovery",      r"recuper[oi]|pausa tra|secondi tra le serie|minuti tra", 3),
    ("intensity",     r"\bRIR\b|\bRPE\b|cedimento|buffer", 3),
    ("periodization", r"mesociclo|macrociclo|periodizzazione|scarico|deload|blocco di", 3),
    ("progression",   r"progressione|sovraccarico|aumentare il carico|incremento di carico", 3),
    ("techniques",    r"drop ?set|superserie|giant set|stripping|rest ?pause|myo.?reps|cluster", 2),
    ("exercise",      r"esecuzione|tecnica|ROM|range di movimento|scapol", 2),
    ("protein",       r"proteine|proteic[oi]", 3),
    ("carbs",         r"carboidrat|glicogen", 3),
    ("fat",           r"\bgrass[io]\b|\blipid", 3),
    ("calories",      r"calorie|kcal|deficit|surplus|bilancio energetico|mantenimento", 3),
    ("timing",        r"pre.?workout|post.?workout|intra.?workout|finestra anabolica", 2),
    ("bodycomp",      r"grasso corporeo|massa grassa|BF%|plicometr", 2),
    ("safety",        r"sovrallenamento|overtraining|infortuni|dolore|controindicazion", 2),
]

# Segnali che la frase e' una prescrizione, non una descrizione.
PRESC = re.compile(
    r"\b(dovrebbe|dovrebbero|consigli|è meglio|e meglio|si consiglia|conviene|"
    r"mantenere|evitare|minimo|massimo|non superare|ideale|ottimale|range|"
    r"tra i|tra le|da \d|circa|almeno|fino a|limite)\b", re.I)
# Righe di tabella o dump: molti numeri, barre, intestazioni di scheda.
TABLE = re.compile(r"Serie\s+Ripetizioni|Ripetizioni\s+Recupero|\d\s*/\s*\d\s*/\s*\d")
NUM = re.compile(r"\d")
UNIT = re.compile(r"\d+\s*(%|g/kg|g\b|kcal|kg|serie|ripetizioni|minuti|secondi|ore|settiman|giorni|volte)", re.I)

def ok(s):
    if not (50 <= len(s) <= 300): return False
    if TABLE.search(s): return False
    if s.count("/") >= 2: return False
    if len(re.findall(r"\d", s)) > 6: return False
    if not NUM.search(s): return False
    if not PRESC.search(s): return False
    if not UNIT.search(s): return False
    if s.count(",") > 8: return False
    return True

seen, out = set(), []
for a in arts:
    w = max([KEEP.get(c, 0) for c in a["cats"]] or [0])
    if w == 0 or DROP_TITLE.search(a["title"]): continue
    for s in re.split(r"(?<=[.!?])\s+", a["text"]):
        s = s.strip()
        if not ok(s): continue
        low = s.lower()
        best = None
        for topic, pat, prio in TOPICS:
            if re.search(pat, low, re.I):
                if best is None or prio > best[1]: best = (topic, prio)
        if not best: continue
        key = re.sub(r"\W+", "", low)[:100]
        if key in seen: continue
        seen.add(key)
        out.append({"topic":best[0], "score":best[1]*w, "statement":s,
                    "title":a["title"], "url":a["url"], "date":a["date"]})

out.sort(key=lambda x:(-x["score"], x["topic"]))
json.dump(out, open("/tmp/claims_v2.json","w"), ensure_ascii=False, indent=1)
print("regole:", len(out))
print(Counter(x["topic"] for x in out).most_common())
