package com.austral.portfolio_tracker.dto

import java.math.BigDecimal

data class CreateCompanyRequest(
    val ticker: String?,
    val companyName: String?,
    val companyPrices: BigDecimal?,
)
