package com.austral.portfolio_tracker.watchlist

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.User
import com.austral.portfolio_tracker.entity.Watchlist
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.UserRepository
import com.austral.portfolio_tracker.repository.WatchlistRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@DataJpaTest
@ActiveProfiles("test")
class WatchlistTests {
    @Autowired
    private lateinit var watchlistRepository: WatchlistRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Test
    fun `should save a watchlist entry with required relations`() {
        val user =
            userRepository.save(
                User(
                    mail = "wuser@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val company =
            companyRepository.save(
                Company(
                    ticker = "WST",
                    companyName = "Watch Corp",
                    cik = "0009100001",
                    companyPrices = BigDecimal("10.00"),
                ),
            )

        val watch =
            Watchlist(
                user = user,
                company = company,
            )

        val saved = watchlistRepository.save(watch)

        assertNotNull(saved.id)
        assertEquals(user.id, saved.user?.id)
        assertEquals(company.id, saved.company?.id)
    }

    @Test
    fun `should fail when saving watchlist without user or company`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "WST2",
                    companyName = "Watch Two",
                    cik = "0009100002",
                    companyPrices = BigDecimal("20.00"),
                ),
            )

        val noUser =
            Watchlist(
                user = null,
                company = company,
            )

        assertThrows<DataIntegrityViolationException> {
            watchlistRepository.save(noUser)
        }

        val user =
            userRepository.save(
                User(
                    mail = "wuser2@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val noCompany =
            Watchlist(
                user = user,
                company = null,
            )

        assertThrows<DataIntegrityViolationException> {
            watchlistRepository.save(noCompany)
        }
    }

    @Test
    fun `should cascade delete watchlist when deleting user`() {
        val user =
            User(
                mail = "cascade-watch-user@example.com",
                password = "pwd",
                history = mutableListOf(),
                watchlist = mutableListOf(),
            )

        val company =
            companyRepository.save(
                Company(
                    ticker = "CWCD",
                    companyName = "Cascade Watch Inc",
                    cik = "0009100010",
                    companyPrices = BigDecimal("30.00"),
                ),
            )

        val watchEntry =
            Watchlist(
                user = user,
                company = company,
            )

        user.watchlist.add(watchEntry)
        val savedUser = userRepository.save(user)

        val all = watchlistRepository.findAll()
        assertEquals(1, all.size)

        userRepository.delete(savedUser)

        val remaining = watchlistRepository.findAll()
        assertEquals(0, remaining.size)
    }

    @Test
    fun `should cascade delete watchlist when deleting company`() {
        val user =
            userRepository.save(
                User(
                    mail = "cw-user@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val company =
            Company(
                ticker = "CWCD2",
                companyName = "Cascade Watch Two",
                cik = "0009100020",
                companyPrices = BigDecimal("40.00"),
            )

        val watchEntry =
            Watchlist(
                user = user,
                company = company,
            )

        company.watchlist.add(watchEntry)
        val savedCompany = companyRepository.save(company)

        val all = watchlistRepository.findAll()
        assertEquals(1, all.size)

        companyRepository.delete(savedCompany)

        val remaining = watchlistRepository.findAll()
        assertEquals(0, remaining.size)
    }

    @Test
    fun `should retrieve watchlist entries by user and company relations`() {
        val user1 =
            userRepository.save(
                User(
                    mail = "rel-user1@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val user2 =
            userRepository.save(
                User(
                    mail = "rel-user2@example.com",
                    password = "pwd",
                    history = mutableListOf(),
                    watchlist = mutableListOf(),
                ),
            )

        val company1 =
            companyRepository.save(
                Company(
                    ticker = "REL1",
                    companyName = "Relation One",
                    cik = "0009100100",
                    companyPrices = BigDecimal("50.00"),
                ),
            )

        val company2 =
            companyRepository.save(
                Company(
                    ticker = "REL2",
                    companyName = "Relation Two",
                    cik = "0009100200",
                    companyPrices = BigDecimal("75.00"),
                ),
            )

        // Create multiple watchlist entries
        val watch1 = Watchlist(user = user1, company = company1)
        val watch2 = Watchlist(user = user1, company = company2)
        val watch3 = Watchlist(user = user2, company = company1)

        watchlistRepository.save(watch1)
        watchlistRepository.save(watch2)
        watchlistRepository.save(watch3)

        val all = watchlistRepository.findAll()
        assertEquals(3, all.size)

        // Verify relationships are maintained
        assertEquals(user1.id, watch1.user?.id)
        assertEquals(company1.id, watch1.company?.id)
        assertEquals(user1.id, watch2.user?.id)
        assertEquals(company2.id, watch2.company?.id)
    }
}
