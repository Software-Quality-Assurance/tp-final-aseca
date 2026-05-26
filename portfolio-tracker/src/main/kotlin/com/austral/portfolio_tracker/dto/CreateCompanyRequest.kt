package com.austral.portfolio_tracker.dto

import jakarta.validation.constraints.NotBlank

data class CreateCompanyRequest(
    @field:NotBlank
    val ticker: String,
    @field:NotBlank
    val companyName: String,
)
