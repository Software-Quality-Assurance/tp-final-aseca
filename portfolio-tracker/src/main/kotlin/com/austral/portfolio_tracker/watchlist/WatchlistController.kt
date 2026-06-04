package com.austral.portfolio_tracker.watchlist

import com.austral.portfolio_tracker.security.JwtPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/watchlist")
class WatchlistController(
    private val watchlistService: WatchlistService,
) {
    @GetMapping
    fun getWatchlist(authentication: Authentication): ResponseEntity<WatchlistResponse> {
        val principal = authentication.principal as JwtPrincipal
        return ResponseEntity.ok(watchlistService.getWatchlist(principal.userId))
    }

    @PostMapping("/{ticker}")
    fun addToWatchlist(
        @PathVariable ticker: String,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val principal = authentication.principal as JwtPrincipal
        watchlistService.addToWatchlist(principal.userId, ticker)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/{ticker}")
    fun removeFromWatchlist(
        @PathVariable ticker: String,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val principal = authentication.principal as JwtPrincipal
        watchlistService.removeFromWatchlist(principal.userId, ticker)
        return ResponseEntity.noContent().build()
    }
}
