package com.austral.portfolio_tracker.config

import com.austral.portfolio_tracker.entity.Company
import com.austral.portfolio_tracker.repository.CompanyRepository
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

data class CompanyData(
    val ticker: String,
    @JsonProperty("company_name")
    val companyName: String,
)

@Component
class DataLoader(
    private val companyRepository: CompanyRepository,
    private val objectMapper: ObjectMapper,
) : CommandLineRunner {
    override fun run(vararg args: String) {
        try {
            val jsonResource = ClassPathResource("companies.json")

            if (jsonResource.exists()) {
                val companiesData =
                    objectMapper.readValue(
                        jsonResource.inputStream,
                        Array<CompanyData>::class.java,
                    )

                val companies =
                    companiesData.mapNotNull { data ->
                        if (data.ticker.isNotBlank() && data.companyName.isNotBlank()) {
                            Company(
                                ticker = data.ticker,
                                companyName = data.companyName,
                            )
                        } else {
                            null
                        }
                    }

                var totalSaved = 0
                var totalFailed = 0
                companies.chunked(50).forEach { batch ->
                    try {
                        companyRepository.saveAll(batch)
                        totalSaved += batch.size
                    } catch (e: Exception) {
                        println("Batch insert failed, trying individually: ${e.message}")
                        batch.forEach { company ->
                            try {
                                companyRepository.save(company)
                                totalSaved++
                            } catch (e: Exception) {
                                totalFailed++
                            }
                        }
                    }
                }
                println("$totalSaved companies loaded from JSON, $totalFailed failed (duplicates or errors)")
            }
        } catch (e: Exception) {
            println("Error loading companies from JSON: ${e.message}")
        }
    }
}
