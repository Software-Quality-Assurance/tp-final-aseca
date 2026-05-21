package com.austral.portfolio_tracker.repository

import com.austral.portfolio_tracker.entity.Company
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByTicker(ticker: String): Company?

    override fun findAll(pageable: Pageable): Page<Company>

    @Query("SELECT c FROM Company c WHERE LOWER(c.companyName) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun searchByName(name: String): List<Company>

    @Query("SELECT c FROM Company c WHERE LOWER(c.ticker) LIKE LOWER(CONCAT('%', :ticker, '%'))")
    fun searchByTicker(ticker: String): List<Company>
}
