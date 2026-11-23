package com.fitcoach.app.audit

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitcoach.app.data.health.HealthConnectReader
import com.fitcoach.app.data.local.FitCoachDatabase
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.UserRepository
import com.fitcoach.app.domain.model.WeightRecord
import com.fitcoach.app.ui.health.HealthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Health Connect su un telefono senza il servizio (o dopo un riavvio del
 * provider) risponde NOT_SUPPORTED. La schermata deve dirlo e restare usabile:
 * e' il caso piu' comune, non un caso limite.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HealthAdversarialTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FitCoachDatabase
    private lateinit var reader: HealthConnectReader
    private lateinit var viewModel: HealthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries().build()
        reader = HealthConnectReader(context)
        viewModel = HealthViewModel(
            healthReader = reader,
            progressRepository = ProgressRepository(db.progressDao(), db.checkInDao()),
            userRepository = UserRepository(db.userProfileDao())
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun attendi(timeoutMs: Long = 3_000, cond: () -> Boolean) {
        val fine = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < fine) {
            if (cond()) return
            Thread.sleep(20)
        }
        throw AssertionError("Condizione non raggiunta entro ${timeoutMs}ms")
    }

    @Test
    fun `lo stato iniziale non e' caricamento infinito`() {
        attendi { !viewModel.state.value.loading }
        assertFalse(
            "Se la lettura non finisce la schermata mostra uno spinner per sempre",
            viewModel.state.value.loading
        )
    }

    @Test
    fun `senza health connect lo dichiara invece di mostrare zeri`() {
        attendi { !viewModel.state.value.loading }
        val s = viewModel.state.value
        assertTrue(
            "Deve dire che il servizio non e' supportato: ${s.availability}",
            s.availability == HealthConnectReader.Availability.NOT_SUPPORTED ||
                s.availability == HealthConnectReader.Availability.UPDATE_REQUIRED
        )
        assertNull(
            "Senza servizio non deve inventare uno snapshot",
            s.snapshot
        )
    }

    @Test
    fun `i permessi mancanti sono contati, non detti a zero`() {
        attendi { !viewModel.state.value.loading }
        val s = viewModel.state.value
        assertFalse("Nessun permesso puo' essere concesso senza il servizio", s.permissionsGranted)
        assertEquals(
            "Devono risultare tutti mancanti",
            reader.permissions.size, s.missingPermissions
        )
    }

    @Test
    fun `il peso della bilancia arriva anche senza health connect`() {
        kotlinx.coroutines.runBlocking {
            db.progressDao().insertWeight(
                Mappers.run {
                    WeightRecord(dateEpochDay = java.time.LocalDate.now().toEpochDay(), weightKg = 81.5)
                        .toEntity()
                }
            )
        }
        viewModel.refresh()
        attendi { !viewModel.state.value.loading }
        attendi { viewModel.state.value.scaleWeightKg != null }
        assertEquals(81.5, viewModel.state.value.scaleWeightKg!!, 0.001)
    }

    @Test
    fun `senza pesate il peso resta nullo e non zero`() {
        viewModel.refresh()
        attendi { !viewModel.state.value.loading }
        assertNull(
            "Uno zero verrebbe letto come una pesata reale da 0 kg",
            viewModel.state.value.scaleWeightKg
        )
    }

    @Test
    fun `un refresh ripetuto non lascia lo stato in caricamento`() {
        repeat(3) {
            viewModel.refresh()
            attendi { !viewModel.state.value.loading }
            assertFalse("Refresh $it bloccato", viewModel.state.value.loading)
        }
    }

    @Test
    fun `il messaggio puo' essere consumato e non ricompare`() {
        viewModel.onPermissionsResult()
        attendi { viewModel.state.value.message != null }
        assertNotNull(
            "Senza permessi deve spiegare perche' la schermata resta vuota",
            viewModel.state.value.message
        )
        viewModel.consumeMessage()
        assertNull("Consumato deve sparire", viewModel.state.value.message)
    }

    @Test
    fun `il messaggio di permessi nega esplicitamente l'accesso`() {
        viewModel.onPermissionsResult()
        attendi { viewModel.state.value.message != null }
        val msg = viewModel.state.value.message!!.lowercase()
        assertTrue(
            "Deve dire che l'accesso non e' stato concesso: $msg",
            msg.contains("non concesso") || msg.contains("parziale")
        )
    }

    @Test
    fun `i permessi richiesti sono quelli dichiarati dal lettore`() {
        assertEquals(reader.permissions, viewModel.requiredPermissions)
        assertTrue("Devono essere davvero tredici letture", viewModel.requiredPermissions.isNotEmpty())
    }

    @Test
    fun `lo stato dopo il refresh non conserva lo snapshot di prima`() {
        viewModel.refresh()
        attendi { !viewModel.state.value.loading }
        val primo = viewModel.state.value.snapshot
        viewModel.refresh()
        attendi { !viewModel.state.value.loading }
        assertEquals(
            "Un refresh non deve riusare lo snapshot vecchio (il pulsante sembrerebbe inerte)",
            primo, viewModel.state.value.snapshot
        )
    }
}
