package com.austral.portfolio_tracker.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/company")
class CompanyController {
    @PostMapping
    fun createCompany(): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.CREATED).build()
}
