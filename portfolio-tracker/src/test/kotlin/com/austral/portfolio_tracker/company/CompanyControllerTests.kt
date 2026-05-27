package com.austral.portfolio_tracker.company

import com.austral.portfolio_tracker.dto.CreateCompanyRequest
import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.entity.History
import com.austral.portfolio_tracker.entity.Price
import com.austral.portfolio_tracker.entity.TransactionTypeEnum
import com.austral.portfolio_tracker.entity.User
import com.austral.portfolio_tracker.entity.Watchlist
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.austral.portfolio_tracker.repository.HistoryRepository
import com.austral.portfolio_tracker.repository.PriceRepository
import com.austral.portfolio_tracker.repository.UserRepository
import com.austral.portfolio_tracker.repository.WatchlistRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanyControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var priceRepository: PriceRepository

    @Autowired
    private lateinit var historyRepository: HistoryRepository

    @Autowired
    private lateinit var watchlistRepository: WatchlistRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        companyRepository.deleteAll()
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
                with(SecurityMockMvcRequestPostProcessors.csrf())
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
                with(SecurityMockMvcRequestPostProcessors.csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }

        val savedCompany = companyRepository.findByTickerAndActiveTrue("TSLA")
        Assertions.assertNotNull(savedCompany)
        Assertions.assertEquals("TSLA", savedCompany?.ticker)
        Assertions.assertEquals("Tesla Inc", savedCompany?.companyName)
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
                    with(SecurityMockMvcRequestPostProcessors.csrf())
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(company)
                }.andExpect {
                    status { isCreated() }
                }
        }

        val savedNVDA = companyRepository.findByTickerAndActiveTrue("NVDA")
        val savedAMD = companyRepository.findByTickerAndActiveTrue("AMD")
        val savedINTC = companyRepository.findByTickerAndActiveTrue("INTC")

        Assertions.assertNotNull(savedNVDA)
        Assertions.assertEquals("NVDA", savedNVDA?.ticker)
        Assertions.assertEquals("NVIDIA Corporation", savedNVDA?.companyName)

        Assertions.assertNotNull(savedAMD)
        Assertions.assertEquals("AMD", savedAMD?.ticker)
        Assertions.assertEquals("Advanced Micro Devices", savedAMD?.companyName)

        Assertions.assertNotNull(savedINTC)
        Assertions.assertEquals("INTC", savedINTC?.ticker)
        Assertions.assertEquals("Intel Corporation", savedINTC?.companyName)
    }

    @Test
    @WithMockUser
    fun `004_should return 409 Conflict when creating company with duplicate ticker`() {
        val existingCompany =
            companyRepository.save(
                Company(
                    ticker = "AAPL",
                    companyName = "Apple Inc",
                ),
            )

        val duplicateRequest =
            CreateCompanyRequest(
                ticker = existingCompany.ticker,
                companyName = "Another Company",
            )

        mockMvc
            .post("/api/company") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
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
        mockMvc
            .post("/api/company") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"ticker": null, "companyName": "Valid Company"}"""
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
                with(SecurityMockMvcRequestPostProcessors.csrf())
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
                with(SecurityMockMvcRequestPostProcessors.csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `008_should return 400 Bad Request when companyName is null`() {
        mockMvc
            .post("/api/company") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"ticker": "VALID", "companyName": null}"""
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
                with(SecurityMockMvcRequestPostProcessors.csrf())
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
                with(SecurityMockMvcRequestPostProcessors.csrf())
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
                with(SecurityMockMvcRequestPostProcessors.csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }

        val savedCompany = companyRepository.findByTickerAndActiveTrue("GOOG")
        Assertions.assertNotNull(savedCompany)
        Assertions.assertNotNull(savedCompany?.id)
        Assertions.assertEquals(true, savedCompany?.id != null && savedCompany?.id!! > 0)
    }

    @Test
    @WithMockUser
    fun `012_get_companies_empty_list_returns_200_ok_with_empty_result`() {
        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.content.length()") { value(0) }
                jsonPath("$.currentPage") { value(1) }
                jsonPath("$.pageSize") { value(10) }
                jsonPath("$.totalElements") { value(0) }
                jsonPath("$.totalPages") { value(0) }
            }
    }

    // Search Company Tests

    private fun setupSearchCompanies() {
        companyRepository.save(Company(ticker = "AAPL", companyName = "Apple Inc"))
        companyRepository.save(Company(ticker = "MSFT", companyName = "Microsoft Corporation"))
        companyRepository.save(Company(ticker = "GOOGL", companyName = "Alphabet Inc"))
    }

    @Test
    @WithMockUser
    fun `013_search_company_by_name_exact`() {
        setupSearchCompanies()

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
    fun `014_search_company_by_name_partial_lowercase`() {
        setupSearchCompanies()

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
    fun `015_search_company_by_name_partial_uppercase`() {
        setupSearchCompanies()

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
    fun `016_search_company_by_name_partial_mixed_case`() {
        setupSearchCompanies()

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
    fun `017_search_company_by_name_partial_substring`() {
        setupSearchCompanies()

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
    fun `018_search_company_by_name_partial_substring_lowercase`() {
        setupSearchCompanies()

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
    fun `019_search_company_by_ticker_exact`() {
        setupSearchCompanies()

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
    fun `020_search_company_by_ticker_lowercase`() {
        setupSearchCompanies()

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
    fun `021_search_company_by_ticker_mixed_case`() {
        setupSearchCompanies()

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
    fun `022_search_company_by_partial_ticker_lowercase`() {
        setupSearchCompanies()

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
    fun `023_search_company_by_partial_ticker_uppercase`() {
        setupSearchCompanies()

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
    fun `024_search_company_by_partial_ticker_mixed_case`() {
        setupSearchCompanies()

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
    fun `025_search_company_no_results_by_name_returns_empty_list`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search?name=NonExistentCompany") {
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    @WithMockUser
    fun `026_search_company_no_results_by_ticker_returns_empty_list`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search?ticker=XYZ") {
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    @WithMockUser
    fun `027_search_company_multiple_results_by_partial_name`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search?name=Inc") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
            }
    }

    @Test
    @WithMockUser
    fun `028_search_company_multiple_results_by_partial_name_lowercase`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search?name=inc") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
            }
    }

    @Test
    @WithMockUser
    fun `029_search_company_empty_query`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search?name=") {
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `030_search_company_blank_query`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search") {
                param("name", "   ")
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `031_search_company_missing_query_parameter`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search") {
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `032_search_given_no_results_when_no_matches_then_returns_200_empty_list`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search?name=ZZZNonExistentCompanyZZZ") {
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    @WithMockUser
    fun `033_search_given_no_results_by_ticker_when_no_matches_then_returns_200_empty_list`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search?ticker=ZZZZ") {
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    @WithMockUser
    fun `034_search_given_valid_query_when_results_exist_then_returns_200_ok_with_results`() {
        setupSearchCompanies()

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
    fun `035_search_given_valid_ticker_query_when_results_exist_then_returns_200_ok_with_results`() {
        setupSearchCompanies()

        mockMvc
            .get("/api/company/search?ticker=MS") {
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].ticker") { value("MSFT") }
                jsonPath("$[0].companyName") { value("Microsoft Corporation") }
            }
    }

    // Pagination Tests

    private fun insertCompanies(count: Int) {
        (1..count).forEach { i ->
            companyRepository.save(Company(ticker = "T$i", companyName = "Company $i"))
        }
    }

    @Test
    @WithMockUser
    fun `036_get_companies_page_1_returns_10_results`() {
        insertCompanies(25)

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.content.length()") { value(10) }
                jsonPath("$.currentPage") { value(1) }
                jsonPath("$.pageSize") { value(10) }
                jsonPath("$.totalElements") { value(25) }
                jsonPath("$.totalPages") { value(3) }
            }
    }

    @Test
    @WithMockUser
    fun `037_get_companies_page_2_returns_10_results`() {
        insertCompanies(25)

        mockMvc
            .get("/api/company?page=2") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.content.length()") { value(10) }
                jsonPath("$.currentPage") { value(2) }
                jsonPath("$.totalPages") { value(3) }
            }
    }

    @Test
    @WithMockUser
    fun `038_get_companies_page_3_returns_remaining_results`() {
        insertCompanies(25)

        mockMvc
            .get("/api/company?page=3") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.content.length()") { value(5) }
                jsonPath("$.currentPage") { value(3) }
                jsonPath("$.totalPages") { value(3) }
            }
    }

    @Test
    @WithMockUser
    fun `039_get_companies_page_out_of_range_returns_404`() {
        insertCompanies(10)

        mockMvc
            .get("/api/company?page=5") {
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser
    fun `040_get_companies_default_page_returns_first_page`() {
        insertCompanies(25)

        mockMvc
            .get("/api/company") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.content.length()") { value(10) }
                jsonPath("$.currentPage") { value(1) }
            }
    }

    @Test
    @WithMockUser
    fun `041_get_companies_page_zero_returns_400`() {
        mockMvc
            .get("/api/company?page=0") {
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `042_get_companies_negative_page_returns_400`() {
        mockMvc
            .get("/api/company?page=-1") {
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser
    fun `043_get_companies_exactly_10_items_returns_1_page`() {
        insertCompanies(10)

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.content.length()") { value(10) }
                jsonPath("$.totalPages") { value(1) }
                jsonPath("$.totalElements") { value(10) }
            }
    }

    @Test
    @WithMockUser
    fun `044_get_companies_exactly_11_items_returns_2_pages`() {
        insertCompanies(11)

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalPages") { value(2) }
                jsonPath("$.totalElements") { value(11) }
            }
    }

    @Test
    @WithMockUser
    fun `045_get_companies_response_contains_total_metadata`() {
        insertCompanies(5)

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.content.length()") { value(5) }
                jsonPath("$.currentPage") { value(1) }
                jsonPath("$.pageSize") { value(10) }
                jsonPath("$.totalElements") { value(5) }
                jsonPath("$.totalPages") { value(1) }
            }
    }

    // Delete Company Tests

    @Test
    @WithMockUser
    fun `046_delete_company_existing_returns_204_no_content`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "DELT",
                    companyName = "Delete Test Company",
                ),
            )

        mockMvc
            .delete("/api/company/${company.id}") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }
    }

    @Test
    @WithMockUser
    fun `047_delete_company_non_existent_returns_404_not_found`() {
        mockMvc
            .delete("/api/company/99999") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser
    fun `048_delete_company_marks_as_inactive`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "VRFY",
                    companyName = "Verify Deletion Company",
                ),
            )

        val id = company.id
        Assertions.assertNotNull(id)

        mockMvc
            .delete("/api/company/$id") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        val deleted = companyRepository.findById(id!!).orElse(null)
        Assertions.assertNotNull(deleted)
        Assertions.assertEquals(false, deleted?.active)
    }

    @Test
    @WithMockUser
    fun `049_delete_company_no_longer_appears_in_search_by_ticker`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "SRCH",
                    companyName = "Search Test Company",
                ),
            )

        mockMvc
            .delete("/api/company/${company.id}") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        mockMvc
            .get("/api/company/search?ticker=SRCH") {
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    @WithMockUser
    fun `050_delete_company_no_longer_appears_in_search_by_name`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "SNAM",
                    companyName = "Unique Search Name Test",
                ),
            )

        mockMvc
            .delete("/api/company/${company.id}") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        mockMvc
            .get("/api/company/search?name=Unique Search Name Test") {
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    @WithMockUser
    fun `051_delete_company_no_longer_appears_in_listing`() {
        val company =
            companyRepository.save(
                Company(
                    ticker = "LIST",
                    companyName = "Listing Test Company",
                ),
            )

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalElements") { value(1) }
            }

        mockMvc
            .delete("/api/company/${company.id}") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalElements") { value(0) }
                jsonPath("$.content.length()") { value(0) }
            }
    }

    @Test
    @WithMockUser
    fun `052_delete_multiple_companies`() {
        val company1 =
            companyRepository.save(
                Company(
                    ticker = "DEL1",
                    companyName = "Delete Test 1",
                ),
            )
        val company2 =
            companyRepository.save(
                Company(
                    ticker = "DEL2",
                    companyName = "Delete Test 2",
                ),
            )
        val company3 =
            companyRepository.save(
                Company(
                    ticker = "DEL3",
                    companyName = "Delete Test 3",
                ),
            )

        mockMvc
            .delete("/api/company/${company1.id}") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        mockMvc
            .delete("/api/company/${company2.id}") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalElements") { value(1) }
                jsonPath("$.content[0].ticker") { value("DEL3") }
            }

        mockMvc
            .delete("/api/company/${company3.id}") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalElements") { value(0) }
            }
    }

    @Test
    @WithMockUser
    fun `053_delete_company_then_recreate_with_same_ticker`() {
        val company1 =
            companyRepository.save(
                Company(
                    ticker = "RECR",
                    companyName = "Recreate Test Company",
                ),
            )

        mockMvc
            .delete("/api/company/${company1.id}") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        val request =
            CreateCompanyRequest(
                ticker = "RECR",
                companyName = "Recreate Test Company New",
            )

        mockMvc
            .post("/api/company") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
            }

        val recreated = companyRepository.findByTickerAndActiveTrue("RECR")
        Assertions.assertNotNull(recreated)
        Assertions.assertEquals("RECR", recreated?.ticker)
        Assertions.assertEquals("Recreate Test Company New", recreated?.companyName)
    }

    @Test
    @WithMockUser
    fun `054_delete_company_soft_delete_preserves_history_and_relationships`() {
        val company = companyRepository.save(Company(ticker = "CASC", companyName = "Cascade Test Company"))
        val user = userRepository.save(User(mail = "cascade@test.com", password = "pass"))

        priceRepository.save(Price(ticker = "CASC", unityPrice = BigDecimal("100.00"), company = company))
        priceRepository.save(Price(ticker = "CASC", unityPrice = BigDecimal("105.00"), company = company))

        historyRepository.save(
            History(
                numberOfStocks = 10,
                transactionValue = BigDecimal("1000.00"),
                transactionTypeEnum = TransactionTypeEnum.BUY,
                company = company,
                user = user,
            ),
        )

        watchlistRepository.save(Watchlist(company = company, user = user))

        val companyId = company.id!!

        // Flush inserts to DB y cargar colecciones lazy en sesión
        entityManager.flush()
        entityManager.refresh(company)

        Assertions.assertEquals(2, company.prices.size)
        Assertions.assertEquals(1, company.history.size)
        Assertions.assertEquals(1, company.watchlist.size)

        mockMvc
            .delete("/api/company/$companyId") {
                with(SecurityMockMvcRequestPostProcessors.csrf())
            }.andExpect {
                status { isNoContent() }
            }

        // Verificar soft delete: compañía marcada como inactiva
        val deletedCompany = companyRepository.findById(companyId).orElse(null)
        Assertions.assertNotNull(deletedCompany)
        Assertions.assertEquals(false, deletedCompany?.active)

        // Verificar que relaciones se mantienen (no se eliminan)
        Assertions.assertEquals(2, priceRepository.findByCompanyIdOrderByTimestampAsc(companyId).size)
        Assertions.assertEquals(1, watchlistRepository.findAll().count { it.company?.id == companyId })
        Assertions.assertEquals(1, historyRepository.findAll().count { it.company?.id == companyId })

        // Verificar que la compañía NO aparece en búsquedas (filtrada por active=true)
        mockMvc
            .get("/api/company/search?ticker=CASC") {
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }

        mockMvc
            .get("/api/company?page=1") {
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalElements") { value(0) }
            }
    }
}