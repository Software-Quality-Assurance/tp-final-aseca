package com.austral.portfolio_tracker.price

import com.austral.portfolio_tracker.company.CompanyRepository
import com.austral.portfolio_tracker.entities.Company
import com.austral.portfolio_tracker.entities.Price
import com.austral.portfolio_tracker.portfolio.PriceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.time.Instant

@DataJpaTest
class PriceTests {
    @Autowired
    private lateinit var priceRepository: PriceRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Test
    fun `should save a price with required fields`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "AAPL",
                    companyName = "Apple Inc.",
                ),
            )

        val price =
            Price(
                ticker = "AAPL",
                unityPrice = BigDecimal("150.25"),
                timestamp = Instant.parse("2026-05-20T10:00:00Z"),
                company = company,
            )

        val saved = priceRepository.save(price)

        assertNotNull(saved.id)
        assertEquals("AAPL", saved.ticker)
        assertEquals(BigDecimal("150.25"), saved.unityPrice)
        assertEquals(company.id, saved.company?.id)
    }

    @Test
    fun `should retrieve prices by ticker`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "MSFT",
                    companyName = "Microsoft Corporation",
                ),
            )

        priceRepository.save(
            Price(
                ticker = "MSFT",
                unityPrice = BigDecimal("420.10"),
                timestamp = Instant.parse("2026-05-20T10:00:00Z"),
                company = company,
            ),
        )
        priceRepository.save(
            Price(
                ticker = "MSFT",
                unityPrice = BigDecimal("421.50"),
                timestamp = Instant.parse("2026-05-20T11:00:00Z"),
                company = company,
            ),
        )

        val found = priceRepository.findByTicker("MSFT")

        assertEquals(2, found.size)
    }

    @Test
    fun `should cascade delete prices when deleting company`() {
        val company =
            Company(
                ticker = "AMZN",
                companyName = "Amazon.com, Inc.",
            ).apply {
                prices.add(
                    Price(
                        ticker = "AMZN",
                        unityPrice = BigDecimal("3300.50"),
                        timestamp = Instant.parse("2026-05-20T09:00:00Z"),
                        company = this,
                    ),
                )
                prices.add(
                    Price(
                        ticker = "AMZN",
                        unityPrice = BigDecimal("3310.75"),
                        timestamp = Instant.parse("2026-05-20T10:00:00Z"),
                        company = this,
                    ),
                )
            }

        val savedCompany = companyRepository.save(company)
        val companyId = savedCompany.id!!

        assertEquals(2, priceRepository.findByCompanyIdOrderByTimestampAsc(companyId).size)

        savedCompany.active = false
        companyRepository.save(savedCompany)

        assertEquals(2, priceRepository.findByCompanyIdOrderByTimestampAsc(companyId).size)
    }

    @Test
    fun `should fail when saving price without company`() {
        val invalidPrice =
            Price(
                ticker = "NVDA",
                unityPrice = BigDecimal("1000.00"),
                timestamp = Instant.now(),
                company = null,
            )

        assertThrows<DataIntegrityViolationException> {
            priceRepository.saveAndFlush(invalidPrice)
        }
    }

    @Test
    fun `should return prices ordered by timestamp for company`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "TSLA",
                    companyName = "Tesla, Inc.",
                ),
            )

        priceRepository.save(
            Price(
                ticker = "TSLA",
                unityPrice = BigDecimal("640.00"),
                timestamp = Instant.parse("2026-05-20T12:00:00Z"),
                company = company,
            ),
        )
        priceRepository.save(
            Price(
                ticker = "TSLA",
                unityPrice = BigDecimal("630.00"),
                timestamp = Instant.parse("2026-05-20T10:00:00Z"),
                company = company,
            ),
        )

        val ordered = priceRepository.findByCompanyIdOrderByTimestampAsc(company.id!!)

        assertEquals(2, ordered.size)
        assertEquals(BigDecimal("630.00"), ordered[0].unityPrice)
        assertEquals(BigDecimal("640.00"), ordered[1].unityPrice)
    }
}
