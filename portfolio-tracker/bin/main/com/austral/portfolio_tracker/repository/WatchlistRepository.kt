package com.austral.portfolio_tracker.repository

import com.austral.portfolio_tracker.entity.Watchlist
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface WatchlistRepository : JpaRepository<Watchlist, Long> {
    fun findByUserId(userId: Long): List<Watchlist>

    fun findByUserIdAndCompanyTicker(
        userId: Long,
        ticker: String,
    ): Optional<Watchlist>

    fun existsByUserIdAndCompanyTicker(
        userId: Long,
        ticker: String,
    ): Boolean
}
