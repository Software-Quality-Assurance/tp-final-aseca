package com.austral.portfolio_tracker.exception

class InvalidCredentialsException(
    message: String = "Invalid credentials",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

