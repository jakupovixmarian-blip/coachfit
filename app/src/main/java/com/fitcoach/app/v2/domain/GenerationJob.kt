package com.fitcoach.app.v2.domain

import java.util.UUID

enum class GenerationPhase(val order: Int) {
    NUTRITION(1),
    WORKOUT(2),
    CALENDAR(3),
    INDICATORS(4)
}

enum class GenerationPhaseStatus {
    PENDING,
    RUNNING,
    VALIDATED,
    FAILED
}

data class GenerationPhaseState(
    val phase: GenerationPhase,
    val status: GenerationPhaseStatus = GenerationPhaseStatus.PENDING,
    val attempt: Int = 0,
    val error: String? = null
)

data class GenerationJob(
    val id: String = UUID.randomUUID().toString(),
    val profileVersion: Long,
    val phases: List<GenerationPhaseState> = GenerationPhase.entries.map { GenerationPhaseState(it) },
    val committed: Boolean = false
) {
    val currentPhase: GenerationPhaseState?
        get() = phases.firstOrNull { it.status == GenerationPhaseStatus.RUNNING }
            ?: phases.firstOrNull { it.status == GenerationPhaseStatus.PENDING }

    val canCommit: Boolean
        get() = phases.all { it.status == GenerationPhaseStatus.VALIDATED } && !committed

    fun startNext(): GenerationJob {
        require(phases.none { it.status == GenerationPhaseStatus.RUNNING }) { "Una fase è già in esecuzione" }
        require(phases.none { it.status == GenerationPhaseStatus.FAILED }) { "Riprendi prima la fase fallita" }
        val next = phases.firstOrNull { it.status == GenerationPhaseStatus.PENDING }
            ?: error("Nessuna fase pendente")
        require(phases.filter { it.phase.order < next.phase.order }
            .all { it.status == GenerationPhaseStatus.VALIDATED }) { "Le fasi precedenti non sono validate" }
        return copy(phases = replace(next.copy(status = GenerationPhaseStatus.RUNNING, attempt = next.attempt + 1, error = null)))
    }

    fun markValidated(phase: GenerationPhase): GenerationJob {
        val state = phases.firstOrNull { it.phase == phase } ?: error("Fase sconosciuta")
        require(state.status == GenerationPhaseStatus.RUNNING) { "La fase ${phase.name} non è in esecuzione" }
        return copy(phases = replace(state.copy(status = GenerationPhaseStatus.VALIDATED)))
    }

    fun markFailed(phase: GenerationPhase, message: String): GenerationJob {
        require(message.isNotBlank()) { "Un errore deve avere una motivazione" }
        val state = phases.firstOrNull { it.phase == phase } ?: error("Fase sconosciuta")
        require(state.status == GenerationPhaseStatus.RUNNING) { "La fase ${phase.name} non è in esecuzione" }
        return copy(phases = replace(state.copy(status = GenerationPhaseStatus.FAILED, error = message)))
    }

    fun retry(phase: GenerationPhase): GenerationJob {
        val state = phases.firstOrNull { it.phase == phase } ?: error("Fase sconosciuta")
        require(state.status == GenerationPhaseStatus.FAILED) { "Solo una fase fallita può essere ripresa" }
        return copy(phases = replace(state.copy(status = GenerationPhaseStatus.PENDING, error = null)))
    }

    fun commit(): GenerationJob {
        check(canCommit) { "Il job non ha quattro artefatti validati" }
        return copy(committed = true)
    }

    private fun replace(value: GenerationPhaseState): List<GenerationPhaseState> =
        phases.map { if (it.phase == value.phase) value else it }
}
