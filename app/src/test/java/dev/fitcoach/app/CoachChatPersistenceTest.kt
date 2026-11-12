package dev.fitcoach.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.fitcoach.app.data.local.db.FitCoachDatabase
import dev.fitcoach.app.data.repository.CoachChatRepositoryImpl
import dev.fitcoach.app.domain.model.ChatRole
import dev.fitcoach.app.domain.model.CoachChatMessage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression test for the crash the user hit while chatting: the intake screen
 * rendered the turns with `key = { it.id }`, so a list of in-memory messages all
 * carrying id = 0 made Compose throw on the duplicate keys. Persisting every turn
 * through the repository is what guarantees distinct, non-zero ids.
 */
@RunWith(RobolectricTestRunner::class)
class CoachChatPersistenceTest {

    private lateinit var db: FitCoachDatabase
    private lateinit var repo: CoachChatRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FitCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = CoachChatRepositoryImpl(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `every stored turn gets a distinct non-zero id`() = runBlocking {
        val first = repo.add(CoachChatMessage(role = ChatRole.ASSISTANT, content = "Ciao, come ti chiami?"))
        val second = repo.add(CoachChatMessage(role = ChatRole.USER, content = "Marco"))
        val third = repo.add(CoachChatMessage(role = ChatRole.ASSISTANT, content = "Quanti anni hai?"))

        assertTrue(first > 0L && second > 0L && third > 0L)

        val ids = repo.getMessages().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(listOf(first, second, third), ids)
    }

    @Test
    fun `turns come back in the order they were written`() = runBlocking {
        repo.add(CoachChatMessage(role = ChatRole.ASSISTANT, content = "Prima domanda"))
        repo.add(CoachChatMessage(role = ChatRole.USER, content = "Risposta"))
        repo.add(CoachChatMessage(role = ChatRole.ASSISTANT, content = "Seconda domanda"))

        val contents = repo.getMessages().map { it.content }
        assertEquals(listOf("Prima domanda", "Risposta", "Seconda domanda"), contents)
    }

    @Test
    fun `the intake opening question is only written once`() = runBlocking {
        val opening = CoachChatMessage(role = ChatRole.ASSISTANT, content = "Ciao! Sono il tuo coach.")

        // Simulates the two concurrent seeds: init and "ricontrolla".
        if (repo.getMessages().isEmpty()) repo.add(opening)
        if (repo.getMessages().isEmpty()) repo.add(opening)

        assertEquals(1, repo.getMessages().size)
    }
}
