package com.austral.portfolio_tracker.controller

import com.austral.portfolio_tracker.dto.RegisterUserRequest
import com.austral.portfolio_tracker.dto.UserResponse
import com.austral.portfolio_tracker.exception.DuplicateUserException
import com.austral.portfolio_tracker.service.UserRegistrationService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class UserController(
	private val userRegistrationService: UserRegistrationService
) {

	@PostMapping("/register", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
	fun register(@RequestBody request: RegisterUserRequest): ResponseEntity<UserResponse> {
		// Basic password strength check (keeps controller behavior minimal for the tests).
		if (request.password.length < 8) {
			throw IllegalArgumentException("Password too weak")
		}

		val response = userRegistrationService.register(request)
		return ResponseEntity.status(HttpStatus.CREATED).body(response)
	}

	@ExceptionHandler(DuplicateUserException::class)
	fun handleDuplicate(e: DuplicateUserException): ResponseEntity<Any> {
		return ResponseEntity.status(HttpStatus.CONFLICT).build()
	}

	@ExceptionHandler(IllegalArgumentException::class)
	fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> {
		val body = mapOf("error" to (e.message ?: "Bad request"))
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
	}
}