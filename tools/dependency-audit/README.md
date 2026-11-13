# Controllo dipendenze — stato e come chiudere la lacuna

## Cosa viene controllato oggi

- `pom.xml` (questa cartella) è un **manifest di solo audit**: elenca le coordinate dichiarate in
  `gradle/libs.versions.toml` con versione esplicita. Serve solo a dare all'OSV-Scanner di AppJet
  qualcosa di leggibile, **non** fa parte del build Gradle dell'app.
- `coverage_check.py` verifica che ogni libreria dichiarata nel catalogo Gradle sia presente nel
  manifest, così nessuna dipendenza **dichiarata** resta fuori dal controllo.
- `import_resolved.py` legge un file risolto da Gradle (`app/gradle.lockfile` o `deps.txt`) e lo
  confronta con il manifest: segnala le versioni diverse da quelle dichiarate e quante coordinate
  esistono **solo come transitive**. È il passo che permette di portare nel manifest le versioni
  realmente usate nell'APK.

Esito attuale, con `app/gradle.lockfile` versionato nel repository:

- **157 pacchetti** risolti nel lockfile, di cui **127 presenti solo come transitive**;
- **168 pacchetti** esaminati dallo scanner (157 del lockfile + 11 dichiarati usati solo in
  compilazione/test);
- **0 vulnerabilità note**, 0 bloccanti.

## Il lockfile deve restare ristretto alle classpath dell'app

`app/gradle.lockfile` copre solo `release`/`debug` Compile+Runtime. **Non** usare
`lockAllConfigurations()`: quel grafo include l'infrastruttura di build di Android e produce
51 segnalazioni (netty, guava, protobuf, commons-io, bouncycastle) che non finiscono nell'APK.
Se il lockfile viene rigenerato con tutte le configurazioni, va riportato a queste sole classpath.

## Cosa resta fuori

1. **Build tooling e test**, per scelta: non entrano nell'APK e quindi non sono nel lockfile
   ristretto. Le 11 coordinate dichiarate solo per ksp/test restano comunque coperte dal manifest.
2. **`build.gradle.kts` di root**: lo scanner segnala che non esiste un file risolto per il
   buildscript di root (solo versioni dei plugin). Va generato con Gradle.
3. Il risultato va letto come "0 vulnerabilità note su ciò che è stato risolto", **non** come
   "progetto pulito": non copre codice sorgente, runtime né vulnerabilità non ancora pubblicate.

## Perché il lockfile va generato fuori da AppJet

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

   L'import è automatizzato, quindi il passaggio è verificabile anche in locale:

   ```bash
   python3 tools/dependency-audit/import_resolved.py deps.txt
   # oppure: python3 tools/dependency-audit/import_resolved.py app/gradle.lockfile
   ```

   Lo script riconosce entrambi i formati (righe `group:artifact:version=config` del lockfile e
   nodi `+--- group:artifact:version` del report, incluse le correzioni `-> x.y.z`), scrive
   `resolved.json` con tutte le coordinate risolte ed esce con:

   - `0` manifest allineato alle versioni risolte;
   - `1` disallineamenti da correggere (drift di versione o coordinate dichiarate assenti dal grafo);
   - `2` file illeggibile o senza coordinate riconoscibili (esito vuoto: non è una copertura).

## Manutenzione

Quando cambi una versione in `gradle/libs.versions.toml`, aggiorna la coordinata corrispondente in
`pom.xml` e rilancia:

```bash
python3 tools/dependency-audit/coverage_check.py
```

Lo script esce con codice 0 se la copertura è completa, 1 se mancano coordinate, 2 se non riesce a
leggere il catalogo (esito vuoto: non è una copertura).
