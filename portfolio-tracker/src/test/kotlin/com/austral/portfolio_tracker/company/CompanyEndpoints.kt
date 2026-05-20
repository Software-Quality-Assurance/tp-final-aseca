package com.austral.portfolio_tracker.company

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.User
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
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

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
    @WithMockUser
    fun `001 should create new company`() {
        val request =
            mapOf(
                "ticker" to "AMZN",
                "companyName" to "Amazon Inc",
                "companyPrices" to "180.50",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }
    }

    @Test
    @WithMockUser
    fun `002 should create new company and persist`() {
        val request =
            mapOf(
                "ticker" to "TSLA",
                "companyName" to "Tesla Inc",
                "companyPrices" to "250.75",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }

        val savedCompany = companyRepository.findByTicker("TSLA")
        assertNotNull(savedCompany)
        assertEquals("TSLA", savedCompany?.ticker)
        assertEquals("Tesla Inc", savedCompany?.companyName)
        assertEquals(BigDecimal("250.75"), savedCompany?.companyPrices)
    }

    @Test
    @WithMockUser
    fun `003 should create multiple companies and persist all`() {
        val companies =
            listOf(
                mapOf(
                    "ticker" to "NVDA",
                    "companyName" to "NVIDIA Corporation",
                    "companyPrices" to "875.50",
                ),
                mapOf(
                    "ticker" to "AMD",
                    "companyName" to "Advanced Micro Devices",
                    "companyPrices" to "180.25",
                ),
                mapOf(
                    "ticker" to "INTC",
                    "companyName" to "Intel Corporation",
                    "companyPrices" to "45.75",
                ),
            )

        companies.forEach { company ->
            mockMvc
                .post("/api/company") {
                    with(csrf())
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(company)
                }.andExpect {
                    status { isCreated() }
                }
        }

        val savedNVDA = companyRepository.findByTicker("NVDA")
        val savedAMD = companyRepository.findByTicker("AMD")
        val savedINTC = companyRepository.findByTicker("INTC")

        assertNotNull(savedNVDA)
        assertEquals("NVDA", savedNVDA?.ticker)
        assertEquals("NVIDIA Corporation", savedNVDA?.companyName)
        assertEquals(BigDecimal("875.50"), savedNVDA?.companyPrices)

        assertNotNull(savedAMD)
        assertEquals("AMD", savedAMD?.ticker)
        assertEquals("Advanced Micro Devices", savedAMD?.companyName)
        assertEquals(BigDecimal("180.25"), savedAMD?.companyPrices)

        assertNotNull(savedINTC)
        assertEquals("INTC", savedINTC?.ticker)
        assertEquals("Intel Corporation", savedINTC?.companyName)
        assertEquals(BigDecimal("45.75"), savedINTC?.companyPrices)
    }
}
