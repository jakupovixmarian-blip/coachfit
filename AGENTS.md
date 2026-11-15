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

## Ricostruzione FitCoach 2.0

- La ricostruzione procede sul branch `fitcoach-2.0-rebuild`; il baseline v1 è conservato in `archive/fitcoach-1.0-a2bea9a`.
- Backup verificato: `/workspace/backups/fitcoach-1.0-20261112/` contiene bundle Git, archivio sorgente, artefatti, dati Room copiati e `SHA256SUMS`.
- Roadmap e confini architetturali: `docs/FITCOACH_2_ROADMAP.md`.
- Il nuovo layer provider è isolato in `app/src/main/java/com/fitcoach/app/v2/ai/`: capability, transport OkHttp, classificazione errori, discovery modelli e job di generazione non dipendono dai repository v1.
- Le chiavi sono referenziate per alias e lette da `SecureKeyStore`; non inserire mai chiavi in Room, log, test o codice.
- Un test provider locale non equivale a un test provider reale: riportare sempre separatamente `PROVIDER_REAL` e `UI_E2E`.

## Comandi

Lo script `/workspace/build.sh` avvolge Gradle e imposta l'ambiente:

```bash
/workspace/build.sh :app:compileDebugKotlin   # compilazione veloce
/workspace/build.sh :app:testDebugUnitTest    # test unitari (235 test all'ultimo run)
/workspace/build.sh :app:assembleDebug        # APK debug
/workspace/build.sh :app:assembleRelease      # APK release (firmabile)
```

L'ambiente si riavvia frequentemente (OOM): **committare spesso** su git.

## Convenzioni

- **Motori deterministici**: tutti i calcoli (calorie, macro, sostituzioni, spesa, diario)
  stanno in `domain/` come `object` puri e testabili. L'AI non calcola e non scrive sul DB:
  interpreta e propone, e le modifiche sono applicate solo dopo conferma
  (`CoachActionExecutor`).
- `DiaryViewModel` pubblica subito la lettura Room e osserva `observeForDay`: il diario deve aggiornarsi anche se l'utente spunta un pasto da Alimentazione prima di aprire Diario. `NutritionDiaryRepository.togglePlanEntry` mantiene una sola voce per coppia piano/data e usa `active` per il consumo effettivo.
- Il calendario allenamenti usa le sette `WorkoutDayEntity` (incluse quelle di riposo): `WorkoutRepository.moveWorkoutDay` scambia gli indici senza creare una nuova scheda.
- `AiProgramGenerationService` avvia la generazione online dopo la conferma dell'anamnesi: costruisce il contesto completo, richiede il contratto JSON, tenta una correzione per JSON invalido e salva solo dopo la validazione di macro, calorie, alimenti, esercizi, attrezzatura e calendario. Se il provider è assente o la proposta non è valida, non usa fallback silenziosi. La diagnosi dei vincoli falliti è centralizzata in `AiProposalDiagnostics` (valore atteso vs ricevuto) e viene passata al modello per una correzione mirata.
- **AI come autorità decisionale (tool calling)**: `CoachOrchestrator` esegue un ciclo agentico in cui il modello chiama strumenti (`Set<CoachTool>` iniettato via Hilt da `CoachToolsModule`). Gli strumenti di **lettura** restituiscono solo dati reali dai repository; gli strumenti di **calcolo** (energia, carb cycling, volume, 1RM, `compute_portion`, `compute_substitution`, `validate_nutrition_day`) sono deterministici; `validate_workout_plan` usa il `WorkoutPlanValidator` puro; gli strumenti `propose_*` registrano solo **proposte** che l'utente conferma tramite `CoachActionExecutor`. L'AI non calcola da sola i numeri critici, non inventa dati e non scrive mai sul DB.
- **Motore offline come scelta esplicita**: `AnamnesisViewModel` prova prima `AiProgramGenerationService`. Se l'AI fallisce (provider assente/offline/proposta non valida) **non** sostituisce l'AI con un motore deterministico: mostra l'errore e offre `useOfflineEngine()` (`GenerateProgramsUseCase`) come **scelta esplicita** dell'utente. Il motore offline non è mai un'autorità decisionale automatica.
- **Sessione di allenamento**: `WorkoutRepository.saveSet` rifiuta serie non valide o su sessioni non `IN_PROGRESS`; `completeSession` ritorna `Boolean` e richiede almeno una serie valida; `repairInvalidCompletedSessions` riapre (non cancella) le sessioni `COMPLETED` senza serie valide.
- **Lista della spesa**: `ShoppingRepository` serializza le rigenerazioni con un `Mutex` e `regenerateAndRead` accoppia rigenerazione e lettura nella stessa sezione critica, per evitare liste vuote da `delete`/`insert` concorrenti.
- **Nessun dato inventato**: i valori nutrizionali vengono da `FoodSeedData` (CREA/USDA).
- **Integrazione**: le funzionalità nutrizionali (piano, sostituzioni, spesa, diario)
  condividono lo stesso database alimentare e gli stessi identificativi del piano. Non
  creare moduli indipendenti.
- **Room**: DB version attuale **11**. Aggiungendo entita/DAO ricordarsi di importarli in
  `FitCoachDatabase.kt` (un import mancante causa `[ksp] MissingType`), registrare la
  migrazione in `Migrations.kt` e i mapper in `Mappers.kt`. Lo schema esportato sta in
  `app/schemas/`. La v10 aggiunge lo storico degli esiti delle modifiche del coach; la v11 aggiunge la persistenza Room isolata per anamnesi e checkpoint di generazione 2.0.
- **UI**: usare i componenti condivisi in `ui/components` (`SectionCard`, `SectionHeader`,
  `StatTile`, `MacroBar`, `Pill`, `EmptyState`, `LoadingBox`) e i colori del tema
  (`Emerald`, `CarbsColor`, `TextSecondary`, ...). Le schermate sono `LazyColumn`.
- **Gate di generazione**: nessun piano si genera senza anamnesi confermata
  (`UserProfile.isAnamnesisReady`). `GenerateProgramsUseCase` ritorna
  `Result.AnamnesisNotConfirmed`. La dashboard reindirizza all'anamnesi.
- **Coach longitudinale**: `LongitudinalCoachEngine` richiede almeno 2 settimane di aderenza nutrizionale e 3 settimane di medie peso affidabili. `CoachActionExecutor` applica in transazione gli aggiustamenti calorici alle grammature del piano e salva un payload di rollback; gli esiti vengono valutati nei check-in successivi.
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
