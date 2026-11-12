package dev.fitcoach.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fitcoach.app.core.util.DateUtils
import dev.fitcoach.app.domain.model.Routine
import dev.fitcoach.app.domain.model.SessionStatus
import dev.fitcoach.app.domain.model.WorkoutSession
import dev.fitcoach.app.domain.repository.RoutineRepository
import dev.fitcoach.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = DateUtils.today(),
    val sessions: List<WorkoutSession> = emptyList(),
    val activeRoutine: Routine? = null,
    val message: String? = null
) {
    fun sessionsOn(date: LocalDate): List<WorkoutSession> =
        sessions.filter { it.dateEpochDay == date.toEpochDay() }

    val selectedSessions: List<WorkoutSession> get() = sessionsOn(selectedDate)

    /** Days of the current month grid, padded to whole weeks starting on Monday. */
    fun gridDays(): List<LocalDate> {
        val start = month.atDay(1).with(DayOfWeek.MONDAY)
        return (0..41).map { start.plusDays(it.toLong()) }
    }
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        reload()
        viewModelScope.launch {
            routineRepository.observeActive().collect { routine ->
                _state.update { it.copy(activeRoutine = routine) }
            }
        }
    }

    private fun reload() {
        viewModelScope.launch {
            val month = _state.value.month
            val from = month.atDay(1).with(DayOfWeek.MONDAY).toEpochDay()
            val to = from + 41
            val sessions = sessionRepository.getAll().filter { it.dateEpochDay in from..to }
            _state.update { it.copy(sessions = sessions) }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun previousMonth() {
        _state.update { it.copy(month = it.month.minusMonths(1)) }
        reload()
    }

    fun nextMonth() {
        _state.update { it.copy(month = it.month.plusMonths(1)) }
        reload()
    }

    fun selectDate(date: LocalDate) = _state.update { it.copy(selectedDate = date) }

    /** Adds an extra session on the selected date. */
    fun addExtraSession() {
        viewModelScope.launch {
            val date = _state.value.selectedDate
            val routine = routineRepository.getActive()
            sessionRepository.createSession(
                WorkoutSession(
                    routineId = routine?.id,
                    name = routine?.name ?: "Seduta extra",
                    dateEpochDay = date.toEpochDay(),
                    startedAt = DateUtils.nowMillis(),
                    status = SessionStatus.PLANNED
                )
            )
            _state.update { it.copy(message = "Seduta aggiunta il ${DateUtils.format(date)}") }
            reload()
        }
    }

    /**
     * Moves a session to another date. Completed sessions are historical records
     * and are never rescheduled.
     */
    fun moveSession(session: WorkoutSession, target: LocalDate) {
        viewModelScope.launch {
            if (session.status == SessionStatus.COMPLETED) {
                _state.update { it.copy(message = "Una seduta completata non puo essere spostata") }
                return@launch
            }
            sessionRepository.updateSession(session.copy(dateEpochDay = target.toEpochDay()))
            _state.update { it.copy(message = "Seduta spostata al ${DateUtils.format(target)}") }
            reload()
        }
    }

    fun skipSession(session: WorkoutSession) {
        viewModelScope.launch {
            if (session.status == SessionStatus.COMPLETED) {
                _state.update { it.copy(message = "Una seduta completata non puo essere saltata") }
                return@launch
            }
            sessionRepository.updateSession(session.copy(status = SessionStatus.SKIPPED))
            reload()
        }
    }

    fun restoreSession(session: WorkoutSession) {
        viewModelScope.launch {
            sessionRepository.updateSession(session.copy(status = SessionStatus.PLANNED))
            reload()
        }
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            if (session.status == SessionStatus.COMPLETED) {
                _state.update { it.copy(message = "Elimina prima i dati registrati dalla seduta completata") }
                return@launch
            }
            sessionRepository.deleteSession(session.id)
            reload()
        }
    }
}
