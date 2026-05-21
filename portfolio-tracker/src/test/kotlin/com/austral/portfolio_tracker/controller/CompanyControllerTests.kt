package com.austral.portfolio_tracker.controller

import com.austral.portfolio_tracker.dto.CreateCompanyRequest
import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.repository.CompanyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    private lateinit var existingCompany: Company

    @BeforeEach
    fun setUp() {
        companyRepository.deleteAll()
        existingCompany =
            companyRepository.save(
                Company(
                    ticker = "AAPL",
                    companyName = "Apple Inc",
                ),
            )
        companyRepository.save(
            Company(
                ticker = "MSFT",
                companyName = "Microsoft Corporation",
            ),
        )
        companyRepository.save(
            Company(
                ticker = "GOOGL",
                companyName = "Alphabet Inc",
            ),
        )
    }

    @Test
    @WithMockUser
    fun `001_should create new company`() {
        val request =
            CreateCompanyRequest(
                ticker = "AMZN",
                companyName = "Amazon Inc",
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
    fun `002_should create new company and persist`() {
        val request =
            CreateCompanyRequest(
                ticker = "TSLA",
                companyName = "Tesla Inc",
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
    }

    @Test
    @WithMockUser
    fun `003_should create multiple companies and persist all`() {
        val companies =
            listOf(
                CreateCompanyRequest(
                    ticker = "NVDA",
                    companyName = "NVIDIA Corporation",
                ),
                CreateCompanyRequest(
                    ticker = "AMD",
                    companyName = "Advanced Micro Devices",
                ),
                CreateCompanyRequest(
                    ticker = "INTC",
                    companyName = "Intel Corporation",
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

        assertNotNull(savedAMD)
        assertEquals("AMD", savedAMD?.ticker)
        assertEquals("Advanced Micro Devices", savedAMD?.companyName)

        assertNotNull(savedINTC)
        assertEquals("INTC", savedINTC?.ticker)
        assertEquals("Intel Corporation", savedINTC?.companyName)
    }

    @Test
    @WithMockUser
    fun `004_should return 409 Conflict when creating company with duplicate ticker`() {
        val duplicateRequest =
            CreateCompanyRequest(
                ticker = existingCompany.ticker,
                companyName = "Another Company",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(duplicateRequest)
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error") { value("Invalid operation: company with ticker already exists") }
            }
    }

    @Test
    @WithMockUser
    fun `005_should return 400 Bad Request when ticker is null`() {
        val request =
            CreateCompanyRequest(
                ticker = null,
                companyName = "Valid Company",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `006_should return 400 Bad Request when ticker is blank`() {
        val request =
            CreateCompanyRequest(
                ticker = "   ",
                companyName = "Valid Company",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `007_should return 400 Bad Request when ticker is empty string`() {
        val request =
            CreateCompanyRequest(
                ticker = "",
                companyName = "Valid Company",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `008_should return 400 Bad Request when companyName is null`() {
        val request =
            CreateCompanyRequest(
                ticker = "VALID",
                companyName = null,
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `009_should return 400 Bad Request when companyName is blank`() {
        val request =
            CreateCompanyRequest(
                ticker = "VALID",
                companyName = "   ",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `010_should return 400 Bad Request when companyName is empty string`() {
        val request =
            CreateCompanyRequest(
                ticker = "VALID",
                companyName = "",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
    }
}
