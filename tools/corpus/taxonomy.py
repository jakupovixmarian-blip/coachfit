"""
Tassonomia del corpus: una categoria per ogni parametro che il generatore
deve decidere. Se una categoria non serve a generare un piano alimentare o una
scheda, non sta qui.
"""

TRAINING = {
    # --- Struttura della scheda ---
    "SPLIT":          "Split e scelta della divisione",
    "FREQUENCY":      "Frequenza per gruppo muscolare",
    "EXERCISE_SELECT":"Selezione e ordine degli esercizi",
    # --- Volume e intensita ---
    "VOLUME":         "Volume settimanale e per seduta",
    "INTENSITY":      "Intensita, RIR e RPE",
    "REPS":           "Range di ripetizioni",
    "REST":           "Recuperi tra serie",
    # --- Progressione e cicli ---
    "PROGRESSION":    "Progressive overload",
    "PERIODIZATION":  "Periodizzazione e mesocicli",
    "DELOAD":         "Deload e scarico",
    "FATIGUE":        "Gestione dell'affaticamento",
    # --- Tecniche e qualita ---
    "TECHNIQUE":      "Tecniche d'intensita",
    "EXECUTION":      "Esecuzione e tecnica",
    "WARMUP":         "Riscaldamento",
    "CARDIO":         "Cardio e attivita accessoria",
    # --- Contesto ---
    "LEVEL":          "Differenze per livello di esperienza",
    "GOAL_TRAIN":     "Obiettivi di allenamento",
    "SAFETY_TRAIN":   "Sicurezza e dolore",
}

NUTRITION = {
    # --- Energia ---
    "CALORIES":       "Calorie, deficit e surplus",
    "METABOLISM":     "Metabolismo e dispendio",
    # --- Macronutrienti ---
    "PROTEIN":        "Proteine",
    "CARBS":          "Carboidrati",
    "FAT":            "Grassi",
    "FIBER":          "Fibre",
    # --- Organizzazione ---
    "MEALS":          "Numero e composizione dei pasti",
    "TIMING":         "Timing e distribuzione",
    "CARB_CYCLING":   "Ciclicita dei carboidrati e ricariche",
    # --- Pratica ---
    "HYDRATION":      "Idratazione",
    "ALCOHOL":        "Alcol",
    "SUBSTITUTION":   "Sostituzioni e flessibilita",
    "ADHERENCE":      "Aderenza e sostenibilita",
    # --- Misurazione ---
    "WEIGHT":         "Peso e sua lettura",
    "BODYCOMP":       "Composizione corporea",
    # --- Contesto ---
    "SUPPLEMENT":     "Integratori",
    "SAFETY_NUT":     "Sicurezza, allergie, patologie",
}

ALL = {**{k: ("allenamento", v) for k, v in TRAINING.items()},
       **{k: ("nutrizione", v) for k, v in NUTRITION.items()}}

def area_of(code: str) -> str:
    return ALL[code][0]

def label_of(code: str) -> str:
    return ALL[code][1]
