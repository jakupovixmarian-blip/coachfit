# AGENTS.md — FitCoach

Guida per gli agenti che lavorano su questo repository.

## Progetto

App Android nativa (`com.fitcoach.app`, UI/testi in italiano) che si comporta come un
coach personale: piano alimentare con carb cycling, programmazione di allenamento
periodizzata con progressive overload, motore adattivo e coach AI conversazionale.

- **Stack**: Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Room,
  Coroutines/Flow, WorkManager, AlarmManager, DataStore, Retrofit/OkHttp, Hilt,
  grafici con Compose Canvas. `minSdk 26`, `targetSdk 35`.
- **Architettura**: offline-first. Solo la chat AI richiede rete.
- **Lingua**: tutto il codice, i commenti e i testi UI sono in italiano. Evitare accenti
  nei commenti dove il file usa gia lo stile senza accenti.

## Struttura

```
app/src/main/java/com/fitcoach/app/
├── data/            # Room (entity, dao, db, Mappers, Migrations), repository, remote (AI), datastore
├── domain/          # modelli, motori (nutrition, workout, adaptive), coach, usecase
├── di/              # moduli Hilt (DatabaseModule, RepositoryModule, NetworkModule)
├── notifications/   # scheduler, receiver, boot receiver
├── ui/              # theme, components, navigation, screens + viewmodels
└── FitCoachApplication.kt
```

## Comandi

Lo script `/workspace/build.sh` avvolge Gradle e imposta l'ambiente:

```bash
/workspace/build.sh :app:compileDebugKotlin   # compilazione veloce
/workspace/build.sh :app:testDebugUnitTest    # test unitari (185 test)
/workspace/build.sh :app:assembleDebug        # APK debug
/workspace/build.sh :app:assembleRelease      # APK release (firmabile)
```

L'ambiente si riavvia frequentemente (OOM): **committare spesso** su git.

## Convenzioni

- **Motori deterministici**: tutti i calcoli (calorie, macro, sostituzioni, spesa, diario)
  stanno in `domain/` come `object` puri e testabili. L'AI non calcola e non scrive sul DB:
  interpreta e propone, e le modifiche sono applicate solo dopo conferma
  (`CoachActionExecutor`).
- **Nessun dato inventato**: i valori nutrizionali vengono da `FoodSeedData` (CREA/USDA).
- **Integrazione**: le funzionalità nutrizionali (piano, sostituzioni, spesa, diario)
  condividono lo stesso database alimentare e gli stessi identificativi del piano. Non
  creare moduli indipendenti.
- **Room**: DB version attuale **5**. Aggiungendo entita/DAO ricordarsi di importarli in
  `FitCoachDatabase.kt` (un import mancante causa `[ksp] MissingType`), registrare la
  migrazione in `Migrations.kt` e i mapper in `Mappers.kt`. Lo schema esportato sta in
  `app/schemas/`.
- **UI**: usare i componenti condivisi in `ui/components` (`SectionCard`, `SectionHeader`,
  `StatTile`, `MacroBar`, `Pill`, `EmptyState`, `LoadingBox`) e i colori del tema
  (`Emerald`, `CarbsColor`, `TextSecondary`, ...). Le schermate sono `LazyColumn`.
- **Gate di generazione**: nessun piano si genera senza anamnesi confermata
  (`UserProfile.isAnamnesisReady`). `GenerateProgramsUseCase` ritorna
  `Result.AnamnesisNotConfirmed`. La dashboard reindirizza all'anamnesi.
- **Fonti esterne**: la ricerca alimentare (`FoodSearchRepository`) usa Open Food Facts e
  USDA FoodData Central con URL completi via Retrofit `@Url`. I test di mappatura DTO stanno
  in `data/repository/FoodSearchMappingTest.kt` (nessuna rete: parsing reale delle DTO).

## Punti chiave del motore nutrizionale

- `FoodSubstitutionEngine` — sostituzione vincolata: quantita calcolata sul macro dominante,
  verifica calorie/macro di pasto e giornata entro tolleranze configurabili (kcal 5%,
  P/C/G 10%), blocco se si supera il limite calorico approvato, sostituzione combinata.
- `ShoppingListEngine` — aggregazione dal piano effettivo, categorie, conservazione stato.
- `NutritionDiaryEngine` — obiettivo/consumato/rimanente, niente doppio conteggio, extra.
- `BudgetEngine` — costo reale del piano (grammature × prezzo/kg), ambito personale/condiviso/
  spesa totale, stima parziale dichiarata, convenienza per grammo di proteine.
- `AnamnesisEngine` — intervista guidata, validazione risposte, contraddizioni, riepilogo,
  gate di generazione.
- Repository: `NutritionDiaryRepository`, `ShoppingRepository`, `SubstitutionRepository`,
  `FoodSearchRepository`.
