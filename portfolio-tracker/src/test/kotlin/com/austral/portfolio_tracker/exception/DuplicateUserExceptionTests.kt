package com.austral.portfolio_tracker.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DuplicateUserExceptionTests {
    @Test
    fun `should create exception with message`() {
        val message = "User with email already exists"
        val exception = DuplicateUserException(message)

        assertNotNull(exception)
        assertEquals(message, exception.message)
    }

    @Test
    fun `should extend RuntimeException`() {
        assert(RuntimeException::class.java.isAssignableFrom(DuplicateUserException::class.java))
    }

    @Test
    fun `should be throwable`() {
        val exception = DuplicateUserException("Email already registered")

        try {
            throw exception
        } catch (e: DuplicateUserException) {
            assertEquals("Email already registered", e.message)
        }
    }
}
