package com.austral.portfolio_tracker.service

import com.austral.portfolio_tracker.dto.CreateCompanyRequest
import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.repository.CompanyRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

data class CompanyData(
    val id: Long,
    val ticker: String,
    val companyName: String,
)

data class PagedResult(
    val content: List<CompanyData>,
    val currentPage: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
)

sealed class CompanyResult {
    data class Created(
        val company: CompanyData,
    ) : CompanyResult()

    data class Found(
        val companies: List<CompanyData>,
    ) : CompanyResult()

    data class Paged(
        val result: PagedResult,
    ) : CompanyResult()

    object Deleted : CompanyResult()

    object NotFound : CompanyResult()

    object Conflict : CompanyResult()

    object InvalidInput : CompanyResult()

    object PageOutOfRange : CompanyResult()
}

private fun Company.toData() = CompanyData(id = id!!, ticker = ticker, companyName = companyName)

@Service
class CompanyService(
    private val companyRepository: CompanyRepository,
) {
    fun createCompany(request: CreateCompanyRequest): CompanyResult {
        val ticker = request.ticker
        val companyName = request.companyName

        if (ticker.isNullOrBlank() || companyName.isNullOrBlank()) return CompanyResult.InvalidInput

        val normalizedTicker = ticker.trim().uppercase()

        if (companyRepository.findByTicker(normalizedTicker) != null) return CompanyResult.Conflict

        val company = companyRepository.save(Company(ticker = normalizedTicker, companyName = companyName.trim()))
        return CompanyResult.Created(company.toData())
    }

    fun getCompanies(page: Int): CompanyResult {
        if (page < 1) return CompanyResult.InvalidInput

        val pageable = PageRequest.of(page - 1, 10, Sort.by("ticker"))
        val result = companyRepository.findAll(pageable)

        if (result.totalPages in 1..<page) return CompanyResult.PageOutOfRange

        return CompanyResult.Paged(
            PagedResult(
                content = result.content.map { it.toData() },
                currentPage = page,
                pageSize = 10,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    fun searchByName(name: String): CompanyResult {
        if (name.isBlank()) return CompanyResult.InvalidInput
        val results = companyRepository.findByCompanyNameContainingIgnoreCase(name.trim())
        return if (results.isEmpty()) CompanyResult.NotFound else CompanyResult.Found(results.map { it.toData() })
    }

    fun searchByTicker(ticker: String): CompanyResult {
        if (ticker.isBlank()) return CompanyResult.InvalidInput
        val results = companyRepository.findByTickerContainingIgnoreCase(ticker.trim())
        return if (results.isEmpty()) CompanyResult.NotFound else CompanyResult.Found(results.map { it.toData() })
    }

    fun deleteCompany(id: Long): CompanyResult {
        val company = companyRepository.findById(id).orElse(null) ?: return CompanyResult.NotFound
        companyRepository.delete(company)
        return CompanyResult.Deleted
    }
}
