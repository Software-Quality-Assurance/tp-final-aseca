package com.austral.portfolio_tracker.user

import com.austral.portfolio_tracker.dto.RegisterUserRequest
import com.austral.portfolio_tracker.exception.DuplicateUserException
import com.austral.portfolio_tracker.repository.UserRepository
import com.austral.portfolio_tracker.service.UserRegistrationService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class UserRegistrationServiceTests {
    @Autowired
    private lateinit var userRegistrationService: UserRegistrationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `should successfully register a user with valid data`() {
        val request =
            RegisterUserRequest(
                email = "newuser@example.com",
                password = "SecurePassword123!",
            )

        val response = userRegistrationService.register(request)

        Assertions.assertNotNull(response.id)
        Assertions.assertEquals("newuser@example.com", response.email)
        Assertions.assertNotNull(response.id)

        // Verify user was persisted in database
        val savedUser = userRepository.findByMail("newuser@example.com")
        Assertions.assertNotNull(savedUser)
        Assertions.assertEquals("newuser@example.com", savedUser?.mail)
    }

    @Test
    fun `should store password in hashed form`() {
        val plainPassword = "MySecurePassword123!"
        val request =
            RegisterUserRequest(
                email = "hashedpwd@example.com",
                password = plainPassword,
            )

        val response = userRegistrationService.register(request)

        val savedUser = userRepository.findByMail("hashedpwd@example.com")
        Assertions.assertNotNull(savedUser)

        // Password should not be stored as plaintext
        Assertions.assertNotEquals(plainPassword, savedUser?.password)

        // Password should be hashed
        Assertions.assertTrue(passwordEncoder.matches(plainPassword, savedUser?.password))
    }

    @Test
    fun `should not expose password or hash in response`() {
        val request =
            RegisterUserRequest(
                email = "hidden@example.com",
                password = "SecurePassword123!",
            )

        val response = userRegistrationService.register(request)

        // Response should not contain password or hash fields
        Assertions.assertNotNull(response)
        Assertions.assertEquals("hidden@example.com", response.email)

        // Attempt to access password field should fail or be null/empty
        val hasPasswordProperty = response::class.members.any { it.name == "password" }
        Assertions.assertTrue(!hasPasswordProperty, "Response should not expose password field")
    }

    @Test
    fun `should reject duplicate email addresses`() {
        val email = "duplicate@example.com"
        val request1 =
            RegisterUserRequest(
                email = email,
                password = "Password123!",
            )
        val request2 =
            RegisterUserRequest(
                email = email,
                password = "DifferentPassword123!",
            )

        // First registration should succeed
        userRegistrationService.register(request1)

        // Second registration with same email should fail
        assertThrows<DuplicateUserException> {
            userRegistrationService.register(request2)
        }
    }

    @Test
    fun `should treat duplicate email addresses as case insensitive`() {
        val request1 =
            RegisterUserRequest(
                email = "CaseTest@Example.com",
                password = "Password123!",
            )
        val request2 =
            RegisterUserRequest(
                email = "casetest@example.com",
                password = "DifferentPassword123!",
            )

        // First registration should succeed
        userRegistrationService.register(request1)

        // Second registration with different case should also fail
        assertThrows<DuplicateUserException> {
            userRegistrationService.register(request2)
        }
    }

    @Test
    fun `should reject registration with missing email`() {
        val request =
            RegisterUserRequest(
                email = "",
                password = "Password123!",
            )

        assertThrows<IllegalArgumentException> {
            userRegistrationService.register(request)
        }
    }

    @Test
    fun `should reject registration with missing password`() {
        val request =
            RegisterUserRequest(
                email = "user@example.com",
                password = "",
            )

        assertThrows<IllegalArgumentException> {
            userRegistrationService.register(request)
        }
    }

    @Test
    fun `should reject registration with invalid email format`() {
        val request =
            RegisterUserRequest(
                email = "invalid-email-format",
                password = "Password123!",
            )

        assertThrows<IllegalArgumentException> {
            userRegistrationService.register(request)
        }
    }

    @Test
    fun `should create user with unique identifier`() {
        val request1 =
            RegisterUserRequest(
                email = "user1@example.com",
                password = "Password123!",
            )
        val request2 =
            RegisterUserRequest(
                email = "user2@example.com",
                password = "Password123!",
            )

        val response1 = userRegistrationService.register(request1)
        val response2 = userRegistrationService.register(request2)

        Assertions.assertNotNull(response1.id)
        Assertions.assertNotNull(response2.id)
        Assertions.assertNotEquals(response1.id, response2.id)
    }

    @Test
    fun `should initialize empty history and watchlist for new user`() {
        val request =
            RegisterUserRequest(
                email = "newuser2@example.com",
                password = "Password123!",
            )

        userRegistrationService.register(request)

        val savedUser = userRepository.findByMail("newuser2@example.com")
        Assertions.assertNotNull(savedUser)
        Assertions.assertEquals(0, savedUser?.history?.size)
        Assertions.assertEquals(0, savedUser?.watchlist?.size)
    }
}
