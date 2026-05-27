package com.austral.portfolio_tracker.user

import com.austral.portfolio_tracker.dto.RegisterUserRequest
import com.austral.portfolio_tracker.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserRegistrationControllerTests {
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
        Assertions.assertNotNull(content)
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
        Assertions.assertNotNull(savedUser)
        Assertions.assertEquals(email, savedUser?.mail)
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

        Assertions.assertEquals(3, userRepository.findAll().count { it.mail.startsWith("sequential") })
    }
}
