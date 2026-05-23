package com.austral.portfolio_tracker.repository

import com.austral.portfolio_tracker.entity.Company
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByTicker(ticker: String): Company?

    override fun findAll(pageable: Pageable): Page<Company>

    fun findByCompanyNameContainingIgnoreCase(name: String): List<Company>

    fun findByTickerContainingIgnoreCase(ticker: String): List<Company>
}
