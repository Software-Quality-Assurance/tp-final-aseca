package com.austral.portfolio_tracker.edgar

class SecRateLimiter(
    requestsPerSecond: Int = 10,
    private val nanoTime: () -> Long = System::nanoTime,
    private val sleepMillis: (Long) -> Unit = Thread::sleep,
) {
    private val intervalNanos = 1_000_000_000L / requestsPerSecond
    private var nextRequestAt = 0L

    @Synchronized
    fun acquire() {
        val now = nanoTime()
        if (nextRequestAt > now) {
            val waitNanos = nextRequestAt - now
            val waitMillis = (waitNanos + 999_999L) / 1_000_000L
            sleepMillis(waitMillis)
        }
        nextRequestAt = maxOf(nextRequestAt, nanoTime()) + intervalNanos
    }
}
