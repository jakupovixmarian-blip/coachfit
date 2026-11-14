# FitCoach 2.0 – Roadmap tecnica e criteri di accettazione

Data: 2026-11-12  
Branch: `fitcoach-2.0-rebuild`  
Baseline archiviata: `archive/fitcoach-1.0-a2bea9a`

## 1. Backup verificato

Prima della ricostruzione è stato creato:

- branch Git `archive/fitcoach-1.0-a2bea9a`;
- bundle completo della cronologia in `/workspace/backups/fitcoach-1.0-20261112/fitcoach-1.0-history.bundle`;
- archivio sorgente in `fitcoach-1.0-source.tar.gz`;
- copia di `dist/`, `docs/` e degli archivi Room presenti in `/workspace/shots/`;
- `SHA256SUMS`, verificato con `sha256sum -c`.

Nessun file del progetto precedente è stato cancellato. Il backup locale non viene inserito nel repository e non contiene chiavi di firma o API.

## 2. Audit della versione 1.0

La versione precedente contiene molto codice funzionante, ma non è una base affidabile per una ricostruzione senza confini chiari:

- il database è monolitico e riunisce profilo, piani, diario, chat, notifiche e storico in un unico grafo Room;
- il client AI è compatibile con endpoint OpenAI-like, ma non espone un contratto completo di capability, function calling e job riprendibili;
- la generazione AI è già presente, ma è una singola pipeline e non rende indipendenti le fasi nutrizione, allenamento, calendario e indicatori;
- alcune regole di sicurezza e validazione sono corrette, ma sono distribuite tra use case, repository e servizio AI;
- il timer di recupero deve avere stato persistente e funzionare con app in background/schermo spento;
- il percorso di onboarding e la generazione sono accoppiati alla struttura precedente;
- i test JVM coprono motori e alcuni flussi, ma non costituiscono collaudo reale di provider, notifiche o UI su Samsung.

### Codice riutilizzabile solo dopo isolamento

I motori puri possono essere portati uno alla volta dopo test indipendenti: calcolo energetico, carb cycling, sostituzioni, diario, progressive overload e analisi adattiva. Non vengono importati direttamente i repository, ViewModel, entità Room o servizi AI della 1.0.

Il codice 1.0 resta disponibile sul branch di archivio. Ogni porting deve avere un test di equivalenza e un nuovo contratto 2.0.

## 3. Architettura 2.0

```text
app/
├── presentation/       Compose, Navigation, ViewModel, UiState
├── domain/
│   ├── anamnesis/      intervista, validazione, consenso
│   ├── nutrition/      calcoli, alimenti, piano, diario, budget
│   ├── workout/        routine, sessioni, timer, progressione
│   ├── progress/       misure, foto, statistiche
│   ├── coaching/       analisi, proposte, approvazione, rollback
│   └── generation/     contratti e validatori dei job AI
├── data/
│   ├── local/          Room, DataStore, backup, migrazioni
│   ├── remote/         provider AI e fonti alimentari
│   └── repository/     implementazioni dei porti domain
└── ai/
    ├── provider/       capability, routing, errori, credenziali
    ├── generation/     job, fasi, retry, JSON schema
    └── tools/          funzioni controllate senza accesso diretto al DB
```

Regola delle dipendenze:

```text
UI → Use case → Domain ports → Data implementations
                         ↘ AI ports → provider adapters
```

I motori deterministici non conoscono Android, Room o un LLM. L'AI restituisce proposte tipizzate; solo un use case autorizzato modifica dati persistenti.

## 4. Modello dati 2.0

Il nuovo database usa un namespace e una migrazione separati dalla v1. Le aggregate root principali sono:

- `ProfileAggregate`: profilo, anamnesi, risposte, conferma e preferenze;
- `AiProviderAggregate`: provider, capability rilevate, endpoint, modello, stato test; la chiave resta in Android Keystore o nel proxy;
- `ProgramAggregate`: job di generazione, fasi, piano nutrizionale, piano workout, calendario e versioni;
- `WorkoutAggregate`: routine, esercizi, sessioni, serie, timer e record;
- `NutritionAggregate`: alimenti con fonte, pasti pianificati, consumi, sostituzioni e costi;
- `ProgressAggregate`: misure, peso, foto locali e consenso per foto;
- `CoachingAggregate`: check-in, proposte, approvazioni, snapshot, rollback e outcome.

Ogni entità storica è append-only o versionata. Consumi e sessioni completate non vengono aggiornati retroattivamente. Le operazioni utente usano idempotency key.

## 5. Fasi verificabili

### Fase 0 – Fondazione e contratti

- nuovo package root `com.fitcoach.v2` senza modificare il codice v1;
- catalogo dipendenze aggiornato e build minima;
- contratti domain per repository, provider, generation job e timer;
- schema Room iniziale e migrazione testata;
- test di avvio, creazione profilo e persistenza.

**Gate:** build debug, test Room in-memory, nessun dato inventato.

### Fase 1 – Provider AI, prima della generazione

- `AiProviderAdapter` con OpenAI, OpenRouter, DeepSeek, Groq e custom OpenAI-compatible;
- capability per modello: token parameter, temperature, JSON schema, tools, reasoning, timeout;
- OpenAI predefinito con modello configurabile verificato tramite endpoint modelli;
- connessione reale con classificazione di 401/403/404/408/429/5xx, rete, timeout, credito e modello;
- chiave in Keystore/proxy, redaction log, nessuna chiave nei DTO persistiti o APK;
- risposta nullable/reasoning/tool calls/refusal gestiti senza mostrare reasoning interno;
- retry limitati, detection truncation, continuation solo se capability dichiarata;
- test JSON e contract test con server HTTP locale; provider reale solo con chiave fornita dall'utente.

**Gate:** nessuna generazione di programma viene abilitata se il test provider non è passato.

### Fase 2 – Anamnesi conversazionale

- domande progressivamente persistite con risposta `UNKNOWN` valida;
- copertura di stile di vita, sonno, stress, recupero, allenamento, carichi, pasti, budget, allergie e limitazioni;
- rilevazione di dati mancanti e contraddizioni;
- riepilogo modificabile e conferma esplicita;
- resume dopo riavvio e modifica successiva senza perdere lo storico.

**Gate:** tre profili fixture (principiante, intermedio, avanzato) con campi differenti e nessun campo inventato.

### Fase 3 – Generazione AI atomica e riprendibile

Job separati e riprendibili:

1. nutrizione 7 giorni;
2. workout e progressione iniziale;
3. calendario e periodizzazione;
4. obiettivi e indicatori.

Ogni fase salva solo un artefatto `DRAFT_VALIDATED`. Il commit finale usa una transazione e attiva una sola versione completa. Fallimenti, JSON troncato e retry non salvano piani parziali.

**Gate:** validatori matematici, alimenti/esercizi esistenti, allergie/attrezzatura, calendario coerente, macro e volume entro tolleranze.

### Fase 4 – Workout tracker

- routine manuali/AI, cartelle, duplicazione, ordine e sessioni libere;
- set editabili con carico, reps, RIR, RPE, tecnica, note, dolore e timestamp;
- ultima prestazione senza sovrascrivere storico;
- timer per esercizio persistito via WorkManager/AlarmManager, background e reboot;
- tecniche con regole e validazione;
- calendario settimanale/mensile con override e collisioni;
- statistiche per esercizio, volume, frequenza, record e 1RM stimato.

**Gate:** test di riapertura app, sessione completata, carichi mantenuti e record storico invariato.

### Fase 5 – Nutrizione, diario e spesa

- fonti Room/CREA/Open Food Facts/USDA con source ID, marca, crudo/cotto e sgocciolato;
- piano, alternative singole/composite, confronto prima/dopo e budget residuo;
- diario idempotente con pianificato/consumato/extra e target uguali in Home/Piano/Diario;
- quantità modificata = ricalcolo immediato; deselezione = rimozione consumo.

**Gate:** test di duplicazione, sostituzione, macro e costo su alimento seed, custom e remoto.

### Fase 6 – Body tracking e coaching

- misure complete e custom con timestamp/unità/lato/note;
- foto importate/scattate localmente, pose, confronto arbitrario e consenso per file;
- grafici con intervalli senza interpolare valori mancanti;
- coaching longitudinale con approvazione, snapshot, rollback e outcome;
- rispetto delle modifiche manuali e protezione dei record completati.

**Gate:** test privacy, revoca consenso, confronto non adiacente, proposta applicata e rollback.

### Fase 7 – Notifiche, backup e collaudo

- notifiche configurabili con reboot recovery;
- export/import verificato e backup cifrato dove necessario;
- offline test per tutte le funzioni locali;
- provider reali solo come test esplicitamente marcati;
- UI test su emulatore/device Samsung, ANR/crash e accessibilità;
- APK debug e release firmata se viene fornito un keystore.

## 6. Criteri di verità dei test

Ogni report separa:

- `AUTOMATIC`: eseguito nel build locale;
- `INTEGRATION`: eseguito contro Room/server locale;
- `PROVIDER_REAL`: eseguito contro un provider reale con credenziali disponibili;
- `UI_E2E`: eseguito su emulatore/device;
- `NOT_RUN`: non verificabile nell'ambiente corrente.

Nessun provider reale, test Samsung o release firmata verrà dichiarato superato senza una prova registrata.
