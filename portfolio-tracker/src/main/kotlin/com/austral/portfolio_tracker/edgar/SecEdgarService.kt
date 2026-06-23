package com.austral.portfolio_tracker.edgar

import com.austral.portfolio_tracker.company.CompanyRepository
import com.austral.portfolio_tracker.exception.ResourceNotFoundException
import com.austral.portfolio_tracker.portfolio.HistoryRepository
import com.austral.portfolio_tracker.watchlist.WatchlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class SecEdgarService(
    private val gateway: SecEdgarGateway,
    private val mapper: SecEdgarMapper,
    private val companyRepository: CompanyRepository,
    private val historyRepository: HistoryRepository,
    private val watchlistRepository: WatchlistRepository,
    private val clock: Clock,
) {
    private val searchCache = ConcurrentHashMap<String, CachedSearchResult>()
    private val searchTtl: Duration = Duration.ofHours(1)

    @Transactional(readOnly = true)
    fun search(query: String): List<EdgarCompanyResponse> {
        val normalized = query.trim()
        if (normalized.isBlank()) throw IllegalArgumentException("Search query is required")

        val cacheKey = normalized.uppercase()
        val cached = searchCache[cacheKey]
        val now = Instant.now(clock)
        val companies =
            if (cached != null && cached.expiresAt.isAfter(now)) {
                cached.companies
            } else {
                gateway.searchCompanies(normalized).also { companies ->
                    searchCache[cacheKey] = CachedSearchResult(companies, now.plus(searchTtl))
                }
            }

        val allowedTickers = companyRepository.findAllByActiveTrue().map { it.ticker.uppercase() }.toSet()
        return companies
            .filter { it.ticker in allowedTickers }
            .map { EdgarCompanyResponse(it.cik, it.ticker, it.name) }
    }

    @Transactional(readOnly = true)
    fun metrics(ticker: String): FinancialMetricsResponse {
        val company = resolveCompany(ticker)
        return mapper.toMetrics(company.ticker, company.cik, gateway.getCompanyFacts(company.cik))
    }

    @Transactional(readOnly = true)
    fun filings(ticker: String): FilingsResponse {
        val company = resolveCompany(ticker)
        return mapper.toFilings(company.ticker, company.cik, gateway.getSubmissions(company.cik))
    }

    @Transactional(readOnly = true)
    fun history(
        ticker: String,
        metric: FinancialMetric,
        quarters: Int,
    ): FinancialHistoryResponse {
        if (quarters !in 4..8) throw IllegalArgumentException("Quarters must be between 4 and 8")
        val company = resolveCompany(ticker)
        return mapper.toHistory(company.ticker, company.cik, metric, quarters, gateway.getCompanyFacts(company.cik))
    }

    @Transactional(readOnly = true)
    fun comparison(userId: Long): FinancialComparisonResponse {
        val portfolioTickers =
            historyRepository
                .findByUserIdOrderByTimestampDesc(userId)
                .groupBy { it.company.ticker.uppercase() }
                .filterValues { entries ->
                    entries.sumOf {
                        if (it.transactionTypeEnum.name == "BUY") it.numberOfStocks else -it.numberOfStocks
                    } > 0
                }.keys
        val watchlistTickers =
            watchlistRepository.findByUserId(userId).mapNotNull { it.company?.ticker?.uppercase() }.toSet()
        val tickers = (portfolioTickers + watchlistTickers).sorted()

        val raw =
            tickers.map { ticker ->
                val companyName =
                    companyRepository.findByTickerAndActiveTrue(ticker)?.companyName
                        ?: ticker
                CompanyComparisonResponse(
                    ticker = ticker,
                    companyName = companyName,
                    inPortfolio = ticker in portfolioTickers,
                    inWatchlist = ticker in watchlistTickers,
                    metrics = metrics(ticker),
                    bestMetrics = emptySet(),
                )
            }
        val winners = calculateWinners(raw)
        val companies = raw.map { it.copy(bestMetrics = winners[it.ticker].orEmpty()) }
        return FinancialComparisonResponse(
            companies = companies,
            warning = if (companies.size == 1) "At least two companies are recommended for comparison" else null,
        )
    }

    private fun resolveCompany(ticker: String): SecCompanyRecord {
        val normalized = ticker.trim().uppercase()
        companyRepository.findByTickerAndActiveTrue(normalized)
            ?: throw ResourceNotFoundException("Company with ticker $normalized not found")
        return gateway.findCompanyByTicker(normalized)
            ?: throw ResourceNotFoundException("SEC EDGAR company with ticker $normalized not found")
    }

    private fun calculateWinners(companies: List<CompanyComparisonResponse>): Map<String, Set<FinancialMetric>> {
        val winners = companies.associate { it.ticker to mutableSetOf<FinancialMetric>() }
        FinancialMetric.entries.forEach { metric ->
            val values =
                companies.mapNotNull { company ->
                    company.metricValue(metric)?.let { company.ticker to it }
                }
            if (values.isEmpty()) return@forEach
            val winningValue =
                if (metric == FinancialMetric.TOTAL_LIABILITIES) {
                    values.minOf { it.second }
                } else {
                    values.maxOf { it.second }
                }
            values.filter { it.second.compareTo(winningValue) == 0 }.forEach { (ticker, _) ->
                winners.getValue(ticker).add(metric)
            }
        }
        return winners
    }

    private fun CompanyComparisonResponse.metricValue(metric: FinancialMetric): BigDecimal? =
        when (metric) {
            FinancialMetric.REVENUE -> metrics.revenue.value
            FinancialMetric.NET_INCOME -> metrics.netIncome.value
            FinancialMetric.EPS -> metrics.eps.value
            FinancialMetric.TOTAL_ASSETS -> metrics.totalAssets.value
            FinancialMetric.TOTAL_LIABILITIES -> metrics.totalLiabilities.value
        }

    private data class CachedSearchResult(
        val companies: List<SecCompanyRecord>,
        val expiresAt: Instant,
    )
}
