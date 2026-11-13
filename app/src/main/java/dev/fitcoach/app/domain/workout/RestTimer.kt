package dev.fitcoach.app.domain.workout

/**
 * Persistable timer state.
 *
 * Only timestamps are stored, never a countdown value: a countdown saved as a number
 * keeps ticking only while the process lives. Storing the deadline means the remaining
 * time is recomputed from the wall clock, so the timer stays correct after navigation,
 * backgrounding or a process death.
 */
data class RestTimerSnapshot(
    val endsAtMillis: Long? = null,
    val durationSeconds: Int = 90,
    val pausedRemainingSeconds: Int? = null
)

/**
 * Rest timer driven by an injectable clock.
 *
 * [start] arms the timer, [pause] freezes the remaining seconds, [resume] re-arms it,
 * [addSeconds] extends or shortens it, [skip] ends it immediately. The state can be
 * written with [snapshot] and rebuilt with [restore] at any point.
 */
class RestTimer(private val clock: () -> Long) {

    private var durationSeconds: Int = DEFAULT_SECONDS
    private var endsAtMillis: Long? = null
    private var pausedRemainingSeconds: Int? = null

    val isRunning: Boolean get() = endsAtMillis != null
    val isPaused: Boolean get() = pausedRemainingSeconds != null
    val isIdle: Boolean get() = endsAtMillis == null && pausedRemainingSeconds == null

    /** True when the timer reached zero and is waiting to be dismissed. */
    val isFinished: Boolean
        get() = endsAtMillis != null && (remainingSeconds() ?: 1) == 0

    val configuredSeconds: Int get() = durationSeconds

    fun start(seconds: Int = durationSeconds) {
        durationSeconds = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
        endsAtMillis = clock() + durationSeconds * 1000L
        pausedRemainingSeconds = null
    }

    fun pause() {
        val remaining = remainingSeconds() ?: return
        if (remaining <= 0) return
        pausedRemainingSeconds = remaining
        endsAtMillis = null
    }

    fun resume() {
        val remaining = pausedRemainingSeconds ?: return
        endsAtMillis = clock() + remaining * 1000L
        pausedRemainingSeconds = null
    }

    /** Adds [delta] seconds; a negative value shortens the rest but never below zero. */
    fun addSeconds(delta: Int) {
        if (isRunning) {
            val base = endsAtMillis ?: return
            val next = base + delta * 1000L
            endsAtMillis = next.coerceAtLeast(clock())
            return
        }
        val paused = pausedRemainingSeconds ?: return
        pausedRemainingSeconds = (paused + delta).coerceAtLeast(0)
    }

    fun skip() {
        endsAtMillis = null
        pausedRemainingSeconds = 0
    }

    fun stop() {
        endsAtMillis = null
        pausedRemainingSeconds = null
    }

    /**
     * Seconds left, rounded up so the user never sees 0 while a fraction is still left.
     * Returns null when the timer is idle, 0 when it has expired.
     */
    fun remainingSeconds(): Int? {
        pausedRemainingSeconds?.let { return it }
        val deadline = endsAtMillis ?: return null
        val left = deadline - clock()
        if (left <= 0) return 0
        return ((left + 999) / 1000).toInt()
    }

    fun snapshot(): RestTimerSnapshot = RestTimerSnapshot(
        endsAtMillis = endsAtMillis,
        durationSeconds = durationSeconds,
        pausedRemainingSeconds = pausedRemainingSeconds
    )

    fun restore(snapshot: RestTimerSnapshot) {
        durationSeconds = snapshot.durationSeconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
        endsAtMillis = snapshot.endsAtMillis
        pausedRemainingSeconds = snapshot.pausedRemainingSeconds
    }

    companion object {
        const val MIN_SECONDS = 5
        const val MAX_SECONDS = 900
        const val DEFAULT_SECONDS = 90
        const val STEP_SECONDS = 15
    }
}
