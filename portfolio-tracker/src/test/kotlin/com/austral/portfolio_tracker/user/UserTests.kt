package com.austral.portfolio_tracker.user

import com.austral.portfolio_tracker.entity.User
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertNotNull
import kotlin.test.Test

class UserTests {

    // Create user
    @Test
    fun `001 should create a new user`() {
        val user = User("1", "John", "Doe", "johndoe@gmail.com", "mysecretpassword")
        assertNotNull(user)
    }

    @Test
    fun `002 new user must have id`() {
        val user = User("1", "John", "Doe", "johndoe@gmail.com", "mysecretpassword")
        assertNotNull(user.id)
    }

    @Test
    fun `003 new user must not have an empty field and should throw an exception`() {
        assertThrows(IllegalArgumentException::class.java) {  User(" ", " ", " ", " ", " ") }
    }



}