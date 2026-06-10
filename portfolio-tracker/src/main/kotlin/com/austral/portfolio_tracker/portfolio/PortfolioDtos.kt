package com.austral.portfolio_tracker.portfolio

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class OperationRequest(
    @field:NotBlank(message = "Ticker is required")
    val ticker: String,
    @field:NotBlank(message = "Transaction type is required")
    val type: String,
    @field:Min(value = 1, message = "Quantity must be greater than zero")
    val quantity: Int,
)

data class UpdateHistoryRequest(
    val type: String? = null,
    @field:Min(value = 1, message = "Quantity must be greater than zero")
    val quantity: Int? = null,
)

data class OperationResponse(
    val id: Long,
    val ticker: String,
    val companyName: String,
    val type: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val timestamp: Instant,
)

data class PortfolioPositionResponse(
    val ticker: String,
    val companyName: String,
    val quantity: Int,
    val currentPrice: BigDecimal?,
    val currentValue: BigDecimal?,
    val lastUpdatedAt: Instant?,
    val priceSource: String?,
    val warning: String? = null,
)

data class PortfolioResponse(
    val positions: List<PortfolioPositionResponse>,
)

data class PortfolioValueResponse(
    val totalValue: BigDecimal,
    val lastUpdatedAt: Instant?,
    val positions: List<PortfolioPositionResponse>,
    val warnings: List<String> = emptyList(),
)

data class ProfitLossPositionResponse(
    val ticker: String,
    val companyName: String,
    val quantity: Int,
    val averageCost: BigDecimal?,
    val currentPrice: BigDecimal?,
    val priceSource: String?,
    val investedCost: BigDecimal?,
    val currentValue: BigDecimal?,
    val profitLoss: BigDecimal?,
    val returnPercentage: BigDecimal?,
    val warning: String? = null,
)

data class ProfitLossResponse(
    val totalInvestedCost: BigDecimal,
    val totalCurrentValue: BigDecimal,
    val totalProfitLoss: BigDecimal,
    val totalReturnPercentage: BigDecimal,
    val positions: List<ProfitLossPositionResponse>,
    val warnings: List<String> = emptyList(),
)
