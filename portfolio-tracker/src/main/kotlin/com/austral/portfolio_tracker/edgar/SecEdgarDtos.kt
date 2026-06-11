package com.austral.portfolio_tracker.edgar

import java.math.BigDecimal
import java.time.LocalDate

enum class FinancialMetric {
    REVENUE,
    NET_INCOME,
    EPS,
    TOTAL_ASSETS,
    TOTAL_LIABILITIES,
}

data class EdgarCompanyResponse(
    val cik: String,
    val ticker: String,
    val name: String,
)

data class FinancialMetricValue(
    val value: BigDecimal?,
    val unit: String?,
    val reportDate: LocalDate?,
    val fiscalYear: Int?,
    val fiscalPeriod: String?,
    val form: String?,
)

data class FinancialMetricsResponse(
    val ticker: String,
    val cik: String,
    val revenue: FinancialMetricValue,
    val netIncome: FinancialMetricValue,
    val eps: FinancialMetricValue,
    val totalAssets: FinancialMetricValue,
    val totalLiabilities: FinancialMetricValue,
    val partial: Boolean,
)

data class FilingResponse(
    val form: String,
    val filingDate: LocalDate,
    val accessionNumber: String,
    val documentUrl: String,
)

data class FilingsResponse(
    val ticker: String,
    val cik: String,
    val filings: List<FilingResponse>,
    val message: String? = null,
)

data class FinancialHistoryPoint(
    val period: String,
    val fiscalYear: Int?,
    val fiscalPeriod: String?,
    val reportDate: LocalDate,
    val value: BigDecimal,
    val unit: String,
)

data class FinancialHistoryResponse(
    val ticker: String,
    val cik: String,
    val metric: FinancialMetric,
    val points: List<FinancialHistoryPoint>,
    val partial: Boolean,
)

data class CompanyComparisonResponse(
    val ticker: String,
    val companyName: String,
    val inPortfolio: Boolean,
    val inWatchlist: Boolean,
    val metrics: FinancialMetricsResponse,
    val bestMetrics: Set<FinancialMetric>,
)

data class FinancialComparisonResponse(
    val companies: List<CompanyComparisonResponse>,
    val warning: String? = null,
)
