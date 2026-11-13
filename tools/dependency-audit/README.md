# Controllo dipendenze — stato e come chiudere la lacuna

## Cosa viene controllato oggi

- `pom.xml` (questa cartella) è un **manifest di solo audit**: elenca le coordinate dichiarate in
  `gradle/libs.versions.toml` con versione esplicita. Serve solo a dare all'OSV-Scanner di AppJet
  qualcosa di leggibile, **non** fa parte del build Gradle dell'app.
- `coverage_check.py` verifica che ogni libreria dichiarata nel catalogo Gradle sia presente nel
  manifest, così nessuna dipendenza **dichiarata** resta fuori dal controllo.
- `sync_pom_from_lockfile.py` **rigenera** il manifest: prende le 162 coordinate risolte dal
  lockfile e vi aggiunge le coordinate di compilazione/test e i **6 plugin di build** con la
  versione dichiarata in `gradle/libs.versions.toml`. `--check` fallisce se il pom non
  corrisponde, così non può tornare a divergere in silenzio.

## Perché i plugin di build sono nel manifest

AGP, Kotlin, Compose compiler, serialization, KSP e Hilt non compaiono nel lockfile dell'app,
quindi senza queste voci le loro versioni non venivano controllate da nessuno scan. Sono marcati
`<scope>provided</scope>` e la mappa id plugin → artefatto Maven è in
`sync_pom_from_lockfile.py`; le versioni non sono mai scritte a mano, si leggono dal catalogo.
Sono le 6 coordinate **dirette**, non l'intero grafo di build: resta esclusa l'infrastruttura
trasitiva (netty, guava, bouncycastle) che non finisce nell'APK.
  confronta con il manifest: segnala le versioni diverse da quelle dichiarate e quante coordinate
  esistono **solo come transitive**. È il passo che permette di portare nel manifest le versioni
  realmente usate nell'APK.

Esito attuale, con `app/gradle.lockfile` versionato nel repository:

- **162 pacchetti** risolti nel lockfile, di cui **127 presenti solo come transitive**;
- **324 pacchetti** esaminati dallo scanner, distribuiti su due grafi distinti: 162 coordinate
  nel grafo dell'app (`app/gradle.lockfile`), 156 nel classpath del buildscript
  (`buildscript-gradle.lockfile`), 174 voci nel manifest di audit. La separazione è verificata
  da `scope_check.py`, non solo dichiarata;
- **app: 0 vulnerabilità note, 0 bloccanti.** Il repository intero, invece, riporta **49
  segnalazioni e 24 bloccanti** (3 critical, 21 high): stanno tutte nel classpath del buildscript.
  Le due cifre non vanno confuse — vedi la sezione sui due grafi.

## Il lockfile deve restare ristretto alle classpath dell'app

`app/gradle.lockfile` copre solo `release`/`debug` Compile+Runtime. **Non** usare
`lockAllConfigurations()`: quel grafo include l'infrastruttura di build di Android e produce
49 segnalazioni (netty, guava, bouncycastle, commons-compress, jdom2, jose4j) che non
finiscono nell'APK.
Se il lockfile viene rigenerato con tutte le configurazioni, va riportato a queste sole classpath.

## Cosa resta fuori

1. **Build tooling e test**, per scelta: non entrano nell'APK e quindi non sono nel lockfile
   ristretto. Le 11 coordinate dichiarate solo per ksp/test e i 6 plugin di build restano
   comunque coperte dal manifest, con le versioni del catalogo.
2. **`build.gradle.kts` di root**: il lockfile del buildscript ora esiste
   (`buildscript-gradle.lockfile`, generato con Gradle 8.11.1) e lo scanner lo legge: le 62
   segnalazioni citano quel file. Il warning viene comunque emesso perché lo scanner si aspetta
   un nome o un percorso predefinito diverso: è un limite di policy, non un dato mancante.
3. Il risultato va letto come "0 vulnerabilità note su ciò che è stato risolto", **non** come
   "progetto pulito": non copre codice sorgente, runtime né vulnerabilità non ancora pubblicate.

## Due grafi, un solo scope: perché 49 segnalazioni non riguardano l'app

`scope_check.py` incrocia i due lockfile e verifica a ogni esecuzione che nessuno dei pacchetti
segnalati compaia nel grafo dell'app. Esito attuale: app 162 coordinate, buildscript 156, 11
coordinate condivise (tutte infrastrutturali e senza segnalazioni: jsr305, javax.inject,
kotlin-stdlib, coroutines, listenablefuture, dagger, annotations), **0 pacchetti segnalati
nell'app**.

Le segnalazioni stanno nel classpath del buildscript: netty 4.1.110.Final, bouncycastle 1.79,
guava 33.3.1-jre, commons-compress 1.21, jdom2 2.0.6, jose4j 0.9.5. Sono dipendenze di AGP e Gradle, **non dichiarazioni nostre**: non
si aggiornano nel catalogo, si aggiornano portando AGP e Gradle a versioni più recenti. Forzare
qui le versioni senza poter compilare significherebbe pin arbitrari sul toolchain e un lockfile
disallineato rispetto a quello verificato con `assembleDebug`. Restano quindi visibili e vanno
affrontate insieme a un aggiornamento del toolchain.

## Finding noto sul toolchain di build (valutato, non nascosto)

Lo scan di revisione 42 segnala una vulnerabilità **moderata** (6.7) su
`org.jetbrains.kotlin:kotlin-gradle-plugin` 2.0.21 — **CHIUSO**: il buildscript ora risolve 2.4.20, non più segnalato:
[GHSA-r937-wjx7-w2jp](https://osv.dev/GHSA-r937-wjx7-w2jp) / CVE-2026-53914, deserializzazione
non sicura nella Kotlin Build Cache.

Esposizione in questo repository, verificata sui file:

- `gradle.properties` ha `org.gradle.caching=true`, quindi la cache è attiva;
- **non** esiste alcuna cache remota: nessun blocco `buildCache { }` in `settings.gradle.kts` né
  `remote(...)`, quindi si usa solo la cache locale sotto `~/.gradle/caches/build-cache-1`;
- il plugin è tooling di build: non viene impacchettato nell'APK.

Conseguenza: l'attacco richiede di piazzare una voce malevola nella cache locale, cioè di avere
già accesso in scrittura alla macchina che compila. Il rischio reale qui è basso, ma il finding
resta valido e visibile.

Come è stato chiuso: l'aggiornamento non è una patch ma una catena — Kotlin 2.4.20 → KSP 2.3.13
(vuole AGP ≥ 8.12) → AGP 8.12.3 (vuole Gradle ≥ 8.13) → Gradle 8.13 (`jvmTarget` solo in
`compilerOptions`) → Hilt 2.56.2 → Room 2.8.5. Il muro era Hilt 2.56.2, che incorpora
`kotlin-metadata-jvm` 2.2.0 e non legge il metadata di Kotlin 2.4:

```
error: [Hilt] Provided metadata instance has version 2.4.0, while maximum supported
version is 2.2.0.
```

Il muro è stato aggirato senza toccare Hilt: forzando `languageVersion`/`apiVersion` a 2.2 su
Kotlin 2.4.20, il compilatore emette metadata 2.2.0, che Hilt 2.56.2 legge.

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}
```

Verificato sul bytecode (`javap -v` su `FitCoachApp.class` → `mv=[2,2,0]`), non solo ipotizzato:
`assembleDebug` + 61 test → BUILD SUCCESSFUL, e la CVE non compare più in nessuno dei due lockfile.
Toolchain attuale: Kotlin 2.4.20, KSP 2.3.13, AGP 8.12.3, Gradle 8.13, Hilt 2.56.2, Room 2.8.5.
Passo-passo completo in `kotlin-upgrade-attempt.md`.

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
