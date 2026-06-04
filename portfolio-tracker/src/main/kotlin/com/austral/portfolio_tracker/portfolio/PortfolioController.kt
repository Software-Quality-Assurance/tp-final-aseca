package com.austral.portfolio_tracker.portfolio

import com.austral.portfolio_tracker.security.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/portfolio")
class PortfolioController(
    private val portfolioService: PortfolioService,
) {
    @PostMapping("/operations")
    fun createOperation(
        authentication: Authentication,
        @Valid @RequestBody request: OperationRequest,
    ): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(portfolioService.createOperation(authentication.userId(), request))

    @GetMapping
    fun getPortfolio(authentication: Authentication) = portfolioService.getPortfolio(authentication.userId())

    @GetMapping("/history")
    fun getHistory(authentication: Authentication) = portfolioService.getHistory(authentication.userId())

    @PatchMapping("/history/{id}")
    fun updateHistory(
        authentication: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateHistoryRequest,
    ) = portfolioService.updateHistory(authentication.userId(), id, request)

    @DeleteMapping("/history/{id}")
    fun deleteHistory(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        portfolioService.deleteHistory(authentication.userId(), id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/value")
    fun getCurrentValue(authentication: Authentication) = portfolioService.getCurrentValue(authentication.userId())

    @GetMapping("/profit-loss")
    fun getProfitLoss(authentication: Authentication) = portfolioService.getProfitLoss(authentication.userId())

    private fun Authentication.userId(): Long = (principal as JwtPrincipal).userId
}
