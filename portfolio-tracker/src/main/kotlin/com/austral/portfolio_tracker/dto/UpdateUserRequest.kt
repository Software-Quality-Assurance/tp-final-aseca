package com.austral.portfolio_tracker.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MIN_LENGTH_MESSAGE = "Password must be at least 8 characters"

data class UpdateUserRequest(
    @field:Email(message = "Invalid email")
    val email: String?,
    @field:Size(min = PASSWORD_MIN_LENGTH, message = PASSWORD_MIN_LENGTH_MESSAGE)
    val password: String?,
)
