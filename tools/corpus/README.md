# Estrazione del corpus Project InVictus

Tre script, in ordine:

1. `taxonomy.py` — le 35 categorie. Una per ogni parametro che il generatore
   deve decidere (split, volume, RIR, recuperi, proteine, ricariche...). Se una
   categoria non serve a generare un piano o una scheda, non sta qui.
2. `extract_corpus.py` — estrae le regole dagli articoli in
   `/tmp/invictus_full.json`. Una frase vale se: contiene un numero con unità di
   misura, parla di allenamento o nutrizione, è prescrittiva (verbo modale o
   cadenza tipo "ogni 3-5 settimane"), non è una ricetta né una tabella.
3. `build_invictus_v2.py` — genera `InvictusKnowledge.kt` dai dati categorizzati.

```bash
cd tools/corpus
python3 extract_corpus.py    # -> /tmp/rules_by_category.json
python3 build_invictus_v2.py # -> app/src/main/java/.../knowledge/InvictusKnowledge.kt
```

## Cosa non fa

Non riempie le lacune. Se la fonte non dice quanto dura uno scarico, la
categoria `DELOAD` resta con due regole. Aggiungere frasi generiche per
pareggiare i conteggi renderebbe il corpus inutile: il modello ha bisogno di
numeri, non di consigli.

Non assegna una frase a una sola categoria. "12-20 serie a settimana in
ricomposizione" dice qualcosa sul volume e sul contesto insieme: sta in
entrambe, e lo strumento filtra per area.

## Verifica

`InvictusKnowledgeCoverageTest` controlla che ogni parametro della scheda e del
piano alimentare abbia almeno una regola, che ogni regola contenga un numero e
una fonte verificabile, e che le due aree restino separate.
