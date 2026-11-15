package com.fitcoach.app.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.repository.SeedInitializer
import com.fitcoach.app.domain.nutrition.FoodAssetImporter
import com.fitcoach.app.domain.nutrition.FoodSeedData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica l'import del database alimentare globale (asset USDA) su Room reale,
 * eseguendo l'effettivo [SeedInitializer] usato dall'app all'avvio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FoodDatabaseSeedIntegrationTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var seeder: SeedInitializer
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        seeder = SeedInitializer(
            context,
            db.foodDao(),
            db.exerciseDao(),
            db.notificationDao(),
            db.scientificSourceDao()
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `l avvio importa il catalogo curato e il database globale`() = runTest {
        seeder.seedIfNeeded()

        val total = db.foodDao().count()
        val usda = db.foodDao().countBySource(FoodAssetImporter.USDA_SOURCE)

        // Catalogo curato (>=150) + database globale USDA (>=7000).
        assertThat(total).isAtLeast(7000 + FoodSeedData.foods.size - 5)
        assertThat(usda).isAtLeast(7000)
    }

    @Test
    fun `gli alimenti curati non vengono sovrascritti dall import`() = runTest {
        seeder.seedIfNeeded()
        // La voce seed "Tempeh" esiste anche nel dataset USDA: deve restare quella
        // curata (fonte del seed, senza "SR Legacy") e non essere sovrascritta.
        val tempeh = db.foodDao().getByNormalizedName("tempeh")
        assertThat(tempeh).isNotNull()
        assertThat(tempeh!!.source).isEqualTo(FoodSeedData.SRC_USDA)
        assertThat(tempeh.source).doesNotContain("SR Legacy")
    }

    @Test
    fun `il seed e idempotente su esecuzioni ripetute`() = runTest {
        seeder.seedIfNeeded()
        val afterFirst = db.foodDao().count()
        seeder.seedIfNeeded()
        assertThat(db.foodDao().count()).isEqualTo(afterFirst)
    }

    @Test
    fun `la ricerca testuale trova alimenti del database globale`() = runTest {
        seeder.seedIfNeeded()
        val results = db.foodDao().searchNormalized("chicken")
        assertThat(results).isNotEmpty()
        assertThat(results.size).isAtMost(120)
    }
}
