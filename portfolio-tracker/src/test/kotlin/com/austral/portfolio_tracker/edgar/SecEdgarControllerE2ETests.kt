package com.austral.portfolio_tracker.edgar

import com.austral.portfolio_tracker.company.CompanyRepository
import com.austral.portfolio_tracker.entities.Company
import com.austral.portfolio_tracker.entities.History
import com.austral.portfolio_tracker.entities.TransactionTypeEnum
import com.austral.portfolio_tracker.entities.Watchlist
import com.austral.portfolio_tracker.portfolio.HistoryRepository
import com.austral.portfolio_tracker.user.RegisterUserRequest
import com.austral.portfolio_tracker.user.UserRepository
import com.austral.portfolio_tracker.watchlist.WatchlistRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecEdgarControllerE2ETests.FakeGatewayConfiguration::class)
@Transactional
class SecEdgarControllerE2ETests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var historyRepository: HistoryRepository

    @Autowired
    private lateinit var watchlistRepository: WatchlistRepository

    @Autowired
    private lateinit var gateway: FakeSecEdgarGateway

    @BeforeEach
    fun setup() {
        historyRepository.deleteAll()
        watchlistRepository.deleteAll()
        companyRepository.deleteAll()
        userRepository.deleteAll()
        gateway.unavailable = false
        companyRepository.save(Company(ticker = "AAPL", companyName = "Apple Inc."))
        companyRepository.save(Company(ticker = "MSFT", companyName = "Microsoft Corporation"))
    }

    @Test
    fun `authenticated user can search whitelist companies and query simple financial DTOs`() {
        val token = registerAndLogin("edgar@example.com")

        mockMvc
            .get("/api/edgar/search?query=apple") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].ticker") { value("AAPL") }
                jsonPath("$[0].cik") { value("0000320193") }
            }

        mockMvc
            .get("/api/edgar/companies/aapl/metrics") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.ticker") { value("AAPL") }
                jsonPath("$.revenue.value") { value(1000) }
                jsonPath("$.netIncome.value") { value(250) }
                jsonPath("$.eps.value") { value(6.25) }
                jsonPath("$.totalLiabilities.value") { doesNotExist() }
                jsonPath("$.partial") { value(true) }
            }

        mockMvc
            .get("/api/edgar/companies/AAPL/filings") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.filings.length()") { value(2) }
                jsonPath("$.filings[0].form") { value("10-K") }
            }

        mockMvc
            .get("/api/edgar/companies/AAPL/history?metric=REVENUE&quarters=4") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.points.length()") { value(4) }
                jsonPath("$.points[3].period") { value("CY2025Q1") }
            }
    }

    @Test
    fun `comparison deduplicates portfolio and watchlist and marks relative winners`() {
        val token = registerAndLogin("comparison@example.com")
        val user = requireNotNull(userRepository.findByMailIgnoreCase("comparison@example.com"))
        val apple = requireNotNull(companyRepository.findByTickerAndActiveTrue("AAPL"))
        val microsoft = requireNotNull(companyRepository.findByTickerAndActiveTrue("MSFT"))

        historyRepository.save(
            History(
                numberOfStocks = 2,
                transactionValue = BigDecimal("200"),
                transactionTypeEnum = TransactionTypeEnum.BUY,
                user = user,
                company = apple,
            ),
        )
        watchlistRepository.save(Watchlist(user = user, company = apple))
        watchlistRepository.save(Watchlist(user = user, company = microsoft))

        mockMvc
            .get("/api/edgar/comparison") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.companies.length()") { value(2) }
                jsonPath("$.companies[0].ticker") { value("AAPL") }
                jsonPath("$.companies[0].inPortfolio") { value(true) }
                jsonPath("$.companies[0].inWatchlist") { value(true) }
                jsonPath("$.companies[1].ticker") { value("MSFT") }
                jsonPath("$.companies[1].bestMetrics[0]") { exists() }
            }

        assertEquals(2, gateway.companyFactsCalls)
    }

    @Test
    fun `external SEC failure returns controlled 503`() {
        val token = registerAndLogin("failure@example.com")
        gateway.unavailable = true

        mockMvc
            .get("/api/edgar/companies/AAPL/metrics") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isServiceUnavailable() }
                jsonPath("$.error") { value("SEC EDGAR is temporarily unavailable") }
            }
    }

    private fun registerAndLogin(email: String): String {
        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(RegisterUserRequest(email, "Password123!"))
            }.andExpect {
                status { isCreated() }
            }
        val response =
            mockMvc
                .post("/api/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to "Password123!"))
                }.andExpect {
                    status { isOk() }
                }.andReturn()
        return objectMapper
            .readTree(response.response.contentAsString)
            .get("token")
            .toString()
            .removeSurrounding("\"")
    }

    @TestConfiguration
    class FakeGatewayConfiguration {
        @Bean
        @Primary
        fun fakeSecEdgarGateway(objectMapper: ObjectMapper) = FakeSecEdgarGateway(objectMapper)
    }

    class FakeSecEdgarGateway(
        private val objectMapper: ObjectMapper,
    ) : SecEdgarGateway {
        var unavailable = false
        var companyFactsCalls = 0

        private val companies =
            listOf(
                SecCompanyRecord("0000320193", "AAPL", "Apple Inc."),
                SecCompanyRecord("0000789019", "MSFT", "Microsoft Corporation"),
                SecCompanyRecord("0000000001", "PRIVATE", "Private Corp"),
            )

        override fun findCompanyByTicker(ticker: String): SecCompanyRecord? = companies.firstOrNull { it.ticker == ticker.uppercase() }

        override fun searchCompanies(query: String): List<SecCompanyRecord> =
            companies.filter { it.ticker.contains(query, true) || it.name.contains(query, true) }

        override fun getCompanyFacts(cik: String): JsonNode {
            if (unavailable) throw SecEdgarUnavailableException("SEC EDGAR is temporarily unavailable")
            companyFactsCalls++
            val multiplier = if (cik == "0000789019") 2 else 1
            return factsJson(multiplier)
        }

        override fun getSubmissions(cik: String): JsonNode =
            objectMapper.readTree(
                """
                {
                  "filings": {
                    "recent": {
                      "accessionNumber": ["0000320193-25-000079", "0000320193-25-000057"],
                      "filingDate": ["2025-10-31", "2025-08-01"],
                      "form": ["10-K", "10-Q"],
                      "primaryDocument": ["annual.htm", "quarter.htm"]
                    }
                  }
                }
                """.trimIndent(),
            )

        private fun factsJson(multiplier: Int): JsonNode =
            objectMapper.readTree(
                """
                {
                  "facts": {
                    "us-gaap": {
                      "Revenues": {"units": {"USD": [
                        {"val": ${10 * multiplier}, "end": "2024-03-31", "fy": 2024, "fp": "Q1", "form": "10-Q", "filed": "2024-05-01", "frame": "CY2024Q1"},
                        {"val": ${20 * multiplier}, "end": "2024-06-30", "fy": 2024, "fp": "Q2", "form": "10-Q", "filed": "2024-08-01", "frame": "CY2024Q2"},
                        {"val": ${30 * multiplier}, "end": "2024-09-30", "fy": 2024, "fp": "Q3", "form": "10-Q", "filed": "2024-11-01", "frame": "CY2024Q3"},
                        {"val": ${40 * multiplier}, "end": "2024-12-31", "fy": 2024, "fp": "FY", "form": "10-K", "filed": "2025-02-01", "frame": "CY2024Q4"},
                        {"val": ${1000 * multiplier}, "end": "2025-03-31", "fy": 2025, "fp": "Q1", "form": "10-Q", "filed": "2025-05-01", "frame": "CY2025Q1"}
                      ]}},
                      "NetIncomeLoss": {"units": {"USD": [
                        {"val": ${250 * multiplier}, "end": "2025-03-31", "fy": 2025, "fp": "Q1", "form": "10-Q", "filed": "2025-05-01"}
                      ]}},
                      "EarningsPerShareBasic": {"units": {"USD/shares": [
                        {"val": ${6.25 * multiplier}, "end": "2025-03-31", "fy": 2025, "fp": "Q1", "form": "10-Q", "filed": "2025-05-01"}
                      ]}},
                      "Assets": {"units": {"USD": [
                        {"val": ${5000 * multiplier}, "end": "2025-03-31", "fy": 2025, "fp": "Q1", "form": "10-Q", "filed": "2025-05-01"}
                      ]}}
                    }
                  }
                }
                """.trimIndent(),
            )
    }
}
