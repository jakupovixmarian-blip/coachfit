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
| 7 – Collaudo | Test unitari motori, build APK debug/release | ✅ |

## Stack tecnologico

- **Kotlin**, **Jetpack Compose**, **Material 3**
- **MVVM + Clean Architecture** (domain / data / ui)
- **Room** (23 entità, DAO, migrazioni, schema esportato)
- **Coroutines / Flow**, **WorkManager**, **AlarmManager** (notifiche esatte)
- **DataStore** per le impostazioni, **Hilt** per la dependency injection
- **Retrofit / OkHttp + kotlinx.serialization** per le API AI
- Grafici con **Compose Canvas** (nessuna dipendenza esterna, offline)
- `minSdk 26`, `targetSdk 35`

Architettura **offline-first**: tutte le funzioni principali operano senza rete.
Solo la chat AI richiede Internet.

## Interfaccia

- Dark mode predefinita, nero/antracite con accento verde smeraldo.
- Bottom bar: **Dashboard · Alimentazione · Allenamento · Progressi · Coach AI**.
- La dashboard mostra giorno, tipo di giornata (HIGH/MEDIUM/LOW), calorie, macro,
  pasti previsti, allenamento del giorno, prossima notifica e stato progressi.

## Motori principali (deterministici, testati)

- `EnergyCalculator` – BMR (Mifflin-St Jeor / Katch-McArdle se nota la massa grassa),
  TDEE, target calorico, distribuzione macro, bilancio settimanale.
- `CarbCyclingEngine` – assegnazione giorni HIGH/MEDIUM/LOW con verifica di coerenza
  calorie/macro e bilancio settimanale (nessun deficit estremo nei giorni LOW).
- `MealPlanGenerator` – piano di 7 giorni, grammature, crudo/cotto, totali pasto e
  sostituzioni con quantità ricalcolate.
- `WorkoutPlanBuilder` + `SplitGenerator` – split Upper/Lower, Push/Pull/Legs, Full Body,
  volume settimanale per gruppo.
- `ProgressiveOverloadEngine` – **double progression**: mantiene il carico e sale di
  ripetizioni fino al top del range, poi incrementa il carico. Non aumenta il carico in
  caso di dolore, tecnica scorretta o RIR insufficiente.
- `AdaptiveEngine` + `WeightAnalyzer` – media settimanale del peso, trend, aderenza,
  performance; genera proposte strutturate (situazione, problema, dati, modifica,
  motivazione, risultato atteso, controindicazioni).

Separazione netta tra **motore matematico deterministico**, **motore di analisi** e
**AI conversazionale**: l'AI interpreta e propone, non calcola né modifica i database
direttamente.

## AI configurabile

Provider supportati: **OpenAI, Groq, OpenRouter, DeepSeek, custom OpenAI-compatible**
(inclusi endpoint gratuiti). Pannello Impostazioni → Provider AI con Base URL, modello,
API key, test connessione e stato.

Le chiavi API sono cifrate con **EncryptedSharedPreferences** (AES-256) e **non vengono
mai incluse nell'APK**. È disponibile l'opzione **backend proxy** per non tenere la chiave
sul dispositivo.

La chat conosce profilo, piano attivo, ciclicità, scheda, storico carichi, progressi e
modifiche precedenti. Le modifiche proposte vengono mostrate all'utente e applicate solo
dopo conferma, tramite funzioni controllate, con possibilità di **annullamento**.

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

# APK release (firmabile)
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

Test unitari dei motori matematici (67 test): energia, carb cycling, generazione pasti,
split, progressive overload, analisi peso, motore adattivo.

```bash
./gradlew :app:testDebugUnitTest
```

## Documentazione

- `docs/ROADMAP.md` – roadmap tecnica, decisioni architetturali, fasi.

## Avvertenza

FitCoach non fornisce consulenza medica. I piani generati sono basati su formule
documentate e sui dati inseriti dall'utente, ma non sostituiscono il parere di un medico,
nutrizionista o professionista qualificato.
