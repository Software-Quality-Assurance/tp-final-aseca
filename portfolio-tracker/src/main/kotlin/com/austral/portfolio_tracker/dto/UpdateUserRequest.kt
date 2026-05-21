package com.austral.portfolio_tracker.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class UpdateUserRequest(
    @field:Email(message = "Invalid email")
    val email: String? = null,
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String? = null,
)
