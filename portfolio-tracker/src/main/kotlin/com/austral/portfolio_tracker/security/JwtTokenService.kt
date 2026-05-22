package com.austral.portfolio_tracker.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class JwtPrincipal(
    val userId: Long,
    val email: String,
)

class JwtValidationException(
    message: String,
) : RuntimeException(message)

@Service
class JwtTokenService(
    @Value("\${app.jwt.secret:portfolio-tracker-dev-secret}") private val secret: String,
    @Value("\${app.jwt.issuer:portfolio-tracker}") private val issuer: String,
    @Value("\${app.jwt.expiration-seconds:3600}") private val expirationSeconds: Long,
    private val objectMapper: ObjectMapper,
) {
    fun generateToken(
        userId: Long,
        email: String,
        issuedAtEpochSecond: Long = Instant.now().epochSecond,
        expiresAtEpochSecond: Long = issuedAtEpochSecond + expirationSeconds,
    ): String {
        val header = mapOf("alg" to "HS256", "typ" to "JWT")
        val jti = UUID.randomUUID().toString()
        val payload =
            mapOf(
                "iss" to issuer,
                "sub" to userId.toString(),
                "email" to email,
                "iat" to issuedAtEpochSecond,
                "exp" to expiresAtEpochSecond,
                "jti" to jti,
            )

        val encodedHeader = encodeJson(header)
        val encodedPayload = encodeJson(payload)
        val signingInput = "$encodedHeader.$encodedPayload"
        return "$signingInput.${sign(signingInput)}"
    }

    fun authenticate(token: String): JwtPrincipal {
        val parts = token.split('.')
        if (parts.size != 3) {
            throw JwtValidationException("JWT must contain exactly three segments")
        }

        val signingInput = "${parts[0]}.${parts[1]}"
        // Compare signatures in constant time to avoid timing attacks
        val expectedSignatureBytes = signBytes(signingInput)
        val providedSignatureBytes =
            try {
                Base64.getUrlDecoder().decode(parts[2])
            } catch (_: IllegalArgumentException) {
                throw JwtValidationException("JWT signature is invalid")
            }

        if (!MessageDigest.isEqual(providedSignatureBytes, expectedSignatureBytes)) {
            throw JwtValidationException("JWT signature is invalid")
        }

        val header = readJson(parts[0], "header")
        if (header.stringField("alg") != "HS256" || header.stringField("typ") != "JWT") {
            throw JwtValidationException("JWT header is invalid")
        }

        val payload = readJson(parts[1], "payload")
        if (payload.stringField("iss") != issuer) {
            throw JwtValidationException("JWT issuer is invalid")
        }

        val expiresAt = payload.longField("exp") ?: throw JwtValidationException("JWT expiration is missing")
        if (Instant.now().epochSecond >= expiresAt) {
            throw JwtValidationException("JWT is expired")
        }

        val userId = payload.stringField("sub")?.toLongOrNull() ?: throw JwtValidationException("JWT subject is invalid")
        val email = payload.stringField("email") ?: throw JwtValidationException("JWT email is missing")

        return JwtPrincipal(userId = userId, email = email)
    }

    private fun encodeJson(value: Any): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value))

    private fun sign(signingInput: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(signingInput.toByteArray(Charsets.UTF_8)))
    }

    private fun signBytes(signingInput: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(signingInput.toByteArray(Charsets.UTF_8))
    }

    private fun readJson(
        encodedPart: String,
        partName: String = "payload",
    ): JsonNode {
        try {
            val decoded =
                try {
                    Base64.getUrlDecoder().decode(encodedPart)
                } catch (_: IllegalArgumentException) {
                    throw JwtValidationException("JWT $partName is invalid")
                }

            return try {
                objectMapper.readTree(decoded)
            } catch (_: Exception) {
                throw JwtValidationException("JWT $partName is invalid")
            }
        } catch (e: JwtValidationException) {
            throw e
        }
    }

    private fun JsonNode.stringField(field: String): String? = get(field)?.takeIf { !it.isNull }?.toString()?.removeSurrounding("\"")

    private fun JsonNode.longField(field: String): Long? = get(field)?.takeIf { !it.isNull }?.asLong()
}
