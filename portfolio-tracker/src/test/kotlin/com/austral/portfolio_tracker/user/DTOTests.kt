package com.austral.portfolio_tracker.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterUserRequestTests {
    @Test
    fun `should create RegisterUserRequest with email and password`() {
        val email = "user@example.com"
        val password = "Password123!"

        val request =
            RegisterUserRequest(
                email = email,
                password = password,
            )

        assertNotNull(request)
        assertEquals(email, request.email)
        assertEquals(password, request.password)
    }

    @Test
    fun `should accept various valid email formats`() {
        val validEmails =
            listOf(
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "user123@test-domain.com",
            )

        validEmails.forEach { email ->
            val request =
                RegisterUserRequest(
                    email = email,
                    password = "Password123!",
                )
            assertEquals(email, request.email)
        }
    }

    @Test
    fun `should store password as provided`() {
        val passwords =
            listOf(
                "SecurePassword123!",
                "SuperStrong@Pass2024",
                "VeryLongPasswordWith32CharactersTotal!",
            )

        passwords.forEach { password ->
            val request =
                RegisterUserRequest(
                    email = "user@example.com",
                    password = password,
                )
            assertEquals(password, request.password)
        }
    }
}

class UserResponseTests {
    @Test
    fun `should create UserResponse with id and email`() {
        val id = 1L
        val email = "user@example.com"

        val response =
            UserResponse(
                id = id,
                email = email,
            )

        assertNotNull(response)
        assertEquals(id, response.id)
        assertEquals(email, response.email)
    }

    @Test
    fun `should not expose password field`() {
        val response =
            UserResponse(
                id = 1L,
                email = "user@example.com",
            )

        // Verify password field does not exist on response
        val hasPasswordProperty = response::class.members.any { it.name == "password" }
        assertFalse(hasPasswordProperty, "UserResponse should not contain password field")
    }

    @Test
    fun `should contain only safe fields`() {
        val response =
            UserResponse(
                id = 1L,
                email = "user@example.com",
            )

        val memberNames = response::class.members.map { it.name }

        assertNotNull(response.id)
        assertNotNull(response.email)

        // Only id and email should be exposed
        assertTrue(memberNames.contains("id"))
        assertTrue(memberNames.contains("email"))
        assertFalse(memberNames.contains("password"))
    }

    @Test
    fun `should handle various user ids`() {
        val ids = listOf(1L, 100L, 9999L, Long.MAX_VALUE)

        ids.forEach { id ->
            val response =
                UserResponse(
                    id = id,
                    email = "user@example.com",
                )
            assertEquals(id, response.id)
        }
    }
}
