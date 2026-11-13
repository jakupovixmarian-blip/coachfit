# Controllo dipendenze — stato e come chiudere la lacuna

## Cosa viene controllato oggi

- `pom.xml` (questa cartella) è un **manifest di solo audit**: elenca le coordinate dichiarate in
  `gradle/libs.versions.toml` con versione esplicita. Serve solo a dare all'OSV-Scanner di AppJet
  qualcosa di leggibile, **non** fa parte del build Gradle dell'app.
- `coverage_check.py` verifica che ogni libreria dichiarata nel catalogo Gradle sia presente nel
  manifest, così nessuna dipendenza **dichiarata** resta fuori dal controllo.
- `sync_pom_from_lockfile.py` **rigenera** il manifest: prende le 157 coordinate risolte dal
  lockfile e vi aggiunge le coordinate di compilazione/test e i **6 plugin di build** con la
  versione dichiarata in `gradle/libs.versions.toml`. `--check` fallisce se il pom non
  corrisponde, così non può tornare a divergere in silenzio.

## Perché i plugin di build sono nel manifest

AGP, Kotlin, Compose compiler, serialization, KSP e Hilt non compaiono nel lockfile dell'app,
quindi senza queste voci le loro versioni non venivano controllate da nessuno scan. Sono marcati
`<scope>provided</scope>` e la mappa id plugin → artefatto Maven è in
`sync_pom_from_lockfile.py`; le versioni non sono mai scritte a mano, si leggono dal catalogo.
Sono le 6 coordinate **dirette**, non l'intero grafo di build: resta esclusa l'infrastruttura
trasitiva (netty, guava, protobuf) che non finisce nell'APK.
  confronta con il manifest: segnala le versioni diverse da quelle dichiarate e quante coordinate
  esistono **solo come transitive**. È il passo che permette di portare nel manifest le versioni
  realmente usate nell'APK.

Esito attuale, con `app/gradle.lockfile` versionato nel repository:

- **157 pacchetti** risolti nel lockfile, di cui **127 presenti solo come transitive**;
- **321 pacchetti** esaminati dallo scanner, distribuiti su due grafi distinti: 157 coordinate
  nel grafo dell'app (`app/gradle.lockfile`), 155 nel classpath del buildscript
  (`buildscript-gradle.lockfile`), 174 voci nel manifest di audit. La separazione è verificata
  da `scope_check.py`, non solo dichiarata;
- **0 vulnerabilità note**, 0 bloccanti.

## Il lockfile deve restare ristretto alle classpath dell'app

`app/gradle.lockfile` copre solo `release`/`debug` Compile+Runtime. **Non** usare
`lockAllConfigurations()`: quel grafo include l'infrastruttura di build di Android e produce
51 segnalazioni (netty, guava, protobuf, commons-io, bouncycastle) che non finiscono nell'APK.
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

## Due grafi, un solo scope: perché 62 segnalazioni non riguardano l'app

`scope_check.py` incrocia i due lockfile e verifica a ogni esecuzione che nessuno dei pacchetti
segnalati compaia nel grafo dell'app. Esito attuale: app 157 coordinate, buildscript 155, 9
coordinate condivise (tutte infrastrutturali e senza segnalazioni: jsr305, javax.inject,
kotlin-stdlib, coroutines, listenablefuture, dagger, annotations), **0 pacchetti segnalati
nell'app**.

Le segnalazioni stanno nel classpath del buildscript: netty 4.1.93, bouncycastle 1.77, guava
32.0.1, protobuf-java 3.22.3, commons-io 2.13.0, commons-compress 1.21, jdom2 2.0.6,
kotlin-gradle-plugin 2.0.21. Sono dipendenze di AGP e Gradle, **non dichiarazioni nostre**: non
si aggiornano nel catalogo, si aggiornano portando AGP e Gradle a versioni più recenti. Forzare
qui le versioni senza poter compilare significherebbe pin arbitrari sul toolchain e un lockfile
disallineato rispetto a quello verificato con `assembleDebug`. Restano quindi visibili e vanno
affrontate insieme a un aggiornamento del toolchain.

## Finding noto sul toolchain di build (valutato, non nascosto)

Lo scan di revisione 42 segnala una vulnerabilità **moderata** (6.7) su
`org.jetbrains.kotlin:kotlin-gradle-plugin` 2.0.21:
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

Perché non è stato corretto: l'unica `fixed_version` pubblicata è **2.4.20-Beta1**, una beta. Un
salto 2.0.21 → 2.4.x trascina Compose compiler, serialization e KSP (oggi 2.0.21-1.0.28, legato
alla versione di Kotlin) e va verificato con `assembleDebug` e i 61 test. Va fatto dove Gradle e
l'SDK Android esistono, non qui. Quando esce una release **stabile** con la patch, la sequenza è:
aggiornare `kotlin` nel catalogo, allineare `ksp`, ricompilare, rilanciare gli scan.

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
