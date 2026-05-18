package com.austral.portfolio_tracker.repository

import com.austral.portfolio_tracker.entity.Watchlist
import org.springframework.data.jpa.repository.JpaRepository

interface WatchlistRepository : JpaRepository<Watchlist, Long>