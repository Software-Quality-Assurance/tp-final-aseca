package com.austral.portfolio_tracker.repository
import com.austral.portfolio_tracker.entity.Price
import org.springframework.data.jpa.repository.JpaRepository

interface PriceRepository : JpaRepository<Price, Long> {
    fun findByTicker(ticker: String): List<Price>

    fun findByCompanyIdOrderByTimestampAsc(companyId: Long): List<Price>
}
