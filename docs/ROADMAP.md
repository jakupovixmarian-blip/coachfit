# FitCoach – Roadmap Tecnica

## 1. Ambiente di sviluppo (analizzato)

| Componente | Stato | Note |
|---|---|---|
| OS | Debian 13 (trixie) x86_64 | 4 CPU, 15 GB RAM, 53 GB liberi |
| JDK | OpenJDK 21.0.13 | Installato in `/usr/lib/jvm/java-21-openjdk-amd64` |
| Android SDK | `/opt/android-sdk` | cmdline-tools latest, platform-tools r37 |
| Android Platform | API 34 (`android-34`) | `minSdk = 26`, `targetSdk = 34` |
| Build Tools | 34.0.0 | |
| Gradle | 8.11.1 (`/opt/gradle-8.11.1`) | wrapper incluso nel progetto |
| Rete | Accesso a dl.google.com, repo1.maven.org, plugins.gradle.org | Verificato |

Nessun progetto preesistente: il repository era vuoto (solo `.git`).

## 2. Scelte architetturali

- **Linguaggio**: Kotlin.
- **UI**: Jetpack Compose + Material 3, dark theme di default.
- **Architettura**: MVVM + Clean Architecture a 3 layer (`data`, `domain`, `ui`).
  - `domain`: modelli puri, motori di calcolo (nutrizione, allenamento, adattivo) — **testabili senza Android**.
  - `data`: Room, DataStore, Retrofit, repository implementations.
  - `ui`: Compose screens + ViewModel + UiState.
- **DI**: Hilt.
- **Persistenza**: Room (offline-first). DataStore per impostazioni.
- **Async**: Coroutines + Flow.
- **Background**: WorkManager (check-in settimanale, analisi) + AlarmManager (notifiche a orario esatto pasti/allenamento).
- **Grafici**: Vico (compose-m3).
- **AI**: Retrofit/OkHttp verso endpoint compatibili OpenAI; il layer AI è isolato dietro un'interfaccia provider-agnostica.

### Principio chiave: separazione dei motori
1. **Motore matematico deterministico** – calcoli puri (BMR, TDEE, macro, carb cycling, progressione). Nessuna AI.
2. **Motore di analisi** – regole deterministiche sui dati reali che producono *proposte di modifica* motivate.
3. **AI conversazionale** – interpreta e spiega, propone; **non** modifica i dati direttamente. Le modifiche passano da funzioni controllate e approvazione utente.

### Gestione segreti API
Le chiavi API non vengono hardcoded nell'APK. L'app supporta:
- inserimento chiave da parte dell'utente, salvata in **EncryptedSharedPreferences** (Android Keystore);
- supporto a un **Base URL backend** configurabile: se impostato, le chiavi restano sul backend e l'app invia solo i payload.

## 3. Roadmap per fasi

### FASE 1 – Fondamenta ✅
- Progetto Gradle, moduli, version catalog.
- Theme dark, navigazione bottom bar (5 tab).
- Database Room: tutte le entità + DAO + repository + migrazioni.
- Onboarding profilo utente (multi-step).
- Dashboard con dati reali (giorno, tipo giornata, macro, pasti, allenamento, prossima notifica, progressi).

### FASE 2 – Nutrizione
- Food database (valori CREA / USDA verificabili).
- BMR (Mifflin-St Jeor, Katch-McArdle), TDEE, target calorico, macro.
- Carb cycling HIGH/MEDIUM/LOW con validazione coerenza calorie↔macro e bilancio settimanale.
- Generazione piano 7 giorni (5 pasti/giorno), grammature, crudo/cotto, totali.
- Sostituzioni alimentari con quantità ricalcolate.

### FASE 3 – Allenamento
- Exercise DB (gruppo muscolare, attrezzatura, tecnica, range reps, RIR, alternative).
- Generazione split (U/L, PPL, Full Body, custom) in base a esperienza/disponibilità.
- Periodizzazione a blocchi/mesocicli + deload.
- Registrazione prestazioni (carico, reps, serie, RIR/RPE).
- Double progression automatica, separata per multiarticolari / isolamento / corpo libero.

### FASE 4 – Coach adattivo
- Analisi peso (medie settimanali, trend), aderenza, performance, composizione.
- Regole decisionali deterministiche → proposte strutturate (situazione, problema, dati, modifica, motivazione, risultato atteso, controindicazioni).
- Check-in settimanale, storico decisioni, rollback modifiche.

### FASE 5 – AI
- Chat con memoria strutturata (profilo, piano attivo, storico carichi, modifiche).
- Provider configurabili (OpenAI, Groq, OpenRouter, DeepSeek, custom OpenAI-compatible).
- Test connessione, stato, gestione errori.
- Funzioni controllate (tool/function calling) che propongono modifiche da approvare.

### FASE 6 – Notifiche e statistiche
- Notifiche pasti/allenamento/idratazione/check-in con orari personalizzabili, posticipo, conferma.
- Ripristino dopo reboot (BootReceiver).
- Grafici peso, macro, carichi, volume, forza; storico settimanale/mensile/trimestrale.
- Export dati (CSV/JSON) e backup.

### FASE 7 – Collaudo
- Unit test motori matematici, analisi, progressione.
- Test Room (in-memory) DAO.
- Build APK debug + release.

## 4. Dipendenze principali

AGP 8.7.3 · Kotlin 2.0.21 · KSP 2.0.21-1.0.28 · Compose BOM 2024.12.01 · Room 2.6.1 · Hilt 2.52 · Navigation-Compose 2.8.5 · DataStore 1.1.1 · WorkManager 2.10.0 · Retrofit 2.11.0 · OkHttp 4.12.0 · kotlinx-serialization 1.7.3 · Vico 2.0.0-alpha.28 · Security-crypto 1.1.0-alpha06.

## 5. Vincoli di sicurezza (implementati come regole)
- L'AI non è un professionista sanitario; nessuna diagnosi/farmaco.
- Controlli allergie/intolleranze/limitazioni negli algoritmi.
- Blocco deficit estremi (floor calorico) e volumi irragionevoli.
- Validazione matematica calorie/macro su ogni piano.
