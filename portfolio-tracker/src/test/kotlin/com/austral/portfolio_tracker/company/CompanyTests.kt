package com.austral.portfolio_tracker.company

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.History
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
                companyPrices = BigDecimal("2800.00"),
            )

        val savedCompany = companyRepository.save(company)

        assertNotNull(savedCompany.id)
        assertEquals("GOOG", savedCompany.ticker)
        assertEquals("Google LLC", savedCompany.companyName)
        assertEquals(BigDecimal("2800.00"), savedCompany.companyPrices)
        assertEquals(0, savedCompany.history.size)
        assertEquals(0, savedCompany.watchlist.size)
    }

    @Test
    fun `should retrieve a company by ticker`() {
        val company =
            Company(
                ticker = "AMZN",
                companyName = "Amazon.com, Inc.",
                companyPrices = BigDecimal("3300.50"),
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
                companyPrices = BigDecimal("650.00"),
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
                companyPrices = BigDecimal("500.00"),
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
    fun `should throw exception when saving a company with duplicate ticker`() {
        val company1 =
            Company(
                ticker = "DUP",
                companyName = "Dup One",
                companyPrices = BigDecimal("10.00"),
            )
        companyRepository.save(company1)

        val company2 =
            Company(
                ticker = "DUP",
                companyName = "Dup Two",
                companyPrices = BigDecimal("20.00"),
            )

        assertThrows<DataIntegrityViolationException> {
            companyRepository.save(company2)
        }
    }
}
