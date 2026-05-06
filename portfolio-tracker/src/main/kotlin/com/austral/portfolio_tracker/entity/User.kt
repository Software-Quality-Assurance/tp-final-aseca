package com.austral.portfolio_tracker.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @field:NotBlank(message = "First name cannot be empty")
    @field:Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Column(nullable = false)
    val firstName: String,
    @field:NotBlank(message = "Last name cannot be empty")
    @field:Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Column(nullable = false)
    val lastName: String,
    @field:NotBlank(message = "Email cannot be empty")
    @field:Email(message = "Email must be valid")
    @Column(nullable = false, unique = true)
    val email: String,
    @field:NotBlank(message = "Password cannot be empty")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @Column(nullable = false)
    val passwordHash: String,
)
