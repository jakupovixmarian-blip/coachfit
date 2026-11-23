package com.fitcoach.app.audit

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.entity.CheckInEntity
import com.fitcoach.app.data.local.entity.ProgramChangeEntity
import com.fitcoach.app.data.local.entity.UserProfileEntity
import com.fitcoach.app.data.local.entity.WeightRecordEntity
import com.fitcoach.app.domain.model.ChangeDomain
import com.fitcoach.app.domain.model.ChangeStatus
import com.fitcoach.app.domain.model.Sex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Le query SQL non lanciano: restituiscono l'ordine sbagliato o il record
 * sbagliato. Qui si testano gli invarianti su cui il motore decide i pesi
 * (l'ultimo record e' davvero il piu' recente) e sulla gestione delle date.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DaoContractAdversarialTest {

    private lateinit var db: FitCoachDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FitCoachDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun peso(giorno: Long, kg: Double) =
        WeightRecordEntity(dateEpochDay = giorno, weightKg = kg)

    @Test
    fun `getLatestWeight restituisce il giorno piu' recente, non il primo`() = runTest {
        val dao = db.progressDao()
        dao.insertWeight(peso(20_000, 82.0))
        dao.insertWeight(peso(20_002, 81.0))
        dao.insertWeight(peso(20_001, 81.5))

        assertEquals(
            "Con l'ordinamento sbagliato il coach leggerebbe un peso vecchio",
            81.0, dao.getLatestWeight()!!.weightKg, 0.001
        )
        assertEquals(20_002, dao.getLatestWeight()!!.dateEpochDay)
    }

    @Test
    fun `getWeights parte dal piu' recente`() = runTest {
        val dao = db.progressDao()
        dao.insertWeight(peso(20_000, 82.0))
        dao.insertWeight(peso(20_005, 80.0))
        dao.insertWeight(peso(20_002, 81.0))

        val giorni = dao.getWeights().map { it.dateEpochDay }
        assertEquals(listOf(20_005L, 20_002L, 20_000L), giorni)
        assertTrue("L'ordine deve essere decrescente", giorni == giorni.sortedDescending())
    }

    @Test
    fun `due pesate lo stesso giorno restano due record distinti`() = runTest {
        val dao = db.progressDao()
        dao.insertWeight(peso(20_000, 82.0))
        dao.insertWeight(peso(20_000, 81.2))
        // Il DAO e' uno storico: sovrascrivere perderebbe la prima misurazione.
        // La media robusta la fa StatisticsEngine, non il DAO.
        assertEquals(2, dao.getWeights().size)
    }

    @Test
    fun `senza pesate getLatestWeight e' null e non zero`() = runTest {
        assertNull(db.progressDao().getLatestWeight())
    }

    @Test
    fun `observeWeights emette la lista aggiornata`() = runTest {
        val dao = db.progressDao()
        assertTrue(dao.observeWeights().first().isEmpty())
        dao.insertWeight(peso(20_000, 80.0))
        assertEquals(1, dao.observeWeights().first().size)
    }

    @Test
    fun `il check-in piu' recente e' quello con la settimana piu' avanzata`() = runTest {
        val dao = db.checkInDao()
        dao.insert(CheckInEntity(weekStartEpochDay = 20_000))
        dao.insert(CheckInEntity(weekStartEpochDay = 20_014))
        dao.insert(CheckInEntity(weekStartEpochDay = 20_007))

        assertEquals(20_014, dao.getLatest()!!.weekStartEpochDay)
        assertTrue(dao.getAll().map { it.weekStartEpochDay } == listOf(20_014L, 20_007L, 20_000L))
    }

    @Test
    fun `senza check-in getLatest e' null e non un record vuoto`() = runTest {
        assertNull(db.checkInDao().getLatest())
    }

    @Test
    fun `le modifiche proposte si leggono solo tra quelle PROPOSED`() = runTest {
        val dao = db.programChangeDao()
        inserisciModifica(dao, "PROPOSED", 1)
        inserisciModifica(dao, "APPLIED", 2)
        inserisciModifica(dao, "REJECTED", 3)

        val proposte = dao.observeProposed().first()
        assertEquals("La coda delle proposte deve contenere solo quelle in attesa", 1, proposte.size)
        assertEquals("PROPOSED", proposte.first().status.name)
    }

    @Test
    fun `una modifica applicata si ritrova per id`() = runTest {
        val dao = db.programChangeDao()
        val id = inserisciModifica(dao, "PROPOSED", 1)
        val trovata = dao.getById(id)
        assertNotNull("Senza lookup per id l'utente non puo' confermare nulla", trovata)
        assertEquals(id, trovata!!.id)
    }

    @Test
    fun `un id inesistente non restituisce una modifica a caso`() = runTest {
        assertNull(db.programChangeDao().getById(999_999L))
    }

    @Test
    fun `le modifiche applicate in attesa di esito sono filtrabili`() = runTest {
        val dao = db.programChangeDao()
        dao.insert(
            ProgramChangeEntity(
                domain = ChangeDomain.NUTRITION, status = ChangeStatus.APPLIED,
                title = "Test", situation = "", problem = "", dataUsed = "",
                proposedChange = "", rationale = "", expectedOutcome = "",
                contraindications = "", payloadJson = "{}", createdAt = 1_000,
                appliedAt = 2_000, outcomeStatus = "PENDING"
            )
        )
        assertEquals(
            "Il motore deve poter chiudere il cerchio sulle modifiche senza esito",
            1, dao.getAppliedPendingOutcome().size
        )
    }

    @Test
    fun `il profilo salvato si rilegge identico`() = runTest {
        val dao = db.userProfileDao()
        val profilo = UserProfileEntity(
            name = "Luca", ageYears = 30, sex = Sex.MALE, heightCm = 175.0, weightKg = 80.0
        )
        dao.upsert(profilo)
        val letto = dao.get()
        assertNotNull(letto)
        assertEquals("Luca", letto!!.name)
        assertEquals(80.0, letto.weightKg!!, 0.001)
    }

    @Test
    fun `senza profilo la lettura e' null e non un profilo a zero`() = runTest {
        assertNull(
            "Un profilo finto a 0 kg produrrebbe un piano alimentare senza senso",
            db.userProfileDao().get()
        )
    }

    @Test
    fun `salvare due volte aggiorna e non duplica`() = runTest {
        val dao = db.userProfileDao()
        dao.upsert(UserProfileEntity(
            name = "Luca", ageYears = 30, sex = Sex.MALE, heightCm = 175.0, weightKg = 80.0
        ))
        dao.upsert(UserProfileEntity(
            name = "Luca", ageYears = 31, sex = Sex.MALE, heightCm = 175.0, weightKg = 79.0
        ))
        val letto = dao.get()!!
        assertEquals("Non deve restare il profilo vecchio", 79.0, letto.weightKg!!, 0.001)
        assertEquals(31, letto.ageYears)
    }

    private suspend fun inserisciModifica(
        dao: com.fitcoach.app.data.local.dao.ProgramChangeDao,
        stato: String,
        creato: Long
    ): Long = dao.insert(
        ProgramChangeEntity(
            domain = ChangeDomain.NUTRITION,
            status = ChangeStatus.valueOf(stato),
            title = "Test $stato",
            situation = "", problem = "", dataUsed = "", proposedChange = "",
            rationale = "", expectedOutcome = "", contraindications = "",
            payloadJson = "{}", createdAt = creato
        )
    )
}
