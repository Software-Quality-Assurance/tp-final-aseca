package com.austral.portfolio_tracker.company

import com.austral.portfolio_tracker.entities.Company
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByTickerAndActiveTrue(ticker: String): Company?

    fun findAllByActiveTrue(pageable: Pageable): Page<Company>

    fun findByCompanyNameContainingIgnoreCaseAndActiveTrue(name: String): List<Company>

    fun findByTickerContainingIgnoreCaseAndActiveTrue(ticker: String): List<Company>
}
