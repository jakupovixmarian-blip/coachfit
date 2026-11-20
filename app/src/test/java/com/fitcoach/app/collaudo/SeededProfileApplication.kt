package com.fitcoach.app.collaudo

import com.fitcoach.app.FitCoachApplication
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.ui.navigation.UserProfileEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

/**
 * Application di test con un utente gia' registrato.
 *
 * L'app parte dall'onboarding quando non c'e' profilo: i test di navigazione
 * devono quindi rappresentare un utente ESISTENTE, altrimenti verificherebbero
 * un percorso impossibile (un primo avvio che salta l'onboarding).
 * Il seme viene scritto in `onCreate`, prima che l'Activity sia lanciata.
 *
 * Viene seminato anche il flag "setup AI gia' visto": un utente ESISTENTE che
 * non ha ancora configurato il provider viene rimandato al setup all'avvio, ed
 * e' il comportamento voluto. Questi test verificano la navigazione di un
 * utente gia' operativo, quindi il setup va considerato alle spalle.
 */
class SeededProfileApplication : FitCoachApplication() {

    override fun onCreate() {
        super.onCreate()
        val repository = EntryPointAccessors
            .fromApplication(this, UserProfileEntryPoint::class.java)
            .userRepository()
        val setupStore = EntryPointAccessors
            .fromApplication(this, UserProfileEntryPoint::class.java)
            .setupStateStore()
        runBlocking {
            setupStore.setAiSetupSkipped(true)
            repository.save(
                UserProfile(
                    name = "Marjan",
                    ageYears = 32,
                    heightCm = 180.0,
                    weightKg = 82.0,
                    sex = Sex.MALE,
                    experience = TrainingExperience.INTERMEDIATE,
                    goal = Goal.RECOMP,
                    trainingDaysPerWeek = 4,
                )
            )
        }
    }
}
