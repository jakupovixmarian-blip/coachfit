# FitCoach 2.0 — AGENTS.md

Native Android fitness app. Kotlin + Jetpack Compose, single-module, clean architecture by package.

## Build & test commands

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$PATH

./gradlew :app:compileDebugKotlin   # fast compile check (~40 s)
./gradlew :app:assembleDebug        # debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # 35 tests (JUnit + Robolectric + Room in-memory)
./gradlew :app:lintDebug            # 0 errors expected
```

Run long Gradle tasks in the background (`nohup ... > /tmp/x.log 2>&1 &`) and poll the log; the
runtime caps foreground commands well below a full clean build.

There is **no emulator and no KVM** in this environment (`ls /dev/kvm` fails), so runtime
verification is done with Robolectric tests instead of an instrumented run. Do not claim an
instrumented/on-device test was executed here.

## Package layout (`dev.fitcoach.app`)

- `domain/model` — pure Kotlin models + enums; no Android imports. Business math lives here
  (`UserProfile.bmr()`, `targetCalories()`, `proteinTarget()`…).
- `domain/repository` — repository interfaces (one file `Repositories.kt`, plus `BackupRepository.kt`).
- `domain/usecase` — pure logic: `ProgressionEngine`, `CoachEngine`, `CoachPromptBuilder`.
- `data/local/{db,dao,entity,mapper}` — Room. 17 entities, `FitCoachDatabase.NAME = "fitcoach.db"`.
- `data/repository` — repository impls (`RepositoriesImpl.kt`).
- `data/seed` — `ExerciseSeed`, `FoodSeed`. `data/bootstrap/CatalogInitializer` seeds on first launch.
- `data/ai` — `AiClient` (OpenAI-compatible) + `OpenFoodFactsClient` (live public API, no key).
- `data/backup` — `BackupModels.kt` (serializable DTOs) + `BackupRepositoryImpl` (export/import).
- `core/{di,security,timer,util,result}` — Hilt modules, Keystore wrapper, rest-timer service, utils.
- `ui/{components,navigation,screens}` — Compose UI; screens grouped by feature.

## Conventions and rules

- **Never invent nutrition values.** Foods come from Open Food Facts (live) or from `FoodSeed`
  where every entry is `source = MANUAL` and documented as an *editable reference*, not verified data.
  Anything the AI proposes must reference an existing catalog `id`; the app recomputes all totals
  locally (`CoachEngine.validatePlan`) and the user must approve before anything is applied.
- Italian is the UI language (labels, messages, comments where they explain domain rules).
- Hilt everywhere: `@HiltAndroidApp` on `FitCoachApp`, `@HiltViewModel`, modules in `core/di`.
- ViewModels expose a single `StateFlow<XUiState>`; avoid `combine` with suspend calls inside the
  transform — collect the combined flow and do suspend work in `collect`.
- Room DAOs must expose suspend getters for anything used outside a Flow.
- New screens: add the route in `ui/navigation/Routes.kt`, register it in `FitCoachNavHost.kt`,
  and only add it to `TopDestination` if it belongs in the bottom bar.
- Keep charts dependency-free: `ui/components/Charts.kt` uses native Compose Canvas.
  (Vico was removed deliberately — it was an alpha dependency.)

## Gotchas

- `RestTimerService` is a plain `Service` (not `LifecycleService`) to avoid pulling in
  `lifecycle-service`. It returns `START_NOT_STICKY` and never calls `startForeground` for a
  stop/pause that arrives while no timer is active.
- `androidx.compose.material3.Snackbar` is a low-level composable, not a host: show messages as
  inline `Text` instead (this bit us twice).
- Chat turns must be persisted before they are rendered: a LazyColumn keyed on `it.id`
  crashes on duplicate keys when several in-memory messages all carry id = 0. Both the intake
  and the Coach tab therefore go through `CoachChatRepository`, which returns real ids.
- Seed the intake opening question with a re-read right before the insert, so two concurrent
  seeds (screen init and "ricontrolla") cannot insert it twice.
- `MeasurementType.CUSTOM` needs a `customLabel`; `Measurement.label` falls back to the enum label.
- Room + Robolectric tests need `ApplicationProvider.getApplicationContext<Context>()` and
  `.allowMainThreadQueries()`.

## Workout program model

- A `Routine` is one session of a program: `programId`, `dayIndex` and `dayLabel` group the
  sessions. A standalone routine keeps `programId = 0`.
- There is NO deterministic program generator: split, exercises, sets, reps, RIR and rest are
  decided by the AI agent (`CoachAgent.generateProgram`), grounded on the profile analysis, the
  exercise catalog and the enabled `KnowledgeNote`s. Do not reintroduce volume/validator rules
  that prescribe numbers — the app only rejects structurally incoherent answers (unknown ids,
  impossible numbers) in `CoachAgentPrompts`.
- Exercise identity is the `Exercise` object, not `id`: a catalog entry can still have `id = 0`
  before it is persisted, so keying de-duplication on the id collapses the whole catalog.
- The AI never returns nutrition values: `CoachAgent.generateMealPlan` only picks food ids and
  grams, so the numbers stay those of Open Food Facts / the user's archive.
- `CoachAgentPrompts` builds every prompt and parses every answer; `AiGateway` is the seam that
  wraps the provider, so the agent is testable with a fake gateway (see `CoachAgentPromptsTest`).
- Manual programming lives only in `ManualProgramScreen`/`ManualProgramViewModel`, independent
  from the AI: that is the one place the user types sets and reps themselves.
- `saveProgram` is atomic: it deactivates and deletes the previous program together with its
  `PLANNED` sessions, so regenerating never stacks days.
- Room schemas are exported to `app/schemas` (`room.schemaLocation`) and exposed as debug assets
  so `MigrationTestHelper` can read them. Bump `version` and add a migration — never
  `fallbackToDestructiveMigration`.
- `AiModelCapabilities.forModel(provider, modelId)` is the single source of truth for the request
  body: reasoning models (o-series, gpt-5) need `max_completion_tokens` and reject
  `temperature`/`top_p`/`response_format`. Verify with `AiRequestPayloadTest`.

## Meal plan generation (per-day)

- A full week in one answer overflows a small output budget and comes back truncated, which is
  what made the plan look broken. `CoachAgent.generateMealPlan` therefore asks **one day per
  call** (`MEAL_PLAN_DAYS` days), passing the previous days' summary so the week stays coherent.
  Keep it that way: do not merge it back into a single weekly request.
- `CoachAgentPrompts.parseMealPlanDay` parses a single day and also tolerates a `days` array, so
  a model that answers with the whole week is still usable. `parseMealPlanHeader` reads name and
  rationale from any day's answer.
- The UI exposes generation from the Plan tab (`PlanScreen` -> `PlanViewModel.generateWithAi`),
  reporting progress day by day. A plan is never generated implicitly at startup.

## Rest timer alarm

- `RestTimerService` plays a tone and a vibration pattern when the countdown ends, and posts a
  high-importance notification on `ALARM_CHANNEL_ID` so it is noticed with the screen off. Sound
  and vibration are wrapped in `runCatching`: a muted ringer or a device without a vibrator must
  never crash the timer.

## UI style

- Dark-first, five-tab bottom bar (`TopDestination`); the other screens are reached from the Home
  shortcuts (`SecondaryDestinations`). Theme tokens live in `ui/theme`; shared widgets
  (`FitCard`, `HeroCard`, `StatTile`, `ProgressTrack`, `Tag`, `ScreenHeader`) in
  `ui/components/Common.kt`. Prefer these over ad-hoc `Card`/`Text` styling so screens stay consistent.

## Dependency locking e audit dipendenze

- `app/build.gradle.kts` attiva il dependency locking **solo** sulle classpath dell'app
  (`release/debug` `Compile`+`Runtime`), non con `lockAllConfigurations()`. Motivo: il lockfile
  di tutte le configurazioni include la toolchain di build e le lib di test, e OSV-Scanner vi
  trova 51 vulnerabilità in 12 pacchetti (netty, guava, protobuf-java, commons-io, bouncycastle)
  che **non finiscono nell'APK**. Ristretto alle classpath dell'app: 162 pacchetti, nessun problema.
- Rigenerare dopo ogni cambio di dipendenze:
  `./gradlew :app:dependencies --write-locks`  → produce `app/gradle.lockfile`
- `tools/dependency-audit/` contiene il manifest di solo audit per AppJet (`pom.xml`), lo script di
  import `import_resolved.py`, `coverage_check.py`, il collaudo `test_import_resolved.py`, più
  `deps.txt` (albero release) e `resolved.json` (329 coordinate, 0 disallineamenti).
- Non serve più `lockAllConfigurations()`: il lockfile versionato va committato, altrimenti il
  build successivo fallisce per lock mancante.

## Ambiente

- L'ambiente di lavoro si azzera tra le sessioni: Java, Android SDK (`/opt/android-sdk`) e la
  cartella `~/apk-share` vanno reinstallati/ricreati. `sudo` è disponibile (utente in gruppo sudo).
- Publish APK: `python3 -m http.server 12000 --directory ~/apk-share` →
  `https://work-1-jqldlxqmurlrokks.prod-runtime.all-hands.dev/<file>`.
- Il token `$GITHUB_TOKEN` in ambiente è di **sola lettura**. Per il push serve il PAT fornito
  dall'utente, passato inline nell'URL e mai salvato in git config.

## Lockfile del buildscript di root

- `build.gradle.kts` attiva il locking sulla `classpath` del buildscript, così i 6 plugin di build
  entrano in `buildscript-gradle.lockfile` (156 pacchetti).
- Il buildscript riporta 49 vulnerabilità in 14 pacchetti, **tutte nella toolchain**: nessuna
  compare in `app/gradle.lockfile`. L'app resta pulita (162 pacchetti, nessun problema).
- Finding noto e accettato: `kotlin-gradle-plugin` 2.0.21 → CVE-2026-53914 (moderata). Fix solo in
  2.4.20-Beta1, ultima stabile 2.2.0. Da rivedere quando esce una stabile con la patch.
- Dettagli in `tools/dependency-audit/buildscript-scan-report.md`.

## Aggiornamento Kotlin 2.4.20 e il vincolo Hilt (CVE-2026-53914) — CHIUSO

- Toolchain attuale: Kotlin **2.4.20**, KSP **2.3.13**, AGP **8.12.3**, Gradle **8.13**,
  Hilt **2.56.2**, Room **2.8.5**. La CVE-2026-53914 non e' piu' presente.
- L'aggiornamento trascina 6 dipendenze in catena: Kotlin -> KSP -> AGP >= 8.12 -> Gradle >= 8.13
  -> Hilt -> Room.
- **Chiave del fix:** Hilt 2.56.2 non legge il metadata di Kotlin 2.4 (`kotlin-metadata-jvm`
  interno fermo a 2.2.0). Soluzione: in `app/build.gradle.kts` forzare `languageVersion` e
  `apiVersion` a `KOTLIN_2_2`, cosi Kotlin 2.4.20 emette metadata 2.2. Verificato con
  `javap -v`: `mv=[2,2,0]`.
- `kotlinOptions { jvmTarget = "17" }` non e' piu' accettato: usare `kotlin { compilerOptions {} }`.
- Verifiche: `assembleDebug` ok, 61 test passati, app 162 pacchetti (OSV pulito), buildscript 156
  pacchetti (49 finding, tutti toolchain), `scope_check.py` 14/14 fuori dall'app.
- Dettagli e percorso completo: `tools/dependency-audit/kotlin-upgrade-attempt.md`.
