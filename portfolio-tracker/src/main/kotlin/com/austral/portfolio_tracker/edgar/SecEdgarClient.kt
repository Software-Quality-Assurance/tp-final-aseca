package com.austral.portfolio_tracker.edgar

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.JsonNode
import java.time.Duration
import java.time.Instant

@Component
class SecEdgarClient(
    @Value("\${app.edgar.user-agent}") private val userAgent: String,
    @Value("\${app.edgar.data-base-url:https://data.sec.gov}") private val dataBaseUrl: String,
    @Value("\${app.edgar.sec-base-url:https://www.sec.gov}") private val secBaseUrl: String,
    @Value("\${app.edgar.efts-base-url:https://efts.sec.gov}") private val eftsBaseUrl: String,
    @Value("\${app.edgar.timeout-seconds:15}") private val timeoutSeconds: Int = 15,
    private val rateLimiter: SecRateLimiter = SecRateLimiter(),
) : SecEdgarGateway {
    private val log = LoggerFactory.getLogger(SecEdgarClient::class.java)
    private val restClient: RestClient
    private val directoryLock = Any()

    @Volatile
    private var cachedDirectory: List<SecCompanyRecord>? = null

    @Volatile
    private var directoryLoadedAt: Instant? = null

    init {
        require(userAgent.isNotBlank() && userAgent.contains("@")) {
            "app.edgar.user-agent must contain a descriptive project name and contact email"
        }
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(timeoutSeconds.toLong()))
                setReadTimeout(Duration.ofSeconds(timeoutSeconds.toLong()))
            }
        restClient =
            RestClient
                .builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build()
    }

    override fun findCompanyByTicker(ticker: String): SecCompanyRecord? {
        val normalized = ticker.trim().uppercase()
        return companyDirectory().firstOrNull { it.ticker == normalized }
    }

    override fun searchCompanies(query: String): List<SecCompanyRecord> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()

        val directory = companyDirectory()
        val localMatches =
            directory.filter {
                it.ticker.contains(normalized, ignoreCase = true) ||
                    it.name.contains(normalized, ignoreCase = true)
            }

        val url =
            UriComponentsBuilder
                .fromUriString("$eftsBaseUrl/LATEST/search-index")
                .queryParam("q", normalized)
                .queryParam("forms", "10-K")
                .queryParam("from", 0)
                .queryParam("size", 50)
                .build()
                .encode()
                .toUriString()
        val searchResult = getJson(url)
        val ciks = mutableSetOf<String>()
        val hits = searchResult.get("hits")?.get("hits")
        if (hits != null) {
            for (hit in hits) {
                val sourceCiks = hit.get("_source")?.get("ciks") ?: continue
                for (cik in sourceCiks) {
                    cik.stringValue()?.let { ciks += it.padStart(10, '0') }
                }
            }
        }

        return (localMatches + directory.filter { it.cik in ciks })
            .distinctBy { it.cik to it.ticker }
            .take(25)
    }

    override fun getCompanyFacts(cik: String): JsonNode = getJson("$dataBaseUrl/api/xbrl/companyfacts/CIK${normalizeCik(cik)}.json")

    override fun getSubmissions(cik: String): JsonNode = getJson("$dataBaseUrl/submissions/CIK${normalizeCik(cik)}.json")

    private fun companyDirectory(): List<SecCompanyRecord> {
        val cached = cachedDirectory
        val loadedAt = directoryLoadedAt
        if (cached != null && loadedAt != null && Duration.between(loadedAt, Instant.now()) < Duration.ofHours(24)) {
            return cached
        }

        synchronized(directoryLock) {
            val secondCheck = cachedDirectory
            val secondLoadedAt = directoryLoadedAt
            if (secondCheck != null && secondLoadedAt != null && Duration.between(secondLoadedAt, Instant.now()) < Duration.ofHours(24)) {
                return secondCheck
            }

            val root = getJson("$secBaseUrl/files/company_tickers.json")
            val directory =
                root.properties().mapNotNull { (_, node) ->
                    val ticker = node.get("ticker")?.textOrNull()?.uppercase() ?: return@mapNotNull null
                    val name = node.get("title")?.textOrNull() ?: return@mapNotNull null
                    val cik = node.get("cik_str")?.asLong()?.toString()?.padStart(10, '0') ?: return@mapNotNull null
                    SecCompanyRecord(cik = cik, ticker = ticker, name = name)
                }
            cachedDirectory = directory
            directoryLoadedAt = Instant.now()
            return directory
        }
    }

    private fun getJson(url: String): JsonNode {
        rateLimiter.acquire()
        try {
            return restClient.get().uri(url).retrieve().body(JsonNode::class.java)
                ?: throw SecEdgarUnavailableException("SEC EDGAR returned an empty response")
        } catch (e: RestClientResponseException) {
            log.warn("SEC EDGAR request failed with status {} for {}", e.statusCode, url)
            throw SecEdgarUnavailableException("SEC EDGAR is temporarily unavailable", e)
        } catch (e: SecEdgarUnavailableException) {
            throw e
        } catch (e: Exception) {
            log.warn("SEC EDGAR request failed for {}: {}", url, e.message)
            throw SecEdgarUnavailableException("SEC EDGAR is temporarily unavailable", e)
        }
    }

    private fun normalizeCik(cik: String): String = cik.filter(Char::isDigit).padStart(10, '0')

    private fun JsonNode.textOrNull(): String? =
        takeUnless { it.isNull }
            ?.toString()
            ?.removeSurrounding("\"")
}
