package com.austral.portfolio_tracker.config

import com.austral.portfolio_tracker.company.CompanyRepository
import com.austral.portfolio_tracker.entities.Company
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

data class CompanyData(
    val ticker: String,
    @JsonProperty("company_name")
    val companyName: String,
    val price: BigDecimal? = null,
)

@Component
@Profile("seed")
class DataLoader(
    private val companyRepository: CompanyRepository,
    private val objectMapper: ObjectMapper,
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(DataLoader::class.java)

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
                        log.warn("Batch insert failed, trying individually: ${e.message}")
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
                log.info("$totalSaved companies loaded from JSON, $totalFailed failed (duplicates or errors)")
            }
        } catch (e: Exception) {
            log.error("Error loading companies from JSON: ${e.message}")
        }
    }
}
