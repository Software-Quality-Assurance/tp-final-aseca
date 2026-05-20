package com.austral.portfolio_tracker.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class ProfileController {
    @GetMapping("/me")
    fun me(
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
    ): ResponseEntity<Map<String, String>> {
        if (authorization != "Bearer token") {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}

