package com.austral.portfolio_tracker.controller

import com.austral.portfolio_tracker.dto.CreateCompanyRequest
import com.austral.portfolio_tracker.service.CompanyResult
import com.austral.portfolio_tracker.service.CompanyService
import jakarta.validation.Valid
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

data class CompanyDTO(
    val id: Long,
    val ticker: String,
    val companyName: String,
)

data class ErrorResponse(
    val error: String,
)

data class PagedResponse(
    val content: List<CompanyDTO>,
    val currentPage: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@RestController
@RequestMapping("/api/company")
class CompanyController(
    private val companyService: CompanyService,
) {
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createCompany(
        @Valid @RequestBody request: CreateCompanyRequest,
    ): ResponseEntity<Any> =
        when (companyService.createCompany(request)) {
            is CompanyResult.Created -> ResponseEntity.status(HttpStatus.CREATED).build()
            is CompanyResult.Conflict ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ErrorResponse("Invalid operation: company with ticker already exists"))
            else -> ResponseEntity.badRequest().build()
        }

    @GetMapping
    fun getCompanies(
        @RequestParam(required = false, defaultValue = "1") page: Int,
    ): ResponseEntity<PagedResponse> =
        when (val result = companyService.getCompanies(page)) {
            is CompanyResult.Paged ->
                ResponseEntity.ok(
                    PagedResponse(
                        content = result.result.content.map { CompanyDTO(it.id, it.ticker, it.companyName) },
                        currentPage = result.result.currentPage,
                        pageSize = result.result.pageSize,
                        totalElements = result.result.totalElements,
                        totalPages = result.result.totalPages,
                    ),
                )
            is CompanyResult.PageOutOfRange -> ResponseEntity.notFound().build()
            else -> ResponseEntity.badRequest().build()
        }

    @GetMapping("/search")
    fun searchCompanies(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) ticker: String?,
    ): ResponseEntity<List<CompanyDTO>> {
        val trimmedName = name?.trim()
        val trimmedTicker = ticker?.trim()

        val result =
            when {
                trimmedName.isNullOrBlank().not() -> companyService.searchByName(trimmedName!!)
                trimmedTicker.isNullOrBlank().not() -> companyService.searchByTicker(trimmedTicker!!)
                else -> return ResponseEntity.badRequest().build()
            }

        return when (result) {
            is CompanyResult.Found -> ResponseEntity.ok(result.companies.map { CompanyDTO(it.id, it.ticker, it.companyName) })
            is CompanyResult.NotFound -> ResponseEntity.ok(emptyList())
            else -> ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCompany(
        @PathVariable id: Long,
    ): ResponseEntity<Any> =
        when (companyService.deleteCompany(id)) {
            is CompanyResult.Deleted -> ResponseEntity.noContent().build()
            is CompanyResult.NotFound -> ResponseEntity.notFound().build()
            else -> ResponseEntity.badRequest().build()
        }
}
