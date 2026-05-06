package com.austral.portfolio_tracker.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @field:NotBlank(message = "Ticker cannot be empty")
    @field:Size(min = 1, max = 10, message = "Ticker must be between 1 and 10 characters")
    @Column(nullable = false, unique = true)
    val ticker: String,
    @field:NotBlank(message = "Company name cannot be empty")
    @field:Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
    @Column(nullable = false)
    val companyName: String,
    @Column
    val cik: String? = null,
    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
    val historyEntries: MutableList<HistoryEntry> = mutableListOf(),
    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
    val watchlistEntries: MutableList<WatchlistEntry> = mutableListOf(),
)
