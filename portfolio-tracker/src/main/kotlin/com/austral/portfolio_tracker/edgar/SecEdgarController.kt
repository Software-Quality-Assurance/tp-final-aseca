package com.austral.portfolio_tracker.edgar

import com.austral.portfolio_tracker.security.JwtPrincipal
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/edgar")
class SecEdgarController(
    private val service: SecEdgarService,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
    ) = service.search(query)

    @GetMapping("/companies/{ticker}/metrics")
    fun metrics(
        @PathVariable ticker: String,
    ) = service.metrics(ticker)

    @GetMapping("/companies/{ticker}/filings")
    fun filings(
        @PathVariable ticker: String,
    ) = service.filings(ticker)

    @GetMapping("/companies/{ticker}/history")
    fun history(
        @PathVariable ticker: String,
        @RequestParam metric: String,
        @RequestParam(defaultValue = "8") quarters: Int,
    ) = service.history(ticker, parseMetric(metric), quarters)

    @GetMapping("/comparison")
    fun comparison(authentication: Authentication) = service.comparison((authentication.principal as JwtPrincipal).userId)

    private fun parseMetric(metric: String): FinancialMetric =
        try {
            FinancialMetric.valueOf(metric.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid financial metric")
        }
}
