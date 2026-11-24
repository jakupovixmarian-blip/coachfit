# Prompt di audit completo di FitCoach

Da incollare in una nuova conversazione OpenHands. Contiene il comportamento
atteso dell'app, il metodo di verifica e i difetti già noti da non riscoprire.

---

## Ruolo

Agisci come team di revisione composto da: Senior Android Developer, Kotlin /
Jetpack Compose Engineer, Software Architect, AI Engineer (LLM e agenti con tool
calling), nutrizionista sportivo, Strength & Conditioning Specialist, QA
Engineer.

Non sei qui per aggiungere funzionalità. Sei qui per **verificare che FitCoach
faccia quello che dice di fare**, trovare dove non lo fa, e correggere. Una
funzionalità che dichiara un risultato senza produrlo è un difetto grave quanto
un crash.

## Contesto tecnico

- Repo `/workspace/project`, branch `appjet-grafica-test`, package `com.fitcoach.app`.
- Kotlin, Compose M3, MVVM + Clean, Room, Hilt, Coroutines/Flow, Retrofit/OkHttp,
  WorkManager, CameraX, ML Kit. `minSdk 26`, `targetSdk 35`.
- Build: `JAVA_HOME=/workspace/toolchain/jdk-17`,
  `ANDROID_HOME=/workspace/toolchain/android-sdk`,
  `GRADLE_USER_HOME=/workspace/gradle-home`, **sempre `--offline`**.
- UI interamente in italiano.
- Leggi `AGENTS.md` prima di toccare il codice: contiene le trappole già scoperte.

## Regola zero: nessun dato inventato

Questa è la regola che l'app viola più spesso, e vale anche per te.

- Non inventare valori nutrizionali, fattori di attività, percentuali, soglie o
  parametri di allenamento. Ogni numero nel codice deve avere una fonte
  rintracciabile (studio, linea guida, banca dati) oppure essere dichiarato come
  convenzione esplicita.
- Non dichiarare completata una funzionalità senza averla eseguita e verificata.
  Se non è verificabile, scrivi perché e cosa manca.
- Non usare dati fittizi come se fossero misurazioni reali. I campi nullable del
  profilo rappresentano dati assenti: non sostituirli con default plausibili.
- Distingui sempre **stima** da **dato registrato** nella UI e nei payload.

## Cosa deve fare FitCoach

FitCoach è un coach digitale per ipertrofia, nutrizione sportiva e
ricomposizione. Non è un diario alimentare né un registro allenamenti.

**Il sistema pianifica e analizza. L'utente esegue e registra i risultati.**
L'utente non deve mai programmare a mano la progressione dei carichi, né
calcolare calorie e macro.

### 1. Profilo e onboarding
Raccogliere età, sesso biologico (serve alle formule), altezza, peso, % grasso
facoltativa, esperienza, obiettivo, giorni disponibili, orari dei pasti, orari
di allenamento, attrezzatura, preferenze alimentari, alimenti esclusi, allergie
e intolleranze, limitazioni fisiche e movimenti da evitare, budget alimentare.
Obiettivi: ricomposizione, ipertrofia, definizione, mantenimento.
Ogni parametro modificabile in seguito.

### 2. Motore nutrizionale
- BMR e TDEE con formule documentate; override manuale possibile; stima e dato
  reale distinti.
- **Carb cycling** con giornate HIGH / MEDIUM / LOW, personalizzabili in numero
  e in macro. Coerenza obbligatoria: kcal = 4P + 4C + 9G su ogni giornata, e
  bilancio settimanale coerente con l'obiettivo.
- Nessun deficit arbitrario nei giorni LOW. Il vincolo vero è il **tasso di
  calo**: 0,5-1% del peso a settimana (Helms 2014). Oltre l'1% il piano non è
  valido, non è solo "sconsigliato".
- Piano alimentare di 7 giorni, 5 pasti (colazione, spuntino, pranzo, spuntino,
  cena). Ogni voce: alimento, grammi, crudo/cotto, kcal, P, C, G, totale pasto.
- Catalogo alimentare con valori verificabili (CREA, USDA), nomi italiani,
  fonte accanto a ogni voce.
- Sostituzione alimenti con quantità ricalcolate per mantenere valori
  comparabili.
- Modifica adattiva su valutazione **settimanale**: peso medio, trend, aderenza,
  prestazioni, composizione corporea se disponibile. Mai su oscillazioni
  giornaliere. Ogni modifica motivata e storicizzata.

### 3. Motore di allenamento
- Split automatico (Upper/Lower, Push/Pull/Legs, Full Body, personalizzate) in
  base a esperienza, disponibilità, recupero, obiettivo.
- Esercizi con gruppo muscolare primario e secondario, attrezzatura, tecnica,
  serie, range ripetizioni, recupero, RIR, alternative.
- Volume settimanale per gruppo muscolare entro le linee guida.
- **Progressive overload a doppia progressione**: l'utente registra solo carico,
  ripetizioni, serie completate, RIR/RPE. Il motore decide l'incremento.
  Nessun aumento se tecnica scorretta, RIR insufficiente o dolore.
  Multiarticolari, isolamento e corpo libero gestiti separatamente.
- Periodizzazione a blocchi: mesociclo, progressione, deload giustificato,
  rivalutazione. Il sistema non cambia schede senza motivo.
- Gestione affaticamento: calo prestazioni, stagnazione, volume, recupero, RIR,
  RPE, dolore, sonno. Non interpretare ogni stagnazione come "serve più volume".

### 4. Motore adattivo
Tre livelli separati e non intercambiabili:
1. **Matematico deterministico** — fa i calcoli.
2. **Analisi** — interpreta i risultati.
3. **AI conversazionale** — spiega e propone, non calcola e non scrive.

Ogni proposta di modifica contiene: situazione attuale, problema, dati usati,
modifica suggerita, motivazione, risultato atteso, controindicazioni.
Le modifiche importanti richiedono approvazione. Ogni modifica è annullabile.

### 5. Chat AI
Deve conoscere profilo, piano attivo, carb cycling, scheda, storico carichi,
progressi, preferenze, modifiche precedenti, obiettivi. Deve poter proporre
modifiche strutturate e applicarle tramite funzioni controllate dopo conferma.
Una chat che risponde senza vedere i dati del programma è un difetto.

### 6. Provider AI
OpenAI, Groq, OpenRouter, DeepSeek e endpoint OpenAI-compatibili. Pannello con
provider, API key, base URL, modello, test connessione, stato. Deve esistere
un'opzione gratuita. Le chiavi non stanno nell'APK: oggi sono in
`EncryptedSharedPreferences` (`SecureKeyStore`), che è accettabile per uso
personale ma non è la soluzione della specifica (backend proxy).

### 7. Notifiche
Pasti, inizio allenamento, idratazione, check-in settimanale, pesata.
Personalizzabili: orari, on/off, posticipo, conferma pasto/allenamento.
Ripristinate dopo il riavvio. Permessi Android gestiti.

### 8. Progressi
Grafici per peso, media settimanale, circonferenze, calorie, macro, carichi,
ripetizioni, volume, progressione forza. Storico settimanale, mensile,
trimestrale. Esportazione dati.

### 9. Sicurezza
L'AI non è un professionista sanitario: nessuna diagnosi, nessuna prescrizione.
Controlli su allergie, intolleranze, limitazioni fisiche. In caso di dolore
l'utente deve poter interrompere e segnalare. Nessun deficit estremo, nessun
volume irragionevole. Verifiche matematiche su tutte le calorie e i macro.

## Metodo di lavoro

Lavora in **tre passaggi separati**. Non mescolarli.

### Passaggio 1 — Mappa
Prima di modificare qualsiasi cosa, produci:
- elenco delle schermate realmente raggiungibili dalla navigazione;
- elenco delle funzionalità dichiarate ma non collegate (pulsanti senza azione,
  ViewModel non istanziati, moduli orfani);
- elenco dei punti in cui un numero viene mostrato all'utente: da dove arriva;
- elenco delle costanti numeriche nei motori, con la fonte di ciascuna o la
  dichiarazione che è una convenzione.

Nota già accertata: esiste `app/src/main/java/com/fitcoach/app/v2/` (19 file:
domain, ai, presentation) **non collegato a nulla** — nessun import da fuori.
Verifica se è codice morto da rimuovere o lavoro da completare, e dillo.

### Passaggio 2 — Verifica per comportamento
Per ogni funzionalità della lista sopra, scrivi un test che la esercita dal
punto di ingresso reale (ViewModel, use case, repository) e **prova a romperla**
con dati che l'app riceve davvero: zero, negativo, `null`, `Infinity`, stringhe
del modello AI, date passate, profili incompleti.

Un test che verifica solo che l'id del piano sia cambiato non prova che il
contenuto sia cambiato. Un test che passa perché una transazione fa rollback non
prova che la funzione funzioni.

### Passaggio 3 — Correzione
Per ogni difetto: causa, correzione minima, test di regressione che diventa
rosso senza la correzione. Poi build e suite completa.

## Difetti già noti (non riscoprirli, verifica se sono chiusi)

1. **Banda PAL che collassa** — risolto in 2.5.8. Con 4-5 allenamenti la banda
   era `[1,99, 2,00]`, larga 0,01: la correzione per stile di vita sedentario
   veniva annullata e il TDEE usciva gonfiato. **Aperto:** il controllo sul
   target calorico confronta le kcal con `safetyFloor` (BMR×0,85 = 1430 kcal),
   non il *fattore implicito* (`target / BMR`) con i limiti fisiologici. Un
   target di 2150 kcal su BMR 1682 passa il cancello pur essendo sotto il
   fabbisogno di una persona sedentaria (1,40 × 1682 = 2355).
2. **`@Insert(REPLACE)` su padri con figli CASCADE** — in SQLite REPLACE è
   DELETE+INSERT: la cascata cancella i figli. Ha reso il deload inservibile.
   Cerca altri punti con lo stesso schema.
3. **Falso successo** — più percorsi dichiaravano "applicato" senza applicare
   (`clonePlanVersion` con id azzerato, `rescaleDay` con catalogo non
   risolvibile, `propose_regenerate_plan` senza direttiva, `applyMoveWorkout`
   su un campo che nessuno legge). **Cerca sistematicamente** ogni punto in cui
   una funzione torna `true`/mostra un messaggio di successo senza aver scritto
   nulla.
4. **Numeri inventati dall'AI** — il modello ha proposto "fattore attività
   1,275", che non esiste in nessuna banda FAO. Verifica quali payload espongono
   al modello i parametri usati, e dove il modello è costretto a dedurli.

## Vincoli

- Non sovrascrivere file senza averli letti.
- Non lasciare TODO su funzioni importanti.
- Non creare pulsanti senza azione.
- Test per i calcoli, sempre.
- Build e test dopo ogni correzione; correggi prima di proseguire.
- Mantieni `README.md` e `AGENTS.md` aggiornati.
- Commit piccoli e motivati; non fare push se non richiesto.

## Consegna

Un report con, per ogni difetto: **cosa dichiara l'app / cosa fa davvero /
causa / correzione / test che lo copre**. In fondo, l'elenco esplicito di ciò
che **non** hai potuto verificare e perché. Un audit che dice "tutto ok" senza
mostrare i test che lo dimostrano non è un audit.
