package com.austral.portfolio_tracker.dto

import java.math.BigDecimal
import java.time.Instant

data class WatchlistItemResponse(
    val id: Long,
    val ticker: String,
    val companyName: String,
    val currentPrice: BigDecimal?,
    val lastUpdatedAt: Instant?,
)

data class WatchlistResponse(
    val items: List<WatchlistItemResponse>,
)
