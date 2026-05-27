package com.austral.portfolio_tracker.portfolio

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PortfolioServiceTests {

    @BeforeEach
    fun setup() {
        val user = User(
            password = "password123",
            mail = "mail@example.com"
        )

        val company = Company(
            ticker="NEWH",
            companyName="NewHydrogen, Inc.",
        )
    }

    @Test
    fun `Test 001 - can create a history row`() {

    }
}