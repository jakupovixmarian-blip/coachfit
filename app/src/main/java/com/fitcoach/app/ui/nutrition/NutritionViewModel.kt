package com.fitcoach.app.ui.nutrition

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.diagnostics.CrashLog
import com.fitcoach.app.data.repository.FoodRepository
import com.fitcoach.app.data.repository.FoodSearchRepository
import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.NutritionRepository
import com.fitcoach.app.data.repository.SubstitutionRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.FoodItem
import com.fitcoach.app.domain.model.Meal
import com.fitcoach.app.domain.model.MealEntry
import com.fitcoach.app.domain.model.NutritionDay
import com.fitcoach.app.domain.model.NutritionPlan
import com.fitcoach.app.domain.nutrition.FoodSubstitutionEngine
import com.fitcoach.app.domain.nutrition.NutritionDiaryEngine
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.usecase.ProgramGenerationResult
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NutritionUiState(
    val loading: Boolean = true,
    val plan: NutritionPlan? = null,
    // Si apre sul giorno di oggi: partire sempre da lunedi' faceva credere
    // all'utente che i target di oggi fossero quelli del lunedi'.
    val selectedDayIndex: Int =
        NutritionDiaryEngine.dayIndexFor(NutritionDiaryRepository.todayEpochDay()),
    val message: String? = null,
    val generating: Boolean = false,
    val epochDay: Long = NutritionDiaryRepository.todayEpochDay(),
    /** planEntryId -> id voce diario attiva (spunta). */
    val checks: Map<Long, Long> = emptyMap(),
    // Sostituzione vincolata
    val substitutionEntry: MealEntry? = null,
    val substitutes: List<FoodItem> = emptyList(),
    /** foodId -> compatibilita precalcolata, per mostrare quali varianti sono valide. */
    val substituteStatuses: Map<Long, FoodSubstitutionEngine.Status> = emptyMap(),
    val substituteEvaluating: Boolean = false,
    val preview: FoodSubstitutionEngine.Result? = null,
    val previewFood: FoodItem? = null,
    // Ricerca alimentare progressiva (§6)
    val foodSearchQuery: String = "",
    val foodSearchResults: List<FoodItem> = emptyList(),
    val foodSearchLoading: Boolean = false,
    val foodSearchNote: String? = null,
    // Modifica manuale del piano: pasto a cui aggiungere un alimento.
    val addFoodToMeal: Meal? = null,
    val addFoodGrams: String = "",
    val selectedFoodToAdd: FoodItem? = null,
    val editingEntry: MealEntry? = null,
    val editEntryGrams: String = "",
    val editingDay: NutritionDay? = null,
    val editDayKcal: String = "",
    val editDayProtein: String = "",
    val editDayCarbs: String = "",
    val editDayFat: String = "",
    val editingDayType: NutritionDay? = null,
    val editingMeal: Meal? = null,
    val editMealTime: String = "",
    // Distribuzione personalizzata delle giornate carb cycling (§5.2).
    val editingCarbDistribution: Boolean = false,
    val editHighDays: String = "",
    val editMediumDays: String = "",
    val editLowDays: String = "",
    val carbDistributionError: String? = null,
    // Piani salvati (AI e manuali): per scegliere quale avviare.
    val plans: List<NutritionPlan> = emptyList(),
    val editingPlan: NutritionPlan? = null,
    val deletingPlan: NutritionPlan? = null
) {
    val selectedDay: NutritionDay?
        get() = plan?.days?.firstOrNull { it.dayIndex == selectedDayIndex }

    /**
     * Data del diario a cui si riferisce il giorno mostrato. Il piano e' una
     * settimana-tipo: se l'utente apre il lunedi' mentre oggi e' sabato, la
     * spunta riguarda lunedi'. Prima questa data era sempre "oggi", e le voci
     * di un altro giorno finivano sul diario di oggi.
     */
    val selectedEpochDay: Long
        get() = NutritionDiaryEngine.epochDayForDayIndex(selectedDayIndex, epochDay)

    /** Vero se il giorno mostrato e' quello di oggi: solo allora si puo' mangiare. */
    val isSelectedDayToday: Boolean
        get() = selectedEpochDay == epochDay
}

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val nutritionRepository: NutritionRepository,
    private val foodRepository: FoodRepository,
    private val diaryRepository: NutritionDiaryRepository,
    private val substitutionRepository: SubstitutionRepository,
    private val foodSearchRepository: FoodSearchRepository,
    private val generationCoordinator: ProgramGenerationCoordinator,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NutritionUiState())
    val state: StateFlow<NutritionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            // La data va riletta: il ViewModel sopravvive alla navigazione e
            // un'app rimasta aperta oltre la mezzanotte resterebbe su ieri.
            val oggi = NutritionDiaryRepository.todayEpochDay()
            val plan = nutritionRepository.getActivePlan()
            _state.update { it.copy(
                    loading = false,
                    plan = plan,
                    epochDay = oggi,
                    selectedDayIndex = it.selectedDayIndex.coerceIn(0, 6)
            ) }
            // Lette dopo l'aggiornamento della data: selectedEpochDay dipende da epochDay.
            val checks = diaryRepository.planChecks(_state.value.selectedEpochDay)
                .mapValues { it.value.id }
            _state.update { it.copy(
                    checks = checks,
                    plans = nutritionRepository.getAllPlans()
            ) }
        }
    }

    fun selectDay(index: Int) {
        _state.update { it.copy( selectedDayIndex = index.coerceIn(0, 6) ) }
        // Le spunte appartengono a una data: cambiando giorno cambia il diario.
        viewModelScope.launch {
            val checks = diaryRepository.planChecks(_state.value.selectedEpochDay)
                .mapValues { it.value.id }
            _state.update { it.copy( checks = checks ) }
        }
    }

    fun toggleEntry(entry: MealEntry) {
        viewModelScope.launch {
            val day = _state.value.selectedDay ?: return@launch
            val meal = day.meals.firstOrNull { m -> m.entries.any { it.id == entry.id } } ?: return@launch
            val data = _state.value.selectedEpochDay
            diaryRepository.togglePlanEntry(entry, meal.slot, meal.time, data)
            val checks = diaryRepository.planChecks(data).mapValues { it.value.id }
            _state.update { it.copy( checks = checks ) }
        }
    }

    fun generatePlan() {
        viewModelScope.launch {
            val profile = userRepository.get()
            if (profile == null || !profile.isComplete) {
                _state.update { it.copy( message = "Completa prima il profilo." ) }
                return@launch
            }
            _state.update { it.copy( generating = true, message = null ) }
            // La generazione tocca rete, database e motori matematici: un errore
            // qui non deve chiudere l'app. Viene registrato e mostrato, cosi
            // l'utente vede cosa e successo invece di un crash.
            val message = try {
                when (val result = generationCoordinator.generate(
                    profile = profile,
                    scope = ProgramGenerationCoordinator.Scope.NUTRITION
                )) {
                    is ProgramGenerationResult.Success -> "Piano alimentare generato dal Coach AI."
                    is ProgramGenerationResult.MissingData ->
                        "Dati mancanti: ${result.missing.joinToString()}."
                    is ProgramGenerationResult.AnamnesisNotConfirmed ->
                        "Completa l'anamnesi guidata prima di generare."
                    is ProgramGenerationResult.Error -> result.message
                }
            } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
                CrashLog.record(context, t, "Generazione piano alimentare")
                "Generazione non riuscita: ${t.message ?: t::class.simpleName}. " +
                    "Dettagli in Impostazioni > Diagnostica."
            }
            val plan = runCatching { nutritionRepository.getActivePlan() }.getOrNull()
            _state.update { it.copy( generating = false, plan = plan ?: it.plan, message = message ) }
            runCatching { refreshPlanList() }
        }
    }

    /**
     * Crea un piano alimentare MANUALE: struttura a 7 giorni con carb cycling e
     * target calcolati dai motori deterministici (formule documentate). I pasti
     * restano vuoti: l'utente li compila a mano dalla schermata. Non vengono
     * inventati alimenti ne valori.
     */
    fun createManualPlan() {
        viewModelScope.launch {
            val profile = userRepository.get()
            if (profile == null || !profile.isComplete) {
                _state.update { it.copy( message = "Completa prima il profilo (età, altezza, peso)." ) }
                return@launch
            }
            val energy = com.fitcoach.app.domain.nutrition.EnergyCalculator.compute(profile)
            if (energy == null) {
                _state.update { it.copy(
                        message = "Servono peso, altezza, età e sesso per stimare il fabbisogno."
                ) }
                return@launch
            }
            val cycling = com.fitcoach.app.domain.nutrition.CarbCyclingEngine.build(profile, energy)
            val dayTypes = com.fitcoach.app.domain.nutrition.CarbCyclingEngine.assignDayTypes(
                highDays = cycling.highDays,
                mediumDays = cycling.mediumDays,
                lowDays = cycling.lowDays,
                trainingDayIndices = trainingDayIndices(profile)
            )
            nutritionRepository.createManualPlan(
                profile = profile,
                energy = energy,
                cycling = cycling,
                dayTypes = dayTypes,
                trainingDayIndices = trainingDayIndices(profile)
            )
            val plan = nutritionRepository.getActivePlan()
            _state.update { it.copy(
                    plan = plan,
                    selectedDayIndex = NutritionDiaryEngine.dayIndexFor(
                        NutritionDiaryRepository.todayEpochDay()
                    ),
                    message = "Piano manuale creato. Aggiungi gli alimenti ai pasti."
            ) }
            refreshPlanList()
        }
    }

    /** Riporta i target del giorno selezionato a quelli calcolati (stima) — nessun valore inventato. */
    private fun trainingDayIndices(profile: com.fitcoach.app.domain.model.UserProfile): List<Int> =
        (0 until profile.trainingDaysPerWeek.coerceIn(0, 7)).toList()

    /** Rende attivo un piano esistente (avvio del piano AI o di quello manuale). */
    fun activatePlan(planId: Long) {
        viewModelScope.launch {
            nutritionRepository.setActivePlan(planId)
            val plan = nutritionRepository.getActivePlan()
            _state.update { it.copy(
                    plan = plan,
                    selectedDayIndex = NutritionDiaryEngine.dayIndexFor(
                        NutritionDiaryRepository.todayEpochDay()
                    ),
                    message = "Piano attivato."
            ) }
            refreshPlanList()
        }
    }

    fun openRenamePlan(plan: NutritionPlan) {
        _state.update { it.copy( editingPlan = plan ) }
    }

    fun closeRenamePlan() {
        _state.update { it.copy( editingPlan = null ) }
    }

    fun renamePlan(name: String) {
        val plan = _state.value.editingPlan ?: return
        viewModelScope.launch {
            if (nutritionRepository.renamePlan(plan.id, name)) {
                _state.update { it.copy( editingPlan = null, message = "Piano rinominato." ) }
                load()
            } else {
                _state.update { it.copy( message = "Inserisci un nome valido." ) }
            }
        }
    }

    fun askDeletePlan(plan: NutritionPlan) {
        _state.update { it.copy( deletingPlan = plan ) }
    }

    fun closeDeletePlan() {
        _state.update { it.copy( deletingPlan = null ) }
    }

    fun deletePlan() {
        val plan = _state.value.deletingPlan ?: return
        viewModelScope.launch {
            nutritionRepository.deletePlan(plan.id)
            _state.update { it.copy( deletingPlan = null, message = "Piano eliminato." ) }
            load()
        }
    }


    fun duplicatePlan(plan: NutritionPlan) {
        viewModelScope.launch {
            val newId = nutritionRepository.duplicatePlan(plan.id)
            if (newId == null) {
                _state.update { it.copy( message = "Impossibile duplicare il piano." ) }
                return@launch
            }
            val active = nutritionRepository.getActivePlan()
            _state.update { it.copy(
                    plan = active,
                    selectedDayIndex = NutritionDiaryEngine.dayIndexFor(
                        NutritionDiaryRepository.todayEpochDay()
                    ),
                    message = "Copia creata e attivata."
            ) }
            refreshPlanList()
        }
    }

    private suspend fun refreshPlanList() {
        _state.update { it.copy( plans = nutritionRepository.getAllPlans() ) }
    }

    fun openEditEntry(entry: MealEntry) {
        _state.update { it.copy(
                editingEntry = entry,
                editEntryGrams = if (entry.grams % 1.0 == 0.0) entry.grams.toInt().toString() else entry.grams.toString()
        ) }
    }

    fun closeEditEntry() {
        _state.update { it.copy( editingEntry = null, editEntryGrams = "" ) }
    }

    fun updateEditEntryGrams(value: String) {
        _state.update { it.copy( editEntryGrams = value ) }
    }

    fun saveEditEntry() {
        val entry = _state.value.editingEntry ?: return
        val grams = _state.value.editEntryGrams.replace(',', '.').toDoubleOrNull()
        if (grams == null || grams <= 0.0 || grams > 10_000.0) {
            _state.update { it.copy( message = "Inserisci una quantita valida (0-10000 g)." ) }
            return
        }
        viewModelScope.launch {
            val food = foodRepository.getAll().firstOrNull { it.id == entry.foodItemId }
            val factor = grams / entry.grams.coerceAtLeast(0.001)
            val planId = _state.value.plan?.id
            val updated = planId != null && nutritionRepository.updateEntryAsNewPlanVersion(
                planId = planId,
                entryId = entry.id,
                grams = grams,
                kcal = food?.kcalPer100g?.times(grams / 100.0) ?: entry.kcal * factor,
                protein = food?.proteinPer100g?.times(grams / 100.0) ?: entry.protein * factor,
                carbs = food?.carbsPer100g?.times(grams / 100.0) ?: entry.carbs * factor,
                fat = food?.fatPer100g?.times(grams / 100.0) ?: entry.fat * factor,
                diaryEpochDay = _state.value.epochDay
            )
            if (!updated) {
                _state.update { it.copy( message = "Impossibile creare la nuova versione del piano." ) }
                return@launch
            }
            _state.update { it.copy(
                    editingEntry = null,
                    editEntryGrams = "",
                    message = "Quantita aggiornata: ${grams.toInt()} g."
            ) }
            load()
        }
    }

    fun openEditDay(day: NutritionDay) {
        _state.update { it.copy(
                editingDay = day,
                editDayKcal = day.targetKcal.toString(),
                editDayProtein = day.targetProtein.toString(),
                editDayCarbs = day.targetCarbs.toString(),
                editDayFat = day.targetFat.toString()
        ) }
    }

    fun closeEditDay() { _state.update { it.copy(editingDay = null) } }

    fun updateDayTargetFields(kcal: String, protein: String, carbs: String, fat: String) {
        _state.update { it.copy(
                editDayKcal = kcal, editDayProtein = protein, editDayCarbs = carbs, editDayFat = fat
        ) }
    }

    /**
     * Apre l'editor della distribuzione HIGH/MEDIUM/LOW precompilando i valori
     * attualmente in uso, cosi' l'utente parte da una terna coerente.
     */
    fun openCarbDistributionEditor() {
        val plan = _state.value.plan ?: return
        _state.update {
            it.copy(
                editingCarbDistribution = true,
                editHighDays = plan.highCarbDays.toString(),
                editMediumDays = plan.mediumCarbDays.toString(),
                editLowDays = plan.lowCarbDays.toString(),
                carbDistributionError = null
            )
        }
    }

    fun closeCarbDistributionEditor() {
        _state.update { it.copy(editingCarbDistribution = false, carbDistributionError = null) }
    }

    fun updateCarbDistributionFields(high: String, medium: String, low: String) {
        _state.update {
            it.copy(
                editHighDays = high,
                editMediumDays = medium,
                editLowDays = low,
                carbDistributionError = null
            )
        }
    }

    /**
     * Salva la terna scelta. La validazione e' qui e non solo nel motore:
     * l'utente deve sapere subito perche' un valore viene rifiutato, invece di
     * vedere il piano invariato senza spiegazione.
     */
    fun saveCarbDistribution() {
        val high = _state.value.editHighDays.trim().toIntOrNull()
        val medium = _state.value.editMediumDays.trim().toIntOrNull()
        val low = _state.value.editLowDays.trim().toIntOrNull()
        if (high == null || medium == null || low == null) {
            _state.update { it.copy(carbDistributionError = "Inserisci tre numeri interi.") }
            return
        }
        if (high < 0 || medium < 0 || low < 0) {
            _state.update { it.copy(carbDistributionError = "I valori non possono essere negativi.") }
            return
        }
        if (high + medium + low != 7) {
            _state.update {
                it.copy(
                    carbDistributionError =
                    "La somma deve essere 7 giorni (ora è ${high + medium + low})."
                )
            }
            return
        }
        viewModelScope.launch {
            val profile = userRepository.get()
            if (profile == null) {
                _state.update { it.copy(carbDistributionError = "Profilo non disponibile.") }
                return@launch
            }
            val updated = profile.copy(
                carbHighDaysOverride = high,
                carbMediumDaysOverride = medium,
                carbLowDaysOverride = low
            )
            userRepository.save(updated)
            val message = applyCarbCycling(updated)
            _state.update {
                it.copy(
                    editingCarbDistribution = false,
                    carbDistributionError = null,
                    message = message
                )
            }
        }
    }

    /**
     * Applica subito la distribuzione al piano attivo con i motori
     * deterministici. Se il piano non esiste o mancano i dati energetici,
     * l'impostazione resta salvata e verra' usata alla prossima generazione.
     */
    private suspend fun applyCarbCycling(
        profile: com.fitcoach.app.domain.model.UserProfile
    ): String {
        val plan = _state.value.plan ?: return "Distribuzione salvata. Verra' applicata al prossimo piano."
        val energy = com.fitcoach.app.domain.nutrition.EnergyCalculator.compute(profile)
            ?: return "Distribuzione salvata. Verra' applicata al prossimo piano."
        val cycling = com.fitcoach.app.domain.nutrition.CarbCyclingEngine.build(profile, energy)
        val dayTypes = com.fitcoach.app.domain.nutrition.CarbCyclingEngine.assignDayTypes(
            highDays = cycling.highDays,
            mediumDays = cycling.mediumDays,
            lowDays = cycling.lowDays,
            trainingDayIndices = trainingDayIndices(profile)
        )
        val ok = nutritionRepository.applyCarbCyclingAsNewPlanVersion(
            planId = plan.id,
            cycling = cycling,
            dayTypes = dayTypes,
            tdee = energy.tdee,
            diaryEpochDay = NutritionDiaryRepository.todayEpochDay()
        )
        if (!ok) return "Distribuzione salvata. Verra' applicata al prossimo piano."
        load()
        return "Distribuzione applicata al piano."
    }

    /** Ripristina la distribuzione automatica derivata dai giorni di allenamento. */
    fun resetCarbDistribution() {
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            val updated = profile.copy(
                carbHighDaysOverride = null,
                carbMediumDaysOverride = null,
                carbLowDaysOverride = null
            )
            userRepository.save(updated)
            val message = applyCarbCycling(updated)
            _state.update {
                it.copy(
                    editingCarbDistribution = false,
                    carbDistributionError = null,
                    message = message
                )
            }
        }
    }

    fun saveEditDay() {
        val day = _state.value.editingDay ?: return
        val kcal = _state.value.editDayKcal.replace(',', '.').toDoubleOrNull()
        val protein = _state.value.editDayProtein.replace(',', '.').toDoubleOrNull()
        val carbs = _state.value.editDayCarbs.replace(',', '.').toDoubleOrNull()
        val fat = _state.value.editDayFat.replace(',', '.').toDoubleOrNull()
        val planId = _state.value.plan?.id
        if (planId == null || kcal == null || protein == null || carbs == null || fat == null) {
            _state.update { it.copy( message = "Inserisci tutti i target numerici." ) }
            return
        }
        viewModelScope.launch {
            val ok = nutritionRepository.updateDayTargetsAsNewPlanVersion(
                planId, day.id, kcal, protein, carbs, fat, _state.value.epochDay
            )
            if (!ok) {
                _state.update { it.copy( message = "Target non validi o piano non disponibile." ) }
                return@launch
            }
            _state.update { it.copy( editingDay = null, message = "Target della giornata aggiornati." ) }
            load()
        }
    }

    fun openEditDayType(day: NutritionDay) { _state.update { it.copy(editingDayType = day) } }
    fun closeEditDayType() { _state.update { it.copy(editingDayType = null) } }

    fun saveEditDayType(type: com.fitcoach.app.domain.model.CarbDayType) {
        val day = _state.value.editingDayType ?: return
        val planId = _state.value.plan?.id
        if (planId == null) return
        viewModelScope.launch {
            val ok = nutritionRepository.updateDayTypeAsNewPlanVersion(
                planId, day.id, type, _state.value.epochDay
            )
            if (ok) {
                _state.update { it.copy( editingDayType = null, message = "Tipo giornata aggiornato." ) }
                load()
            } else {
                _state.update { it.copy( message = "Impossibile aggiornare il tipo giornata." ) }
            }
        }
    }

    fun openEditMeal(meal: Meal) {
        _state.update { it.copy( editingMeal = meal, editMealTime = meal.time ) }
    }

    fun closeEditMeal() { _state.update { it.copy(editingMeal = null, editMealTime = "") } }

    fun updateEditMealTime(value: String) { _state.update { it.copy(editMealTime = value) } }

    fun saveEditMeal() {
        val meal = _state.value.editingMeal ?: return
        val planId = _state.value.plan?.id
        if (planId == null) {
            _state.update { it.copy( message = "Piano non disponibile." ) }
            return
        }
        viewModelScope.launch {
            val ok = nutritionRepository.updateMealScheduleAsNewPlanVersion(
                planId, meal.id, meal.slot, _state.value.editMealTime, _state.value.epochDay
            )
            if (!ok) {
                _state.update { it.copy( message = "Orario non valido: usa HH:mm." ) }
                return@launch
            }
            _state.update { it.copy( editingMeal = null, message = "Orario pasto aggiornato." ) }
            load()
        }
    }

    fun startSubstitution(entry: MealEntry) {
        viewModelScope.launch {
            val profile = userRepository.get() ?: return@launch
            val all = foodRepository.getAll()
            val original = all.firstOrNull { it.id == entry.foodItemId }
                ?: all.firstOrNull { it.name == entry.foodName }
            val subs = if (original != null) {
                substitutionRepository.candidates(original, profile)
            } else {
                all.take(8)
            }
            _state.update { it.copy(
                    substitutionEntry = entry,
                    substitutes = subs,
                    substituteStatuses = emptyMap(),
                    substituteEvaluating = subs.isNotEmpty(),
                    preview = null,
                    previewFood = null
            ) }
            evaluateSubstitutes(entry, subs)
        }
    }

    /**
     * Precalcola la compatibilita di ogni variante, cosi il dialogo mostra subito
     * quali funzionano (invece di far scoprire "non idonea" solo al tocco) e le
     * ordina mettendo per prime quelle applicabili.
     */
    private suspend fun evaluateSubstitutes(entry: MealEntry, candidates: List<FoodItem>) {
        val dayIndex = _state.value.selectedDayIndex
        val statuses = linkedMapOf<Long, FoodSubstitutionEngine.Status>()
        for (food in candidates) {
            val result = runCatching {
                substitutionRepository.preview(entry.id, dayIndex, food)
            }.getOrNull()
            if (result != null) statuses[food.id] = result.status
        }
        val ordered = candidates.sortedWith(
            compareBy(
                { statuses[it.id] == FoodSubstitutionEngine.Status.INCOMPATIBLE },
                { statuses[it.id]?.ordinal ?: Int.MAX_VALUE }
            )
        )
        // Aggiorna solo se il dialogo e ancora aperto sulla stessa voce.
        if (_state.value.substitutionEntry?.id == entry.id) {
            _state.update { it.copy(
                    substitutes = ordered,
                    substituteStatuses = statuses,
                    substituteEvaluating = false
            ) }
        }
    }

    fun cancelSubstitution() {
        _state.update { it.copy(
                substitutionEntry = null, substitutes = emptyList(), preview = null, previewFood = null,
                substituteStatuses = emptyMap(), substituteEvaluating = false,
                foodSearchQuery = "", foodSearchResults = emptyList(), foodSearchNote = null
        ) }
    }

    fun updateFoodSearch(query: String) {
        _state.update { it.copy( foodSearchQuery = query, foodSearchNote = null ) }
    }

    /**
     * Ricerca progressiva (§6): prima nel database locale, poi (se non ci sono
     * risultati) nelle fonti esterne (Open Food Facts, USDA). I risultati trovati
     * vengono salvati localmente per gli usi futuri.
     */
    fun searchFoods() {
        val query = _state.value.foodSearchQuery.trim()
        if (query.length < 2) {
            _state.update { it.copy(
                    foodSearchResults = emptyList(),
                    foodSearchNote = "Digita almeno 2 caratteri."
            ) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy( foodSearchLoading = true, foodSearchNote = null ) }
            val outcome = runCatching { foodSearchRepository.search(query) }.getOrNull()
            _state.update { it.copy(
                    foodSearchLoading = false,
                    foodSearchResults = outcome?.results.orEmpty(),
                    foodSearchNote = outcome?.note
                        ?: if (outcome == null) "Ricerca non disponibile: controlla la connessione." else null
            ) }
        }
    }

    /** Verifica un alimento trovato con la ricerca come sostituto della voce corrente. */
    fun previewSubstituteFromSearch(food: FoodItem) = previewSubstitution(food)

    /** Calcola il controllo preventivo per il sostituto scelto, senza applicare nulla. */
    fun previewSubstitution(substitute: FoodItem) {
        viewModelScope.launch {
            val entry = _state.value.substitutionEntry ?: return@launch
            val result = substitutionRepository.preview(entry.id, _state.value.selectedDayIndex, substitute)
            _state.update { it.copy( preview = result, previewFood = substitute ) }
        }
    }

    /** Applica la sostituzione solo se compatibile o compatibile con aggiustamento. */
    fun applySubstitution() {
        viewModelScope.launch {
            val entry = _state.value.substitutionEntry ?: return@launch
            val substitute = _state.value.previewFood ?: return@launch
            val result = _state.value.preview ?: return@launch
            if (result.status == FoodSubstitutionEngine.Status.INCOMPATIBLE) {
                _state.update { it.copy( message = "Sostituzione non compatibile con i vincoli del piano." ) }
                return@launch
            }
            runCatching {
                substitutionRepository.apply(
                    entryId = entry.id,
                    dayIndex = _state.value.selectedDayIndex,
                    epochDay = _state.value.selectedEpochDay,
                    substitute = substitute,
                    result = result
                )
            }.onFailure {
                _state.update { it.copy( message = "Errore nell'applicazione: ${it.message}" ) }
                return@launch
            }
            _state.update { it.copy(
                    substitutionEntry = null,
                    substitutes = emptyList(),
                    preview = null,
                    previewFood = null,
                    message = "Sostituzione applicata: ${substitute.name} (${result.substituteGrams.toInt()} g)."
            ) }
            load()
        }
    }

    fun confirmMeal(mealId: Long, confirmed: Boolean) {
        viewModelScope.launch {
            nutritionRepository.confirmMeal(mealId, confirmed)
            _state.update { it.copy(
                    message = if (confirmed) "Pasto confermato." else "Conferma rimossa."
            ) }
            load()
        }
    }

    fun consumeMessage() {
        _state.update { it.copy( message = null ) }
    }

    /** Apre il riquadro per aggiungere manualmente un alimento a un pasto. */
    fun openAddFood(meal: Meal) {
        _state.update { it.copy(
                addFoodToMeal = meal, addFoodGrams = "", selectedFoodToAdd = null,
                foodSearchQuery = "", foodSearchResults = emptyList(), foodSearchNote = null
        ) }
    }

    fun closeAddFood() {
        _state.update { it.copy( addFoodToMeal = null, selectedFoodToAdd = null, addFoodGrams = "" ) }
    }

    fun updateAddFoodGrams(value: String) {
        _state.update { it.copy( addFoodGrams = value.filter { c -> c.isDigit() || c == ',' || c == '.' } ) }
    }

    fun selectFoodToAdd(food: FoodItem) {
        _state.update { it.copy( selectedFoodToAdd = food ) }
    }

    /** Conferma l'aggiunta manuale dell'alimento scelto con i grammi indicati. */
    fun confirmAddFood() {
        viewModelScope.launch {
            val meal = _state.value.addFoodToMeal ?: return@launch
            val food = _state.value.selectedFoodToAdd
            if (food == null) {
                _state.update { it.copy( message = "Seleziona un alimento da aggiungere." ) }
                return@launch
            }
            val grams = _state.value.addFoodGrams.replace(',', '.').toDoubleOrNull()
            if (grams == null || grams <= 0 || grams > 3000) {
                _state.update { it.copy( message = "Inserisci una quantita valida in grammi." ) }
                return@launch
            }
            nutritionRepository.addEntryToMeal(meal.id, food, grams, food.defaultUnit)
            _state.update { it.copy(
                    addFoodToMeal = null, selectedFoodToAdd = null, addFoodGrams = "",
                    message = "${food.name} aggiunto a ${meal.slot.label}."
            ) }
            load()
        }
    }

    /** Rimuove manualmente un alimento dal piano. */
    fun removeEntry(entry: MealEntry) {
        viewModelScope.launch {
            nutritionRepository.removeEntry(entry)
            _state.update { it.copy( message = "${entry.foodName} rimosso dal piano." ) }
            load()
        }
    }
}