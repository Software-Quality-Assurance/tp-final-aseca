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
import org.springframework.test.web.servlet.get
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

    @Test
    @WithMockUser
    fun `011_should verify id is not null or blank when creating company`() {
        val request =
            CreateCompanyRequest(
                ticker = "GOOG",
                companyName = "Google LLC",
            )

        mockMvc
            .post("/api/company") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }

        val savedCompany = companyRepository.findByTicker("GOOG")
        assertNotNull(savedCompany)
        assertNotNull(savedCompany?.id)
        assertEquals(true, savedCompany?.id != null && savedCompany?.id!! > 0)
    }

    // Search Company Tests

    @Test
    @WithMockUser
    fun `012_search_company_by_name_exact`() {
        mockMvc
            .get("/api/company/search?name=Apple Inc") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("AAPL") }
                jsonPath("$[0].companyName") { value("Apple Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `013_search_company_by_name_partial_lowercase`() {
        mockMvc
            .get("/api/company/search?name=apple") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("AAPL") }
                jsonPath("$[0].companyName") { value("Apple Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `014_search_company_by_name_partial_uppercase`() {
        mockMvc
            .get("/api/company/search?name=APPLE") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("AAPL") }
                jsonPath("$[0].companyName") { value("Apple Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `015_search_company_by_name_partial_mixed_case`() {
        mockMvc
            .get("/api/company/search?name=AppLe") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("AAPL") }
                jsonPath("$[0].companyName") { value("Apple Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `016_search_company_by_name_partial_substring`() {
        mockMvc
            .get("/api/company/search?name=Micro") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("MSFT") }
                jsonPath("$[0].companyName") { value("Microsoft Corporation") }
            }
    }

    @Test
    @WithMockUser
    fun `017_search_company_by_name_partial_substring_lowercase`() {
        mockMvc
            .get("/api/company/search?name=soft") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("MSFT") }
                jsonPath("$[0].companyName") { value("Microsoft Corporation") }
            }
    }

    @Test
    @WithMockUser
    fun `018_search_company_by_ticker_exact`() {
        mockMvc
            .get("/api/company/search?ticker=AAPL") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("AAPL") }
                jsonPath("$[0].companyName") { value("Apple Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `019_search_company_by_ticker_lowercase`() {
        mockMvc
            .get("/api/company/search?ticker=aapl") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("AAPL") }
                jsonPath("$[0].companyName") { value("Apple Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `020_search_company_by_ticker_mixed_case`() {
        mockMvc
            .get("/api/company/search?ticker=MsFt") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("MSFT") }
                jsonPath("$[0].companyName") { value("Microsoft Corporation") }
            }
    }

    @Test
    @WithMockUser
    fun `021_search_company_by_partial_ticker_lowercase`() {
        mockMvc
            .get("/api/company/search?ticker=goo") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("GOOGL") }
                jsonPath("$[0].companyName") { value("Alphabet Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `022_search_company_by_partial_ticker_uppercase`() {
        mockMvc
            .get("/api/company/search?ticker=GOO") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("GOOGL") }
                jsonPath("$[0].companyName") { value("Alphabet Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `023_search_company_by_partial_ticker_mixed_case`() {
        mockMvc
            .get("/api/company/search?ticker=GoOgL") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("GOOGL") }
                jsonPath("$[0].companyName") { value("Alphabet Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `024_search_company_no_results_by_name_returns_404`() {
        mockMvc
            .get("/api/company/search?name=NonExistentCompany") {
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser
    fun `025_search_company_no_results_by_ticker_returns_404`() {
        mockMvc
            .get("/api/company/search?ticker=XYZ") {
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser
    fun `026_search_company_multiple_results_by_partial_name`() {
        mockMvc
            .get("/api/company/search?name=Inc") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
            }
    }

    @Test
    @WithMockUser
    fun `027_search_company_multiple_results_by_partial_name_lowercase`() {
        mockMvc
            .get("/api/company/search?name=inc") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
            }
    }

    @Test
    @WithMockUser
    fun `028_search_company_empty_query`() {
        mockMvc
            .get("/api/company/search?name=") {
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `029_search_company_blank_query`() {
        mockMvc
            .get("/api/company/search") {
                param("name", "   ")
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `030_search_company_missing_query_parameter`() {
        mockMvc
            .get("/api/company/search") {
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `031_search_given_no_results_when_no_matches_then_returns_404_not_found`() {
        mockMvc
            .get("/api/company/search?name=ZZZNonExistentCompanyZZZ") {
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser
    fun `032_search_given_no_results_by_ticker_when_no_matches_then_returns_404_not_found`() {
        mockMvc
            .get("/api/company/search?ticker=ZZZZ") {
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser
    fun `033_search_given_valid_query_when_results_exist_then_returns_200_ok_with_results`() {
        mockMvc
            .get("/api/company/search?name=Apple") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("AAPL") }
                jsonPath("$[0].companyName") { value("Apple Inc") }
            }
    }

    @Test
    @WithMockUser
    fun `034_search_given_valid_ticker_query_when_results_exist_then_returns_200_ok_with_results`() {
        mockMvc
            .get("/api/company/search?ticker=MS") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("MSFT") }
                jsonPath("$[0].companyName") { value("Microsoft Corporation") }
            }
    }
}
