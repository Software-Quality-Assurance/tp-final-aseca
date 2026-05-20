package com.austral.portfolio_tracker.controller

import com.austral.portfolio_tracker.dto.RegisterUserRequest
import com.austral.portfolio_tracker.dto.UserResponse
import com.austral.portfolio_tracker.exception.DuplicateUserException
import com.austral.portfolio_tracker.exception.InvalidCredentialsException
import com.austral.portfolio_tracker.repository.UserRepository
import com.austral.portfolio_tracker.service.UserRegistrationService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRegistrationService: UserRegistrationService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper,
) {
    private companion object {
        const val JWT_SECRET = "portfolio-tracker-dev-secret"
        const val JWT_ISSUER = "portfolio-tracker"
        const val JWT_EXPIRATION_SECONDS = 60 * 60
    }

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

        return ResponseEntity.ok(mapOf("token" to createJwt(user.id, user.mail)))
    }

    private fun createJwt(userId: Long?, email: String): String {
        val header = mapOf("alg" to "HS256", "typ" to "JWT")
        val now = Instant.now().epochSecond
        val payload = mapOf(
            "iss" to JWT_ISSUER,
            "sub" to requireNotNull(userId).toString(),
            "email" to email,
            "iat" to now,
            "exp" to (now + JWT_EXPIRATION_SECONDS),
        )

        val encodedHeader = encodeJson(header)
        val encodedPayload = encodeJson(payload)
        val signingInput = "$encodedHeader.$encodedPayload"
        val signature = sign(signingInput)

        return "$signingInput.$signature"
    }

    private fun encodeJson(value: Any): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value))

    private fun sign(signingInput: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(JWT_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(signingInput.toByteArray(Charsets.UTF_8)))
    }

    @ExceptionHandler(DuplicateUserException::class)
    @Suppress("UNUSED_PARAMETER")
    fun handleDuplicate(e: DuplicateUserException): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.CONFLICT).build()

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(e: InvalidCredentialsException): ResponseEntity<Map<String, String>> {
        val body = mapOf("error" to (e.message ?: "Invalid credentials"))
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        val body = mapOf("error" to (e.message ?: "Bad request"))
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }
}

