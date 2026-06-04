package com.austral.portfolio_tracker.auth

import com.austral.portfolio_tracker.security.JwtTokenService
import com.austral.portfolio_tracker.security.JwtValidationException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenServiceTests {
    @Autowired
    private lateinit var jwtTokenService: JwtTokenService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = 123L
    private val testEmail = "test@example.com"

    @Test
    fun `should validate a correctly generated token`() {
        // Arrange
        val token = jwtTokenService.generateToken(testUserId, testEmail)

        // Act
        val principal = jwtTokenService.authenticate(token)

        // Assert
        assertEquals(testUserId, principal.userId)
        assertEquals(testEmail, principal.email)
    }

    @Test
    fun `should throw JwtValidationException when token has only one segment`() {
        // Arrange
        val invalidToken = "onlyone"

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT must contain exactly three segments", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when token has two segments`() {
        // Arrange
        val invalidToken = "segment1.segment2"

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT must contain exactly three segments", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when token has four segments`() {
        // Arrange
        val invalidToken = "segment1.segment2.segment3.segment4"

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT must contain exactly three segments", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when signature is invalid`() {
        // Arrange
        val validToken = jwtTokenService.generateToken(testUserId, testEmail)
        val parts = validToken.split(".")
        val invalidToken = "${parts[0]}.${parts[1]}.invalidsignature"

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT signature is invalid", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when header algorithm is invalid`() {
        // Arrange - manually create token with invalid header
        val invalidToken =
            createSignedTokenWithCustomHeader(
                detailedHeader = mapOf("alg" to "RS256", "typ" to "JWT"),
                payload =
                    mapOf(
                        "iss" to "portfolio-tracker",
                        "sub" to testUserId.toString(),
                        "email" to testEmail,
                        "iat" to Instant.now().epochSecond,
                        "exp" to (Instant.now().epochSecond + 3600),
                    ),
            )

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT header is invalid", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when header type is invalid`() {
        // Arrange - manually create token with invalid header typ
        val invalidToken =
            createSignedTokenWithCustomHeader(
                detailedHeader = mapOf("alg" to "HS256", "typ" to "BEARER"),
                payload =
                    mapOf(
                        "iss" to "portfolio-tracker",
                        "sub" to testUserId.toString(),
                        "email" to testEmail,
                        "iat" to Instant.now().epochSecond,
                        "exp" to (Instant.now().epochSecond + 3600),
                    ),
            )

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT header is invalid", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when issuer is invalid`() {
        // Arrange - manually create token with wrong issuer
        val invalidToken =
            createSignedTokenWithCustomHeader(
                detailedHeader = mapOf("alg" to "HS256", "typ" to "JWT"),
                payload =
                    mapOf(
                        "iss" to "wrong-issuer",
                        "sub" to testUserId.toString(),
                        "email" to testEmail,
                        "iat" to Instant.now().epochSecond,
                        "exp" to (Instant.now().epochSecond + 3600),
                    ),
            )

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT issuer is invalid", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when expiration is expired`() {
        // Arrange
        val expiredToken =
            jwtTokenService.generateToken(
                testUserId,
                testEmail,
                issuedAtEpochSecond = Instant.now().epochSecond - 7200,
                expiresAtEpochSecond = Instant.now().epochSecond - 3600,
            )

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(expiredToken)
            }
        assertEquals("JWT is expired", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when expiration field is missing`() {
        // Arrange
        val invalidToken =
            createSignedTokenWithCustomHeader(
                detailedHeader = mapOf("alg" to "HS256", "typ" to "JWT"),
                payload =
                    mapOf(
                        "iss" to "portfolio-tracker",
                        "sub" to testUserId.toString(),
                        "email" to testEmail,
                        "iat" to Instant.now().epochSecond,
                        // Missing "exp"
                    ),
            )

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT expiration is missing", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when subject is not numeric`() {
        // Arrange
        val invalidToken =
            createSignedTokenWithCustomHeader(
                detailedHeader = mapOf("alg" to "HS256", "typ" to "JWT"),
                payload =
                    mapOf(
                        "iss" to "portfolio-tracker",
                        "sub" to "not-a-number",
                        "email" to testEmail,
                        "iat" to Instant.now().epochSecond,
                        "exp" to (Instant.now().epochSecond + 3600),
                    ),
            )

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT subject is invalid", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when subject is missing`() {
        // Arrange
        val invalidToken =
            createSignedTokenWithCustomHeader(
                detailedHeader = mapOf("alg" to "HS256", "typ" to "JWT"),
                payload =
                    mapOf(
                        "iss" to "portfolio-tracker",
                        // Missing "sub"
                        "email" to testEmail,
                        "iat" to Instant.now().epochSecond,
                        "exp" to (Instant.now().epochSecond + 3600),
                    ),
            )

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT subject is invalid", exception.message)
    }

    @Test
    fun `should throw JwtValidationException when email is missing`() {
        // Arrange
        val invalidToken =
            createSignedTokenWithCustomHeader(
                detailedHeader = mapOf("alg" to "HS256", "typ" to "JWT"),
                payload =
                    mapOf(
                        "iss" to "portfolio-tracker",
                        "sub" to testUserId.toString(),
                        // Missing "email"
                        "iat" to Instant.now().epochSecond,
                        "exp" to (Instant.now().epochSecond + 3600),
                    ),
            )

        // Act & Assert
        val exception =
            assertFailsWith<JwtValidationException> {
                jwtTokenService.authenticate(invalidToken)
            }
        assertEquals("JWT email is missing", exception.message)
    }

    @Test
    fun `should generate token with all required fields`() {
        // Act
        val token = jwtTokenService.generateToken(testUserId, testEmail)

        // Assert (decode and verify)
        val parts = token.split(".")
        assertEquals(3, parts.size, "Token should have 3 segments")

        val payloadJson =
            String(
                Base64.getUrlDecoder().decode(parts[1]),
                Charsets.UTF_8,
            )
        val payload = objectMapper.readTree(payloadJson)

        assertEquals("portfolio-tracker", payload.get("iss")?.toString()?.removeSurrounding("\""))
        assertEquals(testUserId.toString(), payload.get("sub")?.toString()?.removeSurrounding("\""))
        assertEquals(testEmail, payload.get("email")?.toString()?.removeSurrounding("\""))
        assertNotNull(payload.get("iat"))
        assertNotNull(payload.get("exp"))
    }

    @Test
    fun `should not expose sensitive information in generated token`() {
        // Act
        val token = jwtTokenService.generateToken(testUserId, testEmail)
        val parts = token.split(".")

        // Assert
        val payloadText =
            String(
                Base64.getUrlDecoder().decode(parts[1]),
                Charsets.UTF_8,
            )

        assertFalse(payloadText.contains("password", ignoreCase = true))
        assertFalse(payloadText.contains("hash", ignoreCase = true))
    }

    @Test
    fun `should handle token with invalid base64 in payload gracefully`() {
        // Arrange - Create a token with invalid base64 encoding
        val validToken = jwtTokenService.generateToken(testUserId, testEmail)
        val parts = validToken.split(".")

        // Replace payload with invalid base64
        val corruptedToken = "${parts[0]}.!!!invalid-base64!!!.${parts[2]}"

        // Act & Assert
        val exception =
            assertFailsWith<Exception> {
                jwtTokenService.authenticate(corruptedToken)
            }
        // Will fail during Base64 decoding
        assertNotNull(exception)
    }

    @Test
    fun `should validate token issued at expected time`() {
        // Arrange
        val now = Instant.now().epochSecond
        val token =
            jwtTokenService.generateToken(
                testUserId,
                testEmail,
                issuedAtEpochSecond = now,
                expiresAtEpochSecond = now + 3600,
            )

        // Act
        val principal = jwtTokenService.authenticate(token)

        // Assert
        assertEquals(testUserId, principal.userId)
        assertEquals(testEmail, principal.email)
    }

    @Test
    fun `should validate a token just before expiration`() {
        // Arrange
        val now = Instant.now().epochSecond
        val token =
            jwtTokenService.generateToken(
                testUserId,
                testEmail,
                issuedAtEpochSecond = now - 3599,
                expiresAtEpochSecond = now + 1, // Expires in 1 second
            )

        // Act & Assert
        val principal = jwtTokenService.authenticate(token)
        assertEquals(testUserId, principal.userId)
    }

    /**
     * Helper function to create a properly signed token with custom header and payload.
     * This allows testing validation of specific payload fields by bypassing signature checks.
     */
    private fun createSignedTokenWithCustomHeader(
        detailedHeader: Map<String, Any>,
        payload: Map<String, Any>,
    ): String {
        val encodedHeader =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(detailedHeader))

        // Ensure a jti exists in the payload so authentication (which enforces jti) behaves as expected
        val effectivePayload = payload.toMutableMap()
        if (!effectivePayload.containsKey("jti")) {
            effectivePayload["jti"] =
                UUID
                    .randomUUID()
                    .toString()
        }

        val encodedPayload =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(effectivePayload))

        val signingInput = "$encodedHeader.$encodedPayload"

        // Get the secret from jwtTokenService via reflection
        val secretField = jwtTokenService.javaClass.getDeclaredField("secret")
        secretField.isAccessible = true
        val secret = secretField.get(jwtTokenService) as String

        // Sign using HmacSHA256
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(signingInput.toByteArray(Charsets.UTF_8)))

        return "$signingInput.$signature"
    }
}
