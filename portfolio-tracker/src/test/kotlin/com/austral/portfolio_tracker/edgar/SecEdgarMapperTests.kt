package com.austral.portfolio_tracker.edgar

import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecEdgarMapperTests {
    private val objectMapper = ObjectMapper()
    private val mapper = SecEdgarMapper()

    @Test
    fun `maps company facts into simple metrics and preserves missing values`() {
        val json =
            objectMapper.readTree(
                """
                {
                  "cik": 320193,
                  "entityName": "Apple Inc.",
                  "facts": {
                    "us-gaap": {
                      "RevenueFromContractWithCustomerExcludingAssessedTax": {
                        "units": {
                          "USD": [
                            {"val": 1000000, "end": "2025-09-27", "fy": 2025, "fp": "FY", "form": "10-K", "filed": "2025-10-31"}
                          ]
                        }
                      },
                      "NetIncomeLoss": {
                        "units": {
                          "USD": [
                            {"val": 250000, "end": "2025-09-27", "fy": 2025, "fp": "FY", "form": "10-K", "filed": "2025-10-31"}
                          ]
                        }
                      },
                      "EarningsPerShareBasic": {
                        "units": {
                          "USD/shares": [
                            {"val": 6.25, "end": "2025-09-27", "fy": 2025, "fp": "FY", "form": "10-K", "filed": "2025-10-31"}
                          ]
                        }
                      },
                      "Assets": {
                        "units": {
                          "USD": [
                            {"val": 5000000, "end": "2025-09-27", "fy": 2025, "fp": "FY", "form": "10-K", "filed": "2025-10-31"}
                          ]
                        }
                      }
                    }
                  }
                }
                """.trimIndent(),
            )

        val result = mapper.toMetrics("AAPL", "0000320193", json)

        assertEquals(BigDecimal("1000000"), result.revenue.value)
        assertEquals(BigDecimal("250000"), result.netIncome.value)
        assertEquals(BigDecimal("6.25"), result.eps.value)
        assertEquals(BigDecimal("5000000"), result.totalAssets.value)
        assertNull(result.totalLiabilities.value)
        assertTrue(result.partial)
    }

    @Test
    fun `maps at most requested quarterly history points ordered oldest to newest`() {
        val json =
            objectMapper.readTree(
                """
                {
                  "facts": {
                    "us-gaap": {
                      "Revenues": {
                        "units": {
                          "USD": [
                            {"val": 10, "end": "2024-03-31", "fy": 2024, "fp": "Q1", "form": "10-Q", "filed": "2024-05-01", "frame": "CY2024Q1"},
                            {"val": 20, "end": "2024-06-30", "fy": 2024, "fp": "Q2", "form": "10-Q", "filed": "2024-08-01", "frame": "CY2024Q2"},
                            {"val": 30, "end": "2024-09-30", "fy": 2024, "fp": "Q3", "form": "10-Q", "filed": "2024-11-01", "frame": "CY2024Q3"},
                            {"val": 40, "end": "2024-12-31", "fy": 2024, "fp": "FY", "form": "10-K", "filed": "2025-02-01", "frame": "CY2024Q4"},
                            {"val": 50, "end": "2025-03-31", "fy": 2025, "fp": "Q1", "form": "10-Q", "filed": "2025-05-01", "frame": "CY2025Q1"}
                          ]
                        }
                      }
                    }
                  }
                }
                """.trimIndent(),
            )

        val result = mapper.toHistory("AAPL", "0000320193", FinancialMetric.REVENUE, 4, json)

        assertEquals(listOf("CY2024Q2", "CY2024Q3", "CY2024Q4", "CY2025Q1"), result.points.map { it.period })
        assertEquals(BigDecimal("50"), result.points.last().value)
        assertEquals(false, result.partial)
    }

    @Test
    fun `maps recent 10-K and 10-Q filings with official archive URLs`() {
        val json =
            objectMapper.readTree(
                """
                {
                  "cik": "320193",
                  "filings": {
                    "recent": {
                      "accessionNumber": ["0000320193-25-000079", "0000320193-25-000057", "0000320193-25-000001"],
                      "filingDate": ["2025-10-31", "2025-08-01", "2025-01-01"],
                      "form": ["10-K", "10-Q", "8-K"],
                      "primaryDocument": ["aapl-20250927.htm", "aapl-20250628.htm", "aapl-8k.htm"]
                    }
                  }
                }
                """.trimIndent(),
            )

        val result = mapper.toFilings("AAPL", "0000320193", json)

        assertEquals(2, result.filings.size)
        assertEquals("10-K", result.filings.first().form)
        assertEquals(
            "https://www.sec.gov/Archives/edgar/data/320193/000032019325000079/aapl-20250927.htm",
            result.filings.first().documentUrl,
        )
    }
}
