package com.austral.portfolio_tracker.repository

import com.austral.portfolio_tracker.entity.History
import org.springframework.data.jpa.repository.JpaRepository

interface HistoryRepository : JpaRepository<History, Long> {
    fun findByUserIdOrderByTimestampDesc(userId: Long): List<History>

    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): History?
}
