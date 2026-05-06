package com.austral.portfolio_tracker.user

import com.austral.portfolio_tracker.entity.User
import jakarta.persistence.Entity
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
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
    fun `003 user must be a JPA entity`() {
        val entityAnnotation = User::class.java.getAnnotation(Entity::class.java)
        assertNotNull(entityAnnotation)
    }

    @Test
    fun `004 new user must not have an empty field and should throw an exception`() {
        assertThrows(IllegalArgumentException::class.java) {  User(" ", " ", " ", " ", " ") }
    }

    // Each specified field

    // id
    @Test
    fun `005 id must be generated automatically`() {
        val user = User(name = "John", lastName = "Doe", email = "johndoe@gmail.com", password = "mysecretpassword")
        assertNotNull(user.id)
    }

    // email
    @Test
    fun `006 a invalid email field should not throw an exception`() {
        assertThrows(IllegalArgumentException::class.java) {  User(name = "John", lastName = "Doe", email = "johndoe", password = "mysecretpassword") }
    }

    @Test
    fun `007 a valid email field should not throw an exception`() {
        assertDoesNotThrow { User(name = "John", lastName = "Doe", email = "johndoe@gmail.com", password = "mysecretpassword") }
    }





}