package com.austral.portfolio_tracker.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

@Entity
data class User(
    @Id
    @NotBlank
    val id: String = UUID.randomUUID().toString(),
    @NotBlank
    val name: String,
    @NotBlank
    val lastName: String,
    @Email
    @NotBlank
    val email: String,
    @NotBlank
    val password: String
)
{
    init {
        require(id.isNotBlank()) { "Id must not be blank" }
        require(name.isNotBlank()) { "Name must not be blank" }
        require(lastName.isNotBlank()) { "Surname must not be blank" }
        require(email.isNotBlank()) { "Email must not be blank" }
        require(password.isNotBlank()) { "Password must not be blank" }
        require(emailRegex.matches(email)) { "Email must be valid" }
    }
}