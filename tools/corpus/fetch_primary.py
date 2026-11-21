#!/usr/bin/env python3
"""
Regole da letteratura primaria indicizzata (PubMed), per i parametri che le
fonti divulgative coprono male: deload, sostituzioni, progressione.

Metodo: query mirate per parametro, abstract completo, estrazione delle frasi
che contengono un numero e un esito. Il risultato ha la stessa forma delle
regole divulgative (statement + fonte + url + data), con kind = PEER_REVIEWED.

Nessuna chiave API: E-utilities e' pubblico.
"""
import json, re, time, urllib.request, urllib.parse, os, sys

BASE = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"
OUT = "/tmp/primary_rules.json"

# Una query per parametro scoperto. [tiab] restringe a titolo/abstract.
QUERIES = {
    "DELOAD": [
        "deload[tiab] AND resistance training[tiab]",
        "deloading[tiab] AND (strength[tiab] OR hypertrophy[tiab])",
    ],
    "VOLUME": [
        "weekly set volume[tiab] AND hypertrophy[tiab]",
        "dose-response[tiab] AND resistance training volume[tiab]",
    ],
    "FREQUENCY": [
        "training frequency[tiab] AND hypertrophy[tiab] AND meta-analysis[pt]",
    ],
    "PROGRESSION": [
        "progressive overload[tiab] AND resistance training[tiab]",
        "load progression[tiab] AND strength[tiab]",
    ],
    "PERIODIZATION": [
        "periodization[tiab] AND resistance training[tiab]",
    ],
    "REPS": [
        "repetition range[tiab] AND hypertrophy[tiab]",
        "load[tiab] AND hypertrophy[tiab] AND meta-analysis[pt]",
    ],
    "REST": [
        "rest interval[tiab] AND resistance training[tiab]",
    ],
    "INTENSITY": [
        "repetitions in reserve[tiab] OR RIR[tiab] AND resistance training[tiab]",
        "proximity to failure[tiab] AND hypertrophy[tiab]",
    ],
    "PROTEIN": [
        "protein intake[tiab] AND resistance training[tiab] AND meta-analysis[pt]",
    ],
    "CARB_CYCLING": [
        "carbohydrate[tiab] AND (refeed[tiab] OR cyclical[tiab]) AND diet[tiab]",
    ],
    "SUBSTITUTION": [
        "food substitution[tiab] AND diet[tiab] AND adherence[tiab]",
    ],
    "ADHERENCE": [
        "diet adherence[tiab] AND (flexible[tiab] OR rigid[tiab])",
    ],
}

# Frasi utili: numero + esito misurabile.
NUM = re.compile(r"\d+(?:\.\d+)?\s*(?:%|kg|sets?|reps?|weeks?|days?|g/kg|g\b|hours?|min)", re.I)
OUTCOME = re.compile(
    r"hypertroph|strength|muscle (?:mass|size|thickness)|fat mass|body mass|"
    r"gains?|increases?|decreases?|reductions?|adherence|performance|"
    r"effect size|SMD|percentage", re.I)
# Rumore: metodologia, non risultato.
NOISE = re.compile(
    r"PubMed|MEDLINE|Embase|search(?:ed)? (?:was|were)|inclusion criteria|"
    r"eligibility|randomi[sz]ed|participants were|were assigned|"
    r"statistical|analys(?:is|es) (?:was|were)|software|p\s*[<=]|"
    r"confidence interval|I2\b|heterogeneity|"
    # Descrizione del metodo, non del risultato: "PURPOSE: to use an artificial
    # neural network", "a 55-item survey was distributed", "assessments were
    # conducted 2 to 4 times yearly". Sono cose che riguardano lo studio, non
    # l'allenamento di chi legge.
    r"^\s*(?:PURPOSE|OBJECTIVE|BACKGROUND|METHODS?|DESIGN|SETTING|CONCLUSION)\s*:|"
    r"survey was|questionnaire was|were (?:distributed|recruited|measured|assessed|"
    r"conducted|allocated|randomi[sz]ed)|"
    r"(?:assessments?|testing|measurements?)\s+(?:was|were)\s+conducted|"
    r"neural network|machine learning|regression model|"
    r"\b(?:n\s*=\s*\d+)\b", re.I)


def get(url):
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=45) as r:
                return r.read().decode("utf-8", "replace")
        except Exception as e:
            if attempt == 2:
                print(f"  ! errore: {e}", file=sys.stderr)
                return ""
            time.sleep(2)


def esearch(term, retmax=8):
    q = urllib.parse.quote(term)
    raw = get(f"{BASE}/esearch.fcgi?db=pubmed&term={q}&retmax={retmax}&retmode=json&sort=relevance")
    try:
        return json.loads(raw)["esearchresult"]["idlist"]
    except Exception:
        return []


def efetch(ids):
    if not ids:
        return ""
    return get(f"{BASE}/efetch.fcgi?db=pubmed&id={','.join(ids)}&retmode=text&rettype=abstract")


def esummary(ids):
    """Metadati strutturati: titolo, rivista, anno. Niente parsing euristico."""
    if not ids:
        return {}
    raw = get(f"{BASE}/esummary.fcgi?db=pubmed&id={','.join(ids)}&retmode=json")
    try:
        result = json.loads(raw)["result"]
    except Exception:
        return {}
    out = {}
    for pmid in result.get("uids", []):
        r = result.get(pmid, {})
        doi = next((x["value"] for x in r.get("articleids", []) if x.get("idtype") == "doi"), "")
        out[pmid] = {
            "title": re.sub(r"<[^>]+>", "", r.get("title", "")).strip().rstrip("."),
            "journal": r.get("fulljournalname") or r.get("source", ""),
            "year": (r.get("pubdate") or "")[:4],
            "doi": doi,
        }
    return out


def parse_records(text):
    """Divide l'output testuale di efetch in record."""
    records = []
    for block in re.split(r"\n(?=\d+\.\s)", text):
        block = block.strip()
        if not block:
            continue
        lines = [l.rstrip() for l in block.split("\n")]
        pmid = re.search(r"PMID:\s*(\d+)", block)
        doi = re.search(r"doi:\s*(10\.[^\s.]+(?:\.[^\s.]+)*)", block)
        title_parts = []
        for line in lines[1:]:
            if not line.strip():
                break
            if re.match(r"^(?:[A-Z][a-z]+ )?\d{4}", line):
                break
            title_parts.append(line.strip())
        title = re.sub(r"\s+", " ", " ".join(title_parts)).strip(" .")
        journal = re.search(r"^1?\.?\s*([A-Z][^.]+)\.\s*(\d{4})", block)
        year = re.search(r"\b(19|20)\d{2}\b", block)
        abstract = block
        records.append({
            "pmid": pmid.group(1) if pmid else "",
            "doi": doi.group(1) if doi else "",
            "title": title,
            "journal": journal.group(1).strip() if journal else "",
            "year": year.group(0) if year else "",
            "text": abstract,
        })
    return records


def sentences(text):
    out = []
    for s in re.split(r"(?<=[.!?])\s+", text):
        s = re.sub(r"\s+", " ", s).strip()
        if not (60 <= len(s) <= 320):
            continue
        out.append(s)
    return out


def main():
    result = {}
    seen = set()

    for category, queries in QUERIES.items():
        rules = []
        for q in queries:
            ids = esearch(q)
            if not ids:
                continue
            text = efetch(ids)
            meta = esummary(ids)
            for rec in parse_records(text):
                if not rec["pmid"]:
                    continue
                m = meta.get(rec["pmid"])
                if not m or not m["title"]:
                    continue
                rec.update(m)
                for s in sentences(rec["text"]):
                    if not NUM.search(s) or not OUTCOME.search(s):
                        continue
                    if NOISE.search(s):
                        continue
                    key = re.sub(r"\W+", "", s.lower())[:100]
                    if key in seen:
                        continue
                    seen.add(key)
                    url = (f"https://pubmed.ncbi.nlm.nih.gov/{rec['pmid']}/")
                    rules.append({
                        "statement": s,
                        "articleTitle": rec["title"][:200],
                        "url": url,
                        "publishedDate": rec["year"],
                        "journal": rec["journal"],
                        "doi": rec["doi"],
                    })
            time.sleep(0.4)

        # Un articolo non deve dominare.
        per = {}
        kept = []
        for r in rules:
            n = per.get(r["articleTitle"], 0)
            if n >= 3:
                continue
            per[r["articleTitle"]] = n + 1
            kept.append(r)
        result[category] = kept

    json.dump(result, open(OUT, "w"), ensure_ascii=False, indent=1)
    print(f"{'CATEGORIA':16s} {'regole':>6s} {'fonti':>6s}")
    print("-" * 32)
    for cat, rs in result.items():
        print(f"{cat:16s} {len(rs):6d} {len({r['articleTitle'] for r in rs}):6d}")
    print("-" * 32)
    print(f"{'TOTALE':16s} {sum(len(v) for v in result.values()):6d}")


if __name__ == "__main__":
    main()
