package com.fitcoach.app.ui.screens.onboarding

/**
 * Stato dell'Onboarding iniziale.
 *
 * Non contiene dati di allenamento o alimentazione: serve solo a introdurre il
 * prodotto e a portare l'utente all'Anamnesi, che è l'unico punto in cui nasce
 * il profilo. Nessun seed, nessun profilo precompilato.
 */
data class OnboardingUiState(
    val page: Int = 0,
    val isRestoring: Boolean = false,
    val errorMessage: String? = null,
) {
    val pages: List<OnboardingPage> = defaultPages

    val current: OnboardingPage get() = pages[page.coerceIn(0, pages.lastIndex)]

    val isLastPage: Boolean get() = page >= pages.lastIndex

    val progress: Float get() = if (pages.isEmpty()) 0f else (page + 1f) / pages.size

    companion object {
        val defaultPages: List<OnboardingPage> = listOf(
            OnboardingPage(
                eyebrow = "COACH DIGITALE",
                title = "Il programma lo costruisce il sistema",
                body = "Inserisci profilo, obiettivi e risultati degli allenamenti. FitCoach calcola calorie, macro, ciclicità dei carboidrati e periodizzazione.",
                highlight = "Tu esegui e registri. Il sistema pianifica e analizza.",
            ),
            OnboardingPage(
                eyebrow = "NUTRIZIONE",
                title = "Piano completo, non suggerimenti",
                body = "Sette giorni con colazione, spuntini, pranzo e cena: alimenti, grammi, kcal e macro. Carb cycling HIGH / MEDIUM / LOW con bilancio settimanale coerente.",
                highlight = "Sostituzioni alimentari con quantità ricalcolate.",
            ),
            OnboardingPage(
                eyebrow = "ALLENAMENTO",
                title = "Progressione gestita dal motore",
                body = "Mesocicli, volume per gruppo muscolare, RIR e recuperi. Tu registri carico e ripetizioni: la doppia progressione la decide il motore.",
                highlight = "Deload solo quando è giustificato dai dati.",
            ),
            OnboardingPage(
                eyebrow = "COACH AI",
                title = "Analisi sui tuoi dati reali",
                body = "Il coach legge peso, aderenza, performance e composizione corporea. Ogni modifica arriva come proposta con motivazione, e resta in attesa del tuo via libera.",
                highlight = "Approvi o annulli: nessuna modifica silenziosa.",
            ),
        )
    }
}

data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val highlight: String,
)
