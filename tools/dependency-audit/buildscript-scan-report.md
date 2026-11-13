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
| `buildscript-gradle.lockfile` (toolchain) | 155 | 15 pacchetti, 61 vulnerabilità |

Le 61 vulnerabilità stanno **tutte nella toolchain di build**, zero nell'app. Verificato
incrociando i nomi: nessuno dei 15 pacchetti colpiti compare in `app/gradle.lockfile`.

Pacchetti colpiti nel buildscript: guava, protobuf-java, commons-io, netty (codec, codec-http,
codec-http2, common, handler, handler-proxy), commons-compress, jdom2, bouncycastle
(bcpkix, bcprov), kotlin-gradle-plugin.

## Il finding Kotlin: reale, senza fix stabile

`org.jetbrains.kotlin:kotlin-gradle-plugin` 2.0.21 → GHSA-r937-wjx7-w2jp (CVE-2026-53914),
severità moderata (6.7): deserializzazione non sicura nella Kotlin Build Cache.

Verificato sull'API OSV: l'intervallo corretto è `fixed: 2.4.20-Beta1`. Su Maven Central l'ultima
release **stabile** è 2.2.0, che resta vulnerabile. Quindi **non esiste una versione stabile con
la patch**: l'unico fix pubblicato è una beta.

Esposizione reale, valutata sui file del progetto:

- `gradle.properties` ha `org.gradle.caching=true` → la cache è attiva.
- Nessuna cache remota: non c'è un blocco `buildCache { }` in `settings.gradle.kts`.
- L'attacco richiede di scrivere una voce malevola nella cache locale, cioè di avere già accesso
  in scrittura alla macchina che compila.
- Il plugin non viene impacchettato nell'APK.

Conclusione: rischio accettato e documentato. L'aggiornamento a 2.4.20-Beta1 comporterebbe un
salto 2.0.21 → 2.4.x che trascina Compose compiler, serialization e KSP (legato alla versione di
Kotlin) e introdurrebbe una beta in produzione. Va fatto quando esce una stabile con la patch.

## Cosa resta aperto, e perché

1. **Lockfile del buildscript**: chiuso con questo file.
2. **Transitive dell'app**: coperte da `app/gradle.lockfile`, 0 problemi.
3. **Finding Kotlin**: documentato, nessun fix stabile disponibile.
