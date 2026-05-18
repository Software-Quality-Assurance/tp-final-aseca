package com.austral.portfolio_tracker.repository

import com.austral.portfolio_tracker.entity.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByTicker(ticker: String): Company?
}
