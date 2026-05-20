package com.austral.portfolio_tracker.company

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.User
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyEndpoints {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        companyRepository.deleteAll()

        val user =
            User(
                mail = "testuser@example.com",
                password = passwordEncoder.encode("password123")!!,
            )
        userRepository.save(user)

        companyRepository.saveAll(
            listOf(
                Company(
                    ticker = "AAPL",
                    companyName = "Apple Inc",
                    companyPrices = BigDecimal("150.25"),
                ),
                Company(
                    ticker = "MSFT",
                    companyName = "Microsoft Corporation",
                    companyPrices = BigDecimal("380.50"),
                ),
                Company(
                    ticker = "GOOGL",
                    companyName = "Alphabet Inc",
                    companyPrices = BigDecimal("140.75"),
                ),
            ),
        )
    }

    @Test
    @WithMockUser // Esto es un estandar que saltea la autenticacion, que no es lo que se esta testeando aca
    fun `001 should create new company`() {
        mockMvc
            .post("/api/company") {
                with(csrf())
            }.andExpect {
                status { isCreated() }
            }
    }
}
