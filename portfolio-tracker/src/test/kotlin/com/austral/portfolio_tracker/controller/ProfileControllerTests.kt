package com.austral.portfolio_tracker.controller

import com.austral.portfolio_tracker.dto.RegisterUserRequest
import com.austral.portfolio_tracker.repository.UserRepository
import com.austral.portfolio_tracker.security.JwtTokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.JsonNodeType
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileControllerTests {
    private companion object {
        const val TEST_PASSWORD = "Password123!"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenService: JwtTokenService

    @BeforeEach
    fun cleanDatabase() {
        userRepository.deleteAll()
    }

    @Test
    fun `should return 201 Created when registering with valid data`() {
        val request =
            RegisterUserRequest(
                email = "newuser@example.com",
                password = "SecurePassword123!",
            )

        val response =
            mockMvc
                .post("/api/auth/register") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isCreated() }
                }.andReturn()

        val content = response.response.contentAsString
        assertNotNull(content)
    }

    @Test
    fun `should return user with id in response`() {
        val request =
            RegisterUserRequest(
                email = "witheid@example.com",
                password = "SecurePassword123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { exists() }
                jsonPath("$.id") { isNotEmpty() }
                jsonPath("$.email") { value("witheid@example.com") }
            }
    }

    @Test
    fun `should not expose password in response`() {
        val request =
            RegisterUserRequest(
                email = "nopwd@example.com",
                password = "SecurePassword123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.password") { doesNotExist() }
            }
    }

    @Test
    fun `should return 409 Conflict when email already registered`() {
        val request =
            RegisterUserRequest(
                email = "existing@example.com",
                password = "Password123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isConflict() }
            }
    }

    @Test
    fun `should treat email as case insensitive for duplicate detection`() {
        val request1 =
            RegisterUserRequest(
                email = "CaseInsensitive@Example.COM",
                password = "Password123!",
            )
        val request2 =
            RegisterUserRequest(
                email = "caseinsensitive@example.com",
                password = "DifferentPassword123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request1)
            }.andExpect {
                status { isCreated() }
            }

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request2)
            }.andExpect {
                status { isConflict() }
            }
    }

    @Test
    fun `should return 400 Bad Request when email is missing`() {
        val request =
            RegisterUserRequest(
                email = "",
                password = "Password123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { exists() }
            }
    }

    @Test
    fun `should return 400 Bad Request when password is missing`() {
        val request =
            RegisterUserRequest(
                email = "user@example.com",
                password = "",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { exists() }
            }
    }

    @Test
    fun `should return 400 Bad Request when email format is invalid`() {
        val request =
            RegisterUserRequest(
                email = "invalid-email-without-at-sign",
                password = "Password123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { exists() }
            }
    }

    @Test
    fun `should return 400 Bad Request when password is too weak`() {
        val request =
            RegisterUserRequest(
                email = "user@example.com",
                password = "weak",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { exists() }
            }
    }

    @Test
    fun `should provide clear error messages for validation failures`() {
        val request =
            RegisterUserRequest(
                email = "invalid",
                password = "",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { isString() }
                jsonPath("$.error") { isNotEmpty() }
            }
    }

    @Test
    fun `should persist registered user in database`() {
        val email = "persistence@example.com"
        val request =
            RegisterUserRequest(
                email = email,
                password = "Password123!",
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }

        val savedUser = userRepository.findByMail(email)
        assertNotNull(savedUser)
        assertEquals(email, savedUser?.mail)
    }

    @Test
    fun `should return 400 when required fields are null`() {
        val invalidJson =
            """
            {
                "email": null,
                "password": null
            }
            """.trimIndent()

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = invalidJson
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `should handle multiple registration requests sequentially`() {
        for (i in 1..3) {
            val request =
                RegisterUserRequest(
                    email = "sequential$i@example.com",
                    password = "Password123!",
                )

            mockMvc
                .post("/api/auth/register") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.email") { value("sequential$i@example.com") }
                }
        }

        assertEquals(3, userRepository.findAll().count { it.mail.startsWith("sequential") })
    }

    @Test
    fun `should return the authenticated user profile when a valid jwt is provided`() {
        val email = "profile-success-${System.currentTimeMillis()}@example.com"

        registerUser(email)
        val token = loginAndGetToken(email)

        mockMvc
            .get("/api/users/me") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { exists() }
                jsonPath("$.email") { value(email) }
            }
    }

    @Test
    fun `should return 401 when an expired jwt is used on a protected endpoint`() {
        val email = "profile-expired-${System.currentTimeMillis()}@example.com"

        registerUser(email)
        val user = requireNotNull(userRepository.findByMail(email))

        val token =
            jwtTokenService.generateToken(
                userId = requireNotNull(user.id),
                email = user.mail,
                expiresAtEpochSecond = Instant.now().epochSecond - 60,
            )

        mockMvc
            .get("/api/users/me") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    private fun registerUser(email: String) {
        val request =
            RegisterUserRequest(
                email = email,
                password = TEST_PASSWORD,
            )

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }
    }

    private fun loginAndGetToken(email: String): String {
        val loginRequest =
            mapOf(
                "email" to email,
                "password" to TEST_PASSWORD,
            )

        val response =
            mockMvc
                .post("/api/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(loginRequest)
                }.andExpect {
                    status { isOk() }
                }.andReturn()

        val tokenNode = objectMapper.readTree(response.response.contentAsString).path("token")
        require(tokenNode.nodeType == JsonNodeType.STRING) { "Login response did not contain a token" }
        return tokenNode.toString().removeSurrounding("\"")
    }

    @Test
    fun `should return 401 when request has no authorization header`() {
        mockMvc
            .get("/api/users/me") {
                // No Authorization header provided
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `should return 404 when user has been deleted after login`() {
        val email = "to-delete-${System.currentTimeMillis()}@example.com"

        // Register and login
        registerUser(email)
        val user = requireNotNull(userRepository.findByMail(email))
        val token = loginAndGetToken(email)

        // Delete the user from the database
        userRepository.deleteById(requireNotNull(user.id))

        // Attempt to access protected endpoint with previously issued token
        mockMvc
            .get("/api/users/me") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `should update email when authenticated`() {
        val email = "update-email-${System.currentTimeMillis()}@example.com"
        registerUser(email)
        val token = loginAndGetToken(email)

        val updateJson = "{\"email\":\"new-$email\"}"

        mockMvc
            .patch("/api/users/me") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = updateJson
            }.andExpect {
                status { isOk() }
                jsonPath("$.email") { value("new-$email") }
            }

        val saved = userRepository.findByMail("new-$email")
        assertNotNull(saved)
    }

    @Test
    fun `should update password and allow login with new password`() {
        val email = "update-password-${System.currentTimeMillis()}@example.com"
        registerUser(email)
        val token = loginAndGetToken(email)

        val newPassword = "NewPass123!"
        val updateJson = "{\"password\":\"$newPassword\"}"

        mockMvc
            .patch("/api/users/me") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = updateJson
            }.andExpect {
                status { isOk() }
            }

        // Try logging in with new password
        val loginRequest = mapOf("email" to email, "password" to newPassword)
        mockMvc
            .post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(loginRequest)
            }.andExpect {
                status { isOk() }
                jsonPath("$.token") { exists() }
            }
    }

    @Test
    fun `should return 400 when updating with invalid email`() {
        val email = "update-invalid-email-${System.currentTimeMillis()}@example.com"
        registerUser(email)
        val token = loginAndGetToken(email)

        val updateJson = "{\"email\":\"invalid-email\"}"

        mockMvc
            .patch("/api/users/me") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = updateJson
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { exists() }
            }
    }

    @Test
    fun `should return 400 when updating with weak password`() {
        val email = "update-weak-pass-${System.currentTimeMillis()}@example.com"
        registerUser(email)
        val token = loginAndGetToken(email)

        val updateJson = "{\"password\":\"123\"}"

        mockMvc
            .patch("/api/users/me") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = updateJson
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { exists() }
            }
    }

    @Test
    fun `should delete own account`() {
        val email = "delete-success-${System.currentTimeMillis()}@example.com"
        registerUser(email)
        val token = loginAndGetToken(email)

        mockMvc
            .delete("/api/users/me") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNoContent() }
            }

        val saved = userRepository.findByMail(email)
        kotlin.test.assertNull(saved)
    }

    @Test
    fun `delete returns 404 when user not found`() {
        val email = "delete-notfound-${System.currentTimeMillis()}@example.com"
        registerUser(email)
        val token = loginAndGetToken(email)

        // Delete user directly
        val user = requireNotNull(userRepository.findByMail(email))
        userRepository.deleteById(requireNotNull(user.id))

        // Now calling delete with token should return 404
        mockMvc
            .delete("/api/users/me") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `should not allow updating another user's id via request body`() {
        // Create two users
        val emailA = "usera-${System.currentTimeMillis()}@example.com"
        val emailB = "userb-${System.currentTimeMillis()}@example.com"

        registerUser(emailA)
        registerUser(emailB)

        val userB = requireNotNull(userRepository.findByMail(emailB))

        // Login as user A
        val tokenA = loginAndGetToken(emailA)

        // Attempt to send a PATCH that includes an 'id' field pointing to userB
        val updateJson = "{\"id\":${userB.id}, \"email\":\"hacked-$emailA\"}"

        mockMvc
            .patch("/api/users/me") {
                header("Authorization", "Bearer $tokenA")
                contentType = MediaType.APPLICATION_JSON
                content = updateJson
            }.andExpect {
                status { isOk() }
                jsonPath("$.email") { value("hacked-$emailA") }
            }

        // Ensure userB was not modified
        val afterUserB = requireNotNull(userRepository.findById(requireNotNull(userB.id)).orElse(null))
        assertEquals(emailB, afterUserB.mail)
    }

    @Test
    fun `should not allow updating another user's email by sending their id and email in request`() {
        // Create two users
        val emailA = "usera2-${System.currentTimeMillis()}@example.com"
        val emailB = "userb2-${System.currentTimeMillis()}@example.com"

        registerUser(emailA)
        registerUser(emailB)

        val userA = requireNotNull(userRepository.findByMail(emailA))
        val userB = requireNotNull(userRepository.findByMail(emailB))

        // Login as user A
        val tokenA = loginAndGetToken(emailA)

        // Attempt to send a PATCH with userB's id and userB's email
        val updateJson = "{\"id\":${userB.id}, \"email\":\"${userB.mail}\"}"

        mockMvc
            .patch("/api/users/me") {
                header("Authorization", "Bearer $tokenA")
                contentType = MediaType.APPLICATION_JSON
                content = updateJson
            }.andExpect {
                status { isConflict() }
            }

        // Ensure both users remain unchanged after conflict
        val afterUserA = requireNotNull(userRepository.findById(requireNotNull(userA.id)).orElse(null))
        val afterUserB = requireNotNull(userRepository.findById(requireNotNull(userB.id)).orElse(null))
        assertEquals(emailA, afterUserA.mail)
        assertEquals(emailB, afterUserB.mail)
    }
}
