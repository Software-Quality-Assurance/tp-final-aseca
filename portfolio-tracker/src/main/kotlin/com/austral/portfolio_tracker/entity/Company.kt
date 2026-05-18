package com.austral.portfolio_tracker.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val ticker: String,

    @Column(nullable = false)
    val companyName: String,

    @Column(nullable = false, unique = true)
    val cik: String,

    @Column(nullable = false, precision = 19, scale = 2)
    val companyPrices: BigDecimal,

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
    val history: MutableList<History> = mutableListOf(),

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
    val watchlist: MutableList<Watchlist> = mutableListOf()
)