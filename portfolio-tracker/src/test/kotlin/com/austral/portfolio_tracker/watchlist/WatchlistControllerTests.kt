package com.austral.portfolio_tracker.watchlist

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.Price
import com.austral.portfolio_tracker.entity.User
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.PriceRepository
import com.austral.portfolio_tracker.repository.UserRepository
import com.austral.portfolio_tracker.repository.WatchlistRepository
import com.austral.portfolio_tracker.security.JwtTokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WatchlistControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var priceRepository: PriceRepository

    @Autowired
    private lateinit var watchlistRepository: WatchlistRepository

    @Autowired
    private lateinit var jwtTokenService: JwtTokenService

    private lateinit var apple: Company
    private lateinit var tesla: Company
    private lateinit var token: String

    @BeforeEach
    fun setup() {
        watchlistRepository.deleteAll()
        priceRepository.deleteAll()
        companyRepository.deleteAll()
        userRepository.deleteAll()

        apple = saveCompanyWithPrice("AAPL", "Apple Inc.", "185.50", "2026-05-20T10:00:00Z")
        tesla = saveCompanyWithPrice("TSLA", "Tesla, Inc.", "220.75", "2026-05-20T11:00:00Z")

        val user =
            userRepository.save(
                User(
                    mail = "watchlist-api@example.com",
                    password = "Password123!",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )
        token = jwtTokenService.generateToken(requireNotNull(user.id), user.mail)
    }

    @Test
    fun `should add list and remove companies from watchlist`() {
        mockMvc
            .post("/api/watchlist/AAPL") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isCreated() }
            }

        mockMvc
            .post("/api/watchlist/TSLA") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isCreated() }
            }

        mockMvc
            .get("/api/watchlist") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.items.length()") { value(2) }
                jsonPath("$.items[0].ticker") { value("AAPL") }
                jsonPath("$.items[0].companyName") { value("Apple Inc.") }
                jsonPath("$.items[0].currentPrice") { value(185.50) }
                jsonPath("$.items[0].lastUpdatedAt") { value("2026-05-20T10:00:00Z") }
                jsonPath("$.items[1].ticker") { value("TSLA") }
            }

        mockMvc
            .delete("/api/watchlist/AAPL") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNoContent() }
            }

        mockMvc
            .get("/api/watchlist") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.items.length()") { value(1) }
                jsonPath("$.items[0].ticker") { value("TSLA") }
            }
    }

    @Test
    fun `should reject duplicate watchlist entries`() {
        mockMvc
            .post("/api/watchlist/AAPL") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isCreated() }
            }

        mockMvc
            .post("/api/watchlist/AAPL") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error") { value("Company already in watchlist") }
            }
    }

    @Test
    fun `should return not found when adding or removing unknown ticker`() {
        mockMvc
            .post("/api/watchlist/UNKNOWN") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("Company with ticker UNKNOWN not found") }
            }

        mockMvc
            .delete("/api/watchlist/AAPL") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("Watchlist entry not found for ticker AAPL") }
            }
    }

    private fun saveCompanyWithPrice(
        ticker: String,
        companyName: String,
        price: String,
        timestamp: String,
    ): Company =
        companyRepository.save(
            Company(
                ticker = ticker,
                companyName = companyName,
            ).apply {
                prices.add(
                    Price(
                        ticker = ticker,
                        unityPrice = BigDecimal(price),
                        timestamp = Instant.parse(timestamp),
                        company = this,
                    ),
                )
            },
        )
}
