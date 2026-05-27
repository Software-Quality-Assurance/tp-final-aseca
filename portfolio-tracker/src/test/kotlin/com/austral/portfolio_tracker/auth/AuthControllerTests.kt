package com.austral.portfolio_tracker.auth

import com.austral.portfolio_tracker.dto.RegisterUserRequest
import com.austral.portfolio_tracker.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanDatabase() {
        userRepository.deleteAll()
    }

    @Test
    fun `should return jwt token when credentials are valid`() {
        // Arrange: register a user first
        val registerRequest =
            RegisterUserRequest(
                email = "login@example.com",
                password = "Password123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(registerRequest)
            }.andExpect {
                status { isCreated() }
            }

        val loginRequest =
            mapOf(
                "email" to "login@example.com",
                "password" to "Password123!",
            )

        // Act + Assert
        val response =
            mockMvc
                .post("/api/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(loginRequest)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.token") { exists() }
                    jsonPath("$.token") { isString() }
                    jsonPath("$.token") { isNotEmpty() }
                }.andReturn()

        assertNotNull(response.response.contentAsString)
    }

    @Test
    fun `should return a jwt shaped token when credentials are valid`() {
        val registerRequest =
            RegisterUserRequest(
                email = "jwt-format@example.com",
                password = "Password123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(registerRequest)
            }.andExpect {
                status { isCreated() }
            }

        val loginRequest =
            mapOf(
                "email" to "jwt-format@example.com",
                "password" to "Password123!",
            )

        val response =
            mockMvc
                .post("/api/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(loginRequest)
                }.andExpect {
                    status { isOk() }
                }.andReturn()

        val token =
            response.response.contentAsString
                .substringAfter("\"token\":\"")
                .substringBefore('"')

        assertTrue(token.isNotBlank(), "Token field was missing from the login response")

        assertEquals(3, token.split('.').size, "JWTs should have three dot-separated segments")
        assertTrue(
            token.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' },
            "JWT should contain only URL-safe characters",
        )
    }

    @Test
    fun `should not expose sensitive information in jwt payload when credentials are valid`() {
        val registerRequest =
            RegisterUserRequest(
                email = "sensitive-check@example.com",
                password = "Password123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(registerRequest)
            }.andExpect {
                status { isCreated() }
            }

        val loginRequest =
            mapOf(
                "email" to "sensitive-check@example.com",
                "password" to "Password123!",
            )

        val response =
            mockMvc
                .post("/api/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(loginRequest)
                }.andExpect {
                    status { isOk() }
                }.andReturn()

        val token =
            response.response.contentAsString
                .substringAfter("\"token\":\"")
                .substringBefore('"')

        val payloadJson =
            String(
                Base64.getUrlDecoder().decode(token.split('.')[1]),
                Charsets.UTF_8,
            )

        val payload = objectMapper.readTree(payloadJson)
        val payloadText = payload.toString().lowercase()

        assertTrue("password" !in payloadText, "JWT payload must not expose passwords")
        assertTrue("passwordhash" !in payloadText, "JWT payload must not expose password hashes")
    }

    @Test
    fun `should return 401 unauthorized with generic message when credentials are invalid`() {
        val registerRequest =
            RegisterUserRequest(
                email = "invalid-login@example.com",
                password = "Password123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(registerRequest)
            }.andExpect {
                status { isCreated() }
            }

        val loginRequest =
            mapOf(
                "email" to "invalid-login@example.com",
                "password" to "WrongPassword123!",
            )

        mockMvc
            .post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(loginRequest)
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { value("Invalid credentials") }
            }
    }

    @Test
    fun `should return 401 unauthorized when jwt is invalid or expired on protected endpoint`() {
        mockMvc
            .get("/api/users/me") {
                header("Authorization", "Bearer invalid-or-expired.jwt.token")
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `login with null email returns unauthorized with generic message`() {
        val invalidJson =
            """
            {
                "email": null,
                "password": "Password123!"
            }
            """.trimIndent()

        mockMvc
            .post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = invalidJson
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { value("Invalid credentials") }
            }
    }

    @Test
    fun `login with null password returns unauthorized with generic message`() {
        val invalidJson =
            """
            {
                "email": "user@example.com",
                "password": null
            }
            """.trimIndent()

        mockMvc
            .post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = invalidJson
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { value("Invalid credentials") }
            }
    }
}
