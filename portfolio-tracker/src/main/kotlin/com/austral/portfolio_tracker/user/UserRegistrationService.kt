package com.austral.portfolio_tracker.user

import com.austral.portfolio_tracker.entities.User
import com.austral.portfolio_tracker.exception.DuplicateUserException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.regex.Pattern

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private val EMAIL_REGEX =
            Pattern.compile(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            )
    }

    @Transactional
    fun register(request: RegisterUserRequest): UserResponse {
        val normalizedEmail = request.email.trim().lowercase()

        // Validate email and password are not empty
        if (normalizedEmail.isBlank()) {
            throw IllegalArgumentException("Email cannot be empty")
        }
        if (request.password.isBlank()) {
            throw IllegalArgumentException("Password cannot be empty")
        }

        // Validate email format
        if (!EMAIL_REGEX.matcher(normalizedEmail).matches()) {
            throw IllegalArgumentException("Invalid email format")
        }

        if (userRepository.existsByMailIgnoreCase(normalizedEmail)) {
            throw DuplicateUserException("User with email ${request.email} already exists")
        }

        // Create and save user with hashed password
        val hashedPassword: String = requireNotNull(passwordEncoder.encode(request.password))
        val newUser =
            User(
                mail = normalizedEmail,
                password = hashedPassword,
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )
        val savedUser =
            try {
                userRepository.save(newUser)
            } catch (_: DataIntegrityViolationException) {
                throw DuplicateUserException("User with email ${request.email} already exists")
            }

        // Return response without exposing password
        return UserResponse(
            id = requireNotNull(savedUser.id),
            email = savedUser.mail,
        )
    }
}
