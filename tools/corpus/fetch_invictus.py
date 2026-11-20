import json, urllib.request, re, html, time, os

BASE = "https://www.projectinvictus.it/wp-json/wp/v2"
UA = {"User-Agent": "FitCoach-corpus-builder/1.0 (educational; contact via repo)"}
CATS = {
    234: "metodi-e-tecniche", 233: "bodybuilding", 236: "scheda-palestra",
    222: "per-la-massa-muscolare", 218: "ricomposizione-corporea",
    155: "esercizi-palestra", 86: "diete", 41: "macronutrienti",
    39: "metodi-e-tecniche-2", 159: "allenamento",
}

def get(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=45) as r:
        return json.loads(r.read().decode("utf-8", "ignore"))

def clean(h):
    t = html.unescape(re.sub(r"<[^>]+>", " ", h))
    return re.sub(r"\s+", " ", t).strip()

out = []
for cid, slug in CATS.items():
    for page in range(1, 4):
        try:
            posts = get(f"{BASE}/posts?categories={cid}&per_page=100&page={page}&_fields=id,title,link,content,date")
        except Exception as e:
            break
        if not posts:
            break
        for p in posts:
            body = clean(p["content"]["rendered"])
            if len(body) < 800:
                continue
            out.append({
                "id": p["id"], "cat": slug,
                "title": clean(p["title"]["rendered"]),
                "url": p["link"], "date": p.get("date", ""),
                "text": body,
            })
        time.sleep(0.4)

seen, uniq = set(), []
for a in out:
    if a["id"] in seen: continue
    seen.add(a["id"]); uniq.append(a)

json.dump(uniq, open("/tmp/invictus_raw.json", "w"), ensure_ascii=False)
print("articoli:", len(uniq), "| caratteri totali:", sum(len(a["text"]) for a in uniq))
