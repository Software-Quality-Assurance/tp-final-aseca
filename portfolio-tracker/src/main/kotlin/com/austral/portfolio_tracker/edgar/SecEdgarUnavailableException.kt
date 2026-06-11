package com.austral.portfolio_tracker.edgar

class SecEdgarUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
