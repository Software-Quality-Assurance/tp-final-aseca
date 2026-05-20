package com.austral.portfolio_tracker.user

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
class UserTests {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Test
    fun `should save a user with all required fields`() {
        val user =
            User(
                mail = "user@example.com",
                password = "hashedPassword123",
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )

        val savedUser = userRepository.save(user)

        assertNotNull(savedUser.id)
        assertEquals("user@example.com", savedUser.mail)
        assertEquals("hashedPassword123", savedUser.password)
        assertEquals(0, savedUser.history.size)
        assertEquals(0, savedUser.watchlist.size)
    }

    @Test
    fun `should retrieve a user by mail`() {
        val user =
            User(
                mail = "test@example.com",
                password = "hashedPassword",
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )
        userRepository.save(user)

        val foundUser = userRepository.findByMail("test@example.com")

        assertNotNull(foundUser)
        assertEquals("test@example.com", foundUser?.mail)
    }

    @Test
    fun `should update user watchlist`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "AAPL",
                    companyName = "Apple Inc.",
                    companyPrices = BigDecimal("150.25"),
                ),
            )

        val user =
            User(
                mail = "watchlist@example.com",
                password = "hashedPassword",
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
                    companyPrices = BigDecimal("420.10"),
                ),
            )

        val user =
            User(
                mail = "history@example.com",
                password = "hashedPassword",
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
    fun `should throw exception when saving a user with duplicate mail`() {
        val user1 =
            User(
                mail = "duplicate@example.com",
                password = "hashedPassword1",
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )
        userRepository.save(user1)

        val user2 =
            User(
                mail = "duplicate@example.com",
                password = "hashedPassword2",
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )

        assertThrows<DataIntegrityViolationException> {
            userRepository.save(user2)
        }
    }
}
