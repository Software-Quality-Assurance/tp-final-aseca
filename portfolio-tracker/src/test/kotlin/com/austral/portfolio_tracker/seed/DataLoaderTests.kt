package com.austral.portfolio_tracker.seed

import com.austral.portfolio_tracker.company.CompanyRepository
import com.austral.portfolio_tracker.config.DataLoader
import com.austral.portfolio_tracker.entities.Company
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.anyList
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

class DataLoaderTests {
    private lateinit var dataLoader: DataLoader
    private lateinit var mockCompanyRepository: CompanyRepository
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        mockCompanyRepository = mock(CompanyRepository::class.java)
        objectMapper = ObjectMapper()
        dataLoader = DataLoader(mockCompanyRepository, objectMapper)
    }

    @Test
    fun `001_should load companies from JSON file`() {
        `when`(mockCompanyRepository.saveAll(anyList())).thenReturn(emptyList())

        dataLoader.run()

        verify(mockCompanyRepository, atLeast(1)).saveAll(anyList())
    }

    @Test
    fun `002_should parse company data correctly`() {
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Company>>
        `when`(mockCompanyRepository.saveAll(anyList())).thenReturn(emptyList())

        dataLoader.run()

        verify(mockCompanyRepository, atLeast(1)).saveAll(captor.capture())
        val allCompanies = captor.allValues.flatten()
        assert(allCompanies.isNotEmpty())
        allCompanies.forEach { company ->
            assert(company.ticker.isNotBlank())
            assert(company.companyName.isNotBlank())
        }
        assert(allCompanies.all { it.prices.isEmpty() })
    }

    @Test
    fun `003_should batch companies in groups of 50`() {
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Company>>
        `when`(mockCompanyRepository.saveAll(anyList())).thenReturn(emptyList())

        dataLoader.run()

        verify(mockCompanyRepository, atLeast(1)).saveAll(captor.capture())
        val batchSizes = captor.allValues.map { it.size }
        assert(batchSizes.isNotEmpty())
        batchSizes.dropLast(1).forEach { size ->
            assert(size == 50) { "Expected batch size 50, got $size" }
        }
        assert(batchSizes.last() <= 50)
    }

    @Test
    fun `004_should handle save exceptions gracefully`() {
        `when`(mockCompanyRepository.saveAll(anyList())).thenThrow(RuntimeException("DB Error"))

        dataLoader.run()

        verify(mockCompanyRepository, atLeast(1)).save(ArgumentMatchers.any())
    }

    @Test
    fun `005_should load correct number of companies`() {
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Company>>
        `when`(mockCompanyRepository.saveAll(anyList())).thenReturn(emptyList())

        dataLoader.run()

        verify(mockCompanyRepository, atLeast(1)).saveAll(captor.capture())
        val totalCompanies = captor.allValues.flatten().size
        assert(totalCompanies == 1000) { "Expected 1000 companies, got $totalCompanies" }
    }
}

@SpringBootTest
@Transactional
class DataLoaderIntegrationTests {
    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Test
    fun `should auto-generate company id when saving without explicit id`() {
        val company =
            Company(
                ticker = "TEST",
                companyName = "Test Company",
            )

        val saved = companyRepository.save(company)

        assert(saved.id != null) { "Company ID should be auto-generated but got null" }
        assert(saved.id!! > 0) { "Company ID should be positive but got ${saved.id}" }
        assert(saved.ticker == "TEST")
        assert(saved.companyName == "Test Company")
    }
}
