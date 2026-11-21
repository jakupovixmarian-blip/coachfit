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
- **Calcoli non aggirabili dall'AI**: il modello sceglie gli alimenti, il codice decide i
  grammi. `PortionResolution` (`domain/nutrition/`) risolve le porzioni con
  `MealPortionSolver` subito dopo il decode JSON e PRIMA della validazione, dentro
  `SubmitFullProgramTool`: non e' un passaggio opzionale, quindi un modello che sbaglia i
  grammi (o li calcola a mente) non puo' piu' far respingere il piano. Le note di
  risoluzione tornano all'AI tra gli `avvisi`. Regressione coperta da
  `integration/AiProgramGenerationFlowIntegrationTest` (grammature dimezzate -> salvate
  ricalcolate) e `domain/nutrition/PortionResolutionTest`.
- **Nessuna soglia inventata**: i limiti di programmazione si ricavano dai dati, non da
  costanti scritte a mano. `CatalogBounds` (`domain/workout/`) deriva serie/ripetizioni/
  recupero/RIR dai campi di `Exercise` (con margini proporzionali); `VolumeGuidelines`
  deriva il volume settimanale per gruppo e lo modula su `TrainingExperience`. Il numero
  di pasti si valida contro `profile.mealsPerDay`, non contro un 3..6 fisso. Se serve un
  nuovo limite, aggiungerlo a queste fonti uniche: una costante nel validatore
  contraddice i dati che l'app stessa pubblica (era il caso di Plank, 30-60s contro un
  massimo di 30, che respingeva ogni scheda che lo includesse).
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
- **Identita grafica Hyperion**: la palette/identita dell'app e **Hyperion** (consegna grafica
  AppJet, commit `c3200ed` in `fitcoach2.4`), che sostituisce integralmente "Obsidian Meridian"
  (smeraldo `#00E599`). Fonte unica dei valori: `design/tokens.json` della consegna. Ciano/blu
  = brand e azione, viola/magenta = AI, verde = progresso. I nomi dei `val` in `Color.kt` NON
  cambiano (solo i valori hex), cosi tutta l'app si ritinta senza toccare i chiamanti.
  `HyperionBackground` (`ui/theme/Theme.kt`) disegna lo sfondo ambientale a tre aure radiali
  (vietato il nero piatto) e va lasciato trasparente il `Scaffold` di `FitCoachNavHost` e la
  `Surface` di `MainActivity` perche l'aura si veda. `SectionCard` ha lo sheen interno.
  Non reintrodurre gli hex vietati: `#00E599 #00C584 #5CFFC4 #00D2FF #9D4EDD #FFD54F #080B0A`.
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

## DeepSeek: Thinking mode + Tool Calling (vincoli API)

Il provider DeepSeek ha due regole che, se violate, producono **HTTP 400** e bloccano
la generazione del piano alimentare. Sono verificate in
`app/src/test/java/com/fitcoach/app/domain/coach/DeepSeekThinkingToolCallingTest.kt`
(provider simulato che applica le stesse regole dell'API reale).

1. **Con Thinking attivo `tool_choice` puo essere solo `auto`.**
   Qualunque scelta forzata (`required` o il nome di uno strumento) restituisce
   `"Thinking mode does not support this tool_choice"`.
   In `CoachOrchestrator.run` la consegna obbligatoria (`requireTool`, es.
   `submit_full_program`) viene quindi perseguita con istruzioni + sollecito, e la
   scelta forzata si usa **solo** dopo che il modello ha risposto in prosa senza
   consegnare (`proseMisses >= 1`). In quel caso **la singola richiesta** parte con
   `thinking: {"type":"disabled"}`; il ragionamento non viene mai disattivato globalmente.
   Il campo `thinking` viene inviato solo ai provider che lo supportano
   (`AiParameterPolicy.supportsThinkingMode`, oggi DeepSeek): gli altri lo rifiuterebbero.

2. **Con `tools` presenti il `reasoning_content` va reinviato integralmente.**
   Ogni messaggio assistant che contiene `tool_calls` deve riportare il
   `reasoning_content` ricevuto dal provider, in tutti i turni successivi della stessa
   conversazione; altrimenti l'API risponde 400. Per questo:
   - `ChatCompletionResponse.Message.reasoningContent` viene esposto in
     `AiResult.Success.reasoningContent`;
   - `assistantToolCallMessage(..., reasoningContent = ...)` lo reinserisce nel messaggio
     assistant (`ChatMessageDto.reasoningContent`, serializzato come `reasoning_content`);
   - il reasoning **non** viene mai usato come testo finale quando ci sono tool call
     (`ChatCompletionResponse.rawContent` vs `content`), perche non e la risposta per
     l'utente.

Difesa in profondita: se una richiesta forzata arrivasse comunque al client con Thinking
attivo, `AiClient` riconosce l'errore (`AiParameterPolicy.isThinkingToolChoiceError`) e
ripete la chiamata con `tool_choice = auto` (`ChatCompletionRequest.withAutoToolChoice`).

Regola generale: non esiste un secondo motore decisionale. `CoachOrchestrator` resta
l'unica autorita; nutrizione e allenamento passano dallo stesso percorso agentico
(`AiProgramGenerationService` -> `orchestrator.run(requireTool = "submit_full_program")`).

## Nutrizione: target vincolanti e solver delle grammature

**Sintomo**: `Il programma AI non e valido: giorno N kcal: attese 2037, ricevute 1281`
(proteine/carboidrati/grassi sotto target della stessa proporzione), su tutti i giorni.

**Causa**: il target di ogni giornata e deterministico (`CarbCyclingEngine`) e il
validatore lo impone, ma l'AI **non lo riceveva**: doveva indovinarlo e poi centrare
quattro macro a mano su ~35 pasti. Sbagliava sistematicamente per difetto (~60% del
target). Non era un problema di catalogo: pollo, riso, olio, uova e avena sono nel seed.

**Soluzione** — divisione netta dei compiti:
- L'AI decide **cosa** si mangia (alimenti, pasti, ordine) e **quale** esercizio fare.
- `MealPortionSolver` (`domain/nutrition/`) calcola **quanto**, sui valori reali del
  catalogo: aggiustamento iterativo per ruolo (PROTEIN/CARBS/FAT), grammi interi,
  verifica entro il 3% (molto piu stretta del 20-30% del validatore).
- `solve_meal_portions` (tool) espone il solver: l'AI passa gli alimenti per pasto e
  riceve le voci pronte (`foodName`/`grams`) da usare in `submit_full_program`.
- `get_nutrition_targets` (tool) restituisce i target esatti HIGH/MEDIUM/LOW e quale
  dayType tocca a ogni giorno. Il contesto di generazione stampa comunque la tabella
  dei target, cosi il modello non deve nemmeno chiederla.

**Onesta sui limiti** (importante, non "aggiustare" rimuovendola): se una giornata non
e centrabile il solver restituisce `ok=false` con `IMPOSSIBILE ...` e la spiegazione
(alimento X al minimo di 1 g gia troppo denso, oppure manca una fonte per un macro).
L'AI deve cambiare alimenti e ripetere. Non introdurre tolleranze piu larghe, non
aggiungere alimenti di nascosto e non far passare una giornata fuori target.

`MealPortionSolver.roleOf` classifica per densita **e** quota calorica: l'uovo intero
resta una fonte proteica (piu kcal da grassi ma 12.4 g proteine/100 g); gli alimenti
sotto 50 kcal/100 g (verdura) sono `MIXED` e non trainano nessun macro.

Test: `MealPortionSolverTest` (14), `NutritionToolContractTest` (9, contratto JSON
verso l'AI), `NutritionTargetCoherenceTest` (7, 7 giorni validi su piu profili e
pesi 55-110 kg). `SimulatedAiPrograms` ora sceglie solo gli alimenti e delega i grammi
al solver: se un test di integrazione torna rosso, il problema e nel solver o nel
contratto, non nell'aritmetica simulata.

## Crash all'avvio / in background: cause reali e difese

Segnalazione utente "l'app continua ad andare in crash". Non esisteva alcun log
recuperabile dal dispositivo, quindi il primo passo e rendere il crash
diagnosticabile, non indovinare la causa.

### Diagnostica
- `diagnostics/CrashLog.kt` installa un `Thread.setDefaultUncaughtExceptionHandler`
  che scrive `last_crash.txt` in `filesDir` (data, thread, modello, stack trace) e
  poi delega al handler precedente (il comportamento di sistema resta invariato).
- Impostazioni > Diagnostica mostra se un errore e registrato e permette di
  condividerlo o eliminarlo. Il file e leggibile senza cavo USB.

### Cause individuate e corrette
1. **`SecureKeyStore` poteva chiudere il processo.** Il Keystore Android puo
   invalidare la master key (ripristino da backup, reinstallazione, cambio di
   blocco schermo, firmware Samsung). `MasterKey.Builder`/`EncryptedSharedPreferences`
   sollevavano `KeyStoreException` e `AiProvidersViewModel.init` legge
   `secureKeyStore.hasKey(...)`: aprire le impostazioni AI chiudeva l'app.
   Ora l'archivio viene ricreato una volta (su un file con nome diverso) e ogni
   operazione degrada a "nessuna chiave" invece di lanciare.
2. **`BootReceiver` e `ReminderReceiver` propagavano errori fuori dalla UI.**
   Usavano `try { ... } finally { finish() }` dentro `CoroutineScope(...).launch`:
   senza `catch` l'eccezione raggiunge l'handler di default e chiude il processo.
   `BootReceiver` gira su `MY_PACKAGE_REPLACED`, cioe subito dopo un aggiornamento:
   e il caso che l'utente percepisce come "crash continuo". Ora entrambi registrano
   l'errore e terminano. La logica e in `BootReceiver.reschedule()` (internal
   suspend) per essere testabile in modo deterministico.
3. **Il lavoro di avvio in `FitCoachApplication` era scoperto.** Seme del
   database, lettura preferenze e programmazione notifiche ora sono in
   `runCatching`, con log: un errore all'avvio non chiude piu l'app.

### Verifica
- `CrashSmokeTest` / `CrashSweepTest` aprono la `MainActivity` reale (Robolectric)
  e tutte le schermate della barra: nessun crash.
- `SecureKeyStoreTest` verifica round-trip e degradazione senza Keystore.
- `CrashLogTest` verifica scrittura, lettura, cancellazione e delega dell'handler.
- `ReceiverCrashTest` verifica che `reschedule()` registri invece di propagare.
- Nota ambiente: su questo host non c'e `/dev/kvm`, quindi l'emulatore x86_64 non
  parte. La verifica runtime passa da Robolectric (Activity reale) e dalla build
  release R8, non dall'emulatore.

## Crash all'avvio della generazione dei programmi (risolto)

**Sintomo:** l'app si chiudeva appena partiva la generazione di piano
alimentare o scheda.

**Causa:** `ProgramGenerationCoordinator.generate()` era una `suspend fun`
senza cambio di dispatcher, quindi l'intero lavoro girava sul thread
principale chiamante (`viewModelScope` = `Dispatchers.Main`): costruzione del
prompt con centinaia di alimenti ed esercizi, serializzazione di payload di
migliaia di token e `MealPortionSolver` in ciclo iterativo. Il blocco della UI
produce un ANR, che Android presenta all'utente come chiusura dell'app.

**Correzioni (da non rimuovere):**
1. `generate()` sposta il lavoro su `Dispatchers.IO` (`dispatcher`
   parametrizzato per i test) e avvolge tutto in `try/catch` che restituisce
   `ProgramGenerationResult.Error`. E' la rete di sicurezza unica per tutti i
   chiamanti (schermate, chat, azioni del Coach).
2. `AnamnesisViewModel.runAiGeneration()` (generazione automatica dopo la
   conferma dell'anamnesi, il percorso realmente usato al primo avvio) gira su
   IO con `try/catch` + `CrashLog.record`.
3. `Nutrition/Workout/DashboardViewModel`: `try/catch` + `CrashLog.record` e
   messaggio all'utente invece della chiusura del processo.
4. `Converters`: i `TypeConverter` enum tollerano valori non piu validi con
   fallback esplicito. Prima un solo valore rinominato faceva fallire la
   lettura della riga (e quindi la generazione) su database esistenti.
5. `CancellationException` viene rilanciata: non va mai inghiottita da un
   `catch (Throwable)`.

**Verifica:** `GenerationThreadingCollaudoTest` (generazione fuori dal thread
chiamante, guasti trasformati in esito, profilo incompleto gestito) + suite
completa (406 test). Il crash log e consultabile in Impostazioni > Diagnostica.
- `OnboardingProvenanceCollaudoTest` — la provenienza delle risposte non si perde
  risalvando l'onboarding (`OnboardingViewModel.answeredFrom` fonde le risposte gia
  registrate). `mealPrepPossible` e `workShift` ora passano da `select(...)`, quindi
  vengono marcati come dichiarati.
- `AnamnesisProvenanceCollaudoTest` (esteso) — BUG: l'intervista guidata non poneva MAI
  le 14 domande con default non nullable (obiettivo, esperienza, giorni di allenamento,
  tipo di lavoro, pasti, qualita del sonno...). `isAnswered` rispondeva `true` perche il
  valore esiste sempre nel profilo, e `nextQuestion` le saltava: il programma veniva
  generato su dati che l'utente non aveva mai visto. Ora `AnamnesisEngine.needsAnswer`
  chiede la domanda se manca la provenienza in `answeredQuestionIds`. L'intervista copre
  42 domande su 43 (resta fuori solo `currentLoads`, non pertinente a chi non si allena).
- `ui/screens/workout/` (2438 righe) era irraggiungibile: la navigazione usa
  `ui/workout/`. Rimosso. Conteneva `WorkoutContent`, `WorkoutDialogs`, `WorkoutUiState`,
  `CustomPlanContent`, `CustomPlanUiState`, `ExercisePickerDialog`; tutte le funzioni
  (volume per muscolo, calendario, sonno, sessione extra, libreria) esistono in
  `ui/workout/WorkoutScreen.kt`.
- Onboarding COLLEGATO: `FitCoachNavHost` sceglie `startDestination` leggendo il profilo
  via `UserProfileEntryPoint` (Hilt `@EntryPoint`, perche' la destinazione iniziale va
  decisa quando il grafo viene composto, non da un ViewModel). Nessun profilo o profilo
  incompleto -> `Routes.ONBOARDING`, altrimenti `Routes.DASHBOARD`. La bottom bar non
  compare in onboarding (`showBottomBar` dipende da `TopLevelDestination`).
- `FitCoachApplication` e' `open` per permettere a `SeededProfileApplication` (test) di
  seminare un utente in `onCreate`. I test che lanciano `MainActivity` devono usarla,
  altrimenti atterrano sull'onboarding: senza seme verificherebbero un primo avvio che
  salta l'onboarding, cioe' un percorso impossibile.
- `excludedFoods` (esclusione DURA, usata da `FoodFilter.isAllowed` e
  `NutritionBuildTools`) era scrivibile SOLO da `OnboardingViewModel`, cioe' da una
  schermata irraggiungibile: nessun utente poteva dichiarare "non voglio il pesce" e
  vederlo rispettato. Ora e' una domanda dell'anamnesi (44 domande, sezione
  "Alimentazione e budget"), con i 5 handler di `AnamnesisEngine` (question, value,
  isAnswered, reset, summary) e la provenienza in `OnboardingViewModel.answeredFrom`.
  NB: `dislikedFoods` NON e' un'esclusione: e' solo una preferenza morbida nel prompt.
- `FoodFilter` confrontava `excludedFoods` per UGUAGLIANZA col nome: l'utente scrive
  "pollo" ma il catalogo ha "Petto di pollo", quindi l'esclusione non scattava quasi
  mai. Ora `matches()` fa `contains` su nome e categoria, come gia facevano `allergies`
  e `NutritionBuildTools`.
- Primo avvio: `needsAnamnesis` era `profile?.isComplete == true && !isAnamnesisReady`,
  quindi con profilo ASSENTE valeva false e la card non compariva proprio a chi ne
  aveva bisogno. Ora `needsAnamnesis(profile) = profile == null || !profile.isAnamnesisReady`
  (funzione `internal` in `DashboardViewModel.kt`, testata da `DashboardFirstRunCollaudoTest`).
- La card da sola non bastava: con `!hasPlan` la Dashboard mostra `DashboardNoPlan`, non
  `DashboardBody`, e la CTA era "Genera il programma" -> `generateProgram()` -> "Completa
  prima il profilo": vicolo cieco. Ora con `needsAnamnesis` la CTA e' "Inizia l'anamnesi"
  e naviga all'anamnesi (label diverse da titolo e pulsante, altrimenti il test trova 2 nodi).
- Resto di `ui/screens/*` rimosso (5174 righe, 25 file): anamnesis, coach, diary,
  knowledge, notifications, nutrition, onboarding, profile, progress, provider, settings,
  shopping. Ognuna aveva gia la sua schermata viva in `ui/<area>/` (o `ui/settings/`),
  nessuna era importata da codice di produzione ne da test, nessuna aveva un produttore
  di stato. I dialoghi (Substitution/AddFood/AddItem) esistono come `private fun` nelle
  schermate vive. `ui/screens/dashboard` invece E VIVO: `DashboardContent` e le classi di
  `DashboardUiState` sono importate da `ui/dashboard/` e dai test. Resta li per ora; se lo
  si sposta in `ui/dashboard/` e solo un refactor di package, senza codice morto.
- Dashboard: nei giorni di riposo la card Allenamento non aveva alcun pulsante, quindi la
  scheda non era raggiungibile da li. Aggiunto "Vedi la scheda".
- AGGIORNATO (sessione turno AI appeso): `Routes.ONBOARDING` E raggiungibile, non e codice
  morto. `FitCoachNavHost` la usa come `startDestination` quando `profile == null ||
  !profile.isComplete`, e `OnboardingScreen` naviga a `Routes.AI_SETUP`. La nota precedente
  era sbagliata. Con profilo presente e completo l'avvio va in Dashboard, quindi l'onboarding
  e il passo provider NON compaiono per chi ha gia un profilo: per questo la verifica del
  provider vive anche in `ProgramGenerationCoordinator` (vale per tutte le schermate).

## Turno del Coach AI che resta appeso (risolto)

Sintomo: la schermata resta su "Generazione..." per decine di minuti, o la generazione
parte senza che l'utente l'abbia chiesta. Tre cause distinte, tutte corrette:

- OkHttp aveva connect/read/write timeout ma **nessun `callTimeout`**: una risposta che
  arriva a goccia non fa mai scattare il `readTimeout`, perche ogni chunk lo azzera. Il
  `callTimeout` e l'unico limite che vale sull'intera chiamata.
- La deadline del turno era controllata **solo tra un'iterazione e l'altra**. Con
  `maxIterations = 8` e retry il caso peggiore era ~36 minuti (8 x 270 s). Ora la scadenza
  e applicata anche alla singola chiamata (`CoachOrchestrator.callWithBudget`, che usa
  `withTimeoutOrNull`). Attenzione: `withTimeout` semplice non andava bene perche serve un
  esito leggibile, non un'eccezione; `withTimeoutOrNull` restituisce null e il turno chiude
  con un errore.
- Confermare l'anamnesi avviava la generazione. Ora `confirm` salva soltanto; si genera dal
  comando esplicito. `generationFinished` diventa true solo in caso di successo, cosi in
  errore resta visibile "Riprova".

Regola generale imparata qui: **ogni percorso che chiama la rete deve fallire in fretta e in
modo leggibile**. Un `isProviderReady()` controllato prima di partire e' piu utile di
qualunque timeout, perche l'utente riceve subito la causa vera.

### Distribuzione degli APK: due file ambigui (risolto)

Sintomo riportato: "l'app appena installata chiede subito l'anamnesi". Non era una
regressione del codice: nella cartella di download convivevano DUE APK.

- `FitCoach-2.2.2-debug.apk` — la build buona, con il passo provider.
- `FitCoach-2.2.3-crashfix-debug.apk` — build di una sessione precedente, con il nome
  che sembra piu' nuovo. Il suo onboarding va **dritto all'anamnesi** (non contiene
  `ai_setup`, ne' "Configura il Coach AI"). Installando quello si vede esattamente il
  comportamento segnalato.

Lezioni:
- Non lasciare piu' di un APK nella cartella servita. `serve-apk.sh` copia ma non pulisce:
  i file vecchi restano e il piu' "recente" per nome non e' detto sia il piu' recente.
- Il `versionCode` era 20 in entrambi i file: per Android due APK con lo stesso codice
  sono lo stesso prodotto, quindi non c'e' modo di distinguerli ne' di sapere quale e'
  installato. Ora e' 21 / 2.2.3, e **deve crescere** a ogni build distribuita, altrimenti
  l'aggiornamento sopra la build esistente viene rifiutato.
- Aggiunta la card **Versione** in Impostazioni (`BuildConfig.VERSION_NAME`/`VERSION_CODE`):
  e' l'unico modo per l'utente di verificare cosa ha davvero in mano.
- Per verificare un APK senza installarlo: estrarre i `.dex` con `python3 zipfile`
  (nel container non c'e' `unzip`) e cercare le stringhe UI distintive. Un controllo sul
  `aapt2 dump badging` da solo non basta, perche' dice la versione dichiarata, non il
  codice che contiene.

### Trappole nei test (verificate)

- `AiProvidersViewModel.save()` lancia una coroutine: navigare subito dopo distrugge il
  ViewModel della schermata uscente e **cancella la scrittura a meta**. Da qui la callback
  `onSaved`. Vale per ogni "salva e vai avanti".
- `CrashSweepTest` falliva a intermittenza: la barra inferiore compare solo dopo la lettura
  del profilo (`startDestination == null` fino ad allora), quindi `waitForIdle` non basta.
  Serve `waitUntil { onAllNodesWithText(label)...isNotEmpty() }`. Un test che passa da solo e
  fallisce nella suite e quasi sempre una race di composizione, non una regressione.
- Un test che asserisce su un testo che non esiste nella UI reale e rumore: prima di scrivere
  l'asserzione si legge il composable. Il titolo vero e' "Anamnesi iniziale - Il tuo profilo".
- Controprova obbligatoria per i fix di timeout: rimuovere il budget e vedere il test
  fallire. Senza questo passaggio non si sa se il test guarda davvero il difetto.

## Ordine del primo avvio: il provider viene prima delle domande

Sintomo riportato piu' volte: "l'installazione porta direttamente all'anamnesi", mai al
setup del provider. La causa non era l'APK sbagliato: **l'onboarding era il flusso di
raccolta dati** e il passo del provider stava in fondo, quindi al primo avvio si vedevano
subito le domande.

Decisione: `startDestination` in `FitCoachNavHost` segue questa precedenza.

1. Provider non configurato e setup mai rimandato -> `AI_SETUP`.
2. Profilo incompleto -> `ONBOARDING`.
3. Altrimenti -> `DASHBOARD`.

Vale anche per un profilo gia' esistente: un utente vecchio senza provider vede il setup.
Se ha scelto "Configura piu' tardi" il flag `SetupStateStore` lo esclude, altrimenti l'app
lo avrebbe bloccato nel setup a ogni avvio.

### Trappole incontrate


- `AiConfigResolver.resolve()` restituisce **sempre** una configurazione, anche senza chiave:
  usarla come test di prontezza fa risultare configurato un provider mai impostato. Serve
  `resolveConfigured()`.

- Navigare da una coroutine non-main fa crashare il `NavController` con "setCurrentState
  must be called on the main thread". La decisione di destinazione si prende una volta
  sola in `LaunchedEffect` e si riusa nel callback, senza rileggere il database.

- Nei test, `CleanDatabaseRule` deve azzerare anche i DataStore: il file sopravvive tra i
  test e l'istanza e' un singleton in memoria, quindi il flag va riscritto via store.
  `SeededProfileApplication` semina il flag "setup gia' visto", perche' rappresenta un
  utente gia' operativo.

## Validazione della generazione AI: una sola funzione, non due

C'e' **una sola funzione**: `ProposalChecker.check` in
`domain/coach/ProposalChecker.kt`. Produce `Report(violations)`; `validate` accetta se
`violations.isEmpty()`, `diagnose` restituisce le stesse violazioni. Prima erano due
implementazioni della stessa regola e sono divergite: il vincolo dolce/salato della
colazione era solo nella diagnostica, quindi il piano veniva respinto e poi **salvato se
il modello insisteva**. Le soglie di volume erano `6..24` fisse nel cancello e dinamiche
nella diagnostica.

Regola: **una regola nuova va aggiunta solo in `ProposalChecker`.** Non reintrodurre
controlli dentro `validate`, `buildNutrition` o `mapWorkout`: quei metodi mappano soltanto
(tipo AI -> entita'). Un `return null` di policy in fase di mappatura e' un rifiuto senza
diagnosi, cioe' esattamente la classe di bug che ha prodotto il caso della colazione.
`GateDiagnosticsAgreementTest` verifica la proprieta' che rende la divergenza impossibile:
*validate accetta se e solo se il checker non trova violazioni*. Se qualcuno aggiunge un
controllo fuori dal checker, quel test cade.

- Il **calendario** della proposta (`workoutDayIndices`/`restDayIndices`) era un duplicato
  di `workout.days`, confrontato solo con esso. Non portava informazione e poteva solo
  divergere: ora `SubmitFullProgramTool` lo **deriva**. Non chiedere al modello dati
  ricavabili da altri dati gia' presenti.

- Lo schema di `submit_full_program` deve dire che `workout.days` contiene **solo le
  sedute**, non i sette giorni della settimana. L'ambiguita' produceva tre giorni vuoti
  su sette, e il messaggio "attesi 4, ricevuti 7" non bastava a correggerla.

- `SimulatedAiPrograms` e' un fixture: deve rispettare i vincoli reali (a colazione usa
  `BreakfastConstraint.flavorOf`), altrimenti i test falliscono per un difetto del fixture
  e non del codice. Non legare i test a nomi di alimento hardcoded ("Petto di pollo"):
  cercare per ruolo (macro dominante) rende il test indipendente dal fixture.

- `NavigationFlowCollaudoTest`, `DashboardRenderCollaudoTest`, `AnamnesisLayoutTest`
  falliscono a rotazione: e' flakiness di Robolectric/Compose (`ArrayIndexOutOfBounds`
  dentro il framework di test), **non** una regressione. Verificare con `git stash` prima
  di indagare: sul codice originale falliscono ugualmente.

## Il corpus InVictus: uno solo, e non nel prompt

Due file facevano la stessa cosa con tassonomie diverse: `InvictusCorpus` (159 regole, 15
topic) finiva **nel prompt**, `InvictusKnowledge` (991 regole, 35 categorie) era quello che
`consult_invictus` interrogava. Il modello leggeva il corpus povero e non poteva cercare in
quello ricco. Ora esiste solo `InvictusKnowledge` (1.046 regole).

- La sorgente del vecchio `InvictusCorpus` era `/tmp/claims_final.json`, che **non esiste
  piu'**. Le 55 regole che non erano presenti altrove sono in
  `tools/corpus/invictus_corpus_extra.json`, versionato. Non cancellarlo: e' l'unica copia.
- `build_invictus_v2.py` mappa i vecchi topic sulle categorie del generatore
  (`RECOVERY`→`REST`, `EXERCISE`→`EXECUTION`, `SAFETY`→`SAFETY_TRAIN`/`SAFETY_NUT`) e fa
  dedup per statement. La rigenerazione e' idempotente: verificato con `diff` prima e dopo.
- **Non rimettere il corpus nel prompt.** La docstring di `InvictusKnowledge` lo diceva
  gia' dal primo giorno, mentre il codice lo iniettava: la divergenza e' durata perche'
  nessun test la misurava. Ora `ThresholdsAreWarningsTest` controlla che
  `AiProgramGenerationService` non contenga `InvictusKnowledge.promptBlock`.

Attenzione al parsing dei `.kt` generati: uno statement conteneva `"grassi):"` e un
parser che bilancia le parentesi senza ignorare le stringhe perde la regola in silenzio.
Contare le regole prima e dopo ogni fusione, e confrontare gli statement, non solo i totali.

## Il corpus va consultato per codice, non chiesto in prosa

Per tre prove live `toolTrace=[submit_full_program]`: zero consultazioni, con il prompt che
lo chiedeva in prosa. La prosa perde contro uno strumento obbligatorio in concorrenza, quindi
il prerequisito ora e' imposto dall'orchestratore (`Prerequisite.consultations`).

Tre vincoli che il codice rispetta, e che e' facile rompere:

- **Il blocco e' PRIMA dell'esecuzione.** `submit_full_program` salva quando risponde `ok`:
  un controllo dopo la chiamata arriverebbe a database gia' scritto. Il rifiuto sostituisce
  l'output e rimanda il modello al prerequisito.
- **La forzatura e' del prerequisito, non del target.** `escalateForce` forzava `submit`
  quando mancava la consegna: con un prerequisito inevaso il modello non poteva piu' chiamare
  `consult_invictus` e il turno falliva. Si forza lo strumento mancante.
- **"Chiamato" non basta.** `consult_invictus` risponde `ok=true` anche con zero risultati, e
  `area=nutrizione&argomento=petto` passa il controllo dell'area senza restituire nulla sulla
  nutrizione. Contano solo le chiamate con contenuto e argomento coerente all'area.

I fixture dei test devono consultare come farebbe un modello reale: `GenerationScript.consultations()`
esiste per questo. Un fixture che consegna subito viene bloccato, e il test fallisce per un
difetto del fixture, non del codice.

Effetto misurato sulla prova live: 4 consultazioni, `HA CONSULTATO INVICTUS: true`, 76-90s
(prima 176s senza consultazioni: il modello non perdeva tempo a leggere).
