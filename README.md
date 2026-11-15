# FitCoach – AI Personal Trainer & Nutrition Coach

FitCoach è un'app Android nativa che si comporta come un vero coach personale: costruisce
programmi alimentari completi con **ciclicità dei carboidrati**, programmazioni di
allenamento **periodizzate** con **progressive overload** automatico e adatta i piani ai
risultati reali dell'utente. L'utente esegue il programma e registra i risultati; il
sistema pianifica e analizza.

## Stato di sviluppo

Sviluppato per fasi verificabili. Ogni fase è stata compilata e testata.

| Fase | Contenuto | Stato |
|------|-----------|-------|
| 1 – Fondamenta | Progetto, Clean Architecture, Room, profilo, dashboard, navigazione | ✅ |
| 2 – Nutrizione | DB alimentare (CREA), calcolo energetico, macro, carb cycling, 7 giorni, sostituzioni | ✅ |
| 3 – Allenamento | DB esercizi, generazione split, periodizzazione, registrazione prestazioni, double progression | ✅ |
| 4 – Coach adattivo | Analisi peso/aderenza/performance, regole decisionali, check-in, proposte, storico | ✅ |
| 5 – AI | Chat con contesto strutturato, provider configurabili, azioni controllate, gestione errori | ✅ |
| 6 – Notifiche e statistiche | Calendario notifiche, grafici, export dati | ✅ |
| 7 – Collaudo | Test unitari motori, build APK debug/release; UI E2E da eseguire su dispositivo | ◐ |

## Stack tecnologico

- **Kotlin**, **Jetpack Compose**, **Material 3**
- **MVVM + Clean Architecture** (domain / data / ui)
- **Room** (24 entità, DAO, migrazioni, schema esportato; DB v10)
- **Coroutines / Flow**, **WorkManager**, **AlarmManager** (notifiche esatte)
- **DataStore** per le impostazioni, **Hilt** per la dependency injection
- **Retrofit / OkHttp + kotlinx.serialization** per le API AI
- Grafici con **Compose Canvas** (nessuna dipendenza esterna, offline)
- `minSdk 26`, `targetSdk 35`

Architettura **offline-first**: tutte le funzioni principali operano senza rete.
Solo la chat AI richiede Internet.

## Interfaccia

- Dark mode predefinita, nero/antracite con accento verde smeraldo.
- Bottom bar: **Dashboard · Piano · Spesa · Diario · Workout · Progressi · Coach**.
- La dashboard mostra giorno, tipo di giornata (HIGH/MEDIUM/LOW), calorie, macro,
  pasti previsti, allenamento del giorno, prossima notifica e stato progressi.

## Sistema nutrizionale integrato

Le quattro funzionalità (piano, spesa, sostituzioni, diario) condividono lo **stesso
database alimentare** e gli **stessi identificativi** del piano: non sono moduli separati.
Ogni modifica al piano si riflette automaticamente sulla spesa e sul diario.

- **Piano alimentare** (`MealPlanGenerator`) – 7 giorni con grammature, crudo/cotto,
  alternative equivalenti, target calorico/macro coerenti e verificati matematicamente.
- **Sostituzione vincolata** (`FoodSubstitutionEngine`) – non sostituisce mai "a parità di
  grammi": calcola la quantità del sostituto sul macro dominante e verifica calorie e
  macro del **pasto** e della **giornata** entro tolleranze configurabili (default: kcal 5%,
  P/C/G 10%). Blocca le sostituzioni che superano il limite calorico approvato. Se un solo
  alimento non basta, propone una **sostituzione combinata** (sostituto + alimento di
  riequilibrio) calcolata matematicamente, mostrando il controllo preventivo (prima/dopo).
- **Lista della spesa** (`ShoppingListEngine`) – aggregata dal piano effettivo (es. pollo
  lun 150 g + mar 200 g = 350 g in una voce), per categoria merceologica, con spunta,
  "già in casa", aggiunta manuale, modifica quantità, avanzamento, condivisione e storico
  settimanale. Una sostituzione aggiorna la lista e segnala gli stati spuntati invalidati.
- **Diario nutrizionale** (`NutritionDiaryEngine`) – spunta degli alimenti del piano,
  alimenti extra fuori piano registrati fedelmente, modifica quantità, spostamento tra
  pasti. Mostra **Obiettivo / Consumato / Rimanente** distinti (con eccedenze esplicite),
  niente doppio conteggio, isolamento tra giorni e riepilogo settimanale sui consumi reali.

Il **Coach AI** riceve nel contesto anche il bilancio del giorno, il riepilogo settimanale,
la lista della spesa e lo storico delle sostituzioni: risponde usando solo dati reali.

## Anamnesi guidata e gate di generazione

Prima di generare qualsiasi piano, l'utente completa un'**anamnesi guidata** una domanda
alla volta (`AnamnesisEngine`): dati anagrafici, stile di vita, recupero, esperienza di
allenamento, obiettivi, preferenze e vincoli alimentari, **budget mensile**. L'intervista
valida ogni risposta, rileva le **contraddizioni** (es. frequenza dichiarata superiore ai
giorni disponibili) e impedisce la conferma finché i dati essenziali non sono completi.

La generazione dei programmi è **bloccata finché l'anamnesi non è confermata**
(`GenerateProgramsUseCase.Result.AnamnesisNotConfirmed`): nessun piano viene costruito su
dati incompleti. La dashboard mostra un avviso e riporta l'utente all'anamnesi finché non è
completata. Il riepilogo confermato viene salvato e fornito anche al Coach AI.

## Budget alimentare

`BudgetEngine` tratta il budget come **vincolo effettivo**: il costo del piano è calcolato
dalle **grammature reali** × **prezzo/kg** degli alimenti. L'ambito è esplicito
(personale, condiviso tra più persone con quota pro capite, o tutta la spesa con stima
dichiarata della quota alimentare). Se manca il prezzo di un alimento, il costo **non viene
inventato**: la voce è segnalata come non stimabile e il totale è marcato come stima
parziale. Quando il budget è stretto, il generatore preferisce le fonti proteiche con il
miglior costo per grammo di proteine (calcolato, non ipotizzato).

## Database alimentare progressivo

`FoodSearchRepository` risolve una ricerca in ordine: **database locale** (Room) → **Open
Food Facts** (prodotti/marche) → **USDA FoodData Central** (alimenti generici). Ciò che
viene trovato è salvato localmente e riutilizzato, quindi una ricerca non viene ripetuta
inutilmente. La deduplicazione avviene su nome normalizzato e su `sourceId`. Nessun valore
nutrizionale è inventato: i risultati privi di macronutrienti vengono scartati. La ricerca è
integrata nel flusso di **sostituzione alimentare** (Impostazioni del piano → sostituisci →
cerca), dove ogni candidato passa comunque dal controllo preventivo vincolato.

## Motori principali (deterministici, testati)

- `EnergyCalculator` – BMR (Mifflin-St Jeor / Katch-McArdle se nota la massa grassa),
  TDEE, target calorico, distribuzione macro, bilancio settimanale. Il fattore di attività
  parte dai giorni di allenamento e viene **corretto dal NEAT reale** (tipo di lavoro,
  passi giornalieri, mezzo di trasporto, camminate pianificate, sport aggiuntivi).
- `CarbCyclingEngine` – assegnazione giorni HIGH/MEDIUM/LOW con verifica di coerenza
  calorie/macro e bilancio settimanale (nessun deficit estremo nei giorni LOW). Il carb
  cycling viene **applicato solo quando è giustificato** dal profilo (obiettivo e numero di
  allenamenti); altrimenti si usa una distribuzione uniforme con motivazione esplicita.
- `MealPlanGenerator` – piano di 7 giorni, grammature, crudo/cotto, totali pasto e
  sostituzioni con quantità ricalcolate. Gestisce **3/4/5/6 pasti**, la **preferenza
  colazione dolce/salata/entrambe** (o nessuna colazione), budget, semplicità e alimenti
  graditi/non graditi.
- `SplitGenerator` – split Full Body, Upper/Lower, Push/Pull/Legs, frequenza e volume
  settimanale per gruppo muscolare.
- `WorkoutPlanBuilder` – selezione esercizi guidata da attrezzatura, limitazioni, esercizi
  preferiti/non graditi/non conosciuti, gruppi prioritari e durata massima di sessione;
  ogni scelta è **motivata**.
- `RestCalculator` – recuperi **individualizzati** (non più valori fissi): categoria
  esercizio, obiettivo, RIR, carico relativo, esperienza, fatica accumulata e tecniche
  d'intensità, con motivazione.
- `IntensityTechniquesEngine` – decide **se e quando** applicare tecniche d'intensità
  (drop set, rest-pause, myo-reps, superset, back-off, cluster, cedimento, eccentriche),
  con motivazione e motivo per cui le tecniche più avanzate NON vengono usate.
- `PeriodizationEngine` + `ProgressiveOverloadEngine` – **double progression**: mantiene il
  carico e sale di ripetizioni fino al top del range, poi incrementa il carico. Non aumenta
  il carico in caso di dolore, tecnica scorretta o RIR insufficiente.
- `FatigueManager` – valuta calo di prestazioni, stagnazione, volume settimanale, qualità
  del recupero (sonno/RPE), dolore; non interpreta ogni stagnazione come richiesta di più
  volume.
- **Sessione guidata** – cronometro della durata sessione, **cronometro di recupero**
  (con la durata consigliata dal motore), registrazione carico/ripetizioni/RIR per ogni
  serie, promemoria dell'**ultima volta** per esercizio (top serie, serie, ripetizioni) e
  **storico delle sessioni** (volume, durata, RPE, dolore).
- `AdaptiveEngine` + `WeightAnalyzer` – media settimanale del peso, trend, aderenza,
  performance; genera proposte strutturate (situazione, problema, dati, modifica,
  motivazione, risultato atteso, controindicazioni). `LongitudinalCoachEngine` è il
  **motore di analisi di supporto** del check-in: combina almeno due settimane
  nutrizionali con trend del peso/vita, aderenza, prestazioni, recupero e dolore e
  produce **classificazioni** (progressione, stagnazione, regressione) e advisory, ma
  **non decide più**: la decisione spetta all'AI (vedi sotto). Blocca le modifiche con
  aderenza insufficiente e non interpreta ogni stagnazione come richiesta di più volume.
  Le variazioni caloriche scalano target e grammature in una transazione Room, rispettano
  limiti di sicurezza e sono annullabili. Gli esiti reali delle modifiche applicate
  vengono rivalutati nel check-in successivo e memorizzati nello storico.
  Include l'**analisi dell'andamento per singolo esercizio** (regressione → riduzione
  volume, progressione solida → nessun cambio).
- `BudgetEngine` – costo reale del piano da grammature × prezzo, quota pro capite per budget
  condivisi, stima parziale dichiarata quando mancano prezzi, convenienza per grammo di
  proteine.

Separazione netta tra **motore matematico deterministico**, **motore di analisi** e
**AI conversazionale**: l'AI interpreta e propone, non calcola né modifica i database
direttamente.

## L'AI come autorità decisionale (tool calling)

L'AI **non è un semplice chatbot**: è il cervello decisionale del coach. Opera in un
**ciclo agentico** (`CoachOrchestrator`) con **tool calling**: il modello legge dati reali,
calcola e propone, mentre l'app verifica e l'utente conferma.

Strumenti (in `domain/coach/tools`, iniettati come `Set<CoachTool>` via Hilt):

- **Lettura (gli "occhi" dell'AI)** – anamnesi/profilo, riepilogo catalogo, elenco
  alimenti ed esercizi, piano nutrizionale attivo, scheda attiva, storico allenamenti,
  progressi, diario alimentare, foto progressi. Restituiscono solo dati reali dai
  repository; se un dato manca, l'AI deve chiederlo, mai inventarlo.
- **Calcolo/analisi (strumenti deterministici)** – fabbisogno energetico e macro,
  carb cycling, volume settimanale per gruppo muscolare, stima 1RM, **calcolo porzioni**
  (`compute_portion`), **sostituzione equivalente** (`compute_substitution`) e
  **validazione di una giornata alimentare** (`validate_nutrition_day`). Sono i mattoni
  con cui l'AI **costruisce i pasti da sola**, senza un piano preimpostato.
- **Validazione** – `validate_workout_plan` (delegato a `WorkoutPlanValidator` puro):
  copertura muscolare, volume, attrezzatura, sicurezza.
- **Check-in settimanale** – `analyze_weekly_checkin`: aggrega i dati **reali** degli
  ultimi 7 giorni e il confronto con le settimane precedenti (tendenza del peso con
  affidabilità, composizione, aderenza, calorie/macro consumati, allenamenti
  completati/pianificati, andamento delle prestazioni, recupero, sonno, dolore). Include
  classificazioni di **supporto** (advisory) ma **non decide**: l'AI le interpreta e
  propone solo se giustificato. Se un campo è null il dato non è disponibile e non va
  inventato.
- **Scrittura controllata** – `propose_*` (sostituzione alimento/esercizio, spostamento
  allenamento, aggiustamento calorie/volume, deload): registrano una **proposta** che
  l'utente conferma o annulla tramite il flusso esistente. L'AI non applica mai una
  modifica direttamente.
- **Rigenerazione per ambito** – `propose_regenerate_plan(scope)`: l'AI la usa quando la
  richiesta cambia la **struttura** del programma (numero di giorni, nuova split, giorni
  HIGH/MEDIUM/LOW del carb cycling, cambio di strategia), non per una modifica puntuale.
  Sceglie l'ambito `WORKOUT`, `NUTRITION` o `BOTH`. Alla conferma dell'utente la
  rigenerazione passa dallo **stesso motore unico** della generazione iniziale
  (`ProgramGenerationCoordinator` → `submit_full_program`): un solo motore, una sola
  autorità decisionale. Per le modifiche puntuali l'AI preferisce gli strumenti `propose_*`
  specifici.

La generazione del percorso (piano alimentare + scheda) avviene tramite il **Coach AI**:
il modello produce il JSON con strategia e struttura, l'app risolve gli identificativi
locali e **rifiuta** ogni piano che non rispetta i valori nutrizionali, il database
esercizi o i vincoli utente. Il motore matematico offline resta disponibile ma **solo
come scelta esplicita dell'utente** quando il provider AI non è configurato: non
sostituisce mai l'autorità decisionale dell'AI.

Il collegamento **chat → rigenerazione per ambito** è verificato end-to-end
(`CoachChatRegenerationIntegrationTest`): la chat propone, l'utente conferma, il motore
unico rigenera solo la parte richiesta; in caso di provider non raggiungibile o proposta
non valida **nessun piano viene alterato** e la rigenerazione è **annullabile** (i piani
precedenti vengono riattivati).

## Check-in settimanale adattivo (l'AI come decisore)

Il **check-in settimanale** è condotto dall'**AI centrale** (`WeeklyCheckInService` →
`CoachOrchestrator`), non da un motore deterministico separato:

1. **Aggregazione dei dati reali** (`WeeklyCheckInAnalyzer`) – peso (medie settimanali e
   trend con affidabilità), composizione, aderenza, calorie/macro effettivamente
   registrati, allenamenti completati/pianificati, andamento dei carichi per esercizio,
   recupero, sonno e dolore. La fotografia viene salvata come `CheckIn` (storico).
2. **Analisi di supporto** (`LongitudinalCoachEngine`) – produce **classificazioni** e
   advisory (stagnazione, regressione, aderenza insufficiente, ricomposizione, deload).
3. **Decisione dell'AI** – il modello riceve il report e propone modifiche con gli
   strumenti `propose_*`. Le proposte restano **`PROPOSED`**: non vengono mai applicate
   automaticamente.
4. **Conferma dell'utente** – dalla sezione Progressi → *Check-in settimanale* l'utente
   vede valutazione, metriche e proposte, e può **approvare**, **rifiutare** o
   **annullare** (rollback) ogni modifica. In assenza di provider configurato il check-in
   salva comunque i dati e lo segnala.

Regole di sicurezza applicate al prompt e al motore: nessuna riduzione calorica basata su
una singola settimana senza aderenza e trend affidabili; mai aumentare il volume quando le
prestazioni peggiorano; nessun dato inventato quando manca (i campi restano null e l'AI
deve chiederli). Il flusso è verificato end-to-end (`WeeklyCheckInFlowIntegrationTest`):
proposta nutrizionale, proposta allenamento, rifiuto, conferma, rollback e persistenza.

## Base di conoscenza

Le indicazioni applicate dai motori sono tracciate in `ScientificSourceSeedData`: ogni
voce ha fonte (linea guida ufficiale, revisione sistematica, studio, articolo tecnico),
titolo, autori, data, URL verificabile, evidenza, **regola derivata** e **limiti di
applicabilità**. La schermata Impostazioni → Fonti e versioni mostra le fonti e lo
**storico delle versioni dei piani** (ogni rigenerazione salva uno snapshot immutabile).

## AI configurabile

Provider supportati: **OpenAI, Groq, OpenRouter, DeepSeek, custom OpenAI-compatible**
(inclusi endpoint gratuiti). Pannello Impostazioni → Provider AI con Base URL, modello,
API key, test connessione e stato.

Le chiavi API sono cifrate con **EncryptedSharedPreferences** (AES-256) e **non vengono
mai incluse nell'APK**. È disponibile l'opzione **backend proxy** per non tenere la chiave
sul dispositivo.

Compatibilità parametri per provider/modello: il limite dei token generati viene inviato
come `max_tokens` (Groq, OpenRouter, DeepSeek, custom e modelli OpenAI storici) oppure come
`max_completion_tokens` (modelli OpenAI recenti: o1/o3/o4, gpt-4.1, gpt-5). Il payload
contiene **un solo** parametro: mai entrambi. Se il provider rifiuta il parametro inviato,
il client esegue **un unico retry controllato** con l'altro parametro.

La chat conosce profilo, piano attivo, ciclicità, scheda, storico carichi, progressi e
modifiche precedenti. Le modifiche proposte passano sempre da funzioni controllate e sono
registrate con possibilità di **annullamento**. In Impostazioni → Modalità Coach AI l'utente
sceglie tra **Consulenziale**, **Adattiva** (solo aggiustamenti ordinari autorizzati) e
**Con conferma** (modifiche sempre approvate manualmente). Cambiare modalità è persistente;
le modifiche significative, come deload o sostituzioni esercizio, restano sempre soggette a
conferma.

## Notifiche

Promemoria per colazione, spuntini, pranzo, cena, allenamento, idratazione, check-in
settimanale e valutazione del peso. Orari personalizzabili, attivazione/disattivazione,
ripristino dopo reboot (`BootReceiver`), gestione permessi Android 12+/13+.

## Sicurezza

- L'AI non è un professionista sanitario: nessuna diagnosi, farmaco, ormone o sostanza.
- Controlli su allergie, intolleranze, alimenti esclusi e limitazioni fisiche.
- In caso di dolore l'utente può interrompere l'esercizio e segnalarlo: il carico non
  viene aumentato.
- Blocco di deficit calorici estremi e volumi irragionevoli; validazione matematica di
  calorie e macro su ogni piano.

## Compilazione

Requisiti: JDK 17, Android SDK (API 35). Imposta `ANDROID_HOME` o crea `local.properties`
con `sdk.dir=...`.

```bash
# Test unitari dei motori
./gradlew :app:testDebugUnitTest

# APK debug (installabile)
./gradlew :app:assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk

# APK release (firmabile; senza keystore produce app-release-unsigned.apk)
./gradlew :app:assembleRelease
```

### Firma release

Crea `keystore.properties` nella root (non versionato):

```properties
storeFile=/percorso/fitcoach-release.jks
storePassword=***
keyAlias=fitcoach
keyPassword=***
```

Il blocco `signingConfigs` in `app/build.gradle.kts` si attiva automaticamente se il file
esiste. Senza di esso la release viene generata non firmata.

## Installazione su dispositivo

1. Abilita "Origini sconosciute" / installazione da file su Android.
2. Copia `app-debug.apk` sul dispositivo e installalo.
3. Al primo avvio completa l'onboarding: profilo, obiettivo, attrezzatura, preferenze
   alimentari, limitazioni. FitCoach genera piano alimentare e scheda.

## Struttura del progetto

```
app/src/main/java/com/fitcoach/app/
├── data/            # Room (entity, dao, db), repository, remote (AI), datastore
├── domain/          # modelli, motori (nutrition, workout, adaptive), coach, usecase
├── di/              # moduli Hilt (database, repository, network)
├── notifications/   # scheduler, receiver, boot receiver
├── ui/              # theme, components, navigation, screens + viewmodels
└── FitCoachApplication.kt
```

## Test

Test unitari dei motori matematici (**248 test**, 27 classi): energia (incl. NEAT da stile
di vita), carb cycling (incl. attivazione condizionale), generazione pasti (incl. n° pasti e
preferenza colazione), sostituzione alimentare vincolata (pollo→tonno, rifiuto oltre il
limite calorico, sostituzione combinata, tolleranze), lista della spesa (somma alimenti
ripetuti, categorie, conservazione spunta, **rigenerazioni concorrenti senza lista vuota**),
diario nutrizionale (obiettivo/consumato/
rimanente, niente doppio conteggio, extra, isolamento giorni), split, recuperi, tecniche
d'intensità, fatica, progressive overload, analisi peso, motore adattivo, **anamnesi guidata**
(parsing risposte, gate di generazione, contraddizioni), **budget mensile** (costo reale,
quota pro capite, stima parziale), **mappatura fonti alimentari** (Open Food Facts, USDA),
**analisi dell'andamento per esercizio** (regressione, progressione, stabilità),
**parametri AI per provider** (max_tokens vs max_completion_tokens, mai entrambi, retry
controllato), **layout schermata anamnesi** (nessuna sovrapposizione, pulsante unico),
**persistenza sessione di allenamento** (serie valide richieste per completare, riapertura
non distruttiva di sessioni chiuse per errore), **diagnostica proposta AI** (valore atteso
vs ricevuto per ogni vincolo fallito), **regressione scenario allenamento** (attrezzatura
limitata, movimenti da evitare).

```bash
./gradlew :app:testDebugUnitTest
```

## Documentazione

- `docs/ROADMAP.md` – roadmap tecnica, decisioni architetturali, fasi.
- `docs/REVISIONE.md` – report tecnico della revisione scientifica: carenze rilevate nel
  codice e piano di correzione per fase.

## Avvertenza

FitCoach non fornisce consulenza medica. I piani generati sono basati su formule
documentate e sui dati inseriti dall'utente, ma non sostituiscono il parere di un medico,
nutrizionista o professionista qualificato.
