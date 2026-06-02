package com.austral.portfolio_tracker.exception

class DuplicateUserException(
    message: String = "User already exists",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
