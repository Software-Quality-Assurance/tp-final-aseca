package com.austral.portfolio_tracker.watchlist

import com.austral.portfolio_tracker.company.CompanyRepository
import com.austral.portfolio_tracker.entities.Watchlist
import com.austral.portfolio_tracker.exception.DuplicateUserException
import com.austral.portfolio_tracker.exception.ResourceNotFoundException
import com.austral.portfolio_tracker.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WatchlistService(
    private val watchlistRepository: WatchlistRepository,
    private val userRepository: UserRepository,
    private val companyRepository: CompanyRepository,
) {
    @Transactional(readOnly = true)
    fun getWatchlist(userId: Long): WatchlistResponse {
        val items =
            watchlistRepository.findByUserId(userId).map {
                val company = it.company!!
                val lastPrice = company.prices.maxByOrNull { p -> p.timestamp }
                WatchlistItemResponse(
                    id = it.id!!,
                    ticker = company.ticker,
                    companyName = company.companyName,
                    currentPrice = lastPrice?.unityPrice,
                    lastUpdatedAt = lastPrice?.timestamp,
                )
            }
        return WatchlistResponse(items)
    }

    @Transactional
    fun addToWatchlist(
        userId: Long,
        ticker: String,
    ) {
        val normalizedTicker = ticker.trim().uppercase()

        val company =
            companyRepository.findByTickerAndActiveTrue(normalizedTicker)
                ?: throw ResourceNotFoundException("Company with ticker $normalizedTicker not found")

        if (watchlistRepository.existsByUserIdAndCompanyTicker(userId, normalizedTicker)) {
            throw DuplicateUserException("Company already in watchlist") // Reuse Conflict exception
        }

        val user =
            userRepository.findById(userId).orElseThrow {
                ResourceNotFoundException("User not found")
            }

        val watchEntry =
            Watchlist(
                user = user,
                company = company,
            )
        watchlistRepository.save(watchEntry)
    }

    @Transactional
    fun removeFromWatchlist(
        userId: Long,
        ticker: String,
    ) {
        val normalizedTicker = ticker.trim().uppercase()
        val entry =
            watchlistRepository
                .findByUserIdAndCompanyTicker(userId, normalizedTicker)
                .orElseThrow { ResourceNotFoundException("Watchlist entry not found for ticker $normalizedTicker") }

        watchlistRepository.delete(entry)
    }
}
