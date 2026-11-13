# Stato di verifica — FitCoach 2.0

Questo file dice cosa è stato realmente verificato e cosa resta da verificare.
Serve a evitare due errori opposti: dichiarare completo ciò che non è stato provato,
e rifare controlli già fatti.

## Verificato in questo ambiente

I motori di dominio sono compilati ed eseguiti con Kotlin 2.0.21 e JUnit 4.13.2,
usando gli stub Room presenti in `stubs/` per le annotazioni.

| Modulo | Test | Esito |
|---|---|---|
| EnergyEngine (BMR, TDEE, macro) | EnergyEngineTest | verde |
| NutritionMath (totali, macro, conversione) | NutritionMathTest | verde |
| FoodValidator (provenienza, affidabilità, coerenza kcal) | FoodValidatorTest | verde |
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

Totale: **125 test, 0 falliti**.

## Non verificato qui, e perché

AppJet non compila app Android native: mancano Gradle e l'Android SDK in questo
ambiente. Quindi i punti seguenti vanno verificati sul tuo ambiente:

1. **Compilazione completa con Gradle** (`./gradlew assembleDebug`), comprese le
   schermate Compose modificate: `WorkoutSessionScreen` (dialog di conferma della
   progressione) e `PlanScreen` (pulsante "Calcola 7 giorni sui dati in archivio").
2. **Migrazione Room 4 → 5**: aggiunge `routine_exercise.targetWeightKg`. Le righe
   esistenti restano con valore nullo, che è lo stato corretto: nessun carico è stato
   ancora approvato. Va provata su un database già popolato.
3. **Iniezione Hilt** di `ProgressionPort` → `ProgressionPortImpl` (binding aggiunto
   in `RepositoryModule`). Un errore qui si vede solo alla compilazione.
4. **Timer in background** con l'app chiusa: la logica di scadenza è testata, il
   servizio e le notifiche richiedono un dispositivo.
5. **Connettori esterni reali** (USDA, Open Food Facts): le chiamate di rete e le
   chiavi non sono provate qui; la logica di cache e deduplica è testata.
6. **APK e resa grafica**: nessun emulatore disponibile, quindi la grafica non è
   stata guardata da qui.

## Come procedere

```bash
./gradlew assembleDebug testDebugUnitTest
```

Se la migrazione fallisce su un database esistente, il messaggio d'errore di Room
indica la colonna mancante: `routine_exercise.targetWeightKg`.
