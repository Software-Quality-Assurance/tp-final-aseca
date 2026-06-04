package com.austral.portfolio_tracker.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class CreateCompanyRequest(
    @field:NotBlank
    val ticker: String,
    @field:NotBlank
    val companyName: String,
    @field:Positive
    val price: BigDecimal? = null,
)
