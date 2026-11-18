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
/workspace/build.sh :app:testDebugUnitTest    # test unitari (347 test all'ultimo run)
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
- **Generazione unificata nell'AI (unico punto di creazione)**: la generazione iniziale e le rigenerazioni successive passano dallo stesso motore agentico della chat. `AiProgramGenerationService.generate(profile, scope)` apre un turno su `CoachOrchestrator` e l'AI consegna il programma con lo strumento `submit_full_program` (in `domain/coach/tools/`): è l'UNICO punto in cui nasce un percorso. `ProgramGenerationEngine.validate(...)` verifica la proposta contro i calcoli deterministici (energia, carb cycling, volume, calendario, alimenti/esercizi reali) e salva solo se valida; se non lo è restituisce la diagnosi (`ProgramGenerationEngine.diagnose`) così il modello si corregge. L'AI non calcola da sola i numeri critici e non scrive mai sul DB.
- **Consegna OBBLIGATORIA del programma**: `CoachOrchestrator.run(..., requireTool = "submit_full_program")` forza `tool_choice` sulla funzione finché non viene chiamata. Se il modello risponde in prosa (tipico dei modelli di ragionamento come gpt-5.x) viene sollecitato a consegnare; se non consegna entro le iterazioni, l'errore reale viene esposto (`providerError`) invece di una sintesi. Il budget token della generazione è `GENERATION_MAX_TOKENS = 32_000` perché sui modelli di ragionamento il budget include i token di ragionamento e un tetto basso tronca la chiamata (`finish_reason = length`, esposto da `AiResult.Success.truncated`). NON reintrodurre `tool_choice = auto` nella generazione.
- **Firma e versioni**: il debug usa il keystore FISSO e versionato `fitcoach-debug.keystore` (config `debugStable` in `app/build.gradle.kts`). Senza di esso ogni macchina genera un `debug.keystore` diverso e Android rifiuta l'aggiornamento (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), lasciando attiva la build vecchia. `versionCode`/`versionName` vanno incrementati ad ogni consegna.
- **Ambiti di generazione**: `ProgramGenerationEngine.Scope` = `BOTH` / `NUTRITION` / `WORKOUT`. Con `NUTRITION`/`WORKOUT` si verifica e si persiste solo la parte richiesta (l'altra resta invariata). Le schermate Alimentazione e Allenamento usano lo scope specifico per i loro pulsanti di rigenerazione.
- **Facade unica per la UI**: `ProgramGenerationCoordinator` (`domain/usecase/`) è l'unico ingresso usato dai ViewModel e instrada sempre all'AI (`AiProgramGenerationService`). **Non esiste più un motore deterministico di fallback**: se il provider non è configurato o non risponde, la generazione fallisce con errore esplicito. Applica il gate anamnesi e i controlli di dati essenziali prima di chiamare l'AI.
- **Chat → rigenerazione per ambito (un solo motore)**: dalla chat l'AI usa lo strumento `propose_regenerate_plan(scope)` (`domain/coach/tools/WriteTools.kt`) quando la richiesta cambia la STRUTTURA del programma (giorni, split, carb cycling, strategia); per le modifiche puntuali preferisce i `propose_*` specifici. Come ogni strumento di scrittura registra una proposta `PROPOSED` (`CoachAction.ACTION_REGENERATE_PLAN`, `domain` = ambito). Alla conferma, `CoachActionExecutor.apply` riconosce l'azione e delega a `ProgramGenerationCoordinator` con lo scope richiesto — **lo stesso motore unico** della generazione iniziale (nessun secondo motore). Il rollback (`revert`) riattiva i piani precedenti tramite lo snapshot degli id (`PLAN_REGENERATED`); se il provider è irraggiungibile o la proposta non è valida, nessun piano viene alterato. Il coordinatore è iniettato come `dagger.Lazy` per evitare cicli (Coordinator → AiProgramGenerationService → CoachOrchestrator → tool → Executor → Coordinator).
- **Annullamento in chat**: `CoachViewModel` espone `appliedChanges` (ultime modifiche APPLIED) e la schermata Coach permette di annullare una modifica (`revert`), ripristinando il programma precedente.
- **Nessun motore offline**: l'AI è l'unica autorità decisionale. Se il provider non è configurato o non risponde, l'app mostra l'errore e invita a configurarlo: non genera un piano alternativo con codice deterministico. Le componenti deterministiche rimaste sono solo calcoli, cataloghi e validatori (giudicano la proposta dell'AI, non la sostituiscono).
- **Sessione di allenamento**: `WorkoutRepository.saveSet` rifiuta serie non valide o su sessioni non `IN_PROGRESS`; `completeSession` ritorna `Boolean` e richiede almeno una serie valida; `repairInvalidCompletedSessions` riapre (non cancella) le sessioni `COMPLETED` senza serie valide.
- **Lista della spesa**: `ShoppingRepository` serializza le rigenerazioni con un `Mutex` e `regenerateAndRead` accoppia rigenerazione e lettura nella stessa sezione critica, per evitare liste vuote da `delete`/`insert` concorrenti.
- **Nessun dato inventato**: i valori nutrizionali vengono da `FoodSeedData` (CREA/USDA).
- **Integrazione**: le funzionalità nutrizionali (piano, sostituzioni, spesa, diario)
  condividono lo stesso database alimentare e gli stessi identificativi del piano. Non
  creare moduli indipendenti.
- **Room**: DB version attuale **13**. La v13 aggiunge l'identita esplicita del piano nelle voci diario e i vincoli SQLite per un solo piano attivo per profilo. Aggiungendo entita/DAO ricordarsi di importarli in
  `FitCoachDatabase.kt` (un import mancante causa `[ksp] MissingType`), registrare la
  migrazione in `Migrations.kt` **e in `di/DatabaseModule.provideDatabase`** (una migrazione
  definita ma non registrata fa scattare il fallback distruttivo e azzera i dati) e i mapper
  in `Mappers.kt`. Lo schema esportato sta in `app/schemas/`. La v10 aggiunge lo storico
  degli esiti delle modifiche del coach; la v11 la persistenza Room isolata per anamnesi e
  checkpoint di generazione 2.0; la v12 lo stato "acquistato" della lista della spesa; la v13
  il legame esplicito delle voci diario al piano e i trigger di integrita per il piano attivo.
  **Nessun `fallbackToDestructiveMigration`**: una migrazione mancante deve fallire in modo
  esplicito, mai cancellare profilo, piani e storico dell'utente.
- **Test delle migrazioni**: `app/src/test/.../data/local/MigrationTest.kt` valida gli schemi
  con `MigrationTestHelper` (Robolectric) contro gli schema esportati e verifica la
  conservazione dei dati e i vincoli v13 per un solo piano attivo. Gli schemi sono resi asset
  della variante debug in `app/build.gradle.kts` (`sourceSets["debug"].assets.srcDir(schemas)`) perche i test
  Robolectric leggono gli asset della variante debug. Il test ha scoperto un bug reale:
  `MIGRATION_1_2` ri-aggiungeva `activityFactorOverride`/`calorieTargetOverride` (gia in v1)
  -> "duplicate column name" in upgrade da v1. Aggiungendo migrazioni, estendere questo test.
- **Gestione piani salvati**: `NutritionRepository` e `WorkoutRepository` normalizzano lo stato per
  garantire esattamente un piano attivo (riparano anche i database precedenti con piu flag
  `isActive`). Entrambi espongono rename/delete; la selezione disattiva tutti gli altri.
  La schermata Allenamento usa `custom_workout/{planId}` per modificare una scheda esistente
  in-place, senza crearne una copia. `NutritionDiaryRepository` filtra le voci collegate al
  piano attivo: gli spuntini di vecchi piani non gonfiano piu il consumo giornaliero.
  `collaudo/PlanManagementCollaudoTest.kt` copre isolamento, selezione, CRUD e modifica reale.
- **Sprint 1 piano/storico**: DB v13 aggiunge `nutritionPlanId` esplicito alle voci diario e
  backfill dalle relazioni legacy. Trigger SQLite impediscono piu piani attivi per profilo;
  la migrazione ripara prima eventuali duplicati. La modifica workout crea una nuova versione
  del piano e lascia intatta la precedente. L'editor nutrizionale consente ora di modificare
  i grammi di una voce e ricalcola i macro dai valori del catalogo. Collaudo: 337 test verdi.
- **Notifiche**: il posticipo usa `UserPreferences.deferMinutes` (5..120 minuti) e il valore
  viene propagato nell'intent dell'allarme, nell'azione di snooze e nella programmazione
  giornaliera/settimanale. Non reintrodurre un valore fisso nel `ReminderReceiver`. Le notifiche
  di pasto e allenamento portano un deep link (`EXTRA_DEEP_LINK_ROUTE`, `ReminderReceiver.routeFor`)
  e le azioni Posticipa/Conferma; `MainActivity` è `singleTop` e instrada la rotta pendente in
  `FitCoachNavHost`.
- **Statistiche per intervallo**: `StatisticsEngine` (`domain/adaptive/`, PURO) aggrega pesate,
  diario reale, sedute completate e prestazioni su `StatRange` = settimana/mese/trimestre. I campi
  restano `null` quando mancano dati (nessuno zero o stima inventata). `ProgressViewModel` espone
  `statRange` + `statistics`; `ProgressScreen.AdvancedStatisticsSection` mostra le metriche e i
  grafici (calorie/giorno a barre, volume/seduta e peso nel periodo a linea). Aggiungendo
  statistiche, estenderle qui e in `collaudo/StatisticsEngineCollaudoTest.kt`.
- **Recovery del provider AI**: `AiClient.sendOnce` ritenta con backoff solo gli errori
  transitori (rete, 408/429/5xx, max `MAX_TRANSIENT_RETRIES`), mai quelli di configurazione
  (401/404/400). La chat espone `CoachViewModel.retry()` (pulsante "Riprova"): rimuove il
  messaggio di errore e rinvia l'ultimo messaggio senza duplicarlo (`lastUserMessage`/`canRetry`).



- **Navigazione di primo livello (Home che "sparisce")**: tutte le destinazioni top-level
  (Dashboard, Alimentazione, Spesa, Allenamento, Progressi, Coach) si aprono SEMPRE con
  `NavController.navigateTopLevel(route)` (`ui/navigation/Nav.kt`), sia dalla barra inferiore
  sia dalle card della Dashboard. Mescolare navigazioni "bare" (senza opzioni) e navigazioni
  con `popUpTo(start){saveState}` + `restoreState` corrompe il ripristino dei tab: toccando
  Home l'app ripristinava Allenamento/Alimentazione e la Home sembrava irraggiungibile.
  Collaudo: `collaudo/NavOptionsCollaudoTest.kt`.
- **UI**: usare i componenti condivisi in `ui/components` (`SectionCard`, `SectionHeader`,
  `StatTile`, `MacroBar`, `Pill`, `EmptyState`, `LoadingBox`) e i colori del tema
  (`Emerald`, `CarbsColor`, `TextSecondary`, ...). Le schermate sono `LazyColumn`.
- **Gate di generazione**: nessun piano si genera senza anamnesi confermata
  (`UserProfile.isAnamnesisReady`). `AiProgramGenerationService.generate` ritorna
  `Result.AnamnesisNotConfirmed`. La dashboard reindirizza all'anamnesi.
- **Nessun motore longitudinale**: `LongitudinalCoachEngine` e `AdaptiveEngine` sono stati
  **rimossi**. Il check-in misura e l'AI decide. `CoachActionExecutor` applica in transazione
  gli aggiustamenti calorici alle grammature del piano e salva un payload di rollback.
- **Check-in settimanale condotto dall'AI (un solo decisore)**: `WeeklyCheckInService` (`domain/coach/`) aggrega i dati reali con `WeeklyCheckInAnalyzer` (`domain/adaptive/`, PURO, produce solo misure e tendenze), salva il `CheckIn` (fotografia) e apre un turno su `CoachOrchestrator` con `CoachPromptBuilder.weeklyCheckInSystemPrompt` + `buildCheckInContext`. L'AI vede il report tramite lo strumento `analyze_weekly_checkin` (`domain/coach/tools/AnalyzeWeeklyCheckInTool.kt`) e propone con i `propose_*`; le proposte restano `PROPOSED` (nessun auto-apply). `RunWeeklyCheckInUseCase` è una facade sottile per la UI. Se il provider manca, il check-in salva comunque i dati e ritorna `providerError`. La configurazione provider è centralizzata in `AiConfigResolver` (con `runCatching` sul keystore: un guasto non deve far crashare la chat).
- **UI check-in**: sezione dedicata in `ProgressScreen` (`WeeklyCheckInSection`): metriche del report, valutazione dell'AI, proposte con Approva/Rifiuta. `ProgressViewModel.approveCheckInProposal`/`rejectCheckInProposal` usano `CoachActionExecutor` (con rollback via `revert`).
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

## Database alimentare globale (offline)

- **Database interno confezionato nell'APK**: asset `app/src/main/assets/food_usda_sr_legacy.tsv`
  (~720 KB, TSV `name, category, kcal, protein, carbs, fat, fiber, sourceId`) con **7.793
  alimenti USDA FoodData Central – SR Legacy** (pubblico dominio). Rigenerabile con
  `python3 tools/build_food_asset.py` (scarica il dump USDA, ~6 MB, e riscrive l'asset).
- **Import**: `SeedInitializer.seedUsdaAssetIfNeeded()` legge l'asset via
  `FoodAssetImporter.parseTsv` (parser puro, testabile) e inserisce in Room al primo avvio.
  Idempotente (guardia `FoodDao.countBySource(USDA_SOURCE)`). L'indice su `name` e UNICO:
  l'import **salta i nomi gia presenti** per non sovrascrivere alimenti curati (es. "Tempeh")
  gia referenziati dai pasti. L'asset non ha duplicati interni (verificato).
- **Ricerca**: `FoodSearchRepository` risolve locale (Room, ~7.800 voci) → Open Food Facts →
  USDA API (fallback). `FoodDao.searchNormalized` ha `LIMIT 120`. Nel tool AI `list_foods`
  (`ReadCatalogTools.kt`) senza query si **privilegia il catalogo curato italiano** (voci con
  `substituteNames`, fonte CREA), cosi i piani usano alimenti adatti all'utente italiano.
- **Test**: `FoodAssetImporterTest` (valida l'asset reale: numerosita, dedup, `sourceId`
  univoci, coerenza kcal/macro — l'alcol e escluso perche l'etanolo apporta 7 kcal/g non
  modellato da Atwater; soglia >=95% entro il 25%) e `FoodDatabaseSeedIntegrationTest`
  (import su Room reale, idempotenza, collisione seed/USDA, ricerca).
- **Fonti esterne valutate** (dump vs API): USDA SR Legacy (integrata), Open Food Facts
  (API, dump globale 1,5 GB troppo grande per l'APK), CIQUAL/Swiss/CoFID/Fineli/Frida/NEVO/
  Matvaretabellen (dump disponibili), BLS (a pagamento). Tabella completa nel README.

## Collaudo dei difetti segnalati (bugfix sessione)

Test di collaudo reali (Robolectric + Room in-memory, nessun mock) in
`app/src/test/java/com/fitcoach/app/collaudo/`:

- `FoodCatalogCollaudoTest` — BUG 1/4: il contesto di generazione conteneva i primi 90
  alimenti in ordine alfabetico (fast food USA in inglese). Ora `FoodCatalogSelector`
  antepone il catalogo CREA ed esclude ristorazione/fast food/marche. Le liste UI
  (`FoodDao.observeAll/getAll`) ordinano CREA-first.
- `WorkoutSessionLifecycleCollaudoTest` — BUG 2/3: `WorkoutRepository.abandonSession`
  chiude una seduta senza serie valide come `SKIPPED` (non `COMPLETED`, non resta
  `IN_PROGRESS`); `WorkoutViewModel.startManualRest` avvia il recupero su richiesta.
- `NavigationFlowCollaudoTest` — BUG 5/6: su `MainActivity` reale ogni tab apre la sua
  schermata; tornare alla Dashboard dopo il workout non lascia schermate vuote.
- `DashboardRenderCollaudoTest` — sweep di tap sulla Dashboard: nessun tocco generico
  naviga ad Allenamento.
- `SubstitutionCompatibilityCollaudoTest` — BUG: albume→yogurt greco rifiutato.
  `FoodSubstitutionEngine` ora applica una tolleranza allargata quando originale e
  sostituto condividono lo stesso macro dominante (ruolo alimentare equivalente).
- `ManualPlanEditCollaudoTest` — NOVITA: modifica manuale del piano. Verifica che
  `NutritionRepository.addEntryToMeal` calcoli i macro dai grammi reali (110 kcal/100 g × 2
  = 220 kcal) e che `removeEntry` elimini la voce.
- `BleMeasurementParserCollaudoTest` — NOVITA: decodifica dei payload BLE standard
  (Weight Measurement 0x2A9D, Heart Rate Measurement 0x2A37) su byte costruiti secondo la
  specifica Bluetooth SIG, inclusa la conversione lb→kg e i payload troncati.

## Dispositivi Bluetooth (BLE)

`domain/device/BleDeviceManager.kt` scansiona e si collega a bilance (Weight Scale Service
0x181D) e cardiofrequenzimetri (Heart Rate Service 0x180D) reali; `BleMeasurementParser`
decodifica i payload. La UI (`ui/settings/DevicesScreen.kt`, voce "Dispositivi" in
Impostazioni) chiede i permessi runtime corretti per la versione di Android
(`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` su API 31+, `ACCESS_FINE_LOCATION` fino ad API 30) e
registra il peso letto nei progressi solo dopo conferma. Nessun valore inventato: se il
dispositivo non risponde, la UI lo dichiara.

## Modifica manuale del piano alimentare

Da "Alimentazione" l'utente puo aggiungere un alimento a un pasto (icona `+`) cercandolo
nel catalogo reale e indicando i grammi, oppure rimuovere una voce (icona cestino). I macro
sono ricalcolati dai grammi tramite `NutritionRepository.addEntryToMeal`, senza rigenerare
il piano. `removeEntry` elimina la voce.

## Piano MANUALE (senza AI) e scelta del piano attivo

- **Piano alimentare manuale**: `NutritionRepository.createManualPlan(profile, energy,
  cycling, dayTypes, trainingDayIndices)` crea 7 giorni con i tipi HIGH/MEDIUM/LOW del carb
  cycling e i target calcolati dai motori deterministici (`EnergyCalculator`,
  `CarbCyclingEngine`) — **nessun valore inventato** — ma con i **pasti vuoti** da compilare
  a mano (stessi strumenti della modifica manuale). Avviato da "Alimentazione" (pulsante
  "Crea piano manuale"). I target restano modificabili.
- **Scelta del piano da avviare**: `getAllPlans()` + `setActivePlan(id)` su
  `NutritionRepository` e `WorkoutRepository`. La UI mostra la sezione "Piani salvati" /
  "Schede salvate" quando ce n'e piu di uno, con pulsante "Attiva" (l'attivo e marcato).
  Cosi l'utente decide esplicitamente se usare il percorso AI o quello manuale.
- **Diario raggiungibile**: il Diario alimentare (`Routes.DIARY`) si apre dall'icona
  `EventNote` in "Alimentazione" (`NutritionScreen`). Prima era definito nel NavHost ma non
  raggiungibile da nessuna schermata, quindi non si potevano registrare gli alimenti extra
  (fuori piano) ne vedere l'eccesso rispetto al target.
- Collaudo: `integration/ManualPlanIntegrationTest.kt` (piano manuale su Room reale, pasti
  vuoti, compilazione pasto + extra, eccesso vs target, passaggio tra piani).

Nota test: `WorkoutViewModel.runRestTimer`/`syncStopwatch` usano `System.currentTimeMillis`
in un loop con `delay`. Nei test NON usare `advanceUntilIdle` (gira all'infinito): pompare
lo scheduler con `runCurrent` e attendere la condizione reale.
