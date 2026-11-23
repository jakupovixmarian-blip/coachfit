package com.fitcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.domain.usecase.ProgramGenerationResult
import com.fitcoach.app.domain.usecase.ProgramGenerationCoordinator
import com.fitcoach.app.domain.workout.WorkoutPlanBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProfileEditUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val weightKg: String = "",
    val bodyFat: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val regenerate: Boolean = true
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository,
    private val generationCoordinator: ProgramGenerationCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val p = userRepository.get()
            _state.value = ProfileEditUiState(
                loading = false,
                profile = p,
                weightKg = p?.weightKg?.let { "%.1f".format(it) }.orEmpty(),
                bodyFat = p?.bodyFatPercent?.let { "%.1f".format(it) }.orEmpty()
            )
        }
    }

    fun update(transform: (UserProfile) -> UserProfile) {
        val p = _state.value.profile ?: return
        _state.value = _state.value.copy(profile = transform(p))
    }

    fun updateWeight(v: String) { _state.value = _state.value.copy(weightKg = v) }
    fun updateBodyFat(v: String) { _state.value = _state.value.copy(bodyFat = v) }
    fun setRegenerate(v: Boolean) { _state.value = _state.value.copy(regenerate = v) }
    fun setGoal(g: Goal) = update { it.copy(goal = g, answeredQuestionIds = it.answeredQuestionIds + "goal") }
    fun setExperience(e: TrainingExperience) = update { it.copy(experience = e, answeredQuestionIds = it.answeredQuestionIds + "experience") }
    fun setSex(s: Sex) = update { it.copy(sex = s, answeredQuestionIds = it.answeredQuestionIds + "sex") }
    fun setTrainingDays(d: Int) = update { it.copy(trainingDaysPerWeek = d, answeredQuestionIds = it.answeredQuestionIds + "trainingDaysPerWeek") }
    fun toggleEquipment(e: Equipment) = update {
        it.copy(
            availableEquipment = if (e in it.availableEquipment) it.availableEquipment - e
            else it.availableEquipment + e,
            answeredQuestionIds = it.answeredQuestionIds + "availableEquipment"
        )
    }

    // ---- Inventario attrezzatura dettagliato (spec §4) ----

    /** Aggiunge o rimuove un preset del catalogo, mantenendo i dati gia' inseriti. */
    fun toggleGearPreset(name: String) = update { p ->
        val inv = p.gearInventory
        val existing = inv.presets.firstOrNull { it.name.equals(name, ignoreCase = true) }
        val presets = if (existing != null) {
            inv.presets - existing
        } else {
            val preset = com.fitcoach.app.domain.model.GearCatalog.find(name)
            inv.presets + com.fitcoach.app.domain.model.GearItem(
                id = name,
                name = name,
                category = preset?.category ?: com.fitcoach.app.domain.model.GearCategory.CUSTOM,
                minWeightKg = preset?.minWeightKg,
                maxWeightKg = preset?.maxWeightKg,
                incrementsKg = preset?.incrementsKg.orEmpty()
            )
        }
        p.copy(gearInventory = inv.copy(presets = presets))
    }

    /** Aggiorna i dettagli di un attrezzo gia' presente (preset o custom). */
    fun updateGearItem(item: com.fitcoach.app.domain.model.GearItem) = update { p ->
        val inv = p.gearInventory
        p.copy(
            gearInventory = inv.copy(
                presets = inv.presets.map { if (it.id == item.id) item else it },
                custom = inv.custom.map { if (it.id == item.id) item else it }
            )
        )
    }

    fun addCustomGear(name: String) = update { p ->
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@update p
        val inv = p.gearInventory
        if (inv.all.any { it.name.equals(trimmed, ignoreCase = true) }) return@update p
        p.copy(
            gearInventory = inv.copy(
                custom = inv.custom + com.fitcoach.app.domain.model.GearItem(
                    id = "custom:$trimmed",
                    name = trimmed,
                    category = com.fitcoach.app.domain.model.GearCategory.CUSTOM
                )
            )
        )
    }

    fun removeGearItem(id: String) = update { p ->
        val inv = p.gearInventory
        p.copy(
            gearInventory = inv.copy(
                presets = inv.presets.filterNot { it.id == id },
                custom = inv.custom.filterNot { it.id == id }
            )
        )
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value.profile ?: return@launch
            // Gli stessi limiti dell'onboarding: una modifica successiva non deve
            // poter inserire misure che il primo inserimento rifiuterebbe.
            val grezzo = _state.value.weightKg.replace(',', '.').toDoubleOrNull()
            val newWeight = grezzo?.takeIf { it in 30.0..300.0 }
            if (grezzo != null && newWeight == null) {
                _state.value = _state.value.copy(
                    message = "Peso fuori intervallo (30-300 kg): non salvato."
                )
                return@launch
            }
            val newBf = _state.value.bodyFat.replace(',', '.').toDoubleOrNull()?.takeIf { it in 1.0..70.0 }
            if (current.trainingDaysPerWeek !in 2..6) {
                _state.value = _state.value.copy(
                    message = "Servono da 2 a 6 giorni di allenamento a settimana."
                )
                return@launch
            }
            val updated = current.copy(
                weightKg = newWeight ?: current.weightKg,
                bodyFatPercent = newBf ?: current.bodyFatPercent,
                // Una misura inserita qui e una risposta dell'utente: senza
                // registrarla resterebbe "non dichiarata" per il generatore.
                answeredQuestionIds = current.answeredQuestionIds +
                    listOfNotNull("weightKg".takeIf { newWeight != null }) +
                    listOfNotNull("bodyFatPercent".takeIf { newBf != null }),
                updatedAt = System.currentTimeMillis()
            )
            _state.value = _state.value.copy(saving = true)
            userRepository.save(updated)

            if (newWeight != null && newWeight != current.weightKg) {
                progressRepository.addWeight(
                    WeightRecord(dateEpochDay = LocalDate.now().toEpochDay(), weightKg = newWeight)
                )
            }

            val message = if (_state.value.regenerate) {
                when (val result = generationCoordinator.generate(
                    profile = updated,
                    scope = ProgramGenerationCoordinator.Scope.BOTH
                )) {
                    is ProgramGenerationResult.Success -> "Profilo aggiornato e programmi rigenerati."
                    is ProgramGenerationResult.MissingData ->
                        "Profilo salvato, ma dati mancanti: ${result.missing.joinToString()}."
                    is ProgramGenerationResult.AnamnesisNotConfirmed ->
                        "Profilo salvato. Completa l'anamnesi per rigenerare i programmi."
                    is ProgramGenerationResult.Error -> "Profilo salvato. ${result.message}"
                }
            } else {
                "Profilo aggiornato."
            }
            _state.value = _state.value.copy(saving = false, profile = updated, message = message)
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
