package com.austral.portfolio_tracker.edgar

import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.LocalDate

private data class FactEntry(
    val value: BigDecimal,
    val unit: String,
    val end: LocalDate,
    val fiscalYear: Int?,
    val fiscalPeriod: String?,
    val form: String?,
    val filed: LocalDate?,
    val frame: String?,
)

@Component
class SecEdgarMapper {
    fun toMetrics(
        ticker: String,
        cik: String,
        root: JsonNode,
    ): FinancialMetricsResponse {
        val revenue = latestMetric(root, FinancialMetric.REVENUE)
        val netIncome = latestMetric(root, FinancialMetric.NET_INCOME)
        val eps = latestMetric(root, FinancialMetric.EPS)
        val assets = latestMetric(root, FinancialMetric.TOTAL_ASSETS)
        val liabilities = latestMetric(root, FinancialMetric.TOTAL_LIABILITIES)
        val values = listOf(revenue, netIncome, eps, assets, liabilities)

        return FinancialMetricsResponse(
            ticker = ticker,
            cik = cik,
            revenue = revenue,
            netIncome = netIncome,
            eps = eps,
            totalAssets = assets,
            totalLiabilities = liabilities,
            partial = values.any { it.value == null },
        )
    }

    fun toHistory(
        ticker: String,
        cik: String,
        metric: FinancialMetric,
        quarters: Int,
        root: JsonNode,
    ): FinancialHistoryResponse {
        val points =
            entries(root, metric)
                .filter { it.frame?.matches(Regex("CY\\d{4}Q[1-4]")) == true }
                .groupBy { it.frame }
                .mapNotNull { (_, values) -> values.maxWithOrNull(compareBy<FactEntry> { it.filed }.thenBy { it.end }) }
                .sortedBy { it.end }
                .takeLast(quarters)
                .map {
                    FinancialHistoryPoint(
                        period = requireNotNull(it.frame),
                        fiscalYear = it.fiscalYear,
                        fiscalPeriod = it.fiscalPeriod,
                        reportDate = it.end,
                        value = it.value,
                        unit = it.unit,
                    )
                }

        return FinancialHistoryResponse(
            ticker = ticker,
            cik = cik,
            metric = metric,
            points = points,
            partial = points.size < minOf(4, quarters),
        )
    }

    fun toFilings(
        ticker: String,
        cik: String,
        root: JsonNode,
    ): FilingsResponse {
        val recent = root.get("filings")?.get("recent")
        val accessionNumbers = recent?.get("accessionNumber")
        val filingDates = recent?.get("filingDate")
        val forms = recent?.get("form")
        val primaryDocuments = recent?.get("primaryDocument")
        val filings = mutableListOf<FilingResponse>()

        if (accessionNumbers != null && filingDates != null && forms != null && primaryDocuments != null) {
            for (index in 0 until accessionNumbers.size()) {
                val form = forms.get(index)?.stringValue().orEmpty()
                if (form !in setOf("10-K", "10-Q")) continue
                val accession = accessionNumbers.get(index).stringValue().orEmpty()
                val filingDate = LocalDate.parse(filingDates.get(index).stringValue())
                val primaryDocument = primaryDocuments.get(index).stringValue().orEmpty()
                val numericCik = cik.toLong().toString()
                val archiveAccession = accession.replace("-", "")
                filings +=
                    FilingResponse(
                        form = form,
                        filingDate = filingDate,
                        accessionNumber = accession,
                        documentUrl = "https://www.sec.gov/Archives/edgar/data/$numericCik/$archiveAccession/$primaryDocument",
                    )
            }
        }

        return FilingsResponse(
            ticker = ticker,
            cik = cik,
            filings = filings.take(20),
            message = if (filings.isEmpty()) "No recent 10-K or 10-Q filings are available" else null,
        )
    }

    private fun latestMetric(
        root: JsonNode,
        metric: FinancialMetric,
    ): FinancialMetricValue {
        val entry =
            entries(root, metric)
                .filter { it.form in setOf("10-K", "10-Q", "20-F", "40-F") }
                .maxWithOrNull(compareBy<FactEntry> { it.filed }.thenBy { it.end })

        return if (entry == null) {
            FinancialMetricValue(null, null, null, null, null, null)
        } else {
            FinancialMetricValue(
                value = entry.value,
                unit = entry.unit,
                reportDate = entry.end,
                fiscalYear = entry.fiscalYear,
                fiscalPeriod = entry.fiscalPeriod,
                form = entry.form,
            )
        }
    }

    private fun entries(
        root: JsonNode,
        metric: FinancialMetric,
    ): List<FactEntry> {
        val usGaap = root.get("facts")?.get("us-gaap") ?: return emptyList()
        for (concept in concepts(metric)) {
            val units = usGaap.get(concept)?.get("units") ?: continue
            for (unit in units.propertyNames()) {
                val entries = units.get(unit) ?: continue
                val parsed =
                    entries.mapNotNull { node ->
                        val value = node.get("val")?.decimalValue() ?: return@mapNotNull null
                        val end =
                            node
                                .get("end")
                                ?.stringValue()
                                ?.takeIf {
                                    it.isNotBlank()
                                }?.let(
                                    LocalDate::parse,
                                )
                                ?: return@mapNotNull null
                        FactEntry(
                            value = value,
                            unit = unit,
                            end = end,
                            fiscalYear = node.get("fy")?.takeUnless { it.isNull }?.asInt(),
                            fiscalPeriod = node.get("fp").textOrNull(),
                            form = node.get("form").textOrNull(),
                            filed =
                                node
                                    .get("filed")
                                    .textOrNull()
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }?.let(LocalDate::parse),
                            frame = node.get("frame").textOrNull(),
                        )
                    }
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return emptyList()
    }

    private fun concepts(metric: FinancialMetric): List<String> =
        when (metric) {
            FinancialMetric.REVENUE ->
                listOf(
                    "RevenueFromContractWithCustomerExcludingAssessedTax",
                    "Revenues",
                    "SalesRevenueNet",
                )
            FinancialMetric.NET_INCOME -> listOf("NetIncomeLoss", "ProfitLoss")
            FinancialMetric.EPS -> listOf("EarningsPerShareBasic", "EarningsPerShareDiluted")
            FinancialMetric.TOTAL_ASSETS -> listOf("Assets")
            FinancialMetric.TOTAL_LIABILITIES -> listOf("Liabilities")
        }

    private fun JsonNode?.textOrNull(): String? =
        this
            ?.takeUnless { it.isNull }
            ?.toString()
            ?.removeSurrounding("\"")
}
