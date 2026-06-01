package com.austral.portfolio_tracker.service

import com.austral.portfolio_tracker.dto.OperationRequest
import com.austral.portfolio_tracker.dto.OperationResponse
import com.austral.portfolio_tracker.dto.PortfolioPositionResponse
import com.austral.portfolio_tracker.dto.PortfolioResponse
import com.austral.portfolio_tracker.dto.PortfolioValueResponse
import com.austral.portfolio_tracker.dto.ProfitLossPositionResponse
import com.austral.portfolio_tracker.dto.ProfitLossResponse
import com.austral.portfolio_tracker.dto.UpdateHistoryRequest
import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.History
import com.austral.portfolio_tracker.entity.Price
import com.austral.portfolio_tracker.entity.TransactionTypeEnum
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.HistoryRepository
import com.austral.portfolio_tracker.repository.PriceRepository
import com.austral.portfolio_tracker.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

private val ZERO = BigDecimal("0.00")

private data class PositionSnapshot(
    val company: Company,
    val quantity: Int,
    val totalBuyQuantity: Int,
    val totalBuyValue: BigDecimal,
)

@Service
class PortfolioService(
    private val userRepository: UserRepository,
    private val companyRepository: CompanyRepository,
    private val priceRepository: PriceRepository,
    private val historyRepository: HistoryRepository,
) {
    @Transactional
    fun createOperation(
        userId: Long,
        request: OperationRequest,
    ): OperationResponse {
        val type = parseType(request.type)
        val company =
            companyRepository.findByTickerAndActiveTrue(request.ticker.trim().uppercase())
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
        val latestPrice =
            priceRepository.findFirstByCompanyIdOrderByTimestampDesc(requireNotNull(company.id))
                ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Company does not have a stored price")
        val user =
            userRepository.findById(userId).orElse(null)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")

        if (type == TransactionTypeEnum.SELL) validateSellQuantity(userId, company, request.quantity)

        val history =
            historyRepository.save(
                History(
                    numberOfStocks = request.quantity,
                    transactionValue = latestPrice.unityPrice.multiply(BigDecimal(request.quantity)).money(),
                    transactionTypeEnum = type,
                    timestamp = Instant.now(),
                    user = user,
                    company = company,
                ),
            )

        return history.toResponse()
    }

    @Transactional(readOnly = true)
    fun getHistory(userId: Long): List<OperationResponse> =
        historyRepository.findByUserIdOrderByTimestampDesc(userId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getPortfolio(userId: Long): PortfolioResponse = PortfolioResponse(buildPositions(userId).map { it.toPortfolioPosition() })

    @Transactional(readOnly = true)
    fun getCurrentValue(userId: Long): PortfolioValueResponse {
        val positions = buildPositions(userId).map { it.toPortfolioPosition() }
        return PortfolioValueResponse(
            totalValue = positions.mapNotNull { it.currentValue }.fold(ZERO, BigDecimal::add).money(),
            lastUpdatedAt = positions.mapNotNull { it.lastUpdatedAt }.maxOrNull(),
            positions = positions,
            warnings = positions.mapNotNull { it.warning },
        )
    }

    @Transactional(readOnly = true)
    fun getProfitLoss(userId: Long): ProfitLossResponse {
        val positions =
            buildPositions(userId).map { position ->
                val latestPrice = latestPrice(position.company)
                if (latestPrice == null || position.totalBuyQuantity == 0) {
                    ProfitLossPositionResponse(
                        ticker = position.company.ticker,
                        companyName = position.company.companyName,
                        quantity = position.quantity,
                        averageCost = null,
                        currentPrice = latestPrice?.unityPrice,
                        investedCost = null,
                        currentValue = latestPrice?.unityPrice?.multiply(BigDecimal(position.quantity))?.money(),
                        profitLoss = null,
                        returnPercentage = null,
                        warning = "Insufficient data to calculate P&L for ${position.company.ticker}",
                    )
                } else {
                    val averageCost = position.totalBuyValue.divide(BigDecimal(position.totalBuyQuantity), 6, RoundingMode.HALF_UP)
                    val investedCost = averageCost.multiply(BigDecimal(position.quantity)).money()
                    val currentValue = latestPrice.unityPrice.multiply(BigDecimal(position.quantity)).money()
                    val profitLoss = currentValue.subtract(investedCost).money()
                    ProfitLossPositionResponse(
                        ticker = position.company.ticker,
                        companyName = position.company.companyName,
                        quantity = position.quantity,
                        averageCost = averageCost.money(),
                        currentPrice = latestPrice.unityPrice.money(),
                        investedCost = investedCost,
                        currentValue = currentValue,
                        profitLoss = profitLoss,
                        returnPercentage = percentage(profitLoss, investedCost),
                    )
                }
            }

        val totalInvested = positions.mapNotNull { it.investedCost }.fold(ZERO, BigDecimal::add).money()
        val totalCurrent = positions.mapNotNull { it.currentValue }.fold(ZERO, BigDecimal::add).money()
        val totalProfitLoss = totalCurrent.subtract(totalInvested).money()
        return ProfitLossResponse(
            totalInvestedCost = totalInvested,
            totalCurrentValue = totalCurrent,
            totalProfitLoss = totalProfitLoss,
            totalReturnPercentage = percentage(totalProfitLoss, totalInvested),
            positions = positions,
            warnings = positions.mapNotNull { it.warning },
        )
    }

    @Transactional
    fun updateHistory(
        userId: Long,
        historyId: Long,
        request: UpdateHistoryRequest,
    ): OperationResponse {
        val history =
            historyRepository.findByIdAndUserId(historyId, userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "History entry not found")
        val replacementType = request.type?.let { parseType(it) } ?: history.transactionTypeEnum
        val replacementQuantity = request.quantity ?: history.numberOfStocks

        ensurePortfolioNeverNegative(userId, historyId, replacementType, replacementQuantity)

        val unitPrice = history.unitPrice()
        history.transactionTypeEnum = replacementType
        history.numberOfStocks = replacementQuantity
        history.transactionValue = unitPrice.multiply(BigDecimal(replacementQuantity)).money()
        return historyRepository.save(history).toResponse()
    }

    @Transactional
    fun deleteHistory(
        userId: Long,
        historyId: Long,
    ) {
        historyRepository.findByIdAndUserId(historyId, userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "History entry not found")

        ensurePortfolioNeverNegative(userId, excludedHistoryId = historyId)
        historyRepository.deleteById(historyId)
    }

    private fun validateSellQuantity(
        userId: Long,
        company: Company,
        quantity: Int,
    ) {
        val available = currentQuantity(userId, company)
        if (available < quantity) {
            throw ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Not enough shares to sell: available $available, requested $quantity",
            )
        }
    }

    private fun currentQuantity(
        userId: Long,
        company: Company,
    ): Int =
        historyRepository
            .findByUserIdOrderByTimestampDesc(userId)
            .filter { it.company.id == company.id }
            .sumOf { if (it.transactionTypeEnum == TransactionTypeEnum.BUY) it.numberOfStocks else -it.numberOfStocks }

    private fun buildPositions(userId: Long): List<PositionSnapshot> =
        historyRepository
            .findByUserIdOrderByTimestampDesc(userId)
            .groupBy { it.company.id }
            .values
            .mapNotNull { entries ->
                val quantity =
                    entries.sumOf {
                        if (it.transactionTypeEnum ==
                            TransactionTypeEnum.BUY
                        ) {
                            it.numberOfStocks
                        } else {
                            -it.numberOfStocks
                        }
                    }
                if (quantity <= 0) {
                    null
                } else {
                    PositionSnapshot(
                        company = entries.first().company,
                        quantity = quantity,
                        totalBuyQuantity = entries.filter { it.transactionTypeEnum == TransactionTypeEnum.BUY }.sumOf { it.numberOfStocks },
                        totalBuyValue =
                            entries
                                .filter {
                                    it.transactionTypeEnum == TransactionTypeEnum.BUY
                                }.fold(ZERO) { acc, history -> acc.add(history.transactionValue) },
                    )
                }
            }.sortedBy { it.company.ticker }

    private fun ensurePortfolioNeverNegative(
        userId: Long,
        excludedHistoryId: Long,
        replacementType: TransactionTypeEnum? = null,
        replacementQuantity: Int? = null,
    ) {
        val entries =
            historyRepository.findByUserIdOrderByTimestampDesc(userId).mapNotNull { history ->
                when {
                    history.id == excludedHistoryId && replacementType == null -> null
                    history.id == excludedHistoryId ->
                        history.copyForValidation(
                            transactionType = requireNotNull(replacementType),
                            quantity = requireNotNull(replacementQuantity),
                        )
                    else -> history.copyForValidation()
                }
            }

        val negativeTicker =
            entries
                .groupBy { it.company.ticker }
                .entries
                .firstOrNull { (_, items) ->
                    items.sumOf { if (it.transactionType == TransactionTypeEnum.BUY) it.quantity else -it.quantity } < 0
                }?.key

        if (negativeTicker != null) {
            throw ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "History change would leave negative holdings for $negativeTicker",
            )
        }
    }

    private fun parseType(type: String): TransactionTypeEnum =
        try {
            TransactionTypeEnum.valueOf(type.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction type")
        }

    private fun PositionSnapshot.toPortfolioPosition(): PortfolioPositionResponse {
        val latestPrice = latestPrice(company)
        val currentValue = latestPrice?.unityPrice?.multiply(BigDecimal(quantity))?.money()
        return PortfolioPositionResponse(
            ticker = company.ticker,
            companyName = company.companyName,
            quantity = quantity,
            currentPrice = latestPrice?.unityPrice?.money(),
            currentValue = currentValue,
            lastUpdatedAt = latestPrice?.timestamp,
            warning = if (latestPrice == null) "Missing current price for ${company.ticker}" else null,
        )
    }

    private fun latestPrice(company: Company): Price? = priceRepository.findFirstByCompanyIdOrderByTimestampDesc(requireNotNull(company.id))

    private fun History.toResponse(): OperationResponse {
        val unitPrice = unitPrice()
        return OperationResponse(
            id = requireNotNull(id),
            ticker = company.ticker,
            companyName = company.companyName,
            type = transactionTypeEnum.name,
            quantity = numberOfStocks,
            unitPrice = unitPrice.money(),
            totalPrice = transactionValue.money(),
            timestamp = timestamp,
        )
    }

    private fun History.unitPrice(): BigDecimal = transactionValue.divide(BigDecimal(numberOfStocks), 6, RoundingMode.HALF_UP)

    private data class ValidationHistory(
        val company: Company,
        val transactionType: TransactionTypeEnum,
        val quantity: Int,
    )

    private fun History.copyForValidation(
        transactionType: TransactionTypeEnum = transactionTypeEnum,
        quantity: Int = numberOfStocks,
    ) = ValidationHistory(company = company, transactionType = transactionType, quantity = quantity)

    private fun percentage(
        numerator: BigDecimal,
        denominator: BigDecimal,
    ): BigDecimal =
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            ZERO
        } else {
            numerator.multiply(BigDecimal("100")).divide(denominator, 2, RoundingMode.HALF_UP)
        }

    private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
