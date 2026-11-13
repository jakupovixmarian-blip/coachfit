# Controllo dipendenze — stato e come chiudere la lacuna

## Cosa viene controllato oggi

- `pom.xml` (questa cartella) è un **manifest di solo audit**: elenca le coordinate dichiarate in
  `gradle/libs.versions.toml` con versione esplicita. Serve solo a dare all'OSV-Scanner di AppJet
  qualcosa di leggibile, **non** fa parte del build Gradle dell'app.
- `coverage_check.py` verifica che ogni libreria dichiarata nel catalogo Gradle sia presente nel
  manifest, così nessuna dipendenza **dichiarata** resta fuori dal controllo.

Esito attuale: 38 voci nel catalogo `[libraries]`, 41 coordinate nel manifest, 0 dichiarate scoperte.

## Cosa NON è coperto

1. **Dipendenze transitive.** Lo scanner di AppJet non le risolve per i manifest Maven.
2. **Nessun file di dipendenze risolto** per `app/build.gradle.kts` e `build.gradle.kts`
   (il progetto non ha dependency locking attivo).

Questo significa che il risultato va letto come "0 vulnerabilità note tra i pacchetti dichiarati",
**non** come "progetto pulito".

## Perché non si chiude in AppJet

- `gradle` non è installato (solo il wrapper nel repo) e non esiste alcuna distribuzione in cache.
- Non c'è Android SDK: `ANDROID_HOME`/`ANDROID_SDK_ROOT` vuoti, `sdkmanager` assente, nessuna
  directory SDK. Senza SDK il plugin Android non configura il progetto, quindi
  `:app:dependencies` non può girare.
- Riprodurre il grafo con Maven **non funziona**: tre tentativi reali (`dependency:list`, poi pin
  diretti, poi `dependencyManagement`) falliscono sui range interni di Compose
  (`foundation-layout` `[1.7.6,1.7.6]` contro `1.7.0`/`1.7.2` imposti da `material3 1.3.1` e
  `navigation-compose 2.8.5`). Maven non replica la conflict resolution di Gradle.

## Come chiuderla in locale

Prerequisiti: JDK 17+ e Android SDK installati, progetto aperto in Android Studio o con
`ANDROID_HOME` valido.

1. Genera il grafo risolto delle configurazioni che finiscono nell'APK:

   ```bash
   ./gradlew :app:dependencies --configuration releaseRuntimeClasspath > deps.txt
   ```

2. (Consigliato) Attiva il dependency locking così il grafo diventa un file versionabile.
   In `app/build.gradle.kts`:

   ```kotlin
   dependencyLocking { lockAllConfigurations() }
   ```

   poi:

   ```bash
   ./gradlew :app:dependencies --write-locks
   # produce app/gradle.lockfile
   ```

3. Scansiona il file risolto con OSV-Scanner:

   ```bash
   osv-scanner --lockfile=app/gradle.lockfile
   ```

4. Riporta qui `deps.txt` o `app/gradle.lockfile`: aggiorno il manifest con le versioni
   effettivamente risolte e rilancio il controllo, così la copertura include anche le transitive.

## Manutenzione

Quando cambi una versione in `gradle/libs.versions.toml`, aggiorna la coordinata corrispondente in
`pom.xml` e rilancia:

```bash
python3 tools/dependency-audit/coverage_check.py
```

Lo script esce con codice 0 se la copertura è completa, 1 se mancano coordinate, 2 se non riesce a
leggere il catalogo (esito vuoto: non è una copertura).
