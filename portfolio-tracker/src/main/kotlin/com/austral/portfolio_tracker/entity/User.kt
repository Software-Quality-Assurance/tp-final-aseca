package com.austral.portfolio_tracker.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Entity
data class User(
    @Id
    @NotBlank
    val id: String = UUID.randomUUID().toString(),
    @NotBlank
    val name: String,
    @NotBlank
    val lastName: String,
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
    }
}