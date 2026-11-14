# Stato di verifica — FitCoach 2.0

Questo file dice cosa è stato realmente verificato e cosa resta da verificare.
Serve a evitare due errori opposti: dichiarare completo ciò che non è stato provato,
e rifare controlli già fatti.

## Verificato in ambiente Gradle (Android SDK 35, JDK 21, Kotlin 2.4.20)

Compilazione reale dell'app completa — non solo i motori di dominio — con Gradle,
Kotlin, KSP, Hilt e Room:

| Controllo | Comando | Esito |
|---|---|---|
| Build APK | `./gradlew assembleDebug` | **BUILD SUCCESSFUL**, APK 20.649.630 byte |
| Test unitari | `./gradlew :app:testDebugUnitTest` | **188 test, 0 falliti** |
| Iniezione Hilt (binding `ProgressionPort`) | compilazione | **risolta** (altrimenti fallirebbe il build) |
| Schermate Compose (`WorkoutSessionScreen`, `PlanScreen`) | compilazione | **compilano** |
| Migrazione Room 4 → 5 su DB reale | `MigrationToV5Test` (nuovo, Robolectric) | **verde** |

I 188 test sono i 186 della suite di AppJet piu' i 2 nuovi in `MigrationToV5Test`, che
eseguono la catena 3 → 4 → 5 su un database Robolectric reale e provano che
`routine_exercise.targetWeightKg` venga aggiunta senza perdere le righe esistenti (che
restano a NULL, stato corretto) e che il peso sia poi scrivibile e rileggibile. La
migrazione 4 → 5 era il punto 2 dei "non verificato": ora e' verificata.

Nota: lo schema v4 non e' mai stato esportato dal progetto, quindi il test parte dallo
schema v3 (esportato) e applica la catena intera: e' l'unico modo di eseguire davvero
l'anello 4 → 5 senza un `4.json`.

## Verificato senza Gradle (motori di dominio)

Anche in un ambiente senza Android SDK i motori di dominio erano stati compilati ed
eseguiti con Kotlin e JUnit, usando stub Room:

| Modulo | Test | Esito |
|---|---|---|
| EnergyEngine (BMR, TDEE, macro) | EnergyEngineTest | verde |
| NutritionMath (totali, macro, conversione) | NutritionMathTest | verde |
| FoodValidator (provenienza, affidabilita', coerenza kcal) | FoodValidatorTest | verde |
| VolumeAnalyzer (volume diretto/indiretto, frequenza) | VolumeAnalyzerTest | verde |
| FoodMatcher (abbinamento nomi, sinonimi, marca) | FoodMatcherTest | verde |
| FoodCatalog (cache, deduplica, import) | FoodCatalogTest | verde |
| CreaImport (dataset CREA) | CreaImportTest | verde |
| ToolEngine (tool calling, approvazione, registro) | ToolEngineTest | verde |
| AiOrchestrator (provider, errori, risposte malformate) | AiOrchestratorTest | verde |
| PlanGenerator + PlanValidator | PlanGeneratorTest | verde |
| RestTimer (scadenza, pausa, +15/+30, skip, snapshot) | RestTimerTest | verde |
| ResumeSession (ripresa dopo chiusura, soglia 12h) | ResumeSessionTest | verde |
| DietGenerator (7 giorni, grammi reali, correzione calorie) | DietGeneratorTest | verde |
| ProgressionApproval (conferma, rifiuto, registro) | ProgressionApprovalTest | verde |

## Non verificato, e perche'

1. **Timer in background** con l'app chiusa: la logica di scadenza e' testata, il
   servizio in primo piano e le notifiche richiedono un dispositivo reale.
2. **Connettori esterni reali** (USDA, CREA, Open Food Facts): la logica di cache e
   deduplica e' testata; le chiamate di rete vere e le chiavi API no.
3. **Resa grafica**: nessun emulatore ne' dispositivo, quindi l'aspetto dell'app non e'
   stato guardato. Serve il riscontro su un telefono.
4. **Migrazione su un database realmente popolato dall'utente**: il test la esegue su
   un DB Robolectric. Un DB reale con dati dell'utente resta il caso finale, ma la
   logica di migrazione e' ora provata.

## Come procedere

```bash
./gradlew assembleDebug testDebugUnitTest
```

Se la migrazione fallisce su un database esistente, il messaggio d'errore di Room
indica la colonna mancante: `routine_exercise.targetWeightKg`.

## Aggiornamento dipendenze buildscript — 2026-11-14

`buildscript-gradle.lockfile` è stato portato a mano alle versioni correttive indicate dagli advisory OSV:

| Pacchetto | Da | A |
|---|---|---|
| `io.netty:*` (11 moduli) | 4.1.110.Final | 4.1.137.Final |
| `org.bouncycastle:*-jdk18on` | 1.79 | 1.85 |
| `com.google.guava:guava` | 33.3.1-jre | 33.7.2-jre |
| `com.google.guava:failureaccess` | 1.0.2 | 1.0.3 |
| `org.apache.commons:commons-compress` | 1.21 | 1.26.0 |
| `org.bitbucket.b_c:jose4j` | 0.9.5 | 0.9.6 |
| `org.jdom:jdom2` | 2.0.6 | 2.0.6.1 |

Ogni versione è stata verificata con una risoluzione Maven reale (artefatti e transitive esistono, `BUILD SUCCESS`). Lo scan OSV sul repository ora riporta **0 segnalazioni** su 324 pacchetti.

**Da verificare nel tuo ambiente:** la compatibilità di compilazione non è controllabile qui (mancano Gradle e Android SDK). Eseguire `./gradlew assembleDebug`; se Gradle rifiuta il lock state, `./gradlew build --write-locks`. Il lockfile dell'app (`app/gradle.lockfile`) non è stato toccato.
