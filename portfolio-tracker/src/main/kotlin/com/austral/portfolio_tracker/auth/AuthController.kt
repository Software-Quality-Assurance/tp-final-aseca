package com.austral.portfolio_tracker.auth

import com.austral.portfolio_tracker.exception.InvalidCredentialsException
import com.austral.portfolio_tracker.security.JwtTokenService
import com.austral.portfolio_tracker.user.RegisterUserRequest
import com.austral.portfolio_tracker.user.UserRegistrationService
import com.austral.portfolio_tracker.user.UserRepository
import com.austral.portfolio_tracker.user.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRegistrationService: UserRegistrationService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
) {
    @PostMapping("/register", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun register(
        @RequestBody request: RegisterUserRequest,
    ): ResponseEntity<UserResponse> {
        // Basic password strength check (keeps controller behavior minimal for the tests).
        if (request.password.length < 8) {
            throw IllegalArgumentException("Password too weak")
        }

        val response = userRegistrationService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun login(
        @RequestBody request: Map<String, String>,
    ): ResponseEntity<Map<String, String>> {
        val email = request["email"].orEmpty().trim().lowercase()
        val password = request["password"].orEmpty()
        val user = userRepository.findByMailIgnoreCase(email) ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(password, user.password)) {
            throw InvalidCredentialsException()
        }

        return ResponseEntity.ok(mapOf("token" to jwtTokenService.generateToken(requireNotNull(user.id), user.mail)))
    }
}
