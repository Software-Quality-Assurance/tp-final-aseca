package com.austral.portfolio_tracker.company

import com.austral.portfolio_tracker.entities.Company
import com.austral.portfolio_tracker.entities.Price
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
        val normalizedTicker = request.ticker.trim().uppercase()

        if (companyRepository.findByTickerAndActiveTrue(normalizedTicker) != null) return CompanyResult.Conflict

        val company =
            Company(ticker = normalizedTicker, companyName = request.companyName.trim()).apply {
                request.price?.let { initialPrice ->
                    prices.add(
                        Price(
                            ticker = normalizedTicker,
                            unityPrice = initialPrice,
                            company = this,
                        ),
                    )
                }
            }

        companyRepository.save(company)
        return CompanyResult.Created(company.toData())
    }

    fun getCompanies(page: Int): CompanyResult {
        if (page < 1) return CompanyResult.InvalidInput

        val pageable = PageRequest.of(page - 1, 10, Sort.by("ticker"))
        val result = companyRepository.findAllByActiveTrue(pageable)

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
        val results = companyRepository.findByCompanyNameContainingIgnoreCaseAndActiveTrue(name.trim())
        return if (results.isEmpty()) CompanyResult.NotFound else CompanyResult.Found(results.map { it.toData() })
    }

    fun searchByTicker(ticker: String): CompanyResult {
        if (ticker.isBlank()) return CompanyResult.InvalidInput
        val results = companyRepository.findByTickerContainingIgnoreCaseAndActiveTrue(ticker.trim())
        return if (results.isEmpty()) CompanyResult.NotFound else CompanyResult.Found(results.map { it.toData() })
    }

    fun deleteCompany(id: Long): CompanyResult {
        val company =
            companyRepository.findById(id).orElse(null)
                ?: return CompanyResult.NotFound
        if (company.active.not()) return CompanyResult.NotFound
        company.active = false
        companyRepository.save(company)
        return CompanyResult.Deleted
    }
}
