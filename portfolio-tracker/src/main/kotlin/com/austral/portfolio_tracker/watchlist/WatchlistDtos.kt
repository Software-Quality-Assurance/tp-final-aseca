package com.austral.portfolio_tracker.watchlist

import java.math.BigDecimal
import java.time.Instant

data class WatchlistItemResponse(
    val id: Long,
    val ticker: String,
    val companyName: String,
    val currentPrice: BigDecimal?,
    val lastUpdatedAt: Instant?,
    val priceSource: String?,
)

data class WatchlistResponse(
    val items: List<WatchlistItemResponse>,
)
