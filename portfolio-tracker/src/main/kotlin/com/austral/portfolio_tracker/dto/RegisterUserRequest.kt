package com.austral.portfolio_tracker.dto

data class RegisterUserRequest(
    val email: String = "",
    val password: String = "",
)
