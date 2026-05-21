package com.austral.portfolio_tracker.security

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class JwtRevocationService(
    private val revokedTokens: MutableMap<String, Long> = ConcurrentHashMap(),
) {
    fun revoke(
        jti: String,
        expiresAt: Long,
    ) {
        revokedTokens[jti] = expiresAt
    }

    fun isRevoked(jti: String): Boolean {
        purgeExpired()
        return revokedTokens.containsKey(jti)
    }

    fun clearAll() {
        revokedTokens.clear()
    }

    private fun purgeExpired() {
        val now = Instant.now().epochSecond
        revokedTokens.entries.removeIf { it.value <= now }
    }
}
