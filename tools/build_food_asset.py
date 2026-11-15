#!/usr/bin/env python3
"""Rigenera l'asset del database alimentare interno di FitCoach.

Scarica il dataset **USDA FoodData Central – SR Legacy** (pubblico dominio,
~7.800 alimenti con valori nutrizionali verificati) e produce il file
`app/src/main/assets/food_usda_sr_legacy.tsv` importato dall'app al primo avvio.

Formato di output (TSV, header incluso):
    name, category, kcal, protein, carbs, fat, fiber, sourceId

I valori sono per 100 g di parte edibile. La categoria e mappata in italiano.

Uso:
    python3 tools/build_food_asset.py

Fonte: https://fdc.nal.usda.gov/download-datasets.html (SR Legacy, aprile 2018).
Licenza: USDA FoodData Central e di pubblico dominio (US Government work).
"""
import csv
import os
import sys
import urllib.request
import zipfile
import io

USDA_URL = (
    "https://fdc.nal.usda.gov/fdc-datasets/"
    "FoodData_Central_sr_legacy_food_csv_2018-04.zip"
)
ASSET_OUT = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "food_usda_sr_legacy.tsv"
)

CATEGORIES = {
    "1": "Latticini", "2": "Spezie", "3": "Alimenti per l'infanzia", "4": "Grassi",
    "5": "Carni", "6": "Salse e condimenti", "7": "Salumi", "8": "Cereali",
    "9": "Frutta", "10": "Carni", "11": "Verdura", "12": "Frutta secca",
    "13": "Carni", "14": "Bevande", "15": "Pesce", "16": "Legumi", "17": "Carni",
    "18": "Cereali", "19": "Dolci", "20": "Cereali", "21": "Fast food",
    "22": "Piatti pronti", "23": "Snack", "24": "Altro", "25": "Ristorazione",
    "26": "Prodotti di marca", "27": "Altro", "28": "Bevande alcoliche",
}
NUTRIENTS = {1003: "p", 1005: "c", 1004: "f", 1008: "k", 1079: "fib"}


def clean(value: str) -> str:
    return value.replace("\t", " ").replace("\n", " ").replace("\r", " ").strip()


def main() -> int:
    print(f"Download: {USDA_URL}")
    with urllib.request.urlopen(USDA_URL, timeout=180) as resp:
        blob = resp.read()
    print(f"Scaricati {len(blob) / 1_000_000:.1f} MB")
    zf = zipfile.ZipFile(io.BytesIO(blob))
    root = zf.namelist()[0].split("/")[0]

    def open_csv(name):
        return io.TextIOWrapper(zf.open(f"{root}/{name}"), encoding="utf-8")

    cat = {}
    with open_csv("food.csv") as f:
        for row in csv.DictReader(f):
            cat[row["fdc_id"]] = row.get("food_category_id") or ""

    nut = {}
    with open_csv("food_nutrient.csv") as f:
        for row in csv.DictReader(f):
            nid = int(row["nutrient_id"])
            if nid in NUTRIENTS:
                try:
                    nut.setdefault(row["fdc_id"], {})[NUTRIENTS[nid]] = float(row["amount"])
                except ValueError:
                    pass

    os.makedirs(os.path.dirname(ASSET_OUT), exist_ok=True)
    written = 0
    with open(ASSET_OUT, "w", encoding="utf-8") as w:
        w.write("name\tcategory\tkcal\tprotein\tcarbs\tfat\tfiber\tsourceId\n")
        with open_csv("food.csv") as f:
            for row in csv.DictReader(f):
                d = nut.get(row["fdc_id"], {})
                if not all(x in d for x in ("p", "c", "f", "k")):
                    continue
                name = clean(row["description"])
                if not name:
                    continue
                category = CATEGORIES.get(cat.get(row["fdc_id"], ""), "Altro")
                w.write(
                    f"{name}\t{category}\t{d['k']:.1f}\t{d['p']:.1f}\t{d['c']:.1f}"
                    f"\t{d['f']:.1f}\t{d.get('fib', 0.0):.1f}\t{row['fdc_id']}\n"
                )
                written += 1
    print(f"Scritti {written} alimenti in {ASSET_OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
