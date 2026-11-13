# Lockfile del buildscript di root — esito e lettura

Generato con Gradle 8.11.1 + Android SDK 35 (JDK 21) il 2026-11-13.

## Cosa è stato prodotto

- `buildscript-gradle.lockfile` (root, 159 righe): risolve la configurazione `classpath` del
  buildscript, cioè i 6 plugin di build con le loro transitive.
- `build.gradle.kts`: `buildscript { configurations.classpath { resolutionStrategy
  .activateDependencyLocking() } }`.

Comando: `./gradlew --write-locks help`

Verificato che regga: `./gradlew :app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL,
61 test, 0 falliti.

## Esito OSV-Scanner

| Lockfile | Pacchetti | Esito |
|---|---|---|
| `app/gradle.lockfile` (app) | 162 | **No issues found** |
| `buildscript-gradle.lockfile` (toolchain) | 156 | 12 pacchetti, 49 segnalazioni (3 critical, 21 high) |

Le 49 segnalazioni stanno **tutte nella toolchain di build**, zero nell'app. Verificato
incrociando i nomi: nessuno dei 12 pacchetti colpiti compare in `app/gradle.lockfile`.

Pacchetti colpiti nel buildscript: guava 33.3.1-jre, netty 4.1.110.Final (codec, codec-http,
codec-http2, common, handler, handler-proxy), commons-compress 1.21, jose4j 0.9.5, jdom2 2.0.6,
bouncycastle (bcpkix, bcprov) 1.79. `kotlin-gradle-plugin`, `protobuf-java` e `commons-io` non
sono più segnalati: il primo per la chiusura della CVE, gli altri perché la toolchain aggiornata
ne risolve versioni pulite.

## Il finding Kotlin: CHIUSO

`org.jetbrains.kotlin:kotlin-gradle-plugin` 2.0.21 → GHSA-r937-wjx7-w2jp (CVE-2026-53914),
severità moderata (6.7): deserializzazione non sicura nella Kotlin Build Cache.

**Risolto.** Il buildscript ora risolve `kotlin-gradle-plugin:2.4.20`, che OSV non segnala. La
vulnerabilità non compare più in nessuno dei due lockfile.

Il percorso non era banale: l'aggiornamento è una catena di sei dipendenze — Kotlin → KSP → AGP →
Gradle → Hilt → Room — e si fermava su Hilt 2.56.2, che incorpora `kotlin-metadata-jvm` 2.2.0 e
non legge il metadata di Kotlin 2.4. Il muro è stato aggirato forzando `languageVersion`/`apiVersion`
a 2.2 in `app/build.gradle.kts`, così Kotlin 2.4.20 emette metadata 2.2.0. Verificato sul bytecode
(`javap -v` → `mv=[2,2,0]`).

Verifiche: `assembleDebug` + 61 test → BUILD SUCCESSFUL; app 162 pacchetti, OSV pulito. Dettagli in
`kotlin-upgrade-attempt.md`.

Esposizione che il finding aveva, comunque limitata: cache Kotlin solo locale (nessun blocco
`buildCache { }`), attacco possibile solo con accesso in scrittura alla macchina che compila, plugin
non impacchettato nell'APK. Non è più un rischio attivo.

## Cosa resta aperto, e perché

1. **Lockfile del buildscript**: chiuso con questo file. Lo scanner di AppJet lo legge e vi
   attribuisce 49 segnalazioni, tutte qui e **nessuna nell'app**;
   `tools/dependency-audit/scope_check.py` lo verifica a ogni esecuzione (12/12 pacchetti
   segnalati presenti solo nel buildscript, 0 nel grafo dell'app).

   Le versioni coinvolte sono transitive di AGP 8.12.3 e Gradle 8.13, che restano indietro: non si
   aggiornano nel catalogo, si aggiornano portando avanti il toolchain. Un pin forzato qui, senza
   poter ricompilare, disallineerebbe il lockfile appena verificato con `assembleDebug`.
2. **Transitive dell'app**: coperte da `app/gradle.lockfile`, 0 problemi.
3. **Finding Kotlin**: chiuso. Kotlin 2.4.20 nel buildscript, CVE-2026-53914 non più presente.
