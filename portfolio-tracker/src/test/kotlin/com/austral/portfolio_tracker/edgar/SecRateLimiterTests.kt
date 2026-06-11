package com.austral.portfolio_tracker.edgar

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SecRateLimiterTests {
    @Test
    fun `spaces requests so the configured rate is never exceeded`() {
        var now = 0L
        val sleeps = mutableListOf<Long>()
        val limiter =
            SecRateLimiter(
                requestsPerSecond = 10,
                nanoTime = { now },
                sleepMillis = { millis ->
                    sleeps += millis
                    now += millis * 1_000_000
                },
            )

        repeat(11) {
            limiter.acquire()
        }

        assertEquals(10, sleeps.size)
        assertEquals(1000L, sleeps.sum())
    }
}
