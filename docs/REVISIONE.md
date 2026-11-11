# FitCoach – Report tecnico di revisione (analisi dello stato attuale)

Ambito: revisione scientifica e funzionale dei motori esistenti
(`app/src/main/java/com/fitcoach/app/`).

## 1. Architettura rilevata

- **3 layer**: `data` (Room, DataStore, Retrofit), `domain` (modelli + motori puri),
  `ui` (Compose + ViewModel).
- **DI**: Hilt. **Persistenza**: Room (23 entità), schema v1, `fallbackToDestructiveMigration`.
- **Motori**: `EnergyCalculator`, `CarbCyclingEngine`, `MealPlanGenerator`,
  `SplitGenerator`, `WorkoutPlanBuilder`, `PeriodizationEngine`,
  `ProgressiveOverloadEngine`, `FatigueManager`, `AdaptiveEngine`, `WeightAnalyzer`.
- **Coach AI**: `CoachContext`, `CoachPromptBuilder`, `CoachService`, `CoachActionParser`,
  `CoachActionExecutor`.
- **Test esistenti**: 67 test sui motori (7 classi).

## 2. Carenze individuate (con evidenza nel codice)

### 2.1 Anamnesi incompleta e in parte ignorata

`UserProfile` contiene SOLO: nome, età, sesso, altezza, peso, % grasso, esperienza,
obiettivo, giorni/settimana, attrezzatura, preferenze alimentari, alimenti esclusi,
allergie, limitazioni, movimenti da evitare, budget, override, pasti/giorno.

**Mancano del tutto**: stile di vita (tipo di lavoro, ore lavorative, turni, ore seduto,
passi, mezzo di trasporto, sport aggiuntivi, camminate), recupero (ore/qualità sonno,
orari sonno-veglia, stress, energia, idratazione, caffeina, alcol), preferenze alimentari
operative (numero pasti 4/6, orari, cibi graditi/non graditi, tempo di cucina, meal prep,
semplicità, difficoltà), **preferenza colazione dolce/salata**, dettagli allenamento (anni
esperienza, durata max sessione, esercizi preferiti/non graditi/non conosciuti, carichi
attuali, livello tecnico, gruppi prioritari), obiettivi secondari e tempistiche.

### 2.2 `EnergyCalculator` ignora lo stile di vita

`activityFactor(profile)` usa SOLO `trainingDaysPerWeek` + piccola correzione per
esperienza. Non considera tipo di lavoro, ore seduto, passi giornalieri, trasporto, sport
aggiuntivi. Due utenti con stesso n° allenamenti ma lavoro sedentario vs manuale ricevono
**lo stesso TDEE** (errore fino a 600-900 kcal).

### 2.3 `CarbCyclingEngine` applicato automaticamente

`GenerateProgramsUseCase` invoca sempre `CarbCyclingEngine.build(...)`. Non esiste alcuna
condizione che ne giustifichi l'uso. La specifica richiede l'applicazione **solo con
motivazione concreta**.

### 2.4 `MealPlanGenerator` non usa la preferenza colazione e ignora budget/semplicità

`generate` sceglie gli alimenti per ruolo in modo deterministico con `pick(list, seed)`.
Non esiste gestione della preferenza dolce/salata per la colazione. La ripartizione pasti
gestisce solo "≤3 pasti" o "5 pasti": non supporta 4 e 6 pasti. Budget, tempo di cucina e
preferenza semplicità non influenzano la selezione.

### 2.5 `WorkoutPlanBuilder` selezione non motivata e parametri non individualizzati

- `selectExercises` filtra per attrezzatura e limitazioni, poi ordina per categoria: non
  usa esercizi preferiti/non graditi/non conosciuti, gruppi prioritari, durata massima
  sessione, carichi attuali, livello tecnico.
- `setsForExercise` usa una formula fissa: volume non individualizzato.
- Nessuna **spiegazione** del perché un esercizio è stato scelto.
- `exercisesPerSession` ignora la **durata massima** dichiarata.

### 2.6 Recuperi statici

`restSeconds` è un valore fisso nel seed per esercizio. Non viene calcolato in base a
intensità, RIR, obiettivo, fatica, prestazioni precedenti (§9).

### 2.7 Tecniche d'intensità assenti

Il campo `technique` è un testo descrittivo: non esiste un motore che valuti quando
applicare drop set, rest-pause, myo-reps, superset, back-off, cluster, cedimento,
eccentriche, né logica di giustificazione o rischio.

### 2.8 Cronometro assente

`WorkoutScreen` mostra `rec ${restSeconds}s` come testo ma non esiste alcun timer di
recupero: nessuna gestione background/stato, pausa, skip, notifica fine recupero.

### 2.9 Coach AI: contesto parziale

`CoachPromptBuilder.buildContext` include profilo (solo i campi esistenti), piano
nutrizionale, piano allenamento, prestazioni, trend peso, modifiche. Manca tutto lo stile
di vita, sonno/recupero, lavoro: il Coach non può spiegare l'impatto di lavoro e sonno sul
recupero (§3).

### 2.10 Nessuna base di conoscenza delle fonti

Non esiste alcuna entità né schermata per memorizzare fonti scientifiche con (fonte,
titolo, autori, data, aggiornamento, argomento, evidenza, regola derivata, limiti). Le
citazioni sono solo commenti nel codice.

### 2.11 Nessuno storico delle versioni dei piani

`NutritionPlanEntity`/`WorkoutPlanEntity` non hanno versionamento: la rigenerazione
sovrascrive. Manca lo storico versioni (§12 test 12).

### 2.12 Motore adattivo non reagisce ai cambi di stile di vita

`AdaptiveEngine.evaluateNutrition` usa solo trend peso/aderenza. Un cambio di lavoro,
turni o attività non genera rivalutazione.

## 3. Cosa NON verrà toccato (funziona ed è testato)

Calcoli energetici di base (Mifflin-St Jeor / Katch-McArdle), doppia progressione,
periodizzazione a blocchi, gestione fatica, struttura Room/DAO/repository, UI esistente e
sua estetica, sistema AI provider-agnostico con azioni controllate.

## 4. Piano di correzione (fasi)

1. Modello: campi stile di vita/recupero/nutrizione/allenamento + enum; migrazione Room v2.
2. `EnergyCalculator`: PAL da lavoro/passi/attività (NEAT da stile di vita).
3. `CarbCyclingEngine`: attivazione condizionale + giorni da stile di vita.
4. `MealPlanGenerator`: colazione dolce/salata, 3/4/5/6 pasti, budget/semplicità/orari.
5. Nuovo `RestCalculator` (recuperi individualizzati) e `IntensityTechniquesEngine`.
6. `WorkoutPlanBuilder`/`SplitGenerator`: selezione con preferenze/priorità/durata +
   spiegazione per esercizio + volume individualizzato.
7. Base di conoscenza fonti (`ScientificSourceEntity` + seed + schermata).
8. Cronometro recupero nella UI allenamento.
9. `CoachContext`/`CoachPromptBuilder`: intera anamnesi.
10. Storico versioni piani.
11. 12 test obbligatori.
12. Build APK debug + release.
