# FitCoach — Brief tecnico per il lavoro sulla grafica (UI)

Documento operativo per chi lavora sulla parte grafica/visiva dell'app.
Tutti i dati sono estratti dal codice reale del repository (branch
`fitcoach-2.0-clean`), non inventati.

---

## 1. Dove sta la grafica (mappa file)

Tutta la UI attiva è in `app/src/main/java/com/fitcoach/app/ui/`.
Il package `app/src/main/java/com/fitcoach/app/v2/` esiste ma **NON è collegato
alla navigazione** (nessuno lo referenzia): la UI in uso è quella sotto `ui/`.

| Area | File | Righe |
|---|---|---|
| Design system (colori) | `ui/theme/Color.kt` | 30 |
| Design system (tema) | `ui/theme/Theme.kt` | 56 |
| Design system (tipografia) | `ui/theme/Type.kt` | 72 |
| Componenti riusabili | `ui/components/Common.kt` | 203 |
| Grafici | `ui/components/Charts.kt` | 135 |
| Scanner barcode | `ui/components/BarcodeScannerDialog.kt` | 225 |
| Navigazione (destinazioni) | `ui/navigation/Destinations.kt` | 45 |
| Navigazione (NavHost) | `ui/navigation/FitCoachNavHost.kt` | 76 |
| Barra inferiore | `ui/navigation/FitCoachBottomBar.kt` | 86 |
| Dashboard | `ui/dashboard/DashboardScreen.kt` | 303 |
| Alimentazione | `ui/nutrition/NutritionScreen.kt` | 514 |
| Diario | `ui/diary/DiaryScreen.kt` | 434 |
| Spesa | `ui/shopping/ShoppingScreen.kt` | 264 |
| Allenamento | `ui/workout/WorkoutScreen.kt` | 1175 |
| Scheda personalizzata | `ui/workout/CustomWorkoutScreen.kt` | 380 |
| Progressi | `ui/progress/ProgressScreen.kt` | 490 |
| Coach AI (chat) | `ui/coach/CoachScreen.kt` | 276 |
| Onboarding | `ui/onboarding/OnboardingScreen.kt` | 621 |
| Anamnesi | `ui/anamnesis/AnamnesisScreen.kt` | 380 |
| Impostazioni | `ui/settings/SettingsScreen.kt` | 258 |
| Provider AI | `ui/settings/AiProvidersScreen.kt` | 213 |
| Profilo (modifica) | `ui/settings/ProfileEditScreen.kt` | 224 |
| Notifiche (impostazioni) | `ui/settings/NotificationSettingsScreen.kt` | 159 |
| Fonti scientifiche | `ui/settings/KnowledgeScreen.kt` | 158 |

Totale UI: **~10.000 righe** in 40 file.

---

## 2. Design system attuale (DA CONSERVARE come base)

### 2.1 Palette — `ui/theme/Color.kt`

| Token | Hex | Uso |
|---|---|---|
| `FitBlack` | `#0B0B0C` | Sfondo app |
| `FitAnthracite` | `#16171B` | Superficie scura |
| `FitSurface` | `#1C1D21` | Card |
| `FitSurfaceVariant` | `#26272C` | Card secondaria / tile |
| `FitOutline` | `#3A3B41` | Bordi |
| `Emerald` | `#10B981` | **Accento primario** |
| `EmeraldDark` | `#0E8F65` | Accento premuto |
| `EmeraldLight` | `#34D399` | Accento chiaro |
| `TextPrimary` | `#ECEDEE` | Testo principale |
| `TextSecondary` | `#A1A3A9` | Testo secondario |
| `TextDisabled` | `#6B6D73` | Testo disabilitato |
| `ProteinColor` | `#60A5FA` | Macro proteine (blu) |
| `CarbsColor` | `#F59E0B` | Macro carboidrati (ambra) |
| `FatColor` | `#A78BFA` | Macro grassi (viola) |
| `HighCarbColor` | `#10B981` | Giorno HIGH carb |
| `MediumCarbColor` | `#F59E0B` | Giorno MEDIUM carb |
| `LowCarbColor` | `#60A5FA` | Giorno LOW carb |
| `ErrorRed` | `#EF4444` | Errori |

**Regola**: la dark mode è **predefinita e forzata** (`Theme.kt` usa sempre
`FitDarkColorScheme`, non c'è tema chiaro).

### 2.2 Tema — `ui/theme/Theme.kt`
`darkColorScheme` con `primary = Emerald`, `background = FitBlack`,
`surface = FitSurface`. Status bar e navigation bar trasparenti, icone chiare.

### 2.3 Tipografia — `ui/theme/Type.kt`
Famiglia: `FontFamily.SansSerif` (di sistema). **Nessun font custom** in
`res/font/` (opportunità di miglioramento).
Stili definiti: `displaySmall` 32sp Bold, `headlineMedium` 26sp Bold,
`headlineSmall` 22sp SemiBold, `titleLarge` 20sp SemiBold, `titleMedium` 16sp
SemiBold, `bodyLarge` 16sp, `bodyMedium` 14sp, `labelLarge` 14sp,
`labelMedium` 12sp, `labelSmall` 11sp.

---

## 3. Componenti riusabili esistenti (riutilizzare, non duplicare)

Da `ui/components/Common.kt`:

- `SectionCard { }` — card con angoli 18dp, sfondo `FitSurface`, bordo 1dp `FitOutline@40%`, padding 16dp.
- `SectionHeader(title, subtitle?)` — titolo + sottotitolo.
- `StatTile(label, value, accent = Emerald)` — tile con angoli 14dp, sfondo `FitSurfaceVariant`.
- `MacroBar(proteinGrams, carbsGrams, fatGrams)` — barra proporzionale P/C/G (10dp alto, colori macro).
- `MacroLegend(protein, carbs, fat)` — legenda "P / C / G" con quadratini colorati.
- `Pill(text, color = Emerald)` — badge a pillola (raggio 50, sfondo colore@15%).
- `LoadingBox()` — spinner centrato emerald.
- `EmptyState(title, message, action?)` — stato vuoto centrato con azione opzionale.

Da `ui/components/Charts.kt`:

- `ChartPoint(label, value)` — punto di serie.
- `LineChart(points, lineColor = Emerald, heightDp = 180, unitSuffix)` —
  grafico a linee su **Compose Canvas** (nessuna dipendenza esterna), con
  griglia, assi etichettati, punti. Mostra "Min · Max" sotto.

> Nota: la dipendenza **Vico** (`com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.28`)
> è dichiarata in `app/build.gradle.kts` ma il grafico attualmente usato è
> quello custom su Canvas. Valutare se adottare Vico o rimuovere la dipendenza.

---

## 4. Navigazione e struttura

### 4.1 Barra inferiore — 5 tab (`ui/navigation/Destinations.kt`)
1. **Dashboard** (route `dashboard`)
2. **Alimentazione** (route `nutrition`)
3. **Allenamento** (route `workout`)
4. **Progressi** (route `progress`)
5. **Coach AI** (route `coach`)

La bottom bar è mostrata solo sulle route top-level
(`FitCoachNavHost.kt`, `TopLevelDestination`). `FitCoachBottomBar` usa
`NavigationBar`/`NavigationBarItem` con `animateColorAsState` per il colore
selezionato (Emerald vs onSurfaceVariant).

### 4.2 Altre route (`Routes`)
`shopping`, `diary`, `custom_workout`, `onboarding`, `anamnesis`,
`anamnesis_edit`, `settings`, `ai_providers`, `notification_settings`,
`profile_edit`, `knowledge`.

### 4.3 Animazioni transizioni (`FitCoachNavHost.kt`)
- enter: `slideInHorizontally(tween 260) { it/6 } + fadeIn(260)`
- exit: `fadeOut(180)`
- popEnter: `fadeIn(220)`
- popExit: `slideOutHorizontally(220) { it/6 } + fadeOut(180)`

Animazioni presenti nel codice: `animateColorAsState` (2), `animateScrollToItem` (1).
**Margine di miglioramento**: animazioni di lista, shimmer di caricamento,
transizioni condivise.

---

## 5. Stato attuale della grafica (cosa c'è / cosa manca)

### C'è
- Tema dark completo e coerente (colori + tipografia).
- Componenti riusabili per card, tile, badge, barre macro.
- Grafico a linee custom funzionante.
- Barra di navigazione a 5 tab con animazione colore.
- Transizioni tra schermate.

### Manca / migliorabile (candidati per il lavoro grafico)
1. **Nessun font custom**: si usa `SansSerif` di sistema → identità visiva debole.
2. **Icona launcher minimale**: `ic_launcher_foreground.xml` è un semplice
   tracciato emerald (croce + linea). Da rifare (logo, adaptive icon, monochrome).
3. **Splash screen assente**: nessun `SplashScreen` / tema di avvio dedicato.
4. **Nessuna immagine/illustrazione**: `res/drawable` ha solo 2 icone vettoriali.
   Nessun asset di onboarding, stato vuoto, badge obiettivi.
5. **`v2/presentation/AnamnesisScreenV2` non collegato**: decidere se adottarlo.
6. **Vico dichiarato ma non usato**: allineare dipendenze e stile grafici.
7. **Screenshot automatici solo per l'anamnesi**: le altre schermate non hanno
   un composable stateless (`*Content`) → difficile testarle/renderizzarle.
8. **Colori/alpha/dp sparsi** fuori dal design system (es. `.copy(alpha=...)`,
   raggi e padding hardcoded nelle schermate) → da centralizzare in token.

---

## 6. Risorse grafiche attuali

```
app/src/main/res/
  drawable/ic_launcher_foreground.xml   (vettore, 108dp)
  drawable/ic_notification.xml
  mipmap-anydpi-v26/ic_launcher.xml
  mipmap-anydpi-v26/ic_launcher_round.xml
  values/strings.xml                     (20 stringhe)
  xml/{backup_rules,data_extraction_rules,file_paths}.xml
```
Non esistono: `res/font/`, `res/mipmap-*dpi/*.png`, splash, immagini raster.

---

## 7. Come verificare la grafica senza emulatore (già funzionante)

Esiste un test che **renderizza davvero** un composable su JVM (Robolectric +
grafica nativa) e salva un PNG: `app/src/test/java/com/fitcoach/app/ui/anamnesis/AnamnesisScreenRenderTest.kt`.

```bash
./gradlew :app:testDebugUnitTest --tests "*AnamnesisScreenRenderTest"
# Output: app/build/screenshots/anamnesis_final.png
```

Pattern da replicare per ogni schermata da verificare visivamente:
```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class XxxScreenRenderTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
    // renderizza un composable stateless con stato finto, poi:
    val view = composeRule.activity.window.decorView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    File("build/screenshots/xxx.png").outputStream().use { bitmap.compress(PNG, 100, it) }
}
```

**Prerequisito per estendere gli screenshot**: estrarre da ogni schermata un
composable stateless, es. `DashboardContent(state: DashboardUiState, ...)`,
lasciando `DashboardScreen(navController, viewModel = hiltViewModel())` come
wrapper. Oggi solo l'anamnesi ha questo pattern.

---

## 8. Vincoli tecnici da rispettare

- **Solo dark mode** (non introdurre tema chiaro senza richiesta).
- **Min SDK 26**, target come da `app/build.gradle.kts`.
- **Offline-first**: la grafica non deve dipendere dalla rete.
- **Compose + Material 3** (niente View/XML per le schermate).
- **Lingua UI: italiano** (le stringhe in `strings.xml`; molte stringhe sono
  ancora hardcoded nei composable → da spostare in risorse).
- **Accessibilità**: `contentDescription` sulle icone, contrasto su sfondo nero.
- **Layout adattivo**: gli screenshot di riferimento usano `w360dp-h800dp-xhdpi`.

---

## 9. Stati UI da progettare (per schermata)

Ogni schermata ha bisogno di questi stati visivi:

| Stato | Componente esistente | Note |
|---|---|---|
| Caricamento | `LoadingBox()` | valutare shimmer/skeleton |
| Vuoto (nessun profilo) | `EmptyState(...)` | |
| Vuoto (nessun dato) | `EmptyState(...)` | es. nessun peso registrato |
| Errore | testo rosso | valutare componente `ErrorState` |
| Contenuto | `SectionCard` + `StatTile` | |
| Offline | — | da progettare (banner "modalità offline") |

`DashboardUiState` (esempio di campi da mostrare):
`loading, hasProfile, needsAnamnesis, anamnesisSummary, profileName, goalLabel,
dayName, dayType (HIGH/MEDIUM/LOW), targetKcal, targetProtein, targetCarbs,
targetFat, meals, workoutTitle, workoutExercises, workoutDone,
nextNotification, weightTrend, latestWeight, warnings`.

---

## 10. Prossimi passi consigliati (ordine)

1. **Token del design system**: centralizzare raggi, spaziature, alpha in
   `ui/theme/` (es. `Dimens.kt`) e sostituire i valori hardcoded nelle schermate.
2. **Icona + splash**: nuovo launcher (adaptive + monochrome) e splash screen.
3. **Font custom** (es. Inter/Manrope) in `res/font/` e aggiornare `Type.kt`.
4. **Refactor stateless**: estrarre `*Content` da ogni schermata per abilitare
   screenshot e preview Compose.
5. **Screenshot di riferimento** di tutte le 5 tab + schermate chiave.
6. **Stati**: shimmer di caricamento, `ErrorState`, banner offline, stati vuoti illustrati.
7. **Decidere** su `v2/presentation` e sulla dipendenza Vico.

---

## 11. Nota sul versionamento

Il lavoro grafico va fatto sul branch **`fitcoach-2.0-clean`** (branch corrente).
Il push verso GitHub richiede un token con permesso di **scrittura**: il token
attualmente disponibile in questo ambiente è di sola lettura (errore 403).
