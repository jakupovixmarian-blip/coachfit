# FitCoach — Prompt di audit completo

Documento da incollare in una nuova conversazione OpenHands. Contiene:
1. cosa deve fare FitCoach, nel dettaglio;
2. cosa fa davvero oggi (stato misurato del codice);
3. tutte le problematiche riscontrate, con causa e correzione;
4. tutte le modifiche applicate, commit per commit;
5. cosa resta aperto;
6. il metodo di verifica da seguire.

---

# PARTE 0 — Ruolo e regole

Agisci come team composto da: Senior Android Developer, Kotlin/Jetpack Compose
Engineer, Software Architect, AI Engineer (LLM e agenti con tool calling),
nutrizionista sportivo, Strength & Conditioning Specialist, QA Engineer.

Non sei qui per aggiungere funzionalità. Sei qui per **verificare che FitCoach
faccia quello che dice di fare**, trovare dove non lo fa, e correggere. Una
funzione che dichiara un risultato senza produrlo è un difetto grave quanto un
crash.

## Regola zero: nessun dato inventato

È la regola che questa app ha violato più spesso, e vale anche per te.

- Nessun valore nutrizionale, PAL, percentuale, soglia o parametro di
  allenamento senza fonte rintracciabile. Se è una convenzione, va dichiarato.
- Non dichiarare completata una funzione senza averla eseguita e verificata.
  Se non è verificabile, scrivi perché e cosa manca.
- I campi nullable del profilo sono **dati assenti**, non numeri da riempire con
  un default plausibile.
- Nella UI e nei payload, **stima** e **dato registrato** non vanno mai
  confusi: l'utente deve sapere se 2.859 kcal è un calcolo o una misura.

## Vincoli operativi

- Non sovrascrivere file senza averli letti.
- Non lasciare `TODO` su funzioni importanti.
- Non creare pulsanti senza azione.
- Test per i calcoli, sempre; un test per ogni difetto corretto, che sia rosso
  senza la correzione.
- Build e test dopo ogni correzione. Correggi prima di proseguire.
- Mantieni `README.md` e `AGENTS.md` aggiornati.
- Commit piccoli e motivati. Non fare push se non richiesto.

---

# PARTE 1 — Cosa deve fare FitCoach

FitCoach è un coach digitale per ipertrofia, nutrizione sportiva e
ricomposizione corporea. **Non** è un diario alimentare, **non** è un registro
allenamenti.

**Il sistema pianifica e analizza. L'utente esegue e registra i risultati.**
L'utente non deve mai programmare a mano la progressione dei carichi, né
calcolare calorie e macro.

## 1.1 Profilo e onboarding

Raccogliere: età, sesso biologico (serve alle formule), altezza, peso, % di
grasso facoltativa, esperienza di allenamento, obiettivo principale, giorni
disponibili, orari dei pasti, orari di allenamento, attrezzatura disponibile,
preferenze alimentari, alimenti esclusi, allergie e intolleranze, limitazioni
fisiche e movimenti da evitare, budget alimentare indicativo.

Obiettivi selezionabili: ricomposizione corporea, ipertrofia, definizione,
mantenimento. (In codice esiste anche `STRENGTH`, "Forza": è un'aggiunta non
prevista dalla specifica, da confermare o rimuovere.)

Ogni parametro deve essere modificabile successivamente, e la modifica deve
passare dagli **stessi** intervalli di validazione del primo inserimento.

## 1.2 Motore nutrizionale

**Calcolo energetico**
- BMR: Mifflin-St Jeor (1990); Katch-McArdle se è disponibile la % di grasso.
- TDEE = BMR × fattore di attività (PAL).
- Target calorico e distribuzione dei macro.
- Bilancio energetico settimanale.
- Override manuale possibile; stima e dato reale distinti.

**Ciclicità dei carboidrati**
Tre tipi di giornata:
- **HIGH CARB** — allenamenti più impegnativi, più carboidrati, calorie
  adeguate al carico.
- **MEDIUM CARB** — allenamento moderato, quota intermedia.
- **LOW CARB** — riposo o attività leggera, carboidrati ridotti, grassi adeguati
  mantenendo proteine appropriate.

Personalizzabili: numero di giornate HIGH/MEDIUM/LOW, carboidrati, proteine,
grassi e calorie per ciascuna tipologia.

Vincoli non negoziabili:
- kcal = 4·P + 4·C + 9·G su **ogni** giornata, e il totale settimanale dei macro
  deve tornare con il totale settimanale delle calorie.
- Nessun deficit arbitrario nei giorni LOW.
- Il vincolo reale è il **tasso di calo**: 0,5–1% del peso a settimana
  (Helms 2014). Oltre l'1% il piano non è valido, non è "sconsigliato".
- I giorni non-LOW non possono essere meno dei giorni di allenamento.

**Generazione dei pasti**
Programma di 7 giorni, 5 pasti: colazione, spuntino mattutino, pranzo,
spuntino pomeridiano, cena. (Il codice prevede anche uno spuntino serale:
6 `MealSlot`. Se il profilo chiede 5 pasti, il sesto non deve comparire.)

Ogni voce: nome alimento, grammi, crudo/cotto, kcal, P, C, G. Ogni pasto:
totale. Ogni voce con la **fonte** del valore nutrizionale.

Database alimentare con valori verificabili, sostituzione alimenti con quantità
ricalcolate per mantenere macro comparabili.

**Modifica adattiva**
Valutazione **settimanale** su: peso medio, trend, calorie effettivamente
consumate, aderenza, prestazioni, composizione corporea se disponibile.
Mai su oscillazioni giornaliere. Ogni modifica motivata e storicizzata.

## 1.3 Motore di allenamento

**Schede** — split (Upper/Lower, Push/Pull/Legs, Full Body, personalizzate)
scelta in base a esperienza, disponibilità, recupero, obiettivo. Per ogni
esercizio: gruppo muscolare primario e secondario, attrezzatura, descrizione
tecnica, serie, range ripetizioni, recupero, RIR target, alternative.
Volume settimanale per gruppo muscolare entro le linee guida.

**Progressive overload a doppia progressione** — l'utente registra solo carico,
ripetizioni, serie completate, RIR/RPE. Il motore decide.
- Si mantiene il carico finché tutte le serie allenanti non raggiungono il
  limite superiore del range.
- Raggiunto il top, si aumenta il carico e si riparte dal minimo.
- L'incremento dipende da esercizio e carico attuale.
- **Nessun aumento** se: RIR insufficiente, tecnica scorretta segnalata, dolore
  riportato, prestazione in calo.
- Multiarticolari, isolamento e corpo libero gestiti separatamente.

**Periodizzazione** — blocchi: mesociclo, progressione graduale, aumento
programmato dello stimolo, deload giustificato, rivalutazione, nuovo mesociclo.
Durata configurabile. Il sistema non cambia schede senza motivo.

**Gestione affaticamento** — calo prestazioni, stagnazione, volume settimanale,
recupero, RIR, RPE, dolori, qualità del sonno. Non interpretare ogni stagnazione
come "serve più volume".

## 1.4 Motore adattivo

Tre livelli separati e non intercambiabili:

| Livello | Cosa fa | Cosa NON deve fare |
|---|---|---|
| Matematico deterministico | i calcoli | decidere |
| Analisi | interpreta i risultati | inventare dati |
| AI conversazionale | spiega, propone | calcolare, scrivere direttamente |

Ogni proposta di modifica contiene: **situazione attuale, problema
identificato, dati utilizzati, modifica suggerita, motivazione, risultato
atteso, possibili controindicazioni**. Le modifiche importanti richiedono
approvazione esplicita. Ogni modifica è annullabile (ripristino del programma
precedente).

## 1.5 Chat AI

Deve conoscere: profilo, piano alimentare attivo, ciclicità, scheda, storico
carichi, progressi, preferenze, modifiche precedenti, obiettivi.

Deve poter proporre modifiche strutturate e, dopo conferma, applicarle tramite
funzioni controllate. Una chat che risponde senza vedere i dati del programma
è un difetto, non una semplificazione.

## 1.6 Provider AI

OpenAI, Groq, OpenRouter, DeepSeek, altri endpoint OpenAI-compatibili.
Pannello: provider, API key, base URL, modello, test connessione, stato.
Deve esistere un'opzione gratuita. Le chiavi non devono stare in chiaro
nell'APK: la soluzione della specifica è un backend proxy.

## 1.7 Notifiche

Pasti, inizio allenamento, idratazione, check-in settimanale, pesata.
Personalizzabili: orari, on/off, posticipo, conferma pasto, conferma
allenamento. Ripristinate dopo il riavvio del dispositivo. Permessi Android
gestiti.

## 1.8 Progressi

Grafici: peso, media settimanale, circonferenze, calorie, macro, carichi,
ripetizioni, volume, progressione della forza. Storico settimanale, mensile,
trimestrale. Esportazione dati.

## 1.9 Sicurezza

L'AI non è un professionista sanitario: nessuna diagnosi, nessuna prescrizione
di farmaci, ormoni o sostanze dopanti. Controlli su allergie, intolleranze e
limitazioni fisiche. In caso di dolore, l'utente deve poter interrompere e
segnalare. Nessun deficit estremo, nessun volume irragionevole. Verifiche
matematiche su tutte le calorie e i macro.

## 1.10 Requisiti tecnici

Kotlin, Compose M3, MVVM + Clean, Room, Hilt, Coroutines/Flow, Retrofit/OkHttp,
WorkManager, AlarmManager, DataStore, Vico (o equivalente) per i grafici.
`minSdk 26`. Architettura **offline-first**: tutto tranne la chat AI deve
funzionare senza rete. UI in italiano. Dark mode predefinita, nero/antracite,
accento verde smeraldo. Barra inferiore a 5 voci: Dashboard, Alimentazione,
Allenamento, Progressi, Coach AI.

---

# PARTE 2 — Cosa fa davvero oggi

Stato **misurato** del repository, non dichiarato.

## 2.1 Numeri

| Voce | Valore |
|---|---|
| File Kotlin in `main` | 224 |
| Righe in `main` | 62.043 |
| File di test | 169 |
| Righe di test | 28.446 |
| Test eseguiti | **1.124** (7 skipped, 0 falliti) |
| Commit | 180 |
| Versione | 2.5.8, versionCode 39 |
| Schema Room | versione 18, 31 entità, 17 migrazioni |
| Strumenti del Coach AI | 31 |
| Alimenti nel catalogo interno | 254 (CREA + USDA, con fonte per voce) |
| Alimenti nel catalogo globale | 7.793 righe USDA SR Legacy (asset, offline) |
| Esercizi nel catalogo | 36, su 10 gruppi muscolari |
| Regole nel corpus InVictus | 1.056 |

## 2.2 Navigazione — discostamento dalla specifica

La specifica chiede **5 tab**. L'app ne mostra **6**:

`Dashboard · Piano · Spesa · Workout · Progressi · Coach`

`Spesa` è un'aggiunta non prevista. `Diario` esiste come route ma **non è nella
barra**: si raggiunge solo dall'icona in Alimentazione o dal deep link di una
notifica pasto. Da verificare se è la scelta voluta.

## 2.3 Architettura AI-native

Dal commit `96a467f` l'unica autorità che genera e modifica i programmi è il
Coach AI (`CoachOrchestrator` + `submit_full_program`). I motori deterministici
**non decidono più**: calcolano, catalogano, validano. Conseguenza importante:
**senza un provider AI configurato l'app non genera programmi.** Esiste un
"piano manuale" (`createManualPlan`) come via d'uscita.

Questo è in tensione con la specifica: "le funzioni principali devono
funzionare senza connessione". Oggi la nutrizione e la scheda richiedono la
rete. Va deciso e documentato: è una scelta architetturale legittima, ma
contraddice un requisito scritto.

## 2.4 Codice orfano da decidere

`com/fitcoach/app/v2/` — 19 file (domain, ai, presentation, 7 file di test)
**non importati da nessuna parte**. Include `AnamnesisScreenV2`,
`GenerationCoordinator`, `NutritionPlanV2`, `DoubleProgressionV2`,
`OpenAiCompatibleClient`. Non è raggiungibile dall'utente. Va rimosso o
completato: lasciarlo è ambiguo e raddoppia la manutenzione.

## 2.5 Grafica

Identità "Obsidian Meridian" (AppJet) compilata nell'APK: ogni schermata ha un
`XxxContent.kt` stateless. **Solo la Dashboard è agganciata al ViewModel reale.**
Le altre 14 mostrano la grafica precedente con la palette nuova.

## 2.6 Provider AI

`AiProvider` enum: OpenAI, Groq, OpenRouter, DeepSeek, Custom. Presente:
- cifratura chiavi con `EncryptedSharedPreferences` (AES-256, master key nel
  Keystore) — meglio del chiaro, ma **non** è il backend proxy della specifica;
- gestione del fallimento della master key (Keystore invalidato dopo restore o
  cambio di blocco schermo, su alcuni firmware Samsung);
- adattamento dei parametri per provider/modello (`AiParameterPolicy`);
- supporto DeepSeek Thinking + Tool Calling;
- retry con backoff su errori transitori.

---

# PARTE 3 — Problemi riscontrati

Ognuno è stato riprodotto, non ipotizzato. Il numero è l'ordine cronologico.

## 3.1 Motore nutrizionale

**P1 — Fattori di attività senza fonte.** Il codice usava `1,2 / 1,375 / 1,55 /
1,725`, i moltiplicatori dei calcolatori online. Nessuno appartiene alle bande
FAO/WHO/UNU 2004 (`1,40–1,69` / `1,70–1,99` / `2,00–2,40`). In particolare
`1,375` sta **sotto** il minimo FAO per chi si allena 2–3 volte a settimana: il
fabbisogno era sottostimato.
*Corretto:* bande FAO come parametri tipizzati in `EngineParameters`, con
citazione.

**P2 — Banda PAL che collassa (il bug segnalato dall'utente).** Con 4–5
allenamenti la banda era `[1,99, 2,00]`, larga **0,01**. La correzione NEAT
(lavoro d'ufficio −0,05, zero passi −0,10: totale −0,11) veniva sempre
schiacciata da `coerceIn`. Risultato: un impiegato con 5 allenamenti e 0 passi
otteneva PAL `1,990` e **TDEE 3.347 kcal** su BMR 1.682 — il fabbisogno di un
atleta professionista.

Causa concettuale: la tabella FAO classifica lo **stile di vita**, non i giorni
di palestra. Usare i giorni di allenamento come unico selettore della banda
rendeva inutile proprio la correzione che doveva distinguere i due casi.
*Corretto:* 4–5 → `[1,70, 2,00]`; 6+ → `[1,99, 2,40]`. Lo scenario passa da
3.347 a **2.859 kcal**.

**P3 — Il "fattore 1,275" inventato dall'AI.** Nello stesso caso il coach
proponeva 2.150 kcal con "fattore attività 1,275". Quel fattore non esiste in
nessuna banda: è **sotto il pavimento fisiologico di 1,20** (Black 1996, persona
non deambulante). Il numero "corretto" era inventato quanto quello che
intendeva correggere.
*Corretto:* lo strumento `energy` espone ora `fattoreAttivita`,
`palMinimoFisiologico`, `palMassimoSostenibile`, `palUsatoInBanda`. Il modello
non deve più dedurre il fattore dal TDEE.

**P4 — Deficit espresso solo in percentuale.** `weeklyAdjustmentFraction`
(CUT −20%) è una convenzione. Su un peso basso il −20% chiede un calo dell'1,4%
a settimana, cioè perdita di massa magra. Il deficit di −4.685 kcal segnalato
dall'utente era semplicemente `3347 × 0,20 × 7`.
*Corretto:* `impliedWeeklyLossPercent` converte il deficit in velocità di calo;
oltre l'1% il piano diventa **non valido**, non solo avvisato.

**P5 — Soglia grassi inventata (0,6 g/kg).** In produzione c'era una soglia di
grassi che nessuna regola del corpus conteneva, mentre il corpus dice 0,8 e 0,5.
*Corretto:* il pavimento dei grassi ora si **calcola** dal 15% delle calorie
(Helms 2014) e non è più un g/kg fisso. Attenzione: il pavimento è una quota
delle calorie del giorno, ma le calorie contengono i grassi stessi, quindi va
risolto (`F = s·(P·4 + C·4) / ((1−s)·9)`), non assegnato.

**P6 — Grassi nei giorni LOW che annullavano i giorni HIGH.** Con 2 HIGH e 2 LOW
il piano finiva con i LOW **6 kcal sopra** gli HIGH: la prima richiesta della
ciclizzazione era violata e nessun test se ne accorgeva.
*Corretto:* lo scarto dei grassi è limitato dal vincolo che i giorni ON
conservino almeno 100 kcal di vantaggio (`MIN_KCAL_GAP_ON_VS_OFF`).

**P7 — Grammature non riscalate quando cambiano i target.** L'utente leggeva
"Distribuzione applicata al piano" e ritrovava pasti identici: cambiava solo
l'intestazione.
*Corretto:* `clonePlanVersion` accetta un hook `afterClone` eseguito nella stessa
transazione, che ricalcola le grammature con `MealPortionSolver`.

**P8 — "Avena" risolta su "Latte d'avena".** `CatalogNameResolver` sceglieva il
nome più corto fra quelli che contengono le parole cercate: "latte d'avena" è più
corto di "fiocchi d'avena". Errore di **otto volte** sulle calorie del pasto
(46 vs 367 kcal/100 g). La mappa dei sinonimi aveva solo le voci inglesi.
*Corretto:* sinonimi in italiano **e** inglese.

**P9 — Numero di pasti trattato come suggerimento.** Il vincolo del profilo
(es. 5 pasti) non era applicato.
*Corretto:* `5b358d1`, il numero di pasti è un vincolo.

## 3.2 Allenamento

**P10 — Incremento raddoppiato a cavallo della soglia.** `increaseFor` sommava
l'incremento proporzionale alla base: 100 kg → +2,5 kg ma 101 kg → +5,0 kg.
*Corretto:* `maxOf(base, proporzionale)`. Sotto i 50 kg il vincolo del 5% non è
applicabile perché 2,5 kg è già oltre il 5%.

**P11 — Esperienza e obiettivo non cambiavano la scheda.** Un neofita poteva
ricevere la stessa scheda di un avanzato: le soglie producevano solo avvisi e
l'obiettivo non entrava in nessun controllo.
*Corretto:* `TrainingPolicyChecks.dayViolations` dentro `ProposalChecker`.

**P12 — Scheda invisibile nei giorni di riposo.** *Corretto:* `bc0f734`.

**P13 — Vincolo di isolamento bloccava chi non aveva alternative.** *Corretto:*
`2dbe188`.

## 3.3 Database — il difetto più insidioso

**P14 — `@Insert(REPLACE)` su padri con figli CASCADE.** In SQLite REPLACE è
**DELETE + INSERT**: la cascata cancella i figli. La riga "riscritta" torna, il
contenuto no. È accaduto **quattro volte**:
1. `applyDeload` — il deload era inservibile per qualsiasi percentuale. Il test
   esisteva e passava **per il motivo sbagliato**: il rollback della transazione
   rimetteva tutto a posto.
2. `applyDeload` — mancava la validazione `volumeReductionPercent in 0..80`: una
   percentuale negativa **aumentava** il volume durante il deload.
3. `moveWorkoutDay` — spostare una seduta ne cancellava gli esercizi.
4. `restoreRegeneratedPlans` — annullare una rigenerazione svuotava la scheda.

Padri interessati: `nutrition_plans → nutrition_days → meals → meal_entries`,
`workout_plans → mesocycles → workout_days → planned_exercises`,
`workout_sessions → workout_sets`.
*Regola:* per modificare un campo di un padre si usa `@Query("UPDATE ...")`;
`insert` solo con `id = 0L`.

**P15 — Doppia colonna in `MIGRATION_1_2`.** *Corretto* con collaudo delle
migrazioni (`95e87e1`).

**P16 — Letture che scrivevano.** `getActivePlan()` apriva una transazione a ogni
lettura; due letture concorrenti fallivano con "Cannot perform this operation
because there is no current transaction".
*Corretto:* normalizzazione spostata in `onCreate` (`normalizeActivePlanState`),
i percorsi di lettura non scrivono più.

## 3.4 Falsi successi — la classe di difetti più grave

Più percorsi dichiaravano "applicato" senza applicare nulla:

**P17 — `clonePlanVersion` azzerava l'id prima delle transform.** Ogni chiamante
identifica la riga con `day.id == dayId`: il confronto era **sempre falso**.
Cambiare target, tipo o orario produceva una copia identica e l'app rispondeva
"aggiornato".

**P18 — `applyMoveWorkout` scriveva su un campo che nessuno legge**
(`nutrition_days.workoutDayIndex`; la dashboard usa `workout_days.dayIndex`).
Conferma verde, scheda invariata. In più il tool esponeva `giorno` (indice)
mentre l'executor si aspettava `dayId`.
*Lezione:* quando un percorso di scrittura tocca la UI, il test deve verificare
**l'esito visibile**, non il valore di ritorno dell'executor. `Applied(...)` non
è una prova.

**P19 — `ComputeCarbCyclingTool` accettava `giornateHigh/Medium/Low` e non li
passava al motore.** Restituiva l'eco della richiesta e il piano di default: il
modello credeva di aver personalizzato la settimana.
*Regola:* un parametro accettato ma non usato è peggio di uno assente, perché
produce una falsa conferma.

**P20 — `rescaleDay` falliva in silenzio** quando il catalogo non era
risolvibile. *Corretto:* `01f267b`.

**P21 — Tipi di giornata non applicati** (`e86a353`, `ea0afb8`).

**P22 — La richiesta dell'utente non arrivava alla rigenerazione** (`20c55b7`).

**P23 — Rigenerazione che chiudeva l'app** (`dc72172`), turno del coach appeso
(`4d3584a`), pulsante "Genera" che non faceva nulla dopo la conferma (`ad67cc6`).

## 3.5 Sicurezza e dati

**P24 — Allergie aggirate via chat.** La generazione filtrava il catalogo con
`FoodFilter.allowedFoods`, la sostituzione puntuale in chat **no**: un profilo
allergico al lattosio riceveva lo yogurt perché il modello lo aveva proposto.
*Corretto:* ogni scrittura di un alimento ripassa da `FoodFilter.isAllowed`.

**P25 — Numeri del modello non validati.** Grammi negativi, kcal negative,
`Infinity`, 50 kg in una porzione e macro incoerenti finivano nel piano alla
conferma dell'utente. I numeri li scrive il modello ma li approva l'utente:
vanno validati come input esterno.
*Corretto:* `MAX_ENTRY_GRAMS = 5000`, `MAX_ENTRY_KCAL = 10000`, coerenza 4-4-9
con tolleranza **assoluta + relativa**.
*Attenzione:* sul catalogo reale il 4-4-9 **non torna** per i vegetali, perché le
fibre contano nelle kcal ma non nei carboidrati. Scarto massimo misurato 24%
(broccoli 34 vs 41, zucchine 16 vs 19). Una tolleranza del 10% avrebbe rifiutato
alimenti veri: servono `ABS = 30` e `REL = 0,25`. Non stringerle senza
rimisurare.

**P26 — `ConsultInvictusTool` muto su 25 voci su 65** (fra cui Panca piana):
restituiva tre array vuoti. Tre array vuoti senza spiegazione invitano il
modello a inventare la tecnica. *Corretto:* ogni voce porta
`contenutoDisponibile`, la risposta include `senzaContenuto` e una nota che vieta
di inventare.

**P27 — Media del peso calcolata su tutte le righe.** Pesarsi due volte nello
stesso giorno fa contare quel giorno il doppio, e la media è ciò che decide "il
peso è fermo". *Corretto:* una pesata per `dateEpochDay` (l'ultima). Il DAO resta
uno storico: non deduplica, perché sovrascrivere perderebbe la prima misurazione.

**P28 — Validazione solo nella UI.** `DashboardViewModel.logWeight` accettava
peso 0; `ProfileEditViewModel.save()` accettava 900 kg e 0 giorni di allenamento,
che entravano nel TDEE e nella generazione. Il "modifica profilo" era una porta
laterale. *Regola:* validare ai bordi, nel ViewModel, non solo nella UI.

**P29 — `require` in un repository dentro `viewModelScope.launch`** chiudeva
l'app (`ProgressViewModel.addPhoto`). *Corretto:* validare prima, o convertire in
messaggio.

**P30 — `restoreBackup` copiava il file sopra il database senza validarlo.** Un
documento qualsiasi o un backup troncato distruggeva tutti i dati.
*Corretto:* validazione (magic SQLite, apertura read-only, tabelle presenti) su
una copia temporanea; la chiusura è l'ultimo passo.
*Regola:* mai chiudere una risorsa prima di sapere che il sostituto è valido.

**P31 — Export CSV: locale del dispositivo.** `"%.1f".format()` su un telefono
italiano produce "1218,0", che spezza la riga. *Corretto:* `Locale.ROOT`.

**P32 — Export CSV: formula injection.** Un testo che inizia con `=`, `+`, `-`,
`@` viene eseguito da Excel. *Corretto:* apostrofo anteposto ai **testi**, non ai
numeri (altrimenti le colonne non sono più sommabili).

**P33 — Escaping CSV e JSON scambiati.** Il CSV raddoppia le virgolette, il JSON
le precede con backslash: due funzioni diverse.

**P34 — `Infinity` in una colonna numerica del CSV.** SQLite accetta `Infinity`
ma rifiuta `NaN`. *Corretto:* valori non finiti → cella vuota nel CSV, `null`
nel JSON.

**P35 — Keystore che invalida la master key.** Dopo un restore, una
reinstallazione, un cambio di blocco schermo o su alcuni firmware Samsung,
l'app andava in crash aprendo le impostazioni AI. *Corretto:* l'archivio cifrato
viene ricreato una volta; se fallisce, le operazioni degradano a "nessuna chiave"
invece di terminare il processo.

## 3.6 UI e raggiungibilità

**P36 — Primo avvio in un vicolo cieco** (`b4ee9b2`).
**P37 — Home che spariva** (`9d7e4db`).
**P38 — Diario non raggiungibile** (`9d7e4db`).
**P39 — 12 directory `ui/screens/*` irraggiungibili** — rimosse (`703b5a9`).
**P40 — Intervista di anamnesi che chiedeva le domande con default** (`e42b6ee`).
**P41 — Ordine del primo avvio**: il provider AI va configurato **prima** delle
domande di anamnesi, altrimenti la generazione finale non può funzionare
(`20f59e5`).
**P42 — Due APK ambigui in distribuzione** (`08c12fc`): un solo APK, versionCode
crescente, versione visibile.
**P43 — Test instabili**: `AnamnesisLayoutTest` fallisce con `AppNotIdleException`
sotto carico parallelo ma passa in isolamento. Non è un loop di composizione
dell'app: verificare in isolamento prima di attribuire il difetto al codice.
`FileProvider` in Robolectric fallisce quando altri test hanno usato un `Context`
diverso: la cache dei provider è statica. Verificare il contenuto prodotto, non
la condivisione via `Intent`.

---

# PARTE 4 — Modifiche applicate

## 4.1 Sessione corrente (2.5.8) — la correzione del bug segnalato

Commit **`46f11c3`**, 5 file, +100/−6.

**`EnergyCalculator.kt`** — mappaggio delle bande:
```kotlin
4, 5 -> { min = palModerateMin (1.70); max = palVigorousMin (2.00) }   // era [1.99, 2.00]
else -> { min = palModerateMax (1.99); max = palVigorousMax (2.40) }   // era [2.00, 2.40]
```
Commento nel codice che spiega perché la tabella FAO classifica lo stile di vita
e non i giorni di palestra, e che il caso 4–5 era largo 0,01.

**`Nutrition.kt`** — `EnergyResult` ha ora `activityFactor: Double = 0.0`. Senza
il fattore, chi legge non può distinguere "BMR alto" da "fattore alto".

**`AnalysisTools.kt`** — lo strumento `compute_energy_targets` emette:
`fattoreAttivita`, `palUsatoInBanda`, `palMinimoFisiologico`,
`palMassimoSostenibile`. Il modello non deve più dedurre il fattore.

**`EnergyCalculatorTest.kt`** — 4 nuovi test (13 → 17):
- `sedentary office worker who trains does not get an athlete activity factor`
  — lo scenario esatto dell'utente;
- `lifestyle shifts activity factor even with many training days` — due profili
  con gli **stessi** allenamenti e stile diverso devono differire di > 0,10.
  Questo è il test che diventa rosso se le bande tornano a collassare;
- `activity factor stays inside the band for every training frequency`;
- `computed energy exposes the activity factor it used`.

**Verifica:** 1.124 test, 0 falliti, 7 skipped. `BUILD SUCCESSFUL`.
APK 2.5.8, versionCode 39, firma valida, SHA-256
`456505315b8ddf7f54d20560f26da3d1a74c95c61197e9ad766d04a8c24adb92`.

Commit **`bdc9f87`** — `AGENTS.md`: documentata la trappola della banda che
collassa (con la regola "se `max − min < ~0,10` la correzione NEAT è inefficace
per costruzione") e quella del target AI sotto il fabbisogno.

## 4.2 Sessione precedente — pulizia dei numeri inventati

Commit **`92d919f`**, 27 file, +2.340/−204. Ha introdotto:

- **`EngineParameters.kt`** (382 righe) — registro di ogni numero dei motori, con
  valore, `Origin` (PRIMARY / PRIMARY_RANGE / CORPUS / CORPUS_RANGE / CONVENTION /
  CLINICAL), studio di riferimento e nota. 22 parametri registrati, elencati in
  `all()` per il test di conformità. L'`Origin` **distingue** un numero che la
  letteratura documenta da una scelta nostra: senza questa distinzione "il motore
  usa il corpus" resta un'affermazione non verificabile.
- **`EngineEvidence.kt`** — citazioni (FAO/WHO/UNU 2004, Helms 2014, Morton 2018,
  Aragon 2017, Black 1996, Xu 2023).
- **`TrainingPolicy.kt` + `TrainingPolicyChecks.kt`** — esperienza e obiettivo
  entrano nei controlli, non solo nel prompt.
- **`CorpusCitation.kt` + `CorpusPrompt.kt`** — il corpus fuori dal prompt
  (un solo corpus, 1.056 regole) e consultabile per codice.
- **`CarbCyclingEngine.kt`** (+334) — escursione in **grammi** (75 g, Xu 2023)
  invece di percentuali sul totale; grassi HIGH/LOW vincolati al vantaggio
  calorico dei giorni ON; pavimento grassi calcolato.
- **`EnergyCalculator.kt`** — bande FAO al posto di 1,2/1,375/1,55/1,725.
- **`ProposalChecker.kt`**, **`CoachActionExecutor.kt`** — firma nutrizionale per
  distinguere "niente è cambiato" da "la scheda è giusta così".
- 8 nuovi file di test (collaudo su parametri, escursione ciclizzazione, scheda
  adatta all'anamnesi, corpus).

## 4.3 Altre correzioni recenti degne di nota

| Commit | Cosa |
|---|---|
| `e291bd6` | I 4 bug REPLACE/CASCADE + `DeloadAdversarialTest`, `CascadeReplaceAdversarialTest`, `MoveWorkoutBugTest` |
| `1a5fce0` | "Sposta la seduta" scrive ora `workout_days.dayIndex`, valida 0..6, scambia se occupato, revert corretto; `get_active_workout_plan` espone `dayId` |
| `e86a353`, `01f267b` | Modifiche puntuali del piano realmente applicate; riscalamento che non fallisce in silenzio |
| `2f6c7fe` | Grammature calcolate sui target reali (10 file, +1.241) |
| `fe2373d` | Difese su Keystore, receiver e avvio; log recuperabile (12 file) |
| `0d7808f` | Calcoli non aggirabili, soglie inventate rimosse (15 file) |
| `6dc7bd8` | 5 difetti reali su export e backup, trovati con test avversariali |
| `aa603ba` | Allergeni tradotti negli alimenti, filtro vegano sugli ingredienti |
| `a4aa8a6` | Chiusura dei buchi dell'audit avversariale (coach, nutrizione, statistiche) |
| `1d9af7a` | Validazione ai bordi (dashboard, profilo, foto) |
| `6c6857d` | Risoluzione "Avena", incremento carico oltre 100 kg |
| `3d27546` | Catalogo italiano a 254 alimenti |
| `f1da370` | Distribuzione carb cycling personalizzabile + backup automatico |
| `1a7499d` | Grafici mancanti: forza, ripetizioni, macro, circonferenze |

---

# PARTE 5 — Cosa resta aperto

Non sono ipotesi: sono cose che ho verificato e non ho chiuso.

**A1 — Il cancello calorico guarda le kcal, non il fattore.** `safetyFloor` è
`max(BMR × 0,85, 1500|1200)` = **1.430 kcal** per BMR 1.682. Un target di 2.150
kcal lo supera tranquillamente, pur essendo sotto il fabbisogno di una persona
sedentaria (`1,40 × 1.682 = 2.355`). Prima di accettare una proposta che cambia
il target va confrontato il **fattore implicito** `target / BMR` con
`palPhysiologicalFloor..palSustainableCeiling`. Da decidere con l'utente:
rifiutare o solo avvisare?

**A2 — `com/fitcoach/app/v2/` è codice morto.** 19 file + 7 test non collegati.

**A3 — Modello OpenAI di default `"gpt-5.6"`.** Compare in
`Enums.kt` e in `v2/ai/provider/ProviderContracts.kt`. Va verificato che esista.

**A4 — Offline-first vs AI-native.** La specifica chiede che le funzioni
principali funzionino senza rete; oggi la generazione richiede il provider AI.

**A5 — 6 tab invece di 5.** `Spesa` è un'aggiunta; `Diario` non è nella barra.

**A6 — Grafica agganciata solo sulla Dashboard.** 14 schermate su 15 usano ancora
la grafica precedente.

**A7 — Nessun test end-to-end su dispositivo reale.** I 1.124 test sono unitari
e di integrazione su JVM/Robolectric. Il percorso completo su un Samsung fisico
(notifiche dopo reboot, BLE, permessi, fotogrammi) non è coperto da test
automatici.

**A8 — Backend proxy assente.** Le chiavi sono cifrate on-device, non su backend.

**A9 — `MealSlot.EVENING_SNACK`** esiste (6 slot) mentre la specifica ne chiede 5.

**A10 — `Goal.STRENGTH`** ("Forza") è un obiettivo non previsto dalla specifica.

---

# PARTE 6 — Metodo di verifica

Lavora in **tre passaggi separati**. Non mescolarli.

## Passaggio 1 — Mappa

Prima di modificare qualsiasi cosa, produci:
- elenco delle schermate **realmente raggiungibili** dalla navigazione;
- elenco delle funzionalità dichiarate ma non collegate (pulsanti senza azione,
  ViewModel non istanziati, moduli orfani);
- elenco dei punti in cui un numero viene **mostrato** all'utente e da dove
  arriva;
- elenco delle costanti numeriche nei motori, con la fonte di ciascuna o la
  dichiarazione che è una convenzione. Il registro `EngineParameters.all()` è il
  punto di partenza: verifica che ogni costante usata dai motori sia lì dentro.

## Passaggio 2 — Verifica per comportamento

Per ogni funzionalità della Parte 1, scrivi un test che la esercita dal punto di
ingresso reale (ViewModel, use case, repository) e **prova a romperla** con dati
che l'app riceve davvero: zero, negativo, `null`, `Infinity`, stringhe scritte
dal modello AI, date passate, profili incompleti.

Avvertenze nate da difetti reali di questo progetto:
- Un test che verifica solo che l'id del piano sia cambiato non prova che il
  contenuto sia cambiato.
- Un test che passa perché una transazione fa rollback non prova che la funzione
  funzioni.
- `createManualPlan` crea pasti **vuoti**: un test su quello non può accorgersi
  di un difetto nel riscalamento. Servono alimenti reali **e** il catalogo
  `food_database` popolato.
- Cattura lo stato **prima** della chiamata, non dopo: catturarlo dopo rende il
  test incapace di fallire.
- Confronta i grassi in **quota**, non in grammi: i grammi dipendono dalle
  calorie del giorno.
- Se un test asserisce un PAL esatto, sta asserendo un numero senza fonte:
  confrontalo con i limiti della banda, non con una costante scritta a mano.

## Passaggio 3 — Correzione

Per ogni difetto: causa, correzione minima, test di regressione che diventa
rosso senza la correzione. Poi build e suite completa.

---

# PARTE 7 — Consegna

Un report con, per ogni difetto: **cosa dichiara l'app / cosa fa davvero / causa
/ correzione / test che lo copre**. In fondo, l'elenco esplicito di ciò che
**non** hai potuto verificare e perché.

Un audit che dice "tutto ok" senza mostrare i test che lo dimostrano non è un
audit. E un numero senza fonte è un difetto, anche quando il numero è giusto.

---

## Appendice — Comandi

```bash
export JAVA_HOME=/workspace/toolchain/jdk-17
export ANDROID_HOME=/workspace/toolchain/android-sdk
export GRADLE_USER_HOME=/workspace/gradle-home

./gradlew :app:testDebugUnitTest --offline          # 1.124 test
./gradlew :app:assembleDebug --offline              # APK debug
./gradlew :app:assembleRelease --offline            # APK release (firmabile)
```

Conteggio esito test:
```bash
python3 - <<'PY'
import glob,xml.etree.ElementTree as ET
t=f=s=0
for p in glob.glob('app/build/test-results/testDebugUnitTest/TEST-*.xml'):
    r=ET.parse(p).getroot()
    t+=int(r.get('tests')); f+=int(r.get('failures'))+int(r.get('errors')); s+=int(r.get('skipped'))
print(f"{t} test, {f} falliti, {s} skipped")
PY
```
