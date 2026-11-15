package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.model.BreakfastPreference
import com.fitcoach.app.domain.model.CookingTimeAvailable
import com.fitcoach.app.domain.model.DietaryPreference
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Level3
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.TechnicalLevel
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.TrainingStatus
import com.fitcoach.app.domain.model.TransportMode
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.model.WorkType

/**
 * Profilo REALE di collaudo: Marjan.
 *
 * Dati dichiarati dall'utente:
 *  - 37 anni, 183 cm, 75 kg, 19% di massa grassa.
 *  - 7 anni di esperienza di allenamento con i pesi.
 *  - 7 mesi di inattivita recente (si e allenato in passato, poi ha smesso).
 *  - 4 allenamenti a settimana.
 *  - Limite di durata per sessione (assunto prudenziale di 60 minuti: il valore
 *    esatto dichiarato dall'utente era troncato; modificabile dall'utente).
 *
 * I dati non forniti NON vengono inventati come se fossero misurazioni reali:
 * sono scelte di configurazione del profilo di test, dichiarate qui.
 */
object MarjanProfile {

    fun build(): UserProfile = UserProfile(
        id = 1L,
        name = "Marjan",
        ageYears = 37,
        sex = Sex.MALE,
        heightCm = 183.0,
        weightKg = 75.0,
        bodyFatPercent = 19.0,
        experience = TrainingExperience.ADVANCED,
        trainingStatus = TrainingStatus.FORMERLY_TRAINED,
        goal = Goal.RECOMP,
        trainingDaysPerWeek = 4,
        mealsPerDay = 5,

        // Stile di vita (dichiarato in anamnesi).
        workType = WorkType.SEDENTARY,
        transportMode = TransportMode.CAR,
        dailySteps = 7000,
        sleepHours = 7.0,
        sleepQuality = Level3.MEDIUM,
        stressLevel = Level3.MEDIUM,
        dailyEnergy = Level3.MEDIUM,
        recoveryCapacity = Level3.MEDIUM,
        maxSessionMinutes = 60,

        // Alimentazione.
        breakfastPreference = BreakfastPreference.SWEET,
        dietaryPreferences = setOf(DietaryPreference.MEDITERRANEAN),
        cookingTimeAvailable = CookingTimeAvailable.MEDIUM,

        // Allenamento.
        trainingYears = 7.0,
        trainingFrequencyPerWeek = 4,
        technicalLevel = TechnicalLevel.PROFICIENT,
        availableEquipment = setOf(
            Equipment.BARBELL, Equipment.DUMBBELL, Equipment.MACHINE,
            Equipment.CABLE, Equipment.PULLUP_BAR, Equipment.DIP_STATION, Equipment.BENCH
        ),

        anamnesisCompleted = true,
        anamnesisConfirmedAt = 1L,
        anamnesisSummary = "Anamnesi confermata dal profilo di collaudo Marjan."
    )
}
