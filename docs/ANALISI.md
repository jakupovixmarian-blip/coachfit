# FitCoach – Rapporto di analisi iniziale (criticità e piano di correzione)

Data: 2026-11-11 · Ambito: revisione dell'app esistente (`com.fitcoach.app`).
Metodo: lettura statica dell'intero codice (102 file Kotlin), dei motori, del DB Room,
dei repository, del layer AI e delle schermate. Nessuna ricostruzione da zero.

## Sintesi

L'app è già solida nelle fondamenta (Clean Architecture, MVVM, Room, Hilt, motori
deterministici testati) ma presenta **scostamenti rispetto alla specifica master** su sei
fronti: (1) anamnesi non condotta dall'AI e non bloccante, (2) budget non realmente
vincolante, (3) database alimentare statico senza fonti esterne, (4) robustezza parser AI
insufficiente, (5) generazione dei piani ancora deterministica e non guidata dall'LLM,
(6) collaudo end-to-end non eseguibile in questo ambiente.

---

## 1. Anamnesi AI (spec. §3)

**Problema.** L'onboarding è un questionario deterministico a 8 step
(`OnboardingScreen.kt`, 583 righe). La generazione dei programmi avviene al termine del
questionario **senza intervista AI, senza riepilogo, senza rilevazione di contraddizioni e
senza conferma esplicita**.

**Cause.** Non esiste uno stato "anamnesi completata/confermata"; `GenerateProgramsUseCase`
è invocato direttamente dall'onboarding.

**Componenti coinvolti.** `ui/onboarding/*`, `domain/usecase/GenerateProgramsUseCase.kt`,
`domain/model/UserProfile.kt`, `data/repository/UserRepository.kt`.

**Correzione.** Introdurre (a) un campo di stato anamnesi nel profilo
(`anamnesisCompleted`, `anamnesisConfirmedAt`); (b) un'intervista AI progressiva con
domande mirate, rilevazione campi mancanti/contraddizioni e riepilogo; (c) un gate: la
generazione dei programmi è **bloccata** finché l'anamnesi non è confermata.

## 2. Budget mensile (spec. §5)

**Problema.** Esiste solo `weeklyFoodBudgetEur` e un filtro grossolano "economico/costoso"
(`MealPlanGenerator.applyPreferences`, soglia fissa 70 €). **Nessun calcolo di costo
giornaliero/settimanale/mensile, nessun budget residuo, nessuna riformulazione** se il
piano sfora. La spec chiede budget **mensile**, con opzione "1 o più persone" e "solo
alimentazione personale o spesa totale".

**Cause.** Manca un motore di costo e prezzi associati agli alimenti.

**Componenti.** `domain/nutrition/MealPlanGenerator.kt`, `UserProfile`, `FoodItem`.

**Correzione.** Nuovo `BudgetEngine` + campo `pricePerKgEur` su `FoodItem`; estensione
profilo con `budgetScope` (personale/totale) e `budgetPersons`; calcolo costi e
riformulazione verso alimenti più economici mantenendo i target.

## 3. Database alimentare progressivo (spec. §6)

**Problema.** `FoodSeedData` è **statico** (~150 alimenti CREA/USDA). Non esistono
integrazioni Open Food Facts / USDA API, né cache di alimenti recuperati, né sezione
"I miei alimenti". La ricerca non consulta fonti esterne.

**Cause.** `FoodRepository`/`FoodDao` operano solo su Room; manca un layer di ricerca
remota con priorità locale e deduplicazione.

**Componenti.** `data/local/entity/Entities.kt` (`FoodItemEntity`), `FoodDao`,
`FoodRepository`, `MealPlanGenerator` (usa solo `foods` seed).

**Correzione.** Estendere `FoodItemEntity` (sourceId, normalizedName, brand, acquiredAt,
updatedAt, state); aggiungere DAO di ricerca/dedup; nuovo `FoodSearchRepository` con
ordine **locale → Open Food Facts → USDA**; UI "I miei alimenti".

## 4. Robustezza parser AI / OpenRouter (spec. §14)

**Problema.** `ChatMessageDto.content` è `String` **non-null** e `Choice.message` è
nullable; se il provider restituisce `content: null` (frequente su OpenRouter con modelli
reasoning), la deserializzazione **fallisce** e la risposta viene scartata. Non sono
gestiti `reasoning`, `tool_calls`, `refusal`. Il `content` mostrato potrebbe includere il
"reasoning" interno. Il logging è già `BASIC` con `redactHeader("Authorization")` (ok).

**Cause.** DTO non aderenti allo schema OpenAI completo.

**Componenti.** `data/remote/AiDtos.kt`, `AiClient.kt`.

**Correzione.** Rendere `content` nullable, aggiungere `reasoning_content`, `tool_calls`,
`refusal`; estrarre il testo utile (escludendo il reasoning), gestire risposte vuote con
messaggio chiaro, non loggare mai la chiave.

## 5. Generazione piani guidata dall'LLM (spec. §4, §5, §10)

**Problema.** I piani sono generati **solo deterministicamente**. La spec chiede che
l'LLM scelga alimenti/preparazioni e la programmazione di allenamento, con i motori
deterministici a validare e calcolare. Le funzionalità esistenti funzionano, quindi **non
vanno sostituite**: va aggiunto un percorso AI con **una sola chiamata** per il piano
settimanale (JSON validato), con fallback deterministico.

**Cause.** Manca un generatore AI + validatore.

**Componenti.** `domain/nutrition/MealPlanGenerator.kt`,
`domain/workout/WorkoutPlanBuilder.kt`, `CoachService.kt`.

**Correzione.** Nuovo `NutritionAiGenerator` (prompt + parsing JSON + validazione con
`NutritionDiaryEngine.targetIsCoherent`, budget e FoodFilter) con **fallback** al motore
deterministico. Idem per l'allenamento (selezione esercizi coerente con anamnesi).

## 6. Collaudo end-to-end (spec. §15)

**Problema.** L'ambiente **non dispone di emulatore né di KVM/virtualizzazione CPU**
(`/dev/kvm` assente, `/proc/cpuinfo` senza vmx/svm, SDK senza `emulator`/`system-images`).
Il collaudo su device/emulatore è quindi **impossibile qui**.

**Correzione.** Eseguire **test di integrazione JVM su Room in-memory** che riproducono
gli scenari A–H a livello di repository/motori (persistenza reale, sostituzioni, spesa,
diario, allenamento). Dichiarare esplicitamente i limiti. Non inventare screenshot.

---

## Altri rilievi minori

- **§11/§12** (registrazione serie, timer sessione/recupero): **già implementati** in
  `WorkoutViewModel`/`WorkoutScreen`; verificati. Da rendere il timer di recupero attivo
  anche a app in background (oggi è in-memory).
- **§7/§8/§9** (sostituzioni, spesa, diario): **già implementati** e testati; da collegare
  al budget e alle fonti esterne.
- **§10** ridondanza esercizi: `SplitGenerator.selectExercisesDetailed` filtra per
  attrezzatura/limitazioni; da rafforzare con penalizzazione esplicita delle ridondanze e
  motivazione biomeccanica per esercizio.
- **Sicurezza**: nessun invio di dati identificativi agli endpoint gratuiti è oggi
  bloccato → da aggiungere avviso in UI.

## Ordine di intervento

1. Gate anamnesi + intervista AI (§3) — prerequisito.
2. Robustezza parser AI (§14) — prerequisito per ogni funzione AI.
3. Budget mensile (§5).
4. Database alimentare progressivo (§6).
5. Generazione piani guidata dall'LLM (§4/§10) con fallback deterministico.
6. Collaudo: test unitari + integrazione Room; APK debug/release.
