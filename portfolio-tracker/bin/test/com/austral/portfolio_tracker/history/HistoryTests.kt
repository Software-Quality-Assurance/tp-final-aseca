package com.austral.portfolio_tracker.history

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.History
import com.austral.portfolio_tracker.entity.Price
import com.austral.portfolio_tracker.entity.TransactionTypeEnum
import com.austral.portfolio_tracker.entity.User
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.HistoryRepository
import com.austral.portfolio_tracker.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.math.BigDecimal
import java.time.Instant

@DataJpaTest
class HistoryTests {
    @Autowired
    private lateinit var historyRepository: HistoryRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Test
    fun `should save a history entry with required relations`() {
        val user =
            userRepository.save(
                User(
                    mail = "huser@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val company =
            companyRepository.save(
                Company(
                    ticker = "HST",
                    companyName = "History Corp",
                ).apply {
                    prices.add(
                        Price(
                            ticker = "HST",
                            unityPrice = BigDecimal("100.00"),
                            timestamp = Instant.parse("2026-05-01T00:00:00Z"),
                            company = this,
                        ),
                    )
                },
            )

        val history =
            History(
                numberOfStocks = 5,
                transactionValue = BigDecimal("500.00"),
                transactionTypeEnum = TransactionTypeEnum.SELL,
                timestamp = Instant.parse("2026-05-16T09:00:00Z"),
                user = user,
                company = company,
            )

        val saved = historyRepository.save(history)

        assertNotNull(saved.id)
        assertEquals(5, saved.numberOfStocks)
        assertEquals(BigDecimal("500.00"), saved.transactionValue)
        assertEquals(TransactionTypeEnum.SELL, saved.transactionTypeEnum)
        assertEquals(user.id, saved.user?.id)
        assertEquals(company.id, saved.company?.id)
    }

    @Test
    fun `should cascade delete history when deleting user`() {
        val user =
            User(
                mail = "cascade-user@example.com",
                password = "pwd",
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )

        val company =
            companyRepository.save(
                Company(
                    ticker = "CDEL",
                    companyName = "Cascade Delete Inc",
                ).apply {
                    prices.add(
                        Price(
                            ticker = "CDEL",
                            unityPrice = BigDecimal("75.00"),
                            timestamp = Instant.parse("2026-05-01T00:00:00Z"),
                            company = this,
                        ),
                    )
                },
            )

        val historyEntry =
            History(
                numberOfStocks = 3,
                transactionValue = BigDecimal("225.00"),
                transactionTypeEnum = TransactionTypeEnum.BUY,
                timestamp = Instant.parse("2026-05-16T10:00:00Z"),
                user = user,
                company = company,
            )

        // Add history to user and save user so cascade persists the history
        user.history.add(historyEntry)
        val savedUser = userRepository.save(user)

        // Ensure history persisted
        val allHist = historyRepository.findAll()
        assertEquals(1, allHist.size)

        // Delete user and expect history to be removed due to cascade
        userRepository.delete(savedUser)

        val remaining = historyRepository.findAll()
        assertEquals(0, remaining.size)
    }

    @Test
    fun `should save and retrieve multiple transaction types`() {
        val user =
            userRepository.save(
                User(
                    mail = "multi-trans@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val company =
            companyRepository.save(
                Company(
                    ticker = "MULTI",
                    companyName = "Multi Transaction Corp",
                ).apply {
                    prices.add(
                        Price(
                            ticker = "MULTI",
                            unityPrice = BigDecimal("100.00"),
                            timestamp = Instant.parse("2026-05-01T00:00:00Z"),
                            company = this,
                        ),
                    )
                },
            )

        // Save a BUY transaction
        val buyHistory =
            History(
                numberOfStocks = 10,
                transactionValue = BigDecimal("1000.00"),
                transactionTypeEnum = TransactionTypeEnum.BUY,
                timestamp = Instant.parse("2026-05-16T11:00:00Z"),
                user = user,
                company = company,
            )
        val savedBuy = historyRepository.save(buyHistory)

        // Save a SELL transaction
        val sellHistory =
            History(
                numberOfStocks = 5,
                transactionValue = BigDecimal("550.00"),
                transactionTypeEnum = TransactionTypeEnum.SELL,
                timestamp = Instant.parse("2026-05-16T12:00:00Z"),
                user = user,
                company = company,
            )
        val savedSell = historyRepository.save(sellHistory)

        // Verify both exist
        val all = historyRepository.findAll()
        assertEquals(2, all.size)
        assertEquals(TransactionTypeEnum.BUY, savedBuy.transactionTypeEnum)
        assertEquals(TransactionTypeEnum.SELL, savedSell.transactionTypeEnum)
    }
}
