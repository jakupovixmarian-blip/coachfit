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

## Ricette dai pasti del piano

La sezione **Ricette** (icona libro nella schermata Alimentazione) non è un
ricettario generico: propone ricette costruite sugli alimenti che il piano
assegna davvero a ciascun pasto, filtrate per colazione, spuntino, pranzo e
cena.

- Il catalogo conta **161 ricette**: 31 colazioni, 27 spuntini mattutini,
  23 spuntini pomeridiani, 40 pranzi e 40 cene, tutte costruite sui 173
  alimenti del database CREA/USDA.
- Le grammature sono quelle calcolate dal motore nutrizionale, non inventate.
- Una ricetta compare solo se gli alimenti che le servono sono presenti in quel
  pasto: niente ingredienti fuori piano.
- Se un ingrediente è escluso per allergia, intolleranza o preferenza, la
  ricetta non viene proposta affatto (una versione ridotta sembrerebbe conforme
  al piano senza esserlo).
- Se un pasto non ha alimenti coperti da una ricetta, la ricetta non compare e
  l'app lo dice: il pasto resta comunque visibile nel piano.
- Un test confronta ogni ingrediente con il database alimentare reale, così un
  refuso in un nome non può far sparire una ricetta in silenzio.

### Creare, vedere e salvare una ricetta

Oltre alle ricette del piano la sezione permette di comporre una ricetta a
partire da un pasto reale e di conservarla.

- **Crea ricetta**: si sceglie il giorno (1-7) e il pasto; il compositore
  `RecipeComposer` riorganizza gli alimenti già previsti in un piatto, in tre
  stili: Classica, Leggera (senza grassi aggiunti) e Ricca (aggiunge una fonte
  extra). Le grammature restano quelle del piano: il piatto non è un pasto
  diverso, i totali coincidono con gli alimenti usati.
- Il ruolo di ogni alimento (fonte proteica, carboidrati, verdura, frutta,
  grassi, latticini) si deduce dai macro, non dalla categoria del catalogo, che
  non è omogenea. Per gli alimenti fuori anagrafica si usano i macro della
  porzione effettiva; il nome da solo non basta a distinguere un piatto
  proteico da uno di carboidrati.
- **Anteprima**: ingredienti con grammi, crudo/cotto, kcal e macro, totali del
  piatto e passi di assemblaggio. I passi non contengono dosaggi: i dosaggi sono
  già nel piano.
- **Salva**: la ricetta finisce in `saved_recipes` (Room, schema v15) con
  ingredienti, passi e totali, e resta consultabile anche dopo il riavvio.
- Se gli alimenti non bastano a comporre un piatto sensato (meno di due
  ingredienti) il compositore restituisce `null` e l'app lo dice, invece di
  proporre un piatto inventato.
- Una riga corrotta nel database non fa fallire l'elenco: gli ingredienti non
  leggibili vengono scartati singolarmente.

## Salute e report giornaliero

La sezione **Salute** (cuore nella Dashboard) legge i dati reali da Health
Connect e mostra il report della giornata.

- Dati letti: calorie attive, passi-equivalenti di attività, frequenza cardiaca
  a riposo, HRV, sonno, peso. Ogni campo è **nullable**: se il dato non esiste
  resta assente e l'app lo dichiara, non mostra uno zero.
- I permessi Health Connect vengono richiesti con `ActivityResultContracts`
  raggruppati per tipo di record; se l'utente nega un permesso la sezione
  continua a funzionare con i dati disponibili.
- Il peso letto da Health Connect è separato dal peso registrato a mano: il
  report non sovrascrive mai una misurazione dell'utente.
- Dalla sezione si raggiungono **Ricette** e **Dispositivi** (bilance BLE).

Health Connect riconosce FitCoach come app che gestisce dati di salute perché il
manifest dichiara `HealthPermissionsRationaleActivity` con i due `activity-alias`
per `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`
(`ACTION_SHOW_PERMISSIONS_RATIONALE` e la variante
`android.health.connect.action.SHOW_PERMISSIONS_RATIONALE`). Senza questa
dichiarazione il sistema non mostra l'app fra quelle autorizzate a leggere i
dati. Se il contract dei permessi non si risolve (Health Connect assente o
aggiornamento richiesto) la schermata apre direttamente le impostazioni di
Health Connect invece di non fare nulla.

## Dispositivi Bluetooth (bilance, BIA, cardio)

La sezione **Dispositivi** collega bilance e cardiofrequenzimetri BLE e legge i
dati reali dal dispositivo.

- Il collegamento abilita le **indications** sulla caratteristica di misura: le
  bilance notificano il peso invece di rispondere a una lettura diretta, che su
  quasi tutti i modelli fallisce con `GATT_READ_NOT_PERMITTED`.
- Molte bilance non annunciano il nome nell'adv packet: il nome viene letto
  dalla Generic Access (0x2A00) dopo la connessione e la scheda si aggiorna.
  Senza nome si mostra il MAC con un'etichetta chiara.
- Le bilance **BIA** espongono la Body Composition (0x2A9C): l'app mostra massa
  grassa, massa magra, massa muscolare, acqua, massa ossea, BMI e impedenza.
  Solo i campi realmente trasmessi vengono mostrati: gli altri restano assenti,
  non stimati.
- Un watchdog di 20 secondi chiude il tentativo e riporta l'errore, invece di
  lasciare la schermata su un indicatore indefinito.
- Se il permesso Bluetooth è stato negato in modo definitivo il sistema non
  mostra più il dialogo: il pulsante aprirebbe un launcher che si chiude subito,
  sembrando rotto. In quel caso l'app porta alla schermata delle autorizzazioni
  dell'app e spiega cosa attivare.

## Stack tecnologico

- **Kotlin**, **Jetpack Compose**, **Material 3**
- **MVVM + Clean Architecture** (domain / data / ui)
- **Room** (24 entità, DAO, migrazioni, schema esportato; DB v13)
- **Coroutines / Flow**, **WorkManager**, **AlarmManager** (notifiche esatte)
- **DataStore** per le impostazioni, **Hilt** per la dependency injection
- **Retrofit / OkHttp + kotlinx.serialization** per le API AI
- **CameraX + ML Kit Barcode Scanning** per la lettura del codice a barre alimentare
- Grafici con **Compose Canvas** (nessuna dipendenza esterna, offline)
- `minSdk 26`, `targetSdk 35`

Architettura **offline-first**: tutte le funzioni principali operano senza rete.
Solo la chat AI richiede Internet.

## Interfaccia

### Stato dell'aggancio grafico "Obsidian Meridian" (AppJet)

La nuova identità grafica (palette, tipografia Sora/Inter/JetBrains Mono, componenti
ridisegnati) è **compilata nell'APK**. Ogni schermata di AppJet è un file `XxxContent.kt`
(stateless) più `XxxUiState.kt`; l'aggancio al ViewModel reale avviene una schermata per volta.

| Schermata | `Content` AppJet | Agganciata |
|-----------|------------------|------------|
| Dashboard | `DashboardContent.kt` | ✅ |
| Onboarding | `OnboardingContent.kt` | — |
| Workout | `WorkoutContent.kt` | — |
| Nutrizione | `NutritionContent.kt` | — |
| Diario | `DiaryContent.kt` | — |
| Progressi | `ProgressContent.kt` | — |
| Coach AI | `CoachContent.kt` | — |
| Anamnesi | `AnamnesisContent.kt` | — |
| Spesa | `ShoppingContent.kt` | — |
| Impostazioni | `SettingsContent.kt` | — |
| Provider AI | `ProviderAiContent.kt` | — |
| Notifiche | `NotificationsContent.kt` | — |
| Profilo | `EditProfileContent.kt` | — |
| Conoscenza | `KnowledgeContent.kt` | — |
| Scheda personalizzata | `CustomPlanContent.kt` | — |

Le schermate non ancora agganciate mostrano la grafica precedente pur usando la palette e i
componenti nuovi: le funzionalità restano intatte. L'aggancio è puramente di presentazione e
non modifica `data/` né `domain/`.

- Dark mode predefinita, nero/antracite con accento verde smeraldo.
- Bottom bar: **Dashboard · Piano · Spesa · Diario · Workout · Progressi · Coach**.
- La dashboard mostra giorno, tipo di giornata (HIGH/MEDIUM/LOW), calorie, macro,
  pasti previsti, allenamento del giorno, prossima notifica e stato progressi.
- **Allenamento**: selettore del giorno della settimana, avvio della sessione per il giorno
  scelto con registrazione di serie/ripetizioni/carichi, cronometro di sessione e **allarme
  di fine recupero** (suona anche a schermo spento). In alto, accesso alla schermata
  **Crea scheda personalizzata** per costruire una split manualmente.

### Riquadri dell'esercizio: serie, ripetizioni, carico e cronometro

Ogni esercizio della seduta è una card espandibile. Chiusa mostra nome, gruppo
muscolare, schema serie×ripetizioni, RIR, recupero e, se presente, la tecnica
d'intensità; toccandola si aprono i riquadri operativi.

La tecnica d'intensità è un campo strutturato (`IntensityTechnique`: DROP_SET,
REST_PAUSE, MYO_REPS, SUPERSET, BACK_OFF_SET, CLUSTER_SET, FAILURE,
ECCENTRIC_EMPHASIS) più una motivazione testuale. Prima la chat scriveva solo la
nota libera `technique`, mentre la scheda leggeva `intensityTechnique`: la
tecnica applicata dall'AI restava invisibile. Ora il tool
`propose_exercise_substitution` scrive entrambi i campi, la scheda mostra la
tecnica anche senza motivazione e l'annullamento ripristina esercizio e tecnica
precedenti. Un valore non riconosciuto non viene applicato come se fosse valido.

- **Serie**: quante serie sono state registrate sul totale previsto (`2/4`).
- **Rep**: il range del programma (`8-10`), non un numero inventato.
- **Carico**: il carico proposto dal motore, oppure "da definire" se il motore
  non ha ancora un riferimento per quell'esercizio.
- **RIR**: il target della serie.
- Sotto i riquadri compare l'elenco delle serie realmente registrate, con carico,
  ripetizioni e RIR effettivi: una serie non registrata non mostra alcun carico.
- **Cronometro esercizio**: tempo dedicato a quell'esercizio, indipendente dal
  recupero e dal cronometro di sessione. Ogni esercizio ha il proprio contatore,
  quindi passare a un altro esercizio non azzera il tempo già speso. Avvia,
  mette in pausa e azzera.
- **Recupero inline**: il timer di recupero vive dentro la card dell'esercizio che
  lo ha generato, non in cima alla lista. Mostra conto alla rovescia, barra di
  avanzamento, +30s, +1 min, salta e l'impostazione manuale in minuti.

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
(`ProgramGenerationResult.AnamnesisNotConfirmed`): nessun piano viene costruito su
dati incompleti. La dashboard mostra un avviso e riporta l'utente all'anamnesi finché non è
completata. Il riepilogo confermato viene salvato e fornito anche al Coach AI.

Confermare l'anamnesi **non avvia** la generazione: la schermata mostra il comando esplicito
"Genera piano alimentare e scheda". Un secondo comando è **bloccato** mentre un turno è in
corso, e dopo un errore diventa "Riprova generazione AI".

La generazione richiede un **provider AI configurato**: `ProgramGenerationCoordinator` lo
verifica prima di partire e restituisce un messaggio che porta a Impostazioni → Provider AI.
Senza questo controllo la schermata restava su "Generazione..." fino alla scadenza del turno.
Per lo stesso motivo il primo avvio passa da un passo di configurazione provider
(`Routes.AI_SETUP`) tra onboarding e anamnesi, saltabile con "Configura più tardi".

Ogni turno del coach ha un **tetto di tempo complessivo**: la scadenza è controllata tra
un'iterazione e l'altra e applicata alla singola chiamata HTTP. Insieme al `callTimeout` di
OkHttp, un provider che non risponde chiude il turno con un errore leggibile invece di
lasciare l'app in attesa.

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

Il **database locale seed** contiene oltre 150 alimenti con valori nutrizionali tratti da
fonti verificabili (**CREA** e **USDA FoodData Central**), organizzati per categoria (carni,
pesce, latticini, uova, cereali, legumi, frutta, verdura, grassi, condimenti, bevande). Il
seed è **incrementale**: gli alimenti nuovi vengono aggiunti agli aggiornamenti senza
duplicare quelli già presenti. Un test di integrità (`FoodSeedDataTest`) verifica assenza di
duplicati, fonti dichiarate, coerenza calorie/macronutrienti e validità delle sostituzioni.

### Database globale integrato (offline)

Oltre al seed curato, l'app include un **database alimentare globale** confezionato
nell'APK: **USDA FoodData Central – SR Legacy**, ~**7.800 alimenti** con valori nutrizionali
verificati per 100 g (proteine, carboidrati, grassi, fibra, kcal, categoria). È importato in
Room al primo avvio (`SeedInitializer.seedUsdaAssetIfNeeded`) dall'asset
`app/src/main/assets/food_usda_sr_legacy.tsv` (≈740 KB) e resta **sempre disponibile
offline**, senza dipendere dalle API esterne.

- L'import è **idempotente** (guardia su `FoodDao.countBySource`): avviene una sola volta.
- Un test (`FoodAssetImporterTest`) valida l'asset reale: numerosità, assenza di duplicati e
  valori negativi, univocità degli `sourceId`, coerenza calorie/macronutrienti e copertura
  degli alimenti comuni.
- L'asset è **rigenerabile** con `python3 tools/build_food_asset.py` (pubblico dominio USDA).
- Nella ricerca l'**ordine di risoluzione** resta: **locale (Room, ~7.800 voci)** → **Open
  Food Facts** (prodotti/marche) → **USDA FoodData Central** (API, fallback). Nel tool AI
  `list_foods` il catalogo curato italiano (CREA, con sostituti) è **prioritario** quando non
  c'è una query, così i piani usano alimenti adatti all'utente italiano; la ricerca testuale
  consulta comunque l'intero database globale.

**Ricerca con fotocamera (stile Yazio).** Dal Diario → *Aggiungi alimento* → *Scansiona
codice a barre* si apre la fotocamera (CameraX + ML Kit). Inquadrando il codice EAN/UPC il
prodotto viene cercato su Open Food Facts e registrato con i suoi valori reali. Richiede il
permesso `CAMERA`, chiesto al primo utilizzo.

### Fonti dati esterne valutate

Per completezza, l'app può attingere a più fonti. Solo le prime due sono integrate come
**dump offline**; le altre sono raggiungibili via API o download su richiesta.

| Fonte | Ambito | Dump offline | Note |
|-------|--------|--------------|------|
| USDA FoodData Central – SR Legacy | Globale | Sì (~7.800 alimenti, 6 MB zip) | **Integrata nell'APK**. Pubblico dominio. |
| Open Food Facts | Globale | Sì (≈1,5 GB csv.gz) | **Integrata via API** (ricerca + barcode). ODbL. |
| USDA FoodData Central – Branded | Globale/USA | Via API (richiede chiave) | Prodotti di marca. |
| CREA (alimentinutrizione.it) | Italia | No (consultazione web) | Fonte del catalogo curato. |
| CIQUAL (ANSES) | Francia | Sì (XLS) | ~3.000 alimenti. |
| Swiss Food Composition Database | Svizzera | Sì (XLSX) | ~1.000 alimenti. |
| CoFID (McCance & Widdowson) | Regno Unito | Sì | ~3.000 alimenti. |
| Fineli | Finlandia | Sì (download) | Pubblico dominio. |
| Frida | Danimarca | Sì (download) | — |
| NEVO | Paesi Bassi | Sì (download) | — |
| Matvaretabellen | Norvegia | Sì (download) | — |
| BLS | Germania | A pagamento | Licenza commerciale. |

Il dump globale completo di Open Food Facts (1,5 GB) **non** viene confezionato nell'APK:
sarebbe sproporzionato per un'app mobile. La strategia adottata è quindi *database globale
interno compatto* (USDA) **+** *API on-demand* (Open Food Facts/USDA) con **cache locale**.


## Componenti matematiche (deterministiche, testate)

> **Architettura AI-native.** FitCoach **non ha motori decisionali offline**: l'unica
> autorità che decide piani, schede e modifiche è il **Coach AI**. Il codice
> deterministico rimasto **non genera e non decide**: fornisce *calcoli* (energia, macro,
> carb cycling), *cataloghi* (alimenti, esercizi) e *validatori* che **giudicano** una
> proposta dell'AI (la bloccano se non è valida). L'AI propone, i vincoli matematici
> giudicano, l'utente conferma.

- `EnergyCalculator` – BMR (Mifflin-St Jeor / Katch-McArdle se nota la massa grassa),
  TDEE, target calorico, distribuzione macro, bilancio settimanale. Il fattore di attività
  parte dai giorni di allenamento e viene **corretto dal NEAT reale** (tipo di lavoro,
  passi giornalieri, mezzo di trasporto, camminate pianificate, sport aggiuntivi).
- `CarbCyclingEngine` – calcola i target HIGH/MEDIUM/LOW con verifica di coerenza
  calorie/macro e bilancio settimanale (nessun deficit estremo nei giorni LOW). Il carb
  cycling viene **applicato solo quando è giustificato** dal profilo (obiettivo e numero di
  allenamenti); altrimenti si usa una distribuzione uniforme. Fornisce anche i target che
  l'AI usa per costruire i pasti.
- `MealPlanGenerator` – restano gli **helper matematici** riutilizzati (grammi per macro,
  sostituzione equivalente, macro dominante, totali pasto). La costruzione autonoma del
  piano è stata **rimossa**: la struttura dei 7 giorni la decide l'AI.
- `WorkoutPlanBuilder` – restano il **calendario dei giorni** e la **sostituzione degli
  esercizi** (contratti usati dall'interfaccia). La costruzione autonoma di schede è stata
  **rimossa**.
- `WorkoutPlanValidator` – **validatore** della scheda proposta: copertura dei gruppi
  previsti, volume per gruppo, compatibilità con attrezzatura/limitazioni/livello, uso di
  esercizi realmente esistenti e sicurezza (serie per seduta). È il **vincolo** che l'AI
  deve soddisfare, non un generatore.
- `ProgramGenerationEngine` – **validatore** della proposta completa (nutrizione + scheda):
  verifica coerenza calorie/macro, bilancio settimanale, presenza degli alimenti/esercizi
  nel catalogo e vincoli del profilo; se non è valida la **blocca** e restituisce la
  diagnosi (`ProposalChecker`) così l'AI può correggersi. Non genera alternative.
- `ProgressiveOverloadEngine` – **double progression**: mantiene il carico e sale di
  ripetizioni fino al top del range, poi incrementa il carico. Non aumenta il carico in caso
  di dolore, tecnica scorretta o RIR insufficiente. Fornisce la raccomandazione all'utente.
- `FatigueManager` – valuta calo di prestazioni, stagnazione, volume settimanale, qualità
  del recupero (sonno/RPE), dolore; alimenta il report di check-in, senza decidere.
- **Sessione guidata** – cronometro della durata sessione, **cronometro di recupero**
  (con la durata consigliata), registrazione carico/ripetizioni/RIR per ogni serie,
  promemoria dell'**ultima volta** per esercizio e **storico delle sessioni**.
- `WeightAnalyzer` + `WeeklyCheckInAnalyzer` – **misurano** (media settimanale del peso,
  trend con affidabilità, aderenza, prestazioni, recupero) e producono un **report** per
  l'AI. Non classificano come decisione e non propongono modifiche: l'analisi è un input
  per l'AI.
- `BudgetEngine` – costo reale del piano da grammature × prezzo, quota pro capite per budget
  condivisi, stima parziale dichiarata quando mancano prezzi, convenienza per grammo di
  proteine.

Separazione netta tra **calcolo/validazione deterministica**, **analisi** e **AI
conversazionale**: l'AI interpreta e propone, i vincoli matematici giudicano, l'AI non
calcola né modifica i database direttamente.

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
  completati/pianificati, andamento delle prestazioni, recupero, sonno, dolore). È una
  **misura**, non una decisione: l'AI interpreta il report e propone solo se giustificato.
  Se un campo è null il dato non è disponibile e non va inventato.
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

### Identificativi nelle proposte: perche "Approva e applica" non fallisce piu

L'AI riceve il contesto del programma in forma testuale e puo citare un `planId`
inesistente, o non citarlo affatto, pur avendo davanti un piano reale. Prima
questo produceva il messaggio *"Impossibile applicare la modifica: Piano non
trovato"* proprio nel caso in cui l'utente vedeva il piano sullo schermo.

Ora la correzione e su due livelli:

- Il prompt espone gli **id reali** di piano nutrizionale, piano di allenamento e
  mesociclo, con l'istruzione esplicita di usarli e non inventarne altri.
- L'esecutore (`CoachActionExecutor`) **risolve** comunque l'id: se l'id e
  assente, inesistente o riferito a una versione non piu attiva, la proposta
  viene applicata al **piano attivo**, cioe quello che l'utente sta seguendo.
  Una proposta non puo modificare una vecchia versione in silenzio, perche non
  avrebbe effetto visibile.
- Se davvero non esiste alcun piano attivo, il messaggio lo dice
  ("Nessun piano alimentare attivo da modificare") invece di un errore generico.

La generazione del percorso (piano alimentare + scheda) avviene tramite il **Coach AI**:
il modello produce il JSON con strategia e struttura, l'app risolve gli identificativi
locali e **rifiuta** ogni piano che non rispetta i valori nutrizionali, il database
esercizi o i vincoli utente. **Non esiste un motore deterministico di fallback**: se il
provider AI non è configurato o non risponde, l'app **non genera** un piano alternativo —
mostra l'errore e invita a configurare il provider.

Il collegamento **chat → rigenerazione per ambito** è verificato end-to-end
(`CoachChatRegenerationIntegrationTest`): la chat propone, l'utente conferma, il motore
unico rigenera solo la parte richiesta; in caso di provider non raggiungibile o proposta
non valida **nessun piano viene alterato** e la rigenerazione è **annullabile** (i piani
precedenti vengono riattivati).

### Un solo cancello, una sola verita

`validate` decideva il salvataggio, `AiProposalDiagnostics.analyze` spiegava al
modello cosa correggere. Due implementazioni della stessa regola, e sono
divergite: il vincolo dolce/salato della colazione esisteva solo nella
diagnostica, quindi un piano con albume a colazione "dolce" veniva respinto e
poi **salvato se il modello insisteva**. Le soglie di volume erano `6..24` fisse
nel cancello e dinamiche nella diagnostica: due verdetti diversi sullo stesso
piano.

Ora c'e' **una sola funzione**, `ProposalChecker.check`, che produce sia l'esito
sia i motivi. `validate` e' `violations.isEmpty()`; `diagnose` restituisce le
stesse violazioni. Non possono divergere perche' non sono piu' due.

`validate` e' sceso da 465 a ~250 righe: calorie, macro, pasti, slot, colazione,
nomi del catalogo, serie/rep/RIR e compatibilita' sono decisi in un punto solo.
`buildNutrition` e `mapWorkout` ora **mappano soltanto** (tipo AI → entita'), e
i loro `return null` non applicano piu' policy: un rifiuto silenzioso senza
diagnosi era la classe di bug che ha prodotto il caso della colazione.
`GateDiagnosticsAgreementTest` verifica la proprieta' che rende la divergenza
impossibile: *validate accetta se e solo se il checker non trova violazioni*.

Il **calendario** (`workoutDayIndices` / `restDayIndices`) non era
informazione: era un duplicato di `workout.days`, confrontato solo con esso.
Chiederlo al modello produceva incoerenze (sedute 0,1,2,3 e calendario
0,1,3,4) che bloccavano programmi per il resto corretti. Ora
`SubmitFullProgramTool` lo **deriva** da `workout.days`: una classe intera di
errori eliminata senza perdere nulla. Anche lo schema dichiara che `days`
contiene **solo le sedute**, non i sette giorni della settimana — era questa
ambiguita' a produrre tre giorni vuoti su sette.

## Check-in settimanale adattivo (l'AI come decisore)

Il **check-in settimanale** è condotto dall'**AI centrale** (`WeeklyCheckInService` →
`CoachOrchestrator`), non da un motore deterministico separato:

1. **Aggregazione dei dati reali** (`WeeklyCheckInAnalyzer`) – peso (medie settimanali e
   trend con affidabilità), composizione, aderenza, calorie/macro effettivamente
   registrati, allenamenti completati/pianificati, andamento dei carichi per esercizio,
   recupero, sonno e dolore. La fotografia viene salvata come `CheckIn` (storico).
2. **Analisi di supporto** (`WeeklyCheckInAnalyzer`) – produce un **report** con i numeri
   misurati e le tendenze (peso con affidabilità, aderenza, prestazioni, recupero). Non
   classifica come decisione e non propone modifiche: è un input per l'AI.
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

### Corpus Project InVictus (metodologia, `InvictusKnowledge`)

Le soglie di volume non possono nascere da un motore di calcolo: 10 serie di polpacci
per un avanzato non sono né giuste né sbagliate in assoluto, dipendono dalla metodologia.
`InvictusKnowledge` porta dentro l'app **1.046 regole** estratte dagli articoli di
projectinvictus.it, ognuna con titolo, URL e data della fonte. Il corpus è
integrato con **letteratura primaria da PubMed** per i parametri che la fonte
divulgativa copre male (deload, progressione, volume): 10 regole da
revisioni sistematiche/meta-analisi e 36 da studi individuali, oltre a 1.000
regole divulgative. Ogni regola dichiara `tipo_fonte`, così il modello sa se
sta leggendo una meta-analisi o un articolo di blog.

Ogni regola è una frase prescrittiva con un numero verificabile (serie, ripetizioni,
grammi, kcal, percentuali), filtrata dalle categorie metodologiche del sito. Sono
classificate per parametro di generazione — 35 categorie, da `SPLIT` a `SAFETY_NUT` — così
lo strumento ne restituisce solo quelle utili.

Esempi di ciò che il corpus porta al motore: ipertrofia tra **10 e 25 serie** allenanti a
settimana, ricomposizione a **12-20**, quadricipiti almeno **8-10** per il mantenimento,
proteine **1,4-2,2 g/kg**, grassi **15-25%** delle calorie con minimo **0,5 g/kg**,
surplus **300-500 kcal**, frequenza **2x** sui grandi, recuperi **2-3 minuti**.

Cosa questo corpus **non** è:

- **Non è una copia degli articoli.** Si estraggono le singole affermazioni con
  attribuzione e link, non i testi: il contenuto del sito è protetto da copyright.
- **Non è una fonte scientifica primaria.** È classificato `SourceKind.DIVULGATIVE`.
  Dove serve autorità si confronta con la letteratura indicizzata (PubMed, OpenAlex,
  Crossref — API pubbliche senza chiave) e con `ScientificSourceSeedData`.

#### Il corpus si consulta, non si versa nel prompt

Il corpus è **dato dell'app**, non testo del prompt. Metterlo nel contesto di generazione
lo trasforma in un elenco interminabile: su un modello piccolo (`qwen3.5-35b-a3b`) un
contesto gonfiato produce programmi non validi — slot dei pasti vuoti, struttura fuori
schema. Verificato: la stessa generazione passa col contesto di base e fallisce con
~45.000 caratteri di corpus appesi in fondo.

L'interfaccia è lo strumento `consult_invictus`, sullo stesso modello di `list_foods` e
`list_exercises`: il modello vede nome, descrizione e schema, e chiede solo la parte che
gli serve, quando gli serve.

La consultazione non è affidata alla buona volontà del modello: è un **prerequisito
imposto dall'orchestratore**. Per tre prove live il modello ha consegnato senza consultare
(`toolTrace=[submit_full_program]`), perché una richiesta in prosa perde contro uno
strumento obbligatorio in concorrenza. `submit_full_program` viene ora bloccato — prima
dell'esecuzione, non dopo, perché il salvataggio avviene dentro la chiamata — finché
`consult_invictus` non ha restituito regole per **entrambe** le aree, `allenamento` e
`nutrizione`. Una chiamata con l'area giusta ma un argomento di un'altra area non conta.

Il corpus è **unico**: `InvictusKnowledge`. Il vecchio `InvictusCorpus` (159 regole, 15
topic) è stato fuso dentro e la sua sorgente — `/tmp/claims_final.json` — non era più
riproducibile, quindi le 55 regole che non esistevano altrove vivono in
`tools/corpus/invictus_corpus_extra.json`. Due corpus con tassonomie diverse (15 e 35
categorie) erano la causa per cui il prompt iniettava quello povero mentre il tool
interrogava quello ricco.

| Area | Cosa restituisce | Voci |
|---|---|---|
| `allenamento` | Split, frequenza, volume, RIR, recuperi, progressione, periodizzazione, deload | 403 |
| `nutrizione` | Calorie, metabolismo, proteine, carboidrati, grassi, fibre, pasti, timing, ricariche | 643 |
| `tecniche` | Come si esegue una tecnica d'intensità | 8 |
| `esercizi` | Muscoli, esecuzione, errori, varianti | 142 |

Le due aree sono divise in **35 categorie**, una per ogni parametro che il
generatore deve decidere. Non sono sezioni tematiche: sono le decisioni che una
scheda o un piano alimentare richiedono.

| Allenamento | | Nutrizione | |
|---|---|---|---|
| Split | Frequenza | Calorie | Metabolismo |
| Volume | Intensità, RIR/RPE | Proteine | Carboidrati |
| Range ripetizioni | Recuperi | Grassi | Fibre |
| Progressione | Periodizzazione | Numero pasti | Timing |
| Deload | Affaticamento | Ricariche | Idratazione |
| Tecniche | Esecuzione | Alcol | Sostituzioni |
| Riscaldamento | Cardio | Aderenza | Lettura del peso |
| Livello | Obiettivo | Composizione | Integratori |
| Sicurezza | | Allergie | |

Se una categoria non serve a generare, non è nel corpus. Ogni regola contiene un
numero verificabile e il link all'articolo: "12-20 serie a settimana in
ricomposizione", "RPE minimo 7,5", "una settimana di scarico ogni 3-5".

Due categorie restano sottili nella fonte divulgativa e sono dichiarate tali:
deload e sostituzioni alimentari. Per il deload la lacuna è colmata dalle fonti
primarie (vedi `InvictusKnowledgeCoverageTest`), non da frasi generiche: il test
verifica che ogni categoria critica abbia una base solida, non solo un conteggio.

Ogni voce torna con `fonte` e `url`, così la proposta del coach è verificabile. Senza
`argomento` lo strumento dà la prima regola di ogni tema: serve a orientarsi, non a
leggere tutto. Un argomento senza risultati risponde `ok:false` invece di inventare.

Il catalogo completo resta in app e continua a funzionare offline; solo la chat AI
richiede rete.

**Rigenerazione.** La pipeline è riproducibile da zero in `tools/corpus/`:
`fetch_invictus.py` legge i 1.357 articoli via REST API pubblica di WordPress
(`/wp-json/wp/v2/posts`, `robots.txt` consente la lettura), `fetch_primary.py`
interroga PubMed/NCBI, `extract_rules.py` + `filter_rules.py` filtrano le
affermazioni, `build_invictus_v2.py` riscrive la tabella Kotlin. Il filtro scarta
tabelle, righe di scheda, frasi senza unità di misura e contenuti enciclopedici
sugli alimenti. Il parsing PubMed usa `esummary` (JSON strutturato): il parsing
testuale di `efetch` produceva titoli errati (date, DOI) al posto dei titoli.

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

Gli allarmi vengono riarmati a ogni ritorno dell'app in primo piano
(`MainActivity.onResume`). Serve perché il sistema può perdere gli allarmi esatti:
dopo un aggiornamento dell'app, quando il permesso viene concesso dopo il primo
avvio, o per le ottimizzazioni batteria dei produttori. Prima il riarmo avveniva
solo in `FitCoachApplication.onCreate`, quindi le notifiche restavano mute finché
l'app non veniva chiusa e riaperta del tutto.

A fine recupero tra le serie parte un avviso sonoro e una vibrazione
(`RestAlarmPlayer`), disattivabili separatamente in Impostazioni > Notifiche >
"Fine recupero". Le due preferenze sono aggiunte dalla migrazione Room 15 -> 16.

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

### Download dell'APK gia compilato

L'APK debug della versione corrente (2.5.0, versionCode 31) e pubblicato su:

- Pagina di download: <https://work-1-htenipprquxeyoak.prod-runtime.all-hands.dev/>
- File diretto: <https://work-1-htenipprquxeyoak.prod-runtime.all-hands.dev/FitCoach-2.5.0-debug.apk>

Il server statico serve solo la cartella `/workspace/apk-download`, che contiene gli APK e
la pagina. Non espone il codice sorgente. Il link e valido finche il processo
`python3 -m http.server 12000` resta in esecuzione nell'ambiente di lavoro.

SHA-256 dell'APK debug 2.5.0:

```
1cc474558271ee72901268a27b52dbd7e9b5470abd0b61cc4b89c0c66d306ebf
```

Confrontalo dopo il download con `sha256sum FitCoach-2.5.0-debug.apk`: se non coincide, il
file e arrivato incompleto o e un'altra build.

### Dove sono Ricette e Salute

Nella Dashboard, subito sotto il saluto, ci sono due riquadri etichettati **Ricette** e
**Salute**. Fino alla 2.4.0 le stesse destinazioni esistevano solo come icone senza testo
sovrapposte al badge del giorno: erano presenti nel codice ma di fatto introvabili.

La sezione Salute legge i dati da Health Connect. Servono i permessi dichiarati nel
manifest (`android.permission.health.READ_*`) e il blocco `<queries>` verso
`com.google.android.apps.healthdata`: senza di essi la richiesta di permessi torna vuota
e la schermata resta su "Concedi accesso" per sempre. I permessi vanno richiesti con
`PermissionController.createRequestPermissionResultContract()`, non con
`ActivityResultContracts.RequestMultiplePermissions()`, che non conosce i permessi di
Health Connect.

### Quale build hai installato

Impostazioni -> in fondo, la card **Versione** mostra `versionName` e `versionCode`.
Serve a distinguere due APK con lo stesso nome ma codice diverso: se il numero non
corrisponde a quello indicato, l'APK installato e' vecchio e va reinstallato.

Nota sulle versioni: `versionCode` deve **crescere** a ogni build distribuita, altrimenti
Android rifiuta l'aggiornamento sopra la build gia' presente. Due file con lo stesso
`versionCode` sono indistinguibili per il sistema anche se il codice interno e' diverso.
Tenere un solo APK per cartella di download, per evitare di installare quello sbagliato.

### Ordine del primo avvio (provider prima delle domande)

Al primo avvio l'app mostra **prima** la configurazione del provider AI, poi le domande
del profilo. La ragione: senza provider nessun piano puo' essere generato, e chiederlo in
fondo significava raccogliere eta', peso e allergie per poi bloccare l'utente.

Regole effettive, verificate da `FirstRunRoutingCollaudoTest`:

| Stato all'avvio | Schermata |
|---|---|
| Provider non configurato e setup mai rimandato | Setup provider AI |
| Setup rimandato, profilo incompleto | Onboarding |
| Setup rimandato o provider pronto, profilo completo | Dashboard |

"Configura piu' tardi" salva un flag persistente (`SetupStateStore`), quindi il setup non
viene riproposto a ogni avvio e resta raggiungibile da Impostazioni. Senza quel flag l'app
avrebbe intrappolato l'utente nel setup a ogni lancio.

Il controllo usa `AiConfigResolver.resolveConfigured()`, non `resolve()`: quest'ultima
restituisce sempre una configurazione (anche con chiave vuota) e farebbe risultare "pronto"
un provider mai impostato.

### Nomi del catalogo: confronto tollerante

L'AI scrive i nomi in italiano corrente ("Uova intere", "Riso", "Yogurt greco"),
il catalogo usa la dicitura della fonte ("Uovo intero di gallina", "Riso bianco
(crudo)", "Yogurt greco 0% grassi"). Il confronto era **esatto**: ogni sinonimo,
plurale o qualificatore mancante faceva respingere un programma altrimenti
corretto, con messaggi come:

```
giorno 1: alimento 'Uova intere' non presente nel database
giorno 4: alimento 'Yogurt greco' non presente nel database
```

`CatalogNameResolver` risolve il nome per passi, dal piu' stretto al piu' largo:

1. nome identico;
2. sinonimo dichiarato ("spigola" -> "branzino", "cottage cheese" -> "fiocchi di latte");
3. singolare/plurale ("uova" -> "uovo");
4. tutte le parole presenti in entrambi i nomi ("riso" -> "Riso bianco (crudo)");
5. stessa testa, taglio diverso ("tagliata di manzo" -> "Fesa di manzo");
6. somiglianza lessicale, solo per nomi lunghi.

Il risolutore cerca **solo nella lista che riceve**, gia' filtrata per allergie ed
esclusioni: la tolleranza sui nomi non puo' far rientrare un alimento vietato
(`CatalogNameResolverTest.la tolleranza sui nomi non reintroduce un alimento escluso`).
Quando nessun candidato e' abbastanza vicino restituisce `null`: meglio un alimento
mancante che un alimento sbagliato, perche' un falso positivo falserebbe le calorie.

Lo stesso risolutore e' usato da validatore, solver delle porzioni e salvataggio.
Prima erano tre confronti esatti separati: il solver scartava in silenzio gli
alimenti non riconosciuti e il giorno finiva a 1463 kcal contro 1891 attese.

### Un pulsante che non risponde e' un bug, non un'attesa

Due difetti della generazione avevano la stessa manifestazione: schermata ferma.
Le cause erano diverse e vale la pena tenerle distinte.

**Il pulsante "Genera" non faceva nulla.** `confirm()` salvava il profilo nel
database ma non lo riportava nello stato: `anamnesisCompleted` restava `false` in
memoria, la guardia di `generateNow()` usciva con un `return` e nessun messaggio.
Regola adottata: ogni blocco deve dire perche' blocca.

**L'attesa era muta.** Il turno di generazione puo' durare fino a 6 minuti (8
tentativi, 90 s di timeout ciascuno). La UI mostrava una barra immobile, quindi
"sta lavorando" e "si e' bloccata" erano indistinguibili. `CoachOrchestrator.run`
accetta ora `onProgress`, e la schermata scrive a che tentativo e' arrivata.

**La diagnosi era inutile.** Il rifiuto diceva "alimento X non presente" senza
elencare cosa esiste: il modello indovinava per 8 tentativi. Il rifiuto include
ora `alimenti_validi` ed `esercizi_validi`, cosi' la correzione avviene al primo
colpo.

**Il JSON malformato passava in silenzio.** Con `ignoreUnknownKeys`, un campo
`name` al posto di `exerciseName` produceva esercizi con nome vuoto e 0 serie, e
la diagnosi parlava di `esercizio ''`. I campi hanno ora alias (`JsonNames`) e la
diagnosi nomina il campo mancante (`campo 'exerciseName' mancante o vuoto`).

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
   alimentari, limitazioni.
4. Configura il provider AI (provider, chiave o backend proxy, modello) e premi
   "Salva e continua": il salvataggio è confermato prima di proseguire.
5. Completa l'anamnesi guidata e premi "Genera piano alimentare e scheda".

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

Test automatici (**295 test**, 44 classi). Coprono i calcoli matematici e i validatori, la
catena di generazione dal Coach AI (proposta → validazione → persistenza), il check-in
settimanale end-to-end, la chat con rigenerazione per ambito e la persistenza su Room reale:

- **Calcoli e vincoli**: energia (incl. NEAT da stile di vita), carb cycling (incl.
  attivazione condizionale), helper di generazione pasti, sostituzione alimentare vincolata
  (pollo→tonno, rifiuto oltre il limite calorico, tolleranze), validatore scheda
  (copertura gruppi, volume, attrezzatura, sicurezza), **diagnostica proposta AI** (valore
  atteso vs ricevuto per ogni vincolo fallito), progressive overload (double progression),
  fatica, analisi peso, anamnesi guidata (parsing, gate, contraddizioni), budget mensile,
  mappatura fonti alimentari, parametri AI per provider.
- **Database alimentare**: `FoodSeedDataTest` (integrità del catalogo curato) e
  `FoodAssetImporterTest` (asset globale USDA: numerosità, dedup, `sourceId` univoci,
  coerenza calorie/macronutrienti, copertura alimenti comuni).
- **Catena AI su Room reale**: `AiProgramGenerationFlowIntegrationTest` (proposta AI →
  `submit_full_program` → validazione → persistenza), `NutritionChainIntegrationTest`
  (generazione → spesa → diario, incl. rigenerazioni concorrenti senza lista vuota),
  `WorkoutSessionFlowIntegrationTest` (registro serie e aggregazione prestazioni),
  `WeeklyCheckInFlowIntegrationTest` (proposta, rifiuto, conferma, rollback),
  `CoachChatRegenerationIntegrationTest` (chat → rigenerazione per ambito),
  `NutritionBuildToolsIntegrationTest` (porzioni, sostituzioni, validazione giornata).
- **Collaudo**: `FitCoachCollaudoTest` esegue l'intero percorso sul profilo reale "Marjan".
- **UI**: layout schermata anamnesi (nessuna sovrapposizione, pulsante unico).

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
