package com.austral.portfolio_tracker.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterUserRequest(
    @field:Email(message = "Invalid email")
    @field:NotBlank(message = "Email must not be blank")
    val email: String,
    @field:NotBlank(message = "Password must not be blank")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String,
)
