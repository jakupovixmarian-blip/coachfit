# Tentativo di aggiornamento Kotlin 2.0.21 → 2.4.20 (fix CVE-2026-53914)

Eseguito il 2026-11-13 con Gradle + Android SDK 35 (JDK 21), per chiudere il finding
`kotlin-gradle-plugin` 2.0.21 → GHSA-r937-wjx7-w2jp.

## Perché il tentativo era giustificato

Il 2026-11-13 l'API OSV conferma: `kotlin-gradle-plugin` 2.4.20 → **0 vulnerabilità**;
2.0.21 → 1. La 2.4.20 è una release **stabile** e contiene il fix. Il blocco quindi non era
più "aspettiamo JetBrains".

## Cosa è successo, passo per passo

L'aggiornamento non è una patch isolata: ogni passo ne trascina un altro. Ogni riga è un errore
reale raccolto da Gradle, non una previsione.

| Passo | Modifica | Esito |
|---|---|---|
| 1 | Kotlin 2.0.21 → 2.4.20, KSP → 2.3.13 | KSP 2.3.13 richiede AGP ≥ 8.12.0 |
| 2 | AGP 8.7.3 → 8.11.2 | AGP 8.11.2 richiede Gradle ≥ 8.13 |
| 3 | Gradle 8.11.1 → 8.13 | `jvmTarget = "17"` non è più accettato, serve `compilerOptions` |
| 4 | Migrato a `compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }` | Hilt e KSP su class loader diversi (dagger#3965) |
| 5 | Hilt 2.52 → 2.56.2 | Room 2.6.1 incompatibile con KSP nuovo (`unexpected jvm signature V`) |
| 6 | Room 2.6.1 → 2.8.5 | **Hilt 2.56.2 non legge il metadata di Kotlin 2.4** |

## Il muro reale

```
error: [Hilt] Provided Metadata instance has version 2.4.0, while maximum supported
version is 2.2.0. To support newer versions, update the kotlin-metadata-jvm library.
```

Hilt 2.56.2 — l'ultima release stabile — incorpora `kotlin-metadata-jvm` 2.2.0. Non legge il
metadata prodotto da Kotlin 2.4.20. Quindi il blocco non è JetBrains: è **Dagger/Hilt**, che deve
pubblicare una versione con `kotlin-metadata-jvm` aggiornato.

Con questo vincolo la catena completa da aggiornare è:

```
Kotlin 2.4.20 → KSP 2.3.13 → AGP 8.12.3 → Gradle 8.13+ → Hilt (bloccato) → Room 2.8.5
```

Sei dipendenze, e l'ultima non ha ancora una release che sblocchi. Aggiornare tutto e fermarsi su
Hilt non produce un APK funzionante: produce una toolchain rotta.

## Decisione

Ripristinata la toolchain di lavoro (Kotlin 2.0.21, KSP 2.0.21-1.0.28, AGP 8.7.3, Gradle 8.11.1,
Hilt 2.52, Room 2.6.1). Verificato: `assembleDebug` ok, 61 test passati, APK 20.222.928 byte.

Rischio accettato e documentato: la vulnerabilità è nella build cache di Kotlin, cache solo
locale, nessuna cache remota, plugin non impacchettato nell'APK. L'attacco richiede accesso in
scrittura alla macchina che compila.

## Quando riprovare

Serve una release di Hilt con `kotlin-metadata-jvm` ≥ 2.4. Controllo rapido:

```bash
curl -s https://repo1.maven.org/maven2/com/google/dagger/hilt-android-gradle-plugin/maven-metadata.xml \
  | grep -oE "<version>[^<]+</version>" | tail -5
```

Quando compare, la sequenza è: aggiornare in `gradle/libs.versions.toml` kotlin, ksp, agp, hilt,
room e il wrapper Gradle; migrare `kotlinOptions` a `compilerOptions`; poi `assembleDebug` e i 61
test. Il punto 4 della tabella è una modifica di codice già nota e pronta.
