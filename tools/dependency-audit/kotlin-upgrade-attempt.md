# Aggiornamento Kotlin 2.0.21 → 2.4.20 (fix CVE-2026-53914) — RIUSCITO

Eseguito il 2026-11-13 con Gradle + Android SDK 35 (JDK 21), per chiudere il finding
`kotlin-gradle-plugin` 2.0.21 → GHSA-r937-wjx7-w2jp.

## Esito: chiuso

La CVE è chiusa. `buildscript-gradle.lockfile` ora risolve `kotlin-gradle-plugin:2.4.20`,
che OSV non segnala più. Il muro Hilt è stato aggirato con l'idea di AppJet: forzare
`languageVersion`/`apiVersion` a 2.2 su Kotlin 2.4.20, così il compilatore emette metadata
2.2 — leggibile da Hilt 2.56.2 senza aspettare Dagger.

Verifica diretta sul bytecode (`javap -v` su `FitCoachApp.class`): `mv=[2,2,0]`, cioè
metadata 2.2.0. Non è una supposizione: è il metadata effettivamente prodotto.

- `assembleDebug` + `testDebugUnitTest` → BUILD SUCCESSFUL, 61 test passati, 0 falliti
- app/gradle.lockfile: 162 pacchetti, OSV "No issues found"
- buildscript-gradle.lockfile: 156 pacchetti, 49 vulnerabilità (erano 61), **nessuna** Kotlin
- `scope_check.py`: 14/14 pacchetti segnalati restano fuori dal grafo dell'app

Toolchain finale: Kotlin 2.4.20, KSP 2.3.13, AGP 8.12.3, Gradle 8.13, Hilt 2.56.2, Room 2.8.5.

La sezione sotto resta come cronaca del percorso: i sei passi sono i vincoli reali che
l'aggiornamento impone, e sono la ragione per cui il passo 7 (`languageVersion 2.2`) è la
chiave che ha sbloccato tutto.

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

## Il passo che ha sbloccato tutto (idea di AppJet)

I sei passi sotto portano a un muro: Hilt 2.56.2 non legge il metadata di Kotlin 2.4. AppJet ha
proposto di non aggiornare Hilt, ma di far emettere a Kotlin un metadata più vecchio:

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}
```

Kotlin supporta due versioni di linguaggio indietro, quindi 2.2 su 2.4.20 è legale. Con
`languageVersion 2.2` il compilatore 2.4.20 emette metadata 2.2.0, che Hilt 2.56.2 legge.

**Verificato, non ipotizzato:** `javap -v` su `FitCoachApp.class` mostra `mv=[2,2,0]`. Il
metadata prodotto è davvero 2.2.0. Build verde, 61 test passati, CVE chiusa.

Questo è il vantaggio di avere due ambienti: AppJet ha avuto l'intuizione (non può verificarla,
non ha Gradle), io ho avuto Gradle per provarla.

## Nota per il futuro

Se un domani Hilt pubblica una release con `kotlin-metadata-jvm` ≥ 2.4, si può togliere il pin
`languageVersion`/`apiVersion` 2.2 e tornare al linguaggio pieno. Finché non accade, il pin resta:
è ciò che permette a Kotlin 2.4.20 (senza CVE) di convivere con Hilt 2.56.2.

Controllo rapido per la nuova release:

```bash
curl -s https://repo1.maven.org/maven2/com/google/dagger/hilt-android-gradle-plugin/maven-metadata.xml \
  | grep -oE "<version>[^<]+</version>" | tail -5
```
