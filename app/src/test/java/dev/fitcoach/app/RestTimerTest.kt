package dev.fitcoach.app

import dev.fitcoach.app.domain.workout.RestTimer
import dev.fitcoach.app.domain.workout.RestTimerSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The timer must survive navigation, backgrounding and a process death, so every
 * assertion here is made against a controllable clock.
 */
class RestTimerTest {

    private var now = 1_000_000L
    private fun timer() = RestTimer { now }

    @Test
    fun `parte dalla durata indicata e scende con il tempo`() {
        val timer = timer()
        timer.start(90)

        assertEquals(90, timer.remainingSeconds())
        now += 30_000L
        assertEquals(60, timer.remainingSeconds())
        now += 59_500L
        assertEquals(1, timer.remainingSeconds())
    }

    @Test
    fun `a scadenza restituisce zero ed e finito`() {
        val timer = timer()
        timer.start(60)
        now += 60_000L

        assertEquals(0, timer.remainingSeconds())
        assertTrue(timer.isFinished)
        assertTrue(timer.isRunning)
    }

    @Test
    fun `la pausa congela il tempo residuo`() {
        val timer = timer()
        timer.start(120)
        now += 45_000L
        timer.pause()

        assertTrue(timer.isPaused)
        assertFalse(timer.isRunning)
        now += 600_000L
        assertEquals(75, timer.remainingSeconds())
    }

    @Test
    fun `la ripresa riparte dal residuo congelato`() {
        val timer = timer()
        timer.start(120)
        now += 45_000L
        timer.pause()
        now += 600_000L
        timer.resume()

        assertTrue(timer.isRunning)
        assertEquals(75, timer.remainingSeconds())
        now += 15_000L
        assertEquals(60, timer.remainingSeconds())
    }

    @Test
    fun `piu quindici e piu trenta allungano il recupero`() {
        val timer = timer()
        timer.start(60)
        now += 20_000L

        timer.addSeconds(RestTimer.STEP_SECONDS)
        assertEquals(55, timer.remainingSeconds())
        timer.addSeconds(30)
        assertEquals(85, timer.remainingSeconds())
    }

    @Test
    fun `saltare il recupero lo chiude subito`() {
        val timer = timer()
        timer.start(180)
        timer.skip()

        assertEquals(0, timer.remainingSeconds())
        assertFalse(timer.isRunning)
        assertTrue(timer.isFinished)
    }

    @Test
    fun `lo stato sopravvive alla morte del processo`() {
        val first = timer()
        first.start(150)
        now += 40_000L
        val saved = first.snapshot()

        // Process death: a brand new instance is rebuilt from the persisted snapshot.
        now += 300_000L
        val restored = timer()
        restored.restore(saved)

        assertEquals(110, restored.remainingSeconds())
        assertTrue(restored.isRunning)
    }

    @Test
    fun `un timer in pausa viene ripristinato in pausa`() {
        val first = timer()
        first.start(90)
        now += 30_000L
        first.pause()
        val saved: RestTimerSnapshot = first.snapshot()

        now += 3_600_000L
        val restored = timer()
        restored.restore(saved)

        assertTrue(restored.isPaused)
        assertEquals(60, restored.remainingSeconds())
    }

    @Test
    fun `la durata richiesta resta nella forbice ammessa`() {
        val timer = timer()
        timer.start(1)
        assertEquals(RestTimer.MIN_SECONDS, timer.configuredSeconds)

        timer.start(5_000)
        assertEquals(RestTimer.MAX_SECONDS, timer.configuredSeconds)
    }

    @Test
    fun `un timer inattivo non ha tempo residuo`() {
        val timer = timer()

        assertTrue(timer.isIdle)
        assertNull(timer.remainingSeconds())
        assertFalse(timer.isFinished)
    }
}
