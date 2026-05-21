package com.austral.portfolio_tracker.controller

import com.austral.portfolio_tracker.dto.CreateCompanyRequest
import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.repository.CompanyRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class ErrorResponse(
    val error: String,
)

data class PagedResponse<T>(
    val content: List<T>,
    val currentPage: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@RestController
@RequestMapping("/api/company")
class CompanyController(
    private val companyRepository: CompanyRepository,
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createCompany(
        @RequestBody request: CreateCompanyRequest,
    ): ResponseEntity<Any> {
        val ticker = request.ticker
        val companyName = request.companyName

        if (ticker.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }
        if (companyName.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }

        if (companyRepository.findByTicker(ticker) != null) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse("Invalid operation: company with ticker already exists"))
        }

        val company =
            Company(
                ticker = ticker,
                companyName = companyName,
            )
        companyRepository.save(company)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping
    fun getCompanies(
        @RequestParam(required = false, defaultValue = "1") page: Int,
    ): ResponseEntity<PagedResponse<Company>> {
        if (page < 1) return ResponseEntity.badRequest().build()

        val pageable = PageRequest.of(page - 1, 10, Sort.by("ticker"))
        val result = companyRepository.findAll(pageable)

        if (page > result.totalPages && result.totalPages > 0) return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            PagedResponse(
                content = result.content,
                currentPage = page,
                pageSize = 10,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    @GetMapping("/search")
    fun searchCompanies(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) ticker: String?,
    ): ResponseEntity<List<Company>> =
        when {
            !name.isNullOrBlank() -> {
                val results = companyRepository.searchByName(name.trim())
                if (results.isEmpty()) {
                    ResponseEntity.notFound().build()
                } else {
                    ResponseEntity.ok(results)
                }
            }
            !ticker.isNullOrBlank() -> {
                val results = companyRepository.searchByTicker(ticker.trim())
                if (results.isEmpty()) {
                    ResponseEntity.notFound().build()
                } else {
                    ResponseEntity.ok(results)
                }
            }
            else -> ResponseEntity.badRequest().build()
        }

    @DeleteMapping("/{id}")
    fun deleteCompany(
        @PathVariable id: Long,
    ): ResponseEntity<Any> {
        val company =
            companyRepository.findById(id).orElse(null)
                ?: return ResponseEntity.notFound().build()

        companyRepository.delete(company)
        return ResponseEntity.noContent().build()
    }
}
