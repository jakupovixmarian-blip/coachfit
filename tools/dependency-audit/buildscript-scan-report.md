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
| `app/gradle.lockfile` (app) | 157 | **No issues found** |
| `buildscript-gradle.lockfile` (toolchain) | 155 | 14 pacchetti, 62 segnalazioni (27 high/critical) |

Le 62 segnalazioni stanno **tutte nella toolchain di build**, zero nell'app. Verificato
incrociando i nomi: nessuno dei 14 pacchetti colpiti compare in `app/gradle.lockfile`.

Pacchetti colpiti nel buildscript: guava, protobuf-java, commons-io, netty (codec, codec-http,
codec-http2, common, handler, handler-proxy), commons-compress, jdom2, bouncycastle
(bcpkix, bcprov), kotlin-gradle-plugin.

## Il finding Kotlin: fix disponibile, bloccato da Hilt

`org.jetbrains.kotlin:kotlin-gradle-plugin` 2.0.21 → GHSA-r937-wjx7-w2jp (CVE-2026-53914),
severità moderata (6.7): deserializzazione non sicura nella Kotlin Build Cache.

Verificato sull'API OSV il 2026-11-13: l'intervallo corretto è `fixed: 2.4.20-Beta1`, ma **2.4.20
è una release stabile** e non ha vulnerabilità. Il fix quindi esiste ed è stabile: il blocco non è
più l'assenza di una versione di Kotlin.

Esposizione reale, valutata sui file del progetto:

- `gradle.properties` ha `org.gradle.caching=true` → la cache è attiva.
- Nessuna cache remota: non c'è un blocco `buildCache { }` in `settings.gradle.kts`.
- L'attacco richiede di scrivere una voce malevola nella cache locale, cioè di avere già accesso
  in scrittura alla macchina che compila.
- Il plugin non viene impacchettato nell'APK.

Conclusione: il fix esiste (Kotlin 2.4.20 stabile) ma l'aggiornamento è una catena di sei
dipendenze — Kotlin → KSP → AGP → Gradle → Hilt → Room — e si ferma su Hilt 2.56.2, che incorpora
`kotlin-metadata-jvm` 2.2.0 e non legge il metadata di Kotlin 2.4:

```
error: [Hilt] Provided metadata instance has version 2.4.0, while maximum supported
version is 2.2.0.
```

Toolchain ripristinata e riverificata (`assembleDebug`, 61 test, APK 20.222.928 byte). Dettagli e
comando di ricontrollo in `kotlin-upgrade-attempt.md`. L'attesa ora è su Dagger/Hilt, non su
JetBrains.

## Cosa resta aperto, e perché

1. **Lockfile del buildscript**: chiuso con questo file. Lo scanner di AppJet lo legge e vi
   attribuisce 62 segnalazioni, 27 high/critical, tutte qui e **nessuna nell'app**;
   `tools/dependency-audit/scope_check.py` lo verifica a ogni esecuzione (14/14 pacchetti
   segnalati presenti solo nel buildscript, 0 nel grafo dell'app).

   Le versioni coinvolte sono quelle che AGP 8.7.3 e Gradle 8.11.1 trascinano con sé: non si
   aggiornano nel catalogo, si aggiornano portando avanti il toolchain. Un pin forzato qui, senza
   poter ricompilare, disallineerebbe il lockfile appena verificato con `assembleDebug`.
2. **Transitive dell'app**: coperte da `app/gradle.lockfile`, 0 problemi.
3. **Finding Kotlin**: fix stabile disponibile (2.4.20), bloccato dal tetto di metadata di Hilt
   2.56.2. Vedi `kotlin-upgrade-attempt.md`; ipotesi da provare: `languageVersion` 2.2 su Kotlin
   2.4.20, così il metadata emesso resta 2.2.0.
