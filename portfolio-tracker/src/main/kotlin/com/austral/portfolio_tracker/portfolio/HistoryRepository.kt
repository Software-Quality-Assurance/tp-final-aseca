package com.austral.portfolio_tracker.portfolio

import com.austral.portfolio_tracker.entities.History
import org.springframework.data.jpa.repository.JpaRepository

interface HistoryRepository : JpaRepository<History, Long> {
    fun findByUserIdOrderByTimestampDesc(userId: Long): List<History>

    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): History?
}
