package com.austral.portfolio_tracker.controller

import com.austral.portfolio_tracker.dto.CreateCompanyRequest
import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.repository.CompanyRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/company")
class CompanyController(
    private val companyRepository: CompanyRepository,
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createCompany(
        @RequestBody request: CreateCompanyRequest,
    ): ResponseEntity<Unit> {
        if (request.ticker != null && request.companyName != null && request.companyPrices != null) {
            val company =
                Company(
                    ticker = request.ticker,
                    companyName = request.companyName,
                    companyPrices = request.companyPrices,
                )
            companyRepository.save(company)
        }
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
