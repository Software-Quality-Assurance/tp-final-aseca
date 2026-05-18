package com.austral.portfolio_tracker.service

import com.austral.portfolio_tracker.dto.RegisterUserRequest
import com.austral.portfolio_tracker.dto.UserResponse
import com.austral.portfolio_tracker.entity.User
import com.austral.portfolio_tracker.exception.DuplicateUserException
import com.austral.portfolio_tracker.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private val EMAIL_REGEX =
            Pattern.compile(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$",
            )
    }

    fun register(request: RegisterUserRequest): UserResponse {
        // Validate email and password are not empty
        if (request.email.isBlank()) {
            throw IllegalArgumentException("Email cannot be empty")
        }
        if (request.password.isBlank()) {
            throw IllegalArgumentException("Password cannot be empty")
        }

        // Validate email format
        if (!EMAIL_REGEX.matcher(request.email).matches()) {
            throw IllegalArgumentException("Invalid email format")
        }

        // Check for duplicate (case-insensitive)
        val existingUser = userRepository.findByMail(request.email)
        if (existingUser != null) {
            throw DuplicateUserException("User with email ${request.email} already exists")
        }

        // Check if user exists with different case (case-insensitive search)
        val allUsers = userRepository.findAll()
        val duplicateByCase =
            allUsers.any {
                it.mail.lowercase() == request.email.lowercase() &&
                    it.mail != request.email
            }
        if (duplicateByCase) {
            throw DuplicateUserException("User with email ${request.email} already exists")
        }

        // Create and save user with hashed password
        val hashedPassword: String = requireNotNull(passwordEncoder.encode(request.password))
        val newUser =
            User(
                mail = request.email,
                password = hashedPassword,
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )
        val savedUser = userRepository.save(newUser)

        // Return response without exposing password
        return UserResponse(
            id = savedUser.id,
            email = savedUser.mail,
        )
    }
}
