package com.austral.portfolio_tracker.company

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.History
import com.austral.portfolio_tracker.entity.Price
import com.austral.portfolio_tracker.entity.TransactionTypeEnum
import com.austral.portfolio_tracker.entity.User
import com.austral.portfolio_tracker.entity.Watchlist
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant

@DataJpaTest
@ActiveProfiles("test")
class CompanyTests {
    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `should save a company with all required fields`() {
        val company =
            Company(
                ticker = "GOOG",
                companyName = "Google LLC",
            )

        // add initial price
        company.prices.add(
            Price(
                ticker = "GOOG",
                unityPrice = BigDecimal("2800.00"),
                timestamp = Instant.parse("2026-05-01T00:00:00Z"),
                company = company,
            ),
        )

        val savedCompany = companyRepository.save(company)

        assertNotNull(savedCompany.id)
        assertEquals("GOOG", savedCompany.ticker)
        assertEquals("Google LLC", savedCompany.companyName)
        assertEquals(1, savedCompany.prices.size)
        assertEquals(BigDecimal("2800.00"), savedCompany.prices[0].unityPrice)
        assertEquals(0, savedCompany.history.size)
        assertEquals(0, savedCompany.watchlist.size)
    }

    @Test
    fun `should retrieve a company by ticker`() {
        val company =
            Company(
                ticker = "AMZN",
                companyName = "Amazon.com, Inc.",
            )
        company.prices.add(
            Price(
                ticker = "AMZN",
                unityPrice = BigDecimal("3300.50"),
                timestamp = Instant.parse("2026-05-01T00:00:00Z"),
                company = company,
            ),
        )
        companyRepository.save(company)

        val found = companyRepository.findByTicker("AMZN")

        assertNotNull(found)
        assertEquals("AMZN", found?.ticker)
    }

    @Test
    fun `should update company history`() {
        val user =
            userRepository.save(
                User(
                    mail = "hist-user@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val company =
            Company(
                ticker = "TSLA",
                companyName = "Tesla, Inc.",
            )

        company.prices.add(
            Price(ticker = "TSLA", unityPrice = BigDecimal("650.00"), timestamp = Instant.parse("2026-05-01T00:00:00Z"), company = company),
        )

        val savedCompany = companyRepository.save(company)

        val historyEntry =
            History(
                numberOfStocks = 2,
                transactionValue = BigDecimal("1300.00"),
                transactionTypeEnum = TransactionTypeEnum.BUY,
                timestamp = Instant.parse("2026-05-15T12:00:00Z"),
                user = user,
                company = savedCompany,
            )

        savedCompany.history.add(historyEntry)

        val updated = companyRepository.save(savedCompany)

        assertEquals(1, updated.history.size)
        val savedHist = updated.history[0]
        assertNotNull(savedHist.id)
        assertEquals(2, savedHist.numberOfStocks)
        assertEquals(BigDecimal("1300.00"), savedHist.transactionValue)
        assertEquals(TransactionTypeEnum.BUY, savedHist.transactionTypeEnum)
        assertEquals(user.id, savedHist.user?.id)
    }

    @Test
    fun `should update company watchlist`() {
        val user =
            userRepository.save(
                User(
                    mail = "watch-user@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val company =
            Company(
                ticker = "NFLX",
                companyName = "Netflix, Inc.",
            )

        company.prices.add(
            Price(ticker = "NFLX", unityPrice = BigDecimal("500.00"), timestamp = Instant.parse("2026-05-01T00:00:00Z"), company = company),
        )

        val savedCompany = companyRepository.save(company)

        val watchEntry =
            Watchlist(
                user = user,
                company = savedCompany,
            )

        savedCompany.watchlist.add(watchEntry)

        val updated = companyRepository.save(savedCompany)

        assertEquals(1, updated.watchlist.size)
        assertNotNull(updated.watchlist[0].id)
        assertEquals(user.id, updated.watchlist[0].user?.id)
    }

    @Test
    fun `should update user watchlist`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "AAPL",
                    companyName = "Apple Inc.",
                ).apply {
                    prices.add(
                        Price(
                            ticker = "AAPL",
                            unityPrice = BigDecimal("150.25"),
                            timestamp = Instant.parse("2026-05-01T00:00:00Z"),
                            company = this,
                        ),
                    )
                },
            )

        val user =
            User(
                mail = "watchlist-from-user@example.com",
                password = "pwd",
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )

        val watchEntry =
            Watchlist(
                user = user,
                company = company,
            )
        user.watchlist.add(watchEntry)

        val savedUser = userRepository.save(user)

        assertEquals(1, savedUser.watchlist.size)
        assertNotNull(savedUser.watchlist[0].id)
        assertEquals(company.id, savedUser.watchlist[0].company?.id)
    }

    @Test
    fun `should update user history`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "MSFT",
                    companyName = "Microsoft Corporation",
                ).apply {
                    prices.add(
                        Price(
                            ticker = "MSFT",
                            unityPrice = BigDecimal("420.10"),
                            timestamp = Instant.parse("2026-05-01T00:00:00Z"),
                            company = this,
                        ),
                    )
                },
            )

        val user =
            User(
                mail = "history-from-user@example.com",
                password = "pwd",
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )

        val historyEntry =
            History(
                numberOfStocks = 3,
                transactionValue = BigDecimal("123.45"),
                transactionTypeEnum = TransactionTypeEnum.BUY,
                timestamp = Instant.parse("2026-05-15T10:15:30Z"),
                user = user,
                company = company,
            )
        user.history.add(historyEntry)

        val savedUser = userRepository.save(user)

        assertEquals(1, savedUser.history.size)
        val savedHist = savedUser.history[0]
        assertNotNull(savedHist.id)
        assertEquals(3, savedHist.numberOfStocks)
        assertEquals(BigDecimal("123.45"), savedHist.transactionValue)
        assertEquals(TransactionTypeEnum.BUY, savedHist.transactionTypeEnum)
        assertEquals(company.id, savedHist.company?.id)
    }

    @Test
    fun `should throw exception when saving a company with duplicate ticker`() {
        val company1 =
            Company(
                ticker = "DUP",
                companyName = "Dup One",
            )
        company1.prices.add(
            Price(ticker = "DUP", unityPrice = BigDecimal("10.00"), timestamp = Instant.parse("2026-05-01T00:00:00Z"), company = company1),
        )
        companyRepository.save(company1)

        val company2 =
            Company(
                ticker = "DUP",
                companyName = "Dup Two",
            )

        assertThrows<DataIntegrityViolationException> {
            companyRepository.save(company2)
        }
    }
}
