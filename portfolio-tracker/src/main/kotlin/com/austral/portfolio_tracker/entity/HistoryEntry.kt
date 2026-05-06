package com.austral.portfolio_tracker.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class TransactionType {
    BUY,
    SELL,
    DIVIDEND,
}

@Entity
@Table(name = "history_entries")
data class HistoryEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    val portfolio: Portfolio,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    val company: Company,
    @field:NotNull(message = "Number of stocks cannot be null")
    @field:DecimalMin(value = "0.01", message = "Number of stocks must be greater than 0")
    @Column(nullable = false, precision = 19, scale = 8)
    val numberOfStocks: BigDecimal,
    @field:NotNull(message = "Transaction value cannot be null")
    @field:DecimalMin(value = "0.01", message = "Transaction value must be greater than 0")
    @Column(nullable = false, precision = 19, scale = 2)
    val transactionValue: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val transactionType: TransactionType,
    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),
)
