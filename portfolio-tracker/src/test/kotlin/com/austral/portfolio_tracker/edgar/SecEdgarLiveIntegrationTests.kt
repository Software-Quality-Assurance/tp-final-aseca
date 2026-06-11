package com.austral.portfolio_tracker.edgar

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecEdgarLiveIntegrationTests {
    @Test
    fun `resolves Apple and retrieves real company facts and submissions`() {
        assumeTrue(System.getenv("RUN_LIVE_SEC_EDGAR_TESTS") == "1")
        val client =
            SecEdgarClient(
                userAgent = "tp-final-aseca fmanfredi@mail.austral.edu.ar",
                dataBaseUrl = "https://data.sec.gov",
                secBaseUrl = "https://www.sec.gov",
                eftsBaseUrl = "https://efts.sec.gov",
                timeoutSeconds = 15,
                rateLimiter = SecRateLimiter(),
            )
        val mapper = SecEdgarMapper()

        val company = assertNotNull(client.findCompanyByTicker("AAPL"))
        assertEquals("0000320193", company.cik)
        assertTrue(client.searchCompanies("Apple").any { it.ticker == "AAPL" })

        val metrics = mapper.toMetrics(company.ticker, company.cik, client.getCompanyFacts(company.cik))
        assertNotNull(metrics.revenue.value)
        assertNotNull(metrics.totalAssets.value)

        val filings = mapper.toFilings(company.ticker, company.cik, client.getSubmissions(company.cik))
        assertTrue(filings.filings.any { it.form == "10-K" })
        assertTrue(filings.filings.any { it.form == "10-Q" })
    }
}
