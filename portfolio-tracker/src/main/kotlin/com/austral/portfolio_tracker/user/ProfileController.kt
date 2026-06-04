package com.austral.portfolio_tracker.user

import com.austral.portfolio_tracker.entities.User
import com.austral.portfolio_tracker.security.JwtPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class ProfileController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @GetMapping("/me")
    fun me(authentication: Authentication?): ResponseEntity<UserResponse> {
        val principal = authentication?.principal as? JwtPrincipal ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val user = userRepository.findById(principal.userId).orElse(null) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            UserResponse(
                id = requireNotNull(user.id),
                email = user.mail,
            ),
        )
    }

    @PatchMapping("/me", consumes = ["application/json"], produces = ["application/json"])
    fun updateMe(
        authentication: Authentication?,
        @jakarta.validation.Valid
        @org.springframework.web.bind.annotation.RequestBody request: UpdateUserRequest,
    ): ResponseEntity<UserResponse> {
        val principal = authentication?.principal as? JwtPrincipal ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val user = userRepository.findById(principal.userId).orElse(null) ?: return ResponseEntity.notFound().build()

        // Update email if provided
        val newEmail = request.email?.trim()?.lowercase()
        if (newEmail != null) {
            if (newEmail.isBlank()) {
                throw IllegalArgumentException("Email cannot be empty")
            }
            if (userRepository.existsByMailIgnoreCase(newEmail) && newEmail != user.mail) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build()
            }
        }

        // Update password if provided
        val newPasswordHash =
            request.password?.let {
                if (it.isBlank()) throw IllegalArgumentException("Password cannot be empty")
                passwordEncoder.encode(it)
            } ?: user.password

        val updatedUser =
            User(
                id = requireNotNull(user.id),
                mail = newEmail ?: user.mail,
                password = newPasswordHash,
                history = user.history,
                watchlist = user.watchlist,
            )

        val saved = userRepository.save(updatedUser)

        return ResponseEntity.ok(UserResponse(id = requireNotNull(saved.id), email = saved.mail))
    }

    @DeleteMapping("/me")
    fun deleteMe(authentication: Authentication?): ResponseEntity<Any> {
        val principal = authentication?.principal as? JwtPrincipal ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val user = userRepository.findById(principal.userId).orElse(null) ?: return ResponseEntity.notFound().build()

        userRepository.deleteById(requireNotNull(user.id))
        return ResponseEntity.noContent().build()
    }
}
