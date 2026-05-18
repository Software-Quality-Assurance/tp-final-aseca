package com.austral.portfolio_tracker.exception

class DuplicateUserException(
    message: String = "User already exists",
) : RuntimeException(message)
