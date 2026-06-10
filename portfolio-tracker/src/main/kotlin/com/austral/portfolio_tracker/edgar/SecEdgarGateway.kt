package com.austral.portfolio_tracker.edgar

import tools.jackson.databind.JsonNode

data class SecCompanyRecord(
    val cik: String,
    val ticker: String,
    val name: String,
)

interface SecEdgarGateway {
    fun findCompanyByTicker(ticker: String): SecCompanyRecord?

    fun searchCompanies(query: String): List<SecCompanyRecord>

    fun getCompanyFacts(cik: String): JsonNode

    fun getSubmissions(cik: String): JsonNode
}
