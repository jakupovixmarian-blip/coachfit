package com.fitcoach.app.domain.usecase

import com.fitcoach.app.domain.adaptive.WeeklyCheckInAnalyzer
import com.fitcoach.app.domain.coach.WeeklyCheckInService
import com.fitcoach.app.domain.model.CheckIn
import com.fitcoach.app.domain.model.ProgramChange
import javax.inject.Inject

/**
 * Facade del check-in settimanale per la UI.
 *
 * NON contiene un secondo decisore: delega interamente a [WeeklyCheckInService],
 * che aggrega i dati reali e fa girare l'UNICA autorita decisionale
 * ([com.fitcoach.app.domain.coach.CoachOrchestrator]). Le proposte tornano in
 * stato PROPOSED e vengono applicate solo dopo la conferma dell'utente.
 */
class RunWeeklyCheckInUseCase @Inject constructor(
    private val service: WeeklyCheckInService
) {
    data class Outcome(
        val checkIn: CheckIn,
        val report: WeeklyCheckInAnalyzer.Report,
        val evaluation: String,
        val proposals: List<ProgramChange>,
        val notes: List<String>,
        val providerError: String?
    )

    suspend operator fun invoke(): Outcome? {
        return when (val outcome = service()) {
            is WeeklyCheckInService.Outcome.MissingData -> null
            is WeeklyCheckInService.Outcome.Success -> {
                val notes = buildList {
                    if (outcome.providerError != null) add(outcome.providerError)
                    when {
                        outcome.proposals.isNotEmpty() ->
                            add("${outcome.proposals.size} proposta/e di modifica da confermare o rifiutare.")
                        outcome.providerError == null ->
                            add("Nessuna modifica proposta: il programma attuale e adeguato ai dati.")
                    }
                    if (!outcome.report.dataSufficient) {
                        add("Dati ancora insufficienti per decisioni affidabili: continua a registrare peso e pasti.")
                    }
                }
                Outcome(
                    checkIn = outcome.checkIn,
                    report = outcome.report,
                    evaluation = outcome.evaluation,
                    proposals = outcome.proposals,
                    notes = notes,
                    providerError = outcome.providerError
                )
            }
        }
    }
}
