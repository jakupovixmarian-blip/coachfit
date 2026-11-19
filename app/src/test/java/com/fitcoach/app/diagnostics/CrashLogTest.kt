package com.fitcoach.app.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Il registro deve essere scrivibile, leggibile e cancellabile: e la base per diagnosticare un crash reale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CrashLogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `read returns null when nothing was recorded`() {
        CrashLog.clear(context)
        assertThat(CrashLog.read(context)).isNull()
    }

    @Test
    fun `clear removes a previously recorded log`() {
        CrashLog.file(context).writeText("errore")
        assertThat(CrashLog.read(context)).isEqualTo("errore")
        CrashLog.clear(context)
        assertThat(CrashLog.read(context)).isNull()
    }

    @Test
    fun `installs an uncaught handler that still delegates to the previous one`() {
        val original = Thread.getDefaultUncaughtExceptionHandler()
        var delegated = false
        val fake = Thread.UncaughtExceptionHandler { _, _ -> delegated = true }
        try {
            Thread.setDefaultUncaughtExceptionHandler(fake)
            CrashLog.install(context)
            val installed = Thread.getDefaultUncaughtExceptionHandler()!!
            assertThat(installed).isNotSameInstanceAs(fake)
            installed.uncaughtException(Thread.currentThread(), IllegalStateException("boom"))
            assertThat(delegated).isTrue()
            assertThat(CrashLog.read(context)).contains("boom")
            assertThat(CrashLog.read(context)).contains("FitCoach")
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original)
            CrashLog.clear(context)
        }
    }
}
