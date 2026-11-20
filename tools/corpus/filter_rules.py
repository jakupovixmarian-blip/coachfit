import json, re
from collections import Counter

c = json.load(open("/tmp/claims_v2.json"))

# Rumore residuo: frasi su singoli alimenti, somatotipi, esperienze personali.
NOISE = re.compile(
    r"fornisce ben|una porzione di|somatotip|mesomorf|endomorf|ectomorf|"
    r"la mia vita|facevo ancora|ho fatto|nel mio caso|personalmente|"
    r"bevanda|500 ml|formaggi grassi|mirtill|fave|banana|pollo|"
    r"ricetta|ingredienti|cottura|forno|padella", re.I)

# Frasi troppo vaghe per diventare una regola.
VAGUE = re.compile(r"^(è|e) (importante|fondamentale|essenziale)|^ricorda|^attenzione", re.I)

clean = []
seen = set()
for x in c:
    s = x["statement"]
    if NOISE.search(s) or VAGUE.search(s): continue
    if not re.search(r"\d", s): continue
    # almeno un numero con unita' di misura utile
    if not re.search(r"\d+\s*(%|g/kg|g\b|kcal|kg|serie|ripetizioni|minuti|secondi|′|'|settiman|giorni|volte)", s, re.I):
        continue
    key = re.sub(r"\W+", "", s.lower())[:90]
    if key in seen: continue
    seen.add(key)
    clean.append(x)

print("regole pulite:", len(clean))
print(Counter(x["topic"] for x in clean).most_common())
json.dump(clean, open("/tmp/claims_final.json","w"), ensure_ascii=False, indent=1)
