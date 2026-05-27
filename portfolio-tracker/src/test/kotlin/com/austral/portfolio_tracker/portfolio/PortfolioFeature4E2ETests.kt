package com.austral.portfolio_tracker.portfolio

import com.austral.portfolio_tracker.dto.RegisterUserRequest
import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.Price
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.HistoryRepository
import com.austral.portfolio_tracker.repository.PriceRepository
import com.austral.portfolio_tracker.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortfolioFeature4E2ETests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var priceRepository: PriceRepository

    @Autowired
    private lateinit var historyRepository: HistoryRepository

    private lateinit var apple: Company
    private lateinit var tesla: Company

    @BeforeEach
    fun setup() {
        historyRepository.deleteAll()
        priceRepository.deleteAll()
        companyRepository.deleteAll()
        userRepository.deleteAll()

        apple = companyRepository.save(Company(ticker = "AAPL", companyName = "Apple Inc."))
        tesla = companyRepository.save(Company(ticker = "TSLA", companyName = "Tesla, Inc."))

        priceRepository.save(
            Price(ticker = "AAPL", unityPrice = BigDecimal("100.00"), timestamp = Instant.parse("2026-05-20T10:00:00Z"), company = apple),
        )
        priceRepository.save(
            Price(ticker = "AAPL", unityPrice = BigDecimal("120.00"), timestamp = Instant.parse("2026-05-21T10:00:00Z"), company = apple),
        )
        priceRepository.save(
            Price(ticker = "TSLA", unityPrice = BigDecimal("200.00"), timestamp = Instant.parse("2026-05-21T11:00:00Z"), company = tesla),
        )
    }

    @Test
    fun `buy and sell operations update portfolio and append history using latest stored price`() {
        val token = registerAndLogin("feature4@example.com")

        postOperation(token, "AAPL", "BUY", 10)
            .andExpect {
                status { isCreated() }
                jsonPath("$.ticker") { value("AAPL") }
                jsonPath("$.type") { value("BUY") }
                jsonPath("$.quantity") { value(10) }
                jsonPath("$.unitPrice") { value(120.00) }
                jsonPath("$.totalPrice") { value(1200.00) }
            }

        postOperation(token, "AAPL", "SELL", 4)
            .andExpect {
                status { isCreated() }
                jsonPath("$.type") { value("SELL") }
                jsonPath("$.quantity") { value(4) }
                jsonPath("$.unitPrice") { value(120.00) }
                jsonPath("$.totalPrice") { value(480.00) }
            }

        mockMvc
            .get("/api/portfolio") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.positions.length()") { value(1) }
                jsonPath("$.positions[0].ticker") { value("AAPL") }
                jsonPath("$.positions[0].quantity") { value(6) }
                jsonPath("$.positions[0].currentPrice") { value(120.00) }
                jsonPath("$.positions[0].currentValue") { value(720.00) }
            }

        mockMvc
            .get("/api/portfolio/history") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].type") { value("SELL") }
                jsonPath("$[1].type") { value("BUY") }
            }

        assertEquals(2, historyRepository.findAll().size)
    }

    @Test
    fun `invalid operations do not create partial history rows`() {
        val token = registerAndLogin("invalid-feature4@example.com")

        postOperation(token, "AAPL", "BUY", 0)
            .andExpect { status { isBadRequest() } }

        postOperation(token, "UNKNOWN", "BUY", 1)
            .andExpect { status { isNotFound() } }

        val noPriceCompany = companyRepository.save(Company(ticker = "NOPR", companyName = "No Price Inc."))
        assertEquals("NOPR", noPriceCompany.ticker)
        postOperation(token, "NOPR", "BUY", 1)
            .andExpect { status { isUnprocessableContent() } }

        postOperation(token, "AAPL", "SELL", 1)
            .andExpect { status { isUnprocessableContent() } }

        assertEquals(0, historyRepository.findAll().size)
    }

    @Test
    fun `current value and profit loss use latest stored prices`() {
        val token = registerAndLogin("valuation-feature4@example.com")

        postOperation(token, "AAPL", "BUY", 10).andExpect { status { isCreated() } }
        postOperation(token, "TSLA", "BUY", 2).andExpect { status { isCreated() } }

        priceRepository.save(
            Price(ticker = "AAPL", unityPrice = BigDecimal("130.00"), timestamp = Instant.parse("2026-05-22T10:00:00Z"), company = apple),
        )
        priceRepository.save(
            Price(ticker = "TSLA", unityPrice = BigDecimal("180.00"), timestamp = Instant.parse("2026-05-22T10:05:00Z"), company = tesla),
        )

        mockMvc
            .get("/api/portfolio/value") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalValue") { value(1660.00) }
                jsonPath("$.lastUpdatedAt") { value("2026-05-22T10:05:00Z") }
                jsonPath("$.positions.length()") { value(2) }
            }

        mockMvc
            .get("/api/portfolio/profit-loss") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalProfitLoss") { value(60.00) }
                jsonPath("$.totalReturnPercentage") { value(3.75) }
                jsonPath("$.positions[0].ticker") { value("AAPL") }
                jsonPath("$.positions[0].profitLoss") { value(100.00) }
                jsonPath("$.positions[1].ticker") { value("TSLA") }
                jsonPath("$.positions[1].profitLoss") { value(-40.00) }
            }
    }

    @Test
    fun `history can be edited and deleted without breaking portfolio consistency`() {
        val token = registerAndLogin("history-edit-feature4@example.com")

        val buyResponse = postOperation(token, "AAPL", "BUY", 10).andExpect { status { isCreated() } }.andReturn()
        val buyId = objectMapper.readTree(buyResponse.response.contentAsString).get("id").asLong()
        val sellResponse = postOperation(token, "AAPL", "SELL", 3).andExpect { status { isCreated() } }.andReturn()
        val sellId = objectMapper.readTree(sellResponse.response.contentAsString).get("id").asLong()

        mockMvc
            .patch("/api/portfolio/history/$sellId") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("quantity" to 2, "type" to "SELL"))
            }.andExpect {
                status { isOk() }
                jsonPath("$.quantity") { value(2) }
            }

        mockMvc
            .delete("/api/portfolio/history/$buyId") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isUnprocessableContent() }
            }

        mockMvc
            .delete("/api/portfolio/history/$sellId") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNoContent() }
            }

        mockMvc
            .get("/api/portfolio") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.positions[0].quantity") { value(10) }
            }
    }

    private fun postOperation(
        token: String,
        ticker: String,
        type: String,
        quantity: Int,
    ) = mockMvc.post("/api/portfolio/operations") {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        content =
            objectMapper.writeValueAsString(
                mapOf(
                    "ticker" to ticker,
                    "type" to type,
                    "quantity" to quantity,
                ),
            )
    }

    private fun registerAndLogin(email: String): String {
        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(RegisterUserRequest(email = email, password = "Password123!"))
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
}
