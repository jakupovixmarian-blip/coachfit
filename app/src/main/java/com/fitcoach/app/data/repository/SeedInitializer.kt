package com.fitcoach.app.data.repository

import com.fitcoach.app.data.local.Mappers.toEntity
import com.fitcoach.app.data.local.dao.ExerciseDao
import com.fitcoach.app.data.local.dao.FoodDao
import com.fitcoach.app.data.local.dao.NotificationDao
import com.fitcoach.app.data.local.dao.ScientificSourceDao
import com.fitcoach.app.data.local.entity.NotificationRecordEntity
import com.fitcoach.app.domain.knowledge.ScientificSourceSeedData
import com.fitcoach.app.domain.model.MealSlot
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.fitcoach.app.domain.workout.ExerciseSeedData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Popola il database con alimenti, esercizi e fonti scientifiche al primo avvio.
 * I dati seed non vengono mai sovrascritti se gia presenti (idempotente).
 */
@Singleton
class SeedInitializer @Inject constructor(
    private val foodDao: FoodDao,
    private val exerciseDao: ExerciseDao,
    private val notificationDao: NotificationDao,
    private val scientificSourceDao: ScientificSourceDao
) {
    suspend fun seedIfNeeded() {
        seedFoodsIncrementally()
        if (exerciseDao.count() == 0) {
            exerciseDao.insertAll(ExerciseSeedData.exercises.map { it.toEntity() })
        }
        if (notificationDao.getAll().isEmpty()) {
            notificationDao.insertAll(defaultNotifications())
        }
        if (scientificSourceDao.count() == 0) {
            scientificSourceDao.insertAll(ScientificSourceSeedData.sources)
        }
    }

    /**
     * Inserisce solo gli alimenti seed non ancora presenti (dedup per nome
     * normalizzato). Cosi gli utenti con un database gia popolato ricevono i
     * nuovi alimenti aggiunti nelle versioni successive senza duplicati.
     */
    private suspend fun seedFoodsIncrementally() {
        if (foodDao.count() == 0) {
            foodDao.insertAll(FoodSeedData.foods.map { it.toEntity() })
            return
        }
        val missing = FoodSeedData.foods.filter { food ->
            foodDao.getByNormalizedName(com.fitcoach.app.domain.model.FoodItem.normalizeName(food.name)) == null
        }
        if (missing.isNotEmpty()) {
            foodDao.insertAll(missing.map { it.toEntity() })
        }
    }

    private fun defaultNotifications(): List<NotificationRecordEntity> {
        var code = 100
        val mealRecords = MealSlot.entries.map { slot ->
            NotificationRecordEntity(
                kind = "MEAL_${slot.name}",
                title = "FitCoach – ${slot.label}",
                body = "Promemoria pasto: ${slot.label}.",
                triggerTime = slot.defaultTime,
                requestCode = code++
            )
        }
        return mealRecords + listOf(
            NotificationRecordEntity(
                kind = "WORKOUT", title = "FitCoach – Allenamento",
                body = "E' ora di allenarti.", triggerTime = "18:00", requestCode = code++
            ),
            NotificationRecordEntity(
                kind = "HYDRATION", title = "FitCoach – Idratazione",
                body = "Bevi un bicchiere d'acqua.", triggerTime = "10:00", requestCode = code++
            ),
            NotificationRecordEntity(
                kind = "CHECKIN", title = "FitCoach – Check-in settimanale",
                body = "Registra peso e misure.", triggerTime = "09:00", requestCode = code++
            ),
            NotificationRecordEntity(
                kind = "WEIGHT", title = "FitCoach – Valutazione del peso",
                body = "Pesati a digiuno.", triggerTime = "07:00", requestCode = code
            )
        )
    }
}
